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

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.SoftAssertionsProvider;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.assertj.core.util.CheckReturnValue;

import com.ericsson.oss.apps.controller.ControllerDetailException;

/**
 * Implementation of SoftAssertions for {@link ControllerDetailException}. Note: Use with
 *
 * <pre>
 * &#64;ExtendWith(SoftAssertionsExtension.class)
 * &#64;InjectSoftAssertions
 * private ControllerDetailExceptionSoftAssertions softly; //no need to call {@link SoftAssertions#assertAll()} explicitly
 * </pre>
 *
 * @see SoftAssertions
 */
public interface IControllerDetailExceptionSoftAssertions extends SoftAssertionsProvider {
    @CheckReturnValue
    default ControllerDetailExceptionAssert assertThat(final ControllerDetailException actual) {
        return proxy(ControllerDetailExceptionAssert.class, ControllerDetailException.class, actual);
    }

    @CheckReturnValue
    default ControllerDetailExceptionAssert assertThatCDException(final ThrowingCallable shouldRaiseControllerDetailException) {
        return ControllerDetailExceptionAssertions.assertThatCDException(shouldRaiseControllerDetailException);
    }
}