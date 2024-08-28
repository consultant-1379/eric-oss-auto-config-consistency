/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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

import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_AUDIT_QUEUE_COMPLETED_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.STATUS;
import static com.ericsson.oss.apps.util.MetricConstants.SUCCEEDED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.ericsson.oss.apps.model.EaccManagedObject;
import com.ericsson.oss.apps.model.Execution;
import com.ericsson.oss.apps.model.Rule;
import com.ericsson.oss.apps.repository.AuditResultsRepository;
import com.ericsson.oss.apps.repository.ExecutionsRepository;
import com.ericsson.oss.apps.service.MetricService;

import lombok.extern.slf4j.Slf4j;

/**
 * Class to check for inconsistencies.
 */
@Slf4j
public final class ConsistencyCheckerConsumer {

    private static final Long SCHEDULED_EXECUTOR_INITIAL_DELAY = 0L;

    private static final Long SCHEDULED_EXECUTOR_PERIOD = 30L;
    private final AtomicInteger eaccManagedObjectsProcessed = new AtomicInteger();
    private final AtomicInteger totalAuditResults = new AtomicInteger();
    private final AtomicInteger totalInconsistencies = new AtomicInteger();
    private boolean finished;
    private final ExecutorService executorService;

    private final ScheduledExecutorService scheduledExecutorService;
    private final List<Future<Integer>> workers;

    ConsistencyCheckerConsumer(
            final BlockingQueue<EaccManagedObject> eaccManagedObjectQueue, final Map<String, List<Rule>> ruleSet,
            final Execution execution, final AuditResultsRepository auditResultsRepository,
            final ExecutionsRepository executionsRepository, final MetricService metricService,
            final int workerCount) {
        log.debug("Consistency checker workers count is {}", workerCount);
        workers = new ArrayList<>(workerCount);
        executorService = Executors.newFixedThreadPool(workerCount);
        for (int i = 0; i < workerCount; i++) {
            workers.add(executorService.submit(
                    new ConsistencyCheckerWorker(eaccManagedObjectQueue, ruleSet, execution, auditResultsRepository, metricService)));
        }
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(updateExecutionDuringAudit(execution, executionsRepository),
                SCHEDULED_EXECUTOR_INITIAL_DELAY, SCHEDULED_EXECUTOR_PERIOD, TimeUnit.SECONDS);
        log.info("Number of consistency checker workers is {}", workers.size());
    }

    public boolean waitForCompletion() {
        boolean fullCheck = true;
        try {
            for (final Future future : workers) {
                future.get();
            }
        } catch (final ExecutionException e) {
            fullCheck = false;
            log.warn("ConsistencyCheckerWorker error occurred", e);
        } catch (final InterruptedException e) {
            fullCheck = false;
            log.warn(e.getMessage(), e);
            Thread.currentThread().interrupt();
        } finally {
            executorService.shutdown();
            scheduledExecutorService.shutdown();
        }

        log.info("Finished Auditing '{}' EACC Managed Objects. Created '{}' audit results and found '{}' Inconsistencies.",
                eaccManagedObjectsProcessed, totalAuditResults, totalInconsistencies);
        return fullCheck;
    }

    private Runnable updateExecutionDuringAudit(final Execution execution, final ExecutionsRepository executionsRepository) {
        return () -> {
            execution.setInconsistenciesIdentified(totalInconsistencies.get());
            execution.setTotalAttributesAudited(totalAuditResults.get());
            execution.setTotalMosAudited(eaccManagedObjectsProcessed.get());
            executionsRepository.save(execution);
        };
    }

    public void finish() {
        finished = true;
    }

    public int getTotalAuditResults() {
        return totalAuditResults.get();
    }

    public int getTotalManagedObjects() {
        return eaccManagedObjectsProcessed.get();
    }

    public int getTotalInconsistencies() {
        return totalInconsistencies.get();
    }

    private class ConsistencyCheckerWorker implements Callable<Integer> {
        final BlockingQueue<EaccManagedObject> eaccManagedObjectQueue;
        final Map<String, List<Rule>> ruleSet;
        final Execution execution;
        final AuditResultsRepository auditResultsRepository;
        final MetricService metricService;

        ConsistencyCheckerWorker(final BlockingQueue<EaccManagedObject> eaccManagedObjectQueue, final Map<String, List<Rule>> ruleSet,
                final Execution execution, final AuditResultsRepository auditResultsRepository, final MetricService metricService) {
            this.eaccManagedObjectQueue = eaccManagedObjectQueue;
            this.ruleSet = ruleSet;
            this.execution = execution;
            this.auditResultsRepository = auditResultsRepository;
            this.metricService = metricService;

        }

        @Override
        public Integer call() throws Exception {
            log.info("Starting ConsistencyChecker...");
            int workerInconsistencyCount = 0;
            int workerauditCount = 0;
            int workerManagedObjectsProcessedCount = 0;
            do {
                final EaccManagedObject eaccManagedObject = eaccManagedObjectQueue.poll(2, TimeUnit.SECONDS);
                if (eaccManagedObject != null) {
                    log.debug("ConsistencyChecker starting processing object '{}'", eaccManagedObject.getFdn());
                    final List<Rule> moRuleSet = getMoRuleSetForManagedObject(eaccManagedObject);
                    final ConsistencyCheckResult consistencyCheckResult = ConsistencyChecker.check(eaccManagedObject, moRuleSet,
                            execution.getId().toString());
                    workerInconsistencyCount += consistencyCheckResult.getInconsistencyCount();
                    totalInconsistencies.addAndGet(consistencyCheckResult.getInconsistencyCount());
                    auditResultsRepository.saveAll(consistencyCheckResult.getAuditResultList());
                    totalAuditResults.addAndGet(consistencyCheckResult.getAuditResultList().size());
                    eaccManagedObjectsProcessed.incrementAndGet();
                    workerauditCount += consistencyCheckResult.getAuditResultList().size();
                    workerManagedObjectsProcessedCount++;
                    metricService.increment(EACC_EXECUTIONS_AUDIT_QUEUE_COMPLETED_TOTAL, STATUS, SUCCEEDED);
                    log.debug("ConsistencyChecker finished processing object '{}'", eaccManagedObject.getFdn());
                } else {
                    log.debug("ConsistencyChecker nothing on blocking queue at the moment...");
                }

            } while (!(finished && eaccManagedObjectQueue.isEmpty()));
            log.info("Completed ConsistencyChecker for '{}' ManagedObjects with '{}' inconsistencies for '{}' " +
                    "audited objects (total processed at this stage '{}')",
                    workerManagedObjectsProcessedCount, workerInconsistencyCount, workerauditCount, eaccManagedObjectsProcessed.get());
            return workerInconsistencyCount;
        }

        private List<Rule> getMoRuleSetForManagedObject(final EaccManagedObject eaccManagedObject) {
            final List<Rule> moRuleList = ruleSet.get(eaccManagedObject.getMoType());
            if (moRuleList == null) {
                return Collections.emptyList();
            }
            return moRuleList;
        }
    }
}
