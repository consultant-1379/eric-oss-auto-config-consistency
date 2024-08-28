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

package com.ericsson.oss.apps.util;

import static com.ericsson.oss.apps.util.Constants.VERSION_PATH;

import java.util.Map;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class MetricConstants {
    public static final String TAG = "tag";
    public static final String DESCRIPTION = "description";
    public static final String REGISTER = "register";
    public static final String CODE = "code";
    public static final String ENDPOINT = "endpoint";
    public static final String METHOD = "method";
    public static final String STATUS = "status";
    public static final String URI = "uri";

    public static final String SKIPPED = "skipped";
    public static final String SUCCEEDED = "succeeded";
    public static final String FAILED = "failed";
    public static final String PARTIAL = "partially audited";

    public static final String HTTP_SERVER_REQUEST = "http.server.requests";
    public static final String HTTP_CLIENT_REQUEST = "http.client.requests";

    public static final String APIGATEWAY_SESSIONID_HTTP_REQUESTS = "apigateway.sessionid.http.requests";
    public static final String NCMP_HTTP_REQUESTS = "ncmp.processing.http.requests";

    public static final String SERVICE_PREFIX = "eacc";
    public static final String CHANGE_IMPLEMENTATION_PREFIX = SERVICE_PREFIX + ".ci.";
    public static final String CONSISTENCY_CHECK_PREFIX = SERVICE_PREFIX + ".cc.";
    public static final String CONSISTENCY_ACTUATOR_PREFIX = SERVICE_PREFIX + ".act.";

    public static final String APIGATEWAY_SESSIONID_HTTP_REQUESTS_TOTAL = CONSISTENCY_CHECK_PREFIX + "apigateway.sessionid.http.requests.total";
    public static final String APIGATEWAY_SESSIONID_HTTP_REQUESTS_DURATION_SECONDS = CONSISTENCY_CHECK_PREFIX
            + "apigateway.sessionid.http.requests.duration.seconds";

    public static final String SCOPE_ENDPOINT = VERSION_PATH + "/scopes";
    public static final String SCOPE_ID_ENDPOINT = VERSION_PATH + "/scopes/{id}";
    public static final String RULESET_ENDPOINT = VERSION_PATH + "/rulesets";
    public static final String RULESET_ID_ENDPOINT = VERSION_PATH + "/rulesets/{id}";
    public static final String JOBS_ENDPOINT = VERSION_PATH + "/jobs";
    public static final String JOBS_ID_ENDPOINT = VERSION_PATH + "/jobs/{jobName}";
    public static final String API_GATEWAY_ENDPOINT = "/auth/v1/login";
    public static final String NCMP_ENDPOINT = "/v1/ch/{cm-handle}/data/ds/{ncmp-datastore-name}";
    public static final String EXECUTIONS_AUDIT_RESULTS_ENDPOINT = VERSION_PATH + "/executions/{id}/audit-results";
    public static final String EXECUTIONS_ENDPOINT = VERSION_PATH + "/executions";

    public static final String EACC_EXECUTIONS_TOTAL = CONSISTENCY_CHECK_PREFIX + "executions.total";
    public static final String EACC_EXECUTIONS_MO_TOTAL = CONSISTENCY_CHECK_PREFIX + "executions.mo.total";
    public static final String EACC_EXECUTIONS_RULES_TOTAL = CONSISTENCY_CHECK_PREFIX + "executions.rules.total";
    public static final String EACC_EXECUTIONS_ATTR_TOTAL = CONSISTENCY_CHECK_PREFIX + "executions.attr.total";
    public static final String EACC_EXECUTIONS_DURATION_SECONDS = CONSISTENCY_CHECK_PREFIX + "executions.duration.seconds";
    public static final String EACC_EXECUTIONS_AUDIT_QUEUE_SUBMITTED_TOTAL = CONSISTENCY_CHECK_PREFIX + "audit.queue.submitted.total";
    public static final String EACC_EXECUTIONS_AUDIT_QUEUE_COMPLETED_TOTAL = CONSISTENCY_CHECK_PREFIX + "audit.queue.completed.total";
    public static final String EACC_HTTP_REQUESTS_TOTAL = CONSISTENCY_CHECK_PREFIX + "http.requests.total";
    public static final String EACC_HTTP_REQUESTS_DURATION_SECONDS = CONSISTENCY_CHECK_PREFIX + "http.requests.duration.seconds";
    public static final String NCMP_HTTP_REQUESTS_TOTAL = CONSISTENCY_CHECK_PREFIX + "ncmp.processing.http.requests.total";
    public static final String NCMP_HTTP_REQUESTS_TOTAL_DURATION_SECONDS = CONSISTENCY_CHECK_PREFIX
            + "ncmp.processing.http.requests.duration.seconds";
    public static final String NCMP_READS_SUBMITTED_TOTAL = CONSISTENCY_CHECK_PREFIX + "ncmp.reads.submitted.total";
    public static final String NCMP_READS_COMPLETED_TOTAL = CONSISTENCY_CHECK_PREFIX + "ncmp.reads.completed.total";

    public static final String EACC_CI_APPLIED_CHANGES_TOTAL = CHANGE_IMPLEMENTATION_PREFIX + "changes.total";
    public static final String EACC_CI_CHANGES_DURATION_SECONDS = CHANGE_IMPLEMENTATION_PREFIX + "changes.duration.seconds";
    public static final String EACC_CI_HTTP_REQUESTS_TOTAL = CHANGE_IMPLEMENTATION_PREFIX + "http.requests.total";
    public static final String EACC_CI_HTTP_REQUESTS_DURATION_SECONDS = CHANGE_IMPLEMENTATION_PREFIX + "http.requests.duration.seconds";
    public static final String NCMP_CI_HTTP_REQUESTS_TOTAL = CHANGE_IMPLEMENTATION_PREFIX + "ncmp.processing.http.requests.total";
    public static final String NCMP_CI_HTTP_REQUESTS_TOTAL_DURATION_SECONDS = CHANGE_IMPLEMENTATION_PREFIX
            + "ncmp.processing.http.requests.duration.seconds";

    public static final String EACC_CI_APPLIED_REVERSIONS_TOTAL = CHANGE_IMPLEMENTATION_PREFIX + "reversions.total";
    public static final String EACC_CI_REVERSIONS_DURATION_SECONDS = CHANGE_IMPLEMENTATION_PREFIX + "reversions.duration.seconds";

    public static final Map<String, String> METRIC_DESCRIPTIONS = Map.ofEntries(
            Map.entry(APIGATEWAY_SESSIONID_HTTP_REQUESTS_TOTAL, "The total number of api gateway http requests."),
            Map.entry(APIGATEWAY_SESSIONID_HTTP_REQUESTS_DURATION_SECONDS, "The total duration of api gateway http requests."),
            Map.entry(EACC_EXECUTIONS_TOTAL, "The total number of consistency check executions."),
            Map.entry(EACC_EXECUTIONS_MO_TOTAL, "The total number of Managed Objects in the consistency check executions."),
            Map.entry(EACC_EXECUTIONS_RULES_TOTAL, "The total number of rules in the consistency check executions."),
            Map.entry(EACC_EXECUTIONS_ATTR_TOTAL, "The total number of attributes in the consistency check executions."),
            Map.entry(EACC_EXECUTIONS_DURATION_SECONDS, "The duration of the consistency check executions."),
            Map.entry(EACC_EXECUTIONS_AUDIT_QUEUE_SUBMITTED_TOTAL, "The total number of MOs queued for audit."),
            Map.entry(EACC_EXECUTIONS_AUDIT_QUEUE_COMPLETED_TOTAL, "The total number of MOs completed for audit."),
            Map.entry(EACC_HTTP_REQUESTS_TOTAL, "The total number of http requests for the audit."),
            Map.entry(EACC_HTTP_REQUESTS_DURATION_SECONDS, "The total duration of http requests for the audit."),
            Map.entry(NCMP_HTTP_REQUESTS_TOTAL, "The total number of ncmp http requests for the audit."),
            Map.entry(NCMP_HTTP_REQUESTS_TOTAL_DURATION_SECONDS, "The total duration of ncmp http requests for the audit."),
            Map.entry(NCMP_READS_SUBMITTED_TOTAL, "The total number of reads submitted to ncmp for the audit."),
            Map.entry(NCMP_READS_COMPLETED_TOTAL, "The total number of read completed to ncmp for the audit."),
            Map.entry(EACC_CI_APPLIED_CHANGES_TOTAL, "The total number of changes that were applied."),
            Map.entry(EACC_CI_CHANGES_DURATION_SECONDS, "The duration of the changes being applied."),
            Map.entry(EACC_CI_HTTP_REQUESTS_TOTAL, "The total number of http requests for change implementation."),
            Map.entry(EACC_CI_HTTP_REQUESTS_DURATION_SECONDS, "The total duration of http requests for change implementation."),
            Map.entry(NCMP_CI_HTTP_REQUESTS_TOTAL, "The total number of ncmp http requests for change implementation."),
            Map.entry(NCMP_CI_HTTP_REQUESTS_TOTAL_DURATION_SECONDS, "The total duration of ncmp http requests for change implementation."),
            Map.entry(EACC_CI_APPLIED_REVERSIONS_TOTAL, "The total number of changes that were reverted"),
            Map.entry(EACC_CI_REVERSIONS_DURATION_SECONDS, "The duration of the changes being reverted."));
}
