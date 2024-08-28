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

package com.ericsson.oss.apps.model

import spock.lang.Specification

import static com.ericsson.oss.apps.util.TestDefaults.ENODE_BFUNCTION
import static com.ericsson.oss.apps.util.TestDefaults.EUTRAN_CELL_FDD

/**
 * Unit tests for {@link com.ericsson.oss.apps.model.RuleSet} class.
 */
class RuleSetTestSpec extends Specification {

    public static final String NAME = "Test RuleSet"

    def "test setRules should Set Rules And Assign RuleSet"() {
        given:
            final RuleSet objectUnderTest = new RuleSet(NAME)
            final Rule rule1 = new Rule(ENODE_BFUNCTION, "prachConfigEnabled", "true", "")
            final Rule rule2 = new Rule(ENODE_BFUNCTION, "zzzTemporary74", "-2000000000", "")
            final List<Rule> rules = new ArrayList<>()
            rules.add(rule1)
            rules.add(rule2)

        expect:
            objectUnderTest.toString() contains(NAME)

        when:
            objectUnderTest.setRules(rules)
            def actualRules = objectUnderTest.getRules()

        then: "Check that the rules have been set correctly and assigned to the RuleSet instance"
            actualRules.size() == 2
            actualRules == [rule1, rule2]
            (rules).stream()
                    .map(Rule::getRuleSet)
                    .allMatch(objectUnderTest::equals)
    }

    def "test setRules should Remove Previous Rules And Assign New Rules To RuleSet"() {
        given: "Create a RuleSet instance with some initial data"
            final RuleSet objectUnderTest = new RuleSet()
            final UUID ruleSetId = objectUnderTest.getId()
            final Rule rule1 = new Rule(ENODE_BFUNCTION, "prachConfigEnabled", "true", "")
            final Rule rule2 = new Rule(ENODE_BFUNCTION, "zzzTemporary74", "-2000000000", "")
            final List<Rule> rules = new ArrayList<>()
            rules.add(rule1)
            rules.add(rule2)

        when:
            objectUnderTest.setRules(rules)

        and: "Create new rules and set them to the RuleSet instance"
            final Rule rule3 = new Rule(EUTRAN_CELL_FDD, "cellCapMinCellSubCap", "1000", "")
            final Rule rule4 = new Rule(EUTRAN_CELL_FDD, "commonCqiPeriodicity", "80", "")
            final List<Rule> newRules = new ArrayList<>()
            newRules.add(rule3)
            newRules.add(rule4)
            objectUnderTest.setRules(newRules)
            def actualRules = objectUnderTest.getRules()

        then: "Check that the previous rules have been removed from the RuleSet instance"
            actualRules.size() == 2
            !actualRules.contains(rule1)
            !actualRules.contains(rule2)

        and: "Check that the new rules have been assigned to the RuleSet instance"
            newRules.stream()
                    .map(Rule::getRuleSet)
                    .allMatch(objectUnderTest::equals)

        and: "Check that the RuleSet ID has not changed"
            objectUnderTest.getId() == ruleSetId
    }
}