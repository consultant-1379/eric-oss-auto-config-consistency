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

import ApplyButton from '../../../../src/components/apply-button/apply-button.js';

/* eslint-disable import/no-named-default */
import { default as LOCALE_EN_US } from '../../../../src/components/apply-button/locale/en-us.json';
/* eslint-disable import/no-named-default */
import { ChangeStatus } from '../../../../src/utils/attributes/changeAttributes.js';

import {
  MOCK_AUDIT_RESULTS_IN_PROGRESS_CHANGES_DATA_ARRAY,
  MOCK_AUDIT_RESULTS_ZERO_IN_PROGRESS_CHANGES_DATA_ARRAY,
} from '../../../resources/mockData.js';

import { MOCK_NOT_FOUND_RESPONSE } from '../../../resources/mockResponses.js';

const MOCK_LOCALE_RESPONSE = {
  json: () => LOCALE_EN_US,
  ok: true,
  status: 200,
};

const MOCK_POPULATED_AUDITS_RESPONSE_CONTAINS_IN_PROGRESS = {
  json: MOCK_AUDIT_RESULTS_IN_PROGRESS_CHANGES_DATA_ARRAY,
  ok: true,
  status: 200,
};

const MOCK_POPULATED_AUDITS_RESPONSE_CONTAINS_ZERO_IN_PROGRESS = {
  json: MOCK_AUDIT_RESULTS_ZERO_IN_PROGRESS_CHANGES_DATA_ARRAY,
  ok: true,
  status: 200,
};

const EXECUTION_ID = 3;
const POLLING_ID = 'apply-all-button';
const AUDIT_RESULTS_API = `/v1/executions/${EXECUTION_ID}/audit-results?filter=changeStatus:(${ChangeStatus.IMPLEMENTATION_IN_PROGRESS},${ChangeStatus.REVERSION_IN_PROGRESS})&page=0&pageSize=1`;
const BUTTON_NAME = 'Apply all';

let fetchStub;

/**
 * Unit tests for <e-revert-all-button>
 */
describe('Given: An <e-apply-button>', () => {
  before(() => {
    ApplyButton.register();
    fetchStub = sinon.stub(window, 'fetch');
  });

  after(() => {
    sinon.restore();
  });

  beforeEach(() => {
    fetchStub.callsFake(url => {
      if (url.indexOf('locale') !== -1) {
        return Promise.resolve(MOCK_LOCALE_RESPONSE);
      }
      return Promise.resolve(true);
    });
  });

  afterEach(() => {
    fetchStub.reset();
  });

  describe(`When: A REST call is made to ${AUDIT_RESULTS_API} with any 'In progress' changes`, () => {
    it(`Then: The <e-apply-button> is disabled`, async () => {
      fetchStub
        .withArgs(sinon.match(AUDIT_RESULTS_API))
        .resolves(MOCK_POPULATED_AUDITS_RESPONSE_CONTAINS_IN_PROGRESS);

      const component = await fixture(
        html`<e-apply-button
          execution-id=${EXECUTION_ID}
          polling-id=${POLLING_ID}
          >${BUTTON_NAME}</e-apply-button
        >`,
      );

      expect(component.hasAttribute('disabled')).to.be.true;
    });
  });

  describe(`When: A REST call is made to ${AUDIT_RESULTS_API} with zero 'In progress' changes`, () => {
    it(`Then: The <e-revert-all-button> is enabled`, async () => {
      fetchStub
        .withArgs(sinon.match(AUDIT_RESULTS_API))
        .resolves(MOCK_POPULATED_AUDITS_RESPONSE_CONTAINS_ZERO_IN_PROGRESS);

      const component = await fixture(
        html`<e-apply-button
          execution-id=${EXECUTION_ID}
          polling-id=${POLLING_ID}
          >${BUTTON_NAME}</e-apply-button
        >`,
      );

      expect(component.hasAttribute('disabled')).to.be.false;

      component.didDisconnect();

      expect(component.hasAttribute('disabled')).to.be.true;
    });
  });

  describe(`When: A REST call is made to ${AUDIT_RESULTS_API} and a server error occurs`, () => {
    it(`Then: The <e-apply-button> is disabled`, async () => {
      fetchStub
        .withArgs(sinon.match(AUDIT_RESULTS_API))
        .resolves(MOCK_NOT_FOUND_RESPONSE);

      const component = await fixture(
        html`<e-apply-button
          execution-id=${EXECUTION_ID}
          polling-id=${POLLING_ID}
          >${BUTTON_NAME}</e-apply-button
        >`,
      );

      expect(component.hasAttribute('disabled')).to.be.true;
    });
  });
});
