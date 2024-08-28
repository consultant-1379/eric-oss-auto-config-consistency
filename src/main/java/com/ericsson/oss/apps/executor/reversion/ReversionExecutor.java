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

import static com.ericsson.oss.apps.util.RanUtilities.getFdnDownToManagedElement;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.springframework.stereotype.Component;

import com.ericsson.oss.apps.executor.AuditConfig;
import com.ericsson.oss.apps.executor.ChangeStatus;
import com.ericsson.oss.apps.model.AuditResult;
import com.ericsson.oss.apps.model.EaccManagedObject;
import com.ericsson.oss.apps.service.AuditResultService;
import com.ericsson.oss.apps.service.ncmp.NcmpService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReversionExecutor {
    private final AuditConfig auditConfig;
    private final NcmpService ncmpService;
    private final AuditResultService auditResultService;

    public Map<ReversionStatus, List<AuditResult>> revert(final List<AuditResult> changesToRevert,
            final BiFunction<String, List<AuditResult>, Boolean> performRevert) {
        final Map<String, List<AuditResult>> auditResultsByManagedElementFdn = changesToRevert.stream().collect(groupingBy(
                auditResult -> getFdnDownToManagedElement(auditResult.getManagedObjectFdn())));

        final BlockingQueue<EaccManagedObject> eaccManagedObjectsQueue = new LinkedBlockingDeque<>(auditConfig.getMoQueueSize());

        final HashMap<String, Map<String, List<String>>> moTypeToAttrByFdn = new HashMap<>();

        auditResultsByManagedElementFdn.forEach((fdn, auditResults) -> {
            final Map<String, List<String>> attrByMo = auditResults.stream()
                    .collect(groupingBy(AuditResult::getManagedObjectType, mapping(AuditResult::getAttributeName, toList())));
            moTypeToAttrByFdn.put(fdn, attrByMo);
        });

        final Map<String, List<AuditResult>> changesByMoFdn = changesToRevert.stream().collect(groupingBy(AuditResult::getManagedObjectFdn));

        final ReversionExecutorConsumer consumer = new ReversionExecutorConsumer(auditResultService, eaccManagedObjectsQueue, changesByMoFdn,
                auditConfig.getRevertThreadPoolSize(), performRevert);

        ncmpService.populateEaccManagedObjects(moTypeToAttrByFdn, eaccManagedObjectsQueue);
        consumer.finish();
        consumer.waitForCompletion();

        final List<AuditResult> failedReversions = new ArrayList<>(consumer.getFailedAuditResults());
        // Handle the failure of changes where the read from NCMP failed for the remaining MO FDNs
        // ReversionExecutorConsumer is not aware of these changes as no EACC MO was added to the queue
        final List<AuditResult> remainingReversions = changesToRevert.stream()
                .filter(Predicate.not(failedReversions::contains))
                .filter(Predicate.not(consumer.getSuccessfulAuditResults()::contains))
                .peek(auditResult -> {
                    log.warn("Reversion failed for attribute '{}' because MO could not be retrieved for '{}'.",
                            auditResult.getAttributeName(), auditResult.getManagedObjectFdn());
                    auditResult.setChangeStatus(ChangeStatus.REVERSION_FAILED);
                })
                .toList();

        if (!remainingReversions.isEmpty()) {
            try {
                auditResultService.saveAll(failedReversions);
            } catch (final Exception e) {
                log.error("Failed to persist failed {} Audit Results", remainingReversions.size(), e);
            }
            failedReversions.addAll(remainingReversions);
        }

        return Map.of(ReversionStatus.SUCCESSFUL, consumer.getSuccessfulAuditResults(),
                ReversionStatus.FAILED, failedReversions);
    }
}
