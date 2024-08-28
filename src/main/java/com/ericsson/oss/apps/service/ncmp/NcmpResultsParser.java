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

package com.ericsson.oss.apps.service.ncmp;

import static com.ericsson.oss.apps.util.Constants.COMMA;
import static com.ericsson.oss.apps.util.Constants.DN_PREFIX;
import static com.ericsson.oss.apps.util.Constants.EQUAL;
import static com.ericsson.oss.apps.util.Constants.MANAGED_ELEMENT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.apps.model.EaccManagedObject;
import com.ericsson.oss.apps.service.CMServiceException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Parser to parse result returned from NCMP into a list of {@link EaccManagedObject}.
 */
@Slf4j
public final class NcmpResultsParser {

    private String fdn;

    /**
     * Parses the result body returned from NCMP and creates a list of {@link EaccManagedObject}.
     *
     * @param body
     *            the result returned from ncmp
     * @param cmHandle
     *            a <code>String</code> representing the cmHandle of the node the body represents
     * @param attributesByMoType
     *            a <code>Map</code> representing the attributes by motype
     * @return a <code>List</code> of {@link EaccManagedObject}
     * @throws CMServiceException
     *             thrown if a problem occurs parsing the body.
     */
    public List<EaccManagedObject> parse(final Object body, final String cmHandle, final Map<String, List<String>> attributesByMoType)
            throws CMServiceException {
        final ObjectMapper mapper = new ObjectMapper();
        final Map<String, Object> jsonElements;
        try {
            jsonElements = mapper.readValue(mapper.writeValueAsString(body), new TypeReference<>() {
            });
        } catch (final IOException e) {
            log.error("Failed to parse results returned from ncmp for cmhandle '{}'", cmHandle);
            throw new CMServiceException(String.format("Failed to parse results returned from ncmp for cmhandle '%s'", cmHandle), e);
        }
        final List<EaccManagedObject> eaccManagedObjects = new ArrayList<>();
        buildEACCManagedObjects(jsonElements, eaccManagedObjects, "", cmHandle, attributesByMoType);
        return eaccManagedObjects;

    }

    private void buildEACCManagedObjects(final Map<String, Object> jsonElements, final List<EaccManagedObject> eaccManagedObjects,
            final String baseFdnForCurrentLevel, final String cmHandle, final Map<String, List<String>> attributesByMoType) {

        jsonElements.entrySet()
                .forEach(entry -> {
                    if (entry.getValue() instanceof Map) {
                        final Map<String, Object> map = (Map<String, Object>) entry.getValue();
                        buildEACCManagedObjects(map, eaccManagedObjects, baseFdnForCurrentLevel, cmHandle, attributesByMoType);
                    } else if (entry.getValue() instanceof List) {
                        final List<?> list = (List<?>) entry.getValue();
                        list.forEach(listEntry -> {
                            final EaccManagedObject eaccManagedObject = new EaccManagedObject();
                            eaccManagedObject.setMoType(entry.getKey());
                            eaccManagedObject.setCmHandle(cmHandle);
                            if (listEntry instanceof Map) {
                                final Map<String, Object> map = (Map<String, Object>) listEntry;
                                eaccManagedObject.setMoId(String.valueOf(map.get("id")));
                                eaccManagedObject.setAttributes((Map<String, Object>) map.get("attributes"));
                                buildFdn(baseFdnForCurrentLevel, eaccManagedObject);
                                eaccManagedObject.setFdn(fdn);
                                buildEACCManagedObjects(map, eaccManagedObjects, fdn, cmHandle, attributesByMoType);
                            }
                            addEaccManagedObjectToListIfDefinedInRules(eaccManagedObjects, attributesByMoType, eaccManagedObject);
                        });
                    }
                });

    }

    private void buildFdn(final String baseFdnForCurrentLevel, final EaccManagedObject eaccManagedObject) {
        //ManagedElement will be the first one in the tree so use its dnprefix to build fdn
        if (MANAGED_ELEMENT.equalsIgnoreCase(eaccManagedObject.getMoType())) {
            fdn = new StringBuilder()
                    .append(eaccManagedObject.getAttributes().get(DN_PREFIX)).append(COMMA)
                    .append(eaccManagedObject.getMoType()).append(EQUAL)
                    .append(eaccManagedObject.getMoId()).toString();
        } else {
            fdn = new StringBuilder(baseFdnForCurrentLevel).append(COMMA)
                    .append(eaccManagedObject.getMoType()).append(EQUAL)
                    .append(eaccManagedObject.getMoId()).toString();
        }
    }

    /**
     * Adds the {@link EaccManagedObject} to the <code>List</code> if it is defined in the attributes and removes the dnPrefix from ManagedElement via
     * {@link this#removeDnPrefixFromManagedElementIfNotDefinedInRules(Map, EaccManagedObject)} if applicable<br>
     * <b>Note: </b> Don't want to add MO's not in the attributes that are just returned in result for structure or are structs
     * 
     * @param eaccManagedObjects
     *            the <code>List</code> to add the {@link EaccManagedObject} too, if applicable
     * @param attributesByMoType
     *            a <code>Map</code> representing the rules by motype
     * @param eaccManagedObject
     *            the {@link EaccManagedObject} to add to the <code>List</code> if applicable
     */
    private void addEaccManagedObjectToListIfDefinedInRules(final List<EaccManagedObject> eaccManagedObjects,
            final Map<String, List<String>> attributesByMoType, final EaccManagedObject eaccManagedObject) {
        final String currentMoType = eaccManagedObject.getMoType();
        if (attributesByMoType.containsKey(currentMoType)) {
            if (MANAGED_ELEMENT.equals(currentMoType)) {
                removeDnPrefixFromManagedElementIfNotDefinedInRules(attributesByMoType, eaccManagedObject);
            }
            eaccManagedObjects.add(eaccManagedObject);
        }
    }

    /**
     * Removes the dnPrefix from the ManagedElement if it was appended to the options in {@link NcmpOptionsBuilder} when it is not defined in the
     * attributes. <br>
     * <b>Note:</b> Needed to do this for now as we need this for building up the Mo's fdn's due to lack of an identity service or node fdn user
     * input. The {@link NcmpResultsParser} will remove it again if it is not defined in the attributes.
     * 
     * @param attributesByMoType
     *            a <code>Map</code> representing the attributes by motype
     * @param eaccManagedObject
     *            the {@link EaccManagedObject} to remove the ManagedElement dn prefix from if applicable
     */
    private void removeDnPrefixFromManagedElementIfNotDefinedInRules(final Map<String, List<String>> attributesByMoType,
            final EaccManagedObject eaccManagedObject) {
        boolean dnPrefixFound = false;
        for (final String attribute : attributesByMoType.get(MANAGED_ELEMENT)) {
            if (DN_PREFIX.equals(attribute)) {
                dnPrefixFound = true;
                break;
            }
        }
        if (!dnPrefixFound) {
            eaccManagedObject.getAttributes().remove(DN_PREFIX);
        }
    }
}
