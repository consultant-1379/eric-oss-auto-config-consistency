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

import java.util.Arrays;

/**
 * Validator to check if the <code>enumeration</code> type attribute value defined in the ruleset is valid according to the
 * {@link com.ericsson.oss.apps.service.mom.model.Model}.
 */
public class EnumerationValueValidator {

    private static final String ENUM_CONSTRAINT_PREFIX = "enum %s";

    private EnumerationValueValidator() {
    }

    /**
     * Checks if the attributeValue of type <code>enumeration</code> is valid according to the {@link com.ericsson.oss.apps.service.mom.model.Model}.
     *
     * @param attributeValue the {@link Object} representing the <code>enumeration</code> value as a {@link String}
     * @param constraint     the {@link String} representing the constraint on the <code>enumeration</code>
     *                       value e.g. format is [enum BARRED, enum NOT_BARRED]
     * @return a {@link Boolean} true if attribute value is valid otherwise false
     */
    public static Boolean isValidValue(final Object attributeValue, final String constraint) {
        //remove square brackets from start and end
        final String clean = constraint.substring(1, constraint.length() - 1);
        final String[] enums = clean.split(",");
        final String expected = ENUM_CONSTRAINT_PREFIX.formatted(attributeValue);
        return Arrays.stream(enums)
                .map(String::trim)
                .anyMatch(expected::equals);
    }
}
