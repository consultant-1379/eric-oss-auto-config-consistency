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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.reset;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.apps.exception.CertificateHandlingException;
import com.ericsson.oss.apps.util.CustomX509ManagerTestUtils;
import com.ericsson.oss.apps.util.CustomX509ManagerUtils;

/**
 * Unit tests for {@link CustomX509KeyManager} class.
 */
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(properties = {
        "tls.enabled=true",
        "tls.keystore.appStorePath=" + CustomX509KeyManagerTest.APP_KEY_STORE,
        "tls.keystore.appKeyStorePass=testPassword"
})
class CustomX509KeyManagerTest {

    private static final String RESOURCE_FILE_PATH = "src/test/resources/tls/";
    private static final int INITIAL_COUNT = 0;

    private static final int UPDATED_COUNT = 1;
    private static final String KEY_STORE_RESOURCE_FILE_PATH = RESOURCE_FILE_PATH + "keystore/";

    private static final String CERT_FILE_NAME = "keystore_cert.crt";

    private static final String KEY_FILE_NAME = "keystore_key.key";

    private static final String CERT_TO_UPDATE_RESOURCE_FILE_PATH = RESOURCE_FILE_PATH + "keystore_certs-to-update/";

    private static final String CERT_TO_UPDATE_FILE_NAME = "new_cert.crt";

    private static final String KEY_TO_UPDATE_FILE_NAME = "new_key.key";

    private static final String INVALID_CERT_FILE_NAME = "not-a-certificate.crt";

    private static final String NON_EXISTENT_RESOURCE_FILE_PATH = "src/test/resources/tls/not-a-valid-directory/";

    static final String CERT_RESOURCE_FILE_PATH = RESOURCE_FILE_PATH + "keystore_cert_and_key/";
    static final String APP_KEY_STORE_FILE_NAME = "eric-log-transformer.p12";

    static final String APP_KEY_STORE = KEY_STORE_RESOURCE_FILE_PATH + APP_KEY_STORE_FILE_NAME;

    @Value("${tls.keystore.appKeyStorePass}")
    private String appKeyStorePass;

    @SpyBean
    private CustomX509KeyManager objectUnderTest;

    @MockBean
    private CustomX509TrustManager trustManager;

    @SpyBean
    private KeyStoreConfig keyStoreConfig;

    @SpyBean
    private TlsConfig tlsConfig;

    @BeforeAll
    static void beforeAll() throws IOException {
        CustomX509ManagerTestUtils.cleanUpTempKeystore(APP_KEY_STORE);
    }

    @AfterAll
    static void afterAll() throws IOException {
        CustomX509ManagerTestUtils.cleanUpTempKeystore(APP_KEY_STORE);
    }

    @BeforeEach
    public void setUp() {
        reset(tlsConfig, objectUnderTest);
    }

    @Test
    @Order(0)
    void whenKeyStoreIsLoaded_verifyKeyStoreWrittenToFile() throws KeyStoreException {
        instantiateNewStore();
        verifyKeyStoreFileCount(appKeyStorePass, INITIAL_COUNT);
    }

    @Test
    @Order(1)
    void whenKeyStoreIsLoadedAndPopulatedWithCerts_verifyKeyStoreWrittenToFile()
            throws CertificateException, InvalidKeySpecException, IOException, KeyStoreException, NoSuchAlgorithmException {
        verifyKeyStoreFileCount(appKeyStorePass, INITIAL_COUNT);
        final Collection<Certificate> certs = objectUnderTest.loadCertsFromFile(Path.of(CERT_RESOURCE_FILE_PATH, CERT_FILE_NAME));
        final Key key = objectUnderTest.loadKeyFromFile(Path.of(CERT_RESOURCE_FILE_PATH, KEY_FILE_NAME));
        objectUnderTest.updateKeyStore(certs, key);

        verifyKeyStoreFileCount(appKeyStorePass, UPDATED_COUNT);
        verifyKeyStoreContainsExpectedCertAndKey(appKeyStorePass, CERT_RESOURCE_FILE_PATH, CERT_FILE_NAME, KEY_FILE_NAME);
    }

    @Test
    @Order(2)
    void whenKeyStoreIsLoaded_verifyKeyManagerDoesNotContainUpdatedCertOrKey()
            throws CertificateException, KeyStoreException, InvalidKeySpecException, NoSuchAlgorithmException {
        verifyKeyStoreDoesNotContainUnexpectedCertAndKey(appKeyStorePass, CERT_TO_UPDATE_RESOURCE_FILE_PATH, CERT_TO_UPDATE_FILE_NAME,
                KEY_TO_UPDATE_FILE_NAME);
    }

    @Test
    @Order(3)
    void whenKeyStoreLoadCertsIsCalled_andCertificateIsNotValid_verifyKeystoreIsNotUpdated()
            throws CertificateException, InvalidKeySpecException, KeyStoreException, NoSuchAlgorithmException {
        CustomX509ManagerTestUtils.assertCertCountInTempKeystore(APP_KEY_STORE, appKeyStorePass, UPDATED_COUNT, KeyStore.getDefaultType());
        final Path certPath = Paths.get(CERT_TO_UPDATE_RESOURCE_FILE_PATH + INVALID_CERT_FILE_NAME);
        assertThatThrownBy(() -> objectUnderTest.loadCertsFromFile(certPath))
                .isInstanceOf(CertificateHandlingException.class)
                .hasMessage("Failed to create Certificates from file");

        verifyKeyStoreDoesNotContainUnexpectedCertAndKey(appKeyStorePass, CERT_TO_UPDATE_RESOURCE_FILE_PATH, CERT_TO_UPDATE_FILE_NAME,
                KEY_TO_UPDATE_FILE_NAME);

        verifyKeyStoreContainsExpectedCertAndKey(appKeyStorePass, CERT_RESOURCE_FILE_PATH, CERT_FILE_NAME, KEY_FILE_NAME);
    }

    @Test
    @Order(4)
    void whenKeyStoreLoadCertsIsCalled_andDirectoryDoesNotExist_verifyKeystoreIsNotUpdated()
            throws CertificateException, InvalidKeySpecException, IOException, KeyStoreException, NoSuchAlgorithmException {
        CustomX509ManagerTestUtils.assertCertCountInTempKeystore(APP_KEY_STORE, appKeyStorePass, UPDATED_COUNT, KeyStore.getDefaultType());

        objectUnderTest.loadCertsFromFile(Paths.get(NON_EXISTENT_RESOURCE_FILE_PATH));

        verifyKeyStoreContainsExpectedCertAndKey(appKeyStorePass, CERT_RESOURCE_FILE_PATH, CERT_FILE_NAME, KEY_FILE_NAME);
        verifyKeyStoreFileCount(appKeyStorePass, UPDATED_COUNT);
    }

    @Test
    @Order(5)
    void whenKeyStoreLoadCertsIsCalled_andAlreadyInitialized_verifyKeystoreIsUpdated()
            throws NoSuchAlgorithmException, KeyStoreException, IOException, InvalidKeySpecException, CertificateException {
        CustomX509ManagerTestUtils.assertCertCountInTempKeystore(APP_KEY_STORE, appKeyStorePass, UPDATED_COUNT, KeyStore.getDefaultType());
        final Key key = objectUnderTest.loadKeyFromFile(Path.of(CERT_TO_UPDATE_RESOURCE_FILE_PATH, KEY_TO_UPDATE_FILE_NAME));
        final Collection<Certificate> certs = objectUnderTest
                .loadCertsFromFile(Path.of(CERT_TO_UPDATE_RESOURCE_FILE_PATH, CERT_TO_UPDATE_FILE_NAME));
        objectUnderTest.updateKeyStore(certs, key);

        verifyKeyStoreContainsExpectedCertAndKey(appKeyStorePass, CERT_TO_UPDATE_RESOURCE_FILE_PATH, CERT_TO_UPDATE_FILE_NAME,
                KEY_TO_UPDATE_FILE_NAME);
        verifyKeyStoreFileCount(appKeyStorePass, UPDATED_COUNT);
    }

    private static void verifyKeyStoreContainsExpectedCertAndKey(final String tempStorePass, final String path, final String certFileName,
            final String keyFileName)
            throws CertificateException, InvalidKeySpecException, KeyStoreException, NoSuchAlgorithmException {

        final List<X509Certificate> inKeyStore = CustomX509ManagerTestUtils.getCertsInKeystore(APP_KEY_STORE, tempStorePass,
                KeyStore.getDefaultType());
        final List<X509Certificate> expectedCerts = CustomX509ManagerTestUtils.loadCertificate(path, certFileName);
        assertThat(inKeyStore).containsAll(expectedCerts);

        final Key keyInKeyStore = CustomX509ManagerTestUtils.getKeyInKeyStore(APP_KEY_STORE, tempStorePass, KeyStore.getDefaultType());
        final Key expectedKey = CustomX509ManagerTestUtils.loadKey(path, keyFileName);
        assertThat(keyInKeyStore).isEqualTo(expectedKey);
    }

    private static void verifyKeyStoreDoesNotContainUnexpectedCertAndKey(final String tempStorePass, final String path, final String certFileName,
            final String keyFileName) throws CertificateException, InvalidKeySpecException, KeyStoreException, NoSuchAlgorithmException {

        final List<X509Certificate> inKeyStore = CustomX509ManagerTestUtils.getCertsInKeystore(APP_KEY_STORE, tempStorePass,
                KeyStore.getDefaultType());
        final List<X509Certificate> toUpdateCerts = CustomX509ManagerTestUtils.loadCertificate(path, certFileName);
        assertThat(inKeyStore).isNotEmpty().doesNotContainAnyElementsOf(toUpdateCerts);

        final Key keyInKeyStore = CustomX509ManagerTestUtils.getKeyInKeyStore(APP_KEY_STORE, tempStorePass, KeyStore.getDefaultType());
        final Key toUpdatedKey = CustomX509ManagerTestUtils.loadKey(path, keyFileName);
        assertThat(keyInKeyStore).isNotEqualTo(toUpdatedKey);
    }

    private static void verifyKeyStoreFileCount(final String tempStorePass, final int expectedCount) throws KeyStoreException {
        CustomX509ManagerTestUtils.assertCertCountInTempKeystore(APP_KEY_STORE, tempStorePass, expectedCount, KeyStore.getDefaultType());
        CustomX509ManagerTestUtils.assertKeyCountInTempKeyStore(APP_KEY_STORE, tempStorePass, expectedCount, KeyStore.getDefaultType());

    }

    private void instantiateNewStore() {
        final var ks = CustomX509ManagerUtils.loadStore(null, appKeyStorePass, KeyStore.getDefaultType());
        objectUnderTest.saveKeyStore(ks);
    }
}
