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

package com.ericsson.oss.apps.service.ncmp;

import static com.ericsson.oss.apps.util.Constants.COMMA;
import static com.ericsson.oss.apps.util.Constants.EQUAL;
import static com.ericsson.oss.apps.util.Constants.NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL;
import static com.ericsson.oss.apps.util.Constants.NCMP_DATASTORE_PASSTHROUGH_RUNNING;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_AUDIT_QUEUE_SUBMITTED_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.FAILED;
import static com.ericsson.oss.apps.util.MetricConstants.NCMP_READS_COMPLETED_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.NCMP_READS_SUBMITTED_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.STATUS;
import static com.ericsson.oss.apps.util.MetricConstants.SUCCEEDED;
import static com.ericsson.oss.apps.util.RanUtilities.convertFdnToCmHandle;
import static com.ericsson.oss.apps.util.RanUtilities.fdnToResourceIdentifier;
import static com.ericsson.oss.apps.util.RanUtilities.getFdnDownToManagedElement;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.ericsson.oss.apps.client.ncmp.NetworkCmProxyApi;
import com.ericsson.oss.apps.client.ncmp.model.CmHandleQueryParameters;
import com.ericsson.oss.apps.client.ncmp.model.ConditionProperties;
import com.ericsson.oss.apps.client.ncmp.model.RestModuleDefinition;
import com.ericsson.oss.apps.client.ncmp.model.RestModuleReference;
import com.ericsson.oss.apps.model.AuditResult;
import com.ericsson.oss.apps.model.EaccManagedObject;
import com.ericsson.oss.apps.service.CMService;
import com.ericsson.oss.apps.service.CMServiceException;
import com.ericsson.oss.apps.service.CMServiceRuntimeException;
import com.ericsson.oss.apps.service.MetricService;
import com.ericsson.oss.apps.service.exception.UnsupportedOperationException;
import com.ericsson.oss.apps.service.ncmp.model.CMHandleModuleRevision;
import com.ericsson.oss.apps.service.ncmp.model.NcmpChangeObj;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * A service class to handle the calls to NCMP.
 */
@Slf4j
@Service
public class NcmpService implements CMService {

    private static final String RESOURCE_IDENTIFIER = "/";
    private static long enqueueTimeout = 5;
    private static TimeUnit timeUnit = TimeUnit.MINUTES;
    private final NetworkCmProxyApi networkCmProxyApi;

    private final MetricService metricService;

    private final ExecutorService readMoExecutor;

    @Value("${modelDiscovery.threadPoolSize:4}")
    private int threadPoolSize;

    private final RetryTemplate retryTemplate;

    @Autowired
    public NcmpService(final NetworkCmProxyApi networkCmProxyApi, final MetricService metricService,
            @Qualifier("ncmpRetryTemplate") final RetryTemplate retryTemplate,
            @Value("${audit.ncmpReadMos.threadPoolSize:3}") final int readMosThreadPoolSize) {
        this.networkCmProxyApi = networkCmProxyApi;
        this.metricService = metricService;
        this.retryTemplate = retryTemplate;
        log.info("NCMP read MOs threadpool size is {}.", readMosThreadPoolSize);
        readMoExecutor = Executors.newFixedThreadPool(readMosThreadPoolSize);
    }

    /**
     * {@inheritDoc} Authentication towards NCMP is handled in {@link com.ericsson.oss.apps.config.client.TokenAuthenticationInterceptor}
     */
    @Override
    @Retryable(retryFor = {
            CMServiceException.class }, maxAttemptsExpression = "${modelDiscovery.retry.maxAttempts}", 
            backoff = @Backoff(delayExpression = "${modelDiscovery.retry.delay}", multiplierExpression = "${modelDiscovery.retry.delayMultiplier}",
                    maxDelayExpression = "${modelDiscovery.retry.maxDelay}"))
    public List<CMHandleModuleRevision> getCMHandleModuleRevisions(final List<String> requestedModuleNames) throws CMServiceException {
        log.info("Requesting Revisions for Requested Modules from NCMP.");
        final List<CMHandleModuleRevision> cmHandleModuleRevisionList;
        final ExecutorService customExecutor = Executors.newFixedThreadPool(threadPoolSize);

        try {
            final Set<String> cmHandles = getAllCmHandles(requestedModuleNames);
            final Map<String, List<RestModuleReference>> cmHandleToRMRMap = new HashMap<>();

            final List<CompletableFuture<Void>> futures = cmHandles.stream()
                    .map(cmHandle -> CompletableFuture.supplyAsync(() -> networkCmProxyApi.getModuleReferencesByCmHandle(cmHandle), customExecutor)
                            .thenAccept(restModuleRef -> cmHandleToRMRMap.put(cmHandle, restModuleRef)))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            cmHandleModuleRevisionList = cmHandleToRMRMap.entrySet().stream()
                    .flatMap(entry -> entry.getValue().stream()
                            .filter(rmr -> requestedModuleNames.contains(rmr.getModuleName()))
                            .map(rmr -> new CMHandleModuleRevision(entry.getKey(), rmr.getModuleName(), rmr.getRevision())))
                    .toList();

        } catch (final RestClientException e) {
            final String errorMessage = "Error encountered while retrieving CMHandle module revisions from NCMP.";
            log.error(errorMessage, e);
            throw new CMServiceException(errorMessage, e);
        } catch (final CompletionException e) {
            final String errorMessage = "Error encountered while retrieving CMHandle module revisions from NCMP.";
            log.error(errorMessage, e);
            if (e.getCause() instanceof RestClientException) {
                throw new CMServiceException(errorMessage, e);
            }
            throw e;
        } finally {
            customExecutor.shutdown();
        }

        log.info("Successfully retrieved {} CMHandle module revisions from NCMP.", cmHandleModuleRevisionList.size());
        return cmHandleModuleRevisionList;
    }

    /**
     * {@inheritDoc} Authentication towards NCMP is handled in {@link com.ericsson.oss.apps.config.client.TokenAuthenticationInterceptor}
     */
    @Override
    @Retryable(retryFor = {
            CMServiceException.class }, maxAttemptsExpression = "${modelDiscovery.retry.maxAttempts}", 
            backoff = @Backoff(delayExpression = "${modelDiscovery.retry.delay}", multiplierExpression = "${modelDiscovery.retry.delayMultiplier}",
                    maxDelayExpression = "${modelDiscovery.retry.maxDelay}"))
    public List<RestModuleDefinition> getModuleDefinitions(final String cmHandle) throws CMServiceException {
        log.info("Requesting module definitions from NCMP for CMHandle {}.", cmHandle);
        try {
            final List<RestModuleDefinition> moduleDefinitions = networkCmProxyApi.getModuleDefinitionsByCmHandleId(cmHandle);
            log.info("{} module definitions received from NCMP for the CMHandle {}", moduleDefinitions.size(), cmHandle);
            return moduleDefinitions;
        } catch (final RestClientException e) {
            final String errorMessage = String.format("Error encountered while retrieving module definitions from NCMP for CMHandle: %s", cmHandle);
            log.error(errorMessage, e);
            throw new CMServiceException(errorMessage, e);
        }
    }

    /**
     * {@inheritDoc} Authentication towards NCMP is handled in {@link com.ericsson.oss.apps.config.client.TokenAuthenticationInterceptor}
     */
    @Override
    @Retryable(retryFor = {
            CMServiceException.class }, maxAttemptsExpression = "${ncmpCalls.retry.maxAttempts}", 
            backoff = @Backoff(delayExpression = "${ncmpCalls.retry.delay}", multiplierExpression = "${ncmpCalls.retry.delayMultiplier}"))
    public int populateEaccManagedObjects(final Map<String, Map<String, List<String>>> fdnToAttributesByMoType,
            final BlockingQueue<EaccManagedObject> eaccManagedObjectsQueue) {
        log.info("Requesting objects from NCMP");

        return (int) fdnToAttributesByMoType.keySet().stream()
                .map(fdn -> submitReadMo(fdn, eaccManagedObjectsQueue, readMoExecutor,
                        fdnToAttributesByMoType.getOrDefault(fdn, Collections.emptyMap())))
                .map(getReadMoFuture())
                .filter(Objects::nonNull)
                .count();
    }

    private CompletableFuture<Void> submitReadMo(final String fdn, final BlockingQueue<EaccManagedObject> eaccManagedObjectsQueue,
            final ExecutorService readMoExecutor, final Map<String, List<String>> attrByMoType) {
        return CompletableFuture.runAsync(() -> {
            final String cmHandle = convertFdnToCmHandle(fdn);
            log.debug("Requesting objects for fdn: '{}' using cmHandle: '{}'", fdn, cmHandle);

            final String options = NcmpOptionsBuilder.build(attrByMoType);
            log.debug("Options built for the fdn: {}", options);

            retryTemplate.execute(callback -> {
                final Object result;
                try {
                    metricService.increment(NCMP_READS_SUBMITTED_TOTAL);

                    result = networkCmProxyApi.getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL,
                            cmHandle, RESOURCE_IDENTIFIER,
                            options, null, false);
                    log.debug("Resource Data returned for the given cmHandle: {}", result);

                    final List<EaccManagedObject> eaccManagedObjects = new NcmpResultsParser().parse(result, cmHandle, attrByMoType);
                    log.debug("Parser created {} EaccManagedObjects for fdn {}", eaccManagedObjects.size(), fdn);
                    log.debug("Parser created the following EaccManagedObjects {}", eaccManagedObjects);
                    if (eaccManagedObjects.isEmpty()) {
                        log.warn("No EaccManagedObjects object found for fdn '{}'", fdn);
                        return null;
                    }
                    final Instant start = Instant.now();
                    for (final EaccManagedObject eaccManagedObject : eaccManagedObjects) {
                        addToEaccManagedObjectsQueue(eaccManagedObjectsQueue, eaccManagedObject);
                    }
                    final Instant finish = Instant.now();
                    final long timeElapsed = Duration.between(start, finish).toMillis();
                    // Will be converted to a metric in the next commit i.e. before story closure
                    log.info("Enqueue Time: {} ms", timeElapsed);
                    metricService.increment(EACC_EXECUTIONS_AUDIT_QUEUE_SUBMITTED_TOTAL, eaccManagedObjects.size());
                } catch (final RestClientException e) {
                    final String errorMessage = getErrorMessage(cmHandle, fdn, e);
                    log.error(errorMessage, e);
                    throw new CMServiceRuntimeException(errorMessage, e);
                } catch (final InterruptedException e) {
                    log.error(e.getMessage(), e);
                    Thread.currentThread().interrupt();
                } catch (final Exception e) {
                    log.error(e.getMessage(), e);
                    throw new CMServiceRuntimeException(e);
                }
                return null;
            });
        }, readMoExecutor);
    }

    private void addToEaccManagedObjectsQueue(final BlockingQueue<EaccManagedObject> eaccManagedObjectsQueue,
                                              final EaccManagedObject eaccManagedObject) throws InterruptedException {
        if (! eaccManagedObjectsQueue.offer(eaccManagedObject, enqueueTimeout, timeUnit)) {
            log.error("Failed to add Managed Object '{}' to Queue", eaccManagedObject.getFdn());
        }
    }

    public static void setEnqueueTimeout(final long timeout, final TimeUnit timeUnit) {
        enqueueTimeout = timeout;
        NcmpService.timeUnit = timeUnit;
    }

    private String getErrorMessage(final String cmHandle, final String fdn, final RestClientException e) {
        String errorMessage = String.format("Failed to retrieve requested data from NCMP for cmHandle: '%s' and FDN: '%s' ", cmHandle, fdn);

        //Note: Need to handle these exception reasons in the interim until proper Rules validation comes in the LA time frame.
        //START
        final Optional<String> exceptionMessage = Optional.ofNullable(e.getMessage());
        if (exceptionMessage.isPresent()) {
            final var message = exceptionMessage.get();
            if (message.contains("ENM CM operation failed due to: '1010'")) {
                errorMessage = String.format(
                        "Invalid attribute defined for CmHandle '%s' and FDN: '%s'. Check node models for " +
                                "the applicable attributes.",
                        cmHandle, fdn);
            } else if (message.contains("ENM CM operation failed due to: '1012'")) {
                errorMessage = String.format("Invalid mo type defined for CmHandle '%s' and FDN: '%s'. Check node models for " +
                        "the applicable mo types.",
                        cmHandle, fdn);
            }
        }
        return errorMessage;
    }

    private Function<CompletableFuture<Void>, Boolean> getReadMoFuture() {
        return completableFuture -> {
            try {
                completableFuture.get();
                metricService.increment(NCMP_READS_COMPLETED_TOTAL, STATUS, SUCCEEDED);
                return true;
            } catch (final ExecutionException e) {
                log.warn(e.getMessage(), e);
                metricService.increment(NCMP_READS_COMPLETED_TOTAL, STATUS, FAILED);
            } catch (final InterruptedException e) {
                log.warn(e.getMessage(), e);
                metricService.increment(NCMP_READS_COMPLETED_TOTAL, STATUS, FAILED);
                Thread.currentThread().interrupt();
            }
            return null;
        };
    }

    /**
     * {@inheritDoc} Authentication towards NCMP is handled in the {@link com.ericsson.oss.apps.config.client.TokenAuthenticationInterceptor} class
     */
    @Override
    @Retryable(retryFor = {
            CMServiceException.class }, maxAttemptsExpression = "${ncmpCalls.retry.maxAttempts}", 
            backoff = @Backoff(delayExpression = "${ncmpCalls.retry.delay}", multiplierExpression = "${ncmpCalls.retry.delayMultiplier}"))
    public void patchManagedObject(final AuditResult auditResult) throws CMServiceException {
        log.info("Attempting to apply changes to the network.");
        final String moFdn = auditResult.getManagedObjectFdn();

        final String patchBody;
        try {
            patchBody = buildNCMPChangeRequestBody(moFdn, auditResult);
        } catch (final JsonProcessingException e) {
            final String errorMessage = String.format("Failed to build change request for FDN: %s.", moFdn);
            throw new CMServiceException(errorMessage, e);
        }

        final String resourceIdentifier;
        try {
            resourceIdentifier = fdnToResourceIdentifier(moFdn, auditResult.getManagedObjectType());
        } catch (final UnsupportedOperationException e) {
            throw new CMServiceException(e.getMessage(), e);
        }
        final String cmHandle = convertFdnToCmHandle(getFdnDownToManagedElement(moFdn));
        final Object result;
        try {
            result = networkCmProxyApi.patchResourceDataRunningForCmHandle(NCMP_DATASTORE_PASSTHROUGH_RUNNING, cmHandle,
                    resourceIdentifier, patchBody, null);
            log.debug("Result of push for given cmHandle: {}", result);
        } catch (final RestClientException e) {
            final String errorMessage = String.format("Failed to apply change in NCMP for FDN: %s.", moFdn);
            throw new CMServiceException(errorMessage, e);
        }

        log.info("Successfully applied changes to network.");
    }

    /**
     * {@inheritDoc} Authentication towards NCMP is handled in the {@link com.ericsson.oss.apps.config.client.TokenAuthenticationInterceptor} class
     */
    @Override
    @Retryable(retryFor = {
            CMServiceException.class }, maxAttemptsExpression = "${ncmpCalls.retry.maxAttempts}", 
            backoff = @Backoff(delayExpression = "${ncmpCalls.retry.delay}", multiplierExpression = "${ncmpCalls.retry.delayMultiplier}"))
    public void patchManagedObjects(final String moFdn, final List<AuditResult> auditResults, final Function<AuditResult, String> valueMapper)
            throws CMServiceException {
        log.info("Attempting to apply changes to the network.");

        final String patchBody;
        try {
            patchBody = buildPatchBody(moFdn, auditResults, valueMapper);
        } catch (final JsonProcessingException e) {
            final String errorMessage = String.format("Failed to build change request for FDN: %s.", moFdn);
            throw new CMServiceException(errorMessage, e);
        }

        final String resourceIdentifier;
        try {
            resourceIdentifier = fdnToResourceIdentifier(moFdn, auditResults.get(0).getManagedObjectType());
        } catch (final UnsupportedOperationException e) {
            throw new CMServiceException(e.getMessage(), e);
        }
        final String cmHandle = convertFdnToCmHandle(getFdnDownToManagedElement(moFdn));
        try {
            networkCmProxyApi.patchResourceDataRunningForCmHandle(NCMP_DATASTORE_PASSTHROUGH_RUNNING, cmHandle,
                    resourceIdentifier, patchBody, null);
            log.info("Successfully applied changes to FDN:{}", moFdn);
        } catch (final RestClientException e) {
            final String errorMessage = String.format("Failed to apply change in NCMP for FDN: %s.", moFdn);
            throw new CMServiceException(errorMessage, e);
        }

        log.info("Successfully applied changes to network.");
    }

    private Set<String> getAllCmHandles(final List<String> allowedModuleNames) {
        final List<CompletableFuture<List<String>>> futures = allowedModuleNames.stream()
                .map(module -> {
                    final CmHandleQueryParameters cmHandleQueryParameters = new CmHandleQueryParameters();
                    cmHandleQueryParameters.setCmHandleQueryParameters(List.of(getConditionProperties("hasAllModules", "moduleName", module)));
                    return CompletableFuture.supplyAsync(() -> networkCmProxyApi.searchCmHandleIds(cmHandleQueryParameters));
                })
                .toList();

        try {
            final Set<String> cmHandles = futures.stream()
                    .flatMap(future -> future.join().stream())
                    .collect(Collectors.toSet());

            log.info("Succesfully retrieved {} cmHandles from NCMP.", cmHandles.size());
            return cmHandles;
        } catch (final CompletionException e) {
            final String errorMessage = "Failed to retrieve cmHandles from NCMP for requested modules: " + allowedModuleNames;
            throw new RestClientException(errorMessage, e);
        }

    }

    private ConditionProperties getConditionProperties(final String conditionName, final String conditionParamKey, final String conditionParamValue) {
        final ConditionProperties conditionProperties = new ConditionProperties();
        conditionProperties.setConditionName(conditionName);
        final List<Map<String, String>> conditionParameters = new ArrayList<>();
        final Map<String, String> hashMap = new HashMap<>();
        hashMap.put(conditionParamKey, conditionParamValue);
        conditionParameters.add(hashMap);
        conditionProperties.setConditionParameters(conditionParameters);
        return conditionProperties;
    }

    /**
     * Method to build the NCMP Change Request for the audit results.
     *
     * @param moFdn
     *            The FDN of the MO
     * @param auditResult
     *            The audit result to convert
     * @return the ncmp change request in json format
     * @throws JsonProcessingException
     *             exception thrown if any problems occur
     */
    public String buildNCMPChangeRequestBody(final String moFdn, final AuditResult auditResult) throws JsonProcessingException {
        log.debug("Building NCMP Change Body Request for audit result: {}", auditResult.toString());
        // Identify the MO to patch
        final String[] splitFdn = moFdn.split(COMMA);
        final String moToPatch = splitFdn[splitFdn.length - 1];

        // Build attributes to be updated
        final Map<String, Object> attrToUpdate = new HashMap<>();
        attrToUpdate.put(auditResult.getAttributeName(), auditResult.getPreferredValue());

        // Build patch body object
        final NcmpChangeObj ncmpChangeObj = new NcmpChangeObj();
        final String[] splitMo = moToPatch.split(EQUAL);
        ncmpChangeObj.setId(splitMo[1]);
        ncmpChangeObj.setAttributes(attrToUpdate);

        final List<NcmpChangeObj> ncmpChangeObjList = List.of(ncmpChangeObj);
        final Map<String, List<NcmpChangeObj>> patchBody = Map.of(splitMo[0], ncmpChangeObjList);

        // Convert to JSON
        return toJsonString(patchBody);
    }

    /**
     * Method to build the NCMP Change Request for the audit results.
     *
     * @param moFdn
     *            The FDN of the MO
     * @param auditResults
     *            The {@link List} of audit results to convert
     * @param valueMapper
     *            The mapper to determine the preferred value
     * @return the ncmp change request in json format
     * @throws CMServiceException
     *             exception thrown if any problems occur
     * @throws JsonProcessingException
     *             exception thrown if any JSON problems occur
     */
    public String buildPatchBody(final String moFdn, final List<AuditResult> auditResults, final Function<AuditResult, String> valueMapper)
            throws CMServiceException, JsonProcessingException {
        // Identify the MO to patch
        final String[] splitFdn = moFdn.split(COMMA);
        if (splitFdn.length < 2 || splitFdn[0].isBlank()) {
            throw new CMServiceException(String.format("Could not identify MO from fdn {%s}", moFdn));
        }
        // Build attributes to be updated
        final Map<String, Object> attrToUpdate = new HashMap<>();
        for (final AuditResult audRes : auditResults) {
            if (!moFdn.equals(audRes.getManagedObjectFdn())) {
                throw new CMServiceException("Audit Results should be grouped by MO");
            }
            attrToUpdate.put(audRes.getAttributeName(), valueMapper.apply(audRes));
        }
        // Build patch body object
        final String dnToPatch = splitFdn[splitFdn.length - 1];
        final String[] splitDn = dnToPatch.split(EQUAL);
        if (splitDn.length < 2) {
            throw new CMServiceException(String.format("Could not identify cmHandle from fdn {%s}", dnToPatch));
        }
        final NcmpChangeObj ncmpObj = new NcmpChangeObj();
        ncmpObj.setId(splitDn[1]);
        ncmpObj.setAttributes(attrToUpdate);
        final List<NcmpChangeObj> ncmpObjectList = List.of(ncmpObj);
        final Map<String, List<NcmpChangeObj>> patchBody = Map.of(splitDn[0], ncmpObjectList);
        // Convert to JSON
        return toJsonString(patchBody);
    }

    private String toJsonString(final Map<String, List<NcmpChangeObj>> patchBody) throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.writeValueAsString(patchBody);
    }

}
