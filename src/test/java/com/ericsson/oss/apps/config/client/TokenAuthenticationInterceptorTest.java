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
package com.ericsson.oss.apps.config.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClientException;

import com.ericsson.oss.apps.client.iam.TokenEndpointApi;
import com.ericsson.oss.apps.client.iam.model.AccessTokenResponse;
import com.ericsson.oss.apps.exception.TokenAuthenticationException;

/**
 * Unit tests for {@link TokenAuthenticationInterceptor} class
 */
@ActiveProfiles("test")
@SpringBootTest(properties = {
        "gateway.services.iam.clientId=" + TokenAuthenticationInterceptorTest.CLIENT_ID,
        "gateway.services.iam.clientSecret=" + TokenAuthenticationInterceptorTest.CLIENT_SECRET,
})
class TokenAuthenticationInterceptorTest {
    static final String CLIENT_ID = "clientId";
    static final String CLIENT_SECRET = "clientSecret";
    private static final String SIMPLE_TOKEN = "access_token";

    @Autowired
    private TokenAuthenticationInterceptor objectUnderTest;
    @MockBean
    private TokenEndpointApi tokenEndpointApi;

    @BeforeEach
    public void setUp() {
        reset(tokenEndpointApi);
        objectUnderTest.resetToken();
    }

    @Test
    void whenInterceptIsCalled_andTokenSetInInterceptor_verifyAuthorizationHeaderSet() throws IOException {
        final AccessTokenResponse accessTokenResponse = new AccessTokenResponse(SIMPLE_TOKEN, 300);
        when(tokenEndpointApi.getToken(CLIENT_ID, CLIENT_SECRET)).thenReturn(accessTokenResponse);

        final HttpHeaders headers = spy(new HttpHeaders());
        final HttpRequest request = mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(headers);

        final byte[] body = new byte[0];
        final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        objectUnderTest.intercept(request, body, execution);

        verify(headers, atMostOnce()).add(HttpHeaders.AUTHORIZATION, "Bearer " + SIMPLE_TOKEN);
        verify(execution, atMostOnce()).execute(request, body);
        assertThat(headers.get(HttpHeaders.AUTHORIZATION)).containsExactly("Bearer " + SIMPLE_TOKEN);
    }

    @Test
    void whenInterceptIsCalledTwice_andTokenSetInInterceptor_verifyTokenEndpointApiCalledOnlyOnce() throws IOException {
        final AccessTokenResponse accessTokenResponse = new AccessTokenResponse(SIMPLE_TOKEN, 300);
        when(tokenEndpointApi.getToken(CLIENT_ID, CLIENT_SECRET)).thenReturn(accessTokenResponse);

        final HttpHeaders headers = spy(new HttpHeaders());
        final HttpRequest request = mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(headers);

        final byte[] body = new byte[0];
        final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        objectUnderTest.intercept(request, body, execution);
        objectUnderTest.intercept(request, body, execution);

        verify(headers, times(2)).add(HttpHeaders.AUTHORIZATION, "Bearer " + SIMPLE_TOKEN);
        verify(tokenEndpointApi, atMostOnce()).getToken(CLIENT_ID, CLIENT_SECRET);
        assertThat(headers.get(HttpHeaders.AUTHORIZATION)).containsOnly("Bearer " + SIMPLE_TOKEN);
    }

    @Test
    void whenInterceptIsCalledByMultipleThreads_verifyTokenEndpointApiCalledOnlyOnce() throws InterruptedException {
        final AccessTokenResponse accessTokenResponse = new AccessTokenResponse(SIMPLE_TOKEN, 300);
        when(tokenEndpointApi.getToken(CLIENT_ID, CLIENT_SECRET)).thenReturn(accessTokenResponse);

        final int numberOfThreads = 5;
        final ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        final CountDownLatch latch = new CountDownLatch(numberOfThreads);

        final HttpHeaders headers = spy(new HttpHeaders());
        final HttpRequest request = mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(headers);

        final byte[] body = new byte[0];
        final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    objectUnderTest.intercept(request, body, execution);
                } catch (final IOException e) { //NOPMD
                }
                latch.countDown();
            });
        }

        latch.await();

        verify(headers, times(numberOfThreads)).add(HttpHeaders.AUTHORIZATION, "Bearer " + SIMPLE_TOKEN);
        verify(tokenEndpointApi, atMostOnce()).getToken(CLIENT_ID, CLIENT_SECRET);
        assertThat(headers.get(HttpHeaders.AUTHORIZATION)).containsOnly("Bearer " + SIMPLE_TOKEN);
    }

    @Test
    void whenInterceptIsCalledTwice_andTokenIsExpired_verifyTokenEndpointApiCalledTwice() throws IOException {
        final AccessTokenResponse accessTokenResponse = new AccessTokenResponse(SIMPLE_TOKEN, 29);
        when(tokenEndpointApi.getToken(CLIENT_ID, CLIENT_SECRET)).thenReturn(accessTokenResponse);

        final HttpHeaders headers = spy(new HttpHeaders());
        final HttpRequest request = mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(headers);

        final byte[] body = new byte[0];
        final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        objectUnderTest.intercept(request, body, execution);
        objectUnderTest.intercept(request, body, execution);

        verify(headers, times(2)).add(HttpHeaders.AUTHORIZATION, "Bearer " + SIMPLE_TOKEN);
        verify(tokenEndpointApi, times(2)).getToken(CLIENT_ID, CLIENT_SECRET);
        assertThat(headers.get(HttpHeaders.AUTHORIZATION)).containsOnly("Bearer " + SIMPLE_TOKEN);
    }

    @Test
    void whenInterceptIsCalled_andTokenEndpointApiFailed_verifyAuthorizationHeaderSetNull() {
        when(tokenEndpointApi.getToken(CLIENT_ID, CLIENT_SECRET)).thenReturn(null);

        final HttpHeaders headers = spy(new HttpHeaders());
        final HttpRequest request = mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(headers);

        final byte[] body = new byte[0];

        assertThatThrownBy(() -> objectUnderTest.intercept(request, body, mock(ClientHttpRequestExecution.class)))
                .isInstanceOf(TokenAuthenticationException.class)
                .hasMessage("Failed to get access token. Request was successful but returned empty body")
                .hasNoCause();

        verify(headers, never()).add(HttpHeaders.AUTHORIZATION, eq(anyString()));
    }

    @Test
    void whenInterceptIsCalled_andTokenEndpointApiThrowsException_verifyAuthorizationHeaderSetNull() {
        final RestClientException exception = new RestClientException("RestClientException");

        when(tokenEndpointApi.getToken(CLIENT_ID, CLIENT_SECRET)).thenThrow(exception);

        final HttpHeaders headers = spy(new HttpHeaders());
        final HttpRequest request = mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(headers);

        final byte[] body = new byte[0];

        assertThatThrownBy(() -> objectUnderTest.intercept(request, body, mock(ClientHttpRequestExecution.class)))
                .isInstanceOf(TokenAuthenticationException.class)
                .hasMessage("Failed to get access token")
                .hasCause(exception);

        verify(headers, never()).add(HttpHeaders.AUTHORIZATION, eq(anyString()));
    }
}
