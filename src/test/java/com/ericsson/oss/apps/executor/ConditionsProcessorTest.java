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

package com.ericsson.oss.apps.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.apps.model.EaccManagedObject;
import com.ericsson.oss.apps.util.InMemoryLogAppender;
import com.ericsson.oss.apps.validation.ModelManager;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;

@ExtendWith({ MockitoExtension.class })
public class ConditionsProcessorTest {

    private static final String EQUALITY_CONDITION = "EUtranCellFDD.earfcndl = 1850 or EUtranCellFDD.earfcndl = 1501";
    private static final String BOOLEAN_EQUALITY_CONDITION = "EUtranCellFDD.loadBasedCaEnabled = false";
    private static final String LESS_THAN_EQUAL_CONDITION = "SectorCarrier.essScPairId <= 9223372036854775806L or SectorCarrier.essScPairId = 9223372036854775805L";
    private static final String IN_CONDITION_INT = "EUtranCellFDD.freqBand in (1,3,7) and EUtranCellFDD.freqBand > 2" +
            " and EUtranCellFDD.freqBand in (3,5,7)";
    private static final String IN_CONDITION_STRING = "EUtranCellFDD.userLabel in ('BSC6697','BSC6698')";
    private static final String LIKE_CONDITION_PREFIX_MATCH = "EUtranCellFDD.userLabel like '%P'";
    private static final String LIKE_CONDITION_SUFFIX_MATCH = "EUtranCellFDD.userLabel like 'b%'";
    private static final String LIKE_CONDITION_BOTH_MATCH = "EUtranCellFDD.userLabel like '%poc%'";
    private static final String INVALID_CONDITION = "EUtranCellFDD.userLabel likes '%poc%'";

    private static final String IN_AND_LIKE_CONDITION = "NRCellDU.srsPeriodicity in (40,30,50) and NRCellDU.userLabel like '%NR05dg2%'";

    private static final String LIKE_AND_IN_CONDITION = "NRCellDU.userLabel like '%NR05dg2%' and NRCellDU.srsPeriodicity in (40,30,50)";
    private static final String USER_DEFINED_OPERATOR_PRECEDENCE = "(EUtranCellFDD.earfcndl = 1850 or EUtranCellFDD.earfcndl = 1501) and (EUtranCellFDD.dlChannelBandwidth = 15000 or EUtranCellFDD.dlChannelBandwidth = 20000)";
    private static final String DEFAULT_OPERATOR_PRECEDENCE = "EUtranCellFDD.earfcndl = 1850 or EUtranCellFDD.earfcndl = 1501 and EUtranCellFDD.dlChannelBandwidth = 15000 or EUtranCellFDD.dlChannelBandwidth = 20000";
    // As both operator precedence and IN condition use parenthesis ()
    // usage of operator precedence impacts the conversion of a condition
    // with IN clause to SPEL expression. Conditions below are used to
    // validate and make sure that conditions with IN clause work as expected
    // with operator precedence
    private static final String OPERATOR_PRECEDENCE_FIRST_CONDITION_IN = "(EUtranCellFDD.freqBand in (1,3,7) and EUtranCellFDD.freqBand > 2) or EUtranCellFDD.earfcndl >= 1850";
    private static final String OPERATOR_PRECEDENCE_SECOND_CONDITION_IN = "(EUtranCellFDD.freqBand > 2 and EUtranCellFDD.freqBand in (1,3,7)) or EUtranCellFDD.earfcndl >= 1850";
    private static final String ENUM_EQUALITY_CONDITION = "NRCellDU.csiRsShiftingPrimary = 'DEACTIVATED'";
    private static final String IN_AND_FDN_LIKE_CONDITION_BOTH = "NRCellDU.srsPeriodicity in (40,30,50) and FDN like '%NR03gNodeB%'";
    private static final String IN_AND_FDN_LIKE_CONDITION_PREFIX = "NRCellDU.userLabel like '%Radio00030%' and " +
            "FDN like '%NRCellDU=NR03gNodeBRadio00030-4'";
    private static final String IN_AND_FDN_LIKE_CONDITION_SUFFIX = "NRCellDU.srsPeriodicity = 40 and " +
            "FDN like 'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00050%'";
    private static final String TEST_FDN = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030," +
            "ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4";
    private static final String SPEL_INJECTION_CONDITION = "FDN like T(java.lang.Runtime).getRuntime().exec('dir')";
    private static final String SPEL_EVAL_EXCEPTION_TEST = "srsPeriodicity = 'NOT_BARRED'";
    private static final String USER_LABEL = "userLabel";
    private static final String EARFCNDL = "earfcndl";
    private static final String E_UTRAN_CELL_FDD = "EUtranCellFDD";
    private static final String SECTOR_CARRIER = "SectorCarrier";
    private static final String ESS_SC_PAIR_ID = "essScPairId";
    private static final String INT64 = "int64";
    private static final String INT32 = "int32";
    private static final String CM_HANDLE = "cm-handle-1";
    private static final String FREQ_BAND = "freqBand";
    private static final String STRING = "string";
    private static final String NR_CELL_DU = "NRCellDU";
    private static final String SRS_PERIODICITY = "srsPeriodicity";
    private static final String DL_CHANNEL_BANDWIDTH = "dlChannelBandwidth";
    private static final String CSI_RS_SHIFTING_PRIMARY = "csiRsShiftingPrimary";
    private static final String ENUM = "enumeration";
    private static final String NOT_EQUALS_CONDITION = " NRCellDU.srsPeriodicity != 42";

    private static final String NOT_EQUALS_ENUM_CONDITION = "EUtranCellFDD.pdcchCfiMode != 'CFI_STATIC_BY_BW'";
    private static final String PDCCH_CFI_MODE = "pdcchCfiMode";

    @Mock
    private ModelManager mockModelManager;

    private InMemoryLogAppender logAppender;

    @BeforeEach
    void setUp() {
        ConditionsProcessor.setModelManager(mockModelManager);
        final Logger logUnderTest = (Logger) LoggerFactory.getLogger(ConditionsProcessor.class);
        logAppender = new InMemoryLogAppender();
        logAppender.start();
        logUnderTest.addAppender(logAppender);
    }

    @Test
    void testEqualityConditionSuccessBoolean() {
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, "loadBasedCaEnabled", CM_HANDLE)).thenReturn("boolean");
        assertThat(ConditionsProcessor.evaluate(BOOLEAN_EQUALITY_CONDITION, E_UTRAN_CELL_FDD,
                buildEaccMOs("loadBasedCaEnabled", "false"))).isTrue();
    }

    @Test
    void testSpelInjection() {
        assertThat(ConditionsProcessor.evaluate(SPEL_INJECTION_CONDITION, "EUtranFreqRelation",
                buildEaccMOs("loadBasedCaEnabled", "false"))).isFalse();
    }

    @Test
    void testEqualityConditionSuccess() {
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, EARFCNDL, CM_HANDLE)).thenReturn(INT32);
        assertThat(ConditionsProcessor.evaluate(EQUALITY_CONDITION, E_UTRAN_CELL_FDD, buildEaccMOs(EARFCNDL, "1850")))
                .isTrue();
    }

    @Test
    void testNullConditionFailure() {
        assertThat(ConditionsProcessor.evaluate(null, E_UTRAN_CELL_FDD, buildEaccMOs(EARFCNDL, "1850")))
                .isFalse();
    }

    @Test
    void testEqualityConditionFailure() {
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, EARFCNDL, CM_HANDLE)).thenReturn(INT32);
        assertThat(ConditionsProcessor.evaluate(EQUALITY_CONDITION, E_UTRAN_CELL_FDD, buildEaccMOs(EARFCNDL, "1852")))
                .isFalse();
    }

    @Test
    void testLessThanEqualConditionSuccess() {
        when(mockModelManager.getDataType(SECTOR_CARRIER, ESS_SC_PAIR_ID, CM_HANDLE)).thenReturn(INT64);
        assertThat(ConditionsProcessor.evaluate(LESS_THAN_EQUAL_CONDITION, SECTOR_CARRIER, buildEaccMOs(ESS_SC_PAIR_ID, "9223372036854775806")))
                .isTrue();
    }

    @Test
    void testLessThanEqualConditionFailure() {
        when(mockModelManager.getDataType(SECTOR_CARRIER, ESS_SC_PAIR_ID, CM_HANDLE)).thenReturn(INT64);
        assertThat(ConditionsProcessor.evaluate(LESS_THAN_EQUAL_CONDITION, SECTOR_CARRIER, buildEaccMOs(ESS_SC_PAIR_ID, "9223372036854775807")))
                .isFalse();
    }

    @Test
    void testInConditionWithIntegerValuesSuccess() {
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, FREQ_BAND, CM_HANDLE)).thenReturn(INT32);
        assertThat(ConditionsProcessor.evaluate(IN_CONDITION_INT, E_UTRAN_CELL_FDD, buildEaccMOs(FREQ_BAND, "3")))
                .isTrue();
    }

    @Test
    void testInConditionWithIntegerValuesFailure() {
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, FREQ_BAND, CM_HANDLE)).thenReturn(INT32);
        assertThat(ConditionsProcessor.evaluate(IN_CONDITION_INT, E_UTRAN_CELL_FDD, buildEaccMOs(FREQ_BAND, "8")))
                .isFalse();
    }

    @Test
    void testInConditionWithStringValuesSuccess() {
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, USER_LABEL, CM_HANDLE)).thenReturn(STRING);
        assertThat(ConditionsProcessor.evaluate(IN_CONDITION_STRING, E_UTRAN_CELL_FDD, buildEaccMOs(USER_LABEL, "BSC6697")))
                .isTrue();
    }

    @Test
    void testInConditionWithStringValuesFailure() {
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, USER_LABEL, CM_HANDLE)).thenReturn(STRING);
        assertThat(ConditionsProcessor.evaluate(IN_CONDITION_STRING, E_UTRAN_CELL_FDD, buildEaccMOs(USER_LABEL, "BSC66970")))
                .isFalse();
    }

    @Test
    void testLikeConditionPrefixSuccess() {
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, USER_LABEL, CM_HANDLE)).thenReturn(STRING);
        assertThat(ConditionsProcessor.evaluate(LIKE_CONDITION_PREFIX_MATCH, E_UTRAN_CELL_FDD, buildEaccMOs(USER_LABEL, "ALP")))
                .isTrue();
    }

    @Test
    void testLikeConditionPrefixFailure() {
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, USER_LABEL, CM_HANDLE)).thenReturn(STRING);
        assertThat(ConditionsProcessor.evaluate(LIKE_CONDITION_PREFIX_MATCH, E_UTRAN_CELL_FDD, buildEaccMOs(USER_LABEL, "ALPHA")))
                .isFalse();
    }

    @Test
    void testLikeConditionSuffixSuccess() {
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, USER_LABEL, CM_HANDLE)).thenReturn(STRING);
        assertThat(ConditionsProcessor.evaluate(LIKE_CONDITION_SUFFIX_MATCH, E_UTRAN_CELL_FDD, buildEaccMOs(USER_LABEL, "base-lte-01")))
                .isTrue();
    }

    @Test
    void testLikeConditionSuffixFailure() {
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, USER_LABEL, CM_HANDLE)).thenReturn(STRING);
        assertThat(ConditionsProcessor.evaluate(LIKE_CONDITION_SUFFIX_MATCH, E_UTRAN_CELL_FDD, buildEaccMOs(USER_LABEL, "BaseLte001")))
                .isFalse();
    }

    @Test
    void testLikeConditionBothSuccess() {
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, USER_LABEL, CM_HANDLE)).thenReturn(STRING);
        assertThat(ConditionsProcessor.evaluate(LIKE_CONDITION_BOTH_MATCH, E_UTRAN_CELL_FDD, buildEaccMOs(USER_LABEL, "spocs")))
                .isTrue();
    }

    @Test
    void testLikeConditionBothFailure() {
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, USER_LABEL, CM_HANDLE)).thenReturn(STRING);
        assertThat(ConditionsProcessor.evaluate(LIKE_CONDITION_BOTH_MATCH, E_UTRAN_CELL_FDD, buildEaccMOs(USER_LABEL, "spcs")))
                .isFalse();
    }

    @Test
    void testInAndLikeConditionSuccess() {
        when(mockModelManager.getDataType(NR_CELL_DU, USER_LABEL, CM_HANDLE)).thenReturn(STRING);
        when(mockModelManager.getDataType(NR_CELL_DU, SRS_PERIODICITY, CM_HANDLE)).thenReturn(INT32);
        final Map<String, Object> attributeValues = Map.of(USER_LABEL, "NR05dg2-01", SRS_PERIODICITY, "30");
        assertThat(ConditionsProcessor
                .evaluate(IN_AND_LIKE_CONDITION, NR_CELL_DU, buildEaccMoMultipleAttrs(NR_CELL_DU, attributeValues)))
                        .isTrue();
    }

    @Test
    void testInAndLikeConditionInFailure() {
        when(mockModelManager.getDataType(NR_CELL_DU, USER_LABEL, CM_HANDLE)).thenReturn(STRING);
        when(mockModelManager.getDataType(NR_CELL_DU, SRS_PERIODICITY, CM_HANDLE)).thenReturn(INT32);
        final Map<String, Object> attributeValues = Map.of(USER_LABEL, "NR05dg2-01", SRS_PERIODICITY, "60");
        assertThat(ConditionsProcessor
                .evaluate(IN_AND_LIKE_CONDITION, NR_CELL_DU, buildEaccMoMultipleAttrs(NR_CELL_DU, attributeValues)))
                        .isFalse();
    }

    @Test
    void testInAndLikeConditionLikeFailure() {
        when(mockModelManager.getDataType(NR_CELL_DU, USER_LABEL, CM_HANDLE)).thenReturn(STRING);
        when(mockModelManager.getDataType(NR_CELL_DU, SRS_PERIODICITY, CM_HANDLE)).thenReturn(INT32);
        final Map<String, Object> attributeValues = Map.of(USER_LABEL, "NR05dg1-01", SRS_PERIODICITY, "30");
        assertThat(ConditionsProcessor
                .evaluate(IN_AND_LIKE_CONDITION, NR_CELL_DU, buildEaccMoMultipleAttrs(NR_CELL_DU, attributeValues)))
                        .isFalse();
    }

    @Test
    void testLikeAndInConditionSuccess() {
        when(mockModelManager.getDataType(NR_CELL_DU, USER_LABEL, CM_HANDLE)).thenReturn(STRING);
        when(mockModelManager.getDataType(NR_CELL_DU, SRS_PERIODICITY, CM_HANDLE)).thenReturn(INT32);
        final Map<String, Object> attributeValues = Map.of(USER_LABEL, "NR05dg2-01", SRS_PERIODICITY, "50");
        assertThat(ConditionsProcessor
                .evaluate(LIKE_AND_IN_CONDITION, NR_CELL_DU, buildEaccMoMultipleAttrs(NR_CELL_DU, attributeValues)))
                        .isTrue();
    }

    @Test
    void testInvalidConditionEvaluatesToFalse() {
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, USER_LABEL, CM_HANDLE)).thenReturn(STRING);
        assertThat(ConditionsProcessor.evaluate(INVALID_CONDITION, E_UTRAN_CELL_FDD, buildEaccMOs(USER_LABEL, "spocs")))
                .isFalse();
    }

    @Test
    void testUserDefinedOperatorPrecedence() {
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, EARFCNDL, CM_HANDLE)).thenReturn(INT32);
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, DL_CHANNEL_BANDWIDTH, CM_HANDLE)).thenReturn(INT32);
        final Map<String, Object> attributeValues = Map.of(EARFCNDL, "2000", DL_CHANNEL_BANDWIDTH, "20000");
        assertThat(ConditionsProcessor.evaluate(USER_DEFINED_OPERATOR_PRECEDENCE, E_UTRAN_CELL_FDD,
                buildEaccMoMultipleAttrs(E_UTRAN_CELL_FDD, attributeValues)))
                        .as("User defined OR has higher precedence over AND")
                        .isFalse();
    }

    @Test
    void testDefaultOperatorPrecedence() {
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, EARFCNDL, CM_HANDLE)).thenReturn(INT32);
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, DL_CHANNEL_BANDWIDTH, CM_HANDLE)).thenReturn(INT32);
        final Map<String, Object> attributeValues = Map.of(EARFCNDL, "2000", DL_CHANNEL_BANDWIDTH, "20000");
        assertThat(ConditionsProcessor.evaluate(DEFAULT_OPERATOR_PRECEDENCE, E_UTRAN_CELL_FDD,
                buildEaccMoMultipleAttrs(E_UTRAN_CELL_FDD, attributeValues)))
                        .as("With default precedence conditions are evaluated left to right ")
                        .isTrue();
    }

    @Test
    void testByteDataType() {
        when(mockModelManager.getDataType(anyString(), anyString(), anyString())).thenReturn("int8");
        assertThat(ConditionsProcessor.evaluate(DEFAULT_OPERATOR_PRECEDENCE.replaceAll("0","").replaceAll("1",""), E_UTRAN_CELL_FDD,
                buildEaccMoMultipleAttrs(E_UTRAN_CELL_FDD, Map.of(
                        "dlChannelBandwidth", "2",
                        "earfcndl", "5"))))
                .isTrue();
    }

    @Test
    void testShortDataType() {
        when(mockModelManager.getDataType(anyString(), anyString(), anyString())).thenReturn("int16");
        assertThat(ConditionsProcessor.evaluate(DEFAULT_OPERATOR_PRECEDENCE, E_UTRAN_CELL_FDD,
                buildEaccMoMultipleAttrs(E_UTRAN_CELL_FDD, Map.of(
                        "dlChannelBandwidth", "20000",
                        "earfcndl", "1501"))))
                .isTrue();
    }

    @Test
    void testUnsignedInt16DataType() {
        when(mockModelManager.getDataType(anyString(), anyString(), anyString())).thenReturn("uint16");
        assertThat(ConditionsProcessor.evaluate(DEFAULT_OPERATOR_PRECEDENCE, E_UTRAN_CELL_FDD,
                buildEaccMoMultipleAttrs(E_UTRAN_CELL_FDD, Map.of(
                        "dlChannelBandwidth", "20000", 
                        "earfcndl", "1501"))))
                .isTrue();
    }

    @Test
    void testUnsignedInt32DataType() {
        when(mockModelManager.getDataType(anyString(), anyString(), anyString())).thenReturn("uint32");
        assertThat(ConditionsProcessor.evaluate(DEFAULT_OPERATOR_PRECEDENCE, E_UTRAN_CELL_FDD,
                buildEaccMoMultipleAttrs(E_UTRAN_CELL_FDD, Map.of(
                        "dlChannelBandwidth", "20000", 
                        "earfcndl", "1501"))))
                .isTrue();
    }

    @Test
    void testDecimal64DataType() {
        when(mockModelManager.getDataType(anyString(), anyString(), anyString())).thenReturn("decimal64");
        assertThat(ConditionsProcessor.evaluate(DEFAULT_OPERATOR_PRECEDENCE, E_UTRAN_CELL_FDD,
                buildEaccMoMultipleAttrs(E_UTRAN_CELL_FDD, Map.of(
                        "dlChannelBandwidth", "20000",
                        "earfcndl", "1501"))))
                .isTrue();
    }

    @Test
    void testUnsupportedDataType() {
        when(mockModelManager.getDataType(anyString(), anyString(), anyString())).thenReturn("foo");
        assertThat(ConditionsProcessor.evaluate(DEFAULT_OPERATOR_PRECEDENCE, E_UTRAN_CELL_FDD,
                buildEaccMOs("attrName", "attVal")))
                        .isFalse();
        assertThatLogContainsInOrder("Unsupported data type :", "foo");
    }

    private void assertThatLogContainsInOrder(final String... content) {
        logAppender.stop();
        final List<ILoggingEvent> loggedEvents = logAppender.getLoggedEvents();
        assertThat(loggedEvents).asString()
                .containsSubsequence(content);
    }

    @Test
    void testInConditionsWithOperatorPrecedenceFirstConditionIn() {
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, EARFCNDL, CM_HANDLE)).thenReturn(INT32);
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, FREQ_BAND, CM_HANDLE)).thenReturn(INT32);
        final Map<String, Object> attributeValues = Map.of(EARFCNDL, "1500", FREQ_BAND, "3");
        assertThat(ConditionsProcessor.evaluate(OPERATOR_PRECEDENCE_FIRST_CONDITION_IN, E_UTRAN_CELL_FDD,
                buildEaccMoMultipleAttrs(E_UTRAN_CELL_FDD, attributeValues)))
                        .isTrue();
    }

    @Test
    void testInConditionsWithOperatorPrecedenceSecondConditionIn() {
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, EARFCNDL, CM_HANDLE)).thenReturn(INT32);
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, FREQ_BAND, CM_HANDLE)).thenReturn(INT32);
        final Map<String, Object> attributeValues = Map.of(EARFCNDL, "1500", FREQ_BAND, "3");
        assertThat(ConditionsProcessor.evaluate(OPERATOR_PRECEDENCE_SECOND_CONDITION_IN, E_UTRAN_CELL_FDD,
                buildEaccMoMultipleAttrs(E_UTRAN_CELL_FDD, attributeValues)))
                        .isTrue();
    }

    @Test
    void testEnumEqualitySuccess() {
        when(mockModelManager.getDataType(NR_CELL_DU, CSI_RS_SHIFTING_PRIMARY, CM_HANDLE)).thenReturn(ENUM);
        final Map<String, Object> attributeValues = Map.of(CSI_RS_SHIFTING_PRIMARY, "DEACTIVATED");
        assertThat(ConditionsProcessor.evaluate(ENUM_EQUALITY_CONDITION, NR_CELL_DU,
                buildEaccMoMultipleAttrs(NR_CELL_DU, attributeValues)))
                        .isTrue();
    }

    @Test
    void testEnumEqualityFailure() {
        when(mockModelManager.getDataType(NR_CELL_DU, CSI_RS_SHIFTING_PRIMARY, CM_HANDLE)).thenReturn(ENUM);
        final Map<String, Object> attributeValues = Map.of(CSI_RS_SHIFTING_PRIMARY, "ACTIVATED");
        assertThat(ConditionsProcessor.evaluate(ENUM_EQUALITY_CONDITION, NR_CELL_DU,
                buildEaccMoMultipleAttrs(NR_CELL_DU, attributeValues)))
                        .isFalse();
    }

    @Test
    void testFdnLikeConditionBothMatch() {
        when(mockModelManager.getDataType(NR_CELL_DU, SRS_PERIODICITY, CM_HANDLE)).thenReturn(INT32);
        final Map<String, Object> attributeValues = Map.of(SRS_PERIODICITY, "30");
        assertThat(ConditionsProcessor.evaluate(IN_AND_FDN_LIKE_CONDITION_BOTH, NR_CELL_DU,
                buildEaccMoMultipleAttrs(NR_CELL_DU, attributeValues)))
                        .isTrue();
    }

    @Test
    void testFdnLikeConditionPrefixMatch() {
        when(mockModelManager.getDataType(NR_CELL_DU, USER_LABEL, CM_HANDLE)).thenReturn(STRING);
        final Map<String, Object> attributeValues = Map.of(USER_LABEL, "NR03gNodeBRadio00030");
        assertThat(ConditionsProcessor.evaluate(IN_AND_FDN_LIKE_CONDITION_PREFIX, NR_CELL_DU,
                buildEaccMoMultipleAttrs(NR_CELL_DU, attributeValues)))
                        .isTrue();
    }

    @Test
    void testFdnLikeConditionSuffixMatch() {
        when(mockModelManager.getDataType(NR_CELL_DU, SRS_PERIODICITY, CM_HANDLE)).thenReturn(INT32);
        final Map<String, Object> attributeValues = Map.of(SRS_PERIODICITY, "40");
        assertThat(ConditionsProcessor.evaluate(IN_AND_FDN_LIKE_CONDITION_SUFFIX, NR_CELL_DU,
                buildEaccMoMultipleAttrs(NR_CELL_DU, attributeValues)))
                        .isFalse();
    }

    @Test
    void testNotEqualsSuccess() {
        when(mockModelManager.getDataType(NR_CELL_DU, SRS_PERIODICITY, CM_HANDLE)).thenReturn(INT32);
        final Map<String, Object> attributeValues = Map.of(SRS_PERIODICITY, "40");
        assertThat(ConditionsProcessor.evaluate(NOT_EQUALS_CONDITION, NR_CELL_DU,
                buildEaccMoMultipleAttrs(NR_CELL_DU, attributeValues)))
                        .isTrue();
    }

    @Test
    void testNotEqualsEnumSuccess() {
        when(mockModelManager.getDataType(E_UTRAN_CELL_FDD, PDCCH_CFI_MODE, CM_HANDLE)).thenReturn(ENUM);
        final Map<String, Object> attributeValues = Map.of(PDCCH_CFI_MODE, "CFI_AUTO_MAXIMUM_2");
        assertThat(ConditionsProcessor.evaluate(NOT_EQUALS_ENUM_CONDITION, E_UTRAN_CELL_FDD,
                buildEaccMoMultipleAttrs(NR_CELL_DU, attributeValues)))
                        .isTrue();
    }

    @Test
    void testExceptionInSpelEvaluationResultsInFailedCondition() {
        final Map<String, Object> attributeValues = Map.of(SRS_PERIODICITY, "40");
        assertThat(ConditionsProcessor.evaluate(SPEL_EVAL_EXCEPTION_TEST, NR_CELL_DU,
                buildEaccMoMultipleAttrs(NR_CELL_DU, attributeValues)))
                .isFalse();
    }

    private EaccManagedObject buildEaccMoMultipleAttrs(final String moType, final Map<String, Object> attributeValues) {
        final EaccManagedObject mockEaccManagedObject = new EaccManagedObject();
        mockEaccManagedObject.setMoType(moType);
        mockEaccManagedObject.setFdn(TEST_FDN);
        mockEaccManagedObject.setMoId("testId-1");
        mockEaccManagedObject.setAttributes(attributeValues);
        mockEaccManagedObject.setCmHandle(CM_HANDLE);
        return mockEaccManagedObject;
    }

    private EaccManagedObject buildEaccMOs(final String attrName, final Object attrValue) {
        final EaccManagedObject mockEaccManagedObject = new EaccManagedObject();
        mockEaccManagedObject.setMoType(E_UTRAN_CELL_FDD);
        mockEaccManagedObject.setFdn("testFdn");
        mockEaccManagedObject.setMoId("LTE31dg2ERBS00034-1");
        mockEaccManagedObject.setAttributes(Map.of(attrName, attrValue));
        mockEaccManagedObject.setCmHandle(CM_HANDLE);
        return mockEaccManagedObject;
    }
}