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
import static com.ericsson.oss.apps.util.Constants.APPLICATION_JSON;
import static com.ericsson.oss.apps.util.Constants.DATABASE_OPERATION_FAILED;
import static com.ericsson.oss.apps.util.Constants.EMPTY_STRING;
import static com.ericsson.oss.apps.util.Constants.INVALID_JOBNAME_FOR_DELETE_OR_UPDATE;
import static com.ericsson.oss.apps.util.Constants.NONEXISTENT_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.NONEXISTENT_RULESET_ERROR;
import static com.ericsson.oss.apps.util.Constants.REGEX_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_COMPLETE;
import static com.ericsson.oss.apps.util.TestDefaults.INVALID_RULESET_NAME_MESSAGE;
import static com.ericsson.oss.apps.util.TestDefaults.SCOPE_ATHLONE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ericsson.oss.apps.api.model.EaccJob;
import com.ericsson.oss.apps.service.ExecutionService;
import com.ericsson.oss.apps.service.JobService;
import com.ericsson.oss.apps.service.ValidationService;
import com.ericsson.oss.apps.util.Constants;
import com.ericsson.oss.apps.util.TestDefaults;
import com.ericsson.oss.apps.util.ValidationObject;

import lombok.val;

/**
 * Unit tests for {@link JobController} class.
 */
@ExtendWith(MockitoExtension.class)
public class JobControllerTest {
    private static final boolean IS_VALIDATED = true;
    private static final ValidationObject VALIDATION_SUCCESS = new ValidationObject(IS_VALIDATED, EMPTY_STRING, VALIDATION_COMPLETE, HttpStatus.OK);
    private static final String VALIDATION_FAILED_TITLE = "Validation failed";
    private static final String S_VALIDATION_FAILED = "%s \"Validation failed\"";
    private static final String BAD_REQUEST_FAILED_MSG = S_VALIDATION_FAILED.formatted(BAD_REQUEST);

    @InjectMocks
    private JobController objectUnderTest;

    @Mock
    private JobService jobService;

    @Mock
    private ExecutionService executionService;

    @Mock
    private ValidationService validationService;

    @Test
    public void whenGetAllJobsEndpointIsHit_thenReturnAllJobs() {
        when(jobService.getAllJobs()).thenReturn(createBasicJobRestModel());
        assertThat(objectUnderTest.getJobs()).hasBody(createBasicJobRestModel());
    }

    @Test
    public void whenGetAllJobsEndpointIsHit_andErrorThrown_theReturn500() {
        final RuntimeException cause = new RuntimeException();
        when(jobService.getAllJobs()).thenThrow(cause);

        assertThatCDException(() -> objectUnderTest.getJobs())
                .hasMessage("%s \"Failed to get job(s).\"", HttpStatus.INTERNAL_SERVER_ERROR, cause)
                .hasSuppressedException(cause)
                .hasNoDetail()
                .hasTitle("Failed to get job(s).");
    }

    @Test
    public void whenGetAllJobsEndpointIsHit_andDataAccessExceptionCaught_thenControllerDetailExceptionIsThrown() {
        when(jobService.getAllJobs()).thenThrow(DataAccessResourceFailureException.class);

        assertThatCDException(() -> objectUnderTest.getJobs())
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasReason("Failed to get job(s).")
                .hasDetail(DATABASE_OPERATION_FAILED);
    }

    @Test
    public void whenPostJobEndpointIsHit_thenReturnNewlyCreatedJob() {
        final EaccJob eaccJob = createBasicJobRestModel().get(0);
        when(jobService.getAllJobs()).thenReturn(createBasicJobRestModel());
        when(jobService.create(eaccJob)).thenReturn(createBasicJobRestModel().get(0));
        when(validationService.validateJobAndThatJobNameDoesNotExist(eaccJob)).thenReturn(VALIDATION_SUCCESS);
        val response = objectUnderTest.postJobs(eaccJob);
        final EaccJob expected = objectUnderTest.getJobs().getBody().get(0);
        assertThat(response).hasBody(expected);
    }

    @Test
    public void whenPostJobsEndpointIsHit_andRulesetDoesNotExist_thenReturn400BadRequest() {
        final EaccJob job = createBasicJobRestModel().get(0);
        final ValidationObject validationObject = new ValidationObject(!IS_VALIDATED, NONEXISTENT_RULESET_ERROR, Constants.VALIDATION_FAILED,
                BAD_REQUEST);
        when(validationService.validateJobAndThatJobNameDoesNotExist(job)).thenReturn(validationObject);
        assertThatCDException(() -> objectUnderTest.postJobs(job))
                .hasMessage(BAD_REQUEST_FAILED_MSG)
                .hasDetail("Invalid Ruleset: Ruleset does not exist")
                .hasTitle(VALIDATION_FAILED_TITLE);
    }

    @Test
    public void whenPostJobsEndpointIsHit_andRulesetDoesExist_thenReturn201CreatedRequest() {
        final EaccJob job = createBasicJobRestModel().get(0);
        when(validationService.validateJobAndThatJobNameDoesNotExist(job)).thenReturn(VALIDATION_SUCCESS);
        when(jobService.create(job)).thenReturn(job);
        final ResponseEntity<EaccJob> result = objectUnderTest.postJobs(job);
        assertThat(result).hasStatus(HttpStatus.CREATED)
                .hasBody();
        assertThat(result.getBody().getJobName()).isEqualTo(job.getJobName());
    }

    @Test
    public void whenPostJobsEndpointIsHit_andNoRulesetIsIncluded_thenReturn400BadRequest() {
        final EaccJob requestJob = createBasicJobRestModel().get(0);
        requestJob.setRulesetName(null);
        final ValidationObject validationObject = new ValidationObject(!IS_VALIDATED, NONEXISTENT_RULESET_ERROR, Constants.VALIDATION_FAILED,
                BAD_REQUEST);
        when(validationService.validateJobAndThatJobNameDoesNotExist(requestJob)).thenReturn(validationObject);
        assertThatCDException(() -> objectUnderTest.postJobs(requestJob))
                .hasStatus(BAD_REQUEST)
                .hasMessage(BAD_REQUEST_FAILED_MSG)
                .hasDetail("Invalid Ruleset: Ruleset does not exist")
                .hasTitle(VALIDATION_FAILED_TITLE);
    }

    @Test
    public void whenPostJobEndpointIsHit_andDataAccessExceptionOccurs_thenControllerDetailExceptionThrown() {
        final EaccJob eaccJob = createBasicJobRestModel().get(0);
        final DataAccessResourceFailureException cause = new DataAccessResourceFailureException("Some data access failure");
        when(validationService.validateJobAndThatJobNameDoesNotExist(eaccJob))
                .thenThrow(cause);
        assertThatCDException(() -> objectUnderTest.postJobs(eaccJob))
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasSuppressedException(cause)
                .hasDetail(DATABASE_OPERATION_FAILED)
                .hasReason("Failed to create job.");
    }

    @Test
    public void whenDeleteJobEndpointIsHit_andJobExists_thenJobIsDeleted() {
        when(validationService.validateJobNameAndJobExists(anyString())).thenReturn(VALIDATION_SUCCESS);
        val response = objectUnderTest.deleteJob("job1", APPLICATION_JSON);
        assertThat(response).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    public void whenDeleteJobEndpointIsHit_andJobDoesNotExist_then404NotFoundReturned() {
        final ValidationObject validationObject = new ValidationObject(!IS_VALIDATED, NONEXISTENT_JOBNAME_ERROR,
                Constants.VALIDATION_FAILED, HttpStatus.NOT_FOUND);

        when(validationService.validateJobNameAndJobExists(anyString())).thenReturn(validationObject);

        assertThatCDException(() -> objectUnderTest.deleteJob("job2", APPLICATION_JSON))
                .hasStatus(NOT_FOUND)
                .hasMessage(S_VALIDATION_FAILED, NOT_FOUND)
                .hasDetail("Invalid jobName: jobName does not exist")
                .hasTitle(VALIDATION_FAILED_TITLE);
    }

    @Test
    public void whenDeleteJobEndpointIsHit_andInvalidNameGiven_thenBadRequestIsReturned() {
        final ValidationObject validationObject = new ValidationObject(!IS_VALIDATED, INVALID_JOBNAME_FOR_DELETE_OR_UPDATE,
                Constants.VALIDATION_FAILED, BAD_REQUEST);

        when(validationService.validateJobNameAndJobExists(null)).thenReturn(validationObject);
        assertThatCDException(() -> objectUnderTest.deleteJob(null, APPLICATION_JSON))
                .hasStatus(BAD_REQUEST)
                .hasMessage(BAD_REQUEST_FAILED_MSG)
                .hasDetail(
                        "Invalid resource identifier in url: Only lower case alphanumeric, underscores and dashes allowed, with between 4 -100 characters.")
                .hasTitle(VALIDATION_FAILED_TITLE);
    }

    @Test
    public void whenDeleteJobEndpointIsHit_andUppercaseNameGiven_thenBadRequestIsReturned() {
        final ValidationObject validationObject = new ValidationObject(!IS_VALIDATED, INVALID_JOBNAME_FOR_DELETE_OR_UPDATE,
                Constants.VALIDATION_FAILED, BAD_REQUEST);

        when(validationService.validateJobNameAndJobExists("JOB1")).thenReturn(validationObject);
        assertThatCDException(() -> objectUnderTest.deleteJob("JOB1", APPLICATION_JSON))
                .hasStatus(BAD_REQUEST)
                .hasMessage(BAD_REQUEST_FAILED_MSG)
                .hasDetail(
                        "Invalid resource identifier in url: Only lower case alphanumeric, underscores and dashes allowed, with between 4 -100 characters.")
                .hasTitle(VALIDATION_FAILED_TITLE);
    }

    @Test
    public void whenDeleteJobEndpointIsHit_andDataAccessExceptionOccurs_thenControllerDetailExceptionThrown() {
        final EaccJob eaccJob = createBasicJobRestModel().get(0);
        final DataAccessResourceFailureException cause = new DataAccessResourceFailureException("Some data access failure");
        when(validationService.validateJobNameAndJobExists(eaccJob.getJobName()))
                .thenThrow(cause);
        assertThatCDException(() -> objectUnderTest.deleteJob(eaccJob.getJobName(), APPLICATION_JSON))
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasSuppressedException(cause)
                .hasDetail(DATABASE_OPERATION_FAILED)
                .hasReason("Failed to delete job.");
    }

    @Test
    public void whenPostJobEndpointIsHit_andJobIsNotValid_thenNotFoundIsReturned() {
        final EaccJob eaccJob = createBasicJobRestModel().get(0);
        when(validationService.validateJobAndThatJobNameDoesNotExist(any()))
                .thenReturn(new ValidationObject(!IS_VALIDATED, REGEX_JOBNAME_ERROR, Constants.VALIDATION_FAILED, HttpStatus.BAD_REQUEST));

        assertThatCDException(() -> objectUnderTest.postJobs(eaccJob))
                .hasStatus(BAD_REQUEST)
                .hasMessage(BAD_REQUEST_FAILED_MSG)
                .hasDetail("Invalid jobName: Only lower case alphanumeric, underscores and dashes allowed")
                .hasTitle(VALIDATION_FAILED_TITLE);
    }

    @Test
    public void whenPostJobEndpointIsHit_andJobNameIsUppercase_thenNotFoundIsReturned() {
        final EaccJob eaccJob = createBasicJobRestModel().get(0);
        eaccJob.setJobName("JOB1");
        when(validationService.validateJobAndThatJobNameDoesNotExist(any())).thenCallRealMethod();

        assertThatCDException(() -> objectUnderTest.postJobs(eaccJob))
                .hasStatus(BAD_REQUEST)
                .hasMessage(BAD_REQUEST_FAILED_MSG)
                .hasDetail("Invalid jobName: Only lower case alphanumeric, underscores and dashes allowed")
                .hasTitle(VALIDATION_FAILED_TITLE);
    }

    @Test
    public void whenPostJobEndpointIsHit_andJobIsValid_thenCreatedIsReturned() {
        final EaccJob eaccJob = createBasicJobRestModel().get(0);
        when(validationService.validateJobAndThatJobNameDoesNotExist(any())).thenReturn(VALIDATION_SUCCESS);
        final ResponseEntity<EaccJob> response = objectUnderTest.postJobs(eaccJob);
        assertThat(response).hasStatus(HttpStatus.CREATED);
    }

    @Test
    public void whenPutJobEndpointIsHitWithValidData_thenReturnOkAndUpdatedJob() {
        final EaccJob eaccJob = createBasicJobRestModel().get(0);
        when(jobService.update(eaccJob)).thenReturn(eaccJob);
        when(validationService.validateJobNameAndJobExists(eaccJob.getJobName())).thenReturn(VALIDATION_SUCCESS);
        when(validationService.validateRulesetNameAndExists(eaccJob.getRulesetName())).thenReturn(VALIDATION_SUCCESS);
        when(validationService.validateScope(eaccJob.getScopeName())).thenReturn(VALIDATION_SUCCESS);
        when(executionService.isJobInUse(eaccJob.getJobName())).thenReturn(false);
        val response = objectUnderTest.putJob(eaccJob.getJobName(), eaccJob, APPLICATION_JSON, APPLICATION_JSON);
        assertThat(response)
                .hasStatus(HttpStatus.OK)
                .hasBody(eaccJob);
    }

    @Test
    public void whenPutJobEndpointIsHit_andJobNameIsInvalid_thenControllerDetailExceptionThrowWithBadRequestAndMessage() {
        final EaccJob eaccJob = createBasicJobRestModel().get(0);
        when(validationService.validateJobNameAndJobExists(any())).thenCallRealMethod();
        assertThatCDException(() -> objectUnderTest.putJob("", eaccJob, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(BAD_REQUEST)
                .hasMessage(BAD_REQUEST_FAILED_MSG)
                .hasDetail(
                        "Invalid resource identifier in url: Only lower case alphanumeric, underscores and dashes allowed, with between 4 -100 characters.")
                .hasTitle(VALIDATION_FAILED_TITLE);
    }

    @Test
    public void whenPutJobEndpointIsHit_andJobNameIsUppercase_thenControllerDetailExceptionThrowWithBadRequestAndMessage() {
        final EaccJob eaccJob = createBasicJobRestModel().get(0);
        when(validationService.validateJobNameAndJobExists(any())).thenCallRealMethod();
        assertThatCDException(() -> objectUnderTest.putJob("JOB1", eaccJob, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(BAD_REQUEST)
                .hasMessage(BAD_REQUEST_FAILED_MSG)
                .hasDetail(
                        "Invalid resource identifier in url: Only lower case alphanumeric, underscores and dashes allowed, with between 4 -100 characters.")
                .hasTitle(VALIDATION_FAILED_TITLE);
    }

    @Test
    public void whenPutJobEndpointIsHit_andRulesetIsInvalid_thenControllerDetailExceptionThrowWithBadRequestAndMessage() {
        final EaccJob eaccJob = createBasicJobRestModel().get(0);
        eaccJob.setRulesetName("UPPERCASE_NAME");
        when(validationService.validateJobNameAndJobExists(eaccJob.getJobName())).thenReturn(VALIDATION_SUCCESS);
        when(validationService.validateRulesetNameAndExists(eaccJob.getRulesetName())).thenCallRealMethod();
        when(validationService.validateRulesetName(eaccJob.getRulesetName())).thenCallRealMethod();
        assertThatCDException(() -> objectUnderTest.putJob(eaccJob.getJobName(), eaccJob, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(BAD_REQUEST)
                .hasMessage(BAD_REQUEST_FAILED_MSG)
                .hasDetail(INVALID_RULESET_NAME_MESSAGE)
                .hasTitle(VALIDATION_FAILED_TITLE);
    }

    @Test
    public void whenPutJobEndpointIsHit_andScopeIsInvalid_thenControllerDetailExceptionThrowWithBadRequestAndMessage() {
        final EaccJob eaccJob = createBasicJobRestModel().get(0);
        eaccJob.scopeName("");
        when(validationService.validateJobNameAndJobExists(eaccJob.getJobName())).thenReturn(VALIDATION_SUCCESS);
        when(validationService.validateRulesetNameAndExists(eaccJob.getRulesetName())).thenReturn(VALIDATION_SUCCESS);
        when(validationService.validateScope(eaccJob.getScopeName())).thenCallRealMethod();
        when(validationService.validateScopeName(any())).thenCallRealMethod();
        assertThatCDException(() -> objectUnderTest.putJob(eaccJob.getJobName(), eaccJob, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(BAD_REQUEST)
                .hasMessage(BAD_REQUEST_FAILED_MSG)
                .hasDetail("Invalid scopeName: Use only lower case alphanumeric, '-' and '_' characters up to a maximum of 255.")
                .hasTitle(VALIDATION_FAILED_TITLE);
    }

    @Test
    public void whenPutJobEndpointIsHit_andJobIsUsedInOngoingExecution_thenControllerDetailExceptionThrowWithConflictAndMessage() {
        final EaccJob eaccJob = createBasicJobRestModel().get(0);
        eaccJob.scopeName(EMPTY_STRING);
        when(validationService.validateJobNameAndJobExists(eaccJob.getJobName())).thenReturn(VALIDATION_SUCCESS);
        when(validationService.validateRulesetNameAndExists(eaccJob.getRulesetName())).thenReturn(VALIDATION_SUCCESS);
        when(validationService.validateScope(eaccJob.getScopeName())).thenReturn(VALIDATION_SUCCESS);
        when(executionService.isJobInUse(eaccJob.getJobName())).thenReturn(true);
        assertThatCDException(() -> objectUnderTest.putJob(eaccJob.getJobName(), eaccJob, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(CONFLICT)
                .hasMessage("%s \"Cannot update job as it is used by an ongoing execution.\"", CONFLICT)
                .hasDetail("Job in use.")
                .hasTitle("Cannot update job as it is used by an ongoing execution.");
    }

    @Test
    public void whenPutJobEndpointIsHit_andInternalErrorOccurs_thenControllerDetailExceptionThrowWithInternalErrorAndMessage() {
        final EaccJob eaccJob = createBasicJobRestModel().get(0);
        final DataAccessResourceFailureException cause = new DataAccessResourceFailureException("Some data access failure");
        when(validationService.validateJobNameAndJobExists(eaccJob.getJobName()))
                .thenThrow(cause);
        assertThatCDException(() -> objectUnderTest.putJob(eaccJob.getJobName(), eaccJob, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasMessage("%s \"Failed to update job.\"", INTERNAL_SERVER_ERROR)
                .hasSuppressedException(cause)
                .hasDetail(DATABASE_OPERATION_FAILED)
                .hasTitle("Failed to update job.");
    }

    private List<EaccJob> createBasicJobRestModel() {
        val config1 = new EaccJob("job1", SCOPE_ATHLONE, "0 */1 * * * *", TestDefaults.DEFAULT_RULESET_NAME);
        return List.of(config1);
    }
}