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

import { expect, fixture, nextFrame } from '@open-wc/testing';
import sinon from 'sinon';
import { html } from '@eui/lit-component';

import CreateRuleset from '../../../../src/components/create-ruleset/create-ruleset.js';
import * as ATTR from '../../../../src/utils/attributes/rulesetAttributes.js';

import {
  MOCK_RULESETS_CREATED_RULESETS,
  MOCK_RULESETS_INVALID_RULES_ARRAY,
  MOCK_RULESETS_INVALID_RULES_OBJECT,
  MOCK_RULESETS_NULL_VALUES,
  RULES,
} from '../../../resources/mockData.js';
import {
  MOCK_ACCESS_DENIED_RESPONSE,
  MOCK_CONFLICT_RESPONSE,
  MOCK_NOT_FOUND_RESPONSE,
  MOCK_PAYLOAD_TOO_LARGE_RESPONSE,
  MOCK_INTERNAL_SERVER_ERROR_RESPONSE,
} from '../../../resources/mockResponses.js';
import {
  resetNotifications,
  simulateEvent,
  simulateEventWithText,
  simulateFileEvent,
} from '../../../resources/elementUtils.js';

/* eslint-disable import/no-named-default */
import { default as LOCALE_EN_US } from '../../../../src/components/create-ruleset/locale/en-us.json';
import { updateLocaleString } from '../../../../src/utils/localization/localeUtils.js';
/* eslint-disable import/no-named-default */

const RULESETS_API = '/v1/rulesets';
const RULESET_NAME = 'ruleset-01';
const RULESET_ID = '13aa6e40-a9e2-4fa2-9468-16c4797a5ca0';
const RULESET_FILE_CSV_NAME = 'ruleset.csv';
const CSV_FILE_TYPE = 'text/csv';
const NO_ERROR_CONTENT = '';

const MOCK_LOCALE_RESPONSE = {
  json: () => LOCALE_EN_US,
  ok: true,
  status: 200,
};

const MOCK_SUCCESSFUL_CREATE_RULESET_RESPONSE = {
  json: MOCK_RULESETS_CREATED_RULESETS,
  ok: true,
  status: 201,
};

const MOCK_SUCCESSFUL_UPDATE_RULESET_RESPONSE = {
  json: MOCK_RULESETS_CREATED_RULESETS,
  ok: true,
  status: 200,
};

const MOCK_RULE_VALIDATION_ERROR_RESPONSE_ARRAY = {
  json: MOCK_RULESETS_INVALID_RULES_ARRAY,
  ok: false,
  status: 400,
};

const MOCK_RULE_VALIDATION_ERROR_RESPONSE_OBJECT = {
  json: MOCK_RULESETS_INVALID_RULES_OBJECT,
  ok: false,
  status: 400,
};

const MOCK_BAD_REQUEST_RESPONSE = {
  json: MOCK_RULESETS_NULL_VALUES,
  ok: false,
  status: 400,
};

let fetchStub;

const preShowChecks = (euiDialog, modifyRuleset = false) => {
  expect(euiDialog.hasAttribute('show')).to.be.false;
  const expectedLabel = modifyRuleset
    ? LOCALE_EN_US.EDIT_RULESET
    : LOCALE_EN_US.CREATE_RULESET;
  const dialogLabel = euiDialog.getAttribute('label');
  expect(dialogLabel).to.equal(expectedLabel);

  const rulesetFileNameField = euiDialog.querySelector(
    'eui-text-field#ruleset-file-name',
  );
  expect(rulesetFileNameField.getAttribute('placeholder')).to.equal(
    LOCALE_EN_US.NO_FILE_SELECTED,
  );
  expect(rulesetFileNameField.value).to.be.empty;

  const saveButton = euiDialog.querySelector('eui-button');
  expect(saveButton.hasAttribute('disabled')).to.be.true;
};

const setDialogValues = async (createRuleset, modifyRuleset = false) => {
  const euiDialog = createRuleset.shadowRoot.querySelector('eui-dialog');
  const saveButton = euiDialog.querySelector('eui-button');
  const rulesetFileNameField = euiDialog.querySelector(
    'eui-text-field#ruleset-file-name',
  );

  preShowChecks(euiDialog, modifyRuleset);

  // Show the dialog
  createRuleset.showDialog();
  expect(euiDialog.hasAttribute('show')).to.be.true;

  const rulesetNameField = euiDialog.querySelector(
    'eui-text-field#ruleset-name',
  );
  const existingRulesetName = euiDialog.querySelector('#existing-ruleset-name');

  if (modifyRuleset) {
    // ruleset name text field should be hidden
    expect(rulesetNameField.hasAttribute('hidden')).to.be.true;

    expect(existingRulesetName.hasAttribute('hidden')).to.be.false;
    expect(existingRulesetName.textContent.trim()).to.eq(RULESET_NAME);
  } else {
    // existing ruleset name should be hidden
    expect(existingRulesetName.hasAttribute('hidden')).to.be.true;
    simulateEventWithText(rulesetNameField, 'input', RULESET_NAME);
    // ruleset name should pass validation
    expect(rulesetNameField.getAttribute('custom-validation')).to.be.empty;
  }
  const fileInput = euiDialog.querySelector('eui-file-input#ruleset-file');

  const rulesetFile = new File([RULES], RULESET_FILE_CSV_NAME, {
    type: CSV_FILE_TYPE,
  });
  simulateFileEvent(fileInput, 'change', rulesetFile);

  expect(rulesetFileNameField.value).to.equal(RULESET_FILE_CSV_NAME);

  await nextFrame();

  expect(saveButton.hasAttribute('disabled')).to.be.false;

  simulateEvent(saveButton, 'click');

  await nextFrame();

  // The dialog should be close now
  expect(euiDialog.hasAttribute('show')).to.be.false;
};

const getNotification = () =>
  document
    .querySelector('#notifications-column')
    .querySelector('eui-notification');

describe('Given: An <e-create-ruleset>', () => {
  before(() => {
    CreateRuleset.register();
    fetchStub = sinon.stub(window, 'fetch');

    fetchStub.callsFake(url => {
      if (url.indexOf('locale') !== -1) {
        return Promise.resolve(MOCK_LOCALE_RESPONSE);
      }
      return Promise.resolve(true);
    });
  });

  afterEach(() => {
    resetNotifications();
  });

  describe('When: show is called with ruleset name and valid ruleset file', () => {
    it('Then: a ruleset is created and notification is displayed', async () => {
      const createRuleset = await fixture(
        '<e-create-ruleset></e-create-ruleset>',
      );
      fetchStub
        .withArgs(sinon.match(RULESETS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_SUCCESSFUL_CREATE_RULESET_RESPONSE);

      let receivedRulesetEvent = false;

      createRuleset.addEventListener('e-create-ruleset:created', ev => {
        const rulesetUri = `v1/rulesets/${RULESET_ID}`;
        expect(ev.detail[ATTR.RULESET_ID]).to.equal(RULESET_ID);
        expect(ev.detail[ATTR.RULESET_NAME]).to.equal(RULESET_NAME);
        expect(ev.detail[ATTR.RULESET_URI]).to.equal(rulesetUri);
        receivedRulesetEvent = true;
      });

      setDialogValues(createRuleset);

      await nextFrame();

      expect(receivedRulesetEvent).to.be.true;

      const notification = getNotification();
      // Verify notification was created with expected message and type
      expect(notification.textContent.trim()).to.equal(
        `Ruleset '${RULESET_NAME}' was created.`,
      );
      expect(
        notification.querySelector('eui-icon').getAttribute('name'),
      ).to.equal('check');
    });
  });

  describe('When: show is called with invalid ruleset name and valid ruleset file', () => {
    it('Then: a ruleset cannot be created due to inactive Save button', async () => {
      const createRuleset = await fixture(
        '<e-create-ruleset></e-create-ruleset>',
      );

      const euiDialog = createRuleset.shadowRoot.querySelector('eui-dialog');
      const saveButton = euiDialog.querySelector('eui-button');
      const rulesetFileNameField = euiDialog.querySelector(
        'eui-text-field#ruleset-file-name',
      );

      preShowChecks(euiDialog);

      // Show the dialog
      createRuleset.showDialog();
      expect(euiDialog.hasAttribute('show')).to.be.true;

      // Set the data fields
      const newRulesetName = 'ruleset +*';
      const rulesetNameField = euiDialog.querySelector(
        'eui-text-field#ruleset-name',
      );
      simulateEventWithText(rulesetNameField, 'input', newRulesetName);
      // ruleset name should not pass validation
      expect(rulesetNameField.getAttribute('custom-validation')).to.equal(
        LOCALE_EN_US.RULESET_NAME_PATTERN_WARNING,
      );

      const fileInput = euiDialog.querySelector('eui-file-input#ruleset-file');

      const rulesetFile = new File([RULES], RULESET_FILE_CSV_NAME, {
        type: CSV_FILE_TYPE,
      });
      simulateFileEvent(fileInput, 'change', rulesetFile);

      expect(rulesetFileNameField.value).to.equal(RULESET_FILE_CSV_NAME);

      await nextFrame();

      expect(saveButton.hasAttribute('disabled')).to.be.true;
    });
  });

  describe('When: show is called with an existing ruleset name and valid ruleset file', () => {
    it('Then: a ruleset cannot be created due to inactive Save button', async () => {
      const existingRulesetNames = [RULESET_NAME];
      const createRuleset = await fixture(
        html`<e-create-ruleset
          .existingRulesetNames="${existingRulesetNames}"
        ></e-create-ruleset>`,
      );

      const euiDialog = createRuleset.shadowRoot.querySelector('eui-dialog');
      const saveButton = euiDialog.querySelector('eui-button');
      const rulesetFileNameField = euiDialog.querySelector(
        'eui-text-field#ruleset-file-name',
      );

      preShowChecks(euiDialog);

      // Show the dialog
      createRuleset.showDialog();
      expect(euiDialog.hasAttribute('show')).to.be.true;

      // Set the data fields
      const rulesetNameField = euiDialog.querySelector(
        'eui-text-field#ruleset-name',
      );
      simulateEventWithText(rulesetNameField, 'input', RULESET_NAME);
      // ruleset name should not pass validation
      expect(rulesetNameField.getAttribute('custom-validation')).to.equal(
        LOCALE_EN_US.RULESET_NAME_EXISTS_WARNING,
      );

      const fileInput = euiDialog.querySelector('eui-file-input#ruleset-file');

      const rulesetFile = new File([RULES], RULESET_FILE_CSV_NAME, {
        type: CSV_FILE_TYPE,
      });
      simulateFileEvent(fileInput, 'change', rulesetFile);

      expect(rulesetFileNameField.value).to.equal(RULESET_FILE_CSV_NAME);

      await nextFrame();

      expect(saveButton.hasAttribute('disabled')).to.be.true;
    });
  });

  describe('When: show is called with a valid ruleset name and a ruleset file that exceeds the max file size', () => {
    it('Then: a ruleset cannot be created due to inactive Save button', async () => {
      const existsRulesetName = 'ruleset_04';
      const newRulesetName = 'ruleset-05';
      const existingRulesetNames = [existsRulesetName];
      const createRuleset = await fixture(
        html`<e-create-ruleset
          .existingRulesetNames="${existingRulesetNames}"
        ></e-create-ruleset>`,
      );

      const euiDialog = createRuleset.shadowRoot.querySelector('eui-dialog');
      const saveButton = euiDialog.querySelector('eui-button');
      const rulesetFileNameField = euiDialog.querySelector(
        'eui-text-field#ruleset-file-name',
      );

      preShowChecks(euiDialog);

      // Show the dialog
      createRuleset.showDialog();
      expect(euiDialog.hasAttribute('show')).to.be.true;

      // Set the data fields
      const rulesetNameField = euiDialog.querySelector(
        'eui-text-field#ruleset-name',
      );
      simulateEventWithText(rulesetNameField, 'input', newRulesetName);

      expect(rulesetNameField.getAttribute('custom-validation')).to.equal(
        NO_ERROR_CONTENT,
      );

      const fileInput = euiDialog.querySelector('eui-file-input#ruleset-file');

      const rulesetFile = new File([RULES], RULESET_FILE_CSV_NAME, {
        type: CSV_FILE_TYPE,
      });
      Object.defineProperty(rulesetFile, 'size', { value: 21577375 });
      simulateFileEvent(fileInput, 'change', rulesetFile);

      expect(rulesetFileNameField.value).to.equal(RULESET_FILE_CSV_NAME);
      expect(rulesetFileNameField.getAttribute('custom-validation')).to.equal(
        LOCALE_EN_US.RULESET_FILE_TOO_LARGE,
      );

      await nextFrame();

      expect(saveButton.hasAttribute('disabled')).to.be.true;
    });
  });

  describe('When: show is called with ruleset name and ruleset file and the backend returns 400', () => {
    it('Then: a ruleset is not created and notification is displayed', async () => {
      const createRuleset = await fixture(
        '<e-create-ruleset></e-create-ruleset>',
      );

      fetchStub
        .withArgs(sinon.match(RULESETS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_BAD_REQUEST_RESPONSE);

      setDialogValues(createRuleset);

      await nextFrame();

      const notification = getNotification();

      // Verify notification was created with expected message and type
      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.RULESET_CREATION_FAILURE_SHORT_DESCRIPTION,
      );
      expect(notification.getAttribute('description')).to.equal(
        LOCALE_EN_US.REST_RESPONSE_400_BAD_REQUEST,
      );
      expect(
        notification.querySelector('eui-icon').getAttribute('name'),
      ).to.equal('failed');
    });
  });

  describe('When: client receives a HTTP response with status code 400 and a body with rule validation errors', () => {
    it('Then: a ruleset is not created and notification is displayed', async () => {
      const createRuleset = await fixture(
        '<e-create-ruleset></e-create-ruleset>',
      );

      fetchStub
        .withArgs(sinon.match(RULESETS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_RULE_VALIDATION_ERROR_RESPONSE_ARRAY);

      setDialogValues(createRuleset);

      await nextFrame();

      const ruleValidationDialog = createRuleset.shadowRoot.querySelector(
        '#ruleValidationDialog',
      );
      const header = ruleValidationDialog.querySelector('p');
      const accordion = ruleValidationDialog.querySelector('eui-accordion');
      const settingsTable = accordion.querySelector(
        'e-settings-error-table',
      ).shadowRoot;
      const tableHeaders = settingsTable.querySelectorAll('th');
      const tableCells = settingsTable.querySelectorAll('td');

      expect(ruleValidationDialog.hasAttribute('show')).to.be.true;
      expect(ruleValidationDialog.getAttribute('label')).to.equal(
        LOCALE_EN_US.RULESET_CREATION_FAILURE_SHORT_DESCRIPTION,
      );
      expect(ruleValidationDialog.hasAttribute('no-cancel')).to.be.true;
      expect(header.textContent).to.equal(
        LOCALE_EN_US.RULESET_HAS_INVALID_RULES,
      );
      expect(accordion.getAttribute('category-title')).to.equal(
        `${LOCALE_EN_US.RULE_VALIDATION_ERRORS} (1)`,
      );
      expect(tableHeaders[0].textContent.trim()).to.contain('Line');
      expect(tableHeaders[1].textContent.trim()).to.contain('Reason');

      expect(tableCells[0].textContent.trim()).to.equal('2');
      expect(tableCells[1].textContent.trim()).to.equal(
        "MoType not specified, Attributes must be specified as '<MO Type>.<Attribute Name>'",
      );

      expect(createRuleset.ruleValidationErrors.length).to.equal(1);
      const okButton = ruleValidationDialog.querySelector('eui-button');
      simulateEvent(okButton, 'click');
      expect(createRuleset.ruleValidationErrors.length).to.equal(0);
    });
  });

  describe('When: show is called with ruleset name and ruleset file and the backend returns 413', () => {
    it('Then: a ruleset is not created and notification is displayed', async () => {
      const createRuleset = await fixture(
        '<e-create-ruleset></e-create-ruleset>',
      );

      fetchStub
        .withArgs(sinon.match(RULESETS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_PAYLOAD_TOO_LARGE_RESPONSE);

      setDialogValues(createRuleset);

      await nextFrame();

      const notification = getNotification();
      // Verify notification was created with expected message and type
      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.RULESET_CREATION_FAILURE_SHORT_DESCRIPTION,
      );
      expect(notification.getAttribute('description')).to.equal(
        LOCALE_EN_US.REST_RESPONSE_413_PAYLOAD_TOO_LARGE,
      );
      expect(
        notification.querySelector('eui-icon').getAttribute('name'),
      ).to.equal('failed');
    });
  });

  describe('When: show is called with ruleset name and ruleset file and the backend returns Internal Server Issue', () => {
    it('Then: a ruleset is not created and correct dialog is displayed', async () => {
      const createRuleset = await fixture(
        '<e-create-ruleset></e-create-ruleset>',
      );

      fetchStub
        .withArgs(sinon.match(RULESETS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_INTERNAL_SERVER_ERROR_RESPONSE);

      setDialogValues(createRuleset);

      await nextFrame();

      const notification = getNotification();
      // Verify notification was created with expected message and type
      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.RULESET_CREATION_FAILURE_SHORT_DESCRIPTION,
      );
      expect(
        notification.querySelector('eui-icon').getAttribute('name'),
      ).to.equal('failed');
      expect(notification.getAttribute('description')).to.equal(
        LOCALE_EN_US.RULESET_CREATION_FAILURE_LONG_DESCRIPTION,
      );
    });
  });

  describe('When: show is called with ruleset name and existing ruleset are being updated', () => {
    it('Then: the corresponding error are highlighted', async () => {
      const existingRulesetNames = [RULESET_NAME, 'ruleset-02'];
      const createRuleset = await fixture(
        html`<e-create-ruleset
          .existingRulesetNames="${existingRulesetNames}"
        ></e-create-ruleset>`,
      );

      const euiDialog = createRuleset.shadowRoot.querySelector('eui-dialog');

      preShowChecks(euiDialog);

      // Show the dialog
      createRuleset.showDialog();
      expect(euiDialog.hasAttribute('show')).to.be.true;

      // Set the data fields
      const rulesetNameField = euiDialog.querySelector(
        'eui-text-field#ruleset-name',
      );
      simulateEventWithText(rulesetNameField, 'input', RULESET_NAME);
      // ruleset name should pass validation
      expect(rulesetNameField.getAttribute('custom-validation')).to.equal(
        LOCALE_EN_US.RULESET_NAME_EXISTS_WARNING,
      );

      // Simulate a delete scenario
      existingRulesetNames.splice(0, 1);

      simulateEventWithText(rulesetNameField, 'input', RULESET_NAME);
      expect(rulesetNameField.getAttribute('custom-validation')).to.be.empty;
    });
  });

  describe('When: show is called and modify flag is true', () => {
    it('Then: a ruleset is updated and notification is displayed', async () => {
      const createRuleset = await fixture(
        html`<e-create-ruleset
          modify-ruleset="true"
          .modifyName=${RULESET_NAME}
          .modifyId=${RULESET_ID}
        ></e-create-ruleset>`,
      );

      fetchStub
        .withArgs(sinon.match(RULESETS_API), {
          method: 'PUT',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_SUCCESSFUL_UPDATE_RULESET_RESPONSE);

      setDialogValues(createRuleset, true);

      await nextFrame();

      const notification = getNotification();
      // Verify notification was created with expected message and type
      expect(notification.textContent.trim()).to.equal(
        `Ruleset '${RULESET_NAME}' was updated.`,
      );
      expect(
        notification.querySelector('eui-icon').getAttribute('name'),
      ).to.equal('check');
    });
  });

  describe('When: client receives a HTTP response with status code 400 and a body with rule validation errors', () => {
    it('Then: a ruleset is not updated and correct dialog is displayed', async () => {
      const createRuleset = await fixture(
        html`<e-create-ruleset
          modify-ruleset="true"
          .modifyName=${RULESET_NAME}
          .modifyId=${RULESET_ID}
        ></e-create-ruleset>`,
      );

      fetchStub
        .withArgs(sinon.match(RULESETS_API), {
          method: 'PUT',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_RULE_VALIDATION_ERROR_RESPONSE_OBJECT);

      setDialogValues(createRuleset, true);

      await nextFrame();
      const ruleValidationDialog = createRuleset.shadowRoot.querySelector(
        '#ruleValidationDialog',
      );
      const header = ruleValidationDialog.querySelector('p');
      const accordion = ruleValidationDialog.querySelector('eui-accordion');
      const settingsTable = accordion.querySelector(
        'e-settings-error-table',
      ).shadowRoot;
      const tableHeaders = settingsTable.querySelectorAll('th');
      const tableCells = settingsTable.querySelectorAll('td');

      expect(ruleValidationDialog.hasAttribute('show')).to.be.true;
      expect(ruleValidationDialog.getAttribute('label')).to.equal(
        LOCALE_EN_US.RULESET_CREATION_FAILURE_SHORT_DESCRIPTION,
      );
      expect(ruleValidationDialog.hasAttribute('no-cancel')).to.be.true;
      expect(header.textContent).to.equal(
        LOCALE_EN_US.RULESET_HAS_INVALID_RULES,
      );
      expect(accordion.getAttribute('category-title')).to.equal(
        `${LOCALE_EN_US.RULE_VALIDATION_ERRORS} (1)`,
      );
      expect(tableHeaders[0].textContent.trim()).to.contain('Line');
      expect(tableHeaders[1].textContent.trim()).to.contain('Reason');

      expect(tableCells[0].textContent.trim()).to.equal('2');
      expect(tableCells[1].textContent.trim()).to.equal(
        'MO not found in Managed Object Model.',
      );
    });
  });

  describe('When: show is called and 400 response code is returned', () => {
    it('Then: a ruleset is not updated and notification is displayed', async () => {
      const createRuleset = await fixture(
        html`<e-create-ruleset
          modify-ruleset="true"
          .modifyName=${RULESET_NAME}
          .modifyId=${RULESET_ID}
        ></e-create-ruleset>`,
      );

      fetchStub
        .withArgs(sinon.match(RULESETS_API), {
          method: 'PUT',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_BAD_REQUEST_RESPONSE);

      setDialogValues(createRuleset, true);

      await nextFrame();

      const notification = getNotification();
      // Verify notification was created with expected message and type
      expect(notification.getAttribute('description')).to.equal(
        LOCALE_EN_US.REST_RESPONSE_400_BAD_REQUEST,
      );
      expect(
        notification.querySelector('eui-icon').getAttribute('name'),
      ).to.equal('failed');
    });
  });

  describe('When: show is called and 404 response code is returned', () => {
    it('Then: a ruleset is not updated and notification is displayed', async () => {
      const createRuleset = await fixture(
        html`<e-create-ruleset
          modify-ruleset="true"
          .modifyName=${RULESET_NAME}
          .modifyId=${RULESET_ID}
        ></e-create-ruleset>`,
      );

      fetchStub
        .withArgs(sinon.match(RULESETS_API), {
          method: 'PUT',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_NOT_FOUND_RESPONSE);

      setDialogValues(createRuleset, true);

      await nextFrame();

      const notification = getNotification();
      // Verify notification was created with expected message and type
      expect(notification.getAttribute('description')).to.equal(
        LOCALE_EN_US.REST_RESPONSE_404_NOT_FOUND,
      );
      expect(
        notification.querySelector('eui-icon').getAttribute('name'),
      ).to.equal('failed');
    });
  });

  describe('When: show is called and 409 response code is returned', () => {
    it('Then: a ruleset is not updated and notification is displayed', async () => {
      const createRuleset = await fixture(
        html`<e-create-ruleset
          modify-ruleset="true"
          .modifyName=${RULESET_NAME}
          .modifyId=${RULESET_ID}
        ></e-create-ruleset>`,
      );

      fetchStub
        .withArgs(sinon.match(RULESETS_API), {
          method: 'PUT',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_CONFLICT_RESPONSE);

      setDialogValues(createRuleset, true);

      await nextFrame();
      const substitutions = new Map();
      substitutions.set('%ruleset_name%', RULESET_NAME);
      const notification = getNotification();
      // Verify notification was created with expected message and type
      expect(notification.getAttribute('description')).to.equal(
        updateLocaleString(
          LOCALE_EN_US.REST_RESPONSE_409_CONFLICT,
          substitutions,
        ),
      );
      expect(
        notification.querySelector('eui-icon').getAttribute('name'),
      ).to.equal('failed');
    });
  });

  describe('When: show is called and 413 response code is returned', () => {
    it('Then: a ruleset is not updated and notification is displayed', async () => {
      const createRuleset = await fixture(
        html`<e-create-ruleset
          modify-ruleset="true"
          .modifyName=${RULESET_NAME}
          .modifyId=${RULESET_ID}
        ></e-create-ruleset>`,
      );

      fetchStub
        .withArgs(sinon.match(RULESETS_API), {
          method: 'PUT',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_PAYLOAD_TOO_LARGE_RESPONSE);

      setDialogValues(createRuleset, true);

      await nextFrame();

      const notification = getNotification();
      // Verify notification was created with expected message and type
      expect(notification.getAttribute('description')).to.equal(
        LOCALE_EN_US.REST_RESPONSE_413_PAYLOAD_TOO_LARGE,
      );
      expect(
        notification.querySelector('eui-icon').getAttribute('name'),
      ).to.equal('failed');
    });
  });

  describe('When: show is called and the backend returns Internal Server Issue', () => {
    it('Then: a ruleset is not updated and notification is displayed', async () => {
      const createRuleset = await fixture(
        html`<e-create-ruleset
          modify-ruleset="true"
          .modifyName=${RULESET_NAME}
          .modifyId=${RULESET_ID}
        ></e-create-ruleset>`,
      );
      fetchStub
        .withArgs(sinon.match(RULESETS_API), {
          method: 'PUT',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_INTERNAL_SERVER_ERROR_RESPONSE);

      setDialogValues(createRuleset, true);
      await nextFrame();

      const notification = getNotification();
      // Verify notification was created with expected message and type
      expect(notification.getAttribute('description')).to.equal(
        LOCALE_EN_US.RULESET_CREATION_FAILURE_LONG_DESCRIPTION,
      );
      expect(
        notification.querySelector('eui-icon').getAttribute('name'),
      ).to.equal('failed');
    });
  });

  describe('When: show is called and the backend returns 403', () => {
    it('Then: a ruleset is not created and notification is displayed', async () => {
      const createRuleset = await fixture(
        '<e-create-ruleset></e-create-ruleset>',
      );

      fetchStub
        .withArgs(sinon.match(RULESETS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_ACCESS_DENIED_RESPONSE);

      setDialogValues(createRuleset);

      await nextFrame();

      const notification = getNotification();
      // Verify notification was created with expected message and type
      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.ACCESS_DENIED,
      );

      expect(notification.getAttribute('description')).to.equal(
        LOCALE_EN_US.REST_403_DESCRIPTION,
      );
      expect(
        notification.querySelector('eui-icon').getAttribute('name'),
      ).to.equal('failed');
    });
  });

  describe('When: show is called and 403 response code is returned', () => {
    it('Then: a ruleset is not updated and notification is displayed', async () => {
      const createRuleset = await fixture(
        html`<e-create-ruleset
          modify-ruleset="true"
          .modifyName=${RULESET_NAME}
          .modifyId=${RULESET_ID}
        ></e-create-ruleset>`,
      );

      fetchStub
        .withArgs(sinon.match(RULESETS_API), {
          method: 'PUT',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_ACCESS_DENIED_RESPONSE);

      setDialogValues(createRuleset, true);

      await nextFrame();

      const notification = getNotification();

      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.ACCESS_DENIED,
      );
      // Verify notification was created with expected message and type
      expect(notification.getAttribute('description')).to.equal(
        LOCALE_EN_US.REST_403_DESCRIPTION,
      );
      expect(
        notification.querySelector('eui-icon').getAttribute('name'),
      ).to.equal('failed');
    });
  });
});
