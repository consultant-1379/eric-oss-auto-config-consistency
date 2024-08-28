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

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.http.ResponseEntity;

import com.ericsson.assertions.exception.ControllerDetailExceptionAssert;
import com.ericsson.assertions.exception.ControllerDetailExceptionAssertions;
import com.ericsson.assertions.response.ResponseEntityAssert;
import com.ericsson.assertions.validation.ValidationObjectAssert;
import com.ericsson.assertions.validation.ValidationObjectAssertions;
import com.ericsson.oss.apps.controller.ControllerDetailException;
import com.ericsson.oss.apps.util.ValidationObject;

/**
 * Implementation of {@link Assertions} for use with {@link ResponseEntityAssert} and {@link ControllerDetailExceptionAssert}.
 *
 * @see Assertions
 */
public final class EaccAssertions extends Assertions {
    private EaccAssertions() {
        super();
    }

    public static <T> ResponseEntityAssert<T> assertThat(final ResponseEntity<T> actual) {
        return ResponseEntityAssert.assertThat(actual);
    }

    public static ControllerDetailExceptionAssert assertThat(final ControllerDetailException actual) {
        return ControllerDetailExceptionAssertions.assertThat(actual);
    }

    public static ControllerDetailExceptionAssert assertThatCDException(final ThrowingCallable shouldRaiseControllerDetailException) {
        return ControllerDetailExceptionAssertions.assertThatCDException(shouldRaiseControllerDetailException);
    }

    public static ValidationObjectAssert assertThat(final ValidationObject actual) {
        return ValidationObjectAssertions.assertThat(actual);
    }
}
