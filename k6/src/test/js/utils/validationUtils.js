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
import * as eacc from "../modules/eacc.js";
import { logData } from "../modules/common.js";
import { EXECUTION_STATUS_AUDIT_SUCCESSFUL, EXECUTION_STATUS_AUDIT_FAILED, EXECUTION_STATUS_AUDIT_IN_PROGRESS, EXECUTION_STATUS_AUDIT_PARTIALLY_SUCCESSFUL, EXECUTION_STATUS_CHANGES_IN_PROGRESS, EXECUTION_STATUS_CHANGES_FAILED, EXECUTION_STATUS_CHANGES_SUCCESSFUL, EXECUTION_STATUS_CHANGES_PARTIALLY_SUCCESSFUL } from "../modules/constants.js";
import { sleep } from "k6";

const JSESSION_ID_PATTERN = /JSESSIONID=[\d\w-]+/;
const DN_PREFIX_PATTERN = /[SubNetwork=(\w\d_\-),]+[MeContext=[\w\d_-]+]?/;

export function isStatusOk(status) {
    return status === 200;
}

export function isStatusCreated(status) {
    return status === 201;
}

export function isStatusAccepted(status) {
    return status === 202;
}

export function isStatusNoContent(status) {
    return status === 204;
}

export function isStatusNotFound(status) {
    return status === 404;
}

export function isStatusForbidden(status) {
    return status === 403;
}

export function isStatusBadRequest(status) {
    return status === 400;
}

export function isStatusTooManyRequests(status) {
    return status === 429;
}

export function isJsonValid(data) {
    try {
        JSON.parse(data);
    } catch (e) {
        return false;
    }
    return true;
}

export function isSessionValid(session) {
    return JSESSION_ID_PATTERN.test(session);
}

export function containsHeader(headers, header, expected) {
    return headers[header] === expected;
}

export function hasDnPrefixAttribute(auditResults) {
    for (const auditResult of auditResults) {
        if (!containsDnPrefixAttribute(auditResult)) {
            return false;
        }
    }

    return true;
}

export function containsDnPrefixAttribute(data) {
    const managedElement = JSON.parse(data)['ericsson-enm-ComTop:ManagedElement'][0];
    const attributes = managedElement["attributes"];
    if (!attributes) {
        return false;
    }
    const dnPrefix = attributes["dnPrefix"];
    return dnPrefix != undefined && DN_PREFIX_PATTERN.test(dnPrefix);
}

export function containsExpectedJob(responseBody, expectedJob) {
    const body = JSON.parse(responseBody);
    const keys = Object.keys(expectedJob);
    logData('keys: ', keys);
    logData('responseBody: ', body);
    logData('expectedJob: ', expectedJob);

    return keys.every(key => body[key] === expectedJob[key]);
}

export function containsExpectedRuleset(responseBody, rulesetName) {
    const body = JSON.parse(responseBody);
    return body.rulesetName === rulesetName;
}

export function containsExpectedScope(responseBody, scopeName) {
    const body = JSON.parse(responseBody);
    return body.scopeName === scopeName &&
           body.uri.startsWith("v1/scopes/") &&
           body.id !== null;
}

export function containsExpectedMetadata(responseBody, expectedMetadata) {
    const body = JSON.parse(responseBody);
    const keys = Object.keys(expectedMetadata);
    return body.scopeName === expectedMetadata.scopeName &&
           body.uri === expectedMetadata.uri &&
           body.id === expectedMetadata.id;
}

export function containsExpectedExecutionIdAndNumberOfResults(response, jobName, expectedCount) {
    let json = JSON.parse(response.body);
    let executionId = eacc.getExecutionId(jobName);
    let count = 0;

    for (const auditResult of json.results) {
        count++;
        if (auditResult.executionId != executionId) {
            return false;
        }
    }

    logData('Number of results found for execution: ', count);
    return count === expectedCount;
}

export function containsExpectedAuditResults(response, expectedCount, eaccJobName, filter, maxRetries = 5, timeout = 10) {
    let count = JSON.parse(response.body).totalElements;
    let retryCount = 0;

    while (retryCount++ < maxRetries) {
        if (count === expectedCount) {
            return true;
        }
        logData(`Retrying containsExpectedAuditResults. Prev count was: ${count} / ${expectedCount} `);
        sleep(timeout);
        response = eacc.getAuditResults(eaccJobName, filter, {'page': 0, 'pageSize': 1});
        count = JSON.parse(response.body).totalElements;
    }
    return count === expectedCount;
}

export function containsOnlyAuditResultsForGivenManagedObject(response, expectedManagedObject) {

    let regex = new RegExp(expectedManagedObject, "g");
    let json = JSON.parse(response.body);

    let containsOnlyAuditResultsForGivenManagedObject=false;

     for (const auditResult of json.results) {
        if(auditResult.managedObjectFdn.match(regex)){
            containsOnlyAuditResultsForGivenManagedObject=true;
        }else{
            return false;
        }
     }
     return containsOnlyAuditResultsForGivenManagedObject;
}

export function containsExpectedAuditResultsForMo(response, expectedManagedObject) {
    let json = JSON.parse(response.body);
    let containsOnlyAuditResultsForGivenManagedObject=false;

    for (const auditResult of json.results) {
       if(auditResult.managedObjectFdn === expectedManagedObject &&
                (auditResult.attributeName == "upperLayerAutoConfEnabled" && auditResult.preferredValue == "true") ||
                (auditResult.attributeName == "outOfCoverageThreshold" && auditResult.preferredValue == "20")){
           containsOnlyAuditResultsForGivenManagedObject=true;
       }else{
           return false;
       }
    }
    return containsOnlyAuditResultsForGivenManagedObject;
}

export function containsOnlyAuditResultsForInconsistentAuditStatus(response, expectedAuditStatus) {

    let json = JSON.parse(response.body);
    let containsOnlyAuditResultsForInconsistentAuditStatus=false;

     for (const auditResult of json.results) {
        if(auditResult.auditStatus === expectedAuditStatus){
            containsOnlyAuditResultsForInconsistentAuditStatus=true;
        }else{
            return false;
        }
     }
     return containsOnlyAuditResultsForInconsistentAuditStatus;
}

export function containsExpectedCountsInExecution(audit, cells, attr, inconsistencies) {
    let json = JSON.parse(audit.body);

    for (const executionResult of json) {
    logData('cells: ', executionResult.totalMosAudited);
    logData('attr: ', executionResult.totalAttributesAudited);
    logData('inconsistencies: ', executionResult.inconsistenciesIdentified);
        if (executionResult.totalMosAudited == cells && executionResult.totalAttributesAudited == attr && executionResult.inconsistenciesIdentified == inconsistencies) {
             return true;
        }
    }

    return false;
}

export function containsEmptyArray(response) {
    let json = JSON.parse(response.body);
    if (json.length === 0) {
        return true;
    } else {
        return false;
    }
}

export function isExecutionCompleted(jobName) {
  let executionStatus = EXECUTION_STATUS_AUDIT_IN_PROGRESS;
  while (executionStatus == EXECUTION_STATUS_AUDIT_IN_PROGRESS) {
    sleep(20);
    const res = eacc.getExecutions(jobName);
    const json = JSON.parse(res.body);
    for (const auditResult of json) {
      if (
        auditResult.jobName == jobName &&
        (auditResult.executionStatus == EXECUTION_STATUS_AUDIT_SUCCESSFUL ||
          auditResult.executionStatus == EXECUTION_STATUS_AUDIT_FAILED ||
          auditResult.executionStatus == EXECUTION_STATUS_AUDIT_PARTIALLY_SUCCESSFUL)
      ) {
        executionStatus = auditResult.executionStatus;
      }
    }
  }
  return true;
}

export function isResponseSuccessfulForExecution(response, jobName) {
    let json = JSON.parse(response.body);

    for (const auditResult of json) {
        if(auditResult.jobName == jobName && (auditResult.executionStatus == EXECUTION_STATUS_AUDIT_SUCCESSFUL || auditResult.executionStatus == EXECUTION_STATUS_AUDIT_PARTIALLY_SUCCESSFUL)) {
            return true;
        }
    }
    return false;
}

export function isRequiredCmHandlePresent(responseBody, requiredCmHandle) {
    let json = JSON.parse(responseBody);
    for (let counter = 0; json.length; counter++) {
        if(json[counter]['cmHandle'] == requiredCmHandle) {
            return true;
        }
    }
    return false;
}

export function containsRuleValidationErrors(responseBody, expectedRuleValidationErrors) {
    const json = JSON.parse(responseBody);

    if (!json.hasOwnProperty("ruleValidationErrors")) return false;

    const ruleValidationErrors = json['ruleValidationErrors'];

    const sortedJson1 = JSON.stringify(ruleValidationErrors.slice().sort().sort((a, b) => a.lineNumber - b.lineNumber));
    const sortedJson2 = JSON.stringify(expectedRuleValidationErrors.slice().sort().sort((a, b) => a.lineNumber - b.lineNumber));

    return sortedJson1 === sortedJson2;
}

export function doesNotContainDeletedJob(response, deletedJobName) {
    let jobs = JSON.parse(response.body);

    logData('Jobs after job deletion: ', jobs);
    if (jobs.length != 0) {
        for (const job of jobs) {
            if (job.jobName == deletedJobName) {
                return false;
            }
        }
    }

    return true
}

export function doesNotContainDeletedScope(response, deletedScopeId) {
    let scopes = JSON.parse(response.body);

    logData('Scopes after scope deletion: ', scopes);
    if (scopes.length != 0) {
        for (const scope of scopes) {
            if (scope.id == deletedScopeId) {
                return false;
            }
        }
    }

    return true
}