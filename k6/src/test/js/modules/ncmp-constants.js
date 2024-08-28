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

export const NCMP_BASE_PATH = '/ncmp/v1/ch';
export const NCMP_SEARCHES_URI = NCMP_BASE_PATH.concat('/searches');
export const DATASTORE_OPERATIONAL = 'data/ds/ncmp-datastore:passthrough-operational?resourceIdentifier=';
export const DATASTORE_RUNNING = 'data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=';

export const ME_RESOURCE_ID = '/&options=fields=ericsson-enm-ComTop:ManagedElement/attributes(*)';

export const LTE_CM_HANDLES = {
    'conditions': [
        {
            'name': 'hasAllModules',
            'conditionParameters': [
                {
                    'moduleName': 'ericsson-enm-Lrat'
                }
            ]
        }
    ]
};

export const NR_CM_HANDLES = {
    'conditions': [
        {
            'name': 'hasAllModules',
            'conditionParameters': [
                {
                    'moduleName': 'ericsson-enm-GNBDU'
                }
            ]
        }
    ]
};
