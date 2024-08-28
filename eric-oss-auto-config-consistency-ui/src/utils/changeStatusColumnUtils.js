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

import { html } from '@eui/app';

import { ChangeStatus, CHANGE_STATUS } from './attributes/changeAttributes.js';

const IN_PROGRESS_STATES = [
  ChangeStatus.IMPLEMENTATION_IN_PROGRESS,
  ChangeStatus.REVERSION_IN_PROGRESS,
];

const SUCCESS_STATES = [
  ChangeStatus.IMPLEMENTATION_COMPLETE,
  ChangeStatus.REVERSION_COMPLETE,
];

/**
 * Returns the custom table cell html based on the 'changeStatus'.
 *
 * @function customStatusCell
 * @param row The current row of data.
 * @returns The table cell html.
 */
const customStatusCell = row => {
  let tableCellIcon;
  const rowChangeStatus = row[CHANGE_STATUS];
  if (IN_PROGRESS_STATES.includes(rowChangeStatus)) {
    tableCellIcon = html`<eui-loader size="small"></eui-loader>`;
  } else if (SUCCESS_STATES.includes(rowChangeStatus)) {
    tableCellIcon = html`<eui-icon
      name="success"
      color="var(--green-35)"
    ></eui-icon>`;
  } else {
    tableCellIcon = html`<eui-icon
      name="failed"
      color="var(--red-52)"
    ></eui-icon>`;
  }
  return html` <div class="table__cell">
    <span class="table__cell-content"
      >${tableCellIcon}&nbsp;${rowChangeStatus}</span
    >
  </div>`;
};

export { customStatusCell };
