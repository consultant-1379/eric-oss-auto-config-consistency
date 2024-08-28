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
import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.oss.apps.executor.AuditStatus;
import com.ericsson.oss.apps.executor.ChangeStatus;
import com.ericsson.oss.apps.executor.ExecutionStatus;
import com.ericsson.oss.apps.executor.ExecutionType;
import com.ericsson.oss.apps.model.AuditResult;
import com.ericsson.oss.apps.model.Execution;
import com.ericsson.oss.apps.repository.AuditResultsRepository;
import com.ericsson.oss.apps.repository.ExecutionsRepository;

@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@Transactional
public class RetentionServiceTest {

    private final OffsetDateTime currentTime = getCurrentUtcDateTime();

    private final ExecutionsRepository executionRepository;

    private final AuditResultsRepository auditResultsRepository;

    private final RetentionService objectUnderTest;

    @Autowired
    public RetentionServiceTest(final ExecutionsRepository executionRepository,
            final AuditResultsRepository auditResultsRepository, final RetentionService retentionService) {
        this.executionRepository = executionRepository;
        this.auditResultsRepository = auditResultsRepository;
        objectUnderTest = retentionService;
    }

    @BeforeEach
    public void clearDatabase() {
        executionRepository.deleteAll();
        auditResultsRepository.deleteAll();
    }

    @Test
    public void whenAnExpiredExecutionExists_thenItsExecutionAndAuditResultsAreRemoved() {
        addAllExecutions();
        objectUnderTest.removeExpiredExecutions();
        assertThat(executionRepository.count()).isEqualTo(2);
        assertThat(auditResultsRepository.count()).isEqualTo(3);
    }

    @Test
    public void whenTwoExecutionsExistAtThe90DayBoundary_thenUnexpiredExecutionRemains() {
        executionRepository.save(createExecution(1L, "expiredExecution_1", currentTime.minusDays(90).minusSeconds(1)));
        executionRepository.save(createExecution(2L, "unexpiredExecution_1", currentTime.minusDays(90).plusMinutes(5)));
        objectUnderTest.removeExpiredExecutions();
        assertThat(executionRepository.count()).isEqualTo(1);
        assertThat(auditResultsRepository.count()).isEqualTo(0);
    }

    @Test
    public void whenRetentionServiceIsInvoked_thenThresholdNotReached_AndExecutionsAreNotDeleted() {
        executionRepository.save(createExecution(34L, "expiredExecution_1", currentTime.minusDays(90)));
        executionRepository.save(createExecution(35L, "unexpiredExecution_1", currentTime.minusDays(65)));

        ReflectionTestUtils.setField(objectUnderTest, "maxExecutionsToBeRetained", 3L);

        objectUnderTest.clearExecutionsByCount();

        assertThat(executionRepository.count()).isEqualTo(2);
        assertThat(auditResultsRepository.count()).isEqualTo(0);
    }

    @Test
    public void whenRetentionServiceIsInvoked_thenNumberOfExecutionsIsCutDownToExpectedRetainedCount() {
        addAllExecutions();

        ReflectionTestUtils.setField(objectUnderTest, "maxExecutionsToBeRetained", 3L);
        objectUnderTest.clearExecutionsByCount();
        assertThat(executionRepository.count()).isEqualTo(3);
        assertThat(auditResultsRepository.count()).isEqualTo(7);

    }

    private void addAllExecutions() {
        executionRepository.save(createExecution(1001L, "expiredExecution_1", currentTime.minusDays(90).minusSeconds(1)));
        executionRepository.save(createExecution(1002L, "unexpiredExecution_1", currentTime.minusDays(65)));
        executionRepository.save(createExecution(1003L, "expiredExecution_2", currentTime.minusDays(91)));
        executionRepository.save(createExecution(1004L, "unexpiredExecution_2", currentTime.minusDays(49)));
        executionRepository.save(createExecution(1005L, "expiredExecution_3", currentTime.minusDays(100)));

        final String[] generatedExecutionIdList = executionRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream().map(execution -> execution.getId().toString()).toArray(String[]::new);

        auditResultsRepository.save(createAuditResults(1L, generatedExecutionIdList[0]));
        auditResultsRepository.save(createAuditResults(2L, generatedExecutionIdList[0]));
        auditResultsRepository.save(createAuditResults(3L, generatedExecutionIdList[0]));
        auditResultsRepository.save(createAuditResults(4L, generatedExecutionIdList[0]));
        auditResultsRepository.save(createAuditResults(5L, generatedExecutionIdList[1]));
        auditResultsRepository.save(createAuditResults(6L, generatedExecutionIdList[2]));
        auditResultsRepository.save(createAuditResults(7L, generatedExecutionIdList[2]));
        auditResultsRepository.save(createAuditResults(8L, generatedExecutionIdList[3]));
        auditResultsRepository.save(createAuditResults(9L, generatedExecutionIdList[3]));
        auditResultsRepository.save(createAuditResults(10L, generatedExecutionIdList[4]));
    }

    public Execution createExecution(final Long id, final String jobName, final OffsetDateTime executionStartedAt) {
        return new Execution(id, jobName,
                "", "", ExecutionType.OPEN_LOOP,
                executionStartedAt,
                OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now(), ExecutionStatus.AUDIT_FAILED, 0, 0, 0, 0, 0);

    }

    public AuditResult createAuditResults(final Long id, final String executionId) {
        return new AuditResult(id, executionId,
                "", "", "", "", "", AuditStatus.CONSISTENT, "", ChangeStatus.IMPLEMENTATION_COMPLETE);
    }
}
