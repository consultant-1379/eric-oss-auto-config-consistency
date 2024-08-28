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

package com.ericsson.oss.apps.service

import com.ericsson.oss.apps.model.Rule
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static com.ericsson.oss.apps.util.TestDefaults.*
import static java.util.stream.Collectors.groupingBy

/**
 * Unit tests for {@link com.ericsson.oss.apps.validation.RuleSetValidator} class.
 */
class RuleServiceTestSpec extends Specification {
    def objectUnderTest = new RuleService(null);

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
    def E_NB_FUNCTION_ATTRIBUTES = List.of("prachConfigEnabled","zzzTemporary74")
    @Shared
    def E_UTRAN_CELL_FDD_ATTRIBUTES = List.of("cellCapMinCellSubCap", "cellCapMinMaxWriProt", "cfraEnable",
        "commonCqiPeriodicity", "ulVolteCovMobThr")

    @Shared
    def RULES_WITH_CONDITIONS = List.of(
        new Rule(ENODE_BFUNCTION, "prachConfigEnabled", "true", "ENodeBFunction.nnsfMode = 'RPLMN_IF_SAME_AS_SPLMN'"),
        new Rule(ENODE_BFUNCTION, "zzzTemporary74", "-2000000000", "ENodeBFunction.endcSplitAllowedNonDynPwrShUe = false"),
        new Rule(EUTRAN_CELL_FDD, "cellCapMinCellSubCap", "1000", "EUtranCellFDD.harqOffsetDl in (1,2,3)"),
        new Rule(EUTRAN_CELL_FDD, "cellCapMinMaxWriProt", "true", "EUtranCellFDD.userLabel like '%dg2%'"),
        new Rule(EUTRAN_CELL_FDD, "cfraEnable", "true", "EUtranCellFDD.lbdarCoverageThreshold = 15 and EUtranCellFDD.noOfPucchCqiUsers > 150"),
        new Rule(EUTRAN_CELL_FDD, "commonCqiPeriodicity", "80", "EUtranCellFDD.ttiBundlingSwitchThres in (80,90,75)"),
        new Rule(EUTRAN_CELL_FDD, "ulVolteCovMobThr", "5", "EUtranCellFDD.lbEUtranAcceptOffloadThreshold != 45"))
    @Shared
    def WITH_CONDITION_E_NB_FUNCTION_ATTRIBUTES = List.of("prachConfigEnabled", "nnsfMode", "zzzTemporary74", "endcSplitAllowedNonDynPwrShUe")
    @Shared
    def WITH_CONDITION_E_UTRAN_CELL_FDD_ATTRIBUTES = List.of("cellCapMinCellSubCap", "harqOffsetDl", "cellCapMinMaxWriProt",
        "userLabel", "cfraEnable", "lbdarCoverageThreshold", "noOfPucchCqiUsers", "commonCqiPeriodicity",
        "ttiBundlingSwitchThres", "ulVolteCovMobThr", "lbEUtranAcceptOffloadThreshold")
    @Shared
    def RULE_FDN_ONLY_CONDITION = new Rule(EUTRAN_CELL_FDD, "cfraEnable", "test", "FDN like '%dg2%'")

    @Unroll
    def "when Rules Grouped by Mo are Provided #condition Then Correct Attributes Are Returned"() {
        given:
            def rules = joinList(RULES_LIST, extraRules as List<Rule>)
            def rulesByMo = rules.stream().collect(groupingBy(Rule::getMoType))
            def fdnToRulesByMo = Map.of(MANAGED_ELEMENT_1_FDN, rulesByMo)

        expect:
            objectUnderTest.getAttributesFromMoLevelRules(fdnToRulesByMo) == expectedAttributes

        where:
            condition                       | extraRules            || expectedAttributes
            "With List of Rules"            | []                    || [(MANAGED_ELEMENT_1_FDN): [(ENODE_BFUNCTION): E_NB_FUNCTION_ATTRIBUTES, (EUTRAN_CELL_FDD): E_UTRAN_CELL_FDD_ATTRIBUTES]]
            "With Duplicate Rules"          | RULES_LIST            || [(MANAGED_ELEMENT_1_FDN): [(ENODE_BFUNCTION): E_NB_FUNCTION_ATTRIBUTES, (EUTRAN_CELL_FDD): E_UTRAN_CELL_FDD_ATTRIBUTES]]
            "With moType Managed-Element"   | [RULE_SITE_LOCATION]  || [(MANAGED_ELEMENT_1_FDN): [("ManagedElement"): ["siteLocation"], (ENODE_BFUNCTION): E_NB_FUNCTION_ATTRIBUTES, (EUTRAN_CELL_FDD): E_UTRAN_CELL_FDD_ATTRIBUTES]]
    }

    @Unroll
    def "when Rules with conditions Grouped By Mo Are Provided #condition Then Correct Attributes Are Returned"() {
        given:
            def rules = joinList(RULES_WITH_CONDITIONS, extraRules as List<Rule>)
            def rulesByMo = rules.stream().collect(groupingBy(Rule::getMoType))
            def fdnToRulesByMo = Map.of(MANAGED_ELEMENT_1_FDN, rulesByMo)

        expect:
            objectUnderTest.getAttributesFromMoLevelRules(fdnToRulesByMo) == expectedAttributes

        where:
            condition                                   | extraRules                       || expectedAttributes
            "When rules have conditions"                | []                               || [(MANAGED_ELEMENT_1_FDN): [(ENODE_BFUNCTION): WITH_CONDITION_E_NB_FUNCTION_ATTRIBUTES, (EUTRAN_CELL_FDD): WITH_CONDITION_E_UTRAN_CELL_FDD_ATTRIBUTES]]
            "When a rule has only FDN based condition"  | [RULE_FDN_ONLY_CONDITION]        || [(MANAGED_ELEMENT_1_FDN): [(ENODE_BFUNCTION): WITH_CONDITION_E_NB_FUNCTION_ATTRIBUTES, (EUTRAN_CELL_FDD): WITH_CONDITION_E_UTRAN_CELL_FDD_ATTRIBUTES]]
            "When a rule does not have any condition"   | [RULE_SITE_LOCATION]             || [(MANAGED_ELEMENT_1_FDN): [("ManagedElement"): ["siteLocation"], (ENODE_BFUNCTION): WITH_CONDITION_E_NB_FUNCTION_ATTRIBUTES, (EUTRAN_CELL_FDD): WITH_CONDITION_E_UTRAN_CELL_FDD_ATTRIBUTES]]
    }

    <T> List<T> joinList(final List<T> original, final List<T> additional) {
        def allElements = new ArrayList<>(original)
        allElements.addAll(additional)
        return allElements
    }
}