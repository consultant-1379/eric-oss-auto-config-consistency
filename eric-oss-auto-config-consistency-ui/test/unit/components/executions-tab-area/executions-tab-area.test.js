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
 * Unit tests for <e-executions-tab-area>
 */
import { expect, fixture, html } from '@open-wc/testing';
import { nextFrame } from '@open-wc/testing-helpers';
import sinon from 'sinon';

import ExecutionsTabArea from '../../../../src/components/executions-tab-area/executions-tab-area.js';
/* eslint-disable import/no-named-default */
import { default as LOCALE_EN_US } from '../../../../src/components/executions-tab-area/locale/en-us.json';
/* eslint-disable import/no-named-default */

import { ChangeStatus } from '../../../../src/utils/attributes/changeAttributes.js';

import { MOCK_AUDIT_RESULTS_ZERO_IN_PROGRESS_CHANGES_DATA_ARRAY } from '../../../resources/mockData.js';

import {
  resetNotifications,
  simulateEvent,
  simulateDetailEvent,
} from '../../../resources/elementUtils.js';

const MOCK_LOCALE_RESPONSE = {
  json: () => LOCALE_EN_US,
  ok: true,
  status: 200,
};

const MOCK_POPULATED_AUDITS_RESPONSE_CONTAINS_ZERO_IN_PROGRESS = {
  json: MOCK_AUDIT_RESULTS_ZERO_IN_PROGRESS_CHANGES_DATA_ARRAY,
  ok: true,
  status: 200,
};

const EXECUTION_ID = 3;
const FILTERED_AUDIT_RESULTS_API = `/v1/executions/${EXECUTION_ID}/audit-results?filter=changeStatus:(${ChangeStatus.IMPLEMENTATION_IN_PROGRESS},${ChangeStatus.REVERSION_IN_PROGRESS})&page=0&pageSize=1`;

let fetchStub;

describe('Given: An <e-executions-tab-area>', () => {
  before(() => {
    ExecutionsTabArea.register('e-executions-tab-area');
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
    resetNotifications();
  });

  describe('When: Tab Area created and Rest Paginated Table issues Events', () => {
    it(`Then: The ${LOCALE_EN_US.AUDIT_REPORT} tab is displayed with an <e-audit-report-tab> component`, async () => {
      const executionsTabArea = await fixture(
        html`<e-executions-tab-area
          .executionId=${EXECUTION_ID}
        ></e-executions-tab-area>`,
      );

      const tabs = executionsTabArea.shadowRoot.querySelector('eui-tabs');
      const auditTab = tabs
        .querySelectorAll('eui-tab')[0]
        .querySelector('label');

      expect(auditTab.textContent).to.equal(LOCALE_EN_US.AUDIT_REPORT);

      const auditReportTabComponent = tabs.querySelector('e-audit-report-tab');

      expect(auditReportTabComponent).to.exist;
    });
  });

  describe(`When: An array of auditResults are provided`, () => {
    it(`Then: The ${LOCALE_EN_US.CHANGES} tab is displayed and the corresponding table is created`, async () => {
      const executionsTabArea = await fixture(
        html`<e-executions-tab-area
          .executionId=${EXECUTION_ID}
        ></e-executions-tab-area>`,
      );

      const tabs = executionsTabArea.shadowRoot.querySelector('eui-tabs');
      const changesTab = tabs.querySelector('#changes-tab');
      expect(changesTab.querySelector('label').textContent).to.contain(
        `${LOCALE_EN_US.CHANGES}`,
      );
    });
  });

  describe('When: Tab Area created and Rest Paginated Table issues Events', () => {
    it(`Then: The ${LOCALE_EN_US.CHANGES} tab is displayed and the corresponding Changes Count is displayed correctly`, async () => {
      const executionsTabArea = await fixture(
        html`<e-executions-tab-area
          .executionId=${EXECUTION_ID}
        ></e-executions-tab-area>`,
      );

      const tabs = executionsTabArea.shadowRoot.querySelector('eui-tabs');
      tabs.querySelectorAll('eui-tab')[1].click();
      await nextFrame();

      const changesTab = tabs
        .querySelectorAll('eui-tab')[1]
        .querySelector('label');

      expect(changesTab.textContent).to.equal(LOCALE_EN_US.CHANGES);

      const changesPoll = tabs.querySelector('e-changes-poll');

      simulateDetailEvent(
        changesPoll,
        'e-rest-paginated-table:changes:total-elements',
        '5',
      );
      await nextFrame();
      const changesTabHeader = tabs
        .querySelector('div[selected="true"]')
        .querySelector('eui-tile')
        .shadowRoot.querySelector('.tile__header__left__title');

      expect(changesTabHeader.textContent).to.contain(LOCALE_EN_US.CHANGES);
    });
  });

  describe('When: A selected-count event is received with count 4', () => {
    it(`Then: The number of selected items is updated and Clear button is present`, async () => {
      const executionsTabArea = await fixture(
        html`<e-executions-tab-area
          .executionId=${EXECUTION_ID}
        ></e-executions-tab-area>`,
      );

      const tabs = executionsTabArea.shadowRoot.querySelector('eui-tabs');

      const changesTab = tabs.querySelectorAll('eui-tab')[1];
      simulateEvent(changesTab, 'click');
      await nextFrame();

      const changesPoll = tabs.querySelector('e-changes-poll');

      simulateDetailEvent(
        changesPoll,
        'e-rest-paginated-table:changes:total-elements',
        5,
      );
      simulateDetailEvent(changesPoll, 'e-changes-poll:selected-count', 4);

      await nextFrame();

      const appChangeSelectionText = tabs
        .querySelector('div[selected=true]')
        .querySelector('.changes-selection');

      // The render will add new lines and padding to the element, using a regex to match the expected text.
      const expectedSelectedText = /\n\s+5\n\s+items\n\s+\|\s+4\n\s+selected/m;
      expect(appChangeSelectionText.textContent).to.match(expectedSelectedText);

      const selectedRows =
        appChangeSelectionText.querySelector('#selectedRows');
      expect(selectedRows.hasAttribute('hidden')).to.be.false;

      const changesPollSpy = sinon.spy(changesPoll, 'clearSelection');
      sinon.assert.notCalled(changesPollSpy);

      const clearSelection =
        appChangeSelectionText.querySelector('.clearSelection');
      simulateEvent(clearSelection, 'click');

      sinon.assert.calledOnce(changesPollSpy);
    });
  });

  describe('When: A selected-count event is received with count 0', () => {
    it(`Then: No selected items or Clear button is present`, async () => {
      const executionsTabArea = await fixture(
        html`<e-executions-tab-area
          .executionId=${EXECUTION_ID}
        ></e-executions-tab-area>`,
      );

      const tabs = executionsTabArea.shadowRoot.querySelector('eui-tabs');

      const changesTab = tabs.querySelectorAll('eui-tab')[1];
      simulateEvent(changesTab, 'click');
      await nextFrame();

      const changesPoll = tabs.querySelector('e-changes-poll');

      simulateDetailEvent(
        changesPoll,
        'e-rest-paginated-table:changes:total-elements',
        1,
      );
      simulateDetailEvent(changesPoll, 'e-changes-poll:selected-count', 0);

      await nextFrame();

      const appChangeSelectionText = tabs
        .querySelector('div[selected=true]')
        .querySelector('.changes-selection');

      // The render will add new lines and padding to the element, using a regex to match the expected text.
      const expectedSelectedText = /\n\s+1\n\s+item/m;
      expect(appChangeSelectionText.textContent).to.match(expectedSelectedText);

      const selectedRows =
        appChangeSelectionText.querySelector('#selectedRows');
      expect(selectedRows.hasAttribute('hidden')).to.be.true;
    });
  });

  describe('When: The number of selected rows is less than the number of changes', () => {
    it(`Then: The "${LOCALE_EN_US.REVERT_SELECTED}" button is visible and can call a revert`, async () => {
      const executionsTabArea = await fixture(
        html`<e-executions-tab-area
          .executionId=${EXECUTION_ID}
        ></e-executions-tab-area>`,
      );

      const tabs = executionsTabArea.shadowRoot.querySelector('eui-tabs');

      const changesTab = tabs.querySelectorAll('eui-tab')[1];
      simulateEvent(changesTab, 'click');
      await nextFrame();

      const changesPoll = tabs.querySelector('e-changes-poll');
      simulateDetailEvent(
        changesPoll,
        'e-rest-paginated-table:changes:total-elements',
        5,
      );
      simulateDetailEvent(changesPoll, 'e-changes-poll:selected-count', 0);
      await nextFrame();

      const actionButtons = tabs
        .querySelector('div[selected=true]')
        .querySelector('.tab-header-changes')
        .querySelector('eui-tile')
        .querySelector('#action-buttons');

      const revertAll = actionButtons.querySelector('#revertAll');
      const revertSelected = actionButtons.querySelector('#revertSelected');

      expect(revertAll.hasAttribute('hidden')).to.be.false;
      expect(revertAll.textContent).to.contain(LOCALE_EN_US.REVERT_ALL);
      expect(revertSelected.hasAttribute('hidden')).to.be.true;

      simulateDetailEvent(
        changesPoll,
        'e-rest-paginated-table:changes:total-elements',
        5,
      );
      simulateDetailEvent(changesPoll, 'e-changes-poll:selected-count', 4);

      await nextFrame();

      expect(revertAll.hasAttribute('hidden')).to.be.true;
      expect(revertSelected.hasAttribute('hidden')).to.be.false;
      expect(revertSelected.textContent).to.contain(
        LOCALE_EN_US.REVERT_SELECTED,
      );

      const changesPollSpy = sinon.spy(changesPoll, 'revertChanges');
      sinon.assert.notCalled(changesPollSpy);

      simulateEvent(revertSelected, 'click');

      sinon.assert.calledOnce(changesPollSpy);
    });
  });

  describe('When: The number of selected rows is zero', () => {
    it(`Then: The "${LOCALE_EN_US.REVERT_ALL}" button is visible and can call revert`, async () => {
      fetchStub
        .withArgs(sinon.match(FILTERED_AUDIT_RESULTS_API))
        .resolves(MOCK_POPULATED_AUDITS_RESPONSE_CONTAINS_ZERO_IN_PROGRESS);

      const executionsTabArea = await fixture(
        html`<e-executions-tab-area
          .executionId=${EXECUTION_ID}
        ></e-executions-tab-area>`,
      );

      const tabs = executionsTabArea.shadowRoot.querySelector('eui-tabs');

      const changesTab = tabs.querySelectorAll('eui-tab')[1];
      simulateEvent(changesTab, 'click');
      await nextFrame();

      const changesPoll = tabs.querySelector('e-changes-poll');
      simulateDetailEvent(
        changesPoll,
        'e-rest-paginated-table:changes:total-elements',
        5,
      );
      simulateDetailEvent(changesPoll, 'e-changes-poll:selected-count', 0);
      await nextFrame();

      const actionButtons = tabs
        .querySelector('div[selected=true]')
        .querySelector('.tab-header-changes')
        .querySelector('eui-tile')
        .querySelector('#action-buttons');

      const revertAll = actionButtons.querySelector('#revertAll');

      expect(revertAll.hasAttribute('hidden')).to.be.false;
      // expect(revertAll.hasAttribute('disabled')).to.be.false;

      const changesPollSpy = sinon.spy(changesPoll, 'revertChanges');
      sinon.assert.notCalled(changesPollSpy);

      simulateEvent(revertAll, 'click');

      sinon.assert.calledOnce(changesPollSpy);
    });
  });
});
