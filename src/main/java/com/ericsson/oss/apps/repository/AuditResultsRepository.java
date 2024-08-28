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

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.oss.apps.executor.AuditStatus;
import com.ericsson.oss.apps.executor.ChangeStatus;
import com.ericsson.oss.apps.model.AuditResult;
import com.ericsson.oss.apps.model.AuditResultId;

@Repository
public interface AuditResultsRepository extends JpaRepository<AuditResult, AuditResultId>, JpaSpecificationExecutor<AuditResult> {

    Optional<List<AuditResult>> findByExecutionId(String executionId);

    List<AuditResult> findByExecutionIdAndAuditStatusAndIdIn(String executionId, AuditStatus auditStatus, List<Long> auditResultIds);

    @Transactional
    void deleteByExecutionIdAndId(String executionId, Long id);

    @Transactional
    void deleteByExecutionId(String executionId);

    boolean existsByExecutionIdAndChangeStatus(String executionId, ChangeStatus status);

    boolean existsByExecutionIdAndChangeStatusAndIdIn(String executionId, ChangeStatus status, List<Long> ids);

    List<AuditResult> findByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusNotIn(
            String executionId, AuditStatus status, List<ChangeStatus> changeStatuses);

    List<AuditResult> findByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusIn(
            String executionId, AuditStatus status, List<ChangeStatus> changeStatuses);

    List<AuditResult> findByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusNotInAndIdIn(
            String executionId, AuditStatus status, List<ChangeStatus> changeStatuses, List<Long> ids);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE AuditResult SET changeStatus = :newChangeStatus WHERE changeStatus = :oldChangeStatus")
    void cleanUpChangeImplementationsOnStartUp(@Param("newChangeStatus") ChangeStatus newChangeStatus,
            @Param("oldChangeStatus") ChangeStatus oldChangeStatus);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE AuditResult SET changeStatus = :newChangeStatus WHERE changeStatus = :oldChangeStatus")
    void cleanUpReversionsOnStartUp(@Param("newChangeStatus") ChangeStatus newChangeStatus,
            @Param("oldChangeStatus") ChangeStatus oldChangeStatus);

    List<AuditResult> findByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusInAndIdIn(String executionId, AuditStatus inconsistent,
            List<ChangeStatus> reversionComplete, List<Long> ids);
}
