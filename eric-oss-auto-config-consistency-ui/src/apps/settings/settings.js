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
import { Tab, Tabs } from '@eui/layout';
import { Notification } from '@eui/base';

import Scope from '../../components/scope/scope.js';
import Ruleset from '../../components/ruleset/ruleset.js';

import { appName } from '../../utils/appNames.js';
// import {
//   setupNavigationMenu,
// } from '../../utils/navMenuUtils.js';

import style from './settings.css';
import { InitializationStatus } from '../../utils/initialization/initializationStatus.js';
import AccessDeniedError from '../../components/access-denied-error/access-denied-error.js';

/**
 * Settings is defined as
 * `<e-settings>`
 *
 * @extends {App}
 */
export default class Settings extends App {
  static get components() {
    return {
      'eui-tab': Tab,
      'eui-tabs': Tabs,
      'e-scope': Scope,
      'eui-notification': Notification,
      'e-ruleset': Ruleset,
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
    this.bubble('app:title', { displayName: this.i18n?.SETTINGS });
    this.bubble('app:lineage', { metaData: this.metaData });
    // setupNavigationMenu(this, appName.settings);
  }

  /**
   * Event handler for when the ruleset tab is selected, forces an update for rulesets
   * @function _updateRulesetData
   * @private
   */
  _updateRulesetData() {
    const ruleset = this.shadowRoot.querySelector('e-ruleset');
    ruleset.getRulesets();
  }

  /**
   * Event handler for when the scope tab is selected, forces an update for node sets
   * @function _updateNodeSetData
   * @private
   */
  _updateNodeSetData() {
    const scope = this.shadowRoot.querySelector('e-scope');
    scope.getNodeSets();
  }

  /**
   * Is called by the user agent when an event is sent to the EventListener.
   *
   * @function handleEvent
   * @param event An event.
   */
  handleEvent(event) {
    if (
      event.type === 'e-ruleset:access-denied' ||
      event.type === 'e-scope:access-denied'
    ) {
      this.initializationState = InitializationStatus.ACCESS_DENIED;
    }
  }

  /**
   * Render the <e-settings> app. This function is called each time a
   * prop changes.
   */
  render() {
    if (this.initializationState === InitializationStatus.ACCESS_DENIED) {
      return html`<e-access-denied-error></e-access-denied-error>`;
    }
    return html`<eui-tabs>
      <eui-tab selected @eui-tab:select=${() => this._updateRulesetData()}
        ><label>${this.i18n?.RULESETS}</label></eui-tab
      >
      <eui-tab @eui-tab:select=${() => this._updateNodeSetData()}
        ><label>${this.i18n?.SCOPE}</label></eui-tab
      >
      <div slot="content">
        <e-ruleset @e-ruleset:access-denied=${this}></e-ruleset>
      </div>
      <div slot="content">
        <e-scope @e-scope:access-denied=${this}></e-scope>
      </div>
    </eui-tabs>`;
  }
}

/**
 * @property {String} initializationState - the initilization status
 */
definition(`e-${appName.settings}`, {
  style,
  props: {
    initializationState: { type: String },
  },
})(Settings);

Settings.register();
