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

import { check, group } from 'k6';

import * as eacc from '../../modules/eacc.js'

import { logData } from '../../modules/common.js';
import { isStatusAccepted, isStatusOk, isStatusCreated, isStatusNoContent, containsHeader, containsExpectedJob, containsExpectedRuleset, containsExpectedScope, isExecutionCompleted, containsExpectedAuditResults, isStatusTooManyRequests} from '../../utils/validationUtils.js';
import { doesNotContainDeletedJob, doesNotContainDeletedScope, isResponseSuccessfulForExecution } from '../../utils/validationUtils.js';
import { AuditStatus, ChangeStatus, getAuditStatusFilter, getChangeStatusFilter } from '../../modules/eacc-constants.js';

function verifyPostRulesetIsSuccessful() {
    eacc.getAdminSessionId();
    logData("verifyPostRulesetIsSuccessful");
    group('WHEN ruleset is created', () => {
        check(eacc.postRuleset(eacc.getRulesetForLoadTest()), {
            'THEN status code is 201': (r) => isStatusCreated(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            'AND response body include ruleset name': (r) => containsExpectedRuleset(r.body, eacc.getRulesetForLoadTest().rulesetName)
        })
    });
    eacc.cleanAdminSession();
}

function verifyPostScopeIsSuccessful() {
    eacc.getAdminSessionId();
    logData("verifyPostScopeIsSuccessful");
    group('WHEN scope is created', () => {
        check(eacc.postScopeForLoadTest(), {
            'THEN status code is 201': (r) => isStatusCreated(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            'AND response body includes scope name, uri and non-null id': (r) => containsExpectedScope(r.body, eacc.getScopeForLoadTest().scopeName)
        })
    });
    eacc.cleanAdminSession();
}

function verifyPostJobIsSuccessful() {
    eacc.getAdminSessionId();
    logData("verifyPostJobIsSuccessful");
    group('WHEN Job is scheduled', () => {
        check(eacc.postJob(eacc.createJobObjectForLoadTest()), {
            'THEN status code is 201': (r) => isStatusCreated(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            "AND response body contains expected job": (r) => containsExpectedJob(r.body, eacc.createJobObjectForLoadTest())
        });
    });
    eacc.cleanAdminSession();
}

function verifyExecutionsExist() {
  eacc.getUserSessionId();
  logData("verifyExecutionsExist");
  group("WHEN viewing Job schedule in v1/executions", () => {
    check(eacc.getExecutions(eacc.getJobNameForLoadTest()), {
          "THEN status code is 200": (r) => isStatusOk(r.status),
          "AND Content-Type is application/json": (r) => containsHeader(r.headers, "Content-Type", "application/json"),
          "AND If execution completed for specified job": () => isExecutionCompleted(eacc.getJobNameForLoadTest())
        });
    check(eacc.getExecutions(eacc.getJobNameForLoadTest()), {
            'THEN status code is 200': (r) => isStatusOk(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            'AND response body contains successful execution for specified job':(r) => isResponseSuccessfulForExecution(r, eacc.getJobNameForLoadTest())
        });
    });
    eacc.cleanUserSession();
}

function verifyDeleteJobIsSuccessful() {
    eacc.getAdminSessionId();
    logData("verifyDeleteJobIsSuccessful");
    group('WHEN Job is deleted', () => {
        check(eacc.deleteJob(eacc.getJobNameForLoadTest()), {
            'THEN status code is 204': (r) => isStatusNoContent(r.status)
        });
        check(eacc.getJob(), {
            'AND Get Jobs returns status 200': (r) => isStatusOk(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            'AND response body does not contain deleted job': (r) => doesNotContainDeletedJob(r, eacc.getJobNameForLoadTest())
        });
    });
    eacc.cleanAdminSession();
}

function verifyDeleteScopeIsSuccessful() {
    eacc.getAdminSessionId();
    logData("verifyDeleteScopeIsSuccessful");
    group('WHEN a Scope is Deleted', () => {
        check(eacc.deleteScope(eacc.findUUIDOfScopeName()), {
            'THEN status code is 204': (r) => isStatusNoContent(r.status),
        });
        check(eacc.getAllScopes(), {
            'AND Get Jobs returns status 200': (r) => isStatusOk(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            'AND response body does not contain deleted scope': (r) => doesNotContainDeletedScope(r, eacc.findUUIDOfScopeName())
        });
    });
    eacc.cleanAdminSession();
}

function verifyDeleteRulesetIsSuccessful() {
    eacc.getAdminSessionId();
    logData("verifyDeleteRulesetIsSuccessful");
    group('WHEN Ruleset is deleted', () => {
        check(eacc.deleteRuleset(), {
            'THEN status code is 204': (r) => isStatusNoContent(r.status)
        });
    });
    eacc.cleanAdminSession();
}

function verifyApplyAllChanges() {
    logData("verifyApplyAllChanges");
    eacc.getAdminSessionId();
    eacc.startApplyChangesTimer();
    let changesForImplementationCount = eacc.getNumberOfChanges(eacc.getJobNameForLoadTest(), getAuditStatusFilter(AuditStatus.INCONSISTENT));
    group('WHEN Applying All Changes using POST v1/executions/{id}/audit-results', () => {
        check(eacc.applyAllChanges(eacc.getJobNameForLoadTest()), {
            'THEN status code is 202': (r) => isStatusAccepted(r.status)
        });
    });

    group('After Applying All changes using POST v1/executions/{id}/audit-results', () => {
        check(eacc.getAuditResults(eacc.getJobNameForLoadTest(), getChangeStatusFilter(ChangeStatus.IMPLEMENTATION_IN_PROGRESS), {'page': 0, 'pageSize': 1}), {
            'THEN No changes are in progress': (r) => containsExpectedAuditResults(r, 0, eacc.getJobNameForLoadTest(), getChangeStatusFilter(ChangeStatus.IMPLEMENTATION_IN_PROGRESS), 45, 60)
        });
        check(eacc.getAuditResults(eacc.getJobNameForLoadTest(), getChangeStatusFilter(ChangeStatus.IMPLEMENTATION_COMPLETE), {'page': 0, 'pageSize': 1}), {
            'THEN All changes are implemented': (r) => containsExpectedAuditResults(r, changesForImplementationCount, eacc.getJobNameForLoadTest(), getChangeStatusFilter(ChangeStatus.IMPLEMENTATION_COMPLETE), 0, 60)
        });
        check(eacc.getAuditResults(eacc.getJobNameForLoadTest(), getChangeStatusFilter(ChangeStatus.IMPLEMENTATION_FAILED), {'page': 0, 'pageSize': 1}), {
            'THEN No changes are failed': (r) => containsExpectedAuditResults(r, 0, eacc.getJobNameForLoadTest(), getChangeStatusFilter(ChangeStatus.IMPLEMENTATION_FAILED), 0, 60)
        });
    });
    eacc.endApplyChangesTimer();
    eacc.cleanAdminSession();
}

function verifyRateLimitSuccessful() {
    eacc.getAdminSessionId();
    logData("verifyRateLimitedSuccessfully");
      group('After existing executions and requests greater than the rate limit', () => {
        check(eacc.getExecutions(""), {
          "THEN status code is 429": (r) => isStatusTooManyRequests(r.status),
        });
      });
    eacc.cleanAdminSession();
}

function verifyRevertAllChanges() {
    logData("verifyRevertAllChanges");
    eacc.getAdminSessionId();
    eacc.startRevertChangesTimer();
    let changesForReversionCount = eacc.getNumberOfChanges(eacc.getJobNameForLoadTest(), getChangeStatusFilter(ChangeStatus.IMPLEMENTATION_COMPLETE));
    
    group('WHEN Revert All Changes using POST v1/executions/{id}/audit-results', () => {
        check(eacc.revertAllChanges(eacc.getJobNameForLoadTest()), {
            'THEN status code is 202': (r) => isStatusAccepted(r.status)
        });
    });

    group('After Reverting All changes using POST v1/executions/{id}/audit-results', () => {
        check(eacc.getAuditResults(eacc.getJobNameForLoadTest(), getChangeStatusFilter(ChangeStatus.REVERSION_IN_PROGRESS), {'page': 0, 'pageSize': 1}), {
            'THEN No changes are in progress': (r) => containsExpectedAuditResults(r, 0, eacc.getJobNameForLoadTest(), getChangeStatusFilter(ChangeStatus.REVERSION_IN_PROGRESS), 45, 60)
        });
        check(eacc.getAuditResults(eacc.getJobNameForLoadTest(), getChangeStatusFilter(ChangeStatus.REVERSION_COMPLETE), {'page': 0, 'pageSize': 1}), {
            'THEN All changes are reverted': (r) => containsExpectedAuditResults(r, changesForReversionCount, eacc.getJobNameForLoadTest(), getChangeStatusFilter(ChangeStatus.REVERSION_COMPLETE), 0)
        });
        check(eacc.getAuditResults(eacc.getJobNameForLoadTest(), getChangeStatusFilter(ChangeStatus.REVERSION_FAILED), {'page': 0, 'pageSize': 1}), {
            'THEN No changes are failed': (r) => containsExpectedAuditResults(r, 0, eacc.getJobNameForLoadTest(), getChangeStatusFilter(ChangeStatus.REVERSION_FAILED), 0)
        });
    });
    eacc.endRevertChangesTimer();
    eacc.cleanAdminSession();
}

function printExecutionStatsForLoadTest(){
    logData("Execution Stats for Load Test");
    eacc.getUserSessionId();

    eacc.printExecutionStats(eacc.getExecutions(eacc.getJobNameForLoadTest()));

    eacc.cleanUserSession();
}

module.exports = {
    verifyPostRulesetIsSuccessful,
    verifyPostScopeIsSuccessful,
    verifyPostJobIsSuccessful,
    verifyExecutionsExist,
    verifyApplyAllChanges,
    verifyRevertAllChanges,
    printExecutionStatsForLoadTest,
    verifyDeleteJobIsSuccessful,
    verifyDeleteScopeIsSuccessful,
    verifyDeleteRulesetIsSuccessful,
    verifyRateLimitSuccessful,
}


