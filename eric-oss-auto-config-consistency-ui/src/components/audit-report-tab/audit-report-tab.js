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

import { LitComponent, html, definition } from '@eui/lit-component';
import { Tile, MultiPanelTile, TilePanel } from '@eui/layout';
import { Button, Dialog, Notification, Pill, TextField } from '@eui/base';
import ApplyButton from '../apply-button/apply-button.js';

import AuditReportTable from '../rest-paginated-table/rest-paginated-table.js';

import { DOLLAR, PERCENTAGE, restCalls } from '../../utils/restCallUtils.js';
import { logWarning } from '../../utils/logUtils.js';
import {
  ApproveForAll,
  Operation,
} from '../../utils/attributes/changeAttributes.js';

import { getNonApplyableChangesCount } from '../../utils/rest/utilityCalls.js';

import * as ATTR from '../../utils/attributes/auditAttributes.js';
import * as EXEC_ATTR from '../../utils/attributes/executionsAttributes.js';

import {
  createNotification,
  DEFAULT_TIMEOUT,
  MANUAL_CLOSE,
  NotificationStatus,
} from '../../utils/notification/notifications.js';

import style from './audit-report-tab.css';

const MAX_FDN_LENGTH = 1600;
const FDN_REGEX = '^[A-Za-z0-9,.%!&=_?:\\-\\/\\s]+$';

/**
 * Component AuditReportTab is defined as
 * `<e-audit-report-tab>`
 *
 * @extends {LitComponent}
 */

export default class AuditReportTab extends LitComponent {
  static get components() {
    return {
      'e-rest-paginated-table': AuditReportTable,
      'eui-button': Button,
      'e-apply-button': ApplyButton,
      'eui-dialog': Dialog,
      'eui-notification': Notification,
      'eui-multi-panel-tile': MultiPanelTile,
      'eui-pill': Pill,
      'eui-tile': Tile,
      'eui-tile-panel': TilePanel,
      'eui-text-field': TextField,
    };
  }

  /**
   * To access the i18n (localized) values for a component.
   *
   * @function meta
   */
  get meta() {
    return import.meta;
  }

  /**
   * Is called when the component is disconnected from the DOM.
   *
   * @function didDisconnect
   */
  didDisconnect() {
    this._resetDefaultState();
  }

  /**
   * Called by the user agent when an event is sent to the EventListener.
   *
   * @function handleEvent
   * @param event An event.
   */
  handleEvent(event) {
    if (event.type === 'e-rest-paginated-table:access-denied') {
      this.bubble(`e-audit-report-tab:access-denied`);
    }
  }

  /**
   * Reset components back to their default state after navigating from different pages.
   * Selected rows should be empty.
   * Apply selected button should be hidden.
   * The filter pill should be in an "on" state and the table should only contain inconsistencies.
   *
   * @function _resetDefaultState
   * @private
   */
  _resetDefaultState() {
    const filterPill = this.shadowRoot.querySelector('#filterPill');
    filterPill.unselected = false;
    this.selectedRows = [];
    this.isProposedChanges = true;
    this.filter = `filter=auditStatus:${ATTR.AuditStatus.INCONSISTENT}`;
  }

  /**
   * Returns the audit columns headers.
   *
   * @function _getAuditColumns
   *
   */
  _getAuditColumns() {
    // prettier-ignore
    return [
          { title: this.i18n?.AUDIT_ID, attribute: ATTR.AUDIT_ID, sortable: true,  width:"auto" },
          { title: this.i18n?.MANAGED_OBJECT, attribute: ATTR.MANAGED_OBJECT_FDN, sortable: true, width:"auto" },
          { title: this.i18n?.MO_CLASS, attribute: ATTR.MANAGED_OBJECT_TYPE, sortable: true, width:"auto" },
          { title: this.i18n?.ATTRIBUTE_NAME, attribute: ATTR.ATTRIBUTE_NAME, sortable: true, width:"auto" },
          { title: this.i18n?.CURRENT_VALUE, attribute: ATTR.CURRENT_VALUE, sortable: true, width:"auto" },
          { title: this.i18n?.PREFERRED_VALUE, attribute: ATTR.PREFERRED_VALUE, sortable: true, width:"auto" },
          { title: this.i18n?.AUDIT_STATUS, attribute: ATTR.AUDIT_STATUS, sortable: true, width:"auto" },
          { title: this.i18n?.EXECUTION_ID, attribute: ATTR.EXECUTION_ID, sortable: true, width:"auto" },
      ];
  }

  /**
   * Returns the context for use in the rest paginated table
   *
   * @function _getContext
   */
  _getContext() {
    return 'audit';
  }

  /**
   * @function _getTextFieldValue
   * @private
   * @returns A string value of the ManagedObject text field
   */
  _getTextFieldValue() {
    return this.shadowRoot.querySelector(
      'eui-text-field#managed-object-text-field',
    ).value;
  }

  /**
   * Sets the filter values from the fields in the panel.
   * Applies filter on table.
   *
   * @function _applyPanelFilter
   * @private
   */
  _applyPanelFilter() {
    this.fdnToFilterBy = this._getTextFieldValue();
    this._applyFilters();
  }

  /**
   * Updates the filter prop value based on values in the text field and the
   * filter pill.
   * This function will trigger a table rerender with the applied filters.
   *
   * @function _applyFilters
   * @private
   */
  _applyFilters() {
    const filterParts = [];
    if (this.isProposedChanges || this.fdnToFilterBy) {
      if (this.isProposedChanges) {
        filterParts.push(`auditStatus:${ATTR.AuditStatus.INCONSISTENT}`);
      }
      if (this.fdnToFilterBy) {
        filterParts.push(
          `managedObjectFdn:${encodeURIComponent(
            PERCENTAGE + this.fdnToFilterBy + PERCENTAGE,
          )}`,
        );
      }
      this.filter = `filter=${filterParts.join(encodeURIComponent(DOLLAR))}`;
    } else {
      this.filter = '';
    }
  }

  /**
   * Sets all the filter value props to be empty and deselects the pill
   * @function _clearFilters()
   * @private
   */
  _clearFilters() {
    const filterPill = this.shadowRoot.querySelector('#filterPill');
    filterPill.unselected = true;
    this.isProposedChanges = false;
    this.filter = '';
    this.fdnToFilterBy = '';
  }

  /**
   * Sets the values in the filter input elements to be empty.
   * @function _resetFilterInputs
   * @private
   */
  _resetFilterInputs() {
    const filterInputs = this.shadowRoot.querySelectorAll('.filter-input');
    filterInputs.forEach(element => {
      element.value = '';
    });
    this._clearFilters();
    this.isValidFdn = true;
  }

  /**
   * Checks the textField matches the correct format
   *
   */

  _checkTextValidity(inputValue) {
    if (inputValue.length === 0) {
      this.isValidFdn = true;
    } else {
      this.isValidFdn = new RegExp(FDN_REGEX).test(inputValue);
    }
  }

  /**
   * Returns whether to turn polling on of off in the rest paginated table
   *
   * @function  _getPoll()
   */
  _getPoll() {
    return false;
  }

  /**
   * Enables the Dialog to allow changes to be made
   *
   * @function _showConfirmChangesDialog
   * @private
   */
  async _showConfirmChangesDialog(applyType) {
    const dialog = this.shadowRoot.querySelector('eui-dialog#changesDialog');
    if (dialog) {
      this.approveForAll = applyType;
    }
    const selectionCount = this.approveForAll
      ? await getNonApplyableChangesCount(this.executionId, this.resultLength)
      : this.selectedRows.length;
    const dialogMessage = `${selectionCount} ${this.i18n?.APPLY_CONFIRMATION}`;
    dialog.querySelector('div[slot="content"]').textContent = dialogMessage;

    if (selectionCount === 0) {
      dialog.querySelector('eui-button#confirm-changes').disabled = true;
    } else {
      dialog.querySelector('eui-button#confirm-changes').disabled = false;
    }
    dialog.showDialog();
  }

  /**
   * Post the selected Audit results and then close the dialog when a response is received.
   * Notification will be sent based on response status.
   *
   * @function _postAuditResults
   * @private
   */
  _postAuditResults() {
    let selectedIds;
    const dialog = this.shadowRoot.querySelector('eui-dialog#changesDialog');
    if (!this.approveForAll) {
      selectedIds = this.selectedRows.map(
        selected => selected[EXEC_ATTR.EXECUTION_ID],
      );
    }

    const data = JSON.stringify({
      auditResultIds: selectedIds,
      approveForAll: this.approveForAll,
      operation: Operation.APPLY_CHANGE,
    });

    fetch(
      `${restCalls.eacc.postProposedChanges.url}/${this.executionId}/audit-results`,
      {
        method: restCalls.eacc.postProposedChanges.method,
        headers: restCalls.eacc.postProposedChanges.headers,
        body: data,
      },
    )
      .then(response => {
        if (!response.ok) {
          logWarning(
            `Post proposed changes failed, reason: status code: ${response.status}, status text: ${response.statusText}`,
          );
          return Promise.reject(response.status);
        }
        return response;
      })
      .then(() => {
        createNotification(
          this,
          NotificationStatus.SUCCESS,
          this.i18n?.AUDIT_RESULTS_UPDATED,
          '',
          DEFAULT_TIMEOUT,
          false,
        );
      })
      .catch(errorCode => {
        let title = this.i18n?.UNABLE_TO_POST_AUDIT_RESULTS;
        let description;
        switch (errorCode) {
          case 400:
            description = this.i18n?.REST_RESPONSE_400_BAD_REQUEST;
            break;
          case 403:
            title = this.i18n?.ACCESS_DENIED;
            description = this.i18n?.REST_403_DESCRIPTION;
            break;
          case 404:
            description = this.i18n?.REST_RESPONSE_404_NOT_FOUND;
            break;
          default:
            description = this.i18n?.AUDIT_RESULT_FAILURE_LONG_DESCRIPTION;
        }
        createNotification(
          this,
          NotificationStatus.FAILURE,
          title,
          description,
          MANUAL_CLOSE,
        );
        logWarning(`Unable to post audit results, error code: 500`);
      })
      .finally(() => {
        dialog.show = false;
      });
  }

  /**
   * Adds Apply selected Button to current page.
   *
   * @function _applySelectedButton
   * @private
   */
  _applySelectedButton() {
    return html`
      <e-apply-button
        @click="${() =>
          this._showConfirmChangesDialog(ApproveForAll.SELECTION)}"
        ?hidden="${this.selectedRows.length === 0}"
        execution-id="${this.executionId}"
        polling-id="apply-selected-button"
        id="applySelected"
      >
        ${this.i18n?.APPLY_SELECTED}
      </e-apply-button>

      <e-apply-button
        id="applyAll"
        ?hidden="${this.selectedRows.length !== 0}"
        execution-id="${this.executionId}"
        polling-id="apply-all-button"
        @click="${() => this._showConfirmChangesDialog(ApproveForAll.ALL)}"
        >${this.i18n?.APPLY_ALL}
      </e-apply-button>
    `;
  }

  /**
   * Adds Dialog box to current page.
   *
   * @function _selectedChangesDialog
   * @private
   */
  _selectedChangesDialog() {
    return html` <eui-dialog
      id="changesDialog"
      label="${this.i18n?.CONFIRM_CHANGES}"
    >
      <div slot="content" class="dialog-message">
        ${this.i18n?.APPLY_SELECTED_DIALOG_MESSAGE}
      </div>
      <eui-button
        @click="${() => this._postAuditResults()}"
        slot="bottom"
        primary
        id="confirm-changes"
      >
        ${this.i18n?.APPLY}</eui-button
      >
    </eui-dialog>`;
  }

  /**
   * Shows selected row count when Proposed Changes are selected.
   *
   * @function _selectedRowsCount
   * @private
   */

  _selectedRowsCount() {
    return this.selectedRows.length > 0
      ? `${this.i18n?.SELECTED} (${this.selectedRows.length})`
      : ``;
  }

  /**
   * Triggered when the Pill component is clicked.
   * Will filter for inconsistencies if the Pill component is in selected mode.
   *
   * @function _pillFilter
   * @private
   *
   */
  _pillFilter(event) {
    this.clearSelection();
    if (!event.target.unselected) {
      this.isProposedChanges = true;
    } else {
      this.isProposedChanges = false;
    }
    this._applyFilters();
  }

  /**
   * Returns e-rest-paginated-table from the shadowRoot.
   *
   * @function _getPaginatedTable()
   * @private
   */
  _getPaginatedTable() {
    return this.shadowRoot.querySelector('e-rest-paginated-table');
  }

  /**
   * Clears all the currently selected rows in the table.
   *
   * @function clearSelection
   */
  clearSelection() {
    const tableElement = this._getPaginatedTable();
    if (tableElement) {
      tableElement.data.forEach(el => {
        el.selected = false;
      });
      tableElement.data = [...tableElement.data];
      this.selectedRows = [];
    }
  }

  /**
   * Render the <e-audit-report-tab> component. This function is called each time a
   * prop changes.
   */
  render() {
    return html`<eui-multi-panel-tile
      tile-title="${this.i18n?.RESULTS}"
      subtitle="${this._selectedRowsCount()}"
      @e-rest-paginated-table:audit:total-elements=${event => {
        this.resultLength = event.detail;
      }}
      @e-rest-paginated-table:audit:row-selected=${event => {
        this.selectedRows = event.detail;
      }}
    >
      <div slot="action" class="action-button">
        ${this._applySelectedButton()}
      </div>
      <div slot="content">
        <div class="header">
          <div class="row-count-text">
            ${`${this.resultLength} ${
              this.resultLength !== 1
                ? `${this.i18n?.ITEMS}`
                : `${this.i18n?.ITEM}`
            }`}
            <span class="filters-applied" ?hidden="${this.filter === ''}">
              | ${this.i18n?.FILTERS_APPLIED} -
              <span class="clear-filter" @click="${() => this._clearFilters()}">
                ${this.i18n?.CLEAR}
              </span>
            </span>
          </div>
          <eui-pill
            id="filterPill"
            icon="filter"
            @click=${event => this._pillFilter(event)}
            toggle
          >
            ${this.i18n?.PROPOSED_CHANGES}
          </eui-pill>
        </div>
        ${this._selectedChangesDialog()}
        <div class="app-paginated-container">
          <e-rest-paginated-table
            .context=${this._getContext()}
            .filter=${this.filter}
            .executionId=${this.executionId}
            .isProposedChanges=${this.isProposedChanges}
            .columns=${this._getAuditColumns()}
            .poll=${this._getPoll()}
            @row-selected=${this}
            @e-rest-paginated-table:access-denied=${this}
            filterable
          ></e-rest-paginated-table>
        </div>
      </div>
      <eui-tile-panel
        id="filter-panel"
        tile-title="${this.i18n?.FILTER_BY}"
        slot="left"
        icon-name="filter"
      >
        <div slot="content" class="filter-content">
          <p class="filter-label">${this.i18n?.MANAGED_OBJECT}</p>
          <eui-text-field
            id="managed-object-text-field"
            class="filter-input"
            .maxlength=${MAX_FDN_LENGTH}
            @input=${event => this._checkTextValidity(event.target.value)}
          >
          </eui-text-field>
          <div class="filter-buttons">
            <eui-button
              id="reset-filters-button"
              @click=${() => this._resetFilterInputs()}
            >
              ${this.i18n?.CLEAR_FILTERS}
            </eui-button>
            <eui-button
              id="apply-filters-button"
              @click=${() => this._applyPanelFilter()}
              ?disabled="${!this.isValidFdn}"
              primary
            >
              ${this.i18n?.APPLY_FILTERS}
            </eui-button>
          </div>
        </div>
      </eui-tile-panel>
    </eui-multi-panel-tile>`;
  }
}

/**
 * @property {String} executionId - The id of the execution of the report.
 * @property {String} selectedRows - The selected rows of the table.
 * @property {Number} resultLength - The number of results in the table.
 * @property {Boolean} isProposedChanges - The filter status of the pill.
 * @property {Boolean} isValidFdn - Enables the apply filter button if true.
 * @property {String} fdnToFilterBy - The value of the managed object fdn search.
 * @property {String} filter - The filter string input of the table.
 */
definition('e-audit-report-tab', {
  style,
  props: {
    executionId: { type: String, attribute: false },
    selectedRows: { type: Array, attribute: false, default: [] },
    resultLength: { type: Number, attribute: false, default: 0 },
    isProposedChanges: { type: Boolean, attribute: false, default: true },
    isValidFdn: { type: Boolean, attribute: false, default: true },
    fdnToFilterBy: { type: String, attribute: false },
    filter: {
      type: String,
      attribute: false,
      default: `filter=auditStatus:${ATTR.AuditStatus.INCONSISTENT}`,
    },
  },
})(AuditReportTab);
