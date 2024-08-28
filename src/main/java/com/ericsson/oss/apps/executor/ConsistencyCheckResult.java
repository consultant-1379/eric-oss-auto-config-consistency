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

import java.util.List;

import com.ericsson.oss.apps.model.AuditResult;
import com.ericsson.oss.apps.model.EaccManagedObject;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Class containing the results of a consistency check against a {@link EaccManagedObject}.
 */
@AllArgsConstructor
@Data
class ConsistencyCheckResult {
    private List<AuditResult> auditResultList;
    private int inconsistencyCount;
}
