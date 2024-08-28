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

/**
 * Unit tests for e-rest-paginated-table
 */
import { expect, fixture, html, nextFrame } from '@open-wc/testing';
import sinon from 'sinon';
import RestPaginatedTable from '../../../../src/components/rest-paginated-table/rest-paginated-table.js';
/* eslint-disable import/no-named-default */
import { default as LOCALE_EN_US } from '../../../../src/components/rest-paginated-table/locale/en-us.json';
import { default as INIT_ERROR } from '../../../../src/components/initialization-error/locale/en-us.json';
/* eslint-disable import/no-named-default */
import {
  MOCK_AUDIT_RESULTS_EMPTY_ARRAY,
  MOCK_AUDIT_RESULTS_IS_PROPOSED_CHANGES_DATA_ARRAY,
  MOCK_AUDIT_RESULTS_IS_NOT_PROPOSED_CHANGES_DATA_ARRAY_PAGE1,
  MOCK_AUDIT_RESULTS_IS_NOT_PROPOSED_CHANGES_DATA_ARRAY_PAGE2,
} from '../../../resources/mockData.js';
import {
  getHeaderText,
  simulateEvent,
} from '../../../resources/elementUtils.js';
import {
  MOCK_INTERNAL_SERVER_ERROR_RESPONSE,
  MOCK_NOT_FOUND_RESPONSE,
} from '../../../resources/mockResponses.js';

import { ChangeStatus } from '../../../../src/utils/attributes/changeAttributes.js';

const EXECUTIONS_AUDITS_API_FOR_CHANGES = `/v1/executions/3/audit-results?page=0&pageSize=2000&filter=changeStatus:(${ChangeStatus.IMPLEMENTATION_IN_PROGRESS},${ChangeStatus.IMPLEMENTATION_COMPLETE},${ChangeStatus.IMPLEMENTATION_FAILED},${ChangeStatus.REVERSION_IN_PROGRESS},${ChangeStatus.REVERSION_COMPLETE},${ChangeStatus.REVERSION_FAILED})`;

const EXECUTIONS_AUDITS_API = '/v1/executions/3/audit-results';

const EXECUTIONS_AUDITS_API_INCONSISTENT = `${EXECUTIONS_AUDITS_API}?page=0&pageSize=2000&filter=auditStatus:Inconsistent`;

const EXECUTIONS_AUDITS_API_PAGE1 = `${EXECUTIONS_AUDITS_API}?page=0&pageSize=2000`;

const EXECUTIONS_AUDITS_API_PAGE2 = `${EXECUTIONS_AUDITS_API}?page=1&pageSize=2000`;

const COMBINED_LOCALE = { ...LOCALE_EN_US, ...INIT_ERROR };

const MOCK_LOCALE_RESPONSE = {
  json: () => COMBINED_LOCALE,
  ok: true,
  status: 200,
};

const MOCK_POPULATED_AUDITS_RESPONSE_IS_PROPOSED = {
  json: MOCK_AUDIT_RESULTS_IS_PROPOSED_CHANGES_DATA_ARRAY,
  ok: true,
  status: 200,
};

const MOCK_POPULATED_AUDITS_RESPONSE_IS_NOT_PROPOSED_PAGE1 = {
  json: MOCK_AUDIT_RESULTS_IS_NOT_PROPOSED_CHANGES_DATA_ARRAY_PAGE1,
  ok: true,
  status: 200,
};

const MOCK_POPULATED_AUDITS_RESPONSE_IS_NOT_PROPOSED_PAGE2 = {
  json: MOCK_AUDIT_RESULTS_IS_NOT_PROPOSED_CHANGES_DATA_ARRAY_PAGE2,
  ok: true,
  status: 200,
};

const MOCK_EMPTY_AUDIT_RESULTS_RESPONSE = {
  json: MOCK_AUDIT_RESULTS_EMPTY_ARRAY,
  ok: true,
  status: 200,
};

function getContext() {
  return 'audit';
}

function getChangesContext() {
  return 'changes';
}

function getFilter() {
  return `filter=auditStatus:Inconsistent`;
}

function getChangesFilter() {
  return `filter=changeStatus:(${ChangeStatus.IMPLEMENTATION_IN_PROGRESS},${ChangeStatus.IMPLEMENTATION_COMPLETE},${ChangeStatus.IMPLEMENTATION_FAILED},${ChangeStatus.REVERSION_IN_PROGRESS},${ChangeStatus.REVERSION_COMPLETE},${ChangeStatus.REVERSION_FAILED})`;
}

function getTestTableColumns() {
  return [
    { title: 'Id', attribute: 'id', sortable: true, width: 'auto' },
    {
      title: 'Managed Object',
      attribute: 'managedObjectFdn',
      sortable: true,
      width: 'auto',
    },
    {
      title: 'MO Class',
      attribute: 'managedObjectType',
      sortable: true,
      width: 'auto',
    },
    {
      title: 'Attribute Name',
      attribute: 'attributeName',
      sortable: true,
      width: 'auto',
    },
    {
      title: 'Current Value',
      attribute: 'currentValue',
      sortable: true,
      width: 'auto',
    },
    {
      title: 'Preferred Value',
      attribute: 'preferredValue',
      sortable: true,
      width: 'auto',
    },
    {
      title: 'Audit Status',
      attribute: 'auditStatus',
      sortable: true,
      width: 'auto',
    },
    {
      title: 'Execution Id',
      attribute: 'executionId',
      sortable: true,
      width: 'auto',
    },
    { title: 'Rule Id', attribute: 'ruleId', sortable: true, width: 'auto' },
  ];
}

let fetchStub;

describe('Given: An <e-rest-paginated-table>', () => {
  before(() => {
    RestPaginatedTable.register('e-rest-paginated-table');
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

  describe(`When: A call is made to ${EXECUTIONS_AUDITS_API_INCONSISTENT} and an array of data is returned`, () => {
    it('Then: The UI displays a table with 3 entries on 1 page', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_INCONSISTENT))
        .resolves(MOCK_POPULATED_AUDITS_RESPONSE_IS_PROPOSED);

      const component = await fixture(
        html`<e-rest-paginated-table
          .executionId=${'3'}
          .columns="${getTestTableColumns()}"
          .context=${getContext()}
          .filter=${getFilter()}
        ></e-rest-paginated-table>`,
      );

      const auditTable = component.shadowRoot
        .querySelector('eui-table')
        .shadowRoot.querySelector('table');

      const tableHeaders = auditTable
        .querySelector('thead')
        .querySelector('tr:not(.filters)')
        .querySelectorAll('th');

      expect(getHeaderText(tableHeaders[1])).to.eq(LOCALE_EN_US.AUDIT_ID);
      expect(getHeaderText(tableHeaders[2])).to.eq(LOCALE_EN_US.MANAGED_OBJECT);
      expect(getHeaderText(tableHeaders[3])).to.eq(LOCALE_EN_US.MO_CLASS);
      expect(getHeaderText(tableHeaders[4])).to.eq(LOCALE_EN_US.ATTRIBUTE_NAME);
      expect(getHeaderText(tableHeaders[5])).to.eq(LOCALE_EN_US.CURRENT_VALUE);
      expect(getHeaderText(tableHeaders[6])).to.eq(
        LOCALE_EN_US.PREFERRED_VALUE,
      );
      expect(getHeaderText(tableHeaders[7])).to.eq(LOCALE_EN_US.AUDIT_STATUS);
      expect(getHeaderText(tableHeaders[8])).to.eq(LOCALE_EN_US.EXECUTION_ID);
      expect(getHeaderText(tableHeaders[9])).to.eq(LOCALE_EN_US.RULE_ID);

      const tableData = auditTable
        .querySelector('tbody')
        .querySelectorAll('tr');
      expect(tableData.length).to.equal(3);

      const pagination = component.shadowRoot.querySelector('eui-pagination');
      expect(pagination.getAttribute('current-page')).to.eq('1');
      expect(pagination.getAttribute('num-entries')).to.eq('2000');
      expect(pagination.getAttribute('num-pages')).to.eq('1');

      const totalElementsEvents = [];

      component.addEventListener(
        'e-rest-paginated-table:audit:total-elements',
        ev => {
          totalElementsEvents.push(ev.detail);
        },
      );

      let receivedSelectedRowEvent = false;
      component.addEventListener(
        'e-rest-paginated-table:audit:row-selected',
        () => {
          receivedSelectedRowEvent = true;
        },
      );

      simulateEvent(tableData[0], 'click');
      component._updateTableData();
      await nextFrame();

      expect(totalElementsEvents).to.be.not.empty;
      expect(totalElementsEvents[0]).to.equal(3);
      expect(receivedSelectedRowEvent).to.be.true;

      // verify didDisconnect emits the total-elements event
      component.didDisconnect();

      expect(totalElementsEvents[1]).to.equal(0);
    });
  });
  describe(`When: A call is made to ${EXECUTIONS_AUDITS_API_INCONSISTENT} and an empty array of data is returned`, () => {
    it('Then: The UI displays no changes implemented message', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_INCONSISTENT))
        .resolves(MOCK_EMPTY_AUDIT_RESULTS_RESPONSE);

      const component = await fixture(
        html`<e-rest-paginated-table
          .executionId=${'3'}
          .columns="${getTestTableColumns()}"
          .filter=${getFilter()}
        ></e-rest-paginated-table>`,
      );

      const auditTable = component.shadowRoot
        .querySelector('eui-table')
        .shadowRoot.querySelector('table');

      const tableData = auditTable
        .querySelector('tbody')
        .querySelectorAll('tr');
      expect(tableData.length).to.equal(0);

      const pagination = component.shadowRoot.querySelector('eui-pagination');
      expect(pagination.getAttribute('current-page')).to.eq('1');
      expect(pagination.getAttribute('num-entries')).to.eq('2000');
      expect(pagination.getAttribute('num-pages')).to.eq('0');
    });
  });

  describe(`When: A call is made to ${EXECUTIONS_AUDITS_API_FOR_CHANGES} and an empty array of data is returned`, () => {
    it('Then: The UI displays no changes implemented message', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_FOR_CHANGES))
        .resolves(MOCK_EMPTY_AUDIT_RESULTS_RESPONSE);

      const component = await fixture(
        html`<e-rest-paginated-table
          .executionId=${'3'}
          .columns="${getTestTableColumns()}"
          .context=${getChangesContext()}
          .filter=${getChangesFilter()}
        ></e-rest-paginated-table>`,
      );

      const emptyStateTitle =
        component.shadowRoot.querySelector('#emptyChangesTitle');
      expect(emptyStateTitle.textContent.trim()).to.equal(
        LOCALE_EN_US.ZERO_CHANGES_IMPLEMENTED,
      );

      const emptyStateSubtitle = component.shadowRoot.querySelector(
        '#emptyChangesSubtitle',
      );
      expect(emptyStateSubtitle.textContent.trim()).to.equal(
        LOCALE_EN_US.ZERO_CHANGES_IMPLEMENTED_SUBTITLE,
      );
    });
  });
  describe(`When: A call is made to ${EXECUTIONS_AUDITS_API_INCONSISTENT} and the table is filterable and an empty array of data is returned`, () => {
    it('Then: The UI displays the table headers and the empty result text', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_INCONSISTENT))
        .resolves(MOCK_EMPTY_AUDIT_RESULTS_RESPONSE);

      const component = await fixture(
        html`<e-rest-paginated-table
          .executionId=${'3'}
          .columns="${getTestTableColumns()}"
          .context=${getContext()}
          .filter=${getFilter()}
          filterable="true"
        ></e-rest-paginated-table>`,
      );

      const noDataMessage = component.shadowRoot.querySelector('.no-data-msg');
      expect(noDataMessage.textContent.trim()).to.contain(
        LOCALE_EN_US.NO_RESULTS_FOUND_FOR_APPLIED_FILTERS,
      );
      const subText = noDataMessage.querySelector('.no-data-msg-sub-text');
      expect(subText.textContent.trim()).to.equal(
        LOCALE_EN_US.EDIT_OR_CLEAR_FILTERS,
      );
    });
  });

  describe(`When: A call is made to ${EXECUTIONS_AUDITS_API_PAGE1} and an array of data is returned`, () => {
    it('Then: The UI displays a table with 2 pages ', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_PAGE1))
        .resolves(MOCK_POPULATED_AUDITS_RESPONSE_IS_NOT_PROPOSED_PAGE1);

      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_PAGE2))
        .resolves(MOCK_POPULATED_AUDITS_RESPONSE_IS_NOT_PROPOSED_PAGE2);

      const component = await fixture(
        html`<e-rest-paginated-table
          .executionId=${'3'}
          .context=${getContext()}
          .filter=${''}
          .columns="${getTestTableColumns()}"
        ></e-rest-paginated-table>`,
      );

      const auditTable = component.shadowRoot
        .querySelector('eui-table')
        .shadowRoot.querySelector('table');

      const tableHeaders = auditTable
        .querySelector('thead')
        .querySelector('tr:not(.filters)')
        .querySelectorAll('th');

      expect(getHeaderText(tableHeaders[1])).to.eq(LOCALE_EN_US.AUDIT_ID);
      expect(getHeaderText(tableHeaders[2])).to.eq(LOCALE_EN_US.MANAGED_OBJECT);
      expect(getHeaderText(tableHeaders[3])).to.eq(LOCALE_EN_US.MO_CLASS);
      expect(getHeaderText(tableHeaders[4])).to.eq(LOCALE_EN_US.ATTRIBUTE_NAME);
      expect(getHeaderText(tableHeaders[5])).to.eq(LOCALE_EN_US.CURRENT_VALUE);
      expect(getHeaderText(tableHeaders[6])).to.eq(
        LOCALE_EN_US.PREFERRED_VALUE,
      );
      expect(getHeaderText(tableHeaders[7])).to.eq(LOCALE_EN_US.AUDIT_STATUS);
      expect(getHeaderText(tableHeaders[8])).to.eq(LOCALE_EN_US.EXECUTION_ID);
      expect(getHeaderText(tableHeaders[9])).to.eq(LOCALE_EN_US.RULE_ID);

      let tableData = auditTable.querySelector('tbody').querySelectorAll('tr');
      expect(tableData.length).to.equal(3);

      const pagination = component.shadowRoot.querySelector('eui-pagination');
      expect(pagination.getAttribute('current-page')).to.eq('1');
      expect(pagination.getAttribute('num-entries')).to.eq('2000');
      expect(pagination.getAttribute('num-pages')).to.eq('2');

      const totalElementsEvents = [];

      component.addEventListener(
        'e-rest-paginated-table:audit:total-elements',
        ev => {
          totalElementsEvents.push(ev.detail);
        },
      );

      component._updateTableData();
      await nextFrame();

      expect(totalElementsEvents).to.be.not.empty;
      expect(totalElementsEvents[0]).to.equal(4);

      // Goto Page 2
      pagination.shadowRoot
        .querySelector('ul.pagination')
        .querySelector('li[data-value="2"]')
        .click();
      await nextFrame();
      tableData = auditTable.querySelector('tbody').querySelectorAll('tr');

      expect(tableData.length).to.equal(1);

      // And back to Page 1
      pagination.shadowRoot
        .querySelector('ul.pagination')
        .querySelector('li[data-value="1"]')
        .click();
      await nextFrame();
      tableData = auditTable.querySelector('tbody').querySelectorAll('tr');

      expect(tableData.length).to.equal(3);
    });
  });

  describe(`When: A call is made to ${EXECUTIONS_AUDITS_API_INCONSISTENT} and a 404 response is returned`, () => {
    it('Then: The UI displays the initialization error component', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_INCONSISTENT))
        .resolves(MOCK_NOT_FOUND_RESPONSE);

      const component = await fixture(
        html`<e-rest-paginated-table
          .executionId=${'3'}
          .context=${getContext()}
          .filter=${getFilter()}
          .columns="${getTestTableColumns()}"
        ></e-rest-paginated-table>`,
      );
      const initializationComponent = component.shadowRoot.querySelector(
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

  describe(`When: A call is made to ${EXECUTIONS_AUDITS_API_INCONSISTENT} and a 500 response is returned`, () => {
    it('Then: The UI displays the initialization error component', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_INCONSISTENT))
        .resolves(MOCK_INTERNAL_SERVER_ERROR_RESPONSE);

      const component = await fixture(
        html`<e-rest-paginated-table
          .executionId=${'3'}
          .context=${getChangesContext()}
          .filter=${getFilter()}
          .columns="${getTestTableColumns()}"
        ></e-rest-paginated-table>`,
      );
      const initializationComponent = component.shadowRoot.querySelector(
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
});
