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

package com.ericsson.oss.apps.model;

import static com.ericsson.oss.apps.util.Constants.BLANK_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.BLANK_RULESET_ERROR;
import static com.ericsson.oss.apps.util.Constants.BLANK_SCHEDULE_ERROR;
import static com.ericsson.oss.apps.util.Constants.BLANK_SCOPENAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.INVALID_CRON_SCHEDULE_ERROR;
import static com.ericsson.oss.apps.util.Constants.JOBNAME_LENGTH_ERROR;
import static com.ericsson.oss.apps.util.Constants.NAME_REGEX;
import static com.ericsson.oss.apps.util.Constants.REGEX_JOBNAME_ERROR;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ericsson.oss.apps.validation.annotations.schedule.ScheduleValidation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity
@Table(name = "job_settings")
@Data
@EntityListeners(AuditingEntityListener.class)
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = BLANK_JOBNAME_ERROR)
    @Pattern(regexp = NAME_REGEX, message = REGEX_JOBNAME_ERROR)
    @Size(min = 4, max = 100, message = JOBNAME_LENGTH_ERROR)
    @NotNull
    @Column(nullable = false, unique = true)
    private String jobName;

    @NotBlank(message = BLANK_SCOPENAME_ERROR)
    @NotNull
    @Column(nullable = false)
    private String scopeName;

    @NotBlank(message = BLANK_SCHEDULE_ERROR)
    @ScheduleValidation(message = INVALID_CRON_SCHEDULE_ERROR)
    @NotNull
    @Column(nullable = false)
    private String schedule;

    @NotBlank(message = BLANK_RULESET_ERROR)
    @NotNull
    @Column(nullable = false)
    private String ruleSetName;
}
