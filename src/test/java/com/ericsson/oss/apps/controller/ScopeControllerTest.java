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
import static com.ericsson.oss.apps.util.Constants.EMPTY_STRING;
import static com.ericsson.oss.apps.util.Constants.REGEX_SCOPE_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.SCOPES_URI;
import static com.ericsson.oss.apps.util.TestDefaults.FILE;
import static com.ericsson.oss.apps.util.TestDefaults.FILL_IN_ALL_REQUIRED_FIELDS;
import static com.ericsson.oss.apps.util.TestDefaults.REALLY_LONG_ALPHANUMERIC_STRING;
import static com.ericsson.oss.apps.util.TestDefaults.SCOPES_URL;
import static com.ericsson.oss.apps.util.TestDefaults.SCOPE_ATHLONE_ID;
import static com.ericsson.oss.apps.util.TestDefaults.SCOPE_ATHLONE_UUID;
import static com.ericsson.oss.apps.util.TestDefaults.SCOPE_DUBLIN_ID;
import static com.ericsson.oss.apps.util.TestDefaults.SOME_IO_EXCEPTION_MESSAGE;
import static java.nio.file.StandardOpenOption.READ;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import com.ericsson.oss.apps.api.model.EaccScopeMetadata;
import com.ericsson.oss.apps.model.Scope;
import com.ericsson.oss.apps.service.ExecutionService;
import com.ericsson.oss.apps.service.JobService;
import com.ericsson.oss.apps.service.ScopeService;
import com.ericsson.oss.apps.service.ValidationService;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.opencsv.exceptions.CsvValidationException;

/**
 * Unit tests for {@link ScopeController} class.
 */
@ExtendWith(MockitoExtension.class)
class ScopeControllerTest {

    private static final String PATH = "src/test/resources/SampleScopeFile.csv";
    private static final String SCOPE_NAME = "sample_scope";
    private static final String INVALID_SCOPE_NAME = "scope&name";
    private static final String SAMPLE_ID = "298949fe-d222-4191-a192-3eccb9f74cda";
    private static final String VALIDATION_FAILED_TITLE = "Validation failed";
    private static final String VALIDATION_FAILED_MESSAGE = String.format("%s \"%s\"", BAD_REQUEST, VALIDATION_FAILED_TITLE);
    private static final String SCOPE_WITH_PROVIDED_ID_DOES_NOT_EXIST = "Scope with provided ID does not exist.";
    private static final String ERROR_PARSING_ID_PROVIDED_ID_IS_INVALID = "Error parsing ID. Provided ID is invalid.";
    private static final String INVALID_UUID_STRING_INVALID_ID = "Invalid UUID string: Invalid_ID";
    private static final String STATUS_TITLE_FORMAT = "%s \"%s\"";

    @Mock
    private MockMultipartFile file;

    @Mock
    private ScopeService scopeService;

    @Mock
    private JobService jobService;

    @Mock
    private ValidationService validationService;

    @Mock
    private ExecutionService executionService;

    @InjectMocks
    private ScopeController objectUnderTest;

    @BeforeEach
    public void setup() throws IOException {
        final InputStream inputStream = Files.newInputStream(Path.of(PATH), READ);
        file = new MockMultipartFile(FILE, PATH, MediaType.MULTIPART_FORM_DATA.getType(), inputStream);
    }

    @Test
    public void whenPostScopeEndpointWithEmptyScopeName_thenExceptionIsThrown() {
        when(validationService.validateScopeName(anyString())).thenCallRealMethod();
        assertThatCDException(() -> objectUnderTest.createScope(EMPTY_STRING, null))
                .hasStatus(BAD_REQUEST)
                .hasMessage(VALIDATION_FAILED_MESSAGE)
                .hasDetail(REGEX_SCOPE_JOBNAME_ERROR)
                .hasTitle(VALIDATION_FAILED_TITLE);
    }

    @Test
    public void whenPostScopeEndpointWithUppercaseScopeName_thenExceptionIsThrown() {
        when(validationService.validateScopeName(anyString())).thenCallRealMethod();
        assertThatCDException(() -> objectUnderTest.createScope(SCOPE_NAME.toUpperCase(Locale.ROOT), null))
                .hasStatus(BAD_REQUEST)
                .hasMessage(VALIDATION_FAILED_MESSAGE)
                .hasDetail(REGEX_SCOPE_JOBNAME_ERROR)
                .hasTitle(VALIDATION_FAILED_TITLE);
    }

    @Test
    public void whenGetAllScopesEndpointIsHit_thenOkIsReturned_andScopesAreReturnedInTheBody() {
        when(scopeService.getAllScopeMetadata()).thenReturn(createScopeMetadataList());
        assertThat(objectUnderTest.listScopes())
                .hasStatus(OK)
                .hasBody(createScopeMetadataList());
    }

    @Test
    public void whenGetAllScopesEndpointIsHit_andDataAccessAcceptionIsCaught_theControllerDetailExceptionIsThrown() {
        when(scopeService.getAllScopeMetadata()).thenThrow(DataAccessResourceFailureException.class);

        assertThatCDException(() -> objectUnderTest.listScopes())
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasReason("Failed to get scopes.")
                .hasDetail((DATABASE_OPERATION_FAILED));
    }

    @Test
    public void whenGetScopeEndpointIsHit_thenCsvIsReturned() throws IOException {
        when(scopeService.getCsvForScope(SCOPE_ATHLONE_UUID))
                .thenReturn(new ByteArrayResource(("""
                        fdn
                        ManagedElement=TestFdn0
                        ManagedElement=TestFdn1
                        ManagedElement=TestFdn2
                        """).getBytes()));

        final ResponseEntity<Resource> responseEntity = objectUnderTest.getScope(SCOPE_ATHLONE_ID, APPLICATION_JSON);

        assertThat(responseEntity).hasStatus(OK);
        final var headers = responseEntity.getHeaders();
        assertThat(headers.getContentType()).isEqualTo(MediaType.parseMediaType(CSV_CONTENT_TYPE));
        assertThat(headers.getContentDisposition().getFilename()).isEqualTo(String.format(CSV_FILENAME_TEMPLATE, SCOPE_ATHLONE_ID));
        assertThat(responseEntity).hasBody(new ByteArrayResource(("""
                fdn
                ManagedElement=TestFdn0
                ManagedElement=TestFdn1
                ManagedElement=TestFdn2
                """).getBytes()));

        verify(scopeService).getCsvForScope(SCOPE_ATHLONE_UUID);
        verifyNoMoreInteractions(scopeService);
    }

    @Test
    void whenGetScopeEndpointIsHit_andIOExceptionIsCaught_thenControllerDetailExceptionIsThrown() throws IOException {
        final IOException cause = new IOException();
        final UUID scopeAthloneUUID = UUID.fromString(SCOPE_ATHLONE_ID);
        when(scopeService.getCsvForScope(scopeAthloneUUID)).thenThrow(cause);

        assertThatCDException(() -> objectUnderTest.getScope(SCOPE_ATHLONE_ID, APPLICATION_JSON))
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasMessage("%s \"Error creating csv file.\"", INTERNAL_SERVER_ERROR)
                .hasSuppressedException(cause)
                .hasDetail("Failed to read InputStream.");

        verify(scopeService).getCsvForScope(scopeAthloneUUID);
        verifyNoMoreInteractions(scopeService);
    }

    @Test
    void whenGetScopeEndpointIsHit_andIllegalArgumentExceptionIsCaught_thenControllerDetailExceptionIsThrown() throws IOException {
        final IllegalArgumentException cause = new IllegalArgumentException();

        when(scopeService.getCsvForScope(SCOPE_ATHLONE_UUID)).thenThrow(cause);

        assertThatCDException(() -> objectUnderTest.getScope(SCOPE_ATHLONE_ID, APPLICATION_JSON))
                .hasStatus(NOT_FOUND)
                .hasMessage(STATUS_TITLE_FORMAT, NOT_FOUND, SCOPE_WITH_PROVIDED_ID_DOES_NOT_EXIST)
                .hasSuppressedException(cause)
                .hasDetail("Provide a valid Scope ID.")
                .hasTitle(SCOPE_WITH_PROVIDED_ID_DOES_NOT_EXIST);

        verify(scopeService).getCsvForScope(SCOPE_ATHLONE_UUID);
        verifyNoMoreInteractions(scopeService);
    }

    @Test
    void whenGetScopeEndpointIsHit_andDataAccessExceptionIsCaught_thenControllerDetailExceptionIsThrown() throws IOException {
        final DataAccessResourceFailureException cause = new DataAccessResourceFailureException("Some data access failure");

        when(scopeService.getCsvForScope(SCOPE_ATHLONE_UUID)).thenThrow(cause);

        assertThatCDException(() -> objectUnderTest.getScope(SCOPE_ATHLONE_ID, APPLICATION_JSON))
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasSuppressedException(cause)
                .hasDetail("Database operation failed.")
                .hasReason("Failed to get scope.");

        verify(scopeService).getCsvForScope(SCOPE_ATHLONE_UUID);
        verifyNoMoreInteractions(scopeService);
    }

    @Test
    public void whenExceptionIsThrownWhenReadingScopes_thenResponseStatusExceptionIsThrown() {
        final RuntimeException cause = new RuntimeException();
        when(scopeService.getAllScopeMetadata()).thenThrow(cause);
        assertThatCDException(() -> objectUnderTest.listScopes())
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasMessage("%s \"Failed to get all Scope Metadata.\"", HttpStatus.INTERNAL_SERVER_ERROR)
                .hasSuppressedException(cause)
                .hasNoDetail()
                .hasTitle("Failed to get all Scope Metadata.");
    }

    @Test
    public void whenCreateScopeCalledWithValidData_thenStatusIsCreatedAndScopeMetadataReturned()
            throws CsvRequiredFieldEmptyException, CsvValidationException, IOException {
        final Scope scope = createScope();
        when(validationService.validateScopeName(anyString())).thenCallRealMethod();
        when(scopeService.createScopeFromCsv(SCOPE_NAME, file)).thenReturn(scope);
        assertThat(objectUnderTest.createScope(SCOPE_NAME, file))
                .hasStatus(CREATED)
                .hasBody(createEaccScopeMetadata(scope));
    }

    @Test
    public void whenCreateScopeCalledWithInvalidScopeName_thenBadRequestReturned() {
        when(validationService.validateScopeName(anyString())).thenCallRealMethod();
        assertThatCDException(() -> objectUnderTest.createScope(INVALID_SCOPE_NAME, null))
                .hasStatus(BAD_REQUEST)
                .hasMessage(VALIDATION_FAILED_MESSAGE)
                .hasDetail(REGEX_SCOPE_JOBNAME_ERROR)
                .hasDetail("Invalid scopeName: Use only lower case alphanumeric, '-' and '_' characters up to a maximum of 255.")
                .hasTitle(VALIDATION_FAILED_TITLE);
    }

    @Test
    public void whenCreateScopeCalledWithTooLongScopeName_thenBadRequestReturned() {
        when(validationService.validateScopeName(anyString())).thenCallRealMethod();
        assertThatCDException(() -> objectUnderTest.createScope(REALLY_LONG_ALPHANUMERIC_STRING, null))
                .hasStatus(BAD_REQUEST)
                .hasMessage(VALIDATION_FAILED_MESSAGE)
                .hasDetail(REGEX_SCOPE_JOBNAME_ERROR);
    }

    @Test
    public void whenCreateScopeCalledWithDuplicate_thenBadRequestReturned() {
        when(validationService.validateScopeName(anyString())).thenCallRealMethod();
        when(scopeService.existsByName(SCOPE_NAME)).thenReturn(true);
        assertThatCDException(() -> objectUnderTest.createScope(SCOPE_NAME, file))
                .hasStatus(BAD_REQUEST)
                .hasMessage("%s \"Failed to create scope. Scope with provided name already exists.\"", BAD_REQUEST)
                .hasTitle("Failed to create scope. Scope with provided name already exists.")
                .hasDetail("Use a unique scope name.");
    }

    @Test
    public void whenCreateScopeCalledWithInvalidCsv_thenBadRequestReturned()
            throws CsvRequiredFieldEmptyException, CsvValidationException, IOException {
        when(validationService.validateScopeName(anyString())).thenCallRealMethod();
        when(scopeService.existsByName(SCOPE_NAME)).thenReturn(false);
        final CsvRequiredFieldEmptyException cause = new CsvRequiredFieldEmptyException("Some CSV exception message");
        when(scopeService.createScopeFromCsv(SCOPE_NAME, file)).thenThrow(cause);

        assertThatCDException(() -> objectUnderTest.createScope(SCOPE_NAME, file))
                .hasStatus(BAD_REQUEST)
                .hasMessage("%s \"File does not match the csv format.\"", BAD_REQUEST)
                .hasSuppressedException(cause)
                .hasTitle("File does not match the csv format.")
                .hasDetail(FILL_IN_ALL_REQUIRED_FIELDS);
    }

    @Test
    public void whenCreateScopeCalledAndIOExceptionOccurs_thenInternalErrorReturned()
            throws CsvRequiredFieldEmptyException, CsvValidationException, IOException {
        when(validationService.validateScopeName(anyString())).thenCallRealMethod();
        when(scopeService.existsByName(SCOPE_NAME)).thenReturn(false);
        final IOException cause = new IOException(SOME_IO_EXCEPTION_MESSAGE);
        when(scopeService.createScopeFromCsv(SCOPE_NAME, file)).thenThrow(cause);

        assertThatCDException(() -> objectUnderTest.createScope(SCOPE_NAME, file))
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasMessage("%s \"Failed to create scope.\"", INTERNAL_SERVER_ERROR)
                .hasSuppressedException(cause)
                .hasNoDetail()
                .hasTitle("Failed to create scope.");
    }

    @Test
    public void whenCreateScopeCalledAndPersistenceExceptionOccurs_thenInternalErrorReturned()
            throws CsvRequiredFieldEmptyException, CsvValidationException, IOException {
        when(validationService.validateScopeName(anyString())).thenCallRealMethod();
        when(scopeService.existsByName(SCOPE_NAME)).thenReturn(false);
        final DataAccessResourceFailureException cause = new DataAccessResourceFailureException("Some data access failure");
        when(scopeService.createScopeFromCsv(SCOPE_NAME, file)).thenThrow(cause);

        assertThatCDException(() -> objectUnderTest.createScope(SCOPE_NAME, file))
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasMessage("%s \"Failed to create scope.\"", INTERNAL_SERVER_ERROR, cause)
                .hasSuppressedException(cause)
                .hasDetail(DATABASE_OPERATION_FAILED)
                .hasTitle("Failed to create scope.");
    }

    @Test
    public void whenPutScopeEndpointIsHit_thenOkIsReturned() throws CsvRequiredFieldEmptyException, CsvValidationException, IOException {
        final EaccScopeMetadata eaccScopeMetadata = new EaccScopeMetadata();
        when(scopeService.getScopeFromUUID(any(UUID.class))).thenReturn(createScope());
        when(scopeService.updateScope(createScope(), file)).thenReturn(eaccScopeMetadata);
        when(executionService.isScopeInUse(SCOPE_NAME)).thenReturn(false);
        final ResponseEntity<EaccScopeMetadata> responseEntity = objectUnderTest.updateScope(SAMPLE_ID, file, APPLICATION_JSON, APPLICATION_JSON);
        assertThat(responseEntity).hasStatus(OK)
                .hasBody(eaccScopeMetadata);
    }

    @Test
    public void whenPutScopeEndpointIsHit_andScopeDoesNotExist_thenNotFoundIsReturned() {
        final IllegalArgumentException cause = new IllegalArgumentException();
        when(scopeService.getScopeFromUUID(any(UUID.class))).thenThrow(cause);
        assertThatCDException(() -> objectUnderTest.updateScope(SAMPLE_ID, file, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(NOT_FOUND)
                .hasMessage(STATUS_TITLE_FORMAT, NOT_FOUND, SCOPE_WITH_PROVIDED_ID_DOES_NOT_EXIST)
                .hasSuppressedException(cause)
                .hasTitle("Scope with provided ID does not exist.")
                .hasDetail("Provide a valid Scope ID.");
    }

    @Test
    public void whenPutScopeEndpointIsHit_andIdProvidedIsInvalid_thenInternalServerErrorIsReturned() {
        final IllegalArgumentException cause = new IllegalArgumentException(INVALID_UUID_STRING_INVALID_ID);
        assertThatCDException(() -> objectUnderTest.updateScope("Invalid_ID", file, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(BAD_REQUEST)
                .hasMessage(STATUS_TITLE_FORMAT, BAD_REQUEST, ERROR_PARSING_ID_PROVIDED_ID_IS_INVALID)
                .hasSuppressedException(cause)
                .hasTitle("Error parsing ID. Provided ID is invalid.")
                .hasDetail("Provide a valid ID.");
    }

    @Test
    public void whenPutScopeEndpointIsHit_andScopeIsInUseByAJob_thenInternalServerErrorIsReturned() {
        when(scopeService.getScopeFromUUID(any(UUID.class))).thenReturn(createScope());
        when(executionService.isScopeInUse(SCOPE_NAME)).thenReturn(true);
        assertThatCDException(() -> objectUnderTest.updateScope(SAMPLE_ID, file, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(CONFLICT)
                .hasMessage(STATUS_TITLE_FORMAT, CONFLICT, "Cannot update scope as it is used by an ongoing execution.")
                .hasTitle("Cannot update scope as it is used by an ongoing execution.")
                .hasDetail("Scope in use.");
    }

    @Test
    public void whenPutScopeCalledWithInvalidCsv_thenBadRequestReturned()
            throws CsvRequiredFieldEmptyException, CsvValidationException, IOException {
        when(scopeService.getScopeFromUUID(any(UUID.class))).thenReturn(createScope());
        final CsvRequiredFieldEmptyException cause = new CsvRequiredFieldEmptyException("Some CSV exception message");
        when(scopeService.updateScope(createScope(), file)).thenThrow(cause);

        assertThatCDException(() -> objectUnderTest.updateScope(SAMPLE_ID, file, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(BAD_REQUEST)
                .hasMessage("%s \"File does not match the csv format.\"", BAD_REQUEST)
                .hasSuppressedException(cause)
                .hasTitle("File does not match the csv format.")
                .hasDetail(FILL_IN_ALL_REQUIRED_FIELDS);
    }

    @Test
    public void whenPutScopeCalled_andIOExceptionOccurs_thenInternalErrorReturned()
            throws CsvRequiredFieldEmptyException, CsvValidationException, IOException {
        when(scopeService.getScopeFromUUID(any(UUID.class))).thenReturn(createScope());
        final IOException cause = new IOException(SOME_IO_EXCEPTION_MESSAGE);
        when(scopeService.updateScope(createScope(), file)).thenThrow(cause);

        assertThatCDException(() -> objectUnderTest.updateScope(SAMPLE_ID, file, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasMessage("%s \"Error updating from csv file.\"", INTERNAL_SERVER_ERROR)
                .hasSuppressedException(cause)
                .hasTitle("Error updating from csv file.")
                .hasDetail("Failed to update the scope from the file.");
    }

    @Test
    void whenPutScopeCalled_andDataAccessExceptionIsCaught_thenControllerDetailExceptionIsThrown()
            throws IOException, CsvRequiredFieldEmptyException, CsvValidationException {

        when(scopeService.getScopeFromUUID(any(UUID.class))).thenReturn(createScope());
        final DataAccessResourceFailureException cause = new DataAccessResourceFailureException("Some data access failure");
        when(scopeService.updateScope(createScope(), file)).thenThrow(cause);

        assertThatCDException(() -> objectUnderTest.updateScope(SAMPLE_ID, file, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasSuppressedException(cause)
                .hasDetail("Database operation failed.")
                .hasReason("Failed to update scope.");
    }

    @Test
    public void whenDeleteScopeEndpointIsHit_andScopeIsInUseByAJob_thenInternalServerErrorIsReturned() {
        when(scopeService.getScopeFromUUID(any(UUID.class))).thenReturn(createScope());
        when(jobService.isScopeInUse(SCOPE_NAME)).thenReturn(true);
        assertThatCDException(() -> objectUnderTest.deleteScope(SAMPLE_ID, APPLICATION_JSON))
                .hasStatus(CONFLICT)
                .hasMessage(STATUS_TITLE_FORMAT, CONFLICT, "Scope with provided ID is used in a Job configuration.")
                .hasTitle("Scope with provided ID is used in a Job configuration.")
                .hasDetail("Only Scopes not associated with a Job configuration can be deleted.");
    }

    @Test
    public void whenDeleteScopeEndpointIsHit_andScopeDoesNotExist_thenNotFoundIsReturned() {
        final IllegalArgumentException cause = new IllegalArgumentException();
        when(scopeService.getScopeFromUUID(any(UUID.class))).thenThrow(cause);
        assertThatCDException(() -> objectUnderTest.deleteScope(SAMPLE_ID, APPLICATION_JSON))
                .hasStatus(NOT_FOUND)
                .hasMessage(STATUS_TITLE_FORMAT, NOT_FOUND, SCOPE_WITH_PROVIDED_ID_DOES_NOT_EXIST)
                .hasSuppressedException(cause)
                .hasTitle("Scope with provided ID does not exist.")
                .hasDetail("Provide a valid Scope ID.");
    }

    @Test
    public void whenDeleteScopeEndpointIsHit_andIdProvidedIsInvalid_thenInternalServerErrorIsReturned() {
        final IllegalArgumentException cause = new IllegalArgumentException(INVALID_UUID_STRING_INVALID_ID);
        assertThatCDException(() -> objectUnderTest.deleteScope("Invalid_ID", APPLICATION_JSON))
                .hasStatus(BAD_REQUEST)
                .hasMessage(STATUS_TITLE_FORMAT, BAD_REQUEST, ERROR_PARSING_ID_PROVIDED_ID_IS_INVALID)
                .hasSuppressedException(cause)
                .hasTitle("Error parsing ID. Provided ID is invalid.")
                .hasDetail("Provide a valid ID.");
    }

    @Test
    public void whenDeleteScopeEndpointIsHit_andDataAccessExceptionIsCaught_thenControllerDetailExceptionIsThrown() {
        final DataAccessResourceFailureException cause = new DataAccessResourceFailureException("Some data access failure");
        when(scopeService.getScopeFromUUID(any(UUID.class))).thenThrow(cause);
        assertThatCDException(() -> objectUnderTest.deleteScope(SAMPLE_ID, APPLICATION_JSON))
                .hasStatus(INTERNAL_SERVER_ERROR)
                .hasSuppressedException(cause)
                .hasDetail("Database operation failed.")
                .hasReason("Failed to delete scope.");
    }

    private Scope createScope() {
        final Scope scope = new Scope();
        scope.setName(SCOPE_NAME);
        scope.setId(UUID.fromString(SAMPLE_ID));
        return scope;
    }

    private EaccScopeMetadata createEaccScopeMetadata(final Scope scope) {
        return new EaccScopeMetadata(scope.getId().toString(), scope.getName(), SCOPES_URI + scope.getId());
    }

    private List<EaccScopeMetadata> createScopeMetadataList() {
        final EaccScopeMetadata metaData = new EaccScopeMetadata(SCOPE_DUBLIN_ID, SCOPE_NAME, SCOPES_URL + SCOPE_NAME);
        return List.of(metaData);
    }

}