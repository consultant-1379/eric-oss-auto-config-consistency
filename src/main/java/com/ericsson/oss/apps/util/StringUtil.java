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

package com.ericsson.oss.apps.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StringUtil {

    public static boolean isNullOrBlank(final String string) {
        return string == null || string.isBlank();
    }

    public static boolean containsExactlyOnce(final String string, final String substring) {
        if (substring == null ||
                Constants.EMPTY_STRING.equals(substring) ||
                string == null ||
                !string.contains(substring)) {
            return false;
        }

        return string.indexOf(substring) == string.lastIndexOf(substring);
    }
}
