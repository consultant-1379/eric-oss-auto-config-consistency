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

import http from 'k6/http';
import { group, check } from 'k6';
import { logData } from '../../modules/common.js';
import { RESTSIM_HOST, RESTSIM_LOGIN_URL, RESTSIM_RESTORE_URL  } from '../../modules/constants.js';
import { isStatusOk } from '../../utils/validationUtils.js';

export function resetNetworkConfigData() {
    group('WHEN Restsim exists', () => {
        check(resetRestSimCMData(), {
            'THEN Expect status 200 when reset is triggered': (r) => isStatusOk(r.status),
        });
    });
}

function resetRestSimCMData() {
    logData("# resetNetworkConfigData");
    logData("Host IP to be used is: ", RESTSIM_HOST);

    let loginResponse = http.post(`https://${RESTSIM_HOST}/${RESTSIM_LOGIN_URL}`, null, { insecure: true, tlsVersion: '1.0' });
    logData("loginResponse: ", loginResponse);

    if (loginResponse.status === 200) {
      let cookie = loginResponse.cookies;
      logData("cookie: ", cookie);

      if (cookie) {
        let restoreResponse = http.post(`https://${RESTSIM_HOST}/${RESTSIM_RESTORE_URL}`, null, { cookies: { 'cookie': cookie } });
        logData("restoreResponse: ", restoreResponse);

        logData(`Restsim Network Config Reset Restore Status Code: ${restoreResponse.status}`);
        return restoreResponse;
      }
    }
}
