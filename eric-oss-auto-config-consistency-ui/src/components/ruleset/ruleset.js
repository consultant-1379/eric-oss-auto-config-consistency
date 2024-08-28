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
import { Icon } from '@eui/theme';
import { ActionableIcon, Notification, Button, Dialog } from '@eui/base';

import CreateRuleset from '../create-ruleset/create-ruleset.js';
import InitializationError from '../initialization-error/initialization-error.js';
import { restCalls } from '../../utils/restCallUtils.js';
import * as ATTR from '../../utils/attributes/rulesetAttributes.js';
import { InitializationStatus } from '../../utils/initialization/initializationStatus.js';
import { logWarning } from '../../utils/logUtils.js';
import { removeDeletedEntry, sortByNewest } from '../../utils/tableUtils.js';
import {
  createNotification,
  DEFAULT_TIMEOUT,
  MANUAL_CLOSE,
  NotificationStatus,
} from '../../utils/notification/notifications.js';
import { updateLocaleString } from '../../utils/localization/localeUtils.js';
import { downloadCell } from '../../utils/columnUtils.js';
import { getRulesetName } from '../../utils/objectMapper.js';

import style from './ruleset.css';

const CREATE_RULESET_ID = 'create-ruleset';
const EDIT_RULESET_ID = 'edit-ruleset';
const DELETE_RULESET_ID = 'delete-dialog';

/**
 * Component Ruleset is defined as
 * `<e-ruleset>`
 *
 * @extends {LitComponent}
 */
export default class Ruleset extends LitComponent {
  static get components() {
    return {
      'eui-button': Button,
      'eui-icon': Icon,
      'e-initialization-error': InitializationError,
      'eui-dialog': Dialog,
      'eui-notification': Notification,
      'eui-table': Table,
      'eui-actionable-icon': ActionableIcon,
      'e-create-ruleset': CreateRuleset,
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
   * Is called when the component is connected to the DOM.
   *
   * @function didConnect
   */
  async didConnect() {
    await this.getRulesets();
    this.key = ATTR.RULESET_NAME;
  }

  /**
   * Is called when the component is about to be disconnected from the DOM.
   *
   * @function didDisconnect
   */
  didDisconnect() {
    this._setRowSelection(undefined);
    this.rulesets = [];
  }

  /**
   * Returns the ruleset columns object.
   *
   * @function _getRulesetsColumns
   * @private
   */
  _getRulesetsColumns() {
    return [
      {
        title: this.i18n?.NAME || 'Name',
        attribute: ATTR.RULESET_NAME,
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
   * Makes a REST call to get all rulesets from the EACC back-end.
   *
   * @function getRulesets
   */
  async getRulesets() {
    this._setRowSelection(undefined);
    await fetch(restCalls.eacc.getRulesets.url)
      .then(response => {
        if (!response.ok) {
          return Promise.reject(response.status);
        }
        return response.json();
      })
      .then(res => {
        this.rulesets = [...res];
        this.initializationState = InitializationStatus.SUCCESSFUL;
      })
      .catch(error => {
        if (error === 403) {
          this.bubble(`e-ruleset:access-denied`);
        } else {
          this.initializationState = InitializationStatus.FAILURE;
        }
      });
  }

  /**
   * Handles the ruleset creation event by adding it to the top of the table.
   *
   * @param event A ruleset creation event.
   * @function _handleRulesetCreation
   * @private
   */
  _handleRulesetCreation(event) {
    this.rulesets = [...this.rulesets, event.detail];
    sortByNewest(
      ATTR.RULESET_NAME,
      event.detail[ATTR.RULESET_NAME],
      this.rulesets,
    );
  }

  /**
   * Called whenever a row in the rulesets table is selected.
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
   * Renders the Create Button.
   *
   * @function _renderCreateButton
   * @private
   */
  _renderCreateButton() {
    return html` <span ?hidden="${!this.hideModifyButtons}">
      <eui-button
        id="create-ruleset-btn"
        @click="${() => this._showActionDialog(CREATE_RULESET_ID)}"
        >${this.i18n?.CREATE_RULESET}</eui-button
      >
    </span>`;
  }

  /**
   * Renders the Action Buttons.
   *
   * @function _renderEditButton
   * @private
   */
  _renderEditButton() {
    return html`<span ?hidden="${this.hideModifyButtons}">
      <eui-button
        id="edit-ruleset-btn"
        @click="${() => this._showActionDialog(EDIT_RULESET_ID)}"
        >${this.i18n?.EDIT}</eui-button
      >
    </span>`;
  }

  /**
   * Makes a DELETE request to remove a ruleset from EACC
   *
   * @function _deleteRuleset
   * @private
   */
  _deleteRuleset() {
    fetch(`${restCalls.eacc.deleteRuleset.url}/${this._getRulesetId()}`, {
      method: restCalls.eacc.deleteRuleset.method,
      headers: restCalls.eacc.deleteRuleset.headers,
    })
      .then(response => {
        if (!response.ok) {
          logWarning(
            `Delete ruleset failed, reason: status code: ${response.status}, status text: ${response.statusText}`,
          );
          let title = this.i18n?.DELETED_FAILURE_MESSAGE;
          let description;
          const substitutions = new Map();
          substitutions.set(`%ruleset_name%`, this._getRulesetName());
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
            `${this.i18n?.RULESET} '${this._getRulesetName()}' ${
              this.i18n?.DELETED_SUCCESS_MESSAGE
            }`,
            '',
            DEFAULT_TIMEOUT,
          );
          this.rulesets = [
            ...removeDeletedEntry(
              this.rulesets,
              this.selectedRow,
              ATTR.RULESET_ID,
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
        logWarning(`Unable to delete ruleset, reason: ${error}`);
      })
      .finally(() => {
        this.shadowRoot.querySelector(`#${DELETE_RULESET_ID}`).hideDialog();
      });
  }

  /**
   * Renders the Action Buttons.
   *
   * @function _renderDeleteButton
   * @private
   */
  _renderDeleteButton() {
    return html`<span ?hidden="${this.hideModifyButtons}">
      <eui-button
        id="delete-ruleset"
        @click="${() => this._showActionDialog(DELETE_RULESET_ID)}"
        >${this.i18n?.DELETE}</eui-button
      >
    </span>`;
  }

  /**
   * Returns the ID of the selected ruleset
   *
   * @function _getRulesetId
   * @private
   *
   */
  _getRulesetId() {
    return this.selectedRow === null ? '' : this.selectedRow[ATTR.RULESET_ID];
  }

  /**
   * Returns the name of the selected ruleset
   *
   * @function _getRulesetName
   * @private
   *
   */
  _getRulesetName() {
    return this.selectedRow === null ? '' : this.selectedRow[ATTR.RULESET_NAME];
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
   * Render the <e-ruleset> component. This function is called each time a
   * prop changes.
   */
  render() {
    if (this.initializationState === InitializationStatus.FAILURE) {
      return html`<e-initialization-error></e-initialization-error>`;
    }

    const rulesetsHeader = html`<p class="app-header">
      <span class="app-count-style">
        ${this.i18n?.RULESETS} (${this.rulesets.length})
      </span>
      <div class="action-buttons">
        <div class="app-create-button">${this._renderCreateButton()}</div>
        <div class="app-modify-buttons">${this._renderEditButton()} ${this._renderDeleteButton()}</div>
      </div>
    </p>`;

    const emptyRulesetStateContent = html`<div class="app-empty-state">
      <eui-icon name="info"></eui-icon>
      <p>${this.i18n?.NO_RULESETS_CREATED}</p>
    </div>`;
    const deleteDialog = html` <eui-dialog
      id=${DELETE_RULESET_ID}
      label="${this.i18n?.CONFIRM_DELETE}"
    >
      <div slot="content">${this.i18n?.DELETE_RULESET_MESSAGE}</div>
      <eui-button slot="bottom" warning @click="${() => this._deleteRuleset()}"
        >${this.i18n?.DELETE_RULESET}</eui-button
      >
    </eui-dialog>`;

    const rulesetsTable = html`<div class="app-table-container">
      <eui-table
        .columns=${this._getRulesetsColumns()}
        .data=${this.rulesets}
        .components=${{
          'eui-actionable-icon': ActionableIcon,
        }}
        @eui-table:row-select=${event => this._rowSelected(event)}
        single-select
        compact
        sortable
      ></eui-table>
    </div>`;
    const rulesetContent =
      this.rulesets.length === 0 ? emptyRulesetStateContent : rulesetsTable;

    const createRuleset = html`<e-create-ruleset
      id=${CREATE_RULESET_ID}
      .existingRulesetNames="${this.rulesets.map(getRulesetName)}"
      @e-create-ruleset:created="${event => this._handleRulesetCreation(event)}"
    ></e-create-ruleset>`;

    const modifyRuleset = html`<e-create-ruleset
      id=${EDIT_RULESET_ID}
      modify-ruleset="true"
      .modifyName=${this._getRulesetName()}
      .modifyId=${this._getRulesetId()}
    ></e-create-ruleset>`;

    return html`${rulesetsHeader}${rulesetContent}${deleteDialog}${createRuleset}${modifyRuleset}`;
  }
}

/**
 * @property {Array} rulesets - array of rulesets.
 * @property {Boolean} hideModifyButtons - Hides or shows modify buttons
 * @property {Object} selectedRow - the selected row of the table.
 * @property {String} initializationState - shows the initializationError if Failure
 */
definition('e-ruleset', {
  style,
  props: {
    rulesets: { type: Array, default: [] },
    hideModifyButtons: { type: Boolean, default: true },
    selectedRow: { type: Object, default: null },
    initializationState: {
      type: String,
      attribute: false,
      default: InitializationStatus.SUCCESSFUL,
    },
  },
})(Ruleset);
