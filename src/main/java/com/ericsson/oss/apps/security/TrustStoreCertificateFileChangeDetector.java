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

import static com.ericsson.oss.apps.util.Constants.CERT_FILE_CHECK_INITIAL_DELAY_IN_SECONDS;
import static com.ericsson.oss.apps.util.Constants.CERT_FILE_CHECK_SCHEDULE_IN_SECONDS;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;

import com.ericsson.oss.apps.exception.CertificateHandlingException;
import com.ericsson.oss.apps.util.CustomX509ManagerUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TrustStoreCertificateFileChangeDetector {
    private final CustomX509TrustManager trustManager;
    private final TlsConfig tlsConfig;

    private final TrustStoreConfig trustStoreConfig;
    private final Map<Path, FileTime> lastAcceptedModifiedTime = new HashMap<>();

    @Scheduled(fixedRate = CERT_FILE_CHECK_SCHEDULE_IN_SECONDS, initialDelay = CERT_FILE_CHECK_INITIAL_DELAY_IN_SECONDS, timeUnit = TimeUnit.SECONDS)
    public void checkForUpdates() {
        log.info("Checking for certificate updates");
        final Path certDirPath = Paths.get(tlsConfig.getTruststoreCertFilePath());

        try (final DirectoryStream<Path> dirStream = Files.newDirectoryStream(certDirPath)) {
            for (final Path path : dirStream) {
                if (!Files.isDirectory(path)) {
                    final FileTime fileLastModifiedTime = Files.getLastModifiedTime(path);
                    if (fileIsChanged(path, fileLastModifiedTime)) {
                        loadCerts(path, fileLastModifiedTime);
                    }
                }
            }
            log.info("Completed check for certificate updates");
        } catch (final IOException e) {
            log.error("Failed to read directory {}. Unable to perform certificate updates.", certDirPath, e);
        }
    }

    void loadCerts(final Path path, final FileTime fileLastModifiedTime) {
        try {
            final Collection<Certificate> certificates = trustManager.loadCertsFromFile(path);
            setLastModifiedTime(path, fileLastModifiedTime);
            trustManager.addCertificates(certificates);
        } catch (final CertificateHandlingException | IOException e) {
            log.error("Failed to reload and update modified certificates for {}", path, e);
        }
    }

    private boolean fileIsChanged(final Path certFilePath, final FileTime fileLastModifiedTime) {
        if (lastAcceptedModifiedTime.containsKey(certFilePath) && fileLastModifiedTime.equals(lastAcceptedModifiedTime.get(certFilePath))) {
            log.info("certificate file {} last modified timestamp has not changed, certificate will not be reloaded", certFilePath);
            return false;
        }

        final KeyStore keystore = readTrustStore();

        //Since we are creating the truststore at deploy time we need to make sure we don't duplicate the cert and key on first load
        if (!Objects.isNull(keystore) && lastAcceptedModifiedTime.isEmpty()) {
            log.info("certificate file {} last modified timestamp has not changed, certificate and key will not be reloaded", certFilePath);
            setLastModifiedTime(certFilePath, fileLastModifiedTime);
            return false;
        }

        log.info("Certificate file {} last modified timestamp has changed, reloading certificate", certFilePath);
        return true;
    }

    private KeyStore readTrustStore() {
        KeyStore keystore = null;

        try {
            keystore = CustomX509ManagerUtils.loadStore(trustStoreConfig.getAppStorePath(), trustStoreConfig.getAppTrustStorePass(), "jks");
        } catch (final CertificateHandlingException e) {
            log.warn("Failed to load keystore", e);
        }
        return keystore;
    }

    //created for unit test
    void setLastModifiedTime(final Path certFilePath, final FileTime fileLastModifiedTime) {
        lastAcceptedModifiedTime.put(certFilePath, fileLastModifiedTime);
    }
}