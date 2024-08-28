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

package com.ericsson.oss.apps.repository;

import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_JOB_NAME;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_RULESET_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.apps.executor.ExecutionStatus;
import com.ericsson.oss.apps.model.Execution;

import lombok.val;

/**
 * Unit tests for {@link ExecutionsRepository} interface.
 */
@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(MockitoExtension.class)
public class ExecutionRepositoryTest {

    @Autowired
    ExecutionsRepository repositoryUnderTest;

    @Test
    void existsByExecutionStatusAndRuleSetName_PositiveTest() {

        val status = ExecutionStatus.AUDIT_IN_PROGRESS;

        final Execution execution = new Execution();

        execution.setExecutionStatus(status);
        execution.setRuleSetName(DEFAULT_RULESET_NAME);

        repositoryUnderTest.saveAndFlush(execution);

        val expectedStatus = repositoryUnderTest.existsByExecutionStatusAndRuleSetName(ExecutionStatus.AUDIT_IN_PROGRESS, DEFAULT_RULESET_NAME);

        assertThat(expectedStatus).isTrue();

    }

    @Test
    void existsStatusByExecutionStatusAndRuleSetName_NegativeTest() {

        val status = ExecutionStatus.AUDIT_SUCCESSFUL;

        final Execution execution = new Execution();

        execution.setExecutionStatus(status);
        execution.setRuleSetName(DEFAULT_RULESET_NAME);

        repositoryUnderTest.saveAndFlush(execution);

        val expectedStatus = repositoryUnderTest.existsByExecutionStatusAndRuleSetName(ExecutionStatus.AUDIT_IN_PROGRESS, DEFAULT_RULESET_NAME);

        assertThat(expectedStatus).isFalse();

    }

    @Test
    void whenFindJobNameByExecutionId_thenCorrectJobNameIsReturned() {

        final Execution execution = new Execution();

        execution.setJobName(DEFAULT_JOB_NAME);
        execution.setId(1L);

        Execution persisted = repositoryUnderTest.saveAndFlush(execution);

        assertThat(repositoryUnderTest.findJobNameByExecutionId(persisted.getId()))
                .isEqualTo(Optional.of(DEFAULT_JOB_NAME));
    }

    @Test
    void whenFindFirstByJobNameOrderByExecutionStartedAtDescIsCalled_thenCorrectExecutionIdIsReturned() {
        final Execution execution = new Execution();

        execution.setJobName(DEFAULT_JOB_NAME);
        execution.setId(1L);

        repositoryUnderTest.saveAndFlush(execution);

        assertThat(repositoryUnderTest.findFirstByJobNameOrderByExecutionStartedAtDesc(DEFAULT_JOB_NAME))
                .isEqualTo(Optional.of(execution));
    }
}
