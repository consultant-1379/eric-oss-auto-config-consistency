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
 * Unit tests for <e-jobs>
 */
import { expect, fixture, nextFrame, html } from '@open-wc/testing';
import sinon from 'sinon';
import {
  MOCK_JOBS_EMPTY_ARRAY,
  MOCK_JOBS_DATA_ARRAY,
  MOCK_JOBS_ONE_DELETED_DATA_ARRAY,
} from '../../../resources/mockData.js';
import {
  MOCK_ACCESS_DENIED_RESPONSE,
  MOCK_INTERNAL_SERVER_ERROR_RESPONSE,
  MOCK_DELETE_SUCCESS_RESPONSE,
  MOCK_BAD_REQUEST_RESPONSE,
  MOCK_NOT_FOUND_RESPONSE,
} from '../../../resources/mockResponses.js';
import {
  getColumnText,
  getHeaderText,
  simulateEvent,
  resetNotifications,
  triggerDeleteNotification,
} from '../../../resources/elementUtils.js';

import Jobs from '../../../../src/apps/jobs/jobs.js';

/* eslint-disable import/no-named-default */
import { default as LOCALE_EN_US } from '../../../../src/apps/jobs/locale/en-us.json';
import { default as INIT_ERROR } from '../../../../src/components/initialization-error/locale/en-us.json';
/* eslint-disable import/no-named-default */

const UNIT_TEST_JOB_NAME = 'unit_test_job1';
const GET_JOBS_API = '/v1/jobs';
const DELETE_JOBS_API = `/v1/jobs/${UNIT_TEST_JOB_NAME}`;
const JOBS_ZERO_MSG = 'Jobs (0)';
const JOBS_TWO_MSG = 'Jobs (2)';
const JOB = 'job';

const COMBINED_LOCALE = { ...LOCALE_EN_US, ...INIT_ERROR };

const MOCK_LOCALE_RESPONSE = {
  json: () => COMBINED_LOCALE,
  ok: true,
  status: 200,
};

const MOCK_EMPTY_JOBS_RESPONSE = {
  json: MOCK_JOBS_EMPTY_ARRAY,
  ok: true,
  status: 200,
};

const MOCK_POPULATED_JOBS_RESPONSE = {
  json: MOCK_JOBS_DATA_ARRAY,
  ok: true,
  status: 200,
};

const MOCK_POPULATED_JOBS_ONE_DELETED_RESPONSE = {
  json: MOCK_JOBS_ONE_DELETED_DATA_ARRAY,
  ok: true,
  status: 200,
};

let fetchStub;
let routerSpy;

describe('Given: An <e-jobs>', () => {
  before(() => {
    Jobs.register('e-jobs');
    fetchStub = sinon.stub(window, 'fetch');

    fetchStub.callsFake(url => {
      if (url.indexOf('locale') !== -1) {
        return Promise.resolve(MOCK_LOCALE_RESPONSE);
      }
      return Promise.resolve(true);
    });

    window.EUI = {
      Router: {
        goto: path => {
          console.log('goto', path);
        },
      },
    };
    routerSpy = sinon.spy(window.EUI.Router, 'goto');
  });

  after(() => {
    sinon.restore();
  });

  afterEach(() => {
    routerSpy.resetHistory();
    resetNotifications();
  });

  describe(`When: A call is made to ${GET_JOBS_API} and an empty array is returned`, () => {
    it('Then: The UI displays Jobs(0) and the message "No jobs created"', async () => {
      fetchStub
        .withArgs(sinon.match(GET_JOBS_API))
        .resolves(MOCK_EMPTY_JOBS_RESPONSE);

      const jobApp = await fixture('<e-jobs></e-jobs>');
      const jobsText = jobApp.shadowRoot.querySelector('p .executions-display');
      expect(jobsText.textContent.trim()).to.equal(JOBS_ZERO_MSG);

      const emptyStateText = jobApp.shadowRoot
        .querySelector('.app-empty-state')
        .querySelector('p');
      expect(emptyStateText.textContent).to.equal(`${LOCALE_EN_US.ZERO_JOBS}`);
    });
  });

  describe(`When: A call is made to ${GET_JOBS_API} and a 404 response is returned`, async () => {
    it('Then: The UI displays the initialization error component', async () => {
      fetchStub
        .withArgs(sinon.match(GET_JOBS_API))
        .resolves(MOCK_NOT_FOUND_RESPONSE);

      const jobApp = await fixture('<e-jobs></e-jobs>');
      const initializationComponent = jobApp.shadowRoot.querySelector(
        'e-initialization-error',
      );

      expect(initializationComponent).to.exist;

      const appContainer =
        initializationComponent.shadowRoot.querySelector('.app-container');

      const headerMsg = appContainer.querySelector('.header-msg');
      expect(headerMsg.textContent).to.contain(INIT_ERROR.ERROR_HEADER);

      const bodyText = appContainer.querySelector('.body-msg span');
      expect(bodyText.textContent).to.contain(INIT_ERROR.ERROR_BODY);
    });
  });

  describe(`When: A call is made to ${GET_JOBS_API} and a 500 response is returned`, () => {
    it('Then: The UI displays the initialization error component', async () => {
      fetchStub
        .withArgs(sinon.match(GET_JOBS_API))
        .resolves(MOCK_INTERNAL_SERVER_ERROR_RESPONSE);

      const jobApp = await fixture('<e-jobs></e-jobs>');
      const initializationComponent = jobApp.shadowRoot.querySelector(
        'e-initialization-error',
      );

      expect(initializationComponent).to.exist;

      const appContainer =
        initializationComponent.shadowRoot.querySelector('.app-container');

      const headerMsg = appContainer.querySelector('.header-msg');
      expect(headerMsg.textContent).to.contain(INIT_ERROR.ERROR_HEADER);

      const bodyText = appContainer.querySelector('.body-msg span');
      expect(bodyText.textContent).to.contain(INIT_ERROR.ERROR_BODY);
    });
  });

  describe(`When: A call is made to ${GET_JOBS_API} and an array of data is returned`, () => {
    it('Then: The UI displays Jobs(2) and a table with 2 entries', async () => {
      fetchStub
        .withArgs(sinon.match(GET_JOBS_API))
        .resolves(MOCK_POPULATED_JOBS_RESPONSE);

      const jobApp = await fixture('<e-jobs></e-jobs>');
      const jobsText = jobApp.shadowRoot.querySelector('p .executions-display');
      expect(jobsText.textContent.trim()).to.equal(JOBS_TWO_MSG);

      const appHeaderButtons = jobApp.shadowRoot
        .querySelector('.app-header')
        .querySelector('.action-buttons')
        .querySelectorAll('eui-button');
      expect(appHeaderButtons[0].textContent.trim()).to.equal(
        `${LOCALE_EN_US.VIEW_EXECUTIONS}`,
      );

      const jobsTable = jobApp.shadowRoot
        .querySelector('.app-table-container')
        .querySelector('eui-table');

      const tableHeaders = jobsTable.shadowRoot
        .querySelector('thead')
        .querySelector('tr:not(.filters)')
        .querySelectorAll('th');
      expect(tableHeaders.length).to.equal(4);
      expect(getHeaderText(tableHeaders[0])).to.eq(LOCALE_EN_US.JOB_NAME);
      expect(getHeaderText(tableHeaders[1])).to.eq(LOCALE_EN_US.SCHEDULE);
      expect(getHeaderText(tableHeaders[2])).to.eq(LOCALE_EN_US.RULESET_NAME);
      expect(getHeaderText(tableHeaders[3])).to.eq(LOCALE_EN_US.SCOPE_NAME);

      const tableData = jobsTable.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');
      expect(tableData.length).to.equal(2);

      simulateEvent(tableData[0], 'click');
      await nextFrame();
      // Add test for 'Edit' button when functionality is added.
      expect(appHeaderButtons[0].hasAttribute('disabled')).to.be.false;
      expect(appHeaderButtons[1].hasAttribute('disabled')).to.be.false;
    });
  });

  describe("When: The 'View Executions' button is clicked", () => {
    it("Then: The app navigates to parameterised 'Executions Overview'", async () => {
      fetchStub
        .withArgs(sinon.match(GET_JOBS_API))
        .resolves(MOCK_POPULATED_JOBS_RESPONSE);

      const jobApp = await fixture('<e-jobs></e-jobs>');
      const appHeaderButtons = jobApp.shadowRoot
        .querySelector('.app-header')
        .querySelector('.action-buttons')
        .querySelectorAll('eui-button');

      const viewExecutionsButton = appHeaderButtons[0];

      const jobsTable = jobApp.shadowRoot
        .querySelector('.app-table-container')
        .querySelector('eui-table');

      const tableData = jobsTable.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');

      const firstRow = tableData[0].querySelectorAll('td');
      const jobName = getColumnText(firstRow[0]);
      simulateEvent(tableData[0], 'click');
      await nextFrame();
      simulateEvent(viewExecutionsButton, 'click');
      await nextFrame();

      expect(routerSpy.args[0][0]).to.equal(
        `/#eacc/executions?jobName=${jobName}`,
      );
    });
  });

  describe("When: The 'Delete Job' button is clicked", () => {
    it('Then: The delete dialog is shown', async () => {
      fetchStub
        .withArgs(sinon.match(GET_JOBS_API))
        .resolves(MOCK_POPULATED_JOBS_RESPONSE);

      const jobApp = await fixture('<e-jobs></e-jobs>');

      const appHeaderButtons = jobApp.shadowRoot
        .querySelector('.app-header')
        .querySelector('.action-buttons')
        .querySelectorAll('eui-button');

      const deleteButton = appHeaderButtons[2];

      const jobsTable = jobApp.shadowRoot
        .querySelector('.app-table-container')
        .querySelector('eui-table');

      const tableData = jobsTable.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');

      simulateEvent(tableData[0], 'click');
      await nextFrame();

      simulateEvent(deleteButton, 'click');
      await nextFrame();

      const deleteDialog = jobApp.shadowRoot.querySelector('eui-dialog');
      expect(deleteDialog.getAttribute('label')).to.eq(
        LOCALE_EN_US.CONFIRM_DELETE,
      );

      const dialogText = deleteDialog.querySelector('div');
      expect(dialogText.textContent).to.eq(
        `${LOCALE_EN_US.DELETE_JOB_MESSAGE} ${UNIT_TEST_JOB_NAME}?`,
      );
    });
  });

  describe("When: 'Delete' on the dialog box is clicked and delete is successful", () => {
    it('Then: A successful notification is displayed and job is removed from the table', async () => {
      fetchStub
        .withArgs(sinon.match(GET_JOBS_API))
        .resolves(MOCK_POPULATED_JOBS_RESPONSE);

      const jobApp = await fixture('<e-jobs></e-jobs>');

      const jobsTable = jobApp.shadowRoot
        .querySelector('.app-table-container')
        .querySelector('eui-table');

      let tableData = jobsTable.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');

      // The jobs table should have 2 rows.
      expect(tableData.length).to.equal(2);

      fetchStub
        .withArgs(sinon.match(DELETE_JOBS_API))
        .resolves(MOCK_DELETE_SUCCESS_RESPONSE);

      fetchStub
        .withArgs(sinon.match(GET_JOBS_API))
        .resolves(MOCK_POPULATED_JOBS_ONE_DELETED_RESPONSE);

      await triggerDeleteNotification(jobApp, JOB);

      const notification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(notification.textContent.trim()).to.eq(
        `${UNIT_TEST_JOB_NAME} ${LOCALE_EN_US.DELETED_SUCCESS_MESSAGE}`,
      );

      const statusIcon = notification.querySelector('eui-icon');

      expect(statusIcon.getAttribute('name')).to.eq('check');
      expect(statusIcon.getAttribute('color')).to.eq('var(--green-35)');

      await nextFrame();

      tableData = jobsTable.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');

      // Delete followed by table update should result in 1 row.
      expect(tableData.length).to.equal(1);
    });
  });

  describe(`When: A call is made to ${DELETE_JOBS_API} and an unknown error occurs`, () => {
    it('Then: A failure notification is displayed with a generic description', async () => {
      fetchStub
        .withArgs(sinon.match(GET_JOBS_API))
        .resolves(MOCK_POPULATED_JOBS_RESPONSE);

      const jobApp = await fixture('<e-jobs></e-jobs>');

      fetchStub
        .withArgs(sinon.match(DELETE_JOBS_API))
        .resolves(new Error('Unable to contact server.'));

      await triggerDeleteNotification(jobApp, JOB);

      const notification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(notification.textContent.trim()).to.eq(
        `${LOCALE_EN_US.DELETED_FAILURE_MESSAGE} ${UNIT_TEST_JOB_NAME}`,
      );
      expect(notification.description).to.eq(
        LOCALE_EN_US.DELETE_FAILURE_GENERIC_DESCRIPTION,
      );

      const statusIcon = notification.querySelector('eui-icon');

      expect(statusIcon.getAttribute('name')).to.eq('failed');
      expect(statusIcon.getAttribute('color')).to.eq('var(--red-52)');
    });
  });

  describe(`When: A call is made to ${DELETE_JOBS_API} and a 400 response is returned`, () => {
    it('Then: A failure notification is displayed with a generic description', async () => {
      fetchStub
        .withArgs(sinon.match(GET_JOBS_API))
        .resolves(MOCK_POPULATED_JOBS_RESPONSE);

      const jobApp = await fixture('<e-jobs></e-jobs>');

      fetchStub
        .withArgs(sinon.match(DELETE_JOBS_API))
        .resolves(MOCK_BAD_REQUEST_RESPONSE);

      await triggerDeleteNotification(jobApp, JOB);

      const notification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(notification.textContent.trim()).to.eq(
        `${LOCALE_EN_US.DELETED_FAILURE_MESSAGE} ${UNIT_TEST_JOB_NAME}`,
      );
      expect(notification.description).to.eq(
        LOCALE_EN_US.DELETE_FAILURE_GENERIC_DESCRIPTION,
      );

      const statusIcon = notification.querySelector('eui-icon');

      expect(statusIcon.getAttribute('name')).to.eq('failed');
      expect(statusIcon.getAttribute('color')).to.eq('var(--red-52)');
    });
  });

  describe(`When: A call is made to ${DELETE_JOBS_API} and a 404 response is returned`, () => {
    it('Then: A failure notification is displayed with 404 failure description', async () => {
      fetchStub
        .withArgs(sinon.match(GET_JOBS_API))
        .resolves(MOCK_POPULATED_JOBS_RESPONSE);

      const jobApp = await fixture('<e-jobs></e-jobs>');

      fetchStub
        .withArgs(sinon.match(DELETE_JOBS_API))
        .resolves(MOCK_NOT_FOUND_RESPONSE);

      await triggerDeleteNotification(jobApp, JOB);

      const notification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(notification.textContent.trim()).to.eq(
        `${LOCALE_EN_US.DELETED_FAILURE_MESSAGE} ${UNIT_TEST_JOB_NAME}`,
      );
      expect(notification.description).to.eq(
        LOCALE_EN_US.DELETE_FAILURE_404_DESCRIPTION,
      );

      const statusIcon = notification.querySelector('eui-icon');

      expect(statusIcon.getAttribute('name')).to.eq('failed');
      expect(statusIcon.getAttribute('color')).to.eq('var(--red-52)');
    });
  });

  describe(`When: A call is made to ${GET_JOBS_API} and an array of data is returned`, () => {
    it('Then: The UI displays Jobs(2) and a table with latest entry at the top', async () => {
      fetchStub
        .withArgs(sinon.match(GET_JOBS_API))
        .resolves(MOCK_POPULATED_JOBS_RESPONSE);

      const unitTestJob2 = 'unit_test_job2';
      const jobApp = await fixture(html`<e-jobs></e-jobs>`);
      jobApp.createdJob = unitTestJob2;
      // simulating what would happen if the url changed. (We are not allowed to modify the url, test framework fails)
      jobApp.didConnect();
      await nextFrame();

      const jobsText = jobApp.shadowRoot.querySelector('p .executions-display');
      expect(jobsText.textContent.trim()).to.equal(JOBS_TWO_MSG);

      const jobsTable = jobApp.shadowRoot
        .querySelector('.app-table-container')
        .querySelector('eui-table');

      const tableData = jobsTable.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');
      expect(tableData.length).to.equal(2);

      // check row one is equal to the value we supply into the e-jobs above
      expect(
        tableData[0].querySelectorAll('td')[0].textContent.trim(),
      ).to.equal(unitTestJob2);
      expect(
        tableData[1].querySelectorAll('td')[0].textContent.trim(),
      ).to.equal(UNIT_TEST_JOB_NAME);
    });
  });
  describe(`When: A call is made to ${GET_JOBS_API} and a 403 response is returned`, () => {
    it('Then: The UI displays the access denied error component', async () => {
      fetchStub
        .withArgs(sinon.match(GET_JOBS_API))
        .resolves(MOCK_ACCESS_DENIED_RESPONSE);

      const jobsApp = await fixture('<e-jobs></e-jobs>');

      const accessDeniedError = jobsApp.shadowRoot.querySelector(
        'e-access-denied-error',
      );

      expect(accessDeniedError).to.exist;
    });
  });

  describe(`When: A call is made to ${DELETE_JOBS_API} and a 403 response is returned`, () => {
    it('Then: A failure notification is displayed with 403 failure description', async () => {
      fetchStub
        .withArgs(sinon.match(GET_JOBS_API))
        .resolves(MOCK_POPULATED_JOBS_RESPONSE);

      const jobApp = await fixture('<e-jobs></e-jobs>');

      fetchStub
        .withArgs(sinon.match(DELETE_JOBS_API))
        .resolves(MOCK_ACCESS_DENIED_RESPONSE);

      await triggerDeleteNotification(jobApp, JOB);

      const notification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.ACCESS_DENIED,
      );
      expect(notification.description).to.eq(LOCALE_EN_US.REST_403_DESCRIPTION);

      const statusIcon = notification.querySelector('eui-icon');

      expect(statusIcon.getAttribute('name')).to.eq('failed');
      expect(statusIcon.getAttribute('color')).to.eq('var(--red-52)');
    });
  });
});
