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

import static com.ericsson.oss.apps.util.Constants.NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL;
import static com.ericsson.oss.apps.util.Constants.NCMP_DATASTORE_PASSTHROUGH_RUNNING;
import static com.ericsson.oss.apps.util.RanUtilities.convertFdnToCmHandle;
import static com.ericsson.oss.apps.util.TestDefaults.CELL_CAP_MIN_CELL_SUB_CAP;
import static com.ericsson.oss.apps.util.TestDefaults.CELL_CAP_MIN_MAX_WRI_PROT;
import static com.ericsson.oss.apps.util.TestDefaults.CFRA_ENABLE;
import static com.ericsson.oss.apps.util.TestDefaults.ENODE_BFUNCTION;
import static com.ericsson.oss.apps.util.TestDefaults.EUTRAN_CELL_FDD;
import static com.ericsson.oss.apps.util.TestDefaults.INJECTED_LOG_TO_VALIDATE_TEST;
import static com.ericsson.oss.apps.util.TestDefaults.MANAGED_ELEMENT_1_CM_HANDLE;
import static com.ericsson.oss.apps.util.TestDefaults.MANAGED_ELEMENT_1_FDN;
import static com.ericsson.oss.apps.util.TestDefaults.MANAGED_ELEMENT_2_CM_HANDLE;
import static com.ericsson.oss.apps.util.TestDefaults.MANAGED_ELEMENT_2_FDN;
import static com.ericsson.oss.apps.util.TestDefaults.PRACH_CONFIG_ENABLED;
import static com.ericsson.oss.apps.util.TestDefaults.SLASH;
import static com.ericsson.oss.apps.util.TestDefaults.TEST_SETUP_ERROR_UNABLE_TO_ACCESS_LOGS;
import static com.ericsson.oss.apps.util.TestDefaults.ZZZ_TEMPORARY_74;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;

import com.ericsson.oss.apps.client.ncmp.NetworkCmProxyApi;
import com.ericsson.oss.apps.client.ncmp.model.CmHandleQueryParameters;
import com.ericsson.oss.apps.client.ncmp.model.RestModuleDefinition;
import com.ericsson.oss.apps.client.ncmp.model.RestModuleReference;
import com.ericsson.oss.apps.model.AuditResult;
import com.ericsson.oss.apps.model.EaccManagedObject;
import com.ericsson.oss.apps.model.Rule;
import com.ericsson.oss.apps.service.CMServiceException;
import com.ericsson.oss.apps.service.MetricService;
import com.ericsson.oss.apps.service.ncmp.model.CMHandleModuleRevision;
import com.ericsson.oss.apps.util.InMemoryLogAppender;
import com.fasterxml.jackson.core.JsonProcessingException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Unit tests for {@link NcmpService} class.
 */
@ExtendWith(MockitoExtension.class)
class NcmpServiceTest {
    private static final String OPTIONS_STRING = "fields=ManagedElement/attributes(dnPrefix);ENodeBFunction/attributes(prachConfigEnabled;zzzTemporary74);EUtranCellFDD/attributes(cellCapMinCellSubCap;cellCapMinMaxWriProt;cfraEnable;commonCqiPeriodicity;ulVolteCovMobThr);";
    private static final String CM_HANDLE_1 = "CMHandle1";
    private static final String CM_HANDLE_2 = "CMHandle2";
    private static final List<String> ALLOWED_MODULE_NAMES = List.of("ericsson-enm-Lrat", "ericsson-enm-GNBDU", "ericsson-enm-GNBCUCP",
            "ericsson-enm-GNBCUUP");
    private static final String FILE_CONTENT = "sample module content";
    private static final String INTERRUPTED_WHILE_ADDING_MESSAGE_TO_QUEUE = "Interrupted while adding message to Queue";

    private final RetryTemplate retryTemplate = RetryTemplate.builder().maxAttempts(1).build();

    private NcmpService objectUnderTest;

    @Mock
    private NetworkCmProxyApi mockNetworkCmProxyApi;

    @Mock
    private MetricService mockMetricService;

    @Mock
    private RestClientException restClientExceptionMock;

    @Mock
    private BlockingQueue<EaccManagedObject> mockEaccManagedObjectsQueue;

    private InMemoryLogAppender logAppender;

    @BeforeEach
    public void setUp() {
        objectUnderTest = new NcmpService(mockNetworkCmProxyApi, mockMetricService, retryTemplate, 3);

        final Logger logUnderTest = (Logger) LoggerFactory.getLogger(NcmpService.class);
        // need to set to at least WARN level so that we can assert that we get log out at the correct level
        logUnderTest.setLevel(Level.INFO);
        logAppender = new InMemoryLogAppender();
        logAppender.start();
        logUnderTest.addAppender(logAppender);
        logUnderTest.error(INJECTED_LOG_TO_VALIDATE_TEST);
    }

    @AfterEach
    public void checkLogsAreAccessible() {
        assertThat(logAppender.getLoggedEvents()).asString()
                .as(TEST_SETUP_ERROR_UNABLE_TO_ACCESS_LOGS)
                .contains(INJECTED_LOG_TO_VALIDATE_TEST);
    }

    @Test
    void whenGetEaccManagedObjectsIsCalledForRulesWithEnodeBFunctionAndEUtranCellFDD_EnsureCorrectMethodFlow_thenNodeIsSuccessfullyRead_andTwoEaccManagedObjectsWithCorrectAttributesArePopulatedInTheBlockingQueue()
            throws IOException {
        //expectations
        when(mockNetworkCmProxyApi.getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL, MANAGED_ELEMENT_1_CM_HANDLE,
                SLASH,
                OPTIONS_STRING,
                null,
                false))
                        .thenReturn(NcmpResponseObjectBuilderUtil
                                .buildResponseObjectFromJsonFile(
                                        "ncmp_results_parser_test_files/4g_ManagedElement_ENodeBFunction_EUtranCellFDD.json"));

        final List<Rule> rules = createListOfRules();
        final Map<String, List<String>> attrByMoType = rules.stream()
                .collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        final Map<String, Object> enodeBFunctionAttributes = new HashMap<>();
        enodeBFunctionAttributes.put("timeAndPhaseSynchAlignment", "true");
        enodeBFunctionAttributes.put("rrcConnReestActive", "false");

        final Map<String, Object> eutranCellFDDAttributes = new HashMap<>();
        eutranCellFDDAttributes.put("ulVolteCovMobThr", "5");

        final BlockingQueue<EaccManagedObject> eaccManagedObjectsQueue = new LinkedBlockingDeque<>(2);
        final Collection<String> fdns = Collections.singleton(MANAGED_ELEMENT_1_FDN);
        final Map<String, Map<String, List<String>>> fdnToAttrByMoTypeType = createFdnToAttrByMoType(fdns, attrByMoType);

        //execute
        final int count = objectUnderTest.populateEaccManagedObjects(fdnToAttrByMoTypeType, eaccManagedObjectsQueue);

        //verification
        assertThat(count).isEqualTo(fdns.size());
        assertThat(eaccManagedObjectsQueue).hasSize(2)
                .extracting(EaccManagedObject::getMoId, EaccManagedObject::getMoType, EaccManagedObject::getFdn, EaccManagedObject::getCmHandle,
                        EaccManagedObject::getAttributes)
                .containsExactlyInAnyOrder(
                        tuple("1", "ENodeBFunction", "test,ManagedElement=LTE04dg2ERBS00082,ENodeBFunction=1", MANAGED_ELEMENT_1_CM_HANDLE,
                                enodeBFunctionAttributes),
                        tuple("LTE04dg2ERBS00082-1", "EUtranCellFDD",
                                "test,ManagedElement=LTE04dg2ERBS00082,ENodeBFunction=1,EUtranCellFDD=LTE04dg2ERBS00082-1",
                                MANAGED_ELEMENT_1_CM_HANDLE,
                                eutranCellFDDAttributes));

        final InOrder inOrder = inOrder(mockNetworkCmProxyApi);

        inOrder.verify(mockNetworkCmProxyApi, times(1)).getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL,
                convertFdnToCmHandle(MANAGED_ELEMENT_1_FDN),
                SLASH, OPTIONS_STRING, null, false);
    }

    @Test
    void whenGetEaccManagedObjectsIsCalledWhereTheNodeHasNoManagedObjectMatchingTheRule_thenNoManagedObjectsAreAddToTheBlockingQueue_andTheCorrectWarningMessageIsLogged()
            throws IOException {
        //expectations
        when(mockNetworkCmProxyApi.getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL, MANAGED_ELEMENT_1_CM_HANDLE,
                SLASH,
                OPTIONS_STRING,
                null,
                false))
                        .thenReturn(NcmpResponseObjectBuilderUtil
                                .buildResponseObjectFromJsonFile(
                                        "ncmp_results_parser_test_files/4g_ManagedElement.json"));

        final List<Rule> rules = createListOfRules();
        final Map<String, List<String>> attrByMoType = rules.stream()
                .collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        final BlockingQueue<EaccManagedObject> eaccManagedObjectsQueue = new LinkedBlockingDeque<>(2);
        final Collection<String> fdns = Collections.singleton(MANAGED_ELEMENT_1_FDN);
        final Map<String, Map<String, List<String>>> fdnToAttrByMoTypeType = createFdnToAttrByMoType(fdns, attrByMoType);

        //execute
        final int count = objectUnderTest.populateEaccManagedObjects(fdnToAttrByMoTypeType, eaccManagedObjectsQueue);
        assertThat(count).isEqualTo(1);
        assertThat(eaccManagedObjectsQueue).isEmpty();

        logAppender.stop();
        final List<ILoggingEvent> loggedEvents = logAppender.getLoggedEvents();
        assertThat(loggedEvents).asString()
                .containsSequence(
                        "[WARN] No EaccManagedObjects object found for fdn '",
                        MANAGED_ELEMENT_1_FDN,
                        "'");
    }

    @Test
    void whenGetEaccManagedObjectsIsCalled_andNCMPThrowsARestClientExceptionDueToUnknownAttributeForOneOfTheTwoNodesBeingRead_thenOnlyOneNodeIsSuccessfullyRead_andTheBlockingQueueIsUpdatedWithTheCorrectManagedObjects()
            throws IOException {
        final List<Rule> rules = createListOfRules();
        final Map<String, List<String>> attrByMoTypeType = rules.stream()
                .collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));
        final BlockingQueue<EaccManagedObject> eaccManagedObjectsQueue = new LinkedBlockingDeque<>(2);
        final Collection<String> fdns = Arrays.asList(MANAGED_ELEMENT_1_FDN, MANAGED_ELEMENT_2_FDN);
        final Map<String, Map<String, List<String>>> fdnToAttrByMoTypeType = createFdnToAttrByMoType(fdns, attrByMoTypeType);

        when(mockNetworkCmProxyApi.getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL, MANAGED_ELEMENT_1_CM_HANDLE,
                SLASH,
                OPTIONS_STRING,
                null,
                false))
                .thenReturn(NcmpResponseObjectBuilderUtil
                        .buildResponseObjectFromJsonFile(
                                "ncmp_results_parser_test_files/4g_ManagedElement_ENodeBFunction_EUtranCellFDD.json"));
        when(mockNetworkCmProxyApi.getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL, MANAGED_ELEMENT_2_CM_HANDLE,
                SLASH,
                OPTIONS_STRING,
                null,
                false))
                .thenThrow(restClientExceptionMock);
        when(restClientExceptionMock.getMessage()).thenReturn(
                "502 Bad Gateway: \"{\"message\":\"Unable to read resource data.\"," +
                        "\"dmi-response\":{\"http-code\":500,\"body\":\"{\\\"status\\\":\\\"Internal Server " +
                        "Error\\\",\\\"message\\\":\\\"OPERATION_FAILED\\\",\\\"details\\\":\\\"ENM CM operation " +
                        "failed due to: '1010', 'An unknown attribute has been encountered; name doesNotExist in the " +
                        "MO class EUtranCellFDD. Verify valid attributes for the MO type in the latest models by executing the following cmedit describe command - \\\\\\\"cmedit describe EUtranCellFDD.*\\\\\\\"'\\\"}\"}}\"");

        final int count = objectUnderTest.populateEaccManagedObjects(fdnToAttrByMoTypeType, eaccManagedObjectsQueue);
        assertThat(count).isEqualTo(1);
        assertThat(eaccManagedObjectsQueue).hasSize(2);

        logAppender.stop();
        final List<ILoggingEvent> loggedEvents = logAppender.getLoggedEvents();
        assertThat(loggedEvents).asString()
                .containsSequence(
                        "[ERROR] Invalid attribute defined for CmHandle '",
                        MANAGED_ELEMENT_2_CM_HANDLE,
                        "' and FDN: '",
                        MANAGED_ELEMENT_2_FDN,
                        "'. Check node models for the applicable attributes.")
                .doesNotContain("doesNotExist");
    }

    @Test
    void whenGetEaccManagedObjectsIsCalled_andNCMPThrowsARestClientExceptionDueToInvalidMoType_thenTheBlockingQueueIsNotUpdated_andTheCorrectErrorMessageIsLogged() {

        final List<Rule> rules = createListOfRules();
        final Map<String, List<String>> attrByMoType = rules.stream()
                .collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        final BlockingQueue<EaccManagedObject> eaccManagedObjectsQueue = new LinkedBlockingDeque<>(2);
        final Collection<String> fdns = Collections.singleton(MANAGED_ELEMENT_1_FDN);
        final Map<String, Map<String, List<String>>> fdnToAttrByMoTypeType = createFdnToAttrByMoType(fdns, attrByMoType);

        when(mockNetworkCmProxyApi.getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL, MANAGED_ELEMENT_1_CM_HANDLE,
                SLASH,
                OPTIONS_STRING,
                null,
                false))
                .thenThrow(restClientExceptionMock);
        when(restClientExceptionMock.getMessage()).thenReturn(
                "502 Bad Gateway: \"{\"message\":\"Unable to read resource data.\"," +
                        "\"dmi-response\":{\"http-code\":500,\"body\":\"{\\\"status\\\":\\\"Internal Server " +
                        "Error\\\",\\\"message\\\":\\\"OPERATION_FAILED\\\",\\\"details\\\":\\\"ENM CM operation " +
                        "failed due to: '1012', 'Invalid MO type, doesNotExist not found Validate specified MO type. " +
                        " Use " +
                        "the <CMEDIT_DESCRIBE_LINK> to find valid MO types.'\\\"}\"}}\"");

        final int count = objectUnderTest.populateEaccManagedObjects(fdnToAttrByMoTypeType, eaccManagedObjectsQueue);
        assertThat(count).isEqualTo(0);
        assertThat(eaccManagedObjectsQueue).isEmpty();

        logAppender.stop();
        final List<ILoggingEvent> loggedEvents = logAppender.getLoggedEvents();
        assertThat(loggedEvents).asString()
                .containsSequence(
                        "[ERROR] Invalid mo type defined for CmHandle '",
                        MANAGED_ELEMENT_1_CM_HANDLE,
                        "' and FDN: '",
                        MANAGED_ELEMENT_1_FDN,
                        "'. Check node models for the applicable mo types.")
                .doesNotContain("doesNotExist");
    }

    @Test
    void whenGetEaccManagedObjectsIsCalled_andNCMPThrowsARestClientExceptionNotDueToInvalidAttributeOrMo_thenTheBlockingQueueIsNotUpdated_andTheCorrectErrorMessageIsLogged() {
        final List<Rule> rules = createListOfRules();
        final Map<String, List<String>> attrByMoType = rules.stream()
                .collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        final BlockingQueue<EaccManagedObject> eaccManagedObjectsQueue = new LinkedBlockingDeque<>(2);
        final Collection<String> fdns = Collections.singleton(MANAGED_ELEMENT_1_FDN);
        final Map<String, Map<String, List<String>>> fdnToAttrByMoTypeType = createFdnToAttrByMoType(fdns, attrByMoType);

        when(mockNetworkCmProxyApi.getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL, MANAGED_ELEMENT_1_CM_HANDLE,
                SLASH,
                OPTIONS_STRING,
                null,
                false))
                .thenThrow(restClientExceptionMock);
        when(restClientExceptionMock.getMessage()).thenReturn(
                "Any Other Exception Message");

        final int count = objectUnderTest.populateEaccManagedObjects(fdnToAttrByMoTypeType, eaccManagedObjectsQueue);
        assertThat(count).isEqualTo(0);
        assertThat(eaccManagedObjectsQueue).isEmpty();

        logAppender.stop();
        final List<ILoggingEvent> loggedEvents = logAppender.getLoggedEvents();
        assertThat(loggedEvents).asString()
                .containsSequence("[ERROR] Failed to retrieve requested data from NCMP for cmHandle: '",
                        MANAGED_ELEMENT_1_CM_HANDLE, "' and FDN: '", MANAGED_ELEMENT_1_FDN, "'");
    }

    @Test
    void whenGetEaccManagedObjectsIsCalled_andNCMPThrowsARestClientExceptionWithNullMessage_thenTheBlockingQueueIsNotUpdated_andTheCorrectErrorMessageIsLogged() {
        final List<Rule> rules = createListOfRules();
        final Map<String, List<String>> attrByMoType = rules.stream()
                .collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        final BlockingQueue<EaccManagedObject> eaccManagedObjectsQueue = new LinkedBlockingDeque<>(2);
        final Collection<String> fdns = Collections.singleton(MANAGED_ELEMENT_1_FDN);
        final Map<String, Map<String, List<String>>> fdnToAttrByMoTypeType = createFdnToAttrByMoType(fdns, attrByMoType);

        when(mockNetworkCmProxyApi.getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL, MANAGED_ELEMENT_1_CM_HANDLE,
                SLASH,
                OPTIONS_STRING,
                null,
                false))
                .thenThrow(restClientExceptionMock);
        when(restClientExceptionMock.getMessage()).thenReturn(
                null);

        final int count = objectUnderTest.populateEaccManagedObjects(fdnToAttrByMoTypeType, eaccManagedObjectsQueue);
        assertThat(count).isEqualTo(0);
        assertThat(eaccManagedObjectsQueue).isEmpty();

        logAppender.stop();
        final List<ILoggingEvent> loggedEvents = logAppender.getLoggedEvents();
        assertThat(loggedEvents).asString()
                .containsSequence(
                        "[ERROR] Failed to retrieve requested data from NCMP for cmHandle: '",
                        MANAGED_ELEMENT_1_CM_HANDLE,
                        "' and FDN: '",
                        MANAGED_ELEMENT_1_FDN,
                        "'");

    }

    @Test
    void whenGetEaccManagedObjectsIsCalled_andTheBlockingQueueIsFull_thenEnqueueTimesOutAfterWaiting()
            throws IOException {
        when(mockNetworkCmProxyApi.getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL, MANAGED_ELEMENT_1_CM_HANDLE,
                SLASH,
                OPTIONS_STRING,
                null,
                false))
                        .thenReturn(NcmpResponseObjectBuilderUtil
                                .buildResponseObjectFromJsonFile(
                                        "ncmp_results_parser_test_files/4g_ManagedElement_ENodeBFunction_EUtranCellFDD.json"));

        final List<Rule> rules = createListOfRules();
        final Map<String, List<String>> attrByMoType = rules.stream()
                .collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        // Note setting queue size to 1 so that when we try to add two ManagedObjects, only one is successful, triggering the test scenario
        final BlockingQueue<EaccManagedObject> eaccManagedObjectsQueue = new LinkedBlockingDeque<>(1);
        NcmpService.setEnqueueTimeout(5, TimeUnit.SECONDS);
        final Collection<String> fdns = Collections.singleton(MANAGED_ELEMENT_1_FDN);
        final Map<String, Map<String, List<String>>> fdnToAttrByMoTypeType = createFdnToAttrByMoType(fdns, attrByMoType);

        //execute
        final int count = objectUnderTest.populateEaccManagedObjects(fdnToAttrByMoTypeType, eaccManagedObjectsQueue);
        assertThat(count).isEqualTo(1);
        assertThat(eaccManagedObjectsQueue).hasSize(1);

        logAppender.stop();
        final List<ILoggingEvent> loggedEvents = logAppender.getLoggedEvents();
        // Look for the log produced when enqueue times out
        assertThat(loggedEvents).asString()
                .containsSequence(
                        "Failed to add Managed Object 'test,ManagedElement=LTE04dg2ERBS00082,ENodeBFunction=1' to Queue");
    }

    @Test
    void whenGetEaccManagedObjectsIsCalled_andInterruptedExceptionIsThrown_thenCorrectErrorMessageIsLogged()
            throws IOException, InterruptedException {
        when(mockNetworkCmProxyApi.getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL, MANAGED_ELEMENT_1_CM_HANDLE,
                SLASH,
                OPTIONS_STRING,
                null,
                false))
                .thenReturn(NcmpResponseObjectBuilderUtil
                        .buildResponseObjectFromJsonFile(
                                "ncmp_results_parser_test_files/4g_ManagedElement_ENodeBFunction_EUtranCellFDD.json"));
        when(mockEaccManagedObjectsQueue.offer(any(EaccManagedObject.class), anyLong(), any(TimeUnit.class))).
                thenThrow(new InterruptedException(INTERRUPTED_WHILE_ADDING_MESSAGE_TO_QUEUE));

        final List<Rule> rules = createListOfRules();
        final Map<String, List<String>> attrByMoType = rules.stream()
                .collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        // Note setting queue size to 1 so that when we try to add two ManagedObjects, only one is successful, triggering the test scenario
        final Collection<String> fdns = Collections.singleton(MANAGED_ELEMENT_1_FDN);
        final Map<String, Map<String, List<String>>> fdnToAttrByMoTypeType = createFdnToAttrByMoType(fdns, attrByMoType);

        //execute
        final int count = objectUnderTest.populateEaccManagedObjects(fdnToAttrByMoTypeType, mockEaccManagedObjectsQueue);

        logAppender.stop();
        final List<ILoggingEvent> loggedEvents = logAppender.getLoggedEvents();
        // Look for the log produced when enqueue times out
        assertThat(loggedEvents).asString()
                .containsSequence(INTERRUPTED_WHILE_ADDING_MESSAGE_TO_QUEUE);
    }

    @Test
    void whenGetCMHandleModuleRevisions_thenEnsureCorrectMethodFlow_andTheCorrectModuleRevisionsReturned() throws CMServiceException {
        ReflectionTestUtils.setField(objectUnderTest, "threadPoolSize", 4);

        final List<RestModuleReference> moduleReferences = Arrays.asList(
                new RestModuleReference("invalid", "dont return me"),
                new RestModuleReference("ericsson-enm-Lrat", "2110-01-20"),
                new RestModuleReference("ericsson-enm-GNBDU", "2110-02-20"),
                new RestModuleReference("ericsson-enm-GNBCUUP", "2110-03-20"),
                new RestModuleReference("ericsson-enm-GNBCUCP", "2110-04-20"));
        final List<String> cmHandles = new ArrayList<>(Arrays.asList("CMHandle1", "CMHandle2"));

        when(mockNetworkCmProxyApi.searchCmHandleIds(any(CmHandleQueryParameters.class))).thenReturn(cmHandles);
        when(mockNetworkCmProxyApi.getModuleReferencesByCmHandle(anyString())).thenReturn(moduleReferences);

        final List<CMHandleModuleRevision> cmHandleModuleRevisions = objectUnderTest.getCMHandleModuleRevisions(ALLOWED_MODULE_NAMES);

        assertThat(cmHandleModuleRevisions).hasSize(8);

        assertThat(cmHandleModuleRevisions)
                .extracting(CMHandleModuleRevision::getCMHandle, CMHandleModuleRevision::getModuleName, CMHandleModuleRevision::getModuleRevision)
                .contains(new Tuple(CM_HANDLE_2, "ericsson-enm-Lrat", "2110-01-20"));

        assertThat(cmHandleModuleRevisions)
                .extracting(CMHandleModuleRevision::getCMHandle, CMHandleModuleRevision::getModuleName, CMHandleModuleRevision::getModuleRevision)
                .contains(new Tuple(CM_HANDLE_2, "ericsson-enm-GNBDU", "2110-02-20"));

        assertThat(cmHandleModuleRevisions)
                .extracting(CMHandleModuleRevision::getCMHandle, CMHandleModuleRevision::getModuleName, CMHandleModuleRevision::getModuleRevision)
                .contains(new Tuple(CM_HANDLE_2, "ericsson-enm-GNBCUUP", "2110-03-20"));

        assertThat(cmHandleModuleRevisions)
                .extracting(CMHandleModuleRevision::getCMHandle, CMHandleModuleRevision::getModuleName, CMHandleModuleRevision::getModuleRevision)
                .contains(new Tuple(CM_HANDLE_2, "ericsson-enm-GNBCUCP", "2110-04-20"));

        assertThat(cmHandleModuleRevisions)
                .extracting(CMHandleModuleRevision::getCMHandle, CMHandleModuleRevision::getModuleName, CMHandleModuleRevision::getModuleRevision)
                .contains(new Tuple(CM_HANDLE_1, "ericsson-enm-Lrat", "2110-01-20"));

        assertThat(cmHandleModuleRevisions)
                .extracting(CMHandleModuleRevision::getCMHandle, CMHandleModuleRevision::getModuleName, CMHandleModuleRevision::getModuleRevision)
                .contains(new Tuple(CM_HANDLE_1, "ericsson-enm-GNBDU", "2110-02-20"));

        assertThat(cmHandleModuleRevisions)
                .extracting(CMHandleModuleRevision::getCMHandle, CMHandleModuleRevision::getModuleName, CMHandleModuleRevision::getModuleRevision)
                .contains(new Tuple(CM_HANDLE_1, "ericsson-enm-GNBCUUP", "2110-03-20"));

        assertThat(cmHandleModuleRevisions)
                .extracting(CMHandleModuleRevision::getCMHandle, CMHandleModuleRevision::getModuleName, CMHandleModuleRevision::getModuleRevision)
                .contains(new Tuple(CM_HANDLE_1, "ericsson-enm-GNBCUCP", "2110-04-20"));

        verify(mockNetworkCmProxyApi, times(4)).searchCmHandleIds(any(CmHandleQueryParameters.class));
        verify(mockNetworkCmProxyApi, times(2)).getModuleReferencesByCmHandle(anyString());
    }

    @Test
    void whenGetCMHandleModuleRevisionsIsCalled_andNCMPThrowsARestClientException_thenCMServiceExceptionIsThrownWithTheCorrectMessage() {
        ReflectionTestUtils.setField(objectUnderTest, "threadPoolSize", 4);
        when(mockNetworkCmProxyApi.searchCmHandleIds(any(CmHandleQueryParameters.class))).thenThrow(restClientExceptionMock);

        assertThatCode(() -> objectUnderTest.patchManagedObject(createAuditResult())).doesNotThrowAnyException();

        assertThatThrownBy(() -> objectUnderTest.getCMHandleModuleRevisions(ALLOWED_MODULE_NAMES))
                .isInstanceOf(CMServiceException.class)
                .hasMessage("Error encountered while retrieving CMHandle module revisions from NCMP.");
    }

    @Test
    void whenGetModuleDefinitionsIsCalled_thenCorrectModuleDefinitionsAreReturned() throws CMServiceException {
        when(mockNetworkCmProxyApi.getModuleDefinitionsByCmHandleId(CM_HANDLE_1)).thenReturn(createRestModuleDefinitions());
        final List<RestModuleDefinition> moduleDefinitions = objectUnderTest.getModuleDefinitions(CM_HANDLE_1);

        assertThat(moduleDefinitions)
                .extracting(RestModuleDefinition::getModuleName, RestModuleDefinition::getRevision, RestModuleDefinition::getContent)
                .contains(new Tuple("ericsson-enm-Lrat", "2110-01-20", FILE_CONTENT));
        assertThat(moduleDefinitions)
                .extracting(RestModuleDefinition::getModuleName, RestModuleDefinition::getRevision, RestModuleDefinition::getContent)
                .contains(new Tuple("ericsson-enm-GNBCUCP", "2110-02-20", FILE_CONTENT));
        assertThat(moduleDefinitions)
                .extracting(RestModuleDefinition::getModuleName, RestModuleDefinition::getRevision, RestModuleDefinition::getContent)
                .contains(new Tuple("ericsson-enm-GNBCUUP", "2110-03-20", FILE_CONTENT));
        assertThat(moduleDefinitions)
                .extracting(RestModuleDefinition::getModuleName, RestModuleDefinition::getRevision, RestModuleDefinition::getContent)
                .contains(new Tuple("ericsson-enm-GNBDU", "2110-04-20", FILE_CONTENT));
    }

    @Test
    void whenGetModuleDefinitionsIsCalled_andNcmpThrowsARestClientException_thenACMServiceExceptionIsThrownWithTheCorrectMessage() {
        when(mockNetworkCmProxyApi.getModuleDefinitionsByCmHandleId(CM_HANDLE_1)).thenThrow(RestClientException.class);
        assertThatThrownBy(() -> objectUnderTest.getModuleDefinitions(CM_HANDLE_1))
                .isInstanceOf(CMServiceException.class)
                .hasMessage("Error encountered while retrieving module definitions from NCMP for CMHandle: " + CM_HANDLE_1);
    }

    @Test
    void whenPatchMOIsCalled_andNcmpThrowsARestClientException_thenExceptionIsLogged_AndNoFurtherExceptionThrown() {
        when(mockNetworkCmProxyApi.patchResourceDataRunningForCmHandle(anyString(), anyString(), anyString(), anyString(), isNull()))
                .thenThrow(RestClientException.class);
        assertThatThrownBy(() -> objectUnderTest.patchManagedObject(createAuditResult()))
                .isInstanceOf(CMServiceException.class)
                .hasMessage("Failed to apply change in NCMP for FDN: SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW," +
                        "ManagedElement=LTE06dg2ERBS00005,ENodeBFunction=1,EUtranCellFDD=LTE06dg2ERBS00005-1.");
    }

    @Test
    void whenPatchMOIsCalled_andResultIsSuccessful_thenNoExceptionThrown() {
        when(mockNetworkCmProxyApi.patchResourceDataRunningForCmHandle(anyString(), anyString(), anyString(), anyString(), isNull()))
                .thenReturn(new Object());
        assertThatCode(() -> objectUnderTest.patchManagedObject(createAuditResult())).doesNotThrowAnyException();

        verify(mockNetworkCmProxyApi, times(1)).patchResourceDataRunningForCmHandle(NCMP_DATASTORE_PASSTHROUGH_RUNNING,
                "22AB8555DBCDDE90D1317F54DC3172A6", "/ericsson-enm-ComTop:ManagedElement[@id=LTE06dg2ERBS00005]/ericsson-enm-Lrat:ENodeBFunction[@id=1]",
                "{\"EUtranCellFDD\":[{\"id\":\"LTE06dg2ERBS00005-1\",\"attributes\":{\"userLabel\":\"new-value\"}}]}", null);
    }

    @Test
    void whenPatchMOsIsCalled_andNcmpThrowsARestClientException_thenExceptionIsLogged_AndNoFurtherExceptionThrown() {
        when(mockNetworkCmProxyApi.patchResourceDataRunningForCmHandle(anyString(), anyString(), anyString(), anyString(), isNull()))
                .thenThrow(RestClientException.class);
        final var auditResult = createAuditResult();
        assertThatThrownBy(
                () -> objectUnderTest.patchManagedObjects(auditResult.getManagedObjectFdn(), List.of(auditResult), AuditResult::getPreferredValue))
                        .isInstanceOf(CMServiceException.class)
                        .hasMessage("Failed to apply change in NCMP for FDN: SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW," +
                                "ManagedElement=LTE06dg2ERBS00005,ENodeBFunction=1,EUtranCellFDD=LTE06dg2ERBS00005-1.");
    }

    @Test
    void whenPatchMosIsCalled_andFdnsDiffer_thenExceptionThrown() {
        final var auditResult1 = createAuditResult();
        final var auditResult2 = new AuditResult();
        auditResult2.setManagedObjectFdn(auditResult1.getManagedObjectFdn() + "0");
        assertThatThrownBy(() -> objectUnderTest.patchManagedObjects(auditResult1.getManagedObjectFdn(), List.of(auditResult1, auditResult2),
                AuditResult::getPreferredValue))
                .isInstanceOf(CMServiceException.class)
                .hasMessage("Audit Results should be grouped by MO");
    }

    @Test
    void whenPatchMosIsCalled_andResultIsSuccessful_thenNoExceptionThrown() {
        when(mockNetworkCmProxyApi.patchResourceDataRunningForCmHandle(anyString(), anyString(), anyString(), anyString(), isNull()))
                .thenReturn(new Object());
        final var auditResult1 = createAuditResult();
        final var auditResult2 = new AuditResult();
        auditResult2.setManagedObjectFdn(auditResult1.getManagedObjectFdn());
        auditResult2.setAttributeName("userLabel2");
        auditResult2.setPreferredValue("new-value2");
        assertThatCode(() -> objectUnderTest.patchManagedObjects(auditResult1.getManagedObjectFdn(), List.of(auditResult1, auditResult2),
                AuditResult::getPreferredValue))
                        .doesNotThrowAnyException();
        verify(mockNetworkCmProxyApi, times(1)).patchResourceDataRunningForCmHandle(NCMP_DATASTORE_PASSTHROUGH_RUNNING,
                "22AB8555DBCDDE90D1317F54DC3172A6",
                "/ericsson-enm-ComTop:ManagedElement[@id=LTE06dg2ERBS00005]/ericsson-enm-Lrat:ENodeBFunction[@id=1]",
                "{\"EUtranCellFDD\":[{\"id\":\"LTE06dg2ERBS00005-1\",\"attributes\":{\"userLabel\":\"new-value\",\"userLabel2\":\"new-value2\"}}]}",
                null);
    }

    @Test
    void whenPatchMosIsCalled_andNCMPThrowsARestClientException_thenNoExceptionThrown() {
        when(mockNetworkCmProxyApi.patchResourceDataRunningForCmHandle(
                NCMP_DATASTORE_PASSTHROUGH_RUNNING,
                "22AB8555DBCDDE90D1317F54DC3172A6",
                "/ericsson-enm-ComTop:ManagedElement[@id=LTE06dg2ERBS00005]/ericsson-enm-Lrat:ENodeBFunction[@id=1]",
                "{\"EUtranCellFDD\":[{\"id\":\"LTE06dg2ERBS00005-1\",\"attributes\":{\"userLabel\":\"new-value\",\"userLabel2\":\"new-value2\"}}]}",
                null)).thenThrow(restClientExceptionMock);
        final var auditResult1 = createAuditResult();
        final var auditResult2 = new AuditResult();
        auditResult2.setManagedObjectFdn(auditResult1.getManagedObjectFdn());
        auditResult2.setAttributeName("userLabel2");
        auditResult2.setPreferredValue("new-value2");
        assertThatThrownBy(() -> objectUnderTest.patchManagedObjects(auditResult1.getManagedObjectFdn(), List.of(auditResult1, auditResult2),
                AuditResult::getPreferredValue))
                        .isInstanceOf(CMServiceException.class)
                        .hasMessage("Failed to apply change in NCMP for FDN: %s.", auditResult1.getManagedObjectFdn());
    }

    @Test
    void whenPatchMosIsCalledForNotSupportedFunction_thenCMExceptionThrown() {

        final var auditResult1 = createNotSupportedFunctionAuditResult();
        final var auditResult2 = new AuditResult();
        auditResult2.setManagedObjectFdn(auditResult1.getManagedObjectFdn());
        auditResult2.setAttributeName("userLabel2");
        auditResult2.setPreferredValue("new-value2");
        assertThatThrownBy(() -> objectUnderTest.patchManagedObjects(auditResult1.getManagedObjectFdn(), List.of(auditResult1, auditResult2),
                AuditResult::getPreferredValue))
                .isInstanceOf(CMServiceException.class)
                .hasMessage("Could not get module for moFdn as the function is not a currently supported type %s", auditResult1.getManagedObjectFdn());
    }

    @Test
    void whenBuildNCMPChangeRequestBodyWithValidFDN_thenExpectedPatchBodyIsReturned() throws JsonProcessingException {
        final String result = objectUnderTest.buildNCMPChangeRequestBody(MANAGED_ELEMENT_1_FDN, createAuditResult());
        assertThat(result).isEqualTo("{\"ManagedElement\":[{\"id\":\"NR03gNodeBRadio00030\",\"attributes\":{\"userLabel\":\"new-value\"}}]}");
    }

    @Test
    void whenBuildNCMPChangeRequestBodyWithInvalidFDN_thenExpectedPatchBodyIsReturned() {
        assertThatThrownBy(() -> objectUnderTest.buildNCMPChangeRequestBody(" ", createAuditResult()))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void whenBuildPatchBodyWithSingleInconsistencyWithValidFDN_thenExpectedPatchBodyIsReturned() throws JsonProcessingException, CMServiceException {
        final var auditResult = createAuditResult();
        final String result = objectUnderTest.buildPatchBody(auditResult.getManagedObjectFdn(), List.of(auditResult), AuditResult::getPreferredValue);
        assertThat(result).isEqualTo("{\"EUtranCellFDD\":[{\"id\":\"LTE06dg2ERBS00005-1\",\"attributes\":{\"userLabel\":\"new-value\"}}]}");
    }

    @Test
    void whenBuildPatchBodyWithMultipleInconsistenciesWithValidFDN_thenExpectedPatchBodyIsReturned()
            throws JsonProcessingException, CMServiceException {
        final var auditResult1 = createAuditResult();
        final var auditResult2 = new AuditResult();
        auditResult2.setManagedObjectFdn(auditResult1.getManagedObjectFdn());
        auditResult2.setAttributeName("userLabel2");
        auditResult2.setPreferredValue("new-value2");
        final String result = objectUnderTest.buildPatchBody(auditResult1.getManagedObjectFdn(), List.of(auditResult1, auditResult2),
                AuditResult::getPreferredValue);
        assertThat(result).isEqualTo(
                "{\"EUtranCellFDD\":[{\"id\":\"LTE06dg2ERBS00005-1\",\"attributes\":{\"userLabel\":\"new-value\",\"userLabel2\":\"new-value2\"}}]}");
    }

    @Test
    void whenBuildPatchBodyWithGetCurrentValueAsFunction_thenExpectedPatchBodyIsReturned2()
            throws JsonProcessingException, CMServiceException {
        final var auditResult1 = createAuditResult();
        final var auditResult2 = new AuditResult();
        auditResult2.setManagedObjectFdn(auditResult1.getManagedObjectFdn());
        auditResult2.setAttributeName("userLabel2");
        auditResult2.setCurrentValue("current-value2");
        final String result = objectUnderTest.buildPatchBody(auditResult1.getManagedObjectFdn(), List.of(auditResult1, auditResult2),
                AuditResult::getCurrentValue);
        assertThat(result).isEqualTo(
                "{\"EUtranCellFDD\":[{\"id\":\"LTE06dg2ERBS00005-1\",\"attributes\":{\"userLabel\":\"current-value\",\"userLabel2\":\"current-value2\"}}]}");
    }

    @Test
    void whenBuildPatchBodyWithMultipleInconsistenciesWithInvalidFDN_thenExpectedPatchBodyIsReturned() {
        final var auditResult1 = createAuditResult();
        final var auditResult2 = new AuditResult();
        auditResult2.setAttributeName("userLabel2");
        auditResult2.setPreferredValue("new-value2");
        assertThatThrownBy(() -> objectUnderTest.buildPatchBody(" ", List.of(auditResult1, auditResult2), AuditResult::getPreferredValue))
                .isInstanceOf(CMServiceException.class);
    }

    private AuditResult createAuditResult() {
        final AuditResult auditResult = new AuditResult();
        auditResult.setAttributeName("userLabel");
        auditResult.setPreferredValue("new-value");
        auditResult.setCurrentValue("current-value");
        auditResult.setManagedObjectType("EUtranCellFDD");
        auditResult.setManagedObjectFdn("SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW," +
                "ManagedElement=LTE06dg2ERBS00005,ENodeBFunction=1,EUtranCellFDD=LTE06dg2ERBS00005-1");
        return auditResult;
    }

    private AuditResult createNotSupportedFunctionAuditResult() {
        final AuditResult auditResult = new AuditResult();
        auditResult.setAttributeName("userLabel");
        auditResult.setPreferredValue("new-value");
        auditResult.setCurrentValue("current-value");
        auditResult.setManagedObjectType("SwVersion");
        auditResult.setManagedObjectFdn("SubNetwork=ONRM_ROOT_MO,SubNetwork=Tampa,MeContext=MA2H3009A2_50k_sim,ManagedElement=MA2H3009A2_50k_sim,SystemFunctions=1,SwInventory=1,SwVersion=10");
        return auditResult;


    }

    private List<Rule> createListOfRules() {
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true", ""));
        rules.add(new Rule(ENODE_BFUNCTION, ZZZ_TEMPORARY_74, "-2000000000", ""));
        rules.add(new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "1000", ""));
        rules.add(new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_MAX_WRI_PROT, "true", ""));
        rules.add(new Rule(EUTRAN_CELL_FDD, CFRA_ENABLE, "true", ""));
        rules.add(new Rule(EUTRAN_CELL_FDD, "commonCqiPeriodicity", "80"));
        rules.add(new Rule(EUTRAN_CELL_FDD, "ulVolteCovMobThr", "5", ""));
        return rules;
    }

    private List<RestModuleDefinition> createRestModuleDefinitions() {
        final List<RestModuleDefinition> restModuleDefinitions = new ArrayList<>();
        restModuleDefinitions.add(new RestModuleDefinition("ericsson-enm-Lrat", "2110-01-20", FILE_CONTENT));
        restModuleDefinitions.add(new RestModuleDefinition("ericsson-enm-GNBCUCP", "2110-02-20", FILE_CONTENT));
        restModuleDefinitions.add(new RestModuleDefinition("ericsson-enm-GNBCUUP", "2110-03-20", FILE_CONTENT));
        restModuleDefinitions.add(new RestModuleDefinition("ericsson-enm-GNBDU", "2110-04-20", FILE_CONTENT));
        return restModuleDefinitions;
    }

    private Map<String, Map<String, List<String>>> createFdnToAttrByMoType(final Collection<String> fdns,
            final Map<String, List<String>> attrByMoType) {
        final Map<String, Map<String, List<String>>> fdnToattrByMoTypeType = new HashMap<>();
        for (final String fdn : fdns) {
            fdnToattrByMoTypeType.put(fdn, attrByMoType);
        }
        return fdnToattrByMoTypeType;
    }

}