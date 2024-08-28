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

package com.ericsson.oss.apps.service.ncmp

import com.ericsson.oss.apps.model.Rule
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static com.ericsson.oss.apps.util.TestDefaults.ENODE_BFUNCTION
import static com.ericsson.oss.apps.util.TestDefaults.EUTRAN_CELL_FDD
import static java.util.stream.Collectors.groupingBy
import static java.util.stream.Collectors.mapping
import static java.util.stream.Collectors.toList

/**
 * Unit tests for {@link NcmpOptionsBuilder} class.
 */
class NCMPOptionsBuilderTestSpec extends Specification {
    @Shared
    def RULES_LIST = List.of(
            new Rule(ENODE_BFUNCTION, "prachConfigEnabled", "true", ""),
            new Rule(ENODE_BFUNCTION, "zzzTemporary74", "-2000000000", ""),
            new Rule(EUTRAN_CELL_FDD, "cellCapMinCellSubCap", "1000", ""),
            new Rule(EUTRAN_CELL_FDD, "cellCapMinMaxWriProt", "true", ""),
            new Rule(EUTRAN_CELL_FDD, "cfraEnable", "true", ""),
            new Rule(EUTRAN_CELL_FDD, "commonCqiPeriodicity", "80", ""),
            new Rule(EUTRAN_CELL_FDD, "ulVolteCovMobThr", "5", ""))
    @Shared
    def RULE_SITE_LOCATION = new Rule("ManagedElement", "siteLocation", "athlone", "")
    @Shared
    def RULE_ME_DNPREFIX = new Rule("ManagedElement", "dnPrefix", "test", "")
    @Shared
    //Note: Doesn't have this attribute in reality but just to test scenario
    def RULE_CELL_PERFORMANCE_DNPREFIX = new Rule("CellPerformance", "dnPrefix", "test", "")
    @Shared
    def FIELDS = "fields="
    @Shared
    def E_NB_FUNCTION_ATTRIBUTES = "ENodeBFunction/attributes(prachConfigEnabled;zzzTemporary74);"
    @Shared
    def E_UTRAN_CELL_FDD_ATTRIBUTES = "EUtranCellFDD/attributes(cellCapMinCellSubCap;cellCapMinMaxWriProt;cfraEnable;commonCqiPeriodicity;ulVolteCovMobThr);"
    @Shared
    def ME_ATTRIBUTES = "ManagedElement/attributes(dnPrefix;siteLocation);"
    @Shared
    def OPTIONS_STRING_1 = FIELDS + E_NB_FUNCTION_ATTRIBUTES + ME_ATTRIBUTES + E_UTRAN_CELL_FDD_ATTRIBUTES + "CellPerformance/attributes(dnPrefix);"

    @Unroll
    def "when Rules Grouped By Mo Are Provided #condition Then Correct Options Are Returned"() {
        given:
            def rules = new ArrayList<>(RULES_LIST)
            rules.addAll(extraRules as List<Rule>)
            def rulesByMo = rules.stream().collect(groupingBy(Rule::getMoType, mapping(Rule::getAttributeName, toList())))

        expect:
            NcmpOptionsBuilder.build(rulesByMo) == expectedOptions

        where:
            condition                                                                        | extraRules                       || expectedOptions
            "Without moType Managed-Element And Attribute Dn Prefix "                        | []                               || FIELDS + "ManagedElement/attributes(dnPrefix);" + E_NB_FUNCTION_ATTRIBUTES + E_UTRAN_CELL_FDD_ATTRIBUTES
            "With moType Managed-Element And Some Attribute But Without Attribute Dn Prefix" | [RULE_SITE_LOCATION]             || FIELDS + E_NB_FUNCTION_ATTRIBUTES + ME_ATTRIBUTES + E_UTRAN_CELL_FDD_ATTRIBUTES
            "With moType Managed-Element And Attribute Dn Prefix"                            | [RULE_SITE_LOCATION,
                                                                                                RULE_ME_DNPREFIX]               || FIELDS + E_NB_FUNCTION_ATTRIBUTES + "ManagedElement/attributes(siteLocation;dnPrefix);" + E_UTRAN_CELL_FDD_ATTRIBUTES
            "With moType Managed-Element And Some Attribute And Dn Prefix In Another MO"     | [RULE_SITE_LOCATION,
                                                                                                RULE_CELL_PERFORMANCE_DNPREFIX] || OPTIONS_STRING_1
            "With moType Managed-Element With Dn Prefix And Dn Prefix In Another MO"         | [RULE_ME_DNPREFIX,
                                                                                                RULE_SITE_LOCATION,
                                                                                                RULE_CELL_PERFORMANCE_DNPREFIX] || OPTIONS_STRING_1
    }
}