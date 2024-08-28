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

import static com.ericsson.oss.apps.util.Constants.CSV_CONTENT_TYPE;
import static com.ericsson.oss.apps.util.Constants.EMPTY_STRING;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_SCOPE_NAME;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_UUID;
import static com.ericsson.oss.apps.util.TestDefaults.INJECTED_ERROR_MESSAGE_TO_VALIDATE_TEST;
import static com.ericsson.oss.apps.util.TestDefaults.INJECTED_LOG_TO_VALIDATE_TEST;
import static com.ericsson.oss.apps.util.TestDefaults.SCOPES_URL;
import static com.ericsson.oss.apps.util.TestDefaults.SLASH;
import static com.ericsson.oss.apps.util.TestDefaults.SOME_IO_EXCEPTION_MESSAGE;
import static com.ericsson.oss.apps.util.TestDefaults.TEST_CSV_NAME;
import static com.ericsson.oss.apps.util.TestDefaults.TEST_SETUP_ERROR_UNABLE_TO_ACCESS_LOGS;
import static java.nio.file.StandardOpenOption.READ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.ericsson.oss.apps.api.model.EaccScopeMetadata;
import com.ericsson.oss.apps.model.Scope;
import com.ericsson.oss.apps.repository.ScopeRepository;
import com.ericsson.oss.apps.util.InMemoryLogAppender;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.opencsv.exceptions.CsvValidationException;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import jakarta.ws.rs.core.MediaType;

/**
 * Unit tests for {@link ScopeService} class.
 */
@ActiveProfiles("test")
@AutoConfigureObservability
@ExtendWith({ MockitoExtension.class, SoftAssertionsExtension.class })
@SpringBootTest(classes = { CoreApplication.class, CoreApplicationTest.class })
public class ScopeServiceTest {

    private static final String ID = "298949fe-d222-4191-a192-3eccb9f74cda";
    private static final String SCOPE_NAME = "ScopeName";
    private static final String PATH = "src/test/resources/";
    private static final String VALID_SCOPE_FILE = "SampleScopeFile.csv";
    private static final String MISSING_HEADER_SCOPE_FILE = "MissingHeaderScopeFile.csv";
    private static final String EMPTY_LINE_SCOPE_FILE_CSV = "EmptyLineScopeFile.csv";
    private static final String EXTRA_VALUES_SCOPE_FILE = "ExtraValuesScopeFile.csv";
    private static final String TOO_LONG_FDN_VALUES_SCOPE_FILE = "TooLongFdnValuesScopeFile.csv";
    private static final UUID UUID_VALUE = UUID.fromString(ID);

    @MockBean
    private ScopeRepository scopeRepository;

    @MockBean
    private MockMultipartFile mockMultipartFile;

    @Value("${database.retry.userInitiated.maxAttempts}")
    private int maxAttempts;

    @Autowired
    private ScopeService objectUnderTest;

    @InjectSoftAssertions
    private SoftAssertions softly;

    private final MockMultipartFile file = new MockMultipartFile(
            TEST_CSV_NAME,
            TEST_CSV_NAME,
            CSV_CONTENT_TYPE,
            "fdn\nmyManagedElementFdn".getBytes());

    private static InMemoryLogAppender logAppender;

    @BeforeEach
    public void setUp() {
        final Logger logUnderTest = (Logger) LoggerFactory.getLogger(ScopeService.class);
        logAppender = new InMemoryLogAppender();
        logAppender.start();
        logUnderTest.addAppender(logAppender);

        logUnderTest.error(INJECTED_LOG_TO_VALIDATE_TEST);
        logUnderTest.error(EMPTY_STRING, new IllegalArgumentException(INJECTED_ERROR_MESSAGE_TO_VALIDATE_TEST));
    }

    @AfterEach
    public void checkLogsAreAccessible() {
        assertThat(logAppender.getLoggedEvents()).asString()
                .as(TEST_SETUP_ERROR_UNABLE_TO_ACCESS_LOGS)
                .contains(INJECTED_LOG_TO_VALIDATE_TEST);
    }

    @Test
    public void whenGetAllScopesMetadataIsCalled_thenAllPersistedScopesAreReturned() {
        final Scope scope = new Scope();
        scope.setName(DEFAULT_SCOPE_NAME);
        when(scopeRepository.findAll()).thenReturn(List.of(scope));

        final List<EaccScopeMetadata> allScopes = objectUnderTest.getAllScopeMetadata();

        assertThat(allScopes).hasSize(1);
        final var eaccScopeMetadata = allScopes.get(0);
        softly.assertThat(eaccScopeMetadata.getScopeName()).isEqualTo(scope.getName());
        softly.assertThat(eaccScopeMetadata.getId()).isEqualTo(String.valueOf(scope.getId()));
        softly.assertThat(eaccScopeMetadata.getUri()).isEqualTo(SCOPES_URL.substring(1) + SLASH + scope.getId());
    }

    @Test
    void whenCreateScopeCalledWithValidData_thenScopeIsPersisted() throws CsvRequiredFieldEmptyException, CsvValidationException, IOException {
        final InputStream inputStream = Files.newInputStream(Path.of(PATH, VALID_SCOPE_FILE), READ);
        final MultipartFile file = new MockMultipartFile(SCOPE_NAME, VALID_SCOPE_FILE, MediaType.MULTIPART_FORM_DATA, inputStream);

        final Scope scope = objectUnderTest.createScopeFromCsv(SCOPE_NAME, file);

        softly.check(() -> verify(scopeRepository).save(scope));
        softly.assertThat(scope.getName()).isEqualTo(SCOPE_NAME);
        softly.assertThat(scope.getFdns())
                .containsExactly(
                        "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030",
                        "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00031,ManagedElement=NR03gNodeBRadio00031",
                        "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00032,ManagedElement=NR03gNodeBRadio00032");
    }

    @Test
    void whenCreateScopeCalledWithValidDataButEmptyLines_thenScopeIsPersisted()
            throws CsvRequiredFieldEmptyException, CsvValidationException, IOException {
        final InputStream inputStream = Files.newInputStream(Path.of(PATH, EMPTY_LINE_SCOPE_FILE_CSV), READ);
        final MultipartFile file = new MockMultipartFile(SCOPE_NAME, EMPTY_LINE_SCOPE_FILE_CSV, MediaType.MULTIPART_FORM_DATA, inputStream);

        final Scope scope = objectUnderTest.createScopeFromCsv(SCOPE_NAME, file);

        softly.check(() -> verify(scopeRepository).save(scope));
        softly.assertThat(scope.getName()).isEqualTo(SCOPE_NAME);
        softly.assertThat(scope.getFdns())
                .containsExactly(
                        "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030",
                        "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00031,ManagedElement=NR03gNodeBRadio00031",
                        "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00032,ManagedElement=NR03gNodeBRadio00032");
    }

    @Test
    void whenCreateScopeCalledWithMissingHeader_thenCsvRequiredFieldEmptyExceptionThrown_andInvalidTextIsNotLogged() throws IOException {
        final InputStream inputStream = Files.newInputStream(Path.of(PATH, MISSING_HEADER_SCOPE_FILE));
        final MultipartFile file = new MockMultipartFile(SCOPE_NAME, MISSING_HEADER_SCOPE_FILE, MediaType.MULTIPART_FORM_DATA, inputStream);

        assertThatThrownBy(() -> objectUnderTest.createScopeFromCsv(SCOPE_NAME, file))
                .isInstanceOf(CsvRequiredFieldEmptyException.class)
                .hasMessage("File column headers/values must match the csv file format.");

        assertInvalidTextHasNotBeenLogged(file);
    }

    @Test
    void whenCreateScopeCalledWithExtraValues_thenCsvRequiredFieldEmptyExceptionThrown_andInvalidTextIsNotLogged() throws IOException {
        final InputStream inputStream = Files.newInputStream(Path.of(PATH, EXTRA_VALUES_SCOPE_FILE), READ);
        final MultipartFile file = new MockMultipartFile(SCOPE_NAME, EXTRA_VALUES_SCOPE_FILE, MediaType.MULTIPART_FORM_DATA, inputStream);

        assertThatThrownBy(() -> objectUnderTest.createScopeFromCsv(SCOPE_NAME, file))
                .isInstanceOf(CsvRequiredFieldEmptyException.class)
                .hasMessage("File column headers/values must match the csv file format.");

        assertInvalidTextHasNotBeenLogged(file);
    }

    @Test
    void whenCreateScopeCalledTooLongFDNValues_thenCsvRequiredFieldEmptyExceptionThrown() throws IOException {
        final InputStream inputStream = Files.newInputStream(Path.of(PATH, TOO_LONG_FDN_VALUES_SCOPE_FILE), READ);
        final MultipartFile file = new MockMultipartFile(SCOPE_NAME, TOO_LONG_FDN_VALUES_SCOPE_FILE, MediaType.MULTIPART_FORM_DATA, inputStream);

        assertThatThrownBy(() -> objectUnderTest.createScopeFromCsv(SCOPE_NAME, file))
                .isInstanceOf(CsvValidationException.class)
                .hasMessageStartingWith(
                        "FDN value is too long at more than 1600 characters: SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00031,ManagedElement=NR03gNodeBRadio00031000000000000000000");
        assertInvalidTextHasNotBeenLogged("00000000000000000000000000000");
    }

    @Test
    void whenCreateScopeCalledWithNonExistingFile_thenIOExceptionThrown_andInvalidTextIsNotLogged() throws IOException {
        when(mockMultipartFile.getInputStream()).thenThrow(new IOException(SOME_IO_EXCEPTION_MESSAGE));
        final var fileName = "invalidFile";
        when(mockMultipartFile.getName()).thenReturn(fileName);

        assertThatThrownBy(() -> objectUnderTest.createScopeFromCsv(SCOPE_NAME, mockMultipartFile))
                .isInstanceOf(IOException.class)
                .hasMessage(SOME_IO_EXCEPTION_MESSAGE);
        assertInvalidTextHasNotBeenLogged(mockMultipartFile.getName());
    }

    @Test
    void whenExistsByNameCalled_thenReturnsStatusFromRepositoryIsReturned() {
        when(scopeRepository.existsByName(SCOPE_NAME)).thenReturn(true);
        assertThat(objectUnderTest.existsByName(SCOPE_NAME)).isTrue();
        verify(scopeRepository).existsByName(SCOPE_NAME);
    }

    @Test
    public void whenUpdateScopeIsRequested_ThenAnEaccScopeMetadataIsReturned()
            throws CsvRequiredFieldEmptyException, CsvValidationException, IOException {
        final Scope scope = createScope();

        final EaccScopeMetadata eaccScopeMetadata = objectUnderTest.updateScope(scope, file);

        softly.check(() -> verify(scopeRepository).save(scope));
        softly.assertThat(eaccScopeMetadata.getScopeName()).isEqualTo(SCOPE_NAME);
        softly.assertThat(eaccScopeMetadata.getId()).isEqualTo(ID);
        softly.assertThat(eaccScopeMetadata.getUri()).isEqualTo(SCOPES_URL.substring(1) + SLASH + ID);
    }

    @Test
    public void whenUpdateScopeIsRequested_andEmptyFile_thenAIOExceptionIsThrown_andInvalidTextIsNotLogged() throws IOException {
        when(mockMultipartFile.getInputStream()).thenThrow(new IOException(SOME_IO_EXCEPTION_MESSAGE));
        final var scope = createScope();

        assertThatThrownBy(() -> objectUnderTest.updateScope(scope, mockMultipartFile))
                .isInstanceOf(IOException.class)
                .hasMessage(SOME_IO_EXCEPTION_MESSAGE);
        assertInvalidTextHasNotBeenLogged(scope.getId().toString());
    }

    @Test
    public void whenUpdateScopeIsRequested_andInvalidFile_thenCsvExceptionIsThrown_andInvalidTextIsNotLogged() throws IOException {
        final InputStream inputStream = Files.newInputStream(Path.of(PATH, MISSING_HEADER_SCOPE_FILE), READ);
        final MultipartFile file = new MockMultipartFile(SCOPE_NAME, MISSING_HEADER_SCOPE_FILE, MediaType.MULTIPART_FORM_DATA, inputStream);
        final var scope = createScope();

        assertThatThrownBy(() -> objectUnderTest.updateScope(scope, file))
                .isInstanceOf(CsvRequiredFieldEmptyException.class)
                .hasMessage("File column headers/values must match the csv file format.");
        assertInvalidTextHasNotBeenLogged(scope.getId().toString());
    }

    @Test
    public void whenGetScopeNameIsCalled_andTheScopeExists_thenTheScopeNameIsReturned() {
        final Scope scope = new Scope();
        scope.setName(DEFAULT_SCOPE_NAME);
        final Optional<Scope> scopeOptional = Optional.of(scope);
        when(scopeRepository.findById(any(UUID.class))).thenReturn(scopeOptional);

        assertThat(objectUnderTest.getScopeFromUUID(DEFAULT_UUID)).isEqualTo(scope);
    }

    @Test
    public void whenGetScopeNameIsCalled_andTheScopeDoesNotExist_anIllegalArgumentExceptionIsThrown_andInvalidTextIsNotLogged() {
        when(scopeRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> objectUnderTest.getScopeFromUUID(DEFAULT_UUID)).isInstanceOf(IllegalArgumentException.class);
        assertInvalidTextHasNotBeenLogged(DEFAULT_UUID.toString());
    }

    @Test
    public void whenDeleteScopeIsCalled_andTheScopeExists_thenTheScopeIsDeleted() {
        final Optional<Scope> scopeOptional = Optional.of(createScope());
        when(scopeRepository.findById(any(UUID.class))).thenReturn(scopeOptional);

        objectUnderTest.deleteScope(UUID_VALUE);
        verify(scopeRepository, times(1)).deleteById(UUID_VALUE);
    }

    @Test
    public void whenDeleteScopeIsCalled_andTheScopeDoesNotExist_anIllegalArgumentExceptionIsThrown_andInvalidTextIsNotLogged() {
        when(scopeRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> objectUnderTest.deleteScope(UUID_VALUE)).isInstanceOf(IllegalArgumentException.class);
        assertInvalidTextHasNotBeenLogged(UUID_VALUE.toString());
    }

    @Test
    public void whenGetCsvForScopeIsRequested_andTheScopeExists_thenTheScopeIsRead() throws IOException {
        final Optional<Scope> scopeOptional = Optional.of(createScope());
        when(scopeRepository.findById(any(UUID.class))).thenReturn(scopeOptional);

        final ByteArrayResource byteArrayResource = objectUnderTest.getCsvForScope(UUID_VALUE);

        assertThat(new String(byteArrayResource.getByteArray(), StandardCharsets.UTF_8)).isEqualTo("""
                fdn
                "ManagedElement=TestFdn0"
                "ManagedElement=TestFdn1"
                "ManagedElement=TestFdn2"
                """);
    }

    @Test
    public void whenGetCsvForScopeIsRequested_andTheScopeDoesNotExist_anIllegalArgumentExceptionIsThrown_andInvalidTextIsNotLogged() {
        when(scopeRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> objectUnderTest.deleteScope(UUID_VALUE)).isInstanceOf(IllegalArgumentException.class);
        assertInvalidTextHasNotBeenLogged(UUID_VALUE.toString());
    }

    @Test
    public void whenGetAllScopeMetadataMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(scopeRepository.findAll()).thenThrow(CannotCreateTransactionException.class);
        assertThatThrownBy(() -> objectUnderTest.getAllScopeMetadata()).isInstanceOf(TransactionException.class);
        verify(scopeRepository, times(maxAttempts)).findAll();
    }

    @Test
    public void whenUpdateScopeMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(scopeRepository.save(any())).thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.updateScope(new Scope(), file)).isInstanceOf(DataAccessException.class);
        verify(scopeRepository, times(maxAttempts)).save(any());
    }

    @Test
    public void whenDeleteScopeMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(scopeRepository.findById(any())).thenThrow(CannotCreateTransactionException.class).thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.deleteScope(UUID_VALUE)).isInstanceOf(DataAccessException.class);
        verify(scopeRepository, times(maxAttempts)).findById(any());
    }

    @Test
    public void whenGetScopeFromUUIDMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(scopeRepository.findById(any())).thenThrow(CannotCreateTransactionException.class).thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.getScopeFromUUID(UUID_VALUE)).isInstanceOf(DataAccessException.class);
        verify(scopeRepository, times(maxAttempts)).findById(any());
    }

    @Test
    public void whenGetCsvForScopeMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(scopeRepository.findById(any())).thenThrow(CannotCreateTransactionException.class).thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.getCsvForScope(UUID_VALUE)).isInstanceOf(DataAccessException.class);
        verify(scopeRepository, times(maxAttempts)).findById(any());
    }

    @Test
    public void whenCreateScopeFromCsvMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(scopeRepository.save(any())).thenThrow(CannotCreateTransactionException.class).thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.createScopeFromCsv(SCOPE_NAME, file)).isInstanceOf(DataAccessException.class);
        verify(scopeRepository, times(maxAttempts)).save(any());
    }

    @Test
    public void whenExistsByNameMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(scopeRepository.existsByName(any())).thenThrow(CannotCreateTransactionException.class)
                .thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.existsByName(SCOPE_NAME)).isInstanceOf(DataAccessException.class);
        verify(scopeRepository, times(maxAttempts)).existsByName(any());
    }

    private void assertInvalidTextHasNotBeenLogged(final MultipartFile file) throws IOException {
        new String(file.getBytes(), StandardCharsets.UTF_8).lines()
                .forEach(this::assertInvalidTextHasNotBeenLogged);
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
    }

    private Scope createScope() {
        final Scope scope = new Scope();
        scope.setName(SCOPE_NAME);
        scope.setId(UUID_VALUE);
        scope.setFdns(List.of("ManagedElement=TestFdn0",
                "ManagedElement=TestFdn1",
                "ManagedElement=TestFdn2"));
        return scope;
    }
}