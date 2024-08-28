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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ericsson.oss.apps.executor.AuditStatus;
import com.ericsson.oss.apps.executor.ChangeStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_results")
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@IdClass(AuditResultId.class)
public class AuditResult {
    private static final Map<String, AtomicLong> ID_MAP = new HashMap();

    @Id
    private Long id;
    @Id
    private String executionId;
    private String managedObjectFdn;
    private String managedObjectType;
    private String attributeName;
    private String currentValue;
    private String preferredValue;
    @Enumerated(EnumType.STRING)
    private AuditStatus auditStatus;
    private String ruleId;
    @Enumerated(EnumType.STRING)
    private ChangeStatus changeStatus;

    //Visible for testing
    public static void clearIdMap() {
        ID_MAP.clear();
    }

    public void setExecutionId(final String executionId) {
        this.executionId = executionId;
        id = ID_MAP.computeIfAbsent(executionId, eId -> new AtomicLong(0L)).incrementAndGet();
    }

    public void setId(final Long id) {
        this.id = id;
        if (executionId != null) {
            final AtomicLong currentVal = ID_MAP.computeIfAbsent(executionId, eId -> new AtomicLong(0L));
            currentVal.set(Math.max(id, currentVal.get()));
        }
    }

}