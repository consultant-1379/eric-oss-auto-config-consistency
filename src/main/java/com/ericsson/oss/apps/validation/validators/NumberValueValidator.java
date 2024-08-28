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

import java.math.BigDecimal;

import com.ericsson.oss.mediation.modeling.yangtools.parser.model.util.DataTypeHelper;
import com.ericsson.oss.mediation.modeling.yangtools.parser.model.util.NumberHelper;

import lombok.extern.slf4j.Slf4j;

/**
 * Validator to check if the <code>number</code> type attribute value defined in the ruleset is valid according to the
 * {@link com.ericsson.oss.apps.service.mom.model.Model}. <br>
 * <br>
 * <b>Note: </b> Number types dealt with are as follows <br>
 * <ul>
 * <li>int8</li>
 * <li>int16</li>
 * <li>int32</li>
 * <li>int64</li>
 * <li>uint8</li>
 * <li>uint16</li>
 * <li>uint32</li>
 * <li>uint64</li>
 * <li>decimal64</li>
 * </ul>
 */
@Slf4j
public class NumberValueValidator {
    private NumberValueValidator() {
    }

    /**
     * Checks if the attributeValue of type <code>number</code> is valid according to the {@link com.ericsson.oss.apps.service.mom.model.Model}.
     *
     * @param attributeValue
     *            the {@link Object} representing the <code>number</code> value as a {@link String}
     * @param constraint
     *            the {@link String} representing the constraint on the <code>number</code> value <br>
     *            <b>Note:</b> Number can have the following constraints
     *            <ul>
     *            <li>null: There is no constraint defined in model so will be checked against max and min for the datatype.<br>
     *            <b>Note: </b> this is always the case for decimal64</li>
     *            <li>single range constraint: e.g. format -140..-44</li>
     *            <li>multi range constraint: e.g. format 1..1 | 2..2 | 3..3 | 4..4 | 6..6 | 8..8 | 16..16 | 32..32</li>
     *            </ul>
     * @param dataType
     *            the {@link String} representing the yang datatype of the value been checked for validity i.e. int32 int64 unit 16 etc....
     * @return a {@link Boolean} true if attribute value is valid otherwise false
     */
    public static Boolean isValidValue(final Object attributeValue, final String constraint, final String dataType) {
        final DataTypeHelper.YangDataType yangDataType;
        final BigDecimal valueOfAttribute;
        try {
            yangDataType = DataTypeHelper.getYangDataType(dataType);
            valueOfAttribute = NumberHelper.extractYangIntegerValueOrDecimalValue(attributeValue.toString());
        } catch (final Exception e) {
            log.error("Failed to extract number attribute value for dataType", e);
            return false;
        }
        return isAttributeValueValidForConstraintForDataType(valueOfAttribute, constraint, yangDataType);
    }

    public static Boolean isAttributeValueValidForConstraintForDataType(final BigDecimal attributeValue, final String constraint,
            final DataTypeHelper.YangDataType yangDataType) {
        boolean isValidValue = false;

        if (constraint == null) { //No Constraint. Check against max and min for datatype

            if ((yangDataType.equals(DataTypeHelper.YangDataType.DECIMAL64)
                    || ((attributeValue.compareTo(NumberHelper.getMinValueForYangIntegerDataType(yangDataType)) >= 0)
                            && (attributeValue.compareTo(NumberHelper.getMaxValueForYangIntegerDataType(yangDataType)) <= 0)))) {
                isValidValue = true;
            }
        } else { // Has a constraint
            if (constraint.contains("|")) { //Multiple Ranges
                isValidValue = isValueValidForConstraintWithMultipleRanges(attributeValue, constraint.replaceAll("\\s", ""));

            } else { //Single Range
                isValidValue = isValueValidForConstraintWithSingleRange(attributeValue, constraint);
            }
        }
        return isValidValue;
    }

    private static boolean isValueValidForConstraintWithMultipleRanges(final BigDecimal attributeValue, final String constraint) {
        boolean isValidValue = false;
        final String[] rangesMinMax = constraint.split("\\|");
        for (final String rangeMinMax : rangesMinMax) {
            if (isValueValidForConstraintWithSingleRange(attributeValue, rangeMinMax)) {
                isValidValue = true;
                break;
            }
        }
        return isValidValue;
    }

    private static boolean isValueValidForConstraintWithSingleRange(final BigDecimal attributeValue, final String constraint) {
        final String[] valueMinMax = constraint.split("\\.\\.");
        final BigDecimal min = NumberHelper.extractYangIntegerValueOrDecimalValue(valueMinMax[0]);
        final BigDecimal max = NumberHelper.extractYangIntegerValueOrDecimalValue(valueMinMax[1]);

        return ((attributeValue.compareTo(min) >= 0)
                && (attributeValue.compareTo(max) <= 0));
    }
}
