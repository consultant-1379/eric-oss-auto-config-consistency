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
import { Icon } from '@eui/theme';
import { Table, Pagination } from '@eui/table';
import InitializationError from '../initialization-error/initialization-error.js';
import { InitializationStatus } from '../../utils/initialization/initializationStatus.js';
import { restCalls } from '../../utils/restCallUtils.js';
import { logWarning } from '../../utils/logUtils.js';
import style from './rest-paginated-table.css';
import * as ATTR from '../../utils/attributes/auditAttributes.js';
import {
  sameObject,
  findNewEntries,
  findDeletedEntries,
} from '../../utils/tableUtils.js';
import {
  DEFAULT_POLLING_STATE,
  POLLING_INTERVAL,
} from '../../utils/changeConfig.js';

/**
 * Component RestPaginatedTable is defined as
 * `<e-rest-paginated-table>`
 *
 * @extends {LitComponent}
 */
export default class RestPaginatedTable extends LitComponent {
  constructor() {
    super();
    this.pollingTimer = DEFAULT_POLLING_STATE;
    this.isPolling = false;
  }

  static get components() {
    return {
      'eui-icon': Icon,
      'eui-pagination': Pagination,
      'eui-table': Table,
      'e-initialization-error': InitializationError,
    };
  }

  get meta() {
    return import.meta;
  }

  /**
   * Is called every time a prop changes.
   *
   * @function didChangeProps
   */
  didChangeProps(changedProps) {
    if (changedProps.has('filter')) {
      this.currentPage = 1;
      this._updateTableData();
    }
    if (changedProps.has('columns')) {
      changedProps.get('columns').forEach((element, index) => {
        // Prevent the resizable column from snapping back to default
        this.columns[index].width = element.width;
      });
    }
  }

  /**
   * Is called when the component added to the DOM.
   *
   * @function didConnect
   */
  async didConnect() {
    if (this.poll) {
      this._enablePolling();
    } else {
      this._updateTableData();
    }
  }

  /**
   * Is called when the component is about to be disconnected from the DOM.
   *
   * @function didDisconnect
   */
  async didDisconnect() {
    this._disablePolling();
    this.data = [];
    this.bubble(`e-rest-paginated-table:${this.context}:total-elements`, 0);
  }

  /**
   * Enables the polling mechanism.
   *
   * @function _enablePolling
   * @private
   */
  _enablePolling() {
    this.isPolling = true;
    if (this._isPollingInDefaultState()) {
      this._pollChanges();
    }
  }

  /**
   * Disables the polling mechanism.
   *
   * @function _disablePolling
   * @private
   */
  _disablePolling() {
    this.isPolling = false;
  }

  /**
   * Checks to see if the polling timer is in the default state.
   *
   * @function _isPollingInDefaultState
   * @private
   */
  _isPollingInDefaultState() {
    return this.pollingTimer === DEFAULT_POLLING_STATE;
  }

  /**
   * Polls the audits endpoint.
   *
   * @function _pollChanges
   * @private
   */
  _pollChanges = async () => {
    if (!this.isPolling) {
      if (!this._isPollingInDefaultState()) {
        clearTimeout(this.pollingTimer);
        this.pollingTimer = DEFAULT_POLLING_STATE;
      }
    } else {
      try {
        this._updateTableData();
        if (!this._isPollingInDefaultState()) {
          clearTimeout(this.pollingTimer);
        }
        this.pollingTimer = setTimeout(this._pollChanges, POLLING_INTERVAL);
      } catch (err) {
        logWarning(`Failed to get changes data, reason: ${err}`);
      }
    }
  };

  /**
   * Called by the user agent when an event is sent to the EventListener.
   *
   * @function handleEvent
   * @param event A UI event.
   */
  handleEvent(event) {
    // Code to execute when page-index-change event is fired
    if (event.type === 'eui-table:page-index-change') {
      event.stopPropagation();
      this.currentPage = event.detail.state.currentPage;
      this.bubble(`e-rest-paginated-table:${this.context}:row-selected`, 0);
      this._updateTableData();
    }
  }

  /**
   * Clears any currently selected rows.
   *
   * @function clearRowSelection
   */
  clearRowSelection() {
    const tableElement = this._getTable();
    if (tableElement) {
      tableElement.data.forEach(el => {
        el.selected = false;
      });
      tableElement.data = [...tableElement.data];
      this.bubble(`e-rest-paginated-table:${this.context}:row-selected`, 0);
    }
  }

  /**
   * Called whenever a row in the table is selected.
   *
   * @function _rowSelected
   * @param event The row selected event.
   * @private
   */
  _rowSelected(event) {
    this.bubble(
      `e-rest-paginated-table:${this.context}:row-selected`,
      event.detail,
    );
  }

  /**
   * Makes a REST call to get a specific number of results, based on pages requested and the filter.
   *
   * @function _updateTableData
   * @private
   */
  _updateTableData() {
    let call = `/${this.executionId}/audit-results?page=${
      this.currentPage - 1
    }&pageSize=${this.numEntries}`;

    if (this.filter.length > 0) {
      call = `${call}&${this.filter}`;
    }
    fetch(restCalls.eacc.getExecutions.url + call)
      .then(response => {
        if (!response.ok) {
          return Promise.reject(response.status);
        }
        return response.json();
      })
      .then(res => {
        // If first time get it to rerender
        if (this.data.length === 0) {
          this.data = res.results;
        } else {
          this._updateTableContents(res.results);
        }
        this.numPages = res.totalPages;
        this.bubble(
          `e-rest-paginated-table:${this.context}:total-elements`,
          res.totalElements,
        );
        this.initializationState = InitializationStatus.SUCCESSFUL;
      })
      .catch(error => {
        if (error === 403) {
          this.bubble(`e-rest-paginated-table:access-denied`);
        } else {
          this.initializationState = InitializationStatus.FAILURE;
          this.bubble(
            `e-rest-paginated-table:${this.context}:total-elements`,
            0,
          );
        }
      });
  }

  /**
   * Updates the existing changes in the table.
   *
   * @function _modifyExistingTableData
   * @param table The current table.
   * @param updatedAuditResults The updated audit results to compare against.
   * @private
   */
  _modifyExistingTableData(table, updatedAuditResults) {
    updatedAuditResults.forEach(auditResult => {
      table.data.forEach((existing, index) => {
        if (sameObject(existing, auditResult, ATTR.AUDIT_ID)) {
          // If the changeItem is selected then restore its state
          if (existing.selected) {
            auditResult.selected = true;
          }
          table.data[index] = auditResult;
        }
      });
    });
    table.data = [...table.data];
  }

  /**
   * Updates the changes table without re-rendering the entire table.
   *
   * @function _updateTableContents
   * @param auditResults The executions to update the table with.
   * @private
   */
  _updateTableContents(auditResults) {
    const tableElement = this._getTable();
    if (tableElement) {
      this._modifyExistingTableData(tableElement, auditResults);
      findNewEntries(tableElement.data, auditResults, ATTR.AUDIT_ID).forEach(
        audit => {
          tableElement.data = [...tableElement.data, audit];
          this.data = tableElement.data;
        },
      );

      findDeletedEntries(
        tableElement.data,
        auditResults,
        ATTR.AUDIT_ID,
      ).forEach(audit => {
        const index = tableElement.data.findIndex(existing =>
          sameObject(existing, audit, ATTR.AUDIT_ID),
        );
        tableElement.data.splice(index, 1);
        tableElement.data = [...tableElement.data];
        this.data = tableElement.data;
      });
    }
  }

  /**
   * Returns the table from the shadowRoot.
   *
   * @function _getTable()
   * @private
   */
  _getTable() {
    return this.shadowRoot.querySelector('#auditDataTable');
  }

  /**
   * Render the <e-rest-paginated-table> component. This function is called each time a
   * prop changes.
   */
  render() {
    if (this.initializationState === InitializationStatus.FAILURE) {
      return html`<e-initialization-error></e-initialization-error>`;
    }

    const emptyContent = html`
      <div class="app-empty-state">
        <eui-icon name="info"></eui-icon>
        <div id="emptyChangesInfo">
          <div id="emptyChangesTitle">
            ${this.i18n?.ZERO_CHANGES_IMPLEMENTED}
          </div>
          <div id="emptyChangesSubtitle">
            ${this.i18n?.ZERO_CHANGES_IMPLEMENTED_SUBTITLE}
          </div>
        </div>
      </div>
    `;

    const table = html`
      <div class="app-table-container">
        <eui-table
          id="auditDataTable"
          .columns=${this.columns}
          .data=${this.data}
          @eui-table:row-select=${event => this._rowSelected(event)}
          compact
          sortable
          multi-select
          virtual-scroll
          resizable
        ></eui-table>
      </div>
      ${this.filterable && this.data.length === 0
        ? html`<div class="no-data-msg">
            <div class="no-data-msg-icon">
              <eui-icon name="info"></eui-icon>
            </div>
            <div class="no-data-msg-text">
              ${this.i18n?.NO_RESULTS_FOUND_FOR_APPLIED_FILTERS}
              <p class="no-data-msg-sub-text">
                ${this.i18n?.EDIT_OR_CLEAR_FILTERS}
              </p>
            </div>
          </div>`
        : html` <eui-pagination
            @eui-table:page-index-change=${this}
            current-page=${this.currentPage}
            num-entries=${this.numEntries}
            num-pages=${this.numPages}
          ></eui-pagination>`}
    `;

    // Do this for now to avoid No changes implemented message appearing in audit report when no inconsistencies
    // reported. Eventually what to do for empty content should be moved out to the calling components.
    const content =
      this.data.length === 0 && this.context === 'changes'
        ? emptyContent
        : table;

    return html`${content}`;
  }
}

/**
 * @property {String} context - The context to use i.e. audit or changes.
 * @property {Boolean} filterable - Renders the no data found div if true.
 * @property {String} filter - The filter to apply when fetching the data.
 * @property {String} executionId - The executionId.
 * @property {Number} currentPage - position of the current page.
 * @property {Number} numEntries - number of entries in the page.
 * @property {Number} numPages - number of pages.
 * @property {Array} data - rows of data to display in the table.
 * @property {Array} selectedRows - The selectedRows.
 * @property {Array} columns - The colunms to display in the table.
 *  @property {Boolean} poll - Flag to turn polling on or off for fetching the data.
 * @property {String} initializationState - the initializationState.
 */
definition('e-rest-paginated-table', {
  style,
  props: {
    context: { type: String, attribute: false, default: '' },
    filterable: { type: Boolean, attribute: true, default: false },
    filter: { type: String, attribute: false, default: '' },
    executionId: { type: String, attribute: false },
    currentPage: { type: Number, default: 1 },
    numEntries: { type: Number, default: 2000 },
    numPages: { type: Number, default: 1 },
    data: { type: Array, attribute: false, default: [] },
    selectedRows: { type: Array, attribute: false, default: [] },
    columns: { type: Array, default: [] },
    poll: { type: Boolean, attribute: false, default: false },
    initializationState: {
      type: String,
      attribute: false,
      default: InitializationStatus.SUCCESSFUL,
    },
  },
})(RestPaginatedTable);
