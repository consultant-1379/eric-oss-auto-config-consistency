/*
 * COPYRIGHT Ericsson 2024
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

import { restCalls } from '../restCallUtils.js';
import { ChangeStatus } from '../attributes/changeAttributes.js';
import { logWarning } from '../logUtils.js';

const IN_PROGRESS_CHANGE_STATUS = [
  ChangeStatus.IMPLEMENTATION_IN_PROGRESS,
  ChangeStatus.REVERSION_IN_PROGRESS,
];

const NON_REVERTABLE_CHANGE_STATUS = [
  ...IN_PROGRESS_CHANGE_STATUS,
  ChangeStatus.REVERSION_COMPLETE,
];

const NON_APPLYABLE_CHANGE_STATUS = [
  ...IN_PROGRESS_CHANGE_STATUS,
  ChangeStatus.IMPLEMENTATION_COMPLETE,
];

const DEFAULT_PAGE = 0;
const DEFAULT_PAGE_SIZE = 1;

const createFilter = filter => `changeStatus:(${filter.join(',')})`;

const IN_PROGRESS_CHANGE_STATUS_FILTER = createFilter(
  IN_PROGRESS_CHANGE_STATUS,
);

const NON_REVERTABLE_CHANGE_STATUS_FILTER = createFilter(
  NON_REVERTABLE_CHANGE_STATUS,
);

const NON_APPLYABLE_CHANGE_STATUS_FILTER = createFilter(
  NON_APPLYABLE_CHANGE_STATUS,
);

/**
 * Makes a REST call to the audit results with based on the parameters provided.
 *
 * @param id The execution id.
 * @param page The page number.
 * @param pageSize The page size number.
 * @param filter The filter to use.
 *
 * @function
 * @private
 *
 * @returns The REST response based on the criteria supplied.
 */
const _fetchAuditResults = async (id, page, pageSize, filter) =>
  fetch(
    `${restCalls.eacc.getExecutions.url}/${id}/audit-results?filter=${filter}&page=${page}&pageSize=${pageSize}`,
  ).then(response => {
    if (!response.ok) {
      logWarning(
        `Get filtered change status failed, reason: status code: ${response.status}, status text: ${response.statusText}`,
      );
      return Promise.reject(response.status);
    }
    return response.json();
  });

/**
 * Makes a REST call to get the audit results and then filters it to only get "Implementation in progress,Reversion in progress,Reversion complete"
 *
 * @param id The execution id.
 * @param totalCount The current total count.
 *
 * @function fetchNonRevertableChangesCount
 */
const getNonRevertableChangesCount = async (id, totalCount) =>
  _fetchAuditResults(
    id,
    DEFAULT_PAGE,
    DEFAULT_PAGE_SIZE,
    NON_REVERTABLE_CHANGE_STATUS_FILTER,
  )
    .then(json => {
      const total = totalCount - json.totalElements;
      return Promise.resolve(total);
    })
    // In an error scenario return the default "totalCount"
    .catch(() => Promise.resolve(totalCount));

/**
 * Makes a REST call to get the audit results and then filters it to only get "Implementation in progress,Reversion in progress,Implementation complete"
 *
 * @param id The execution id.
 * @param totalCount The current total count.
 *
 * @function getNonApplyableChangesCount
 */
const getNonApplyableChangesCount = async (id, totalCount) =>
  _fetchAuditResults(
    id,
    DEFAULT_PAGE,
    DEFAULT_PAGE_SIZE,
    NON_APPLYABLE_CHANGE_STATUS_FILTER,
  )
    .then(json => {
      const total = totalCount - json.totalElements;
      return Promise.resolve(total);
    })
    // In an error scenario return the default "totalCount"
    .catch(() => Promise.resolve(totalCount));

/**
 * Makes a REST call to get the audit results and then filters it to only get "Implementation in progress,Reversion in progress"
 *
 * @param id The execution id.
 *
 * @function changesInProgress
 */
const changesInProgress = async id =>
  _fetchAuditResults(
    id,
    DEFAULT_PAGE,
    DEFAULT_PAGE_SIZE,
    IN_PROGRESS_CHANGE_STATUS_FILTER,
  )
    .then(json => Promise.resolve(json.results.length !== 0))
    // In an error scenario return the default "true"
    .catch(() => Promise.resolve(true));

export {
  getNonRevertableChangesCount,
  getNonApplyableChangesCount,
  changesInProgress,
};
