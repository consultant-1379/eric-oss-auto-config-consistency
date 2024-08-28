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

import { expect, fixture, nextFrame, html } from '@open-wc/testing';
import sinon from 'sinon';
import Scope from '../../../../src/components/scope/scope.js';

/* eslint-disable import/no-named-default */
import { default as LOCALE_EN_US } from '../../../../src/components/scope/locale/en-us.json';
import { default as INIT_ERROR } from '../../../../src/components/initialization-error/locale/en-us.json';
import {
  MOCK_SCOPES_DATA_ARRAY,
  MOCK_EMPTY_ARRAY,
} from '../../../resources/mockData.js';
import {
  MOCK_DELETE_SUCCESS_RESPONSE,
  MOCK_BAD_REQUEST_RESPONSE,
  MOCK_NOT_FOUND_RESPONSE,
  MOCK_CONFLICT_RESPONSE,
  MOCK_ACCESS_DENIED_RESPONSE,
} from '../../../resources/mockResponses.js';
import {
  getHeaderText,
  getColumnText,
  simulateEvent,
  resetNotifications,
  triggerDeleteNotification,
} from '../../../resources/elementUtils.js';
import { updateLocaleString } from '../../../../src/utils/localization/localeUtils.js';

const UNIT_TEST_NODE_SET_ID = '1';
const UNIT_TEST_NODE_SET_NAME = 'athlone';
const SCOPES_API = '/v1/scopes';
const NODESETS_ZERO_MSG = 'Node sets (0)';
const NODESETS_TWO_MSG = 'Node sets (2)';
const DELETE_SCOPE_API = `${SCOPES_API}/${UNIT_TEST_NODE_SET_ID}`;
const SCOPE = 'scope';

const COMBINED_LOCALE = { ...LOCALE_EN_US, ...INIT_ERROR };

const MOCK_LOCALE_RESPONSE = {
  json: () => COMBINED_LOCALE,
  ok: true,
  status: 200,
};

const MOCK_EMPTY_SCOPES_RESPONSE = {
  json: MOCK_EMPTY_ARRAY,
  ok: true,
  status: 200,
};

const MOCK_SUCCESSFUL_SCOPES_RESPONSE = {
  json: MOCK_SCOPES_DATA_ARRAY,
  ok: true,
  status: 200,
};

let fetchStub;

describe('Given: An <e-scope>', () => {
  before(() => {
    Scope.register('e-scope');
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

  afterEach(() => {
    resetNotifications();
  });

  describe(`When: A call is made to ${SCOPES_API} and an empty array is returned`, () => {
    it(`Then: The UI displays a tab with ${NODESETS_ZERO_MSG} and the message "${LOCALE_EN_US.NO_NODESETS_CREATED}"`, async () => {
      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_EMPTY_SCOPES_RESPONSE);

      const scopeComponent = (await fixture('<e-scope></e-scope>')).shadowRoot;

      const tabAreacontent = scopeComponent.querySelector('.app-header');
      expect(tabAreacontent.textContent.trim()).to.eq(NODESETS_ZERO_MSG);

      const tabAreaContentDescription = scopeComponent.querySelector(
        '.app-empty-state > p',
      );
      expect(tabAreaContentDescription.textContent.trim()).to.eq(
        LOCALE_EN_US.NO_NODESETS_CREATED,
      );
    });
  });

  describe(`When: A call is made to ${SCOPES_API} and an unknown error occurs`, () => {
    it(`Then: The UI displays the Initialisation Error`, async () => {
      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(Promise.reject(new Error('Unable to contact server.')));

      const scopeComponent = (await fixture('<e-scope></e-scope>')).shadowRoot;

      const initializationComponent = scopeComponent.querySelector(
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

  describe(`When: A call is made to ${SCOPES_API} and an array of data is returned`, () => {
    it(`Then: The UI displays a tab with "${NODESETS_TWO_MSG}"`, async () => {
      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_SUCCESSFUL_SCOPES_RESPONSE);
      const scopeComponent = await fixture(html`<e-scope></e-scope>`);
      const appHeaderButtons =
        scopeComponent.shadowRoot.querySelector('.action-buttons');
      const createButtonSpan = appHeaderButtons
        .querySelector('.app-create-button')
        .querySelector('span');
      const modifyButtonSpan = appHeaderButtons
        .querySelector('.app-modify-buttons')
        .querySelector('span');
      const createButton = createButtonSpan.querySelector('eui-button');
      expect(createButton.textContent.trim()).to.equal(
        `${LOCALE_EN_US.CREATE_NODE_SET}`,
      );

      const modifyButtons = modifyButtonSpan.querySelectorAll('eui-button');
      expect(modifyButtons[0].textContent.trim()).to.equal(
        `${LOCALE_EN_US.EDIT}`,
      );
      expect(modifyButtons[1].textContent.trim()).to.equal(
        `${LOCALE_EN_US.DELETE}`,
      );

      expect(createButtonSpan.hasAttribute('hidden')).to.be.false;
      expect(modifyButtonSpan.hasAttribute('hidden')).to.be.true;

      const scopeStateHeader =
        scopeComponent.shadowRoot.querySelector('.app-header > span');
      expect(scopeStateHeader.textContent.trim()).to.equal(NODESETS_TWO_MSG);

      const scopeTable = scopeComponent.shadowRoot
        .querySelector('.app-table-container')
        .querySelector('eui-table');

      const tableHeaders = scopeTable.shadowRoot
        .querySelector('thead')
        .querySelector('tr:not(.filters)')
        .querySelectorAll('th');

      expect(tableHeaders.length).to.equal(2);
      expect(getHeaderText(tableHeaders[0])).to.equal(LOCALE_EN_US.SCOPE_NAME);
      expect(getHeaderText(tableHeaders[1])).to.equal('');

      const tableData = scopeTable.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');

      expect(tableData.length).to.equal(2);
      const rowOneDownloadButton = tableData[0].querySelector(
        'eui-actionable-icon',
      );
      expect(rowOneDownloadButton.hasAttribute('hidden')).to.be.true;
      simulateEvent(tableData[0], 'click');
      await nextFrame();
      expect(rowOneDownloadButton.hasAttribute('hidden')).to.be.false;
      expect(createButtonSpan.hasAttribute('hidden')).to.be.true;
      expect(modifyButtonSpan.hasAttribute('hidden')).to.be.false;
    });
  });

  describe("When: The 'Delete' button is clicked", () => {
    it('Then: The delete dialog is shown', async () => {
      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_SUCCESSFUL_SCOPES_RESPONSE);
      const scopeComponent = await fixture(html`<e-scope></e-scope>`);

      const scopeTable = scopeComponent.shadowRoot
        .querySelector('.app-table-container')
        .querySelector('eui-table');

      const tableData = scopeTable.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');

      simulateEvent(tableData[0], 'click');
      await nextFrame();

      const deleteButton = scopeComponent.shadowRoot
        .querySelector('.action-buttons')
        .querySelector('#delete-scope');

      simulateEvent(deleteButton, 'click');
      await nextFrame();

      const deleteDialog =
        scopeComponent.shadowRoot.querySelector('eui-dialog');
      expect(deleteDialog.getAttribute('label')).to.equal(
        LOCALE_EN_US.CONFIRM_DELETE,
      );

      const dialogText = deleteDialog.querySelector('div');
      expect(dialogText.textContent).to.equal(
        LOCALE_EN_US.DELETE_NODESET_MESSAGE,
      );
    });
  });

  describe("When: 'Delete' on the dialog box is clicked and delete is successful", () => {
    it('Then: A successful notification is displayed and the selected entry is removed', async () => {
      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_SUCCESSFUL_SCOPES_RESPONSE);
      const scopeComponent = await fixture(html`<e-scope></e-scope>`);

      fetchStub
        .withArgs(sinon.match(DELETE_SCOPE_API))
        .resolves(MOCK_DELETE_SUCCESS_RESPONSE);

      await triggerDeleteNotification(scopeComponent, SCOPE);

      const notification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(notification.textContent.trim()).to.equal(
        `${LOCALE_EN_US.NODE_SET} '${UNIT_TEST_NODE_SET_NAME}' ${LOCALE_EN_US.DELETED_SUCCESS_MESSAGE}`,
      );

      const statusIcon = notification.querySelector('eui-icon');

      expect(statusIcon.getAttribute('name')).to.equal('check');
      expect(statusIcon.getAttribute('color')).to.equal('var(--green-35)');

      await nextFrame();

      const scopeTable = scopeComponent.shadowRoot
        .querySelector('.app-table-container')
        .querySelector('eui-table');

      const tableData = scopeTable.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');

      expect(tableData.length).to.equal(1);
    });
  });

  describe(`When: A call is made to ${DELETE_SCOPE_API} and an error occurs`, () => {
    it('Then: A failure notification is displayed with a generic description', async () => {
      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_SUCCESSFUL_SCOPES_RESPONSE);
      const scopeComponent = await fixture(html`<e-scope></e-scope>`);

      fetchStub
        .withArgs(sinon.match(DELETE_SCOPE_API))
        .resolves(new Error('Unable to contact server.'));

      await triggerDeleteNotification(scopeComponent, SCOPE);

      const notification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.DELETED_FAILURE_MESSAGE,
      );
      expect(notification.description).to.equal(
        LOCALE_EN_US.DELETE_FAILURE_GENERIC_DESCRIPTION,
      );

      const statusIcon = notification.querySelector('eui-icon');

      expect(statusIcon.getAttribute('name')).to.equal('failed');
      expect(statusIcon.getAttribute('color')).to.equal('var(--red-52)');
    });
  });

  describe(`When: A call is made to ${DELETE_SCOPE_API} and a 400 response is returned`, () => {
    it('Then: A failure notification is displayed with a generic description', async () => {
      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_SUCCESSFUL_SCOPES_RESPONSE);
      const scopeComponent = await fixture(html`<e-scope></e-scope>`);

      fetchStub
        .withArgs(sinon.match(DELETE_SCOPE_API))
        .resolves(MOCK_BAD_REQUEST_RESPONSE);

      await triggerDeleteNotification(scopeComponent, SCOPE);

      const notification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.DELETED_FAILURE_MESSAGE,
      );
      expect(notification.description).to.equal(
        LOCALE_EN_US.DELETE_FAILURE_GENERIC_DESCRIPTION,
      );

      const statusIcon = notification.querySelector('eui-icon');

      expect(statusIcon.getAttribute('name')).to.equal('failed');
      expect(statusIcon.getAttribute('color')).to.equal('var(--red-52)');
    });
  });

  describe(`When: A call is made to ${DELETE_SCOPE_API} and a 404 response is returned`, () => {
    it('Then: A failure notification is displayed with a not found description', async () => {
      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_SUCCESSFUL_SCOPES_RESPONSE);
      const scopeComponent = await fixture(html`<e-scope></e-scope>`);

      fetchStub
        .withArgs(sinon.match(DELETE_SCOPE_API))
        .resolves(MOCK_NOT_FOUND_RESPONSE);

      await triggerDeleteNotification(scopeComponent, SCOPE);

      const notification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.DELETED_FAILURE_MESSAGE,
      );
      expect(notification.description).to.equal(
        LOCALE_EN_US.DELETE_FAILURE_404_DESCRIPTION,
      );

      const statusIcon = notification.querySelector('eui-icon');

      expect(statusIcon.getAttribute('name')).to.equal('failed');
      expect(statusIcon.getAttribute('color')).to.equal('var(--red-52)');
    });
  });

  describe(`When: A call is made to ${DELETE_SCOPE_API} and a 409 response is returned`, () => {
    it('Then: A failure notification is displayed with a conflict description', async () => {
      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_SUCCESSFUL_SCOPES_RESPONSE);
      const scopeComponent = await fixture(html`<e-scope></e-scope>`);

      const substitutions = new Map();
      substitutions.set('%node_set_name%', UNIT_TEST_NODE_SET_NAME);
      fetchStub
        .withArgs(sinon.match(DELETE_SCOPE_API))
        .resolves(MOCK_CONFLICT_RESPONSE);

      await triggerDeleteNotification(scopeComponent, SCOPE);

      const notification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.DELETED_FAILURE_MESSAGE,
      );
      expect(notification.description).to.equal(
        updateLocaleString(
          LOCALE_EN_US.DELETE_FAILURE_409_DESCRIPTION,
          substitutions,
        ),
      );

      const statusIcon = notification.querySelector('eui-icon');

      expect(statusIcon.getAttribute('name')).to.equal('failed');
      expect(statusIcon.getAttribute('color')).to.equal('var(--red-52)');
    });
  });

  describe(`When: The UI is launched the ${LOCALE_EN_US.CREATE_NODE_SET} button is visible`, () => {
    it(`Then: A click on ${LOCALE_EN_US.CREATE_NODE_SET} button opens a dialog`, async () => {
      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_SUCCESSFUL_SCOPES_RESPONSE);
      const scopeComponent = await fixture(html`<e-scope></e-scope>`);

      const appHeaderButtons =
        scopeComponent.shadowRoot.querySelector('.action-buttons');

      const createButtonSpan = appHeaderButtons
        .querySelector('.app-create-button')
        .querySelector('span');

      const createButton = createButtonSpan.querySelector('eui-button');
      expect(createButton.textContent.trim()).to.equal(
        LOCALE_EN_US.CREATE_NODE_SET,
      );

      const createScope =
        scopeComponent.shadowRoot.querySelector('e-create-scope');
      const euiDialog = createScope.shadowRoot.querySelector('eui-dialog');

      expect(euiDialog.hasAttribute('show')).to.be.false;

      simulateEvent(createButton, 'click');

      expect(euiDialog.hasAttribute('show')).to.be.true;
    });
  });

  describe(`When: A created node set event occurs`, () => {
    it(`Then: That node set goes to the top of the table`, async () => {
      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_EMPTY_SCOPES_RESPONSE);

      const scopeComponent = await fixture(html`<e-scope></e-scope>`);

      // No table visible at the moment, only "Node sets (0)"
      const appHeader = scopeComponent.shadowRoot.querySelector('.app-header');
      expect(appHeader.textContent.trim()).to.eq(NODESETS_ZERO_MSG);

      const newScopeName1 = 'scope_athlone-316';
      // Create a new event to simulate a scope has been created
      const event1 = new CustomEvent('e-create-scope:created', {
        detail: {
          id: '13aa6e40-a9e2-4fa2-9468-16c4797a5ca0',
          scopeName: newScopeName1,
          uri: 'v1/scopes/13aa6e40-a9e2-4fa2-9468-16c4797a5ca0',
        },
      });

      scopeComponent._handleScopeCreation(event1);

      await nextFrame();

      const scopeTable = scopeComponent.shadowRoot
        .querySelector('.app-table-container')
        .querySelector('eui-table');

      // The table should now have one row in it
      let tableData = scopeTable.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');
      expect(tableData.length).to.equal(1);

      expect(getColumnText(tableData[0].querySelectorAll('td')[0])).to.equal(
        newScopeName1,
      );

      // Create another event an ensure it goes to the top of the table
      const newScopeName2 = 'scope_athlone-619';
      const event2 = new CustomEvent('e-create-scope:created', {
        detail: {
          id: '13aa6e40-a9e2-4fa2-9468-16c4797a5ca0',
          scopeName: newScopeName2,
          uri: 'v1/scopes/13aa6e40-a9e2-4fa2-9468-16c4797a5ca0',
        },
      });

      scopeComponent._handleScopeCreation(event2);

      await nextFrame();

      tableData = scopeTable.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');
      expect(tableData.length).to.equal(2);

      expect(getColumnText(tableData[0].querySelectorAll('td')[0])).to.equal(
        newScopeName2,
      );
      expect(getColumnText(tableData[1].querySelectorAll('td')[0])).to.equal(
        newScopeName1,
      );
    });
  });

  describe(`When: A call is made to ${DELETE_SCOPE_API} and a 403 response is returned`, () => {
    it('Then: A failure notification is displayed with an access denied description', async () => {
      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_SUCCESSFUL_SCOPES_RESPONSE);
      const scopeComponent = await fixture(html`<e-scope></e-scope>`);

      const substitutions = new Map();
      substitutions.set('%node_set_name%', UNIT_TEST_NODE_SET_NAME);
      fetchStub
        .withArgs(sinon.match(DELETE_SCOPE_API))
        .resolves(MOCK_ACCESS_DENIED_RESPONSE);

      await triggerDeleteNotification(scopeComponent, SCOPE);

      const notification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.ACCESS_DENIED,
      );

      expect(notification.description).to.equal(
        updateLocaleString(LOCALE_EN_US.REST_403_DESCRIPTION, substitutions),
      );

      const statusIcon = notification.querySelector('eui-icon');

      expect(statusIcon.getAttribute('name')).to.equal('failed');
      expect(statusIcon.getAttribute('color')).to.equal('var(--red-52)');
    });
  });
});
