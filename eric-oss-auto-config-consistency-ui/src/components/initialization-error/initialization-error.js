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

import { Icon } from '@eui/theme/icon';

import style from './initialization-error.css';

/**
 * Component InitializationError is defined as
 * `<e-initialization-error>`
 *
 * @extends {LitComponent}
 */
export default class InitializationError extends LitComponent {
  static get components() {
    return {
      'eui-icon': Icon,
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
   * Render the <e-initialization-error> component. This function is called each time a
   * prop changes.
   */
  render() {
    return html`<div class="app-container">
      <div class="header-msg">
        <eui-icon name="failed" color="var(--red-52)"></eui-icon>
        ${this.i18n?.ERROR_HEADER}
      </div>
      <div class="body-msg"><span>${this.i18n?.ERROR_BODY}</span></div>
    </div>`;
  }
}

definition('e-initialization-error', {
  style,
  props: {},
})(InitializationError);
