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
 * Unit tests for {@link NcmpResultsParser} class. This tests the parser when the result returned from ncmp is for a
 * 4g5g_ManagedElement_GNBDUFunctionWith3NRCellDU_GNBCUCPFunctionWith2NRCellCU_ENodeBFunctionWith4EUtranCellFDD.json to mimic the result returned from
 * ncmp. <br>
 * 
 * @see Ncmp4GResultsParserTest
 * @see Ncmp5GResultsParserTest
 * @see Ncmp5GResultsParserForNodeWithNRCellWithStructAttributeTest
 */
class Ncmp4G5GResultsParserTest {

    private static final String TEST_CM_HANDLE = "E3C5531A165CA4A9AEE417A6E671A6E2";

    @Test
    void whenParseManagedElementWithGNBDUFunctionAndGNBCUCPFunctionAndGNBCUUPFunctionAndENodeBFunction_thenCorrectEaccManagedObjectsReturned()
            throws Exception {
        //rules defined
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule("GNBDUFunction", "endpointResDepHEnabled", "true", ""));
        rules.add(new Rule("NRCellDU", "drxEnable", "true", ""));
        rules.add(new Rule("NRCellDU", "csiRsShiftingPrimary", "DEACTIVATED", ""));
        rules.add(new Rule("GNBCUCPFunction", "tXDcOverall", "5", ""));
        rules.add(new Rule("GNBCUCPFunction", "tXDcPrep", "10", ""));
        rules.add(new Rule("NRCellCU", "hiPrioDetEnabled", "false", ""));
        rules.add(new Rule("GNBCUUPFunction", "xnIpAddrViaNgActive", "true", ""));
        rules.add(new Rule("GNBCUUPFunction", "dataFwdRateTo5Gs", "5000", ""));
        rules.add(new Rule("GNBCUUPFunction", "dataFwdRateToEps", "200", ""));
        rules.add(new Rule("ENodeBFunction", "timeAndPhaseSynchAlignment", "true", ""));
        rules.add(new Rule("ENodeBFunction", "rrcConnReestActive", "false", ""));
        rules.add(new Rule("EUtranCellFDD", "ulVolteCovMobThr", "5", ""));
        final Map<String, List<String>> attrByMoType = rules.stream().collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        //expectations
        final Map<String, Object> gnbduFunctionAttributes = new HashMap<>();
        gnbduFunctionAttributes.put("endpointResDepHEnabled", "true");

        final Map<String, Object> nrCellDUAttributes = new HashMap<>();
        nrCellDUAttributes.put("drxEnable", "true");
        nrCellDUAttributes.put("csiRsShiftingPrimary", "DEACTIVATED");

        final Map<String, Object> gnbcucpFunctionAttributes = new HashMap<>();
        gnbcucpFunctionAttributes.put("tXDcOverall", "5");
        gnbcucpFunctionAttributes.put("tXDcPrep", "10");

        final Map<String, Object> nrCellCUAttributes = new HashMap<>();
        nrCellCUAttributes.put("hiPrioDetEnabled", "false");

        final Map<String, Object> gnbcuupFunctionAttributes = new HashMap<>();
        gnbcuupFunctionAttributes.put("xnIpAddrViaNgActive", "true");
        gnbcuupFunctionAttributes.put("dataFwdRateTo5Gs", "5000");
        gnbcuupFunctionAttributes.put("dataFwdRateToEps", "200");

        final Map<String, Object> enodeBFunctionAttributes = new HashMap<>();
        enodeBFunctionAttributes.put("timeAndPhaseSynchAlignment", "true");
        enodeBFunctionAttributes.put("rrcConnReestActive", "false");

        final Map<String, Object> eutranCellFDDAttributes = new HashMap<>();
        eutranCellFDDAttributes.put("ulVolteCovMobThr", "5");

        //execute
        final List<EaccManagedObject> eaccManagedObjects = new NcmpResultsParser().parse(
                NcmpResponseObjectBuilderUtil
                        .buildResponseObjectFromJsonFile(
                                "ncmp_results_parser_test_files/4g5g_ManagedElement_GNBDUFunctionWith3NRCellDU_GNBCUCPFunctionWith2NRCellCU_ENodeBFunctionWith4EUtranCellFDD.json"),
                TEST_CM_HANDLE, attrByMoType);

        //verification
        assertThat(eaccManagedObjects).hasSize(13)
                .extracting(EaccManagedObject::getMoId, EaccManagedObject::getMoType, EaccManagedObject::getFdn, EaccManagedObject::getCmHandle,
                        EaccManagedObject::getAttributes)
                .containsExactlyInAnyOrder(
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
                                nrCellDUAttributes),
                        tuple("1", "GNBCUCPFunction", "test,ManagedElement=NR01gNodeBRadio00007,GNBCUCPFunction=1", TEST_CM_HANDLE,
                                gnbcucpFunctionAttributes),
                        tuple("NR01gNodeBRadio00007-4", "NRCellCU",
                                "test,ManagedElement=NR01gNodeBRadio00007,GNBCUCPFunction=1,NRCellCU=NR01gNodeBRadio00007-4", TEST_CM_HANDLE,
                                nrCellCUAttributes),
                        tuple("NR01gNodeBRadio00007-5", "NRCellCU",
                                "test,ManagedElement=NR01gNodeBRadio00007,GNBCUCPFunction=1,NRCellCU=NR01gNodeBRadio00007-5", TEST_CM_HANDLE,
                                nrCellCUAttributes),
                        tuple("1", "GNBCUUPFunction", "test,ManagedElement=NR01gNodeBRadio00007,GNBCUUPFunction=1", TEST_CM_HANDLE,
                                gnbcuupFunctionAttributes),
                        tuple("1", "ENodeBFunction", "test,ManagedElement=NR01gNodeBRadio00007,ENodeBFunction=1", TEST_CM_HANDLE,
                                enodeBFunctionAttributes),
                        tuple("LTE04dg2ERBS00082-1", "EUtranCellFDD",
                                "test,ManagedElement=NR01gNodeBRadio00007,ENodeBFunction=1,EUtranCellFDD=LTE04dg2ERBS00082-1", TEST_CM_HANDLE,
                                eutranCellFDDAttributes),
                        tuple("LTE04dg2ERBS00082-2", "EUtranCellFDD",
                                "test,ManagedElement=NR01gNodeBRadio00007,ENodeBFunction=1,EUtranCellFDD=LTE04dg2ERBS00082-2", TEST_CM_HANDLE,
                                eutranCellFDDAttributes),
                        tuple("LTE04dg2ERBS00082-3", "EUtranCellFDD",
                                "test,ManagedElement=NR01gNodeBRadio00007,ENodeBFunction=1,EUtranCellFDD=LTE04dg2ERBS00082-3", TEST_CM_HANDLE,
                                eutranCellFDDAttributes),
                        tuple("LTE04dg2ERBS00082-4", "EUtranCellFDD",
                                "test,ManagedElement=NR01gNodeBRadio00007,ENodeBFunction=1,EUtranCellFDD=LTE04dg2ERBS00082-4", TEST_CM_HANDLE,
                                eutranCellFDDAttributes));
    }

}