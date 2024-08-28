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

import static com.ericsson.oss.apps.util.Constants.BLANK_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.BLANK_RULESET_ERROR;
import static com.ericsson.oss.apps.util.Constants.EMPTY_STRING;
import static com.ericsson.oss.apps.util.Constants.EXECUTION_AND_RULESET_NAME_AND_JOB_ID_MAX_LENGTH;
import static com.ericsson.oss.apps.util.Constants.EXISTING_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.FDN_PATTERN;
import static com.ericsson.oss.apps.util.Constants.INVALID_FILTER_VALUE_MESSAGE;
import static com.ericsson.oss.apps.util.Constants.INVALID_JOBNAME_FOR_DELETE_OR_UPDATE;
import static com.ericsson.oss.apps.util.Constants.INVALID_PAGESIZE_MESSAGE;
import static com.ericsson.oss.apps.util.Constants.INVALID_PAGE_MESSAGE;
import static com.ericsson.oss.apps.util.Constants.NAME_MAX_LENGTH;
import static com.ericsson.oss.apps.util.Constants.NAME_PATTERN;
import static com.ericsson.oss.apps.util.Constants.NODE_FDN_MAX_LENGTH;
import static com.ericsson.oss.apps.util.Constants.NONEXISTENT_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.NONEXISTENT_RULESET_ERROR;
import static com.ericsson.oss.apps.util.Constants.NONEXISTENT_SCOPE_ERROR;
import static com.ericsson.oss.apps.util.Constants.REGEX_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.REGEX_RULESET_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.REGEX_SCOPE_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_COMPLETE;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_FAILED;
import static com.ericsson.oss.apps.util.StringUtil.isNullOrBlank;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ericsson.oss.apps.api.model.EaccJob;
import com.ericsson.oss.apps.executor.AuditStatus;
import com.ericsson.oss.apps.executor.ChangeStatus;
import com.ericsson.oss.apps.repository.FilterCriteria;
import com.ericsson.oss.apps.util.ValidationObject;

import lombok.extern.slf4j.Slf4j;

/**
 * A utility class to handle validation.
 */
@Slf4j
@Service
public class ValidationService {
    private static final boolean IS_VALIDATED = true;
    private static final ValidationObject VALIDATION_SUCCESS = new ValidationObject(IS_VALIDATED, EMPTY_STRING, VALIDATION_COMPLETE, HttpStatus.OK);

    private static final String ALLOWED_FILTER_NAME_AUDITSTATUS = "auditStatus";
    private static final String ALLOWED_FILTER_NAME_CHANGESTATUS = "changeStatus";
    private static final String ALLOWED_FILTER_NAME_FDN = "managedObjectFdn";
    private static final String[] ALLOWED_FILTER_NAMES = { ALLOWED_FILTER_NAME_AUDITSTATUS, ALLOWED_FILTER_NAME_FDN,
            ALLOWED_FILTER_NAME_CHANGESTATUS };
    private static final String INVALID_FILTER_NAME_MESSAGE = String.format("Invalid filter key: The allowed filters are: %s",
            Arrays.stream(ALLOWED_FILTER_NAMES).toList());
    private static final String INVALID_FILTER_ENUM_MESSAGE = "Invalid filter value: The allowed values for the filter %s are: %s";
    private static final String INVALID_FILTER_VALUE_FDN_TOO_LONG = "Invalid filter value: FDN value is too long at more than "
            + NODE_FDN_MAX_LENGTH + " characters";

    private static final String INVALID_FILTER_FORMAT = "Invalid filter format: Filter format is 'filter_attribute:filter_value'";

    private final RuleSetService ruleSetService;
    private final JobService jobService;
    private final ScopeService scopeService;

    @Autowired
    public ValidationService(final RuleSetService ruleSetService, final JobService jobService, final ScopeService scopeService) {
        this.ruleSetService = ruleSetService;
        this.jobService = jobService;
        this.scopeService = scopeService;
    }

    /**
     * A method for validating a Job.
     *
     * @param job
     *            the EaccJob to validate
     * @return ValidateObject
     * @see ValidationObject
     */
    public ValidationObject validateJobAndThatJobNameDoesNotExist(final EaccJob job) {
        final Optional<ValidationObject> jobInvalid = checkJobNameAndThatJobNameDoesNotExist(job);
        if (jobInvalid.isPresent()) {
            return jobInvalid.get();
        }
        if (!scopeService.existsByName(job.getScopeName())) {
            return new ValidationObject(!IS_VALIDATED, NONEXISTENT_SCOPE_ERROR, VALIDATION_FAILED, BAD_REQUEST);
        }
        return validateRulesetNameAndExists(job.getRulesetName());
    }

    private Optional<ValidationObject> checkJobNameAndThatJobNameDoesNotExist(final EaccJob job) {
        if (job == null) {
            return Optional.of(new ValidationObject(!IS_VALIDATED, "Job cannot be null", VALIDATION_FAILED, BAD_REQUEST));
        }
        final String jobName = job.getJobName();
        if (!StringUtils.hasText(jobName)) {
            return Optional.of(new ValidationObject(!IS_VALIDATED, BLANK_JOBNAME_ERROR, VALIDATION_FAILED, BAD_REQUEST));
        }
        if (isNotAllowed(jobName)) {
            return Optional.of(new ValidationObject(!IS_VALIDATED, REGEX_JOBNAME_ERROR, VALIDATION_FAILED, BAD_REQUEST));
        }
        if (jobService.existsByName(jobName)) {
            return Optional.of(new ValidationObject(!IS_VALIDATED, EXISTING_JOBNAME_ERROR, VALIDATION_FAILED, BAD_REQUEST));
        }
        return Optional.empty();
    }

    public ValidationObject validateRulesetName(final String rulesetName) {
        if (rulesetName.isEmpty()) {
            return new ValidationObject(!IS_VALIDATED, BLANK_RULESET_ERROR, VALIDATION_FAILED, BAD_REQUEST);
        }
        if (isNotAllowed(rulesetName)) {
            return new ValidationObject(!IS_VALIDATED, REGEX_RULESET_JOBNAME_ERROR, VALIDATION_FAILED, BAD_REQUEST);
        }

        return VALIDATION_SUCCESS;
    }

    /**
     * Validate a ruleset name and check that it exists.
     *
     * @param rulesetName
     *            the name of the ruleset
     * @return ValidateObject
     * @see ValidationObject
     */
    public ValidationObject validateRulesetNameAndExists(final String rulesetName) {
        final ValidationObject checkRulesetName = validateRulesetName(rulesetName);
        if (!checkRulesetName.getValidated()) {
            return checkRulesetName;
        }

        if (!ruleSetService.existsByName(rulesetName)) {
            return new ValidationObject(!IS_VALIDATED, NONEXISTENT_RULESET_ERROR, VALIDATION_FAILED, BAD_REQUEST);
        }

        return VALIDATION_SUCCESS;
    }

    /**
     * A method for validating the job name and that a job with that name exists.
     *
     * @param jobName
     *            the name of the job to delete
     * @return ValidateObject
     * @see ValidationObject
     */
    public ValidationObject validateJobNameAndJobExists(final String jobName) {
        if (isNotAllowed(jobName)) {
            return new ValidationObject(!IS_VALIDATED, INVALID_JOBNAME_FOR_DELETE_OR_UPDATE, VALIDATION_FAILED, BAD_REQUEST);
        } else if (!jobService.existsByName(jobName)) {
            return new ValidationObject(!IS_VALIDATED, NONEXISTENT_JOBNAME_ERROR, VALIDATION_FAILED, NOT_FOUND);
        }
        return VALIDATION_SUCCESS;
    }

    private boolean isNotAllowed(final String name) {
        return isNullOrBlank(name) || !isAllowedIgnoreEmpty(name);
    }

    private boolean isAllowedIgnoreEmpty(final String name) {
        return name == null || name.isEmpty()
                || (StringUtils.hasText(name)
                        && isShortEnoughAndMatchesPattern(name));
    }

    private boolean isShortEnoughAndMatchesPattern(final String name) {
        return name.length() <= EXECUTION_AND_RULESET_NAME_AND_JOB_ID_MAX_LENGTH
                && NAME_PATTERN.matcher(name).matches();
    }

    /**
     * Validate a scope name and check that it exists.
     *
     * @param name
     *            the name of the scope
     * @return ValidateObject
     * @see ValidationObject
     */
    public ValidationObject validateScope(final String name) {
        final ValidationObject checkValidName = validateScopeName(name);
        if (!checkValidName.getValidated()) {
            return checkValidName;
        }
        if (!scopeService.existsByName(name)) {
            return new ValidationObject(!IS_VALIDATED, NONEXISTENT_SCOPE_ERROR, VALIDATION_FAILED, BAD_REQUEST);
        }
        return VALIDATION_SUCCESS;
    }

    /**
     * A method for validating a ScopeName.
     *
     * @param name
     *            the ScopeName to validate
     * @return ValidateObject
     * @see ValidationObject
     */
    public ValidationObject validateScopeName(final String name) {
        if (isNullOrBlank(name)) {
            log.debug("Scope name is null or blank");
            return new ValidationObject(!IS_VALIDATED, REGEX_SCOPE_JOBNAME_ERROR, VALIDATION_FAILED, BAD_REQUEST);
        }
        if (name.length() > NAME_MAX_LENGTH) {
            log.debug("Scope name is too long");
            return new ValidationObject(!IS_VALIDATED, REGEX_SCOPE_JOBNAME_ERROR, VALIDATION_FAILED, BAD_REQUEST);
        }

        if (!NAME_PATTERN.matcher(name).matches()) {
            log.debug("Scope name does not match valid pattern");
            return new ValidationObject(!IS_VALIDATED, REGEX_SCOPE_JOBNAME_ERROR, VALIDATION_FAILED, BAD_REQUEST);
        }

        return VALIDATION_SUCCESS;
    }

    public ValidationObject validateExecutionIdFormat(final String executionId) {
        if (isNullOrBlank(executionId)) {
            return new ValidationObject(!IS_VALIDATED, "Execution Id should not be blank", VALIDATION_FAILED, BAD_REQUEST);
        }
        try {
            Long.parseLong(executionId);
        } catch (final NumberFormatException e) {
            return new ValidationObject(!IS_VALIDATED,
                    "Execution ID can only contain numeric characters.",
                    VALIDATION_FAILED, BAD_REQUEST);
        }
        return VALIDATION_SUCCESS;
    }

    public ValidationObject validatePagination(final Integer page, final Integer pageSize) {
        if (page != null && page < 0) {
            return new ValidationObject(!IS_VALIDATED, INVALID_PAGE_MESSAGE, VALIDATION_FAILED, BAD_REQUEST);
        }
        if (pageSize != null && pageSize < 1) {
            return new ValidationObject(!IS_VALIDATED, INVALID_PAGESIZE_MESSAGE, VALIDATION_FAILED, BAD_REQUEST);
        }

        return VALIDATION_SUCCESS;
    }

    public ValidationObject validateFilter(final String filter) {
        if (!isNullOrBlank(filter)) {
            final Optional<FilterCriteria> filterCriteria = FilterCriteria.build(filter);
            if (filterCriteria.isPresent()) {
                if (isAllowedFilterKey(filterCriteria.get().getKey())) {
                    return validateFilterValue(filterCriteria.orElse(null));
                } else {
                    return new ValidationObject(!IS_VALIDATED, INVALID_FILTER_NAME_MESSAGE, VALIDATION_FAILED, BAD_REQUEST);
                }
            } else {
                return new ValidationObject(!IS_VALIDATED, INVALID_FILTER_FORMAT, VALIDATION_FAILED, BAD_REQUEST);
            }
        }

        return VALIDATION_SUCCESS;
    }

    private boolean isAllowedFilterKey(final String name) {
        return Arrays.asList(ALLOWED_FILTER_NAMES).contains(name);
    }

    private ValidationObject validateFilterValue(final FilterCriteria filterCriteria) {
        List<String> values = filterCriteria.getValues();
        if (values == null) {
            values = new ArrayList<>();
            if (filterCriteria.getValue() != null) {
                values.add(filterCriteria.getValue().toString());
            }
        }
        for (final String value : values) {
            switch (filterCriteria.getKey()) {
                case ALLOWED_FILTER_NAME_AUDITSTATUS:
                    if (Arrays.stream(AuditStatus.values())
                            .noneMatch(auditStatus -> auditStatus.toString().equals(value))) {
                        return new ValidationObject(!IS_VALIDATED, String.format(INVALID_FILTER_ENUM_MESSAGE,
                                ALLOWED_FILTER_NAME_AUDITSTATUS, Arrays.stream(AuditStatus.values()).toList()), VALIDATION_FAILED, BAD_REQUEST);
                    }
                    break;
                case ALLOWED_FILTER_NAME_CHANGESTATUS:
                    if (Arrays.stream(ChangeStatus.values())
                            .noneMatch(changeStatus -> changeStatus.toString().equals(value))) {
                        return new ValidationObject(!IS_VALIDATED, String.format(INVALID_FILTER_ENUM_MESSAGE,
                                ALLOWED_FILTER_NAME_CHANGESTATUS, Arrays.stream(ChangeStatus.values()).toList()), VALIDATION_FAILED, BAD_REQUEST);
                    }
                    break;
                case ALLOWED_FILTER_NAME_FDN:
                    return validateFdn(value);
                default:
                    return VALIDATION_SUCCESS;
            }
        }
        return VALIDATION_SUCCESS;
    }

    private ValidationObject validateFdn(final String fdn) {
        if (isFdnTooLong(fdn)) {
            return new ValidationObject(!IS_VALIDATED, INVALID_FILTER_VALUE_FDN_TOO_LONG, VALIDATION_FAILED, BAD_REQUEST);
        }
        if (!(FDN_PATTERN.matcher(fdn).matches())) {
            return new ValidationObject(!IS_VALIDATED, INVALID_FILTER_VALUE_MESSAGE, VALIDATION_FAILED, BAD_REQUEST);
        }
        return VALIDATION_SUCCESS;
    }

    private boolean isFdnTooLong(final String fdn) {
        return (fdn.length() > NODE_FDN_MAX_LENGTH);
    }
}
