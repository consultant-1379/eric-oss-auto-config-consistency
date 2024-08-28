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

import static com.ericsson.oss.apps.util.Constants.OSS_TIME_ZONE;
import static com.ericsson.oss.apps.util.Constants.UTC;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import com.ericsson.oss.apps.config.ThreadPoolTaskSchedulerBean;
import com.ericsson.oss.apps.executor.AuditConfig;
import com.ericsson.oss.apps.executor.ConsistencyCheckExecutor;
import com.ericsson.oss.apps.model.Job;
import com.ericsson.oss.apps.repository.AuditResultsRepository;
import com.ericsson.oss.apps.repository.ExecutionsRepository;
import com.ericsson.oss.apps.repository.ScopeRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * A utility class to handle the job schedules and their in memory references.
 * 
 * @see ThreadPoolTaskSchedulerBean for configuration of this.
 */

@Slf4j
@Service
public class ScheduleService {

    private final Map<Long, Future> schedules = new HashMap<>();

    private final AuditResultsRepository auditResultsRepository;
    private final ExecutionsRepository executionsRepository;
    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;
    private final ScopeRepository scopeRepository;
    private final Services services;

    @Autowired
    private AuditConfig auditConfig;

    @Autowired
    private RetryTemplate retryTemplate;

    @Autowired
    public ScheduleService(final ThreadPoolTaskScheduler threadPoolTaskScheduler,
            final ExecutionsRepository executionsRepository, final Services services,
            final AuditResultsRepository auditResultsRepository, final ScopeRepository scopeRepository) {
        this.threadPoolTaskScheduler = threadPoolTaskScheduler;
        this.services = services;
        this.executionsRepository = executionsRepository;
        this.auditResultsRepository = auditResultsRepository;
        this.scopeRepository = scopeRepository;
    }

    /**
     * A method for scheduling the executions of the given Job.
     * 
     * @param job
     *            the Job received
     */
    public void schedule(final Job job) {
        log.info("Scheduling {}", job);
        ConsistencyCheckExecutor.setRetryTemplate(retryTemplate);
        final ConsistencyCheckExecutor consistencyCheckExecutor = new ConsistencyCheckExecutor(auditResultsRepository, job,
                executionsRepository, services, scopeRepository, auditConfig);
        final Future schedule = threadPoolTaskScheduler.schedule(consistencyCheckExecutor,
                new CronTrigger(job.getSchedule(), ZoneId.of(UTC)));

        log.info("{}:: {schedule: {}, ossTimeZone: {}, threadPoolSize: {}} applied",
                getClass().getSimpleName(), job.getSchedule(),
                OSS_TIME_ZONE, threadPoolTaskScheduler.getPoolSize());

        schedules.put(job.getId(), schedule);
    }

    /**
     * A method for cancelling the executions of the given Job.
     *
     * @param jobId
     *            the Job ID
     * @param jobName
     *            the Job Name
     */
    public void cancel(final Long jobId, final String jobName) {
        log.info("Cancelling {}", jobName);
        if (Objects.isNull(schedules.get(jobId))) {
            log.info("Job already cancelled {}", jobName);
        } else {
            schedules.get(jobId).cancel(false);
            schedules.remove(jobId);
            log.info("Successfully cancelled {}", jobName);
        }
    }

    Map<Long, Future> getSchedules() {
        return new HashMap<>(schedules);
    }

}
