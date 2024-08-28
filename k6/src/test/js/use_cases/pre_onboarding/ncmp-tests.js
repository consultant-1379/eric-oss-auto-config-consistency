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

import * as ncmp from '../../modules/ncmp.js'
import { logData } from '../../modules/common.js';

import { hasDnPrefixAttribute, isStatusOk } from '../../utils/validationUtils.js';

function verifyExistingNodes() {
    logData("verifyExistingNodes");
    group('WHEN LTE Nodes exist in NCMP', () => {
        check(ncmp.getLteManagedElements(), {
            'THEN Expect LTE Nodes to be returned': (r) => r.length > 0,
            'AND They contain the "dnPrefix" attribute': (r) => hasDnPrefixAttribute(r)
        });
    });

    group('WHEN NR Nodes exist in NCMP', () => {
        check(ncmp.getNrManagedElements(), {
            'THEN Expect NR Nodes to be returned': (r) => r.length > 0,
            'AND They contain the "dnPrefix" attribute': (r) => hasDnPrefixAttribute(r)
        });
    });
}

module.exports = {
    verifyExistingNodes
}
