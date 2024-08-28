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

package com.ericsson.oss.apps.config.client;

import static com.ericsson.oss.apps.util.Constants.NCMP;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.apps.client.ncmp.NetworkCmProxyApi;
import com.ericsson.oss.apps.config.GatewayProperties;

/**
 * Unit tests for {@link NetworkCmProxyClientConfiguration} class.
 */
@ExtendWith(MockitoExtension.class)
class NetworkCmProxyClientConfigurationTest {
    private static final String NCMP_URL = "https://ncmp.test.ericsson.se";
    private static final String NCMP_BASE_PATH = "/ncmp";
    private static final String NCMP_API_CLIENT_BASE_PATH = NCMP_URL + NCMP_BASE_PATH;

    private final GatewayProperties gatewayProperties = new GatewayProperties();
    private final RestTemplate restTemplate = new RestTemplateBuilder().build();

    private NetworkCmProxyClientConfiguration objectUnderTest;

    @BeforeEach
    public void setUp() {
        final GatewayProperties.Service ncmpService = new GatewayProperties.Service();
        ncmpService.setUrl(NCMP_URL);
        ncmpService.setBasePath(NCMP_BASE_PATH);

        gatewayProperties.setServices(new HashMap<>());
        gatewayProperties.getServices().put(NCMP, ncmpService);

        objectUnderTest = new NetworkCmProxyClientConfiguration(gatewayProperties);
    }

    @Test
    void whenNetworkCmProxyApiIsCreated_verifyBasePathOfApiClientConfiguredCorrectly() {
        final NetworkCmProxyApi ncmpApi = objectUnderTest.networkCmProxyApi(restTemplate);

        assertThat(ncmpApi.getApiClient().getBasePath())
                .isEqualTo(NCMP_API_CLIENT_BASE_PATH);
    }
}