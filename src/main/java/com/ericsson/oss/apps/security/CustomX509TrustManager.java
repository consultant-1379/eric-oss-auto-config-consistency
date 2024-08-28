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

package com.ericsson.oss.apps.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.ericsson.oss.apps.exception.CertificateHandlingException;
import com.ericsson.oss.apps.util.CustomX509ManagerUtils;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(value = "tls.enabled", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class CustomX509TrustManager {
    private static final String KEY_STORE_TYPE = "jks";
    private final TrustStoreConfig trustStoreConfig;
    private final RestTemplateSslContextCustomizer restTemplateSslContextCustomizer;
    private Set<Certificate> addedCerts;

    @PostConstruct
    void instantiateNewStore() {
        addedCerts = new HashSet<>();
    }

    Collection<Certificate> loadCertsFromFile(final Path platformCertFilePath) throws IOException {
        return CustomX509ManagerUtils.loadCertsFromFile(platformCertFilePath);
    }

    void saveKeyStore(final KeyStore trustStore) {
        try (final var outputStream = Files.newOutputStream(Paths.get(trustStoreConfig.getAppStorePath()))) {
            trustStore.store(outputStream, trustStoreConfig.getAppTrustStorePass().toCharArray());
        } catch (final IOException | NoSuchAlgorithmException | KeyStoreException | CertificateException e) {
            throw new CertificateHandlingException("Failed to save keystore file", e);
        }
        restTemplateSslContextCustomizer.updateRestTemplates(trustStore);
    }

    void addCertificates(final Collection<Certificate> certs) {
        final var trustStore = CustomX509ManagerUtils.loadStore(trustStoreConfig.getAppStorePath(), trustStoreConfig.getAppTrustStorePass(),
                KEY_STORE_TYPE);
        final Set<Certificate> successfullyAddedCerts = new HashSet<>();
        boolean shouldUpdate = false;

        for (final Certificate cert : certs) {
            final boolean newCert = !addedCerts.contains(cert);
            if (newCert) {
                try {
                    trustStore.setCertificateEntry(String.valueOf(UUID.randomUUID()), cert);
                    shouldUpdate = true;
                    successfullyAddedCerts.add(cert);
                } catch (final KeyStoreException e) {
                    log.error("Failed to set certificate entry in truststore", e);
                }
            } else {
                log.warn("Duplicate certificate was accepted but not added");
            }
        }

        if (shouldUpdate) {
            saveKeyStore(trustStore);
            addedCerts.addAll(successfullyAddedCerts);
            log.debug("Certificates are added and accepted successfully");
        }
    }
}