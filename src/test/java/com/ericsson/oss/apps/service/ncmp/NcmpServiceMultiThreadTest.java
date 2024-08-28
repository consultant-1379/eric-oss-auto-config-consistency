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

import static com.ericsson.assertions.EaccAssertions.assertThat;
import static com.ericsson.oss.apps.util.Constants.NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL;
import static com.ericsson.oss.apps.util.TestDefaults.CELL_CAP_MIN_CELL_SUB_CAP;
import static com.ericsson.oss.apps.util.TestDefaults.CELL_CAP_MIN_MAX_WRI_PROT;
import static com.ericsson.oss.apps.util.TestDefaults.CFRA_ENABLE;
import static com.ericsson.oss.apps.util.TestDefaults.ENODE_BFUNCTION;
import static com.ericsson.oss.apps.util.TestDefaults.EUTRAN_CELL_FDD;
import static com.ericsson.oss.apps.util.TestDefaults.PRACH_CONFIG_ENABLED;
import static com.ericsson.oss.apps.util.TestDefaults.SLASH;
import static com.ericsson.oss.apps.util.TestDefaults.ZZZ_TEMPORARY_74;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.support.RetryTemplate;

import com.ericsson.oss.apps.client.ncmp.NetworkCmProxyApi;
import com.ericsson.oss.apps.model.EaccManagedObject;
import com.ericsson.oss.apps.model.Rule;
import com.ericsson.oss.apps.service.MetricService;
import com.ericsson.oss.apps.util.RanUtilities;

/**
 * Unit tests for {@link NcmpService} class. <br>
 * <br>
 * Introduced to verify fix for [IDUN-98582] [EACC] FDN value incorrect when multiple response from NCMP are processed in parallel
 */
@ExtendWith(MockitoExtension.class)
class NcmpServiceMultiThreadTest {

    private static final String FDN_LTE04dg2ERBS00082 = "test,ManagedElement=LTE04dg2ERBS00082";
    private static final String FDN_LTE04dg2ERBS00083 = "test,ManagedElement=LTE04dg2ERBS00083";
    private static final String OPTIONS_STRING = "fields=ManagedElement/attributes(dnPrefix);ENodeBFunction/attributes(prachConfigEnabled;zzzTemporary74);EUtranCellFDD/attributes(cellCapMinCellSubCap;cellCapMinMaxWriProt;cfraEnable;commonCqiPeriodicity;ulVolteCovMobThr);";

    private final RetryTemplate retryTemplate = RetryTemplate.builder().maxAttempts(1).build();

    private NcmpService objectUnderTest;

    @Mock
    private NetworkCmProxyApi mockNetworkCmProxyApi;

    @Mock
    private MetricService mockMetricService;

    @BeforeEach
    public void setUp() {
        objectUnderTest = new NcmpService(mockNetworkCmProxyApi, mockMetricService, retryTemplate, 3);
    }

    @Test
    void whenGetEaccManagedObjectsIsCalled_100Times_For2FdnsAndRulesWithEnodeBFunctionAndEUtranCellFDD_thenNodeIsSuccessfullyRead_and4EaccManagedObjectsWithCorrectFdnsAndAttributesArePopulatedInTheBlockingQueueForEachRun()
            throws Exception {
        //expectations
        when(mockNetworkCmProxyApi.getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL,
                RanUtilities.convertFdnToCmHandle(FDN_LTE04dg2ERBS00082),
                SLASH,
                OPTIONS_STRING,
                null,
                false))
                        .thenReturn(NcmpResponseObjectBuilderUtil
                                .buildResponseObjectFromJsonFile(
                                        "ncmp_results_parser_test_files" +
                                                "/4g_LTE04dg2ERBS00082_ManagedElement_ENodeBFunction_EUtranCellFDD.json"));

        when(mockNetworkCmProxyApi.getResourceDataForCmHandle(NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL,
                RanUtilities.convertFdnToCmHandle(FDN_LTE04dg2ERBS00083),
                SLASH,
                OPTIONS_STRING,
                null,
                false))
                        .thenReturn(NcmpResponseObjectBuilderUtil
                                .buildResponseObjectFromJsonFile(
                                        "ncmp_results_parser_test_files" +
                                                "/4g_LTE04dg2ERBS00083_ManagedElement_ENodeBFunction_EUtranCellFDD.json"));

        final List<Rule> rules = createListOfRules();
        final Map<String, List<String>> attrByMoType = rules.stream()
                .collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        final Map<String, Object> enodeBFunctionAttributes = Map.of("timeAndPhaseSynchAlignment", "true",
                "rrcConnReestActive", "false");

        final Map<String, Object> eutranCellFDDAttributes = Map.of("ulVolteCovMobThr", "5");

        for (int i = 0; i < 100; i++) {
            final BlockingQueue<EaccManagedObject> eaccManagedObjectsQueue = new LinkedBlockingDeque<>(4);
            final List<String> fdns = List.of(FDN_LTE04dg2ERBS00082, FDN_LTE04dg2ERBS00083);
            final Map<String, Map<String, List<String>>> fdnToAttrByMoType = createFdnToAttrByMoType(fdns, attrByMoType);

            //execute
            final int count = objectUnderTest.populateEaccManagedObjects(fdnToAttrByMoType, eaccManagedObjectsQueue);

            assertThat(count).isEqualTo(fdns.size());
            assertThat(eaccManagedObjectsQueue).hasSize(4)
                    .extracting(EaccManagedObject::getMoId,
                            EaccManagedObject::getMoType, EaccManagedObject::getFdn, EaccManagedObject::getCmHandle, EaccManagedObject::getAttributes)
                    .containsExactlyInAnyOrder(
                            tuple("1", "ENodeBFunction", "test,ManagedElement=LTE04dg2ERBS00082,ENodeBFunction=1",
                                    RanUtilities.convertFdnToCmHandle(FDN_LTE04dg2ERBS00082), enodeBFunctionAttributes),
                            tuple("LTE04dg2ERBS00082-1", "EUtranCellFDD",
                                    "test,ManagedElement=LTE04dg2ERBS00082,ENodeBFunction=1,EUtranCellFDD=LTE04dg2ERBS00082-1",
                                    RanUtilities.convertFdnToCmHandle(FDN_LTE04dg2ERBS00082), eutranCellFDDAttributes),
                            tuple("1", "ENodeBFunction", "test,ManagedElement=LTE04dg2ERBS00083,ENodeBFunction=1",
                                    RanUtilities.convertFdnToCmHandle(FDN_LTE04dg2ERBS00083), enodeBFunctionAttributes),
                            tuple("LTE04dg2ERBS00083-1", "EUtranCellFDD",
                                    "test,ManagedElement=LTE04dg2ERBS00083,ENodeBFunction=1,EUtranCellFDD=LTE04dg2ERBS00083-1",
                                    RanUtilities.convertFdnToCmHandle(FDN_LTE04dg2ERBS00083), eutranCellFDDAttributes));

        }

    }

    private List<Rule> createListOfRules() {
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true", ""));
        rules.add(new Rule(ENODE_BFUNCTION, ZZZ_TEMPORARY_74, "-2000000000", ""));
        rules.add(new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "1000", ""));
        rules.add(new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_MAX_WRI_PROT, "true"));
        rules.add(new Rule(EUTRAN_CELL_FDD, CFRA_ENABLE, "true"));
        rules.add(new Rule(EUTRAN_CELL_FDD, "commonCqiPeriodicity", "80", ""));
        rules.add(new Rule(EUTRAN_CELL_FDD, "ulVolteCovMobThr", "5", ""));
        return rules;
    }

    private Map<String, Map<String, List<String>>> createFdnToAttrByMoType(final Collection fdns, final Map<String, List<String>> attrByMoType) {
        final Map<String, Map<String, List<String>>> fdnToAttrByMoType = new HashMap<>();
        for (final Object fdn : fdns) {
            fdnToAttrByMoType.put((String) fdn, attrByMoType);
        }
        return fdnToAttrByMoType;
    }

}