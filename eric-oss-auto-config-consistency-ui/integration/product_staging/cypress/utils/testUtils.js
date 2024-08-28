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

const EXECUTION_OFFSET = 3;

const ONE_SECOND = 1000;
const LOGIN_REDIRECT_PERIOD = ONE_SECOND * 10;
const UI_RENDERING_PERIOD = ONE_SECOND * 30;
const PAGE_CHANGE_PERIOD = ONE_SECOND * 8;
const DIALOG_WAIT_PERIOD = ONE_SECOND * 2;
const NOTIFICATION_WAIT_PERIOD = ONE_SECOND * 5;
const POLLING_TIMEOUT = ONE_SECOND * 60 * 4;
const CHANGES_POLLING_TIMEOUT = ONE_SECOND * 16;
const APPLY_CHANGES_TIMEOUT = ONE_SECOND * 180;
const EXECUTIONS_TABLE_POLLING_TIMEOUT = ONE_SECOND * 60 * 2;

const CONTAIN = 'contain';
const HAVE_ATTRIBUTE = 'have.attr';
const HAVE_LENGTH = 'have.length';
const HAVE_TEXT = 'have.text';
const MATCH = 'match';
const NOT_BE_EMPTY = 'not.be.empty';

const OPEN_LOOP = 'Open Loop';
const AUDIT_SUCCESSFUL = 'Audit Successful';

const DIGIT_REGEX = /^\d+$/;

export {
  CONTAIN,
  HAVE_ATTRIBUTE,
  HAVE_LENGTH,
  HAVE_TEXT,
  MATCH,
  NOT_BE_EMPTY,
  LOGIN_REDIRECT_PERIOD,
  UI_RENDERING_PERIOD,
  PAGE_CHANGE_PERIOD,
  DIALOG_WAIT_PERIOD,
  NOTIFICATION_WAIT_PERIOD,
  EXECUTION_OFFSET,
  POLLING_TIMEOUT,
  OPEN_LOOP,
  AUDIT_SUCCESSFUL,
  DIGIT_REGEX,
  CHANGES_POLLING_TIMEOUT,
  APPLY_CHANGES_TIMEOUT,
  EXECUTIONS_TABLE_POLLING_TIMEOUT,
};
