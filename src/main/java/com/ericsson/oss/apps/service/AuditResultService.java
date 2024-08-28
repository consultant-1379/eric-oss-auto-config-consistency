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

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

import com.ericsson.oss.apps.executor.ChangeStatus;
import com.ericsson.oss.apps.model.AuditResult;
import com.ericsson.oss.apps.repository.AuditResultsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditResultService {
    private final RetryTemplate retryTemplate;
    private final AuditResultsRepository auditResultsRepository;

    public void saveAll(final List<AuditResult> auditResults, final ChangeStatus changeStatus) {
        auditResults.forEach(auditResult -> auditResult.setChangeStatus(changeStatus));
        saveAll(auditResults);
    }

    public void saveAll(final List<AuditResult> auditResults) {
        retryTemplate.execute(context -> {
            log.debug("Attempt {} to persist Audit Results", context.getRetryCount() + 1);
            try {
                auditResultsRepository.saveAll(auditResults);
            } catch (final DataAccessException | TransactionException e) {
                log.error("Failed to persist audit results", e);
                throw e;
            } catch (final Exception e) {
                log.error("Failed to persist audit results with non-retryable reason", e);
                context.setExhaustedOnly();
                throw e;
            }
            return null;
        });
    }

    public void cleanUpAuditResultsOnStartUp() {
        auditResultsRepository.cleanUpChangeImplementationsOnStartUp(ChangeStatus.IMPLEMENTATION_ABORTED, ChangeStatus.IMPLEMENTATION_IN_PROGRESS);
        auditResultsRepository.cleanUpReversionsOnStartUp(ChangeStatus.REVERSION_ABORTED, ChangeStatus.REVERSION_IN_PROGRESS);
    }

}
