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

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.SoftAssertionsProvider;
import org.assertj.core.util.CheckReturnValue;

import com.ericsson.oss.apps.util.ValidationObject;

/**
 * Implementation of SoftAssertions for {@link ValidationObject}. Note: Use with
 * 
 * <pre>
 * &#64;ExtendWith(SoftAssertionsExtension.class)
 * &#64;InjectSoftAssertions
 * private ValidationObjectSoftAssertions softly; //no need to call {@link SoftAssertions#assertAll()} explicitly
 * </pre>
 * 
 * @see SoftAssertions
 */
public interface IValidationObjectSoftAssertions<T> extends SoftAssertionsProvider {
    @CheckReturnValue
    default ValidationObjectAssert assertThat(final ValidationObject actual) {
        return proxy(ValidationObjectAssert.class, ValidationObject.class, actual);
    }
}
