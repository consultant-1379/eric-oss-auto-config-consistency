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

package com.ericsson.oss.apps.repository;

import static com.ericsson.oss.apps.util.TestDefaults.CELL_CAP_MIN_CELL_SUB_CAP;
import static com.ericsson.oss.apps.util.TestDefaults.CELL_CAP_MIN_MAX_WRI_PROT;
import static com.ericsson.oss.apps.util.TestDefaults.CFRA_ENABLE;
import static com.ericsson.oss.apps.util.TestDefaults.ENODE_BFUNCTION;
import static com.ericsson.oss.apps.util.TestDefaults.EUTRAN_CELL_FDD;
import static com.ericsson.oss.apps.util.TestDefaults.PRACH_CONFIG_ENABLED;
import static com.ericsson.oss.apps.util.TestDefaults.ZZZ_TEMPORARY_74;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.apps.model.Rule;
import com.ericsson.oss.apps.model.RuleSet;

import lombok.val;

/**
 * Unit tests for {@link RuleRepository} interface.
 */
@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(MockitoExtension.class)
public class RuleRepositoryTest {
    private static final String SIMPLE_RULESET = "simple_ruleset";
    @Autowired
    private RuleRepository ruleRepository;
    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Test
    void whenPersistRule_verifyExists() {
        val ruleset = createPersistedRuleset(
                "simple_ruleset",
                new Rule(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true",""),
                new Rule(ENODE_BFUNCTION, ZZZ_TEMPORARY_74, "-2000000000",""),
                new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "1000"),
                new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_MAX_WRI_PROT, "true"),
                new Rule(EUTRAN_CELL_FDD, CFRA_ENABLE, "true"),
                new Rule(EUTRAN_CELL_FDD, "commonCqiPeriodicity", "80",""),
                new Rule(EUTRAN_CELL_FDD, "ulVolteCovMobThr", "5",""));
        val rulesetId = ruleset.getId();

        assertThat(ruleRepository.findAllByRuleSetName(ruleset.getName()))
                .hasSize(7)
                .extracting(Rule::getMoType, Rule::getAttributeName, Rule::getAttributeValue, RuleRepositoryTest::getRulesetId)
                .containsExactly(
                        tuple(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true", rulesetId),
                        tuple(ENODE_BFUNCTION, ZZZ_TEMPORARY_74, "-2000000000", rulesetId),
                        tuple(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "1000", rulesetId),
                        tuple(EUTRAN_CELL_FDD, CELL_CAP_MIN_MAX_WRI_PROT, "true", rulesetId),
                        tuple(EUTRAN_CELL_FDD, CFRA_ENABLE, "true", rulesetId),
                        tuple(EUTRAN_CELL_FDD, "commonCqiPeriodicity", "80", rulesetId),
                        tuple(EUTRAN_CELL_FDD, "ulVolteCovMobThr", "5", rulesetId));
    }

    @Test
    void whenCreateSecondRules_verifyExists() {
        final var ruleset = createPersistedRuleset(
                "simple_ruleset",
                new Rule(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true"),
                new Rule(ENODE_BFUNCTION, ZZZ_TEMPORARY_74, "-2000000000"),
                new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "1000", ""),
                new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_MAX_WRI_PROT, "true",""));

        val rulesetId = ruleset.getId();

        assertThat(ruleRepository.findAllByRuleSetName(ruleset.getName()))
                .hasSize(4)
                .extracting(Rule::getMoType, Rule::getAttributeName, Rule::getAttributeValue, RuleRepositoryTest::getRulesetId)
                .containsExactly(
                        tuple(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true", rulesetId),
                        tuple(ENODE_BFUNCTION, ZZZ_TEMPORARY_74, "-2000000000", rulesetId),
                        tuple(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "1000", rulesetId),
                        tuple(EUTRAN_CELL_FDD, CELL_CAP_MIN_MAX_WRI_PROT, "true", rulesetId));

        final var rulesetTwo = createPersistedRuleset(
                "simple_ruleset_2",
                new Rule(EUTRAN_CELL_FDD, CFRA_ENABLE, "true",""),
                new Rule(EUTRAN_CELL_FDD, "commonCqiPeriodicity", "80"),
                new Rule(EUTRAN_CELL_FDD, "ulVolteCovMobThr", "5"));
        val rulesetTwoId = rulesetTwo.getId();

        assertThat(ruleRepository.findAllByRuleSetName(rulesetTwo.getName()))
                .hasSize(3)
                .extracting(Rule::getMoType, Rule::getAttributeName, Rule::getAttributeValue, RuleRepositoryTest::getRulesetId)
                .containsExactly(
                        tuple(EUTRAN_CELL_FDD, CFRA_ENABLE, "true", rulesetTwoId),
                        tuple(EUTRAN_CELL_FDD, "commonCqiPeriodicity", "80", rulesetTwoId),
                        tuple(EUTRAN_CELL_FDD, "ulVolteCovMobThr", "5", rulesetTwoId));
    }

    @Test
    void whenUpdateRules_verifyExists() {
        final var ruleset = createPersistedRuleset(
                "simple_ruleset",
                new Rule(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true"),
                new Rule(ENODE_BFUNCTION, ZZZ_TEMPORARY_74, "-2000000000",""),
                new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "1000"),
                new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_MAX_WRI_PROT, "true",""),
                new Rule(EUTRAN_CELL_FDD, CFRA_ENABLE, "true"),
                new Rule(EUTRAN_CELL_FDD, "commonCqiPeriodicity", "80",""),
                new Rule(EUTRAN_CELL_FDD, "ulVolteCovMobThr", "5"));
        val rulesetId = ruleset.getId();

        assertThat(ruleRepository.findAllByRuleSetName(ruleset.getName()))
                .hasSize(7)
                .extracting(Rule::getMoType, Rule::getAttributeName, Rule::getAttributeValue, RuleRepositoryTest::getRulesetId)
                .containsExactly(
                        tuple(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true", rulesetId),
                        tuple(ENODE_BFUNCTION, ZZZ_TEMPORARY_74, "-2000000000", rulesetId),
                        tuple(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "1000", rulesetId),
                        tuple(EUTRAN_CELL_FDD, CELL_CAP_MIN_MAX_WRI_PROT, "true", rulesetId),
                        tuple(EUTRAN_CELL_FDD, CFRA_ENABLE, "true", rulesetId),
                        tuple(EUTRAN_CELL_FDD, "commonCqiPeriodicity", "80", rulesetId),
                        tuple(EUTRAN_CELL_FDD, "ulVolteCovMobThr", "5", rulesetId));

        ruleset.setRules(List.of(
                new Rule(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true"),
                new Rule(ENODE_BFUNCTION, ZZZ_TEMPORARY_74, "-2000000000", ""),
                new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "1000")));
        ruleSetRepository.saveAndFlush(ruleset);

        assertThat(ruleRepository.findAllByRuleSetName(ruleset.getName()))
                .hasSize(3)
                .extracting(Rule::getMoType, Rule::getAttributeName, Rule::getAttributeValue, RuleRepositoryTest::getRulesetId)
                .containsExactly(
                        tuple(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true", rulesetId),
                        tuple(ENODE_BFUNCTION, ZZZ_TEMPORARY_74, "-2000000000", rulesetId),
                        tuple(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "1000", rulesetId));
    }

    @Test
    void whenFindRulesByRulesetId_andRulesExist_verifyRulesReturned() {
        val ruleset = createPersistedRuleset(
                SIMPLE_RULESET,
                new Rule(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true"),
                new Rule(ENODE_BFUNCTION, ZZZ_TEMPORARY_74, "-2000000000"),
                new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "1000"),
                new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_MAX_WRI_PROT, "true",""),
                new Rule(EUTRAN_CELL_FDD, CFRA_ENABLE, "true",""),
                new Rule(EUTRAN_CELL_FDD, "commonCqiPeriodicity", "80",""),
                new Rule(EUTRAN_CELL_FDD, "ulVolteCovMobThr", "5",""));
        val rulesetId = ruleset.getId();

        assertThat(ruleRepository.findAllByRuleSetId(rulesetId))
                .hasSize(7)
                .extracting(Rule::getMoType, Rule::getAttributeName, Rule::getAttributeValue, RuleRepositoryTest::getRulesetId)
                .containsExactly(
                        tuple(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true", rulesetId),
                        tuple(ENODE_BFUNCTION, ZZZ_TEMPORARY_74, "-2000000000", rulesetId),
                        tuple(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "1000", rulesetId),
                        tuple(EUTRAN_CELL_FDD, CELL_CAP_MIN_MAX_WRI_PROT, "true", rulesetId),
                        tuple(EUTRAN_CELL_FDD, CFRA_ENABLE, "true", rulesetId),
                        tuple(EUTRAN_CELL_FDD, "commonCqiPeriodicity", "80", rulesetId),
                        tuple(EUTRAN_CELL_FDD, "ulVolteCovMobThr", "5", rulesetId));
    }

    private static UUID getRulesetId(final Rule rule) {
        return rule.getRuleSet().getId();
    }

    private RuleSet createPersistedRuleset(final String name, final Rule... rules) {
        final var ruleSet = new RuleSet(name);
        for (final Rule rule : rules) {
            rule.setRuleSet(ruleSet);
            ruleSet.getRules().add(rule);
        }
        return ruleSetRepository.saveAndFlush(ruleSet);
    }
}
