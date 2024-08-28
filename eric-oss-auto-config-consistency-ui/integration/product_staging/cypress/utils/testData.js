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

import { getGasBuildNumber } from './eaccEnvironment.js';
import { EXECUTION_OFFSET, OPEN_LOOP, AUDIT_SUCCESSFUL } from './testUtils.js';

// GAS Constants
export const APP_ID = 'Automated Configuration Consistency';
export const APP_DESCRIPTION =
  'Perform CM consistency audits to detect baseline consistency errors.';

// URL Constants
export const BASE_EACC_URL = '/#eacc';
export const EACC_SETTINGS_URL = `${BASE_EACC_URL}/settings`;
export const EACC_JOBS_URL = `${BASE_EACC_URL}/jobs`;
export const EACC_EXECUTIONS_URL = `${BASE_EACC_URL}/executions`;

// Use Case Constants
const STAGING_ID = 'product_staging';
const TEST = 'test';
const TECH_4G = '4g';
const TECH_5G = '5g';

export const NODE_SET_NAME_4G = `${STAGING_ID}_node_set_${TECH_4G}-${getGasBuildNumber()}`;
export const NODE_SET_FILE_NAME_4G_CREATE = `${TEST}_scope_${TECH_4G}.csv`;
export const NODE_SET_FILE_NAME_4G_UPDATE = `${TEST}_scope_${TECH_4G}_update.csv`;

export const NODE_SET_NAME_5G = `${STAGING_ID}_node_set_${TECH_5G}-${getGasBuildNumber()}`;
export const NODE_SET_FILE_NAME_5G_CREATE = `${TEST}_scope_${TECH_5G}.csv`;
export const NODE_SET_FILE_NAME_5G_UPDATE = `${TEST}_scope_${TECH_5G}_update.csv`;

export const TEST_SCOPES = [
  {
    name: NODE_SET_NAME_4G,
    createFile: NODE_SET_FILE_NAME_4G_CREATE,
    updateFile: NODE_SET_FILE_NAME_4G_UPDATE,
  },
  {
    name: NODE_SET_NAME_5G,
    createFile: NODE_SET_FILE_NAME_5G_CREATE,
    updateFile: NODE_SET_FILE_NAME_5G_UPDATE,
  },
];

export const RULESET_NAME_4G = `${STAGING_ID}_ruleset_${TECH_4G}-${getGasBuildNumber()}`;
export const RULESET_FILE_NAME_4G_CREATE = `${TEST}_ruleset_${TECH_4G}.csv`;
export const RULESET_FILE_NAME_4G_UPDATE = `${TEST}_ruleset_${TECH_4G}_update.csv`;

export const RULESET_NAME_5G = `${STAGING_ID}_ruleset_${TECH_5G}-${getGasBuildNumber()}`;
export const RULESET_FILE_NAME_5G_CREATE = `${TEST}_ruleset_${TECH_5G}.csv`;
export const RULESET_FILE_NAME_5G_UPDATE = `${TEST}_ruleset_${TECH_5G}_update.csv`;

export const TEST_RULESETS = [
  {
    name: RULESET_NAME_4G,
    createFile: RULESET_FILE_NAME_4G_CREATE,
    updateFile: RULESET_FILE_NAME_4G_UPDATE,
  },
  {
    name: RULESET_NAME_5G,
    createFile: RULESET_FILE_NAME_5G_CREATE,
    updateFile: RULESET_FILE_NAME_5G_UPDATE,
  },
];

export const JOB_NAME_4G = `${STAGING_ID}_job_${TECH_4G}-${getGasBuildNumber()}`;
export const JOB_NAME_5G = `${STAGING_ID}_job_${TECH_5G}-${getGasBuildNumber()}`;

const FDN_PARTIAL_MATCH_4G = 'LTE63dg2ERBST00075';
const FDN_PARTIAL_MATCH_5G = 'NR174gNodeBRadioT00023';

const FDN_FULL_MATCH_4G =
  'SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE63dg2ERBST00075,ENodeBFunction=1,EUtranCellFDD=LTE63dg2ERBST00075-10';
const FDN_FULL_MATCH_5G =
  'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR174gNodeBRadioT00023,ManagedElement=NR174gNodeBRadioT00023,GNBDUFunction=1,NRCellDU=NR174gNodeBRadioT00023-1';

let now = new Date();
now.setMinutes(now.getMinutes() + EXECUTION_OFFSET);
const MINUTE_4G = now.getMinutes().toString().padStart(2, 0);
const HOUR_4G = now.getHours().toString().padStart(2, 0);

now = new Date();
now.setMinutes(now.getMinutes() + EXECUTION_OFFSET + 1);
const MINUTE_5G = now.getMinutes().toString().padStart(2, 0);
const HOUR_5G = now.getHours().toString().padStart(2, 0);

export const TEST_JOBS = [
  {
    name: JOB_NAME_4G,
    scheduleMinute: MINUTE_4G,
    scheduleHour: HOUR_4G,
    ruleset: RULESET_NAME_4G,
    scope: NODE_SET_NAME_4G,
  },
  {
    name: JOB_NAME_5G,
    scheduleMinute: MINUTE_5G,
    scheduleHour: HOUR_5G,
    ruleset: RULESET_NAME_5G,
    scope: NODE_SET_NAME_5G,
  },
];

export const EXECUTIONS_4G_VALUES = {
  jobName: JOB_NAME_4G,
  executionType: OPEN_LOOP,
  status: AUDIT_SUCCESSFUL,
  attributesAudited: 59,
  mosAudited: 31,
  inconsistencies: 31,
  reports: {
    filteredSize: 31,
    unfilteredSize: 59,
  },
  filtering: {
    partialFDN: FDN_PARTIAL_MATCH_4G,
    fullFDN: FDN_FULL_MATCH_4G,
    inconsistentCountForPartialMO: 13,
    countForPartialMO: 25,
    inconsistentCountForFullMO: 1,
    countForFullMO: 2,
  },
  changes: {
    expectedRows: 31,
  },
  applyAll: false,
  revertAll: false,
};

export const EXECUTIONS_5G_VALUES = {
  jobName: JOB_NAME_5G,
  executionType: OPEN_LOOP,
  status: AUDIT_SUCCESSFUL,
  attributesAudited: 27,
  mosAudited: 15,
  inconsistencies: 15,
  reports: {
    filteredSize: 15,
    unfilteredSize: 27,
  },
  filtering: {
    partialFDN: FDN_PARTIAL_MATCH_5G,
    fullFDN: FDN_FULL_MATCH_5G,
    inconsistentCountForPartialMO: 5,
    countForPartialMO: 9,
    inconsistentCountForFullMO: 1,
    countForFullMO: 2,
  },
  changes: {
    expectedRows: 15,
  },
  applyAll: true,
  revertAll: true,
};

export const TEST_EXECUTIONS = [EXECUTIONS_4G_VALUES, EXECUTIONS_5G_VALUES];
