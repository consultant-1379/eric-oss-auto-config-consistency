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
import static com.ericsson.oss.apps.util.Constants.EMPTY_STRING;
import static com.ericsson.oss.apps.util.TestDefaults.CELL_CAP_MIN_CELL_SUB_CAP;
import static com.ericsson.oss.apps.util.TestDefaults.CFRA_ENABLE;
import static com.ericsson.oss.apps.util.TestDefaults.ENODE_BFUNCTION;
import static com.ericsson.oss.apps.util.TestDefaults.EUTRAN_CELL_FDD;
import static com.ericsson.oss.apps.util.TestDefaults.INVALID_MO;
import static com.ericsson.oss.apps.util.TestDefaults.MANAGED_ELEMENT_1_FDN;
import static com.ericsson.oss.apps.util.TestDefaults.MANAGED_ELEMENT_2_FDN;
import static com.ericsson.oss.apps.util.TestDefaults.PRACH_CONFIG_ENABLED;
import static com.ericsson.oss.apps.util.TestDefaults.TEST_SETUP_ERROR;
import static java.util.stream.Collectors.groupingBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.oss.apps.model.Rule;

/**
 * Unit tests for {@link RuleSetValidator} class.
 */
@ExtendWith(MockitoExtension.class)
public class RuleSetValidatorTest {
    private static final String RULE_VALIDATION_ERRORS = "ruleValidationErrors";
    private static final String UPPER_LAYER_AUTO_CONF_ENABLED = "upperLayerAutoConfEnabled";
    private static final Rule INVALID_MO_RULE = new Rule(INVALID_MO, PRACH_CONFIG_ENABLED, "true", "");
    private static final String INVALID_ATTR = "invalidAttr";
    private static final Rule INVALID_ATTRIBUTE_RULE = new Rule(ENODE_BFUNCTION, INVALID_ATTR, "1000");
    private static final Rule INVALID_VALUE_RULE = new Rule(EUTRAN_CELL_FDD, CFRA_ENABLE, "invalidValue", "", 1);
    private static final Rule INVALID_CONDITION_RULE = new Rule(EUTRAN_CELL_FDD, CFRA_ENABLE, "true", "<invalid condition>", 2);
    private static final String VALID_CONDITION = "valid";
    private static final List<Rule> INVALID_PRIORITY = List.of(
            new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "true", VALID_CONDITION, 10),
            new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "true", VALID_CONDITION));
    private static final List<Rule> INVALID_MO_ATTR_PRIORITY_COMBO = List.of(
            new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "true", VALID_CONDITION, 10),
            new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "true", VALID_CONDITION, 10));
    private static final String ERRORS_EXIST_IN_RULES = "Errors exist in rules.";
    private static final String NRCELLDU = "NRCellDU";
    private static final String USER_LABEL = "userLabel";
    private static final String ATTR_NAME = "attrName";
    private static final String THOUSAND = "1000";
    private static final String VALID_LONG_CONDITION = "EUtranCellFDD.releaseInactiveUesMpLoadLevel in ('HIGH_LOAD','VERY_HIGH_LOAD','ZZ') and " +
            "FDN like '%SubNetwork=ONRM_ROOT_MO,SubNetwork=MKT_145,MeContext=145306_PALM_BAY_UTILITY,ManagedElement=145306_PALM_BAY_UTILITY," +
            "ENodeBFunction=1%' and EUtranCellFDD.userLabel like '%SubNetwork=ONRM_ROOT_MO,SubNetwork=MKT_145,MeContext=145306_PALM_BAY_UTILITY," +
            "ManagedElement=145306_PALM_BAY_UTILITY%' or EUtranCellFDD.userLabel like '%SubNetwork=ONRM_ROOT_MO,SubNetwork=MKT_145," +
            "MeContext=145307_PALM_BAY_UTILITY,ManagedElemen%'";
    private static final Rule VALID_CONDITION_LENGTH_RULE = new Rule(EUTRAN_CELL_FDD, UPPER_LAYER_AUTO_CONF_ENABLED, "FALSE", VALID_LONG_CONDITION);
    private static final Rule INVALID_CONDITION_LENGTH_RULE = new Rule(EUTRAN_CELL_FDD, UPPER_LAYER_AUTO_CONF_ENABLED, "FALSE",
            VALID_LONG_CONDITION.replace("ZZ", "ZZZ"));
    private static final String INVALID_CONDITION = "Invalid Condition.";
    private final List<String> scopeFdns = List.of(MANAGED_ELEMENT_1_FDN, MANAGED_ELEMENT_2_FDN);
    private List<Rule> rulesList;

    @InjectMocks
    private RuleSetValidator objUnderTest;

    @Mock
    private ModelManager mockModelManager;

    @BeforeEach
    public void setUp() {
        rulesList = createListOfValidRules();
    }

    @Test
    public void whenValidateNetworkLevelRulesIsCalledWithValidRules_thenNoErrorThrown() throws RuleValidationException {
        // When
        when(mockModelManager.isValidMo(ENODE_BFUNCTION)).thenReturn(true);
        when(mockModelManager.isValidAttribute(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED)).thenReturn(true);
        when(mockModelManager.isValidValue(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true")).thenReturn(true);
        when(mockModelManager.isValidMo(EUTRAN_CELL_FDD)).thenReturn(true);
        when(mockModelManager.isValidAttribute(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP)).thenReturn(true);
        when(mockModelManager.isValidValue(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, THOUSAND)).thenReturn(true);
        when(mockModelManager.isValidAttribute(EUTRAN_CELL_FDD, USER_LABEL)).thenReturn(true);

        // Then
        assertDoesNotThrow(() -> objUnderTest.validateRules(rulesList));
    }

    @Test
    public void whenValidateNetworkLevelRulesIsCalledWithInvalidMO_thenExceptionThrown_with1ErrorCreated() {
        // Given
        rulesList.add(INVALID_MO_RULE);

        // When
        when(mockModelManager.isValidAttribute(any(), anyString())).thenReturn(true);
        when(mockModelManager.isValidValue(any(), anyString(), anyString())).thenReturn(true);
        when(mockModelManager.isValidMo(ENODE_BFUNCTION)).thenReturn(true);
        when(mockModelManager.isValidMo(EUTRAN_CELL_FDD)).thenReturn(true);
        when(mockModelManager.isValidMo(INVALID_MO_RULE.getMoType())).thenReturn(false);

        // Then
        assertThatThrownBy(() -> objUnderTest.validateRules(rulesList))
                .isInstanceOf(RuleValidationException.class)
                .hasMessage(ERRORS_EXIST_IN_RULES)
                .extracting(RULE_VALIDATION_ERRORS).asList().hasSize(1);
    }

    @Test
    public void whenValidateNetworkLevelRulesIsCalledWithInvalidAttribute_thenExceptionThrown_with1ErrorCreated() {
        // Given
        rulesList.add(INVALID_ATTRIBUTE_RULE);

        // When
        when(mockModelManager.isValidAttribute(ENODE_BFUNCTION, INVALID_ATTR)).thenReturn(false);
        when(mockModelManager.isValidAttribute(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED)).thenReturn(true);
        when(mockModelManager.isValidAttribute(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP)).thenReturn(true);
        when(mockModelManager.isValidMo(any())).thenReturn(true);
        when(mockModelManager.isValidValue(any(), anyString(), anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(EUTRAN_CELL_FDD, USER_LABEL)).thenReturn(true);

        // Then
        assertThatThrownBy(() -> objUnderTest.validateRules(rulesList))
                .isInstanceOf(RuleValidationException.class)
                .hasMessage(ERRORS_EXIST_IN_RULES)
                .extracting(RULE_VALIDATION_ERRORS).asList().hasSize(1);
    }

    @Test
        public void whenValidateNetworkLevelRulesIsCalledWithMultipleInvalidRules_thenErrorThrown_andCorrectNumberOfErrorsCreated() {
        // Given
        rulesList = createListOfInvalidRules();

        // When
        when(mockModelManager.isValidMo(INVALID_MO)).thenReturn(false);
        when(mockModelManager.isValidMo(ENODE_BFUNCTION)).thenReturn(true);
        when(mockModelManager.isValidAttribute(ENODE_BFUNCTION, INVALID_ATTR)).thenReturn(false);
        when(mockModelManager.isValidMo(EUTRAN_CELL_FDD)).thenReturn(true);
        when(mockModelManager.isValidAttribute(EUTRAN_CELL_FDD, CFRA_ENABLE)).thenReturn(true);
        when(mockModelManager.isValidAttribute(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP)).thenReturn(true);
        when(mockModelManager.isValidValue(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "true")).thenReturn(true);
        when(mockModelManager.isValidValue(EUTRAN_CELL_FDD, CFRA_ENABLE, "invalidValue")).thenReturn(false);

        // Then
        assertThatThrownBy(() -> objUnderTest.validateRules(rulesList))
                .isInstanceOf(RuleValidationException.class)
                .hasMessage(ERRORS_EXIST_IN_RULES)
                .extracting(RULE_VALIDATION_ERRORS).asList().hasSize(6)
                .extracting("lineNumber", "errorType", "errorDetails", "additionalInfo")
                .containsExactly(
                        tuple(null, "Invalid priority value.", "Priority value must be given if the moType & attrName combination is not unique.", ""),
                        tuple(null, "Invalid Rule values combination.", "This moType, attribute name and priority combination already exists.", ""),
                        tuple(null, "Invalid Rule values combination.", "This moType, attribute name and priority combination already exists.", ""),
                        tuple(null, "Invalid Attribute name.", "Attribute not found in Managed Object Model.", ""),
                        tuple(null, "Invalid Attribute value.", "Attribute value is invalid according to the Managed Object Model.", ""),
                        tuple(null, "Invalid MO.", "MO not found in Managed Object Model.", "")
                );
    }

    @Test
    public void whenValidateNodeLevelRulesIsCalledWithValidRules_thenCorrectFdnToRulesByMoTypeMapIsReturned() {
        // Given
        final List<Rule> validAndInvalidRules = createListOfValidRules();
        validAndInvalidRules.addAll(createListOfInvalidRules());
        final Map<String, List<Rule>> rulesByMoType = validAndInvalidRules.stream().collect(groupingBy(Rule::getMoType));

        // When
        when(mockModelManager.isValidMo(eq(ENODE_BFUNCTION), anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(eq(ENODE_BFUNCTION), eq(PRACH_CONFIG_ENABLED), anyString())).thenReturn(true);
        when(mockModelManager.isValidValue(eq(ENODE_BFUNCTION), eq(PRACH_CONFIG_ENABLED), eq("true"), anyString())).thenReturn(true);

        when(mockModelManager.isValidMo(eq(EUTRAN_CELL_FDD), anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(eq(EUTRAN_CELL_FDD), eq(CELL_CAP_MIN_CELL_SUB_CAP), anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(eq(EUTRAN_CELL_FDD), eq(USER_LABEL), anyString())).thenReturn(true);
        when(mockModelManager.isValidValue(eq(EUTRAN_CELL_FDD), eq(CELL_CAP_MIN_CELL_SUB_CAP), eq(THOUSAND), anyString()))
                .thenReturn(true);

        when(mockModelManager.isValidMo(eq(INVALID_MO_RULE.getMoType()), anyString())).thenReturn(false);
        when(mockModelManager.isValidAttribute(eq(INVALID_ATTRIBUTE_RULE.getMoType()), eq(INVALID_ATTRIBUTE_RULE.getAttributeName()), anyString()))
                .thenReturn(false);

        final Map<String, Map<String, List<Rule>>> fdnToRulesByMoType = objUnderTest.validateNodeLevelRules(scopeFdns, rulesByMoType);

        // Then
        assertThat(fdnToRulesByMoType).as("Map size should be 2 and not 3.").containsOnlyKeys(MANAGED_ELEMENT_1_FDN, MANAGED_ELEMENT_2_FDN);

        final String invalidAttrName = INVALID_MO_RULE.getAttributeName();
        for (final String fdn : scopeFdns) {
            //Validate invalid MO is not present
            assertFalse(fdnToRulesByMoType.containsKey(INVALID_MO_RULE.getAttributeName()));
            //Validate invalid Attr Name is not present
            final Map<String, List<Rule>> innerMap = fdnToRulesByMoType.get(fdn);
            assertThat(innerMap).as("Incorrect map size for fdn:%s", fdn).hasSize(2);
            assertThat(innerMap).as("Unexpected attribute for fdn:%s", fdn).doesNotContainKey(invalidAttrName);
            //Validate invalid Attr value is not present
            assertThat(innerMap.values().stream()
                    .flatMap(List::stream)
                    .map(Rule::getAttributeValue)
                    .toArray())
                    .as("Unexpected inner attribute for fdn:%s", fdn)
                    .doesNotContain(invalidAttrName);
        }
    }

    @Test
    void testValidateMoTypeAttributeNamePriority_andTheCombinationsAreEqual_thenErrorThrown () {
        when(mockModelManager.isValidMo(anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(anyString(), anyString())).thenReturn(true);
        when(mockModelManager.isValidValue(anyString(), anyString(), anyString())).thenReturn(true);
        assertThatThrownBy(() -> objUnderTest.validateRules(List.of(//
                new Rule(NRCELLDU, ATTR_NAME, "12",
                        " NRCellDU.cellBarred = 'NOT_BARRED'", 20),
                new Rule(NRCELLDU, ATTR_NAME, "12",
                        " NRCellDU.cellBarred = 'BARRED'", 20),
                new Rule(NRCELLDU, ATTR_NAME, "2", EMPTY_STRING))))
                .isInstanceOf(RuleValidationException.class)
                .hasMessage(ERRORS_EXIST_IN_RULES)
                .extracting("ruleValidationErrors")
                .asList()
                .contains(new RuleValidationError(null, "Invalid Rule values combination.",
                        "This moType, attribute name and priority combination already exists.", ""),
                        new RuleValidationError(null, "Invalid priority value.",
                                "Priority value must be given if the moType & attrName combination is not unique.", ""));

        verifyNoMoreInteractions(mockModelManager);
    }

    @Test
    public void whenValidateNetworkLevelRulesAndConditionsIsCalledWithValidSyntax_thenNoErrorThrown() throws RuleValidationException {
        // When
        when(mockModelManager.isValidMo(ENODE_BFUNCTION)).thenReturn(true);
        when(mockModelManager.isValidAttribute(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED)).thenReturn(true);
        when(mockModelManager.isValidValue(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true")).thenReturn(true);
        when(mockModelManager.isValidMo(EUTRAN_CELL_FDD)).thenReturn(true);
        when(mockModelManager.isValidAttribute(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP)).thenReturn(true);
        when(mockModelManager.isValidValue(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, THOUSAND)).thenReturn(true);
        when(mockModelManager.isValidAttribute(EUTRAN_CELL_FDD, USER_LABEL)).thenReturn(true);

        objUnderTest.validateRules(rulesList);

        // Then
        assertDoesNotThrow(() -> objUnderTest.validateRules(rulesList));
    }

    @Test
    public void whenValidateNetworkLevelRulesAndConditionsIsCalledWithInvalidSyntax_thenErrorThrown() {
        // Given
        createListOfValidRules();
        rulesList.add(INVALID_CONDITION_RULE);

        // When
        when(mockModelManager.isValidMo(anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(anyString(), anyString())).thenReturn(true);
        when(mockModelManager.isValidValue(anyString(), anyString(), anyString())).thenReturn(true);

        // Then
        assertThatThrownBy(() -> objUnderTest.validateRules(rulesList))
                .isInstanceOf(RuleValidationException.class)
                .hasMessage(ERRORS_EXIST_IN_RULES)
                .extracting(RULE_VALIDATION_ERRORS).asList()
                .containsExactly(new RuleValidationError(null, INVALID_CONDITION, "Condition has invalid syntax.", ""));
    }

    @Test
    public void whenValidationNetworkLevelRulesAndConditionsIsCalledWithInvalidConditionLength_thenNoErrorThrown() throws RuleValidationException {
        assertThat(INVALID_CONDITION_LENGTH_RULE.getConditions().trim()).as(TEST_SETUP_ERROR).hasSize(513);
        rulesList = createListOfValidRules();
        rulesList.add(INVALID_CONDITION_LENGTH_RULE);

        // When
        when(mockModelManager.isValidMo(anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(anyString(), anyString())).thenReturn(true);
        when(mockModelManager.isValidValue(anyString(), anyString(), anyString())).thenReturn(true);

        // Then
        assertThatThrownBy(() -> objUnderTest.validateRules(rulesList))
                .isInstanceOf(RuleValidationException.class)
                .hasMessage(ERRORS_EXIST_IN_RULES)
                .extracting(RULE_VALIDATION_ERRORS).asList()
                .containsExactly(new RuleValidationError(null, INVALID_CONDITION,
                        "Conditions too long, cannot be more than %d characters.".formatted(CONDITION_MAX_LENGTH), ""));
    }

    @Test
    public void whenValidationNetworkLevelRulesAndConditionsIsCalledWithValidConditionLength_thenNoErrorThrown() {
        assertThat(VALID_CONDITION_LENGTH_RULE.getConditions().trim()).as(TEST_SETUP_ERROR).hasSize(512);
        rulesList = createListOfValidRules();
        rulesList.add(VALID_CONDITION_LENGTH_RULE);

        // When
        when(mockModelManager.isValidMo(anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(anyString(), anyString())).thenReturn(true);
        when(mockModelManager.isValidValue(anyString(), anyString(), anyString())).thenReturn(true);

        // Then
        assertDoesNotThrow(() -> objUnderTest.validateRules(rulesList));

        verifyNoMoreInteractions(mockModelManager);
    }

    @Test
    public void whenRuleValidationIsPerformedOnBooleanTypeAttributeAndAttributeValueIsUppercase_thenAttributeValueValidationIsSuccess() {
        // Given
        rulesList = createBooleanValueRulesWithUpperCaseValues();

        // When
        when(mockModelManager.isValidMo(anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(anyString(), anyString())).thenReturn(true);
        when(mockModelManager.isValidValue(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "TRUE")).thenReturn(true);
        when(mockModelManager.isValidValue(EUTRAN_CELL_FDD, UPPER_LAYER_AUTO_CONF_ENABLED, "FALSE")).thenReturn(false);

        // Then
        assertThatThrownBy(() -> objUnderTest.validateRules(rulesList))
                .isInstanceOf(RuleValidationException.class)
                .hasMessage(ERRORS_EXIST_IN_RULES)
                .extracting(RULE_VALIDATION_ERRORS).asList()
                .containsExactly(new RuleValidationError(null, "Invalid Attribute value.",
                        "Attribute value is invalid according to the Managed Object Model.", ""));
    }

    @Test
    void testValidateMoTypeAndAttributes_ComplexConditions_WithPriorityValues_thenNoErrorThrown () {
        when(mockModelManager.isValidMo(anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(anyString(), anyString())).thenReturn(true);
        when(mockModelManager.isValidValue(anyString(), anyString(), anyString())).thenReturn(true);
        assertDoesNotThrow(() -> objUnderTest.validateRules(List.of(//
                new Rule(EUTRAN_CELL_FDD, ATTR_NAME, Boolean.TRUE.toString(),
                        " EUtranCellFDD.cellCapMinMaxWriProt = true", 1),
                new Rule(EUTRAN_CELL_FDD, ATTR_NAME, Boolean.TRUE.toString(), " FDN like '%LTE06dg2ERBS00030%' ", 2),
                new Rule(EUTRAN_CELL_FDD, ATTR_NAME, Boolean.TRUE.toString(),
                        "  EUtranCellFDD.dscpLabel <= 50  ", 3),
                new Rule(EUTRAN_CELL_FDD, ATTR_NAME, Boolean.TRUE.toString(),
                        "  EUtranCellFDD.cellCapMinCellSubCap in (98,100,102)   ", 4),
                new Rule(EUTRAN_CELL_FDD, ATTR_NAME, Boolean.TRUE.toString(),
                        "( EUtranCellFDD.dscpLabel <= 50 and EUtranCellFDD.cellCapMinCellSubCap in (98,100,102))  ", 5),
                new Rule(EUTRAN_CELL_FDD, ATTR_NAME, Boolean.TRUE.toString(),
                        " EUtranCellFDD.cellCapMinMaxWriProt = true or FDN like '%LTE06dg2ERBS00030%' " +
                                "or  ( EUtranCellFDD.dscpLabel <= 50 and EUtranCellFDD.cellCapMinCellSubCap in (98,100,102))  ", 6),
                new Rule(EUTRAN_CELL_FDD, ATTR_NAME, Boolean.TRUE.toString(),
                        " EUtranCellFDD.cellCapMinCellSubCap = 98 or EUtranCellFDD.cellCapMinCellSubCap < 100 or " +
                                "EUtranCellFDD.cellCapMinCellSubCap > 100 or EUtranCellFDD.cellCapMinCellSubCap != 17 ", 7),
                new Rule(EUTRAN_CELL_FDD, ATTR_NAME, Boolean.TRUE.toString(),
                        " EUtranCellFDD.cellCapMinCellSubCap < 98 or EUtranCellFDD.cellCapMinCellSubCap > 100", 8),
                new Rule(EUTRAN_CELL_FDD, "pdcchOuterLoopInitialAdj", "-70", "EUtranCellFDD.primaryUpperLayerInd = 'OFF' and " +
                        "(EUtranCellFDD.userLabel like '%dg2%' or EUtranCellFDD.userLabel like '%sync%')"),
                new Rule("NRCellDU", ATTR_NAME, "12",
                        " NRCellDU.cellBarred = 'NOT_BARRED'", 1),
                new Rule("NRCellDU", ATTR_NAME, "2", EMPTY_STRING, 2),
                new Rule("NRCellDU", ATTR_NAME, "2", null, 3))));
        verifyNoMoreInteractions(mockModelManager);
    }

    @Test
    void testValidateMoTypeAndAttributes_moType_DoesNotMatch_ThenErrorThrown() {
        when(mockModelManager.isValidMo(anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(anyString(), anyString())).thenReturn(true);
        when(mockModelManager.isValidValue(anyString(), anyString(), anyString())).thenReturn(true);
        assertThatThrownBy(() -> objUnderTest.validateRules(List.of(//
                new Rule(EUTRAN_CELL_FDD, ATTR_NAME, Boolean.TRUE.toString(),
                        "FDN like '%LTE06dg2ERBS00030%' or FDN like '%LTE06dg2ERBS00130%' or " +
                                "ENodeBFunction.dscpLabel <= 50 and FDN like '%LTE06dg2ERBS00030%'"))))
                .isInstanceOf(RuleValidationException.class)
                .hasMessage(ERRORS_EXIST_IN_RULES)
                .extracting("ruleValidationErrors")
                .asList()
                .containsExactly(new RuleValidationError(null, INVALID_CONDITION,
                        "Attributes in Conditions must be from the moType field (" + EUTRAN_CELL_FDD + ")'s attributes",
                        ""));

        verifyNoMoreInteractions(mockModelManager);
    }

    @Test
    void testValidateMoTypeAndAttributes_Empty_Conditions_thenNoErrorThrown() {
        when(mockModelManager.isValidMo(anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(anyString(), anyString())).thenReturn(true);
        when(mockModelManager.isValidValue(anyString(), anyString(), anyString())).thenReturn(true);
        assertDoesNotThrow(() -> objUnderTest.validateRules(List.of(//
                new Rule("EUtranCellFDD", ATTR_NAME, Boolean.TRUE.toString(), ""))));

        verifyNoMoreInteractions(mockModelManager);
    }

    @Test
    void testValidateMoTypeAndAttributes_Null_Conditions_thenNoErrorThrown() {
        when(mockModelManager.isValidMo(anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(anyString(), anyString())).thenReturn(true);
        when(mockModelManager.isValidValue(anyString(), anyString(), anyString())).thenReturn(true);
        assertDoesNotThrow(() -> objUnderTest.validateRules(List.of(//
                new Rule("EUtranCellFDD", ATTR_NAME, Boolean.TRUE.toString(), null))));

        verifyNoMoreInteractions(mockModelManager);
    }

    @Test
    void testValidateMoTypeAndAttributes_Conditions_without_attributes_thenNoErrorThrown() {
        when(mockModelManager.isValidMo(anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(anyString(), anyString())).thenReturn(true);
        when(mockModelManager.isValidValue(anyString(), anyString(), anyString())).thenReturn(true);
        assertDoesNotThrow(() -> objUnderTest.validateRules(List.of(//
                new Rule("EUtranCellFDD", ATTR_NAME, Boolean.TRUE.toString(), "FDN like '%LTE06dg2ERBS00030%'"))));

        verifyNoMoreInteractions(mockModelManager);
    }

    @Test
    void testValidateMoTypeAndAttributes_Conditions_withAttributeWithoutMO_thenThrowError() {
        when(mockModelManager.isValidMo(anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(anyString(), anyString())).thenReturn(true);
        when(mockModelManager.isValidValue(anyString(), anyString(), anyString())).thenReturn(true);
        assertThatThrownBy(() -> objUnderTest.validateRules(List.of(//
                new Rule(EUTRAN_CELL_FDD, ATTR_NAME, Boolean.TRUE.toString(),
                        " EUtranCellFDD.cellCapMinMaxWriProt = true or FDN like '%LTE06dg2ERBS00030%' " +
                                "or ( cellBarred = 'NOT_BARRED' and FDN like '%LTE06dg2ERBS00020%'  )"))))
                .isInstanceOf(RuleValidationException.class)
                .hasMessage(ERRORS_EXIST_IN_RULES)
                .extracting(RULE_VALIDATION_ERRORS).asList()
                .containsExactly(new RuleValidationError(null, INVALID_CONDITION,
                        "MoType not specified, Attributes must be specified as '<MO Type>.<Attribute Name>'",
                        "Attributes must be specified as '<MO Type>.<Attribute Name>'"));

        verifyNoMoreInteractions(mockModelManager);
    }

    @Test
    void testValidateNodeLevelRules_andAllAttributesInConditionsAreValidForCell_thenCorrectFdnToRulesByMoTypeMapIsReturned() {
        final String attrNameInRule = ATTR_NAME;
        final String moType = "EUtranCellFDD";
        final String valueInRule = "valueInRule";
        when(mockModelManager.isValidMo(eq(moType), anyString())).thenReturn(true);
        when(mockModelManager.isValidValue(eq(moType), eq(attrNameInRule), eq(valueInRule), anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(eq(moType), anyString(), anyString())).thenReturn(true);

        final Rule ruleA = new Rule(moType, attrNameInRule, valueInRule,
                "EUtranCellFDD.primaryUpperLayerInd = 'OFF' and (EUtranCellFDD.userLabel like '%dg2%' or EUtranCellFDD.userLabel like '%sync%')");
        final Rule ruleB = new Rule(moType, attrNameInRule, valueInRule,
                "EUtranCellFDD.lbEUtranAcceptOffloadThreshold != 45");
        final String fdn = "fdn";
        final List<Rule> rules = List.of(ruleA, ruleB);
        assertThat(objUnderTest.validateNodeLevelRules(Set.of(fdn), Map.of(moType, rules)))
                .containsExactlyEntriesOf(Map.of("fdn", Map.of(EUTRAN_CELL_FDD, rules)));
    }

    @Test
    void testValidateNodeLevelRules_andSomeAttributesInConditionsAreInvalidForCell_thenIgnoreRule() {
        final String attrNameInRule = ATTR_NAME;
        final String moType = EUTRAN_CELL_FDD;
        final String invalidAttribute = "primaryUpperLayerInd";
        final String validAttribute = USER_LABEL;
        final String valueInRule = "valueInRule";
        when(mockModelManager.isValidMo(eq(moType), anyString())).thenReturn(true);
        when(mockModelManager.isValidValue(eq(moType), eq(attrNameInRule), eq(valueInRule), anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(eq(moType), eq(attrNameInRule), anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(eq(moType), eq(invalidAttribute), anyString())).thenReturn(false);
        when(mockModelManager.isValidAttribute(eq(moType), eq(validAttribute), anyString())).thenReturn(true);

        final Rule ruleWithInvalidAttribute = new Rule(moType, attrNameInRule, valueInRule,
                EUTRAN_CELL_FDD + '.' + validAttribute +
                        " = 'OFF' and (" +
                        EUTRAN_CELL_FDD + '.' + invalidAttribute +
                        " like '%dg2%' or " +
                        EUTRAN_CELL_FDD + '.' + validAttribute +
                        " like '%sync%')");
        final Rule ruleWithValidAttribute = new Rule(moType, attrNameInRule, valueInRule,
                EUTRAN_CELL_FDD + '.' + validAttribute +
                        " = 'OFF' or (" +
                        EUTRAN_CELL_FDD + '.' + validAttribute +
                        " like '%dg2%' or " +
                        EUTRAN_CELL_FDD + '.' + validAttribute +
                        " like '%sync%')");
        final String fdn = "fdn";
        assertThat(objUnderTest.validateNodeLevelRules(Set.of(fdn), Map.of(moType, List.of(ruleWithInvalidAttribute, ruleWithValidAttribute))))
                .containsExactlyEntriesOf(Map.of(fdn, Map.of(EUTRAN_CELL_FDD, List.of(ruleWithValidAttribute))));
    }

    @Test
    void testValidateMoTypeAndAttributes_Conditions_withAttributeNotMatchingMoTypeAttrPattern_thenThrowError() {
        when(mockModelManager.isValidMo(anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(anyString(), anyString())).thenReturn(true);
        when(mockModelManager.isValidValue(anyString(), anyString(), anyString())).thenReturn(true);
        assertThatThrownBy(() -> objUnderTest.validateRules(List.of(//
                new Rule(EUTRAN_CELL_FDD, ATTR_NAME, Boolean.TRUE.toString(),
                        "EUtranCellFDD.attribute.with.multiple.dots >= 17"))))
                .isInstanceOf(RuleValidationException.class)
                .hasMessage(ERRORS_EXIST_IN_RULES)
                .extracting(RULE_VALIDATION_ERRORS).asList().hasSize(1);

        verifyNoMoreInteractions(mockModelManager);
    }

    @Test
    void testValidateMoTypeAndAttributes_whenMultipleMoTypesInConditions_ThenErrorIsThrown() {
        when(mockModelManager.isValidMo(anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(anyString(), anyString())).thenReturn(true);
        when(mockModelManager.isValidValue(anyString(), anyString(), anyString())).thenReturn(true);
        assertThatThrownBy(() -> objUnderTest.validateRules(List.of(//
                new Rule(ENODE_BFUNCTION, ATTR_NAME, Boolean.TRUE.toString(),
                        " ( ENodeBFunction.dscpLabel <= 50  or FDN like '%LTE06dg2ERBS00030%' )" +
                                "or FDN like '%LTE06dg2ERBS00030%' and EUtranCellFDD.cellCapMinMaxWriProt = true "))))
                .hasMessage(ERRORS_EXIST_IN_RULES)
                .extracting(RULE_VALIDATION_ERRORS).asList()
                .containsExactly(new RuleValidationError(null, INVALID_CONDITION,
                        "Attributes in Conditions must be from the moType field (" + ENODE_BFUNCTION + ")'s attributes",
                        ""));

        verifyNoMoreInteractions(mockModelManager);
    }

    @Test
    void testValidateMoTypeAndAttributes_whenInvalidAttributeInConditions_ThenErrorIsThrown() {
        when(mockModelManager.isValidMo(anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(anyString(), eq(ATTR_NAME))).thenReturn(true);
        when(mockModelManager.isValidValue(anyString(), anyString(), anyString())).thenReturn(true);
        when(mockModelManager.isValidAttribute(EUTRAN_CELL_FDD, "cellCapMinMaxWriProt")).thenReturn(true);
        when(mockModelManager.isValidAttribute(EUTRAN_CELL_FDD, "dscpLabel")).thenReturn(false);
        assertThatThrownBy(() -> objUnderTest.validateRules(List.of(//
                new Rule(EUTRAN_CELL_FDD, ATTR_NAME, Boolean.TRUE.toString(),
                        " (  FDN like '%LTE06dg2ERBS00030%' or EUtranCellFDD.dscpLabel <= 50 )" +
                                "or EUtranCellFDD.cellCapMinMaxWriProt = true and FDN like '%LTE06dg2ERBS00030%'"))))
                .hasMessage(ERRORS_EXIST_IN_RULES)
                .extracting(RULE_VALIDATION_ERRORS).asList()
                .containsExactly(new RuleValidationError(null, "Invalid Condition.",
                        "Attribute(s) in Conditions not found in Managed Object model.", ""));

        verifyNoMoreInteractions(mockModelManager);
    }

    private List<Rule> createListOfInvalidRules() {
        final List<Rule> rules = new ArrayList<>();
        rules.add(INVALID_MO_RULE);
        rules.add(INVALID_ATTRIBUTE_RULE);
        rules.add(INVALID_VALUE_RULE);
        rules.addAll(INVALID_PRIORITY);
        rules.addAll(INVALID_MO_ATTR_PRIORITY_COMBO);
        return rules;
    }

    private List<Rule> createListOfValidRules() {
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true", ""));
        rules.add(new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, THOUSAND, "EUtranCellFDD.userLabel like '%dg2%'"));
        return rules;
    }

    private List<Rule> createBooleanValueRulesWithUpperCaseValues() {
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "TRUE", ""));
        rules.add(new Rule(EUTRAN_CELL_FDD, UPPER_LAYER_AUTO_CONF_ENABLED, "FALSE", "EUtranCellFDD.userLabel like '%dg2%'"));
        return rules;
    }

}
