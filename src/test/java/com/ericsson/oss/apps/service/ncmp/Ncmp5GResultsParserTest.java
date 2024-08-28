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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ericsson.oss.apps.model.EaccManagedObject;
import com.ericsson.oss.apps.model.Rule;

/**
 * Unit tests for {@link NcmpResultsParser} class. <br>
 * This tests the parser when the result returned from ncmp is for a 5g node using the files 5g_ManagedElement.json,
 * 5g_ManagedElement_GNBDUFunction.json, 5g_ManagedElement_GNBDUFunction_NRCellDU.json, 5g_ManagedElement_GNBDUFunction_3NRCellDU.json and
 * 5g_ManagedElement_GNBCUCPFunction_EUtraNetwork_ExternalENodeBFunction_ExternalEUtranCell.json to mimic the result returned from ncmp.
 * 
 * @see Ncmp4GResultsParserTest
 * @see Ncmp4G5GResultsParserTest
 * @see Ncmp5GResultsParserForNodeWithNRCellWithStructAttributeTest
 */
class Ncmp5GResultsParserTest {

    private static final String TEST_CM_HANDLE = "E3C5531A165CA4A9AEE417A6E671A6E2";

    @Test
    void whenParseManagedElement_thenCorrectEaccManagedObjectReturned() throws Exception {
        //rules defined
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule("ManagedElement", "dnPrefix", "test", ""));
        final Map<String, List<String>> attrByMoType = rules.stream().collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        //expectations
        final Map<String, Object> managedElementAttributes = new HashMap<>();
        managedElementAttributes.put("dnPrefix", "test");

        //execute
        final List<EaccManagedObject> eaccManagedObjects = new NcmpResultsParser().parse(
                NcmpResponseObjectBuilderUtil.buildResponseObjectFromJsonFile("ncmp_results_parser_test_files/5g_ManagedElement.json"),
                TEST_CM_HANDLE, attrByMoType);

        //verification
        assertThat(eaccManagedObjects).hasSize(1)
                .extracting(EaccManagedObject::getMoId, EaccManagedObject::getMoType, EaccManagedObject::getFdn, EaccManagedObject::getCmHandle,
                        EaccManagedObject::getAttributes)
                .containsExactlyInAnyOrder(
                        tuple("NR01gNodeBRadio00007", "ManagedElement", "test,ManagedElement=NR01gNodeBRadio00007", TEST_CM_HANDLE,
                                managedElementAttributes));
    }

    @Test
    void whenParseManagedElementWithGNBDUFunctionChild_thenCorrectEaccManagedObjectsReturned() throws Exception {
        //rules defined
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule("ManagedElement", "dnPrefix", "test", ""));
        rules.add(new Rule("GNBDUFunction", "endpointResDepHEnabled", "true", ""));
        final Map<String, List<String>> attrByMoType = rules.stream().collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        //expectations
        final Map<String, Object> managedElementAttributes = new HashMap<>();
        managedElementAttributes.put("dnPrefix", "test");

        final Map<String, Object> gnbduFunctionAttributes = new HashMap<>();
        gnbduFunctionAttributes.put("endpointResDepHEnabled", "true");

        //execute
        final List<EaccManagedObject> eaccManagedObjects = new NcmpResultsParser().parse(
                NcmpResponseObjectBuilderUtil.buildResponseObjectFromJsonFile("ncmp_results_parser_test_files/5g_ManagedElement_GNBDUFunction.json"),
                TEST_CM_HANDLE, attrByMoType);

        //verification
        assertThat(eaccManagedObjects).hasSize(2)
                .extracting(EaccManagedObject::getMoId, EaccManagedObject::getMoType, EaccManagedObject::getFdn, EaccManagedObject::getCmHandle,
                        EaccManagedObject::getAttributes)
                .containsExactlyInAnyOrder(
                        tuple("NR01gNodeBRadio00007", "ManagedElement", "test,ManagedElement=NR01gNodeBRadio00007", TEST_CM_HANDLE,
                                managedElementAttributes),
                        tuple("1", "GNBDUFunction", "test,ManagedElement=NR01gNodeBRadio00007,GNBDUFunction=1", TEST_CM_HANDLE,
                                gnbduFunctionAttributes));
    }

    @Test
    void whenParseManagedElementWithGNBDUFunctionChildAndNRCellDUGrandChild_thenCorrectEaccManagedObjectsReturned() throws Exception {
        //rules defined
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule("ManagedElement", "dnPrefix", "test", ""));
        rules.add(new Rule("GNBDUFunction", "endpointResDepHEnabled", "true", ""));
        rules.add(new Rule("NRCellDU", "drxEnable", "true", ""));
        rules.add(new Rule("NRCellDU", "csiRsShiftingPrimary", "DEACTIVATED", ""));

        final Map<String, List<String>> attrByMoType = rules.stream().collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        //expectations
        final Map<String, Object> managedElementAttributes = new HashMap<>();
        managedElementAttributes.put("dnPrefix", "test");

        final Map<String, Object> gnbduFunctionAttributes = new HashMap<>();
        gnbduFunctionAttributes.put("endpointResDepHEnabled", "true");

        final Map<String, Object> nrCellDUAttributes = new HashMap<>();
        nrCellDUAttributes.put("drxEnable", "true");
        nrCellDUAttributes.put("csiRsShiftingPrimary", "DEACTIVATED");

        //execute
        final List<EaccManagedObject> eaccManagedObjects = new NcmpResultsParser().parse(
                NcmpResponseObjectBuilderUtil
                        .buildResponseObjectFromJsonFile("ncmp_results_parser_test_files/5g_ManagedElement_GNBDUFunction_NRCellDU.json"),
                TEST_CM_HANDLE, attrByMoType);

        //verification
        assertThat(eaccManagedObjects).hasSize(3)
                .extracting(EaccManagedObject::getMoId, EaccManagedObject::getMoType, EaccManagedObject::getFdn, EaccManagedObject::getCmHandle,
                        EaccManagedObject::getAttributes)
                .containsExactlyInAnyOrder(
                        tuple("NR01gNodeBRadio00007", "ManagedElement", "test,ManagedElement=NR01gNodeBRadio00007", TEST_CM_HANDLE,
                                managedElementAttributes),
                        tuple("1", "GNBDUFunction", "test,ManagedElement=NR01gNodeBRadio00007,GNBDUFunction=1", TEST_CM_HANDLE,
                                gnbduFunctionAttributes),
                        tuple("NR01gNodeBRadio00007-1", "NRCellDU",
                                "test,ManagedElement=NR01gNodeBRadio00007,GNBDUFunction=1,NRCellDU=NR01gNodeBRadio00007-1", TEST_CM_HANDLE,
                                nrCellDUAttributes));
    }

    @Test
    void whenParseManagedElementWithGNBDUFunctionChildAnd3NRCellDUGrandChild_thenCorrectEaccManagedObjectsReturned() throws Exception {
        //rules defined
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule("ManagedElement", "dnPrefix", "test", ""));
        rules.add(new Rule("GNBDUFunction", "endpointResDepHEnabled", "true", ""));
        rules.add(new Rule("NRCellDU", "drxEnable", "true", ""));
        rules.add(new Rule("NRCellDU", "csiRsShiftingPrimary", "DEACTIVATED", ""));
        final Map<String, List<String>> attrByMoType = rules.stream().collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        //expectations
        final Map<String, Object> managedElementAttributes = new HashMap<>();
        managedElementAttributes.put("dnPrefix", "test");

        final Map<String, Object> gnbduFunctionAttributes = new HashMap<>();
        gnbduFunctionAttributes.put("endpointResDepHEnabled", "true");

        final Map<String, Object> nrCellDUAttributes = new HashMap<>();
        nrCellDUAttributes.put("drxEnable", "true");
        nrCellDUAttributes.put("csiRsShiftingPrimary", "DEACTIVATED");

        //execute
        final List<EaccManagedObject> eaccManagedObjects = new NcmpResultsParser().parse(
                NcmpResponseObjectBuilderUtil
                        .buildResponseObjectFromJsonFile("ncmp_results_parser_test_files/5g_ManagedElement_GNBDUFunction_3NRCellDU.json"),
                TEST_CM_HANDLE, attrByMoType);

        //verification
        assertThat(eaccManagedObjects).hasSize(5)
                .extracting(EaccManagedObject::getMoId, EaccManagedObject::getMoType, EaccManagedObject::getFdn, EaccManagedObject::getCmHandle,
                        EaccManagedObject::getAttributes)
                .containsExactlyInAnyOrder(
                        tuple("NR01gNodeBRadio00007", "ManagedElement", "test,ManagedElement=NR01gNodeBRadio00007", TEST_CM_HANDLE,
                                managedElementAttributes),
                        tuple("1", "GNBDUFunction", "test,ManagedElement=NR01gNodeBRadio00007,GNBDUFunction=1", TEST_CM_HANDLE,
                                gnbduFunctionAttributes),
                        tuple("NR01gNodeBRadio00007-1", "NRCellDU",
                                "test,ManagedElement=NR01gNodeBRadio00007,GNBDUFunction=1,NRCellDU=NR01gNodeBRadio00007-1", TEST_CM_HANDLE,
                                nrCellDUAttributes),
                        tuple("NR01gNodeBRadio00007-2", "NRCellDU",
                                "test,ManagedElement=NR01gNodeBRadio00007,GNBDUFunction=1,NRCellDU=NR01gNodeBRadio00007-2", TEST_CM_HANDLE,
                                nrCellDUAttributes),
                        tuple("NR01gNodeBRadio00007-3", "NRCellDU",
                                "test,ManagedElement=NR01gNodeBRadio00007,GNBDUFunction=1,NRCellDU=NR01gNodeBRadio00007-3", TEST_CM_HANDLE,
                                nrCellDUAttributes));
    }

    @Test
    void whenParseManagedElementWithGNBDUFunctionChildAndEUtraNetworkGrandChildAndExternalENodeBFunctionGreatGrandChildAndExternalEUtranCellGreatGreatGrandChild_thenCorrectEaccManagedObjectsReturned_withoutEUtraNetwork()
            throws Exception {

        //rules defined
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule("ManagedElement", "dnPrefix", "test", ""));
        rules.add(new Rule("GNBCUCPFunction", "tXnDcOverall", "5", ""));
        rules.add(new Rule("ExternalENodeBFunction", "isRemoveAllowed", "false", ""));
        rules.add(new Rule("ExternalEUtranCell", "earfcnul", "18000", ""));
        rules.add(new Rule("ExternalEUtranCell", "tac", "0", ""));
        final Map<String, List<String>> attrByMoType = rules.stream().collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        //expectations
        final Map<String, Object> managedElementAttributes = new HashMap<>();
        managedElementAttributes.put("dnPrefix", "test");

        final Map<String, Object> gnbcucpFunctionAttributes = new HashMap<>();
        gnbcucpFunctionAttributes.put("tXnDcOverall", "5");

        final Map<String, Object> externalENodeBFunctionAttributes = new HashMap<>();
        externalENodeBFunctionAttributes.put("isRemoveAllowed", "false");

        final Map<String, Object> externalEUtranCellAttributes = new HashMap<>();
        externalEUtranCellAttributes.put("earfcnul", "18000");
        externalEUtranCellAttributes.put("tac", "0");

        //execute
        final List<EaccManagedObject> eaccManagedObjects = new NcmpResultsParser().parse(
                NcmpResponseObjectBuilderUtil.buildResponseObjectFromJsonFile(
                        "ncmp_results_parser_test_files/5g_ManagedElement_GNBCUCPFunction_EUtraNetwork_ExternalENodeBFunction_ExternalEUtranCell.json"),
                TEST_CM_HANDLE, attrByMoType);

        //verification
        assertThat(eaccManagedObjects).hasSize(4)
                .extracting(EaccManagedObject::getMoId, EaccManagedObject::getMoType, EaccManagedObject::getFdn, EaccManagedObject::getCmHandle,
                        EaccManagedObject::getAttributes)
                .containsExactlyInAnyOrder(
                        tuple("NR01gNodeBRadio00007", "ManagedElement", "test,ManagedElement=NR01gNodeBRadio00007", TEST_CM_HANDLE,
                                managedElementAttributes),
                        tuple("1", "GNBCUCPFunction", "test,ManagedElement=NR01gNodeBRadio00007,GNBCUCPFunction=1", TEST_CM_HANDLE,
                                gnbcucpFunctionAttributes),
                        tuple("100", "ExternalENodeBFunction",
                                "test,ManagedElement=NR01gNodeBRadio00007,GNBCUCPFunction=1,EUtraNetwork=1,ExternalENodeBFunction=100",
                                TEST_CM_HANDLE,
                                externalENodeBFunctionAttributes),
                        tuple("10", "ExternalEUtranCell",
                                "test,ManagedElement=NR01gNodeBRadio00007,GNBCUCPFunction=1,EUtraNetwork=1,ExternalENodeBFunction=100,ExternalEUtranCell=10",
                                TEST_CM_HANDLE,
                                externalEUtranCellAttributes));

    }
}
