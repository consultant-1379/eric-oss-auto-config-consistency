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

import { group, check } from 'k6';
import { logData } from '../../modules/common.js';

import * as rbac from '../../modules/rbac.js'
import * as eacc from '../../modules/eacc.js'
import * as ncmp from '../../modules/ncmp.js'
import { INGRESS_EACC_USER, INGRESS_EACC_ADMIN, VALIDATE_EACC_RBAC, INGRESS_TEST_USER } from '../../modules/constants.js';
import { isStatusOk, isStatusNoContent, isStatusNotFound, isSessionValid, isStatusForbidden } from '../../utils/validationUtils.js';

function verifyEaccRbac() {
    group('WHEN Delete current EACC RBAC', () => {
        check(rbac.deleteEaccRbac(), {
            'THEN Expect 404 OR 204': (r) => isStatusNotFound(r.status) || isStatusNoContent(r.status)
        });
    });

    group('WHEN create EACC RBAC', () => {
        check(rbac.createEaccRbac(), {
            'THEN Expect 200': (r) => isStatusOk(r.status)
        });
    });

    rbac.clearSession();
}

function verifyUser(name, getUser, deleteUser, createUser, getSessionId, clearSession) {
    const response = getUser();
    const user = JSON.parse(response.body).find(user => user.username === name)

    if (!!user) {
        group(`WHEN ${name} already exists, delete user`, () => {
            check(deleteUser(), {
                'THEN Expect 204': (r) => isStatusNoContent(r.status)
            });
        });
    }

    group(`WHEN create ${name}`, () => {
        check(createUser(), {
            'THEN Expect 200': (r) => isStatusOk(r.status)
        });
    });

    group(`WHEN login as ${name}`, () => {
        check(getSessionId(), {
            'THEN JSESSIONID is created and valid': (r) => isSessionValid(r)
        });
    });

    rbac.clearSession();
    clearSession();
}

function verifyEaccUser() {
    verifyUser(INGRESS_EACC_USER, rbac.getEaccUser, rbac.deleteEaccUser, rbac.createEaccUser, eacc.getUserSessionId, eacc.cleanUserSession);
}

function verifyEaccAdmin() {
    verifyUser(INGRESS_EACC_ADMIN, rbac.getEaccAdmin, rbac.deleteEaccAdmin, rbac.createEaccAdmin, eacc.getAdminSessionId, eacc.cleanAdminSession);
}

function verifyTestUser() {
    verifyUser(INGRESS_TEST_USER, rbac.getTestUser, rbac.deleteTestUser, rbac.createTestUser, ncmp.getSessionId, ncmp.clearSession);
}

function verifyRbacEnforced() {
    rbac.getSessionId();

    group('WHEN Logged in as GAS user and GET jobs', () => {
        check(eacc.getJob(), {
            'THEN Expect 403': (r) => isStatusForbidden(r.status)
        });
    });

    group('WHEN Logged in as GAS user and POST jobs', () => {
        check(eacc.postJob(), {
            'THEN Expect 403': (r) => isStatusForbidden(r.status)
        });
    });

    rbac.clearSession();

    eacc.getUserSessionId();
    group('WHEN Logged in as EACC user and GET jobs', () => {
        check(eacc.getJob(), {
            'THEN Expect 200': (r) => isStatusOk(r.status)
        });
    });

    group('WHEN Logged in as EACC user and POST jobs', () => {
        check(eacc.postJob(), {
            'THEN Expect 403': (r) => isStatusForbidden(r.status)
        });
    });
    eacc.cleanUserSession();

    eacc.getAdminSessionId();
    group('WHEN Logged in as EACC admin and GET jobs', () => {
        check(eacc.getJob(), {
            'THEN Expect 200': (r) => isStatusOk(r.status)
        });
    });
    eacc.cleanAdminSession();
}

function verifyCleanUpEaccRbac() {
    if (VALIDATE_EACC_RBAC) {
        group('WHEN Delete EACC RBAC', () => {
            check(rbac.deleteEaccRbac(), {
                'THEN Expect 204': (r) => isStatusNoContent(r.status)
            });
        });
    }

    group('WHEN Delete EACC User', () => {
        check(rbac.deleteEaccUser(), {
            'THEN Expect 204': (r) => isStatusNoContent(r.status)
        });
    });

    group('WHEN Delete EACC Admin', () => {
        check(rbac.deleteEaccAdmin(), {
            'THEN Expect 204': (r) => isStatusNoContent(r.status)
        });
    });

    group(`WHEN Delete Network Configuration user (${INGRESS_TEST_USER})`, () => {
        check(rbac.deleteTestUser(), {
            'THEN Expect 204': (r) => isStatusNoContent(r.status)
        });
    });

    rbac.clearSession();
}

module.exports = {
    verifyEaccRbac,
    verifyEaccUser,
    verifyEaccAdmin,
    verifyTestUser,
    verifyCleanUpEaccRbac,
    verifyRbacEnforced,
}
