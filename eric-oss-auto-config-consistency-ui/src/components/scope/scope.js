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

import { LitComponent, html, definition } from '@eui/lit-component';
import { Table } from '@eui/table';
import { Button } from '@eui/base/button';
import { ActionableIcon, Dialog, Notification } from '@eui/base';
import { Icon } from '@eui/theme/icon';
import { restCalls } from '../../utils/restCallUtils.js';
import { InitializationStatus } from '../../utils/initialization/initializationStatus.js';
import InitializationError from '../initialization-error/initialization-error.js';

import CreateScope from '../create-scope/create-scope.js';

import { getScopeName } from '../../utils/objectMapper.js';
import { removeDeletedEntry, sortByNewest } from '../../utils/tableUtils.js';
import * as ATTR from '../../utils/attributes/scopeAttributes.js';
import { logWarning } from '../../utils/logUtils.js';
import style from './scope.css';
import {
  createNotification,
  DEFAULT_TIMEOUT,
  MANUAL_CLOSE,
  NotificationStatus,
} from '../../utils/notification/notifications.js';
import { updateLocaleString } from '../../utils/localization/localeUtils.js';
import { downloadCell } from '../../utils/columnUtils.js';

const DELETE_SCOPE_ID = 'delete-dialog';
const EDIT_SCOPE_ID = 'edit-scope';
const CREATE_SCOPE_ID = 'create-scope';

/**
 * Component Scope is defined as
 * `<e-scope>`
 *
 * @extends {LitComponent}
 */
export default class Scope extends LitComponent {
  static get components() {
    return {
      'eui-table': Table,
      'eui-button': Button,
      'eui-icon': Icon,
      'e-create-scope': CreateScope,
      'eui-dialog': Dialog,
      'eui-notification': Notification,
      'eui-actionable-icon': ActionableIcon,
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
   * Is called when the component is about to be disconnected from the DOM.
   *
   * @function didDisconnect
   */
  didDisconnect() {
    this._setRowSelection(undefined);
    this.nodeSets = [];
  }

  /**
   * Is called when the component is connected to the DOM.
   *
   * @function didConnect
   */
  async didConnect() {
    await this.getNodeSets();
    this.key = ATTR.SCOPE_NAME;
  }

  /**
   * Returns the scopes columns object.
   *
   * @function _getScopesColumns
   * @private
   */
  _getScopesColumns() {
    return [
      {
        title: this.i18n?.SCOPE_NAME || 'Name',
        attribute: ATTR.SCOPE_NAME,
        sortable: true,
      },
      {
        title: '',
        attribute: 'download',
        sortable: false,
        cell: downloadCell.bind(this),
      },
    ];
  }

  /**
   * Makes a REST call to get all Node Sets from the EACC back-end.
   *
   * @function getNodeSets
   */
  async getNodeSets() {
    this._setRowSelection(undefined);
    fetch(restCalls.eacc.getNodeSets.url)
      .then(response => {
        if (!response.ok) {
          logWarning(
            `Get node sets failed, reason: status code: ${response.status}, status text: ${response.statusText}`,
          );
          return Promise.reject(response.status);
        }
        return response.json();
      })
      .then(res => {
        this.nodeSets = [...res];
        this.initializationState = InitializationStatus.SUCCESSFUL;
      })
      .catch(error => {
        if (error === 403) {
          this.bubble(`e-scope:access-denied`);
        } else {
          this.initializationState = InitializationStatus.FAILURE;
        }
      });
  }

  /**
   * Called whenever a row in the scopes table is selected.
   *
   * @function _rowSelected
   * @param event The row selected event.
   * @private
   */
  _rowSelected(event) {
    const [firstDetail] = event.detail;
    this._setRowSelection(firstDetail);
  }

  /**
   * Sets the current row selection.
   *
   * @function _setRowSelection
   * @param value The value to set.
   * @private
   */
  _setRowSelection(value) {
    this.selectedRow = value;
    this.hideModifyButtons = value === undefined;
  }

  /**
   * Makes a DELETE request to remove a node set from EACC
   *
   * @function _deleteNodeSet
   * @private
   */
  _deleteNodeSet() {
    fetch(`${restCalls.eacc.deleteNodeSet.url}/${this._getScopeId()}`, {
      method: restCalls.eacc.deleteNodeSet.method,
      headers: restCalls.eacc.deleteNodeSet.headers,
    })
      .then(response => {
        if (!response.ok) {
          logWarning(
            `Delete node set failed, reason: status code: ${response.status}, status text: ${response.statusText}`,
          );
          let title = this.i18n?.DELETED_FAILURE_MESSAGE;
          let description;
          const substitutions = new Map();
          substitutions.set(`%node_set_name%`, this._getScopeName());
          switch (response.status) {
            case 400:
              description = this.i18n?.DELETE_FAILURE_GENERIC_DESCRIPTION;
              break;
            case 403:
              title = this.i18n?.ACCESS_DENIED;
              description = this.i18n?.REST_403_DESCRIPTION;
              break;
            case 404:
              description = this.i18n?.DELETE_FAILURE_404_DESCRIPTION;
              break;
            case 409:
              description = updateLocaleString(
                this.i18n?.DELETE_FAILURE_409_DESCRIPTION,
                substitutions,
              );
              break;
            default:
              description = this.i18n?.DELETE_FAILURE_GENERIC_DESCRIPTION;
          }
          createNotification(
            this,
            NotificationStatus.FAILURE,
            title,
            description,
            MANUAL_CLOSE,
          );
        } else {
          createNotification(
            this,
            NotificationStatus.SUCCESS,
            `${this.i18n?.NODE_SET} '${this._getScopeName()}' ${
              this.i18n?.DELETED_SUCCESS_MESSAGE
            }`,
            '',
            DEFAULT_TIMEOUT,
          );
          this.nodeSets = [
            ...removeDeletedEntry(
              this.nodeSets,
              this.selectedRow,
              ATTR.SCOPE_ID,
            ),
          ];
          this._setRowSelection(undefined);
        }
        return [];
      })
      .catch(error => {
        createNotification(
          this,
          NotificationStatus.FAILURE,
          this.i18n?.DELETED_FAILURE_MESSAGE,
          this.i18n?.DELETE_FAILURE_GENERIC_DESCRIPTION,
          MANUAL_CLOSE,
        );
        logWarning(`Unable to delete node set, reason: ${error}`);
      })
      .finally(() => {
        this.shadowRoot.querySelector(`#${DELETE_SCOPE_ID}`).hideDialog();
      });
  }

  /**
   * Renders the Action Buttons.
   *
   * @function _renderEditAndDeleteButtons
   * @private
   */
  _renderEditAndDeleteButtons() {
    return html`<span ?hidden="${this.hideModifyButtons}">
      <eui-button
        id="edit-scope-btn"
        @click="${() => this._showActionDialog(EDIT_SCOPE_ID)}"
        >Edit</eui-button
      >
      <eui-button
        id="delete-scope"
        @click="${() => this._showActionDialog(DELETE_SCOPE_ID)}"
        >${this.i18n?.DELETE}</eui-button
      >
    </span>`;
  }

  /**
   * Returns the ID of the selected scope
   *
   * @function _getScopeId
   * @private
   *
   */
  _getScopeId() {
    return this.selectedRow === null ? '' : this.selectedRow[ATTR.SCOPE_ID];
  }

  /**
   * Returns the name of the selected scope
   *
   * @function _getScopeName
   * @private
   *
   */
  _getScopeName() {
    return this.selectedRow === null ? '' : this.selectedRow[ATTR.SCOPE_NAME];
  }

  /**
   * Shows the dialog with the delete confirmation
   *
   * @function _showDeleteDialog
   * @param tag the id of the element to show.
   * @private
   */
  _showActionDialog(tag) {
    this.shadowRoot.querySelector(`#${tag}`).showDialog();
  }

  /**
   * Renders the Create Button.
   *
   * @function _renderCreateButton
   * @private
   */
  _renderCreateButton() {
    return html` <span ?hidden="${!this.hideModifyButtons}">
      <eui-button
        id="create-scope-btn"
        @click="${() => this._showActionDialog(CREATE_SCOPE_ID)}"
        >${this.i18n?.CREATE_NODE_SET}</eui-button
      >
    </span>`;
  }

  /**
   * Handles the scope creation event by adding it to the top of the table.
   *
   * @param event A scope creation event.
   * @function _handleScopeCreation
   * @private
   */
  _handleScopeCreation(event) {
    this.nodeSets = [...this.nodeSets, event.detail];
    sortByNewest(ATTR.SCOPE_NAME, event.detail[ATTR.SCOPE_NAME], this.nodeSets);
  }

  /**
   * Render the <e-scope> component. This function is called each time a
   * prop changes.
   */
  render() {
    if (this.initializationState === InitializationStatus.FAILURE) {
      return html`<e-initialization-error></e-initialization-error>`;
    }

    const scopesHeader = html`<p class="app-header">
                              <span class="app-count-style">
                                ${this.i18n?.NODESETS} (${this.nodeSets.length})
                              </span>
                              <div class="action-buttons">
                                <div class="app-modify-buttons">${this._renderEditAndDeleteButtons()}</div>
                                <div class="app-create-button">${this._renderCreateButton()}</div>
                              </div>
                            </p>`;

    const emptyScopeStateContent = html`<div class="app-empty-state">
      <eui-icon name="info"></eui-icon>
      <p>${this.i18n?.NO_NODESETS_CREATED}</p>
    </div>`;
    const deleteDialog = html` <eui-dialog
      id=${DELETE_SCOPE_ID}
      label="${this.i18n?.CONFIRM_DELETE}"
    >
      <div slot="content">${this.i18n?.DELETE_NODESET_MESSAGE}</div>
      <eui-button slot="bottom" warning @click="${() => this._deleteNodeSet()}"
        >${this.i18n?.DELETE_NODESET}</eui-button
      >
    </eui-dialog>`;
    const scopesTable = html` <div class="app-table-container">
      <eui-table
        .columns=${this._getScopesColumns()}
        .data=${this.nodeSets}
        .components=${{
          'eui-actionable-icon': ActionableIcon,
        }}
        @eui-table:row-select=${event => this._rowSelected(event)}
        single-select
        compact
        sortable
      ></eui-table>
    </div>`;

    const scopeTabAreaContent =
      this.nodeSets.length === 0 ? emptyScopeStateContent : scopesTable;

    const createScope = html`<e-create-scope
      id=${CREATE_SCOPE_ID}
      .existingScopeNames="${this.nodeSets.map(getScopeName)}"
      @e-create-scope:created="${event => this._handleScopeCreation(event)}"
    ></e-create-scope>`;

    const modifyScope = html`<e-create-scope
      id=${EDIT_SCOPE_ID}
      modify-scope="true"
      .modifyName=${this._getScopeName()}
      .modifyId=${this._getScopeId()}
    ></e-create-scope>`;

    return html`${scopesHeader}${scopeTabAreaContent}${createScope}${deleteDialog}${modifyScope}`;
  }
}
/**
 * @property {Array} nodeSets - array of nodeSets.
 * @property {Boolean} hideModifyButtons - Hides or shows modify buttons
 * @property {Object} selectedRow - the selected row of the table.
 * @property {String} initializationState - shows the initializationError if Failure
 */
definition('e-scope', {
  style,
  props: {
    nodeSets: { type: Array, default: [] },
    hideModifyButtons: { type: Boolean, default: true },
    selectedRow: { type: Object, default: null },
    initializationState: {
      type: String,
      attribute: false,
      default: InitializationStatus.SUCCESSFUL,
    },
  },
})(Scope);
