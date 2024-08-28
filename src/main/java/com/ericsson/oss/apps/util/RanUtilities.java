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

package com.ericsson.oss.apps.util;

import static com.ericsson.oss.apps.util.Constants.COLON;
import static com.ericsson.oss.apps.util.Constants.COMMA;
import static com.ericsson.oss.apps.util.Constants.EQUAL;
import static com.ericsson.oss.apps.util.Constants.MANAGED_ELEMENT;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.util.DigestUtils;

import com.ericsson.oss.apps.service.exception.UnsupportedOperationException;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class RanUtilities {

    public static String convertFdnToCmHandle(final String target) {
        return DigestUtils.md5DigestAsHex(
                String.format("EricssonENMAdapter-%s", target).getBytes(StandardCharsets.UTF_8))
                .toUpperCase(Locale.US);
    }

    public static String getFdnDownToManagedElement(final String target) {
        final String[] pairs = target.split(COMMA);
        final StringBuilder result = new StringBuilder();

        for (final String pair : pairs) {
            final String[] keyValue = pair.split(EQUAL);
            if (keyValue[0].equals(MANAGED_ELEMENT)) {
                result.append(pair);
                break;
            } else {
                result.append(pair).append(COMMA);
            }
        }
        return result.toString();
    }

    /**
     * Gets resourceIdentifier for the given moFdn and moType combination.
     * 
     * @param moFdn
     *            {@link String} representing the fdn of the mo to get a resourceIdentifier for
     * @param moType
     *            {@link String} representing the type of mo to get a resourceIdentifier for
     * @return a {@link String} representing the resourceIdentifier for the given moFdn and moType combination
     * @throws UnsupportedOperationException
     *             if module for mo function is currently not supported by EACC.
     */
    public static String fdnToResourceIdentifier(final String moFdn, final String moType) throws UnsupportedOperationException {

        // If its ManagedElement return the root resource identifier
        if (MANAGED_ELEMENT.equals(moType)) {
            return "/";
        }

        final String module = getModuleBasedOnFdn(moFdn);

        final String moFdnWithDnPrefixRemoved = moFdn.substring(moFdn.indexOf(",ManagedElement") + 1);

        return getReourceIdentifier(moType, moFdnWithDnPrefixRemoved, module);
    }

    private static String getModuleBasedOnFdn(final String moFdn) throws UnsupportedOperationException {
        final String module;
        if (moFdn.contains("ENodeBFunction")) {
            module = "ericsson-enm-Lrat";
        } else if (moFdn.contains("GNBDUFunction")) {
            module = "ericsson-enm-GNBDU";
        } else if (moFdn.contains("GNBCUCPFunction")) {
            module = "ericsson-enm-GNBCUCP";
        } else if (moFdn.contains("GNBCUUPFunction")) {
            module = "ericsson-enm-GNBCUUP";
        } else {
            final String message = String.format("Could not get module for moFdn as the function is not a currently supported type %s", moFdn);
            log.error(message);
            throw new UnsupportedOperationException(message);
        }
        return module;
    }

    private static String getReourceIdentifier(final String moType, final String moFdnWithDnPrefixRemoved, final String module) {
        final String[] splitFdn = moFdnWithDnPrefixRemoved.split(COMMA);
        final StringBuilder sb = new StringBuilder();

        for (final String item : splitFdn) {
            if (!item.startsWith(moType + EQUAL)) { // Don't include the moType itself
                if (item.startsWith(MANAGED_ELEMENT + EQUAL)) {
                    sb.append("/ericsson-enm-ComTop:");
                    sb.append(item.replace(EQUAL, "[@id="));
                    sb.append(']');
                } else {
                    sb.append('/');
                    sb.append(module);
                    sb.append(COLON);
                    sb.append(item.replace(EQUAL, "[@id="));
                    sb.append(']');
                }
            }
        }
        return sb.toString();
    }
}
