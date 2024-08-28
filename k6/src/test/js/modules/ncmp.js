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

import { check } from 'k6';

import { CM_HANDLE_4G, CM_HANDLE_5G } from './eacc-constants.js';
import { createSession, httpGet, httpPatchJson, httpPostJson, logData } from './common.js';
import {
    containsDnPrefixAttribute,
    isJsonValid,
    isRequiredCmHandlePresent,
    isStatusOk
} from '../utils/validationUtils.js';

import { APPLICATION_JSON, INGRESS_TEST_USER_PARAMS, INGRESS_LOGIN_URI, INGRESS_URL } from './constants.js';
import {
    DATASTORE_OPERATIONAL,
    DATASTORE_RUNNING,
    LTE_CM_HANDLES,
    ME_RESOURCE_ID,
    NCMP_BASE_PATH,
    NCMP_SEARCHES_URI,
    NR_CM_HANDLES,
} from './ncmp-constants.js';

let sessionId;

function getSessionId(options = {}) {
    if (!sessionId) {
        sessionId = createSession(INGRESS_URL, INGRESS_LOGIN_URI, '', INGRESS_TEST_USER_PARAMS, options);
        logData('CREATE SESSION', sessionId);
    }
    return sessionId;
}

function clearSession() {
  sessionId = undefined;
}

function getSessionParams() {
    return {
        headers: {
            'Content-Type': APPLICATION_JSON,
            'Cookie': getSessionId()
        }
    };
}

function getLteCmHandles(options = {}) {
    const response = httpPostJson(INGRESS_URL, NCMP_SEARCHES_URI, LTE_CM_HANDLES, getSessionParams(), options);
    check4GCmHandleResponse(response);
    return getCmHandles(response.body);
}

function getNrCmHandles(options = {}) {
    const response = httpPostJson(INGRESS_URL, NCMP_SEARCHES_URI, NR_CM_HANDLES, getSessionParams(), options);
    check5GCmHandleResponse(response);
    return getCmHandles(response.body);
}

function check4GCmHandleResponse(response) {
    check(response, {
        'THEN [POST] CM Handles status code is 200': (r) => isStatusOk(r.status),
        'AND [POST] CM Handles body is valid JSON': (r) => isJsonValid(r.body),
        'AND the required 4G CM Handle is present': (r) => isRequiredCmHandlePresent(r.body, CM_HANDLE_4G)
    });
}

function check5GCmHandleResponse(response) {
    check(response, {
        'THEN [POST] CM Handles status code is 200': (r) => isStatusOk(r.status),
        'AND [POST] CM Handles body is valid JSON': (r) => isJsonValid(r.body),
        'AND the required 5G CM Handle is present': (r) => isRequiredCmHandlePresent(r.body, CM_HANDLE_5G),
    });
}

function getCmHandles(body) {
    let uniqueCmHandles = [];
    let json = JSON.parse(body);
    json.forEach((item) => {
        addCmHandle(item['cmHandle'], uniqueCmHandles);
    });
    logData('CM Handles', uniqueCmHandles.length)
    return uniqueCmHandles;
}

function addCmHandle(cmHandle, cmHandles) {
    if (!cmHandles.includes(cmHandle)) {
        cmHandles.push(cmHandle);
    }
}

function getManagedElements(cmHandles, options = {}) {
    let results = [];
    cmHandles.forEach((cmHandle) => {
        const ncmpUri = `${NCMP_BASE_PATH}/${cmHandle}/${DATASTORE_OPERATIONAL}${ME_RESOURCE_ID}`;
        logData("ncmpUri :", ncmpUri);
        const response = httpGet(INGRESS_URL, ncmpUri, getSessionParams(), options);
        if (check(response, {
//            'THEN [GET] NCMP Status code is 200': (r) => isStatusOk(r.status),
            'AND [GET] NCMP Body is valid JSON': (r) => isJsonValid(r.body)
        })) {
            if (containsDnPrefixAttribute(response.body)) {
                results.push(response.body);
            }
        }
    });
    return results;
}

function getLteManagedElements() {
   let cmHandles = getLteCmHandles();
   let managedElements = getManagedElements(cmHandles);
   logData("LTE Managed Elements found: ", managedElements);
   return managedElements;
}

function getNrManagedElements() {
    let cmHandles = getNrCmHandles();
    let managedElements = getManagedElements(cmHandles);
    logData("NR Managed Elements found: ", managedElements);
    return managedElements;
}

function patchLteFddManagedObject(cmHandle, meId, cellId, attributes) {
    const ncmpUri = `${NCMP_BASE_PATH}/${cmHandle}/${DATASTORE_RUNNING}/ericsson-enm-ComTop:ManagedElement[@id=${meId}]/ericsson-enm-Lrat:ENodeBFunction`;
    logData("Patching EUtranCellFDD resource @", ncmpUri);
    const body = { "EUtranCellFDD": [ { "id": cellId, attributes } ] };
    return httpPatchJson(INGRESS_URL, ncmpUri, body, getSessionParams())
}

module.exports = {
    clearSession,
    getLteManagedElements,
    getNrManagedElements,
    getSessionParams,
    getCmHandles,
    patchLteFddManagedObject,
    getSessionId,
}