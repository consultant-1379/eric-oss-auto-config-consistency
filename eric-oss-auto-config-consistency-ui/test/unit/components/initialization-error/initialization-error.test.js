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
import { expect, fixture, html } from '@open-wc/testing';
import sinon from 'sinon';

import InitializationError from '../../../../src/components/initialization-error/initialization-error.js';

/* eslint-disable import/no-named-default */
import { default as LOCALE_EN_US } from '../../../../src/components/initialization-error/locale/en-us.json';
/* eslint-disable import/no-named-default */

const MOCK_LOCALE_RESPONSE = {
  json: () => LOCALE_EN_US,
  ok: true,
  status: 200,
};

let fetchStub;

describe('Given: An <e-initialization-error>', () => {
  before(() => {
    InitializationError.register();
    fetchStub = sinon.stub(window, 'fetch');

    fetchStub.callsFake(url => {
      if (url.indexOf('locale') !== -1) {
        return Promise.resolve(MOCK_LOCALE_RESPONSE);
      }
      return Promise.resolve(true);
    });
  });

  after(() => {
    sinon.restore();
  });

  describe('When: The app is displayed', () => {
    it(`Then: It should render with header text ${LOCALE_EN_US.ERROR_HEADER} and icon with the body containing ${LOCALE_EN_US.ERROR_BODY}`, async () => {
      const initializationComponent = await fixture(
        html`<e-initialization-error></e-initialization-error>`,
      );

      const appContainer =
        initializationComponent.shadowRoot.querySelector('.app-container');

      const headerMsg = appContainer.querySelector('.header-msg');
      expect(headerMsg.textContent).to.contain(LOCALE_EN_US.ERROR_HEADER);

      const bodyText = appContainer.querySelector('.body-msg span');
      expect(bodyText.textContent).to.contain(LOCALE_EN_US.ERROR_BODY);
    });
  });
});
