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

import { html, definition } from '@eui/lit-component';
import { Table } from '@eui/table';
import { Tooltip } from '@eui/base/tooltip';

import { RULESET_ERRORS_ERROR_DETAILS } from '../../utils/attributes/rulesetAttributes.js';

import style from './settings-error-table.css';

/**
 * Component SettingsErrorTable is defined as
 * `<e-settings-error-table>`
 *
 * @extends {Table}
 */
export default class SettingsErrorTable extends Table {
  static get components() {
    return {
      'eui-tooltip': Tooltip,
    };
  }

  /**
   * Overwrite the basic table cell functionality to display a custom cell.
   */
  /* eslint no-unused-vars: ["error", { "args": "none" }] */
  cell(row, column, rowIndex, colIndex) {
    if (column.attribute === RULESET_ERRORS_ERROR_DETAILS) {
      const currentText = row[column.attribute];
      const toolTip = html`<eui-tooltip
        smart="true"
        position="top"
        message="${currentText}"
        class="custom-tooltip"
        >${currentText}</eui-tooltip
      >`;

      return html`
        <div class="table__cell">
          <span class="table__cell-content">${toolTip}</span>
        </div>
      `;
    }
    return super.cell(row, column);
  }

  /**
   * Render the <e-settings-error-table> component. This function is called each time a
   * prop changes.
   */
  render() {
    return html`${super.render()}`;
  }
}

/**
 * @property {Boolean} dashed Adds a dashed line to separate rows.
 * @property {Boolean} tiny Shrinks the table to its smallest size to view more rows in a smaller space.
 * @property {Boolean} sortable Enables sortable columns on the table.
 */
definition('e-settings-error-table', {
  style,
  props: {
    dashed: { attribute: true, type: Boolean, default: true },
    tiny: { attribute: true, type: Boolean, default: true },
    sortable: { attribute: true, type: Boolean, default: true },
  },
})(SettingsErrorTable);
