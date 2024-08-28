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

package com.ericsson.oss.apps.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.ericsson.oss.apps.client.ncmp.model.RestModuleDefinition;
import com.ericsson.oss.apps.model.AuditResult;
import com.ericsson.oss.apps.model.EaccManagedObject;
import com.ericsson.oss.apps.service.ncmp.model.CMHandleModuleRevision;

/**
 * CMService interface.
 */
@Service
public interface CMService {

    /**
     * Populate the provided queue with the EaccManagedObjects based on a given FDNs and attributes.
     *
     * @param fdnToAttributesByMoType
     *            the attributes specifically for FDN grouped by MO type
     * @param eaccManagedObjectsQueue
     *            the queue to be populated
     * @return a count of the node FDN successfully read from NCMP.
     */
    int populateEaccManagedObjects(Map<String, Map<String, List<String>>> fdnToAttributesByMoType,
            BlockingQueue<EaccManagedObject> eaccManagedObjectsQueue);

    /**
     * Returns a list of revisions, mapped to the list of modules given.
     *
     * @param requestedModuleNames
     *            the modules we're interested in
     * @return {@link List} of {@link CMHandleModuleRevision}
     * @throws CMServiceException
     *             exception thrown if any problems occur
     */
    List<CMHandleModuleRevision> getCMHandleModuleRevisions(List<String> requestedModuleNames) throws CMServiceException;

    /**
     * Returns a list of module definitions for a given cmHandle.
     *
     * @param cmHandle
     *            The cmHandle to get module definitions for.
     * @return {@link List} of {@link RestModuleDefinition}
     * @throws CMServiceException
     *             exception thrown if any problems occur
     */
    List<RestModuleDefinition> getModuleDefinitions(String cmHandle) throws CMServiceException;

    /**
     * Sends the proposed changes down to NCMP.
     *
     * @deprecated Use {@link #patchManagedObjects(String, List, Function)}
     * @param auditResult
     *            An {@link AuditResult}
     * @throws CMServiceException
     *             exception thrown if any problems occur
     */
    @Deprecated
    void patchManagedObject(AuditResult auditResult) throws CMServiceException;

    /**
     * Sends the proposed changes for a specific MO FDN down to NCMP.
     *
     * @param fdn
     *            The FDN of the MO
     * @param auditResults
     *            A {@link List} of {@link AuditResult}s
     * @param valueMapper
     *            A {@link Function} to map {@link AuditResult}s to the desired value
     * @throws CMServiceException
     *             exception thrown if any problems occur
     */
    void patchManagedObjects(String fdn, List<AuditResult> auditResults, Function<AuditResult, String> valueMapper) throws CMServiceException;
}
