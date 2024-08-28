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

import { createSession, httpGet, logData, httpPostJson, httpDelete } from './common.js';

import { INGRESS_URL, INGRESS_LOGIN_URI, INGRESS_ROUTES_NCMP_URI,
         APPLICATION_JSON, INGRESS_ROUTES_URI, EACC_ROUTE_ID, INGRESS_HOST,  
         INGRESS_TEST_USER_PARAMS} from './constants.js';

let sessionId;

function getSessionId(options = {}) {
    if (!sessionId) {
        sessionId = createSession(INGRESS_URL, INGRESS_LOGIN_URI, '', INGRESS_TEST_USER_PARAMS, options);
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

function getNcmpGatewayRoute(options = {}) {
    return httpGet(INGRESS_URL, INGRESS_ROUTES_NCMP_URI, getSessionParams(), options);
}

function getRoutePayload () {
    const routePayload = JSON.parse(open('../../resources/eacc-route.json'))
    routePayload.predicates[1].args._genkey_0 = INGRESS_HOST
    return routePayload
}

const eaccRbacPayload = getRoutePayload();
function onboardEaccApis () {
    return httpPostJson(INGRESS_URL, INGRESS_ROUTES_URI, eaccRbacPayload, getSessionParams())
}

function offboardEaccApis () {
    return httpDelete(INGRESS_URL, INGRESS_ROUTES_URI + '/' + EACC_ROUTE_ID, undefined, getSessionParams())
}

module.exports = {
    getSessionId,
    getNcmpGatewayRoute,
    onboardEaccApis,
    offboardEaccApis
}
