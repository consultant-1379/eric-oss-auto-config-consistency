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

import static com.ericsson.oss.apps.executor.ChangeStatus.IMPLEMENTATION_FAILED;
import static com.ericsson.oss.apps.executor.ChangeStatus.IMPLEMENTATION_IN_PROGRESS;
import static com.ericsson.oss.apps.executor.ChangeStatus.REVERSION_COMPLETE;
import static com.ericsson.oss.apps.executor.ExecutionStatus.AUDIT_SUCCESSFUL;
import static com.ericsson.oss.apps.executor.ExecutionType.OPEN_LOOP;
import static com.ericsson.oss.apps.util.Constants.CHANGES_IN_PROGRESS;
import static com.ericsson.oss.apps.util.Constants.PREVIOUS_JOB_EXECUTION;
import static com.ericsson.oss.apps.util.Constants.PROPOSED_IDS_DONT_EXIST;
import static com.ericsson.oss.apps.util.Constants.REVERSIONS_IN_PROGRESS;
import static com.ericsson.oss.apps.util.DateUtil.getCurrentUtcDateTime;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_JOB_NAME;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_RULESET_NAME;
import static com.ericsson.oss.apps.util.TestDefaults.INJECTED_LOG_TO_VALIDATE_TEST;
import static com.ericsson.oss.apps.util.TestDefaults.TEST_SETUP_ERROR_UNABLE_TO_ACCESS_LOGS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionException;

import com.ericsson.oss.apps.CoreApplication;
import com.ericsson.oss.apps.CoreApplicationTest;
import com.ericsson.oss.apps.api.model.EaccApprovedAuditResults;
import com.ericsson.oss.apps.api.model.EaccAuditResult;
import com.ericsson.oss.apps.api.model.EaccExecution;
import com.ericsson.oss.apps.api.model.EaccOperation;
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
import com.ericsson.oss.apps.repository.QueryOperator;
import com.ericsson.oss.apps.service.exception.ChangesInProgressException;
import com.ericsson.oss.apps.service.exception.ProposedIdsNotFoundException;
import com.ericsson.oss.apps.service.exception.UnsupportedOperationException;
import com.ericsson.oss.apps.service.ncmp.NcmpService;
import com.ericsson.oss.apps.util.InMemoryLogAppender;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Unit tests for {@link ExecutionService} class.
 */
@ActiveProfiles("test")
@AutoConfigureObservability
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = { CoreApplication.class, CoreApplicationTest.class })
public class ExecutionServiceTest {
    private final OffsetDateTime dateNow = getCurrentUtcDateTime();
    @Autowired
    private ExecutionService objectUnderTest;
    @MockBean
    private ExecutionsRepository executionsRepository;
    @MockBean
    private AuditResultsRepository auditResultsRepository;
    @MockBean
    private AuditResultService auditResultService;
    @MockBean
    private NcmpService ncmpService;
    @MockBean
    private ChangeImplementationService changeImplementationService;
    @MockBean
    private MetricService metricService;

    private InMemoryLogAppender logAppender;

    @Value("${database.retry.userInitiated.maxAttempts}")
    private int maxAttempts;

    @BeforeEach
    public void initLogs() {
        final Logger logUnderTest = (Logger) LoggerFactory.getLogger(ExecutionService.class);
        logUnderTest.setLevel(Level.WARN);
        final Logger changeISLogUnderTest = (Logger) LoggerFactory.getLogger(ChangeImplementationService.class);
        changeISLogUnderTest.setLevel(Level.ERROR);
        logAppender = new InMemoryLogAppender();
        logAppender.start();
        logUnderTest.addAppender(logAppender);
        changeISLogUnderTest.addAppender(logAppender);
        logUnderTest.error(INJECTED_LOG_TO_VALIDATE_TEST);
        changeISLogUnderTest.error(INJECTED_LOG_TO_VALIDATE_TEST);
        AuditResult.clearIdMap();
    }

    @AfterEach
    public void checkLogsAreAccessible() {
        assertThat(logAppender.getLoggedEvents()).asString()
                .as(TEST_SETUP_ERROR_UNABLE_TO_ACCESS_LOGS)
                .containsSubsequence(INJECTED_LOG_TO_VALIDATE_TEST, INJECTED_LOG_TO_VALIDATE_TEST);
    }

    @Test
    public void whenGetAllExecutionsIsRequested_thenAllExecutionsAreReturned() {
        when(executionsRepository.findAll()).thenReturn(createDefaultDomainExecutionModel());
        final List<EaccExecution> expected = createDefaultRestExecutionModel();
        final List<EaccExecution> actual = objectUnderTest.getAllExecutions();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void whenGetExecutionByJobName_AndJobNameDoesntExist_thenEmptyListReturned() {
        when(executionsRepository.findByJobName(any())).thenReturn(Optional.empty());
        assertThat(objectUnderTest.getExecutionByJobName(any())).isEmpty();
    }

    @Test
    public void whenExistsByExecutionIdCalled_andExecutionIdExists_thenMethodReturnsTrue() {
        when(executionsRepository.existsById(1L)).thenReturn(true);
        assertThat(objectUnderTest.existsByExecutionId("1")).isTrue();
    }

    @Test
    public void whenGetAllExecutionsMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(executionsRepository.findAll()).thenThrow(CannotCreateTransactionException.class);
        assertThatThrownBy(() -> objectUnderTest.getAllExecutions()).isInstanceOf(TransactionException.class);
        verify(executionsRepository, times(maxAttempts)).findAll();
    }

    @Test
    public void whenGetExecutionsByJobNameMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(executionsRepository.findByJobName(any())).thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.getExecutionByJobName("jobname")).isInstanceOf(DataAccessException.class);
        verify(executionsRepository, times(maxAttempts)).findByJobName(any());
    }

    @Test
    public void whenGetAuditResultsMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(auditResultsRepository.findAll(any(Specification.class), any(Pageable.class))).thenThrow(CannotCreateTransactionException.class)
                .thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.getAuditResults("executionId", null, null, Collections.emptyList()))
                .isInstanceOf(DataAccessException.class);
        verify(auditResultsRepository, times(maxAttempts)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    public void whenProcessApprovedChangesMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(auditResultsRepository.findByExecutionIdAndAuditStatusAndIdIn(any(), any(), any())).thenThrow(CannotCreateTransactionException.class)
                .thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.processApprovedChanges("executionId", new EaccApprovedAuditResults()))
                .isInstanceOf(DataAccessException.class);
        verify(auditResultsRepository, times(maxAttempts)).findByExecutionIdAndAuditStatusAndIdIn(any(), any(), any());
    }

    @Test
    public void whenIsRulesetInUseMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(executionsRepository.existsByExecutionStatusAndRuleSetName(any(), any())).thenThrow(CannotCreateTransactionException.class)
                .thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.isRulesetInUse("rulesetname")).isInstanceOf(DataAccessException.class);
        verify(executionsRepository, times(maxAttempts)).existsByExecutionStatusAndRuleSetName(any(), any());
    }

    @Test
    public void whenIsScopeInUseMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(executionsRepository.existsByExecutionStatusAndScopeName(any(), any())).thenThrow(CannotCreateTransactionException.class)
                .thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.isScopeInUse("scopename")).isInstanceOf(DataAccessException.class);
        verify(executionsRepository, times(maxAttempts)).existsByExecutionStatusAndScopeName(any(), any());
    }

    @Test
    public void whenIsJobInUseMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(executionsRepository.existsByExecutionStatusAndJobName(any(), any())).thenThrow(CannotCreateTransactionException.class)
                .thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.isJobInUse("jobname")).isInstanceOf(DataAccessException.class);
        verify(executionsRepository, times(maxAttempts)).existsByExecutionStatusAndJobName(any(), any());
    }

    @Test
    public void whenExistsByExecutionIdMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(executionsRepository.existsById(1L)).thenThrow(CannotCreateTransactionException.class)
                .thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.existsByExecutionId("1")).isInstanceOf(DataAccessException.class);
        verify(executionsRepository, times(maxAttempts)).existsById(1L);
    }

    @Test
    public void whenGetAllAuditResultsIsRequestedWithoutPagination_thenAllAuditResultsAreReturned() {
        final AuditResultsSpecification auditResultsExecutionIdSpecification = new AuditResultsSpecification(
                Optional.of(new FilterCriteria("executionId", QueryOperator.EQUALS, "1", null)));
        final AuditResultsSpecification auditResultsFilterSpecification = null;
        final Specification<AuditResult> specification = Specification.where(auditResultsExecutionIdSpecification)
                .and(auditResultsFilterSpecification);
        when(auditResultsRepository.findAll(refEq(specification), eq(Pageable.unpaged())))
                .thenReturn(new PageImpl<>(createTwoAuditResults()));
        final List<EaccAuditResult> expected = createTwoEaccAuditResults();
        final EaccPaginatedAuditResults actual = objectUnderTest.getAuditResults("1", null, null, Collections.emptyList());
        assertThat(actual.getResults()).hasSize(2)
                .isEqualTo(expected);
        assertThat(actual.getTotalElements()).isEqualTo(2);
        assertThat(actual.getTotalPages()).isEqualTo(1);
        assertThat(actual.getHasNext()).isFalse();
        assertThat(actual.getHasPrev()).isFalse();
        assertThat(actual.getPerPage()).isNull();
    }

    @Test
    public void whenGetAuditResultsIsRequestedWithPaginationForPageZeroAndPageSizeTwo_thenReturnTwoOfTheTenAuditResults() {
        final Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "id"));
        final AuditResultsSpecification auditResultsExecutionIdSpecification = new AuditResultsSpecification(
                Optional.of(new FilterCriteria("executionId", QueryOperator.EQUALS, "1", null)));
        final AuditResultsSpecification auditResultsFilterSpecification = null;
        final Specification<AuditResult> a = Specification.where(auditResultsExecutionIdSpecification).and(auditResultsFilterSpecification);

        when(auditResultsRepository.findAll(refEq(a), eq(pageable)))
                .thenReturn(new PageImpl<>(createTwoAuditResults(), pageable, 10));
        final List<EaccAuditResult> expected = createTwoEaccAuditResults();
        final EaccPaginatedAuditResults actual = objectUnderTest.getAuditResults("1", 0, 2, Collections.emptyList());
        assertThat(actual.getResults()).hasSize(2)
                .isEqualTo(expected);
        assertThat(actual.getTotalElements()).isEqualTo(10);
        assertThat(actual.getTotalPages()).isEqualTo(5);
        assertThat(actual.getHasNext()).isTrue();
        assertThat(actual.getHasPrev()).isFalse();
        assertThat(actual.getPerPage()).isEqualTo(2);
        assertThat(actual.getCurrentPage()).isZero();
    }

    @Test
    public void whenGetAuditResultsIsRequestedWithPaginationForPageTwoAndPageSizeTwo_thenReturnTwoOfTheTenAuditResults() {
        final Pageable pageable = PageRequest.of(2, 2, Sort.by(Sort.Direction.ASC, "id"));
        final AuditResultsSpecification auditResultsExecutionIdSpecification = new AuditResultsSpecification(
                Optional.of(new FilterCriteria("executionId", QueryOperator.EQUALS, "1", null)));
        final AuditResultsSpecification auditResultsFilterSpecification = null;
        final Specification<AuditResult> specification = Specification.where(auditResultsExecutionIdSpecification)
                .and(auditResultsFilterSpecification);

        when(auditResultsRepository.findAll(refEq(specification), eq(pageable)))
                .thenReturn(new PageImpl<>(createTwoAuditResults(), pageable, 10));
        final List<EaccAuditResult> expected = createTwoEaccAuditResults();
        final EaccPaginatedAuditResults actual = objectUnderTest.getAuditResults("1", 2, 2, Collections.emptyList());
        assertThat(actual.getResults()).hasSize(2)
                .isEqualTo(expected);
        assertThat(actual.getTotalElements()).isEqualTo(10);
        assertThat(actual.getTotalPages()).isEqualTo(5);
        assertThat(actual.getHasNext()).isTrue();
        assertThat(actual.getHasPrev()).isTrue();
        assertThat(actual.getPerPage()).isEqualTo(2);
        assertThat(actual.getCurrentPage()).isEqualTo(2);
    }

    @Test
    public void whenGetAuditResultsIsRequestedWithPaginationForPageFourAndPageSizeTwo_thenReturnTwoOfTheTenAuditResults() {
        final Pageable pageable = PageRequest.of(4, 2, Sort.by(Sort.Direction.ASC, "id"));
        final AuditResultsSpecification auditResultsExecutionIdSpecification = new AuditResultsSpecification(
                Optional.of(new FilterCriteria("executionId", QueryOperator.EQUALS, "1", null)));
        final AuditResultsSpecification auditResultsFilterSpecification = null;
        final Specification<AuditResult> specification = Specification.where(auditResultsExecutionIdSpecification)
                .and(auditResultsFilterSpecification);

        when(auditResultsRepository.findAll(refEq(specification), eq(pageable)))
                .thenReturn(new PageImpl<>(createTwoAuditResults(), pageable, 10));

        final List<EaccAuditResult> expected = createTwoEaccAuditResults();
        final EaccPaginatedAuditResults actual = objectUnderTest.getAuditResults("1", 4, 2, Collections.emptyList());
        assertThat(actual.getResults()).hasSize(2)
                .isEqualTo(expected);
        assertThat(actual.getTotalElements()).isEqualTo(10);
        assertThat(actual.getTotalPages()).isEqualTo(5);
        assertThat(actual.getHasNext()).isFalse();
        assertThat(actual.getHasPrev()).isTrue();
        assertThat(actual.getPerPage()).isEqualTo(2);
        assertThat(actual.getCurrentPage()).isEqualTo(4);
    }

    @Test
    public void whenGetAllAuditResultsIsRequestedWithoutPagination_andNoneExist_thenEmptyResultIsReturned() {

        final AuditResultsSpecification auditResultsExecutionIdSpecification = new AuditResultsSpecification(
                Optional.of(new FilterCriteria("executionId", QueryOperator.EQUALS, "1", null)));
        final AuditResultsSpecification auditResultsFilterSpecification = null;
        final Specification<AuditResult> specification = Specification.where(auditResultsExecutionIdSpecification)
                .and(auditResultsFilterSpecification);

        when(auditResultsRepository.findAll(refEq(specification), eq(Pageable.unpaged())))
                .thenReturn(Page.empty());

        final EaccPaginatedAuditResults actual = objectUnderTest.getAuditResults("1", null, null, Collections.emptyList());
        assertThat(actual.getResults()).isEmpty();
        assertThat(actual.getTotalElements()).isZero();
        assertThat(actual.getTotalPages()).isEqualTo(1);
        assertThat(actual.getHasNext()).isFalse();
        assertThat(actual.getHasPrev()).isFalse();
        assertThat(actual.getPerPage()).isNull();
    }

    @Test
    void whenJobExecutionWithRulesetNameInProgress_andInProgress_thenShouldReturnTrue() {
        when(executionsRepository.existsByExecutionStatusAndRuleSetName(ExecutionStatus.AUDIT_IN_PROGRESS, DEFAULT_RULESET_NAME))
                .thenReturn(true);
        assertThat(objectUnderTest.isRulesetInUse(DEFAULT_RULESET_NAME)).isTrue();
    }

    @Test
    void whenJobExecutionWithRulesetNameInProgress_andNotInProgress_thenShouldReturnFalse() {
        when(executionsRepository.existsByExecutionStatusAndRuleSetName(ExecutionStatus.AUDIT_IN_PROGRESS, DEFAULT_RULESET_NAME))
                .thenReturn(false);
        assertThat(objectUnderTest.isRulesetInUse(DEFAULT_RULESET_NAME)).isFalse();
    }

    @Test
    void whenJobExecutionWitJobNameInProgress_andInProgress_thenShouldReturnTrue() {
        when(executionsRepository.existsByExecutionStatusAndJobName(ExecutionStatus.AUDIT_IN_PROGRESS, DEFAULT_JOB_NAME))
                .thenReturn(true);
        assertThat(objectUnderTest.isJobInUse(DEFAULT_JOB_NAME)).isTrue();
    }

    @Test
    void whenJobExecutionWithJobNameInProgress_andNotInProgress_thenShouldReturnFalse() {
        when(executionsRepository.existsByExecutionStatusAndJobName(ExecutionStatus.AUDIT_IN_PROGRESS, DEFAULT_JOB_NAME))
                .thenReturn(false);
        assertThat(objectUnderTest.isJobInUse(DEFAULT_JOB_NAME)).isFalse();
    }

    @Test
    public void whenProcessProposedChangesIsCalled_withInvalidChanges_thenErrorIsLoggedAsynchronously()
            throws Exception {
        changeImplementationService.ncmpService = ncmpService;
        changeImplementationService.auditResultService = auditResultService;
        changeImplementationService.executionsRepository = executionsRepository;
        changeImplementationService.metricService = metricService;
        final List<AuditResult> auditResults = createTwoAuditResults();
        final AuditResult firstAuditResult = auditResults.get(0);
        final List<Execution> executionList = createDefaultDomainExecutionModel();
        final Execution firstExecution = executionList.get(0);
        mockAuditResultService();
        when(auditResultsRepository.findByExecutionIdAndAuditStatusAndIdIn("1", AuditStatus.INCONSISTENT, List.of(1L, 2L))).thenReturn(auditResults);
        when(executionsRepository.findById(any())).thenReturn(Optional.of(firstExecution));
        doCallRealMethod().when(changeImplementationService).processApprovedChangesAsync(any(), anyLong());
        doAnswer(throwExceptionAfterDelay()).when(ncmpService).patchManagedObjects(any(), any(), any());
        final EaccApprovedAuditResults eaccApprovedAuditResults = new EaccApprovedAuditResults();
        eaccApprovedAuditResults.setAuditResultIds(List.of("1", "2"));
        assertThat(auditResults.get(0).getChangeStatus()).as("Test Setup Failure").isNotEqualTo(IMPLEMENTATION_FAILED);

        objectUnderTest.processApprovedChanges("1", eaccApprovedAuditResults);
        await().alias("Waiting for change to be attempted asynchronously.")
                .atMost(60, TimeUnit.SECONDS).with()
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> firstAuditResult.getChangeStatus().equals(IMPLEMENTATION_FAILED));

        logAppender.stop();
        assertThat(Optional.of(firstExecution).get().getExecutionStatus()).isEqualTo(ExecutionStatus.CHANGES_FAILED);
        assertThat(logAppender.getLoggedEvents()).asString()
                .contains("[ERROR] Failed to apply change(s) for fdn:%s".formatted(auditResults.get(0).getManagedObjectFdn()));
    }

    @Test
    void whenApplyAllChangesIsCalled_thenAllChangesAreMovedToInProgress() throws Exception {
        final EaccApprovedAuditResults approvedAuditResults = new EaccApprovedAuditResults(List.of("1", "2"));
        approvedAuditResults.approveForAll(true);
        final List<AuditResult> auditResults = createTwoAuditResults();
        final Map<String, List<AuditResult>> auditResultsMappedByFdn = auditResults.stream()
                .collect(Collectors.groupingBy(AuditResult::getManagedObjectFdn));
        final List<Execution> executions = createDefaultDomainExecutionModel();
        changeImplementationService.ncmpService = ncmpService;
        changeImplementationService.auditResultService = auditResultService;
        changeImplementationService.executionsRepository = executionsRepository;
        changeImplementationService.metricService = metricService;

        when(auditResultsRepository.findAll(any(Specification.class))).thenReturn(auditResults);
        when(executionsRepository.findById(1L)).thenReturn(Optional.of(executions.get(0)));

        assertThat(auditResults.get(0).getChangeStatus()).isNull();
        assertThat(auditResults.get(1).getChangeStatus()).isNull();

        objectUnderTest.processApprovedChanges("1", approvedAuditResults);

        assertThat(auditResults.get(0).getChangeStatus()).isEqualTo(IMPLEMENTATION_IN_PROGRESS);
        assertThat(auditResults.get(1).getChangeStatus()).isEqualTo(IMPLEMENTATION_IN_PROGRESS);
        verify(changeImplementationService, times(1)).processApprovedChangesAsync(
                auditResultsMappedByFdn, executions.get(0).getId());
    }

    @Test
    void whenApplySelectedChangesIsCalled_thenCorrectChangesAreMovedToInProgress() throws Exception {
        final EaccApprovedAuditResults approvedAuditResults = new EaccApprovedAuditResults(List.of("1"));
        final List<AuditResult> auditResults = createTwoAuditResults();
        final Map<String, List<AuditResult>> auditResultMappedByFdn = Map.of(auditResults.get(0).getManagedObjectFdn(), List.of(auditResults.get(0)));
        final List<Execution> executions = createDefaultDomainExecutionModel();
        changeImplementationService.ncmpService = ncmpService;
        changeImplementationService.auditResultService = auditResultService;
        changeImplementationService.executionsRepository = executionsRepository;
        changeImplementationService.metricService = metricService;

        when(auditResultsRepository.findByExecutionIdAndAuditStatusAndIdIn("1", AuditStatus.INCONSISTENT,
                List.of(1L))).thenReturn(List.of(auditResults.get(0)));
        doCallRealMethod().when(changeImplementationService).processApprovedChangesAsync(any(), anyLong());
        when(executionsRepository.findById(1L)).thenReturn(Optional.of(executions.get(0)));

        assertThat(auditResults.get(0).getChangeStatus()).isNull();
        assertThat(auditResults.get(1).getChangeStatus()).isNull();

        objectUnderTest.processApprovedChanges("1", approvedAuditResults);

        assertThat(auditResults.get(0).getChangeStatus()).isEqualTo(IMPLEMENTATION_IN_PROGRESS);
        assertThat(auditResults.get(1).getChangeStatus()).isNull();
        verify(changeImplementationService, times(1))
                .processApprovedChangesAsync(auditResultMappedByFdn, executions.get(0).getId());
    }

    @Test
    public void whenApplyAllChangesIsCalled_AndExecutionDoesntExist_thenCorrectExceptionIsThrown() {
        final EaccApprovedAuditResults eaccApprovedAuditResults = new EaccApprovedAuditResults(List.of("1", "2"));
        eaccApprovedAuditResults.approveForAll(true);

        when(executionsRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> objectUnderTest.processApprovedChanges("1", eaccApprovedAuditResults))
                .isInstanceOf(ProposedIdsNotFoundException.class)
                .hasMessage(PROPOSED_IDS_DONT_EXIST);
    }

    @Test
    public void whenApplySelectedChangesIsCalled_AndAuditResultsDontExist_thenCorrectExceptionIsThrown() {
        final EaccApprovedAuditResults eaccApprovedAuditResults = new EaccApprovedAuditResults(List.of("1", "2"));
        final List<AuditResult> auditResultsEmptyList = new ArrayList<>();

        when(auditResultsRepository.findByExecutionIdAndAuditStatusAndIdIn("1", AuditStatus.INCONSISTENT, List.of(1L, 2L)))
                .thenReturn(auditResultsEmptyList);

        assertThatThrownBy(() -> objectUnderTest.processApprovedChanges("1", eaccApprovedAuditResults))
                .isInstanceOf(ProposedIdsNotFoundException.class)
                .hasMessage(PROPOSED_IDS_DONT_EXIST);
        verify(auditResultsRepository, times(1)).findByExecutionIdAndAuditStatusAndIdIn("1", AuditStatus.INCONSISTENT, List.of(1L, 2L));
    }

    @Test
    public void whenApplyAllChangesIsCalled_AndChangesAreInProgress_thenCorrectExceptionIsThrown() {
        final EaccApprovedAuditResults approvedAuditResults = new EaccApprovedAuditResults(List.of("1", "2"));
        approvedAuditResults.approveForAll(true);
        final List<Execution> executions = createDefaultDomainExecutionModel();
        executions.get(0).setExecutionStatus(ExecutionStatus.CHANGES_IN_PROGRESS);

        when(executionsRepository.findById(1L)).thenReturn(Optional.of(executions.get(0)));

        assertThatThrownBy(() -> objectUnderTest.processApprovedChanges("1", approvedAuditResults))
                .isInstanceOf(ChangesInProgressException.class)
                .hasMessage(CHANGES_IN_PROGRESS);

    }

    @Test
    public void whenApplyAllChangesIsCalled_AndReversionsAreInProgress_thenCorrectExceptionIsThrown() {
        final EaccApprovedAuditResults approvedAuditResults = new EaccApprovedAuditResults(List.of("1", "2"));
        approvedAuditResults.approveForAll(true);
        final List<Execution> executions = createDefaultDomainExecutionModel();
        executions.get(0).setExecutionStatus(ExecutionStatus.REVERSION_IN_PROGRESS);

        when(executionsRepository.findById(1L)).thenReturn(Optional.of(executions.get(0)));

        assertThatThrownBy(() -> objectUnderTest.processApprovedChanges("1", approvedAuditResults))
                .isInstanceOf(ChangesInProgressException.class)
                .hasMessage(REVERSIONS_IN_PROGRESS);

    }

    @Test
    public void whenApplySelectedChangesIsCalled_AndChangesAreInProgress_thenCorrectExceptionIsThrown() {
        final EaccApprovedAuditResults approvedAuditResults = new EaccApprovedAuditResults(List.of("1", "2"));
        final List<Execution> executions = createDefaultDomainExecutionModel();

        when(auditResultsRepository.existsByExecutionIdAndChangeStatusAndIdIn("1", IMPLEMENTATION_IN_PROGRESS, List.of(1L, 2L)))
                .thenReturn(true);

        assertThatThrownBy(() -> objectUnderTest.processApprovedChanges("1", approvedAuditResults))
                .isInstanceOf(ChangesInProgressException.class)
                .hasMessage(CHANGES_IN_PROGRESS);

    }

    @Test
    public void whenProcessProposedChangesIsCalled_withValidAndInvalidChanges_thenExecutionStatusIsPartiallySuccessful() throws Exception {
        changeImplementationService.ncmpService = ncmpService;
        changeImplementationService.auditResultService = auditResultService;
        changeImplementationService.executionsRepository = executionsRepository;
        changeImplementationService.metricService = metricService;
        final List<AuditResult> auditResults = createFourInconsistentAuditResults();
        final List<Execution> executionList = createDefaultDomainExecutionModel();
        final Execution firstExecution = executionList.get(0);
        mockAuditResultService();
        when(auditResultsRepository.findByExecutionIdAndAuditStatusAndIdIn("1", AuditStatus.INCONSISTENT, List.of(1L, 2L, 3L, 4L)))
                .thenReturn(auditResults);
        when(executionsRepository.findById(any())).thenReturn(Optional.of(firstExecution));
        doCallRealMethod().when(changeImplementationService).processApprovedChangesAsync(any(), anyLong());
        doAnswer(throwExceptionAfterDelay()).doNothing().when(ncmpService).patchManagedObjects(any(), any(), any());
        final EaccApprovedAuditResults eaccApprovedAuditResults = new EaccApprovedAuditResults();
        eaccApprovedAuditResults.setAuditResultIds(List.of("1", "2", "3", "4"));
        objectUnderTest.processApprovedChanges("1", eaccApprovedAuditResults);

        await().alias("Waiting for change to be attempted asynchronously.")
                .atMost(60, TimeUnit.SECONDS).with()
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> firstExecution.getExecutionStatus().equals(ExecutionStatus.CHANGES_PARTIALLY_SUCCESSFUL));

        assertThat(firstExecution.getExecutionStatus()).isEqualTo(ExecutionStatus.CHANGES_PARTIALLY_SUCCESSFUL);
    }

    private static Answer throwExceptionAfterDelay() {
        return invocation -> {
            try {
                Thread.sleep(10 * 1_000); //NOSONAR Adding delay so that async call can be mocked
            } catch (final InterruptedException e) {
                fail("TEST SETUP EXCEPTION - Unable to suspend Thread");
            }
            throw new CMServiceException("problem");
        };
    }

    @Test
    public void whenRevertChangesIsCalled_forAllChanges_thenChangeImplementationServiceIsCalled() {
        final List<Execution> executionList = createDefaultDomainExecutionModel();
        final Execution firstExecution = executionList.get(0);
        when(executionsRepository.findJobNameByExecutionId(1L))
                .thenReturn(Optional.of(DEFAULT_JOB_NAME));
        when(executionsRepository.findFirstByJobNameOrderByExecutionStartedAtDesc(DEFAULT_JOB_NAME))
                .thenReturn(Optional.of(firstExecution));
        when(auditResultsRepository.findByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusIn("1", AuditStatus.INCONSISTENT,
                List.of(REVERSION_COMPLETE)))
                .thenReturn(createTwoRevertedEaccAuditResults());
        when(auditResultsRepository.findByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusNotIn("1", AuditStatus.INCONSISTENT,
                List.of(ChangeStatus.IMPLEMENTATION_IN_PROGRESS, ChangeStatus.REVERSION_IN_PROGRESS, REVERSION_COMPLETE, IMPLEMENTATION_FAILED)))
                .thenReturn(createTwoAuditResults());
        final EaccApprovedAuditResults eaccApprovedAuditResults = new EaccApprovedAuditResults().approveForAll(true)
                .operation(EaccOperation.REVERT_CHANGE);
        assertThatCode(() -> objectUnderTest.revertChanges("1", eaccApprovedAuditResults))
                .doesNotThrowAnyException();
        verify(changeImplementationService, times(1)).revertChangesAsync(anyList(), eq(1L));
        verify(auditResultsRepository, times(1)).saveAll(anyList());

        logAppender.stop();
        final String exclusionMsg = "[WARN] Excluding %s as it has already reverted %s";
        assertThat(
                logAppender.getLoggedEvents().stream()
                        .filter(iLoggingEvent -> iLoggingEvent.getLevel().equals(Level.WARN)))
                .asString()
                .contains(exclusionMsg.formatted("fdn3", "attrName3"), exclusionMsg.formatted("fdn4", "attrName4"));
    }

    @Test
    void whenRevertChangesIsCalled_forSelectedChanges_thenChangeImplementationServiceIsCalled() {
        final List<Execution> executionList = createDefaultDomainExecutionModel();
        final Execution firstExecution = executionList.get(0);
        when(executionsRepository.findJobNameByExecutionId(1L))
                .thenReturn(Optional.of(DEFAULT_JOB_NAME));
        when(executionsRepository.findFirstByJobNameOrderByExecutionStartedAtDesc(DEFAULT_JOB_NAME))
                .thenReturn(Optional.of(firstExecution));
        when(auditResultsRepository.findByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusNotInAndIdIn("1", AuditStatus.INCONSISTENT,
                List.of(ChangeStatus.IMPLEMENTATION_IN_PROGRESS, ChangeStatus.REVERSION_IN_PROGRESS, REVERSION_COMPLETE, IMPLEMENTATION_FAILED),
                List.of(1L, 2L)))
                .thenReturn(createTwoAuditResults());
        when(auditResultsRepository.findByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusInAndIdIn("1", AuditStatus.INCONSISTENT,
                List.of(REVERSION_COMPLETE), List.of(1L, 2L)))
                .thenReturn(createTwoRevertedEaccAuditResults());
        final EaccApprovedAuditResults eaccApprovedAuditResults = new EaccApprovedAuditResults().approveForAll(false)
                .operation(EaccOperation.REVERT_CHANGE).auditResultIds(List.of("1", "2"));
        assertThatCode(() -> objectUnderTest.revertChanges("1", eaccApprovedAuditResults))
                .doesNotThrowAnyException();
        verify(changeImplementationService, times(1)).revertChangesAsync(anyList(), eq(1L));
        verify(auditResultsRepository, times(1)).saveAll(anyList());

        logAppender.stop();
        final String exclusionMsg = "[WARN] Excluding %s as it has already reverted %s";
        assertThat(
                logAppender.getLoggedEvents().stream()
                        .filter(iLoggingEvent -> iLoggingEvent.getLevel().equals(Level.WARN)))
                .asString()
                .contains(exclusionMsg.formatted("fdn3", "attrName3"), exclusionMsg.formatted("fdn4", "attrName4"));
    }

    @Test
    void whenRevertChangesIsCalled_forAllChangesAndChangesDoNotExist_thenCorrectExceptionIsThrown() {
        final List<Execution> executionList = createDefaultDomainExecutionModel();
        final Execution firstExecution = executionList.get(0);
        when(executionsRepository.findJobNameByExecutionId(1L))
                .thenReturn(Optional.of(DEFAULT_JOB_NAME));
        when(executionsRepository.findFirstByJobNameOrderByExecutionStartedAtDesc(DEFAULT_JOB_NAME))
                .thenReturn(Optional.of(firstExecution));
        when(auditResultsRepository.findByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusNotIn("1", AuditStatus.INCONSISTENT,
                List.of(ChangeStatus.IMPLEMENTATION_IN_PROGRESS, ChangeStatus.REVERSION_IN_PROGRESS)))
                .thenReturn(new ArrayList<>());
        final EaccApprovedAuditResults eaccApprovedAuditResults = new EaccApprovedAuditResults().approveForAll(true)
                .operation(EaccOperation.REVERT_CHANGE);
        assertThatThrownBy(() -> objectUnderTest.revertChanges("1", eaccApprovedAuditResults))
                .isInstanceOf(ProposedIdsNotFoundException.class)
                .hasMessage(PROPOSED_IDS_DONT_EXIST);
        verify(changeImplementationService, never()).revertChangesAsync(anyList(), eq(1L));
        verify(auditResultsRepository, never()).saveAll(anyList());
    }

    @Test
    void whenRevertChangesIsCalled_forSelectedChangesAndChangesDoNotExist_thenCorrectExceptionIsThrown() {
        final List<Execution> executionList = createDefaultDomainExecutionModel();
        final Execution firstExecution = executionList.get(0);
        when(executionsRepository.findJobNameByExecutionId(1L))
                .thenReturn(Optional.of(DEFAULT_JOB_NAME));
        when(executionsRepository.findFirstByJobNameOrderByExecutionStartedAtDesc(DEFAULT_JOB_NAME))
                .thenReturn(Optional.of(firstExecution));
        when(auditResultsRepository.findByExecutionIdAndAuditStatusAndChangeStatusNotNullAndChangeStatusNotInAndIdIn("1", AuditStatus.INCONSISTENT,
                List.of(ChangeStatus.IMPLEMENTATION_IN_PROGRESS, ChangeStatus.REVERSION_IN_PROGRESS), List.of(1L, 2L)))
                .thenReturn(new ArrayList<>());
        final EaccApprovedAuditResults eaccApprovedAuditResults = new EaccApprovedAuditResults().approveForAll(false)
                .operation(EaccOperation.REVERT_CHANGE).auditResultIds(List.of("1", "2"));
        assertThatThrownBy(() -> objectUnderTest.revertChanges("1", eaccApprovedAuditResults))
                .isInstanceOf(ProposedIdsNotFoundException.class)
                .hasMessage(PROPOSED_IDS_DONT_EXIST);
        verify(changeImplementationService, never()).revertChangesAsync(anyList(), eq(1L));
        verify(auditResultsRepository, never()).saveAll(anyList());
    }

    @Test
    void whenRevertChangesIsCalled_forAllChangesAndChangesAreInProgress_thenCorrectExceptionIsThrown() {
        final List<Execution> executionList = createDefaultDomainExecutionModel();
        final Execution firstExecution = executionList.get(0);
        when(executionsRepository.findJobNameByExecutionId(1L))
                .thenReturn(Optional.of(DEFAULT_JOB_NAME));
        when(executionsRepository.findFirstByJobNameOrderByExecutionStartedAtDesc(DEFAULT_JOB_NAME))
                .thenReturn(Optional.of(firstExecution));
        when(auditResultsRepository.existsByExecutionIdAndChangeStatus("1", ChangeStatus.IMPLEMENTATION_IN_PROGRESS))
                .thenReturn(true);
        final EaccApprovedAuditResults eaccApprovedAuditResults = new EaccApprovedAuditResults().approveForAll(true)
                .operation(EaccOperation.REVERT_CHANGE);
        assertThatThrownBy(() -> objectUnderTest.revertChanges("1", eaccApprovedAuditResults))
                .isInstanceOf(ChangesInProgressException.class)
                .hasMessage(CHANGES_IN_PROGRESS);
        verify(changeImplementationService, never()).revertChangesAsync(anyList(), eq(1L));
        verify(auditResultsRepository, never()).saveAll(anyList());
    }

    @Test
    void whenRevertChangesIsCalled_forSelectedChangesAndChangesAreInProgress_thenCorrectExceptionIsThrown() {
        final List<Execution> executionList = createDefaultDomainExecutionModel();
        final Execution firstExecution = executionList.get(0);
        when(executionsRepository.findJobNameByExecutionId(1L))
                .thenReturn(Optional.of(DEFAULT_JOB_NAME));
        when(executionsRepository.findFirstByJobNameOrderByExecutionStartedAtDesc(DEFAULT_JOB_NAME))
                .thenReturn(Optional.of(firstExecution));
        when(auditResultsRepository.existsByExecutionIdAndChangeStatusAndIdIn("1", ChangeStatus.IMPLEMENTATION_IN_PROGRESS, List.of(1L, 2L)))
                .thenReturn(true);
        final EaccApprovedAuditResults eaccApprovedAuditResults = new EaccApprovedAuditResults().approveForAll(false)
                .operation(EaccOperation.REVERT_CHANGE).auditResultIds(List.of("1", "2"));
        assertThatThrownBy(() -> objectUnderTest.revertChanges("1", eaccApprovedAuditResults))
                .isInstanceOf(ChangesInProgressException.class)
                .hasMessage(CHANGES_IN_PROGRESS);
        verify(changeImplementationService, never()).revertChangesAsync(anyList(), eq(1L));
        verify(auditResultsRepository, never()).saveAll(anyList());
    }

    @Test
    void whenRevertChangesIsCalled_withOldExecution_thenCorrectExceptionIsThrown() {
        final List<Execution> executionList = createDefaultDomainExecutionModel();
        final Execution firstExecution = executionList.get(0);
        when(executionsRepository.findJobNameByExecutionId(2L))
                .thenReturn(Optional.of(DEFAULT_JOB_NAME));
        when(executionsRepository.findFirstByJobNameOrderByExecutionStartedAtDesc(DEFAULT_JOB_NAME))
                .thenReturn(Optional.of(firstExecution));
        final EaccApprovedAuditResults eaccApprovedAuditResults = new EaccApprovedAuditResults().approveForAll(false)
                .operation(EaccOperation.REVERT_CHANGE).auditResultIds(List.of("1", "2"));
        assertThatThrownBy(() -> objectUnderTest.revertChanges("2", eaccApprovedAuditResults))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage(PREVIOUS_JOB_EXECUTION);
        verify(changeImplementationService, never()).revertChangesAsync(anyList(), eq(1L));
        verify(auditResultsRepository, never()).saveAll(anyList());
    }

    private List<EaccExecution> createDefaultRestExecutionModel() {
        final EaccExecution execution = new EaccExecution();
        execution.setId(String.valueOf(1L));
        execution.setJobName(DEFAULT_JOB_NAME);
        execution.setExecutionStartedAt(dateNow);
        execution.setExecutionEndedAt(dateNow);
        execution.setConsistencyAuditStartedAt(dateNow);
        execution.setConsistencyAuditEndedAt(dateNow);
        execution.setExecutionStatus(AUDIT_SUCCESSFUL.toString());
        execution.setExecutionType(OPEN_LOOP.toString());
        execution.setExecutionStatus(AUDIT_SUCCESSFUL.toString());
        execution.setTotalNodesAudited(2);
        execution.setTotalNodesFailed(1);
        execution.setTotalMosAudited(2);
        execution.setTotalAttributesAudited(4);
        execution.setInconsistenciesIdentified(3);
        return List.of(execution);
    }

    private List<Execution> createDefaultDomainExecutionModel() {
        final Execution execution = new Execution();
        execution.setId(1L);
        execution.setJobName(DEFAULT_JOB_NAME);
        execution.setExecutionStartedAt(dateNow);
        execution.setExecutionEndedAt(dateNow);
        execution.setConsistencyAuditStartedAt(dateNow);
        execution.setConsistencyAuditEndedAt(dateNow);
        execution.setExecutionStatus(AUDIT_SUCCESSFUL);
        execution.setExecutionType(OPEN_LOOP);
        execution.setTotalNodesAudited(2);
        execution.setTotalNodesFailed(1);
        execution.setTotalMosAudited(2);
        execution.setTotalAttributesAudited(4);
        execution.setInconsistenciesIdentified(3);
        return List.of(execution);
    }

    private List<AuditResult> createFourInconsistentAuditResults() {
        final List<AuditResult> list = new ArrayList<>(3);
        final AuditResult auditResult = new AuditResult();
        auditResult.setExecutionId("1");
        auditResult.setId(1L);
        auditResult.setManagedObjectFdn("fdn");
        auditResult.setManagedObjectType("moClass");
        auditResult.setAttributeName("attrName3");
        auditResult.setCurrentValue("5");
        auditResult.setPreferredValue("6");
        auditResult.setAuditStatus(AuditStatus.INCONSISTENT);
        auditResult.setRuleId("1");
        auditResult.setChangeStatus(null);
        list.add(auditResult);

        final AuditResult auditResult2 = new AuditResult();
        auditResult2.setExecutionId("1");
        auditResult2.setId(2L);
        auditResult2.setManagedObjectFdn("fdn");
        auditResult2.setManagedObjectType("moClass");
        auditResult2.setAttributeName("attrName3");
        auditResult2.setCurrentValue("5");
        auditResult2.setPreferredValue("6");
        auditResult2.setAuditStatus(AuditStatus.INCONSISTENT);
        auditResult2.setRuleId("1");
        auditResult2.setChangeStatus(null);
        list.add(auditResult2);

        final AuditResult auditResult3 = new AuditResult();
        auditResult3.setExecutionId("1");
        auditResult3.setId(3L);
        auditResult3.setManagedObjectFdn("fdn1");
        auditResult3.setManagedObjectType("moClass");
        auditResult3.setAttributeName("attrName3");
        auditResult3.setCurrentValue("5");
        auditResult3.setPreferredValue("6");
        auditResult3.setAuditStatus(AuditStatus.INCONSISTENT);
        auditResult3.setRuleId("1");
        auditResult3.setExecutionId("1");
        auditResult3.setChangeStatus(null);
        list.add(auditResult3);

        final AuditResult auditResult4 = new AuditResult();
        auditResult4.setExecutionId("1");
        auditResult4.setId(4L);
        auditResult4.setManagedObjectFdn("fdn1");
        auditResult4.setManagedObjectType("moClass");
        auditResult4.setAttributeName("attrName3");
        auditResult4.setCurrentValue("5");
        auditResult4.setPreferredValue("6");
        auditResult4.setAuditStatus(AuditStatus.INCONSISTENT);
        auditResult4.setRuleId("1");
        auditResult4.setExecutionId("1");
        auditResult4.setChangeStatus(null);
        list.add(auditResult4);
        return list;
    }

    private List<AuditResult> createTwoAuditResults() {
        final AuditResult auditResult = new AuditResult();
        auditResult.setExecutionId("1");
        auditResult.setId(1L);
        auditResult.setManagedObjectFdn("fdn");
        auditResult.setManagedObjectType("moClass");
        auditResult.setAttributeName("attrName");
        auditResult.setCurrentValue("2");
        auditResult.setPreferredValue("3");
        auditResult.setAuditStatus(AuditStatus.INCONSISTENT);
        auditResult.setRuleId("1");
        auditResult.setChangeStatus(null);
        final AuditResult auditResult2 = new AuditResult();
        auditResult2.setExecutionId("1");
        auditResult2.setId(2L);
        auditResult2.setManagedObjectFdn("fdn2");
        auditResult2.setManagedObjectType("moClass2");
        auditResult2.setAttributeName("attrName2");
        auditResult2.setCurrentValue("2");
        auditResult2.setPreferredValue("2");
        auditResult2.setAuditStatus(AuditStatus.INCONSISTENT);
        auditResult2.setRuleId("1");
        auditResult2.setExecutionId("1");
        auditResult2.setChangeStatus(null);
        return List.of(auditResult, auditResult2);
    }

    private List<EaccAuditResult> createTwoEaccAuditResults() {
        final EaccAuditResult eaccAuditResult = new EaccAuditResult();
        eaccAuditResult.setExecutionId("1");
        eaccAuditResult.setId("1");
        eaccAuditResult.setManagedObjectFdn("fdn");
        eaccAuditResult.setManagedObjectType("moClass");
        eaccAuditResult.setAttributeName("attrName");
        eaccAuditResult.setCurrentValue("2");
        eaccAuditResult.setPreferredValue("3");
        eaccAuditResult.setAuditStatus(AuditStatus.INCONSISTENT.toString());
        eaccAuditResult.setRuleId("1");
        eaccAuditResult.setChangeStatus("");
        final EaccAuditResult eaccAuditResult2 = new EaccAuditResult();
        eaccAuditResult2.setExecutionId("1");
        eaccAuditResult2.setId("3");
        eaccAuditResult2.setManagedObjectFdn("fdn2");
        eaccAuditResult2.setManagedObjectType("moClass2");
        eaccAuditResult2.setAttributeName("attrName2");
        eaccAuditResult2.setCurrentValue("2");
        eaccAuditResult2.setPreferredValue("2");
        eaccAuditResult2.setAuditStatus(AuditStatus.INCONSISTENT.toString());
        eaccAuditResult2.setRuleId("1");
        eaccAuditResult2.setChangeStatus("");
        return List.of(eaccAuditResult, eaccAuditResult2);
    }

    private List<AuditResult> createTwoRevertedEaccAuditResults() {
        final AuditResult auditResult = new AuditResult();
        auditResult.setExecutionId("1");
        auditResult.setId(3L);
        auditResult.setManagedObjectFdn("fdn3");
        auditResult.setManagedObjectType("moClass3");
        auditResult.setAttributeName("attrName3");
        auditResult.setCurrentValue("2");
        auditResult.setPreferredValue("3");
        auditResult.setAuditStatus(AuditStatus.INCONSISTENT);
        auditResult.setRuleId("1");
        auditResult.setChangeStatus(REVERSION_COMPLETE);
        final AuditResult auditResult2 = new AuditResult();
        auditResult2.setExecutionId("1");
        auditResult2.setId(4L);
        auditResult2.setManagedObjectFdn("fdn4");
        auditResult2.setManagedObjectType("moClass4");
        auditResult2.setAttributeName("attrName4");
        auditResult2.setCurrentValue("2");
        auditResult2.setPreferredValue("2");
        auditResult2.setAuditStatus(AuditStatus.CONSISTENT);
        auditResult2.setRuleId("1");
        auditResult2.setExecutionId("1");
        auditResult2.setChangeStatus(REVERSION_COMPLETE);
        return List.of(auditResult, auditResult2);
    }

    private void mockAuditResultService() {
        doAnswer((invocation) -> {
            final var auditResults = (List<AuditResult>) invocation.getArgument(0, List.class);
            final var status = invocation.getArgument(1, ChangeStatus.class);
            auditResults.forEach(auditResult -> auditResult.setChangeStatus(status));
            return true;
        }).when(auditResultService).saveAll(anyList(), any());
    }

}
