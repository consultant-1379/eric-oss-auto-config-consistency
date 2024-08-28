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

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import com.ericsson.oss.apps.controller.ControllerDetailException;

/**
 * Implementation of {@link Assertions} for use with {@link ControllerDetailExceptionAssert}.
 *
 * @see Assertions
 */
public class ControllerDetailExceptionAssertions extends Assertions {
    public static ControllerDetailExceptionAssert assertThat(final ControllerDetailException actual) {
        return ControllerDetailExceptionAssert.assertThat(actual);
    }

    public static ControllerDetailExceptionAssert assertThatCDException(final ThrowingCallable shouldRaiseControllerDetailException) {
        return assertThat(catchThrowableOfType(shouldRaiseControllerDetailException, ControllerDetailException.class)).isNotNull();
    }
}
