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

import { expect, fixture, html } from '@open-wc/testing';
import { nextFrame } from '@open-wc/testing-helpers';
import sinon from 'sinon';

import AuditReportTab from '../../../../src/components/audit-report-tab/audit-report-tab.js';
/* eslint-disable import/no-named-default */
import { default as LOCALE_EN_US } from '../../../../src/components/audit-report-tab/locale/en-us.json';
/* eslint-disable import/no-named-default */

import {
  triggerPostChangesNotification,
  resetNotifications,
  simulateEvent,
  simulateDetailEvent,
  simulateEventWithText,
} from '../../../resources/elementUtils.js';
import {
  MOCK_ACCESS_DENIED_RESPONSE,
  MOCK_BAD_REQUEST_RESPONSE,
  MOCK_NOT_FOUND_RESPONSE,
  MOCK_INTERNAL_SERVER_ERROR_RESPONSE,
} from '../../../resources/mockResponses.js';

const MOCK_LOCALE_RESPONSE = {
  json: () => LOCALE_EN_US,
  ok: true,
  status: 200,
};

const MOCK_OK_RESPONSE = {
  ok: true,
  status: 200,
};

const POST_EXECUTIONS_API = '/v1/executions/3/audit-results';

let fetchStub;

describe('Given: An <e-audit-report-tab>', () => {
  before(() => {
    AuditReportTab.register('e-audit-report-tab');
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

  describe('When: Audit report tab created and Rest Paginated Table issues Events', () => {
    it(`Then: The ${LOCALE_EN_US.AUDIT_REPORT} tab is displayed and the corresponding Audit Results Count and Selected are displayed correctly`, async () => {
      const auditReportTab = await fixture(
        html`<e-audit-report-tab .executionId=${'3'}></e-audit-report-tab>`,
      );

      const multiPanelTile = auditReportTab.shadowRoot.querySelector(
        'eui-multi-panel-tile',
      );
      expect(multiPanelTile).to.have.attr('tile-title', LOCALE_EN_US.RESULTS);

      simulateDetailEvent(
        multiPanelTile,
        'e-rest-paginated-table:audit:total-elements',
        '10',
      );
      await nextFrame();
      const rows = [{ executionId: '3', id: '1' }];
      simulateDetailEvent(
        multiPanelTile,
        'e-rest-paginated-table:audit:row-selected',
        rows,
      );
      await nextFrame();

      const rowCountText = multiPanelTile.querySelector('.row-count-text');
      expect(rowCountText.textContent).to.contain(`10 ${LOCALE_EN_US.ITEMS}`);

      expect(multiPanelTile).to.have.attr(
        'subtitle',
        `${LOCALE_EN_US.SELECTED} (1)`,
      );
    });
  });

  describe(`When: table row is selected and unselected in ${LOCALE_EN_US.RESULTS}`, () => {
    it(`Then: "${LOCALE_EN_US.APPLY_SELECTED} button" and ${LOCALE_EN_US.APPLY_ALL} buttons are enabled and disabled`, async () => {
      const auditReportTab = await fixture(
        html`<e-audit-report-tab .executionId=${'3'}></e-audit-report-tab>`,
      );

      const multiPanelTile = auditReportTab.shadowRoot.querySelector(
        'eui-multi-panel-tile',
      );

      // Default ApplyAll is enabled and ApplySelected is disabled
      const applySelectedButton = auditReportTab.shadowRoot.querySelector(
        'e-apply-button#applySelected',
      );
      const applyAllButton = auditReportTab.shadowRoot.querySelector(
        'e-apply-button#applyAll',
      );

      expect(applyAllButton).to.not.have.attr('hidden');
      expect(applyAllButton.textContent).to.contain(LOCALE_EN_US.APPLY_ALL);
      expect(applySelectedButton).to.have.attr('hidden');
      expect(applySelectedButton.textContent).to.contain(
        LOCALE_EN_US.APPLY_SELECTED,
      );

      // Simulate row been selected in the table and check ApplySelected is now enabled and ApplyAll is disabled
      const rowSelect = [{ executionId: '3', id: '1' }];
      simulateDetailEvent(
        multiPanelTile,
        'e-rest-paginated-table:audit:row-selected',
        rowSelect,
      );
      await nextFrame();

      expect(applySelectedButton).to.not.have.attr('hidden');
      expect(applyAllButton).to.have.attr('hidden');

      // Simulate row been unselected in the table and check ApplySelected is now disabled and ApplyAll is enabled
      const rowDeselect = [];
      simulateDetailEvent(
        multiPanelTile,
        'e-rest-paginated-table:audit:row-selected',
        rowDeselect,
      );
      await nextFrame();

      expect(applySelectedButton).to.have.attr('hidden');
      expect(applyAllButton).to.not.have.attr('hidden');
    });
  });

  describe(`When: A call is made to ${POST_EXECUTIONS_API} and 200 OK is returned`, () => {
    it(`Then: The UI displays a positive notification with the message "${LOCALE_EN_US.AUDIT_RESULTS_UPDATED}"`, async () => {
      const auditReportTab = await fixture(
        html`<e-audit-report-tab .executionId=${'3'}></e-audit-report-tab>`,
      );
      fetchStub
        .withArgs(sinon.match(POST_EXECUTIONS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_OK_RESPONSE);

      const multiPanelTile = auditReportTab.shadowRoot.querySelector(
        'eui-multi-panel-tile',
      );

      const rows = [{ executionId: '3', id: '1' }];
      simulateDetailEvent(
        multiPanelTile,
        'e-rest-paginated-table:audit:row-selected',
        rows,
      );
      await nextFrame();

      await triggerPostChangesNotification(auditReportTab);
      await nextFrame();

      const notification = document
        .querySelector('div#notifications-column')
        .querySelector('eui-notification');

      // Verify notification was created with expected message and type
      expect(notification.textContent.trim()).to.equal(
        `${LOCALE_EN_US.AUDIT_RESULTS_UPDATED}`,
      );
    });
  });

  describe(`When: A call is made to ${POST_EXECUTIONS_API} and 400 OK is returned`, () => {
    it(`Then: The UI displays a positive notification with the message "${LOCALE_EN_US.REST_RESPONSE_400_BAD_REQUEST}"`, async () => {
      const auditReportTab = await fixture(
        html`<e-audit-report-tab .executionId=${'3'}></e-audit-report-tab>`,
      );
      fetchStub
        .withArgs(sinon.match(POST_EXECUTIONS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_BAD_REQUEST_RESPONSE);

      const multiPanelTile = auditReportTab.shadowRoot.querySelector(
        'eui-multi-panel-tile',
      );

      const rows = [{ executionId: '3', id: '1' }];
      simulateDetailEvent(
        multiPanelTile,
        'e-rest-paginated-table:audit:row-selected',
        rows,
      );
      await nextFrame();

      await triggerPostChangesNotification(auditReportTab);
      await nextFrame();

      const notification = document
        .querySelector('div#notifications-column')
        .querySelector('eui-notification');

      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.UNABLE_TO_POST_AUDIT_RESULTS,
      );
      expect(notification.description).to.equal(
        LOCALE_EN_US.REST_RESPONSE_400_BAD_REQUEST,
      );
    });
  });

  describe(`When: A call is made to ${POST_EXECUTIONS_API} and 404 NOT FOUND is returned`, () => {
    it(`Then: The UI displays a negative notification with the message "${LOCALE_EN_US.REST_RESPONSE_404_NOT_FOUND}"`, async () => {
      const auditReportTab = await fixture(
        html`<e-audit-report-tab .executionId=${'3'}></e-audit-report-tab>`,
      );
      fetchStub
        .withArgs(sinon.match(POST_EXECUTIONS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_NOT_FOUND_RESPONSE);
      const multiPanelTile = auditReportTab.shadowRoot.querySelector(
        'eui-multi-panel-tile',
      );

      const rows = [{ executionId: '3', id: '1' }];
      simulateDetailEvent(
        multiPanelTile,
        'e-rest-paginated-table:audit:row-selected',
        rows,
      );
      await nextFrame();

      await triggerPostChangesNotification(auditReportTab);

      await nextFrame();
      const notification = document
        .querySelector('div#notifications-column')
        .querySelector('eui-notification');

      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.UNABLE_TO_POST_AUDIT_RESULTS,
      );
      expect(notification.description).to.equal(
        LOCALE_EN_US.REST_RESPONSE_404_NOT_FOUND,
      );
    });
  });

  describe(`When: A call is made to ${POST_EXECUTIONS_API} and 500 Internal Server Error is returned`, () => {
    it(`Then: The UI displays a negative notification with the message "${LOCALE_EN_US.AUDIT_RESULT_FAILURE_LONG_DESCRIPTION}"`, async () => {
      const auditReportTab = await fixture(
        html`<e-audit-report-tab .executionId=${'3'}></e-audit-report-tab>`,
      );

      fetchStub
        .withArgs(sinon.match(POST_EXECUTIONS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_INTERNAL_SERVER_ERROR_RESPONSE);

      const multiPanelTile = auditReportTab.shadowRoot.querySelector(
        'eui-multi-panel-tile',
      );

      const rows = [{ executionId: '3', id: '1' }];
      simulateDetailEvent(
        multiPanelTile,
        'e-rest-paginated-table:audit:row-selected',
        rows,
      );
      await nextFrame();

      await triggerPostChangesNotification(auditReportTab);

      await nextFrame();
      const notification = document
        .querySelector('div#notifications-column')
        .querySelector('eui-notification');

      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.UNABLE_TO_POST_AUDIT_RESULTS,
      );
      expect(notification.description).to.equal(
        LOCALE_EN_US.AUDIT_RESULT_FAILURE_LONG_DESCRIPTION,
      );
    });
  });

  describe(`When: filter pill is selected`, () => {
    it(`Then: it has value unselected and ${LOCALE_EN_US.RESULTS} shows all results`, async () => {
      const auditReportTab = await fixture(
        html`<e-audit-report-tab .executionId=${'3'}></e-audit-report-tab>`,
      );

      const multiPanelTile = auditReportTab.shadowRoot.querySelector(
        'eui-multi-panel-tile',
      );

      const filterPill = auditReportTab.shadowRoot.querySelector('#filterPill');

      simulateDetailEvent(
        multiPanelTile,
        'e-rest-paginated-table:audit:total-elements',
        '1',
      );
      await nextFrame();

      const rowCountText = multiPanelTile.querySelector('.row-count-text');
      expect(rowCountText.textContent).to.contain(`1 ${LOCALE_EN_US.ITEM}`);

      expect(filterPill.hasAttribute('unselected')).is.false;

      filterPill.click();
      await nextFrame();

      expect(filterPill.hasAttribute('unselected')).is.true;

      simulateDetailEvent(
        multiPanelTile,
        'e-rest-paginated-table:audit:total-elements',
        '2',
      );
      await nextFrame();
      expect(rowCountText.textContent).to.contain(`2 ${LOCALE_EN_US.ITEMS}`);
    });
  });

  describe(`When: A call is made to ${POST_EXECUTIONS_API} and 403 is returned`, () => {
    it(`Then: The UI displays a negative notification with the message "${LOCALE_EN_US.REST_403_DESCRIPTION}"`, async () => {
      const auditReportTab = await fixture(
        html`<e-audit-report-tab .executionId=${'3'}></e-audit-report-tab>`,
      );
      fetchStub
        .withArgs(sinon.match(POST_EXECUTIONS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_ACCESS_DENIED_RESPONSE);
      const multiPanelTile = auditReportTab.shadowRoot.querySelector(
        'eui-multi-panel-tile',
      );

      const rows = [{ executionId: '3', id: '1' }];
      simulateDetailEvent(
        multiPanelTile,
        'e-rest-paginated-table:audit:row-selected',
        rows,
      );
      await nextFrame();

      await triggerPostChangesNotification(auditReportTab);

      await nextFrame();
      const notification = document
        .querySelector('div#notifications-column')
        .querySelector('eui-notification');

      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.ACCESS_DENIED,
      );

      expect(notification.description).to.equal(
        LOCALE_EN_US.REST_403_DESCRIPTION,
      );
    });
  });
  describe(`When: The panel icon is clicked`, () => {
    it('Then: The filter panel opens and contains a text field', async () => {
      const auditReportTab = await fixture(
        html`<e-audit-report-tab .executionId=${'3'}></e-audit-report-tab>`,
      );

      const multiPanelTile = auditReportTab.shadowRoot.querySelector(
        'eui-multi-panel-tile',
      );

      const filterPanelTile = multiPanelTile.querySelector('eui-tile-panel');

      expect(filterPanelTile).to.have.attr('hidden');

      const filterPanelButton =
        multiPanelTile.querySelector('eui-panel-button');

      simulateEvent(filterPanelButton, 'click');

      await nextFrame();

      expect(filterPanelTile).to.not.have.attr('hidden');
      const managedObjectText =
        filterPanelTile.querySelectorAll('.filter-label')[0];
      expect(managedObjectText.textContent).to.eq(LOCALE_EN_US.MANAGED_OBJECT);
    });
  });
  describe(`When: The Managed Object text field is typed and the apply filter button is clicked`, () => {
    it(`Then: Internal props are set in audit report tab and table is filtered by Managed Object`, async () => {
      const auditReportTab = await fixture(
        html`<e-audit-report-tab .executionId=${'3'}></e-audit-report-tab>`,
      );

      const multiPanelTile = auditReportTab.shadowRoot.querySelector(
        'eui-multi-panel-tile',
      );
      const filterPanelTile = multiPanelTile.querySelector('eui-tile-panel');

      const filterContent = filterPanelTile.querySelector('.filter-content');
      const mangedObjectTextField = filterContent.querySelector(
        'eui-text-field#managed-object-text-field',
      );
      const managedObjectTextInput = 'NR03gNodeBRadio00030-4';
      simulateEventWithText(
        mangedObjectTextField,
        'input',
        managedObjectTextInput,
      );
      await nextFrame();

      const applyFilterButton = filterContent.querySelector(
        '#apply-filters-button',
      );

      simulateEvent(applyFilterButton, 'click');

      await nextFrame();

      expect(auditReportTab.filter).to.eq(
        `filter=auditStatus:Inconsistent%24managedObjectFdn:%25${managedObjectTextInput}%25`,
      );
    });
  });
  describe(`When: Filter is applied and Clear filter button is clicked`, () => {
    it(`Then: The filter fields are reset and the filter is cleared`, async () => {
      const auditReportTab = await fixture(
        html`<e-audit-report-tab .executionId=${'3'}></e-audit-report-tab>`,
      );

      const multiPanelTile = auditReportTab.shadowRoot.querySelector(
        'eui-multi-panel-tile',
      );
      const filterPanelTile = multiPanelTile.querySelector('eui-tile-panel');

      const filterContent = filterPanelTile.querySelector('.filter-content');
      const mangedObjectTextField = filterContent.querySelector(
        'eui-text-field#managed-object-text-field',
      );
      const managedObjectTextInput = 'NR03gNodeBRadio00030-4';
      simulateEventWithText(
        mangedObjectTextField,
        'input',
        managedObjectTextInput,
      );

      await nextFrame();

      const applyFilterButton = filterContent.querySelector(
        '#apply-filters-button',
      );

      simulateEvent(applyFilterButton, 'click');

      await nextFrame();

      const resetFilterBtton = filterContent.querySelector(
        '#reset-filters-button',
      );

      simulateEvent(resetFilterBtton, 'click');

      const filterInputs = filterContent.querySelectorAll('.filter-input');
      filterInputs.forEach(element => {
        expect(element.value).to.be.empty;
      });

      expect(auditReportTab.props.fdnToFilterBy).to.be.empty;
    });
  });

  describe(`When: A filter is applied and Clear button is clicked`, () => {
    it(`Then: The filter is cleared`, async () => {
      const auditReportTab = await fixture(
        html`<e-audit-report-tab .executionId=${'3'}></e-audit-report-tab>`,
      );

      const multiPanelTile = auditReportTab.shadowRoot.querySelector(
        'eui-multi-panel-tile',
      );
      const filterPanelTile = multiPanelTile.querySelector('eui-tile-panel');

      const filterContent = filterPanelTile.querySelector('.filter-content');
      const mangedObjectTextField = filterContent.querySelector(
        'eui-text-field#managed-object-text-field',
      );
      const managedObjectTextInput = 'NR03gNodeBRadio00030-4';
      simulateEventWithText(
        mangedObjectTextField,
        'input',
        managedObjectTextInput,
      );

      await nextFrame();

      const applyFilterButton = filterContent.querySelector(
        '#apply-filters-button',
      );

      simulateEvent(applyFilterButton, 'click');

      await nextFrame();

      const clearLinkSpan =
        auditReportTab.shadowRoot.querySelector('.filters-applied');

      expect(clearLinkSpan).to.not.have.attr('hidden');
      const clearLink =
        auditReportTab.shadowRoot.querySelector('.clear-filter');

      simulateEvent(clearLink, 'click');

      await nextFrame();

      expect(clearLinkSpan).to.have.attr('hidden');
      expect(auditReportTab.isProposedChanges).to.be.false;
      expect(auditReportTab.fdnToFilterBy).to.be.empty;
      expect(auditReportTab.filter).to.be.empty;
    });
  });
  describe(`When: An invalid input is typed into the managedObject search box`, () => {
    it(`Then: The apply filter button is disabled`, async () => {
      const auditReportTab = await fixture(
        html`<e-audit-report-tab .executionId=${'3'}></e-audit-report-tab>`,
      );

      const multiPanelTile = auditReportTab.shadowRoot.querySelector(
        'eui-multi-panel-tile',
      );
      const filterPanelTile = multiPanelTile.querySelector('eui-tile-panel');

      const filterContent = filterPanelTile.querySelector('.filter-content');
      const mangedObjectTextField = filterContent.querySelector(
        'eui-text-field#managed-object-text-field',
      );
      const managedObjectTextInput = '^^^^^^';
      simulateEventWithText(
        mangedObjectTextField,
        'input',
        managedObjectTextInput,
      );

      await nextFrame();

      const applyFilterButton = filterContent.querySelector(
        '#apply-filters-button',
      );

      expect(applyFilterButton).to.have.attr('disabled');
    });
  });
});
