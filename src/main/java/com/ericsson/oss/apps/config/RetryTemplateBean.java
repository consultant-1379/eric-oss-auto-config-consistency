/*******************************************************************************
 * COPYRIGHT Ericsson 2024
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

package com.ericsson.oss.apps.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RetryTemplateBean {
    @Bean
    @Primary
    public RetryTemplate retryTemplate(@Value("${database.retry.scheduleInitiated.maxAttempts}") final int maxAttempts,
            @Value("${database.retry.scheduleInitiated.delay}") final Long delay,
            @Value("${database.retry.scheduleInitiated.maxDelay}") final Long maxDelay,
            @Value("${database.retry.scheduleInitiated.delayMultiplier}") final int multiplier) {
        return RetryTemplate.builder()
                .exponentialBackoff(delay, multiplier, maxDelay)
                .maxAttempts(maxAttempts)
                .build();
    }

    @Bean("ncmpRetryTemplate")
    public RetryTemplate ncmpRetryTemplate(@Value("${ncmpCalls.retry.maxAttempts}") final int maxAttempts,
            @Value("${ncmpCalls.retry.delay}") final Long delay,
            @Value("${ncmpCalls.retry.maxDelay}") final Long maxDelay,
            @Value("${ncmpCalls.retry.delayMultiplier}") final int multiplier) {
        return RetryTemplate.builder()
                .exponentialBackoff(delay, multiplier, maxDelay)
                .maxAttempts(maxAttempts)
                .build();
    }
}
