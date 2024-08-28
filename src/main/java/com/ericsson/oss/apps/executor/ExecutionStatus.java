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
 * Status of EACC Executions.
 */
public enum ExecutionStatus {
    AUDIT_IN_PROGRESS("Audit in Progress"),
    AUDIT_SKIPPED("Audit Skipped"),
    AUDIT_SUCCESSFUL("Audit Successful"),
    AUDIT_PARTIALLY_SUCCESSFUL("Audit Partially Successful"),
    AUDIT_FAILED("Audit Failed"),
    AUDIT_ABORTED("Audit Aborted"),
    CHANGES_IN_PROGRESS("Changes in Progress"),
    CHANGES_FAILED("Changes Failed"),
    CHANGES_SUCCESSFUL("Changes Successful"),
    CHANGES_PARTIALLY_SUCCESSFUL("Changes Partially Successful"),
    CHANGES_ABORTED("Changes Aborted"),
    REVERSION_IN_PROGRESS("Reversion in Progress"),
    REVERSION_FAILED("Reversion Failed"),
    REVERSION_SUCCESSFUL("Reversion Successful"),
    REVERSION_PARTIALLY_SUCCESSFUL("Reversion Partially Successful"),
    REVERSION_ABORTED("Reversion Aborted");

    private final String description;

    ExecutionStatus(final String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
