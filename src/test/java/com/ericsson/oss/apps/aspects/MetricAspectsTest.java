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

package com.ericsson.oss.apps.aspects;

import static com.ericsson.oss.apps.util.Constants.VERSION_PATH;
import static com.ericsson.oss.apps.util.MetricConstants.APIGATEWAY_SESSIONID_HTTP_REQUESTS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.CODE;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_CI_HTTP_REQUESTS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_HTTP_REQUESTS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.ENDPOINT;
import static com.ericsson.oss.apps.util.MetricConstants.METHOD;
import static com.ericsson.oss.apps.util.MetricConstants.NCMP_HTTP_REQUESTS_TOTAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.ericsson.oss.apps.service.MetricService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Unit tests for {@link MetricAspects} class.
 */
@ExtendWith(MockitoExtension.class)
class MetricAspectsTest {
    private static final String EXECUTIONS_ENDPOINT = VERSION_PATH + "/executions";
    private static final String EXECUTIONS_AUDIT_RESULTS_ENDPOINT = VERSION_PATH + "/executions/{id}/audit-results";
    private static final String SCOPE_ENDPOINT = VERSION_PATH + "/scopes";
    private static final String SCOPE_ID_ENDPOINT = VERSION_PATH + "/scopes/{id}";
    private static final String RULESET_ENDPOINT = VERSION_PATH + "/rulesets";
    private static final String RULESET_ID_ENDPOINT = VERSION_PATH + "/rulesets/{id}";
    private static final String JOBS_ENDPOINT = VERSION_PATH + "/jobs";
    private static final String JOBS_ID_ENDPOINT = VERSION_PATH + "/jobs/{jobName}";
    private static final String API_GATEWAY_ENDPOINT = "/auth/v1/login";
    private static final String NCMP_ENDPOINT = "/v1/ch/{cm-handle}/data/ds/{ncmp-datastore-name}";
    private MetricAspects objectUnderTest;
    private MeterRegistry meterRegistry;
    private MetricService metricService;

    @Mock
    private ProceedingJoinPoint pjp;
    @Mock
    private JoinPoint jp;
    @Mock
    private ResponseEntity responseEntity;

    @BeforeEach
    public void setup() {
        meterRegistry = new SimpleMeterRegistry();
        metricService = new MetricService(meterRegistry);
        objectUnderTest = new MetricAspects(metricService);
    }

    @AfterEach
    public void clearMetrics() {
        meterRegistry.clear();
    }

    @Test
    void whenClientInvokeApiCalledForApiGateway_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.OK.value();
        when(pjp.getArgs()).thenReturn(new Object[] { API_GATEWAY_ENDPOINT, HttpMethod.GET });
        when(pjp.proceed()).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(expectedHttpCode));

        objectUnderTest.handleAroundClientInvokeApi(pjp);

        assertThat(getCountForMetric(APIGATEWAY_SESSIONID_HTTP_REQUESTS_TOTAL, API_GATEWAY_ENDPOINT, HttpMethod.GET, expectedHttpCode))
                .isEqualTo(1.0);
    }

    @Test
    void whenClientInvokeApiCalledForNcmp_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.OK.value();
        when(pjp.getArgs()).thenReturn(new Object[] { NCMP_ENDPOINT, HttpMethod.GET });
        when(pjp.proceed()).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(expectedHttpCode));

        objectUnderTest.handleAroundClientInvokeApi(pjp);

        assertThat(getCountForMetric(NCMP_HTTP_REQUESTS_TOTAL, NCMP_ENDPOINT, HttpMethod.GET, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenClientInvokeApiCalledForNcmpAndExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.FORBIDDEN.value();
        when(pjp.getArgs()).thenReturn(new Object[] { NCMP_ENDPOINT, HttpMethod.GET });
        when(pjp.proceed()).thenThrow(new RestClientResponseException("", expectedHttpCode, "", null, null, null));

        assertThrows(RestClientResponseException.class, () -> {
            objectUnderTest.handleAroundClientInvokeApi(pjp);
        });
        assertThat(getCountForMetric(NCMP_HTTP_REQUESTS_TOTAL, NCMP_ENDPOINT, HttpMethod.GET, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenGetJobsCalled_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.OK.value();
        when(pjp.proceed()).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(expectedHttpCode));

        objectUnderTest.handleAroundGetJobs(pjp);

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, JOBS_ENDPOINT, HttpMethod.GET, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenPostJobsCalled_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.OK.value();
        when(pjp.proceed()).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(expectedHttpCode));

        objectUnderTest.handleAroundPostJobs(pjp);

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, JOBS_ENDPOINT, HttpMethod.POST, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenPostJobsCalledAndExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        objectUnderTest.handlePostJobsException(jp, new IllegalArgumentException());

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, JOBS_ENDPOINT, HttpMethod.POST, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenPostJobsCalledAndResponseStatusExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        objectUnderTest.handlePostJobsException(jp, new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, JOBS_ENDPOINT, HttpMethod.POST, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenPutJobCalled_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.OK.value();
        when(pjp.proceed()).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(expectedHttpCode));

        objectUnderTest.handleAroundPutJob(pjp);

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, JOBS_ID_ENDPOINT, HttpMethod.PUT, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenPutJobCalledAndExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        objectUnderTest.handlePutJobException(jp, new IllegalArgumentException());

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, JOBS_ID_ENDPOINT, HttpMethod.PUT, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenPutJobCalledAndResponseStatusExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        objectUnderTest.handlePutJobException(jp, new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, JOBS_ID_ENDPOINT, HttpMethod.PUT, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenDeleteJobCalled_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.NO_CONTENT.value();
        when(pjp.proceed()).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(expectedHttpCode));

        objectUnderTest.handleAroundDeleteJob(pjp);

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, JOBS_ID_ENDPOINT, HttpMethod.DELETE, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenDeleteJobCalledAndExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        objectUnderTest.handleDeleteJobException(jp, new IllegalArgumentException());

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, JOBS_ID_ENDPOINT, HttpMethod.DELETE, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenDeleteJobCalledAndResponseStatusExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        objectUnderTest.handleDeleteJobException(jp, new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, JOBS_ID_ENDPOINT, HttpMethod.DELETE, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenGetExecutionsCalled_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.OK.value();
        when(pjp.proceed()).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(expectedHttpCode));

        objectUnderTest.handleAroundGetExecutions(pjp);

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, EXECUTIONS_ENDPOINT, HttpMethod.GET, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenGetAuditResultsCalled_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.OK.value();
        when(pjp.proceed()).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(expectedHttpCode));

        objectUnderTest.handleAroundGetAuditResults(pjp);

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, EXECUTIONS_AUDIT_RESULTS_ENDPOINT, HttpMethod.GET, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenPostAuditResultsCalled_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.ACCEPTED.value();
        when(pjp.proceed()).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(expectedHttpCode));

        objectUnderTest.handleAroundPostAuditResults(pjp);

        assertThat(getCountForMetric(EACC_CI_HTTP_REQUESTS_TOTAL, EXECUTIONS_AUDIT_RESULTS_ENDPOINT, HttpMethod.POST, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenCreateRulesetCalled_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.OK.value();
        when(pjp.proceed()).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(expectedHttpCode));

        objectUnderTest.handleAroundCreateRuleset(pjp);

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, RULESET_ENDPOINT, HttpMethod.POST, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenCreateRulesetCalledAndExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        objectUnderTest.handleCreateRulesetException(jp, new IllegalArgumentException());

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, RULESET_ENDPOINT, HttpMethod.POST, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenCreateRulesetCalledAndResponseStatusExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        objectUnderTest.handleCreateRulesetException(jp, new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, RULESET_ENDPOINT, HttpMethod.POST, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenListRulesetsCalled_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.OK.value();
        when(pjp.proceed()).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(expectedHttpCode));

        objectUnderTest.handleAroundListRulesets(pjp);

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, RULESET_ENDPOINT, HttpMethod.GET, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenUpdateRulesetCalled_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.OK.value();
        when(pjp.proceed()).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(expectedHttpCode));

        objectUnderTest.handleAroundUpdateRuleset(pjp);

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, RULESET_ID_ENDPOINT, HttpMethod.PUT, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenUpdateRulesetCalledAndExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        objectUnderTest.handleUpdateRulesetException(jp, new IllegalArgumentException());

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, RULESET_ID_ENDPOINT, HttpMethod.PUT, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenUpdateRulesetCalledAndResponseStatusExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.CONFLICT.value();

        objectUnderTest.handleUpdateRulesetException(jp, new ResponseStatusException(HttpStatus.CONFLICT));

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, RULESET_ID_ENDPOINT, HttpMethod.PUT, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenDeleteRulesetCalled_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.OK.value();
        when(pjp.proceed()).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(expectedHttpCode));

        objectUnderTest.handleAroundDeleteRuleset(pjp);

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, RULESET_ID_ENDPOINT, HttpMethod.DELETE, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenDeleteRulesetCalledAndExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        objectUnderTest.handleDeleteRulesetException(jp, new IllegalArgumentException());

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, RULESET_ID_ENDPOINT, HttpMethod.DELETE, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenDeleteRulesetCalledAndResponseExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.CONFLICT.value();

        objectUnderTest.handleDeleteRulesetException(jp, new ResponseStatusException(HttpStatus.CONFLICT));

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, RULESET_ID_ENDPOINT, HttpMethod.DELETE, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenGetRulesetCalled_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.OK.value();
        when(pjp.proceed()).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(expectedHttpCode));

        objectUnderTest.handleAroundGetRuleset(pjp);

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, RULESET_ID_ENDPOINT, HttpMethod.GET, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenCreateScopeCalled_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.OK.value();
        when(pjp.proceed()).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(expectedHttpCode));

        objectUnderTest.handleAroundCreateScope(pjp);

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, SCOPE_ENDPOINT, HttpMethod.POST, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenCreateScopeCalledAndExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        objectUnderTest.handleCreateScopeException(jp, new IllegalArgumentException());

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, SCOPE_ENDPOINT, HttpMethod.POST, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenCreateScopeCalledAndResponseStatusExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        objectUnderTest.handleCreateScopeException(jp, new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, SCOPE_ENDPOINT, HttpMethod.POST, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenListScopesCalled_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.OK.value();
        when(pjp.proceed()).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(expectedHttpCode));

        objectUnderTest.handleAroundListScopes(pjp);

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, SCOPE_ENDPOINT, HttpMethod.GET, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenListScopeIsCalledAndExceptionIsThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        objectUnderTest.handleListScopeException(jp, new RuntimeException());

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, SCOPE_ENDPOINT, HttpMethod.GET, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenListScopeIsCalledAndResponseStatusExceptionIsThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        objectUnderTest.handleListScopeException(jp, new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, SCOPE_ENDPOINT, HttpMethod.GET, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenUpdateScopeCalled_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.OK.value();
        when(pjp.proceed()).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(expectedHttpCode));

        objectUnderTest.handleAroundUpdateScope(pjp);

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, SCOPE_ID_ENDPOINT, HttpMethod.PUT, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenUpdateScopeCalledAndExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        objectUnderTest.handleUpdateScopeException(jp, new IllegalArgumentException());

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, SCOPE_ID_ENDPOINT, HttpMethod.PUT, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenUpdateScopeCalledAndResponseStatusExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        objectUnderTest.handleUpdateScopeException(jp, new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, SCOPE_ID_ENDPOINT, HttpMethod.PUT, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenDeleteScopeCalled_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.NO_CONTENT.value();
        when(pjp.proceed()).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(expectedHttpCode));

        objectUnderTest.handleAroundDeleteScope(pjp);

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, SCOPE_ID_ENDPOINT, HttpMethod.DELETE, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenDeleteScopeCalledAndExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        objectUnderTest.handleDeleteScopeException(jp, new IllegalArgumentException());

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, SCOPE_ID_ENDPOINT, HttpMethod.DELETE, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenDeleteScopeCalledAndResponseStatusExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.CONFLICT.value();

        objectUnderTest.handleDeleteScopeException(jp, new ResponseStatusException(HttpStatus.CONFLICT));

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, SCOPE_ID_ENDPOINT, HttpMethod.DELETE, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenGetScopeCalled_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.OK.value();
        when(pjp.proceed()).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(expectedHttpCode));

        objectUnderTest.handleAroundGetScope(pjp);

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, SCOPE_ID_ENDPOINT, HttpMethod.GET, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenGetScopeCalledAndExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        objectUnderTest.handleGetScopeException(jp, new IllegalArgumentException());

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, SCOPE_ID_ENDPOINT, HttpMethod.GET, expectedHttpCode)).isEqualTo(1.0);
    }

    @Test
    void whenGetScopeCalledAndResponseStatusExceptionThrown_thenClientCountMetricIsIncremented() throws Throwable {
        final int expectedHttpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        objectUnderTest.handleGetScopeException(jp, new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThat(getCountForMetric(EACC_HTTP_REQUESTS_TOTAL, SCOPE_ID_ENDPOINT, HttpMethod.GET, expectedHttpCode)).isEqualTo(1.0);
    }

    private double getCountForMetric(final String eaccHttpRequestsTotal, final String jobsEndpoint, final HttpMethod method,
            final int expectedHttpCode) {
        return metricService.findCounter(eaccHttpRequestsTotal,
                ENDPOINT, jobsEndpoint,
                METHOD, method.toString(),
                CODE, String.valueOf(expectedHttpCode)).get().count();
    }

}