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

package com.ericsson.oss.apps.executor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.ericsson.oss.apps.model.AuditResult;
import com.ericsson.oss.apps.model.EaccManagedObject;
import com.ericsson.oss.apps.model.Execution;
import com.ericsson.oss.apps.model.Rule;

import lombok.extern.slf4j.Slf4j;

/**
 * Class to check for inconsistencies.
 */
@Slf4j
public final class ConsistencyChecker {

    private ConsistencyChecker() {

    }

    /**
     * Method to check for inconsistencies.
     *
     * @param eaccManagedObject
     *            the {@link EaccManagedObject} to be checked for inconsistencies
     * @param moRuleSet
     *            {@link List} of {@link Rule}s to be checked against the {@link EaccManagedObject}
     * @param executionId
     *            The {@link Execution} id being run
     * @return {@link ConsistencyCheckResult} containing a {@link List} of {@link AuditResult}s and the number of inconsistencies found
     */
    public static ConsistencyCheckResult check(final EaccManagedObject eaccManagedObject, final List<Rule> moRuleSet,
                                               final String executionId) {
        int inconsistencyCount = 0;
        final List<AuditResult> auditResultList = new ArrayList<>();
        final Set<String> processedCombinations = new HashSet<>();

        // Sort the rules in descending order to check highest priority first
        moRuleSet.sort(Comparator.comparingInt((final Rule rule) -> {
            final Integer priority = rule.getPriority();
            return priority == null ? Integer.MIN_VALUE : priority;
        }).reversed());

        for (final Rule moRule : moRuleSet) {
            final String key = getKey(moRule);

            if (skipRule(eaccManagedObject, processedCombinations, moRule, key)) {
                continue;
            }

            final Object attActualValue = eaccManagedObject.getAttributes().get(moRule.getAttributeName());
            final String attStringValue = attActualValue == null ? "null" : attActualValue.toString();
            final String origRuleValue = moRule.getAttributeValue();
            final String ruleValue = isUpperCaseBoolean(origRuleValue) ? origRuleValue.toLowerCase(Locale.ROOT) : origRuleValue;
            final AuditStatus auditStatus;
            if (attStringValue.equals(ruleValue)) {
                auditStatus = AuditStatus.CONSISTENT;
            } else {
                auditStatus = AuditStatus.INCONSISTENT;
                inconsistencyCount++;
            }

            final AuditResult auditResult = new AuditResult();
            auditResult.setExecutionId(executionId);
            auditResult.setManagedObjectFdn(eaccManagedObject.getFdn());
            auditResult.setManagedObjectType(moRule.getMoType());
            auditResult.setAttributeName(moRule.getAttributeName());
            auditResult.setCurrentValue(attStringValue);
            auditResult.setPreferredValue(ruleValue);
            auditResult.setAuditStatus(auditStatus);
            auditResult.setRuleId(moRule.getId().toString());
            auditResult.setChangeStatus(ChangeStatus.NOT_APPLIED);
            auditResultList.add(auditResult);
            processedCombinations.add(key);
        }
        return new ConsistencyCheckResult(auditResultList, inconsistencyCount);
    }

    private static boolean skipRule(final EaccManagedObject eaccManagedObject,
                                    final Set<String> processedCombinations, final Rule moRule, final String key) {
        if (ruleConditionNotMet(eaccManagedObject, moRule)) {
            log.debug("Skipping rule as the condition was not met for MO '{}'. The rule being skipped is: {}",
                    eaccManagedObject.getFdn(), moRule);
            return true;
        } else if (processedCombinations.contains(key)) {
            log.debug("Skipping rule as a higher priority rule was already implemented. The rule being skipped is: {}", moRule);
            return true;
        }
        return false;
    }

    private static boolean isUpperCaseBoolean(final String attributeValue) {
        return (("TRUE".equals(attributeValue)) || ("FALSE".equals(attributeValue)));
    }

    private static boolean ruleConditionNotMet(final EaccManagedObject eaccManagedObject, final Rule moRule) {
        return (moRule.getConditions() != null && !moRule.getConditions().isEmpty() &&
                !ConditionsProcessor.evaluate(moRule.getConditions(), moRule.getMoType(), eaccManagedObject));
    }

    private static String getKey(final Rule rule) {
        return rule.getMoType() + "_" + rule.getAttributeName();
    }

}
