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

import java.util.Objects;

import org.assertj.core.api.AbstractAssert;
import org.springframework.http.HttpStatus;

import com.ericsson.oss.apps.util.ValidationObject;

public class ValidationObjectAssert extends AbstractAssert<ValidationObjectAssert, ValidationObject> {
    protected ValidationObjectAssert(final ValidationObject actual) {
        super(actual, ValidationObjectAssert.class);
    }

    public static ValidationObjectAssert assertThat(final ValidationObject actual) {
        return new ValidationObjectAssert(actual);
    }

    /**
     * Verifies that the actual ValidationObject has validated==true.
     *
     * <pre>
     * Fails if actual is null, or actual.getValidated()==false
     * </pre>
     *
     * @return {@link ValidationObjectAssert} assertion wrapper for {@link ValidationObject}
     */
    public ValidationObjectAssert isValidated() {
        isNotNull();
        if (!actual.getValidated()) {
            failWithMessage("Expected ValidationObject %n'%s'%n to have be valid, but the actual validation status is false.", actual);
        }
        return this;
    }

    /**
     * Verifies that the actual ValidationObject has validated==false.
     *
     * <pre>
     * Fails if actual is null, or actual.getValidated()==true
     * </pre>
     *
     * @return {@link ValidationObjectAssert} assertion wrapper for {@link ValidationObject}
     */
    public ValidationObjectAssert isNotValidated() {
        isNotNull();
        if (actual.getValidated()) {
            failWithMessage("Expected ValidationObject %n'%s'%n to have be invalid, but the actual validation status is true.", actual);
        }
        return this;
    }

    /**
     * Verifies that the actual ValidationObject has the specified HttpStatus.
     *
     * <pre>
     * Fails if actual is null, or actual.getHttpStatus() != expectedHttpStatus
     * </pre>
     *
     * @param expectedHttpStatus
     *            the status expected in the actual ValidationObject
     * @return {@link ValidationObjectAssert} assertion wrapper for {@link ValidationObject}
     * @see HttpStatus
     */
    public ValidationObjectAssert hasStatus(final HttpStatus expectedHttpStatus) {
        isNotNull();
        final HttpStatus actualHttpStatus = actual.getHttpStatus();
        if (expectedHttpStatus != actualHttpStatus) {
            failWithMessage("Expected ValidationObject %n'%s'%n to have status:'%s', but the actual validation status is '%s'.",
                    actual, expectedHttpStatus, actualHttpStatus);
        }
        return this;
    }

    /**
     * Verifies that the actual ValidationObject has the specified details.
     *
     * <pre>
     * Fails if actual is null, or !actual.getDetails().equals(expectedDetails)
     * </pre>
     *
     * @param expectedDetailsTemplate
     *            the format string details expected for the actual ValidationObject.
     * @param args
     *            the arguments referenced by the format specifiers in the format string.
     * @return {@link ValidationObjectAssert} assertion wrapper for {@link ValidationObject}
     */
    public ValidationObjectAssert hasDetails(final String expectedDetailsTemplate, final Object... args) {
        isNotNull();

        String expectedDetail = null;
        if (expectedDetailsTemplate != null) {
            expectedDetail = String.format(expectedDetailsTemplate, args);
        }
        final String actualDetails = actual.getDetails();
        if (!Objects.equals(actualDetails, expectedDetail)) {
            failWithMessage("Expected ValidationObject %n'%s'%n to have details:'%s', but the actual details are '%s'.",
                    actual, expectedDetail, actualDetails);
        }
        return this;
    }

    /**
     * Verifies that the actual ValidationObject has the specified details.
     *
     * <pre>
     * Fails if actual is null, or !actual.getDetails().equals(expectedDetails)
     * </pre>
     *
     * @param expectedTitleTemplate
     *            the format string title expected for the actual ValidationObject.
     * @param args
     *            the arguments referenced by the format specifiers in the format string.
     * @return {@link ValidationObjectAssert} assertion wrapper for {@link ValidationObject}
     */
    public ValidationObjectAssert hasTitle(final String expectedTitleTemplate, final Object... args) {
        isNotNull();

        String expectedDetail = null;
        if (expectedTitleTemplate != null) {
            expectedDetail = String.format(expectedTitleTemplate, args);
        }
        final String actualTitle = actual.getTitle();
        if (!Objects.equals(actualTitle, expectedDetail)) {
            failWithMessage("Expected ValidationObject %n'%s'%n to have title:'%s', but the actual title are '%s'.",
                    actual, expectedDetail, actualTitle);
        }
        return this;
    }
}
