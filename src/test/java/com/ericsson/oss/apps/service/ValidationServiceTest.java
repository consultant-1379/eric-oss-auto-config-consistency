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

import static com.ericsson.oss.apps.util.Constants.EMPTY_STRING;
import static com.ericsson.oss.apps.util.Constants.EXISTING_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.INVALID_FILTER_VALUE_MESSAGE;
import static com.ericsson.oss.apps.util.Constants.INVALID_JOBNAME_FOR_DELETE_OR_UPDATE;
import static com.ericsson.oss.apps.util.Constants.INVALID_PAGESIZE_MESSAGE;
import static com.ericsson.oss.apps.util.Constants.INVALID_PAGE_MESSAGE;
import static com.ericsson.oss.apps.util.Constants.NONEXISTENT_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.NONEXISTENT_RULESET_ERROR;
import static com.ericsson.oss.apps.util.Constants.NONEXISTENT_SCOPE_ERROR;
import static com.ericsson.oss.apps.util.Constants.REGEX_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.REGEX_RULESET_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.REGEX_SCOPE_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_COMPLETE;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_FAILED;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_SCOPE_NAME;
import static com.ericsson.oss.apps.util.TestDefaults.INVALID_FDN_TOO_LONG;
import static com.ericsson.oss.apps.util.TestDefaults.INVALID_RULESET_NAME_MESSAGE;
import static com.ericsson.oss.apps.util.TestDefaults.RULESET_NAME_FOR_VALIDATION;
import static com.ericsson.oss.apps.util.TestDefaults.SCOPE_NAME_FOR_VALIDATION;
import static com.ericsson.oss.apps.util.TestDefaults.VALID_JOBNAME_FOR_VALIDATION;
import static com.ericsson.oss.apps.util.TestDefaults.VALID_SCHEDULE_FOR_VALIDATION;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.ericsson.assertions.validation.ValidationObjectSoftAssertions;
import com.ericsson.oss.apps.api.model.EaccJob;
import com.ericsson.oss.apps.util.ValidationObject;

/**
 * Unit tests for {@link ValidationService} class.
 */
@ExtendWith({ MockitoExtension.class, SoftAssertionsExtension.class })
class ValidationServiceTest {

    private static final String INVALID_NAME = "\"*(\\" + (char) 163 + "(*&\"";
    private static final String WHITESPACE = "  ";

    private static final String FORMATTED_INVALID_FILTER_VALUE_MESSAGE = INVALID_FILTER_VALUE_MESSAGE.replace("%", "%%");
    private static final String FILTER_SEPARATOR = ":";
    private static final String INVALID_FILTER_NAME_MESSAGE = //
            "Invalid filter key: The allowed filters are: [auditStatus, managedObjectFdn, changeStatus]";

    private static final String LOWER_CASE_100_CHAR_SNAKE_CASE_STRING = "a234567890_234567890-234567890b234567890c234567890c234567890d234567890e234567890f234567890g234567890";

    @InjectMocks
    private ValidationService objectUnderTest;

    @Mock
    private RuleSetService ruleSetService;

    @Mock
    private JobService jobService;

    @Mock
    private ScopeService scopeService;

    @InjectSoftAssertions
    private ValidationObjectSoftAssertions softly;

    private final EaccJob eaccJob = new EaccJob();
    private ValidationObject actualValidationObject;

    @BeforeEach
    public void setUp() {
        eaccJob.setJobName(VALID_JOBNAME_FOR_VALIDATION);
        eaccJob.setSchedule(VALID_SCHEDULE_FOR_VALIDATION);
        eaccJob.setRulesetName(RULESET_NAME_FOR_VALIDATION);
        eaccJob.setScopeName(DEFAULT_SCOPE_NAME);
    }

    @Test
    public void whenValidatingAJobAndItsUniqueness_andJobIsNull_thenReturnFalseValidationObject() {
        actualValidationObject = objectUnderTest.validateJobAndThatJobNameDoesNotExist(null);
        softly.assertThat(actualValidationObject).isNotValidated()
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasDetails("Job cannot be null");
    }

    @Test
    public void whenValidatingAJobAndItsUniqueness_andJobNameGivenAlreadyExists_thenReturnFalseValidationObject() {
        when(jobService.existsByName(anyString())).thenReturn(true);
        actualValidationObject = objectUnderTest.validateJobAndThatJobNameDoesNotExist(eaccJob);
        softly.assertThat(actualValidationObject).isNotValidated()
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasDetails(EXISTING_JOBNAME_ERROR);
    }

    @Test
    public void whenValidatingAJobAndItsUniqueness_andWhitespaceRulesetNameIsIncluded_thenReturnFalseValidationObject() {
        when(scopeService.existsByName(DEFAULT_SCOPE_NAME)).thenReturn(true);
        eaccJob.setJobName(VALID_JOBNAME_FOR_VALIDATION);
        eaccJob.setSchedule(VALID_SCHEDULE_FOR_VALIDATION);
        eaccJob.setRulesetName(WHITESPACE);
        actualValidationObject = objectUnderTest.validateJobAndThatJobNameDoesNotExist(eaccJob);
        softly.assertThat(actualValidationObject).isNotValidated()
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasDetails(REGEX_RULESET_JOBNAME_ERROR);
    }

    @Test
    public void whenValidatingAJobAndItsUniqueness_andRulesetDoesNotExist_thenReturnFalseValidationObject() {
        when(scopeService.existsByName(DEFAULT_SCOPE_NAME)).thenReturn(true);
        when(ruleSetService.existsByName(RULESET_NAME_FOR_VALIDATION)).thenReturn(false);
        eaccJob.setJobName(VALID_JOBNAME_FOR_VALIDATION);
        eaccJob.setSchedule(VALID_SCHEDULE_FOR_VALIDATION);
        eaccJob.setRulesetName(RULESET_NAME_FOR_VALIDATION);
        actualValidationObject = objectUnderTest.validateJobAndThatJobNameDoesNotExist(eaccJob);
        softly.assertThat(actualValidationObject).isNotValidated()
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasDetails(NONEXISTENT_RULESET_ERROR);
    }

    @Test
    public void whenValidatingAJobAndItsUniqueness_andJobIsValid_thenReturnTrueValidationObject() {
        when(scopeService.existsByName(DEFAULT_SCOPE_NAME)).thenReturn(true);
        when(jobService.existsByName(VALID_JOBNAME_FOR_VALIDATION)).thenReturn(false);
        when(ruleSetService.existsByName(RULESET_NAME_FOR_VALIDATION)).thenReturn(true);
        actualValidationObject = objectUnderTest.validateJobAndThatJobNameDoesNotExist(eaccJob);
        softly.assertThat(actualValidationObject).isValidated()
                .hasStatus(HttpStatus.OK)
                .hasDetails(EMPTY_STRING);
    }

    @Test
    public void whenValidatingAJobAndItsUniqueness_andJobNameIsInvalid_thenReturnFalseValidationObject() {
        eaccJob.setJobName(INVALID_NAME);
        actualValidationObject = objectUnderTest.validateJobAndThatJobNameDoesNotExist(eaccJob);
        softly.assertThat(actualValidationObject).isNotValidated()
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasDetails(REGEX_JOBNAME_ERROR);
    }

    @Test
    public void whenValidatingAJobAndItsUniqueness_andScopeDoesNotExist_thenReturnFalseValidationObjectAndNotFoundStatusCode() {
        actualValidationObject = objectUnderTest.validateJobAndThatJobNameDoesNotExist(eaccJob);
        softly.assertThat(actualValidationObject).hasStatus(HttpStatus.BAD_REQUEST)
                .hasDetails(NONEXISTENT_SCOPE_ERROR);
    }

    @Test
    public void whenValidatingAJobAndItsUniqueness_andScopeDoesExist_thenReturnTrueValidationObjectAndNotFoundStatusCode() {
        when(ruleSetService.existsByName(eaccJob.getRulesetName())).thenReturn(true);
        when(scopeService.existsByName(eaccJob.getScopeName())).thenReturn(true);
        actualValidationObject = objectUnderTest.validateJobAndThatJobNameDoesNotExist(eaccJob);
        softly.assertThat(actualValidationObject).hasStatus(HttpStatus.OK)
                .hasTitle(VALIDATION_COMPLETE);
    }

    @Test
    public void whenValidatingAJobForDeleteOrUpdate_andBlankJobNameGiven_thenReturnFalseValidationObject() {
        actualValidationObject = objectUnderTest.validateJobNameAndJobExists(EMPTY_STRING);
        softly.assertThat(actualValidationObject).isNotValidated()
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasDetails(INVALID_JOBNAME_FOR_DELETE_OR_UPDATE);
    }

    @Test
    public void whenValidatingAJobForDeleteOrUpdate_andInvalidJobNameGiven_thenReturnFalseValidationObject() {
        actualValidationObject = objectUnderTest.validateJobNameAndJobExists("£Test^");
        softly.assertThat(actualValidationObject).isNotValidated()
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasDetails(INVALID_JOBNAME_FOR_DELETE_OR_UPDATE);
    }

    @Test
    public void whenValidatingAJobForDeleteOrUpdate_andNullJobNameGiven_thenReturnFalseValidationObject() {
        actualValidationObject = objectUnderTest.validateJobNameAndJobExists(null);
        softly.assertThat(actualValidationObject).isNotValidated()
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasDetails(INVALID_JOBNAME_FOR_DELETE_OR_UPDATE);
    }

    @Test
    public void whenValidatingAJobForDeleteOrUpdate_andJobDoesNotExist_thenReturnFalseValidationObjectAndNotFoundStatusCode() {
        when(jobService.existsByName(anyString())).thenReturn(false);
        actualValidationObject = objectUnderTest.validateJobNameAndJobExists(VALID_JOBNAME_FOR_VALIDATION);
        softly.assertThat(actualValidationObject).isNotValidated()
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasTitle(VALIDATION_FAILED)
                .hasDetails(NONEXISTENT_JOBNAME_ERROR);
    }

    @Test
    public void whenValidatingAJobForDeleteOrUpdate_andJobDoesExist_thenReturnTrueValidationObjectAndOkStatusCode() {
        when(jobService.existsByName(anyString())).thenReturn(true);
        actualValidationObject = objectUnderTest.validateJobNameAndJobExists(VALID_JOBNAME_FOR_VALIDATION);
        softly.assertThat(actualValidationObject).isValidated()
                .hasStatus(HttpStatus.OK)
                .hasTitle(VALIDATION_COMPLETE)
                .hasDetails(EMPTY_STRING);
    }

    @Test
    public void whenValidatingScope_andItsValid_andExists_thenValidationSucceeds() {
        when(scopeService.existsByName(anyString())).thenReturn(true);
        actualValidationObject = objectUnderTest.validateScope(SCOPE_NAME_FOR_VALIDATION);
        softly.assertThat(actualValidationObject).isValidated()
                .hasStatus(HttpStatus.OK)
                .hasTitle(VALIDATION_COMPLETE)
                .hasDetails(EMPTY_STRING);
    }

    @Test
    public void whenValidatingScope_andItsValid_andDoesNotExists_thenValidationFails() {
        when(scopeService.existsByName(anyString())).thenReturn(false);
        actualValidationObject = objectUnderTest.validateScope(SCOPE_NAME_FOR_VALIDATION);
        softly.assertThat(actualValidationObject).isNotValidated()
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasTitle(VALIDATION_FAILED)
                .hasDetails(NONEXISTENT_SCOPE_ERROR);
    }

    @Test
    public void whenValidatingScope_andItsInvalid_thenValidationFails() {
        actualValidationObject = objectUnderTest.validateScope("name with invalid characters like space and &");
        softly.assertThat(actualValidationObject).isNotValidated()
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasTitle(VALIDATION_FAILED)
                .hasDetails(REGEX_SCOPE_JOBNAME_ERROR);
    }

    @Test
    public void whenValidatingRuleset_andItsValid_thenReturnTrue() {
        actualValidationObject = objectUnderTest.validateRulesetName(LOWER_CASE_100_CHAR_SNAKE_CASE_STRING);
        softly.assertThat(actualValidationObject)
                .isValidated()
                .hasDetails(EMPTY_STRING)
                .hasTitle(VALIDATION_COMPLETE)
                .hasStatus(HttpStatus.OK);
    }

    @Test
    public void whenValidatingRuleset_andNameIsTooLong_thenReturnFalseValidationObject() {
        actualValidationObject = objectUnderTest.validateRulesetName('a' + LOWER_CASE_100_CHAR_SNAKE_CASE_STRING);
        softly.assertThat(actualValidationObject)
                .isNotValidated()
                .hasDetails(INVALID_RULESET_NAME_MESSAGE)
                .hasTitle(VALIDATION_FAILED)
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void whenValidatingRuleset_andNameHasInvalidCharacters_thenReturnFalseValidationObject() {
        actualValidationObject = objectUnderTest.validateRulesetName("ARuleset");
        softly.assertThat(actualValidationObject)
                .isNotValidated()
                .hasDetails(INVALID_RULESET_NAME_MESSAGE)
                .hasTitle(VALIDATION_FAILED)
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void whenValidatingPagination_andPageIsNegative_thenReturnFalseValidationObject() {
        actualValidationObject = objectUnderTest.validatePagination(-100, null);
        softly.assertThat(actualValidationObject)
                .isNotValidated()
                .hasDetails(INVALID_PAGE_MESSAGE)
                .hasTitle(VALIDATION_FAILED)
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void whenValidatingPagination_andPageAndPageSizeAreNull_thenReturnTrueValidationObjectAndOkStatusCode() {
        actualValidationObject = objectUnderTest.validatePagination(null, null);
        softly.assertThat(actualValidationObject)
                .isValidated()
                .hasDetails(EMPTY_STRING)
                .hasTitle(VALIDATION_COMPLETE)
                .hasStatus(HttpStatus.OK);
    }

    @Test
    public void whenValidatingPagination_andPageIsZero_thenReturnTrueValidationObjectAndOkStatusCode() {
        actualValidationObject = objectUnderTest.validatePagination(0, null);
        softly.assertThat(actualValidationObject)
                .isValidated()
                .hasDetails(EMPTY_STRING)
                .hasTitle(VALIDATION_COMPLETE)
                .hasStatus(HttpStatus.OK);
    }

    @Test
    public void whenValidatingPagination_andPageIsOne_thenReturnTrueValidationObjectAndOkStatusCode() {
        actualValidationObject = objectUnderTest.validatePagination(1, null);
        softly.assertThat(actualValidationObject)
                .isValidated()
                .hasDetails(EMPTY_STRING)
                .hasTitle(VALIDATION_COMPLETE)
                .hasStatus(HttpStatus.OK);
    }

    @Test
    public void whenValidatingPagination_andPageSizeIsNegative_thenReturnFalseValidationObject() {
        actualValidationObject = objectUnderTest.validatePagination(null, -100);
        softly.assertThat(actualValidationObject)
                .isNotValidated()
                .hasDetails(INVALID_PAGESIZE_MESSAGE)
                .hasTitle(VALIDATION_FAILED)
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void whenValidatingPagination_andPageSizeIsZero_thenReturnFalseValidationObject() {
        actualValidationObject = objectUnderTest.validatePagination(null, 0);
        softly.assertThat(actualValidationObject)
                .isNotValidated()
                .hasDetails(INVALID_PAGESIZE_MESSAGE)
                .hasTitle(VALIDATION_FAILED)
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void whenValidatingPagination_andPageSizeIsOne_thenReturnTrueValidationObjectAndOkStatusCode() {
        actualValidationObject = objectUnderTest.validatePagination(null, 1);
        softly.assertThat(actualValidationObject)
                .isValidated()
                .hasDetails(EMPTY_STRING)
                .hasTitle(VALIDATION_COMPLETE)
                .hasStatus(HttpStatus.OK);
    }

    @Test
    public void whenValidatingFilter_andFilterKeyIsAllowed_thenReturnTrueValidationObjectAndOkStatusCode() {
        actualValidationObject = objectUnderTest.validateFilter("auditStatus:Inconsistent");
        softly.assertThat(actualValidationObject)
                .isValidated()
                .hasDetails(EMPTY_STRING)
                .hasTitle(VALIDATION_COMPLETE)
                .hasStatus(HttpStatus.OK);
    }

    @Test
    public void whenValidatingFilter_andFilterKeyIsNotAllowed_thenReturnFalseValidationObject() {
        final String invalidFilter = 'a' + LOWER_CASE_100_CHAR_SNAKE_CASE_STRING + FILTER_SEPARATOR + LOWER_CASE_100_CHAR_SNAKE_CASE_STRING;
        actualValidationObject = objectUnderTest.validateFilter(invalidFilter);
        softly.assertThat(actualValidationObject)
                .isNotValidated()
                .hasDetails(INVALID_FILTER_NAME_MESSAGE)
                .hasTitle(VALIDATION_FAILED)
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void whenValidatingFilter_andFilterKeyIsAllowedList_thenReturnTrueValidationObjectAndOkStatusCode() {
        actualValidationObject = objectUnderTest
                .validateFilter("changeStatus:(Implementation complete,Implementation in progress,Implementation failed)");
        softly.assertThat(actualValidationObject)
                .isValidated()
                .hasDetails(EMPTY_STRING)
                .hasTitle(VALIDATION_COMPLETE)
                .hasStatus(HttpStatus.OK);
    }

    @Test
    public void whenValidatingFilter_andFilterKeyFdnIsAllowed_thenReturnTrueValidationObjectAndOkStatusCode() {
        actualValidationObject = objectUnderTest.validateFilter("managedObjectFdn:%SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE416dg2ERBS00006,EUtranCellFDD=LTE06dg2ERBS00030-3%");
        softly.assertThat(actualValidationObject)
                .isValidated()
                .hasDetails(EMPTY_STRING)
                .hasTitle(VALIDATION_COMPLETE)
                .hasStatus(HttpStatus.OK);
    }

    @Test
    public void whenValidatingFilter_andFilterValueFdnIsNotAllowed_thenReturnFalseValidationObject() {
        actualValidationObject = objectUnderTest.validateFilter("managedObjectFdn:%SubNetwork=Europe,%SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE416dg2ERBS00006,EUtranCellFDD=LTE06dg2ERBS00030-$%");
        softly.assertThat(actualValidationObject)
                .isNotValidated()
                .hasDetails(FORMATTED_INVALID_FILTER_VALUE_MESSAGE)
                .hasTitle(VALIDATION_FAILED)
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void whenValidatingFilter_andFilterValueFdnIsTooLong_thenReturnFalseValidationObject() {
        actualValidationObject = objectUnderTest.validateFilter("managedObjectFdn:" + INVALID_FDN_TOO_LONG);
        softly.assertThat(actualValidationObject)
                .isNotValidated()
                .hasDetails("Invalid filter value: FDN value is too long at more than 1600 characters")
                .hasTitle(VALIDATION_FAILED)
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void whenValidatingFilter_andAuditStatusValueIsNotAllowed_thenReturnFalseValidationObject() {
        actualValidationObject = objectUnderTest.validateFilter("auditStatus:incorrect");
        softly.assertThat(actualValidationObject)
                .isNotValidated()
                .hasDetails("Invalid filter value: The allowed values for the filter auditStatus are: [Consistent, Inconsistent]")
                .hasTitle(VALIDATION_FAILED)
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void whenValidatingFilter_andChangeStatusValueIsNotAllowed_thenReturnFalseValidationObject() {
        actualValidationObject = objectUnderTest.validateFilter("changeStatus:incorrect");
        softly.assertThat(actualValidationObject)
                .isNotValidated()
                .hasDetails(
                        "Invalid filter value: The allowed values for the filter changeStatus are: [Not applied, Implementation in progress, Implementation complete, Implementation failed, Implementation aborted, Reversion in progress, Reversion complete, Reversion failed, Reversion aborted]")
                .hasTitle(VALIDATION_FAILED)
                .hasStatus(HttpStatus.BAD_REQUEST);
    }
}