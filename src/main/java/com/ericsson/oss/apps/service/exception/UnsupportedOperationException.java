/*******************************************************************************
 * COPYRIGHT Ericsson 2024
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

package com.ericsson.oss.apps.service.exception;

import java.io.Serial;

public class UnsupportedOperationException extends Exception {
    @Serial
    private static final long serialVersionUID = 8331715070847296333L;

    public UnsupportedOperationException(final String errorMessage) {
        super(errorMessage);
    }
}
