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

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * A Validation object to be used by the Validation Service. Represents if an object has been validated by the validation service and provides a
 * message about the validation and status code to be sent to the user.
 */
@Getter
@AllArgsConstructor
@ToString
public class ValidationObject {
    /**
     * Represents if the object being validated has passed validation or not.
     */
    private final Boolean validated;
    private final String details;
    private final String title;
    private final HttpStatus httpStatus;
}
