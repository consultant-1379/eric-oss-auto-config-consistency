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

import static com.ericsson.oss.apps.util.MetricConstants.APIGATEWAY_SESSIONID_HTTP_REQUESTS;
import static com.ericsson.oss.apps.util.MetricConstants.API_GATEWAY_ENDPOINT;
import static com.ericsson.oss.apps.util.MetricConstants.CODE;
import static com.ericsson.oss.apps.util.MetricConstants.CONSISTENCY_ACTUATOR_PREFIX;
import static com.ericsson.oss.apps.util.MetricConstants.CONSISTENCY_CHECK_PREFIX;
import static com.ericsson.oss.apps.util.MetricConstants.ENDPOINT;
import static com.ericsson.oss.apps.util.MetricConstants.HTTP_CLIENT_REQUEST;
import static com.ericsson.oss.apps.util.MetricConstants.HTTP_SERVER_REQUEST;
import static com.ericsson.oss.apps.util.MetricConstants.METHOD;
import static com.ericsson.oss.apps.util.MetricConstants.NCMP_ENDPOINT;
import static com.ericsson.oss.apps.util.MetricConstants.NCMP_HTTP_REQUESTS;
import static com.ericsson.oss.apps.util.MetricConstants.STATUS;
import static com.ericsson.oss.apps.util.MetricConstants.URI;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class MetricServiceConfig {

    @Bean
    public MeterFilter meterFilter() {
        return new MeterFilter() {
            @Override
            public Meter.Id map(final Meter.Id id) {
                final String metricName = id.getName();
                if (id.getName().equals(HTTP_SERVER_REQUEST) || id.getName().equals(HTTP_CLIENT_REQUEST)) {
                    return processMetric(id, metricName);
                }
                return id;
            }
        };
    }

    private Meter.Id processMetric(final Meter.Id id, final String name) {
        String metricName = name;
        final List<Tag> newTags = new ArrayList<>();
        String prefixName = CONSISTENCY_CHECK_PREFIX;
        for (final Tag tag : id.getTags()) {
            switch (tag.getKey()) {
                case URI:
                    String baseEndPoint = tag.getValue().split("\\?")[0];
                    if (tag.getValue().startsWith("/actuator")) {
                        prefixName = CONSISTENCY_ACTUATOR_PREFIX;
                    }
                    if (tag.getValue().startsWith("/ncmp/v1/ch/")) {
                        metricName = NCMP_HTTP_REQUESTS;
                        baseEndPoint = NCMP_ENDPOINT;
                    }
                    if (tag.getValue().contains(API_GATEWAY_ENDPOINT)) {
                        metricName = APIGATEWAY_SESSIONID_HTTP_REQUESTS;
                        baseEndPoint = API_GATEWAY_ENDPOINT;
                    }
                    newTags.add(Tag.of(ENDPOINT, baseEndPoint));
                    break;
                case STATUS:
                    newTags.add(Tag.of(CODE, tag.getValue()));
                    break;
                case METHOD:
                    newTags.add(tag);
                    break;
                default:
            }
        }
        if (id.getName().equals(HTTP_SERVER_REQUEST)) {
            metricName = "http.requests";
        }
        return id.replaceTags(newTags).withName(prefixName + metricName + ".duration");
    }
}
