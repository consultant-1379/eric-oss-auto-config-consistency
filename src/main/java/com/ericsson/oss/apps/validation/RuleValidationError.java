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

/**
 * A Validation object to be used by the Ruleset Validator.
 * 
 * @param lineNumber
 *            the (1-indexed) number of the line with the problem
 * @param errorType
 *            the type of problem
 * @param errorDetails
 *            a short description of the problem
 * @param additionalInfo
 *            an extended description of the problem
 */
public record RuleValidationError(Long lineNumber, String errorType, String errorDetails, String additionalInfo) {
}
