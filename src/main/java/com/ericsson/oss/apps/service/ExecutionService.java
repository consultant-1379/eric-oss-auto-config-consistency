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

package com.ericsson.oss.apps.service;

import static com.ericsson.oss.apps.util.Constants.CHANGES_IN_PROGRESS;
import static com.ericsson.oss.apps.util.Constants.PREVIOUS_JOB_EXECUTION;
import static com.ericsson.oss.apps.util.Constants.PROPOSED_IDS_DONT_EXIST;
import static com.ericsson.oss.apps.util.Constants.REVERSIONS_IN_PROGRESS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

import com.ericsson.oss.apps.api.model.EaccApprovedAuditResults;
import com.ericsson.oss.apps.api.model.EaccAuditResult;
import com.ericsson.oss.apps.api.model.EaccExecution;
import com.ericsson.oss.apps.api.model.EaccPaginatedAuditResults;
import com.ericsson.oss.apps.executor.AuditStatus;
import com.ericsson.oss.apps.executor.ChangeStatus;
import com.ericsson.oss.apps.executor.ExecutionStatus;
import com.ericsson.oss.apps.model.AuditResult;
import com.ericsson.oss.apps.model.Execution;
import com.ericsson.oss.apps.repository.AuditResultsRepository;
import com.ericsson.oss.apps.repository.AuditResultsSpecification;
import com.ericsson.oss.apps.repository.ExecutionsRepository;
import com.ericsson.oss.apps.repository.FilterCriteria;
import com.ericsson.oss.apps.service.exception.ChangesInProgressException;
import com.ericsson.oss.apps.service.exception.InconsistencyProcessingFailedException;
import com.ericsson.oss.apps.service.exception.ProposedIdsNotFoundException;
import com.ericsson.oss.apps.service.exception.UnsupportedOperationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ExecutionService {

    @Autowired
    AuditResultsRepository auditResultsRepository;

    @Autowired
    ExecutionsRepository executionsRepository;


    @Autowired
    ChangeImplementationService changeImplementationService;

    @Autowired
    AuditResultService auditResultService;

    @Retryable(retryFor = {
            DataAccessException.class,
            TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}", 
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public List<EaccExecution> getAllExecutions() {
        final List<Execution> executionList = executionsRepository.findAll();
        return createEaccExecutionList(executionList);
    }

    @Retryable(retryFor = {
            DataAccessException.class,
            TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}", 
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public List<EaccExecution> getExecutionByJobName(final String jobName) {
        final Optional<List<Execution>> executionList = executionsRepository.findByJobName(jobName);
        return executionList.map(this::createEaccExecutionList).orElse(Collections.emptyList());
    }

    @Retryable(retryFor = {
            DataAccessException.class,
            TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}", 
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public EaccPaginatedAuditResults getAuditResults(final String executionId, final Integer page,
            final Integer pageSize, final List<String> filters) {

        final Pageable pageable = getPageable(page, pageSize);

        final Specification<AuditResult> auditResultsSpecification = buildAuditResultsSpecification(executionId, filters);

        final Optional<Page<AuditResult>> auditResults = Optional.of(auditResultsRepository.findAll(auditResultsSpecification, pageable));

        final EaccPaginatedAuditResults eaccPaginatedAuditResults = new EaccPaginatedAuditResults();

        eaccPaginatedAuditResults.setResults(auditResults.get().getContent().stream()
                .map(auditResult -> {
                    final EaccAuditResult eaccAuditResult = new EaccAuditResult();
                    eaccAuditResult.setId(String.valueOf(auditResult.getId()));
                    eaccAuditResult.setManagedObjectFdn(auditResult.getManagedObjectFdn());
                    eaccAuditResult.setManagedObjectType(auditResult.getManagedObjectType());
                    eaccAuditResult.setAttributeName(auditResult.getAttributeName());
                    eaccAuditResult.setCurrentValue(auditResult.getCurrentValue());
                    eaccAuditResult.setPreferredValue(auditResult.getPreferredValue());
                    eaccAuditResult.setAuditStatus(auditResult.getAuditStatus().toString());
                    eaccAuditResult.setExecutionId(auditResult.getExecutionId());
                    eaccAuditResult.setRuleId(auditResult.getRuleId());
                    if (auditResult.getChangeStatus() == null) {
                        eaccAuditResult.setChangeStatus("");
                    } else {
                        eaccAuditResult.setChangeStatus(auditResult.getChangeStatus().toString());
                    }
                    return eaccAuditResult;
                }).collect(Collectors.toList()));

        if (pageable.isPaged()) {
            eaccPaginatedAuditResults.setCurrentPage(auditResults.get().getPageable().getPageNumber());
            eaccPaginatedAuditResults.setPerPage(auditResults.get().getPageable().getPageSize());
        }
        eaccPaginatedAuditResults.setHasNext(auditResults.get().hasNext());
        eaccPaginatedAuditResults.setHasPrev(auditResults.get().hasPrevious());
        eaccPaginatedAuditResults.setTotalElements(auditResults.get().getTotalElements());
        eaccPaginatedAuditResults.setTotalPages(auditResults.get().getTotalPages());
        return eaccPaginatedAuditResults;
    }

    private Pageable getPageable(final Integer page, final Integer pageSize) {
        final Pageable pageable;
        if (page == null || pageSize == null) {
            pageable = Pageable.unpaged();
        } else {
            // Note: Need to sort as Postgres is given back ids in random order whereas h2 didn't. 
            //       Caused issue sometimes in audit/changes table in UI when applying 
            //       all changes on the page. 
            pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "id"));
        }
        return pageable;
    }

    private List<EaccExecution> createEaccExecutionList(final List<Execution> executionList) {
        final List<EaccExecution> eaccExecutionList = new ArrayList<>();
        for (final Execution execution : executionList) {
            final EaccExecution eaccExecution = new EaccExecution();
            eaccExecution.setId(execution.getId().toString());
            eaccExecution.setJobName(execution.getJobName());
            eaccExecution.setExecutionType(execution.getExecutionType().toString());
            eaccExecution.setConsistencyAuditStartedAt(execution.getConsistencyAuditStartedAt());
            eaccExecution.consistencyAuditEndedAt(execution.getConsistencyAuditEndedAt());
            eaccExecution.setExecutionStartedAt(execution.getExecutionStartedAt());
            eaccExecution.setExecutionEndedAt(execution.getExecutionEndedAt());
            eaccExecution.setExecutionStatus(execution.getExecutionStatus().toString());
            eaccExecution.setTotalNodesAudited(execution.getTotalNodesAudited());
            eaccExecution.setTotalNodesFailed(execution.getTotalNodesFailed());
            eaccExecution.setTotalMosAudited(execution.getTotalMosAudited());
            eaccExecution.setTotalAttributesAudited(execution.getTotalAttributesAudited());
            eaccExecution.setInconsistenciesIdentified(execution.getInconsistenciesIdentified());
            eaccExecutionList.add(eaccExecution);
        }
        return eaccExecutionList;
    }

    @Retryable(retryFor = {
            DataAccessException.class,
            TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}", 
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public void processApprovedChanges(final String executionId, final EaccApprovedAuditResults approvedAuditResults)
            throws InconsistencyProcessingFailedException, ProposedIdsNotFoundException, ChangesInProgressException {
        final List<AuditResult> changesToApply;
        if (approvedAuditResults.getApproveForAll()) {
            changesToApply = getChangesToApply(executionId);
        } else {
            changesToApply = getChangesToApply(executionId, approvedAuditResults);
        }

        if (changesToApply.isEmpty()) {
            log.error(PROPOSED_IDS_DONT_EXIST + ": {}", approvedAuditResults);
            throw new ProposedIdsNotFoundException(PROPOSED_IDS_DONT_EXIST);
        }
        final Map<String, List<AuditResult>> auditResultsMap = changesToApply.stream()
                .peek(auditResult -> auditResult.setChangeStatus(ChangeStatus.IMPLEMENTATION_IN_PROGRESS))
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(AuditResult::getManagedObjectFdn),
                            map -> {
                                auditResultsRepository.saveAll(map.values().stream()
                                        .flatMap(List::stream)
                                        .toList());
                                return map;
                            }));
        if (auditResultsMap.isEmpty()) {
            final String errorMessage = "Failed to process the inconsistencies";
            log.error(errorMessage);
            throw new InconsistencyProcessingFailedException(errorMessage);
        } else {
            final Long executionLongId = Long.parseLong(executionId);
            changeImplementationService.processApprovedChangesAsync(auditResultsMap, executionLongId);
        }
    }

    private List<AuditResult> getChangesToApply(final String executionId) throws ChangesInProgressException {
        final Optional<Execution> execution = executionsRepository.findById(Long.parseLong(executionId));
        if (execution.isPresent()) {
            final ExecutionStatus status = execution.get().getExecutionStatus();
            if (status.equals(ExecutionStatus.CHANGES_IN_PROGRESS)) {
                throw new ChangesInProgressException(CHANGES_IN_PROGRESS);
            } else if (status.equals(ExecutionStatus.REVERSION_IN_PROGRESS)) {
                throw new ChangesInProgressException(REVERSIONS_IN_PROGRESS);
            }
            final AuditResultsSpecification auditResultsExecutionIdSpecification = new AuditResultsSpecification(
                    FilterCriteria.build("executionId:" + executionId));
            final AuditResultsSpecification auditResultsAuditStatusSpecification = new AuditResultsSpecification(
                    FilterCriteria.build("auditStatus:" + AuditStatus.INCONSISTENT));
            final AuditResultsSpecification auditResultsChangeStatusNotCompleteSpecification =
                    new AuditResultsSpecification(FilterCriteria.build("changeStatus:(" +
                            ChangeStatus.NOT_APPLIED + "," + ChangeStatus.IMPLEMENTATION_ABORTED + "," +
                            ChangeStatus.IMPLEMENTATION_FAILED + ")"));
            final Specification<AuditResult> specification = Specification.where(auditResultsExecutionIdSpecification)
                    .and(auditResultsAuditStatusSpecification).and(auditResultsChangeStatusNotCompleteSpecification);
            return auditResultsRepository.findAll(specification);
        } else {
            return Collections.emptyList();
        }

    }

    private List<AuditResult> getChangesToApply(final String executionId, final EaccApprovedAuditResults approvedAuditResults)
            throws ChangesInProgressException {
        final List<Long> auditResultIds = approvedAuditResults.getAuditResultIds().stream()
                .map(Long::valueOf)
                .toList();
        if (auditResultsRepository.existsByExecutionIdAndChangeStatusAndIdIn(
                executionId, ChangeStatus.IMPLEMENTATION_IN_PROGRESS, auditResultIds)) {
            throw new ChangesInProgressException(CHANGES_IN_PROGRESS);
        }
        return auditResultsRepository.findByExecutionIdAndAuditStatusAndIdIn(
                executionId,
                AuditStatus.INCONSISTENT,
                auditResultIds);
    }

    @Retryable(retryFor = {
            DataAccessException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}", 
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public void revertChanges(final String executionId, final EaccApprovedAuditResults approvedAuditResults)
            throws ChangesInProgressException, UnsupportedOperationException, ProposedIdsNotFoundException {
        if (!isLatestExecution(executionId)) {
            throw new UnsupportedOperationException(PREVIOUS_JOB_EXECUTION);
        }
        final List<AuditResult> changesToRevert;
        if (approvedAuditResults.getApproveForAll()) {
            changesToRevert = getChangesToRevert(executionId);
        } else {
            final List<String> idsToRevert = approvedAuditResults.getAuditResultIds();
            changesToRevert = getChangesToRevert(executionId, idsToRevert);
        }
        if (changesToRevert.isEmpty()) {
            log.error(PROPOSED_IDS_DONT_EXIST + ": {}", approvedAuditResults);
            throw new ProposedIdsNotFoundException(PROPOSED_IDS_DONT_EXIST);
        }
        changesToRevert.forEach(auditResult -> auditResult.setChangeStatus(ChangeStatus.REVERSION_IN_PROGRESS));
        auditResultsRepository.saveAll(changesToRevert);
        final Long executionLongId = Long.parseLong(executionId);
        changeImplementationService.revertChangesAsync(changesToRevert, executionLongId);
    }

    public boolean isLatestExecution(final String executionIdStr) {
        final Long executionId = Long.parseLong(executionIdStr);

        return executionsRepository.findJobNameByExecutionId(executionId)
                .flatMap(executionsRepository::findFirstByJobNameOrderByExecutionStartedAtDesc)
                .map(execution -> executionId.equals(execution.getId()))
                .orElse(false);
    }

    private List<AuditResult> getChangesToRevert(final String executionId) throws ChangesInProgressException {
        if (auditResultsRepository.existsByExecutionIdAndChangeStatus(executionId, ChangeStatus.IMPLEMENTATION_IN_PROGRESS)) {
            throw new ChangesInProgressException(CHANGES_IN_PROGRESS);
        }

        auditResultsRepository.findByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusIn(
                executionId, AuditStatus.INCONSISTENT, List.of(ChangeStatus.REVERSION_COMPLETE))
                .forEach(auditResult -> log.warn("Excluding {} as it has already reverted {}", auditResult.getManagedObjectFdn(),
                        auditResult.getAttributeName()));

        return auditResultsRepository.findByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusNotIn(
                executionId,
                AuditStatus.INCONSISTENT,
                List.of(ChangeStatus.IMPLEMENTATION_IN_PROGRESS, ChangeStatus.REVERSION_IN_PROGRESS, ChangeStatus.REVERSION_COMPLETE,
                        ChangeStatus.IMPLEMENTATION_FAILED));
    }

    private List<AuditResult> getChangesToRevert(final String executionId, final List<String> idsToRevert) throws ChangesInProgressException {
        final var ids = idsToRevert.stream().map(Long::valueOf).toList();
        if (auditResultsRepository.existsByExecutionIdAndChangeStatusAndIdIn(executionId, ChangeStatus.IMPLEMENTATION_IN_PROGRESS, ids)) {
            throw new ChangesInProgressException(CHANGES_IN_PROGRESS);
        }

        auditResultsRepository.findByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusInAndIdIn(
                executionId, AuditStatus.INCONSISTENT, List.of(ChangeStatus.REVERSION_COMPLETE), ids)
                .forEach(auditResult -> log.warn("Excluding {} as it has already reverted {}", auditResult.getManagedObjectFdn(),
                        auditResult.getAttributeName()));

        return auditResultsRepository.findByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusNotInAndIdIn(
                executionId,
                AuditStatus.INCONSISTENT,
                List.of(ChangeStatus.IMPLEMENTATION_IN_PROGRESS, ChangeStatus.REVERSION_IN_PROGRESS, ChangeStatus.REVERSION_COMPLETE,
                        ChangeStatus.IMPLEMENTATION_FAILED),
                ids);
    }

    @Async
    @EventListener
    public void cleanUpExecutionsOnStartUp(final ApplicationReadyEvent event) {
        log.info("Cleaning up execution and audit result statuses on start up");
        executionsRepository.cleanUpExecutionStatusesAtStartUp(
                ExecutionStatus.AUDIT_IN_PROGRESS, ExecutionStatus.AUDIT_ABORTED);
        executionsRepository.cleanUpExecutionStatusesAtStartUp(
                ExecutionStatus.CHANGES_IN_PROGRESS, ExecutionStatus.CHANGES_ABORTED);
        executionsRepository.cleanUpExecutionStatusesAtStartUp(
                ExecutionStatus.REVERSION_IN_PROGRESS, ExecutionStatus.REVERSION_ABORTED);
        auditResultService.cleanUpAuditResultsOnStartUp();
        log.info("Clean up of execution and audit result statuses completed");
    }

    @Retryable(retryFor = {
            DataAccessException.class,
            TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}", 
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public boolean isRulesetInUse(final String ruleSetName) {
        return executionsRepository.existsByExecutionStatusAndRuleSetName(ExecutionStatus.AUDIT_IN_PROGRESS, ruleSetName);
    }

    @Retryable(retryFor = {
            DataAccessException.class,
            TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}", 
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public boolean isScopeInUse(final String scopeName) {
        return executionsRepository.existsByExecutionStatusAndScopeName(ExecutionStatus.AUDIT_IN_PROGRESS, scopeName);
    }

    @Retryable(retryFor = {
            DataAccessException.class,
            TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}", 
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public boolean isJobInUse(final String jobName) {
        return executionsRepository.existsByExecutionStatusAndJobName(ExecutionStatus.AUDIT_IN_PROGRESS, jobName);
    }

    @Retryable(retryFor = {
            DataAccessException.class,
            TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}", 
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public boolean existsByExecutionId(final String executionId) {
        return executionsRepository.existsById(Long.parseLong(executionId));
    }

    private Specification<AuditResult> buildAuditResultsSpecification(final String executionId, final List<String> filtersToAdd) {
        final AuditResultsSpecification auditResultsExecutionIdSpecification = new AuditResultsSpecification(
                FilterCriteria.build("executionId:" + executionId));
        final AuditResultsSpecification auditResultsFilterSpecification = null;
        Specification<AuditResult> specification = Specification.where(auditResultsExecutionIdSpecification)
                .and(auditResultsFilterSpecification);

        for (final String filter : filtersToAdd) {
            final AuditResultsSpecification additionalFilter = new AuditResultsSpecification(FilterCriteria.build(filter));
            specification = specification.and(additionalFilter);
        }

        return specification;
    }
}
