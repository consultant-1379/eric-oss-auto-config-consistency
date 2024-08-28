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

import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

import com.ericsson.oss.apps.model.Rule;
import com.ericsson.oss.apps.repository.RuleRepository;
import com.ericsson.oss.apps.util.ConditionsUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for retrieving and grouping {@link Rule}s from a CSV.
 */
@Slf4j
@Service
public class RuleService {

    private final RuleRepository ruleRepository;

    public RuleService(final RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Retryable(retryFor = {
            DataAccessException.class, TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}",
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public List<Rule> getRulesForRuleset(final String rulesetName) {
        return ruleRepository.findAllByRuleSetName(rulesetName);
    }

    @Retryable(retryFor = {
            DataAccessException.class, TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}",
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public List<Rule> getRulesForRulesetId(final UUID uuid) {
        return ruleRepository.findAllByRuleSetId(uuid);
    }

    public Map<String, List<Rule>> getRulesByMoType(final List<Rule> rules) {
        log.info("Grouping rules by MoType...");
        return rules.stream().collect(groupingBy(Rule::getMoType));
    }

    public Map<String, Map<String, List<String>>> getAttributesFromMoLevelRules(final Map<String, Map<String, List<Rule>>> fdnToRulesByMoType) {
        final Map<String, Map<String, List<String>>> fdnToAttrByMoType = new HashMap<>();

        fdnToRulesByMoType.forEach((fdn, moToRules) -> moToRules.forEach((moType, rules) -> {
            final List<String> validAttributes = rules.stream()
                    .flatMap(rule -> {
                        final ArrayList<String> attributes = new ArrayList<>();
                        attributes.add(rule.getAttributeName());
                        attributes.addAll(ConditionsUtils.extractAttributeNamesFromConditions(rule.getConditions(), rule.getMoType()));
                        return attributes.stream();
                    })
                    .distinct()
                    .toList();

            if (!validAttributes.isEmpty()) {
                fdnToAttrByMoType.computeIfAbsent(fdn, innerMap -> new HashMap<>())
                        .put(moType, validAttributes);
            }
        }));

        return fdnToAttrByMoType;
    }
}
