/*
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
 */

import { group, sleep } from "k6";

import * as eaccLoadTest from "./use_cases/post_instantiation/eacc-load-tests.js";

import { logData } from "./modules/common.js";

import { textSummary } from "./modules/k6-summary.js";

import { htmlReport } from "https://arm1s11-eiffel004.eiffel.gic.ericsson.se:8443/nexus/content/sites/oss-sites/common/k6/eric-k6-static-report-plugin/latest/bundle/eric-k6-static-report-plugin.js";

import http from 'k6/http';

export const options = {
  scenarios: {
    ratelimit_scenario: {
        exec: "rateLimitTest",
        executor: 'constant-arrival-rate',
        timeUnit: '1s',
        duration: '5s',
        rate: 50,
        preAllocatedVUs: 50,
    },
  },
  thresholds: {
    checks: ['rate > 0.05']
  },
};

export function rateLimitTest(){
    logData("Starting rate limiter test");
    group(
        "GIVEN The Execution Endpoint is Available & Node for Load Testing",
        () => {
            eaccLoadTest.verifyRateLimitSuccessful();
        }
    );
}

export function handleSummary(data) {
    const reportPath = __ENV.STAGING_LEVEL === 'PRODUCT' ? '/doc/Test_Report/' : './reports/';
    let result = { 'stdout': textSummary(data) };
    result[reportPath.concat('rate-limit-test-results.html')] = htmlReport(data);
    result[reportPath.concat('rate-limit-test-summary.json')] = JSON.stringify(data)
    return result;
}