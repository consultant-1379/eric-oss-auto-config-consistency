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

package com.ericsson.oss.apps.controller;

import static com.ericsson.assertions.EaccAssertions.assertThat;
import static com.ericsson.assertions.EaccAssertions.assertThatCDException;
import static com.ericsson.oss.apps.util.Constants.APPLICATION_JSON;
import static com.ericsson.oss.apps.util.Constants.CSV_CONTENT_TYPE;
import static com.ericsson.oss.apps.util.Constants.CSV_FILENAME_TEMPLATE;
import static com.ericsson.oss.apps.util.Constants.DATABASE_OPERATION_FAILED;
import static com.ericsson.oss.apps.util.Constants.FAILED_TO_CREATE_RULESET;
import static com.ericsson.oss.apps.util.Constants.FAILED_TO_UPDATE_RULESET;
import static com.ericsson.oss.apps.util.Constants.INVALID_UUID;
import static com.ericsson.oss.apps.util.Constants.PROVIDE_A_VALID_RULESET_ID;
import static com.ericsson.oss.apps.util.Constants.REGEX_RULESET_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_FAILED;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_RULESET_NAME;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_UUID;
import static com.ericsson.oss.apps.util.TestDefaults.ENODE_BFUNCTION;
import static com.ericsson.oss.apps.util.TestDefaults.FILE;
import static com.ericsson.oss.apps.util.TestDefaults.FILL_IN_ALL_REQUIRED_FIELDS;
import static com.ericsson.oss.apps.util.TestDefaults.INVALID_RULESET_NAME_MESSAGE;
import static com.ericsson.oss.apps.util.TestDefaults.PRACH_CONFIG_ENABLED;
import static com.ericsson.oss.apps.util.TestDefaults.REALLY_LONG_ALPHANUMERIC_STRING;
import static com.ericsson.oss.apps.util.TestDefaults.RULESET_ID_EXAMPLE;
import static com.ericsson.oss.apps.util.TestDefaults.RULESET_URI_EXAMPLE;
import static com.ericsson.oss.apps.util.TestDefaults.TEST_CSV_NAME;
import static java.nio.file.StandardOpenOption.READ;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.apps.api.model.EaccRulesetMetadata;
import com.ericsson.oss.apps.model.Rule;
import com.ericsson.oss.apps.model.RuleSet;
import com.ericsson.oss.apps.service.ExecutionService;
import com.ericsson.oss.apps.service.JobService;
import com.ericsson.oss.apps.service.RuleSetService;
import com.ericsson.oss.apps.service.ValidationService;
import com.ericsson.oss.apps.validation.ModelManagerImpl;
import com.ericsson.oss.apps.validation.RuleValidationError;
import com.ericsson.oss.apps.validation.RuleValidationException;

import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

/**
 * Unit tests for {@link RulesetController} class.
 */
@ExtendWith(MockitoExtension.class)
class RulesetControllerTest {

    private static final String PATH = "src/test/resources/SampleRulesetFile.csv";
    private static final String ERROR_PARSING_ID_PROVIDED_ID_IS_INVALID = "Error parsing ID. Provided ID is invalid.";
    private static final String STATUS_VALIDATION_FAILED = "%s \"Validation failed\"";
    private static final String FILE_CONTENT = "moType,attributeName,attributeValue\nENodeBFunction,prachConfigEnabled,true";
    private static final String RULESET_NAME = "ruleset";
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    @Mock
    private RuleSetService ruleSetService;
    @Mock
    private JobService jobService;
    @Mock
    private ExecutionService executionService;
    @Mock
    private ValidationService validationService;
    @Mock
    private ModelManagerImpl modelManager;

    @InjectMocks
    private RulesetController objectUnderTest;

    private MockMultipartFile file;
    private String rulesetName;

    @Test
    public void whenGetAllRulesetsEndpointIsHit_thenOkIsReturned() {
        when(ruleSetService.getAllRulesetMetadata()).thenReturn(createRulesetMetadadata());
        assertThat(objectUnderTest.listRulesets())
                .hasStatus(OK)
                .hasBody(createRulesetMetadadata());
    }

    @Test
    public void whenExceptionIsThrownWhenReadingRulesets_thenResponseStatusExceptionsIsThrown() {
        final RuntimeException cause = new RuntimeException();
        when(ruleSetService.getAllRulesetMetadata()).thenThrow(cause);
        assertThatCDException(() -> objectUnderTest.listRulesets())
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasMessage("%s \"Failed to get all RuleSet metadata.\"", INTERNAL_SERVER_ERROR)
                .hasSuppressedException(cause)
                .hasNoDetail()
                .hasTitle("Failed to get all RuleSet metadata.");
    }

    @Test
    public void whenDataAccessExceptionIsThrownWhenReadingRulesets_thenControllerDetailExceptionIsThrown() {
        when(ruleSetService.getAllRulesetMetadata()).thenThrow(DataAccessResourceFailureException.class);
        assertThatCDException(() -> objectUnderTest.listRulesets())
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasDetail(DATABASE_OPERATION_FAILED)
                .hasReason("Failed to get rulesets.");
    }

    @Test
    void whenUpdateRulesetEndpointIsHit_thenRulesetIsUpdated() throws CsvRequiredFieldEmptyException, IOException, RuleValidationException {
        file = new MockMultipartFile(
                TEST_CSV_NAME,
                TEST_CSV_NAME,
                CSV_CONTENT_TYPE,
                FILE_CONTENT.getBytes());

        final List<Rule> rules = List.of(
                new Rule(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true", ""));

        when(ruleSetService.existsById(DEFAULT_UUID)).thenReturn(Boolean.TRUE);
        when(ruleSetService.getRulesetNameFromId(DEFAULT_UUID)).thenReturn(DEFAULT_RULESET_NAME);
        when(modelManager.isModelValidationReady()).thenReturn(Boolean.TRUE);
        final RuleSet updatedRuleSet = new RuleSet(DEFAULT_RULESET_NAME);
        updatedRuleSet.setRules(rules);

        when(ruleSetService.updateRulesetFromCsv(DEFAULT_UUID, file)).thenReturn(updatedRuleSet);
        final EaccRulesetMetadata updatedMetadata = new EaccRulesetMetadata(DEFAULT_UUID.toString(), DEFAULT_RULESET_NAME, RULESET_URI_EXAMPLE);

        when(ruleSetService.createEaccRulesetMetadata(updatedRuleSet)).thenReturn(updatedMetadata);

        final ResponseEntity<EaccRulesetMetadata> responseEntity = objectUnderTest.updateRuleset(DEFAULT_UUID.toString(), file, APPLICATION_JSON,
                APPLICATION_JSON);

        assertThat(responseEntity).hasStatus(OK)
                .hasBody(updatedMetadata);

        verify(ruleSetService).updateRulesetFromCsv(DEFAULT_UUID, file);
        verify(ruleSetService).createEaccRulesetMetadata(updatedRuleSet);
        verify(ruleSetService).existsById(DEFAULT_UUID);
        verify(ruleSetService).getRulesetNameFromId(DEFAULT_UUID);
        verify(executionService).isRulesetInUse(DEFAULT_RULESET_NAME);
        verifyNoMoreInteractions(ruleSetService);
    }

    @Test
    void whenUpdateRulesetEndpointIsHit_andModelDiscoveryIsNotReady_thenServiceUnavailableIsReturned() {
        file = new MockMultipartFile(
                TEST_CSV_NAME,
                TEST_CSV_NAME,
                CSV_CONTENT_TYPE,
                FILE_CONTENT.getBytes());

        final List<Rule> rules = List.of(
                new Rule(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true"));

        when(ruleSetService.existsById(DEFAULT_UUID)).thenReturn(Boolean.TRUE);
        when(ruleSetService.getRulesetNameFromId(DEFAULT_UUID)).thenReturn(DEFAULT_RULESET_NAME);
        when(modelManager.isModelValidationReady()).thenReturn(Boolean.FALSE);
        final RuleSet updatedRuleSet = new RuleSet(DEFAULT_RULESET_NAME);
        updatedRuleSet.setRules(rules);

        assertThatCDException(() -> objectUnderTest.updateRuleset(DEFAULT_UUID.toString(), file, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(SERVICE_UNAVAILABLE)
                .hasTitle("Failed to create/update ruleset. Service not ready.")
                .hasDetail("Model Discovery has not yet completed and therefore cannot validate the ruleset.");

        verifyNoMoreInteractions(ruleSetService);
    }

    @Test
    void whenUpdateRulesetEndpoint_andJobExecutionInProgress_thenThrowControllerDetailException() {

        when(ruleSetService.existsById(DEFAULT_UUID)).thenReturn(Boolean.TRUE);

        when(ruleSetService.getRulesetNameFromId(DEFAULT_UUID)).thenReturn(DEFAULT_RULESET_NAME);

        when(executionService.isRulesetInUse(DEFAULT_RULESET_NAME)).thenReturn(true);

        assertThatCDException(//
                () -> objectUnderTest.updateRuleset(DEFAULT_UUID.toString(), any(MultipartFile.class), APPLICATION_JSON, APPLICATION_JSON))
                        .hasStatus(CONFLICT)
                        .hasMessage("%s \"Cannot update ruleset as it is used by an ongoing execution.\"", CONFLICT)
                        .hasDetail("Ruleset in use.")
                        .hasTitle("Cannot update ruleset as it is used by an ongoing execution.");

        verify(ruleSetService).existsById(DEFAULT_UUID);
        verify(ruleSetService).getRulesetNameFromId(DEFAULT_UUID);
        verify(executionService).isRulesetInUse(DEFAULT_RULESET_NAME);
    }

    @Test
    void whenUpdateRulesetEndpointIsHitWithLongId_thenThrowsIllegalArgumentException() {
        final IllegalArgumentException cause = new IllegalArgumentException("UUID string too large");
        file = new MockMultipartFile(
                TEST_CSV_NAME,
                TEST_CSV_NAME,
                CSV_CONTENT_TYPE,
                FILE_CONTENT.getBytes());

        assertThatCDException(() -> objectUnderTest.updateRuleset(REALLY_LONG_ALPHANUMERIC_STRING, file, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(BAD_REQUEST)
                .hasMessage("%s \"Error parsing ID. Provided ID is invalid.\"", BAD_REQUEST)
                .hasSuppressedException(cause)
                .hasDetail(PROVIDE_A_VALID_RULESET_ID)
                .hasTitle(ERROR_PARSING_ID_PROVIDED_ID_IS_INVALID);
    }

    @Test
    void whenUpdateRulesetEndpointIsHitWithInvalidId_thenThrowsIllegalArgumentException() {
        final IllegalArgumentException cause = new IllegalArgumentException("Invalid UUID string: invalid-uuid");
        file = new MockMultipartFile(TEST_CSV_NAME, TEST_CSV_NAME, CSV_CONTENT_TYPE,
                FILE_CONTENT.getBytes());

        assertThatCDException(() -> objectUnderTest.updateRuleset(INVALID_UUID, file, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(BAD_REQUEST)
                .hasSuppressedException(cause)
                .hasMessage("%s \"Error parsing ID. Provided ID is invalid.\"", BAD_REQUEST)
                .hasDetail("Provide a valid Ruleset ID.")
                .hasTitle(ERROR_PARSING_ID_PROVIDED_ID_IS_INVALID);
    }

    @Test
    void whenUpdateRulesetEndpointIsHit_withCsvRequiredFieldEmptyException_thenBadRequestIsThrown()
            throws CsvRequiredFieldEmptyException, IOException, RuleValidationException {
        file = new MockMultipartFile(TEST_CSV_NAME, TEST_CSV_NAME, CSV_CONTENT_TYPE,
                "moType,,attributeValue\nENodeBFunction,prachConfigEnabled,true".getBytes());

        when(ruleSetService.existsById(DEFAULT_UUID)).thenReturn(Boolean.TRUE);
        when(modelManager.isModelValidationReady()).thenReturn(Boolean.TRUE);
        when(ruleSetService.getRulesetNameFromId(DEFAULT_UUID)).thenReturn(DEFAULT_RULESET_NAME);

        final CsvRequiredFieldEmptyException cause = new CsvRequiredFieldEmptyException();
        when(ruleSetService.updateRulesetFromCsv(DEFAULT_UUID, file)).thenThrow(cause);

        assertThatCDException(() -> objectUnderTest.updateRuleset(DEFAULT_UUID.toString(), file, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(BAD_REQUEST)
                .hasMessage("%s \"File cannot contain null values and column headers must match the Rule model\"",
                        BAD_REQUEST)
                .hasSuppressedException(cause)
                .hasDetail("Fill in all required fields with the appropriate information");
        verify(ruleSetService).updateRulesetFromCsv(DEFAULT_UUID, file);
        verify(ruleSetService).existsById(DEFAULT_UUID);
        verify(ruleSetService).getRulesetNameFromId(DEFAULT_UUID);
        verify(executionService).isRulesetInUse(DEFAULT_RULESET_NAME);
        verifyNoMoreInteractions(ruleSetService);
    }

    @Test
    void whenUpdateRulesetEndpointIsHit_withIOException_thenInternalServerErrorIsThrown()
            throws CsvRequiredFieldEmptyException, IOException, RuleValidationException {
        file = new MockMultipartFile(TEST_CSV_NAME, TEST_CSV_NAME, CSV_CONTENT_TYPE,
                FILE_CONTENT.getBytes());

        when(ruleSetService.existsById(DEFAULT_UUID)).thenReturn(Boolean.TRUE);
        when(ruleSetService.getRulesetNameFromId(DEFAULT_UUID)).thenReturn(DEFAULT_RULESET_NAME);
        when(modelManager.isModelValidationReady()).thenReturn(Boolean.TRUE);

        final IOException cause = new IOException();
        when(ruleSetService.updateRulesetFromCsv(DEFAULT_UUID, file)).thenThrow(cause);

        assertThatCDException(() -> objectUnderTest.updateRuleset(DEFAULT_UUID.toString(), file, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasMessage("%s \"Failed to update ruleset.\"", INTERNAL_SERVER_ERROR)
                .hasSuppressedException(cause)
                .hasDetail("Failed to read InputStream")
                .hasTitle(FAILED_TO_UPDATE_RULESET);
        verify(ruleSetService).updateRulesetFromCsv(DEFAULT_UUID, file);
        verify(ruleSetService).existsById(DEFAULT_UUID);
        verify(ruleSetService).getRulesetNameFromId(DEFAULT_UUID);
        verify(executionService).isRulesetInUse(DEFAULT_RULESET_NAME);
        verifyNoMoreInteractions(ruleSetService);
    }

    @Test
    public void whenPostRulesetEndpointIsHit_thenCreatedIsReturned() throws CsvRequiredFieldEmptyException, IOException, RuleValidationException {
        final InputStream inputStream = Files.newInputStream(Path.of(PATH), READ);
        final MultipartFile file = new MockMultipartFile(FILE, PATH, MULTIPART_FORM_DATA.getType(), inputStream);
        rulesetName = "sample_ruleset";

        final EaccRulesetMetadata eaccRulesetMetadata = new EaccRulesetMetadata(DEFAULT_UUID.toString(), "v1/ruleset", DEFAULT_RULESET_NAME);

        when(ruleSetService.createRulesetFromCsv(eq(rulesetName), any(MultipartFile.class))).thenReturn(new RuleSet());
        when(ruleSetService.createEaccRulesetMetadata(any(RuleSet.class))).thenReturn(eaccRulesetMetadata);
        when(validationService.validateRulesetName(anyString())).thenCallRealMethod();
        when(ruleSetService.existsByName(any())).thenReturn(false);
        when(modelManager.isModelValidationReady()).thenReturn(Boolean.TRUE);
        final ResponseEntity<EaccRulesetMetadata> responseEntity = objectUnderTest.createRuleset(rulesetName, file);
        assertThat(responseEntity).hasStatus(CREATED);

        verify(ruleSetService).createRulesetFromCsv(eq(rulesetName), any(MultipartFile.class));
        verify(ruleSetService).createEaccRulesetMetadata(any(RuleSet.class));
        verify(ruleSetService).existsByName(rulesetName);
        verifyNoMoreInteractions(ruleSetService);
    }

    @Test
    public void whenPostRulesetEndpointIsHit_InvalidNameIsRejected() {
        file = new MockMultipartFile(FILE, PATH, MULTIPART_FORM_DATA.getType(), EMPTY_BYTE_ARRAY);
        rulesetName = "invalid ruleset name!";
        when(validationService.validateRulesetName(anyString())).thenCallRealMethod();

        assertThatCDException(() -> objectUnderTest.createRuleset(rulesetName, file))
                .hasStatus(BAD_REQUEST)
                .hasMessage(STATUS_VALIDATION_FAILED, BAD_REQUEST)
                .hasDetail(INVALID_RULESET_NAME_MESSAGE)
                .hasTitle(VALIDATION_FAILED);

        verifyNoInteractions(ruleSetService);
    }

    @Test
    public void whenPostRulesetEndpointIsHit_UppercaseIsRejected() {
        file = new MockMultipartFile(FILE, PATH, MULTIPART_FORM_DATA.getType(), EMPTY_BYTE_ARRAY);
        rulesetName = "InvalidRulesetName";
        when(validationService.validateRulesetName(anyString())).thenCallRealMethod();

        assertThatCDException(() -> objectUnderTest.createRuleset(rulesetName, file))
                .hasStatus(BAD_REQUEST)
                .hasMessage(STATUS_VALIDATION_FAILED, BAD_REQUEST)
                .hasDetail(INVALID_RULESET_NAME_MESSAGE)
                .hasTitle(VALIDATION_FAILED);

        verifyNoInteractions(ruleSetService);
    }

    @Test
    public void whenPostRulesetEndpointIsHit_IOExceptionIsThrown() throws CsvRequiredFieldEmptyException, IOException, RuleValidationException {
        final InputStream inputStream = Files.newInputStream(Path.of(PATH), READ);
        final MultipartFile file = new MockMultipartFile(FILE, PATH, MULTIPART_FORM_DATA.getType(), inputStream);

        final IOException cause = new IOException();
        when(ruleSetService.createRulesetFromCsv(any(), any(MultipartFile.class))).thenThrow(cause);
        when(validationService.validateRulesetName(anyString())).thenCallRealMethod();
        when(modelManager.isModelValidationReady()).thenReturn(Boolean.TRUE);
        assertThatCDException(() -> objectUnderTest.createRuleset(RULESET_NAME, file))
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasMessage("%s \"Failed to create ruleset.\"", INTERNAL_SERVER_ERROR)
                .hasSuppressedException(cause)
                .hasNoDetail()
                .hasTitle(FAILED_TO_CREATE_RULESET);
        verify(ruleSetService).createRulesetFromCsv(eq(RULESET_NAME), eq(file));
    }

    @Test
    public void whenGetRulesetEndpointIsHit_thenCsvIsReturned() throws IOException {
        // mock the ruleService and ruleSetService
        when(ruleSetService.existsById(DEFAULT_UUID)).thenReturn(true);
        when(ruleSetService.createCsvFromRuleset(DEFAULT_UUID))
                .thenReturn(new ByteArrayResource(("""
                        moType,attributeName,attributeValue
                        ENodeBFunction,forcedSiTunnelingActive,true
                        EUtranCellFDD,cellCapMinCellSubCap,1000
                        EUtranCellFDD,cellCapMinMaxWriProt,true
                        EUtranCellFDD,cfraEnable,true
                        """).getBytes()));

        // make the request to the endpoint
        final ResponseEntity<Resource> responseEntity = objectUnderTest.getRuleset(DEFAULT_UUID.toString(), APPLICATION_JSON);

        // check the response
        assertThat(responseEntity).hasStatus(OK);
        final var headers = responseEntity.getHeaders();
        assertThat(headers.getContentType()).isEqualTo(MediaType.parseMediaType(CSV_CONTENT_TYPE));
        assertThat(headers.getContentDisposition().getFilename()).isEqualTo(String.format(CSV_FILENAME_TEMPLATE, DEFAULT_UUID));
        assertThat(responseEntity).hasBody(new ByteArrayResource(("""
                moType,attributeName,attributeValue
                ENodeBFunction,forcedSiTunnelingActive,true
                EUtranCellFDD,cellCapMinCellSubCap,1000
                EUtranCellFDD,cellCapMinMaxWriProt,true
                EUtranCellFDD,cfraEnable,true
                """).getBytes()));

        // verify the interactions with the ruleService and ruleSetService
        verify(ruleSetService).existsById(DEFAULT_UUID);
        verify(ruleSetService).createCsvFromRuleset(DEFAULT_UUID);
        verifyNoMoreInteractions(ruleSetService);
    }

    @Test
    void whenGetRulesetEndpointIsHit_andIOExceptionIsCaught_thenControllerDetailExceptionIsThrown() throws IOException {
        when(ruleSetService.existsById(DEFAULT_UUID)).thenReturn(true);
        final IOException cause = new IOException();
        when(ruleSetService.createCsvFromRuleset(DEFAULT_UUID)).thenThrow(cause);

        assertThatCDException(() -> objectUnderTest.getRuleset(DEFAULT_UUID.toString(), APPLICATION_JSON))
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasSuppressedException(cause)
                .hasMessage("%s \"Error parsing ID. Provided ID is invalid.\"", INTERNAL_SERVER_ERROR)
                .hasDetail("Failed to read InputStream")
                .hasTitle(ERROR_PARSING_ID_PROVIDED_ID_IS_INVALID);

        verify(ruleSetService).createCsvFromRuleset(DEFAULT_UUID);
        verify(ruleSetService).existsById(DEFAULT_UUID);
        verifyNoMoreInteractions(ruleSetService);
    }

    @Test
    void whenGetRulesetEndpointIsHit_andDataAccessExceptionIsCaught_thenControllerDetailExceptionIsThrown() {
        when(ruleSetService.existsById(DEFAULT_UUID)).thenThrow(DataAccessResourceFailureException.class);

        assertThatCDException(() -> objectUnderTest.getRuleset(DEFAULT_UUID.toString(), APPLICATION_JSON))
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasDetail("Database operation failed.")
                .hasReason("Failed to get ruleset.");

        verifyNoMoreInteractions(ruleSetService);
    }

    @Test
    void whenGetRulesetEndpointIsHitWithInvalidID_IllegalArgumentExceptionIsThrown() {
        final IllegalArgumentException cause = new IllegalArgumentException("Invalid UUID string: invalid-uuid");
        assertThatCDException(() -> objectUnderTest.getRuleset(INVALID_UUID, APPLICATION_JSON))
                .hasStatus(BAD_REQUEST)
                .hasMessage("%s \"Error parsing ID. Provided ID is invalid.\"", BAD_REQUEST)
                .hasSuppressedException(cause)
                .hasDetail(PROVIDE_A_VALID_RULESET_ID)
                .hasTitle(ERROR_PARSING_ID_PROVIDED_ID_IS_INVALID);
        assertThatCDException(() -> objectUnderTest.getRuleset(INVALID_UUID, APPLICATION_JSON))
                .hasSuppressedException(cause);
        verifyNoMoreInteractions(ruleSetService);
    }

    @Test
    void whenGetRulesetEndpointIsHit_NotFoundIsReturned() {
        when(ruleSetService.existsById(DEFAULT_UUID)).thenReturn(false);

        assertThatCDException(() -> objectUnderTest.getRuleset(DEFAULT_UUID.toString(), APPLICATION_JSON)).hasStatus(NOT_FOUND)
                .hasStatus(NOT_FOUND)
                .hasTitle("Failed to find ruleset. Ruleset doesn't exist.")
                .hasDetail("Enter a valid Ruleset ID.");

        verify(ruleSetService).existsById(DEFAULT_UUID);
        verifyNoMoreInteractions(ruleSetService);
    }

    @Test
    public void whenCreateRulesetWithNullsInCsv_CsvRequiredFieldEmptyExceptionIsThrown()
            throws CsvRequiredFieldEmptyException, IOException, RuleValidationException {

        final InputStream inputStream = Files.newInputStream(Path.of(PATH), READ);
        final MultipartFile file = new MockMultipartFile(FILE, PATH, MULTIPART_FORM_DATA.getType(), inputStream);
        rulesetName = RULESET_NAME;

        when(ruleSetService.createRulesetFromCsv(any(), any(MultipartFile.class))).thenThrow(CsvRequiredFieldEmptyException.class);
        when(validationService.validateRulesetName(anyString())).thenCallRealMethod();
        when(ruleSetService.existsByName(rulesetName)).thenReturn(false);
        when(modelManager.isModelValidationReady()).thenReturn(Boolean.TRUE);
        assertThatCDException(() -> objectUnderTest.createRuleset(rulesetName, file))
                .hasStatus(BAD_REQUEST)
                .hasMessage(
                        "400 BAD_REQUEST \"File cannot contain missing values and column headers must match the Rule model.\"")
                .hasSuppressedException(new CsvRequiredFieldEmptyException())
                .hasDetail(FILL_IN_ALL_REQUIRED_FIELDS)
                .hasTitle("File cannot contain missing values and column headers must match the Rule model.");

        verify(ruleSetService).createRulesetFromCsv(eq(rulesetName), eq(file));
        verify(ruleSetService).existsByName(rulesetName);
        verifyNoMoreInteractions(ruleSetService);
    }

    @Test
    public void whenCreateRulesetWithEmptyName_BadRequestIsReturned() {
        file = new MockMultipartFile(FILE, PATH, MULTIPART_FORM_DATA.getType(), EMPTY_BYTE_ARRAY);
        rulesetName = "";
        when(validationService.validateRulesetName(anyString())).thenCallRealMethod();
        assertThatCDException(() -> objectUnderTest.createRuleset(rulesetName, file))
                .hasStatus(BAD_REQUEST)
                .hasMessage(STATUS_VALIDATION_FAILED, BAD_REQUEST)
                .hasDetail("Invalid rulesetName: Must not be blank")
                .hasTitle(VALIDATION_FAILED);

        verifyNoInteractions(ruleSetService);
    }

    @Test
    public void whenCreateRuleset_andModelDiscoveryIsNotReady_ServiceUnavailableIsReturned() {
        file = new MockMultipartFile(FILE, PATH, MULTIPART_FORM_DATA.getType(), EMPTY_BYTE_ARRAY);
        rulesetName = "sample_ruleset";

        when(validationService.validateRulesetName(anyString())).thenCallRealMethod();
        when(ruleSetService.existsByName(rulesetName)).thenReturn(false);
        when(modelManager.isModelValidationReady()).thenReturn(Boolean.FALSE);

        assertThatCDException(() -> objectUnderTest.createRuleset(rulesetName, file))
                .hasStatus(SERVICE_UNAVAILABLE)
                .hasDetail("Model Discovery has not yet completed and therefore cannot validate the ruleset.")
                .hasTitle("Failed to create/update ruleset. Service not ready.");

        verifyNoMoreInteractions(ruleSetService);
    }

    @Test
    public void whenCreateRulesetWithSpecialCharactersInName_BadRequestIsReturned() {
        file = new MockMultipartFile(FILE, PATH, MULTIPART_FORM_DATA.getType(), EMPTY_BYTE_ARRAY);
        rulesetName = "sample ruleset#";
        when(validationService.validateRulesetName(anyString())).thenCallRealMethod();

        assertThatCDException(() -> objectUnderTest.createRuleset(rulesetName, file))
                .hasStatus(BAD_REQUEST)
                .hasMessage(STATUS_VALIDATION_FAILED, BAD_REQUEST)
                .hasDetail(REGEX_RULESET_JOBNAME_ERROR)
                .hasTitle(VALIDATION_FAILED);

        verifyNoInteractions(ruleSetService);
    }

    @Test
    public void whenCreateRulesetThrowsRuleValidationException_thenCorrectControllerDetailRuleExceptionIsThrown()
            throws CsvRequiredFieldEmptyException, IOException, RuleValidationException {
        file = new MockMultipartFile(FILE, PATH, MULTIPART_FORM_DATA.getType(), EMPTY_BYTE_ARRAY);
        rulesetName = "sample_ruleset";
        final List<RuleValidationError> ruleValidationErrors = new ArrayList<>();
        ruleValidationErrors.add(new RuleValidationError(5L, "Invalid MO.", "MO not found in Managed Object Model", ""));

        final RuleValidationException ruleValidationException = new RuleValidationException("", ruleValidationErrors);
        when(validationService.validateRulesetName(anyString())).thenCallRealMethod();
        when(ruleSetService.createRulesetFromCsv(rulesetName, file)).thenThrow(ruleValidationException);
        when(modelManager.isModelValidationReady()).thenReturn(Boolean.TRUE);

        final ControllerDetailRuleException controllerDetailRuleException = (ControllerDetailRuleException) catchThrowable(
                () -> objectUnderTest.createRuleset(rulesetName, file));
        assertThat(controllerDetailRuleException)
                .hasStatus(BAD_REQUEST)
                .hasMessage("400 BAD_REQUEST \"Problems found in ruleset.\"")
                .hasDetail("Ruleset cannot contain any invalid MO types, attributes or values.")
                .hasTitle("Problems found in ruleset.");
        assertThat(controllerDetailRuleException.getRuleValidationErrors()).hasSize(1);
        assertThat(controllerDetailRuleException.getRuleValidationErrors()).hasSameElementsAs(ruleValidationErrors);
    }

    @Test
    public void whenCreateRulesetCatchesDataAccessException_thenCorrectControllerDetailExceptionIsThrown() {
        file = new MockMultipartFile(FILE, PATH, MULTIPART_FORM_DATA.getType(), EMPTY_BYTE_ARRAY);
        rulesetName = "sample_ruleset";
        when(validationService.validateRulesetName(anyString())).thenCallRealMethod();
        when(ruleSetService.existsByName(rulesetName)).thenThrow(DataAccessResourceFailureException.class);

        assertThatCDException(() -> objectUnderTest.createRuleset(rulesetName, file))
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasDetail("Database operation failed.")
                .hasReason("Failed to create ruleset.");

        verifyNoMoreInteractions(ruleSetService);
    }

    @Test
    public void whenUpdateRulesetThrowsRuleValidationException_thenCorrectControllerDetailExceptionIsThrown()
            throws CsvRequiredFieldEmptyException, IOException, RuleValidationException {
        file = new MockMultipartFile(FILE, PATH, MULTIPART_FORM_DATA.getType(), EMPTY_BYTE_ARRAY);
        final List<RuleValidationError> ruleValidationErrors = new ArrayList<>();
        ruleValidationErrors.add(new RuleValidationError(5L, "Invalid MO.", "MO not found in Managed Object Model", ""));
        final RuleValidationException ruleValidationException = new RuleValidationException("", ruleValidationErrors);

        when(ruleSetService.existsById(DEFAULT_UUID)).thenReturn(Boolean.TRUE);
        when(ruleSetService.getRulesetNameFromId(DEFAULT_UUID)).thenReturn(DEFAULT_RULESET_NAME);
        when(ruleSetService.updateRulesetFromCsv(DEFAULT_UUID, file)).thenThrow(ruleValidationException);
        when(modelManager.isModelValidationReady()).thenReturn(Boolean.TRUE);

        final ControllerDetailRuleException controllerDetailRuleException = (ControllerDetailRuleException) catchThrowable(
                () -> objectUnderTest.updateRuleset(DEFAULT_UUID.toString(), file, APPLICATION_JSON, APPLICATION_JSON));
        assertThat(controllerDetailRuleException)
                .hasStatus(BAD_REQUEST)
                .hasMessage("400 BAD_REQUEST \"Problems found in ruleset.\"")
                .hasDetail("Ruleset cannot contain any invalid MO types, attributes or values.")
                .hasTitle("Problems found in ruleset.");
        assertThat(controllerDetailRuleException.getRuleValidationErrors()).hasSize(1);
        assertThat(controllerDetailRuleException.getRuleValidationErrors()).hasSameElementsAs(ruleValidationErrors);
    }

    @Test
    public void whenUpdateRulesetCatchesDataAccessException_thenCorrectControllerDetailExceptionIsThrown() {
        file = new MockMultipartFile(FILE, PATH, MULTIPART_FORM_DATA.getType(), EMPTY_BYTE_ARRAY);
        when(ruleSetService.existsById(DEFAULT_UUID)).thenThrow(DataAccessResourceFailureException.class);

        assertThatCDException(() -> objectUnderTest.updateRuleset(DEFAULT_UUID.toString(), file, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasDetail("Database operation failed.")
                .hasReason("Failed to update ruleset.");

        verifyNoMoreInteractions(ruleSetService);
    }

    @Test
    public void whenDeleteRulesetEndpointIsHit_andRulesetIsNotInUseByAJob_thenNoContentIsReturned() {
        rulesetName = "test_ruleset_name";
        when(ruleSetService.getRulesetNameFromId(any(UUID.class))).thenReturn(rulesetName);
        when(jobService.isRuleSetInUse(rulesetName)).thenReturn(false);

        assertThat(objectUnderTest.deleteRuleset(RULESET_ID_EXAMPLE, APPLICATION_JSON)).hasStatus(NO_CONTENT);
    }

    @Test
    public void whenDeleteRulesetEndpointIsHit_andRulesetIsInUseByAJob_thenConflictIsReturned() {
        rulesetName = "test_ruleset_name";
        when(ruleSetService.getRulesetNameFromId(any(UUID.class))).thenReturn(rulesetName);
        when(jobService.isRuleSetInUse(rulesetName)).thenReturn(true);

        assertThatCDException(() -> objectUnderTest.deleteRuleset(RULESET_ID_EXAMPLE, APPLICATION_JSON))
                .hasStatus(CONFLICT)
                .hasReason("Ruleset with provided ID is used in a Job configuration.")
                .hasDetail("Only Rulesets not associated with a Job configuration can be deleted.");
    }

    @Test
    public void whenDeleteRulesetEndpointIsHit_andRulesetDoesNotExist_thenNotFoundIsReturned() {
        when(ruleSetService.getRulesetNameFromId(any(UUID.class))).thenThrow(IllegalArgumentException.class);

        assertThatCDException(() -> objectUnderTest.deleteRuleset(RULESET_ID_EXAMPLE, APPLICATION_JSON))
                .hasStatus(NOT_FOUND)
                .hasTitle("Ruleset with provided ID does not exist.")
                .hasDetail(PROVIDE_A_VALID_RULESET_ID);
    }

    @Test
    public void whenDeleteRulesetEndpointIsHit_andIdProvidedIdIsInvalid_thenInternalServerErrorIsReturned() {
        assertThatCDException(() -> objectUnderTest.deleteRuleset("<script>h4ckingCode</script>", APPLICATION_JSON))
                .hasStatus(BAD_REQUEST)
                .hasTitle(ERROR_PARSING_ID_PROVIDED_ID_IS_INVALID)
                .hasDetail(PROVIDE_A_VALID_RULESET_ID);
    }

    @Test
    public void whenDeleteRulesetEndpointIsHit_andDataAccessExceptionIsCaught_thenCorrectControllerDetailExceptionIsThrown() {
        when(ruleSetService.getRulesetNameFromId(any(UUID.class))).thenThrow(DataAccessResourceFailureException.class);
        assertThatCDException(() -> objectUnderTest.deleteRuleset(DEFAULT_UUID.toString(), APPLICATION_JSON))
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasDetail("Database operation failed.")
                .hasReason("Failed to delete ruleset.");

        verifyNoMoreInteractions(ruleSetService);
    }

    @Test
    public void whenCreateRulesetWithIdenticalName_BadRequestIsReturned() {
        file = new MockMultipartFile(FILE, PATH, MULTIPART_FORM_DATA.getType(), EMPTY_BYTE_ARRAY);
        rulesetName = "sample_ruleset";

        when(validationService.validateRulesetName(anyString())).thenCallRealMethod();
        when(ruleSetService.existsByName(rulesetName)).thenReturn(true);

        assertThatCDException(() -> objectUnderTest.createRuleset(rulesetName, file))
                .hasStatus(BAD_REQUEST)
                .hasTitle("Failed to create ruleset. Ruleset with provided name already exists.")
                .hasDetail("Use a unique Ruleset name.");

        verify(ruleSetService).existsByName(rulesetName);
        verifyNoMoreInteractions(ruleSetService);
    }

    private List<EaccRulesetMetadata> createRulesetMetadadata() {
        final EaccRulesetMetadata metaData = new EaccRulesetMetadata("1", DEFAULT_RULESET_NAME, "sampleUri");
        return List.of(metaData);
    }

}