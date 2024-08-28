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

import { LitComponent, html, definition } from '@eui/lit-component';
import { Button, Dialog } from '@eui/base';
import { LAUNCHER_URL } from '../../utils/restCallUtils.js';
import style from './access-denied-error.css';

/**
 * Component AccessDeniedError is defined as
 * `<e-access-denied-error>`
 *
 * @extends {LitComponent}
 */
export default class AccessDeniedError extends LitComponent {
  static get components() {
    return {
      'eui-button': Button,
      'eui-dialog': Dialog,
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
   * Render the <e-access-denied-error> component. This function is called each time a
   * prop changes.
   */
  render() {
    return html`<eui-dialog label="${this.i18n?.ACCESS_DENIED_TITLE}" show>
      <div slot="content">
        <p>
          ${this.i18n?.ROLE_DOES_NOT_ALLOW_ACCESS}<br />
          ${this.i18n?.CONTACT_SYSTEM_ADMIN}
        </p>
      </div>
      <p slot="cancel"></p>
      <eui-button slot="bottom" href="${LAUNCHER_URL}" primary
        >${this.i18n?.RETURN_TO_PORTAL}</eui-button
      >
    </eui-dialog>`;
  }
}

/**
 */
definition('e-access-denied-error', {
  style,
  props: {},
})(AccessDeniedError);
