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

import { expect, fixture, html, nextFrame } from '@open-wc/testing';
import sinon from 'sinon';
import ChangesPoll from '../../../../src/components/changes-poll/changes-poll.js';
/* eslint-disable import/no-named-default */
import { default as LOCALE_EN_US } from '../../../../src/components/changes-poll/locale/en-us.json';
import { default as INIT_ERROR } from '../../../../src/components/initialization-error/locale/en-us.json';
/* eslint-disable import/no-named-default */
import {
  Operation,
  ApproveForAll,
  ChangeStatus,
} from '../../../../src/utils/attributes/changeAttributes.js';

import {
  MOCK_AUDIT_RESULTS_FOR_CHANGES_DATA_ARRAY,
  MOCK_AUDIT_RESULTS_ZERO_IN_PROGRESS_CHANGES_DATA_ARRAY,
  MOCK_AUDIT_RESULTS_ALL_REVERTED_CHANGES_DATA_ARRAY,
} from '../../../resources/mockData.js';

import {
  MOCK_ACCESS_DENIED_RESPONSE,
  MOCK_INTERNAL_SERVER_ERROR_RESPONSE,
  MOCK_NOT_FOUND_RESPONSE,
  MOCK_IN_PROGRESS_RESPONSE,
  MOCK_LATEST_EXECUTION,
  MOCK_INVALID_FORMAT_EXECUTION,
} from '../../../resources/mockResponses.js';

import {
  resetNotifications,
  simulateDetailEvent,
  simulateEvent,
  triggerRevertChangesNotification,
} from '../../../resources/elementUtils.js';

const EXECUTION_ID = 3;
const EXECUTIONS_AUDITS_API = `/v1/executions/${EXECUTION_ID}/audit-results`;
const EXECUTIONS_AUDITS_API_FOR_CHANGES = `${EXECUTIONS_AUDITS_API}?page=0&pageSize=2000&filter=changeStatus:(${ChangeStatus.IMPLEMENTATION_IN_PROGRESS},${ChangeStatus.IMPLEMENTATION_COMPLETE},${ChangeStatus.IMPLEMENTATION_FAILED},${ChangeStatus.IMPLEMENTATION_ABORTED},${ChangeStatus.REVERSION_IN_PROGRESS},${ChangeStatus.REVERSION_COMPLETE},${ChangeStatus.REVERSION_FAILED},${ChangeStatus.REVERSION_ABORTED})`;
const EXECUTIONS_AUDITS_API_FOR_NON_REVERTABLE_CHANGES = `${EXECUTIONS_AUDITS_API}?filter=changeStatus:(${ChangeStatus.IMPLEMENTATION_IN_PROGRESS},${ChangeStatus.REVERSION_IN_PROGRESS},${ChangeStatus.REVERSION_COMPLETE})&page=0&pageSize=1`;

const COMBINED_LOCALE = { ...LOCALE_EN_US, ...INIT_ERROR };

const MOCK_LOCALE_RESPONSE = {
  json: () => COMBINED_LOCALE,
  ok: true,
  status: 200,
};

const MOCK_POPULATED_AUDITS_RESPONSE = {
  json: MOCK_AUDIT_RESULTS_FOR_CHANGES_DATA_ARRAY,
  ok: true,
  status: 200,
};

const MOCK_POPULATED_AUDITS_REPSONSE_NON_REVERTBALE = {
  json: MOCK_AUDIT_RESULTS_ZERO_IN_PROGRESS_CHANGES_DATA_ARRAY,
  ok: true,
  status: 200,
};

const MOCK_POPULATED_AUDITS_REPSONSE_ALL_REVERTED = {
  json: MOCK_AUDIT_RESULTS_ALL_REVERTED_CHANGES_DATA_ARRAY,
  ok: true,
  status: 200,
};

const MOCK_OK_RESPONSE = {
  ok: true,
  status: 200,
};

let fetchStub;

describe('Given: An <e-changes-poll>', () => {
  before(() => {
    ChangesPoll.register('e-changes-poll');
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

  describe(`When: A row is selected in the change table a revert event occurs`, () => {
    it('Then: A e-changes-poll:selected-count event is sent containing the selection count', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_FOR_CHANGES))
        .resolves(MOCK_POPULATED_AUDITS_RESPONSE);

      const component = await fixture(
        html`<e-changes-poll execution-id="${EXECUTION_ID}"></e-changes-poll>`,
      );

      const changesTable = component.shadowRoot.querySelector(
        'e-rest-paginated-table',
      );

      const UNDEFINED = undefined;
      let receivedSelectedCountEvent = UNDEFINED;
      let receivedRowCount = UNDEFINED;

      component.addEventListener('e-changes-poll:selected-count', ev => {
        receivedSelectedCountEvent = true;
        receivedRowCount = ev.detail;
      });

      simulateDetailEvent(
        changesTable,
        'e-rest-paginated-table:changes:total-elements',
        3,
      );

      // simulate a row selection event on the audit table
      simulateDetailEvent(
        changesTable,
        'e-rest-paginated-table:changes:row-selected',
        MOCK_AUDIT_RESULTS_FOR_CHANGES_DATA_ARRAY.call().results,
      );

      await nextFrame();

      // The e-changes-poll:selected-count should be recieved now
      expect(receivedSelectedCountEvent).to.be.true;
      expect(receivedRowCount).to.equal(2);

      receivedSelectedCountEvent = UNDEFINED;
      receivedRowCount = UNDEFINED;

      component.clearSelection();

      await nextFrame();

      expect(receivedSelectedCountEvent).to.be.true;
      expect(receivedRowCount).to.equal(0);
    });
  });

  describe(`When: A row in the report table is clicked on and revert selected is executed`, () => {
    it('Then: The revert dialog appears, the change is reverted and the dialog is closed', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_FOR_CHANGES))
        .resolves(MOCK_POPULATED_AUDITS_RESPONSE);

      const component = await fixture(
        html`<e-changes-poll execution-id="${EXECUTION_ID}"></e-changes-poll>`,
      );

      const changesTable = component.shadowRoot
        .querySelector('e-rest-paginated-table')
        .shadowRoot.querySelector('eui-table');

      expect(changesTable.data.length).to.equal(4);

      const tableData = changesTable.shadowRoot
        .querySelector('table')
        .querySelector('tbody')
        .querySelectorAll('tr');
      expect(tableData.length).to.equal(4);

      const revertDialog = component.shadowRoot.querySelector(
        'eui-dialog#revertDialog',
      );
      expect(revertDialog.hasAttribute('show')).to.be.false;

      simulateEvent(tableData[0], 'click');

      component.revertChanges(ApproveForAll.SELECTION);
      expect(revertDialog.hasAttribute('show')).to.be.true;
      expect(revertDialog.getAttribute('label')).to.be.equal(
        LOCALE_EN_US.CONFIRM_REVERT_CHANGES,
      );
      expect(revertDialog.querySelector('div').textContent).to.contain(
        LOCALE_EN_US.REVERT_CONFIRMATION,
      );
      const revertButton = revertDialog.querySelector('eui-button');
      expect(revertButton.textContent).to.contain(LOCALE_EN_US.REVERT);

      const expectedBody = {
        auditResultIds: ['3'],
        approveForAll: false,
        operation: Operation.REVERT_CHANGE,
      };

      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_FOR_CHANGES), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match(expectedBody),
        })
        .resolves(MOCK_OK_RESPONSE);

      simulateEvent(revertButton, 'click');

      expect(revertDialog.hasAttribute('show')).to.be.true;
    });
  });

  describe('When: revertChanges with approveForAll true is called', () => {
    it('Then: The revert dialog appears, the change is reverted and the dialog is closed', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_FOR_CHANGES))
        .resolves(MOCK_POPULATED_AUDITS_RESPONSE);

      const component = await fixture(
        html`<e-changes-poll execution-id="${EXECUTION_ID}"></e-changes-poll>`,
      );

      const changesTable = component.shadowRoot
        .querySelector('e-rest-paginated-table')
        .shadowRoot.querySelector('eui-table');

      expect(changesTable.data.length).to.equal(4);

      const revertDialog = component.shadowRoot.querySelector(
        'eui-dialog#revertDialog',
      );
      expect(revertDialog.hasAttribute('show')).to.be.false;

      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_FOR_NON_REVERTABLE_CHANGES))
        .resolves(MOCK_POPULATED_AUDITS_REPSONSE_NON_REVERTBALE);

      component.revertChanges(ApproveForAll.ALL);
      await nextFrame();

      expect(component.approveForAll).to.be.true;
      expect(revertDialog.hasAttribute('show')).to.be.true;
    });
  });

  describe('When: revertChanges is called and there are zero changes', () => {
    it(`Then: The revert dialog appears but the ${LOCALE_EN_US.REVERT} button is disabled`, async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_FOR_CHANGES))
        .resolves(MOCK_POPULATED_AUDITS_REPSONSE_ALL_REVERTED);

      const component = await fixture(
        html`<e-changes-poll
          execution-id="${EXECUTION_ID}"
          .totalChanges="2"
        ></e-changes-poll>`,
      );

      const changesTable = component.shadowRoot
        .querySelector('e-rest-paginated-table')
        .shadowRoot.querySelector('eui-table');

      expect(changesTable.data.length).to.equal(2);

      const revertDialog = component.shadowRoot.querySelector(
        'eui-dialog#revertDialog',
      );
      expect(revertDialog.hasAttribute('show')).to.be.false;

      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_FOR_NON_REVERTABLE_CHANGES))
        .resolves(MOCK_POPULATED_AUDITS_REPSONSE_ALL_REVERTED);

      component.revertChanges(ApproveForAll.ALL);
      await nextFrame();

      expect(component.approveForAll).to.be.true;
      expect(revertDialog.hasAttribute('show')).to.be.true;

      const dialogText = revertDialog.querySelector(
        'div[slot="content"]',
      ).textContent;
      const dialogRevertButton =
        revertDialog.querySelector('eui-button#revert');

      expect(dialogText).to.equal(`0 ${LOCALE_EN_US.REVERT_CONFIRMATION}`);
      expect(dialogRevertButton.hasAttribute('disabled')).to.be.true;
    });
  });

  describe(`When: A call is made to ${EXECUTIONS_AUDITS_API} and changes are in progress and 400 response code is returned`, () => {
    it(`Then: The UI displays a notification with the message "${LOCALE_EN_US.REST_RESPONSE_400_IN_PROGRESS}"`, async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API))
        .resolves(MOCK_POPULATED_AUDITS_RESPONSE);

      const component = await fixture(
        html`<e-changes-poll execution-id="${EXECUTION_ID}"></e-changes-poll>`,
      );

      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_IN_PROGRESS_RESPONSE);

      await triggerRevertChangesNotification(component);
      await nextFrame();

      const euiNotification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(euiNotification.getAttribute('description')).to.contain(
        LOCALE_EN_US.REST_RESPONSE_400_IN_PROGRESS,
      );
      expect(euiNotification.textContent).to.contain(
        LOCALE_EN_US.UNABLE_TO_APPLY_REVERT,
      );
      expect(
        euiNotification.querySelector('eui-icon').getAttribute('name'),
      ).to.contain('failed');
    });
  });

  describe(`When: A row in the report table is clicked on and revert selected is executed`, () => {
    it('Then: The revert dialog appears, the change is reverted and the dialog is closed', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_FOR_CHANGES))
        .resolves(MOCK_POPULATED_AUDITS_RESPONSE);

      const component = await fixture(
        html`<e-changes-poll execution-id="${EXECUTION_ID}"></e-changes-poll>`,
      );

      const changesTable = component.shadowRoot
        .querySelector('e-rest-paginated-table')
        .shadowRoot.querySelector('eui-table');

      expect(changesTable.data.length).to.equal(4);

      const tableData = changesTable.shadowRoot
        .querySelector('table')
        .querySelector('tbody')
        .querySelectorAll('tr');
      expect(tableData.length).to.equal(4);

      const revertDialog = component.shadowRoot.querySelector(
        'eui-dialog#revertDialog',
      );
      expect(revertDialog.hasAttribute('show')).to.be.false;

      simulateEvent(tableData[0], 'click');

      component.revertChanges(ApproveForAll.SELECTION);

      expect(component.approveForAll).to.be.false;
      expect(revertDialog.hasAttribute('show')).to.be.true;
      expect(revertDialog.getAttribute('label')).to.be.equal(
        LOCALE_EN_US.CONFIRM_REVERT_CHANGES,
      );
      expect(revertDialog.querySelector('div').textContent).to.contain(
        LOCALE_EN_US.REVERT_CONFIRMATION,
      );
      const revertButton = revertDialog.querySelector('eui-button');
      expect(revertButton.textContent).to.contain(LOCALE_EN_US.REVERT);

      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_OK_RESPONSE);

      simulateEvent(revertButton, 'click');

      expect(revertDialog.hasAttribute('show')).to.be.true;

      await nextFrame();

      const euiNotification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');
      expect(euiNotification.textContent).to.contain(
        LOCALE_EN_US.REVERT_STARTED,
      );
    });
  });

  describe(`When: A call is made to ${EXECUTIONS_AUDITS_API} and 400 response code is returned`, () => {
    it(`Then: The UI displays a notification with the message "${LOCALE_EN_US.REST_RESPONSE_400_BAD_REQUEST}"`, async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API))
        .resolves(MOCK_POPULATED_AUDITS_RESPONSE);

      const component = await fixture(
        html`<e-changes-poll execution-id="${EXECUTION_ID}"></e-changes-poll>`,
      );

      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_INVALID_FORMAT_EXECUTION);

      await triggerRevertChangesNotification(component);
      await nextFrame();

      const euiNotification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(euiNotification.getAttribute('description')).to.contain(
        LOCALE_EN_US.REST_RESPONSE_400_BAD_REQUEST,
      );
      expect(euiNotification.textContent).to.contain(
        LOCALE_EN_US.UNABLE_TO_APPLY_REVERT,
      );
      expect(
        euiNotification.querySelector('eui-icon').getAttribute('name'),
      ).to.contain('failed');
    });
  });

  describe(`When: A call is made to ${EXECUTIONS_AUDITS_API} and 400 response code is returned`, () => {
    it(`Then: The UI displays a notification with the message "${LOCALE_EN_US.REST_RESPONSE_400_LATEST_EXECUTION}"`, async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API))
        .resolves(MOCK_POPULATED_AUDITS_RESPONSE);

      const component = await fixture(
        html`<e-changes-poll execution-id="${EXECUTION_ID}"></e-changes-poll>`,
      );

      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_LATEST_EXECUTION);

      await triggerRevertChangesNotification(component);
      await nextFrame();

      const euiNotification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(euiNotification.getAttribute('description')).to.contain(
        LOCALE_EN_US.REST_RESPONSE_400_LATEST_EXECUTION,
      );
      expect(euiNotification.textContent).to.contain(
        LOCALE_EN_US.UNABLE_TO_APPLY_REVERT,
      );
      expect(
        euiNotification.querySelector('eui-icon').getAttribute('name'),
      ).to.contain('failed');
    });
  });

  describe(`When: A call is made to ${EXECUTIONS_AUDITS_API} and 403 response code is returned`, () => {
    it(`Then: The UI displays a notification with the message "${LOCALE_EN_US.REST_RESPONSE_403_DESCRIPTION}"`, async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API))
        .resolves(MOCK_POPULATED_AUDITS_RESPONSE);

      const component = await fixture(
        html`<e-changes-poll execution-id="${EXECUTION_ID}"></e-changes-poll>`,
      );

      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_ACCESS_DENIED_RESPONSE);

      await triggerRevertChangesNotification(component);
      await nextFrame();

      const euiNotification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(euiNotification.getAttribute('description')).to.contain(
        LOCALE_EN_US.REST_RESPONSE_403_DESCRIPTION,
      );
      expect(euiNotification.textContent).to.contain(
        LOCALE_EN_US.ACCESS_DENIED,
      );
      expect(
        euiNotification.querySelector('eui-icon').getAttribute('name'),
      ).to.contain('failed');
    });
  });

  describe(`When: A call is made to ${EXECUTIONS_AUDITS_API} and 404 response code is returned`, () => {
    it(`Then: The UI displays a notification with the message "${LOCALE_EN_US.REST_RESPONSE_404_NOT_FOUND}"`, async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API))
        .resolves(MOCK_POPULATED_AUDITS_RESPONSE);

      const component = await fixture(
        html`<e-changes-poll execution-id="${EXECUTION_ID}"></e-changes-poll>`,
      );

      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_NOT_FOUND_RESPONSE);

      await triggerRevertChangesNotification(component);
      await nextFrame();

      const euiNotification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(euiNotification.getAttribute('description')).to.contain(
        LOCALE_EN_US.REST_RESPONSE_404_NOT_FOUND,
      );
      expect(euiNotification.textContent).to.contain(
        LOCALE_EN_US.UNABLE_TO_APPLY_REVERT,
      );
      expect(
        euiNotification.querySelector('eui-icon').getAttribute('name'),
      ).to.contain('failed');
    });
  });

  describe(`When: A call is made to ${EXECUTIONS_AUDITS_API} and 500 response code is returned`, () => {
    it(`Then: The UI displays a notification with the message "${LOCALE_EN_US.REVERT_FAILURE_LONG_DESCRIPTION}"`, async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API))
        .resolves(MOCK_POPULATED_AUDITS_RESPONSE);

      const component = await fixture(
        html`<e-changes-poll execution-id="${EXECUTION_ID}"></e-changes-poll>`,
      );

      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_INTERNAL_SERVER_ERROR_RESPONSE);

      await triggerRevertChangesNotification(component);
      await nextFrame();

      const euiNotification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(euiNotification.getAttribute('description')).to.contain(
        LOCALE_EN_US.REVERT_FAILURE_LONG_DESCRIPTION,
      );
      expect(euiNotification.textContent).to.contain(
        LOCALE_EN_US.UNABLE_TO_APPLY_REVERT,
      );
      expect(
        euiNotification.querySelector('eui-icon').getAttribute('name'),
      ).to.contain('failed');
    });
  });
});
