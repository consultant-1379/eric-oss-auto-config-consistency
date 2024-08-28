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

package com.ericsson.oss.apps.executor;

import static com.ericsson.oss.apps.executor.ExecutionType.OPEN_LOOP;
import static com.ericsson.oss.apps.util.Constants.DATABASE_OPERATION_FAILED;
import static com.ericsson.oss.apps.util.DateUtil.getCurrentUtcDateTime;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_ATTR_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_MO_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_RULES_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.FAILED;
import static com.ericsson.oss.apps.util.MetricConstants.PARTIAL;
import static com.ericsson.oss.apps.util.MetricConstants.SKIPPED;
import static com.ericsson.oss.apps.util.MetricConstants.STATUS;
import static com.ericsson.oss.apps.util.MetricConstants.SUCCEEDED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.TransactionException;

import com.ericsson.oss.apps.model.EaccManagedObject;
import com.ericsson.oss.apps.model.Execution;
import com.ericsson.oss.apps.model.Job;
import com.ericsson.oss.apps.model.Rule;
import com.ericsson.oss.apps.model.Scope;
import com.ericsson.oss.apps.repository.AuditResultsRepository;
import com.ericsson.oss.apps.repository.ExecutionsRepository;
import com.ericsson.oss.apps.repository.ScopeRepository;
import com.ericsson.oss.apps.service.CMService;
import com.ericsson.oss.apps.service.CMServiceException;
import com.ericsson.oss.apps.service.MetricService;
import com.ericsson.oss.apps.service.RetentionService;
import com.ericsson.oss.apps.service.RuleService;
import com.ericsson.oss.apps.service.Services;
import com.ericsson.oss.apps.validation.RuleSetValidator;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to run the consistency checks.
 */
@Slf4j
public class ConsistencyCheckExecutor implements Runnable {
    private static boolean isAuditInProgress;
    private static final Object LOCK = new Object();
    private static RetryTemplate retryTemplate;
    private final AuditResultsRepository auditResultsRepository;
    private final ExecutionsRepository executionsRepository;
    private final ScopeRepository scopeRepository;
    private final CMService ncmpService;
    private final Job job;
    private final RuleService ruleService;
    private final RetentionService retentionService;
    private final MetricService metricService;
    private final AuditConfig auditConfig;
    private final RuleSetValidator ruleSetValidator;
    private int numberOfRules;

    public ConsistencyCheckExecutor(final AuditResultsRepository auditResultsRepository, final Job job,
            final ExecutionsRepository executionsRepository, final Services services,
            final ScopeRepository scopeRepository, final AuditConfig auditConfig) {
        this.auditResultsRepository = auditResultsRepository;
        this.job = job;
        this.executionsRepository = executionsRepository;
        ncmpService = services.getCmService();
        ruleService = services.getRuleService();
        retentionService = services.getRetentionService();
        metricService = services.getMetricService();
        this.scopeRepository = scopeRepository;
        this.auditConfig = auditConfig;
        ruleSetValidator = services.getRuleSetValidator();
    }

    @Override
    public void run() {
        log.info("Consistency Check Executor Has Been Started.");
        log.info("Using Job: {}", job);

        class ExecutionHolder {

            private Execution execution;
            @Getter
            @Setter
            private boolean skippedExecution;

            public Execution getExecution() {
                return execution;
            }

            public void setExecution(final Execution execution) {
                this.execution = execution;
            }
        }

        final ExecutionHolder executionHolder = new ExecutionHolder();

        final Execution execution = new Execution();
        execution.setJobName(job.getJobName());
        execution.setRuleSetName(job.getRuleSetName());
        execution.setScopeName(job.getScopeName());
        execution.setExecutionStartedAt(getCurrentUtcDateTime());
        execution.setExecutionStatus(ExecutionStatus.AUDIT_IN_PROGRESS);
        execution.setExecutionType(OPEN_LOOP);
        numberOfRules = 0;
        startExecutionMetrics();

        try {
            retryTemplate.execute(context -> {
                final int attemptNumber = context.getRetryCount() + 1;
                try {
                    log.info("Attempt {} at performing audit", attemptNumber);
                    if (!canSetInProgress()) {
                        log.warn("Audit skipped for job name {} as another execution is in progress.", job.getJobName());
                        log.warn("Execution: {} of job {}: skipped,", execution, job);
                        executionHolder.setExecution(execution);
                        executionHolder.setSkippedExecution(true);
                        incrementExecutionMetrics(executionHolder.getExecution(), SKIPPED);
                        executionHolder.getExecution().setExecutionStatus((ExecutionStatus.AUDIT_SKIPPED));
                        executionsRepository.save(executionHolder.getExecution());
                        return null;
                    }
                    executionHolder.setExecution(executionsRepository.save(execution));

                    if (performAudit(executionHolder.getExecution())) {
                        executionHolder.getExecution().setExecutionStatus(ExecutionStatus.AUDIT_SUCCESSFUL);
                        incrementExecutionMetrics(executionHolder.getExecution(), SUCCEEDED);
                    } else {
                        executionHolder.getExecution().setExecutionStatus(ExecutionStatus.AUDIT_PARTIALLY_SUCCESSFUL);
                        incrementExecutionMetrics(executionHolder.getExecution(), PARTIAL);
                    }
                    executionHolder.getExecution().setExecutionEndedAt(getCurrentUtcDateTime());
                    executionsRepository.save(executionHolder.getExecution());

                    log.info("Attempt {} at performing audit was successful", attemptNumber);
                    log.info("Execution Succeeded for job: {}", execution.getJobName());
                    log.info("Consistency Check Executor Has Succeeded For Job: {}", job);
                } catch (final DataAccessException | TransactionException e) {
                    log.info("Attempt {} at performing audit was unsuccessful. Reason: {}", attemptNumber, DATABASE_OPERATION_FAILED);
                    throw e;
                } catch (final Exception e) {
                    log.error("Attempt {} at performing audit was unsuccessful", attemptNumber);
                    log.error("Execution: {} of job {}: failed,", executionHolder.getExecution(), job, e);
                    incrementExecutionMetrics(executionHolder.getExecution(), FAILED);
                    executionHolder.getExecution().setExecutionStatus((ExecutionStatus.AUDIT_FAILED));
                    executionsRepository.save(executionHolder.getExecution());
                } finally {
                    if (!executionHolder.isSkippedExecution()) {
                        clearAuditInProgress();
                    }
                }
                return null;
            });
        } catch (final DataAccessException | TransactionException e) {
            log.error("Execution: {} of job {}: failed,", executionHolder.getExecution(), job, e);
            incrementExecutionMetrics(executionHolder.getExecution(), FAILED);
            executionHolder.getExecution().setExecutionStatus((ExecutionStatus.AUDIT_FAILED));
            executionsRepository.save(executionHolder.getExecution());
        }
        retentionService.clearExecutionsByCount();
        log.info("Consistency Check Executor Has Finished.");
    }

    private boolean performAudit(final Execution execution) throws CMServiceException {
        log.info("Performing Audit");
        final List<Rule> rules = ruleService.getRulesForRuleset(job.getRuleSetName());
        log.debug("Rules retrieved for Ruleset '{}': {}", job.getRuleSetName(), rules);
        final Map<String, List<Rule>> rulesByMoType = ruleService.getRulesByMoType(rules);
        numberOfRules = rulesByMoType.values().stream().mapToInt(Collection::size).sum();
        log.debug("Rules grouped by MoType: {}", rulesByMoType);
        final Collection<String> scopeFdns = getScopeFdns(job.getScopeName());

        final Map<String, Map<String, List<Rule>>> fdnToRulesByMoType = ruleSetValidator.validateNodeLevelRules(scopeFdns, rulesByMoType);
        final Map<String, Map<String, List<String>>> fdnToAttrByMoType = ruleService.getAttributesFromMoLevelRules(fdnToRulesByMoType);

        log.info("Consistency Checker ManageObject queue size is {}", auditConfig.getMoQueueSize());
        final BlockingQueue<EaccManagedObject> eaccManagedObjectsQueue = new LinkedBlockingDeque<>(auditConfig.getMoQueueSize());

        execution.setConsistencyAuditStartedAt(getCurrentUtcDateTime());

        final ConsistencyCheckerConsumer consistencyCheckerConsumer = new ConsistencyCheckerConsumer(
                eaccManagedObjectsQueue, rulesByMoType, execution, auditResultsRepository, executionsRepository,
                metricService, auditConfig.getThreadPoolSize());
        final int nodesSuccessfullRead = ncmpService.populateEaccManagedObjects(fdnToAttrByMoType, eaccManagedObjectsQueue);

        consistencyCheckerConsumer.finish(); //indicate the producers have all finished (i.e. EACC MOs have been populated with NCMP data)
        final boolean fullCheckCompleted = consistencyCheckerConsumer.waitForCompletion();

        execution.setTotalAttributesAudited(consistencyCheckerConsumer.getTotalAuditResults());
        execution.setTotalMosAudited(consistencyCheckerConsumer.getTotalManagedObjects());
        execution.setInconsistenciesIdentified(consistencyCheckerConsumer.getTotalInconsistencies());
        execution.setConsistencyAuditEndedAt(getCurrentUtcDateTime());
        execution.setTotalNodesAudited(fdnToRulesByMoType.size());
        execution.setTotalNodesFailed(fdnToRulesByMoType.size() - nodesSuccessfullRead);

        if (fdnToRulesByMoType.size() == nodesSuccessfullRead && fullCheckCompleted) {
            return true;
        } else if (nodesSuccessfullRead == 0) {
            throw new CMServiceException(String.format("Not possible to read information from NCMP for Job: %s", job));
        }
        return false;
    }

    private void startExecutionMetrics() {
        metricService.startTimer(String.valueOf(hashCode()), EACC_EXECUTIONS_DURATION_SECONDS, STATUS, SKIPPED);
        metricService.startTimer(String.valueOf(hashCode()), EACC_EXECUTIONS_DURATION_SECONDS, STATUS, SUCCEEDED);
        metricService.startTimer(String.valueOf(hashCode()), EACC_EXECUTIONS_DURATION_SECONDS, STATUS, FAILED);
        metricService.startTimer(String.valueOf(hashCode()), EACC_EXECUTIONS_DURATION_SECONDS, STATUS, PARTIAL);
    }

    private void incrementExecutionMetrics(final Execution execution, final String status) {
        metricService.stopTimer(String.valueOf(hashCode()), EACC_EXECUTIONS_DURATION_SECONDS, STATUS, status);
        metricService.increment(EACC_EXECUTIONS_TOTAL, STATUS, status);
        metricService.increment(EACC_EXECUTIONS_MO_TOTAL, execution.getTotalMosAudited(), STATUS, status);
        metricService.increment(EACC_EXECUTIONS_RULES_TOTAL, numberOfRules, STATUS, status);
        metricService.increment(EACC_EXECUTIONS_ATTR_TOTAL, execution.getTotalAttributesAudited(), STATUS, status);
    }

    protected boolean canSetInProgress() {
        synchronized (LOCK) {
            if (!isAuditInProgress) {
                isAuditInProgress = true;
                return true;
            }
            return false;
        }
    }

    protected void clearAuditInProgress() {
        synchronized (LOCK) {
            isAuditInProgress = false;
        }
    }

    protected List<String> getScopeFdns(final String scopeName) {
        final Optional<Scope> scope = scopeRepository.findByName(scopeName);
        if (scope.isPresent()) {
            final List<String> allFdns = scope.get().getFdns();
            final Set<String> uniqueFdns = allFdns.stream().collect(Collectors.toSet());
            if (uniqueFdns.size() < allFdns.size()) {
                final Set<String> duplicateFdns = allFdns.stream()
                        .filter(fdn -> Collections.frequency(allFdns, fdn) > 1)
                        .collect(Collectors.toSet());
                log.warn("Each Fdn will only be processed once. Duplicate Fdns were found in the scope: {}."
                        + " The duplicate Fdns were: {}", scopeName, duplicateFdns);
            }
            return new ArrayList<>(uniqueFdns);
        }
        return Collections.emptyList();
    }

    public static void setRetryTemplate(final RetryTemplate retryTemplate) {
        ConsistencyCheckExecutor.retryTemplate = retryTemplate;
    }
}
