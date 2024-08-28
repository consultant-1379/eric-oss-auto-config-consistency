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

package com.ericsson.assertions.validation;

import static com.ericsson.oss.apps.util.Constants.EMPTY_STRING;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_COMPLETE;
import static com.ericsson.oss.apps.util.TestDefaults.VALIDATION_FAILED_TITLE;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;

import com.ericsson.oss.apps.util.ValidationObject;

/**
 * Unit test class for {@link ValidationObjectAssertions} and {@link ValidationObjectSoftAssertions} classes.
 */
@ExtendWith(SoftAssertionsExtension.class)
class ValidationObjectAssertionsTest {
    private static final boolean IS_VALIDATED = true;
    private static final ValidationObject VALIDATION_SUCCESS = new ValidationObject(IS_VALIDATED, EMPTY_STRING, VALIDATION_COMPLETE, HttpStatus.OK);

    private static final String FAILURE_CAUSED_BY_ISSUES = "failure cause by issues";
    private static final ValidationObject FAILED_VALIDATION_OBJECT = //
            new ValidationObject(!IS_VALIDATED, FAILURE_CAUSED_BY_ISSUES, VALIDATION_FAILED_TITLE, HttpStatus.BAD_REQUEST);
    private static final ValidationObject NULL_VALIDATION_OBJECT = null;

    @InjectSoftAssertions
    private ValidationObjectSoftAssertions softly;

    @Test
    void testAllAssertionsCanPass() {
        ValidationObjectAssertions.assertThat(VALIDATION_SUCCESS)
                .isValidated()
                .hasTitle("Validation %s", "complete")
                .hasDetails(EMPTY_STRING)
                .hasStatus(HttpStatus.OK);
        ValidationObjectAssertions.assertThat(FAILED_VALIDATION_OBJECT)
                .isNotValidated()
                .hasTitle(VALIDATION_FAILED_TITLE)
                .hasDetails(FAILURE_CAUSED_BY_ISSUES)
                .hasStatus(HttpStatus.BAD_REQUEST);
        ValidationObjectAssertions.assertThat(NULL_VALIDATION_OBJECT).isNull();
        softly.assertThat(VALIDATION_SUCCESS)
                .isValidated()
                .hasTitle("Validation %s", "complete")
                .hasDetails(EMPTY_STRING)
                .hasStatus(HttpStatus.OK);
        softly.assertThat(FAILED_VALIDATION_OBJECT)
                .isNotValidated()
                .hasTitle(VALIDATION_FAILED_TITLE)
                .hasDetails(FAILURE_CAUSED_BY_ISSUES)
                .hasStatus(HttpStatus.BAD_REQUEST);
        softly.assertThat(NULL_VALIDATION_OBJECT).isNull();
    }

    @Test
    void testIsValidatedCanFail() {
        assertThatCode(() -> ValidationObjectAssertions.assertThat(FAILED_VALIDATION_OBJECT).isValidated()).isInstanceOf(AssertionError.class);
    }

    @Test
    void testIsNotValidatedCanFail() {
        assertThatCode(() -> ValidationObjectAssertions.assertThat(VALIDATION_SUCCESS).isNotValidated()).isInstanceOf(AssertionError.class);
    }

    @Test
    void testHasTitleCanFail() {
        assertThatCode(() -> ValidationObjectAssertions.assertThat(VALIDATION_SUCCESS).hasTitle("nope")).isInstanceOf(AssertionError.class);
    }

    @Test
    void testHasDetailsCanFail() {
        assertThatCode(() -> ValidationObjectAssertions.assertThat(VALIDATION_SUCCESS).hasDetails("nope")).isInstanceOf(AssertionError.class);
    }

    @Test
    void testHasStatusCanFail() {
        assertThatCode(() -> ValidationObjectAssertions.assertThat(VALIDATION_SUCCESS).hasStatus(HttpStatus.I_AM_A_TEAPOT))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void testAssertionCanFailIfActualIsNull() {
        assertThatCode(() -> ValidationObjectAssertions.assertThat(NULL_VALIDATION_OBJECT).isValidated())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expecting actual not to be null");
    }
}
