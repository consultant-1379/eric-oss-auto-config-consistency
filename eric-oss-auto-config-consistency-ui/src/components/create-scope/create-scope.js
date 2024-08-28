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
import { LitComponent, html, definition } from '@eui/lit-component';

import { Button } from '@eui/base/button';
import { Dialog } from '@eui/base/dialog';
import { FileInput } from '@eui/base/file-input';
import { Notification } from '@eui/base/notification';
import { TextField } from '@eui/base/text-field';

import { restCalls } from '../../utils/restCallUtils.js';
import { logWarning } from '../../utils/logUtils.js';
import { updateLocaleString } from '../../utils/localization/localeUtils.js';

import {
  NotificationStatus,
  createNotification,
  MANUAL_CLOSE,
} from '../../utils/notification/notifications.js';

import * as ATTR from '../../utils/attributes/scopeAttributes.js';

import style from './create-scope.css';

/**
 * This is the maximum permitted file size for a .csv file that can be uploaded.
 */
const MAX_FILE_SIZE = 20971520;

/**
 * The regexp for a valid scope name. Note: The Uppercase A-Z is accepted because a user can turn
 * on caps lock and enter these letters but the css will always display lowercase equivalent.
 */
const SCOPE_NAME_REGEX = /^[A-Za-z0-9-_]+$/g;

/**
 * An empty string used to clear content.
 */
const CLEAR_CONTENT = '';

/**
 * Component CreateScope is defined as
 * `<e-create-scope>`
 *
 * @extends {LitComponent}
 */
export default class CreateScope extends LitComponent {
  constructor() {
    super();
    this.validScope = false;
    this.validScopeFile = false;
    this.existingScopeNames = [];
  }

  static get components() {
    return {
      'eui-button': Button,
      'eui-dialog': Dialog,
      'eui-file-input': FileInput,
      'eui-notification': Notification,
      'eui-text-field': TextField,
    };
  }

  /**
   * To access the i18n (localized) values for a component.
   *
   * @function meta
   */
  get meta() {
    return import.meta;
  }

  /**
   * Is called when the component added to the DOM.
   *
   * @function didConnect()
   */
  async didConnect() {
    if (this.props.modifyScope) {
      this.saveActionCallBack = () => this._updateScope();
      this.dialogLabel = this.i18n?.EDIT_NODE_SET || 'Edit node set';
    } else {
      this.saveActionCallBack = () => this._saveScope();
      this.dialogLabel = this.i18n?.CREATE_NODE_SET || 'Create node set';
    }
  }

  /**
   * Is called when the component is about to be disconnected from the DOM.
   *
   * @function didDisconnect
   */
  async didDisconnect() {
    this._close();
  }

  /**
   * Updates the UI scope file name field and validates the input file size
   * does not exceed the maximum value 10 Mb.
   *
   * @param event A file based event.
   * @function _fileSelected
   * @private
   */
  async _fileSelected(event) {
    const uploadedFile = event.target.files[0];
    const scopeFileName = this._getScopeFileNameField();

    scopeFileName.value = uploadedFile.name;

    if (uploadedFile.size > MAX_FILE_SIZE) {
      scopeFileName.setCustomValidity(this.i18n?.NODE_SET_FILE_TOO_LARGE);
      this._disableValidScopeFile();
    } else {
      scopeFileName.setCustomValidity(CLEAR_CONTENT);
      this._enableValidScopeFile();
    }

    this._enableSaveButton();
  }

  /**
   * Validates the UI scope name field entry to make sure the name is valid and
   * not an existing name.
   *
   * @param value The text field value.
   * @function _onInputHandler
   * @private
   */
  _onInputHandler = value => {
    const scopeName = this._getScopeNameField();
    if (this._isScopeNameValid(value)) {
      if (this.existingScopeNames.includes(value.toLowerCase())) {
        scopeName.setCustomValidity(this.i18n?.NODE_SET_NAME_EXISTS_WARNING);
        this._disableValidScope();
      } else {
        scopeName.setCustomValidity(CLEAR_CONTENT);
        this._enableValidScope();
      }
    } else {
      scopeName.setCustomValidity(this.i18n?.NODE_SET_NAME_PATTERN_WARNING);
      this._disableValidScope();
    }

    this._enableSaveButton();
  };

  /**
   * Closes the create node set dialog and resets the various fields.
   *
   * @function _close
   * @private
   */
  _close() {
    this._disableValidScope();
    this._disableValidScopeFile();
    this._getScopeNameField().value = CLEAR_CONTENT;
    this._getScopeFileNameField().value = CLEAR_CONTENT;
    this._getScopeFileInput().shadowRoot.querySelector('input').value =
      CLEAR_CONTENT;
    this._getScopeNameField().setCustomValidity(CLEAR_CONTENT);
    this._getScopeFileNameField().setCustomValidity(CLEAR_CONTENT);
    this.disableSave = true;
  }

  /**
   * Enables the save button if the scope name and scope file are valid.
   *
   * @function _enableSaveButton
   * @private
   */
  _enableSaveButton() {
    if (this.validScope && this.validScopeFile) {
      this.disableSave = false;
    } else {
      this.disableSave = true;
    }
  }

  /**
   * Enables the valid scope(helps determine if the save button is enabled)
   *
   * @function _enableValidScope
   * @private
   */
  _enableValidScope() {
    this.validScope = true;
  }

  /**
   * Disables the valid scope(helps determine if the save button is enabled)
   *
   * @function _disableValidScope
   * @private
   */
  _disableValidScope() {
    this.validScope = false;
  }

  /**
   * Enables the valid scope file(helps determine if the save button is enabled)
   *
   * @function _enableValidScopeFile
   * @private
   */
  _enableValidScopeFile() {
    this.validScopeFile = true;
  }

  /**
   * Disables the valid scope file(helps determine if the save button is enabled)
   *
   * @function _disableValidScopeFile
   * @private
   */
  _disableValidScopeFile() {
    this.validScopeFile = false;
  }

  /**
   * Returns a boolean based on whether or not the name matches the scope name regexp.
   *
   * @param name The name to test against.
   * @returns True if the name is valid otherwise false.
   * @function _isScopeNameValid
   * @private
   */
  _isScopeNameValid(name) {
    return new RegExp(SCOPE_NAME_REGEX).test(name);
  }

  /**
   * Returns the createScope eui dialog.
   *
   * @returns The createScope eui dialog.
   * @function _getScopeDialog
   * @private
   */
  _getScopeDialog() {
    return this.shadowRoot.querySelector('eui-dialog#createScope');
  }

  /**
   * Returns the scope-name text field element.
   *
   * @returns The scope-name text field element.
   * @function _getScopeNameField
   * @private
   */
  _getScopeNameField() {
    return this.shadowRoot.querySelector('eui-text-field#scope-name');
  }

  /**
   * Returns the scope-file-name text field element.
   *
   * @returns The scope-file-name text field element.
   * @function _getScopeFileNameField
   * @private
   */
  _getScopeFileNameField() {
    return this.shadowRoot.querySelector('eui-text-field#scope-file-name');
  }

  /**
   * Returns the scope-file input element.
   *
   * @returns The scope-file input element.
   * @function _getScopeFileInput
   * @private
   */
  _getScopeFileInput() {
    return this.shadowRoot.querySelector('eui-file-input#scope-file');
  }

  /**
   * Makes a REST call to send the updated scope to the EACC back-end.
   *
   * @function _updateScope
   * @private
   */
  _updateScope() {
    this.disableSave = true;
    const formData = new FormData();
    formData.append(ATTR.SCOPE_NAME, this.props.modifyName);
    formData.append(ATTR.SCOPE_FILE_NAME, this._getScopeFileInput().files[0]);

    fetch(`${restCalls.eacc.putNodeSet.url}/${this.props.modifyId}`, {
      method: restCalls.eacc.putNodeSet.method,
      headers: restCalls.eacc.putNodeSet.headers,
      body: formData,
    })
      .then(response => {
        if (!response.ok) {
          logWarning(
            `Put node set failed, reason: status code: ${response.status}, status text: ${response.statusText}`,
          );
          return Promise.reject(response.status);
        }
        return response.json();
      })
      .then(res => {
        this.bubble('e-create-scope:created', res);

        const substitutions = new Map();
        substitutions.set('%node_set_name%', this.props.modifyName);

        createNotification(
          this,
          NotificationStatus.SUCCESS,
          updateLocaleString(this.i18n?.NODE_SET_UPDATED, substitutions),
        );
      })
      .catch(errorCode => {
        const substitutions = new Map();
        substitutions.set('%node_set_name%', this.props.modifyName);
        let title = this.i18n?.UNABLE_TO_UPDATE_NODE_SET;
        let description;
        switch (errorCode) {
          case 400:
            description = this.i18n?.REST_RESPONSE_400_BAD_REQUEST;
            break;
          case 403:
            title = this.i18n?.ACCESS_DENIED;
            description = this.i18n?.REST_403_DESCRIPTION;
            break;
          case 404:
            description = this.i18n?.REST_RESPONSE_404_NOT_FOUND;
            break;
          case 409:
            description = updateLocaleString(
              this.i18n?.REST_RESPONSE_409_CONFLICT,
              substitutions,
            );
            break;
          case 413:
            description = this.i18n?.REST_RESPONSE_413_PAYLOAD_TOO_LARGE;
            break;
          default:
            description = this.i18n?.NODE_SET_CREATION_FAILURE_LONG_DESCRIPTION;
        }
        createNotification(
          this,
          NotificationStatus.FAILURE,
          title,
          description,
          MANUAL_CLOSE,
        );

        logWarning(`Unable to post node set, error code: ${errorCode}`);
      })
      .finally(() => {
        this._getScopeDialog().hideDialog();
      });
  }

  /**
   * Makes a REST call to send the validated scope to the EACC back-end.
   *
   * @function _saveScope
   * @private
   */
  _saveScope() {
    this.disableSave = true;

    const formData = new FormData();
    formData.append(
      ATTR.SCOPE_NAME,
      this._getScopeNameField().value.toLowerCase(),
    );
    formData.append(ATTR.SCOPE_FILE_NAME, this._getScopeFileInput().files[0]);
    fetch(restCalls.eacc.postNodeSet.url, {
      method: restCalls.eacc.postNodeSet.method,
      headers: restCalls.eacc.postNodeSet.headers,
      body: formData,
    })
      .then(response => {
        if (!response.ok) {
          logWarning(
            `Post node set failed, reason: status code: ${response.status}, status text: ${response.statusText}`,
          );
          return Promise.reject(response.status);
        }
        return response.json();
      })
      .then(res => {
        this.bubble('e-create-scope:created', res);

        const substitutions = new Map();
        substitutions.set('%node_set_name%', res[ATTR.SCOPE_NAME]);

        createNotification(
          this,
          NotificationStatus.SUCCESS,
          updateLocaleString(this.i18n?.NODE_SET_CREATED, substitutions),
        );
      })
      .catch(errorCode => {
        let title = this.i18n?.NODE_SET_CREATION_FAILURE_SHORT_DESCRIPTION;
        let description;
        switch (errorCode) {
          case 400:
            description = this.i18n?.REST_RESPONSE_400_BAD_REQUEST;
            break;
          case 403:
            title = this.i18n?.ACCESS_DENIED;
            description = this.i18n?.REST_403_DESCRIPTION;
            break;
          case 413:
            description = this.i18n?.REST_RESPONSE_413_PAYLOAD_TOO_LARGE;
            break;
          default:
            description = this.i18n?.NODE_SET_CREATION_FAILURE_LONG_DESCRIPTION;
        }
        createNotification(
          this,
          NotificationStatus.FAILURE,
          title,
          description,
          MANUAL_CLOSE,
        );

        logWarning(`Unable to post node set, error code: ${errorCode}`);
      })
      .finally(() => {
        this._getScopeDialog().hideDialog();
      });
  }

  /**
   * Add custom styling to underlying UI SDK components.
   *
   * @function _addCustomStyle
   * @private
   */
  _addCustomStyle() {
    this._getScopeFileNameField().shadowRoot.querySelector(
      'input',
    ).style.cursor = 'not-allowed';
    this._getScopeNameField().shadowRoot.querySelector(
      'input',
    ).style.textTransform = 'lowercase';
  }

  /**
   * Shows the create scope dialog.
   * @function showDialog
   */
  showDialog() {
    if (this.props.modifyScope) {
      this._enableValidScope();
    }
    this._getScopeDialog().showDialog();
    this._addCustomStyle();
  }

  /**
   * Render the <e-create-scope> component. This function is called each time a
   * prop changes.
   */
  render() {
    return html` <eui-dialog
      label="${this.dialogLabel}"
      id="createScope"
      @eui-dialog:cancel="${() => this._close()}"
    >
      <div slot="content">
        <div>
          <p>${this.i18n?.NAME}</p>
          <p id="existing-scope-name" ?hidden="${!this.props.modifyScope}">
            ${this.props.modifyName}
          </p>
          <eui-text-field
            id="scope-name"
            ?hidden=${this.props.modifyScope}
            fullwidth
            @input=${event => this._onInputHandler(event.currentTarget.value)}
            maxlength="255"
          ></eui-text-field>
        </div>
        <div>
          <eui-text-field
            id="scope-file-name"
            placeholder="${this.i18n?.NO_FILE_SELECTED}"
            size="22"
          ></eui-text-field>
          <eui-file-input
            id="scope-file"
            required
            accept=".csv"
            @change="${event => this._fileSelected(event)}"
            >${this.i18n?.UPLOAD_FILE}</eui-file-input
          >
        </div>
      </div>
      <eui-button
        slot="bottom"
        primary
        @click="${() => this.saveActionCallBack()}"
        ?disabled="${this.disableSave}"
        >${this.i18n?.SAVE}</eui-button
      >
    </eui-dialog>`;
  }
}
/**
 * @property {Boolean} disableSave - disables save functionality.
 * @property {Array} existingScopeNames - array of existing scope names.
 * @property {String} modifyName - value for scope name being modified.
 * @property {String} modifyId - value for scope ID being modified.
 * @property {Boolean} modifyScope - Indicates whether a scope is being modified.
 */
definition('e-create-scope', {
  style,
  props: {
    disableSave: { attribute: false, type: Boolean, default: true },
    existingScopeNames: { attribute: false, type: Array, default: [] },
    modifyName: { attribute: false, type: String },
    modifyId: { attribute: false, type: String },
    modifyScope: { attribute: true, type: Boolean, default: false },
  },
})(CreateScope);
