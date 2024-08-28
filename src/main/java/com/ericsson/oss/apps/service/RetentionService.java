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

import static com.ericsson.oss.apps.util.DateUtil.getCurrentUtcDateTime;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.oss.apps.executor.ExecutionStatus;
import com.ericsson.oss.apps.model.Execution;
import com.ericsson.oss.apps.repository.AuditResultsRepository;
import com.ericsson.oss.apps.repository.ExecutionsRepository;

import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class RetentionService {

    @Autowired
    ExecutionsRepository executionsRepository;

    @Autowired
    AuditResultsRepository auditResultsRepository;

    @Value("${database.retention.executionExpirationPeriodInDays}")
    int expirationPeriodInDays;

    @Value("${database.retention.maxExecutionsToBeRetained}")
    long maxExecutionsToBeRetained;

    @Scheduled(cron = "${database.retention.executionCronExpression}")
    public void removeExpiredExecutions() {
        log.info("Starting removal of Executions older than {} days.", expirationPeriodInDays);
        clearExecutionsByTimestamp();
    }

    private void clearExecutionsByTimestamp() {
        final OffsetDateTime expiryDateTime = getCurrentUtcDateTime().minusDays(expirationPeriodInDays);
        final List<Execution> executionsExpired = executionsRepository.findByExecutionStartedAtBefore(expiryDateTime);
        log.info("Starting removal of {} expired Execution(s).", executionsExpired.size());
        for (final Execution execution : executionsExpired) {
            auditResultsRepository.deleteByExecutionId(execution.getId().toString());
            executionsRepository.delete(execution);
        }
        log.info("Successfully removed all expired executions");
    }

    @Synchronized
    public void clearExecutionsByCount() {
        final int numberOfExecutionsToBeDeleted = (int) (executionsRepository.count() - maxExecutionsToBeRetained);
        if (numberOfExecutionsToBeDeleted > 0) {
            log.info("Number of executions retained has reached the threshold. Starting removal of {} oldest execution(s)",
                    numberOfExecutionsToBeDeleted);
            final Page<Execution> page = executionsRepository.findAll(
                    PageRequest.of(0, numberOfExecutionsToBeDeleted, Sort.by(Sort.Direction.ASC, "executionStartedAt")));
            page.forEach(execution -> {
                if (ExecutionStatus.AUDIT_IN_PROGRESS != execution.getExecutionStatus()) {
                    auditResultsRepository.deleteByExecutionId(execution.getId().toString());
                    executionsRepository.delete(execution);
                }
            });
            log.info("Successfully removed {} oldest executions", numberOfExecutionsToBeDeleted);
        }
    }
}
