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

import style from './revert-all-button.css';

const POLLING_ID = 'revert-all-button';

/**
 * Component RevertAllButton is defined as
 * `<e-revert-all-button>`
 *
 * @extends {Button}
 */
export default class RevertAllButton extends Button {
  /**
   * Is called when the component added to the DOM.
   *
   * @function didConnect
   */
  async didConnect() {
    super.didConnect();
    this.disabled = true;
    setCallbackFunction(POLLING_ID, this._fetchInProgressAudit);
    enablePolling();
  }

  /**
   * Is called when the component is about to be disconnected from the DOM.
   *
   * @function didDisconnect
   */
  async didDisconnect() {
    super.didDisconnect();
    disablePolling(POLLING_ID);
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
   * Render the <e-revert-all-button> component. This function is called each time a
   * prop changes.
   */
  render() {
    return html`${super.render()}`;
  }
}

/**
 * @property {String} executionId The execution Id.
 */
definition('e-revert-all-button', {
  style,
  props: {
    executionId: { type: String, attribute: true },
  },
})(RevertAllButton);
