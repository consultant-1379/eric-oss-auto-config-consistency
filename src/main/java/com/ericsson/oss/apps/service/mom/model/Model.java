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

import java.util.HashMap;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public class Model {
    String moduleName;
    String moduleVersion;
    Map<String, ModelObject> modelObjectsMapKeyedByName;

    public Model(final String moduleName, final String moduleVersion) {
        this.moduleName = moduleName;
        this.moduleVersion = moduleVersion;
        modelObjectsMapKeyedByName = new HashMap<>();
    }
}
