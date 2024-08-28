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
import { TextField } from '@eui/base/text-field';
import { Dropdown } from '@eui/base/dropdown';
import { Button } from '@eui/base/button';
import { TimePicker } from '@eui/base/time-picker';
import { Menu } from '@eui/base/menu';
import { MenuItem } from '@eui/base/menu-item';
import { Icon } from '@eui/theme/icon';
import { Notification } from '@eui/base';

import { restCalls } from '../../utils/restCallUtils.js';
import { logWarning } from '../../utils/logUtils.js';
import {
  alignRulesetValue,
  alignScopeValue,
} from '../../utils/objectMapper.js';
import {
  JOB_NAME,
  SCHEDULE_EXPRESSION,
  SCHEDULE_FREQUENCY,
  SCHEDULE_TIME,
} from '../../utils/attributes/jobAttributes.js';
import {
  getValueFromDropDown,
  getValueFromInputTag,
  displayStyle,
  getRetrievalErrorMessage,
  populateScheduleDropdowns,
  clearWarnings,
} from '../../utils/createJobUtils.js';
// import { setupNavigationMenu } from '../../utils/navMenuUtils.js';
import {
  createNotification,
  NotificationStatus,
  MANUAL_CLOSE,
} from '../../utils/notification/notifications.js';
import { appName } from '../../utils/appNames.js';
import { routes } from '../../utils/routeUtils.js';
import { SCOPE_NAME } from '../../utils/attributes/scopeAttributes.js';
import { RULESET_NAME } from '../../utils/attributes/rulesetAttributes.js';

import style from './create-job.css';
import { updateLocaleString } from '../../utils/localization/localeUtils.js';
import { convertToCron, isNumeric } from '../../utils/cronUtils.js';
import { InitializationStatus } from '../../utils/initialization/initializationStatus.js';
import AccessDeniedError from '../../components/access-denied-error/access-denied-error.js';

const JOB_NAME_REGEX = /^[A-Za-z0-9\-\\_]{1,}$/g;
const DAILY = 'Daily';
const TODAY = 'Today';

/**
 * CreateJob is defined as
 * `<e-create-job>`
 *
 * @extends {App}
 */
export default class CreateJob extends App {
  constructor() {
    super();
    this.job = {};
    this.jobsList = [];
  }

  static get components() {
    return {
      'eui-button': Button,
      'eui-dropdown': Dropdown,
      'eui-text-field': TextField,
      'eui-time-picker': TimePicker,
      'eui-notification': Notification,
      'eui-icon': Icon,
      'eui-menu': Menu,
      'eui-menu-item': MenuItem,
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
    this.bubble('app:lineage', { metaData: this.metaData });
    // setupNavigationMenu(this, `${appName.jobs}`);
    await this._getJobs();
    if (this.initializationState === InitializationStatus.SUCCESSFUL) {
      populateScheduleDropdowns(this);
      this._setDefaultValueForScheduleFrequency();
      this._getRulesets();
      this._getScopes();
      if (!this.updateJob) {
        this._getJobNameField().shadowRoot.querySelector(
          'input',
        ).style.textTransform = 'lowercase';
        this._getJobNameField().value = '';
      }
    }
  }

  /**
   * Is called when the component is disconnected from the DOM.
   *
   * @function didConnect
   */
  didDisconnect() {
    this.updateJob = null;
    this.job = {};
  }

  /**
   * Is called by the user agent when an event is sent to the EventListener.
   *
   * @function handleEvent
   * @param event A UI event.
   */
  handleEvent(event) {
    const paramName = event.target.id;
    // Default value applied for schedule frequency(Daily), no warnings for the field.
    if (paramName !== SCHEDULE_FREQUENCY) {
      displayStyle(this, `${paramName}Warning`, 'display:none', '');
    }

    // Schedule String should populate, only values for scheduleFrequency(Today/Daily) & scheduleTime are present.
    if (
      (paramName === SCHEDULE_FREQUENCY || paramName === SCHEDULE_TIME) &&
      !this._isEmpty(getValueFromInputTag(`#${SCHEDULE_TIME}`, this))
    ) {
      this.shadowRoot.getElementById('scheduleString').style = 'display:block';
      const frequency = getValueFromDropDown(`#${SCHEDULE_FREQUENCY}`, this);
      const time = getValueFromInputTag(`#${SCHEDULE_TIME}`, this);

      const scheduleString = this._composeScheduleString(frequency, time);
      this.shadowRoot.getElementById('scheduleString').textContent =
        scheduleString;
    } else if (this._isEmpty(getValueFromInputTag(`#${SCHEDULE_TIME}`, this))) {
      this.shadowRoot.getElementById('scheduleString').style = 'display:none';
    }
  }

  /**
   * Makes a REST call to get all jobs from the EACC back-end.
   *
   * @function _getJobs
   * @private
   */
  async _getJobs() {
    await fetch(restCalls.eacc.getJobs.url)
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
        this.jobsList = [...res];
        if (this.updateJob) {
          const jobToUpdate = this.jobsList.find(
            element => element.jobName === this.updateJob,
          );
          this.job = jobToUpdate;
        }
      })
      .catch(error => {
        if (error === 403) {
          this.initializationState = InitializationStatus.ACCESS_DENIED;
        }
        logWarning(`Unable to fetch jobs, reason: ${error}`);
      });
  }

  /**
   * Sets the schedule frequency dropdown value.
   *
   * @function _setDefaultValueForScheduleFrequency
   * @private
   */
  _setDefaultValueForScheduleFrequency() {
    const splitCron = this.job?.schedule
      ? this.job?.schedule.split(' ')
      : '* * * * * *'.split(' ');
    const date = splitCron[3];
    const data = [...this.scheduleData];
    if (this.updateJob) {
      data.forEach(element => {
        if (isNumeric(date) && element.label === TODAY) {
          element.checked = true;
        } else if (!isNumeric(date) && element.label === DAILY) {
          element.checked = true;
        }
      });
    } else {
      data.forEach(element => {
        if (element.label === DAILY) {
          element.checked = true;
        }
      });
    }
    this.scheduleData = [...data];
  }

  /**
   * Makes a REST call to get all rulesets from the EACC back-end.
   *
   * @function _getRulesets
   * @private
   */
  _getRulesets() {
    fetch(restCalls.eacc.getRulesets.url)
      .then(response => {
        if (!response.ok) {
          logWarning(
            `Get rulesets failed, reason: status code: ${response.status}, status text: ${response.statusText}`,
          );
          return Promise.reject(response.statusText);
        }
        return response.json();
      })
      .then(res => {
        const rulesetNames = res.map(alignRulesetValue);
        this.ruleSetData = [...rulesetNames];
        this.ruleSetData.forEach(menuItem => {
          if (menuItem.label === this.job?.rulesetName) {
            menuItem.checked = true;
          }
        });
      })
      .catch(error => {
        this.shadowRoot
          .querySelector(`#${RULESET_NAME}`)
          .shadowRoot.querySelector('.dropdown > eui-menu').innerHTML =
          getRetrievalErrorMessage(
            this.i18n?.RETRIEVAL_ERROR_MSG_SHORT || 'Unable to retrieve data',
            this.i18n?.SERVER_ERROR_SHORT ||
              'The server encountered an internal error.',
          );
        logWarning(`Unable to fetch rulesets, reason: ${error}`);
      });
  }

  /**
   * Makes a REST call to get all scopes from the EACC back-end.
   *
   * @function _getScopes
   * @private
   */
  _getScopes() {
    fetch(restCalls.eacc.getNodeSets.url)
      .then(response => {
        if (!response.ok) {
          logWarning(
            `Get scopes failed, reason: status code: ${response.status}, status text: ${response.statusText}`,
          );
          return Promise.reject(response.statusText);
        }
        return response.json();
      })
      .then(res => {
        const scopeNames = res.map(alignScopeValue);
        this.scopeData = [...scopeNames];
        this.scopeData.forEach(menuItem => {
          if (menuItem.label === this.job?.scopeName) {
            menuItem.checked = true;
          }
        });
      })
      .catch(error => {
        this.shadowRoot
          .querySelector(`#${SCOPE_NAME}`)
          .shadowRoot.querySelector('.dropdown > eui-menu').innerHTML =
          getRetrievalErrorMessage(
            this.i18n?.RETRIEVAL_ERROR_MSG_SHORT || 'Unable to retrieve data',
            this.i18n?.SERVER_ERROR_SHORT ||
              'The server encountered an internal error.',
          );
        logWarning(`Unable to fetch scopes, reason: ${error}`);
      });
  }

  /**
   * Create a schedule String based on frequency and time
   *
   * @function _composeScheduleString
   * @param frequency frequency at which schedule is set.
   * @param time time at which schedule is set.
   * @private
   */
  _composeScheduleString(frequency, time) {
    let scheduleString;
    if (frequency === DAILY) {
      scheduleString = `Repeat ${frequency} at ${time}`;
    } else {
      scheduleString = `${frequency} at ${time}`;
    }
    return scheduleString;
  }

  /**
   * Updates the Job Parameter values
   *
   * @function _updateJobParameter
   * @param key job param name.
   * @param value value associated with the job param.
   * @private
   */
  _updateJobParameter(key, value) {
    this.job[key] = value;
  }

  /**
   * Fetches the Job Parameter values from the components
   *
   * @function _fetchJobParams
   * @private
   */
  _fetchJobParams() {
    if (this.updateJob) {
      this._updateJobParameter(JOB_NAME, this.updateJob.toLowerCase());
    }

    this._updateJobParameter(
      SCHEDULE_FREQUENCY,
      getValueFromDropDown(`#${SCHEDULE_FREQUENCY}`, this),
    );
    this._updateJobParameter(
      SCHEDULE_TIME,
      getValueFromInputTag(`#${SCHEDULE_TIME}`, this),
    );
    this._updateJobParameter(
      RULESET_NAME,
      getValueFromDropDown(`#${RULESET_NAME}`, this),
    );
    this._updateJobParameter(
      SCOPE_NAME,
      getValueFromDropDown(`#${SCOPE_NAME}`, this),
    );
  }

  /**
   * Check if a value is empty or not.
   *
   * @function _isEmpty
   * @param value Value to be checked if empty or not.
   * @private
   */
  _isEmpty(value) {
    return value === null || value === '' || value === undefined;
  }

  /**
   * Validate the Job param.
   *
   * @function _validateParam
   * @param paramName Param to be validated.
   * @param warningMsg Warning message to be displayed.
   * @private
   */
  _validateParam(paramName, warningMsg) {
    if (this._isEmpty(this.job[paramName])) {
      displayStyle(this, `${paramName}Warning`, 'display:block', warningMsg);
      return false;
    }
    displayStyle(this, `${paramName}Warning`, 'display:none', '');
    return true;
  }

  /**
   * Returns the Job Name compoenent in the DOM.
   *
   * @function _getJobNameField
   * @private
   */
  _getJobNameField() {
    return this.shadowRoot.querySelector(`eui-text-field#${JOB_NAME}`);
  }

  /**
   * Validate the Job Name param - job name is valid or does exists.
   *
   * @function _jobNameCheck
   * @private
   */
  _jobNameCheck() {
    if (this.updateJob) {
      return true;
    }
    const jobNameField = this._getJobNameField();
    const jobNameFieldValue = jobNameField.value.toLowerCase();
    this.job[JOB_NAME] = jobNameFieldValue;

    const matchedJobName = this.jobsList.filter(
      job => job[JOB_NAME] === jobNameFieldValue,
    );

    const found = new RegExp(JOB_NAME_REGEX).test(jobNameFieldValue);

    // Checking for Job Name field is empty
    if (this._isEmpty(jobNameFieldValue)) {
      jobNameField.setCustomValidity(this.i18n?.JOB_NAME_REQUIRED_WARNING);
      return false;
    }

    // Checking for Existsing Job Name
    if (matchedJobName.length !== 0 && !this._isEmpty(jobNameFieldValue)) {
      jobNameField.setCustomValidity(this.i18n?.JOB_NAME_EXISTS_WARNING);
      return false;
    }

    // Checking for Job Name Pattern Mismatch
    if (!found && !this._isEmpty(jobNameFieldValue)) {
      jobNameField.setCustomValidity(
        this.i18n?.JOB_NAME_PATTERN_MISMATCH_WARNING,
      );
      return false;
    }

    if (jobNameFieldValue.length < 4 || jobNameFieldValue.length > 100) {
      jobNameField.setCustomValidity(this.i18n?.JOB_NAME_LENGTH_WARNING);
      return false;
    }
    jobNameField.setCustomValidity('');
    return true;
  }

  /**
   * Validate All Job params, if valid save the job, if not valid display warning.
   * Clear the component values after saving.
   *
   * @function _validateAndSaveJob
   * @private
   */
  _validateAndSaveJob() {
    this._fetchJobParams();
    if (this._validateFields()) {
      if (this.updateJob) {
        this._updateJob();
      } else {
        this._createJob();
      }
    }
  }

  /**
   * Validates the job parameters used to create a new job.
   * @function _validateFields
   * @private
   *
   * @returns true if the parameters are valid, false otherwise
   */
  _validateFields() {
    if (
      this._jobNameCheck() & // eslint-disable-line no-bitwise
      this._validateParam(SCHEDULE_TIME, this.i18n?.FIELD_REQUIRED_WARNING) & // eslint-disable-line no-bitwise
      this._validateParam(RULESET_NAME, this.i18n?.FIELD_REQUIRED_WARNING) & // eslint-disable-line no-bitwise
      this._validateParam(SCOPE_NAME, this.i18n?.FIELD_REQUIRED_WARNING)
    ) {
      return true;
    }

    return false;
  }

  /**
   * Set the Create Job object.
   *
   * @function _createSaveJob
   */
  _createSaveJob() {
    this.job[SCHEDULE_EXPRESSION] = convertToCron(
      this.job[SCHEDULE_FREQUENCY],
      this.job[SCHEDULE_TIME],
    );
    delete this.job[SCHEDULE_FREQUENCY];
    delete this.job[SCHEDULE_TIME];
  }

  /**
   * Makes a REST call to validate and send the updated job to the EACC back-end.
   *
   * @function _updateJob
   * @private
   */
  _updateJob() {
    this._createSaveJob();
    fetch(`${restCalls.eacc.putJob.url}/${this.job[JOB_NAME]}`, {
      method: restCalls.eacc.putJob.method,
      headers: restCalls.eacc.putJob.headers,
      body: JSON.stringify(this.job),
    })
      .then(response => {
        if (!response.ok) {
          return Promise.reject(response.status);
        }
        return response.json();
      })
      .then(res => {
        window.EUI.Router.goto(`/#${routes.jobs}`);
        const substitutions = new Map();
        substitutions.set('%job_name%', res[JOB_NAME]);
        createNotification(
          this,
          NotificationStatus.SUCCESS,
          updateLocaleString(this.i18n?.JOB_UPDATED, substitutions),
        );
      })
      .catch(error => {
        let description;
        let title = this.i18n?.UNABLE_TO_SAVE_JOB;
        switch (error) {
          case 403:
            title = this.i18n?.ACCESS_DENIED;
            description = this.i18n?.REST_403_DESCRIPTION;
            break;
          case 409:
            description = this.i18n?.JOB_USED_IN_EXECUTION;
            break;
          default:
            description = this.i18n?.SERVER_ERROR_LONG;
        }
        createNotification(
          this,
          NotificationStatus.FAILURE,
          title,
          description,
          MANUAL_CLOSE,
        );
      });
  }

  /**
   * Makes a REST call to send the validated job to the EACC back-end.
   *
   * @function _createJob
   * @private
   */
  _createJob() {
    this._createSaveJob();
    fetch(restCalls.eacc.postJob.url, {
      method: restCalls.eacc.postJob.method,
      headers: restCalls.eacc.postJob.headers,
      body: JSON.stringify(this.job),
    })
      .then(response => {
        if (!response.ok) {
          logWarning(
            `Post Job failed, reason: status code: ${response.status}, status text: ${response.statusText}`,
          );
          return Promise.reject(response.status);
        }
        return response.json();
      })
      .then(job => {
        window.EUI.Router.goto(`/#${routes.jobs}?createdJob=${job[JOB_NAME]}`);
        const substitutions = new Map();
        substitutions.set('%job_name%', job[JOB_NAME]);
        createNotification(
          this,
          NotificationStatus.SUCCESS,
          updateLocaleString(this.i18n?.JOB_CREATED, substitutions),
        );
      })
      .catch(error => {
        let title = this.i18n?.UNABLE_TO_SAVE_JOB;
        let description;
        switch (error) {
          case 403:
            title = this.i18n?.ACCESS_DENIED;
            description = this.i18n?.REST_403_DESCRIPTION;
            break;
          default:
            description = this.i18n?.SERVER_ERROR_LONG;
        }
        createNotification(
          this,
          NotificationStatus.FAILURE,
          title,
          description,
          MANUAL_CLOSE,
        );
        logWarning(`Unable to post job, reason: ${error}`);
      });
  }

  /**
   * Render the <e-create-job> app. This function is called each time a
   * prop changes.
   */
  render() {
    if (this.initializationState === InitializationStatus.ACCESS_DENIED) {
      return html`<e-access-denied-error></e-access-denied-error>`;
    }

    const header = html`<p class="app-header">
      ${this.updateJob ? this.i18n?.UPDATE_JOB : this.i18n?.CREATE_JOB}
    </p>`;

    const jobName = !this.updateJob
      ? html`<div class="app-sub-header">${this.i18n?.JOB_NAME}</div>
          <div class="app-component app-single-component">
            <eui-text-field
              size="40"
              id=${JOB_NAME}
              @input="${() => this._jobNameCheck()}"
            >
            </eui-text-field>
          </div>`
      : html`<div class="app-sub-header">${this.i18n?.JOB_NAME}</div>
          <div class="app-component app-single-component">
            <p id=${JOB_NAME}>${this.updateJob}</p>
          </div>`;

    const scheduleFrequency = html`<eui-dropdown
      class="app-schedule-frequency"
      data-type="single"
      label=${this.i18n?.SCHEDULE_FREQUENCY}
      @eui-dropdown:change=${this}
      .data=${this.scheduleData}
      id=${SCHEDULE_FREQUENCY}
    >
    </eui-dropdown>`;

    const scheduleTime = html`<eui-time-picker
        class="app-schedule-time"
        step="1"
        min="00:00"
        max="23:59"
        id=${SCHEDULE_TIME}
        @eui-time-picker:change=${this}
        separate
      ></eui-time-picker>
      <div id="${SCHEDULE_TIME}Warning"></div>`;

    const schedule = html`<div class="app-sub-header">
        ${this.i18n?.SCHEDULE}
      </div>
      <div class="app-component app-multiple-component">
        Repeat
        <div class="frequency">${scheduleFrequency}</div>
        at
        <div class="time">${scheduleTime}</div>
      </div>
      <div id="scheduleString"></div>`;

    const ruleset = html`<div class="app-sub-header">${this.i18n?.RULESET}</div>
      <eui-dropdown
        class="app-component app-single-component ruleset"
        data-type="single"
        id=${RULESET_NAME}
        @eui-dropdown:change=${this}
        label=${this.i18n?.SELECT_RULESET}
        .data=${this.ruleSetData}
      ></eui-dropdown>
      <div id="${RULESET_NAME}Warning"></div>`;

    const scope = html`<div class="app-sub-header">${this.i18n?.SCOPE}</div>
      <eui-dropdown
        class="app-component app-single-component scope"
        data-type="single"
        id=${SCOPE_NAME}
        @eui-dropdown:change=${this}
        label=${this.i18n?.SELECT_SCOPE}
        .data=${this.scopeData}
      ></eui-dropdown>
      <div id="${SCOPE_NAME}Warning"></div>`;

    const saveButton = html`<eui-button
      id="saveButton"
      class="action-buttons"
      @click="${() => this._validateAndSaveJob()}"
      primary
      >${this.i18n?.SAVE}</eui-button
    >`;

    const cancelButton = html`<eui-button
      id="cancelButton"
      class="action-buttons"
      @click="${() => {
        clearWarnings(this);
        window.EUI.Router.goto(`/#${routes.jobs}`);
      }}"
      >${this.i18n?.CANCEL}</eui-button
    >`;

    const content = html`${jobName}${schedule}${ruleset}${scope}`;

    return html`<div class="row">
      <div class="col-1">${header}${content}</div>
      <div class="col-2">${cancelButton}${saveButton}</div>
    </div> `;
  }
}

/**
 * @property {String} updateJob - Job being updated (if job update and not creation)
 * @property {Array} ruleSetData - the ruleSetData.
 * @property {Array} scopeData - the scopeData.
 * @property {Array} scheduleData - the scheduleData.
 * @property {String} initializationState - the initialization status
 */
definition(`e-${appName['create-job']}`, {
  style,
  props: {
    updateJob: { type: String, attribute: true, default: null },
    ruleSetData: { type: Array, default: [] },
    scopeData: { type: Array, default: [] },
    scheduleData: {
      type: Array,
      default: [
        { label: DAILY, value: DAILY },
        { label: TODAY, value: TODAY },
      ],
    },
    initializationState: {
      type: String,
      default: InitializationStatus.SUCCESSFUL,
    },
  },
})(CreateJob);

CreateJob.register();
