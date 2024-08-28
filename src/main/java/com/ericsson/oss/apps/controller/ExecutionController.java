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

import static com.ericsson.oss.apps.util.Constants.DATABASE_OPERATION_FAILED;
import static com.ericsson.oss.apps.util.Constants.EXECUTION_ID_DOES_NOT_EXIST;
import static com.ericsson.oss.apps.util.Constants.INVALID_CHANGE_OPERATION;
import static com.ericsson.oss.apps.util.Constants.PROVIDE_EXECUTION_ID;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_FAILED;
import static com.ericsson.oss.apps.util.StringUtil.isNullOrBlank;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.oss.apps.api.ExecutionsApi;
import com.ericsson.oss.apps.api.model.EaccApprovedAuditResults;
import com.ericsson.oss.apps.api.model.EaccExecution;
import com.ericsson.oss.apps.api.model.EaccOperation;
import com.ericsson.oss.apps.api.model.EaccPaginatedAuditResults;
import com.ericsson.oss.apps.service.ExecutionService;
import com.ericsson.oss.apps.service.ValidationService;
import com.ericsson.oss.apps.service.exception.ChangesInProgressException;
import com.ericsson.oss.apps.service.exception.InconsistencyProcessingFailedException;
import com.ericsson.oss.apps.service.exception.ProposedIdsNotFoundException;
import com.ericsson.oss.apps.service.exception.UnsupportedOperationException;
import com.ericsson.oss.apps.util.ValidationObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class ExecutionController implements ExecutionsApi {

    private static final String DB_ERROR = "Error retrieving executions from the database.";
    private static final String FAILED_TO_GET_EXECUTIONS = "Failed to get execution(s).";
    private final ExecutionService executionService;
    private final ValidationService validationService;

    @Autowired
    public ExecutionController(final ExecutionService executionService, final ValidationService validationService) {
        this.executionService = executionService;
        this.validationService = validationService;
    }

    @Override
    public ResponseEntity<List<EaccExecution>> getExecutions(final String accept, final String jobName) {
        log.info("GET /executions REST interface is invoked.");

        try {
            if (isNullOrBlank(jobName)) {
                return new ResponseEntity<>(executionService.getAllExecutions(), HttpStatus.OK);
            }

            final ValidationObject validationObject = validationService.validateJobNameAndJobExists(jobName);
            if (!validationObject.getValidated()) {
                log.error(validationObject.getDetails());
                throw new ControllerDetailException(validationObject.getHttpStatus(), validationObject.getDetails(), validationObject.getTitle());
            }
            log.debug("Format of Job Name validated.");

            return new ResponseEntity<>(executionService.getExecutionByJobName(jobName), HttpStatus.OK);
        } catch (final ControllerDetailException e) {
            throw e;
        } catch (final DataAccessException | TransactionException e) {
            log.error(FAILED_TO_GET_EXECUTIONS, e);
            final var exception = ControllerDetailException.builder()
                    .withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                    .withTitle(FAILED_TO_GET_EXECUTIONS)
                    .withDetail(DATABASE_OPERATION_FAILED)
                    .build();
            exception.addSuppressed(e);
            throw exception; //NOPMD
        } catch (final Exception e) {
            log.error(DB_ERROR, e);
            final var exception = ControllerDetailException.builder()
                    .withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                    .withTitle(FAILED_TO_GET_EXECUTIONS)
                    .build();
            exception.addSuppressed(e);
            throw exception; //NOPMD
        }

    }

    @Override
    public ResponseEntity<EaccPaginatedAuditResults> getAuditResults(final String executionId, final String accept, final Integer page,
            final Integer pageSize, final String filter) {
        log.info("GET /{id}/audit-results REST interface is invoked.");
        try {
            validateExecId(executionId);
            validatePagination(page, pageSize);
            final List<String> filtersOrEmptyList = parseFilterKeyValuePairsFromString(filter);
            filtersOrEmptyList.forEach(this::validateFilter);
            return new ResponseEntity<>(
                    executionService.getAuditResults(executionId, page, pageSize, filtersOrEmptyList), HttpStatus.OK);
        } catch (final ControllerDetailException e) {
            throw e;
        } catch (final DataAccessException | TransactionException e) {
            log.error("Failed to get audit results.", e);
            final var exception = ControllerDetailException.builder()
                    .withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                    .withTitle("Failed to get audit results.")
                    .withDetail(DATABASE_OPERATION_FAILED)
                    .build();
            exception.addSuppressed(e);
            throw exception; //NOPMD
        } catch (final Exception e) {
            log.error("Error retrieving audit results from the db.", e);
            final ControllerDetailException exception = new ControllerDetailException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to get audit result(s).");
            exception.addSuppressed(e);
            throw exception; //NOPMD
        }
    }

    @Override
    public ResponseEntity<Void> applyChange(final String executionId, final EaccApprovedAuditResults eaccApprovedAuditResults,
            final String accept, final String contentType) {
        log.info("POST /executions/{id}/audit-results REST interface is invoked.");
        try {
            validateExecId(executionId);
            if (EaccOperation.APPLY_CHANGE.equals(eaccApprovedAuditResults.getOperation())) {
                executionService.processApprovedChanges(executionId, eaccApprovedAuditResults);
            } else if (EaccOperation.REVERT_CHANGE.equals(eaccApprovedAuditResults.getOperation())) {
                executionService.revertChanges(executionId, eaccApprovedAuditResults);
            } else {
                throw ControllerDetailException.builder()
                        .withStatus(HttpStatus.BAD_REQUEST)
                        .withTitle(VALIDATION_FAILED)
                        .withDetail(INVALID_CHANGE_OPERATION)
                        .build();
            }
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        } catch (final DataAccessException | TransactionException e) {
            log.error("Failed to apply change.", e);
            final var exception = ControllerDetailException.builder()
                    .withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                    .withTitle("Failed to apply change.")
                    .withDetail(DATABASE_OPERATION_FAILED)
                    .build();
            exception.addSuppressed(e);
            throw exception; //NOPMD
        } catch (final InconsistencyProcessingFailedException e) {
            final ControllerDetailException exception = new ControllerDetailException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            exception.addSuppressed(e);
            throw exception; //NOPMD
        } catch (final ChangesInProgressException | ProposedIdsNotFoundException | UnsupportedOperationException e) {
            final var exception = ControllerDetailException.builder()
                    .withStatus(HttpStatus.BAD_REQUEST)
                    .withTitle(VALIDATION_FAILED)
                    .withDetail(e.getMessage())
                    .build();
            exception.addSuppressed(e);
            throw exception; //NOPMD
        }
    }

    private void validateExecId(final String executionId) {
        final ValidationObject validationObject = validationService.validateExecutionIdFormat(executionId);
        if (!validationObject.getValidated()) {
            log.error(validationObject.getDetails());
            throw new ControllerDetailException(validationObject.getHttpStatus(), validationObject.getDetails(), validationObject.getTitle());
        }
        log.debug("Format of ExecutionId validated.");

        if (!executionService.existsByExecutionId(executionId)) {
            throw new ControllerDetailException(HttpStatus.NOT_FOUND, PROVIDE_EXECUTION_ID, EXECUTION_ID_DOES_NOT_EXIST);
        }
    }

    private void validatePagination(final Integer page, final Integer pageSize) {
        final ValidationObject validationObject = validationService.validatePagination(page, pageSize);
        if (!validationObject.getValidated()) {
            log.error(validationObject.getDetails());
            throw new ControllerDetailException(validationObject.getHttpStatus(), validationObject.getDetails(), validationObject.getTitle());
        }
        log.debug("Format of Pagination parameters validated.");
    }

    private void validateFilter(final String filter) {
        final ValidationObject validationObject = validationService.validateFilter(filter);
        if (!validationObject.getValidated()) {
            log.error(validationObject.getDetails());
            throw new ControllerDetailException(validationObject.getHttpStatus(), validationObject.getDetails(), validationObject.getTitle());
        }
        log.debug("Format of filter parameter validated.");
    }

    private List<String> parseFilterKeyValuePairsFromString(final String filter) {
        return filter == null ? Collections.emptyList() : Arrays.asList(filter.split("\\$"));
    }
}
