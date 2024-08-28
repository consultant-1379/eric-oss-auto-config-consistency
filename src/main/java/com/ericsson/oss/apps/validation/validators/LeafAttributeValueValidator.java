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

import com.ericsson.oss.apps.service.mom.model.AbstractModelAttribute;
import com.ericsson.oss.apps.service.mom.model.LeafModelAttribute;

/**
 * Validator to check if the leaf type attribute value defined in the ruleset is valid according to the
 * {@link com.ericsson.oss.apps.service.mom.model.Model}.
 */
public class LeafAttributeValueValidator {
    private LeafAttributeValueValidator() {
    }

    /**
     * Checks if the attributeValue is valid according to the {@link com.ericsson.oss.apps.service.mom.model.Model}.
     * 
     * @param attribute
     *            the {@link AbstractModelAttribute} representing the attribute
     * @param attributeValue
     *            the {@link Object} representing the attribute value
     * @return a <code>boolean</code> true if attribute value is valid otherwise false
     */
    public static boolean isValidValue(final AbstractModelAttribute attribute, final Object attributeValue) {
        final boolean isValidValue;
        final LeafModelAttribute attributeLeaf = (LeafModelAttribute) attribute;
        final String constraintValue = attributeLeaf.getValueConstraint();

        switch (attributeLeaf.getDataType()) {
            case "boolean":
                isValidValue = BooleanValueValidator.isValidValue(attributeValue);
                break;
            case "enumeration":
                isValidValue = EnumerationValueValidator.isValidValue(attributeValue, constraintValue);
                break;
            case "string":
                isValidValue = StringValueValidator.isValidValue(attributeValue, constraintValue);
                break;
            default: //Number datatype
                isValidValue = NumberValueValidator.isValidValue(attributeValue, constraintValue, attributeLeaf.getDataType());
                break;
        }
        return isValidValue;
    }
}
