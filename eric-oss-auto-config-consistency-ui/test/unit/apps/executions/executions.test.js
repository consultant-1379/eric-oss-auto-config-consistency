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
 * Unit tests for <e-executions>
 */
import { expect, fixture, nextFrame, html } from '@open-wc/testing';
import { fixtureWrapper } from '@open-wc/testing-helpers';
import sinon from 'sinon';

import {
  MOCK_EXECUTIONS_EMPTY_ARRAY,
  MOCK_EXECUTIONS_DATA_ARRAY,
  MOCK_EXECUTIONS_EXTRA_LARGE_DATA_ARRAY,
  MOCK_EXECUTIONS_DATA_ARRAY_TWO_ENTRIES,
  MOCK_EXECUTIONS_DATA_ARRAY_WITH_DIFFERENT_EXECUTION_STATUSES,
} from '../../../resources/mockData.js';
import {
  MOCK_ACCESS_DENIED_RESPONSE,
  MOCK_INTERNAL_SERVER_ERROR_RESPONSE,
  MOCK_NOT_FOUND_RESPONSE,
} from '../../../resources/mockResponses.js';
import {
  getColumnText,
  getHeaderText,
  simulateEvent,
} from '../../../resources/elementUtils.js';

import Executions from '../../../../src/apps/executions/executions.js';

/* eslint-disable import/no-named-default */
import { default as LOCALE_EN_US } from '../../../../src/apps/executions/locale/en-us.json';
import { default as INIT_ERROR } from '../../../../src/components/initialization-error/locale/en-us.json';

/* eslint-disable import/no-named-default */

import { DEFAULT_POLLING_STATE } from '../../../../src/utils/executionsConfig.js';

const EXECUTIONS_API = '/v1/executions';
const NO_EXECUTIONS_CREATED_MSG = 'No jobs have been executed';
const JOB_EXECUTIONS_ZERO_MSG = 'Job Executions (0)';
const VIEW_JOBS_MSG = 'View Jobs';

const COMBINED_LOCALE = { ...LOCALE_EN_US, ...INIT_ERROR };

const MOCK_LOCALE_RESPONSE = {
  json: () => COMBINED_LOCALE,
  ok: true,
  status: 200,
};

const MOCK_EMPTY_JOBS_RESPONSE = {
  json: MOCK_EXECUTIONS_EMPTY_ARRAY,
  ok: true,
  status: 200,
};

const MOCK_POPULATED_EXECUTIONS_RESPONSE = {
  json: MOCK_EXECUTIONS_DATA_ARRAY,
  ok: true,
  status: 200,
};

const MOCK_POPULATED_EXECUTIONS_RESPONSE_WITH_DIFFERENT_EXECUTION_STATUSES = {
  json: MOCK_EXECUTIONS_DATA_ARRAY_WITH_DIFFERENT_EXECUTION_STATUSES,
  ok: true,
  status: 200,
};

const MOCK_POPULATED_EXECUTIONS_RESPONSE_TWO_ENTRIES = {
  json: MOCK_EXECUTIONS_DATA_ARRAY_TWO_ENTRIES,
  ok: true,
  status: 200,
};

const MOCK_VIRTUAL_SCROLLING_EXECUTIONS_RESPONSE = {
  json: MOCK_EXECUTIONS_EXTRA_LARGE_DATA_ARRAY,
  ok: true,
  status: 200,
};

const JOB_NAME = 'unit_test_job1';

let fetchStub;
let routerSpy;

function createMenuStructure() {
  const body = document.createElement('body');
  const euiContainer = document.createElement('eui-container');
  const main = document.createElement('main');
  const div = document.createElement('div');
  const euiAppBar = document.createElement('eui-app-bar');

  euiContainer.attachShadow({ mode: 'open' });
  body.appendChild(euiContainer);

  euiContainer.shadowRoot.appendChild(main);
  main.appendChild(div);
  div.appendChild(euiAppBar);
  return body;
}

describe('Given: An <e-executions>', () => {
  before(() => {
    Executions.register('e-executions');
    fetchStub = sinon.stub(window, 'fetch');

    // Mock the window.EUI.Router.goto call as it is not available.
    window.EUI = {
      Router: {
        goto: path => {
          console.log('goto', path);
        },
      },
    };
    routerSpy = sinon.spy(window.EUI.Router, 'goto');
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
    routerSpy.resetHistory();
  });

  after(() => {
    sinon.restore();
  });

  describe(`When: A call is made to ${EXECUTIONS_API} and an empty array is returned`, () => {
    it('Then: The UI displays Job Executions(0) and the message "No jobs have been executed"', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_API))
        .resolves(MOCK_EMPTY_JOBS_RESPONSE);

      const wrapper = fixtureWrapper(createMenuStructure());
      const executionsApp = await fixture('<e-executions></e-executions>');
      wrapper.appendChild(executionsApp);

      const executionsText = executionsApp.shadowRoot.querySelector(
        'p .executions-display',
      );
      expect(executionsText.textContent.trim()).to.equal(
        JOB_EXECUTIONS_ZERO_MSG,
      );

      const emptyStateText = executionsApp.shadowRoot
        .querySelector('.app-empty-state')
        .querySelector('p');
      expect(emptyStateText.textContent.trim()).to.equal(
        NO_EXECUTIONS_CREATED_MSG,
      );

      const viewJobsButton = executionsApp.shadowRoot
        .querySelector('.app-empty-navigation')
        .querySelector('p')
        .querySelector('eui-button');
      expect(viewJobsButton.textContent).to.equal(VIEW_JOBS_MSG);

      // Click the button now
      simulateEvent(viewJobsButton, 'click');
      await nextFrame();
      expect(routerSpy.args[0][0]).to.equal('/#eacc/jobs');
    });
  });

  describe(`When: A call is made to ${EXECUTIONS_API} and a 404 response is returned`, async () => {
    it('Then: The UI displays the initialization error component', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_API))
        .resolves(MOCK_NOT_FOUND_RESPONSE);

      const wrapper = fixtureWrapper(createMenuStructure());
      const executionsApp = await fixture('<e-executions></e-executions>');
      wrapper.appendChild(executionsApp);

      const initializationComponent = executionsApp.shadowRoot.querySelector(
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

  describe(`When: A call is made to ${EXECUTIONS_API} and a 500 response is returned`, () => {
    it('Then: The UI displays the initialization error component', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_API))
        .resolves(MOCK_INTERNAL_SERVER_ERROR_RESPONSE);

      const wrapper = fixtureWrapper(createMenuStructure());
      const executionsApp = await fixture('<e-executions></e-executions>');
      wrapper.appendChild(executionsApp);

      const initializationComponent = executionsApp.shadowRoot.querySelector(
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

  describe(`When: A call is made to ${EXECUTIONS_API} and an array of data is returned`, () => {
    it('Then: The UI displays Job Executions(4) and a table with 4 entries', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_API))
        .resolves(MOCK_POPULATED_EXECUTIONS_RESPONSE);

      const wrapper = fixtureWrapper(createMenuStructure());
      const executionsApp = await fixture('<e-executions></e-executions>');
      wrapper.appendChild(executionsApp);

      const executionsText = executionsApp.shadowRoot.querySelector(
        'p .executions-display',
      );
      expect(executionsText.textContent.trim()).to.equal(
        `${LOCALE_EN_US.JOB_EXECUTIONS} (6)`,
      );

      const viewReportsButton = executionsApp.shadowRoot
        .querySelector('p')
        .querySelector('eui-button');
      expect(viewReportsButton.textContent.trim()).to.equal(
        `${LOCALE_EN_US.VIEW_REPORTS}`,
      );

      const executionsTable = executionsApp.shadowRoot
        .querySelector('div')
        .querySelector('eui-table');

      const tableHeaders = executionsTable.shadowRoot
        .querySelector('thead')
        .querySelector('tr:not(.filters)')
        .querySelectorAll('th');
      expect(tableHeaders.length).to.equal(10);
      expect(getHeaderText(tableHeaders[0])).to.eq(LOCALE_EN_US.EXECUTION_ID);
      expect(getHeaderText(tableHeaders[1])).to.eq(LOCALE_EN_US.JOB_NAME);
      expect(getHeaderText(tableHeaders[2])).to.eq(LOCALE_EN_US.EXECUTION_TYPE);
      expect(getHeaderText(tableHeaders[3])).to.eq(
        LOCALE_EN_US.EXECUTION_START,
      );
      expect(getHeaderText(tableHeaders[4])).to.eq(LOCALE_EN_US.EXECUTION_END);
      expect(getHeaderText(tableHeaders[5])).to.eq(LOCALE_EN_US.STATUS);
      expect(getHeaderText(tableHeaders[6])).to.eq(
        LOCALE_EN_US.ATTRIBUTES_AUDITED,
      );
      expect(getHeaderText(tableHeaders[7])).to.eq(LOCALE_EN_US.MOS_AUDITED);
      expect(getHeaderText(tableHeaders[8])).to.eq(
        LOCALE_EN_US.INCONSISTENCIES,
      );

      const tableData = executionsTable.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');
      expect(tableData.length).to.equal(6);

      const rowOneColumns = tableData[0].querySelectorAll('td');
      const executionId = getColumnText(rowOneColumns[0]);
      const jobName = getColumnText(rowOneColumns[1]);

      const changesFailedRow = tableData[1].querySelectorAll('td');
      const changesInProgressRow = tableData[0].querySelectorAll('td');

      const executionStatusChangesFailedRow = getColumnText(
        changesFailedRow[5],
      );
      const executionStatusChangesInProgressRow = getColumnText(
        changesInProgressRow[5],
      );
      expect(executionStatusChangesFailedRow).to.contain(
        LOCALE_EN_US.CHANGES_FAILED_STATUS,
      );
      expect(executionStatusChangesInProgressRow).to.contain(
        LOCALE_EN_US.CHANGES_IN_PROGRESS_STATUS,
      );

      simulateEvent(tableData[0], 'click');
      simulateEvent(viewReportsButton, 'click');
      await nextFrame();
      expect(routerSpy.args[0][0]).to.equal(
        `/#eacc/executions/execution-reports?executionId=${executionId}&jobName=${jobName}`,
      );
    });
  });

  describe(`When: Visiting the Executions app and job filter has been applied`, () => {
    it('Then: The table will render only rows for that job and the filter text is visible', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_API))
        .resolves(MOCK_POPULATED_EXECUTIONS_RESPONSE);

      const wrapper = fixtureWrapper(createMenuStructure());
      const executionsApp = await fixture(
        `<e-executions job-name="${JOB_NAME}"></e-executions>`,
      );
      wrapper.appendChild(executionsApp);

      let executionsText = executionsApp.shadowRoot.querySelector('p');

      expect(
        executionsText.querySelector('.executions-display').textContent.trim(),
      ).to.equal(`${LOCALE_EN_US.JOB_EXECUTIONS} (1)`);
      expect(executionsText.textContent.trim()).to.contain(JOB_NAME);
      expect(executionsText.textContent.trim()).to.contain(LOCALE_EN_US.CLEAR);

      const executionsTable = executionsApp.shadowRoot
        .querySelector('div')
        .querySelector('eui-table');

      let tableData = executionsTable.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');
      expect(tableData.length).to.equal(1);

      const clearFilterLink = executionsText.querySelector(
        '.app-filter-text eui-link',
      );
      simulateEvent(clearFilterLink, 'click');
      await nextFrame();

      executionsText = executionsApp.shadowRoot.querySelector('p');

      expect(executionsText.textContent.trim()).to.not.equal(JOB_NAME);
      expect(executionsText.textContent.trim()).to.not.equal(
        LOCALE_EN_US.CLEAR,
      );

      tableData = executionsTable.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');
      expect(tableData.length).to.equal(6);
    });
  });

  describe(`When: A call is made to ${EXECUTIONS_API} and a very large array of data is returned`, () => {
    it('Then: The virtual scrolling table does not render all rows, only enough to fill the viewport', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_API))
        .resolves(MOCK_VIRTUAL_SCROLLING_EXECUTIONS_RESPONSE);

      const wrapper = fixtureWrapper(createMenuStructure());
      const executionsApp = await fixture('<e-executions></e-executions>');
      wrapper.appendChild(executionsApp);

      const executionsText = executionsApp.shadowRoot.querySelector(
        'p .executions-display',
      );

      expect(executionsText.textContent.trim()).to.equal(
        `${LOCALE_EN_US.JOB_EXECUTIONS} (1000)`,
      );

      const executionsTable = executionsApp.shadowRoot
        .querySelector('div')
        .querySelector('eui-table');

      const tableContainerHeight =
        executionsTable.shadowRoot.querySelector(
          '.table__container',
        ).clientHeight;

      // Stripping the --row-height value of letters (E.g. "px")
      const rowHeight = executionsTable.shadowRoot
        .querySelector('tbody')
        .style.getPropertyValue('--row-height')
        .replace(/\D/g, '');

      // Get the actual count of rows rendered
      const renderedRows = executionsTable.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');

      const additionalRowsBuffer = 20;

      // The formula for calculating how many rows a virtual scroll table SHOULD render at a time
      const expectedRows =
        Math.ceil(tableContainerHeight / rowHeight) + additionalRowsBuffer;

      expect(renderedRows.length).to.equal(expectedRows);
    });
  });

  describe('When: Polling is not enabled', async () => {
    it(`Then: No call is made to ${EXECUTIONS_API} and polling state is set to ${DEFAULT_POLLING_STATE}`, async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_API))
        .resolves(MOCK_POPULATED_EXECUTIONS_RESPONSE);

      const wrapper = fixtureWrapper(createMenuStructure());

      const executionsApp = await fixture('<e-executions></e-executions>');
      wrapper.append(executionsApp);
      fetchStub.reset(); // Resetting the stub again due to lifecycle calls

      executionsApp.isPolling = false;
      executionsApp._pollTableData();

      expect(executionsApp.pollingTimer).to.equal(DEFAULT_POLLING_STATE);
      sinon.assert.notCalled(fetchStub);
    });
  });

  describe('When: e-executions contains existing data', async () => {
    it('Then: old executions are removed, existing executions updated, and new executions added', async () => {
      const wrapper = fixtureWrapper(createMenuStructure());
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_API))
        .resolves(MOCK_POPULATED_EXECUTIONS_RESPONSE_TWO_ENTRIES);

      const executionsApp = await fixture(html`<e-executions></e-executions>`);
      wrapper.append(executionsApp);

      let executionsText = executionsApp.shadowRoot.querySelector(
        'p .executions-display',
      );
      await nextFrame();
      expect(executionsText.textContent.trim()).to.equal(
        `${LOCALE_EN_US.JOB_EXECUTIONS} (2)`,
      );

      let executionsTable = executionsApp.shadowRoot
        .querySelector('div')
        .querySelector('eui-table');

      let tableData = executionsTable.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');
      expect(tableData.length).to.equal(2);

      /* eslint radix: ["error", "as-needed"] */
      expect(
        parseInt(tableData[0].querySelectorAll('td')[0].textContent.trim()),
      ).to.equal(100);
      expect(
        parseInt(tableData[1].querySelectorAll('td')[0].textContent.trim()),
      ).to.equal(1);
      expect(tableData[1].querySelectorAll('td')[4].textContent.trim()).to.be
        .empty;

      expect(executionsApp.selectedRow).to.equal(undefined);
      // Select the row with id: 100
      simulateEvent(tableData[0].querySelectorAll('td')[0], 'click');
      expect(executionsApp.selectedRow).to.be.not.empty;

      fetchStub
        .withArgs(sinon.match(EXECUTIONS_API))
        .resolves(MOCK_POPULATED_EXECUTIONS_RESPONSE);

      executionsApp._updateTableData();
      await nextFrame();

      // selected row should now be unset now it has been removed from the table
      expect(executionsApp.selectedRow).to.equal(undefined);

      executionsText = executionsApp.shadowRoot.querySelector(
        'p .executions-display',
      );
      expect(executionsText.textContent.trim()).to.equal(
        `${LOCALE_EN_US.JOB_EXECUTIONS} (6)`,
      );

      executionsTable = executionsApp.shadowRoot
        .querySelector('div')
        .querySelector('eui-table');

      tableData = executionsTable.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');
      expect(tableData.length).to.equal(6);

      expect(
        parseInt(tableData[0].querySelectorAll('td')[0].textContent.trim()),
      ).to.equal(6);
      expect(
        parseInt(tableData[1].querySelectorAll('td')[0].textContent.trim()),
      ).to.equal(5);
      expect(
        parseInt(tableData[2].querySelectorAll('td')[0].textContent.trim()),
      ).to.equal(4);
      expect(
        parseInt(tableData[3].querySelectorAll('td')[0].textContent.trim()),
      ).to.equal(3);
      expect(tableData[3].querySelectorAll('td')[4].textContent.trim()).to.not
        .be.empty;
    });
  });
  describe(`When: A call is made to ${EXECUTIONS_API} and a 403 response is returned`, () => {
    it('Then: The UI displays the access denied error component', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_API))
        .resolves(MOCK_ACCESS_DENIED_RESPONSE);

      const executionsApp = await fixture('<e-executions></e-executions>');

      const accessDeniedError = executionsApp.shadowRoot.querySelector(
        'e-access-denied-error',
      );

      expect(accessDeniedError).to.exist;
    });
  });
  describe(`When: A call is made to ${EXECUTIONS_API} and an array of data with different execution statuses is returned`, () => {
    it('Then: The UI displays the view reports button when a valid execution is clicked', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_API))
        .resolves(
          MOCK_POPULATED_EXECUTIONS_RESPONSE_WITH_DIFFERENT_EXECUTION_STATUSES,
        );

      const wrapper = fixtureWrapper(createMenuStructure());
      const executionsApp = await fixture('<e-executions></e-executions>');
      wrapper.appendChild(executionsApp);

      const viewReportsButton = executionsApp.shadowRoot
        .querySelector('p')
        .querySelector('eui-button');

      expect(viewReportsButton.disabled).to.be.true;

      const executionsTable = executionsApp.shadowRoot
        .querySelector('div')
        .querySelector('eui-table');

      const tableData = executionsTable.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');
      expect(tableData.length).to.equal(10);

      simulateEvent(tableData[0], 'click'); // Changes in Progress
      expect(viewReportsButton.disabled).to.be.false;

      simulateEvent(tableData[1], 'click'); // Changes Partially Successful
      expect(viewReportsButton.disabled).to.be.false;

      simulateEvent(tableData[2], 'click'); // Random Status
      expect(viewReportsButton.disabled).to.be.true;

      simulateEvent(tableData[3], 'click'); // Changes Failed
      expect(viewReportsButton.disabled).to.be.false;

      simulateEvent(tableData[4], 'click'); // Changes Successful
      expect(viewReportsButton.disabled).to.be.false;

      simulateEvent(tableData[5], 'click'); // Audit Partially Successful
      expect(viewReportsButton.disabled).to.be.false;

      simulateEvent(tableData[6], 'click'); // Audit in Progress
      expect(viewReportsButton.disabled).to.be.true;

      simulateEvent(tableData[7], 'click'); // Audit Failed
      expect(viewReportsButton.disabled).to.be.true;

      simulateEvent(tableData[8], 'click'); // Audit Successful
      expect(viewReportsButton.disabled).to.be.false;

      simulateEvent(tableData[9], 'click'); // Audit Skipped
      expect(viewReportsButton.disabled).to.be.true;
    });
  });
});
