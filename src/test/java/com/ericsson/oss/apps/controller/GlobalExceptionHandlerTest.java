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
import static com.ericsson.oss.apps.util.Constants.INVALID_CHANGE_OPERATION;
import static com.ericsson.oss.apps.util.Constants.UNEXPECTED_ERROR;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_FAILED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.ericsson.assertions.response.ResponseEntitySoftAssertions;
import com.ericsson.oss.apps.api.model.EaccErrorDetails;
import com.ericsson.oss.apps.api.model.EaccOperation;
import com.ericsson.oss.apps.api.model.EaccRuleErrorDetailsRuleValidationErrorsInner;
import com.ericsson.oss.apps.validation.RuleValidationError;
import com.ericsson.oss.apps.validation.RuleValidationException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;

/**
 * Unit Tests for {@link GlobalExceptionHandler} class.
 */
@ExtendWith(SoftAssertionsExtension.class)
@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerTest {

    private static final String EXCEEDS_THE_CONFIGURED_MAXIMUM = "the request was rejected because its size (21577375) exceeds the " +
            "configured maximum (20971520)";
    private static final String CDE_TITLE = "Failed to create scope.";
    private static final String CDRE_TITLE = "Problems found in ruleset.";
    private static final String CDRE_DETAIL_MSG = "Ruleset cannot contain any invalid MO types, attributes or values.";
    private static final String INVALID_MO_ERROR = "Invalid MO.";
    private static final String INVALID_ATTRIBUTE_NAME_ERROR = "Invalid Attribute name.";
    private static final String INVALID_ATTRIBUTE_VALUE_ERROR = "Invalid Attribute value.";
    private static final String INVALID_MO_DETAILS = "MO ENodBFunction not found in Managed Object Model.";
    private static final String INVALID_ATTRIBUTE_NAME_DETAILS = "Attribute forcedSiTunnelingActiv not found in Managed Object Model.";
    private static final String INVALID_ATTRIBUTE_VALUE_DETAILS = "Attribute value 10 is invalid according to the Managed Object Model.";
    private static final String INVALID_PAYLOAD_MSG = "Failed to read request";
    private static final String UNSUPPORTED_MEDIA_TYPE_MSG = "Content-Type 'application/json' is not supported.";
    private GlobalExceptionHandler objectUnderTest;

    @InjectSoftAssertions
    private ResponseEntitySoftAssertions softly;

    @BeforeEach
    void setUp() {
        objectUnderTest = new GlobalExceptionHandler();
    }

    @Test
    void whenMaxUploadSizeExceededExceptionIsHandled_thenCorrectResponseIsReturned() {
        final var illegalStateException = new IllegalStateException();
        final var sizeLimitExceededException = new SizeLimitExceededException(EXCEEDS_THE_CONFIGURED_MAXIMUM, 2L, 1L);
        illegalStateException.initCause(sizeLimitExceededException);
        final var maxUploadSizeExceededException = new MaxUploadSizeExceededException(1L, illegalStateException);
        final var responseEntity = objectUnderTest.customMaxSizeErrorHandling(maxUploadSizeExceededException);
        assertThat(responseEntity).hasStatus(HttpStatus.PAYLOAD_TOO_LARGE)
                .hasBody();
        final var errorDetails = responseEntity.getBody();
        softly.assertThat(errorDetails.getTitle()).isEqualTo("Maximum upload size exceeded.");
        softly.assertThat(errorDetails.getDetail()).isEqualTo("File is too large: " + EXCEEDS_THE_CONFIGURED_MAXIMUM);
        softly.assertThat(errorDetails.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
    }

    @Test
    void whenControllerDetailExceptionIsHandled_thenCorrectResponseIsReturned() {
        final ControllerDetailException exception = new ControllerDetailException(HttpStatus.BAD_REQUEST,
                CDE_TITLE);
        final ResponseEntity<EaccErrorDetails> responseEntity = objectUnderTest.customErrorHandling(exception);
        assertThat(responseEntity).hasStatus(HttpStatus.BAD_REQUEST)
                .hasBody();
        final var errorDetails = responseEntity.getBody();
        softly.assertThat(errorDetails.getTitle()).isEqualTo(CDE_TITLE);
        softly.assertThat(errorDetails.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        softly.assertThat(errorDetails.getDetail()).isNull();
    }

    @Test
    void whenControllerDetailRuleExceptionIsHandled_thenCorrectResponseIsReturned() {
        final var ruleValidationException = new RuleValidationException("Errors exist in rules", createRuleValidationErrors());

        final ControllerDetailRuleException controllerDetailRuleException = new ControllerDetailRuleException(HttpStatus.BAD_REQUEST,
                CDRE_DETAIL_MSG, CDRE_TITLE, ruleValidationException.getRuleValidationErrors());

        final var responseEntity = objectUnderTest.customErrorHandling(controllerDetailRuleException);
        assertThat(responseEntity).hasStatus(HttpStatus.BAD_REQUEST)
                .hasBody();
        final var body = responseEntity.getBody();
        softly.assertThat(body.getTitle()).isEqualTo(CDRE_TITLE);
        softly.assertThat(body.getDetail()).isEqualTo(CDRE_DETAIL_MSG);
        softly.assertThat(body.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());

        final List<EaccRuleErrorDetailsRuleValidationErrorsInner> eaccRuleValidationErrors = body.getRuleValidationErrors();

        softly.assertThat(eaccRuleValidationErrors).hasSize(3);
        softly.assertThat(eaccRuleValidationErrors).extracting("lineNumber").containsExactly(1L, 2L, 3L);
        softly.assertThat(eaccRuleValidationErrors).extracting("errorType").containsExactly(
                INVALID_MO_ERROR, INVALID_ATTRIBUTE_NAME_ERROR, INVALID_ATTRIBUTE_VALUE_ERROR);
        softly.assertThat(eaccRuleValidationErrors).extracting("errorDetails").containsExactly(
                INVALID_MO_DETAILS,
                INVALID_ATTRIBUTE_NAME_DETAILS,
                INVALID_ATTRIBUTE_VALUE_DETAILS);
        softly.assertThat(eaccRuleValidationErrors).extracting("additionalInfo").containsExactly("", "", "");
    }

    @Test
    void whenControllerDetailRuleExceptionIsHandled_AndRuleListIsEmpty_thenCorrectResponseIsReturned() {
        final var ruleValidationException = new RuleValidationException("Errors exist in rules", Collections.emptyList());

        final ControllerDetailRuleException controllerDetailRuleException = new ControllerDetailRuleException(HttpStatus.BAD_REQUEST,
                CDRE_DETAIL_MSG, CDRE_TITLE, ruleValidationException.getRuleValidationErrors());

        final var responseEntity = objectUnderTest.customErrorHandling(controllerDetailRuleException);
        assertThat(responseEntity).hasStatus(HttpStatus.BAD_REQUEST)
                .hasBody();
        final var body = responseEntity.getBody();
        softly.assertThat(body.getTitle()).isEqualTo(CDRE_TITLE);
        softly.assertThat(body.getDetail()).isEqualTo(CDRE_DETAIL_MSG);
        softly.assertThat(body.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());

        final List<EaccRuleErrorDetailsRuleValidationErrorsInner> eaccRuleValidationErrors = body.getRuleValidationErrors();

        softly.assertThat(eaccRuleValidationErrors).isEmpty();
    }

    @Test
    void whenControllerDetailExceptionIsHandled_AndValueInstantiationExceptionForEaccOperationIsTheCause_thenCorrectResponseIsReturned() {
        final var javaType = mock(JavaType.class);
        when(javaType.hasRawClass(EaccOperation.class)).thenReturn(true);
        final var valueInstantiationException = mock(ValueInstantiationException.class);
        when(valueInstantiationException.getType()).thenReturn(javaType);
        final var httpMessageNotReadable = mock(HttpMessageNotReadableException.class);
        when(httpMessageNotReadable.getCause()).thenReturn(valueInstantiationException);

        final var response = objectUnderTest.handleHttpMessageNotReadable(httpMessageNotReadable, mock(), HttpStatus.BAD_REQUEST, mock());
        assertThat(response)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasBody();

        assertThat(response.getBody())
                .isInstanceOfSatisfying(EaccErrorDetails.class, eaccErrorDetails -> assertThat(eaccErrorDetails)
                        .extracting(EaccErrorDetails::getStatus, EaccErrorDetails::getTitle, EaccErrorDetails::getDetail)
                        .contains(HttpStatus.BAD_REQUEST.value(), VALIDATION_FAILED, INVALID_CHANGE_OPERATION));
    }

    @Test
    void whenCreateResponseEntityForInvalidPaylodFormat_thenResponseContainsErrorDetails() {
        final var errorBody = new EaccErrorDetails(VALIDATION_FAILED, HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                INVALID_PAYLOAD_MSG);
        final ProblemDetail problemDetail = new ProblemDetail() {
        };
        problemDetail.setDetail(INVALID_PAYLOAD_MSG);

        assertThat(objectUnderTest.createResponseEntity(problemDetail, null, HttpStatus.UNSUPPORTED_MEDIA_TYPE, null))
                .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .hasBody(errorBody);
    }

    @Test
    void whenCreateResponseEntityForUnsupportedMediaType_thenResponseContainsErrorDetails() {
        final var errorBody = new EaccErrorDetails(VALIDATION_FAILED, HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                UNSUPPORTED_MEDIA_TYPE_MSG);
        final ProblemDetail problemDetail = new ProblemDetail() {
        };
        problemDetail.setDetail(UNSUPPORTED_MEDIA_TYPE_MSG);

        assertThat(objectUnderTest.createResponseEntity(problemDetail, null, HttpStatus.UNSUPPORTED_MEDIA_TYPE, null))
                .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .hasBody(errorBody);
    }

    @Test
    void whenCreateResponseEntityForBadRequestWithUnexpectedBody_thenResponseContainsUnexpectedError() {
        final var errorBody = new EaccErrorDetails(VALIDATION_FAILED, HttpStatus.BAD_REQUEST.value(), UNEXPECTED_ERROR);
        assertThat(objectUnderTest.createResponseEntity(null, null, HttpStatus.BAD_REQUEST, null))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasBody(errorBody);
    }

    private static List<RuleValidationError> createRuleValidationErrors() {
        return List.of(new RuleValidationError(1L, INVALID_MO_ERROR,
                INVALID_MO_DETAILS, ""),
                new RuleValidationError(2L, INVALID_ATTRIBUTE_NAME_ERROR,
                        INVALID_ATTRIBUTE_NAME_DETAILS, ""),
                new RuleValidationError(3L, INVALID_ATTRIBUTE_VALUE_ERROR,
                        INVALID_ATTRIBUTE_VALUE_DETAILS, ""));
    }
}
