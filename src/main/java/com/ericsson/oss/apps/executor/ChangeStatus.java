/*******************************************************************************
 * COPYRIGHT Ericsson 2023 - 2024
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

package com.ericsson.oss.apps.executor;

/**
 * Change Status of an EACC Audit Result.
 */
public enum ChangeStatus {
    NOT_APPLIED("Not applied"),
    IMPLEMENTATION_IN_PROGRESS("Implementation in progress"),
    IMPLEMENTATION_COMPLETE("Implementation complete"),
    IMPLEMENTATION_FAILED("Implementation failed"),
    IMPLEMENTATION_ABORTED("Implementation aborted"),
    REVERSION_IN_PROGRESS("Reversion in progress"),
    REVERSION_COMPLETE("Reversion complete"),
    REVERSION_FAILED("Reversion failed"),
    REVERSION_ABORTED("Reversion aborted");

    private final String description;

    ChangeStatus(final String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
