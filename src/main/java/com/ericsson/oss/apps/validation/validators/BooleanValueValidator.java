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

package com.ericsson.oss.apps.validation.validators;

/**
 * Validator to check if the <code>boolean</code> type attribute value defined in the ruleset is valid according to the
 * {@link com.ericsson.oss.apps.service.mom.model.Model}.
 */
public class BooleanValueValidator {
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String UPPERCASE_TRUE = "TRUE";
    private static final String UPPERCASE_FALSE = "FALSE";

    private BooleanValueValidator() {
    }

    /**
     * Checks if the attributeValue of type <code>boolean</code> is valid according to the {@link com.ericsson.oss.apps.service.mom.model.Model}.
     * 
     * @param attributeValue
     *            the {@link Object} representing the <code>boolean</code> value as a {@link String}
     * @return a {@link Boolean} true if attribute value is valid otherwise false
     */
    public static Boolean isValidValue(final Object attributeValue) {
        final String attributeValueAsString = attributeValue.toString();
        return (attributeValueAsString.equals(TRUE) || attributeValueAsString.equals(FALSE)
                || attributeValueAsString.equals(UPPERCASE_TRUE) || attributeValueAsString.equals(UPPERCASE_FALSE));

    }
}
