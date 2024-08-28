/*
 * COPYRIGHT Ericsson 2023
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
import Ruleset from '../../../../src/components/ruleset/ruleset.js';

/* eslint-disable import/no-named-default */
import { default as LOCALE_EN_US } from '../../../../src/components/ruleset/locale/en-us.json';
import { default as INIT_ERROR } from '../../../../src/components/initialization-error/locale/en-us.json';
import {
  MOCK_RULESET_RESULTS_DATA_ARRAY,
  MOCK_EMPTY_ARRAY,
} from '../../../resources/mockData.js';
import {
  MOCK_NOT_FOUND_RESPONSE,
  MOCK_DELETE_SUCCESS_RESPONSE,
  MOCK_CONFLICT_RESPONSE,
  MOCK_BAD_REQUEST_RESPONSE,
  MOCK_ACCESS_DENIED_RESPONSE,
} from '../../../resources/mockResponses.js';
import {
  getHeaderText,
  simulateEvent,
  resetNotifications,
  triggerDeleteNotification,
} from '../../../resources/elementUtils.js';
import { updateLocaleString } from '../../../../src/utils/localization/localeUtils.js';

const UNIT_TEST_RULESET_ID = '1';
const UNIT_TEST_RULESET_NAME = 'ruleset1';
const RULESETS_API = '/v1/rulesets';
const RULESETS_ZERO_MSG = 'Rulesets (0)';
const RULESETS_TWO_MSG = 'Rulesets (2)';
const DELETE_RULESET_API = `${RULESETS_API}/${UNIT_TEST_RULESET_ID}`;
const RULESET = 'ruleset';

const COMBINED_LOCALE = { ...LOCALE_EN_US, ...INIT_ERROR };

const MOCK_LOCALE_RESPONSE = {
  json: () => COMBINED_LOCALE,
  ok: true,
  status: 200,
};

const MOCK_EMPTY_RULESETS_RESPONSE = {
  json: MOCK_EMPTY_ARRAY,
  ok: true,
  status: 200,
};

const MOCK_SUCCESSFUL_RULESETS_RESPONSE = {
  json: MOCK_RULESET_RESULTS_DATA_ARRAY,
  ok: true,
  status: 200,
};

let fetchStub;

describe('Given: An <e-ruleset>', () => {
  before(() => {
    Ruleset.register('e-ruleset');
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

  describe(`When: A call is made to ${RULESETS_API} and an empty array is returned`, () => {
    it(`Then: The UI displays a tab with ${RULESETS_ZERO_MSG} and the message "${LOCALE_EN_US.NO_RULESETS_CREATED}"`, async () => {
      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_EMPTY_RULESETS_RESPONSE);

      const rulesetComponent = (await fixture('<e-ruleset></e-ruleset>'))
        .shadowRoot;

      const tabAreacontent = rulesetComponent.querySelector('.app-header');
      expect(tabAreacontent.textContent.trim()).to.eq(RULESETS_ZERO_MSG);

      const tabAreaContentDescription = rulesetComponent.querySelector(
        '.app-empty-state > p',
      );

      expect(tabAreaContentDescription.textContent.trim()).to.eq(
        LOCALE_EN_US.NO_RULESETS_CREATED,
      );
    });
  });

  describe(`When: A call is made to ${RULESETS_API} and an unknown error occurs`, () => {
    it(`Then: The UI displays the Initialisation Error`, async () => {
      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(Promise.reject(new Error('Unable to contact server.')));

      const rulesetComponent = (await fixture('<e-ruleset></e-ruleset>'))
        .shadowRoot;

      const initializationComponent = rulesetComponent.querySelector(
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

  describe(`When: A call is made to ${RULESETS_API} and an array of data is returned`, () => {
    it(`Then: The UI displays a tab with "${RULESETS_TWO_MSG}"`, async () => {
      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_SUCCESSFUL_RULESETS_RESPONSE);

      const rulesetComponent = (await fixture('<e-ruleset></e-ruleset>'))
        .shadowRoot;

      const rulesetStateHeader =
        rulesetComponent.querySelector('.app-header > span');
      expect(rulesetStateHeader.textContent.trim()).to.equal(RULESETS_TWO_MSG);

      const createRulesetButton = rulesetComponent.querySelector(
        '.action-buttons > .app-create-button > span > eui-button',
      );

      expect(createRulesetButton.textContent.trim()).to.equal(
        `${COMBINED_LOCALE.CREATE_RULESET}`,
      );

      const rulesetTable = rulesetComponent
        .querySelector('.app-table-container')
        .querySelector('eui-table');

      const tableHeaders = rulesetTable.shadowRoot
        .querySelector('thead')
        .querySelector('tr:not(.filters)')
        .querySelectorAll('th');

      expect(tableHeaders.length).to.equal(2);
      expect(getHeaderText(tableHeaders[0])).to.equal(LOCALE_EN_US.NAME);

      const tableData = rulesetTable.shadowRoot
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
    });
  });

  describe("When: The 'Delete' button is clicked", () => {
    it('Then: The delete dialog is shown', async () => {
      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_SUCCESSFUL_RULESETS_RESPONSE);
      const rulesetComponent = await fixture(html`<e-ruleset></e-ruleset>`);

      const appHeaderButtons =
        rulesetComponent.shadowRoot.querySelector('.action-buttons');

      const modifyButtonSpan = appHeaderButtons
        .querySelector('.app-modify-buttons')
        .querySelectorAll('span');
      const deleteButtonSpan = modifyButtonSpan[1];
      const deleteButton = modifyButtonSpan[1].querySelector(
        'eui-button#delete-ruleset',
      );

      expect(deleteButton.textContent.trim()).to.equal(
        `${LOCALE_EN_US.DELETE}`,
      );
      expect(deleteButtonSpan.hasAttribute('hidden')).to.be.true;

      const rulesetTable = rulesetComponent.shadowRoot
        .querySelector('.app-table-container')
        .querySelector('eui-table');

      const tableData = rulesetTable.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');

      simulateEvent(tableData[0], 'click');
      await nextFrame();

      expect(modifyButtonSpan[1].hasAttribute('hidden')).to.be.false;

      simulateEvent(deleteButton, 'click');
      await nextFrame();

      const deleteDialog =
        rulesetComponent.shadowRoot.querySelector('eui-dialog');
      expect(deleteDialog.getAttribute('label')).to.equal(
        LOCALE_EN_US.CONFIRM_DELETE,
      );

      const dialogText = deleteDialog.querySelector('div');
      expect(dialogText.textContent).to.equal(
        LOCALE_EN_US.DELETE_RULESET_MESSAGE,
      );
    });
  });

  describe("When: 'Delete' on the dialog box is clicked and delete is successful", () => {
    it('Then: A successful notification is displayed and the selected entry is removed', async () => {
      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_SUCCESSFUL_RULESETS_RESPONSE);
      const rulesetComponent = await fixture(html`<e-ruleset></e-ruleset>`);

      fetchStub
        .withArgs(sinon.match(DELETE_RULESET_API))
        .resolves(MOCK_DELETE_SUCCESS_RESPONSE);

      await triggerDeleteNotification(rulesetComponent, RULESET);

      const notification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(notification.textContent.trim()).to.equal(
        `${LOCALE_EN_US.RULESET} '${UNIT_TEST_RULESET_NAME}' ${LOCALE_EN_US.DELETED_SUCCESS_MESSAGE}`,
      );

      const statusIcon = notification.querySelector('eui-icon');

      expect(statusIcon.getAttribute('name')).to.equal('check');
      expect(statusIcon.getAttribute('color')).to.equal('var(--green-35)');

      await nextFrame();

      const rulesetTable = rulesetComponent.shadowRoot
        .querySelector('.app-table-container')
        .querySelector('eui-table');

      const tableData = rulesetTable.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');

      expect(tableData.length).to.equal(1);
    });
  });

  describe(`When: A call is made to ${DELETE_RULESET_API} and an error occurs`, () => {
    it('Then: A failure notification is displayed with a generic description', async () => {
      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_SUCCESSFUL_RULESETS_RESPONSE);
      const rulesetComponent = await fixture(html`<e-ruleset></e-ruleset>`);

      fetchStub
        .withArgs(sinon.match(DELETE_RULESET_API))
        .resolves(new Error('Unable to contact server.'));

      await triggerDeleteNotification(rulesetComponent, RULESET);

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

  describe(`When: A call is made to ${DELETE_RULESET_API} and a 400 response is returned`, () => {
    it('Then: A failure notification is displayed with a generic description', async () => {
      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_SUCCESSFUL_RULESETS_RESPONSE);
      const rulesetComponent = await fixture(html`<e-ruleset></e-ruleset>`);

      fetchStub
        .withArgs(sinon.match(DELETE_RULESET_API))
        .resolves(MOCK_BAD_REQUEST_RESPONSE);

      await triggerDeleteNotification(rulesetComponent, RULESET);

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

  describe(`When: A call is made to ${DELETE_RULESET_API} and a 404 response is returned`, () => {
    it('Then: A failure notification is displayed with a not found description', async () => {
      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_SUCCESSFUL_RULESETS_RESPONSE);
      const rulesetComponent = await fixture(html`<e-ruleset></e-ruleset>`);

      fetchStub
        .withArgs(sinon.match(DELETE_RULESET_API))
        .resolves(MOCK_NOT_FOUND_RESPONSE);

      await triggerDeleteNotification(rulesetComponent, RULESET);

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

  describe(`When: A call is made to ${DELETE_RULESET_API} and a 409 response is returned`, () => {
    it('Then: A failure notification is displayed with a conflict description', async () => {
      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_SUCCESSFUL_RULESETS_RESPONSE);
      const rulesetComponent = await fixture(html`<e-ruleset></e-ruleset>`);

      const substitutions = new Map();
      substitutions.set('%ruleset_name%', UNIT_TEST_RULESET_NAME);
      fetchStub
        .withArgs(sinon.match(DELETE_RULESET_API))
        .resolves(MOCK_CONFLICT_RESPONSE);

      await triggerDeleteNotification(rulesetComponent, RULESET);

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

  describe(`When: A call is made to ${DELETE_RULESET_API} and a 403 response is returned`, () => {
    it('Then: A failure notification is displayed with an access denied description', async () => {
      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_SUCCESSFUL_RULESETS_RESPONSE);
      const rulesetComponent = await fixture(html`<e-ruleset></e-ruleset>`);

      const substitutions = new Map();
      substitutions.set('%ruleset_name%', UNIT_TEST_RULESET_NAME);
      fetchStub
        .withArgs(sinon.match(DELETE_RULESET_API))
        .resolves(MOCK_ACCESS_DENIED_RESPONSE);

      await triggerDeleteNotification(rulesetComponent, RULESET);

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
