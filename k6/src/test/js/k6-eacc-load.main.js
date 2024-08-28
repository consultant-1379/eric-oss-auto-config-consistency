/*
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
 */

import { group, sleep } from "k6";

import * as gateway from "./use_cases/pre_onboarding/gateway-tests.js";
import * as rbac from "./use_cases/post_instantiation/rbac-tests.js";
import * as eaccLoadTest from "./use_cases/post_instantiation/eacc-load-tests.js";

import { logData } from "./modules/common.js";
import {
  DEFAULT_E2E_OPTIONS,
  VALIDATE_EACC_RBAC,
} from "./modules/constants.js";
import { textSummary } from "./modules/k6-summary.js";

import { htmlReport } from "https://arm1s11-eiffel004.eiffel.gic.ericsson.se:8443/nexus/content/sites/oss-sites/common/k6/eric-k6-static-report-plugin/latest/bundle/eric-k6-static-report-plugin.js";

export const options = DEFAULT_E2E_OPTIONS;

export default function () {
  group("GIVEN The API Gateway is available", () => {
    gateway.verifySessionCreation();
  });

  group("GIVEN The User Administration API is available", () => {
    if (VALIDATE_EACC_RBAC) {
      rbac.verifyEaccRbac();
    }
    rbac.verifyEaccUser();
    rbac.verifyEaccAdmin();
  });

  logData("Sleeping for 60s to allow RBAC to take effect");
  sleep(60); // Allow RBAC to take place

  if (VALIDATE_EACC_RBAC) {
    group("GIVEN EACC RBAC is in place", () => {
      rbac.verifyRbacEnforced();
    });
  }

  group(
    "GIVEN The Execution and Job Endpoint is Available & Node for Load Testing",
    () => {
      eaccLoadTest.verifyPostRulesetIsSuccessful();
      eaccLoadTest.verifyPostScopeIsSuccessful();
      eaccLoadTest.verifyPostJobIsSuccessful();
      eaccLoadTest.verifyExecutionsExist();
      eaccLoadTest.verifyApplyAllChanges();
      eaccLoadTest.verifyRevertAllChanges();
      eaccLoadTest.printExecutionStatsForLoadTest();
      eaccLoadTest.verifyDeleteJobIsSuccessful();
      eaccLoadTest.verifyDeleteScopeIsSuccessful();
      eaccLoadTest.verifyDeleteRulesetIsSuccessful();
    }
  );
}

export function handleSummary(data) {
  const reportPath =
    __ENV.STAGING_LEVEL === "PRODUCT" ? "/doc/Test_Report/" : "./reports/";
  let result = { stdout: textSummary(data) };
  result[reportPath.concat("k6-load-test-results.html")] = htmlReport(data);
  result[reportPath.concat("load-test-summary.json")] = JSON.stringify(data);
  return result;
}
