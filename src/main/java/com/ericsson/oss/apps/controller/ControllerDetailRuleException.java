/*******************************************************************************
 * COPYRIGHT Ericsson 2024
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

package com.ericsson.oss.apps.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpStatus;

import com.ericsson.oss.apps.validation.RuleValidationError;

public class ControllerDetailRuleException extends ControllerDetailException {

    private static final long serialVersionUID = -4427646581880066211L;

    private final transient List<RuleValidationError> ruleValidationErrorList;

    public ControllerDetailRuleException(final HttpStatus status, final String detail, final String title,
            final List<RuleValidationError> ruleValidationErrorList) {
        super(status, detail, title);
        this.ruleValidationErrorList = new ArrayList<>(ruleValidationErrorList);
    }

    public ControllerDetailRuleException(final HttpStatus status, final String detail, final String title) {
        this(status, detail, title, Collections.emptyList());
    }

    public List<RuleValidationError> getRuleValidationErrors() {
        return new ArrayList<>(ruleValidationErrorList);
    }

}
