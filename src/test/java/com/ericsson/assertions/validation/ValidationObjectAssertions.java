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

import org.assertj.core.api.Assertions;

import com.ericsson.oss.apps.util.ValidationObject;

/**
 * Implementation of {@link Assertions} for use with {@link ValidationObjectAssert}.
 *
 * @see Assertions
 */
public class ValidationObjectAssertions extends Assertions {
    public static ValidationObjectAssert assertThat(final ValidationObject actual) {
        return ValidationObjectAssert.assertThat(actual);
    }
}
