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

package com.ericsson.oss.apps.util;

import static com.ericsson.oss.apps.util.Constants.CONDITION_MAX_LENGTH;
import static com.ericsson.oss.apps.util.Constants.FDN;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.ericsson.oss.apps.model.Rule;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public final class ConditionsUtils {
    private static final String IN_CONDITION = " in ";
    private static final String LIKE_CONDITION = " like ";
    private static final String EQUALITY_CONDITION = " = ";
    private static final String DOUBLE_EQUALS = " == ";
    private static final String ATTRIBUTE_NAME_EXTRACT_REGEX = "\\.[a-zA-Z_0-9]*";
    private static final String DOT = ".";
    private static final String EMPTY_STRING = "";
    private static final String HASH = "#";
    private static final String AND_OR = " and | or ";
    private static final int ATTR_NAME_INDEX = 0;
    private static final int ATTR_VALUE_INDEX = 1;
    private static final String SPEL_LIKE = " matches ";
    private static final String LIKE_WILDCARD = "%";
    private static final String SPEL_LIKE_WILDCARD = ".*?";
    private static final String IN_VALUES_START = "(";
    private static final String SPEL_LIST_VALUES_START = "{";
    private static final String IN_VALUES_END = ")";
    private static final String SPEL_LIST_VALUES_END = "}";
    private static final String SPEL_IN_START = ".contains(";
    private static final String SPEL_IN_END = ")";
    private static final String SPEL_LIST_VALUES_START_WITH_PRECEDENCE = "({";
    private static final String IN_VALUES_END_WITH_PRECEDENCE = "))";
    private static final String SPEL_IN_END_WITH_PRECEDENCE = "))";
    private static final String SPEL_FDN = "#FDN";

    public static Set<String> extractAttributeNamesFromConditions(final String conditions, final String moType) {
        final Set<String> attributes = new HashSet<>();
        if (conditions != null && !conditions.isEmpty()) {
            final Pattern regex = Pattern.compile(moType + ATTRIBUTE_NAME_EXTRACT_REGEX);
            final Matcher matcher = regex.matcher(conditions);
            while (matcher.find()) {
                attributes.add((matcher.group().replace(moType + DOT, EMPTY_STRING)).trim());
            }
        }
        return attributes;
    }

    public static String convertToSpel(final String condition, final String moType) {
        final String preProcessedCondition = condition.replace(moType + DOT, HASH).replace(EQUALITY_CONDITION, DOUBLE_EQUALS);
        String spelExpression = preProcessedCondition;

        if (preProcessedCondition.contains(LIKE_CONDITION)) {
            spelExpression = buildSpelExprForLike(preProcessedCondition);
        }

        if (preProcessedCondition.contains(IN_CONDITION)) {
            spelExpression = buildSpelExprForIn(spelExpression);
        }

        if (preProcessedCondition.contains(FDN)) {
            spelExpression = spelExpression.replace(FDN, SPEL_FDN);
        }

        return spelExpression;
    }

    public static boolean isValidSyntax(final Rule rule) {
        final String nullable = rule.getConditions();
        if (nullable == null) {
            log.debug("null Condition is valid.");
            return true;
        }
        final String conditionString = nullable.trim();
        if (conditionString.isEmpty()) {
            log.debug("Empty Condition is valid.");
            return true;
        }
        final String moType = rule.getMoType();
        final String spel = ConditionsUtils.convertToSpel(conditionString, moType);
        final SpelExpressionParser parser = new SpelExpressionParser();
        try {
            parser.parseRaw(spel);
            log.debug("Condition:<{}> syntax is valid.", conditionString);
        } catch (final IllegalStateException | SpelParseException e) { //NOSONAR Rule:Exception should not be Logged as it is not-sanitised
            log.error("Condition syntax is invalid at lineNumber:{}", rule.getLineNumber());
            return false;
        }
        return true;
    }

    public static boolean isValidLength(final Rule rule) {
        final String conditions = rule.getConditions();
        if (StringUtil.isNullOrBlank(conditions)) {
            log.debug("null or empty Condition is valid.");
            return true;
        }
        final String conditionsString = conditions.trim();
        if (conditionsString.length() > CONDITION_MAX_LENGTH) {
            log.error("Conditions too long, cannot be more than {} characters at lineNumber:{}",
                    CONDITION_MAX_LENGTH, rule.getLineNumber());
            return false;
        }
        return true;
    }

    private static String buildSpelExprForIn(final String inputExpression) {
        final String[] individualConditions = inputExpression.split(AND_OR);
        String spelExpression = inputExpression;
        for (final String condition : individualConditions) {
            if (condition.contains(IN_CONDITION)) {
                final String[] splitCondition = condition.split(IN_CONDITION);
                final String inExpression = createSpelIn(splitCondition[ATTR_NAME_INDEX], splitCondition[ATTR_VALUE_INDEX]);
                spelExpression = spelExpression.replace(condition, inExpression);
            }
        }
        return spelExpression;
    }

    private static String createSpelIn(final String attributeName, final String attributeValues) {
        String attrValueCopy = attributeValues;
        if (attributeValues.endsWith(IN_VALUES_END_WITH_PRECEDENCE)) {
            attrValueCopy = attributeValues.replace(IN_VALUES_END_WITH_PRECEDENCE, IN_VALUES_END);
        }
        return attrValueCopy.trim()
                .replace(IN_VALUES_START, calcStartDelimiter(attributeName))
                .replace(IN_VALUES_END, SPEL_LIST_VALUES_END + SPEL_IN_START)
                .concat(attributeName.trim().replace(IN_VALUES_START, EMPTY_STRING))
                .concat(calcEndDelimiter(attributeValues));
    }

    private static String calcStartDelimiter(final String attributeName) {
        if (attributeName.startsWith(IN_VALUES_START)) {
            return SPEL_LIST_VALUES_START_WITH_PRECEDENCE;
        } else {
            return SPEL_LIST_VALUES_START;
        }
    }

    private static String calcEndDelimiter(final String attributeValues) {
        if (attributeValues.endsWith(IN_VALUES_END_WITH_PRECEDENCE)) {
            return SPEL_IN_END_WITH_PRECEDENCE;
        } else {
            return SPEL_IN_END;
        }
    }

    private static String buildSpelExprForLike(final String condition) {
        return condition.replace(LIKE_CONDITION, SPEL_LIKE).replace(LIKE_WILDCARD, SPEL_LIKE_WILDCARD);
    }
}
