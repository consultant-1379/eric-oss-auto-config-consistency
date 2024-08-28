/*******************************************************************************
 * COPYRIGHT Ericsson 2024
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

package com.ericsson.oss.apps.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit Tests for {@link RuleValidationError} record.
 */
class RuleValidationErrorTest {

    private RuleValidationError objectUnderTest;

    @BeforeEach
    void setUp() {
        objectUnderTest = new RuleValidationError(0L, "errorType", "errorDetails", "additionalInfo");
    }

    @Test
    public void testHashCode() {
        assertThat(objectUnderTest)
                .hasSameHashCodeAs(new RuleValidationError(0L, "errorType", "errorDetails", "additionalInfo"));
    }

    @Test
    public void testEquals() {
        assertThat(objectUnderTest)
                .isEqualTo(new RuleValidationError(0L, "errorType", "errorDetails", "additionalInfo"));
    }

    @Test
    public void testToString() {
        assertThat(objectUnderTest)
                .hasToString("RuleValidationError[lineNumber=0, errorType=errorType, errorDetails=errorDetails, additionalInfo=additionalInfo]");
    }
}
