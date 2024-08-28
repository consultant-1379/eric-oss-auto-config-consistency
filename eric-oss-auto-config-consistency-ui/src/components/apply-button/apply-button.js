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

import { html, definition } from '@eui/lit-component';
import { Button } from '@eui/base/button';

import { changesInProgress } from '../../utils/rest/utilityCalls.js';

import {
  disablePolling,
  enablePolling,
  setCallbackFunction,
} from '../../utils/polling/poll.js';

import style from './apply-button.css';

/**
 * Component ApplyButton is defined as
 * `<e-apply-button>`
 *
 * @extends {Button}
 */
export default class ApplyButton extends Button {
  /**
   * Is called when the component added to the DOM.
   *
   * @function didConnect
   */
  async didConnect() {
    super.didConnect();
    this.disabled = true;
    setCallbackFunction(this.pollingId, this._fetchInProgressAudit);
    enablePolling();
  }

  /**
   * Is called when the component is about to be disconnected from the DOM.
   *
   * @function didDisconnect
   */
  async didDisconnect() {
    super.didDisconnect();
    disablePolling(this.pollingId);
    this.disabled = true;
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
   * Makes a REST call to get the audit results and then filter it to get the applied changes based on the changeStatus
   *
   * @function _fetchInProgressAudit
   * @private
   */
  _fetchInProgressAudit = async () => {
    this.disabled = await changesInProgress(this.executionId);
  };

  /**
   * Render the <e-apply-all-button> component. This function is called each time a
   * prop changes.
   */
  render() {
    return html`${super.render()}`;
  }
}

/**
 * @property {String} executionId The execution Id.
 * @property {String} pollingId The Id to use for the polling callback.
 */
definition('e-apply-button', {
  style,
  props: {
    executionId: { type: String, attribute: true },
    pollingId: { type: String, attribute: true },
  },
})(ApplyButton);
