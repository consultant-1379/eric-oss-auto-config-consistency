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

package com.ericsson.oss.apps.aspects;

import static com.ericsson.oss.apps.util.MetricConstants.APIGATEWAY_SESSIONID_HTTP_REQUESTS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.API_GATEWAY_ENDPOINT;
import static com.ericsson.oss.apps.util.MetricConstants.CODE;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_CI_HTTP_REQUESTS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_HTTP_REQUESTS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.ENDPOINT;
import static com.ericsson.oss.apps.util.MetricConstants.METHOD;
import static com.ericsson.oss.apps.util.MetricConstants.NCMP_CI_HTTP_REQUESTS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.NCMP_ENDPOINT;
import static com.ericsson.oss.apps.util.MetricConstants.NCMP_HTTP_REQUESTS_TOTAL;

import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ericsson.oss.apps.api.ExecutionsApi;
import com.ericsson.oss.apps.api.JobsApi;
import com.ericsson.oss.apps.api.RulesetsApi;
import com.ericsson.oss.apps.api.ScopesApi;
import com.ericsson.oss.apps.api.model.EaccApprovedAuditResults;
import com.ericsson.oss.apps.api.model.EaccJob;
import com.ericsson.oss.apps.service.MetricService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Configuration
@AllArgsConstructor

public class MetricAspects {

    private final MetricService metricService;

    @Pointcut("execution(* com.ericsson.oss.apps.client.ApiClient.invokeAPI(..))")
    private void invokeApi() { // NOPMD used in aspect
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.controller.JobController.getJobs(..))")
    private void invokeGetJobs() { // NOPMD used in aspect
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.controller.JobController.postJobs(..))")
    private void invokePostJobs() { // NOPMD used in aspect
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.controller.JobController.putJob(..))")
    private void invokePutJob() { // NOPMD used in aspect
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.controller.JobController.deleteJob(..))")
    private void invokeDeleteJob() { // NOPMD used in aspect
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.controller.ExecutionController.getExecutions(..))")
    private void invokeGetExecutions() { // NOPMD used in aspect
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.controller.ExecutionController.getAuditResults(..))")
    private void invokeGetAuditResults() { // NOPMD used in aspect
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.controller.ExecutionController.applyChange(..))")
    private void invokePostAuditResults() { // NOPMD used in aspect
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.controller.RulesetController.createRuleset(..))")
    private void invokeCreateRuleset() { // NOPMD used in aspect
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.controller.RulesetController.listRulesets(..))")
    private void invokeListRulesets() { // NOPMD used in aspect
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.controller.RulesetController.updateRuleset(..))")
    private void invokeUpdateRuleset() { // NOPMD used in aspect
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.controller.RulesetController.deleteRuleset(..))")
    private void invokeDeleteRuleset() { // NOPMD used in aspect
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.controller.RulesetController.getRuleset(..))")
    private void invokeGetRuleset() { // NOPMD used in aspect
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.controller.ScopeController.createScope(..))")
    private void invokeCreateScope() { // NOPMD used in aspect
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.controller.ScopeController.listScopes(..))")
    private void invokeListScopes() { // NOPMD used in aspect
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.controller.ScopeController.updateScope(..))")
    private void invokeUpdateScope() { // NOPMD used in aspect
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.controller.ScopeController.deleteScope(..))")
    private void invokeDeleteScope() { // NOPMD used in aspect
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.controller.ScopeController.getScope(..))")
    private void invokeGetScope() { // NOPMD used in aspect
        throw new UnsupportedOperationException();
    }

    @Around("invokeApi()")
    @SuppressWarnings("checkstyle:illegalthrows")
    public Object handleAroundClientInvokeApi(final ProceedingJoinPoint pjp) throws Throwable {
        final Object[] args = pjp.getArgs();
        final String endpoint = (String) args[0];
        final HttpMethod httpOperation = (HttpMethod) args[1];
        try {
            final Object returnObject = pjp.proceed();
            incrementMetric(getClientCounter(endpoint, httpOperation),
                    endpoint, ((ResponseEntity) returnObject).getStatusCode().value(), httpOperation.toString());
            return returnObject;
        } catch (final RestClientResponseException e) {
            incrementMetric(getClientCounter(endpoint, httpOperation), endpoint, e.getStatusCode().value(), httpOperation.toString());
            throw e;
        }
    }

    @Around("invokeGetJobs()")
    @SuppressWarnings("checkstyle:illegalthrows")
    public Object handleAroundGetJobs(final ProceedingJoinPoint pjp) throws Throwable {
        final Object returnObject = pjp.proceed();
        incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseEntity) returnObject).getStatusCode().value(), JobsApi.class,
                "getJobs");
        return returnObject;
    }

    @Around("invokePostJobs()")
    @SuppressWarnings("checkstyle:illegalthrows")
    public Object handleAroundPostJobs(final ProceedingJoinPoint pjp) throws Throwable {
        final Object returnObject = pjp.proceed();
        incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseEntity) returnObject).getStatusCode().value(), JobsApi.class,
                "postJobs", EaccJob.class);
        return returnObject;
    }

    @Around("invokePutJob()")
    @SuppressWarnings("checkstyle:illegalthrows")
    public Object handleAroundPutJob(final ProceedingJoinPoint pjp) throws Throwable {
        final Object returnObject = pjp.proceed();
        incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseEntity) returnObject).getStatusCode().value(), JobsApi.class,
                "putJob", String.class, EaccJob.class, String.class, String.class);
        return returnObject;
    }

    @Around("invokeDeleteJob()")
    @SuppressWarnings("checkstyle:illegalthrows")
    public Object handleAroundDeleteJob(final ProceedingJoinPoint pjp) throws Throwable {
        final Object returnObject = pjp.proceed();
        incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseEntity) returnObject).getStatusCode().value(), JobsApi.class,
                "deleteJob", String.class, String.class);
        return returnObject;
    }

    @AfterThrowing(pointcut = "invokePostJobs()", throwing = "error")
    public void handlePostJobsException(final JoinPoint joinPoint, final Exception error) throws NoSuchMethodException {
        if (error instanceof ResponseStatusException) {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseStatusException) error).getStatusCode().value(), JobsApi.class,
                    "postJobs", EaccJob.class);
        } else {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, HttpStatus.INTERNAL_SERVER_ERROR.value(), JobsApi.class,
                    "postJobs", EaccJob.class);
        }
    }

    @AfterThrowing(pointcut = "invokePutJob()", throwing = "error")
    public void handlePutJobException(final JoinPoint joinPoint, final Exception error) throws NoSuchMethodException {
        if (error instanceof ResponseStatusException) {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseStatusException) error).getStatusCode().value(), JobsApi.class,
                    "putJob", String.class, EaccJob.class, String.class, String.class);
        } else {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, HttpStatus.INTERNAL_SERVER_ERROR.value(), JobsApi.class,
                    "putJob", String.class, EaccJob.class, String.class, String.class);
        }
    }

    @AfterThrowing(pointcut = "invokeDeleteJob()", throwing = "error")
    public void handleDeleteJobException(final JoinPoint joinPoint, final Exception error) throws NoSuchMethodException {
        if (error instanceof ResponseStatusException) {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseStatusException) error).getStatusCode().value(), JobsApi.class,
                    "deleteJob", String.class, String.class);
        } else {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, HttpStatus.INTERNAL_SERVER_ERROR.value(), JobsApi.class,
                    "deleteJob", String.class, String.class);
        }
    }

    @Around("invokeGetExecutions()")
    @SuppressWarnings("checkstyle:illegalthrows")
    public Object handleAroundGetExecutions(final ProceedingJoinPoint pjp) throws Throwable {
        final Object returnObject = pjp.proceed();
        incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseEntity) returnObject).getStatusCode().value(), ExecutionsApi.class,
                "getExecutions", String.class, String.class);
        return returnObject;
    }

    @Around("invokeGetAuditResults()")
    @SuppressWarnings("checkstyle:illegalthrows")
    public Object handleAroundGetAuditResults(final ProceedingJoinPoint pjp) throws Throwable {
        final Object returnObject = pjp.proceed();
        incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseEntity) returnObject).getStatusCode().value(), ExecutionsApi.class,
                "getAuditResults", String.class, String.class, Integer.class, Integer.class, String.class);
        return returnObject;
    }

    @Around("invokePostAuditResults()")
    @SuppressWarnings("checkstyle:illegalthrows")
    public Object handleAroundPostAuditResults(final ProceedingJoinPoint pjp) throws Throwable {
        final Object returnObject = pjp.proceed();
        incrementMetric(EACC_CI_HTTP_REQUESTS_TOTAL, ((ResponseEntity) returnObject).getStatusCode().value(), ExecutionsApi.class,
                "applyChange", String.class, EaccApprovedAuditResults.class, String.class, String.class);
        return returnObject;
    }

    @Around("invokeCreateRuleset()")
    @SuppressWarnings("checkstyle:illegalthrows")
    public Object handleAroundCreateRuleset(final ProceedingJoinPoint pjp) throws Throwable {
        final Object returnObject = pjp.proceed();
        incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseEntity) returnObject).getStatusCode().value(), RulesetsApi.class,
                "createRuleset", String.class, MultipartFile.class);
        return returnObject;
    }

    @AfterThrowing(pointcut = "invokeCreateRuleset()", throwing = "error")
    public void handleCreateRulesetException(final JoinPoint joinPoint, final Exception error) throws NoSuchMethodException {
        if (error instanceof ResponseStatusException) {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseStatusException) error).getStatusCode().value(), RulesetsApi.class,
                    "createRuleset", String.class, MultipartFile.class);
        } else {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, HttpStatus.INTERNAL_SERVER_ERROR.value(), RulesetsApi.class,
                    "createRuleset", String.class, MultipartFile.class);
        }
    }

    @Around("invokeListRulesets()")
    @SuppressWarnings("checkstyle:illegalthrows")
    public Object handleAroundListRulesets(final ProceedingJoinPoint pjp) throws Throwable {
        final Object returnObject = pjp.proceed();
        incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseEntity) returnObject).getStatusCode().value(), RulesetsApi.class,
                "listRulesets");
        return returnObject;
    }

    @Around("invokeUpdateRuleset()")
    @SuppressWarnings("checkstyle:illegalthrows")
    public Object handleAroundUpdateRuleset(final ProceedingJoinPoint pjp) throws Throwable {
        final Object returnObject = pjp.proceed();
        incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseEntity) returnObject).getStatusCode().value(), RulesetsApi.class,
                "updateRuleset", String.class, MultipartFile.class, String.class, String.class);
        return returnObject;
    }

    @AfterThrowing(pointcut = "invokeUpdateRuleset()", throwing = "error")
    public void handleUpdateRulesetException(final JoinPoint joinPoint, final Exception error) throws NoSuchMethodException {
        if (error instanceof ResponseStatusException) {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseStatusException) error).getStatusCode().value(), RulesetsApi.class,
                    "updateRuleset", String.class, MultipartFile.class, String.class, String.class);
        } else {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, HttpStatus.INTERNAL_SERVER_ERROR.value(), RulesetsApi.class,
                    "updateRuleset", String.class, MultipartFile.class, String.class, String.class);
        }
    }

    @Around("invokeDeleteRuleset()")
    @SuppressWarnings("checkstyle:illegalthrows")
    public Object handleAroundDeleteRuleset(final ProceedingJoinPoint pjp) throws Throwable {
        final Object returnObject = pjp.proceed();
        incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseEntity) returnObject).getStatusCode().value(), RulesetsApi.class,
                "deleteRuleset", String.class, String.class);
        return returnObject;
    }

    @AfterThrowing(pointcut = "invokeDeleteRuleset()", throwing = "error")
    public void handleDeleteRulesetException(final JoinPoint joinPoint, final Exception error) throws NoSuchMethodException {
        if (error instanceof ResponseStatusException) {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseStatusException) error).getStatusCode().value(), RulesetsApi.class,
                    "deleteRuleset", String.class, String.class);
        } else {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, HttpStatus.INTERNAL_SERVER_ERROR.value(), RulesetsApi.class,
                    "deleteRuleset", String.class, String.class);
        }
    }

    @Around("invokeGetRuleset()")
    @SuppressWarnings("checkstyle:illegalthrows")
    public Object handleAroundGetRuleset(final ProceedingJoinPoint pjp) throws Throwable {
        final Object returnObject = pjp.proceed();
        incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseEntity) returnObject).getStatusCode().value(), RulesetsApi.class,
                "getRuleset", String.class, String.class);
        return returnObject;
    }

    @Around("invokeCreateScope()")
    @SuppressWarnings("checkstyle:illegalthrows")
    public Object handleAroundCreateScope(final ProceedingJoinPoint pjp) throws Throwable {
        final Object returnObject = pjp.proceed();
        incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseEntity) returnObject).getStatusCode().value(), ScopesApi.class,
                "createScope", String.class, MultipartFile.class);
        return returnObject;
    }

    @AfterThrowing(pointcut = "invokeCreateScope()", throwing = "error")
    public void handleCreateScopeException(final JoinPoint joinPoint, final Exception error) throws NoSuchMethodException {
        if (error instanceof ResponseStatusException) {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseStatusException) error).getStatusCode().value(), ScopesApi.class,
                    "createScope", String.class, MultipartFile.class);
        } else {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, HttpStatus.INTERNAL_SERVER_ERROR.value(), ScopesApi.class,
                    "createScope", String.class, MultipartFile.class);
        }
    }

    @AfterThrowing(pointcut = "invokeDeleteScope()", throwing = "error")
    public void handleDeleteScopeException(final JoinPoint joinPoint, final Exception error) throws NoSuchMethodException {
        if (error instanceof ResponseStatusException) {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseStatusException) error).getStatusCode().value(), ScopesApi.class,
                    "deleteScope", String.class, String.class);
        } else {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, HttpStatus.INTERNAL_SERVER_ERROR.value(), ScopesApi.class,
                    "deleteScope", String.class, String.class);
        }
    }

    @Around("invokeListScopes()")
    @SuppressWarnings("checkstyle:illegalthrows")
    public Object handleAroundListScopes(final ProceedingJoinPoint pjp) throws Throwable {
        final Object returnObject = pjp.proceed();
        incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseEntity) returnObject).getStatusCode().value(), ScopesApi.class,
                "listScopes");
        return returnObject;
    }

    @AfterThrowing(pointcut = "invokeListScopes()", throwing = "error")
    public void handleListScopeException(final JoinPoint joinPoint, final Exception error) throws NoSuchMethodException {
        if (error instanceof ResponseStatusException) {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseStatusException) error).getStatusCode().value(), ScopesApi.class,
                    "listScopes");
        } else {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, HttpStatus.INTERNAL_SERVER_ERROR.value(), ScopesApi.class,
                    "listScopes");
        }
    }

    @Around("invokeUpdateScope()")
    @SuppressWarnings("checkstyle:illegalthrows")
    public Object handleAroundUpdateScope(final ProceedingJoinPoint pjp) throws Throwable {
        final Object returnObject = pjp.proceed();
        incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseEntity) returnObject).getStatusCode().value(), ScopesApi.class,
                "updateScope", String.class, MultipartFile.class, String.class, String.class);
        return returnObject;
    }

    @AfterThrowing(pointcut = "invokeUpdateScope()", throwing = "error")
    public void handleUpdateScopeException(final JoinPoint joinPoint, final Exception error) throws NoSuchMethodException {
        if (error instanceof ResponseStatusException) {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseStatusException) error).getStatusCode().value(), ScopesApi.class,
                    "updateScope", String.class, MultipartFile.class, String.class, String.class);
        } else {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, HttpStatus.INTERNAL_SERVER_ERROR.value(), ScopesApi.class,
                    "updateScope", String.class, MultipartFile.class, String.class, String.class);
        }
    }

    @Around("invokeDeleteScope()")
    @SuppressWarnings("checkstyle:illegalthrows")
    public Object handleAroundDeleteScope(final ProceedingJoinPoint pjp) throws Throwable {
        final Object returnObject = pjp.proceed();
        incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseEntity) returnObject).getStatusCode().value(), ScopesApi.class,
                "deleteScope", String.class, String.class);
        return returnObject;
    }

    @Around("invokeGetScope()")
    @SuppressWarnings("checkstyle:illegalthrows")
    public Object handleAroundGetScope(final ProceedingJoinPoint pjp) throws Throwable {
        final Object returnObject = pjp.proceed();
        incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseEntity) returnObject).getStatusCode().value(), ScopesApi.class,
                "getScope", String.class, String.class);
        return returnObject;
    }

    @AfterThrowing(pointcut = "invokeGetScope()", throwing = "error")
    public void handleGetScopeException(final JoinPoint joinPoint, final Exception error) throws NoSuchMethodException {
        if (error instanceof ResponseStatusException) {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, ((ResponseStatusException) error).getStatusCode().value(), ScopesApi.class,
                    "getScope", String.class, String.class);
        } else {
            incrementMetric(EACC_HTTP_REQUESTS_TOTAL, HttpStatus.INTERNAL_SERVER_ERROR.value(), ScopesApi.class,
                    "getScope", String.class, String.class);
        }
    }

    private String getClientCounter(final String endPoint, final HttpMethod httpOperation) {
        if (endPoint.startsWith(API_GATEWAY_ENDPOINT)) {
            return APIGATEWAY_SESSIONID_HTTP_REQUESTS_TOTAL;
        } else if (endPoint.startsWith(NCMP_ENDPOINT)) {
            if (HttpMethod.PATCH.equals(httpOperation)) {
                return NCMP_CI_HTTP_REQUESTS_TOTAL;
            } else {
                return NCMP_HTTP_REQUESTS_TOTAL;
            }

        } else {
            return "Unknown endpoint";
        }
    }

    private void incrementMetric(final String metricName, final int httpCode, final Class interfaceClass,
                                 final String methodName, final Class<?>... paramTypes) throws NoSuchMethodException {
        final Method interfaceMethod = interfaceClass.getMethod(methodName, paramTypes);
        final RequestMapping requestMapping = interfaceMethod.getAnnotation(RequestMapping.class);
        final String endpoints = requestMapping.value()[0];
        final String httpMethod = requestMapping.method()[0].name();
        incrementMetric(metricName, endpoints, httpCode, httpMethod);
    }

    private void incrementMetric(final String metricName, final String endPoint, final int httpCode, final String method) {
        metricService.increment(metricName,
                ENDPOINT, endPoint,
                METHOD, method,
                CODE, String.valueOf(httpCode));
    }

}
