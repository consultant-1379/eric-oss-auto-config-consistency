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

import static com.ericsson.oss.apps.util.Constants.DN_PREFIX;
import static com.ericsson.oss.apps.util.Constants.MANAGEDELEMENT_FWD_SLASH_ATTRIBUTES;
import static com.ericsson.oss.apps.util.Constants.SEMICOLON;

import java.util.List;
import java.util.Map;

import com.ericsson.oss.apps.model.Rule;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A utility class to build the options String to be used in the NCMP query.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class NcmpOptionsBuilder {

    private static final String FIELDS_EQUALS = "fields=";

    public static String build(final Map<String, List<String>> attrByMoType) {
        final StringBuilder optionsBuilder = new StringBuilder();
        optionsBuilder.append(FIELDS_EQUALS);
        attrByMoType.forEach((mo, attributes) -> optionsBuilder.append(mo)
                .append("/attributes(")
                .append(String.join(SEMICOLON, attributes))
                .append(");"));
        updateOptionsWithManagedElementDnPrefix(optionsBuilder);
        return optionsBuilder.toString();
    }

    /**
     * Updates the options built from the rules with ManagedElement attribute dnPrefix if it doesn't already exist. <br>
     * <b>Note:</b> Need to do this for now as we need this for building up the Mo's fdn's in {@link NcmpResultsParser} due to lack of an identity
     * service or node fdn user input. The {@link NcmpResultsParser} will remove it again if it is not defined in the rules.
     * 
     * @param optionsBuilder
     *            A {@link StringBuilder} representing the options built from the {@link Rule}(s)
     */
    private static void updateOptionsWithManagedElementDnPrefix(final StringBuilder optionsBuilder) {
        final int indexOfManagedElementFwdSlashAttributes = optionsBuilder.indexOf(MANAGEDELEMENT_FWD_SLASH_ATTRIBUTES);
        if (isManagedElementNotPresent(indexOfManagedElementFwdSlashAttributes)) {
            insertManagedElementWithAttributeDnPrefixIntoOptions(optionsBuilder);
        } else {
            final int indexOfDnPrefix = optionsBuilder.indexOf(DN_PREFIX);
            if (isMissingDnPrefix(optionsBuilder, indexOfManagedElementFwdSlashAttributes, indexOfDnPrefix)) {
                insertAttributeDnPrefixIntoExistingManagedElementInOptions(optionsBuilder, indexOfManagedElementFwdSlashAttributes);
            }
        }
    }

    private static boolean isManagedElementNotPresent(final int indexOfManagedElementFwdSlashAttributes) {
        return indexOfManagedElementFwdSlashAttributes == -1;
    }

    private static boolean isMissingDnPrefix(final StringBuilder optionsBuilder,
            final int indexOfManagedElementFwdSlashAttributes,
            final int indexOfDnPrefix) {
        return (indexOfDnPrefix <= indexOfManagedElementFwdSlashAttributes
                || indexOfDnPrefix >= optionsBuilder.indexOf(")", indexOfManagedElementFwdSlashAttributes));
    }

    private static void insertManagedElementWithAttributeDnPrefixIntoOptions(final StringBuilder optionsBuilder) {
        optionsBuilder.insert(optionsBuilder.indexOf(FIELDS_EQUALS) + FIELDS_EQUALS.length(),
                MANAGEDELEMENT_FWD_SLASH_ATTRIBUTES + DN_PREFIX + ");");
    }

    private static void insertAttributeDnPrefixIntoExistingManagedElementInOptions(final StringBuilder optionsBuilder,
            final int indexOfManagedElementFwdSlashAttributes) {
        optionsBuilder.insert(indexOfManagedElementFwdSlashAttributes + MANAGEDELEMENT_FWD_SLASH_ATTRIBUTES.length(),
                DN_PREFIX + SEMICOLON);
    }
}
