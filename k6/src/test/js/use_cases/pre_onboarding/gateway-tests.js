/*
 * COPYRIGHT Ericsson 2023
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

import { isStatusOk, containsHeader, isSessionValid,
         isStatusCreated, isStatusNoContent, isStatusNotFound } from '../../utils/validationUtils.js';

import * as gateway from '../../modules/api-gateway.js'

function verifySessionCreation() {
    group('WHEN no existing JSESSIONID', () => {
        check(gateway.getSessionId(), {
            'THEN JSESSIONID is created and valid': (r) => isSessionValid(r)
        });
    });
}

function verifyNcmpRouteExists() {
    group('WHEN eric-oss-ncmp route exists in the API Gateway', () => {
        check(gateway.getNcmpGatewayRoute(), {
            'THEN Status code is 200': (r) => isStatusOk(r.status),
            'AND Content-Type is application/json': (r) => containsHeader(r.headers, 'Content-Type', 'application/json'),
            'AND JSON body contains id "eric-oss-ncmp"': (r) => r.json('id') === 'eric-oss-ncmp'
        });
    });
}

function verifyEaccApiOnboarding () {
    group('Delete EACC Route if exists', () => {
      check(gateway.offboardEaccApis(), {
        'THEN Expect 404 OR 204': r => {
          return isStatusNotFound(r.status) || isStatusNoContent(r.status)
        }
      })
    })

    group('Create EACC Route', () => {
      check(
        gateway.onboardEaccApis(),
        {
          'THEN Expect 201': r => isStatusCreated(r.status)
        }
      )
    })
  }

module.exports = {
    verifySessionCreation,
    verifyNcmpRouteExists,
    verifyEaccApiOnboarding
}
