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

package com.ericsson.oss.apps.service.mom.model;

import lombok.Data;

/**
 * Represents top level eacc minified mom attribute containing attributes common to all attribute types. <br>
 * <br>
 * <b>Note: </b> The different types of attributes i.e. leaf, leaf-list, container and list would extend this.
 */
@Data
public class AbstractModelAttribute {
    private String name;
    private String type;
}
