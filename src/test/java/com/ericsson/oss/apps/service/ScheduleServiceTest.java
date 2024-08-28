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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.ericsson.oss.apps.model.Job;
import com.ericsson.oss.apps.repository.AuditResultsRepository;
import com.ericsson.oss.apps.repository.ExecutionsRepository;
import com.ericsson.oss.apps.repository.ScopeRepository;

/**
 * Unit tests for {@link ScheduleService} class.
 */
@ExtendWith(MockitoExtension.class)
public class ScheduleServiceTest {

    private static final Long JOB_ID_1 = 1L;

    private static final Long JOB_ID_2 = 2L;

    private static final String JOB_NAME_1 = "Job 1";

    private static final String JOB_NAME_2 = "Job 2";

    private static final String VALID_CRON_EXPRESSION = "0 15 11 ? * *";

    private ScheduleService objectUnderTest;

    private Job job1;
    private Job job2;

    @Mock
    private ExecutionsRepository executionsRepository;

    @Mock
    private AuditResultsRepository auditResultsRepository;

    @Mock
    private ScheduledFuture mockedScheduleFuture;

    @Mock
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Mock
    private Services services;

    @Mock
    private CMService ncmpService;

    @Mock
    private RuleService ruleService;

    @Mock
    private MetricService metricService;

    @Mock
    private ScopeRepository scopeRepository;

    @BeforeEach
    public void setUp() {
        job1 = new Job();
        job1.setId(JOB_ID_1);
        job1.setJobName(JOB_NAME_1);
        job1.setSchedule(VALID_CRON_EXPRESSION);
        job2 = new Job();
        job2.setId(JOB_ID_2);
        job2.setJobName(JOB_NAME_2);
        job2.setSchedule(VALID_CRON_EXPRESSION);
        objectUnderTest = new ScheduleService(threadPoolTaskScheduler, executionsRepository, services,
                auditResultsRepository, scopeRepository);
        assertThat(objectUnderTest.getSchedules()).as("TEST SETUP FAILURE").isEmpty();
    }

    public void setUpMocks() {
        when(threadPoolTaskScheduler.schedule(any(Runnable.class), any(Trigger.class))).thenReturn(mockedScheduleFuture);
        when(services.getCmService()).thenReturn(ncmpService);
        when(services.getRuleService()).thenReturn(ruleService);
        when(services.getMetricService()).thenReturn(metricService);
    }

    @Test
    public void whenScheduleIsRequested_ThenTaskIsScheduled_AndScheduleIsStoredInMemory() {
        setUpMocks();
        objectUnderTest.schedule(job1);
        verify(threadPoolTaskScheduler, times(1)).schedule(any(Runnable.class), any(Trigger.class));
        assertThat(objectUnderTest.getSchedules()).hasSize(1);
    }

    @Test
    public void whenCancellationIsRequested_ThenTaskIsCancelled_AndScheduleIsRemovedFromMemory() {
        setUpMocks();
        objectUnderTest.schedule(job1);
        when(mockedScheduleFuture.cancel(false)).thenReturn(true);

        objectUnderTest.cancel(JOB_ID_1, JOB_NAME_1);
        verify(mockedScheduleFuture, times(1)).cancel(false);
        assertThat(objectUnderTest.getSchedules()).isEmpty();
    }

    @Test
    public void whenCancellationIsRequestedForNonExistingJob_ThenNoExceptionIsThrown() {
        assertThatCode(()->  objectUnderTest.cancel(99L, "some_not_scheduled_job")).doesNotThrowAnyException();
    }

    @Test
    public void whenMultipleSchedulesAreRequested_AndAScheduleIsThenCancelled_ThenRemainingSchedulesAreStillInMemory() {
        setUpMocks();
        objectUnderTest.schedule(job1);
        objectUnderTest.schedule(job2);
        assertThat(objectUnderTest.getSchedules()).hasSize(2);

        when(mockedScheduleFuture.cancel(false)).thenReturn(true);
        objectUnderTest.cancel(JOB_ID_1, JOB_NAME_1);
        assertThat(objectUnderTest.getSchedules()).hasSize(1);
    }

    @Test
    public void whenDeleteJobIsRequested_thenScheduleIsRemovedFromMemory() {
        setUpMocks();
        objectUnderTest.schedule(job1);
        assertThat(objectUnderTest.getSchedules()).as("TEST SETUP FAILURE").hasSize(1);

        objectUnderTest.cancel(JOB_ID_1, JOB_NAME_1);
        assertThat(objectUnderTest.getSchedules()).isEmpty();
    }

}
