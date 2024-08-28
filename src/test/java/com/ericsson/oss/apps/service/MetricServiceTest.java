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

import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.ENDPOINT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Unit tests for {@link MetricServiceConfig} class.
 */
@ExtendWith(MockitoExtension.class)
class MetricServiceTest {

    private static final String MY_ENDPOINT = "/v1/my_endpoint";
    private MeterRegistry meterRegistry;
    private MetricService objectUnderTest;

    @BeforeEach
    public void setup() {
        meterRegistry = new SimpleMeterRegistry();
        objectUnderTest = new MetricService(meterRegistry);
    }

    @AfterEach
    void clearMetrics() {
        meterRegistry.clear();
    }

    @Test
    void whenCounterMetricIsIncrementedInitially_thenCounterMetricIsCreatedAndIncremented() {
        objectUnderTest.increment(EACC_EXECUTIONS_TOTAL, ENDPOINT, MY_ENDPOINT);
        assertThat(objectUnderTest.findCounter(EACC_EXECUTIONS_TOTAL, ENDPOINT, MY_ENDPOINT).get().count()).isEqualTo(1.0);
    }

    @Test
    void whenCounterMetricIsIncrementedWhichAlreadyExists_thenCounterMetricIsFoundAndIncremented() {
        objectUnderTest.increment(EACC_EXECUTIONS_TOTAL, ENDPOINT, MY_ENDPOINT);
        objectUnderTest.increment(EACC_EXECUTIONS_TOTAL, ENDPOINT, MY_ENDPOINT);
        assertThat(objectUnderTest.findCounter(EACC_EXECUTIONS_TOTAL, ENDPOINT, MY_ENDPOINT).get().count()).isEqualTo(2.0);
    }

    @Test
    void whenCounterMetricIsIncrementedWithValue_thenCounterMetricIsIncrementedWithThatValue() {
        objectUnderTest.increment(EACC_EXECUTIONS_TOTAL, 5, ENDPOINT, MY_ENDPOINT);
        assertThat(objectUnderTest.findCounter(EACC_EXECUTIONS_TOTAL, ENDPOINT, MY_ENDPOINT).get().count()).isEqualTo(5.0);
    }

    @Test
    void whenFindCounterMetricCalledForNonExistingCounter_thenEmptyOptionalIsReturned() {
        assertThat(objectUnderTest.findCounter(EACC_EXECUTIONS_TOTAL)).isEmpty();
    }

    @Test
    void whenTimerMetricIsStarted_thenTimerMetricIsCreatedAndTimerIsZero() {
        objectUnderTest.startTimer(String.valueOf(hashCode()), EACC_EXECUTIONS_DURATION_SECONDS, ENDPOINT, MY_ENDPOINT);
        assertThat(objectUnderTest.findTimer(EACC_EXECUTIONS_DURATION_SECONDS, ENDPOINT, MY_ENDPOINT).get().totalTime(TimeUnit.MILLISECONDS))
                .isZero();
    }

    @Test
    void whenTimerMetricIsStartedAndStoppedInitially_thenTimerMetricIsCreatedAndTimerIsGreaterThanZero() {
        objectUnderTest.startTimer(String.valueOf(hashCode()), EACC_EXECUTIONS_DURATION_SECONDS, ENDPOINT, MY_ENDPOINT);
        objectUnderTest.stopTimer(String.valueOf(hashCode()), EACC_EXECUTIONS_DURATION_SECONDS, ENDPOINT, MY_ENDPOINT);
        assertThat(objectUnderTest.findTimer(EACC_EXECUTIONS_DURATION_SECONDS, ENDPOINT, MY_ENDPOINT).get().totalTime(TimeUnit.MILLISECONDS))
                .isGreaterThan(0.0);
    }

    @Test
    void whenTimerMetricIsStartedAndStoppedWhichAlreadyExists_thenTimerMetricIsFoundAndTimerIsGreaterThanTheInitialTime() {
        objectUnderTest.startTimer(String.valueOf(hashCode()), EACC_EXECUTIONS_DURATION_SECONDS, ENDPOINT, MY_ENDPOINT);
        objectUnderTest.stopTimer(String.valueOf(hashCode()), EACC_EXECUTIONS_DURATION_SECONDS, ENDPOINT, MY_ENDPOINT);
        final double timeAfterFirstStartStop = objectUnderTest.findTimer(EACC_EXECUTIONS_DURATION_SECONDS, ENDPOINT, MY_ENDPOINT).get()
                .totalTime(TimeUnit.MILLISECONDS);
        objectUnderTest.startTimer(String.valueOf(hashCode()), EACC_EXECUTIONS_DURATION_SECONDS, ENDPOINT, MY_ENDPOINT);
        objectUnderTest.stopTimer(String.valueOf(hashCode()), EACC_EXECUTIONS_DURATION_SECONDS, ENDPOINT, MY_ENDPOINT);
        assertThat(objectUnderTest.findTimer(EACC_EXECUTIONS_DURATION_SECONDS, ENDPOINT, MY_ENDPOINT).get().totalTime(TimeUnit.MILLISECONDS))
                .isGreaterThan(timeAfterFirstStartStop);
    }

    @Test
    void whenTimerMetricIsStartedTwice_thenTimerIsStillZero() {
        objectUnderTest.startTimer(String.valueOf(hashCode()), EACC_EXECUTIONS_DURATION_SECONDS, ENDPOINT, MY_ENDPOINT);
        objectUnderTest.startTimer(String.valueOf(hashCode()), EACC_EXECUTIONS_DURATION_SECONDS, ENDPOINT, MY_ENDPOINT);
        assertThat(objectUnderTest.findTimer(EACC_EXECUTIONS_DURATION_SECONDS, ENDPOINT, MY_ENDPOINT).get().totalTime(TimeUnit.MILLISECONDS))
                .isZero();
    }

    @Test
    void whenFindTimerMetricCalledForNonExistingCounter_thenEmptyOptionalIsReturned() {
        assertThat(objectUnderTest.findTimer(EACC_EXECUTIONS_DURATION_SECONDS)).isEmpty();
    }

    @Test
    void whenMetricsAreInitialized_thenVerifyCountTotal() {
        objectUnderTest.initializeMetrics();
        assertThat(meterRegistry.getMeters()).hasSize(172);
    }
}