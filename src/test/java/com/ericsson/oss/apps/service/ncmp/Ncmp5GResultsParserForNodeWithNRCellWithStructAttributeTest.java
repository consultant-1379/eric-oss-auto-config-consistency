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
 * <b>Note:</b> Structs are not officially supported yet by EACC. <br>
 * This is just a test to see what way the parser handles them. <br>
 * At the moment if they were included in the rules (Which they shouldn't be for the trial) the parser would store them <br>
 * in the attributes of the {@link EaccManagedObject} in their raw format, as shown in the test below.
 */
class Ncmp5GResultsParserForNodeWithNRCellWithStructAttributeTest {

    private static final String TEST_CM_HANDLE = "E3C5531A165CA4A9AEE417A6E671A6E2";

    @Test
    void whenParseManagedElementWithGNBDUFunctionChildAndNRCellDUGrandChildWithStructAttribute_thenCorrectEaccManagedObjectsReturned_WithRawStructStoredAsPartOfNRCellDUAttributePLMNIdList()
            throws Exception {
        //rules defined
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule("ManagedElement", "dnPrefix", "test", ""));
        rules.add(new Rule("NRCellCU", "cellLocalId", "1", ""));
        rules.add(new Rule("NRCellCU", "pSCellCapable", "false", ""));
        rules.add(new Rule("NRCellCU", "smtcDuration", "0", ""));
        rules.add(new Rule("NRCellCU", "smtcOffset", "0", ""));
        rules.add(new Rule("NRCellCU", "smtcPeriodicity", "0", ""));
        final Map<String, List<String>> rulesByMoType = rules.stream().collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())));

        //expectations
        final Map<String, Object> managedElementAttributes = new HashMap<>();
        managedElementAttributes.put("dnPrefix", "test");

        final Map<Object, Object> pLMNIdListStruct = new HashMap<>();
        pLMNIdListStruct.put("mcc", "001");
        pLMNIdListStruct.put("mnc", "01");

        final List<Map<Object, Object>> pLMNIdList = new ArrayList<>();
        pLMNIdList.add(pLMNIdListStruct);

        final Map<String, Object> nrCellCUAttributes = new HashMap<>();
        nrCellCUAttributes.put("cellLocalId", "1");
        nrCellCUAttributes.put("pSCellCapable", "false");
        nrCellCUAttributes.put("pLMNIdList", pLMNIdList);
        nrCellCUAttributes.put("smtcDuration", "0");
        nrCellCUAttributes.put("smtcOffset", "0");
        nrCellCUAttributes.put("smtcPeriodicity", "0");

        //execute
        final List<EaccManagedObject> eaccManagedObjects = new NcmpResultsParser().parse(
                NcmpResponseObjectBuilderUtil
                        .buildResponseObjectFromJsonFile(
                                "ncmp_results_parser_test_files/5g_ManagedElement_GNBCUCPFunction_NRCellCUWithPLMNIdStruct.json"),
                TEST_CM_HANDLE, rulesByMoType);

        //verification
        assertThat(eaccManagedObjects).hasSize(2)
                .extracting(EaccManagedObject::getMoId, EaccManagedObject::getMoType, EaccManagedObject::getFdn, EaccManagedObject::getCmHandle,
                        EaccManagedObject::getAttributes)
                .containsExactlyInAnyOrder(
                        tuple("NR45gNodeBRadio00022", "ManagedElement", "test,ManagedElement=NR45gNodeBRadio00022", TEST_CM_HANDLE,
                                managedElementAttributes),
                        tuple("NR45gNodeBRadio00022-1", "NRCellCU",
                                "test,ManagedElement=NR45gNodeBRadio00022,GNBCUCPFunction=1,NRCellCU=NR45gNodeBRadio00022-1", TEST_CM_HANDLE,
                                nrCellCUAttributes));
    }
}