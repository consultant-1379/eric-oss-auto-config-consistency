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

export const INGRESS_SCHEMA = __ENV.INGRESS_SCHEMA ? __ENV.INGRESS_SCHEMA : 'https';
export const EACC_INGRESS_SCHEMA = __ENV.EACC_INGRESS_SCHEMA ? __ENV.EACC_INGRESS_SCHEMA : INGRESS_SCHEMA;
export const INGRESS_HOST = __ENV.INGRESS_HOST ? __ENV.INGRESS_HOST : 'gas.stsvp1eic28.stsoss.sero.gic.ericsson.se';
export const RESTSIM_HOST = __ENV.RESTSIM_HOST ? __ENV.RESTSIM_HOST : 'restsim.stsvp1accd28.stsoss.sero.gic.ericsson.se';
export const EACC_INGRESS_HOST = __ENV.EACC_INGRESS_HOST ? __ENV.EACC_INGRESS_HOST : INGRESS_HOST;
export const INGRESS_URL = INGRESS_SCHEMA.concat('://').concat(INGRESS_HOST);
export const EACC_INGRESS_URL = EACC_INGRESS_SCHEMA.concat('://').concat(EACC_INGRESS_HOST);
// RESTSIM URIs
export const RESTSIM_LOGIN_URL="login?IDToken1=administrator&IDToken2=TestPassw0rd";
export const RESTSIM_RESTORE_URL="restore?teamname=rApp&interface=cm";
// EIC URIs
export const INGRESS_LOGIN_URI = '/auth/v1/login';
export const INGRESS_ROUTES_URI = '/v1/routes';
export const INGRESS_ROUTES_RBAC_URI = '/idm/rolemgmt/v1/extapp/rbac';
export const INGRESS_ROUTES_USER_URI = '/idm/usermgmt/v1/users';
export const INGRESS_ROUTES_NCMP_URI = INGRESS_ROUTES_URI.concat('/eric-oss-ncmp');
// EIC Users
export const INGRESS_GAS_USER = __ENV.INGRESS_GAS_USER ? __ENV.INGRESS_GAS_USER : 'gas-user';
export const INGRESS_LOGIN_USER = INGRESS_GAS_USER;
export const INGRESS_TEST_USER = 'eacc-testware-user';
export const INGRESS_EACC_USER = __ENV.INGRESS_EACC_USER  ? __ENV.INGRESS_EACC_USER  : 'eacc-user';
export const INGRESS_EACC_ADMIN = __ENV.INGRESS_EACC_ADMIN ? __ENV.INGRESS_EACC_ADMIN : 'eacc-admin';
export const INGRESS_LOGIN_PASSWORD = __ENV.INGRESS_LOGIN_PASSWORD ? __ENV.INGRESS_LOGIN_PASSWORD : 'idunEr!css0n';
// EIC Roles
export const GAS_ACCESS_ROLE = 'System_ReadOnly';
export const API_ROUTE_ACCESS_ROLE = 'Exposure_Application_Administrator';
export const NETWORK_CONFIGURATION_ROLE = 'NetworkConfiguration_Application_ReadWrite';
//Test Params
export const VALIDATE_EACC_RBAC = __ENV.VALIDATE_EACC_RBAC ? __ENV.VALIDATE_EACC_RBAC : false;
export const X_TENNANT = 'master';
export const APPLICATION_FORM_URL_ENCODED = 'application/x-www-form-urlencoded';
export const APPLICATION_JSON = 'application/json';

export const DEFAULT_TIMEOUT = 60;
export const DEFAULT_SLEEP_TIME = 5;
export const MAX_RETRY = 10;
export const EXECUTION_SCHEDULE_OFFSET = 10;

export const EACC_ROUTE_ID = "eacc-route-001";

export const EXECUTION_STATUS_AUDIT_SUCCESSFUL = "Audit Successful";
export const EXECUTION_STATUS_AUDIT_FAILED = "Audit Failed";
export const EXECUTION_STATUS_AUDIT_IN_PROGRESS = "Audit in Progress";
export const EXECUTION_STATUS_AUDIT_PARTIALLY_SUCCESSFUL = "Audit Partially Successful";
export const EXECUTION_STATUS_CHANGES_IN_PROGRESS = "Changes in Progress";
export const EXECUTION_STATUS_CHANGES_FAILED = "Changes Failed";
export const EXECUTION_STATUS_CHANGES_SUCCESSFUL = "Changes Successful";
export const EXECUTION_STATUS_CHANGES_PARTIALLY_SUCCESSFUL = "Changes Partially Successful";
export const PERCENTAGE = "%";
export const DEFAULT_E2E_OPTIONS = {
    duration: '60m',
    vus: 1,
    iterations: 1,
    thresholds: {
        checks: ['rate == 1.0']
    }
};

const getLoginParams = (user, password = INGRESS_LOGIN_PASSWORD) => ({
    headers: {
        'X-Login': user,
        'X-password': INGRESS_LOGIN_PASSWORD,
        'X-tenant': X_TENNANT,
        'Content-Type': APPLICATION_FORM_URL_ENCODED
    }
})

export const INGRESS_LOGIN_PARAMS = getLoginParams(INGRESS_LOGIN_USER);
export const INGRESS_GAS_LOGIN_PARAMS = getLoginParams(INGRESS_GAS_USER);
export const INGRESS_EACC_USER_PARAMS = getLoginParams(INGRESS_EACC_USER);
export const INGRESS_EACC_ADMIN_PARAMS = getLoginParams(INGRESS_EACC_ADMIN);
export const INGRESS_TEST_USER_PARAMS = getLoginParams(INGRESS_TEST_USER);
