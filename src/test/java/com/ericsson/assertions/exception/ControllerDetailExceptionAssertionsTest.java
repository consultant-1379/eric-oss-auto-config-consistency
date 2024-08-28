/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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
package com.ericsson.assertions.exception;

import static com.ericsson.assertions.exception.ControllerDetailExceptionAssert.assertThat;
import static com.ericsson.assertions.exception.ControllerDetailExceptionAssertions.assertThatCDException;
import static com.ericsson.oss.apps.util.Constants.EMPTY_STRING;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.I_AM_A_TEAPOT;
import static org.springframework.http.HttpStatus.OK;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.oss.apps.controller.ControllerDetailException;

/**
 * Unit test class for {@link ControllerDetailExceptionAssertions} and {@link ControllerDetailExceptionSoftAssertions} classes.
 */
@ExtendWith({ SoftAssertionsExtension.class, MockitoExtension.class })
class ControllerDetailExceptionAssertionsTest {

    private static final String BAD_GATEWAY_DETAIL = "bg detail";
    private static final String BAD_GATEWAY_REASON = "bg reason";
    private static final String TEAPOT_DETAIL = "t detail";
    private static final String TEAPOT_REASON = "t reason";
    private static final String EXPECTING_ACTUAL_NOT_TO_BE_NULL = "Expecting actual not to be null";
    private static final ControllerDetailException NULL_EXCEPTION = null;
    private static final ControllerDetailException BAD_GATEWAY_EXCEPTION = new ControllerDetailException(BAD_GATEWAY, BAD_GATEWAY_DETAIL,
            BAD_GATEWAY_REASON);
    private static final ControllerDetailException TEAPOT_EXCEPTION = new ControllerDetailException(I_AM_A_TEAPOT, TEAPOT_DETAIL, TEAPOT_REASON);
    private static final ControllerDetailException NO_FIELDS_EXCEPTION = new ControllerDetailException(OK, null, null);

    @InjectSoftAssertions
    private ControllerDetailExceptionSoftAssertions softly;

    @Test
    void testAllAssertionsCanPass() {
        assertThat(BAD_GATEWAY_EXCEPTION)
                .hasDetail(BAD_GATEWAY_DETAIL)
                .hasReason(BAD_GATEWAY_REASON)
                .hasTitle(BAD_GATEWAY_REASON)
                .hasStatus(BAD_GATEWAY);
        assertThatCDException(() -> {
            throw BAD_GATEWAY_EXCEPTION;
        })
                .hasDetail(BAD_GATEWAY_DETAIL)
                .hasReason(BAD_GATEWAY_REASON)
                .hasTitle(BAD_GATEWAY_REASON)
                .hasStatus(BAD_GATEWAY);
        assertThat(TEAPOT_EXCEPTION)
                .hasDetail(TEAPOT_DETAIL)
                .hasReason(TEAPOT_REASON)
                .hasTitle(TEAPOT_REASON)
                .hasStatus(I_AM_A_TEAPOT);
        assertThat(NO_FIELDS_EXCEPTION)
                .hasNoDetail()
                .hasNoReason()
                .hasNoTitle()
                .hasStatus(OK);
        softly.assertThat(BAD_GATEWAY_EXCEPTION)
                .hasDetail(BAD_GATEWAY_DETAIL)
                .hasReason(BAD_GATEWAY_REASON)
                .hasTitle(BAD_GATEWAY_REASON)
                .hasStatus(BAD_GATEWAY);
        softly.assertThatCDException(() -> {
            throw BAD_GATEWAY_EXCEPTION;
        })
                .hasDetail(BAD_GATEWAY_DETAIL)
                .hasReason(BAD_GATEWAY_REASON)
                .hasTitle(BAD_GATEWAY_REASON)
                .hasStatus(BAD_GATEWAY);
        softly.assertThat(TEAPOT_EXCEPTION)
                .hasDetail(TEAPOT_DETAIL)
                .hasReason(TEAPOT_REASON)
                .hasTitle(TEAPOT_REASON)
                .hasStatus(I_AM_A_TEAPOT);
        softly.assertThat(NO_FIELDS_EXCEPTION)
                .hasNoDetail()
                .hasNoReason()
                .hasNoTitle()
                .hasStatus(OK);
    }

    @Test
    void testHasStatusCanFail() {
        assertThatCode(() -> ControllerDetailExceptionAssertions.assertThat(BAD_GATEWAY_EXCEPTION).hasStatus(I_AM_A_TEAPOT))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void testHasDetailCanFail() {
        assertThatCode(() -> ControllerDetailExceptionAssertions.assertThat(BAD_GATEWAY_EXCEPTION).hasDetail(EMPTY_STRING))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void testHasNoDetailCanFail() {
        assertThatCode(() -> ControllerDetailExceptionAssertions.assertThat(BAD_GATEWAY_EXCEPTION).hasNoDetail())
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void testHasReasonCanFail() {
        assertThatCode(() -> ControllerDetailExceptionAssertions.assertThat(BAD_GATEWAY_EXCEPTION).hasReason(EMPTY_STRING))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void testHasNoReasonCanFail() {
        assertThatCode(() -> ControllerDetailExceptionAssertions.assertThat(BAD_GATEWAY_EXCEPTION).hasNoReason())
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void testHasTitleCanFail() {
        assertThatCode(() -> ControllerDetailExceptionAssertions.assertThat(BAD_GATEWAY_EXCEPTION).hasTitle(EMPTY_STRING))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void testHasNoTitleCanFail() {
        assertThatCode(() -> ControllerDetailExceptionAssertions.assertThat(BAD_GATEWAY_EXCEPTION).hasNoTitle())
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void testAssertionCanFailIfActualIsNull() {
        assertThatCode(() -> ControllerDetailExceptionAssertions.assertThat(NULL_EXCEPTION).hasReason(EMPTY_STRING))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(EXPECTING_ACTUAL_NOT_TO_BE_NULL);
    }

    @Test
    void testAssertThatCDExceptionCanFailIfCallableIsNull() {
        final ThrowingCallable callable = null;
        assertThatCode(() -> assertThatCDException(callable).hasDetail(BAD_GATEWAY_DETAIL))
                .isInstanceOf(AssertionError.class)
                .hasMessageContainingAll("Expecting actual throwable to be an instance of:",
                        ControllerDetailException.class.getSimpleName());
    }

    @Test
    void testAssertThatCDExceptionCanFailIfNoExceptionThrown() {
        assertThatCode(() -> assertThatCDException(() -> {
        }).hasDetail(BAD_GATEWAY_DETAIL))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(EXPECTING_ACTUAL_NOT_TO_BE_NULL);
    }

    @Test
    void testAssertThatCDExceptionCanFailIfExceptionIsWrongType() {
        assertThatCode(() -> assertThatCDException(IllegalArgumentException::new).hasDetail(BAD_GATEWAY_DETAIL))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(EXPECTING_ACTUAL_NOT_TO_BE_NULL);
    }
}
