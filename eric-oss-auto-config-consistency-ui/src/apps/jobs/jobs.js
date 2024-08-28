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

import { App, html, definition } from '@eui/app';
import { Button } from '@eui/base/button';
import { Dialog } from '@eui/base/dialog';
import { Icon } from '@eui/theme/icon';
import { Notification } from '@eui/base/notification';
import { Table } from '@eui/table';
import { nothing } from '@eui/lit-component';

import InitializationError from '../../components/initialization-error/initialization-error.js';
import AccessDeniedError from '../../components/access-denied-error/access-denied-error.js';

import { restCalls } from '../../utils/restCallUtils.js';
import { routes } from '../../utils/routeUtils.js';
import { appName } from '../../utils/appNames.js';
import { logWarning } from '../../utils/logUtils.js';
// import {
//   setupNavigationMenu,
// } from '../../utils/navMenuUtils.js';
import { InitializationStatus } from '../../utils/initialization/initializationStatus.js';
import { updateJobObject } from '../../utils/objectMapper.js';
import {
  createNotification,
  NotificationStatus,
} from '../../utils/notification/notifications.js';
import {
  findNewEntries,
  removeDeletedEntry,
  sortByNewest,
} from '../../utils/tableUtils.js';

import * as ATTR from '../../utils/attributes/jobAttributes.js';
import style from './jobs.css';

/**
 * Jobs is defined as
 * `<e-jobs>`
 *
 * @extends {App}
 */
export default class Jobs extends App {
  static get components() {
    return {
      'eui-button': Button,
      'eui-dialog': Dialog,
      'eui-icon': Icon,
      'eui-notification': Notification,
      'eui-table': Table,
      'e-initialization-error': InitializationError,
      'e-access-denied-error': AccessDeniedError,
    };
  }

  get meta() {
    return import.meta;
  }

  /**
   * Is called when the component added to the DOM.
   *
   * @function didConnect
   */
  async didConnect() {
    this.bubble('app:title', { displayName: this.i18n?.JOBS });
    this.bubble('app:lineage', { metaData: this.metaData });
    const buttonElement = this.createElement('eui-button');
    buttonElement.textContent = this.i18n?.CREATE_JOB || 'Create Job';
    buttonElement.primary = true;
    buttonElement.href = `${window.location.pathname}#${routes['create-job']}`;
    buttonElement.disabled = false;
    this.bubble('app:actions', {
      actions: [buttonElement],
    });
    // setupNavigationMenu(this, appName.jobs);
    await this._updateTableData();
  }

  /**
   * Is called when the component is about to be disconnected from the DOM.
   *
   * @function didDisconnect
   */
  didDisconnect() {
    this._setRowSelection(undefined);
    this.data = [];
  }

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
    this._enableJobOptions();
  }

  /**
   * Returns the jobs table.
   *
   * @function _getJobsTable
   * @private
   */
  _getJobsTable() {
    return this.shadowRoot.querySelector('eui-table');
  }

  /**
   * Returns the currently selected job name.
   *
   * @function _getJobName
   * @private
   */
  _getJobName() {
    return this.selectedRow === null ? '' : this.selectedRow[ATTR.JOB_NAME];
  }

  /**
   * Navigates to the Executions app with filter.
   *
   * @function _navigateToFilteredExecutions
   * @private
   */
  _navigateToFilteredExecutions() {
    const url = `/#${routes.executions}?jobName=${this._getJobName()}`;
    window.EUI.Router.goto(url);
  }

  /**
   * Navigates to the update-job app.
   *
   * @function _navigateToUpdateJob
   * @private
   */
  _navigateToUpdateJob() {
    const url = `/#${routes['update-job']}?updateJob=${this.selectedRow.jobName}`;
    window.EUI.Router.goto(url);
  }

  /**
   * Renders the "View Executions", "Update Job" and "Delete Job" buttons.
   *
   * @function _viewJobOptionsButtons
   * @private
   */
  _viewJobOptionsButtons() {
    if (this.data?.length !== 0) {
      return html` <span class="action-buttons">
        <eui-button
          id="view-executions"
          disabled
          @click="${() => this._navigateToFilteredExecutions()}"
          >${this.i18n?.VIEW_EXECUTIONS}</eui-button
        >
        <eui-button
          id="update-job"
          disabled
          @click="${() => this._navigateToUpdateJob()}"
          >${this.i18n?.UPDATE_JOB}</eui-button
        >
        <eui-button
          id="delete-job"
          disabled
          @click="${() => this._showDeleteDialog()}"
          >${this.i18n?.DELETE}</eui-button
        >
        <eui-dialog id="delete-dialog" label="${this.i18n?.CONFIRM_DELETE}">
          <div slot="content"></div>
          <eui-button slot="bottom" warning @click="${() => this._deleteJob()}"
            >${this.i18n?.DELETE_JOB}</eui-button
          >
        </eui-dialog>
      </span>`;
    }
    return html`${nothing}`;
  }

  /**
   * Determines the ability to enable/disable the "View Executions", "Update Job" and "Delete Job" buttons.
   * True disables the component, False enables it.
   *
   * @function _enableJobOptions
   * @private
   */
  _enableJobOptions() {
    const viewExecutions = this.shadowRoot.querySelector(
      'eui-button#view-executions',
    );
    const updateJob = this.shadowRoot.querySelector('eui-button#update-job');
    const deleteJob = this.shadowRoot.querySelector('eui-button#delete-job');
    if (viewExecutions) {
      if (this.selectedRow?.[ATTR.JOB_NAME]) {
        viewExecutions.disabled = false;
        updateJob.disabled = false;
        deleteJob.disabled = false;
      } else {
        viewExecutions.disabled = true;
        updateJob.disabled = true;
        deleteJob.disabled = true;
      }
    }
  }

  /**
   * Shows the delete confirmation dialog.
   *
   * @function _showDeleteDialog
   * @private
   */
  _showDeleteDialog() {
    const message = `${this.i18n?.DELETE_JOB_MESSAGE} ${this._getJobName()}?`;
    this.shadowRoot.querySelector(
      '#delete-dialog div[slot=content]',
    ).textContent = message;
    this.shadowRoot.querySelector('#delete-dialog').showDialog();
  }

  /**
   * Makes a REST call to get all jobs from EACC.
   *
   * @function _updateTableData
   * @private
   */
  async _updateTableData() {
    fetch(restCalls.eacc.getJobs.url)
      .then(response => {
        if (!response.ok) {
          logWarning(
            `Get jobs failed, reason: status code: ${response.status}, status text: ${response.statusText}`,
          );
          return Promise.reject(response.status);
        }
        return response.json();
      })
      .then(res => {
        const updatedJobs = res.map(updateJobObject);

        if (this.data.length === 0) {
          if (this.createdJob) {
            sortByNewest(ATTR.JOB_NAME, this.createdJob, updatedJobs);
          }
          this.data = [...updatedJobs];
        }
        this._updateTableContents(updatedJobs);
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

  /**
   * Updates the jobs table with deletes and creations.
   *
   * @function _updateTableContents
   * @param jobs The jobs to update the table with.
   * @private
   */
  _updateTableContents(jobs) {
    const tableElement = this._getJobsTable();
    if (tableElement) {
      // Currently no modify scenario
      if (this.createdJob) {
        sortByNewest(ATTR.JOB_NAME, this.createdJob, jobs);
        tableElement.data = [...jobs];
      }

      findNewEntries(tableElement.data, jobs, ATTR.JOB_NAME).forEach(
        execution => {
          tableElement.data = [...tableElement.data, execution];
        },
      );

      // Needs to be placed here once all jobs are removed
      if (tableElement.data.length === 0) {
        this.data = [];
      }
    }
  }

  /**
   * Makes a DELETE request to remove a job from EACC
   *
   * @function _deleteJob
   * @private
   */
  _deleteJob() {
    fetch(`${restCalls.eacc.deleteJob.url}/${this._getJobName()}`, {
      method: restCalls.eacc.deleteJob.method,
      headers: restCalls.eacc.deleteJob.headers,
    })
      .then(response => {
        if (!response.ok) {
          logWarning(
            `Delete Job failed, reason: status code: ${response.status}, status text: ${response.statusText}`,
          );
          let title = `${
            this.i18n?.DELETED_FAILURE_MESSAGE
          } ${this._getJobName()}`;
          let description;
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
            default:
              description = this.i18n?.DELETE_FAILURE_GENERIC_DESCRIPTION;
          }

          createNotification(
            this,
            NotificationStatus.FAILURE,
            title,
            description,
          );
        } else {
          createNotification(
            this,
            NotificationStatus.SUCCESS,
            `${this._getJobName()} ${this.i18n?.DELETED_SUCCESS_MESSAGE}`,
          );
          this.data = [
            ...removeDeletedEntry(this.data, this.selectedRow, ATTR.JOB_NAME),
          ];
          this._setRowSelection(undefined);
        }
      })
      .catch(error => {
        createNotification(
          this,
          NotificationStatus.FAILURE,
          `${this.i18n?.DELETED_FAILURE_MESSAGE} ${this._getJobName()}`,
          this.i18n?.DELETE_FAILURE_GENERIC_DESCRIPTION,
        );
        logWarning(`Unable to delete job, reason: ${error}`);
      })
      .finally(() => {
        if (this.shadowRoot.querySelector('#delete-dialog')) {
          this.shadowRoot.querySelector('#delete-dialog').hideDialog();
        }
      });
  }

  /**
   * Returns the job columns object.
   *
   * @function _getJobColumns
   * @private
   */
  _getJobColumns() {
    // prettier-ignore
    return [
      { title: this.i18n?.JOB_NAME, attribute: ATTR.JOB_NAME, sortable: true },
      { title: this.i18n?.SCHEDULE, attribute: ATTR.SCHEDULE, sortable: true },
      { title: this.i18n?.RULESET_NAME, attribute: ATTR.RULESET_NAME, sortable: true },
      { title: this.i18n?.SCOPE_NAME, attribute: ATTR.SCOPE_NAME, sortable: true },
    ];
  }

  /**
   * Render the <e-jobs> app. This function is called each time a
   * prop changes.
   */
  render() {
    const header = html` <p class="app-header">
      <span class="executions-display">
        ${this.i18n?.JOBS} (${this.data.length})
      </span>
      ${this._viewJobOptionsButtons()}
    </p>`;

    const emptyStateContent = html` <div class="app-empty-state">
      <eui-icon name="info"></eui-icon>
      <p>${this.i18n?.ZERO_JOBS}</p>
    </div>`;

    const table = html` <div class="app-table-container">
      <eui-table
        .columns=${this._getJobColumns()}
        .data=${this.data}
        @eui-table:row-select=${event => this._rowSelected(event)}
        virtual-scroll
        single-select
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
 * @property {String} createdJob - the createdJob.
 * @property {Array} data - the data.
 * @property {String} initializationState - shows the initializationError if Failure
 */
definition(`e-${appName.jobs}`, {
  style,
  props: {
    createdJob: { type: String },
    data: { type: Array, default: [] },
    initializationState: {
      type: String,
      default: InitializationStatus.SUCCESSFUL,
    },
  },
})(Jobs);

Jobs.register();
