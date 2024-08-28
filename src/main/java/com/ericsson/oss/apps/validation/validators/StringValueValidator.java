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

package com.ericsson.oss.apps.validation.validators;

/**
 * Validator to check if the <code>string</code> type attribute value defined in the ruleset is valid according to the
 * {@link com.ericsson.oss.apps.service.mom.model.Model}.
 */
public class StringValueValidator {
    private StringValueValidator() {
    }

    /**
     * Checks if the attributeValue of type <code>string</code> is valid according to the {@link com.ericsson.oss.apps.service.mom.model.Model}.
     *
     * @param attributeValue
     *            the {@link Object} representing the <code>string</code> value as a {@link String}
     * @param constraint
     *            the {@link String} representing the constraint on the <code>string</code> value e.g. format is length 1..128
     * @return a {@link Boolean} true if attribute value is valid otherwise false
     */
    public static Boolean isValidValue(final Object attributeValue, final String constraint) {
        boolean isValidValue = false;
        if (constraint == null) { //No Constraint
            isValidValue = true;
        } else if (attributeValue != null && constraint.startsWith("length")) {
            final String constraintWithLengthKeywordRemoved = constraint.replace("length", "").trim();
            final String[] lengthMinMax = constraintWithLengthKeywordRemoved.split("\\.\\.");
            final int min = Integer.parseInt(lengthMinMax[0]);
            final int max = Integer.parseInt(lengthMinMax[1]);
            if (attributeValue.toString().length() >= min && attributeValue.toString().length() <= max) {
                isValidValue = true;
            }
        }
        return isValidValue;
    }
}
