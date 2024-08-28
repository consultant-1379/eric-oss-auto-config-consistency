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

import { DEFAULT_POLLING_STATE, POLLING_INTERVAL } from '../changeConfig.js';
import { logWarning } from '../logUtils.js';

let pollingTimer = DEFAULT_POLLING_STATE;
let isPolling = false;

const callbacks = new Map();

/**
 * Sets the callback function that will be used during polling.
 *
 * @function setCallbackFunction
 * @param id The polling id.
 * @param _function The function to use.
 */
const setCallbackFunction = (id, _function) => {
  callbacks.set(id, _function);
};

/**
 * Checks to see if the polling timer is in the default state.
 *
 * @function isPollingInDefaultState
 */
const isPollingInDefaultState = () => pollingTimer === DEFAULT_POLLING_STATE;

/**
 * Polls at an interval of 15 seconds and executes the user supplied callback.
 *
 * @function poll
 */
const poll = () => {
  if (!isPolling) {
    if (!isPollingInDefaultState()) {
      clearTimeout(pollingTimer);
      pollingTimer = DEFAULT_POLLING_STATE;
    }
  } else {
    try {
      callbacks.forEach(_callback => _callback());
      if (!isPollingInDefaultState()) {
        clearTimeout(pollingTimer);
      }
      pollingTimer = setTimeout(poll, POLLING_INTERVAL);
    } catch (err) {
      logWarning(`Failed to poll, reason: ${err}`);
    }
  }
};

/**
 * Enables the polling mechanism.
 *
 * @function enablePolling
 */
const enablePolling = () => {
  isPolling = true;
  if (isPollingInDefaultState()) {
    poll();
  }
};

/**
 * Disables the polling mechanism.
 *
 * @function disablePolling
 * @param id The polling id.
 */
const disablePolling = id => {
  if (callbacks.has(id)) {
    callbacks.delete(id);
  }

  if (callbacks.size === 0) {
    isPolling = false;
    pollingTimer = DEFAULT_POLLING_STATE;
  }
};

export { enablePolling, disablePolling, setCallbackFunction };
