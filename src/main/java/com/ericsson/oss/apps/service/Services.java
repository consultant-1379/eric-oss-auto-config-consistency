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

package com.ericsson.oss.apps.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.oss.apps.validation.RuleSetValidator;

import lombok.Data;

@Data
@Component
public class Services {
    @Autowired
    private final CMService cmService;
    @Autowired
    private final RuleService ruleService;
    @Autowired
    private final RetentionService retentionService;
    @Autowired
    private final MetricService metricService;
    @Autowired
    private final RuleSetValidator ruleSetValidator;
}
