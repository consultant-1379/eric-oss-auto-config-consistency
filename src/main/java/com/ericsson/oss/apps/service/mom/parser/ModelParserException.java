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

package com.ericsson.oss.apps.service.mom.parser;

/**
 * Exception that is thrown from {@link ModelParser} if problems occur when parser the yang files and generating the eacc minified mom from them.
 */
public class ModelParserException extends Exception {
    private static final long serialVersionUID = -4631274688402761307L;

    public ModelParserException(final String errorMessage, final Exception error) {
        super(errorMessage, error);
    }

    public ModelParserException(final String errorMessage) {
        super(errorMessage);
    }
}
