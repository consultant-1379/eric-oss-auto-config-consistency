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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.oss.apps.api.model.EaccJob;
import com.ericsson.oss.apps.model.Job;
import com.ericsson.oss.apps.repository.JobRepository;
import com.ericsson.oss.apps.scheduler.SchedulerOperation;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JobService {

    private final JobRepository jobRepository;
    private final ScheduleService scheduleService;

    @Autowired
    public JobService(final JobRepository jobRepository, final ScheduleService scheduleService) {
        this.jobRepository = jobRepository;
        this.scheduleService = scheduleService;
    }

    @Async
    @EventListener
    public void runOnStartup(final ApplicationReadyEvent event) {
        log.info("Scheduling services from persistence storage started");
        final List<Job> jobs = jobRepository.findAll();
        log.info("Scheduling {} jobs from persistence storage", jobs.size());
        for (final Job job : jobs) {
            scheduleService.schedule(job);
            log.info("Scheduling job {}", job.getJobName());
        }
        log.info("Scheduling services from persistence storage completed");
    }

    @Retryable(retryFor = {
            DataAccessException.class, TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}",
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public EaccJob create(final EaccJob eaccJob) {
        final Job job = new Job();
        job.setJobName(eaccJob.getJobName());
        job.setSchedule(eaccJob.getSchedule());
        job.setRuleSetName(eaccJob.getRulesetName());
        job.setScopeName(eaccJob.getScopeName());
        final var persistedJob = jobRepository.save(job);
        log.info("Job saved with Job Name: {}", persistedJob.getJobName());

        updateEACCSchedules(persistedJob, SchedulerOperation.CREATE);
        return eaccJob;
    }

    @Retryable(retryFor = {
            DataAccessException.class, TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}",
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public EaccJob update(final EaccJob eaccJob) {
        final Optional<Job> optionalJob = jobRepository.findByJobName(eaccJob.getJobName());
        if (optionalJob.isPresent()) {
            final Job job = optionalJob.get();
            job.setJobName(eaccJob.getJobName());
            job.setSchedule(eaccJob.getSchedule());
            job.setRuleSetName(eaccJob.getRulesetName());
            job.setScopeName(eaccJob.getScopeName());
            final var persistedJob = jobRepository.save(job);
            log.info("Job updated with Job Name: {}", persistedJob.getJobName());

            updateEACCSchedules(persistedJob, SchedulerOperation.UPDATE);
            return eaccJob;
        } else {
            log.error("Job does not exist. Job name: {}", eaccJob.getJobName());
            return null;
        }
    }

    @Retryable(retryFor = {
            DataAccessException.class, TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}",
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public List<EaccJob> getAllJobs() {
        log.debug("Get all jobs.");
        final List<Job> jobList = jobRepository.findAll();
        return jobList.stream()
                .map(config -> {
                    final EaccJob eaccJob = new EaccJob();
                    eaccJob.setJobName(config.getJobName());
                    eaccJob.setSchedule(config.getSchedule());
                    eaccJob.setRulesetName(config.getRuleSetName());
                    eaccJob.setScopeName(config.getScopeName());
                    return eaccJob;
                }).collect(Collectors.toList());
    }

    @Retryable(retryFor = {
            DataAccessException.class, TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}",
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    @Transactional
    public void deleteJobByName(final String jobName) {
        final Optional<Job> optionalJob = jobRepository.findByJobName(jobName);
        if (optionalJob.isPresent()) {
            final Job job = optionalJob.get();
            jobRepository.deleteByJobName(jobName);
            updateEACCSchedules(job, SchedulerOperation.DELETE);
        } else {
            log.error("Job does not exist. Job name: {}", jobName);
        }
    }

    @Retryable(retryFor = {
            DataAccessException.class, TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}",
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public boolean existsByName(final String jobName) {
        return jobRepository.existsByJobName(jobName);
    }

    @Retryable(retryFor = {
            DataAccessException.class, TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}",
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public boolean isRuleSetInUse(final String rulesetName) {
        return jobRepository.findAll()
                .stream()
                .map(Job::getRuleSetName).anyMatch(rulesetName::equals);
    }

    @Retryable(retryFor = {
            DataAccessException.class, TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}",
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public boolean isScopeInUse(final String scopeName) {
        return jobRepository.findAll()
                .stream()
                .map(Job::getScopeName).anyMatch(scopeName::equals);
    }

    void updateEACCSchedules(final Job job, final SchedulerOperation operation) {
        if (operation.equals(SchedulerOperation.CREATE)) {
            scheduleService.schedule(job);
            log.info("Successfully created the schedule: '{}', for job: '{}'", job.getSchedule(), job.getJobName());
        } else if (operation.equals(SchedulerOperation.DELETE)) {
            scheduleService.cancel(job.getId(), job.getJobName());
            log.info("Successfully deleted the schedule: '{}', for job: '{}'", job.getSchedule(), job.getJobName());
        } else if (operation.equals(SchedulerOperation.UPDATE)) {
            scheduleService.cancel(job.getId(), job.getJobName());
            scheduleService.schedule(job);
            log.info("Successfully changed the schedule: '{}', for job: '{}'", job.getSchedule(), job.getJobName());
        }
    }
}
