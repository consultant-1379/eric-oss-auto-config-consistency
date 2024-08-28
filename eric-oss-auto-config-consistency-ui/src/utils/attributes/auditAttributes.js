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
const AUDIT_ID = 'id';
const MANAGED_OBJECT_FDN = 'managedObjectFdn';
const MANAGED_OBJECT_TYPE = 'managedObjectType';
const ATTRIBUTE_NAME = 'attributeName';
const CURRENT_VALUE = 'currentValue';
const PREFERRED_VALUE = 'preferredValue';
const AUDIT_STATUS = 'auditStatus';
const EXECUTION_ID = 'executionId';

const AuditStatus = {
  CONSISTENT: 'Consistent',
  INCONSISTENT: 'Inconsistent',
};

export {
  AUDIT_ID,
  MANAGED_OBJECT_FDN,
  MANAGED_OBJECT_TYPE,
  ATTRIBUTE_NAME,
  CURRENT_VALUE,
  PREFERRED_VALUE,
  AUDIT_STATUS,
  EXECUTION_ID,
  AuditStatus,
};
