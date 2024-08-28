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
 * A utility module that defines the attributes of the audit results object.
 */
const EXECUTION_ID = 'executionId';
const AUDIT_ID = 'id';
const MANAGED_OBJECT_FDN = 'managedObjectFdn';
const MANAGED_OBJECT_TYPE = 'managedObjectType';
const ATTRIBUTE_NAME = 'attributeName';
const CURRENT_VALUE = 'currentValue';
const PREFERRED_VALUE = 'preferredValue';
const CHANGE_STATUS = 'changeStatus';

const ChangeStatus = {
  NOT_APPLIED: 'Not applied',
  IMPLEMENTATION_IN_PROGRESS: 'Implementation in progress',
  IMPLEMENTATION_COMPLETE: 'Implementation complete',
  IMPLEMENTATION_FAILED: 'Implementation failed',
  IMPLEMENTATION_ABORTED: 'Implementation aborted',
  REVERSION_IN_PROGRESS: 'Reversion in progress',
  REVERSION_COMPLETE: 'Reversion complete',
  REVERSION_FAILED: 'Reversion failed',
  REVERSION_ABORTED: 'Reversion aborted',
};

const VALID_CHANGE_STATUS = [
  ChangeStatus.IMPLEMENTATION_IN_PROGRESS,
  ChangeStatus.IMPLEMENTATION_COMPLETE,
  ChangeStatus.IMPLEMENTATION_FAILED,
  ChangeStatus.IMPLEMENTATION_ABORTED,
  ChangeStatus.REVERSION_IN_PROGRESS,
  ChangeStatus.REVERSION_COMPLETE,
  ChangeStatus.REVERSION_FAILED,
  ChangeStatus.REVERSION_ABORTED,
];

const Operation = {
  APPLY_CHANGE: 'APPLY_CHANGE',
  REVERT_CHANGE: 'REVERT_CHANGE',
};

const ApproveForAll = {
  SELECTION: false,
  ALL: true,
};

export {
  AUDIT_ID,
  EXECUTION_ID,
  MANAGED_OBJECT_FDN,
  MANAGED_OBJECT_TYPE,
  ATTRIBUTE_NAME,
  CURRENT_VALUE,
  PREFERRED_VALUE,
  CHANGE_STATUS,
  ChangeStatus,
  VALID_CHANGE_STATUS,
  Operation,
  ApproveForAll,
};
