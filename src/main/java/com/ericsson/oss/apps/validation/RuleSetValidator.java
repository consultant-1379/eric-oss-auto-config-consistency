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

package com.ericsson.oss.apps.validation;

import static com.ericsson.oss.apps.util.Constants.CONDITION_MAX_LENGTH;
import static com.ericsson.oss.apps.util.Constants.FDN;
import static com.ericsson.oss.apps.util.RanUtilities.convertFdnToCmHandle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.oss.apps.model.Rule;
import com.ericsson.oss.apps.util.ConditionsUtils;
import com.ericsson.oss.apps.util.StringUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * A utility class to handle the ruleset file validation.
 */
@Slf4j
@Service
public class RuleSetValidator {
    private static final Pattern MO_TYPE_DOT_ATTR_REGEX = Pattern.compile("([A-Za-z0-9]+)\\.([A-Za-z0-9.]+)\\s*");
    //matches anything left of =, <, <=, >, >=, !=, like, in
    private static final Pattern LEFT_OF_OPERATOR_REGEX = Pattern.compile("([A-Za-z0-9.]+)\\s+(<|<=|!=|>|>=|=|like|in)\\s+");
    private static final String INVALID_CONDITION = "Invalid Condition.";

    private final ModelManager modelManager;

    @Autowired
    public RuleSetValidator(final ModelManager modelManager) {
        this.modelManager = modelManager;
    }

    /**
     * Validates the rules
     *      Including: Network level validation, 'condition' constraints & 'priority' constraints.
     * 
     * @param rulesList
     *            A {@link List} of {@link Rule}s to be validated
     * @throws RuleValidationException
     *             thrown if any rule validation is failed.
     */
    public void validateRules(final List<Rule> rulesList) throws RuleValidationException {
        final List<RuleValidationError> ruleValidationErrorList = Collections.synchronizedList(new ArrayList<>());
        final Set<String> uniqueMoTypeAttrNamePriorityCombos = ConcurrentHashMap.newKeySet();

        final Map<String, List<Rule>> groupedRules = rulesList.parallelStream()
                .collect(Collectors.groupingBy(rule -> rule.getMoType() + "_" + rule.getAttributeName()));

        groupedRules.entrySet().forEach(entry -> {
            final int numberOfRules = entry.getValue().size();

            entry.getValue().forEach(rule -> {

                final String moTypeAttrNamePriorityCombination = rule.getMoType() + "_" + rule.getAttributeName() + "_" + rule.getPriority();
                if (isInvalidMo(rule)) {
                    ruleValidationErrorList.add(new RuleValidationError(rule.getLineNumber(), "Invalid MO.",
                            "MO not found in Managed Object Model.", ""));
                } else if (!isValidAttribute(rule)) { //NOPMD
                    ruleValidationErrorList.add(new RuleValidationError(rule.getLineNumber(), "Invalid Attribute name.",
                            "Attribute not found in Managed Object Model.", ""));
                } else if (!isValidValue(rule)) { //NOPMD
                    ruleValidationErrorList.add(new RuleValidationError(rule.getLineNumber(), "Invalid Attribute value.",
                            "Attribute value is invalid according to the Managed Object Model.", ""));
                } else if (!StringUtil.isNullOrBlank(rule.getConditions())) { //NOPMD
                    ruleValidationErrorList.addAll(validateConditionsReturningErrors(rule));
                }
                if (!uniqueMoTypeAttrNamePriorityCombos.add(moTypeAttrNamePriorityCombination)) { //NOPMD
                    ruleValidationErrorList.add(new RuleValidationError(rule.getLineNumber(), "Invalid Rule values combination.",
                            "This moType, attribute name and priority combination already exists.", ""));
                }
                if (numberOfRules > 1 && rule.getPriority() == null) { //NOPMD
                    ruleValidationErrorList.add(new RuleValidationError(rule.getLineNumber(), "Invalid priority value.",
                            "Priority value must be given if the moType & attrName combination is not unique.", ""));
                }
            });
        });

        if (!ruleValidationErrorList.isEmpty()) {
            log.warn("A total of '{}' errors were found.", ruleValidationErrorList.size());
            throw new RuleValidationException("Errors exist in rules.", ruleValidationErrorList);
        }
    }

    private List<RuleValidationError> validateConditionsReturningErrors(final Rule ruleWithNonNullConditions) {
        final String conditions = ruleWithNonNullConditions.getConditions().trim();
        final Long lineNumber = ruleWithNonNullConditions.getLineNumber();
        final List<RuleValidationError> result = new ArrayList<>();
        if (!isConditionsLengthValid(ruleWithNonNullConditions)) { //NOPMD
            result.add(new RuleValidationError(lineNumber, INVALID_CONDITION,
                    "Conditions too long, cannot be more than %d characters.".formatted(CONDITION_MAX_LENGTH), ""));
        }
        if (!isConditionSyntaxValid(ruleWithNonNullConditions)) { //NOPMD
            result.add(new RuleValidationError(lineNumber, INVALID_CONDITION,
                    "Condition has invalid syntax.", ""));
        } else if (conditionsContainsAttributeWithoutMoType(conditions)) {
            result.add(new RuleValidationError(lineNumber, INVALID_CONDITION,
                    "MoType not specified, Attributes must be specified as '<MO Type>.<Attribute Name>'",
                    "Attributes must be specified as '<MO Type>.<Attribute Name>'"));
        } else {
            final Pair<Set<String>, Set<String>> moTypesAndAttributesInConditions = extractMoTypesAndAttributesFromConditions(conditions);
            final Set<String> moTypesInConditions = moTypesAndAttributesInConditions.getLeft();
            final String moType = ruleWithNonNullConditions.getMoType();
            if (!moTypesInConditions.isEmpty()) {
                if (!Set.of(moType).equals(moTypesInConditions)) { //NOPMD
                    result.add(new RuleValidationError(lineNumber, INVALID_CONDITION,
                            "Attributes in Conditions must be from the moType field (%s)'s attributes".formatted(moType), ""));
                } else if (conditionsContainInvalidAttributes(ruleWithNonNullConditions, moTypesAndAttributesInConditions)) {
                    result.add(new RuleValidationError(lineNumber, INVALID_CONDITION,
                            "Attribute(s) in Conditions not found in Managed Object model.", ""));
                }
            }
        }
        return result;
    }

    private boolean conditionsContainInvalidAttributes(final Rule rule, final Pair<Set<String>, Set<String>> moTypesAndAttributesInConditions) {
        final Set<String> invalidAttributes = moTypesAndAttributesInConditions.getRight().stream()
                .filter(attributeInCondition -> !modelManager.isValidAttribute(rule.getMoType(), attributeInCondition))
                .collect(Collectors.toSet());
        final boolean foundInvalidAttributes = !invalidAttributes.isEmpty();
        if (foundInvalidAttributes) {
            log.error("Invalid attribute(s) ({}) found in conditions, line number:{}, ", invalidAttributes, rule.getLineNumber());
        }
        return foundInvalidAttributes;
    }

    private boolean conditionsContainsAttributeWithoutMoType(final String conditions) {
        final Matcher leftHandSideMatcher = LEFT_OF_OPERATOR_REGEX.matcher(conditions);
        while (leftHandSideMatcher.find()) {
            final String leftHandSide = leftHandSideMatcher.group(1).trim();
            if (!FDN.equals(leftHandSide) &&
                    !(StringUtil.containsExactlyOnce(leftHandSide, "."))) {
                log.error("moType not specified in conditions");
                return true;
            }
        }

        return false;
    }

    private Pair<Set<String>, Set<String>> extractMoTypesAndAttributesFromConditions(final String conditions) {
        final Set<String> moTypes = new HashSet<>();
        final Set<String> attributes = new HashSet<>();

        final Matcher moAttrMatcher = MO_TYPE_DOT_ATTR_REGEX.matcher(conditions.trim());
        while (moAttrMatcher.find()) {
            moTypes.add(moAttrMatcher.group(1));
            attributes.add(moAttrMatcher.group(2));
        }

        return Pair.of(moTypes, attributes);
    }

    /**
     * Validate the rules against the model of the specific FDN.
     *
     * @param fdns
     *            A {@link Collection} of the fdns in scope.
     * @param rulesByMoType
     *            A {@link Map} of the rules sorted by Mo Type.
     * @return fdnToRulesByMoType A {@link Map} of the applicable rules for each fdn
     */
    public Map<String, Map<String, List<Rule>>> validateNodeLevelRules(final Collection<String> fdns, final Map<String, List<Rule>> rulesByMoType) {
        final Map<String, Map<String, List<Rule>>> fdnToRulesByMoType = new ConcurrentHashMap<>();

        fdns.parallelStream().forEach(fdn -> rulesByMoType.forEach((moType, rules) -> {
            if (modelManager.isValidMo(moType, convertFdnToCmHandle(fdn))) {
                final String cmHandle = convertFdnToCmHandle(fdn);
                final List<Rule> validRules = rules.stream()
                        .filter(rule -> isValidRule(moType, rule, cmHandle))
                        .toList();
                if (!validRules.isEmpty()) {
                    fdnToRulesByMoType.computeIfAbsent(fdn, innerMap -> new HashMap<>())
                            .put(moType, validRules);
                }
            }
        }));
        log.info("The total number of FDN's, after rule validation per fdn, to be audited is: '{}' out of '{}'.",
                fdnToRulesByMoType.size(), fdns.size());
        return fdnToRulesByMoType;
    }

    private boolean isValidRule(final String moType, final Rule rule, final String cmHandle) {
        return modelManager.isValidAttribute(moType, rule.getAttributeName(), cmHandle) &&
                modelManager.isValidValue(moType, rule.getAttributeName(), rule.getAttributeValue(), cmHandle)
                && validateNodeLevelAttributesInConditions(moType, rule, cmHandle);
    }

    private boolean validateNodeLevelAttributesInConditions(final String moType, final Rule rule, final String cmHandle) {
        final String conditions = rule.getConditions();
        return conditions == null ||
                extractMoTypesAndAttributesFromConditions(conditions).getRight().stream()
                        .filter(attributeName -> !isValidAttribute(moType, rule, cmHandle, attributeName, conditions))
                        .collect(Collectors.toSet()).isEmpty();
    }

    private boolean isInvalidMo(final Rule rule) {
        return !modelManager.isValidMo(rule.getMoType());
    }

    private boolean isValidAttribute(final String moType, final Rule rule, final String cmHandle, final String attributeName,
            final String conditions) {
        if (modelManager.isValidAttribute(moType, attributeName, cmHandle)) {
            return true;
        }
        log.info("Conditions <{}> contain invalid attribute <{}.{}> for cmHandle <{}>. Line number {} will be ignored.",
                conditions, moType, attributeName, cmHandle, rule.getLineNumber());
        return false;
    }

    private boolean isValidAttribute(final Rule rule) {
        return modelManager.isValidAttribute(rule.getMoType(), rule.getAttributeName());
    }

    private boolean isValidValue(final Rule rule) {
        return modelManager.isValidValue(rule.getMoType(), rule.getAttributeName(), rule.getAttributeValue());
    }

    private boolean isConditionSyntaxValid(final Rule rule) {
        return ConditionsUtils.isValidSyntax(rule);
    }

    private boolean isConditionsLengthValid(final Rule rule) {
        return ConditionsUtils.isValidLength(rule);
    }
}
