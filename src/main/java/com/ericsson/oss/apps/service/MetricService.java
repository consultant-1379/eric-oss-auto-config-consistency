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

import static com.ericsson.oss.apps.util.MetricConstants.APIGATEWAY_SESSIONID_HTTP_REQUESTS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.APIGATEWAY_SESSIONID_HTTP_REQUESTS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.API_GATEWAY_ENDPOINT;
import static com.ericsson.oss.apps.util.MetricConstants.CODE;
import static com.ericsson.oss.apps.util.MetricConstants.DESCRIPTION;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_CI_APPLIED_CHANGES_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_CI_APPLIED_REVERSIONS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_CI_CHANGES_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_CI_HTTP_REQUESTS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_CI_HTTP_REQUESTS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_CI_REVERSIONS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_ATTR_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_AUDIT_QUEUE_COMPLETED_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_AUDIT_QUEUE_SUBMITTED_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_MO_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_RULES_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_HTTP_REQUESTS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_HTTP_REQUESTS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.ENDPOINT;
import static com.ericsson.oss.apps.util.MetricConstants.EXECUTIONS_AUDIT_RESULTS_ENDPOINT;
import static com.ericsson.oss.apps.util.MetricConstants.EXECUTIONS_ENDPOINT;
import static com.ericsson.oss.apps.util.MetricConstants.FAILED;
import static com.ericsson.oss.apps.util.MetricConstants.JOBS_ENDPOINT;
import static com.ericsson.oss.apps.util.MetricConstants.JOBS_ID_ENDPOINT;
import static com.ericsson.oss.apps.util.MetricConstants.METHOD;
import static com.ericsson.oss.apps.util.MetricConstants.METRIC_DESCRIPTIONS;
import static com.ericsson.oss.apps.util.MetricConstants.NCMP_CI_HTTP_REQUESTS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.NCMP_CI_HTTP_REQUESTS_TOTAL_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.NCMP_ENDPOINT;
import static com.ericsson.oss.apps.util.MetricConstants.NCMP_HTTP_REQUESTS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.NCMP_HTTP_REQUESTS_TOTAL_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.NCMP_READS_COMPLETED_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.NCMP_READS_SUBMITTED_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.PARTIAL;
import static com.ericsson.oss.apps.util.MetricConstants.REGISTER;
import static com.ericsson.oss.apps.util.MetricConstants.RULESET_ENDPOINT;
import static com.ericsson.oss.apps.util.MetricConstants.RULESET_ID_ENDPOINT;
import static com.ericsson.oss.apps.util.MetricConstants.SCOPE_ENDPOINT;
import static com.ericsson.oss.apps.util.MetricConstants.SCOPE_ID_ENDPOINT;
import static com.ericsson.oss.apps.util.MetricConstants.SKIPPED;
import static com.ericsson.oss.apps.util.MetricConstants.STATUS;
import static com.ericsson.oss.apps.util.MetricConstants.SUCCEEDED;
import static com.ericsson.oss.apps.util.MetricConstants.TAG;
import static jakarta.ws.rs.HttpMethod.PATCH;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetricService {

    private static final String GET = HttpMethod.GET.toString();
    private static final String POST = HttpMethod.POST.toString();
    private static final String PUT = HttpMethod.PUT.toString();
    private static final String DELETE = HttpMethod.DELETE.toString();
    private static final String HTTP_CODE_200 = "200";
    private static final String HTTP_CODE_201 = "201";
    private static final String HTTP_CODE_202 = "202";
    private static final String HTTP_CODE_204 = "204";
    private static final String HTTP_CODE_400 = "400";
    private static final String HTTP_CODE_401 = "401";
    private static final String HTTP_CODE_403 = "403";
    private static final String HTTP_CODE_404 = "404";
    private static final String HTTP_CODE_409 = "409";
    private static final String HTTP_CODE_500 = "500";
    private static final String HTTP_CODE_503 = "503";

    private final MeterRegistry meterRegistry;
    private final Map<String, Timer.Sample> timedTasks = new ConcurrentHashMap<>();

    @PostConstruct
    @Async
    public void initializeMetrics() {
        log.info("Initializing metrics started");
        increment(EACC_EXECUTIONS_TOTAL, 0, STATUS, SUCCEEDED);
        increment(EACC_EXECUTIONS_MO_TOTAL, 0, STATUS, SUCCEEDED);
        increment(EACC_EXECUTIONS_RULES_TOTAL, 0, STATUS, SUCCEEDED);
        increment(EACC_EXECUTIONS_ATTR_TOTAL, 0, STATUS, SUCCEEDED);

        increment(EACC_EXECUTIONS_TOTAL, 0, STATUS, FAILED);
        increment(EACC_EXECUTIONS_MO_TOTAL, 0, STATUS, FAILED);
        increment(EACC_EXECUTIONS_RULES_TOTAL, 0, STATUS, FAILED);
        increment(EACC_EXECUTIONS_ATTR_TOTAL, 0, STATUS, FAILED);

        increment(EACC_EXECUTIONS_TOTAL, 0, STATUS, PARTIAL);
        increment(EACC_EXECUTIONS_MO_TOTAL, 0, STATUS, PARTIAL);
        increment(EACC_EXECUTIONS_RULES_TOTAL, 0, STATUS, PARTIAL);
        increment(EACC_EXECUTIONS_ATTR_TOTAL, 0, STATUS, PARTIAL);

        increment(EACC_CI_APPLIED_CHANGES_TOTAL, 0, STATUS, SUCCEEDED);
        increment(EACC_CI_APPLIED_CHANGES_TOTAL, 0, STATUS, FAILED);

        increment(EACC_CI_APPLIED_REVERSIONS_TOTAL, 0, STATUS, SUCCEEDED);
        increment(EACC_CI_APPLIED_REVERSIONS_TOTAL, 0, STATUS, FAILED);

        increment(NCMP_READS_COMPLETED_TOTAL, 0, STATUS, SUCCEEDED);
        increment(NCMP_READS_COMPLETED_TOTAL, 0, STATUS, FAILED);
        increment(NCMP_READS_SUBMITTED_TOTAL, 0);

        increment(EACC_EXECUTIONS_AUDIT_QUEUE_COMPLETED_TOTAL, 0, STATUS, SUCCEEDED);
        increment(EACC_EXECUTIONS_AUDIT_QUEUE_COMPLETED_TOTAL, 0, STATUS, FAILED);
        increment(EACC_EXECUTIONS_AUDIT_QUEUE_SUBMITTED_TOTAL, 0);

        registerMetric(Timer.builder(EACC_EXECUTIONS_DURATION_SECONDS), EACC_EXECUTIONS_DURATION_SECONDS, STATUS, SKIPPED);
        registerMetric(Timer.builder(EACC_EXECUTIONS_DURATION_SECONDS), EACC_EXECUTIONS_DURATION_SECONDS, STATUS, SUCCEEDED);
        registerMetric(Timer.builder(EACC_EXECUTIONS_DURATION_SECONDS), EACC_EXECUTIONS_DURATION_SECONDS, STATUS, FAILED);
        registerMetric(Timer.builder(EACC_EXECUTIONS_DURATION_SECONDS), EACC_EXECUTIONS_DURATION_SECONDS, STATUS, PARTIAL);

        registerMetric(Timer.builder(EACC_CI_CHANGES_DURATION_SECONDS), EACC_CI_CHANGES_DURATION_SECONDS);
        registerMetric(Timer.builder(EACC_CI_REVERSIONS_DURATION_SECONDS), EACC_CI_REVERSIONS_DURATION_SECONDS);

        registerHttpMetrics(EACC_HTTP_REQUESTS_TOTAL, EACC_HTTP_REQUESTS_DURATION_SECONDS, JOBS_ENDPOINT, GET,
                HTTP_CODE_200, HTTP_CODE_404, HTTP_CODE_500);
        registerHttpMetrics(EACC_HTTP_REQUESTS_TOTAL, EACC_HTTP_REQUESTS_DURATION_SECONDS, JOBS_ENDPOINT, POST,
                HTTP_CODE_201, HTTP_CODE_400, HTTP_CODE_500);
        registerHttpMetrics(EACC_HTTP_REQUESTS_TOTAL, EACC_HTTP_REQUESTS_DURATION_SECONDS, JOBS_ID_ENDPOINT, PUT,
                HTTP_CODE_200, HTTP_CODE_400, HTTP_CODE_404, HTTP_CODE_409, HTTP_CODE_500);
        registerHttpMetrics(EACC_HTTP_REQUESTS_TOTAL, EACC_HTTP_REQUESTS_DURATION_SECONDS, JOBS_ID_ENDPOINT, DELETE,
                HTTP_CODE_204, HTTP_CODE_400, HTTP_CODE_404, HTTP_CODE_500);
        registerHttpMetrics(EACC_HTTP_REQUESTS_TOTAL, EACC_HTTP_REQUESTS_DURATION_SECONDS, EXECUTIONS_ENDPOINT, GET,
                HTTP_CODE_200, HTTP_CODE_404, HTTP_CODE_500);
        registerHttpMetrics(EACC_HTTP_REQUESTS_TOTAL, EACC_HTTP_REQUESTS_DURATION_SECONDS, EXECUTIONS_AUDIT_RESULTS_ENDPOINT, GET,
                HTTP_CODE_200, HTTP_CODE_404, HTTP_CODE_500);
        registerHttpMetrics(EACC_CI_HTTP_REQUESTS_TOTAL, EACC_CI_HTTP_REQUESTS_DURATION_SECONDS, EXECUTIONS_AUDIT_RESULTS_ENDPOINT, POST,
                HTTP_CODE_202, HTTP_CODE_400, HTTP_CODE_500);
        registerHttpMetrics(EACC_HTTP_REQUESTS_TOTAL, EACC_HTTP_REQUESTS_DURATION_SECONDS, RULESET_ENDPOINT, GET,
                HTTP_CODE_200, HTTP_CODE_500);
        registerHttpMetrics(EACC_HTTP_REQUESTS_TOTAL, EACC_HTTP_REQUESTS_DURATION_SECONDS, RULESET_ENDPOINT, POST,
                HTTP_CODE_201, HTTP_CODE_500);
        registerHttpMetrics(EACC_HTTP_REQUESTS_TOTAL, EACC_HTTP_REQUESTS_DURATION_SECONDS, RULESET_ID_ENDPOINT, GET,
                HTTP_CODE_200, HTTP_CODE_404, HTTP_CODE_500);
        registerHttpMetrics(EACC_HTTP_REQUESTS_TOTAL, EACC_HTTP_REQUESTS_DURATION_SECONDS, RULESET_ID_ENDPOINT, PUT,
                HTTP_CODE_200, HTTP_CODE_404, HTTP_CODE_500);
        registerHttpMetrics(EACC_HTTP_REQUESTS_TOTAL, EACC_HTTP_REQUESTS_DURATION_SECONDS, RULESET_ID_ENDPOINT, DELETE,
                HTTP_CODE_204, HTTP_CODE_404, HTTP_CODE_409, HTTP_CODE_500);

        registerHttpMetrics(EACC_HTTP_REQUESTS_TOTAL, EACC_HTTP_REQUESTS_DURATION_SECONDS, SCOPE_ENDPOINT, GET,
                HTTP_CODE_200, HTTP_CODE_500);
        registerHttpMetrics(EACC_HTTP_REQUESTS_TOTAL, EACC_HTTP_REQUESTS_DURATION_SECONDS, SCOPE_ENDPOINT, POST,
                HTTP_CODE_201, HTTP_CODE_400, HTTP_CODE_500);
        registerHttpMetrics(EACC_HTTP_REQUESTS_TOTAL, EACC_HTTP_REQUESTS_DURATION_SECONDS, SCOPE_ID_ENDPOINT, GET,
                HTTP_CODE_200, HTTP_CODE_400, HTTP_CODE_404, HTTP_CODE_500);
        registerHttpMetrics(EACC_HTTP_REQUESTS_TOTAL, EACC_HTTP_REQUESTS_DURATION_SECONDS, SCOPE_ID_ENDPOINT, PUT,
                HTTP_CODE_200, HTTP_CODE_400, HTTP_CODE_404, HTTP_CODE_409, HTTP_CODE_500);
        registerHttpMetrics(EACC_HTTP_REQUESTS_TOTAL, EACC_HTTP_REQUESTS_DURATION_SECONDS, SCOPE_ID_ENDPOINT, DELETE,
                HTTP_CODE_204, HTTP_CODE_400, HTTP_CODE_404, HTTP_CODE_409, HTTP_CODE_500);

        registerHttpMetrics(APIGATEWAY_SESSIONID_HTTP_REQUESTS_TOTAL, APIGATEWAY_SESSIONID_HTTP_REQUESTS_DURATION_SECONDS, API_GATEWAY_ENDPOINT, POST,
                HTTP_CODE_200, HTTP_CODE_500, HTTP_CODE_503);
        registerHttpMetrics(NCMP_HTTP_REQUESTS_TOTAL, NCMP_HTTP_REQUESTS_TOTAL_DURATION_SECONDS, NCMP_ENDPOINT, GET,
                HTTP_CODE_200, HTTP_CODE_400, HTTP_CODE_401, HTTP_CODE_403, HTTP_CODE_404, HTTP_CODE_500);
        registerHttpMetrics(NCMP_CI_HTTP_REQUESTS_TOTAL, NCMP_CI_HTTP_REQUESTS_TOTAL_DURATION_SECONDS, NCMP_ENDPOINT, PATCH,
                HTTP_CODE_200, HTTP_CODE_400, HTTP_CODE_401, HTTP_CODE_403, HTTP_CODE_404, HTTP_CODE_500);
        log.info("Initializing metrics completed");
    }

    public void increment(final String metricName, final String... tags) {
        increment(metricName, 1, tags);
    }

    public void increment(final String metricName, final double value, final String... tags) {
        Optional<Counter> counter = findMetric(Counter.class, metricName, tags);
        if (counter.isEmpty()) {
            final Optional<Object> result = registerMetric(Counter.builder(metricName), metricName, tags);
            counter = result.map(Counter.class::cast);
        }

        counter.ifPresent(counter1 -> counter1.increment(value));
    }

    public void startTimer(final String keyId, final String metricName, final String... tags) {
        if (findTimer(metricName, tags).isEmpty()) {
            registerMetric(Timer.builder(metricName), metricName, tags);
        }
        timedTasks.put(metricName + keyId, Timer.start(meterRegistry));
    }

    public void stopTimer(final String keyId, final String metricName, final String... tags) {
        final Optional<Timer> timer = findTimer(metricName, tags);
        if (timer.isPresent()) {
            timedTasks.get(metricName + keyId).stop(timer.get());
            timedTasks.remove(metricName + keyId);
        } else {
            log.info("stopTimer(): Timer not found {}, {}, {}", keyId, metricName, tags);
        }
    }

    public Optional<Counter> findCounter(final String metricName, final String... tags) {
        return findMetric(Counter.class, metricName, tags);
    }

    public Optional<Timer> findTimer(final String metricName, final String... tags) {
        return findMetric(Timer.class, metricName, tags);
    }

    private void registerHttpMetrics(final String counterName, final String timerName, final String endPoint, final String method,
                                     final String... httpCodes) {
        for (final String httpCode : httpCodes) {
            registerMetric(Counter.builder(counterName), counterName, ENDPOINT, endPoint, METHOD, method, CODE, httpCode);
            registerMetric(Timer.builder(timerName), timerName, ENDPOINT, endPoint, METHOD, method, CODE, httpCode);
        }
    }

    private <M extends Meter> Optional<M> findMetric(final Class<M> meterClass, final String metricName, final String... tags) {
        final Set<Tag> tagSet = new HashSet<>();
        for (int i = 0; i < tags.length - 1; i += 2) {
            tagSet.add(Tag.of(tags[i], tags[i + 1]));
        }

        final List<M> resultList = meterRegistry.getMeters().stream()
                .filter(meterClass::isInstance)
                .map(meterClass::cast)
                .filter(m -> metricName.equals(m.getId().getName()))
                .filter(m -> tagSet.equals(new HashSet<>(m.getId().getTags()))).toList();

        if (resultList.size() > 1) {
            log.warn("Multiple metrics found for {} {}", metricName, tags);
        }

        return resultList.isEmpty() ? Optional.empty() : Optional.of(resultList.get(0));
    }

    private Optional<Object> registerMetric(final Object builder, final String metricName, final String... tags) {
        Object metricBuilder = builder;
        try {
            for (int i = 0; i < tags.length - 1; i += 2) {
                metricBuilder = metricBuilder.getClass().getMethod(TAG, String.class, String.class)
                        .invoke(metricBuilder, tags[i], tags[i + 1]);
            }

            metricBuilder = metricBuilder.getClass().getMethod(DESCRIPTION, String.class)
                    .invoke(metricBuilder, METRIC_DESCRIPTIONS.get(metricName));
            return Optional.of(builder.getClass().getMethod(REGISTER, MeterRegistry.class).invoke(metricBuilder, meterRegistry));
        } catch (final Exception e) {
            log.error("Metric method invoke error", e);
        }
        return Optional.empty();
    }
}
