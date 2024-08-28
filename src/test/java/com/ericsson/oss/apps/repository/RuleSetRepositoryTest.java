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

import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_UUID;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

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

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(MockitoExtension.class)
public class RuleSetRepositoryTest {
    private static final String SIMPLE_RULESET = "simple_ruleset";
    @Autowired
    private RuleSetRepository objectUnderTest;

    @Test
    void whenExistsRulesetByName_andRulesetExists_verifyTrueIsReturned() {
        createPersistedRuleset(SIMPLE_RULESET);

        assertThat(objectUnderTest.existsByName(SIMPLE_RULESET))
                .isTrue();
    }

    @Test
    void whenExistsRulesetByName_andRulesetDoesNotExist_verifyFalseReturned() {
        assertThat(objectUnderTest.existsByName(SIMPLE_RULESET)).isFalse();
    }

    @Test
    void whenFindRulesetByName_andRulesetExists_verifyRulesetIsReturned() {
        val ruleset = createPersistedRuleset(SIMPLE_RULESET);

        assertThat(objectUnderTest.findByName(SIMPLE_RULESET))
                .isNotEmpty()
                .contains(ruleset);
    }

    @Test
    void whenFindRulesetByName_andMultipleRulesetsExists_verifyCorrectRulesetIsReturned() {
        createPersistedRuleset("another_simple_ruleset");
        val ruleset = createPersistedRuleset(SIMPLE_RULESET);

        assertThat(objectUnderTest.findByName(SIMPLE_RULESET))
                .isNotEmpty()
                .contains(ruleset);
    }

    @Test
    void whenFindRulesetByName_andRulesetDoesNotExist_verifyEmptyOptionalReturned() {
        assertThat(objectUnderTest.findByName(SIMPLE_RULESET)).isEmpty();
    }

    @Test
    void whenExistsRulesetById_andRulesetExists_verifyTrueReturned() {
        val ruleset = createPersistedRuleset(SIMPLE_RULESET);

        assertThat(objectUnderTest.existsById(ruleset.getId())).isTrue();
    }

    @Test
    void whenExistsRulesetById_andRulesetsDoesNotExist_verifyFalseReturned() {
        createPersistedRuleset(SIMPLE_RULESET);

        assertThat(objectUnderTest.existsById(DEFAULT_UUID)).isFalse();
    }

    @Test
    void whenFindRulesetById_andRulesetExists_verifyRulesetIsReturned() {
        val ruleSet = createPersistedRuleset(SIMPLE_RULESET);
        val uuid = ruleSet.getId();

        val retrievedRuleSet = objectUnderTest.getById(uuid);

        assertThat(retrievedRuleSet)
                .isNotNull()
                .isEqualTo(ruleSet);
    }

    private RuleSet createPersistedRuleset(final String name, final Rule... rules) {
        val ruleSet = new RuleSet(name);
        ruleSet.setRules(Arrays.asList(rules));
        return objectUnderTest.saveAndFlush(ruleSet);
    }
}
