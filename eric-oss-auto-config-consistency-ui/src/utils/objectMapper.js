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

import cronstrue from 'cronstrue';

import {
  EXECUTION_STARTED_AT,
  EXECUTION_ENDED_AT,
  CONSISTENCY_AUDIT_STARTED_AT,
  CONSISTENCY_AUDIT_ENDED_AT,
} from './attributes/executionsAttributes.js';
import { SCHEDULE } from './attributes/jobAttributes.js';
import { RULESET_ID, RULESET_NAME } from './attributes/rulesetAttributes.js';
import { SCOPE_ID, SCOPE_NAME } from './attributes/scopeAttributes.js';
import { adjustCronToTimezone, MILLISECONDS_PER_MINUTE } from './cronUtils.js';

/**
 * Converts a UTC time to local time using the offset value.
 *
 * @function convertUTCToLocal
 * @param utc The UTC time in ISO format.
 * @returns The local time equivalent in ISO format.
 */
const convertUTCToLocal = utc => {
  const offset = new Date().getTimezoneOffset();
  const unixEpoch = new Date(utc).getTime();
  const localISOEquivalent = new Date(
    unixEpoch - offset * MILLISECONDS_PER_MINUTE,
  ).toISOString();
  return localISOEquivalent;
};

/**
 * Converts a timestamp in the format '2023-03-13T15:18:44.033Z' to '2023/03/13 15:18'.
 *
 * @function convertISOToReadable
 * @param iso An ISO formatted string.
 * @returns A date string of format yyyy/MM/dd HH:mm.
 */
const convertISOToReadable = iso => {
  const [date, time] = iso.split('T');
  const readableString = `${date.replaceAll('-', '/')} ${time.match(
    /\d+:\d+/,
  )}`;
  return readableString;
};

/**
 * Converts a UTC time in ISO format. Performs 2 operations:
 * 1. Converts from UTC time to Local time
 * 2. Converts from ISO format to human redable format.
 *
 * @function formatDate
 * @param obj Any object.
 * @returns An updated timestamp String.
 */
const formatDate = obj =>
  obj === '' || obj === null || obj === undefined
    ? ''
    : convertISOToReadable(convertUTCToLocal(obj));

/**
 * Updates the time based fields in an executions object.
 *
 * @function updateExecutionObject
 * @param executionObj An executions object.
 * @returns An executions object with updated time fields.
 */
const updateExecutionObject = executionObj => {
  executionObj[EXECUTION_STARTED_AT] = formatDate(
    executionObj[EXECUTION_STARTED_AT],
  );
  executionObj[EXECUTION_ENDED_AT] = formatDate(
    executionObj[EXECUTION_ENDED_AT],
  );
  executionObj[CONSISTENCY_AUDIT_STARTED_AT] = formatDate(
    executionObj[CONSISTENCY_AUDIT_STARTED_AT],
  );
  executionObj[CONSISTENCY_AUDIT_ENDED_AT] = formatDate(
    executionObj[CONSISTENCY_AUDIT_ENDED_AT],
  );
  return executionObj;
};

/**
 * Aligns the ruleset object as the dropdown expects.
 *
 * @function alignRulesetValue
 * @param rulesetObj A Ruleset object.
 */
const alignRulesetValue = rulesetObj => ({
  label: rulesetObj[RULESET_NAME],
  value: rulesetObj[RULESET_ID],
});

/**
 * Aligns the scope object as the dropdown expects.
 *
 * @function alignScopeValue
 * @param scopeObject A Scope object.
 */
const alignScopeValue = scopeObject => ({
  label: scopeObject[SCOPE_NAME],
  value: scopeObject[SCOPE_ID],
});

/**
 * Update the Schedule field in a job object.
 *
 * @function updateJobObject
 * @param jobObj A job object.
 * @returns A job object with updated schedule field.
 */
const updateJobObject = jobObj => {
  try {
    jobObj[SCHEDULE] = cronstrue.toString(
      adjustCronToTimezone(jobObj[SCHEDULE]),
      {
        verbose: true,
        use24HourTimeFormat: true,
      },
    );
  } catch (error) {
    jobObj[SCHEDULE] = cronstrue.toString(jobObj[SCHEDULE], {
      verbose: true,
      use24HourTimeFormat: true,
    });

    jobObj[SCHEDULE] = `${jobObj[SCHEDULE]} (UTC)`;
  }
  return jobObj;
};

/**
 * A map function that only returns the scope name attribute from the scope object.
 *
 * @function getScopeNames
 * @param scopeObj A scope object
 * @returns An Array of scope names.
 */
const getScopeName = scopeObj => scopeObj[SCOPE_NAME].toLowerCase();

/**
 * A map function that only returns the ruleset name attribute from the ruleset object.
 *
 * @function getRulesetName
 * @param rulesetObj A ruleset object
 * @returns An Array of ruleset names.
 */
const getRulesetName = rulesetObj => rulesetObj[RULESET_NAME].toLowerCase();

export {
  updateExecutionObject,
  updateJobObject,
  formatDate,
  alignRulesetValue,
  alignScopeValue,
  getScopeName,
  getRulesetName,
};
