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

import {
  SCHEDULE_TIME,
  RULESET_NAME,
  SCOPE_NAME,
} from './attributes/jobAttributes.js';
import { adjustCronToTimezone } from './cronUtils.js';

/**
 * Returns the warning component along with the message.
 *
 * @function returnWarningComponent
 * @param className Classname of the icon.
 * @param iconName Name of the Icon to be rendered.
 * @param message Warning message to be shown.
 * @private
 */
const returnWarningComponent = (className, iconName, message) =>
  `<div class="input__validation">
              <eui-icon class=${className} name=${iconName}> </eui-icon>
              <span class="input__validation-message">${message}</span>
            </div>`;

/**
 * Calls the warning component along with the Icon name and the message.
 *
 * @function validationMessage
 * @param message Warning message to be passed on to the warning component.
 * @private
 */
const validationMessage = message =>
  returnWarningComponent('icon-triangle-warning', 'triangle-warning', message);

/**
 * Display style if warning exists for a particular component.
 *
 * @function displayStyle
 * @param createJob An object of create job app.
 * @param elementName Name of the component.
 * @param styleWarning Style for the warning (to display or not).
 * @param warningMsg Warning message.
 * @private
 */
const displayStyle = (createJob, elementName, styleWarning, warningMsg) => {
  createJob.shadowRoot.getElementById(elementName).style = styleWarning;
  if (warningMsg !== '') {
    createJob.shadowRoot.getElementById(
      elementName,
    ).innerHTML = `${validationMessage(warningMsg)}`;
  }
};

/**
 * Clear Warnings associated with each components rendered in create job app.
 *
 * @function clearWarnings
 * @param createJob Create Job object passed in.
 */
const clearWarnings = createJob => {
  if (!createJob.updateJob) {
    createJob._getJobNameField().setCustomValidity('');
  }
  displayStyle(createJob, `${SCHEDULE_TIME}Warning`, 'display:none', '');
  displayStyle(createJob, `${RULESET_NAME}Warning`, 'display:none', '');
  displayStyle(createJob, `${SCOPE_NAME}Warning`, 'display:none', '');
  displayStyle(createJob, 'scheduleString', 'display:none', '');
};

/**
 * Fetching Combo box in the timer component.
 *
 * @function getCombos
 * @param createJobApp Create Job object passed in.
 * @param paramName Id of the parameter
 * @param i Combo Box index
 */
const getCombos = (createJobApp, paramName, i) =>
  createJobApp.shadowRoot
    .querySelector(paramName)
    .shadowRoot.querySelector('.time-picker')
    .querySelectorAll('eui-combo-box')
    [i].shadowRoot.querySelector('.dropdown');

/**
 * Get value from the dropdown components.
 *
 * @function getValueFromDropDown
 * @param paramName Id of job param name.
 * @param createJob Create Job object passed in.
 */
const getValueFromDropDown = (paramName, createJobApp) => {
  let value;
  createJobApp.shadowRoot
    .querySelector(paramName)
    .shadowRoot.querySelectorAll('eui-menu > eui-menu-item')
    .forEach(element => {
      if (element.selected === true) {
        value = element.label;
      }
    });
  return value;
};

/**
 * Get value from the Input tag components.
 *
 * @function getValueFromInputTag
 * @param paramName Id of job param name.
 * @param createJob Create Job object passed in.
 */
const getValueFromInputTag = (paramName, createJobApp) =>
  createJobApp.shadowRoot.querySelector(paramName).value;

/**
 * Returns the error message for dropdown if server not available.
 *
 * @function getRetrievalErrorMessage
 */
const getRetrievalErrorMessage = (
  RETRIEVAL_ERROR_MSG_SHORT,
  SERVER_ERROR,
) => `<eui-menu-item disabled>
          <div slot="right">
            <eui-icon color="red" name="failed"></eui-icon>
            &nbsp;${RETRIEVAL_ERROR_MSG_SHORT}<br><br>${SERVER_ERROR}
          </div>
      </eui-menu-item>`;

/**
 * Populates the schedule dropdowns with hours and minutes values.
 *
 * @function populateScheduleDropdowns
 * @param createJob Create Job object passed in.
 */
const populateScheduleDropdowns = createJob => {
  let splitValues = [];
  try {
    splitValues = adjustCronToTimezone(createJob.job.schedule).split(' ');
  } catch (error) {
    splitValues = '* * * * * *'.split(' ');
  }
  const hours = splitValues[2];
  const minutes = splitValues[1];
  getCombos(createJob, `#${SCHEDULE_TIME}`, 0)
    .querySelectorAll('eui-menu > eui-menu-item')
    .forEach(element => {
      element.selected = +element.value === +hours;
    });
  getCombos(createJob, `#${SCHEDULE_TIME}`, 1)
    .querySelectorAll('eui-menu > eui-menu-item')
    .forEach(element => {
      element.selected = +element.value === +minutes;
    });
};

export {
  clearWarnings,
  getValueFromDropDown,
  getValueFromInputTag,
  displayStyle,
  getRetrievalErrorMessage,
  populateScheduleDropdowns,
};
