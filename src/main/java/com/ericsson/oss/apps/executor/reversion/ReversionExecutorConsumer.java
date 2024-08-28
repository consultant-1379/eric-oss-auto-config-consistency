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

import static java.util.Collections.synchronizedList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import com.ericsson.oss.apps.executor.ChangeStatus;
import com.ericsson.oss.apps.model.AuditResult;
import com.ericsson.oss.apps.model.EaccManagedObject;
import com.ericsson.oss.apps.service.AuditResultService;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class ReversionExecutorConsumer {
    private final AtomicInteger eaccManagedObjectsProcessed = new AtomicInteger();
    private final AtomicInteger totalAuditResultsProcessed = new AtomicInteger();
    private final AtomicInteger totalChangesValidated = new AtomicInteger();
    private final List<AuditResult> successfulAuditResults = synchronizedList(new ArrayList<>());
    private final List<AuditResult> failedAuditResults = synchronizedList(new ArrayList<>());
    private final ExecutorService executorService;
    private final List<Future<Integer>> workers;

    private boolean finished;
    @Getter
    private int totalChangesReverted;

    ReversionExecutorConsumer(final AuditResultService auditResultsRepository, final BlockingQueue<EaccManagedObject> eaccManagedObjects,
            final Map<String, List<AuditResult>> changesByMoFdn, final int workerCount,
            final BiFunction<String, List<AuditResult>, Boolean> performRevert) {
        executorService = Executors.newFixedThreadPool(workerCount);
        workers = new ArrayList<>();
        for (int i = 0; i < workerCount; i++) {
            workers.add(executorService.submit(new ReversionWorker(auditResultsRepository, eaccManagedObjects, changesByMoFdn, performRevert)));
        }
    }

    public void finish() {
        finished = true;
    }

    public boolean waitForCompletion() {
        boolean fullCheck = true;
        int changesRevertedCount = 0;
        for (final Future<Integer> future : workers) {
            try {
                changesRevertedCount += future.get();
            } catch (final ExecutionException e) {
                fullCheck = false;
                log.warn("ReversionWorker error occurred", e);
            } catch (final InterruptedException e) {
                fullCheck = false;
                log.warn(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }
        totalChangesReverted = changesRevertedCount;

        executorService.shutdown();
        log.info("Finished reverting '{}' changes for '{}' EACC Managed Objects. {} total changes to revert, {} reversions attempted, {} successful.",
                totalAuditResultsProcessed.get(), eaccManagedObjectsProcessed.get(), totalChangesReverted, totalChangesValidated.get(),
                successfulAuditResults.size());
        return fullCheck;
    }

    public List<AuditResult> getSuccessfulAuditResults() {
        return Collections.unmodifiableList(successfulAuditResults);
    }

    public List<AuditResult> getFailedAuditResults() {
        return Collections.unmodifiableList(failedAuditResults);
    }

    @RequiredArgsConstructor
    private class ReversionWorker implements Callable<Integer> {
        private final AuditResultService auditResultService;
        private final BlockingQueue<EaccManagedObject> eaccManagedObjects;
        private final Map<String, List<AuditResult>> changesByMoFdn;
        private final BiFunction<String, List<AuditResult>, Boolean> performRevert;

        @Override
        public Integer call() throws Exception {
            int auditResultsProcessed = 0;

            do {
                final EaccManagedObject eaccManagedObject = eaccManagedObjects.poll(2, TimeUnit.SECONDS);

                if (Objects.nonNull(eaccManagedObject) && changesByMoFdn.containsKey(eaccManagedObject.getFdn())) {
                    final List<AuditResult> auditResultsForMo = changesByMoFdn.get(eaccManagedObject.getFdn());
                    final List<AuditResult> validatedForMo = new ArrayList<>();
                    final List<AuditResult> failedForMo = new ArrayList<>();

                    validateAuditResults(eaccManagedObject, auditResultsForMo, validatedForMo, failedForMo);

                    if (!validatedForMo.isEmpty() && revertChanges(eaccManagedObject.getFdn(), validatedForMo)) {
                        successfulAuditResults.addAll(validatedForMo);
                    } else {
                        failedForMo.addAll(validatedForMo);
                    }

                    if (!failedForMo.isEmpty()) {
                        failAuditResults(eaccManagedObject.getFdn(), failedForMo);
                    }

                    auditResultsProcessed += auditResultsForMo.size();
                    eaccManagedObjectsProcessed.incrementAndGet();
                    totalChangesValidated.addAndGet(validatedForMo.size());
                    totalAuditResultsProcessed.addAndGet(auditResultsForMo.size());
                } else {
                    log.debug("No Managed Objects for reversion on blocking queue at the moment");
                }

            } while (!(finished && eaccManagedObjects.isEmpty()));

            return auditResultsProcessed;
        }

        private boolean revertChanges(final String moFdn, final List<AuditResult> changesToRevert) {
            try {
                return performRevert.apply(moFdn, changesToRevert);
            } catch (final Exception e) {
                log.error("Failed to revert changes for MO '{}'", moFdn, e);
                return false;
            }
        }

        private void validateAuditResults(final EaccManagedObject eaccManagedObject, final List<AuditResult> auditResultsForMo,
                final List<AuditResult> validated, final List<AuditResult> failed) {
            auditResultsForMo.forEach(auditResult -> {
                if (!eaccManagedObject.getAttributes().containsKey(auditResult.getAttributeName())) {
                    log.warn("Reversion failed for attribute '{}' because attribute was not present for MO '{}'.",
                            auditResult.getAttributeName(), eaccManagedObject.getFdn());
                    failed.add(auditResult);
                    return;
                }
                final Object attrValue = eaccManagedObject.getAttributes().get(auditResult.getAttributeName());
                if (!auditResult.getPreferredValue().equals(attrValue.toString())) {
                    log.warn("Reversion failed for attribute '{}' because attribute value does not match value set by EACC for MO '{}'.",
                            auditResult.getAttributeName(), eaccManagedObject.getFdn());
                    failed.add(auditResult);
                    return;
                }
                validated.add(auditResult);
            });
        }

        private void failAuditResults(final String moFdn, final List<AuditResult> auditResults) {
            try {
                auditResultService.saveAll(auditResults, ChangeStatus.REVERSION_FAILED);
            } catch (final Exception e) {
                log.error("Failed to update Audit Result status for MO {}", moFdn, e);
            }
            failedAuditResults.addAll(auditResults);
        }
    }
}
