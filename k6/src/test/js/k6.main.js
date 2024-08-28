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

import { group, sleep } from 'k6';

import * as gateway from './use_cases/pre_onboarding/gateway-tests.js';
import * as ncmpTest from './use_cases/pre_onboarding/ncmp-tests.js';
import * as restsim from './use_cases/pre_onboarding/restsim.js';
import * as rbac from './use_cases/post_instantiation/rbac-tests.js';
import * as eaccFuncTest from './use_cases/post_instantiation/eacc-func-tests.js';

import { logData } from './modules/common.js';
import { DEFAULT_E2E_OPTIONS, VALIDATE_EACC_RBAC } from './modules/constants.js';
import { textSummary } from './modules/k6-summary.js';

import { htmlReport } from "https://arm1s11-eiffel004.eiffel.gic.ericsson.se:8443/nexus/content/sites/oss-sites/common/k6/eric-k6-static-report-plugin/latest/bundle/eric-k6-static-report-plugin.js";

export const options = DEFAULT_E2E_OPTIONS;

export default function() {
    if (__ENV.TEST_PHASE === 'PRE_ONBOARDING') {
        logData("PRE_ONBOARDING");
        group('GIVEN The User Administration API is available', () => {
            rbac.verifyTestUser();
        });

        group('GIVEN The API Gateway is available', () => {
            gateway.verifySessionCreation();
            gateway.verifyNcmpRouteExists();
            restsim.resetNetworkConfigData();
        });

        group('GIVEN The NCMP API is available', () => {
            ncmpTest.verifyExistingNodes();
        });
    } else {
        logData("POST_INSTANTIATION");
        group('GIVEN The API Gateway is available', () => {
            gateway.verifySessionCreation();
            gateway.verifyEaccApiOnboarding();
//          const startTime = new Date().getTime();
//          ncmp.getLteManagedElements();
//          ncmp.clearSession();
//          ncmp.getNrManagedElements();
//          ncmp.clearSession();
//          const endTime = new Date().getTime();
//          const elapsedTimeInMinutes = (endTime - startTime) / 1000 / 60;
//          console.log('Scope file generation took ' + elapsedTimeInMinutes + ' minutes to execute.');
        });

        group('GIVEN The User Administration API is available', () => {
            if (VALIDATE_EACC_RBAC) {
                rbac.verifyEaccRbac();
            }
            rbac.verifyEaccUser();
            rbac.verifyEaccAdmin();
        });

        logData("Sleeping for 60s to allow RBAC to take effect")
        sleep(60); // Allow RBAC to take place

        if (VALIDATE_EACC_RBAC) {
            group('GIVEN EACC RBAC is in place', () => {
                rbac.verifyRbacEnforced();
            });
        }

        group('GIVEN The Execution and Job Endpoint is Available & required nodes exist', () => {
            eaccFuncTest.verifyPostInvalidRulesetFails(); // Checks for invalid rules
            eaccFuncTest.verifyPostRulesetIsSuccessful();
            eaccFuncTest.verifyUpdateValidRulesetIsSuccessful();
            eaccFuncTest.verifyUpdateInvalidRulesetFails(); // Checks for invalid rules
            eaccFuncTest.verifyPostScopeIsSuccessful();
            eaccFuncTest.verifyPostJobIsSuccessful();
            eaccFuncTest.verifyExecutionsExist();
            eaccFuncTest.verifyAuditResults();
            eaccFuncTest.verifyAuditResultsFilteredByFullMatchForMo();
            eaccFuncTest.verifyAuditResultsFilteredByPartialMatchForMo();
            eaccFuncTest.verifyAuditResultsFilteredByLeftLikeMatchForMoFdn();
            eaccFuncTest.verifyAuditResultsFilteredByRightLikeMatchForMoFdn();
            eaccFuncTest.verifyAuditResultsFilteredByInconsistentAuditResultStatus();
            eaccFuncTest.verifyAuditResultsFilteredByAuditStatusAndFilteredByMo();
            eaccFuncTest.verifyApplyChanges();
            eaccFuncTest.verifyRevertChanges();
            eaccFuncTest.verifyPutJobIsSuccessful();
            eaccFuncTest.verifyReadScopeIsSuccessful();
            eaccFuncTest.verifyDeleteJobIsSuccessful();
            eaccFuncTest.verifyUpdateScopeIsSuccessful();
            eaccFuncTest.verifyDeleteRulesetIsSuccessful();
            eaccFuncTest.verifyDeleteScopeIsSuccessful();
        });

        group('GIVEN The Log transformer is in place', () => {
            eaccFuncTest.verifyLogStreamingIsSuccessful()
            eaccFuncTest.verifyAuditLogIsSuccessful()
        });
    }
}

export function handleSummary(data) {
    const reportPath = __ENV.STAGING_LEVEL === 'PRODUCT' ? '/doc/Test_Report/' : './reports/';
    let result = { 'stdout': textSummary(data) };
    result[reportPath.concat('k6-test-results.html')] = htmlReport(data);
    result[reportPath.concat('summary.json')] = JSON.stringify(data)
    return result;
}
