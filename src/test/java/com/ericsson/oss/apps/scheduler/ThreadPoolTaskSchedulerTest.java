
/*******************************************************************************
 * COPYRIGHT Ericsson 2018 - 2023
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
package com.ericsson.oss.apps.scheduler;

import static com.ericsson.oss.apps.util.Constants.OSS_TIME_ZONE;

import java.time.LocalTime;
import java.time.ZoneId;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

/**
 * Unit tests for {@link ThreadPoolTaskScheduler}.
 */
@ExtendWith(SoftAssertionsExtension.class)
public class ThreadPoolTaskSchedulerTest {

    /**
     * Logger for the class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ThreadPoolTaskSchedulerTest.class);

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Test
    public void whenCreatingAnInvalidScheduledActivity_thenExceptionIsThrown() {
        final String[] invalidCronExpressions = {
                "2 ? * MON,TUE,WED,THU,FRI *",
                "*",
                "0 0 */6 ? * INVALID *",
                "?? ? ** ? * MON,TUE,WED,THU,FRI *"
        };

        for (final String invalidCronExpression : invalidCronExpressions) {
            softly.assertThatCode(() -> threadPoolTaskScheduler.schedule(getTask(invalidCronExpression),
                    new CronTrigger(invalidCronExpression, ZoneId.of(OSS_TIME_ZONE))))
                    .as("'%s' should be invalid", invalidCronExpression)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    public Runnable getTask(final String schedule) {
        return () -> LOG.info("{}:: ScheduledTask: {}, Schedule: {} Time: {}", getClass().getSimpleName(),
                Thread.currentThread().getName(), schedule, LocalTime.now());
    }
}
