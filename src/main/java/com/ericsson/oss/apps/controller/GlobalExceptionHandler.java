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

import static com.ericsson.oss.apps.util.Constants.INVALID_CHANGE_OPERATION;
import static com.ericsson.oss.apps.util.Constants.SERVER_ERROR;
import static com.ericsson.oss.apps.util.Constants.UNEXPECTED_ERROR;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_FAILED;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.ericsson.oss.apps.api.model.EaccErrorDetails;
import com.ericsson.oss.apps.api.model.EaccOperation;
import com.ericsson.oss.apps.api.model.EaccRuleErrorDetails;
import com.ericsson.oss.apps.api.model.EaccRuleErrorDetailsRuleValidationErrorsInner;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
@AllArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ControllerDetailException.class)
    public ResponseEntity<EaccErrorDetails> customErrorHandling(@NonNull final ControllerDetailException ex) {
        final EaccErrorDetails eaccErrorDetails = new EaccErrorDetails(ex.getReason(),
                ex.getStatusCode().value(), ex.getDetail());
        return new ResponseEntity<>(eaccErrorDetails, Objects.requireNonNull(HttpStatus.resolve(ex.getStatusCode().value())));
    }

    @ExceptionHandler(ControllerDetailRuleException.class)
    public ResponseEntity<EaccRuleErrorDetails> customErrorHandling(@NonNull final ControllerDetailRuleException ex) {
        final EaccRuleErrorDetails eaccRuleValidationError = new EaccRuleErrorDetails(ex.getReason(),
                ex.getStatusCode().value(), ex.getDetail());

        eaccRuleValidationError.setRuleValidationErrors(new ArrayList<>());
        ex.getRuleValidationErrors().forEach(ruleValidationError -> {
            final EaccRuleErrorDetailsRuleValidationErrorsInner ruleValidationErrors = new EaccRuleErrorDetailsRuleValidationErrorsInner(
                    ruleValidationError.lineNumber(),
                    ruleValidationError.errorType(),
                    ruleValidationError.errorDetails(),
                    ruleValidationError.additionalInfo());
            eaccRuleValidationError.addRuleValidationErrorsItem(ruleValidationErrors);
        });
        return new ResponseEntity<>(eaccRuleValidationError, Objects.requireNonNull(HttpStatus.resolve(ex.getStatusCode().value())));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<EaccErrorDetails> customMaxSizeErrorHandling(@NonNull final MaxUploadSizeExceededException ex) {
        final EaccErrorDetails error = new EaccErrorDetails("Maximum upload size exceeded.",
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "File is too large: " + ex.getCause().getCause().getMessage());
        return new ResponseEntity<>(error, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<EaccErrorDetails> handleJobValidation(@NonNull final ConstraintViolationException ex) {
        final EaccErrorDetails error = new EaccErrorDetails(VALIDATION_FAILED,
                HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        final Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        if (violations.size() > 1) {
            error.detail(ex.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", ", "[", "]")));
        } else if (violations.size() == 1) {
            error.detail(violations.iterator().next().getMessage());
        }
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(final HttpMessageNotReadableException ex, @NonNull final HttpHeaders headers,
            @NonNull final HttpStatusCode status, @NonNull final WebRequest request) {

        if (ex.getCause() instanceof ValueInstantiationException) {
            final ValueInstantiationException valueInstantiationException = (ValueInstantiationException) ex.getCause();
            if (valueInstantiationException.getType().hasRawClass(EaccOperation.class)) {
                final EaccErrorDetails error = new EaccErrorDetails(VALIDATION_FAILED,
                        HttpStatus.BAD_REQUEST.value(), INVALID_CHANGE_OPERATION);
                return new ResponseEntity<>(error, status);
            }
        }
        return super.handleHttpMessageNotReadable(ex, headers, status, request);
    }

    @Override
    @NonNull
    protected ResponseEntity<Object> createResponseEntity(@Nullable final Object body, @NonNull final HttpHeaders headers,
            @NonNull final HttpStatusCode statusCode, @NonNull final WebRequest request) {
        String errorTitle = SERVER_ERROR;
        String errorDetails = UNEXPECTED_ERROR;
        if (statusCode.is4xxClientError()) {
            errorTitle = VALIDATION_FAILED;
            if (body instanceof ProblemDetail) {
                errorDetails = ((ProblemDetail) body).getDetail();
            }
        }
        final var error = new EaccErrorDetails(errorTitle, statusCode.value(), errorDetails);
        return new ResponseEntity<>(error, statusCode);
    }
}
