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

import org.assertj.core.api.AbstractThrowableAssert;
import org.springframework.http.HttpStatus;

import com.ericsson.oss.apps.controller.ControllerDetailException;

/**
 * Assert methods for {@link ControllerDetailException} class.
 * <p>
 * To create a new instance of this class, invoke {@link ControllerDetailExceptionAssertions#assertThat(ControllerDetailException)}.
 *
 * @see AbstractThrowableAssert
 */
public class ControllerDetailExceptionAssert extends AbstractThrowableAssert<ControllerDetailExceptionAssert, ControllerDetailException> {
    protected ControllerDetailExceptionAssert(final ControllerDetailException actual) {
        super(actual, ControllerDetailExceptionAssert.class);
    }

    public static ControllerDetailExceptionAssert assertThat(final ControllerDetailException actual) {
        return new ControllerDetailExceptionAssert(actual);
    }

    /**
     * Verifies that the actual exception has the specified HttpStatus.
     *
     * <pre>
     * Fails if actual is null, or !actual.getStatus().equals(expectedStatus)
     * </pre>
     *
     * @param expectedStatus
     *            the HttpStatus expected for the actual exception
     * @return {@link ControllerDetailExceptionAssert} assertion wrapper for {@link ControllerDetailException}
     * @see HttpStatus
     */
    public ControllerDetailExceptionAssert hasStatus(final HttpStatus expectedStatus) {
        isNotNull();
        final int actualStatusHttpCode = actual.getStatusCode().value();
        final HttpStatus actualStatus = HttpStatus.valueOf(actualStatusHttpCode);

        if (expectedStatus != actualStatus) {
            if (actualStatus == null) {
                failWithMessage("Expected Exception to have status '%s', but the actual status is null.", expectedStatus);
            } else {
                failWithMessage("Expected Exception to have status '%s', but it was '%s'.", expectedStatus, actualStatus);
            }
        }
        return this;
    }

    /**
     * Verifies that the actual exception has the specified reason.
     *
     * <pre>
     * Fails if actual is null, or !actual.getReason().equals(expectedReason)
     * </pre>
     *
     * @param expectedReason
     *            the string reason expected for the actual exception
     * @return {@link ControllerDetailExceptionAssert} assertion wrapper for {@link ControllerDetailException}
     */
    public ControllerDetailExceptionAssert hasReason(final String expectedReason) {
        isNotNull();
        final String actualReason = actual.getReason();
        if (expectedReason == null) {
            if (actualReason != null) {
                failWithMessage("Expected Exception to have reason:null, but the actual reason was '%s'.", actualReason);
            }
        } else {
            if (!expectedReason.equals(actualReason)) {
                if (actualReason == null) {
                    failWithMessage("Expected Exception to have reason '%s', but it was null.", expectedReason);
                } else {
                    failWithMessage("Expected Exception to have reason '%s', but it was '%s'.", expectedReason, actualReason);
                }
            }
        }
        return this;
    }

    /**
     * Verifies that the actual exception has the specified reason.
     *
     * <pre>
     * Fails if actual is null, or !actual.getReason().equals(expectedReason)
     * </pre>
     *
     * @param expectedReasonTemplate
     *            the format string reason expected for the actual exception.
     * @param args
     *            the arguments referenced by the format specifiers in the format string.
     * @return {@link ControllerDetailExceptionAssert} assertion wrapper for {@link ControllerDetailException}
     */
    public ControllerDetailExceptionAssert hasReason(final String expectedReasonTemplate, final Object... args) {
        return hasReason(String.format(expectedReasonTemplate, args));
    }

    /**
     * Verifies that the actual exception has no specified reason or that actual.getReason() == null;
     *
     * <pre>
     * Fails if actual is not null
     * </pre>
     *
     * @return {@link ControllerDetailExceptionAssert} assertion wrapper for {@link ControllerDetailException}
     */
    public ControllerDetailExceptionAssert hasNoReason() {
        return hasReason(null);
    }

    /**
     * Duplicate of #hasReason
     *
     * @param expectedTitle
     *            the string title (or reason) expected for the actual exception
     * @return {@link ControllerDetailExceptionAssert} assertion wrapper for {@link ControllerDetailException}
     * @see #hasReason
     */
    public ControllerDetailExceptionAssert hasTitle(final String expectedTitle) {
        return hasReason(expectedTitle);
    }

    /**
     * Duplicate of #hasReason
     *
     * @param expectedTitleTemplate
     *            the format string title (or reason) expected for the actual exception.
     * @param args
     *            the arguments referenced by the format specifiers in the format string.
     * @return {@link ControllerDetailExceptionAssert} assertion wrapper for {@link ControllerDetailException}
     * @see #hasReason
     */
    public ControllerDetailExceptionAssert hasTitle(final String expectedTitleTemplate, final Object... args) {
        return hasReason(expectedTitleTemplate, args);
    }

    /**
     * Duplicate of #hasNoReason
     *
     * @return {@link ControllerDetailExceptionAssert} assertion wrapper for {@link ControllerDetailException}
     * @see #hasNoReason
     */
    public ControllerDetailExceptionAssert hasNoTitle() {
        return hasNoReason();
    }

    /**
     * Verifies that the actual exception has the specified detail.
     *
     * <pre>
     * Fails if actual is null, or !actual.getDetail().equals(expectedDetail)
     * </pre>
     *
     * @param expectedDetail
     *            the string reason expected for the actual exception
     * @return {@link ControllerDetailExceptionAssert} assertion wrapper for {@link ControllerDetailException}
     */
    public ControllerDetailExceptionAssert hasDetail(final String expectedDetail) {
        isNotNull();
        final String actualDetail = actual.getDetail();
        if (expectedDetail == null) {
            if (actualDetail != null) {
                failWithMessage("Expected Exception to have detail:null, but the actual detail was '%s'.", actualDetail);
            }
        } else {
            if (!expectedDetail.equals(actualDetail)) {
                if (actualDetail == null) {
                    failWithMessage("Expected Exception to have detail '%s', but it was null.", expectedDetail);
                } else {
                    failWithMessage("Expected Exception to have detail '%s', but it was '%s'.", expectedDetail, actualDetail);
                }
            }
        }
        return this;
    }

    /**
     * Verifies that the actual exception has the specified detail.
     *
     * <pre>
     * Fails if actual is null, or !actual.getDetail().equals(expectedDetail)
     * </pre>
     *
     * @param expectedDetailTemplate
     *            the format string reason expected for the actual exception.
     * @param args
     *            the arguments referenced by the format specifiers in the format string.
     * @return {@link ControllerDetailExceptionAssert} assertion wrapper for {@link ControllerDetailException}
     */
    public ControllerDetailExceptionAssert hasDetail(final String expectedDetailTemplate, final Object... args) {
        return hasDetail(String.format(expectedDetailTemplate, args));
    }

    /**
     * Verifies that the actual exception has no specified detail, or that actual.getDetail() == null.
     *
     * <pre>
     * Fails if actual is not null
     * </pre>
     *
     * @return {@link ControllerDetailExceptionAssert} assertion wrapper for {@link ControllerDetailException}
     */
    public ControllerDetailExceptionAssert hasNoDetail() {
        return hasDetail(null);
    }
}
