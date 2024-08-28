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

package com.ericsson.oss.apps.executor;

/**
 * Status of an EACC Audit.
 */
public enum AuditStatus {
    CONSISTENT("Consistent"),
    INCONSISTENT("Inconsistent");

    private final String description;

    AuditStatus(final String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
