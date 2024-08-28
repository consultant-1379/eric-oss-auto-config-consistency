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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ericsson.oss.apps.model.EaccManagedObject;
import com.ericsson.oss.apps.model.Rule;
import com.ericsson.oss.apps.service.CMServiceException;

/**
 * Unit tests for {@link NcmpResultsParser} class. <br>
 * This tests the parser when the result returned from ncmp is for a 4g node using the files 4g_ManagedElement.json,
 * 4g_ManagedElement_ENodeBFunction.json, 4g_ManagedElement_ENodeBFunction_EUtranCellFDD.json.json,
 * 4g_ManagedElement_ENodeBFunction_3EUtranCellFDD.json and 4g_ManagedElement_ENodeBFunction_LoadBalancingFunction_IdleModePrioAtRelease.json to mimic
 * the result returned from ncmp. <br>
 * It also tests the scenario where {@link CMServiceException} is thrown from the parser.
 * 
 * @see Ncmp5GResultsParserTest
 * @see Ncmp4G5GResultsParserTest
 * @see Ncmp5GResultsParserForNodeWithNRCellWithStructAttributeTest
 */
class Ncmp4GResultsParserTest {

    private static final String TEST_CM_HANDLE = "E3C5531A165CA4A9AEE417A6E671A6E2";

    @Test
    void whenParseManagedElement_thenCorrectEaccManagedObjectReturned() throws Exception {
        //rules defined
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule("ManagedElement", "dnPrefix", "test", ""));
        rules.add(new Rule("ManagedElement", "siteLocation", "athlone", ""));
        final Map<String, List<String>> attrByMoType = rules.stream().collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        //expectations
        final Map<String, Object> managedElementAttributes = new HashMap<>();
        managedElementAttributes.put("dnPrefix", "test");
        managedElementAttributes.put("siteLocation", "athlone");

        //execute
        final List<EaccManagedObject> eaccManagedObjects = new NcmpResultsParser().parse(
                NcmpResponseObjectBuilderUtil.buildResponseObjectFromJsonFile("ncmp_results_parser_test_files/4g_ManagedElement.json"),
                TEST_CM_HANDLE, attrByMoType);

        //verification
        assertThat(eaccManagedObjects).hasSize(1)
                .extracting(EaccManagedObject::getMoId, EaccManagedObject::getMoType, EaccManagedObject::getFdn, EaccManagedObject::getCmHandle,
                        EaccManagedObject::getAttributes)
                .containsExactlyInAnyOrder(
                        tuple("LTE04dg2ERBS00082", "ManagedElement", "test,ManagedElement=LTE04dg2ERBS00082", TEST_CM_HANDLE,
                                managedElementAttributes));

    }

    @Test
    void whenParseManagedElementWhenDnPrefixNotInTheRules_thenEaccManagedObjectReturnedWithoutDnPrefix() throws Exception {
        //rules defined
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule("ManagedElement", "siteLocation", "athlone", ""));
        final Map<String, List<String>> attrByMoType = rules.stream().collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        //expectations
        final Map<String, Object> managedElementAttributes = new HashMap<>();
        managedElementAttributes.put("siteLocation", "athlone");

        //execute
        final List<EaccManagedObject> eaccManagedObjects = new NcmpResultsParser().parse(
                NcmpResponseObjectBuilderUtil.buildResponseObjectFromJsonFile("ncmp_results_parser_test_files/4g_ManagedElement.json"),
                TEST_CM_HANDLE, attrByMoType);

        //verification
        assertThat(eaccManagedObjects).hasSize(1)
                .extracting(EaccManagedObject::getMoId, EaccManagedObject::getMoType, EaccManagedObject::getFdn, EaccManagedObject::getCmHandle,
                        EaccManagedObject::getAttributes)
                .containsExactlyInAnyOrder(
                        tuple("LTE04dg2ERBS00082", "ManagedElement", "test,ManagedElement=LTE04dg2ERBS00082", TEST_CM_HANDLE,
                                managedElementAttributes));

    }

    @Test
    void whenParseManagedElementWithENodeBFunctionChildWhenManagedElementDnPrefixInTheRules_thenCorrectEaccManagedObjectsReturned() throws Exception {
        //rules defined
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule("ManagedElement", "dnPrefix", "test", ""));
        rules.add(new Rule("ManagedElement", "siteLocation", "athlone", ""));
        rules.add(new Rule("ENodeBFunction", "timeAndPhaseSynchAlignment", "true", ""));
        rules.add(new Rule("ENodeBFunction", "rrcConnReestActive", "false", ""));
        final Map<String, List<String>> attrByMoType = rules.stream().collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        //expectations
        final Map<String, Object> managedElementAttributes = new HashMap<>();
        managedElementAttributes.put("dnPrefix", "test");
        managedElementAttributes.put("siteLocation", "athlone");

        final Map<String, Object> enodeBFunctionAttributes = new HashMap<>();
        enodeBFunctionAttributes.put("timeAndPhaseSynchAlignment", "true");
        enodeBFunctionAttributes.put("rrcConnReestActive", "false");

        //execute
        final List<EaccManagedObject> eaccManagedObjects = new NcmpResultsParser()
                .parse(NcmpResponseObjectBuilderUtil
                        .buildResponseObjectFromJsonFile("ncmp_results_parser_test_files/4g_ManagedElement_ENodeBFunction.json"), TEST_CM_HANDLE,
                        attrByMoType);

        //verification
        assertThat(eaccManagedObjects).hasSize(2)
                .extracting(EaccManagedObject::getMoId, EaccManagedObject::getMoType, EaccManagedObject::getFdn, EaccManagedObject::getCmHandle,
                        EaccManagedObject::getAttributes)
                .containsExactlyInAnyOrder(
                        tuple("LTE04dg2ERBS00082", "ManagedElement", "test,ManagedElement=LTE04dg2ERBS00082", TEST_CM_HANDLE,
                                managedElementAttributes),
                        tuple("1", "ENodeBFunction", "test,ManagedElement=LTE04dg2ERBS00082,ENodeBFunction=1", TEST_CM_HANDLE,
                                enodeBFunctionAttributes));
    }

    @Test
    void whenParseManagedElementWithENodeBFunctionChildWhenManagedElementDnPrefixNotInTheRules_thenCorrectEaccManagedObjectsReturned()
            throws Exception {
        //rules defined
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule("ManagedElement", "siteLocation", "athlone", ""));
        rules.add(new Rule("ENodeBFunction", "timeAndPhaseSynchAlignment", "true", ""));
        rules.add(new Rule("ENodeBFunction", "rrcConnReestActive", "false", ""));
        final Map<String, List<String>> attrByMoType = rules.stream().collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        //expectations
        final Map<String, Object> managedElementAttributes = new HashMap<>();
        managedElementAttributes.put("siteLocation", "athlone");

        final Map<String, Object> enodeBFunctionAttributes = new HashMap<>();
        enodeBFunctionAttributes.put("timeAndPhaseSynchAlignment", "true");
        enodeBFunctionAttributes.put("rrcConnReestActive", "false");

        //execute
        final List<EaccManagedObject> eaccManagedObjects = new NcmpResultsParser()
                .parse(NcmpResponseObjectBuilderUtil
                        .buildResponseObjectFromJsonFile("ncmp_results_parser_test_files/4g_ManagedElement_ENodeBFunction.json"), TEST_CM_HANDLE,
                        attrByMoType);

        //verification
        assertThat(eaccManagedObjects).hasSize(2)
                .extracting(EaccManagedObject::getMoId, EaccManagedObject::getMoType, EaccManagedObject::getFdn, EaccManagedObject::getCmHandle,
                        EaccManagedObject::getAttributes)
                .containsExactlyInAnyOrder(
                        tuple("LTE04dg2ERBS00082", "ManagedElement", "test,ManagedElement=LTE04dg2ERBS00082", TEST_CM_HANDLE,
                                managedElementAttributes),
                        tuple("1", "ENodeBFunction", "test,ManagedElement=LTE04dg2ERBS00082,ENodeBFunction=1", TEST_CM_HANDLE,
                                enodeBFunctionAttributes));
    }

    @Test
    void whenParseManagedElementWithENodeBFunctionChildWhenManagedElementNotInTheRules_thenCorrectEaccManagedObjectReturned() throws Exception {
        //rules defined
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule("ENodeBFunction", "timeAndPhaseSynchAlignment", "true", ""));
        rules.add(new Rule("ENodeBFunction", "rrcConnReestActive", "false", ""));
        final Map<String, List<String>> attrByMoType = rules.stream().collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        //expectations
        final Map<String, Object> enodeBFunctionAttributes = new HashMap<>();
        enodeBFunctionAttributes.put("timeAndPhaseSynchAlignment", "true");
        enodeBFunctionAttributes.put("rrcConnReestActive", "false");

        //execute
        final List<EaccManagedObject> eaccManagedObjects = new NcmpResultsParser()
                .parse(NcmpResponseObjectBuilderUtil
                        .buildResponseObjectFromJsonFile("ncmp_results_parser_test_files/4g_ManagedElement_ENodeBFunction.json"), TEST_CM_HANDLE,
                        attrByMoType);

        //verification
        assertThat(eaccManagedObjects).hasSize(1)
                .extracting(EaccManagedObject::getMoId, EaccManagedObject::getMoType, EaccManagedObject::getFdn, EaccManagedObject::getCmHandle,
                        EaccManagedObject::getAttributes)
                .containsExactlyInAnyOrder(
                        tuple("1", "ENodeBFunction", "test,ManagedElement=LTE04dg2ERBS00082,ENodeBFunction=1", TEST_CM_HANDLE,
                                enodeBFunctionAttributes));
    }

    @Test
    void whenParseManagedElementWithWithENodeBFunctionChildAndEUtranCellFDDGrandChild_thenCorrectEaccManagedObjectsReturned() throws Exception {
        //rules defined
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule("ManagedElement", "dnPrefix", "test", ""));
        rules.add(new Rule("ENodeBFunction", "timeAndPhaseSynchAlignment", "true", ""));
        rules.add(new Rule("ENodeBFunction", "rrcConnReestActive", "false", ""));
        rules.add(new Rule("EUtranCellFDD", "ulVolteCovMobThr", "5", ""));
        final Map<String, List<String>> attrByMoType = rules.stream().collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        //expectations
        final Map<String, Object> managedElementAttributes = new HashMap<>();
        managedElementAttributes.put("dnPrefix", "test");

        final Map<String, Object> enodeBFunctionAttributes = new HashMap<>();
        enodeBFunctionAttributes.put("timeAndPhaseSynchAlignment", "true");
        enodeBFunctionAttributes.put("rrcConnReestActive", "false");

        final Map<String, Object> eutranCellFDDAttributes = new HashMap<>();
        eutranCellFDDAttributes.put("ulVolteCovMobThr", "5");

        //execute
        final List<EaccManagedObject> eaccManagedObjects = new NcmpResultsParser()
                .parse(NcmpResponseObjectBuilderUtil
                        .buildResponseObjectFromJsonFile("ncmp_results_parser_test_files/4g_ManagedElement_ENodeBFunction_EUtranCellFDD.json"),
                        TEST_CM_HANDLE, attrByMoType);

        //verification
        assertThat(eaccManagedObjects).hasSize(3)
                .extracting(EaccManagedObject::getMoId, EaccManagedObject::getMoType, EaccManagedObject::getFdn, EaccManagedObject::getCmHandle,
                        EaccManagedObject::getAttributes)
                .containsExactlyInAnyOrder(
                        tuple("LTE04dg2ERBS00082", "ManagedElement", "test,ManagedElement=LTE04dg2ERBS00082", TEST_CM_HANDLE,
                                managedElementAttributes),
                        tuple("1", "ENodeBFunction", "test,ManagedElement=LTE04dg2ERBS00082,ENodeBFunction=1", TEST_CM_HANDLE,
                                enodeBFunctionAttributes),
                        tuple("LTE04dg2ERBS00082-1", "EUtranCellFDD",
                                "test,ManagedElement=LTE04dg2ERBS00082,ENodeBFunction=1,EUtranCellFDD=LTE04dg2ERBS00082-1", TEST_CM_HANDLE,
                                eutranCellFDDAttributes));
    }

    @Test
    void whenParseManagedElementWithWithENodeBFunctionChildAnd3EUtranCellFDDGrandChild_thenCorrectEaccManagedObjectsReturned() throws Exception {
        //rules defined
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule("ManagedElement", "dnPrefix", "test", ""));
        rules.add(new Rule("ENodeBFunction", "timeAndPhaseSynchAlignment", "true", ""));
        rules.add(new Rule("ENodeBFunction", "rrcConnReestActive", "false", ""));
        rules.add(new Rule("EUtranCellFDD", "ulVolteCovMobThr", "5", ""));
        final Map<String, List<String>> attrByMoType = rules.stream().collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        //expectations
        final Map<String, Object> managedElementAttributes = new HashMap<>();
        managedElementAttributes.put("dnPrefix", "test");

        final Map<String, Object> enodeBFunctionAttributes = new HashMap<>();
        enodeBFunctionAttributes.put("timeAndPhaseSynchAlignment", "true");
        enodeBFunctionAttributes.put("rrcConnReestActive", "false");

        final Map<String, Object> eutranCellFDDAttributes = new HashMap<>();
        eutranCellFDDAttributes.put("ulVolteCovMobThr", "5");

        //execute
        final List<EaccManagedObject> eaccManagedObjects = new NcmpResultsParser()
                .parse(NcmpResponseObjectBuilderUtil
                        .buildResponseObjectFromJsonFile("ncmp_results_parser_test_files/4g_ManagedElement_ENodeBFunction_3EUtranCellFDD.json"),
                        TEST_CM_HANDLE, attrByMoType);

        //verification
        assertThat(eaccManagedObjects).hasSize(5)
                .extracting(EaccManagedObject::getMoId, EaccManagedObject::getMoType, EaccManagedObject::getFdn, EaccManagedObject::getCmHandle,
                        EaccManagedObject::getAttributes)
                .containsExactlyInAnyOrder(
                        tuple("LTE04dg2ERBS00082", "ManagedElement", "test,ManagedElement=LTE04dg2ERBS00082", TEST_CM_HANDLE,
                                managedElementAttributes),
                        tuple("1", "ENodeBFunction", "test,ManagedElement=LTE04dg2ERBS00082,ENodeBFunction=1", TEST_CM_HANDLE,
                                enodeBFunctionAttributes),
                        tuple("LTE04dg2ERBS00082-1", "EUtranCellFDD",
                                "test,ManagedElement=LTE04dg2ERBS00082,ENodeBFunction=1,EUtranCellFDD=LTE04dg2ERBS00082-1", TEST_CM_HANDLE,
                                eutranCellFDDAttributes),
                        tuple("LTE04dg2ERBS00082-2", "EUtranCellFDD",
                                "test,ManagedElement=LTE04dg2ERBS00082,ENodeBFunction=1,EUtranCellFDD=LTE04dg2ERBS00082-2", TEST_CM_HANDLE,
                                eutranCellFDDAttributes),
                        tuple("LTE04dg2ERBS00082-3", "EUtranCellFDD",
                                "test,ManagedElement=LTE04dg2ERBS00082,ENodeBFunction=1,EUtranCellFDD=LTE04dg2ERBS00082-3", TEST_CM_HANDLE,
                                eutranCellFDDAttributes));
    }

    @Test
    void whenParseManagedElementWithENodeBFunctionChildAndLoadBalancingFunctionGrandChildAndIdleModePrioAtReleaseGreatGrandChild_thenCorrectEaccManagedObjectsReturned_withoutENodeBFunctionAndLoadBalancingFunction()
            throws Exception {
        //rules defined
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule("ManagedElement", "dnPrefix", "test", ""));
        rules.add(new Rule("IdleModePrioAtRelease", "highLoadThreshold", "800", ""));
        rules.add(new Rule("IdleModePrioAtRelease", "mediumLoadThreshold", "400", ""));
        rules.add(new Rule("IdleModePrioAtRelease", "lowLoadThreshold", "0", ""));
        rules.add(new Rule("IdleModePrioAtRelease", "mediumHighLoadThreshold", "600", ""));
        rules.add(new Rule("IdleModePrioAtRelease", "lowMediumLoadThreshold", "200", ""));
        final Map<String, List<String>> attrByMoType = rules.stream().collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        //expectations
        final Map<String, Object> managedElementAttributes = new HashMap<>();
        managedElementAttributes.put("dnPrefix", "test");

        //Mimicking No attributes requested on enodeBfunction only attributes on its descendants. Should not get EaccManagedObject built for this
        //Mimicking No attributes requested on loadBalancingFunction only attributes on its descendants. Should not get EaccManagedObject built for this

        final Map<String, Object> idleModePrioAtReleaseAttributes = new HashMap<>();
        idleModePrioAtReleaseAttributes.put("highLoadThreshold", "800");
        idleModePrioAtReleaseAttributes.put("mediumLoadThreshold", "400");
        idleModePrioAtReleaseAttributes.put("lowLoadThreshold", "0");
        idleModePrioAtReleaseAttributes.put("mediumHighLoadThreshold", "600");
        idleModePrioAtReleaseAttributes.put("lowMediumLoadThreshold", "200");

        //execute
        final List<EaccManagedObject> eaccManagedObjects = new NcmpResultsParser()
                .parse(NcmpResponseObjectBuilderUtil
                        .buildResponseObjectFromJsonFile(
                                "ncmp_results_parser_test_files/4g_ManagedElement_ENodeBFunction_LoadBalancingFunction_IdleModePrioAtRelease.json"),
                        TEST_CM_HANDLE, attrByMoType);

        //verification
        assertThat(eaccManagedObjects).hasSize(2)
                .extracting(EaccManagedObject::getMoId, EaccManagedObject::getMoType, EaccManagedObject::getFdn, EaccManagedObject::getCmHandle,
                        EaccManagedObject::getAttributes)
                .containsExactlyInAnyOrder(
                        tuple("LTE04dg2ERBS00082", "ManagedElement", "test,ManagedElement=LTE04dg2ERBS00082", TEST_CM_HANDLE,
                                managedElementAttributes),
                        tuple("1000", "IdleModePrioAtRelease",
                                "test,ManagedElement=LTE04dg2ERBS00082,ENodeBFunction=1,LoadBalancingFunction=1,IdleModePrioAtRelease=1000",
                                TEST_CM_HANDLE, idleModePrioAtReleaseAttributes));

    }

    @Test
    void whenParseManagedElementWithMalformedBody_thenCMServiceExceptionIsThrown() {

        final Map<String, List<String>> attrByMoType = new HashMap<>();

        final String ManagedElement_Malformed_With_Missing_Quotation_Mark = "{"
                + "ManagedElement\":"
                + "["
                + "    {"
                + "        \"id\": \"LTE04dg2ERBS00082\","
                + "        \"attributes\":"
                + "         {"
                + "             \"dnPrefix\": \"test\""
                + "         }"
                + "   }"
                + "]";

        assertThatThrownBy(() -> new NcmpResultsParser().parse(ManagedElement_Malformed_With_Missing_Quotation_Mark,
                TEST_CM_HANDLE, attrByMoType))
                        .isInstanceOf(CMServiceException.class)
                        .hasMessage("Failed to parse results returned from ncmp for cmhandle '" + TEST_CM_HANDLE + "'");
    }
}