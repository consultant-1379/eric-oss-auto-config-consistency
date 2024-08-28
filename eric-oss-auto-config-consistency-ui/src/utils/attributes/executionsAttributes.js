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

/**
 * A utility module that defines the attributes of the executions object.
 */
const EXECUTION_ID = 'id';
const JOB_NAME = 'jobName';
const EXECUTION_TYPE = 'executionType';
const EXECUTION_STARTED_AT = 'executionStartedAt';
const EXECUTION_ENDED_AT = 'executionEndedAt';
const CONSISTENCY_AUDIT_STARTED_AT = 'consistencyAuditStartedAt';
const CONSISTENCY_AUDIT_ENDED_AT = 'consistencyAuditEndedAt';
const EXECUTION_STATUS = 'executionStatus';
const TOTAL_ATTRIBUTES_AUDITED = 'totalAttributesAudited';
const TOTAL_MOS_AUDITED = 'totalMosAudited';
const INCONSISTENCIES_IDENTIFIED = 'inconsistenciesIdentified';

const ExecutionState = {
  AUDIT_IN_PROGRESS: 'Audit in Progress',
  AUDIT_SUCCESSFUL: 'Audit Successful',
  AUDIT_PARTIALLY_SUCCESSFUL: 'Audit Partially Successful',
  AUDIT_SKIPPED: 'Audit Skipped',
  AUDIT_FAILED: 'Audit Failed',
  AUDIT_ABORTED: 'Audit Aborted',
  CHANGES_IN_PROGRESS: 'Changes in Progress',
  CHANGES_FAILED: 'Changes Failed',
  CHANGES_SUCCESSFUL: 'Changes Successful',
  CHANGES_PARTIALLY_SUCCESSFUL: 'Changes Partially Successful',
  CHANGES_ABORTED: 'Changes Aborted',
  REVERSION_IN_PROGRESS: 'Reversion in Progress',
  REVERSION_FAILED: 'Reversion Failed',
  REVERSION_SUCCESSFUL: 'Reversion Successful',
  REVERSION_PARTIALLY_SUCCESSFUL: 'Reversion Partially Successful',
  REVERSION_ABORTED: 'Reversion Aborted',
};

export {
  EXECUTION_ID,
  JOB_NAME,
  EXECUTION_TYPE,
  EXECUTION_STARTED_AT,
  EXECUTION_ENDED_AT,
  CONSISTENCY_AUDIT_STARTED_AT,
  CONSISTENCY_AUDIT_ENDED_AT,
  EXECUTION_STATUS,
  TOTAL_ATTRIBUTES_AUDITED,
  TOTAL_MOS_AUDITED,
  INCONSISTENCIES_IDENTIFIED,
  ExecutionState,
};
