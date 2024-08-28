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

import java.time.OffsetDateTime;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ericsson.oss.apps.executor.ExecutionStatus;
import com.ericsson.oss.apps.executor.ExecutionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "eacc_executions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String jobName;
    private String ruleSetName;
    private String scopeName;
    @Enumerated(EnumType.STRING)
    private ExecutionType executionType;
    private OffsetDateTime executionStartedAt;
    private OffsetDateTime consistencyAuditStartedAt;
    private OffsetDateTime consistencyAuditEndedAt;
    private OffsetDateTime executionEndedAt;
    @Enumerated(EnumType.STRING)
    private ExecutionStatus executionStatus;
    @Column(columnDefinition = "int default 0")
    private int totalNodesAudited;
    @Column(columnDefinition = "int default 0")
    private int totalNodesFailed;
    @Column(columnDefinition = "int default 0")
    private int totalMosAudited;
    @Column(columnDefinition = "int default 0")
    private int totalAttributesAudited;
    @Column(columnDefinition = "int default 0")
    private int inconsistenciesIdentified;
}
