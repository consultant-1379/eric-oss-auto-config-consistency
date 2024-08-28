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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.oss.apps.executor.ChangeStatus;
import com.ericsson.oss.apps.model.AuditResult;
import com.ericsson.oss.apps.model.EaccManagedObject;
import com.ericsson.oss.apps.service.AuditResultService;

@ExtendWith(MockitoExtension.class)
class ReversionExecutorConsumerTest {
    private static final String NODE_FDN = "%s,%s=1".formatted(MANAGED_ELEMENT_1_FDN, ENODE_BFUNCTION);
    private static final String EUTRAN_CELL_FDN = "%s,%s=1".formatted(NODE_FDN, EUTRAN_CELL_FDD);
    private static final String FORCED_SI_TUNNELING_ACTIVE_ATTR = "forcedSiTunnelingActive";

    private final BlockingQueue<EaccManagedObject> eaccManagedObjectQueue = new LinkedBlockingQueue<>();
    @Mock
    private AuditResultService auditResultService;
    @Mock
    private BiFunction<String, List<AuditResult>, Boolean> performRevert;
    private final Map<String, List<AuditResult>> auditResultByMo = new HashMap<>();

    private ReversionExecutorConsumer objectUnderTest;

    @BeforeEach
    void setUp() {
        auditResultByMo.clear();
        eaccManagedObjectQueue.clear();

        objectUnderTest = new ReversionExecutorConsumer(
                auditResultService, eaccManagedObjectQueue, auditResultByMo, 1, performRevert);
    }

    @Test
    void whenManagedObjectsAreOnQueue_andFlowRunsSuccessfully_verifyCounts() throws InterruptedException {
        final var nodeAuditResults = List.of(createAuditResult(FORCED_SI_TUNNELING_ACTIVE_ATTR, "true"));
        final var cellAuditResults = List.of(createAuditResult(CELL_CAP_MIN_CELL_SUB_CAP, "1000"),
                createAuditResult(CELL_CAP_MIN_MAX_WRI_PROT, "true"),
                createAuditResult(CFRA_ENABLE, "false"));

        final ArrayList<AuditResult> expectedSuccessful = new ArrayList<>();
        expectedSuccessful.addAll(nodeAuditResults);
        expectedSuccessful.addAll(cellAuditResults);

        auditResultByMo.put(NODE_FDN, nodeAuditResults);
        auditResultByMo.put(EUTRAN_CELL_FDN, cellAuditResults);
        when(performRevert.apply(anyString(), anyList())).thenReturn(true);

        eaccManagedObjectQueue.put(createCellManagedObject());
        eaccManagedObjectQueue.put(createENodeBManagedObject());

        objectUnderTest.finish();
        assertThat(objectUnderTest.waitForCompletion()).isTrue();
        assertThat(objectUnderTest.getTotalChangesReverted()).isEqualTo(4);
        assertThat(objectUnderTest.getSuccessfulAuditResults()).containsExactlyInAnyOrderElementsOf(expectedSuccessful);
        assertThat(objectUnderTest.getFailedAuditResults()).isEmpty();

        verify(performRevert, times(1)).apply(NODE_FDN, nodeAuditResults);
        verify(performRevert, times(1)).apply(EUTRAN_CELL_FDN, cellAuditResults);
        verifyNoMoreInteractions(performRevert, auditResultService);
    }

    @Test
    void whenManagedObjectsAreOnQueue_performRevertFails_verifyCounts() throws InterruptedException {
        final var nodeAuditResults = List.of(createAuditResult(FORCED_SI_TUNNELING_ACTIVE_ATTR, "true"));
        final var cellAuditResults = List.of(createAuditResult(CELL_CAP_MIN_CELL_SUB_CAP, "1000"),
                createAuditResult(CELL_CAP_MIN_MAX_WRI_PROT, "true"),
                createAuditResult(CFRA_ENABLE, "false"));

        final ArrayList<AuditResult> expectedFailures = new ArrayList<>();
        expectedFailures.addAll(nodeAuditResults);
        expectedFailures.addAll(cellAuditResults);

        auditResultByMo.put(NODE_FDN, nodeAuditResults);
        auditResultByMo.put(EUTRAN_CELL_FDN, cellAuditResults);
        when(performRevert.apply(anyString(), anyList())).thenReturn(false);

        eaccManagedObjectQueue.put(createCellManagedObject());
        eaccManagedObjectQueue.put(createENodeBManagedObject());

        objectUnderTest.finish();
        assertThat(objectUnderTest.waitForCompletion()).isTrue();
        assertThat(objectUnderTest.getTotalChangesReverted()).isEqualTo(4);
        assertThat(objectUnderTest.getSuccessfulAuditResults()).isEmpty();
        assertThat(objectUnderTest.getFailedAuditResults()).containsExactlyInAnyOrderElementsOf(expectedFailures);

        verify(performRevert, times(1)).apply(NODE_FDN, nodeAuditResults);
        verify(performRevert, times(1)).apply(EUTRAN_CELL_FDN, cellAuditResults);
        verify(auditResultService, times(2)).saveAll(anyList(), eq(ChangeStatus.REVERSION_FAILED));
        verifyNoMoreInteractions(performRevert);
    }

    @Test
    void whenManagedObjectsAreOnQueue_andSomeValidationFails_verifyCounts() throws InterruptedException {
        final var nodeAuditResults = List.of(createAuditResult(FORCED_SI_TUNNELING_ACTIVE_ATTR, "true"));

        final var failedAuditResults = List.of(createAuditResult(CELL_CAP_MIN_CELL_SUB_CAP, "500"),
                createAuditResult(CELL_CAP_MIN_MAX_WRI_PROT, "false"));
        final var successfulCellAuditResults = List.of(createAuditResult(CFRA_ENABLE, "false"));

        final var cellAuditResults = new ArrayList<>(failedAuditResults);
        cellAuditResults.addAll(successfulCellAuditResults);

        final ArrayList<AuditResult> expectedSuccessful = new ArrayList<>(nodeAuditResults);
        expectedSuccessful.addAll(successfulCellAuditResults);

        auditResultByMo.put(NODE_FDN, nodeAuditResults);
        auditResultByMo.put(EUTRAN_CELL_FDN, cellAuditResults);
        when(performRevert.apply(anyString(), anyList())).thenReturn(true);

        eaccManagedObjectQueue.put(createCellManagedObject());
        eaccManagedObjectQueue.put(createENodeBManagedObject());

        objectUnderTest.finish();
        assertThat(objectUnderTest.waitForCompletion()).isTrue();
        assertThat(objectUnderTest.getTotalChangesReverted()).isEqualTo(4);
        assertThat(objectUnderTest.getSuccessfulAuditResults())
                .containsExactlyInAnyOrderElementsOf(expectedSuccessful);
        assertThat(objectUnderTest.getFailedAuditResults())
                .containsExactlyInAnyOrderElementsOf(failedAuditResults);

        verify(performRevert, times(1)).apply(NODE_FDN, nodeAuditResults);
        verify(performRevert, times(1)).apply(EUTRAN_CELL_FDN, successfulCellAuditResults);
        verify(auditResultService, times(1)).saveAll(anyList(), eq(ChangeStatus.REVERSION_FAILED));
        verifyNoMoreInteractions(performRevert);
    }

    @Test
    void whenManagedObjectsAreOnQueue_andPerformRevertThrowsException_verifyCounts() throws InterruptedException {
        final var nodeAuditResults = List.of(createAuditResult(FORCED_SI_TUNNELING_ACTIVE_ATTR, "true"));

        final var failedCellAuditResults = List.of(createAuditResult(CELL_CAP_MIN_CELL_SUB_CAP, "500"),
                createAuditResult(CELL_CAP_MIN_MAX_WRI_PROT, "false"));

        final var successfulCellAuditResults = List.of(createAuditResult(CFRA_ENABLE, "false"));

        final var expectedFailed = new ArrayList<>(nodeAuditResults);
        expectedFailed.addAll(failedCellAuditResults);

        final var cellAuditResults = new ArrayList<>(failedCellAuditResults);
        cellAuditResults.addAll(successfulCellAuditResults);

        final ArrayList<AuditResult> expectedSuccessful = new ArrayList<>(successfulCellAuditResults);

        auditResultByMo.put(NODE_FDN, nodeAuditResults);
        auditResultByMo.put(EUTRAN_CELL_FDN, cellAuditResults);

        when(performRevert.apply(anyString(), anyList()))
                .thenThrow(RuntimeException.class)
                .thenReturn(true);

        eaccManagedObjectQueue.put(createENodeBManagedObject());
        eaccManagedObjectQueue.put(createCellManagedObject());

        objectUnderTest.finish();
        assertThat(objectUnderTest.waitForCompletion()).isTrue();
        assertThat(objectUnderTest.getTotalChangesReverted()).isEqualTo(4);
        assertThat(objectUnderTest.getSuccessfulAuditResults())
                .containsExactlyInAnyOrderElementsOf(expectedSuccessful);
        assertThat(objectUnderTest.getFailedAuditResults())
                .containsExactlyInAnyOrderElementsOf(expectedFailed);

        verify(performRevert, times(1)).apply(NODE_FDN, nodeAuditResults);
        verify(performRevert, times(1)).apply(EUTRAN_CELL_FDN, successfulCellAuditResults);
        verify(auditResultService, times(2)).saveAll(anyList(), eq(ChangeStatus.REVERSION_FAILED));
        verifyNoMoreInteractions(performRevert);
    }

    @Test
    void whenNoManagedObjectsInQueue_andFinish_verifyAllCountsAreZero() {
        objectUnderTest.finish();

        assertThat(objectUnderTest.waitForCompletion()).isTrue();
        assertThat(objectUnderTest.getTotalChangesReverted()).isZero();
        assertThat(objectUnderTest.getSuccessfulAuditResults()).isEmpty();
        assertThat(objectUnderTest.getFailedAuditResults()).isEmpty();

        verifyNoInteractions(performRevert, auditResultService);
    }

    private AuditResult createAuditResult(final String attr, final String value) {
        final var auditResult = new AuditResult();
        auditResult.setAttributeName(attr);
        auditResult.setPreferredValue(value);
        auditResult.setExecutionId("1");
        return auditResult;
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
