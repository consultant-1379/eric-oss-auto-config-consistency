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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.oss.apps.executor.ExecutionStatus;
import com.ericsson.oss.apps.model.Execution;

@Repository
public interface ExecutionsRepository extends JpaRepository<Execution, Long> {

    Optional<List<Execution>> findByJobName(String jobName);

    boolean existsByExecutionStatusAndRuleSetName(@Param("status") ExecutionStatus status,
                                                  @Param("ruleSetName") String ruleSetName);

    boolean existsByExecutionStatusAndScopeName(@Param("status") ExecutionStatus status,
                                                @Param("scopeName") String scopeName);

    boolean existsByExecutionStatusAndJobName(@Param("status") ExecutionStatus status,
                                              @Param("jobName") String jobName);

    List<Execution> findByExecutionStartedAtBefore(OffsetDateTime executionStartedAt);

    @Query("SELECT e.jobName FROM Execution e WHERE e.id = :id")
    Optional<String> findJobNameByExecutionId(@Param("id") Long executionId);

    Optional<Execution> findFirstByJobNameOrderByExecutionStartedAtDesc(String jobName);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Execution SET executionStatus = :newExecutionStatus WHERE executionStatus = :oldExecutionStatus")
    void cleanUpExecutionStatusesAtStartUp(@Param("oldExecutionStatus") ExecutionStatus oldExecutionStatus,
                                           @Param("newExecutionStatus") ExecutionStatus newExecutionStatus);
}
