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

import * as eacc from '../../modules/eacc.js';

import { logData } from '../../modules/common.js';
import {
    containsExpectedAuditResults,
    containsExpectedCountsInExecution,
    containsExpectedExecutionIdAndNumberOfResults,
    containsExpectedJob,
    containsExpectedMetadata,
    containsExpectedRuleset,
    containsExpectedScope,
    containsHeader,
    containsOnlyAuditResultsForGivenManagedObject,
    containsOnlyAuditResultsForInconsistentAuditStatus,
    containsExpectedAuditResultsForMo,
    containsRuleValidationErrors,
    doesNotContainDeletedJob,
    doesNotContainDeletedScope,
    isExecutionCompleted,
    isResponseSuccessfulForExecution,
    isStatusAccepted,
    isStatusBadRequest,
    isStatusCreated,
    isStatusNoContent,
    isStatusOk,
} from '../../utils/validationUtils.js';
import {
    AUDIT_LOG_FILTER_PAYLOAD,
    AuditStatus,
    ChangeStatus,
    CM_HANDLE_4G,
    FDD_CELL_ID,
    FILTER_PAYLOAD,
    getChangeStatusFilter,
    MANAGED_ELEMENT_4G_ID,
    REVERT_CHANGE,
} from '../../modules/eacc-constants.js';
import { PERCENTAGE } from '../../modules/constants.js';
import * as ncmp from '../../modules/ncmp.js';

function verifyPostInvalidRulesetFails() {
    eacc.getAdminSessionId();
    logData("verifyPostInvalidRulesetFails");
    group('WHEN invalid ruleset is provided', () => {
        check(eacc.postRuleset(eacc.getInvalidRuleset()), {
            'THEN status code is 400': (r) => isStatusBadRequest(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            'AND response body includes the rule validation errors': (r) => containsRuleValidationErrors(r.body, eacc.getExpectedRuleValidationErrors())
        })
    });
    eacc.cleanAdminSession();
}

function verifyPostRulesetIsSuccessful() {
    eacc.getAdminSessionId();
    logData("verifyPostRulesetIsSuccessful");
    group('WHEN ruleset is created', () => {
        check(eacc.postRuleset(eacc.getFuncTestRuleset()), {
            'THEN status code is 201': (r) => isStatusCreated(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            'AND response body include ruleset name': (r) => containsExpectedRuleset(r.body, eacc.getFuncTestRuleset().rulesetName)
        })
    });
    eacc.cleanAdminSession();
}

function verifyUpdateValidRulesetIsSuccessful() {
    eacc.getAdminSessionId();
    logData("verifyUpdateValidRulesetIsSuccessful");
    group('WHEN valid ruleset is used in an update', () => {
        check(eacc.putRuleset(eacc.getValidUpdatedRuleset()), {
            'THEN status code is 200': (r) => isStatusOk(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json')
        })
    });
    eacc.cleanAdminSession();
}

function verifyUpdateInvalidRulesetFails() {
    eacc.getAdminSessionId();
    logData("verifyUpdateInvalidRulesetFails");
    group('WHEN invalid ruleset is used in an update', () => {
        check(eacc.putRuleset(eacc.getInvalidUpdatedRuleset()), {
            'THEN status code is 400': (r) => isStatusBadRequest(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            'AND response body includes the rule validation errors': (r) => containsRuleValidationErrors(r.body, eacc.getExpectedRuleValidationErrors())
        })
    });
    eacc.cleanAdminSession();
}

function verifyPostScopeIsSuccessful() {
    eacc.getAdminSessionId();
    logData("verifyPostScopeIsSuccessful");
    group('WHEN scope is created', () => {
        check(eacc.postFuncTestScope(), {
            'THEN status code is 201': (r) => isStatusCreated(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            'AND response body includes scope name, uri and non-null id': (r) => containsExpectedScope(r.body, eacc.getScopeForFuncTest().scopeName)
        })
    });
    eacc.cleanAdminSession();
}

function verifyPostJobIsSuccessful() {
    eacc.getAdminSessionId();
    logData("verifyPostJobIsSuccessful");
    group('WHEN Job is scheduled', () => {
        check(eacc.postJob(eacc.createJobObjectFuncTest()), {
            'THEN status code is 201': (r) => isStatusCreated(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            "AND response body contains expected job": (r) => containsExpectedJob(r.body, eacc.createJobObjectFuncTest())
        });
    });
    eacc.cleanAdminSession();
}

function verifyPutJobIsSuccessful() {
    eacc.getAdminSessionId();
    logData("verifyPutJobIsSuccessful");
    group('WHEN Job is updated', () => {
        check(eacc.putJob(eacc.createJobObjectFuncTest()), {
            'THEN status code is 200': (r) => isStatusOk(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            "AND response body contains expected job": (r) => containsExpectedJob(r.body, eacc.createJobObjectFuncTest())
        });
    });
    eacc.cleanAdminSession();
}

function verifyExecutionsExist() {
  eacc.getUserSessionId();
  logData("verifyExecutionsExist");
  group("WHEN viewing Job schedule in v1/executions", () => {
    check(eacc.getExecutions(eacc.getJobNameFuncTest()), {
          "THEN status code is 200": (r) => isStatusOk(r.status),
          "AND Content-Type is application/json": (r) => containsHeader(r.headers, "Content-Type", "application/json"),
          "AND If execution completed for specified job": () => isExecutionCompleted(eacc.getJobNameFuncTest())
        });
    check(eacc.getExecutions(eacc.getJobNameFuncTest()), {
            'THEN status code is 200': (r) => isStatusOk(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            'AND response body contains successful execution for specified job':(r) => isResponseSuccessfulForExecution(r, eacc.getJobNameFuncTest()),
            'AND response body contains expected audit counts for specified job':(r) => containsExpectedCountsInExecution(r, 58, 98, 58)
        });
    });
    eacc.cleanUserSession();
}

function verifyReadScopeIsSuccessful() {
    eacc.getAdminSessionId();
    logData("verifyReadScopeIsSuccessful");
    group('WHEN scope is read', () => {
        check(eacc.readScope(eacc.findUUIDOfScopeName()), {
            'THEN status code is 200': (r) => isStatusOk(r.status),
            'AND Content-Type is text/csv': (r) => containsHeader(r.headers, 'Content-Type', 'text/csv'),
            'AND response body includes proper fdn csv file': (r) => eacc.verifyFuncTestReadScope(r.body)
        })
    });
    eacc.cleanAdminSession();
}

function verifyUpdateScopeIsSuccessful() {
    eacc.getAdminSessionId();
    logData("verifyUpdateScopeIsSuccessful");
    const UUID_SCOPE = eacc.findUUIDOfScopeName();
    group('WHEN scope is updated', () => {
        check(eacc.updateScope(UUID_SCOPE, eacc.getFuncTestFdnBody()), {
            'THEN status code is 200': (r) => isStatusOk(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            'AND response body includes proper metadata': (r) => containsExpectedMetadata(r.body, eacc.createMetadata(UUID_SCOPE))
        })
        check(eacc.readScope(UUID_SCOPE), {
            'AND read scope status code is 200': (r) => isStatusOk(r.status),
            'AND Content-Type is text/csv': (r) => containsHeader(r.headers, 'Content-Type', 'text/csv'),
            'AND response body includes proper updated fdn csv file': (r) => eacc.verifyFuncTestUpdatedScope(r.body)
        })
    });
    eacc.cleanAdminSession();
}

function verifyAuditResults() {
    eacc.getUserSessionId();
    logData("verifyAuditResults");
    group('WHEN viewing Audit Results in v1/executions/{id}/audit-results', () => {
        check(eacc.getAuditResults(eacc.getJobNameFuncTest()), {
            'THEN status code is 200': (r) => isStatusOk(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            'AND response body contains only audit results for the specified execution and the correct number of results': (r) => containsExpectedExecutionIdAndNumberOfResults(r, eacc.getJobNameFuncTest(), 98)
        });
    });
    eacc.cleanUserSession();
}

function verifyAuditResultsFilteredByFullMatchForMo() {
    eacc.getUserSessionId();
    logData("verifyAuditResultsFilteredByFullMatchForMo");
    const moFdn = "SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE63dg2ERBST00068,ENodeBFunction=1,EUtranCellFDD=LTE63dg2ERBST00068-7";

    group('WHEN viewing Audit Results in v1/executions/{id}/audit-results for a fully qualified name for MO', () => {
        check(eacc.getAuditResultsForAGivenMo(eacc.getJobNameFuncTest(), moFdn), {
            'THEN status code is 200': (r) => isStatusOk(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            'AND response body contains only audit results for the specified execution and the correct number of results': (r) => containsExpectedExecutionIdAndNumberOfResults(r, eacc.getJobNameFuncTest(), 2),
            'AND response body contains only audit results for the specified managed object': (r) => containsOnlyAuditResultsForGivenManagedObject(r, moFdn),
            'AND the results use the correct rule based on priority and conditions': (r) => containsExpectedAuditResultsForMo(r, moFdn)
        });
    });

    eacc.cleanUserSession();
}

function verifyAuditResultsFilteredByPartialMatchForMo() {
    eacc.getUserSessionId();
    logData("verifyAuditResultsFilteredByPartialMatchForMo");
    const moFdn = "EUtranCellFDD=LTE63dg2ERBST00068";
    const appendPercentageOnMoFdn = encodeURIComponent(PERCENTAGE+moFdn+PERCENTAGE);

    group('WHEN viewing Audit Results in v1/executions/{id}/audit-results for a partial qualified name for MO', () => {
        check(eacc.getAuditResultsForAGivenMo(eacc.getJobNameFuncTest(), appendPercentageOnMoFdn), {
            'THEN status code is 200': (r) => isStatusOk(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            'AND response body contains only audit results for the specified execution and the correct number of results': (r) => containsExpectedExecutionIdAndNumberOfResults(r, eacc.getJobNameFuncTest(), 24),
            'AND response body contains only audit results for the specified managed object': (r) => containsOnlyAuditResultsForGivenManagedObject(r, moFdn)
        });
    });
    eacc.cleanUserSession();
}

function verifyAuditResultsFilteredByLeftLikeMatchForMoFdn() {
    eacc.getUserSessionId();
    logData("verifyAuditResultsFilteredByLeftLikeMatchForMoFdn");
    const moFdn = "EUtranCellFDD=LTE63dg2ERBST00075-1";
    const appendPercentageOnMoFdn = encodeURIComponent(PERCENTAGE+moFdn);

    group('WHEN viewing Audit Results in v1/executions/{id}/audit-results for a left like match for MO', () => {
        check(eacc.getAuditResultsForAGivenMo(eacc.getJobNameFuncTest(), appendPercentageOnMoFdn), {
            'THEN status code is 200': (r) => isStatusOk(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            'AND response body contains only audit results for the specified execution and the correct number of results': (r) => containsExpectedExecutionIdAndNumberOfResults(r, eacc.getJobNameFuncTest(), 2),
            'AND response body contains only audit results for the specified partial managed object': (r) => containsOnlyAuditResultsForGivenManagedObject(r, moFdn)
        });
    });
    eacc.cleanUserSession();
}

function verifyAuditResultsFilteredByRightLikeMatchForMoFdn() {
    eacc.getUserSessionId();
    logData("verifyAuditResultsFilteredByRightLikeMatchForMoFdn");
    const moFdn = "SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE63dg2ERBST00075";
    const appendPercentageOnMoFdn = encodeURIComponent(moFdn+PERCENTAGE);

    group('WHEN viewing Audit Results in v1/executions/{id}/audit-results for a right like match for MO', () => {
        check(eacc.getAuditResultsForAGivenMo(eacc.getJobNameFuncTest(), appendPercentageOnMoFdn), {
            'THEN status code is 200': (r) => isStatusOk(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            'AND response body contains only audit results for the specified execution and the correct number of results': (r) => containsExpectedExecutionIdAndNumberOfResults(r, eacc.getJobNameFuncTest(), 25),
            'AND response body contains only audit results for the specified partial managed object': (r) => containsOnlyAuditResultsForGivenManagedObject(r, moFdn)
        });
    });
    eacc.cleanUserSession();
}

function verifyAuditResultsFilteredByInconsistentAuditResultStatus() {
    eacc.getUserSessionId();
    logData("verifyAuditResultsFilteredByInconsistentAuditResultStatus");
    const auditStatus = AuditStatus.INCONSISTENT;

    group('WHEN viewing Audit Results in v1/executions/{id}/audit-results for inconsistent audit status', () => {
        check(eacc.getAuditResultsForAGivenAuditStatus(eacc.getJobNameFuncTest(), auditStatus), {
            'THEN status code is 200': (r) => isStatusOk(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            'AND response body contains only audit results for the specified execution and the correct number of results': (r) => containsExpectedExecutionIdAndNumberOfResults(r, eacc.getJobNameFuncTest(), 58),
            'AND response body contains only audit results for the inconsistent audit status': (r) => containsOnlyAuditResultsForInconsistentAuditStatus(r, auditStatus)
        });
    });
    eacc.cleanUserSession();
}

function verifyAuditResultsFilteredByAuditStatusAndFilteredByMo() {
    eacc.getUserSessionId();
    logData("verifyAuditResultsFilteredByAuditStatusAndFilteredByMo");
    const moFdn = "SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE63dg2ERBST00068,ENodeBFunction=1,EUtranCellFDD=LTE63dg2ERBST00068-7";

    group('WHEN viewing Audit Results in v1/executions/{id}/audit-results for a given audit status and a given MO', () => {
        check(eacc.getAuditResultsForAuditStatusFilterAndAGivenMo(eacc.getJobNameFuncTest(), AuditStatus.INCONSISTENT, moFdn), {
            'THEN status code is 200': (r) => isStatusOk(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            'AND response body contains only audit results for the specified execution and the correct number of results': (r) => containsExpectedExecutionIdAndNumberOfResults(r, eacc.getJobNameFuncTest(), 1),
            'AND response body contains only audit results for the specified managed object': (r) => containsOnlyAuditResultsForGivenManagedObject(r, moFdn)
        });
    });
    eacc.cleanUserSession();
}

function verifyApplyChanges() {
    eacc.getAdminSessionId();
    logData("verifyApplyChanges");
    group('WHEN Applying a 4G Change using POST v1/executions/{id}/audit-results', () => {
        check(eacc.postAuditResultFor4GChange(eacc.getJobNameFuncTest()), {
            'THEN status code is 202': (r) => isStatusAccepted(r.status)
       });
       verifyChangeApplicationState('AND One Change is applied', 1)
    });
    group('WHEN Applying a 2nd 4G Change using POST v1/executions/{id}/audit-results', () => {
        check(eacc.postAuditResultForSecond4GChange(eacc.getJobNameFuncTest()), {
            'THEN status code is 202': (r) => isStatusAccepted(r.status)
       });
       verifyChangeApplicationState('AND Two Changes are applied', 2)
    });
    group('WHEN Applying a 5G Change using POST v1/executions/{id}/audit-results', () => {
        check(eacc.postAuditResultFor5GChange(eacc.getJobNameFuncTest()), {
            'THEN status code is 202': (r) => isStatusAccepted(r.status)
       });
       verifyChangeApplicationState('AND Three Changes are applied', 3)
    });

    group('WHEN Applying a 2nd 5G Change using POST v1/executions/{id}/audit-results', () => {
        check(eacc.postAuditResultForSecond5GChange(eacc.getJobNameFuncTest()), {
            'THEN status code is 202': (r) => isStatusAccepted(r.status)
       });
       verifyChangeApplicationState('AND Four Changes are applied', 4)
    });


    group('WHEN Applying all changes using POST v1/executions/{id}/audit-results', () => {
        check(eacc.applyAllChanges(eacc.getJobNameFuncTest()), {
                'THEN status code is 202': (r) => isStatusAccepted(r.status)
        });
        verifyChangeApplicationState('AND all Changes are applied', 58)
    });

    eacc.cleanAdminSession();
}

function verifyRevertChanges() {
    logData("verifyRevertChanges");

    // Reset back to 'current value' for reversion to fail
    group("WHEN Patch EUtranCellFDD attribute 'upperLayerAutoConfEnabled' back to original value", () => {
        check(ncmp.patchLteFddManagedObject(CM_HANDLE_4G, MANAGED_ELEMENT_4G_ID, FDD_CELL_ID, {"upperLayerAutoConfEnabled": "false"}), {
            'THEN status code is 200': (r) => isStatusOk(r.status)
        })
    });

    eacc.getAdminSessionId();

    group('WHEN Reverting a Change using POST v1/executions/{id}/audit-results', () => {
        check(eacc.postAuditResultFor4GChange(eacc.getJobNameFuncTest(), REVERT_CHANGE), {
            'THEN status code is 202': (r) => isStatusAccepted(r.status)
        });
        verifyChangeReversionState('AND No Changes are reverted', 0);
    });

    group('WHEN Reverting a 2nd Change using POST v1/executions/{id}/audit-results', () => {
        check(eacc.postAuditResultForSecond4GChange(eacc.getJobNameFuncTest(), REVERT_CHANGE), {
            'THEN status code is 202': (r) => isStatusAccepted(r.status)
        });
        verifyChangeReversionState('AND One Change is reverted', 1);
    });

    group('WHEN Reverting all Changes using POST v1/executions/{id}/audit-results', () => {
        check(eacc.revertAllChanges(eacc.getJobNameFuncTest()), {
            'THEN status code is 202': (r) => isStatusAccepted(r.status)
        });
        verifyChangeReversionState('AND all Changes are reverted', 57);
    });

    eacc.cleanAdminSession();
}

// TODO: https://eteamproject.internal.ericsson.com/browse/IDUN-115862
function verifyChangeApplicationState(successfulText, successfulCount) {
    check(eacc.getAuditResults(eacc.getJobNameFuncTest(), getChangeStatusFilter(ChangeStatus.IMPLEMENTATION_IN_PROGRESS), {'page': 0, 'pageSize': 1}), {
        'AND No Changes are in progress': (r) => containsExpectedAuditResults(r, 0, eacc.getJobNameFuncTest(), getChangeStatusFilter(ChangeStatus.IMPLEMENTATION_IN_PROGRESS), 50, 10)
    });
    check(eacc.getAuditResults(eacc.getJobNameFuncTest(), getChangeStatusFilter(ChangeStatus.IMPLEMENTATION_COMPLETE), {'page': 0, 'pageSize': 1}), {
        [successfulText]: (r) => containsExpectedAuditResults(r, successfulCount, eacc.getJobNameFuncTest(), getChangeStatusFilter(ChangeStatus.IMPLEMENTATION_COMPLETE), 0, 10)
    });
    check(eacc.getAuditResults(eacc.getJobNameFuncTest(), getChangeStatusFilter(ChangeStatus.IMPLEMENTATION_FAILED), {'page': 0, 'pageSize': 1}), {
        'AND No Change is failed': (r) => containsExpectedAuditResults(r, 0, eacc.getJobNameFuncTest(), getChangeStatusFilter(ChangeStatus.IMPLEMENTATION_FAILED), 0, 10)
    });
}

function verifyChangeReversionState(successfulText, successfulCount) {
    check(eacc.getAuditResults(eacc.getJobNameFuncTest(), getChangeStatusFilter(ChangeStatus.REVERSION_IN_PROGRESS), {'page': 0, 'pageSize': 1}), {
        'AND No Changes are in progress': (r) => containsExpectedAuditResults(r, 0, eacc.getJobNameFuncTest(), getChangeStatusFilter(ChangeStatus.REVERSION_IN_PROGRESS), 50, 10)
    });
    check(eacc.getAuditResults(eacc.getJobNameFuncTest(), getChangeStatusFilter(ChangeStatus.REVERSION_COMPLETE), {'page': 0, 'pageSize': 1}), {
        [successfulText]: (r) => containsExpectedAuditResults(r, successfulCount, eacc.getJobNameFuncTest(), getChangeStatusFilter(ChangeStatus.REVERSION_COMPLETE), 0, 10)
    });
    check(eacc.getAuditResults(eacc.getJobNameFuncTest(), getChangeStatusFilter(ChangeStatus.REVERSION_FAILED), {'page': 0, 'pageSize': 1}), {
        'AND One Change is failed': (r) => containsExpectedAuditResults(r, 1, eacc.getJobNameFuncTest(), getChangeStatusFilter(ChangeStatus.REVERSION_FAILED), 0, 10)
    });
}

function verifyDeleteJobIsSuccessful() {
    eacc.getAdminSessionId();
    logData("verifyDeleteJobIsSuccessful");
    group('WHEN Job is deleted', () => {
        check(eacc.deleteJob(eacc.getJobNameFuncTest()), {
            'THEN status code is 204': (r) => isStatusNoContent(r.status)
        });
        check(eacc.getJob(), {
            'AND Get Jobs returns status 200': (r) => isStatusOk(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            'AND response body does not contain deleted job': (r) => doesNotContainDeletedJob(r, eacc.getJobNameFuncTest())
        });
    });
    eacc.cleanAdminSession();
}

function verifyDeleteRulesetIsSuccessful() {
    logData("verifyDeleteRulesetIsSuccessful");
    group('WHEN Ruleset is deleted', () => {
        check(eacc.deleteRuleset(), {
            'THEN status code is 204': (r) => isStatusNoContent(r.status)
        });
    });
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

function verifyLogStreamingIsSuccessful() {
    eacc.getUserSessionId();
    logData("getUserSessionId");
    group('WHEN Logs are streamed', () => {
        check(eacc.getLogsWithFilter(FILTER_PAYLOAD), {
             'THEN status code is 200': (r) => isStatusOk(r.status),
             'AND response body contains logs for the right service id in the past 5 minutes': (r) => eacc.responseContainsLog(r)
             });
    });
    eacc.cleanUserSession();
}

function verifyAuditLogIsSuccessful() {
    eacc.getUserSessionId();
    logData("getUserSessionId");
    group('WHEN audit Logs are streamed', () => {
        check(eacc.getLogsWithFilter(AUDIT_LOG_FILTER_PAYLOAD), {
            'THEN status code is 200': (r) => isStatusOk(r.status),
            'AND response body contains an audit log for the correct service id in the past 5 minutes': (r) => eacc.responseContainsLog(r)
        });
    });
    eacc.cleanUserSession();
}

module.exports = {
    verifyPostInvalidRulesetFails,
    verifyPostRulesetIsSuccessful,
    verifyUpdateValidRulesetIsSuccessful,
    verifyUpdateInvalidRulesetFails,
    verifyPostScopeIsSuccessful,
    verifyPostJobIsSuccessful,
    verifyExecutionsExist,
    verifyAuditResults,
    verifyAuditResultsFilteredByFullMatchForMo,
    verifyAuditResultsFilteredByPartialMatchForMo,
    verifyAuditResultsFilteredByLeftLikeMatchForMoFdn,
    verifyAuditResultsFilteredByRightLikeMatchForMoFdn,
    verifyAuditResultsFilteredByInconsistentAuditResultStatus,
    verifyAuditResultsFilteredByAuditStatusAndFilteredByMo,
    verifyApplyChanges,
    verifyRevertChanges,
    verifyPutJobIsSuccessful,
    verifyReadScopeIsSuccessful,
    verifyUpdateScopeIsSuccessful,
    verifyDeleteJobIsSuccessful,
    verifyDeleteRulesetIsSuccessful,
    verifyDeleteScopeIsSuccessful,
    verifyLogStreamingIsSuccessful,
    verifyAuditLogIsSuccessful
}


