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

package com.ericsson.oss.apps.service;

import static com.ericsson.oss.apps.executor.ExecutionStatus.AUDIT_SUCCESSFUL;
import static com.ericsson.oss.apps.executor.ExecutionStatus.CHANGES_FAILED;
import static com.ericsson.oss.apps.executor.ExecutionStatus.CHANGES_SUCCESSFUL;
import static com.ericsson.oss.apps.executor.ExecutionType.OPEN_LOOP;
import static com.ericsson.oss.apps.util.DateUtil.getCurrentUtcDateTime;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_CI_APPLIED_REVERSIONS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_CI_REVERSIONS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.FAILED;
import static com.ericsson.oss.apps.util.MetricConstants.STATUS;
import static com.ericsson.oss.apps.util.MetricConstants.SUCCEEDED;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_JOB_NAME;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.oss.apps.api.model.EaccExecution;
import com.ericsson.oss.apps.executor.AuditStatus;
import com.ericsson.oss.apps.executor.ChangeStatus;
import com.ericsson.oss.apps.executor.ExecutionStatus;
import com.ericsson.oss.apps.executor.reversion.ReversionExecutor;
import com.ericsson.oss.apps.executor.reversion.ReversionStatus;
import com.ericsson.oss.apps.model.AuditResult;
import com.ericsson.oss.apps.model.Execution;
import com.ericsson.oss.apps.repository.ExecutionsRepository;
import com.ericsson.oss.apps.service.ncmp.NcmpService;

@ExtendWith(MockitoExtension.class)
public class ChangeImplementationServiceTest {
    private static final Long EXECUTION_ID = 1L;

    private final OffsetDateTime dateNow = getCurrentUtcDateTime();

    @InjectMocks
    private ChangeImplementationService objectUnderTest;

    @Mock
    private NcmpService ncmpService;
    @Mock
    private ExecutionsRepository executionsRepository;
    @Mock
    private AuditResultService auditResultService;
    @Mock
    private MetricService metricService;
    @Mock
    private ReversionExecutor reversionExecutor;

    @BeforeEach
    void setUp() {
        doAnswer((invocation) -> {
            final var auditResults = (List<AuditResult>) invocation.getArgument(0, List.class);
            final var status = invocation.getArgument(1, ChangeStatus.class);
            auditResults.forEach(auditResult -> auditResult.setChangeStatus(status));
            return true;
        }).when(auditResultService).saveAll(anyList(), any());
    }

    @Test
    void whenApplyChangesIsCalledWithValidChanges_thenStatusesAreUpdatedCorrectly() throws CMServiceException {

        final Map<String, List<AuditResult>> auditResultsMappedByFdn = createAuditResultsMappedByFdn();
        final Execution execution = createExecution();

        when(executionsRepository.findById(1L)).thenReturn(Optional.of(execution));
        doNothing().when(ncmpService).patchManagedObjects(anyString(), anyList(), any());

        assertThat(execution.getExecutionStatus()).isEqualTo(AUDIT_SUCCESSFUL);
        auditResultsMappedByFdn.entrySet().stream()
                .forEach(entry -> {
                    final List<AuditResult> auditResults = entry.getValue();
                    for (AuditResult auditResult : auditResults) {
                        assertThat(auditResult.getChangeStatus()).isEqualTo(ChangeStatus.IMPLEMENTATION_IN_PROGRESS);
                    }
                });

        objectUnderTest.processApprovedChangesAsync(auditResultsMappedByFdn, EXECUTION_ID);

        assertThat(execution.getExecutionStatus()).isEqualTo(CHANGES_SUCCESSFUL);
        auditResultsMappedByFdn.entrySet().stream()
                .forEach(entry -> {
                    final List<AuditResult> auditResults = entry.getValue();
                    for (AuditResult auditResult : auditResults) {
                        assertThat(auditResult.getChangeStatus()).isEqualTo(ChangeStatus.IMPLEMENTATION_COMPLETE);
                    }
                });

    }

    @Test
    void whenApplyChangesIsCalledWithInvalidChanges_thenStatusesAreUpdatedCorrectly() throws CMServiceException {

        final Map<String, List<AuditResult>> auditResultsMappedByFdn = createAuditResultsMappedByFdn();
        final Execution execution = createExecution();

        when(executionsRepository.findById(1L)).thenReturn(Optional.of(execution));
        doThrow(CMServiceException.class).when(ncmpService).patchManagedObjects(anyString(), anyList(), any());

        assertThat(execution.getExecutionStatus()).isEqualTo(AUDIT_SUCCESSFUL);
        auditResultsMappedByFdn.entrySet().stream()
                .forEach(entry -> {
                    final List<AuditResult> auditResults = entry.getValue();
                    for (AuditResult auditResult : auditResults) {
                        assertThat(auditResult.getChangeStatus()).isEqualTo(ChangeStatus.IMPLEMENTATION_IN_PROGRESS);
                    }
                });

        objectUnderTest.processApprovedChangesAsync(auditResultsMappedByFdn, EXECUTION_ID);

        assertThat(execution.getExecutionStatus()).isEqualTo(CHANGES_FAILED);
        auditResultsMappedByFdn.entrySet().stream()
                .forEach(entry -> {
                    final List<AuditResult> auditResults = entry.getValue();
                    for (AuditResult auditResult : auditResults) {
                        assertThat(auditResult.getChangeStatus()).isEqualTo(ChangeStatus.IMPLEMENTATION_FAILED);
                    }
                });

    }

    @Test
    void whenRevertChangesIsCalled_thenExecutionStatusIsUpdated() throws CMServiceException {
        final var execution = mockExecution();
        final var auditResults = mockTwoAuditResults();
        mockReversionExecutor(auditResults);

        objectUnderTest.revertChangesAsync(auditResults, 1L);

        verify(execution, times(1)).setExecutionStatus(ExecutionStatus.REVERSION_IN_PROGRESS);
        verify(execution, times(1)).setExecutionStatus(ExecutionStatus.REVERSION_SUCCESSFUL);
        verify(ncmpService, times(1))
                .patchManagedObjects("fdn", auditResults, ChangeImplementationService.REVERSION_MAPPER);
        verify(auditResultService, times(1)).saveAll(auditResults, ChangeStatus.REVERSION_COMPLETE);
        verify(reversionExecutor, times(1)).revert(eq(auditResults), any());

        auditResults.forEach(auditResult -> verify(auditResult, times(1))
                .setChangeStatus(ChangeStatus.REVERSION_COMPLETE));

        verifyMetrics(2, 2, 0);
    }

    @Test
    void whenRevertChangesIsCalled_andNcmpServiceThrowsException_thenExecutionStatusIsUpdated() throws CMServiceException {
        final var execution = mockExecution();
        final var auditResults = mockTwoAuditResults();
        doThrow(CMServiceException.class).when(ncmpService)
                .patchManagedObjects("fdn", auditResults, ChangeImplementationService.REVERSION_MAPPER);
        mockReversionExecutor(auditResults);

        objectUnderTest.revertChangesAsync(auditResults, 1L);

        verify(execution, times(1)).setExecutionStatus(ExecutionStatus.REVERSION_IN_PROGRESS);
        verify(execution, times(1)).setExecutionStatus(ExecutionStatus.REVERSION_FAILED);
        verify(ncmpService, times(1))
                .patchManagedObjects("fdn", auditResults, ChangeImplementationService.REVERSION_MAPPER);
        verify(auditResultService, times(1)).saveAll(auditResults, ChangeStatus.REVERSION_FAILED);
        verify(reversionExecutor, times(1)).revert(eq(auditResults), any());

        auditResults.forEach(auditResult -> verify(auditResult, times(1))
                .setChangeStatus(ChangeStatus.REVERSION_FAILED));
        verifyMetrics(2, 0, 2);
    }

    @Test
    void whenRevertChangesIsCalled_additionalFailedReturned_thenExecutionStatusIsReversionPartiallySuccessful() throws CMServiceException {
        final var execution = mockExecution();
        final var auditResultsToSucceed = mockTwoAuditResults();
        final var auditResultToFail = mock(AuditResult.class);
        final var auditResults = new ArrayList<>(auditResultsToSucceed);
        auditResults.add(auditResultToFail);

        mockReversionExecutor(auditResultsToSucceed, Map.of(ReversionStatus.FAILED, List.of(mock(AuditResult.class))));

        objectUnderTest.revertChangesAsync(auditResults, 1L);

        verify(execution, times(1)).setExecutionStatus(ExecutionStatus.REVERSION_IN_PROGRESS);
        verify(execution, times(1)).setExecutionStatus(ExecutionStatus.REVERSION_PARTIALLY_SUCCESSFUL);
        verify(ncmpService, times(1))
                .patchManagedObjects("fdn", auditResultsToSucceed, ChangeImplementationService.REVERSION_MAPPER);
        verify(auditResultService, times(1)).saveAll(auditResultsToSucceed, ChangeStatus.REVERSION_COMPLETE);
        verify(reversionExecutor, times(1)).revert(eq(auditResults), any());

        auditResultsToSucceed.forEach(auditResult -> verify(auditResult, times(1))
                .setChangeStatus(ChangeStatus.REVERSION_COMPLETE));
        verifyMetrics(3, 2, 1);
    }

    private void verifyMetrics(final int total, final int succeeded, final int failed) {
        verify(metricService, times(1))
                .startTimer(String.valueOf(objectUnderTest.hashCode()), EACC_CI_REVERSIONS_DURATION_SECONDS);
        verify(metricService, times(1)).increment(EACC_CI_APPLIED_REVERSIONS_TOTAL, total);
        verify(metricService, times(1)).increment(EACC_CI_APPLIED_REVERSIONS_TOTAL, succeeded, STATUS, SUCCEEDED);
        verify(metricService, times(1)).increment(EACC_CI_APPLIED_REVERSIONS_TOTAL, failed, STATUS, FAILED);
        verify(metricService, times(1))
                .stopTimer(String.valueOf(objectUnderTest.hashCode()), EACC_CI_REVERSIONS_DURATION_SECONDS);
    }

    private Execution mockExecution() {
        final var execution = mock(Execution.class);
        when(executionsRepository.findById(EXECUTION_ID)).thenReturn(Optional.of(execution));
        return execution;
    }

    private void mockReversionExecutor(final List<AuditResult> auditResults) {
        mockReversionExecutor(auditResults, Collections.emptyMap());
    }

    private void mockReversionExecutor(final List<AuditResult> auditResults, final Map<ReversionStatus, List<AuditResult>> additional) {
        when(reversionExecutor.revert(anyList(), any()))
                .thenAnswer((invocation) -> {
                    final BiFunction<String, List<AuditResult>, Boolean> performRevert = invocation.getArgument(1, BiFunction.class);
                    // Manually invoke performRevert to verify internal #performChangesForFdn
                    final var status = performRevert.apply("fdn", auditResults) ? ReversionStatus.SUCCESSFUL : ReversionStatus.FAILED;
                    final var mapToReturn = new HashMap<>(additional);
                    mapToReturn.computeIfAbsent(status, (s) -> mapToReturn.getOrDefault(s, new ArrayList<>()))
                            .addAll(auditResults);
                    return mapToReturn;
                });
    }

    private List<AuditResult> mockTwoAuditResults() {
        return List.of(mock(AuditResult.class), mock(AuditResult.class));
    }

    private Map<String, List<AuditResult>> createAuditResultsMappedByFdn() {
        final AuditResult auditResult = new AuditResult();
        auditResult.setExecutionId("1");
        auditResult.setId(1L);
        auditResult.setManagedObjectFdn("fdn");
        auditResult.setManagedObjectType("moClass");
        auditResult.setAttributeName("attrName");
        auditResult.setCurrentValue("2");
        auditResult.setPreferredValue("3");
        auditResult.setAuditStatus(AuditStatus.INCONSISTENT);
        auditResult.setRuleId("1");
        auditResult.setChangeStatus(ChangeStatus.IMPLEMENTATION_IN_PROGRESS);
        final AuditResult auditResult2 = new AuditResult();
        auditResult2.setExecutionId("1");
        auditResult2.setId(2L);
        auditResult2.setManagedObjectFdn("fdn2");
        auditResult2.setManagedObjectType("moClass2");
        auditResult2.setAttributeName("attrName2");
        auditResult2.setCurrentValue("2");
        auditResult2.setPreferredValue("2");
        auditResult2.setAuditStatus(AuditStatus.INCONSISTENT);
        auditResult2.setRuleId("1");
        auditResult2.setExecutionId("1");
        auditResult2.setChangeStatus(ChangeStatus.IMPLEMENTATION_IN_PROGRESS);
        return List.of(auditResult, auditResult2).stream()
                .collect(Collectors.groupingBy(AuditResult::getManagedObjectFdn));
    }

    private Execution createExecution() {
        final Execution execution = new Execution();
        execution.setId(EXECUTION_ID);
        execution.setJobName(DEFAULT_JOB_NAME);
        execution.setExecutionStartedAt(dateNow);
        execution.setExecutionEndedAt(dateNow);
        execution.setConsistencyAuditStartedAt(dateNow);
        execution.setConsistencyAuditEndedAt(dateNow);
        execution.setExecutionStatus(AUDIT_SUCCESSFUL);
        execution.setExecutionType(OPEN_LOOP);
        execution.setExecutionStatus(AUDIT_SUCCESSFUL);
        execution.setTotalNodesAudited(2);
        execution.setTotalNodesFailed(1);
        execution.setTotalMosAudited(2);
        execution.setTotalAttributesAudited(4);
        execution.setInconsistenciesIdentified(3);
        return execution;
    }
}
