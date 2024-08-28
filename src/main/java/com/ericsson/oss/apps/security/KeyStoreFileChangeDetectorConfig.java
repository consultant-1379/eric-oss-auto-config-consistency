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

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "tls.enabled", havingValue = "true")
@ConditionalOnExpression("'${logging.streamingMethod}'=='direct' || '${logging.streamingMethod}'=='dual'")
public class KeyStoreFileChangeDetectorConfig {

    @Bean
    public KeyStoreCertificateFileChangeDetector keyStoreCertificateFileScheduledWatcher(
            final CustomX509KeyManager keyManager, final TlsConfig certConfig, final KeyStoreConfig keyStoreConfig) {
        return new KeyStoreCertificateFileChangeDetector(keyManager, certConfig, keyStoreConfig);
    }
}
