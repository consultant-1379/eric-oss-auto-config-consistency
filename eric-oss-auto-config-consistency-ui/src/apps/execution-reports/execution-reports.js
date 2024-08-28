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

import AccessDeniedError from '../../components/access-denied-error/access-denied-error.js';
import ExecutionsTabArea from '../../components/executions-tab-area/executions-tab-area.js';
import { routes } from '../../utils/routeUtils.js';
import { appName } from '../../utils/appNames.js';
import { InitializationStatus } from '../../utils/initialization/initializationStatus.js';

import style from './execution-reports.css';

/**
 * Reports is defined as
 * `<e-execution-reports>`
 *
 * @extends {App}
 */
export default class ExecutionReports extends App {
  static get components() {
    return {
      'e-executions-tab-area': ExecutionsTabArea,
      'e-access-denied-error': AccessDeniedError,
    };
  }

  static get properties() {
    return {
      data: Object,
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
    this.bubble('app:breadcrumb', {
      breadcrumb: [
        {
          displayName:
            this.i18n?.AUTOMATED_CONFIGURATION_CONSISTENCY ||
            'Automated Configuration Consistency',
        },
        {
          displayName: this.i18n?.EXECUTIONS_OVERVIEW || 'Executions Overview',
          action: () => {
            window.EUI.Router.goto(`/#${routes.executions}`);
          },
        },
        {
          displayName: this.jobName,
        },
      ],
    });
  }

  /**
   * Called by the user agent when an event is sent to the EventListener.
   *
   * @function handleEvent
   * @param event An event.
   */
  handleEvent(event) {
    if (event.type === 'e-executions-tab-area:access-denied') {
      this.initializationState = InitializationStatus.ACCESS_DENIED;
    }
  }

  /**
   * Render the <e-execution-reports> app. This function is called each time a
   * prop changes.
   */
  render() {
    if (this.initializationState === InitializationStatus.ACCESS_DENIED) {
      return html`<e-access-denied-error></e-access-denied-error>`;
    }
    const content = html`<div class="audit-report-table-container">
      <e-executions-tab-area
        @e-executions-tab-area:access-denied=${this}
        id="executions-tab-area"
        .executionId=${this.executionId}
      ></e-executions-tab-area>
    </div>`;

    return html`${content}`;
  }
}

/**
 * @property {String} executionId - the executionId.
 * @property {String} jobName - the jobname.
 * @property {String} initializationState - the initialization status
 */
definition(`e-${appName['execution-reports']}`, {
  style,
  props: {
    executionId: { type: String, attribute: true },
    jobName: { type: String, attribute: true, default: '' },
    initializationState: {
      type: String,
      default: InitializationStatus.SUCCESSFUL,
    },
  },
})(ExecutionReports);

ExecutionReports.register();
