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

package com.ericsson.oss.apps.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.apps.executor.AuditStatus;
import com.ericsson.oss.apps.executor.ChangeStatus;
import com.ericsson.oss.apps.model.AuditResult;

/**
 * Unit tests for {@link AuditResultsRepository}.
 */
@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(MockitoExtension.class)
public class AuditResultsRepositoryTest {
    private static final String EXECUTION_ID = "1";
    @Autowired
    AuditResultsRepository auditResultsRepository;

    @Test
    void whenExistsByExecutionIdAndChangeStatus_andAuditResultsExist_thenTrueIsReturned() {
        final AuditResult auditResult = new AuditResult();
        auditResult.setExecutionId(EXECUTION_ID);
        auditResult.setChangeStatus(ChangeStatus.IMPLEMENTATION_COMPLETE);
        auditResult.setManagedObjectFdn("auditResultFdn");
        auditResultsRepository.saveAndFlush(auditResult);
        assertThat(auditResultsRepository.existsByExecutionIdAndChangeStatus(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE))
                .isTrue();
    }

    @Test
    void whenExistsByExecutionIdAndChangeStatus_andAuditResultsDoNotExist_thenFalseIsReturned() {
        final AuditResult auditResult = new AuditResult();
        auditResult.setExecutionId(EXECUTION_ID);
        auditResult.setChangeStatus(ChangeStatus.IMPLEMENTATION_IN_PROGRESS);
        auditResult.setManagedObjectFdn("auditResultFdn");
        auditResultsRepository.saveAndFlush(auditResult);
        assertThat(auditResultsRepository.existsByExecutionIdAndChangeStatus(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE))
                .isFalse();
    }

    @Test
    void whenExistsByExecutionIdAndChangeStatusAndIdIn_andAuditResultsExist_thenTrueIsReturned() {
        final AuditResult auditResult1 = new AuditResult();
        auditResult1.setExecutionId(EXECUTION_ID);
        auditResult1.setChangeStatus(ChangeStatus.IMPLEMENTATION_COMPLETE);
        auditResult1.setManagedObjectFdn("auditResultFdn");
        auditResult1.setId(1L);
        final AuditResult auditResult2 = new AuditResult();
        auditResult2.setExecutionId(EXECUTION_ID);
        auditResult2.setChangeStatus(ChangeStatus.IMPLEMENTATION_COMPLETE);
        auditResult2.setManagedObjectFdn("auditResultFdn");
        auditResult2.setId(1L);
        auditResultsRepository.saveAllAndFlush(List.of(auditResult1, auditResult2));
        assertThat(auditResultsRepository.existsByExecutionIdAndChangeStatusAndIdIn(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE,
                List.of(auditResult1.getId(), auditResult2.getId())))
                        .isTrue();
    }

    @Test
    void whenExistsByExecutionIdAndChangeStatusAndIdIn_andAuditResultsDoNotExist_thenFalseIsReturned() {
        final AuditResult auditResult1 = new AuditResult();
        auditResult1.setExecutionId(EXECUTION_ID);
        auditResult1.setChangeStatus(ChangeStatus.IMPLEMENTATION_COMPLETE);
        auditResult1.setManagedObjectFdn("auditResultFdn");
        auditResult1.setId(1L);
        final AuditResult auditResult2 = new AuditResult();
        auditResult2.setExecutionId(EXECUTION_ID);
        auditResult2.setChangeStatus(ChangeStatus.IMPLEMENTATION_COMPLETE);
        auditResult2.setManagedObjectFdn("auditResultFdn");
        auditResult2.setId(2L);
        auditResultsRepository.saveAllAndFlush(List.of(auditResult1, auditResult2));
        assertThat(
                auditResultsRepository.existsByExecutionIdAndChangeStatusAndIdIn(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE, List.of(3L, 4L)))
                        .isFalse();
    }

    @Test
    void whenFindByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusNotInIsCalled_thenCorrectAuditResultsAreReturned() {
        final AuditResult auditResult1 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE, 11L, AuditStatus.INCONSISTENT);
        final AuditResult auditResult2 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE, 12L, AuditStatus.INCONSISTENT);
        final AuditResult auditResult3 = createAuditResult(EXECUTION_ID, null, 13L, AuditStatus.INCONSISTENT);
        final AuditResult auditResult4 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_IN_PROGRESS, 14L, AuditStatus.INCONSISTENT);
        final AuditResult auditResult5 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE, 15L, AuditStatus.CONSISTENT);
        final AuditResult auditResult6 = createAuditResult("2", ChangeStatus.IMPLEMENTATION_COMPLETE, 16L, AuditStatus.INCONSISTENT);
        final AuditResult auditResult7 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_FAILED, 17L, AuditStatus.INCONSISTENT);
        auditResultsRepository
                .saveAllAndFlush(List.of(auditResult1, auditResult2, auditResult3, auditResult4, auditResult5, auditResult6, auditResult7));
        assertThat(auditResultsRepository.findByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusNotIn(EXECUTION_ID,
                AuditStatus.INCONSISTENT,
                List.of(ChangeStatus.IMPLEMENTATION_IN_PROGRESS, ChangeStatus.REVERSION_IN_PROGRESS, ChangeStatus.IMPLEMENTATION_FAILED)))
                        .hasSize(2)
                        .containsExactly(auditResult1, auditResult2);
    }

    @Test
    void whenFindByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusNotInAndIdInIsCalled_thenCorrectAuditResultsAreReturned() {
        final AuditResult auditResult1 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE, 1L, AuditStatus.INCONSISTENT);
        final AuditResult auditResult2 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE, 2L, AuditStatus.INCONSISTENT);
        final AuditResult auditResult3 = createAuditResult(EXECUTION_ID, null, 3L, AuditStatus.INCONSISTENT);
        final AuditResult auditResult4 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_IN_PROGRESS, 4L, AuditStatus.INCONSISTENT);
        final AuditResult auditResult5 = createAuditResult("2", ChangeStatus.IMPLEMENTATION_COMPLETE, 5L, AuditStatus.INCONSISTENT);
        final AuditResult auditResult6 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE, 6L, AuditStatus.CONSISTENT);
        final AuditResult auditResult7 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE, 7L, AuditStatus.INCONSISTENT);
        final AuditResult auditResult8 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_FAILED, 8L, AuditStatus.INCONSISTENT);
        auditResultsRepository.saveAllAndFlush(
                List.of(auditResult1, auditResult2, auditResult3, auditResult4, auditResult5, auditResult6, auditResult7, auditResult8));
        assertThat(auditResultsRepository.findByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusNotInAndIdIn(EXECUTION_ID,
                AuditStatus.INCONSISTENT,
                List.of(ChangeStatus.IMPLEMENTATION_IN_PROGRESS, ChangeStatus.REVERSION_IN_PROGRESS, ChangeStatus.IMPLEMENTATION_FAILED),
                List.of(1L, 2L, 3L, 4L, 5L, 6L, 8L)))
                        .hasSize(2)
                        .containsExactlyInAnyOrder(auditResult1, auditResult2);
    }

    @Test
    void whenFindByExecutionIdAndAuditStatusAndIdInIsCalled_thenCorrectAuditResultsAreReturned() {
        final AuditResult auditResult1 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE, 1L, AuditStatus.INCONSISTENT);
        final AuditResult auditResult2 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE, 2L, AuditStatus.INCONSISTENT);
        final AuditResult auditResult3 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE, 3L, AuditStatus.CONSISTENT);
        final AuditResult auditResult4 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE, 4L, AuditStatus.CONSISTENT);
        auditResultsRepository.saveAllAndFlush(List.of(auditResult1, auditResult2, auditResult3, auditResult4));
        assertThat(auditResultsRepository.findByExecutionIdAndAuditStatusAndIdIn(EXECUTION_ID, AuditStatus.INCONSISTENT, List.of(1L)))
                .hasSize(1)
                .containsExactly(auditResult1);
    }

    @Test
    void whenFindByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusInIsCalled_thenCorrectAuditResultsAreReturned() {
        final AuditResult auditResult1 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE, AuditStatus.INCONSISTENT);
        final AuditResult auditResult2 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE, AuditStatus.INCONSISTENT);
        final AuditResult auditResult3 = createAuditResult(EXECUTION_ID, null, AuditStatus.INCONSISTENT);
        final AuditResult auditResult4 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_IN_PROGRESS, AuditStatus.CONSISTENT);
        final AuditResult auditResult5 = createAuditResult(EXECUTION_ID, ChangeStatus.REVERSION_COMPLETE, AuditStatus.INCONSISTENT);
        final AuditResult auditResult6 = createAuditResult("2", ChangeStatus.REVERSION_IN_PROGRESS, AuditStatus.INCONSISTENT);
        auditResultsRepository.saveAllAndFlush(List.of(auditResult1, auditResult2, auditResult3, auditResult4, auditResult5, auditResult6));
        assertThat(auditResultsRepository.findByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusIn(EXECUTION_ID,
                AuditStatus.INCONSISTENT, List.of(ChangeStatus.REVERSION_COMPLETE)))
                        .hasSize(1)
                        .containsExactly(auditResult5);
    }

    @Test
    void whenFindByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusInAndIdInIsCalled_thenCorrectAuditResultsAreReturned() {
        final AuditResult auditResult1 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE, 1L, AuditStatus.INCONSISTENT);
        final AuditResult auditResult2 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE, 2L, AuditStatus.INCONSISTENT);
        final AuditResult auditResult3 = createAuditResult(EXECUTION_ID, null, 3L, AuditStatus.INCONSISTENT);
        final AuditResult auditResult4 = createAuditResult(EXECUTION_ID, ChangeStatus.IMPLEMENTATION_IN_PROGRESS, 4L, AuditStatus.CONSISTENT);
        final AuditResult auditResult5 = createAuditResult(EXECUTION_ID, ChangeStatus.REVERSION_COMPLETE, 5L, AuditStatus.INCONSISTENT);
        final AuditResult auditResult6 = createAuditResult("2", ChangeStatus.REVERSION_IN_PROGRESS, 6L, AuditStatus.INCONSISTENT);
        final AuditResult auditResult7 = createAuditResult(EXECUTION_ID, ChangeStatus.REVERSION_COMPLETE, 7L, AuditStatus.INCONSISTENT);
        auditResultsRepository
                .saveAllAndFlush(List.of(auditResult1, auditResult2, auditResult3, auditResult4, auditResult5, auditResult6, auditResult7));
        assertThat(auditResultsRepository.findByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusInAndIdIn(EXECUTION_ID,
                AuditStatus.INCONSISTENT, List.of(ChangeStatus.REVERSION_COMPLETE), List.of(1L, 2L, 3L, 4L, 5L, 6L)))
                        .hasSize(1)
                        .containsExactly(auditResult5);
    }

    private AuditResult createAuditResult(final String executionId, final ChangeStatus changeStatus, final AuditStatus auditStatus) {
        final AuditResult auditResult = new AuditResult();
        auditResult.setExecutionId(executionId);
        auditResult.setChangeStatus(changeStatus);
        auditResult.setAuditStatus(auditStatus);
        return auditResult;
    }

    private AuditResult createAuditResult(final String executionId, final ChangeStatus changeStatus, final Long id, final AuditStatus auditStatus) {
        final AuditResult auditResult = new AuditResult();
        auditResult.setExecutionId(executionId);
        auditResult.setChangeStatus(changeStatus);
        auditResult.setId(id);
        auditResult.setAuditStatus(auditStatus);
        return auditResult;
    }
}
