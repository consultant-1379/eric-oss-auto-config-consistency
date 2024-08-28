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

import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_RULESET_NAME;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_RULESET_NAME_TWO;
import static com.ericsson.oss.apps.util.TestDefaults.SCOPE_ATHLONE;
import static com.ericsson.oss.apps.util.TestDefaults.SCOPE_DUBLIN;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionException;

import com.ericsson.oss.apps.CoreApplication;
import com.ericsson.oss.apps.CoreApplicationTest;
import com.ericsson.oss.apps.api.model.EaccJob;
import com.ericsson.oss.apps.model.Job;
import com.ericsson.oss.apps.repository.JobRepository;

/**
 * Unit tests for {@link JobService} class.
 */
@ActiveProfiles("test")
@AutoConfigureObservability
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = { CoreApplication.class, CoreApplicationTest.class })
public class JobServiceTest {

    private static final Long JOB_ID_1 = 1L;

    private static final Long JOB_ID_2 = 2L;

    private static final String JOB_NAME_1 = "Job 1";

    private static final String JOB_NAME_2 = "Job 2";

    private static final String VALID_CRON_EXPRESSION = "0 15 11 ? * *";
    @Autowired
    private JobService objectUnderTest;

    @MockBean
    private JobRepository jobRepository;

    @MockBean
    private ScheduleService scheduleService;

    @MockBean
    ApplicationReadyEvent applicationReadyEvent;

    @Value("${database.retry.userInitiated.maxAttempts}")
    private int maxAttempts;


    @Test
    public void whenSaveJob_thenShouldSaveJobToDatabase() {
        final Job defaultConfig = createDefaultDomainConfigModel().get(0);
        defaultConfig.setId(1L);
        when(jobRepository.save(any(Job.class))).thenReturn(defaultConfig);
        final EaccJob config = createDefaultRestConfigModel().get(0);
        objectUnderTest.create(config);
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    public void whenGetAllJobs_thenAllJobsAreReturned() {
        when(jobRepository.findAll()).thenReturn(createDefaultDomainConfigModel());
        final List<EaccJob> expected = createDefaultRestConfigModel();
        final List<EaccJob> actual = objectUnderTest.getAllJobs();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void whenCheckingIfJobExists_andJobExists_thenReturnsTrue() {
        when(jobRepository.existsByJobName(any())).thenReturn(Boolean.TRUE);
        assertThat(objectUnderTest.existsByName("jobName")).isTrue();
    }

    @Test
    public void whenCheckingIfJobExists_andJobDoesNotExist_thenReturnsFalse() {
        when(jobRepository.existsByJobName(any())).thenReturn(Boolean.FALSE);
        assertThat(objectUnderTest.existsByName("jobName")).isFalse();
    }

    @Test
    public void whenCheckingIsRuleSetInUse_andRuleSetExists_thenReturnsTrue() {
        when(jobRepository.findAll()).thenReturn(createDefaultDomainConfigModel());
        assertThat(objectUnderTest.isRuleSetInUse(DEFAULT_RULESET_NAME)).isTrue();
    }

    @Test
    public void whenCheckingIsRuleSetInUse_andRuleSetDoesNotExists_thenReturnsFalse() {
        when(jobRepository.findAll()).thenReturn(createDefaultDomainConfigModel());
        assertThat(objectUnderTest.isRuleSetInUse(DEFAULT_RULESET_NAME_TWO)).isFalse();
    }

    @Test
    public void whenCheckingIsScopeInUse_andScopeExists_thenReturnsTrue() {
        when(jobRepository.findAll()).thenReturn(createDefaultDomainConfigModel());
        assertThat(objectUnderTest.isScopeInUse(SCOPE_ATHLONE)).isTrue();
    }

    @Test
    public void whenCheckingIsScopeInUse_andScopeDoesNotExists_thenReturnsFalse() {
        when(jobRepository.findAll()).thenReturn(createDefaultDomainConfigModel());
        assertThat(objectUnderTest.isScopeInUse(SCOPE_DUBLIN)).isFalse();
    }

    @Test
    public void whenUpdateJob_thenShouldSaveJobToDatabase() {
        final Job defaultConfig = createDefaultDomainConfigModel().get(0);
        defaultConfig.setId(1L);

        when(jobRepository.findByJobName(any(String.class))).thenReturn(Optional.of(defaultConfig));
        when(jobRepository.save(any(Job.class))).thenReturn(defaultConfig);
        final EaccJob config = createDefaultRestConfigModel().get(0);
        objectUnderTest.update(config);
        verify(jobRepository).save(any(Job.class));
        verify(scheduleService).cancel(defaultConfig.getId(), defaultConfig.getJobName());
        verify(scheduleService).schedule(defaultConfig);
    }

    @Test
    public void whenUpdateJob_andJobDoesNotExist_thenShouldNotSave() {
        final EaccJob config = createDefaultRestConfigModel().get(0);
        config.setScopeName(SCOPE_DUBLIN);
        final EaccJob result = objectUnderTest.update(config);
        assertThat(result).isNull();
    }

    private List<EaccJob> createDefaultRestConfigModel() {
        final EaccJob config = new EaccJob();
        config.setJobName("job1");
        config.setScopeName(SCOPE_ATHLONE);
        config.setSchedule("daily");
        config.setSchedule("0 */1 * * * *");
        config.setRulesetName(DEFAULT_RULESET_NAME);
        return Collections.singletonList(config);
    }

    private List<Job> createDefaultDomainConfigModel() {
        final Job job = new Job();
        job.setJobName("job1");
        job.setScopeName(SCOPE_ATHLONE);
        job.setSchedule("daily");
        job.setSchedule("0 */1 * * * *");
        job.setRuleSetName(DEFAULT_RULESET_NAME);
        return Collections.singletonList(job);
    }

    @Test
    public void whenStartupAndNoJobsDefined_thenNoSchedulesCreatedInMemory() {
        when(jobRepository.findAll()).thenReturn(new ArrayList<>());
        objectUnderTest.runOnStartup(applicationReadyEvent);
        assertThat(jobRepository.findAll()).isEmpty();
    }

    @Test
    public void whenStartupAndJobsDefined_thenSchedulesCreatedInMemory() {
        final Job job1 = new Job();
        job1.setId(JOB_ID_1);
        job1.setJobName(JOB_NAME_1);
        job1.setSchedule(VALID_CRON_EXPRESSION);

        final Job job2 = new Job();
        job2.setId(JOB_ID_2);
        job2.setJobName(JOB_NAME_2);
        job2.setSchedule(VALID_CRON_EXPRESSION);

        when(jobRepository.findAll()).thenReturn(Arrays.asList(job1, job2));
        objectUnderTest.runOnStartup(applicationReadyEvent);
        await().atMost(1, SECONDS).until(() -> true);
        verify(scheduleService, times(2)).schedule(any(Job.class));
    }

    @Test
    public void whenCreateJobMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(jobRepository.save(any())).thenThrow(CannotCreateTransactionException.class);
        assertThatThrownBy(() -> objectUnderTest.create(new EaccJob())).isInstanceOf(TransactionException.class);
        verify(jobRepository, times(maxAttempts)).save(any());
    }

    @Test
    public void whenUpdateJobMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(jobRepository.findByJobName(any())).thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.update(new EaccJob())).isInstanceOf(DataAccessException.class);
        verify(jobRepository, times(maxAttempts)).findByJobName(any());
    }

    @Test
    public void whenGetAllJobsMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(jobRepository.findAll()).thenThrow(CannotCreateTransactionException.class).thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.getAllJobs()).isInstanceOf(DataAccessException.class);
        verify(jobRepository, times(maxAttempts)).findAll();
    }

    @Test
    public void whenDeleteJobByNameMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(jobRepository.findByJobName(any())).thenThrow(CannotCreateTransactionException.class).thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.deleteJobByName("jobname")).isInstanceOf(DataAccessException.class);
        verify(jobRepository, times(maxAttempts)).findByJobName(any());
    }

    @Test
    public void whenExistsByNameMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(jobRepository.existsByJobName(any())).thenThrow(CannotCreateTransactionException.class).thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.existsByName("jobname")).isInstanceOf(DataAccessException.class);
        verify(jobRepository, times(maxAttempts)).existsByJobName(any());
    }

    @Test
    public void whenIsRuleSetInUseMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(jobRepository.findAll()).thenThrow(CannotCreateTransactionException.class).thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.isRuleSetInUse("rulesetName")).isInstanceOf(DataAccessException.class);
        verify(jobRepository, times(maxAttempts)).findAll();
    }

    @Test
    public void whenIsScopeInUseMethodCalled_andDatabaseIssuesOccur_thenMethodIsRetried() {
        when(jobRepository.findAll()).thenThrow(CannotCreateTransactionException.class).thenThrow(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> objectUnderTest.isScopeInUse("scopeName")).isInstanceOf(DataAccessException.class);
        verify(jobRepository, times(maxAttempts)).findAll();
    }
}
