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

import { App, html, definition } from '@eui/app';
import { Button } from '@eui/base/button';
import { Icon } from '@eui/theme/icon';
import { Loader } from '@eui/base/loader';
import { Link } from '@eui/base/link';
import { Table } from '@eui/table';
import { nothing } from '@eui/lit-component';

import InitializationError from '../../components/initialization-error/initialization-error.js';
import AccessDeniedError from '../../components/access-denied-error/access-denied-error.js';

import { restCalls } from '../../utils/restCallUtils.js';
import { routes } from '../../utils/routeUtils.js';
import { appName } from '../../utils/appNames.js';
import { logWarning } from '../../utils/logUtils.js';
import { customStatusCell } from '../../utils/columnUtils.js';
import { updateExecutionObject } from '../../utils/objectMapper.js';
import { InitializationStatus } from '../../utils/initialization/initializationStatus.js';
import {
  DEFAULT_POLLING_STATE,
  POLLING_INTERVAL,
} from '../../utils/executionsConfig.js';

import {
  findDeletedEntries,
  findNewEntries,
  sameObject,
} from '../../utils/tableUtils.js';

import * as ATTR from '../../utils/attributes/executionsAttributes.js';

import style from './executions.css';

const DEFAULT_JOB = '';
const JOB_NAME_PROPERTY = 'jobName';

/**
 * Executions is defined as
 * `<e-executions>`
 *
 * @extends {App}
 */
export default class Executions extends App {
  constructor() {
    super();
    this.pollingTimer = DEFAULT_POLLING_STATE;
    this.isPolling = false;
    this.unfilteredData = [];
    this.previousFilter = DEFAULT_JOB;
    this.invalidExecutionStatusesForReports = [
      ATTR.ExecutionState.AUDIT_FAILED,
      ATTR.ExecutionState.AUDIT_IN_PROGRESS,
      ATTR.ExecutionState.AUDIT_SKIPPED,
    ];
  }

  static get components() {
    return {
      'eui-button': Button,
      'eui-icon': Icon,
      'eui-link': Link,
      'eui-loader': Loader,
      'eui-table': Table,
      'e-initialization-error': InitializationError,
      'e-access-denied-error': AccessDeniedError,
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
   * @function didConnect
   */
  async didConnect() {
    this.bubble('app:title', { displayName: this.i18n?.JOB_EXECUTIONS });
    this.bubble('app:lineage', { metaData: this.metaData });
    // setupNavigationMenu(this, appName.executions);
    this._enablePolling();
  }

  /**
   * Is only ever called once when the component is upgraded. Rendering has taken place and changes flushed to the DOM.
   *
   * @function didUpgrade
   */
  async didUpgrade() {
    this._pollTableData();
    // openNavigationMenu();
    this._setTotalExecutions();
  }

  /**
   * Is called when the component is about to be disconnected from the DOM.
   *
   * @function didDisconnect
   */
  async didDisconnect() {
    this.jobName = DEFAULT_JOB;
    this.previousFilter = DEFAULT_JOB;
    this._disablePolling();
  }

  /**
   * Is called when an application that has previously been initialized and
   * cached is loaded into the display.
   *
   * @function onResume
   *
   */
  onResume() {
    this._updateTableData();
  }

  /**
   * Is called every time a prop changes.
   * @param changedProps The changed properties.
   */
  didChangeProps(changedProps) {
    if (changedProps.has(JOB_NAME_PROPERTY)) {
      const changedJobName = changedProps.get(JOB_NAME_PROPERTY);
      this.previousFilter = changedJobName;
      this._updateTableData();
    } else {
      super.didChangeProps(changedProps);
    }
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
      this._pollTableData();
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
   * Polls the executions endpoint for updates.
   *
   * @function _pollTableData
   * @private
   */
  _pollTableData = async () => {
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
        this.pollingTimer = setTimeout(this._pollTableData, POLLING_INTERVAL);
      } catch (err) {
        logWarning(`Failed to update executions data, reason: ${err}`);
      }
    }
  };

  /**
   * Makes a REST call to get all executions from the EACC back-end.
   *
   * @function _updateTableData
   * @private
   */
  _updateTableData() {
    fetch(restCalls.eacc.getExecutions.url)
      .then(response => {
        if (!response.ok) {
          logWarning(
            `Get executions failed, reason: status code: ${response.status}, status text: ${response.statusText}`,
          );
          return Promise.reject(response.status);
        }
        return response.json();
      })
      .then(res => {
        const updatedExecutions = res.map(updateExecutionObject);
        if (this.jobName !== DEFAULT_JOB) {
          this.unfilteredData = [...updatedExecutions];
          const filteredExecutions = updatedExecutions.filter(this._filterJobs);
          if (this.data.length === 0 || this.previousFilter !== this.jobName) {
            this.previousFilter = this.jobName;
            this._resetTableContents();
            this.data = [...filteredExecutions];
            this._setRowSelection(undefined);
          }
        } else if (this.data.length === 0 && updatedExecutions.length > 0) {
          this.data = [...updatedExecutions];
        }
        this._updateTableContents(updatedExecutions);
        this._setTotalExecutions();
        this.initializationState = InitializationStatus.SUCCESSFUL;
      })
      .catch(error => {
        if (error === 403) {
          this.initializationState = InitializationStatus.ACCESS_DENIED;
        } else {
          this.initializationState = InitializationStatus.FAILURE;
        }
      });
  }

  _getExecutionsTable() {
    return this.shadowRoot.querySelector('eui-table');
  }

  /**
   * Clears the executions table of any rows currently present in it.
   *
   * @function _resetTableContents
   * @private
   */
  _resetTableContents() {
    const tableElement = this._getExecutionsTable();
    if (tableElement?.data.length > 0) {
      tableElement.data.splice(0, tableElement.data.length);
      tableElement.data = [...tableElement.data];
      this._setTotalExecutions();
    }
  }

  /**
   * Updates the executions table with deletes, creations, and modifications.
   *
   * @function _updateTableContents
   * @param executions The executions to update the table with.
   * @private
   */
  _updateTableContents(executions) {
    const tableElement = this._getExecutionsTable();
    if (tableElement) {
      this._modifyExistingTableData(tableElement, executions);

      findNewEntries(tableElement.data, executions, ATTR.EXECUTION_ID)
        .filter(this._filterJobs)
        .forEach(execution => {
          tableElement.data = [...tableElement.data, execution];
        });

      findDeletedEntries(tableElement.data, executions, ATTR.EXECUTION_ID)
        .filter(this._filterJobs)
        .forEach(execution => {
          const index = tableElement.data.findIndex(existing =>
            sameObject(existing, execution, ATTR.EXECUTION_ID),
          );
          tableElement.data.splice(index, 1);
          tableElement.data = [...tableElement.data];

          if (
            execution[ATTR.EXECUTION_ID] ===
            this.selectedRow?.[ATTR.EXECUTION_ID]
          ) {
            this._setRowSelection(undefined);
          }
        });

      // Needs to be placed here once all executions are removed
      if (tableElement.data.length === 0) {
        this.data = [];
      }
    }
  }

  /**
   * Updates the existing elements in the table.
   *
   * @function _modifyExistingTableData
   * @param table The current table.
   * @param updatedExecutions The updated executions to compare against.
   * @private
   */
  _modifyExistingTableData(table, updatedExecutions) {
    updatedExecutions.filter(this._filterJobs).forEach(execution => {
      table.data.forEach((existing, index) => {
        if (sameObject(existing, execution, ATTR.EXECUTION_ID)) {
          table.data[index] = execution;
          table.data = [...table.data];
        }
      });
    });
  }

  /**
   * Filters executions based on the jobName attribute.
   *
   * @function _filterJobs
   * @param execution The execution to filter on.
   * @private
   */
  _filterJobs = execution => {
    if (DEFAULT_JOB === this.jobName) {
      return execution;
    }
    return execution[ATTR.JOB_NAME] === this.jobName ? execution : undefined;
  };

  /**
   * Called whenever a row in the executions table is selected.
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
    this._enableReports();
  }

  /**
   * Sets the total number of executions.
   *
   * @function _setTotalExecutions
   * @private
   */
  _setTotalExecutions() {
    const executions = this._getTotalExecutions();
    const totalExecutionsElement = this.shadowRoot.querySelector(
      '.app-header > span > span#execution-size',
    );
    totalExecutionsElement.textContent = executions;
  }

  /**
   * Returns the total number of executions.
   *
   * @function _getTotalExecutions
   * @private
   */
  _getTotalExecutions() {
    const tableElement = this._getExecutionsTable();
    return tableElement?.data === undefined
      ? this.data.length
      : tableElement.data.length;
  }

  /**
   * Determines the ability to enable/disable the 'View Reports' button.
   * True disables the component, False enables it.
   *
   * @function _enableReports
   * @private
   */
  _enableReports() {
    const viewReports = this.shadowRoot.querySelector('eui-button#viewReports');
    if (viewReports) {
      viewReports.disabled = !(
        this.selectedRow?.[ATTR.EXECUTION_ID] &&
        this._validExecutionStatus(this.selectedRow?.[ATTR.EXECUTION_STATUS])
      );
    }
  }

  /**
   * Check for valid status to view reports
   *
   * @function _validExecutionStatus
   * @private
   */
  _validExecutionStatus(status) {
    return (
      Object.values(ATTR.ExecutionState).includes(status) &&
      !this.invalidExecutionStatusesForReports.includes(status)
    );
  }

  /**
   * Navigates to the jobs app.
   *
   * @function _navigateToJobs
   * @private
   */
  _navigateToJobs() {
    window.EUI.Router.goto(`/#${routes.jobs}`);
  }

  /**
   * Navigates to the reports app.
   *
   * @function _navigateToReports
   * @private
   */
  _navigateToReports() {
    const reportPath = `/#${routes['execution-reports']}?executionId=${
      this.selectedRow[ATTR.EXECUTION_ID]
    }&jobName=${this.selectedRow[ATTR.JOB_NAME]}`;
    window.EUI.Router.goto(reportPath);
  }

  /**
   * Returns the execution columns object.
   *
   * @function _getExecutionColumns
   * @private
   */
  _getExecutionColumns() {
    // prettier-ignore
    return [
        { title: this.i18n?.EXECUTION_ID, attribute: ATTR.EXECUTION_ID, sortable: true },
        { title: this.i18n?.JOB_NAME, attribute: ATTR.JOB_NAME, sortable: true },
        { title: this.i18n?.EXECUTION_TYPE, attribute: ATTR.EXECUTION_TYPE, sortable: true },
        { title: this.i18n?.EXECUTION_START, attribute: ATTR.EXECUTION_STARTED_AT, sortable: true, sort: 'desc' },
        { title: this.i18n?.EXECUTION_END, attribute: ATTR.EXECUTION_ENDED_AT, sortable: true },
        { title: this.i18n?.STATUS, attribute: ATTR.EXECUTION_STATUS, cell: customStatusCell, sortable: true },
        { title: this.i18n?.ATTRIBUTES_AUDITED, attribute: ATTR.TOTAL_ATTRIBUTES_AUDITED, sortable: true },
        { title: this.i18n?.MOS_AUDITED, attribute: ATTR.TOTAL_MOS_AUDITED, sortable: true },
        { title: this.i18n?.INCONSISTENCIES, attribute: ATTR.INCONSISTENCIES_IDENTIFIED, sortable: true },
    ];
  }

  /**
   * Creates the 'View Reports' button if there is execution data.
   *
   * @function _viewReports
   * @private
   */
  _viewReports() {
    if (this.data?.length === 0) {
      return html`${nothing}`;
    }
    return html`<eui-button
      disabled
      @click="${() => this._navigateToReports()}"
      class="view-reports"
      id="viewReports"
      >${this.i18n?.VIEW_REPORTS}</eui-button
    >`;
  }

  /**
   * Creates the filter text and link to clear the filter.
   *
   * @function _viewFilter
   * @private
   */
  _viewFilter() {
    if (this.jobName !== DEFAULT_JOB) {
      return html` <span class="app-filter-text"
        >&nbsp;${this.jobName}&nbsp;
        <eui-link
          href="/#${routes.executions}"
          @click="${() => this._clearFilter()}"
          >${this.i18n?.CLEAR}
        </eui-link>
      </span>`;
    }
    return html`${nothing}`;
  }

  /**
   * Sets table data to unfiltered state and clears the selected job.
   *
   * @function _clearFilter
   * @private
   */
  _clearFilter() {
    this.jobName = DEFAULT_JOB;
    this.data = [...this.unfilteredData];
    const tableElement = this._getExecutionsTable();
    if (tableElement) {
      tableElement.data = [...this.unfilteredData];
    }
    this._setRowSelection(undefined);
    this._setTotalExecutions();
  }

  /**
   * Render the <e-executions> app. This function is called each time a prop changes.
   */
  render() {
    const header = html` <p class="app-header">
      <span class="executions-display">
        ${this.i18n?.JOB_EXECUTIONS} (<span id="execution-size">0</span>)
      </span>
      ${this._viewFilter()} ${this._viewReports()}
    </p>`;

    const emptyStateContent = html` <div class="app-empty-state">
        <eui-icon name="info"></eui-icon>
        <p>${this.i18n?.ZERO_JOBS_EXECUTED}</p>
      </div>
      <div class="app-empty-navigation">
        <p>
          <eui-button @click="${this._navigateToJobs}"
            >${this.i18n?.VIEW_JOBS}</eui-button
          >
        </p>
      </div>`;

    const table = html` <div class="app-table-container">
      <eui-table
        .columns=${this._getExecutionColumns()}
        .data=${this.data}
        @eui-table:row-select=${event => this._rowSelected(event)}
        virtual-scroll
        single-select
        resizable
        compact
        sortable
      ></eui-table>
    </div>`;

    const content = this.data.length === 0 ? emptyStateContent : table;

    switch (this.initializationState) {
      case InitializationStatus.SUCCESSFUL:
        return html`${header}${content}`;

      case InitializationStatus.ACCESS_DENIED:
        return html`<e-access-denied-error></e-access-denied-error>`;

      case InitializationStatus.FAILURE:
        return html`<e-initialization-error></e-initialization-error>`;

      default:
        return nothing;
    }
  }
}

/**
 * @property {Array} data - the data.
 * @property {String} jobName - the jobName.
 * @property {String} initializationState - shows the initializationError if Failure
 */
definition(`e-${appName.executions}`, {
  style,
  props: {
    data: { type: Array, attribute: false, default: [] },
    jobName: { type: String, attribute: true, default: DEFAULT_JOB },
    initializationState: {
      type: String,
      default: InitializationStatus.SUCCESSFUL,
    },
  },
})(Executions);

Executions.register();
