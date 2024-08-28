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

import { sleep } from 'k6';
import { Trend } from 'k6/metrics';
import {
  createSession,
  httpDelete,
  httpFile,
  httpGet,
  httpPost,
  httpPostJson,
  httpRequest,
  logData,
} from './common.js';
import {
  APP_JSON_PARAMS,
  APPLY_CHANGE,
  APPLY_CHANGES_TIMEOUT,
  AuditStatus,
  ChangeStatus,
  EXECUTION_URI,
  FILTER_PAYLOAD,
  getAuditStatusFilter,
  getChangeStatusFilter,
  JOB_URI, REVERT_CHANGE,
  RULESET_URI,
  SCOPE_URI,
  SEARCH_ENGINE_ENTRIES_URI,
} from './eacc-constants.js';
import {
  EACC_INGRESS_URL,
  EXECUTION_SCHEDULE_OFFSET,
  INGRESS_EACC_ADMIN_PARAMS,
  INGRESS_EACC_USER_PARAMS,
  INGRESS_LOGIN_URI,
  INGRESS_URL,
} from './constants.js';

const FUNC_TEST_RULESET_FILE = open("../../resources/func_test_ruleset.csv");
const FUNC_TEST_SCOPE_FILE = open("../../resources/func_test_scope.csv");
const FUNC_TEST_UPDATED_RULESET_FILE = open("../../resources/func_test_update_ruleset.csv");
const FUNC_TEST_UPDATED_SCOPE_FILE = open("../../resources/func_test_update_scope.csv");
const FUNC_TEST_INVALID_RULESET_FILE = open("../../resources/func_test_invalid_mo_and_attr_ruleset.csv");

const RULESET_FILE_LOAD_TEST = open("../../resources/load_test_ruleset.csv");
const SCOPE_FILE_LOAD_TEST = open("../../resources/load_test_scope.csv");
const TIMESTAMP = Date.now();

const RULESET_NAME_FUNC_TEST = `func_test_ruleset_${TIMESTAMP}`;
const RULESET_NAME_LOAD_TEST = `load_test_ruleset_${TIMESTAMP}`;
const SCOPE_NAME = `test_scope_${TIMESTAMP}`;
const executionTimeLoadTest = new Trend('execution_time_load_test');
const numberOfNodesLoadTest = new Trend('nodes_count_load_test');
const mosAuditedLoadTest = new Trend('mos_audited_load_test');
const attributesAuditedLoadTest = new Trend('attributes_audited_load_test');
const inconsistenciesIdentifiedLoadTest = new Trend('inconsistencies_identified_load_test');
const timeTakenToApplyAllChangesLoadTest = new Trend('apply_changes_time_load_test');
const timeTakenToRevertAllChangesLoadTest = new Trend('revert_changes_time_load_test');


let cronExpressionFuncTest;
let cronExpressionLoadTest;
let jobNameFuncTest;
let jobNameLoadTest;
let rulesetId;
let applyChangesStartTime;
let applyChangesEndTime;
let revertChangesStartTime;
let revertChangesEndTime;
let timeTakenToApplyAllChangesVar;
let timeTakenToRevertAllChangesVar;

let userSession;
let adminSession;

function getUserSessionId(options = {}) {
  if (!userSession) {
    userSession = createSession(
      INGRESS_URL,
      INGRESS_LOGIN_URI,
      "",
      INGRESS_EACC_USER_PARAMS,
      options
    );
    logData("CREATE USER SESSION", INGRESS_EACC_USER_PARAMS);
    logData("CREATE USER SESSION", userSession);
  }
  return userSession;
}

function cleanUserSession() {
  userSession = undefined;
}

function getAdminSessionId(options = {}) {
  if (!adminSession) {
    adminSession = createSession(
      INGRESS_URL,
      INGRESS_LOGIN_URI,
      "",
      INGRESS_EACC_ADMIN_PARAMS,
      options
    );
    logData("CREATE ADMIN SESSION", INGRESS_EACC_ADMIN_PARAMS);
    logData("CREATE ADMIN SESSION", adminSession);
  }
  return adminSession;
}

function cleanAdminSession() {
  adminSession = undefined;
}

function getCronExpressionFuncTest() {
  if (!cronExpressionFuncTest) {
      const now = new Date();
      now.setSeconds(now.getSeconds() + EXECUTION_SCHEDULE_OFFSET);
      cronExpressionFuncTest = `${now.getUTCSeconds()} ${now.getUTCMinutes()} ${now.getUTCHours()} ${now.getUTCDate()} ${
        now.getUTCMonth() + 1
      } ${now.getUTCDay()}`;
  }
  return cronExpressionFuncTest;
}

function getCronExpressionForLoadTest() {
  if (!cronExpressionLoadTest) {
      const now = new Date();
      now.setSeconds(now.getSeconds() + EXECUTION_SCHEDULE_OFFSET);
      cronExpressionLoadTest = `${now.getUTCSeconds()} ${now.getUTCMinutes()} ${now.getUTCHours()} ${now.getUTCDate()} ${
        now.getUTCMonth() + 1
      } ${now.getUTCDay()}`;
  }
  return cronExpressionLoadTest;
}

function getJobNameFuncTest() {
  if (!jobNameFuncTest) {
    const time = Date.now();
    jobNameFuncTest = "test_job_func_test_" + time;
  }
  return jobNameFuncTest;
}

function getJobNameForLoadTest() {
  if (!jobNameLoadTest) {
    const time = Date.now();
    jobNameLoadTest = "load_test_job_" + time;
  }
  return jobNameLoadTest;
}

function createJobObjectFuncTest() {
  let cronExpression = getCronExpressionFuncTest();
  let jobName = getJobNameFuncTest();
  return {
    jobName: jobName,
    schedule: cronExpression,
    rulesetName: RULESET_NAME_FUNC_TEST,
    scopeName: SCOPE_NAME,
  };
}

function createJobObjectForLoadTest() {
  let cronExpression = getCronExpressionForLoadTest();
  let jobName = getJobNameForLoadTest();
  return {
    jobName: jobName,
    schedule: cronExpression,
    rulesetName: RULESET_NAME_LOAD_TEST,
    scopeName: SCOPE_NAME,
  };
}

function getFuncTestRuleset() {
  return {
    rulesetName: RULESET_NAME_FUNC_TEST,
    fileName: httpFile(FUNC_TEST_RULESET_FILE, "test_ruleset.csv"),
  };
}

function getInvalidRuleset() {
    return {
        rulesetName: RULESET_NAME_FUNC_TEST,
        fileName: httpFile(FUNC_TEST_INVALID_RULESET_FILE, "func_test_invalid_mo_and_attr_ruleset.csv")
    }
}

function getValidUpdatedRuleset() {
  return {
    fileName: httpFile(FUNC_TEST_UPDATED_RULESET_FILE, "func_test_update_ruleset.csv")
  }
}

function getInvalidUpdatedRuleset() {
  return {
    fileName: httpFile(FUNC_TEST_INVALID_RULESET_FILE, "func_test_invalid_mo_and_attr_ruleset.csv")
  }
}

function getExpectedRuleValidationErrors() {
  return [
    {
      "lineNumber": 2,
      "errorType": "Invalid MO.",
      "errorDetails": "MO not found in Managed Object Model.",
      "additionalInfo": ""
    },
    {
      "lineNumber": 3,
      "errorType": "Invalid Attribute name.",
      "errorDetails": "Attribute not found in Managed Object Model.",
      "additionalInfo": ""
    },
    {
      "lineNumber": 4,
      "errorType": "Invalid Attribute value.",
      "errorDetails": "Attribute value is invalid according to the Managed Object Model.",
      "additionalInfo": ""
    }
  ]
}

function getRulesetForLoadTest() {
  return {
    rulesetName: RULESET_NAME_LOAD_TEST,
    fileName: httpFile(RULESET_FILE_LOAD_TEST, "load_test_ruleset.csv"),
  };
}

function getScopeForFuncTest() {
  return {
    scopeName: SCOPE_NAME,
    fileName: httpFile(FUNC_TEST_SCOPE_FILE, "func_test_scope.csv"),
  };
}

function getScopeForLoadTest() {
  return {
    scopeName: SCOPE_NAME,
    fileName: httpFile(SCOPE_FILE_LOAD_TEST, "load_test_scope.csv"),
  };
}

function getFuncTestFdnBody() {
  return {
    fileName: httpFile(FUNC_TEST_UPDATED_SCOPE_FILE, "func_test_update_scope.csv"),
  };
}

function createMetadata(uuid) {
  return {
    scopeName: SCOPE_NAME,
    uri: "v1/scopes/"+ uuid,
    id: uuid
  };
}

function verifyFuncTestReadScope(responseText) {
    let expectedFile = FUNC_TEST_SCOPE_FILE + "\n"
    return expectedFile == responseText
}

function verifyFuncTestUpdatedScope(responseText) {
    let expectedFile = FUNC_TEST_UPDATED_SCOPE_FILE + "\n"
    return expectedFile == responseText
}

function readScope(id) {
    return httpGet(EACC_INGRESS_URL, SCOPE_URI + "/" + id, {}, {});
}

function getAllScopes() {
    return httpGet(EACC_INGRESS_URL, SCOPE_URI, {}, {});
}

function updateScope(id, body) {
    return httpRequest('PUT', EACC_INGRESS_URL, SCOPE_URI + "/" + id, body, {});
}

function getExecutionId(jobName) {
  let executions = httpGet(
    EACC_INGRESS_URL,
    EXECUTION_URI + "?jobName=" + jobName,
    {}
  ).body;
  let json = JSON.parse(executions);
  return json[0]["id"];
}

function getExecutions(jobName) {
  return httpGet(EACC_INGRESS_URL, EXECUTION_URI + "?jobName=" + jobName);
}

function getAuditResults(jobName, filter = undefined, paginationParams = {}) {
  let endpoint = `/${getExecutionId(jobName)}/audit-results`;
  let queryParamaters = "";

  if (filter !== undefined) {
    queryParamaters += `filter=${filter}`
  }

  if (paginationParams.page !== undefined && paginationParams.pageSize !== undefined) {
    if (queryParamaters.length > 0) {
      queryParamaters += "&"
    }
    queryParamaters += `page=${paginationParams.page}&pageSize=${paginationParams.pageSize}`
  }

  endpoint += "?" + queryParamaters;

  return httpGet(EACC_INGRESS_URL, EXECUTION_URI + endpoint, {});
}

function getAuditResultsForAGivenMo(jobName, managedObjectFdn) {
  return httpGet(
    EACC_INGRESS_URL,
    EXECUTION_URI + "/" + getExecutionId(jobName) + "/audit-results?filter=managedObjectFdn:" + managedObjectFdn,
    {}
  );
}

function getAuditResultsForAGivenAuditStatus(jobName, auditStatus) {
  return httpGet(
    EACC_INGRESS_URL,
    EXECUTION_URI + "/" + getExecutionId(jobName) + "/audit-results?filter=auditStatus:" + auditStatus,
    {}
  );
}

function getAuditResultsForAuditStatusFilterAndAGivenMo(jobName, auditStatus, managedObjectFdn) {
  return httpGet(
    EACC_INGRESS_URL,
    EXECUTION_URI + "/" + getExecutionId(jobName) + "/audit-results?filter=" + getAuditStatusFilter(auditStatus) + "$" + "managedObjectFdn:" + managedObjectFdn,
    {}
  );
}

// Ensure
// auditStatus = Inconsistent
// managedObjectFdn = SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE63dg2ERBST00068
// attributeName = upperLayerAutoConfEnabled
// currentValue=false, preferredValue=true
function postAuditResultFor4GChange(jobName, operation = APPLY_CHANGE) {
  const auditResultsJson = JSON.parse(getAuditResults(jobName, getAuditStatusFilter(AuditStatus.INCONSISTENT)).body);
  const desiredObject = auditResultsJson.results.find(obj =>
    obj.managedObjectFdn === "SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE63dg2ERBST00068,ENodeBFunction=1,EUtranCellFDD=LTE63dg2ERBST00068-12"
    && obj.attributeName === "upperLayerAutoConfEnabled");
  logData("desiredObject for 4G Change: ", desiredObject);

  const eaccApprovedAuditResults = getEaccApprovedAuditResultsObj(desiredObject.id, operation);

  logData("EaccProposedChange is : ", eaccApprovedAuditResults);

  return httpPostJson(EACC_INGRESS_URL, EXECUTION_URI + "/" + desiredObject.executionId + "/audit-results", eaccApprovedAuditResults, APP_JSON_PARAMS);
}

function postAuditResultForSecond4GChange(jobName, operation = APPLY_CHANGE) {
  const auditResultsJson = JSON.parse(getAuditResults(jobName, getAuditStatusFilter(AuditStatus.INCONSISTENT)).body);
  const desiredObject = auditResultsJson.results.find(obj =>
    obj.managedObjectFdn === "SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE63dg2ERBST00075,ENodeBFunction=1"
    && obj.attributeName === "timePhaseMaxDeviationOtdoa");
  logData("desiredObject for 4G Change: ", desiredObject);

  const eaccApprovedAuditResults = getEaccApprovedAuditResultsObj(desiredObject.id, operation);

  logData("EaccProposedChange is : ", eaccApprovedAuditResults);

  return httpPostJson(EACC_INGRESS_URL, EXECUTION_URI + "/" + desiredObject.executionId + "/audit-results", eaccApprovedAuditResults, APP_JSON_PARAMS);
}

// Ensure
// auditStatus = Inconsistent
// managedObjectFdn = SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR127gNodeBRadioT00005,ManagedElement=NR127gNodeBRadioT00005
// attributeName = ailgDlPrbLoadLevel
// currentValue=0, preferredValue=1
function postAuditResultFor5GChange(jobName) {
  const auditResultsJson = JSON.parse(getAuditResults(jobName, getAuditStatusFilter(AuditStatus.INCONSISTENT)).body);
  const desiredObject = auditResultsJson.results.find(obj =>
    obj.managedObjectFdn === "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR127gNodeBRadioT00005,ManagedElement=NR127gNodeBRadioT00005,GNBDUFunction=1,NRCellDU=NR127gNodeBRadioT00005-3"
    && obj.attributeName === "ailgDlPrbLoadLevel");
  logData("desiredObject for 5G Change: ", desiredObject);

  logData("EaccApprovedAuditResult is : ", getEaccApprovedAuditResultsObj(desiredObject.id));
  let httpResponse = httpPostJson(EACC_INGRESS_URL, EXECUTION_URI + "/" + desiredObject.executionId + "/audit-results", getEaccApprovedAuditResultsObj(desiredObject.id), APP_JSON_PARAMS);

  return httpResponse;
}

function postAuditResultForSecond5GChange(jobName) {
  const auditResultsJson = JSON.parse(getAuditResults(jobName, getAuditStatusFilter(AuditStatus.INCONSISTENT)).body);
  const desiredObject = auditResultsJson.results.find(obj =>
    obj.managedObjectFdn === "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR174gNodeBRadioT00023,ManagedElement=NR174gNodeBRadioT00023,GNBDUFunction=1"
    && obj.attributeName === "endpointResDepHEnabled");
  logData("desiredObject for 5G Change: ", desiredObject);

  logData("EaccApprovedAuditResult is : ", getEaccApprovedAuditResultsObj(desiredObject.id));
  let httpResponse = httpPostJson(EACC_INGRESS_URL, EXECUTION_URI + "/" + desiredObject.executionId + "/audit-results", getEaccApprovedAuditResultsObj(desiredObject.id), APP_JSON_PARAMS);

  return httpResponse;
}

function applyAllChanges(jobName) {
  logData("applyAllChanges");
  const auditResultsBody = JSON.parse(getAuditResults(jobName, getAuditStatusFilter(AuditStatus.INCONSISTENT)).body);
  const totalCountOfInconsistencies = auditResultsBody.totalElements;

  logData("The number of Inconsistencies to apply is: ", totalCountOfInconsistencies)

    const eaccApprovedAuditResults = {
      auditResultIds: [],
      approveForAll: true,
      operation: 'APPLY_CHANGE'
    };

  const httpResponse = httpPostJson(EACC_INGRESS_URL, EXECUTION_URI + "/" + getExecutionId(jobName) + "/audit-results", eaccApprovedAuditResults, APP_JSON_PARAMS, APPLY_CHANGES_TIMEOUT);

  logData("Apply all changes response status: ", httpResponse.status);

  return httpResponse;
}

function revertAllChanges(jobName) {
  logData("revertAllChanges");
  const auditResultsBody = JSON.parse(getAuditResults(jobName, getAuditStatusFilter(AuditStatus.INCONSISTENT)).body);
  const eaccApprovedAuditResults = {
    auditResultIds: [],
    approveForAll: true,
    operation: 'REVERT_CHANGE'
  };

  logData("EaccProposedChange is : ", eaccApprovedAuditResults);

  return httpPostJson(EACC_INGRESS_URL, EXECUTION_URI + "/" + getExecutionId(jobName) + "/audit-results", eaccApprovedAuditResults, APP_JSON_PARAMS, APPLY_CHANGES_TIMEOUT);
}

function getNumberOfChanges(jobName, filter) {
  const auditResultsBody = JSON.parse(getAuditResults(jobName, filter).body);
  const totalCountOfInconsistencies = auditResultsBody.totalElements;

  return totalCountOfInconsistencies;
}

function startApplyChangesTimer() {
  applyChangesStartTime = new Date().getTime();
}

function endApplyChangesTimer() {
  applyChangesEndTime = new Date().getTime();
  timeTakenToApplyAllChangesVar = Math.floor((applyChangesEndTime - applyChangesStartTime)/1000);
}

function startRevertChangesTimer() {
  revertChangesStartTime = new Date().getTime();
}

function endRevertChangesTimer() {
  revertChangesEndTime = new Date().getTime();
  timeTakenToRevertAllChangesVar = Math.floor((revertChangesEndTime - revertChangesStartTime)/1000);
}

function getEaccApprovedAuditResultsObj(auditResultId, operation = APPLY_CHANGE) {
  const auditResultIds = Array.isArray(auditResultId) ? auditResultId : [auditResultId];

    return {
      auditResultIds: auditResultIds,
      operation
    };
}

function getJob() {
  return httpGet(EACC_INGRESS_URL, JOB_URI, {}, {});
}

function postJob(job = {}) {
  return httpPostJson(
    EACC_INGRESS_URL,
    JOB_URI,
    job,
    APP_JSON_PARAMS
  );
}

function putJob(job = {}) {
  let response = {};
  response = httpRequest('PUT', EACC_INGRESS_URL, JOB_URI + "/" + job.jobName, JSON.stringify(job), APP_JSON_PARAMS);
  return response;
}

function deleteJob(jobName) {
  return httpDelete(EACC_INGRESS_URL, JOB_URI + "/" + jobName, {}, {});
}

function postRuleset(ruleset = {}) {
  let httpResponse = httpPost(EACC_INGRESS_URL, RULESET_URI, ruleset, {});
  let json = JSON.parse(httpResponse.body);
  console.log("json[id}: " + json["id"]);
  if ("id" in json === true) {
    rulesetId = json["id"];
  }
  return httpResponse;
}

function putRuleset(ruleset = {}) {
  let response = {};
  console.log("rulesetId: " + rulesetId);
  response = httpRequest('PUT', EACC_INGRESS_URL, RULESET_URI + "/" + rulesetId, ruleset, {});
  return response;
}

function postFuncTestScope() {
  let httpResponse = httpPost(EACC_INGRESS_URL, SCOPE_URI, getScopeForFuncTest(), {});
  return httpResponse;
}

function post5GScope() {
  let httpResponse = httpPost(EACC_INGRESS_URL, SCOPE_URI, get5GScope(), {});
  return httpResponse;
}

function postScopeForLoadTest() {
  let httpResponse = httpPost(EACC_INGRESS_URL, SCOPE_URI, getScopeForLoadTest(), {});
  return httpResponse;
}

function deleteRuleset() {
  return httpDelete(EACC_INGRESS_URL, RULESET_URI + "/" + rulesetId, {}, {});
}

function findUUIDOfScopeName()
{
    let currentScopesJson = JSON.parse(getAllScopes().body);
    for(let index in currentScopesJson)
    {
        if(currentScopesJson[index].scopeName == SCOPE_NAME)
        {
            return currentScopesJson[index].id;
        }
    }
}

function deleteScope(id) {
  return httpDelete(EACC_INGRESS_URL, SCOPE_URI + "/" + id, {}, {});
}

function getLogsWithFilter(filterPayload) {
    let date = new Date(Date.now());
    let dateDay = date.getDate().toString().padStart(2, '0');
    let dateMonthNum = date.getMonth() + 1;
    let dateMonth = dateMonthNum.toString().padStart(2, '0');
    let dateYear = date.getFullYear().toString();
    let formattedDate = dateYear + "." + dateMonth + "." + dateDay;

    let rAppIndex = "/rapps-logs-" + formattedDate;
    let rAppURI = rAppIndex.concat(SEARCH_ENGINE_ENTRIES_URI);
    return httpRequest('GET', EACC_INGRESS_URL, rAppURI, JSON.stringify(filterPayload), APP_JSON_PARAMS);
}

function responseContainsLog(response) {
    let jsonString = JSON.parse((response.body).toString());
    let latestLogSource = jsonString["hits"]["hits"];
  return !(latestLogSource.length == 0);
}

function printExecutionStats(execution){
  let nodeCount;
  const json = JSON.parse(execution.body);

  for (const exec of json) {
    const start = new Date(exec.executionStartedAt);
    const end = new Date(exec.executionEndedAt);
    const totalTimeInSeconds = Math.floor((end - start)/1000);

    executionTimeLoadTest.add(totalTimeInSeconds);
    mosAuditedLoadTest.add(exec.totalMosAudited);
    attributesAuditedLoadTest.add(exec.totalAttributesAudited);
    inconsistenciesIdentifiedLoadTest.add(exec.inconsistenciesIdentified);
    timeTakenToApplyAllChangesLoadTest.add(timeTakenToApplyAllChangesVar);
    timeTakenToRevertAllChangesLoadTest.add(timeTakenToRevertAllChangesVar);
    logData("Total Execution Time for Load Test: ", totalTimeInSeconds);
    logData("Total Time to apply changes for Load Test: ", timeTakenToApplyAllChangesVar);
  }

  nodeCount = (SCOPE_FILE_LOAD_TEST.split('\n')).length - 1;
  numberOfNodesLoadTest.add(nodeCount);
  logData("Number of nodes processed: ", nodeCount);
}

module.exports = {
  getExecutions,
  getAuditResults,
  getAuditResultsForAGivenMo,
  getAuditResultsForAGivenAuditStatus,
  getAuditResultsForAuditStatusFilterAndAGivenMo,
  postAuditResultFor4GChange,
  postAuditResultForSecond4GChange,
  postAuditResultFor5GChange,
  postAuditResultForSecond5GChange,
  applyAllChanges,
  revertAllChanges,
  startApplyChangesTimer,
  endApplyChangesTimer,
  startRevertChangesTimer,
  endRevertChangesTimer,
  getNumberOfChanges,
  getExecutionId,
  postJob,
  putJob,
  deleteJob,
  getJobNameFuncTest,
  getJobNameForLoadTest,
  getJob,
  createJobObjectFuncTest,
  createJobObjectForLoadTest,
  getFuncTestRuleset,
  getInvalidRuleset,
  getValidUpdatedRuleset,
  getInvalidUpdatedRuleset,
  getExpectedRuleValidationErrors,
  getRulesetForLoadTest,
  postRuleset,
  putRuleset,
  getScopeForLoadTest,
  getScopeForFuncTest,
  verifyFuncTestReadScope,
  getFuncTestFdnBody,
  createMetadata,
  readScope,
  getAllScopes,
  postFuncTestScope,
  postScopeForLoadTest,
  findUUIDOfScopeName,
  deleteScope,
  updateScope,
  verifyFuncTestUpdatedScope,
  deleteRuleset,
  getUserSessionId,
  cleanUserSession,
  getAdminSessionId,
  cleanAdminSession,
  getLogsWithFilter,
  responseContainsLog,
  printExecutionStats,
};
