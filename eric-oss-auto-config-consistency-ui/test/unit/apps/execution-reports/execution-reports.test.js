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
 * Unit tests for <e-execution-reports>
 */
import { expect, fixture } from '@open-wc/testing';
import { aTimeout, fixtureWrapper, nextFrame } from '@open-wc/testing-helpers';
import sinon from 'sinon';

import ExecutionReports from '../../../../src/apps/execution-reports/execution-reports.js';
import { simulateEvent } from '../../../resources/elementUtils.js';

/* eslint-disable import/no-named-default */
import { default as LOCALE_EN_US } from '../../../../src/apps/execution-reports/locale/en-us.json';
import { default as INIT_ERROR } from '../../../../src/components/initialization-error/locale/en-us.json';
import { default as EXEC_TAB_AREA } from '../../../../src/components/executions-tab-area/locale/en-us.json';
/* eslint-disable import/no-named-default */

const COMBINED_LOCALE = { ...LOCALE_EN_US, ...INIT_ERROR, ...EXEC_TAB_AREA };

const MOCK_LOCALE_RESPONSE = {
  json: () => COMBINED_LOCALE,
  ok: true,
  status: 200,
};

const JOB_NAME = 'athlone-01';

let fetchStub;

describe('Given: An <e-execution-reports>', () => {
  before(() => {
    ExecutionReports.register('e-execution-reports');
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

  describe(`When: The bubble('app:breadcrumb') is called`, async () => {
    it(`Then: The UI displays ${LOCALE_EN_US.AUTOMATED_CONFIGURATION_CONSISTENCY} > ${LOCALE_EN_US.EXECUTIONS_OVERVIEW} > ${JOB_NAME}`, async () => {
      const executionReports = new ExecutionReports();
      executionReports.jobName = JOB_NAME;
      await nextFrame();

      const bubbleSpy = sinon.spy(ExecutionReports.prototype, 'bubble');
      const wrapper = fixtureWrapper();

      wrapper.appendChild(executionReports);
      await nextFrame();
      await aTimeout(200);

      const { breadcrumb } = bubbleSpy.getCall(0).args[1];

      expect(breadcrumb.length).to.equal(3);
      expect(breadcrumb[0].displayName).to.equal(
        LOCALE_EN_US.AUTOMATED_CONFIGURATION_CONSISTENCY,
      );
      expect(breadcrumb[1].displayName).to.equal(
        LOCALE_EN_US.EXECUTIONS_OVERVIEW,
      );
      expect(breadcrumb[2].displayName).to.equal(JOB_NAME);
    });
  });

  describe(`When: An 'e-executions-tab-area:access-denied' is bubbled`, () => {
    it('Then: The UI displays the access denied error component', async () => {
      const executionReports = await fixture(
        '<e-execution-reports></e-execution-reports>',
      );
      const tabArea = executionReports.shadowRoot.querySelector(
        'e-executions-tab-area',
      );

      simulateEvent(tabArea, 'e-executions-tab-area:access-denied');

      await nextFrame();
      const accessDeniedError = executionReports.shadowRoot.querySelector(
        'e-access-denied-error',
      );
      expect(accessDeniedError).to.exist;
    });
  });
});
