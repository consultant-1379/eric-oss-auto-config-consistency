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

package com.ericsson.oss.apps.controller;

import static com.ericsson.assertions.EaccAssertions.assertThat;
import static com.ericsson.assertions.EaccAssertions.assertThatCDException;
import static com.ericsson.oss.apps.api.model.EaccOperation.REVERT_CHANGE;
import static com.ericsson.oss.apps.util.Constants.APPLICATION_JSON;
import static com.ericsson.oss.apps.util.Constants.CHANGES_IN_PROGRESS;
import static com.ericsson.oss.apps.util.Constants.EMPTY_STRING;
import static com.ericsson.oss.apps.util.Constants.EXECUTION_AND_RULESET_NAME_AND_JOB_ID_MAX_LENGTH;
import static com.ericsson.oss.apps.util.Constants.INVALID_CHANGE_OPERATION;
import static com.ericsson.oss.apps.util.Constants.PREVIOUS_JOB_EXECUTION;
import static com.ericsson.oss.apps.util.Constants.PROPOSED_IDS_DONT_EXIST;
import static com.ericsson.oss.apps.util.Constants.REGEX_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_COMPLETE;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_FAILED;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_JOB_NAME;
import static com.ericsson.oss.apps.util.TestDefaults.INJECTED_LOG_TO_VALIDATE_TEST;
import static com.ericsson.oss.apps.util.TestDefaults.INVALID_FDN_TOO_LONG;
import static com.ericsson.oss.apps.util.TestDefaults.REALLY_LONG_ALPHANUMERIC_STRING;
import static com.ericsson.oss.apps.util.TestDefaults.TEST_SETUP_ERROR_UNABLE_TO_ACCESS_LOGS;
import static com.ericsson.oss.apps.util.TestDefaults.VALID_JOBNAME_FOR_VALIDATION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;

import com.ericsson.assertions.validation.ValidationObjectSoftAssertions;

import com.ericsson.oss.apps.api.model.EaccApprovedAuditResults;
import com.ericsson.oss.apps.api.model.EaccAuditResult;
import com.ericsson.oss.apps.api.model.EaccExecution;
import com.ericsson.oss.apps.api.model.EaccPaginatedAuditResults;
import com.ericsson.oss.apps.executor.AuditStatus;
import com.ericsson.oss.apps.executor.ChangeStatus;
import com.ericsson.oss.apps.executor.ExecutionStatus;
import com.ericsson.oss.apps.service.ExecutionService;
import com.ericsson.oss.apps.service.ValidationService;
import com.ericsson.oss.apps.service.exception.ChangesInProgressException;
import com.ericsson.oss.apps.service.exception.InconsistencyProcessingFailedException;
import com.ericsson.oss.apps.service.exception.UnsupportedOperationException;
import com.ericsson.oss.apps.service.exception.ProposedIdsNotFoundException;
import com.ericsson.oss.apps.util.InMemoryLogAppender;
import com.ericsson.oss.apps.util.ValidationObject;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Unit tests for {@link ExecutionController} class.
 */
@ExtendWith(MockitoExtension.class)
public class ExecutionControllerTest {

    private static final boolean IS_VALIDATED = true;
    private static final ValidationObject VALIDATION_SUCCESS = new ValidationObject(IS_VALIDATED, EMPTY_STRING, VALIDATION_COMPLETE, HttpStatus.OK);

    private static final String DEFAULT_EXECUTION_ID = "1";

    @InjectMocks
    private ExecutionController objectUnderTest;

    @Mock
    private ExecutionService executionService;

    @Mock
    private ValidationService validationService;

    @InjectSoftAssertions
    private ValidationObjectSoftAssertions softly;

    private static final String MANAGED_ELEMENT_FDN = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030";
    private static final String GNBCUCP_FUNCTION_FDN = MANAGED_ELEMENT_FDN + ",GNBCUCPFunction=1";
    private static final String NR_CELL_CU_FDN = GNBCUCP_FUNCTION_FDN + ",NRCellCU=NR03gNodeBRadio00030-4";
    private static final String NR_CELL_DU_FDN = MANAGED_ELEMENT_FDN + ",GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4";
    private static final String NR_CELL_DU = "NRCellDU";
    private static InMemoryLogAppender logAppender;

    EaccAuditResult auditResult1;
    EaccAuditResult auditResult2;
    EaccAuditResult auditResult3;
    EaccAuditResult auditResult4;

    @BeforeEach
    public void setup() {
        auditResult1 = createEaccAuditResult("1", NR_CELL_DU_FDN, NR_CELL_DU,
                "csiRsShiftingPrimary", "DEACTIVATED", "DEACTIVATED",
                AuditStatus.CONSISTENT, "16", null);
        auditResult2 = createEaccAuditResult("2", NR_CELL_DU_FDN, NR_CELL_DU,
                "subCarrierSpacing", "120", "110",
                AuditStatus.INCONSISTENT, "17", ChangeStatus.IMPLEMENTATION_IN_PROGRESS);
        auditResult3 = createEaccAuditResult("3", NR_CELL_CU_FDN, "NRCellCU",
                "mcpcPSCellEnabled", "false", "true",
                AuditStatus.INCONSISTENT, "14", ChangeStatus.IMPLEMENTATION_FAILED);
        auditResult4 = createEaccAuditResult("4", GNBCUCP_FUNCTION_FDN, "GNBCUCPFunction",
                "maxNgRetryTime", "30", "20",
                AuditStatus.INCONSISTENT, "12", ChangeStatus.IMPLEMENTATION_COMPLETE);

        final Logger logUnderTest = (Logger) LoggerFactory.getLogger(ExecutionController.class);
        logUnderTest.setLevel(Level.DEBUG);
        logAppender = new InMemoryLogAppender();
        logAppender.start();
        logUnderTest.addAppender(logAppender);
        logUnderTest.error(INJECTED_LOG_TO_VALIDATE_TEST);
    }

    @AfterEach
    public void checkLogsAreAccessible() {
        assertThat(logAppender.getLoggedEvents()).asString()
                .as(TEST_SETUP_ERROR_UNABLE_TO_ACCESS_LOGS)
                .contains(INJECTED_LOG_TO_VALIDATE_TEST);
    }

    @Test
    public void whenGetAllExecutionsEndpointHit_thenReturnAllExecutions() {
        when(executionService.getAllExecutions()).thenReturn(createBasicExecutionRestModel());
        assertThat(objectUnderTest.getExecutions(APPLICATION_JSON, EMPTY_STRING)).hasBody(createBasicExecutionRestModel());
        verify(executionService, never()).getExecutionByJobName(anyString());
    }

    @Test
    public void whenGetExecutionsEndpointHitWithJobNameSpecified_thenReturnExecutionsForThatJobName() {
        when(executionService.getExecutionByJobName(DEFAULT_JOB_NAME)).thenReturn(createBasicExecutionRestModel());
        when(validationService.validateJobNameAndJobExists(DEFAULT_JOB_NAME)).thenReturn(VALIDATION_SUCCESS);
        assertThat(objectUnderTest.getExecutions(APPLICATION_JSON, DEFAULT_JOB_NAME)).hasBody(createBasicExecutionRestModel());
        verify(executionService, times(1)).getExecutionByJobName(DEFAULT_JOB_NAME);
    }

    @Test
    public void whenGetExecutionsEndpointHitWithLongJobNameSpecified_thenExceptionIsThrown() {
        when(validationService.validateJobNameAndJobExists(VALID_JOBNAME_FOR_VALIDATION))
                .thenReturn(new ValidationObject(!IS_VALIDATED, VALIDATION_FAILED,
                        "Job Name should be a string of length less than 100 characters", BAD_REQUEST));
        assertThatCDException(() -> objectUnderTest.getExecutions(APPLICATION_JSON, VALID_JOBNAME_FOR_VALIDATION))
                .hasMessage("%s \"Job Name should be a string of length less than %s characters\"", BAD_REQUEST,
                        EXECUTION_AND_RULESET_NAME_AND_JOB_ID_MAX_LENGTH)
                .hasDetail(VALIDATION_FAILED)
                .hasTitle("Job Name should be a string of length less than 100 characters");
    }

    @Test
    public void whenGetExecutionsEndpointHitWithIllegalJobNameSpecified_thenExceptionIsThrown() {
        when(validationService.validateJobNameAndJobExists(VALID_JOBNAME_FOR_VALIDATION))
                .thenReturn(new ValidationObject(!IS_VALIDATED, VALIDATION_FAILED, REGEX_JOBNAME_ERROR, BAD_REQUEST));
        assertThatCDException(() -> objectUnderTest.getExecutions(APPLICATION_JSON, VALID_JOBNAME_FOR_VALIDATION))
                .hasMessage("%s \"Invalid jobName: Only lower case alphanumeric, underscores and dashes allowed\"", BAD_REQUEST)
                .hasDetail(VALIDATION_FAILED)
                .hasTitle("Invalid jobName: Only lower case alphanumeric, underscores and dashes allowed");
    }

    @Test
    public void whenGetAllExecutionsEndpointHit_andNoExecutionsExist_thenEmptyListReturned() {
        when(executionService.getAllExecutions()).thenReturn(Collections.emptyList());
        assertThat(objectUnderTest.getExecutions(APPLICATION_JSON, EMPTY_STRING)).hasBody(Collections.EMPTY_LIST);
    }

    @Test
    public void whenGetAllExecutionsEndpointHit_andErrorThrown_thenReturn500() {
        final RuntimeException cause = new RuntimeException();
        when(executionService.getAllExecutions()).thenThrow(cause);

        assertThatCDException(() -> objectUnderTest.getExecutions(APPLICATION_JSON, EMPTY_STRING))
                .hasMessage("%s \"Failed to get execution(s).\"", INTERNAL_SERVER_ERROR, cause)
                .hasSuppressedException(cause)
                .hasNoDetail()
                .hasTitle("Failed to get execution(s).");
    }

    @Test
    public void whenGetAllExecutionsEndpointHit_andDataAccessExceptionCaught_thenThrowCorrectControllerDetailException() {
        when(executionService.getAllExecutions()).thenThrow(DataAccessResourceFailureException.class);

        assertThatCDException(() -> objectUnderTest.getExecutions(APPLICATION_JSON, EMPTY_STRING))
                .hasStatus((INTERNAL_SERVER_ERROR))
                .hasReason("Failed to get execution(s).")
                .hasDetail("Database operation failed.");
    }

    @Test
    public void whenGetExecutionsByJobNameEndpointHit_andErrorThrown_thenReturn500() {
        final RuntimeException cause = new RuntimeException();
        when(executionService.getExecutionByJobName(DEFAULT_JOB_NAME)).thenThrow(cause);
        when(validationService.validateJobNameAndJobExists(DEFAULT_JOB_NAME)).thenReturn(VALIDATION_SUCCESS);
        assertThatCDException(() -> objectUnderTest.getExecutions(APPLICATION_JSON, DEFAULT_JOB_NAME))
                .hasSuppressedException(cause)
                .hasTitle("Failed to get execution(s).")
                .hasNoDetail();
    }

    @Test
    public void whenGetAuditResultsByExecIdEndpointHitWithoutPagination_thenReturnAllAuditResults() {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(validationService.validatePagination(any(), any())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);
        when(executionService.getAuditResults(DEFAULT_EXECUTION_ID, null, null, Collections.emptyList()))
                .thenReturn(createEaccPaginatedAuditResult(4L, 1, null, null, false, false, Arrays.asList(auditResult1,
                        auditResult2, auditResult3, auditResult4)));
        assertThat(objectUnderTest.getAuditResults(DEFAULT_EXECUTION_ID, APPLICATION_JSON, null, null, null))
                .hasBody(createEaccPaginatedAuditResult(4L, 1, null, null, false, false, Arrays.asList(auditResult1,
                        auditResult2, auditResult3, auditResult4)));
    }

    @Test
    public void whenGetAuditResultsByExecIdEndpointHitWithPaginationForPageZeroAndPageSizeThree_thenReturnThreeOfTheFourAuditResults() {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(validationService.validatePagination(any(), any())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);
        when(executionService.getAuditResults(DEFAULT_EXECUTION_ID, 0, 3, Collections.emptyList()))
                .thenReturn(createEaccPaginatedAuditResult(4L, 2, 3, 0, false, true, Arrays.asList(auditResult1,
                        auditResult2, auditResult3)));
        assertThat(objectUnderTest.getAuditResults(DEFAULT_EXECUTION_ID, APPLICATION_JSON, 0, 3, null))
                .hasBody(createEaccPaginatedAuditResult(4L, 2, 3, 0, false, true, Arrays.asList(auditResult1,
                        auditResult2, auditResult3)));
    }

    @Test
    public void whenGetAuditResultsByExecIdEndpointHitWithPaginationForPage1AndPageSizeThree_thenReturnOneOfTheFourAuditResults() {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(validationService.validatePagination(any(), any())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);
        when(executionService.getAuditResults(DEFAULT_EXECUTION_ID, 1, 3, Collections.emptyList()))
                .thenReturn(createEaccPaginatedAuditResult(4L, 2, 3, 1, true, false, List.of(auditResult4)));
        assertThat(objectUnderTest.getAuditResults(DEFAULT_EXECUTION_ID, APPLICATION_JSON, 1, 3, null))
                .hasBody(createEaccPaginatedAuditResult(4L, 2, 3, 1, true, false, List.of(auditResult4)));
    }

    @Test
    public void whenGetAuditResultsWithLongNameByExecIdEndpointHit_thenThrowException() {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        assertThatCDException(() -> objectUnderTest.getAuditResults(REALLY_LONG_ALPHANUMERIC_STRING, APPLICATION_JSON, null, null, null))
                .hasMessage("%s \"Validation failed\"", BAD_REQUEST)
                .hasDetail("Execution ID can only contain numeric characters.");
    }

    @Test
    public void whenGetAuditResultsWithEmptyNameByExecIdEndpointHit_thenThrowException() {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        assertThatCDException(() -> objectUnderTest.getAuditResults(EMPTY_STRING, APPLICATION_JSON, null, null, null))
                .hasMessage("%s \"Validation failed\"", BAD_REQUEST)
                .hasDetail("Execution Id should not be blank");
    }

    @Test
    public void whenGetAuditResultsWithIllegalCharactersNameByExecIdEndpointHit_thenThrowException() {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        assertThatCDException(() -> objectUnderTest.getAuditResults("A*", APPLICATION_JSON, null, null, null))
                .hasStatus(BAD_REQUEST)
                .hasMessage("%s \"Validation failed\"", BAD_REQUEST)
                .hasDetail("Execution ID can only contain numeric characters.")
                .hasTitle("Validation failed");
    }

    @Test
    public void whenGetAuditResultsByExecIdEndpointHit_andErrorThrown_thenReturn500() {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(validationService.validatePagination(any(), any())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);
        final RuntimeException cause = new RuntimeException();
        when(executionService.getAuditResults(DEFAULT_EXECUTION_ID, null, null, Collections.emptyList())).thenThrow(cause);

        assertThatCDException(() -> objectUnderTest.getAuditResults(DEFAULT_EXECUTION_ID, APPLICATION_JSON, null, null, null))
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasMessage("%s \"Failed to get audit result(s).\"", INTERNAL_SERVER_ERROR, cause)
                .hasSuppressedException(cause)
                .hasNoDetail()
                .hasTitle("Failed to get audit result(s).");
    }

    @Test
    public void whenGetAuditResultsByExecIdEndpointHit_andDataAccessExceptionCaught_thenThrowCorrectControllerDetailException() {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(validationService.validatePagination(any(), any())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);
        when(executionService.getAuditResults(DEFAULT_EXECUTION_ID, null, null, Collections.emptyList()))
                .thenThrow(DataAccessResourceFailureException.class);

        assertThatCDException(() -> objectUnderTest.getAuditResults(DEFAULT_EXECUTION_ID, APPLICATION_JSON, null, null, null))
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasReason("Failed to get audit results.")
                .hasDetail("Database operation failed.");
    }

    @Test
    public void whenGetAuditResultsByExecIdEndpointHit_andExecIdDoesNotExist_thenThrowCorrectControllerDetailException() {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(false);

        assertThatCDException(() -> objectUnderTest.getAuditResults(DEFAULT_EXECUTION_ID, APPLICATION_JSON, null, null, null))
                .hasStatus(NOT_FOUND)
                .hasReason("Execution ID does not exist.")
                .hasDetail("Provide a valid Execution ID.");
    }

    @Test
    public void whenGetAuditResultsByExecIdEndpointHitWithPaginationForPageLessThanZero_thenThrowException() {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);
        when(validationService.validatePagination(any(), any())).thenCallRealMethod();

        assertThatCDException(() -> objectUnderTest.getAuditResults(DEFAULT_EXECUTION_ID, null, -1, null, null))
                .hasStatus(BAD_REQUEST)
                .hasMessage("%s \"Validation failed\"", BAD_REQUEST)
                .hasDetail("Invalid page number: Must not be a negative value.")
                .hasReason("Validation failed");
    }

    @Test
    public void whenGetAuditResultsByExecIdEndpointHitWithPaginationForPageSizeLessThanOne_thenThrowException() {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);
        when(validationService.validatePagination(any(), any())).thenCallRealMethod();

        assertThatCDException(() -> objectUnderTest.getAuditResults(DEFAULT_EXECUTION_ID, null, null, -1, null))
                .hasStatus(BAD_REQUEST)
                .hasMessage("%s \"Validation failed\"", BAD_REQUEST)
                .hasDetail("Invalid pageSize: Use only numeric values of 1 or more.")
                .hasReason("Validation failed");
    }

    @Test
    public void whenGetAuditResultsByExecIdEndpointHitWithFilterForFdnThatIsTooLong_thenThrowException() {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);
        when(validationService.validatePagination(any(), any())).thenCallRealMethod();
        when(validationService.validateFilter(any())).thenCallRealMethod();
        assertThatCDException(() -> objectUnderTest.getAuditResults(DEFAULT_EXECUTION_ID, null, null, null, "managedObjectFdn:" + INVALID_FDN_TOO_LONG))
                .hasStatus(BAD_REQUEST)
                .hasMessage("%s \"Validation failed\"", BAD_REQUEST)
                .hasDetail("Invalid filter value: FDN value is too long at more than 1600 characters")
                .hasReason("Validation failed");
        assertInvalidTextHasNotBeenLogged(INVALID_FDN_TOO_LONG);
    }

    @Test
    public void whenGetAuditResultsByExecIdEndpointHitWithFilterForFdnThatIsInValid_thenThrowException() {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);
        when(validationService.validatePagination(any(), any())).thenCallRealMethod();
        when(validationService.validateFilter(any())).thenCallRealMethod();
        final String illegalCharacters = "managedObjectFdn:*";
        assertThatCDException(() -> objectUnderTest.getAuditResults(DEFAULT_EXECUTION_ID, null,  null, null, illegalCharacters))
                .hasStatus(BAD_REQUEST)
                .hasMessage("%s \"Validation failed\"", BAD_REQUEST)
                .hasDetail("Invalid filter value. The allowed characters for values, are as follows:'A-Z', 'a-z', '0-9', '-' , '_', '/' , ',', '.' , '%' , '&', '!', '?', ' ' (space) and ':'")
                .hasReason("Validation failed");
        assertInvalidTextHasNotBeenLogged(illegalCharacters);
    }

    @Test
    public void whenGetAuditResultsWithFilter_AuditStatusIsValid_thenReturnThreeOfTheFourAuditResults() {
        final String filterString = "auditStatus:Inconsistent";
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(validationService.validatePagination(any(), any())).thenCallRealMethod();
        when(validationService.validateFilter(any())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);
        when(executionService.getAuditResults(DEFAULT_EXECUTION_ID, 0, 3, List.of(filterString)))
                .thenReturn(createEaccPaginatedAuditResult(4L, 2, 3, 0, false, true, Arrays.asList(auditResult1,
                        auditResult2, auditResult3)));

        assertThat(objectUnderTest.getAuditResults(DEFAULT_EXECUTION_ID, APPLICATION_JSON, 0, 3, filterString))
                .hasBody(createEaccPaginatedAuditResult(4L, 2, 3, 0, false, true, Arrays.asList(auditResult1,
                        auditResult2, auditResult3)));
    }

    @Test
    public void whenGetAuditResultsWithFilter_AuditStatusIsValid_AND_ManagedObjectFdnIsValid_thenReturnThreeOfTheFourAuditResults() {
        final String filterString = "auditStatus:Inconsistent$managedObjectFdn:SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00030-4";
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(validationService.validatePagination(any(), any())).thenCallRealMethod();
        when(validationService.validateFilter(any())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);
        when(executionService.getAuditResults(DEFAULT_EXECUTION_ID, 0, 3, List.of("auditStatus:Inconsistent", "managedObjectFdn:SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00030-4")))
                .thenReturn(createEaccPaginatedAuditResult(4L, 2, 3, 0, false, true, Arrays.asList(auditResult1,
                        auditResult2, auditResult3)));

        assertThat(objectUnderTest.getAuditResults(DEFAULT_EXECUTION_ID, APPLICATION_JSON, 0, 3, filterString))
                .hasBody(createEaccPaginatedAuditResult(4L, 2, 3, 0, false, true, Arrays.asList(auditResult1,
                        auditResult2, auditResult3)));
    }

    @Test
    public void whenGetAuditResultsWithFilter_AuditStatusIsValid_AND_ManagedObjectFdnIsValid_AND_FilterKeyThatIsInValid_thenThrowException() {
        final String filterString = "auditStatus:Inconsistent$managedObjectFdn:SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00030-4$inValidKey:value";
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(validationService.validatePagination(any(), any())).thenCallRealMethod();
        when(validationService.validateFilter(any())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);

        assertThatCDException(() -> objectUnderTest.getAuditResults(DEFAULT_EXECUTION_ID, APPLICATION_JSON, 0, 3, filterString))
                .hasStatus(BAD_REQUEST)
                .hasMessage("%s \"Validation failed\"", BAD_REQUEST)
                .hasDetail("Invalid filter key: The allowed filters are: [auditStatus, managedObjectFdn, changeStatus]")
                .hasReason("Validation failed");
        assertInvalidTextHasNotBeenLogged("inValidKey:value");
    }

    @Test
    public void whenApplyChangesWithInvalidExecId_thenThrowException() {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        final EaccApprovedAuditResults eaccApprovedAuditResults = new EaccApprovedAuditResults();
        eaccApprovedAuditResults.setAuditResultIds(List.of(DEFAULT_EXECUTION_ID));
        assertThatCDException(() -> objectUnderTest.applyChange("A*", eaccApprovedAuditResults,
                APPLICATION_JSON, APPLICATION_JSON))
                        .hasStatus(BAD_REQUEST)
                        .hasMessage("%s \"Validation failed\"", BAD_REQUEST)
                        .hasDetail("Execution ID can only contain numeric characters.")
                        .hasTitle("Validation failed");
    }

    @Test
    public void whenProposedChangeIdsDontExist_thenCorrectExceptionIsThrown() throws Exception {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);
        doThrow(new ProposedIdsNotFoundException(PROPOSED_IDS_DONT_EXIST))
                .when(executionService).processApprovedChanges(anyString(), any(EaccApprovedAuditResults.class));
        final EaccApprovedAuditResults eaccApprovedAuditResults = new EaccApprovedAuditResults();
        eaccApprovedAuditResults.setAuditResultIds(List.of(DEFAULT_EXECUTION_ID));

        assertThatCDException(() -> objectUnderTest.applyChange(DEFAULT_EXECUTION_ID, eaccApprovedAuditResults,
                APPLICATION_JSON, APPLICATION_JSON))
                        .hasStatus(BAD_REQUEST)
                        .hasMessage("%s \"Validation failed\"", BAD_REQUEST)
                        .hasDetail(PROPOSED_IDS_DONT_EXIST)
                        .hasReason(VALIDATION_FAILED);
    }

    @Test
    public void whenApplyChangeFails_thenCorrectExceptionIsThrown() throws Exception {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);
        doThrow(new InconsistencyProcessingFailedException("Failed to apply change(s)"))
                .when(executionService).processApprovedChanges(anyString(), any(EaccApprovedAuditResults.class));
        final EaccApprovedAuditResults eaccApprovedAuditResults = new EaccApprovedAuditResults();
        eaccApprovedAuditResults.setAuditResultIds(List.of(DEFAULT_EXECUTION_ID));

        assertThatCDException(() -> objectUnderTest.applyChange(DEFAULT_EXECUTION_ID, eaccApprovedAuditResults,
                APPLICATION_JSON, APPLICATION_JSON))
                        .hasStatus(INTERNAL_SERVER_ERROR)
                        .hasMessage("%s \"Failed to apply change(s)\"", INTERNAL_SERVER_ERROR)
                        .hasNoDetail()
                        .hasTitle("Failed to apply change(s)");
    }

    @Test
    public void whenApplyChangeEndpointHit_andDataAccessExceptionCaught_thenThrowCorrectControllerDetailException()
            throws InconsistencyProcessingFailedException, ChangesInProgressException, ProposedIdsNotFoundException {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);
        doThrow(DataAccessResourceFailureException.class)
                .when(executionService)
                .processApprovedChanges(anyString(), any(EaccApprovedAuditResults.class));
        final EaccApprovedAuditResults eaccApprovedAuditResults = new EaccApprovedAuditResults();
        eaccApprovedAuditResults.setAuditResultIds(List.of(DEFAULT_EXECUTION_ID));

        assertThatCDException(() -> objectUnderTest.applyChange(DEFAULT_EXECUTION_ID, eaccApprovedAuditResults,
                APPLICATION_JSON, APPLICATION_JSON))
                        .hasStatus(INTERNAL_SERVER_ERROR)
                        .hasReason("Failed to apply change.")
                        .hasDetail("Database operation failed.");
    }

    @Test
    public void whenApplyChangeIsSent_thenCorrectStatusReturned() {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);
        assertThat(objectUnderTest.applyChange(DEFAULT_EXECUTION_ID, new EaccApprovedAuditResults(),
                APPLICATION_JSON, APPLICATION_JSON)).hasStatus(HttpStatus.ACCEPTED);
    }

    @Test
    void whenApplyChangesIsSent_withInvalidOperation_thenCorrectExceptionIsThrown() {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);
        assertThatCDException(() -> objectUnderTest.applyChange(DEFAULT_EXECUTION_ID, new EaccApprovedAuditResults().operation(null),
                APPLICATION_JSON, APPLICATION_JSON))
                        .hasStatus(BAD_REQUEST)
                        .hasMessage("%s \"Validation failed\"", BAD_REQUEST)
                        .hasDetail(INVALID_CHANGE_OPERATION)
                        .hasReason("Validation failed");
    }

    @Test
    void whenRevertChangesIsSent_thenCorrectStatusIsReturned() {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);
        assertThat(objectUnderTest.applyChange(DEFAULT_EXECUTION_ID, new EaccApprovedAuditResults().operation(REVERT_CHANGE),
                APPLICATION_JSON, APPLICATION_JSON))
                        .hasStatus(HttpStatus.ACCEPTED);
    }

    @Test
    void whenRevertChangesIsSent_withInvalidExecId_thenCorrectExceptionIsThrown() {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        assertThatCDException(() -> objectUnderTest.applyChange("A*", new EaccApprovedAuditResults().operation(REVERT_CHANGE),
                APPLICATION_JSON, APPLICATION_JSON))
                        .hasStatus(BAD_REQUEST)
                        .hasMessage("%s \"Validation failed\"", BAD_REQUEST)
                        .hasDetail("Execution ID can only contain numeric characters.")
                        .hasReason("Validation failed");
    }

    @Test
    void whenRevertChangesIsSent_andChangesDoNotExist_thenCorrectExceptionIsThrown() throws ChangesInProgressException, UnsupportedOperationException, ProposedIdsNotFoundException {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);
        doThrow(new ProposedIdsNotFoundException(PROPOSED_IDS_DONT_EXIST))
                .when(executionService)
                .revertChanges(anyString(), any(EaccApprovedAuditResults.class));
        assertThatCDException(() -> objectUnderTest.applyChange(DEFAULT_EXECUTION_ID, new EaccApprovedAuditResults().operation(REVERT_CHANGE),
                APPLICATION_JSON, APPLICATION_JSON))
                        .hasStatus(BAD_REQUEST)
                        .hasMessage("%s \"Validation failed\"", BAD_REQUEST)
                        .hasDetail(PROPOSED_IDS_DONT_EXIST)
                        .hasReason(VALIDATION_FAILED);
    }

    @Test
    void whenRevertChangesIsSent_andChangesAreInProgress_thenCorrectExceptionIsThrown()
            throws ChangesInProgressException, ProposedIdsNotFoundException, UnsupportedOperationException {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);
        doThrow(new ChangesInProgressException(CHANGES_IN_PROGRESS))
                .when(executionService)
                .revertChanges(anyString(), any(EaccApprovedAuditResults.class));
        assertThatCDException(() -> objectUnderTest.applyChange(DEFAULT_EXECUTION_ID, new EaccApprovedAuditResults().operation(REVERT_CHANGE),
                APPLICATION_JSON, APPLICATION_JSON))
                        .hasStatus(BAD_REQUEST)
                        .hasMessage("%s \"Validation failed\"", BAD_REQUEST)
                        .hasDetail(CHANGES_IN_PROGRESS)
                        .hasReason(VALIDATION_FAILED);
    }

    @Test
    void whenRevertChangesIsSent_withOldExecution_thenCorrectExceptionIsThrown() throws ChangesInProgressException, UnsupportedOperationException, ProposedIdsNotFoundException {
        when(validationService.validateExecutionIdFormat(anyString())).thenCallRealMethod();
        when(executionService.existsByExecutionId(anyString())).thenReturn(true);
        doThrow(new UnsupportedOperationException(PREVIOUS_JOB_EXECUTION))
                .when(executionService)
                .revertChanges(anyString(), any(EaccApprovedAuditResults.class));
        assertThatCDException(() -> objectUnderTest.applyChange(DEFAULT_EXECUTION_ID, new EaccApprovedAuditResults().operation(REVERT_CHANGE),
                APPLICATION_JSON, APPLICATION_JSON))
                        .hasStatus(BAD_REQUEST)
                        .hasMessage("%s \"Validation failed\"", BAD_REQUEST)
                        .hasDetail(PREVIOUS_JOB_EXECUTION)
                        .hasReason(VALIDATION_FAILED);
    }

    private List<EaccExecution> createBasicExecutionRestModel() {
        //TODO: multiple jobs -> multiple executions
        final var execution = new EaccExecution();
        execution.setJobName(DEFAULT_JOB_NAME);
        execution.setExecutionStatus(ExecutionStatus.AUDIT_SUCCESSFUL.toString());
        return List.of(execution);
    }

    private EaccAuditResult createEaccAuditResult(final String id, final String fdn, final String moType,
            final String attributeName, final String currentVale,
            final String preferredValue, final AuditStatus status,
            final String ruleId, final ChangeStatus changeStatus) {
        final EaccAuditResult auditResult = new EaccAuditResult();
        auditResult.setId(id);
        auditResult.setManagedObjectFdn(fdn);
        auditResult.setManagedObjectType(moType);
        auditResult.setAttributeName(attributeName);
        auditResult.setCurrentValue(currentVale);
        auditResult.setPreferredValue(preferredValue);
        auditResult.setAuditStatus(status.toString());
        auditResult.setExecutionId(DEFAULT_EXECUTION_ID);
        auditResult.setRuleId(ruleId);
        if (changeStatus == null) {
            auditResult.setChangeStatus("");
        } else {
            auditResult.setChangeStatus(changeStatus.toString());
        }
        return auditResult;
    }

    private EaccPaginatedAuditResults createEaccPaginatedAuditResult(final Long totalElements, final Integer totalPages,
            final Integer perPage,
            final Integer currentPage, final Boolean hasPrev, final Boolean hasNext,
            final List<EaccAuditResult> auditResults) {
        final EaccPaginatedAuditResults eaccPaginatedAuditResults = new EaccPaginatedAuditResults();
        eaccPaginatedAuditResults.setTotalElements(totalElements);
        eaccPaginatedAuditResults.setTotalPages(totalPages);
        eaccPaginatedAuditResults.setPerPage(perPage);
        eaccPaginatedAuditResults.setCurrentPage(currentPage);
        eaccPaginatedAuditResults.setHasPrev(hasPrev);
        eaccPaginatedAuditResults.setHasNext(hasNext);
        eaccPaginatedAuditResults.results(auditResults);
        return eaccPaginatedAuditResults;
    }

    private void assertInvalidTextHasNotBeenLogged(final String invalidText) {
        logAppender.stop();
        final List<ILoggingEvent> loggedEvents = logAppender.getLoggedEvents();
        assertThat(loggedEvents).asString()
                .as(TEST_SETUP_ERROR_UNABLE_TO_ACCESS_LOGS)
                .contains(INJECTED_LOG_TO_VALIDATE_TEST);
        assertThat(loggedEvents).asString()
                .contains(INJECTED_LOG_TO_VALIDATE_TEST)
                .doesNotContain(invalidText);
    }
}
