/*******************************************************************************
 * COPYRIGHT Ericsson 2021 - 2024
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

package com.ericsson.oss.apps;

import static com.ericsson.oss.apps.util.Constants.VERSION_PATH;

import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.ericsson.oss.apps.util.AuditLogInterceptor;

/**
 * Core Application, the starting point of the application.
 */
@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing
@EnableRetry
@EnableAsync
public class CoreApplication {

    private static final String DELETE = "DELETE";
    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String PUT = "PUT";

    @Value("${frontEnd.corsValueGasUrl:corsValue}")
    private String corsValueGasUrl;

    /**
     * Main entry point of the application.
     *
     * @param args
     *            Command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(CoreApplication.class, args);
    }

    /**
     * Configuration bean for Web MVC.
     *
     * @param auditLogInterceptors
     *            auditLogInterceptor
     * @return WebMvcConfigurer
     */
    @Bean
    public WebMvcConfigurer webConfigurer(final ObjectProvider<AuditLogInterceptor> auditLogInterceptors) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(final CorsRegistry registry) {
                registry.addMapping(VERSION_PATH + "/executions/**").allowedOrigins(corsValueGasUrl);
                registry.addMapping(VERSION_PATH + "/jobs/**").allowedOrigins(corsValueGasUrl).allowedMethods(GET, POST, PUT, DELETE);
                registry.addMapping(VERSION_PATH + "/rulesets/**").allowedOrigins(corsValueGasUrl).allowedMethods(GET, PUT, POST, DELETE);
                registry.addMapping(VERSION_PATH + "/scopes/**").allowedOrigins(corsValueGasUrl).allowedMethods(GET, PUT, POST, DELETE);
            }

            @Override
            public void addInterceptors(final InterceptorRegistry registry) {
                final Optional<AuditLogInterceptor> auditLogInterceptor = auditLogInterceptors.orderedStream().findAny();
                if (auditLogInterceptor.isPresent()) {
                    final AuditLogInterceptor interceptor = auditLogInterceptor.get();
                    registry.addInterceptor(interceptor)
                            .addPathPatterns(VERSION_PATH + "/**");
                }
            }
        };
    }

}