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

import {
  resetNotifications,
  simulateEvent,
  simulateEventWithText,
  simulateFileEvent,
} from '../../../resources/elementUtils.js';

import {
  SCOPE_FDNS,
  MOCK_SCOPES_CREATED_SCOPE,
} from '../../../resources/mockData.js';
import {
  MOCK_ACCESS_DENIED_RESPONSE,
  MOCK_BAD_REQUEST_RESPONSE,
  MOCK_CONFLICT_RESPONSE,
  MOCK_NOT_FOUND_RESPONSE,
  MOCK_PAYLOAD_TOO_LARGE_RESPONSE,
  MOCK_INTERNAL_SERVER_ERROR_RESPONSE,
} from '../../../resources/mockResponses.js';
import * as ATTR from '../../../../src/utils/attributes/scopeAttributes.js';

import CreateScope from '../../../../src/components/create-scope/create-scope.js';

/* eslint-disable import/no-named-default */
import { default as LOCALE_EN_US } from '../../../../src/components/create-scope/locale/en-us.json';
/* eslint-disable import/no-named-default */

import { updateLocaleString } from '../../../../src/utils/localization/localeUtils.js';

const SCOPES_API = '/v1/scopes';
const SCOPE_NAME_ATHLONE = 'scope_athlone-01';
const SCOPE_ID = '13aa6e40-a9e2-4fa2-9468-16c4797a5ca0';
const SCOPE_FILE_CSV_NAME = 'scope.csv';
const CSV_FILE_TYPE = 'text/csv';

const MOCK_LOCALE_RESPONSE = {
  json: () => LOCALE_EN_US,
  ok: true,
  status: 200,
};

const MOCK_SUCCESSFUL_CREATE_SCOPE_RESPONSE = {
  json: MOCK_SCOPES_CREATED_SCOPE,
  ok: true,
  status: 201,
};

const MOCK_SUCCESSFUL_UPDATE_SCOPE_RESPONSE = {
  json: MOCK_SCOPES_CREATED_SCOPE,
  ok: true,
  status: 200,
};

let fetchStub;

const getNotification = () =>
  document
    .querySelector('#notifications-column')
    .querySelector('eui-notification');

const preShowChecks = (euiDialog, modifyScope = false) => {
  expect(euiDialog.hasAttribute('show')).to.be.false;
  const expectedLabel = modifyScope
    ? LOCALE_EN_US.EDIT_NODE_SET
    : LOCALE_EN_US.CREATE_NODE_SET;

  const dialogLabel = euiDialog.getAttribute('label');
  expect(dialogLabel).to.equal(expectedLabel);

  const scopeFileNameField = euiDialog.querySelector(
    'eui-text-field#scope-file-name',
  );
  expect(scopeFileNameField.getAttribute('placeholder')).to.equal(
    LOCALE_EN_US.NO_FILE_SELECTED,
  );
  expect(scopeFileNameField.value).to.be.empty;

  const saveButton = euiDialog.querySelector('eui-button');
  expect(saveButton.hasAttribute('disabled')).to.be.true;
};

const setDialogValues = async (createScope, modifyScope = false) => {
  const euiDialog = createScope.shadowRoot.querySelector('eui-dialog');
  const saveButton = euiDialog.querySelector('eui-button');
  const scopeFileNameField = euiDialog.querySelector(
    'eui-text-field#scope-file-name',
  );

  preShowChecks(euiDialog, modifyScope);

  // Show the dialog
  createScope.showDialog();
  expect(euiDialog.hasAttribute('show')).to.be.true;

  const scopeNameField = euiDialog.querySelector('eui-text-field#scope-name');
  const existingScopeName = euiDialog.querySelector('#existing-scope-name');

  if (modifyScope) {
    // scope name text field should be hidden
    expect(scopeNameField.hasAttribute('hidden')).to.be.true;

    expect(existingScopeName.hasAttribute('hidden')).to.be.false;
    expect(existingScopeName.textContent.trim()).to.eq(SCOPE_NAME_ATHLONE);
  } else {
    // existing scope name should be hidden
    expect(existingScopeName.hasAttribute('hidden')).to.be.true;
    simulateEventWithText(scopeNameField, 'input', SCOPE_NAME_ATHLONE);
    // scope name should pass validation
    expect(scopeNameField.getAttribute('custom-validation')).to.be.empty;
  }
  const fileInput = euiDialog.querySelector('eui-file-input#scope-file');

  const scopeFile = new File([SCOPE_FDNS], SCOPE_FILE_CSV_NAME, {
    type: CSV_FILE_TYPE,
  });
  simulateFileEvent(fileInput, 'change', scopeFile);

  expect(scopeFileNameField.value).to.equal(SCOPE_FILE_CSV_NAME);

  await nextFrame();

  expect(saveButton.hasAttribute('disabled')).to.be.false;

  simulateEvent(saveButton, 'click');

  await nextFrame();

  // The dialog should be close now
  expect(euiDialog.hasAttribute('show')).to.be.false;
};

describe('Given: An <e-create-scope>', () => {
  before(() => {
    CreateScope.register();
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

  describe('When: show is called with node set name and valid node set file', () => {
    it('Then: a scope is created and notification is displayed', async () => {
      const createScope = await fixture('<e-create-scope></e-create-scope>');

      fetchStub
        .withArgs(sinon.match(SCOPES_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_SUCCESSFUL_CREATE_SCOPE_RESPONSE);

      let receivedScopeEvent = false;

      createScope.addEventListener('e-create-scope:created', ev => {
        const scopeId = '13aa6e40-a9e2-4fa2-9468-16c4797a5ca0';
        const scopeUri = `v1/scopes/${scopeId}`;
        expect(ev.detail[ATTR.SCOPE_ID]).to.equal(scopeId);
        expect(ev.detail[ATTR.SCOPE_NAME]).to.equal(SCOPE_NAME_ATHLONE);
        expect(ev.detail[ATTR.SCOPE_URI]).to.equal(scopeUri);
        receivedScopeEvent = true;
      });

      setDialogValues(createScope);

      await nextFrame();

      expect(receivedScopeEvent).to.be.true;

      const notification = getNotification();
      // Verify notification was created with expected message and type
      expect(notification.textContent.trim()).to.equal(
        `Node set '${SCOPE_NAME_ATHLONE}' was created.`,
      );
      expect(
        notification.querySelector('eui-icon').getAttribute('name'),
      ).to.equal('check');
    });
  });

  describe('When: show is called with invalid node set name and valid node set file', () => {
    it('Then: a scope cannot be created due to inactive Save button', async () => {
      const createScope = await fixture('<e-create-scope></e-create-scope>');

      const euiDialog = createScope.shadowRoot.querySelector('eui-dialog');
      const saveButton = euiDialog.querySelector('eui-button');
      const scopeFileNameField = euiDialog.querySelector(
        'eui-text-field#scope-file-name',
      );

      preShowChecks(euiDialog);

      // Show the dialog
      createScope.showDialog();
      expect(euiDialog.hasAttribute('show')).to.be.true;

      // Set the data fields
      const newScopeName = 'scope_athlone-*+?';
      const scopeNameField = euiDialog.querySelector(
        'eui-text-field#scope-name',
      );
      simulateEventWithText(scopeNameField, 'input', newScopeName);
      // scope name should not pass validation
      expect(scopeNameField.getAttribute('custom-validation')).to.equal(
        LOCALE_EN_US.NODE_SET_NAME_PATTERN_WARNING,
      );

      const fileInput = euiDialog.querySelector('eui-file-input#scope-file');

      const scopeFile = new File([SCOPE_FDNS], SCOPE_FILE_CSV_NAME, {
        type: CSV_FILE_TYPE,
      });
      simulateFileEvent(fileInput, 'change', scopeFile);

      expect(scopeFileNameField.value).to.equal(SCOPE_FILE_CSV_NAME);

      await nextFrame();

      expect(saveButton.hasAttribute('disabled')).to.be.true;
    });
  });

  describe('When: show is called with an existing node set name and valid node set file', () => {
    it('Then: a scope cannot be created due to inactive Save button', async () => {
      const existingScopeNames = [SCOPE_NAME_ATHLONE];
      const createScope = await fixture(
        html`<e-create-scope
          .existingScopeNames="${existingScopeNames}"
        ></e-create-scope>`,
      );

      const euiDialog = createScope.shadowRoot.querySelector('eui-dialog');
      const saveButton = euiDialog.querySelector('eui-button');
      const scopeFileNameField = euiDialog.querySelector(
        'eui-text-field#scope-file-name',
      );

      preShowChecks(euiDialog);

      // Show the dialog
      createScope.showDialog();
      expect(euiDialog.hasAttribute('show')).to.be.true;

      // Set the data fields
      const scopeNameField = euiDialog.querySelector(
        'eui-text-field#scope-name',
      );
      simulateEventWithText(scopeNameField, 'input', SCOPE_NAME_ATHLONE);
      // scope name should not pass validation
      expect(scopeNameField.getAttribute('custom-validation')).to.equal(
        LOCALE_EN_US.NODE_SET_NAME_EXISTS_WARNING,
      );

      const fileInput = euiDialog.querySelector('eui-file-input#scope-file');

      const scopeFile = new File([SCOPE_FDNS], SCOPE_FILE_CSV_NAME, {
        type: CSV_FILE_TYPE,
      });
      simulateFileEvent(fileInput, 'change', scopeFile);

      expect(scopeFileNameField.value).to.equal(SCOPE_FILE_CSV_NAME);

      await nextFrame();

      expect(saveButton.hasAttribute('disabled')).to.be.true;
    });
  });

  describe('When: show is called with a valid node set name and a node set file that exceeds the max file size', () => {
    it('Then: a scope cannot be created due to inactive Save button', async () => {
      const newScopeName = 'westmeath_04';
      const existingScopeNames = [newScopeName];
      const createScope = await fixture(
        html`<e-create-scope
          .existingScopeNames="${existingScopeNames}"
        ></e-create-scope>`,
      );

      const euiDialog = createScope.shadowRoot.querySelector('eui-dialog');
      const saveButton = euiDialog.querySelector('eui-button');
      const scopeFileNameField = euiDialog.querySelector(
        'eui-text-field#scope-file-name',
      );

      preShowChecks(euiDialog);

      // Show the dialog
      createScope.showDialog();
      expect(euiDialog.hasAttribute('show')).to.be.true;

      // Set the data fields
      const scopeNameField = euiDialog.querySelector(
        'eui-text-field#scope-name',
      );
      simulateEventWithText(scopeNameField, 'input', newScopeName);
      // scope name should not pass validation
      expect(scopeNameField.getAttribute('custom-validation')).to.equal(
        LOCALE_EN_US.NODE_SET_NAME_EXISTS_WARNING,
      );

      const fileInput = euiDialog.querySelector('eui-file-input#scope-file');

      const scopeFile = new File([SCOPE_FDNS], SCOPE_FILE_CSV_NAME, {
        type: CSV_FILE_TYPE,
      });
      Object.defineProperty(scopeFile, 'size', { value: 21577375 });
      simulateFileEvent(fileInput, 'change', scopeFile);

      expect(scopeFileNameField.value).to.equal(SCOPE_FILE_CSV_NAME);
      expect(scopeFileNameField.getAttribute('custom-validation')).to.equal(
        LOCALE_EN_US.NODE_SET_FILE_TOO_LARGE,
      );

      await nextFrame();

      expect(saveButton.hasAttribute('disabled')).to.be.true;
    });
  });

  describe('When: show is called with node set name and node set file and the backend returns 400', () => {
    it('Then: a scope is not created and notification is displayed', async () => {
      const createScope = await fixture('<e-create-scope></e-create-scope>');

      fetchStub
        .withArgs(sinon.match(SCOPES_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_BAD_REQUEST_RESPONSE);

      setDialogValues(createScope);

      await nextFrame();

      const notification = getNotification();
      // Verify notification was created with expected message and type
      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.NODE_SET_CREATION_FAILURE_SHORT_DESCRIPTION,
      );
      expect(notification.getAttribute('description')).to.equal(
        LOCALE_EN_US.REST_RESPONSE_400_BAD_REQUEST,
      );
      expect(
        notification.querySelector('eui-icon').getAttribute('name'),
      ).to.equal('failed');
    });
  });

  describe('When: show is called with node set name and node set file and the backend returns 413', () => {
    it('Then: a scope is not created and notification is displayed', async () => {
      const createScope = await fixture('<e-create-scope></e-create-scope>');

      fetchStub
        .withArgs(sinon.match(SCOPES_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_PAYLOAD_TOO_LARGE_RESPONSE);

      setDialogValues(createScope);

      await nextFrame();

      const notification = getNotification();
      // Verify notification was created with expected message and type
      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.NODE_SET_CREATION_FAILURE_SHORT_DESCRIPTION,
      );
      expect(notification.getAttribute('description')).to.equal(
        LOCALE_EN_US.REST_RESPONSE_413_PAYLOAD_TOO_LARGE,
      );
      expect(
        notification.querySelector('eui-icon').getAttribute('name'),
      ).to.equal('failed');
    });
  });

  describe('When: show is called with node set name and node set file and the backend returns Internal Server Issue', () => {
    it('Then: a scope is not created and notification is displayed', async () => {
      const createScope = await fixture('<e-create-scope></e-create-scope>');

      fetchStub
        .withArgs(sinon.match(SCOPES_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_INTERNAL_SERVER_ERROR_RESPONSE);

      setDialogValues(createScope);

      await nextFrame();

      const notification = getNotification();
      // Verify notification was created with expected message and type
      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.NODE_SET_CREATION_FAILURE_SHORT_DESCRIPTION,
      );
      expect(
        notification.querySelector('eui-icon').getAttribute('name'),
      ).to.equal('failed');
      expect(notification.getAttribute('description')).to.equal(
        LOCALE_EN_US.NODE_SET_CREATION_FAILURE_LONG_DESCRIPTION,
      );
    });
  });

  describe('When: show is called with node set name and existing node sets are being updated', () => {
    it('Then: the corresponding error are highlighted', async () => {
      const existingScopeNames = [SCOPE_NAME_ATHLONE, 'scope_dublin-02'];
      const createScope = await fixture(
        html`<e-create-scope
          .existingScopeNames="${existingScopeNames}"
        ></e-create-scope>`,
      );

      const euiDialog = createScope.shadowRoot.querySelector('eui-dialog');

      preShowChecks(euiDialog);

      // Show the dialog
      createScope.showDialog();
      expect(euiDialog.hasAttribute('show')).to.be.true;

      // Set the data fields
      const scopeNameField = euiDialog.querySelector(
        'eui-text-field#scope-name',
      );
      simulateEventWithText(scopeNameField, 'input', SCOPE_NAME_ATHLONE);
      // scope name should pass validation
      expect(scopeNameField.getAttribute('custom-validation')).to.equal(
        LOCALE_EN_US.NODE_SET_NAME_EXISTS_WARNING,
      );

      // Simulate a delete scenario
      existingScopeNames.splice(0, 1);

      simulateEventWithText(scopeNameField, 'input', SCOPE_NAME_ATHLONE);
      expect(scopeNameField.getAttribute('custom-validation')).to.be.empty;
    });
  });

  describe('When: show is called and modify flag is true', () => {
    it('Then: a scope is updated and notification is displayed', async () => {
      const createScope = await fixture(
        html`<e-create-scope
          modify-scope="true"
          .modifyName=${SCOPE_NAME_ATHLONE}
          .modifyId=${SCOPE_ID}
        ></e-create-scope>`,
      );

      fetchStub
        .withArgs(sinon.match(SCOPES_API), {
          method: 'PUT',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_SUCCESSFUL_UPDATE_SCOPE_RESPONSE);

      setDialogValues(createScope, true);

      await nextFrame();

      const notification = getNotification();
      // Verify notification was created with expected message and type
      expect(notification.textContent.trim()).to.equal(
        `Node set '${SCOPE_NAME_ATHLONE}' was updated.`,
      );
      expect(
        notification.querySelector('eui-icon').getAttribute('name'),
      ).to.equal('check');
    });
  });

  describe('When: show is called and 400 response code is returned', () => {
    it('Then: a scope is not updated and notification is displayed', async () => {
      const createScope = await fixture(
        html`<e-create-scope
          modify-scope="true"
          .modifyName=${SCOPE_NAME_ATHLONE}
          .modifyId=${SCOPE_ID}
        ></e-create-scope>`,
      );

      fetchStub
        .withArgs(sinon.match(SCOPES_API), {
          method: 'PUT',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_BAD_REQUEST_RESPONSE);

      setDialogValues(createScope, true);

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
    it('Then: a scope is not updated and notification is displayed', async () => {
      const createScope = await fixture(
        html`<e-create-scope
          modify-scope="true"
          .modifyName=${SCOPE_NAME_ATHLONE}
          .modifyId=${SCOPE_ID}
        ></e-create-scope>`,
      );

      fetchStub
        .withArgs(sinon.match(SCOPES_API), {
          method: 'PUT',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_NOT_FOUND_RESPONSE);

      setDialogValues(createScope, true);

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
    it('Then: a scope is not updated and notification is displayed', async () => {
      const createScope = await fixture(
        html`<e-create-scope
          modify-scope="true"
          .modifyName=${SCOPE_NAME_ATHLONE}
          .modifyId=${SCOPE_ID}
        ></e-create-scope>`,
      );

      fetchStub
        .withArgs(sinon.match(SCOPES_API), {
          method: 'PUT',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_CONFLICT_RESPONSE);

      setDialogValues(createScope, true);

      await nextFrame();
      const substitutions = new Map();
      substitutions.set('%node_set_name%', SCOPE_NAME_ATHLONE);
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
    it('Then: a scope is not updated and notification is displayed', async () => {
      const createScope = await fixture(
        html`<e-create-scope
          modify-scope="true"
          .modifyName=${SCOPE_NAME_ATHLONE}
          .modifyId=${SCOPE_ID}
        ></e-create-scope>`,
      );

      fetchStub
        .withArgs(sinon.match(SCOPES_API), {
          method: 'PUT',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_PAYLOAD_TOO_LARGE_RESPONSE);

      setDialogValues(createScope, true);

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
    it('Then: a scope is not updated and notification is displayed', async () => {
      const createScope = await fixture(
        html`<e-create-scope
          modify-scope="true"
          .modifyName=${SCOPE_NAME_ATHLONE}
          .modifyId=${SCOPE_ID}
        ></e-create-scope>`,
      );
      fetchStub
        .withArgs(sinon.match(SCOPES_API), {
          method: 'PUT',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_INTERNAL_SERVER_ERROR_RESPONSE);

      setDialogValues(createScope, true);
      await nextFrame();

      const notification = getNotification();
      // Verify notification was created with expected message and type
      expect(notification.getAttribute('description')).to.equal(
        LOCALE_EN_US.NODE_SET_CREATION_FAILURE_LONG_DESCRIPTION,
      );
      expect(
        notification.querySelector('eui-icon').getAttribute('name'),
      ).to.equal('failed');
    });
  });

  describe('When: show is called and 403 response is returned', () => {
    it('Then: a scope is not created and notification is displayed', async () => {
      const createScope = await fixture('<e-create-scope></e-create-scope>');

      fetchStub
        .withArgs(sinon.match(SCOPES_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_ACCESS_DENIED_RESPONSE);

      setDialogValues(createScope);

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
    it('Then: a scope is not updated and notification is displayed', async () => {
      const createScope = await fixture(
        html`<e-create-scope
          modify-scope="true"
          .modifyName=${SCOPE_NAME_ATHLONE}
          .modifyId=${SCOPE_ID}
        ></e-create-scope>`,
      );

      fetchStub
        .withArgs(sinon.match(SCOPES_API), {
          method: 'PUT',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_ACCESS_DENIED_RESPONSE);

      setDialogValues(createScope, true);

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
