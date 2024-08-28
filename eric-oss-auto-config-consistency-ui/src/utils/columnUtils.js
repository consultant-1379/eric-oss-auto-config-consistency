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

import { html } from '@eui/app';

import {
  ExecutionState,
  EXECUTION_STATUS,
} from './attributes/executionsAttributes.js';
import { EACC_URL } from './restCallUtils.js';
import { download } from './settingsUtils.js';
import {
  NotificationStatus,
  createNotification,
} from './notification/notifications.js';

const IN_PROGRESS_STATES = [
  ExecutionState.AUDIT_IN_PROGRESS,
  ExecutionState.CHANGES_IN_PROGRESS,
  ExecutionState.REVERSION_IN_PROGRESS,
];

const SUCCESS_STATES = [
  ExecutionState.AUDIT_SUCCESSFUL,
  ExecutionState.CHANGES_SUCCESSFUL,
  ExecutionState.REVERSION_SUCCESSFUL,
];

const PARTIAL_SUCCESS_STATES = [
  ExecutionState.AUDIT_PARTIALLY_SUCCESSFUL,
  ExecutionState.CHANGES_PARTIALLY_SUCCESSFUL,
  ExecutionState.REVERSION_PARTIALLY_SUCCESSFUL,
];

/**
 * Returns the custom table cell html based on the 'executionStatus'.
 *
 * @function customStatusCell
 * @param row The current row of data.
 * @returns The table cell html.
 */
const customStatusCell = row => {
  let tableCellIcon;
  const rowExecutionStatus = row[EXECUTION_STATUS];
  if (IN_PROGRESS_STATES.includes(rowExecutionStatus)) {
    tableCellIcon = html`<eui-loader size="small"></eui-loader>`;
  } else if (SUCCESS_STATES.includes(rowExecutionStatus)) {
    tableCellIcon = html`<eui-icon
      name="success"
      color="var(--green-35)"
    ></eui-icon>`;
  } else if (PARTIAL_SUCCESS_STATES.includes(rowExecutionStatus)) {
    tableCellIcon = html`<eui-icon
      name="triangle-warning"
      color="var(--yellow-43)"
    ></eui-icon>`;
  } else if (rowExecutionStatus === ExecutionState.AUDIT_SKIPPED) {
    tableCellIcon = html`<eui-icon
      name="warning-circle"
      color="var(--white)"
    ></eui-icon>`;
  } else {
    tableCellIcon = html`<eui-icon
      name="failed"
      color="var(--red-52)"
    ></eui-icon>`;
  }
  return html` <div class="table__cell">
    <span class="table__cell-content"
      >${tableCellIcon}&nbsp;${rowExecutionStatus}</span
    >
  </div>`;
};

function downloadCell(row) {
  const downloadLink = `${EACC_URL}/${row.uri}`;
  const downloadIcon = html`<eui-tooltip
    message="${this.i18n?.DOWNLOAD}"
    position="left"
    action="true"
    ><eui-actionable-icon
      name="download-save"
      ?hidden=${!row.selected}
      @click="${async () => {
        if (!(await download(downloadLink, `${row[this.key]}.csv`))) {
          createNotification(
            this,
            NotificationStatus.FAILURE,
            this.i18n?.FILE_DOWNLOAD_FAILED_SHORT,
            this.i18n?.FILE_DOWNLOAD_FAILED_LONG,
          );
        }
      }}"
    ></eui-actionable-icon
  ></eui-tooltip>`;
  return html`<span part="download-icon-cell">${downloadIcon}</span>`;
}

export { customStatusCell, downloadCell };
