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

package com.ericsson.oss.apps.service;

/**
 * Exception that should be thrown for any problems that occur when retrieving CM information.
 */
public class CMServiceException extends Exception {
    private static final long serialVersionUID = 7227277113238119976L;

    public CMServiceException(final String errorMessage, final Throwable error) {
        super(errorMessage, error);
    }

    public CMServiceException(final String errorMessage) {
        super(errorMessage);
    }
}
