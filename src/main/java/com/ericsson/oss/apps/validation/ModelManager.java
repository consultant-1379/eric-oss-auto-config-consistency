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
 * ModelManager interface.
 */
public interface ModelManager {

    /**
     * Checks if the moType is valid.
     *
     * @param moType
     *            {@link String} the MO Type
     * @return {@link Boolean} isValidMo
     */
    boolean isValidMo(String moType);

    /**
     * Checks if the moType is valid for the cmHandle specific revision.
     *
     * @param moType
     *            {@link String} the MO Type
     * @param cmHandle
     *            {@link String} the cmHandle
     * @return {@link Boolean} isValidMo
     */
    boolean isValidMo(String moType, String cmHandle);

    /**
     * Checks if the attribute is valid.
     *
     * @param moType
     *            {@link String} the MO Type
     * @param attributeName
     *            {@link String} the attribute name
     * @return {@link Boolean} isValidAttribute
     */
    boolean isValidAttribute(String moType, String attributeName);

    /**
     * Checks if the attribute is valid for the cmHandle specific revision.
     *
     * @param moType
     *            {@link String} the MO Type
     * @param attributeName
     *            {@link String} the attribute name
     * @param cmHandle
     *            {@link String} the cmHandle
     * @return {@link Boolean} isValidAttribute
     */
    boolean isValidAttribute(String moType, String attributeName, String cmHandle);

    /**
     * Checks if the attribute value is valid.
     *
     * @param moType
     *            {@link String} the MO Type
     * @param attributeName
     *            {@link String} the attribute name
     * @param attributeValue
     *            {@link Object} the attribute value
     * @return {@link Boolean} isValidValue
     */
    boolean isValidValue(String moType, String attributeName, Object attributeValue);

    /**
     * Checks if the attribute value is valid for the cmHandle specific revision.
     *
     * @param moType
     *            {@link String} the MO Type
     * @param attributeName
     *            {@link String} the attribute name
     * @param attributeValue
     *            {@link Object} the attribute value
     * @param cmHandle
     *            {@link String} the cmHandle
     * @return {@link Boolean} isValidValue
     */
    boolean isValidValue(String moType, String attributeName, Object attributeValue, String cmHandle);

    /**
     * Provides the data type of given attribute for the cmHandle specific revision.
     *
     * @param moType
     *            {@link String} the MO Type
     * @param attributeName
     *            {@link String} the attribute name
     * @param cmHandle
     *            {@link String} the cmHandle
     * @return {@link String} data type of the attribute
     */
    String getDataType(String moType, String attributeName, String cmHandle);
}
