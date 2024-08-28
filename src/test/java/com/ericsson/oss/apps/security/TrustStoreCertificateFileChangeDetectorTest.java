/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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

package com.ericsson.oss.apps.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.oss.apps.exception.CertificateHandlingException;

/**
 * Unit tests for {@link TrustStoreCertificateFileChangeDetector} class.
 */
@ExtendWith(MockitoExtension.class)
class TrustStoreCertificateFileChangeDetectorTest {
    private static final String CERTS_TO_UPDATE_PATH = "src/test/resources/tls/truststore_certs-to-update";
    private static final String CERT_PATH = "src/test/resources/tls/truststore_certs";
    private static final String DIR_CERTS_PATH = "src/test/resources/tls";
    private static final String NON_EXISTENT_DIR_PATH = "src/test/resources/tls/not-a-valid-directory";

    private static final String TRUSTSTORE_PATH = "src/test/resources/tls/truststore/cacerts";
    @Mock
    private CustomX509TrustManager customX509TrustManager;
    @Mock
    private TlsConfig tlsConfig;

    @Mock
    private TrustStoreConfig trustStoreConfig;

    @Spy
    @InjectMocks
    private TrustStoreCertificateFileChangeDetector objectUnderTest;

    @Test
    void whenFileTimeStampHasChanged_verifyLoadCertsFromFileIsCalled() throws IOException {
        prepareTest();
        setFileModifiedTime();
        reset(customX509TrustManager, objectUnderTest);
        objectUnderTest.checkForUpdates();
        verify(customX509TrustManager, times(2)).loadCertsFromFile(any(Path.class));
    }

    @Test
    void whenFileTimeStampNotChanged_verifyLoadCertsFromFileNotCalled() {
        prepareTest();
        objectUnderTest.checkForUpdates();
        verifyNoMoreInteractions(customX509TrustManager);
    }

    @Test
    void whenCheckForUpdates_andTrustManagerThrowsCertificateHandlingException_verifyLastModifiedTimeIsNotCalled() throws IOException {
        prepareTest();
        doThrow(CertificateHandlingException.class)
                .when(customX509TrustManager).loadCertsFromFile(any(Path.class));
        setFileModifiedTime();
        when(trustStoreConfig.getAppTrustStorePass()).thenReturn("pass");
        objectUnderTest.checkForUpdates();
        verify(objectUnderTest, never()).setLastModifiedTime(any(Path.class), any(FileTime.class));
    }

    @Test
    void whenCheckForUpdates_andCertFilePathContainsOnlyDirectories_verifyLastModifiedTimeAndLoadCertsIsNotCalled() throws IOException {
        prepareTest();
        when(tlsConfig.getTruststoreCertFilePath()).thenReturn(DIR_CERTS_PATH);
        setFileModifiedTime();

        objectUnderTest.checkForUpdates();

        verify(customX509TrustManager, never()).loadCertsFromFile(any(Path.class));
        verify(objectUnderTest, never()).setLastModifiedTime(any(Path.class), any(FileTime.class));
    }

    @Test
    void whenCheckForUpdates_andCertFilePathDoesNotExist_verifyLastModifiedTimeAndLoadCertsIsNotCalled() throws IOException {
        prepareTest();
        when(tlsConfig.getTruststoreCertFilePath()).thenReturn(NON_EXISTENT_DIR_PATH);
        setFileModifiedTime();

        objectUnderTest.checkForUpdates();

        verify(customX509TrustManager, never()).loadCertsFromFile(any(Path.class));
        verify(objectUnderTest, never()).setLastModifiedTime(any(Path.class), any(FileTime.class));
    }

    @Test
    void whenCheckForUpdates_andCertIsInvalid_verifyLastModifiedTimeIsNotCalled() throws IOException {
        prepareTest();
        when(tlsConfig.getTruststoreCertFilePath()).thenReturn(CERTS_TO_UPDATE_PATH);
        objectUnderTest.checkForUpdates();
        doThrow(IOException.class).when(customX509TrustManager).loadCertsFromFile(any(Path.class));
        setFileModifiedTime();
        objectUnderTest.checkForUpdates();
        verify(objectUnderTest, never()).setLastModifiedTime(any(Path.class), any(FileTime.class));
    }

    @Test
    void whenKeyStoreIsNotNull_andLastModifiedTimeIsNotSet_verifyLastModifiedTimeIsCalled_andLoadCertsIsNotCalled()
            throws IOException {
        when(trustStoreConfig.getAppTrustStorePass()).thenReturn("pass");
        when(tlsConfig.getTruststoreCertFilePath()).thenReturn(CERT_PATH);
        when(trustStoreConfig.getAppStorePath()).thenReturn(TRUSTSTORE_PATH);
        when(trustStoreConfig.getAppTrustStorePass()).thenReturn("changeit");
        objectUnderTest.checkForUpdates();
        verify(objectUnderTest, times(1)).setLastModifiedTime(any(Path.class), any(FileTime.class));
        verify(customX509TrustManager, never()).loadCertsFromFile(any(Path.class));
    }

    private void setFileModifiedTime() throws IOException {
        try (final DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(CERTS_TO_UPDATE_PATH))) {
            for (final Path path : dirStream) {
                final FileTime currentFileLastModifiedTime = Files.getLastModifiedTime(path);
                final Instant timeBeforeCurrentFileLastModifiedTime = currentFileLastModifiedTime.toInstant().minus(Duration.ofMinutes(120));
                final FileTime newFileTime = FileTime.fromMillis(timeBeforeCurrentFileLastModifiedTime.toEpochMilli());
                objectUnderTest.setLastModifiedTime(path, newFileTime);
            }
        }
        reset(objectUnderTest);
    }

    private void prepareTest() {
        when(tlsConfig.getTruststoreCertFilePath()).thenReturn(CERTS_TO_UPDATE_PATH);
        when(trustStoreConfig.getAppTrustStorePass()).thenReturn("pass");
        objectUnderTest.checkForUpdates();
        reset(customX509TrustManager, objectUnderTest);
    }
}
