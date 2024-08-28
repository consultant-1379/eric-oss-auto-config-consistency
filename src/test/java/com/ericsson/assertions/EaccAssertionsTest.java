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
package com.ericsson.assertions;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ericsson.oss.apps.controller.ControllerDetailException;

/**
 * Unit test class for {@link EaccAssertions} and {@link EaccSoftAssertions} classes.
 */
@ExtendWith(SoftAssertionsExtension.class)
public class EaccAssertionsTest {
    private static final ControllerDetailException NO_FIELDS_CDEXCEPTION = new ControllerDetailException(OK, null, null);
    private static final String DETAIL = "detail";
    private static final String REASON = "reason";
    private static final ControllerDetailException BAD_REQUEST_CDEXCEPTION = new ControllerDetailException(BAD_REQUEST, DETAIL, REASON);
    private static final String STRING_BODY = "body";
    private static final ResponseEntity<String> STRING_OK_ENTITY = ResponseEntity.ok(STRING_BODY);

    @InjectSoftAssertions
    private EaccSoftAssertions softly;

    @Test
    void testAllAssertionsCanPass() {
        EaccAssertions.assertThat(BAD_REQUEST_CDEXCEPTION)
                .hasDetail("det%s%s", "ai", "l")
                .hasStatus(BAD_REQUEST)
                .hasReason("rea%s%s", "so", "n")
                .hasTitle("rea%s%s", "so", "n");
        EaccAssertions.assertThatCDException(() -> {
            throw NO_FIELDS_CDEXCEPTION;
        })
                .hasNoTitle()
                .hasNoDetail();
        EaccAssertions.assertThat(STRING_OK_ENTITY)
                .hasBody()
                .hasBody(STRING_BODY)
                .hasStatus(HttpStatus.OK)
                .hasStatusValue(200);
        softly.assertThat(BAD_REQUEST_CDEXCEPTION)
                .hasDetail(DETAIL)
                .hasStatus(BAD_REQUEST)
                .hasReason("reaso%s", "n")
                .hasTitle(REASON);
        softly.assertThatCDException(() -> {
            throw NO_FIELDS_CDEXCEPTION;
        })
                .hasNoTitle()
                .hasNoDetail();
        softly.assertThat(STRING_OK_ENTITY)
                .hasBody()
                .hasBody(STRING_BODY)
                .hasStatus(HttpStatus.OK)
                .hasStatusValue(200);
    }

    @Test
    void testResponseEntityAssertionCanFail() {
        assertThatCode(() -> EaccAssertions.assertThat(STRING_OK_ENTITY).hasBody("")).isInstanceOf(AssertionError.class);
    }

    @Test
    void testControllerDetailExceptionAssertionCanFail() {
        assertThatCode(() -> EaccAssertions.assertThat(BAD_REQUEST_CDEXCEPTION).hasReason("%s", "")).isInstanceOf(AssertionError.class);
    }

    @Test
    void testControllerDetailExceptionAssertionCanFailIfNull() {
        final ControllerDetailException nullException = null;
        assertThatCode(() -> EaccAssertions.assertThat(nullException).hasReason("%s", "")).isInstanceOf(AssertionError.class);
    }
}
