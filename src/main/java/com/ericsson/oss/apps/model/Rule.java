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

import com.opencsv.bean.CsvBindByName;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "rules")
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Rule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Transient
    @CsvBindByName(column = "line_number")
    private Long lineNumber;
    @CsvBindByName(required = true)
    private String moType;
    @CsvBindByName(required = true)
    private String attributeName;
    @CsvBindByName(required = true)
    private String attributeValue;
    @CsvBindByName
    private String conditions;
    @CsvBindByName
    private Integer priority;

    @ToString.Exclude
    @ManyToOne(optional = false)
    @JoinColumn(name = "ruleset_id")
    private RuleSet ruleSet;

    public Rule(final String moType, final String attributeName, final String attributeValue) {
        this.moType = moType;
        this.attributeName = attributeName;
        this.attributeValue = attributeValue;
    }

    public Rule(final String moType, final String attributeName, final String attributeValue, final String conditions) {
        this.moType = moType;
        this.attributeName = attributeName;
        this.attributeValue = attributeValue;
        this.conditions = conditions;

        if (this.conditions != null) {
            this.conditions = this.conditions.trim();
        }
    }

    public Rule(final String moType, final String attributeName, final String attributeValue, final String conditions, final Integer priority) {
        this.moType = moType;
        this.attributeName = attributeName;
        this.attributeValue = attributeValue;
        this.conditions = conditions;
        this.priority = priority;

        if (this.conditions != null) {
            this.conditions = this.conditions.trim();
        }
    }
}
