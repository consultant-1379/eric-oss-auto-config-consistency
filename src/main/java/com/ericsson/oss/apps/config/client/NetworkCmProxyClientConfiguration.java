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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.apps.client.ApiClient;
import com.ericsson.oss.apps.client.ncmp.NetworkCmProxyApi;
import com.ericsson.oss.apps.config.GatewayProperties;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class NetworkCmProxyClientConfiguration {
    private final GatewayProperties gatewayProperties;

    @Bean
    @Primary
    public NetworkCmProxyApi networkCmProxyApi(final RestTemplate restTemplate) {
        final ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(gatewayProperties.getBasePath(NCMP));
        return new NetworkCmProxyApi(apiClient);
    }
}
