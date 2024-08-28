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

import static com.ericsson.assertions.exception.ControllerDetailExceptionAssertions.assertThat;
import static com.ericsson.assertions.exception.ControllerDetailExceptionAssertions.assertThatCDException;
import static com.ericsson.assertions.exception.ControllerDetailExceptionAssertions.assertThatThrownBy;
import static com.ericsson.oss.apps.util.Constants.EMPTY_STRING;
import static com.ericsson.oss.apps.util.Constants.MAX_RULES_EXCEEDED;
import static com.ericsson.oss.apps.util.Constants.VERSION;
import static com.ericsson.oss.apps.util.TestDefaults.CSV_CONTENT_TYPE;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_RULESET_NAME;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_UUID;
import static com.ericsson.oss.apps.util.TestDefaults.ENODE_BFUNCTION;
import static com.ericsson.oss.apps.util.TestDefaults.EUTRAN_CELL_FDD;
import static com.ericsson.oss.apps.util.TestDefaults.INJECTED_ERROR_MESSAGE_TO_VALIDATE_TEST;
import static com.ericsson.oss.apps.util.TestDefaults.INJECTED_LOG_TO_VALIDATE_TEST;
import static com.ericsson.oss.apps.util.TestDefaults.PRACH_CONFIG_ENABLED;
import static com.ericsson.oss.apps.util.TestDefaults.SOME_IO_EXCEPTION_MESSAGE;
import static com.ericsson.oss.apps.util.TestDefaults.TEST_CSV_NAME;
import static com.ericsson.oss.apps.util.TestDefaults.TEST_SETUP_ERROR;
import static com.ericsson.oss.apps.util.TestDefaults.TEST_SETUP_ERROR_UNABLE_TO_ACCESS_LOGS;
import static com.ericsson.oss.apps.util.TestDefaults.ZZZ_TEMPORARY_74;
import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static java.nio.file.StandardOpenOption.READ;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.apps.CoreApplication;
import com.ericsson.oss.apps.CoreApplicationTest;
import com.ericsson.oss.apps.api.model.EaccRulesetMetadata;
import com.ericsson.oss.apps.controller.ControllerDetailException;
import com.ericsson.oss.apps.model.Rule;
import com.ericsson.oss.apps.model.RuleSet;
import com.ericsson.oss.apps.repository.RuleSetRepository;
import com.ericsson.oss.apps.util.InMemoryLogAppender;
import com.ericsson.oss.apps.validation.RuleSetValidator;
import com.ericsson.oss.apps.validation.RuleValidationException;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;

/**
 * Unit tests for {@link RuleSetService} class.
 */
@ActiveProfiles("test")
@AutoConfigureObservability
@ExtendWith({ MockitoExtension.class, SoftAssertionsExtension.class })
@SpringBootTest(classes = { CoreApplication.class, CoreApplicationTest.class })
public class RuleSetServiceTest {

    private static final String FOLDER = "src/test/resources/";
    private static final String VALID_CSV_PATH_STRING = FOLDER + "SampleRulesetFile.csv";
    private static final Path VALID_CSV_PATH = Path.of(VALID_CSV_PATH_STRING);
    private static final String INVALID_CSV_PATH = FOLDER + "InvalidRulesetFile.csv";
    private static final String INVALID_CONDITION_CSV_PATH_STRING = FOLDER + "InvalidConditionRuleset.csv";
    private static final String FAILING_RULESET_CSV_MAX_RULES = FOLDER + "FailingRulesetMaxRules.csv";
    private static final String FAILING_RULESET_CSV = "IncorrectColumnsRuleset2.csv";
    private static final String URI_PREFIX = VERSION + "/rulesets/";
    private static final String RULE_SET_NAME = "ruleset";
    private static final String INVALID_TEXT_IN_CSV = "illegalText";

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Autowired
    private RuleSetService objectUnderTest;
    @MockBean
    private RuleSetRepository ruleSetRepository;
    @MockBean
    private RuleService ruleService;
    @Mock
    private MockMultipartFile mockMultipartFile;
    @MockBean
    private RuleSetValidator ruleSetValidator;

    @TempDir
    private File csvDir;
    private File failingFile;

    @Value("${database.retry.userInitiated.maxAttempts}")
    private int maxAttempts;

    @BeforeEach
    public void setUp() {
        failingFile = new File(csvDir, FAILING_RULESET_CSV);
    }

    @Test
    void whenCreateRulesetFromCsvIsCalledWithInvalidConditionSyntax__thenRuleValidationExceptionThrown() throws RuleValidationException {
        doThrow(RuleValidationException.class).when(ruleSetValidator).validateRules(anyList());

        final MultipartFile file = parseCsvFile(INVALID_CONDITION_CSV_PATH_STRING);
        assertThatThrownBy(() -> objectUnderTest.createRulesetFromCsv(DEFAULT_RULESET_NAME, file))
                .isInstanceOf(RuleValidationException.class)
                .hasMessage(null);

        verifyNoMoreInteractions(ruleService);
    }

    @Test
    void whenUpdateRulesetFromCsvIsCalledWithInvalidConditionSyntax_thenRuleValidationExceptionThrown() throws RuleValidationException {
        doThrow(RuleValidationException.class).when(ruleSetValidator).validateRules(anyList());

        final MultipartFile file = parseCsvFile(INVALID_CONDITION_CSV_PATH_STRING);
        assertThatThrownBy(() -> objectUnderTest.updateRulesetFromCsv(DEFAULT_UUID, file))
                .isInstanceOf(RuleValidationException.class)
                .hasMessage(null);

        verifyNoMoreInteractions(ruleService);
    }

    @Test
    void whenCreateRulesetFromCsvIsCalledWithValidParameters_thenARulesetIsReturned()
            throws CsvRequiredFieldEmptyException, IOException, RuleValidationException {
        // given
        final MockMultipartFile file = parseCsvFile(VALID_CSV_PATH_STRING);

        // when
        final RuleSet ruleSet = objectUnderTest.createRulesetFromCsv(RULE_SET_NAME, file);

        // then
        verify(ruleSetRepository).save(ruleSet);
        verify(ruleSetValidator).validateRules(ruleSet.getRules());
        softly.assertThat(ruleSet.getName()).isEqualTo(RULE_SET_NAME);
        softly.assertThat(ruleSet.getRules())
                .extracting(Rule::getLineNumber, Rule::getMoType, Rule::getAttributeName, Rule::getAttributeValue)
                .contains(tuple(2L, ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true"),
                        tuple(3L, ENODE_BFUNCTION, ZZZ_TEMPORARY_74, "-2000000000"))
                .hasSize(7);
    }

    @Test
    void whenCreateRulesetFromCsvIsCalledWithNonExistingFile_thenIOExceptionThrown() throws IOException {
        final String message = SOME_IO_EXCEPTION_MESSAGE;
        when(mockMultipartFile.getInputStream()).thenThrow(new IOException(message));
        assertThatThrownBy(() -> objectUnderTest.createRulesetFromCsv(RULE_SET_NAME, mockMultipartFile))
                .hasMessage(message);
    }

    @Test
    void whenParseCsvToRules_invalidContentsAreNotLogged() {
        // given
        final MockMultipartFile file = parseCsvFile(INVALID_CSV_PATH);
        final Logger logUnderTest = (Logger) LoggerFactory.getLogger(RuleSetService.class);
        final InMemoryLogAppender logAppender = new InMemoryLogAppender();
        logAppender.start();
        logUnderTest.addAppender(logAppender);

        logUnderTest.error(INJECTED_LOG_TO_VALIDATE_TEST);
        logUnderTest.error("some error", new IllegalArgumentException(INJECTED_ERROR_MESSAGE_TO_VALIDATE_TEST));

        // when
        assertThatThrownBy(() -> objectUnderTest.parseCsvToRules(file))
                .isInstanceOf(CsvRequiredFieldEmptyException.class)
                .hasMessageNotContaining(INVALID_TEXT_IN_CSV);
        logAppender.stop();
        final List<ILoggingEvent> loggedEvents = logAppender.getLoggedEvents();
        assertThat(loggedEvents).asString()
                .as(TEST_SETUP_ERROR_UNABLE_TO_ACCESS_LOGS)
                .contains(INJECTED_LOG_TO_VALIDATE_TEST);
        softly.assertThat(loggedEvents).asString()
                .contains(INJECTED_LOG_TO_VALIDATE_TEST)
                .doesNotContain(INVALID_TEXT_IN_CSV);

        final var logMessages = loggedEvents.stream()
                .map(ILoggingEvent::getMessage)
                .toList();
        assertThat(logMessages).asString()
                .as(TEST_SETUP_ERROR_UNABLE_TO_ACCESS_LOGS)
                .contains(INJECTED_LOG_TO_VALIDATE_TEST);
        softly.assertThat(logMessages).asString()
                .contains(INJECTED_LOG_TO_VALIDATE_TEST)
                .doesNotContain(INVALID_TEXT_IN_CSV);

        final var throwableMessages = loggedEvents.stream()
                .map(ILoggingEvent::getThrowableProxy)
                .filter(Objects::nonNull)
                .map(IThrowableProxy::getMessage)
                .toList();
        assertThat(throwableMessages).asString()
                .as(TEST_SETUP_ERROR_UNABLE_TO_ACCESS_LOGS)
                .contains(INJECTED_ERROR_MESSAGE_TO_VALIDATE_TEST);
        softly.assertThat(throwableMessages).asString().doesNotContain(INVALID_TEXT_IN_CSV);
    }

    @Test
    void whenCreateRulesetFromCsvICalledWithInvalidCSV_thenRuleValidationException() {
        // given
        final MockMultipartFile file = parseCsvFile(INVALID_CSV_PATH);

        assertThatThrownBy(() -> objectUnderTest.createRulesetFromCsv(RULE_SET_NAME, file))
                .isInstanceOf(CsvRequiredFieldEmptyException.class)
                .hasMessage("File cannot contain null values and column headers must match the Rule model");
    }

    @Test
    void whenCreateEaccRulesetMetadataIsPassedARuleset_thenTheExpectedRulesetMetadataIsReturned() {
        // given
        final RuleSet ruleSet = new RuleSet("testRuleset");

        // when
        final EaccRulesetMetadata metadata = objectUnderTest.createEaccRulesetMetadata(ruleSet);

        // then
        assertThat(metadata).isNotNull();
        softly.assertThat(metadata.getRulesetName()).isEqualTo(ruleSet.getName());
        softly.assertThat(metadata.getId()).isEqualTo(String.valueOf(ruleSet.getId()));
        softly.assertThat(metadata.getUri()).isEqualTo(URI_PREFIX + ruleSet.getId());
    }

    @Test
    void whenParseCsvToRulesIsPassedAValidRuleset_thenAListOfRulesIsReturned_andCorrectLineNumberReturned()
            throws CsvRequiredFieldEmptyException, IOException {
        // given
        final MockMultipartFile file = parseCsvFile(VALID_CSV_PATH_STRING);

        // when
        final List<Rule> rules = objectUnderTest.parseCsvToRules(file);

        // then
        assertThat(rules).hasSize(7)
                .extracting(Rule::getLineNumber, Rule::getMoType, Rule::getAttributeName, Rule::getAttributeValue, Rule::getPriority)
                .contains(tuple(2L, ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true", 1),
                        tuple(3L, ENODE_BFUNCTION, ZZZ_TEMPORARY_74, "-2000000000", 1));
    }

    @Test
    void whenGetAllRulesetMetaDataIsCalled_thenMetaDataForAllPersistedRulesetsIsReturned() {
        // given
        final RuleSet ruleSet = new RuleSet("testRuleset");
        when(ruleSetRepository.findAll()).thenReturn(List.of(ruleSet));

        // when 
        final List<EaccRulesetMetadata> allMetaData = objectUnderTest.getAllRulesetMetadata();

        // then
        assertThat(allMetaData).hasSize(1);
        final EaccRulesetMetadata metadata = allMetaData.get(0);
        assertThat(metadata).isNotNull();
        softly.assertThat(metadata.getRulesetName()).isEqualTo(ruleSet.getName());
        softly.assertThat(metadata.getId()).isEqualTo(String.valueOf(ruleSet.getId()));
        softly.assertThat(metadata.getUri()).isEqualTo(URI_PREFIX + ruleSet.getId());
    }

    @Test
    void whenParseCsvToRulesIsPassedAMissingColumn_thenCsvRequiredFieldEmptyExceptionIsReturnedBest() throws CsvException, IOException {
        //given
        try (final Reader bufferedReader = Files.newBufferedReader(VALID_CSV_PATH);
                final CSVReader csvReader = new CSVReaderBuilder(bufferedReader)
                        .withSkipLines(0)
                        .build()) {
            final List<String[]> rules = csvReader.readAll();

            rules.add(new String[] { "99", EUTRAN_CELL_FDD, EMPTY_STRING, "false" });

            writeRulesToFile(rules);
        }
        assertThat(failingFile).as(TEST_SETUP_ERROR)
                .exists()
                .canRead();

        final InputStream inputStream = Files.newInputStream(failingFile.toPath(), READ);
        final MockMultipartFile file = new MockMultipartFile(RULE_SET_NAME, failingFile.getPath(), MULTIPART_FORM_DATA, inputStream);

        assertThatThrownBy(() -> objectUnderTest.parseCsvToRules(file))
                .isInstanceOf(CsvRequiredFieldEmptyException.class)
                .hasMessage("File cannot contain null values and column headers must match the Rule model");
    }

    @Test
    void whenParseCsvToRulesIsPassedMissingColumnHeaders_thenCsvRequiredFieldEmptyExceptionIsReturned() throws CsvException, IOException {
        //given
        try (final Reader bufferedReader = Files.newBufferedReader(VALID_CSV_PATH);
                final CSVReader csvReader = new CSVReaderBuilder(bufferedReader)
                        .withSkipLines(1)
                        .build()) {
            final List<String[]> rules = csvReader.readAll();

            writeRulesToFile(rules);
        }
        try (final Reader bufferedReader = Files.newBufferedReader(VALID_CSV_PATH);
                final CSVReader csvReader = new CSVReaderBuilder(bufferedReader)
                        .withSkipLines(0)
                        .build()) {
            final List<String[]> rules = csvReader.readAll();

            rules.add(new String[] { "99", EUTRAN_CELL_FDD, EMPTY_STRING, "false" });

            writeRulesToFile(rules);
        }
        assertThat(failingFile).as(TEST_SETUP_ERROR)
                .exists()
                .canRead();
        final AtomicReference<MultipartFile> file = new AtomicReference<>();
        assertThatCode(() -> {
            try (final InputStream inputStream = Files.newInputStream(failingFile.toPath(), READ)) {
                file.set(new MockMultipartFile(RULE_SET_NAME, failingFile.getPath(), MULTIPART_FORM_DATA, inputStream));
            }
        }).doesNotThrowAnyException();
        assertThatThrownBy(() -> objectUnderTest.parseCsvToRules(file.get()))
                .isInstanceOf(CsvRequiredFieldEmptyException.class)
                .hasMessage("File cannot contain null values and column headers must match the Rule model");
    }

    @Test
    void whenParseCsvToRulesIsPassedNotEnoughColumns_thenCsvRequiredFieldEmptyExceptionIsReturned() throws CsvException, IOException {
        //given
        try (final Reader bufferedReader = Files.newBufferedReader(VALID_CSV_PATH);
                final CSVReader csvReader = new CSVReaderBuilder(bufferedReader)
                        .withSkipLines(0)
                        .build()) {
            final List<String[]> rules = csvReader.readAll();

            rules.add(new String[] { "99", EUTRAN_CELL_FDD });

            writeRulesToFile(rules);
        }
        assertThat(failingFile).as(TEST_SETUP_ERROR)
                .exists()
                .canRead();

        final AtomicReference<MultipartFile> file = new AtomicReference<>();
        assertThatCode(() -> {
            try (final InputStream inputStream = Files.newInputStream(failingFile.toPath(), READ)) {
                file.set(new MockMultipartFile(RULE_SET_NAME, failingFile.getPath(), MULTIPART_FORM_DATA, inputStream));
            }
        }).doesNotThrowAnyException();

        assertThatThrownBy(() -> objectUnderTest.parseCsvToRules(file.get()))
                .isInstanceOf(CsvRequiredFieldEmptyException.class)
                .hasMessage("File cannot contain null values and column headers must match the Rule model");
    }

    @Test
    void whenParseCsvToRulesIsPassedWrongColumnHeaderNames_thenCsvRequiredFieldEmptyExceptionIsReturned() throws CsvException, IOException {
        //given
        try (final Reader fileReader = Files.newBufferedReader(VALID_CSV_PATH);
                final CSVReader csvReader = new CSVReaderBuilder(fileReader)
                        .withSkipLines(1)
                        .build()) {
            final List<String[]> rules = csvReader.readAll();

            rules.add(0, new String[] { "id", "attributeName", "attributeValue" });

            writeRulesToFile(rules);
        }

        assertThat(failingFile).as(TEST_SETUP_ERROR)
                .exists()
                .canRead();

        final AtomicReference<MultipartFile> file = new AtomicReference<>();
        assertThatCode(() -> {
            try (final InputStream inputStream = Files.newInputStream(failingFile.toPath(), READ)) {
                file.set(new MockMultipartFile(RULE_SET_NAME, failingFile.getPath(), MULTIPART_FORM_DATA, inputStream));
            }
        }).doesNotThrowAnyException();

        assertThatThrownBy(() -> objectUnderTest.parseCsvToRules(file.get()))
                .isInstanceOf(CsvRequiredFieldEmptyException.class)
                .hasMessage("File cannot contain null values and column headers must match the Rule model");
    }

    @Test
    void whenParseCsvToRulesIsPassedWithMoreThanMaxRules_thenControllerDetailExceptionThrown() {
        // given
        final MockMultipartFile file = parseCsvFile(FAILING_RULESET_CSV_MAX_RULES);

        // when
        assertThatCDException(() -> objectUnderTest.parseCsvToRules(file))
                .isInstanceOf(ControllerDetailException.class)
                .hasMessageContaining(MAX_RULES_EXCEEDED);
    }

    @Test
    void whenCreateCsvFromRuleseWithoutConditionsColumn_thenByteArrayResourceIsReturned() throws IOException {
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true"));

        when(ruleService.getRulesForRulesetId(DEFAULT_UUID)).thenReturn(rules);

        final ByteArrayResource csvByteArray = objectUnderTest.createCsvFromRuleset(DEFAULT_UUID);

        assertThat(csvByteArray).isInstanceOf(ByteArrayResource.class);
        assertThat(csvByteArray.getByteArray()).hasSize(115);

        final String converted = new String(csvByteArray.getByteArray(), StandardCharsets.UTF_8);
        assertThat(converted).isEqualTo("""
                "moType","attributeName","attributeValue","conditions","priority"
                "ENodeBFunction","prachConfigEnabled","true",,""
                """);
        verify(ruleService, times(1)).getRulesForRulesetId(DEFAULT_UUID);
        verifyNoMoreInteractions(ruleService);

    }

    @Test
    void whenCreateCsvFromRulesetWithValidRuleset_andContainsConditions_thenByteArrayResourceIsReturned() throws IOException {
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule("ENodeBFunction", "prachConfigEnabled", "true", "cellCapMinMaxWriProt = true"));
        rules.add(new Rule("ENodeBFunction", "prachConfigEnabled", "true", "EUtranCellFDD.cellCapMinCellSubCap in (98,100,102)"));
        rules.add(new Rule("ENodeBFunction", "prachConfigEnabled", "true", "EUtranCellFDD.userLabel like '%dg2%'"));

        when(ruleService.getRulesForRulesetId(DEFAULT_UUID)).thenReturn(rules);

        final ByteArrayResource csvByteArray = objectUnderTest.createCsvFromRuleset(DEFAULT_UUID);

        assertThat(csvByteArray).isInstanceOf(ByteArrayResource.class);
        assertThat(csvByteArray.getByteArray()).hasSize(332);

        final String converted = new String(csvByteArray.getByteArray(), StandardCharsets.UTF_8);
        assertThat(converted).isEqualTo("""
                "moType","attributeName","attributeValue","conditions","priority"
                "ENodeBFunction","prachConfigEnabled","true","cellCapMinMaxWriProt = true",""
                "ENodeBFunction","prachConfigEnabled","true","EUtranCellFDD.cellCapMinCellSubCap in (98,100,102)",""
                "ENodeBFunction","prachConfigEnabled","true","EUtranCellFDD.userLabel like '%dg2%'",""
                """);
        verify(ruleService, times(1)).getRulesForRulesetId(DEFAULT_UUID);
        verifyNoMoreInteractions(ruleService);
    }

    @Test
    void whenUpdateRulesetFromCsvWithValidInput_thenRulesetIsUpdated() throws CsvRequiredFieldEmptyException, IOException, RuleValidationException {
        final RuleSet ruleSet = new RuleSet(DEFAULT_RULESET_NAME);
        ruleSet.setId(DEFAULT_UUID);

        final MockMultipartFile file = new MockMultipartFile(
                TEST_CSV_NAME,
                TEST_CSV_NAME,
                CSV_CONTENT_TYPE,
                "moType,attributeName,attributeValue\nENodeBFunction,prachConfigEnabled,true".getBytes());

        when(ruleSetRepository.getById(ruleSet.getId())).thenReturn(ruleSet);

        final RuleSet updatedRuleSet = objectUnderTest.updateRulesetFromCsv(ruleSet.getId(), file);

        assertThat(updatedRuleSet.getRules())
                .extracting(Rule::getMoType, Rule::getAttributeName, Rule::getAttributeValue)
                .contains(tuple(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true"));

        verify(ruleSetRepository, times(1)).getById(ruleSet.getId());
        verify(ruleSetValidator).validateRules(ruleSet.getRules());
        verify(ruleSetRepository, times(1)).save(ruleSet);
    }

    @Test
    public void whenExistsById_WithAnExistingRuleSet_trueIsReturned() {
        when(ruleSetRepository.existsById(DEFAULT_UUID)).thenReturn(false);
        assertThat(objectUnderTest.existsById(DEFAULT_UUID)).isFalse();
        verify(ruleSetRepository, times(1)).existsById(DEFAULT_UUID);
        verifyNoMoreInteractions(ruleSetRepository);
    }

    @Test
    public void whenExistsById_andRulesetDoesNotExist_falseIsReturned() {
        when(ruleSetRepository.existsById(DEFAULT_UUID)).thenReturn(true);
        assertThat(objectUnderTest.existsById(DEFAULT_UUID)).isTrue();
        verify(ruleSetRepository, times(1)).existsById(DEFAULT_UUID);
        verifyNoMoreInteractions(ruleSetRepository);
    }

    @Test
    public void whenGetRulesetNameIsCalled_andTheRulesetExists_theRulesetNameIsReturned() {
        final String testRulesetName = "testRuleset";
        final RuleSet ruleset = new RuleSet();
        ruleset.setName(testRulesetName);
        final Optional<RuleSet> rulesetOptional = Optional.of(ruleset);
        when(ruleSetRepository.findById(any(UUID.class))).thenReturn(rulesetOptional);

        assertThat(objectUnderTest.getRulesetNameFromId(DEFAULT_UUID)).isEqualTo(testRulesetName);

    }

    @Test
    public void whenGetRulesetNameIsCalled_andTheRulesetDoesNotExists_anIllegalArgumentExceptionIsThrown() {
        when(ruleSetRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> objectUnderTest.getRulesetNameFromId(DEFAULT_UUID)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void whenDeleteRulesetIsCalled_andTheRulesetExists_theRulesetIsDeleted() {
        final String testRulesetName = "testRuleset";
        final RuleSet ruleset = new RuleSet();
        ruleset.setName(testRulesetName);
        final Optional<RuleSet> rulesetOptional = Optional.of(ruleset);
        when(ruleSetRepository.findById(any(UUID.class))).thenReturn(rulesetOptional);

        objectUnderTest.deleteRulesetById(DEFAULT_UUID);
        verify(ruleSetRepository, times(1)).deleteById(DEFAULT_UUID);
    }

    @Test
    public void whenDeleteRulesetIsCalled_andTheRulesetDoesNotExist_IllegalArgumentExceptionIsThrown() {
        when(ruleSetRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> objectUnderTest.deleteRulesetById(DEFAULT_UUID)).isInstanceOf(IllegalArgumentException.class);
    }

    private void writeRulesToFile(final List<String[]> rules) throws IOException {
        try (final Writer bufferedWriter = Files.newBufferedWriter(failingFile.toPath());
                final CSVWriter writer = new CSVWriter(bufferedWriter)) {
            writer.writeAll(rules);
        }
    }

    private static MockMultipartFile parseCsvFile(final String pathToCsv) {
        MockMultipartFile file = null;
        try (final InputStream inputStream = Files.newInputStream(Path.of(pathToCsv), READ)) {
            file = new MockMultipartFile(RULE_SET_NAME, VALID_CSV_PATH_STRING, MULTIPART_FORM_DATA, inputStream);
        } catch (final IOException e) {
            fail("TEST SETUP EXCEPTION", e);
        }
        return file;
    }

    @Test
    public void whenCreateRulesetFromCsvMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        final MockMultipartFile file = parseCsvFile(VALID_CSV_PATH_STRING);
        when(ruleSetRepository.save(any())).thenThrow(CannotCreateTransactionException.class);
        assertThatThrownBy(() -> objectUnderTest.createRulesetFromCsv("rulesetName", file)).isInstanceOf(TransactionException.class);
        verify(ruleSetRepository, times(maxAttempts)).save(any());
    }

    @Test
    public void whenGetAllRulesetMetadataMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(ruleSetRepository.findAll()).thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.getAllRulesetMetadata()).isInstanceOf(DataAccessException.class);
        verify(ruleSetRepository, times(maxAttempts)).findAll();
    }

    @Test
    public void whenDeleteRulesetByIdMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(ruleSetRepository.findById(any())).thenThrow(CannotCreateTransactionException.class).thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.deleteRulesetById(new UUID(1, 1))).isInstanceOf(DataAccessException.class);
        verify(ruleSetRepository, times(maxAttempts)).findById((any()));
    }

    @Test
    public void whenGetRulesetNameFromIdMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(ruleSetRepository.findById(any())).thenThrow(CannotCreateTransactionException.class).thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.getRulesetNameFromId(new UUID(1, 1))).isInstanceOf(DataAccessException.class);
        verify(ruleSetRepository, times(maxAttempts)).findById((any()));
    }

    @Test
    public void whenExistsByNameMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(ruleSetRepository.existsByName(any())).thenThrow(CannotCreateTransactionException.class)
                .thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.existsByName("rulesetName")).isInstanceOf(DataAccessException.class);
        verify(ruleSetRepository, times(maxAttempts)).existsByName((any()));
    }

    @Test
    public void whenExistsByIdMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(ruleSetRepository.existsById(any())).thenThrow(CannotCreateTransactionException.class)
                .thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.existsById(new UUID(1, 1))).isInstanceOf(DataAccessException.class);
        verify(ruleSetRepository, times(maxAttempts)).existsById((any()));
    }

    @Test
    public void whenUpdateRulesetFromCsvMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        final MockMultipartFile file = parseCsvFile(VALID_CSV_PATH_STRING);
        when(ruleSetRepository.getById(any())).thenThrow(CannotCreateTransactionException.class).thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.updateRulesetFromCsv(new UUID(1, 1), file)).isInstanceOf(DataAccessException.class);
        verify(ruleSetRepository, times(maxAttempts)).getById((any()));
    }
}