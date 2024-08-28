/*******************************************************************************
 * COPYRIGHT Ericsson 2023 - 2024
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

import static com.ericsson.oss.apps.util.MetricConstants.EACC_CI_APPLIED_CHANGES_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_CI_APPLIED_REVERSIONS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_CI_CHANGES_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_CI_REVERSIONS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.FAILED;
import static com.ericsson.oss.apps.util.MetricConstants.STATUS;
import static com.ericsson.oss.apps.util.MetricConstants.SUCCEEDED;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ericsson.oss.apps.executor.ChangeStatus;
import com.ericsson.oss.apps.executor.ExecutionStatus;
import com.ericsson.oss.apps.executor.reversion.ReversionExecutor;
import com.ericsson.oss.apps.executor.reversion.ReversionStatus;
import com.ericsson.oss.apps.model.AuditResult;
import com.ericsson.oss.apps.model.Execution;
import com.ericsson.oss.apps.repository.ExecutionsRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChangeImplementationService {
    static final Function<AuditResult, String> REVERSION_MAPPER = AuditResult::getCurrentValue;
    private static final Function<AuditResult, String> IMPLEMENTATION_MAPPER = AuditResult::getPreferredValue;
    private static final int PARTIAL_FAILURE_MINIMUM = 1;
    @Autowired
    CMService ncmpService;
    @Autowired
    MetricService metricService;
    @Autowired
    AuditResultService auditResultService;
    @Autowired
    ExecutionsRepository executionsRepository;
    @Autowired
    ReversionExecutor reversionExecutor;

    @Async
    public void processApprovedChangesAsync(final Map<String, List<AuditResult>> auditResultsMap, final Long executionId) {
        int moFailures = 0;
        int attrFailures = 0;
        setExecutionStatus(ExecutionStatus.CHANGES_IN_PROGRESS, executionId);
        metricService.startTimer(String.valueOf(hashCode()), EACC_CI_CHANGES_DURATION_SECONDS);
        for (final Entry<String, List<AuditResult>> fdnAndListOfChanges : auditResultsMap.entrySet()) {
            final String moFdn = fdnAndListOfChanges.getKey();
            final List<AuditResult> listForFdn = fdnAndListOfChanges.getValue();
            final Boolean status = processChangesForFdn(moFdn, listForFdn, IMPLEMENTATION_MAPPER,
                    ChangeStatus.IMPLEMENTATION_COMPLETE, ChangeStatus.IMPLEMENTATION_FAILED);
            if (Boolean.FALSE.equals(status)) {
                moFailures++;
                attrFailures += listForFdn.size();
            }
        }
        if (moFailures == auditResultsMap.entrySet().size()) {
            setExecutionStatus(ExecutionStatus.CHANGES_FAILED, executionId);
        } else if (moFailures >= PARTIAL_FAILURE_MINIMUM) {
            setExecutionStatus(ExecutionStatus.CHANGES_PARTIALLY_SUCCESSFUL, executionId);
        } else {
            setExecutionStatus(ExecutionStatus.CHANGES_SUCCESSFUL, executionId);
        }

        recordMetrics(auditResultsMap.values().stream().mapToInt(List::size).sum(), attrFailures, EACC_CI_APPLIED_CHANGES_TOTAL);
        metricService.stopTimer(String.valueOf(hashCode()), EACC_CI_CHANGES_DURATION_SECONDS);
    }

    @Async
    public void revertChangesAsync(final List<AuditResult> changesToRevert, final Long executionId) {
        setExecutionStatus(ExecutionStatus.REVERSION_IN_PROGRESS, executionId);
        metricService.startTimer(String.valueOf(hashCode()), EACC_CI_REVERSIONS_DURATION_SECONDS);

        final BiFunction<String, List<AuditResult>, Boolean> performRevert = (moFdn, auditResults) -> processChangesForFdn(
                moFdn, auditResults, REVERSION_MAPPER, ChangeStatus.REVERSION_COMPLETE, ChangeStatus.REVERSION_FAILED);
        final Map<ReversionStatus, List<AuditResult>> results = reversionExecutor.revert(changesToRevert, performRevert);

        final int attrFailureCount = results.getOrDefault(ReversionStatus.FAILED, Collections.emptyList()).size();

        if (attrFailureCount == changesToRevert.size()) {
            setExecutionStatus(ExecutionStatus.REVERSION_FAILED, executionId);
        } else if (attrFailureCount >= PARTIAL_FAILURE_MINIMUM) {
            setExecutionStatus(ExecutionStatus.REVERSION_PARTIALLY_SUCCESSFUL, executionId);
        } else {
            setExecutionStatus(ExecutionStatus.REVERSION_SUCCESSFUL, executionId);
        }

        recordMetrics(changesToRevert.size(), attrFailureCount, EACC_CI_APPLIED_REVERSIONS_TOTAL);
        metricService.stopTimer(String.valueOf(hashCode()), EACC_CI_REVERSIONS_DURATION_SECONDS);
    }

    private Boolean processChangesForFdn(final String moFdn, final List<AuditResult> listForFdn, final Function<AuditResult, String> valueMapper,
            final ChangeStatus successfulStatus, final ChangeStatus failedStatus) {
        try {
            ncmpService.patchManagedObjects(moFdn, listForFdn, valueMapper);
            setChangeStatus(listForFdn, successfulStatus);
            log.debug("Applied change(s) for fdn:{}.", moFdn);
            return true;
        } catch (final CMServiceException e) {
            setChangeStatus(listForFdn, failedStatus);
            log.error("Failed to apply change(s) for fdn:%s. {}".formatted(moFdn), e);
            return false;
        }
    }

    private void recordMetrics(final int totalChanges, final int failedChanges, final String metricName) {
        metricService.increment(metricName, totalChanges);
        metricService.increment(metricName, totalChanges - failedChanges, STATUS, SUCCEEDED);
        metricService.increment(metricName, failedChanges, STATUS, FAILED);
    }

    private void setExecutionStatus(final ExecutionStatus executionStatus, final Long executionId) {
        final Optional<Execution> execution = executionsRepository.findById(executionId);
        if (execution.isPresent()) {
            execution.get().setExecutionStatus(executionStatus);
            executionsRepository.save(execution.get());
        }
    }

    private void setChangeStatus(final List<AuditResult> auditResults, final ChangeStatus status) {
        auditResultService.saveAll(auditResults, status);
    }
}
