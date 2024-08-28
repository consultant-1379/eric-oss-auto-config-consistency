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

package com.ericsson.oss.apps.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Composite key wrapper for {@link AuditResult} Entity.
 */
@NoArgsConstructor
public class AuditResultId implements Serializable {

    @Serial
    private static final long serialVersionUID = 2804638272487081449L;

    @Getter
    @Setter
    private Long id;

    @Getter
    @Setter
    private String executionId;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AuditResultId that = (AuditResultId) o;
        return getExecutionId().equals(that.getExecutionId()) && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getExecutionId(), getId());
    }

    @Override
    public String toString() {
        return "AuditResultId{" +
                "executionId='" + executionId + '\'' +
                ", id=" + id +
                '}';
    }
}
