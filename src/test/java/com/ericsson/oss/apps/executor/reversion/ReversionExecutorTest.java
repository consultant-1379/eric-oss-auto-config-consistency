/*******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.apps.executor.reversion;

import static com.ericsson.oss.apps.util.TestDefaults.CELL_CAP_MIN_CELL_SUB_CAP;
import static com.ericsson.oss.apps.util.TestDefaults.CELL_CAP_MIN_MAX_WRI_PROT;
import static com.ericsson.oss.apps.util.TestDefaults.CFRA_ENABLE;
import static com.ericsson.oss.apps.util.TestDefaults.ENODE_BFUNCTION;
import static com.ericsson.oss.apps.util.TestDefaults.EUTRAN_CELL_FDD;
import static com.ericsson.oss.apps.util.TestDefaults.MANAGED_ELEMENT_1_FDN;
import static com.ericsson.oss.apps.util.TestDefaults.MANAGED_ELEMENT_2_FDN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.oss.apps.executor.AuditConfig;
import com.ericsson.oss.apps.executor.ChangeStatus;
import com.ericsson.oss.apps.model.AuditResult;
import com.ericsson.oss.apps.model.EaccManagedObject;
import com.ericsson.oss.apps.service.AuditResultService;
import com.ericsson.oss.apps.service.ncmp.NcmpService;

@ExtendWith(MockitoExtension.class)
class ReversionExecutorTest {
    private static final String NODE_FDN = "%s,%s=1".formatted(MANAGED_ELEMENT_1_FDN, ENODE_BFUNCTION);
    private static final String NODE_2_FDN = "%s,%s=1".formatted(MANAGED_ELEMENT_2_FDN, ENODE_BFUNCTION);
    private static final String EUTRAN_CELL_FDN = "%s,%s=1".formatted(NODE_FDN, EUTRAN_CELL_FDD);
    private static final String FORCED_SI_TUNNELING_ACTIVE_ATTR = "forcedSiTunnelingActive";

    @Mock
    private AuditConfig auditConfig;
    @Mock
    private NcmpService ncmpService;
    @Mock
    private AuditResultService auditResultService;
    @Mock
    private BiFunction<String, List<AuditResult>, Boolean> performRevert;
    @InjectMocks
    private ReversionExecutor objectUnderTest;

    @BeforeEach
    void setUp() {
        when(auditConfig.getMoQueueSize()).thenReturn(1);
        when(auditConfig.getRevertThreadPoolSize()).thenReturn(1);

        when(performRevert.apply(anyString(), anyList())).thenAnswer((invocation) -> {
            final var auditResults = (List<AuditResult>) invocation.getArgument(1, List.class);
            auditResults.forEach(auditResult -> auditResult.setChangeStatus(ChangeStatus.REVERSION_COMPLETE));
            return true;
        });

        when(ncmpService.populateEaccManagedObjects(anyMap(), any()))
                .thenAnswer((invocation) -> {
                    invocation.getArgument(1, BlockingQueue.class).put(createCellManagedObject());
                    invocation.getArgument(1, BlockingQueue.class).put(createENodeBManagedObject());
                    return 2;
                });
    }

    @Test
    void whenRevertIsCalled_verifyMethodFlowIsCorrect() {
        final var auditResults = List.of(createNodeAuditResult(NODE_FDN, "true"),
                createCellAuditResult(CELL_CAP_MIN_CELL_SUB_CAP, "1000"),
                createCellAuditResult(CELL_CAP_MIN_MAX_WRI_PROT, "true"),
                createCellAuditResult(CFRA_ENABLE, "false"));

        assertThat(objectUnderTest.revert(auditResults, performRevert))
                .hasEntrySatisfying(ReversionStatus.SUCCESSFUL,
                        (successful) -> assertThat(successful).containsExactlyInAnyOrderElementsOf(auditResults))
                .containsEntry(ReversionStatus.FAILED, Collections.emptyList());

    }

    @Test
    void whenRevertIsCalled_andSomeReversionsFailed_thenSomeFailuresReturned() {
        doAnswer((invocation) -> {
            final var auditResults = (List<AuditResult>) invocation.getArgument(0, List.class);
            final var status = invocation.getArgument(1, ChangeStatus.class);
            auditResults.forEach(auditResult -> auditResult.setChangeStatus(status));
            return true;
        }).when(auditResultService).saveAll(anyList(), any());

        final var failedAuditResults = List.of(
                createNodeAuditResult(NODE_FDN, "false"), // Fails validation
                createNodeAuditResult(NODE_2_FDN, "true"), // MO not 'returned' from NCMP
                createAuditResult(NODE_FDN, ENODE_BFUNCTION, "someAttr", "false") // Attr not 'returned' from NCMP
        );

        final var successfulAuditResults = List.of(
                createCellAuditResult(CELL_CAP_MIN_CELL_SUB_CAP, "1000"),
                createCellAuditResult(CELL_CAP_MIN_MAX_WRI_PROT, "true"),
                createCellAuditResult(CFRA_ENABLE, "false"));

        final var auditResults = new ArrayList<>(failedAuditResults);
        auditResults.addAll(successfulAuditResults);

        assertThat(objectUnderTest.revert(auditResults, performRevert))
                .hasEntrySatisfying(ReversionStatus.SUCCESSFUL, (successful) -> assertThat(successful)
                        .as("Validating successful reversions")
                        .containsExactlyInAnyOrderElementsOf(successfulAuditResults)
                        .allMatch(a -> ChangeStatus.REVERSION_COMPLETE.equals(a.getChangeStatus())))
                .hasEntrySatisfying(ReversionStatus.FAILED, (failed) -> assertThat(failed)
                        .as("Validating failed reversions")
                        .containsExactlyInAnyOrderElementsOf(failedAuditResults)
                        .allMatch(a -> ChangeStatus.REVERSION_FAILED.equals(a.getChangeStatus())));
    }

    private AuditResult createAuditResult(final String fdn, final String moType, final String attr, final String value) {
        final var auditResult = new AuditResult();
        auditResult.setManagedObjectFdn(fdn);
        auditResult.setManagedObjectType(moType);
        auditResult.setAttributeName(attr);
        auditResult.setPreferredValue(value);
        return auditResult;
    }

    private AuditResult createCellAuditResult(final String attr, final String value) {
        return createAuditResult(EUTRAN_CELL_FDN, EUTRAN_CELL_FDD, attr, value);
    }

    private AuditResult createNodeAuditResult(final String fdn, final String value) {
        return createAuditResult(fdn, ENODE_BFUNCTION, FORCED_SI_TUNNELING_ACTIVE_ATTR, value);
    }

    private EaccManagedObject createEaccManagedObject(final String fdn, final String moType, final Map<String, Object> attr) {
        final EaccManagedObject eaccManagedObject = new EaccManagedObject();
        eaccManagedObject.setFdn(fdn);
        eaccManagedObject.setMoType(moType);
        eaccManagedObject.setAttributes(attr);
        return eaccManagedObject;
    }

    private EaccManagedObject createCellManagedObject() {
        return createEaccManagedObject(EUTRAN_CELL_FDN, EUTRAN_CELL_FDN, Map.of(
                CELL_CAP_MIN_CELL_SUB_CAP, 1000,
                CELL_CAP_MIN_MAX_WRI_PROT, true,
                CFRA_ENABLE, false));
    }

    private EaccManagedObject createENodeBManagedObject() {
        return createEaccManagedObject(NODE_FDN, ENODE_BFUNCTION, Map.of(FORCED_SI_TUNNELING_ACTIVE_ATTR, true));
    }
}
