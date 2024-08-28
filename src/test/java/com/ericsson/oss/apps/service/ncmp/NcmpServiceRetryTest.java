/*******************************************************************************
 * COPYRIGHT Ericsson 2024
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
import static com.ericsson.oss.apps.util.TestDefaults.MANAGED_ELEMENT;
import static com.ericsson.oss.apps.util.TestDefaults.MANAGED_ELEMENT_1_CM_HANDLE;
import static com.ericsson.oss.apps.util.TestDefaults.MANAGED_ELEMENT_1_FDN;
import static com.ericsson.oss.apps.util.TestDefaults.MANAGED_ELEMENT_2_CM_HANDLE;
import static com.ericsson.oss.apps.util.TestDefaults.MANAGED_ELEMENT_2_FDN;
import static com.ericsson.oss.apps.util.TestDefaults.PRACH_CONFIG_ENABLED;
import static com.ericsson.oss.apps.util.TestDefaults.SLASH;
import static com.ericsson.oss.apps.util.TestDefaults.ZZZ_TEMPORARY_74;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.LinkedBlockingDeque;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClientException;

import com.ericsson.oss.apps.client.ncmp.NetworkCmProxyApi;
import com.ericsson.oss.apps.model.AuditResult;
import com.ericsson.oss.apps.model.EaccManagedObject;
import com.ericsson.oss.apps.model.Rule;
import com.ericsson.oss.apps.service.CMServiceException;
import com.ericsson.oss.apps.service.MetricService;

/**
 * Unit tests for {@link NcmpService} class.
 */
@ActiveProfiles("test")
@SpringBootTest(properties = { "ncmpCalls.retry.maxAttempts=3", "ncmpCalls.retry.delay=100",
        "modelDiscovery.retry.maxAttempts=3", "modelDiscovery.retry.delay=100" })
class NcmpServiceRetryTest {

    private static final List<String> ALLOWED_MODULE_NAMES = List.of("ericsson-enm-Lrat", "ericsson-enm-GNBDU", "ericsson-enm-GNBCUCP",
            "ericsson-enm-GNBCUUP");
    private static final String OPTIONS_STRING = "fields=ManagedElement/attributes(dnPrefix);ENodeBFunction/attributes(prachConfigEnabled;zzzTemporary74);EUtranCellFDD/attributes(cellCapMinCellSubCap;cellCapMinMaxWriProt;cfraEnable;commonCqiPeriodicity;ulVolteCovMobThr);";
    private static Map<String, Object> lteNodeAndCell;
    @Value("${modelDiscovery.retry.maxAttempts}")
    private Integer modelDiscoveryMaxAttempts;
    @Autowired
    private NcmpService objectUnderTest;
    @MockBean
    private NetworkCmProxyApi mockNetworkCmProxyApi;
    @MockBean
    private MetricService mockMetricService;
    @Mock
    private RestClientException restClientExceptionMock;

    @BeforeAll
    static void beforeAll() throws IOException {
        lteNodeAndCell = NcmpResponseObjectBuilderUtil.buildResponseObjectFromJsonFile(
                "ncmp_results_parser_test_files/4g_ManagedElement_ENodeBFunction_EUtranCellFDD.json");
    }

    @Test
    void whenPopulateEaccManagedObjects_withNoErrorsFromNcmpApi_thenTwoMosReturnedAndNoRetriesAttempted() {
        when(mockNetworkCmProxyApi.getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL, MANAGED_ELEMENT_1_CM_HANDLE,
                SLASH, OPTIONS_STRING, null, false))
                .thenReturn(lteNodeAndCell);

        final List<Rule> rules = createListOfRules();
        final Map<String, List<String>> attrByMoType = rules.stream()
                .collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        final BlockingQueue<EaccManagedObject> eaccManagedObjectsQueue = new LinkedBlockingDeque<>(2);
        final Collection<String> fdns = Collections.singleton(MANAGED_ELEMENT_1_FDN);
        final Map<String, Map<String, List<String>>> fdnToAttrByMoTypeType = createFdnToAttrByMoType(fdns, attrByMoType);

        final int count = objectUnderTest.populateEaccManagedObjects(fdnToAttrByMoTypeType, eaccManagedObjectsQueue);

        assertThat(count).isEqualTo(fdns.size());
        assertThat(eaccManagedObjectsQueue).hasSize(2);

        verify(mockNetworkCmProxyApi, times(1)).getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL,
                convertFdnToCmHandle(MANAGED_ELEMENT_1_FDN),
                SLASH, OPTIONS_STRING, null, false);
    }

    @Test
    void whenPopulateEaccManagedObjects_withTwoErrorsFromNcmpApi_thenTwoMosReturnedAndTwoRetriesAttempted() {
        when(mockNetworkCmProxyApi.getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL, MANAGED_ELEMENT_1_CM_HANDLE,
                SLASH, OPTIONS_STRING, null, false))
                .thenThrow(restClientExceptionMock)
                .thenThrow(restClientExceptionMock)
                .thenReturn(lteNodeAndCell);

        final List<Rule> rules = createListOfRules();
        final Map<String, List<String>> attrByMoType = rules.stream()
                .collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        final BlockingQueue<EaccManagedObject> eaccManagedObjectsQueue = new LinkedBlockingDeque<>(2);
        final Collection<String> fdns = Collections.singleton(MANAGED_ELEMENT_1_FDN);
        final Map<String, Map<String, List<String>>> fdnToAttrByMoTypeType = createFdnToAttrByMoType(fdns, attrByMoType);

        final int count = objectUnderTest.populateEaccManagedObjects(fdnToAttrByMoTypeType, eaccManagedObjectsQueue);

        assertThat(count).isEqualTo(fdns.size());
        assertThat(eaccManagedObjectsQueue).hasSize(2);

        verify(mockNetworkCmProxyApi, times(3)).getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL,
                convertFdnToCmHandle(MANAGED_ELEMENT_1_FDN), SLASH, OPTIONS_STRING, null, false);
    }

    @Test
    void whenPopulateEaccManagedObjectsForTwoFdns_withErrorsFromNcmpApiForOneFdns_thenCorrectCountIsReturnedAndMosVerifiedForFdn() {
        when(mockNetworkCmProxyApi.getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL, MANAGED_ELEMENT_1_CM_HANDLE,
                SLASH, OPTIONS_STRING, null, false))
                .thenThrow(restClientExceptionMock)
                .thenThrow(restClientExceptionMock)
                .thenReturn(lteNodeAndCell);
        when(mockNetworkCmProxyApi.getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL, MANAGED_ELEMENT_2_CM_HANDLE,
                SLASH, OPTIONS_STRING, null, false))
                .thenThrow(restClientExceptionMock);

        final List<Rule> rules = createListOfRules();
        final Map<String, List<String>> attrByMoType = rules.stream()
                .collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        final BlockingQueue<EaccManagedObject> eaccManagedObjectsQueue = new LinkedBlockingDeque<>(2);
        final Collection<String> fdns = List.of(MANAGED_ELEMENT_1_FDN, MANAGED_ELEMENT_2_FDN);
        final Map<String, Map<String, List<String>>> fdnToAttrByMoTypeType = createFdnToAttrByMoType(fdns, attrByMoType);

        final int count = objectUnderTest.populateEaccManagedObjects(fdnToAttrByMoTypeType, eaccManagedObjectsQueue);

        assertThat(count).isOne();
        assertThat(eaccManagedObjectsQueue).hasSize(2);
        
        verify(mockNetworkCmProxyApi, times(3)).getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL,
                convertFdnToCmHandle(MANAGED_ELEMENT_1_FDN), SLASH, OPTIONS_STRING, null, false);

        verify(mockNetworkCmProxyApi, times(3)).getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL,
                convertFdnToCmHandle(MANAGED_ELEMENT_2_FDN), SLASH, OPTIONS_STRING, null, false);
    }

    @Test
    void whenPopulateEaccManagedObjectsForTwoFdns_withErrorsFromNcmpApiExceedMaxAttempts_thenCorrectCountIsReturnedAndMosVerifiedForFdn() {
        when(mockNetworkCmProxyApi.getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL, MANAGED_ELEMENT_1_CM_HANDLE,
                SLASH, OPTIONS_STRING, null, false))
                .thenThrow(restClientExceptionMock);

        final List<Rule> rules = createListOfRules();
        final Map<String, List<String>> attrByMoType = rules.stream()
                .collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        final BlockingQueue<EaccManagedObject> eaccManagedObjectsQueue = new LinkedBlockingDeque<>(2);
        final Collection<String> fdns = Collections.singleton(MANAGED_ELEMENT_1_FDN);
        final Map<String, Map<String, List<String>>> fdnToAttrByMoTypeType = createFdnToAttrByMoType(fdns, attrByMoType);

        final int count = objectUnderTest.populateEaccManagedObjects(fdnToAttrByMoTypeType, eaccManagedObjectsQueue);

        assertThat(count).isZero();
        assertThat(eaccManagedObjectsQueue).isEmpty();

        verify(mockNetworkCmProxyApi, times(3)).getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL,
                convertFdnToCmHandle(MANAGED_ELEMENT_1_FDN),
                SLASH, OPTIONS_STRING, null, false);
    }

    @Test
    void whenPatchManagedObjects_withErrorsFromNcmpApiThrownTwice_verifyRetriesAttempted() {
        when(mockNetworkCmProxyApi.patchResourceDataRunningForCmHandle(eq(NCMP_DATASTORE_PASSTHROUGH_RUNNING), eq(MANAGED_ELEMENT_1_CM_HANDLE),
                anyString(), any(), any()))
                .thenThrow(restClientExceptionMock)
                .thenThrow(restClientExceptionMock)
                .thenReturn(null);
        
        assertThatCode(()-> objectUnderTest.patchManagedObjects(MANAGED_ELEMENT_1_FDN, List.of(createAuditResult()), 
                        AuditResult::getPreferredValue))
                .doesNotThrowAnyException();
        
        verify(mockNetworkCmProxyApi, times(3))
                .patchResourceDataRunningForCmHandle(eq(NCMP_DATASTORE_PASSTHROUGH_RUNNING), eq(MANAGED_ELEMENT_1_CM_HANDLE),
                        anyString(), any(), any());
    }

    @Test
    void whenPatchManagedObjects_withErrorsFromNcmpApiExceedMaxAttempts_verifyRetriesAttemptedAndCmServiceExceptionIsThrown() {
        when(mockNetworkCmProxyApi.patchResourceDataRunningForCmHandle(eq(NCMP_DATASTORE_PASSTHROUGH_RUNNING), eq(MANAGED_ELEMENT_1_CM_HANDLE),
                anyString(), any(), any()))
                .thenThrow(restClientExceptionMock);

        assertThatCode(()-> objectUnderTest.patchManagedObjects(MANAGED_ELEMENT_1_FDN, List.of(createAuditResult()),
                        AuditResult::getPreferredValue))
                .isInstanceOf(CMServiceException.class)
                .hasCause(restClientExceptionMock);

        verify(mockNetworkCmProxyApi, times(3))
                .patchResourceDataRunningForCmHandle(eq(NCMP_DATASTORE_PASSTHROUGH_RUNNING), eq(MANAGED_ELEMENT_1_CM_HANDLE),
                        anyString(), any(), any());
    }

    @Test
    void whenGetModuleDefinitions_withErrorsFromNcmpApiThrownTwice_verifyRetriesAttempted() {
        when(mockNetworkCmProxyApi.getModuleDefinitionsByCmHandleId(MANAGED_ELEMENT_1_CM_HANDLE))
                .thenThrow(restClientExceptionMock)
                .thenThrow(restClientExceptionMock)
                .thenReturn(List.of());

        assertThatCode(()-> objectUnderTest.getModuleDefinitions(MANAGED_ELEMENT_1_CM_HANDLE))
                .doesNotThrowAnyException();

        verify(mockNetworkCmProxyApi, times(3)).getModuleDefinitionsByCmHandleId(MANAGED_ELEMENT_1_CM_HANDLE);
    }

    @Test
    void whenGetModuleDefinitions_withErrorsFromNcmpApiExceedMaxAttempts_verifyRetriesAttemptedAndCmServiceExceptionIsThrown() {
        when(mockNetworkCmProxyApi.getModuleDefinitionsByCmHandleId(MANAGED_ELEMENT_1_CM_HANDLE))
                .thenThrow(restClientExceptionMock);


        assertThatCode(()-> objectUnderTest.getModuleDefinitions(MANAGED_ELEMENT_1_CM_HANDLE))
                .isInstanceOf(CMServiceException.class)
                .hasCause(restClientExceptionMock);

        verify(mockNetworkCmProxyApi, times(3)).getModuleDefinitionsByCmHandleId(MANAGED_ELEMENT_1_CM_HANDLE);
    }

    @Test
    void whenCMHandleModuleRevisions_withNcmpApiThrownTwice_verifyRetriesAttempted() {
        when(mockNetworkCmProxyApi.searchCmHandleIds(any()))
                .thenThrow(restClientExceptionMock)
                .thenThrow(restClientExceptionMock)
                .thenThrow(restClientExceptionMock)
                .thenThrow(restClientExceptionMock)
                .thenThrow(restClientExceptionMock)
                .thenThrow(restClientExceptionMock)
                .thenThrow(restClientExceptionMock)
                .thenThrow(restClientExceptionMock)
                .thenReturn(List.of(MANAGED_ELEMENT_1_CM_HANDLE));
        when(mockNetworkCmProxyApi.getModuleReferencesByCmHandle(MANAGED_ELEMENT_1_CM_HANDLE))
                .thenReturn(List.of());
        
        assertThatCode(() -> objectUnderTest.getCMHandleModuleRevisions(ALLOWED_MODULE_NAMES))
                .doesNotThrowAnyException();

        verify(mockNetworkCmProxyApi, times(ALLOWED_MODULE_NAMES.size() * modelDiscoveryMaxAttempts))
                .searchCmHandleIds(any());
        verify(mockNetworkCmProxyApi, times(1)).getModuleReferencesByCmHandle(MANAGED_ELEMENT_1_CM_HANDLE);
    }

    @Test
    void whenCMHandleModuleRevisions_withSearchCmHandlesErrorsExceedsMaxAttempts_verifyRetriesAttempted() {
        when(mockNetworkCmProxyApi.searchCmHandleIds(any()))
                .thenThrow(restClientExceptionMock);

        assertThatCode(() -> objectUnderTest.getCMHandleModuleRevisions(ALLOWED_MODULE_NAMES))
                .isInstanceOf(CMServiceException.class);

        verify(mockNetworkCmProxyApi, times(ALLOWED_MODULE_NAMES.size() * modelDiscoveryMaxAttempts))
                .searchCmHandleIds(any());
        verify(mockNetworkCmProxyApi, never()).getModuleReferencesByCmHandle(any());
    }

    @Test
    void whenCMHandleModuleRevisions_withErrorsFromNcmpApiExceedMaxAttempts_verifyRetriesAttemptedAndCmServiceExceptionIsThrown() {
        when(mockNetworkCmProxyApi.searchCmHandleIds(any())).thenReturn(List.of(MANAGED_ELEMENT_1_CM_HANDLE));
        when(mockNetworkCmProxyApi.getModuleReferencesByCmHandle(MANAGED_ELEMENT_1_CM_HANDLE))
                .thenThrow(restClientExceptionMock);

        assertThatCode(() -> objectUnderTest.getCMHandleModuleRevisions(ALLOWED_MODULE_NAMES))
                .isInstanceOf(CMServiceException.class);

        verify(mockNetworkCmProxyApi, times(ALLOWED_MODULE_NAMES.size() * modelDiscoveryMaxAttempts))
                .searchCmHandleIds(any());
        verify(mockNetworkCmProxyApi, times(3)).getModuleReferencesByCmHandle(MANAGED_ELEMENT_1_CM_HANDLE);
    }

    @Test
    void whenCMHandleModuleRevisions_withErrorsFromNcmpIsRuntimeException_verifyNoRetriesAttemptedAndCmServiceExceptionIsThrown() {
        when(mockNetworkCmProxyApi.searchCmHandleIds(any())).thenReturn(List.of(MANAGED_ELEMENT_1_CM_HANDLE));
        when(mockNetworkCmProxyApi.getModuleReferencesByCmHandle(MANAGED_ELEMENT_1_CM_HANDLE))
                .thenThrow(RuntimeException.class);

        assertThatCode(() -> objectUnderTest.getCMHandleModuleRevisions(ALLOWED_MODULE_NAMES))
                .isInstanceOf(CompletionException.class);

        verify(mockNetworkCmProxyApi, times(ALLOWED_MODULE_NAMES.size()))
                .searchCmHandleIds(any());
        verify(mockNetworkCmProxyApi, times(1)).getModuleReferencesByCmHandle(MANAGED_ELEMENT_1_CM_HANDLE);
    }

    private AuditResult createAuditResult() {
        final AuditResult auditResult = new AuditResult();
        auditResult.setManagedObjectFdn(MANAGED_ELEMENT_1_FDN);
        auditResult.setManagedObjectType(MANAGED_ELEMENT);
        auditResult.setAttributeName("attribute");
        auditResult.setPreferredValue("new-value");
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

    private Map<String, Map<String, List<String>>> createFdnToAttrByMoType(final Collection<String> fdns,
            final Map<String, List<String>> attrByMoType) {
        final Map<String, Map<String, List<String>>> fdnToattrByMoTypeType = new HashMap<>();
        for (final String fdn : fdns) {
            fdnToattrByMoTypeType.put(fdn, attrByMoType);
        }
        return fdnToattrByMoTypeType;
    }

}