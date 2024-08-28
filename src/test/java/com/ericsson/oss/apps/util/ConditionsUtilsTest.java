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
import static com.ericsson.oss.apps.util.Constants.EMPTY_STRING;
import static com.ericsson.oss.apps.util.TestDefaults.ENODEBFUNCTION;
import static com.ericsson.oss.apps.util.TestDefaults.ENODE_BFUNCTION;
import static com.ericsson.oss.apps.util.TestDefaults.INJECTED_ERROR_MESSAGE_TO_VALIDATE_TEST;
import static com.ericsson.oss.apps.util.TestDefaults.INJECTED_LOG_TO_VALIDATE_TEST;
import static com.ericsson.oss.apps.util.TestDefaults.TEST_SETUP_ERROR;
import static com.ericsson.oss.apps.util.TestDefaults.TEST_SETUP_ERROR_UNABLE_TO_ACCESS_LOGS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.apps.model.Rule;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;

@ExtendWith({ MockitoExtension.class, SoftAssertionsExtension.class })
class ConditionsUtilsTest {

    private static final AtomicLong LINE_NUMBER = new AtomicLong();
    private static final Map<String, String> OPERATOR_AND_INVALID_REPLACEMENT_MAP = Map.of(
            " like '", " like ",
            "like ", " kinda like ",
            " in (", " in ",
            "<", "(",
            "=", ")",
            ">", "#",
            " ", "=*/<.",
            " or ", " ",
            " and ", " ");
    private static final String ATTRIBUTE_NAME = "attributeName";
    private static final String ATTRIBUTE_VALUE = "attributeValue";
    private static final String MO_TYPE = "moType";
    private static final String LONGEST_VALID_CONDITION = "ENodeBFunction.releaseInactiveUesMpLoadLevel in ('HIGH_LOAD','VERY_HIGH_LOAD') and FDN " +
            "like '%SubNetwork=ONRM_ROOT_MO,SubNetwork=MKT_145,MeContext=145306_PALM_BAY_UTILITY,ManagedElement=145306_PALM_BAY_UTILITY," +
            "ENodeBFunction=1%' and ENodeBFunction.userLabel like '%SubNetwork=ONRM_ROOT_MO,SubNetwork=MKT_145,MeContext=145306_PALM_BAY_UTILITY," +
            "ManagedElement=145306_PALM_BAY_UTILITY%' or ENodeBFunction.userLabel like '%SubNetwork=ONRM_ROOT_MO,SubNetwork=MKT_145," +
            "MeContext=145307_PALM_BAY_UTILITY,ManagedElement=%'";

    private static InMemoryLogAppender logAppender;

    @InjectSoftAssertions
    private SoftAssertions softly;

    @BeforeEach
    public void initLogging() {
        if (logAppender != null) {
            logAppender.stop();
        }
        final Logger logUnderTest = (Logger) LoggerFactory.getLogger(ConditionsUtils.class);
        logUnderTest.setLevel(Level.DEBUG);
        logAppender = new InMemoryLogAppender();
        logAppender.start();
        logUnderTest.addAppender(logAppender);
        logUnderTest.error(INJECTED_LOG_TO_VALIDATE_TEST);
        logUnderTest.error(EMPTY_STRING, new IllegalArgumentException(INJECTED_ERROR_MESSAGE_TO_VALIDATE_TEST));
        LINE_NUMBER.set(0L);
    }

    @ParameterizedTest
    @CsvFileSource(resources = { "/conditions_test_files/user_input_condition_to_spel_expression.csv" }, delimiter = ':', numLinesToSkip = 1)
    void testConditionSyntaxFailureBoolean(final String conditions, final String moType) {
        OPERATOR_AND_INVALID_REPLACEMENT_MAP.entrySet().stream()
                .filter(validAndSubstitution -> conditions.contains(validAndSubstitution.getKey()))
                .map(validAndSubstitution -> conditions.replace(validAndSubstitution.getKey(), validAndSubstitution.getValue()))
                .map(invalidCondition -> new Rule(moType, ATTRIBUTE_NAME, ATTRIBUTE_VALUE, invalidCondition))
                .peek(ruleWithInvalidCondition -> ruleWithInvalidCondition.setLineNumber(LINE_NUMBER.incrementAndGet()))
                .peek(ruleWithInvalidCondition -> softly.assertThat(ConditionsUtils.isValidSyntax(ruleWithInvalidCondition))
                        .as("<%s> should be invalid", ruleWithInvalidCondition.getConditions())
                        .isFalse())
                .forEach(this::assertLogContainsLineNumberButNotRawCondition);

        logAppender.stop();
    }

    @ParameterizedTest
    @CsvFileSource(resources = { "/conditions_test_files/attributename_extraction_from_conditions.csv" }, delimiter = ':', numLinesToSkip = 1)
    void testExtractAttributeNamesFromConditions(final String conditions, final String moType, final String attributeNames) {
        final Set<String> expectedAttributeNames = new HashSet<>();
        if (attributeNames != null) {
            expectedAttributeNames.addAll(Stream.of(attributeNames.trim().split(",")).collect(Collectors.toSet()));
        }
        assertThat(ConditionsUtils.extractAttributeNamesFromConditions(conditions, moType)).containsAll(expectedAttributeNames);
    }

    @ParameterizedTest
    @CsvFileSource(resources = { "/conditions_test_files/user_input_condition_to_spel_expression.csv" }, delimiter = ':', numLinesToSkip = 1)
    void testConvertToSpel(final String conditions, final String moType, final String spelExpression) {
        assertThat(ConditionsUtils.convertToSpel(conditions, moType)).isEqualTo(spelExpression);
    }

    @Test
    void testConditionSyntaxSuccessBoolean() {
        final var rule = new Rule(MO_TYPE, ATTRIBUTE_NAME, ATTRIBUTE_VALUE,
                "(EUtranCellFDD.earfcndl) = 1850 or (EUtranCellFDD.earfcndl = 1501)");

        assertThat(ConditionsUtils.isValidSyntax(rule)).isTrue();
        assertLogContains("Condition:<%s> syntax is valid.".formatted(rule.getConditions().trim()));
    }

    @Test
    void testConditionSyntaxBlankConditionSuccessBoolean() {
        final var rule = new Rule(MO_TYPE, ATTRIBUTE_NAME, ATTRIBUTE_VALUE,
                " ");
        assertThat(ConditionsUtils.isValidSyntax(rule)).isTrue();
    }

    @Test
    void testConditionSyntaxNULLConditionSuccessBoolean() {
        final var rule = new Rule(MO_TYPE, ATTRIBUTE_NAME, ATTRIBUTE_VALUE, null);
        assertThat(ConditionsUtils.isValidSyntax(rule)).isTrue();
    }

    @Test
    void testConditionLengthInvalidSuccessBoolean() {
        final String conditions = 'E' + LONGEST_VALID_CONDITION;
        assertThat(conditions).as(TEST_SETUP_ERROR).hasSize(513);
        final var rule = new Rule(MO_TYPE, ATTRIBUTE_NAME, ATTRIBUTE_VALUE, conditions);

        rule.setLineNumber(999L);
        assertThat(ConditionsUtils.isValidLength(rule)).isFalse();
        assertLogContains(
                "Conditions too long, cannot be more than %d characters at lineNumber:%d".formatted(CONDITION_MAX_LENGTH, rule.getLineNumber()));
    }

    @Test
    void testConditionLengthValidSuccessBoolean() {
        assertThat(LONGEST_VALID_CONDITION).as(TEST_SETUP_ERROR).hasSize(512);
        final var rule = new Rule(MO_TYPE, ATTRIBUTE_NAME, ATTRIBUTE_VALUE, LONGEST_VALID_CONDITION);
        assertThat(ConditionsUtils.isValidLength(rule)).isTrue();
    }

    @Test
    void testConditionWithNullValidSuccessBoolean() {
        final var rule = new Rule(MO_TYPE, ATTRIBUTE_NAME, ATTRIBUTE_VALUE,
                null);
        assertThat(ConditionsUtils.isValidLength(rule)).isTrue();
    }

    @Test
    void testConditionWithEmptyValidSuccessBoolean() {
        final var rule = new Rule(MO_TYPE, ATTRIBUTE_NAME, ATTRIBUTE_VALUE,
                "");
        assertThat(ConditionsUtils.isValidLength(rule)).isTrue();
    }

    @Test
    void testConditionSyntaxTrueConditionSuccessBoolean() {
        final var rule = new Rule(MO_TYPE, ATTRIBUTE_NAME, ATTRIBUTE_VALUE, "true");
        assertThat(ConditionsUtils.isValidSyntax(rule)).isTrue();
    }

    @Test
    void testConditionSyntaxIllegalCharactersConditionSuccessBoolean() {
        final String illegalCharacter = Character.toString((char) 163); //pound sign
        final var rule = new Rule(MO_TYPE, ATTRIBUTE_NAME, ATTRIBUTE_VALUE, illegalCharacter);
        rule.setLineNumber(999L);
        assertThat(ConditionsUtils.isValidSyntax(rule)).isFalse();

        assertLogContainsLineNumberButNotRawCondition(rule);
        assertInvalidTextHasNotBeenLogged(illegalCharacter);
    }

    @Test
    void testUndocumentedConditionSyntaxFailureBoolean() {
        //this test, tracks SPEL standard syntax, which EACC's SPEL does not accept and EACC has not documented,
        // so that we can identify when this changes
        final var invalidConditions = List.of(
                "ENodeBFunction.dscpLabel ;= 24",
                "ENodeBFunction.dscpLabel ~= 24", //regex matcher
                "#{T(java.lang.Math).random()}", //method injection
                " ( EUtranCellFDD.cellCapMinCellSubCap in (98,100,102)   )   ", //spaces before closing bracket
                "${'hello world'}" //template expression
        );

        invalidConditions.stream()
                .map(condition -> new Rule(ENODEBFUNCTION, ATTRIBUTE_NAME, ATTRIBUTE_VALUE, condition))
                .forEach(rule -> softly.assertThat(ConditionsUtils.isValidSyntax(rule))
                        .as("<%s> condition is not supported, remove this test case if that has changed", rule.getConditions())
                        .isFalse());
    }

    @Test
    void testUndocumentedConditionSyntaxSuccessBoolean() {
        //this test, tracks syntax which EACC's SPEL accepts, but that EACC has not documented, so that we can identify when this changes -
        // TODO Document or change behaviour [IDUN-102430]
        final var validConditions = new ArrayList<>(Arrays.asList(
                null,
                "'Hello'.concat('World')",
                " Alph4NumbericStr1ng ",
                "ENodeBFunction.dscpLabel >= 6.0221415E+23", //int literal
                "ENodeBFunction.dscpLabel>=14.2", //without spaces
                "ENodeBFunction.reallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReally" +
                        "ReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReally" +
                        "ReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReally" +
                        "ReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReally" +
                        "LongAttributeName >= 2.3", // > 512 chars
                "ENodeBFunction.dscpLabel == 0x7FFFFFFF", //int literal
                "new String('hello world').toUpperCase()", //java instantiation
                "{1,2,3,4}", //list
                "{{1,2},{3,4,5}}", //list of lists
                "new int[4]", //array
                "new int[]{1,2,3}", //array
                "new int[4][5]", //array
                "booleanList[0]", //list index
                "booleanMap['isOpen']", //map key
                "booleanList.?[Name == 'John']", //collection filtering
                "map.?[value<10]", //map filtering
                "addressList.![address.city]", //collection projection
                "'Hello World'.bytes", //javabean
                "1+2 == 3", //context-free mathematical boolean expression
                "'abc' < 'abd'", //context-free String comparison
                "instanceOf", //boolean operator
                "'abc' matches 'ab\\.*'", //regex matcher
                "true", //boolean literal
                "false", //boolean literal
                "!true", //unitary boolean operator 
                "booleanValue1 = true", //assignment
                "booleanValue2 = #booleanValue1", //variable
                "T(java.util.Date)", //Types
                "java.lang.String()", //constructor
                "#someUDF('hello')", //user defined function
                "@myBean", // bean
                "false ? false : true", //ternary operation
                "false?:true", //elvis operator
                "null?.instanceValue", //safe call operator (cannot throw Null Pointer)
                "^[true]", //start of list
                "$[true]", //end of list
                "null",
                " ( EUtranCellFDD.cellCapMinCellSubCap in (  98,100,102))   ", //added whitespace after opening bracket
                "   \n    ", //whitespace
                ""));

        validConditions.stream()
                .map(condition -> new Rule(ENODE_BFUNCTION, ATTRIBUTE_NAME, ATTRIBUTE_VALUE, condition))
                .forEach(rule -> softly.assertThat(ConditionsUtils.isValidSyntax(rule))
                        .as("<%s> condition is supported, remove this test case if that has changed", rule.getConditions())
                        .isTrue());
    }

    private void assertLogContains(final String text) {
        logAppender.stop();
        final List<ILoggingEvent> loggedEvents = logAppender.getLoggedEvents();
        softly.assertThat(loggedEvents).asString()
                .contains(text);
    }

    private void assertLogContainsLineNumberButNotRawCondition(final Rule ruleWithInvalidCondition) {
        assertLogContains("Condition syntax is invalid at lineNumber:%d".formatted(ruleWithInvalidCondition.getLineNumber()));
        assertInvalidTextHasNotBeenLogged(ruleWithInvalidCondition.getConditions());
    }

    private void assertInvalidTextHasNotBeenLogged(final String invalidText) {
        logAppender.stop();
        final List<ILoggingEvent> loggedEvents = logAppender.getLoggedEvents();
        assertThat(loggedEvents).asString()
                .as(TEST_SETUP_ERROR_UNABLE_TO_ACCESS_LOGS)
                .contains(INJECTED_LOG_TO_VALIDATE_TEST);
        softly.assertThat(loggedEvents).asString()
                .contains(INJECTED_LOG_TO_VALIDATE_TEST)
                .doesNotContain(invalidText);

        final var logMessages = loggedEvents.stream()
                .map(ILoggingEvent::getMessage)
                .toList();
        assertThat(logMessages).asString()
                .as(TEST_SETUP_ERROR_UNABLE_TO_ACCESS_LOGS)
                .contains(INJECTED_LOG_TO_VALIDATE_TEST);
        softly.assertThat(logMessages).asString()
                .contains(INJECTED_LOG_TO_VALIDATE_TEST)
                .doesNotContain(invalidText);

        final var throwableMessages = loggedEvents.stream()
                .map(ILoggingEvent::getThrowableProxy)
                .filter(Objects::nonNull)
                .map(IThrowableProxy::getMessage)
                .toList();
        assertThat(throwableMessages).asString()
                .as(TEST_SETUP_ERROR_UNABLE_TO_ACCESS_LOGS)
                .contains(INJECTED_ERROR_MESSAGE_TO_VALIDATE_TEST);
        softly.assertThat(throwableMessages).asString().doesNotContain(invalidText);

        //ensure logging is not stopped within forEach
        logAppender.start();
    }

}
