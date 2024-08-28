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

import cronstrue from 'cronstrue';

import { formatDate } from '../../../../src/utils/objectMapper.js';
import { adjustCronToTimezone } from '../../../../src/utils/cronUtils.js';

// Execution IDs
export const ID_ONE = 1;
export const ID_TWO = 2;
export const ID_THREE = 3;
export const ID_FOUR = 4;
export const ID_FIVE = 5;

// Job Name
export const ALL_IRELAND = 'all-ireland';

// Execution Type
export const OPEN_LOOP = 'Open Loop';

// Execution Start & End dates
export const ID_ONE_EXECUTION_START = formatDate('2023-03-14T15:18:44.033Z');
export const ID_ONE_EXECUTION_END = formatDate('2023-03-14T15:23:44.033Z');
export const ID_TWO_EXECUTION_START = formatDate('2023-03-15T15:18:44.033Z');
export const ID_TWO_EXECUTION_END = formatDate('2023-03-15T15:23:44.033Z');
export const ID_THREE_EXECUTION_START = formatDate('2023-03-16T15:18:44.033Z');
export const ID_THREE_EXECUTION_END = formatDate('2023-03-16T15:23:44.033Z');
export const ID_FOUR_EXECUTION_START = formatDate('2023-03-17T15:18:44.033Z');
export const ID_FOUR_EXECUTION_END = formatDate('2023-03-17T15:23:44.033Z');

// Managed Object fdns
const MANAGED_ELEMENT_FDN =
  'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030';
const GNBCUCP_FUNCTION_FDN = `${MANAGED_ELEMENT_FDN},GNBCUCPFunction=1`;
export const MANAGED_OBJECT_ONE = `${MANAGED_ELEMENT_FDN},GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4`;
export const MANAGED_OBJECT_TWO = MANAGED_OBJECT_ONE;
export const MANAGED_OBJECT_THREE = `${GNBCUCP_FUNCTION_FDN},NRCellCU=NR03gNodeBRadio00030-4`;
export const MANAGED_OBJECT_FOUR = GNBCUCP_FUNCTION_FDN;
export const PARTIAL_MATCH_FDN = 'NR03gNodeBRadio00030-4';
// Managed Object types
export const NR_CELL_CU = 'NRCellCU';
export const NR_CELL_DU = 'NRCellDU';
export const GNBCUCP_FUNCTION = 'GNBCUCPFunction';

// Attribute names
export const CSI_RS_SHIFTING_PRIMARY = 'csiRsShiftingPrimary';
export const SUB_CARRIER_SPACING = 'subCarrierSpacing';
export const MCPC_PS_CELL_ENABLED = 'mcpcPSCellEnabled';
export const MAX_NG_RETRY_TIME = 'maxNgRetryTime';
export const ENDPOINT_RES_DEP_H_ENABLED = 'endpointResDepHEnabled';
// MO values
export const DEACTIVATED = 'DEACTIVATED';

// Schedule '0 15 11 ? * *'
export const DAILY_AT_QUARTER_PAST_ELEVEN = cronstrue.toString(
  adjustCronToTimezone('0 15 11 ? * *'),
  {
    verbose: true,
    use24HourTimeFormat: true,
  },
);

// Ruleset Name
export const TEST_RULESET = 'test_ruleset';

// Scope Name
export const TEST_SCOPE = 'test_scope';

// Scope Name
export const SCOPE_NAME_ONE = 'athlone';
export const SCOPE_NAME_TWO = 'dublin';

export const SAMPLE_RULESET = 'sample_ruleset';

// Scope ID
export const SCOPE_ID = '13aa6e40-a9e2-4fa2-9468-16c4797a5ca0';

// Create Job Paramas
export const JOB_NAME = 'all-ireland-4';
export const SCHEDULE_FREQUENCY = 'Daily';
export const SCHEDULE_HOUR = `${
  DAILY_AT_QUARTER_PAST_ELEVEN.split(' ')[1].split(':')[0]
}`;
export const SCHEDULE_MINUTE = '15';
export const RULESET_VALUE = '3f2504e0-4f89-11d3-9a0c-0305e82c3301';
export const SCOPE_VALUE = '13aa6e40-a9e2-4fa2-9468-16c4797a5ca0';
export const JOB_SUCCESS_MSG = ` Job '${JOB_NAME}' was created.`;

export const JOB_UPDATE_MSG = ` Job '${ALL_IRELAND}-${ID_ONE}' was updated.`;
