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

package com.ericsson.oss.apps.validation.annotations.schedule;

import org.springframework.scheduling.support.CronExpression;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ScheduleValidator implements ConstraintValidator<ScheduleValidation, String> {

    @Override
    public boolean isValid(final String scheduleExpression, final ConstraintValidatorContext constraintValidatorContext) {
        return CronExpression.isValidExpression(scheduleExpression);
    }
}
