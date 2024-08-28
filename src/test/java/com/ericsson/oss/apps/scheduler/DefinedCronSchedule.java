
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

package com.ericsson.oss.apps.scheduler;

/**
 * ENUM defining some default CRON schedules.
 */
public enum DefinedCronSchedule {

    EVERY_FIFTEEN_MINUTES("0 0/15 * 1/1 * ?"),
    EVERY_MINUTE("0 * * * * *"),
    EVERY_HOUR("@hourly"),
    EVERY_DAY("@daily"),
    ANNUALLY("@yearly"),
    MONTHLY("@monthly"),
    WEEKLY("@weekly");

    private final String cronSchedule;

    DefinedCronSchedule(final String cronSchedule) {
        this.cronSchedule = cronSchedule;
    }

    public String getCronSchedule() {
        return cronSchedule;
    }
}
