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

import { ChangeStatus } from '../../src/utils/attributes/changeAttributes.js';

export const MOCK_DELETE_SUCCESS_RESPONSE = {
  ok: true,
  status: 204,
};

export const MOCK_BAD_REQUEST_RESPONSE = {
  statusText: 'Bad Request',
  ok: false,
  status: 400,
};

export const MOCK_RULE_VALIDATION_ERROR_RESPONSE = {
  title: 'Problems found in ruleset.',
  status: 400,
  detail: 'Ruleset cannot contain any invalid MO types, attributes or values.',
  ruleValidationErrrors: {
    lineNumber: 2,
    errorType: 'Invalid MO.',
    errorDetails: 'MO not found in Managed Object Model.',
    additionalInfo: '',
  },
};

export const MOCK_IN_PROGRESS_RESPONSE = {
  json: async () => ({
    title: 'Validation failed',
    detail: `Cannot revert changes with status '${ChangeStatus.IMPLEMENTATION_IN_PROGRESS}'`,
  }),
  ok: false,
  status: 400,
};

export const MOCK_LATEST_EXECUTION = {
  json: async () => ({
    title: 'Validation failed',
    detail: 'Changes can only be reverted from the latest execution.',
  }),
  ok: false,
  status: 400,
};

export const MOCK_INVALID_FORMAT_EXECUTION = {
  json: async () => ({
    title: 'Validation failed',
    detail: 'No inconsistencies found for proposed change id(s)',
  }),
  ok: false,
  status: 400,
};

export const MOCK_ACCESS_DENIED_RESPONSE = {
  statusText: 'Access denied',
  ok: false,
  status: 403,
};

export const MOCK_NOT_FOUND_RESPONSE = {
  statusText: 'Not Found',
  ok: false,
  status: 404,
};

export const MOCK_CONFLICT_RESPONSE = {
  statusText: 'Conflict',
  ok: false,
  status: 409,
};

export const MOCK_PAYLOAD_TOO_LARGE_RESPONSE = {
  statusText: 'Payload Too Large',
  ok: false,
  status: 413,
};

export const MOCK_INTERNAL_SERVER_ERROR_RESPONSE = {
  statusText: 'Internal Server Error',
  ok: false,
  status: 500,
};
