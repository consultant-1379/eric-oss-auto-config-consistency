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

package com.ericsson.oss.apps.validation;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exception that should be thrown for any problems that occur when validating rules.
 */
public class RuleValidationException extends Exception {

    private static final long serialVersionUID = 7846634860982288103L;

    private final transient List<RuleValidationError> ruleValidationErrors;

    public RuleValidationException(final String message, final List<RuleValidationError> ruleValidationErrors) {
        super(message);
        this.ruleValidationErrors = ruleValidationErrors.stream()
                .collect(Collectors.toList());
    }

    public List<RuleValidationError> getRuleValidationErrors() {
        return ruleValidationErrors.stream()
                .sorted(Comparator.comparingLong(RuleValidationError::lineNumber))
                .collect(Collectors.toList());
    }

}
