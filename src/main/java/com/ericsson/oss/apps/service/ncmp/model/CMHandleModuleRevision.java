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

package com.ericsson.oss.apps.service.ncmp.model;

import static com.ericsson.oss.apps.util.Constants.COLON;

import java.util.Objects;

public class CMHandleModuleRevision {

    private String cmHandle;
    private String moduleName;
    private String moduleRevision;

    public CMHandleModuleRevision(final String cmHandle, final String moduleName, final String moduleRevision) {
        this.cmHandle = cmHandle;
        this.moduleName = moduleName;
        this.moduleRevision = moduleRevision;
    }

    public String getCMHandle() {
        return cmHandle;
    }

    public void setCmHandle(final String cmHandle) {
        this.cmHandle = cmHandle;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(final String moduleName) {
        this.moduleName = moduleName;
    }

    public String getModuleRevision() {
        return moduleRevision;
    }

    public void setRevisions(final String revision) {
        moduleRevision = revision;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CMHandleModuleRevision that = (CMHandleModuleRevision) o;
        return Objects.equals(cmHandle, that.cmHandle) &&
                Objects.equals(moduleName, that.moduleName) &&
                Objects.equals(moduleRevision, that.moduleRevision);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cmHandle, moduleName, moduleRevision);
    }

    public String toNameAndRevision() {
        return getModuleName() + COLON + getModuleRevision();
    }
}
