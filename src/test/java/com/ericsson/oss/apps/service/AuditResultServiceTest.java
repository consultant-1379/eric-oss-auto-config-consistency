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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.CannotCreateTransactionException;

import com.ericsson.oss.apps.config.RetryTemplateBean;
import com.ericsson.oss.apps.executor.ChangeStatus;
import com.ericsson.oss.apps.model.AuditResult;
import com.ericsson.oss.apps.repository.AuditResultsRepository;

@SpringBootTest(classes = { AuditResultService.class, RetryTemplateBean.class }, properties = {
        "database.retry.scheduleInitiated.maxAttempts=3",
        "database.retry.scheduleInitiated.delay=1",
        "database.retry.scheduleInitiated.maxDelay=5",
        "database.retry.scheduleInitiated.delayMultiplier=2"
})
public class AuditResultServiceTest {
    @MockBean
    private AuditResultsRepository auditResultsRepository;
    @Autowired
    private AuditResultService objectUnderTest;

    @Test
    void whenSaveAll_verifyRepositorySaveAllIsCalled() {
        when(auditResultsRepository.saveAll(anyList()))
                .thenReturn(List.of());

        final var auditResults = getMockedAuditResults();

        objectUnderTest.saveAll(auditResults);

        verify(auditResultsRepository, times(1)).saveAll(auditResults);
    }

    @Test
    void whenSaveAll_andExceptionThrownTwoTimes_verifyRepositorySaveAllAttemptedThreeTimes() {
        when(auditResultsRepository.saveAll(anyList()))
                .thenThrow(DataAccessResourceFailureException.class)
                .thenThrow(CannotCreateTransactionException.class)
                .thenReturn(List.of());

        final var auditResults = getMockedAuditResults();

        objectUnderTest.saveAll(auditResults);

        verify(auditResultsRepository, times(3)).saveAll(auditResults);
    }

    @Test
    void whenSaveAll_andDataAccessExceptionThrown_verifyExceptionThrown() {
        when(auditResultsRepository.saveAll(anyList()))
                .thenThrow(DataAccessResourceFailureException.class)
                .thenThrow(DataAccessResourceFailureException.class)
                .thenThrow(DataAccessResourceFailureException.class);

        final var auditResults = getMockedAuditResults();

        assertThatThrownBy(() -> objectUnderTest.saveAll(auditResults))
                .isInstanceOf(DataAccessException.class);

        verify(auditResultsRepository, times(3)).saveAll(auditResults);
    }

    @Test
    void whenSaveAll_andRuntimeExceptionThrown_verifySaveAllNotRetried() {
        when(auditResultsRepository.saveAll(anyList()))
                .thenThrow(RuntimeException.class);

        final var auditResults = getMockedAuditResults();

        assertThatThrownBy(() -> objectUnderTest.saveAll(auditResults))
                .isInstanceOf(RuntimeException.class);

        verify(auditResultsRepository, times(1)).saveAll(auditResults);
    }

    @Test
    void whenSaveAll_withStatus_verifyChangeStatusUpdatedAndRepositorySaveAllIsCalled() {
        final var auditResults = getMockedAuditResults();
        
        objectUnderTest.saveAll(auditResults, ChangeStatus.REVERSION_COMPLETE);

        verify(auditResultsRepository, times(1)).saveAll(auditResults);

        auditResults.forEach(
                auditResult -> verify(auditResult, times(1)).setChangeStatus(ChangeStatus.REVERSION_COMPLETE)
        );
    }

    private List<AuditResult> getMockedAuditResults() {
        return List.of(
                mock(AuditResult.class),
                mock(AuditResult.class),
                mock(AuditResult.class),
                mock(AuditResult.class));
    }
}
