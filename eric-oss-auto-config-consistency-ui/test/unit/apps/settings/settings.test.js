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

/**
 * Unit tests for <e-settings>
 */
import { expect, fixture, nextFrame } from '@open-wc/testing';
import sinon from 'sinon';
import Settings from '../../../../src/apps/settings/settings.js';

/* eslint-disable import/no-named-default */
import { default as LOCALE_EN_US } from '../../../../src/apps/settings/locale/en-us.json';
import { default as INIT_ERROR } from '../../../../src/components/initialization-error/locale/en-us.json';
import { simulateEvent } from '../../../resources/elementUtils.js';
/* eslint-disable import/no-named-default */

const COMBINED_LOCALE = { ...LOCALE_EN_US, ...INIT_ERROR };

const MOCK_LOCALE_RESPONSE = {
  json: () => COMBINED_LOCALE,
  ok: true,
  status: 200,
};

let fetchStub;

describe('Given: An <e-access-denied-error>', () => {
  before(() => {
    Settings.register('e-settings');
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

  describe('Given: An <e-settings> app is created', () => {
    it(`Then: The UI displays two tabs with ${LOCALE_EN_US.RULESETS} and ${LOCALE_EN_US.SCOPE}`, async () => {
      const settingsApp = await fixture('<e-settings></e-settings>');
      const tabs = settingsApp.shadowRoot.querySelector('eui-tabs');
      const rulesetTab = tabs.querySelector('eui-tab:nth-of-type(1) > label');
      const scopeTab = tabs.querySelector('eui-tab:nth-of-type(2) > label');

      expect(rulesetTab.textContent).to.eq(LOCALE_EN_US.RULESETS);
      expect(scopeTab.textContent).to.eq(LOCALE_EN_US.SCOPE);
    });

    describe(`When: An 'e-ruleset:access-denied' is bubbled`, () => {
      it('Then: The UI displays the access denied error component', async () => {
        const settingsApp = await fixture('<e-settings></e-settings>');
        const ruleset = settingsApp.shadowRoot.querySelector('e-ruleset');

        simulateEvent(ruleset, 'e-ruleset:access-denied');

        await nextFrame();
        const accessDeniedError = settingsApp.shadowRoot.querySelector(
          'e-access-denied-error',
        );
        expect(accessDeniedError).to.exist;
      });
    });

    describe(`When: An 'e-scope:access-denied' is bubbled`, () => {
      it('Then: The UI displays the access denied error component', async () => {
        const settingsApp = await fixture('<e-settings></e-settings>');
        const scope = settingsApp.shadowRoot.querySelector('e-scope');

        simulateEvent(scope, 'e-scope:access-denied');

        await nextFrame();
        const accessDeniedError = settingsApp.shadowRoot.querySelector(
          'e-access-denied-error',
        );
        expect(accessDeniedError).to.exist;
      });
    });
  });
});
