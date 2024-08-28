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

import { createSession, httpDelete, httpGet, httpPostJson, logData } from './common.js';
import { INGRESS_URL, INGRESS_LOGIN_URI, INGRESS_ROUTES_USER_URI, INGRESS_GAS_LOGIN_PARAMS, INGRESS_ROUTES_RBAC_URI, APPLICATION_JSON, INGRESS_EACC_USER, INGRESS_EACC_ADMIN, INGRESS_TEST_USER, INGRESS_LOGIN_PASSWORD, VALIDATE_EACC_RBAC, GAS_ACCESS_ROLE, NETWORK_CONFIGURATION_ROLE, API_ROUTE_ACCESS_ROLE } from './constants.js';
import { EACC_PREFIX } from './eacc-constants.js';

let sessionId;

function getEaccUserPayload(user, role) {
    const eaccUserPayload = JSON.parse(open('../../resources/user.json'));
    eaccUserPayload.password = INGRESS_LOGIN_PASSWORD;
    eaccUserPayload.user.username = user;
    if (VALIDATE_EACC_RBAC) {
        eaccUserPayload.user.privileges = role
    } else {
        eaccUserPayload.user.privileges = []
    }
    return eaccUserPayload;
}

function getTestUserPayload(user, role) {
    const testUserPayload = JSON.parse(open('../../resources/user.json'));
    testUserPayload.password = INGRESS_LOGIN_PASSWORD;
    testUserPayload.user.username = user;
    testUserPayload.user.privileges = role;
    return testUserPayload;
}

function getRbacPayload() {
    const eaccRbacPayload = JSON.parse(open('../../resources/eacc_rbac.json'));
    eaccRbacPayload.authorization.resources[0].uris = [EACC_PREFIX + '/**'];
    return eaccRbacPayload;
}

const eaccRbacPayload = getRbacPayload();
const eaccUserPayload = getEaccUserPayload(INGRESS_EACC_USER, ['eacc-read-only', 'LogAPI_ExtApps_Application_ReadOnly', GAS_ACCESS_ROLE]);
const eaccAdminPayload = getEaccUserPayload(INGRESS_EACC_ADMIN, ['eacc-read-write', GAS_ACCESS_ROLE]);
const testUserPayload = getTestUserPayload(INGRESS_TEST_USER, [NETWORK_CONFIGURATION_ROLE, API_ROUTE_ACCESS_ROLE]);

function getSessionId(options = {}) {
    if (!sessionId) {
        sessionId = createSession(INGRESS_URL, INGRESS_LOGIN_URI, '', INGRESS_GAS_LOGIN_PARAMS, options);
        logData('CREATE SESSION', sessionId);
    }
    return sessionId;
}

function getSessionParams() {
    return {
        headers: {
            'Content-Type': APPLICATION_JSON,
            'Accept': APPLICATION_JSON,
            'Cookie': getSessionId()
        }
    };
}

function clearSession() {
  sessionId = undefined;
}

function getSearchQuery(user = INGRESS_EACC_USER) {
    return `?\&search=(username==*${user}*;tenantname==master)`
}

function getDeleteUri(user = INGRESS_EACC_USER) {
    return `/${user}?tenantname=master`
}

function deleteEaccRbac(force = false, options = {}) {
    return httpDelete(INGRESS_URL, INGRESS_ROUTES_RBAC_URI, eaccRbacPayload, getSessionParams(force), options);
}

function createEaccRbac(options = {}) {
    return httpPostJson(INGRESS_URL, INGRESS_ROUTES_RBAC_URI, eaccRbacPayload, getSessionParams(), options);
}

function getEaccUser(options = {}) {
    return httpGet(INGRESS_URL, INGRESS_ROUTES_USER_URI.concat(getSearchQuery()), getSessionParams(), options);
}

function createEaccUser(options = {}) {
    return httpPostJson(INGRESS_URL, INGRESS_ROUTES_USER_URI, eaccUserPayload, getSessionParams(), options);
}

function deleteEaccUser(options = {}) {
    return httpDelete(INGRESS_URL, INGRESS_ROUTES_USER_URI.concat(getDeleteUri()), undefined, getSessionParams(), options);
}

function getEaccAdmin(options = {}) {
    return httpGet(INGRESS_URL, INGRESS_ROUTES_USER_URI.concat(getSearchQuery(INGRESS_EACC_ADMIN)), getSessionParams(), options);
}

function createEaccAdmin(options = {}) {
    return httpPostJson(INGRESS_URL, INGRESS_ROUTES_USER_URI, eaccAdminPayload, getSessionParams(), options);
}

function deleteEaccAdmin(options = {}) {
    return httpDelete(INGRESS_URL, INGRESS_ROUTES_USER_URI.concat(getDeleteUri(INGRESS_EACC_ADMIN)), undefined, getSessionParams(), options);
}

function createTestUser(options = {}) {
    return httpPostJson(INGRESS_URL, INGRESS_ROUTES_USER_URI, testUserPayload, getSessionParams(), options);
}

function getTestUser(options = {}) {
    return httpGet(INGRESS_URL, INGRESS_ROUTES_USER_URI.concat(getSearchQuery(INGRESS_TEST_USER)), getSessionParams(), options);
}

function deleteTestUser(options = {}) {
    return httpDelete(INGRESS_URL, INGRESS_ROUTES_USER_URI.concat(getDeleteUri(INGRESS_TEST_USER)), undefined, getSessionParams(), options);
}

module.exports = {
    getSessionId,
    deleteEaccRbac,
    createEaccRbac,
    getEaccUser,
    createEaccUser,
    deleteEaccUser,
    clearSession,
    getEaccAdmin,
    createEaccAdmin,
    deleteEaccAdmin,
    createTestUser,
    getTestUser,
    deleteTestUser,
}