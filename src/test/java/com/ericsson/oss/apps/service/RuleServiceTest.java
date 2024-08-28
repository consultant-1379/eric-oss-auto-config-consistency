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

package com.ericsson.oss.apps.service;

import static com.ericsson.oss.apps.util.TestDefaults.CELL_CAP_MIN_CELL_SUB_CAP;
import static com.ericsson.oss.apps.util.TestDefaults.CELL_CAP_MIN_MAX_WRI_PROT;
import static com.ericsson.oss.apps.util.TestDefaults.CFRA_ENABLE;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_RULESET_NAME;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_UUID;
import static com.ericsson.oss.apps.util.TestDefaults.ENODE_BFUNCTION;
import static com.ericsson.oss.apps.util.TestDefaults.EUTRAN_CELL_FDD;
import static com.ericsson.oss.apps.util.TestDefaults.MANAGED_ELEMENT_1_FDN;
import static com.ericsson.oss.apps.util.TestDefaults.MANAGED_ELEMENT_2_FDN;
import static com.ericsson.oss.apps.util.TestDefaults.PRACH_CONFIG_ENABLED;
import static java.util.stream.Collectors.groupingBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionException;

import com.ericsson.oss.apps.CoreApplication;
import com.ericsson.oss.apps.CoreApplicationTest;
import com.ericsson.oss.apps.model.Rule;
import com.ericsson.oss.apps.repository.RuleRepository;

/**
 * Unit tests for {@link RuleService} class.
 */
@ActiveProfiles("test")
@AutoConfigureObservability
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = { CoreApplication.class, CoreApplicationTest.class })
public class RuleServiceTest {
    private static final String USER_LABEL = "userLabel";

    @Autowired
    private RuleService objectUnderTest;
    @MockBean
    private RuleRepository ruleRepository;

    @Value("${database.retry.userInitiated.maxAttempts}")
    private int maxAttempts;

    final List<Rule> rules = List.of(
            new Rule(ENODE_BFUNCTION, "forcedSiTunnelingActive", "true"),
            new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "1000",""),
            new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_MAX_WRI_PROT, "true",""),
            new Rule(EUTRAN_CELL_FDD, CFRA_ENABLE, "true",""));
    @Test
    void whenGetAllForRuleset_thenRulesAreReturned() {
        when(ruleRepository.findAllByRuleSetName(any())).thenReturn(rules);

        assertThat(objectUnderTest.getRulesForRuleset(DEFAULT_RULESET_NAME))
                .containsExactlyElementsOf(rules);
    }

    @Test
    public void whenListOfRulesIsGiven_ThenRulesAreGroupedByMoType() {
        final Map<String, List<Rule>> rulesByMo = objectUnderTest.getRulesByMoType(rules);
        assertThat(rulesByMo).containsExactly(
                entry(ENODE_BFUNCTION, rules.subList(0, 1)),
                entry(EUTRAN_CELL_FDD, rules.subList(1, 4)));
    }

    @Test
    public void whenGetRulesById_thenRulesAreReturned() {
        when(ruleRepository.findAllByRuleSetId(DEFAULT_UUID)).thenReturn(rules);

        assertThat(objectUnderTest.getRulesForRulesetId(DEFAULT_UUID))
                .containsExactlyElementsOf(rules);

    }

    @Test
    public void whenGetRulesForRulesetMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(ruleRepository.findAllByRuleSetName(any())).thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.getRulesForRuleset("rulesetName")).isInstanceOf(DataAccessException.class);
        verify(ruleRepository, times(maxAttempts)).findAllByRuleSetName(any());
    }

    @Test
    public void whenGetRulesForRulesetIdMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(ruleRepository.findAllByRuleSetId(any())).thenThrow(CannotCreateTransactionException.class);
        assertThatThrownBy(() -> objectUnderTest.getRulesForRulesetId(new UUID(1,1))).isInstanceOf(TransactionException.class);
        verify(ruleRepository, times(maxAttempts)).findAllByRuleSetId(any());
    }

    @Test
    public void whenGetAttributesFromMoLevelRulesIsCalled_withMultipleFdns_thenCorrectAttributesAreReturned() {
        final var additionalAttr = "outOfCoverageThreshold";
        final List<Rule> me1RulesByMo = createListOfValidRules();
        me1RulesByMo.add(new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_MAX_WRI_PROT, "1000", "EUtranCellFDD.%s in (400,500,600)".formatted(additionalAttr)));
        final List<Rule> me2RulesByMo = createListOfValidRules();
        me2RulesByMo.addAll(createListOfValidRules());

        final Map<String, Map<String, List<Rule>>> fdnToRulesByMoType = Map.of(
                MANAGED_ELEMENT_1_FDN, me1RulesByMo.stream().collect(groupingBy(Rule::getMoType)),
                MANAGED_ELEMENT_2_FDN, me2RulesByMo.stream().collect(groupingBy(Rule::getMoType)));

        final var result = objectUnderTest.getAttributesFromMoLevelRules(fdnToRulesByMoType);

        assertThat(result)
                .hasSize(2)
                .hasEntrySatisfying(MANAGED_ELEMENT_1_FDN, (_rulesByMoType) -> assertThat(_rulesByMoType)
                        .as("%s rules by MO type".formatted(MANAGED_ELEMENT_1_FDN))
                        .hasSize(2)
                        .containsEntry(ENODE_BFUNCTION, List.of(PRACH_CONFIG_ENABLED))
                        .containsEntry(EUTRAN_CELL_FDD, List.of(CELL_CAP_MIN_CELL_SUB_CAP, USER_LABEL, CELL_CAP_MIN_MAX_WRI_PROT, additionalAttr)))
                .hasEntrySatisfying(MANAGED_ELEMENT_2_FDN, (_rulesByMoType) -> assertThat(_rulesByMoType)
                        .as("%s rules by MO type".formatted(MANAGED_ELEMENT_2_FDN))
                        .hasSize(2)
                        .containsEntry(ENODE_BFUNCTION, List.of(PRACH_CONFIG_ENABLED))
                        .containsEntry(EUTRAN_CELL_FDD, List.of(CELL_CAP_MIN_CELL_SUB_CAP, USER_LABEL)));
    }

    private List<Rule> createListOfValidRules() {
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true", ""));
        rules.add(new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "1000", "EUtranCellFDD.userLabel like '%dg2%'"));
        return rules;
    }
}
