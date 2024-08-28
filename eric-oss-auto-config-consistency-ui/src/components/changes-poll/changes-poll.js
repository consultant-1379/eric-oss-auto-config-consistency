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
 * Component ChangesPoll is defined as
 * `<e-changes-poll>`
 *
 * @extends {LitComponent}
 */
import { LitComponent, html, definition } from '@eui/lit-component';
import { Dialog, Notification, Button, Loader } from '@eui/base';
import { Icon } from '@eui/theme/icon';

import RestPaginatedTable from '../rest-paginated-table/rest-paginated-table.js';
import InitializationError from '../initialization-error/initialization-error.js';

import { customStatusCell } from '../../utils/changeStatusColumnUtils.js';
import { InitializationStatus } from '../../utils/initialization/initializationStatus.js';
import { ChangeStatus } from '../../utils/attributes/changeAttributes.js';
import { restCalls } from '../../utils/restCallUtils.js';
import { logWarning } from '../../utils/logUtils.js';

import {
  createNotification,
  DEFAULT_TIMEOUT,
  MANUAL_CLOSE,
  NotificationStatus,
} from '../../utils/notification/notifications.js';

import { getNonRevertableChangesCount } from '../../utils/rest/utilityCalls.js';

import * as ATTR from '../../utils/attributes/changeAttributes.js';

import style from './changes-poll.css';

// Replace this when IDUN-107668 is delivered. The rows should then be disabled.
const UN_SELECTABLE_STATES = [
  ATTR.ChangeStatus.IMPLEMENTATION_IN_PROGRESS,
  ATTR.ChangeStatus.REVERSION_IN_PROGRESS,
  ATTR.ChangeStatus.REVERSION_COMPLETE,
  ATTR.ChangeStatus.IMPLEMENTATION_FAILED,
];

export default class ChangesPoll extends LitComponent {
  static get components() {
    return {
      // register components here
      'eui-button': Button,
      'eui-dialog': Dialog,
      'eui-icon': Icon,
      'eui-loader': Loader,
      'eui-notification': Notification,
      'e-rest-paginated-table': RestPaginatedTable,
      'e-initialization-error': InitializationError,
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
   * Is called when the component added to the DOM.
   *
   * @function didConnect()
   */
  didConnect() {
    this.selected = [];
  }

  /**
   * Is called when the component is disconnected from the DOM.
   *
   * @function didDisconnect
   */
  didDisconnect() {
    this.clearSelection();
    this.selected = [];
  }

  /**
   * Returns the context for use in the rest paginated table
   *
   * @function _getContext
   */
  _getContext() {
    return 'changes';
  }

  /**
   * Called by the user agent when an event is sent to the EventListener.
   *
   * @function handleEvent
   * @param event An event.
   */
  handleEvent(event) {
    if (event.type === 'e-rest-paginated-table:access-denied') {
      this.bubble(`e-changes-poll:access-denied`);
    }
  }

  /**
   * Returns the filter for use in the rest paginated table
   *
   * @function _getFilter
   */
  _getFilter() {
    return `filter=changeStatus:(${ATTR.VALID_CHANGE_STATUS.join(',')})`;
  }

  /**
   * Returns wheter to turn polling on of off in the rest paginated table
   *
   * @function  _getPoll()
   */
  _getPoll() {
    return true;
  }

  /**
   * Returns e-rest-paginated-table from the shadowRoot.
   *
   * @function _getChangesTable()
   * @private
   */
  _getChangesTable() {
    return this.shadowRoot.querySelector('e-rest-paginated-table');
  }

  /**
   * Returns the proposed changes headers.
   *
   * @function _getProposedChangesColumns
   * @private
   */
  _getChangesColumns() {
    // prettier-ignore
    return [
        { title: this.i18n?.AUDIT_ID, attribute: ATTR.AUDIT_ID, sortable: true, width:"auto" },
        { title: this.i18n?.MANAGED_OBJECT, attribute: ATTR.MANAGED_OBJECT_FDN, sortable: true, width:"auto" },
        { title: this.i18n?.MO_CLASS, attribute: ATTR.MANAGED_OBJECT_TYPE, sortable: true, width:"auto" },
        { title: this.i18n?.ATTRIBUTE_NAME, attribute: ATTR.ATTRIBUTE_NAME, sortable: true, width:"auto" },
        { title: this.i18n?.INITIAL_VALUE, attribute: ATTR.CURRENT_VALUE, sortable: true, width:"auto" },
        { title: this.i18n?.PREFERRED_VALUE, attribute: ATTR.PREFERRED_VALUE, sortable: true, width:"auto" },
        { title: this.i18n?.STATUS, attribute: ATTR.CHANGE_STATUS, cell: customStatusCell, sortable: true, width:"auto" },
    ];
  }

  /**
   * Event method used to enable revert functionality.
   *
   * @function _enableRevert
   * @param event A row(s) selection event.
   * @private
   */
  _enableRevert(event) {
    const changeItems = event.detail;
    this.selected = [];
    if (changeItems.length > 0) {
      for (const changeItem of changeItems) {
        if (UN_SELECTABLE_STATES.includes(changeItem.changeStatus)) {
          // temporary workaround until IDUN-107668 is delivered.
          changeItem.selected = false;
        } else {
          this.selected.push(changeItem.id);
        }
      }
    }
    this.bubble('e-changes-poll:selected-count', this.selected.length);
  }

  /**
   * Performs a revert of the selected changes, be it some or all.
   *
   * @function _revert
   * @param event A row(s) selection event.
   * @private
   */
  _revert() {
    const revertDialog = this.shadowRoot.querySelector(
      'eui-dialog#revertDialog',
    );

    const auditIds = [];
    if (!this.approveForAll) {
      this.selected.forEach(id => auditIds.push(id));
    }

    const data = JSON.stringify({
      auditResultIds: auditIds,
      approveForAll: this.approveForAll,
      operation: ATTR.Operation.REVERT_CHANGE,
    });

    fetch(
      `${restCalls.eacc.postProposedChanges.url}/${this.executionId}/audit-results`,
      {
        method: restCalls.eacc.postProposedChanges.method,
        headers: restCalls.eacc.postProposedChanges.headers,
        body: data,
      },
    )
      .then(async response => {
        if (!response.ok) {
          const failureMessage =
            response.status === 400
              ? await response.json().then(res => Promise.resolve(res.detail))
              : response.statusText;

          logWarning(
            `POST: Revert changes failed, reason: status code: ${response.status}, status text: ${failureMessage}`,
          );

          return Promise.reject({
            status: response.status,
            message: failureMessage,
          });
        }
        return response;
      })
      .then(() => {
        createNotification(
          this,
          NotificationStatus.SUCCESS,
          this.i18n?.REVERT_STARTED,
          '',
          DEFAULT_TIMEOUT,
          false,
        );
      })
      .catch(response => {
        const errorCode = response.status;
        let title = this.i18n?.UNABLE_TO_APPLY_REVERT;
        let description;
        switch (errorCode) {
          case 400: {
            const errText = response.message;
            if (errText.includes(ChangeStatus.IMPLEMENTATION_IN_PROGRESS)) {
              description = this.i18n?.REST_RESPONSE_400_IN_PROGRESS;
            } else if (errText.includes('latest execution')) {
              description = this.i18n?.REST_RESPONSE_400_LATEST_EXECUTION;
            } else {
              description = this.i18n?.REST_RESPONSE_400_BAD_REQUEST;
            }
            break;
          }
          case 403:
            title = this.i18n?.ACCESS_DENIED;
            description = this.i18n?.REST_RESPONSE_403_DESCRIPTION;
            break;
          case 404:
            description = this.i18n?.REST_RESPONSE_404_NOT_FOUND;
            break;
          default:
            description = this.i18n?.REVERT_FAILURE_LONG_DESCRIPTION;
        }
        createNotification(
          this,
          NotificationStatus.FAILURE,
          title,
          description,
          MANUAL_CLOSE,
        );
        logWarning(`Unable to revert audit results, error code: ${errorCode}`);
      })
      .finally(() => {
        revertDialog.hideDialog();
        this.clearSelection();
      });
  }

  /**
   * Clears all the currently selected rows in the table.
   *
   * @function clearSelection
   */
  clearSelection() {
    const tableElement = this._getChangesTable();
    if (tableElement) {
      tableElement.clearRowSelection();
    }
  }

  /**
   * Reverts the currently selected rows in the table.
   *
   * @function revertSelected
   */
  async revertChanges(reversionType) {
    const revertDialog = this.shadowRoot.querySelector(
      'eui-dialog#revertDialog',
    );

    if (revertDialog) {
      this.approveForAll = reversionType;
      const selectionCount = this.approveForAll
        ? await getNonRevertableChangesCount(
            this.executionId,
            this.totalChanges,
          )
        : this.selected.length;
      const dialogMessage = `${selectionCount} ${this.i18n?.REVERT_CONFIRMATION}`;
      revertDialog.querySelector('div[slot="content"]').textContent =
        dialogMessage;

      if (selectionCount === 0) {
        revertDialog.querySelector('eui-button#revert').disabled = true;
      }
      revertDialog.showDialog();
    }
  }

  /**
   * Render the <e-changes-poll> component. This function is called each time a
   * prop changes.
   */
  render() {
    if (this.initializationState === InitializationStatus.FAILURE) {
      return html`<e-initialization-error></e-initialization-error>`;
    }

    const table = html`
      <div class="app-paginated-container">
        <e-rest-paginated-table
          @e-rest-paginated-table:changes:row-selected=${event => {
            this._enableRevert(event);
          }}
          @e-rest-paginated-table:changes:total-elements=${event => {
            this.totalChanges = event.detail;
          }}
          @e-rest-paginated-table:access-denied=${this}
          .executionId=${this.executionId}
          .columns=${this._getChangesColumns()}
          .context=${this._getContext()}
          .filter=${this._getFilter()}
          .poll=${this._getPoll()}
        >
        </e-rest-paginated-table>
      </div>
    `;

    const revertDialog = html`<eui-dialog
      id="revertDialog"
      label="${this.i18n?.CONFIRM_REVERT_CHANGES}"
      @eui-dialog:cancel="${() => this.clearSelection()}"
    >
      <div slot="content" class="dialog-message">
        ${this.i18n?.REVERT_CONFIRMATION}
      </div>
      <eui-button
        id="revert"
        slot="bottom"
        primary
        @click="${() => this._revert()}"
        >${this.i18n?.REVERT}</eui-button
      >
    </eui-dialog>`;

    const content = html`${table}${revertDialog}`;

    return html`${content}`;
  }
}

/**
 * @property {String} executionId - the executionId.
 * @property {Array} totalChanges - the total number of change results.
 * @property {String} initializationState - the initializationState.
 */
definition('e-changes-poll', {
  style,
  props: {
    executionId: { type: String, attribute: true },
    totalChanges: { type: Number, attribute: false },
    initializationState: {
      type: String,
      attribute: false,
      default: InitializationStatus.SUCCESSFUL,
    },
  },
})(ChangesPoll);
