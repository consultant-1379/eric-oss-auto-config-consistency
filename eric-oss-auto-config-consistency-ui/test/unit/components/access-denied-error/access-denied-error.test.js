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

import { expect, fixture } from '@open-wc/testing';
import sinon from 'sinon';
import AccessDeniedError from '../../../../src/components/access-denied-error/access-denied-error.js';

/* eslint-disable import/no-named-default */
import { default as LOCALE_EN_US } from '../../../../src/components/access-denied-error/locale/en-us.json';
/* eslint-disable import/no-named-default */
import { LAUNCHER_URL } from '../../../../src/utils/restCallUtils.js';

const MOCK_LOCALE_RESPONSE = {
  json: () => LOCALE_EN_US,
  ok: true,
  status: 200,
};

let fetchStub;

describe('Given: An <e-access-denied-error>', () => {
  before(() => {
    AccessDeniedError.register();
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
    it(`Then: It should render a dialog box with title ${LOCALE_EN_US.ACCESS_DENIED_TITLE} and text ${LOCALE_EN_US.ROLE_DOES_NOT_ALLOW_ACCESS}`, async () => {
      const component = await fixture(
        '<e-access-denied-error></e-access-denied-error>',
      );
      const dialog = component.shadowRoot.querySelector('eui-dialog');
      expect(dialog.label).to.eq(LOCALE_EN_US.ACCESS_DENIED_TITLE);

      const content = dialog.querySelector('div[slot=content]');
      const text = content.querySelector('p').textContent;
      expect(text).to.contain(LOCALE_EN_US.ROLE_DOES_NOT_ALLOW_ACCESS);
      expect(text).to.contain(LOCALE_EN_US.CONTACT_SYSTEM_ADMIN);

      const button = dialog.querySelector('eui-button');
      expect(button.textContent).to.eq(LOCALE_EN_US.RETURN_TO_PORTAL);
      expect(button.getAttribute('href')).to.eq(LAUNCHER_URL);
    });
  });
});
