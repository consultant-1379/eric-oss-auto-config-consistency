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

package com.ericsson.oss.apps.service.exception;

import java.io.Serial;

public class InconsistencyProcessingFailedException extends Exception {

    @Serial
    private static final long serialVersionUID = 2573273236989118138L;

    public InconsistencyProcessingFailedException(final String errorMessage) {
        super(errorMessage);
    }
}
