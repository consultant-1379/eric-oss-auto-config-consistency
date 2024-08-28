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
 * The different types of executions.
 */
public enum ExecutionType {
    OPEN_LOOP("Open Loop"),
    CLOSED_LOOP("Closed Loop"),
    ON_DEMAND("On Demand");

    private final String description;

    ExecutionType(final String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

}
