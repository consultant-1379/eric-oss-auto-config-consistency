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

import { LitComponent, html, definition, nothing } from '@eui/lit-component';
import { Button } from '@eui/base/button';
import { Tabs, Tab, Tile } from '@eui/layout';

import AuditReportTab from '../audit-report-tab/audit-report-tab.js';
import ChangesPoll from '../changes-poll/changes-poll.js';
import RevertAllButton from '../revert-all-button/revert-all-button.js';

import { ApproveForAll } from '../../utils/attributes/changeAttributes.js';

import style from './executions-tab-area.css';

/**
 * Component ExecutionsTabArea is defined as
 * `<e-executions-tab-area>`
 *
 * @extends {LitComponent}
 */
export default class ExecutionsTabArea extends LitComponent {
  static get components() {
    return {
      'eui-button': Button,
      'eui-tab': Tab,
      'eui-tabs': Tabs,
      'eui-tile': Tile,
      'e-audit-report-tab': AuditReportTab,
      'e-changes-poll': ChangesPoll,
      'e-revert-all-button': RevertAllButton,
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
   * Called by the user agent when an event is sent to the EventListener.
   *
   * @function handleEvent
   * @param event An event.
   */
  handleEvent(event) {
    if (
      event.type === 'e-audit-report-tab:access-denied' ||
      event.type === 'e-changes-poll:access-denied'
    ) {
      this.bubble(`e-executions-tab-area:access-denied`);
    }
  }

  /**
   * Gets the changes-poll element.
   *
   * @function _getChangesPoll
   * @private
   */
  _getChangesPoll() {
    return this.shadowRoot.querySelector('#changes-poll');
  }

  /**
   * Clears the currently selected rows in the changes-poll table.
   *
   * @function _clearChangesSelection
   * @private
   */
  _clearChangesSelection() {
    const changesPoll = this._getChangesPoll();
    if (changesPoll) {
      changesPoll.clearSelection();
    }
  }

  /**
   * Reverts the selected changes in the changes-poll table.
   *
   * @function _enableRevertSelected
   * @private
   */
  _enableRevertSelected() {
    const changesPoll = this._getChangesPoll();
    if (changesPoll) {
      changesPoll.revertChanges(ApproveForAll.SELECTION);
    }
  }

  /**
   * Reverts all the changes in the changes-poll table.
   *
   * @function _enableRevertAll
   * @private
   */
  _enableRevertAll() {
    const changesPoll = this._getChangesPoll();
    if (changesPoll) {
      changesPoll.revertChanges(ApproveForAll.ALL);
    }
  }

  /**
   * Render the <e-executions-tab-area> component. This function is called each time a
   * prop changes.
   */
  render() {
    return html`
      <eui-tabs>
        <eui-tab selected id="audit-tab">
          <label>${this.i18n?.AUDIT_REPORT}</label>
        </eui-tab>

        <eui-tab id="changes-tab">
          <label>${this.i18n?.CHANGES}</label>
        </eui-tab>

        <div slot="content" selected>
          <e-audit-report-tab
            .executionId=${this.executionId}
            @e-audit-report-tab:access-denied=${this}
          >
          </e-audit-report-tab>
        </div>

        <div slot="content" class="content-box">
          ${this.changeCount !== 0
            ? html`<div class="tab-header-changes">
                <eui-tile
                  class="tab-header-changes-tile"
                  tile-title="${this.i18n?.CHANGES}"
                >
                  <div slot="action" id="action-buttons">
                    <eui-button
                      id="revertSelected"
                      ?hidden="${this.selectedChangeCount === 0}"
                      @click="${() => this._enableRevertSelected()}"
                      >${this.i18n?.REVERT_SELECTED}</eui-button
                    >
                    <e-revert-all-button
                      id="revertAll"
                      ?hidden="${this.selectedChangeCount !== 0}"
                      execution-id="${this.executionId}"
                      @click="${() => this._enableRevertAll()}"
                      >${this.i18n?.REVERT_ALL}</e-revert-all-button
                    >
                  </div>
                </eui-tile>
              </div>`
            : nothing}

          <div class="changes-selection" ?hidden="${this.changeCount === 0}">
            ${this.changeCount}
            ${this.changeCount > 1
              ? `${this.i18n?.ITEMS}`
              : `${this.i18n?.ITEM}`}
            <span id="selectedRows" ?hidden="${this.selectedChangeCount === 0}"
              >&nbsp;|&nbsp;${this.selectedChangeCount}
              ${this.i18n?.SELECTED.toLowerCase()} -&nbsp;
              <span
                class="clearSelection"
                @click="${() => this._clearChangesSelection()}"
                >${this.i18n?.CLEAR}</span
              >
            </span>
          </div>

          <e-changes-poll
            id="changes-poll"
            @e-changes-poll:access-denied=${this}
            @e-rest-paginated-table:changes:total-elements=${event => {
              this.changeCount = event.detail;
            }}
            @e-changes-poll:selected-count=${event => {
              this.selectedChangeCount = event.detail;
            }}
            .executionId=${this.executionId}
          >
          </e-changes-poll>
        </div>
      </eui-tabs>
    `;
  }
}

/**
 * @property {String} executionId - The executionId.
 * @property {Number} changeCount - The changeCount.
 * @property { Number } selectedChangeCount - The number of selected changes.

 */
definition('e-executions-tab-area', {
  style,
  props: {
    executionId: { type: String, attribute: false },
    changeCount: { type: Number, attribute: false, default: 0 },
    selectedChangeCount: { type: Number, attribute: false, default: 0 },
  },
})(ExecutionsTabArea);
