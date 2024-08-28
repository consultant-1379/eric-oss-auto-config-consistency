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
import { LitComponent, html, definition, nothing } from '@eui/lit-component';
import {
  Accordion,
  Button,
  Dialog,
  FileInput,
  TextField,
  Notification,
} from '@eui/base';

import SettingsErrorTable from '../settings-error-table/settings-error-table.js';

import { restCalls } from '../../utils/restCallUtils.js';
import { logWarning } from '../../utils/logUtils.js';

import * as ATTR from '../../utils/attributes/rulesetAttributes.js';

import style from './create-ruleset.css';
import {
  MANUAL_CLOSE,
  NotificationStatus,
  createNotification,
} from '../../utils/notification/notifications.js';
import { updateLocaleString } from '../../utils/localization/localeUtils.js';

/**
 * This is the maximum permitted file size for a .csv file that can be uploaded.
 */
const MAX_FILE_SIZE = 20971520;

/**
 * The regexp for a valid ruleset name. Note: The Uppercase A-Z is accepted because a user can turn
 * on caps lock and enter these letters but the css will always display lowercase equivalent.
 */
const RULESET_NAME_REGEX = /^[A-Za-z0-9-_]+$/g;

const CLEAR_CONTENT = '';

export default class CreateRuleset extends LitComponent {
  constructor() {
    super();
    this.validRuleset = false;
    this.validRulesetFile = false;
    this.existingRulesetNames = [];
    this.containsRuleValidationErrors = false;
  }

  static get components() {
    return {
      'eui-accordion': Accordion,
      'eui-button': Button,
      'eui-dialog': Dialog,
      'eui-file-input': FileInput,
      'eui-notification': Notification,
      'eui-text-field': TextField,
      'e-settings-error-table': SettingsErrorTable,
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
    if (this.props.modifyRuleset) {
      this.saveActionCallBack = () => this._updateRuleset();
      this.dialogLabel = this.i18n?.EDIT_RULESET || 'Edit Ruleset';
    } else {
      this.saveActionCallBack = () => this._saveRuleset();
      this.dialogLabel = this.i18n?.CREATE_RULESET || 'Create Ruleset';
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
   * Updates the UI ruleset file name field and validates the input file size
   * does not exceed the maximum value 20 Mb.
   *
   * @param event A file based event.
   * @function _fileSelected
   * @private
   */
  async _fileSelected(event) {
    const uploadedFile = event.target.files[0];
    const rulesetFileName = this._getRulesetFileNameField();

    rulesetFileName.value = uploadedFile.name;

    if (uploadedFile.size > MAX_FILE_SIZE) {
      rulesetFileName.setCustomValidity(this.i18n?.RULESET_FILE_TOO_LARGE);
      this._disableValidRulesetFile();
    } else {
      rulesetFileName.setCustomValidity(CLEAR_CONTENT);
      this._enableValidRulesetFile();
    }

    this._enableSaveButton();
  }

  /**
   * Validates the UI ruleset name field entry to make sure the name is valid and
   * not an existing name.
   *
   * @param value The text field value.
   * @function _onInputHandler
   * @private
   */
  _onInputHandler = value => {
    const rulesetName = this._getRulesetNameField();
    if (this._isrulesetNameValid(value)) {
      if (this.existingRulesetNames.includes(value.toLowerCase())) {
        rulesetName.setCustomValidity(this.i18n?.RULESET_NAME_EXISTS_WARNING);
        this._disableValidRuleset();
      } else {
        rulesetName.setCustomValidity(CLEAR_CONTENT);
        this._enableValidRuleset();
      }
    } else {
      rulesetName.setCustomValidity(this.i18n?.RULESET_NAME_PATTERN_WARNING);
      this._disableValidRuleset();
    }

    this._enableSaveButton();
  };

  /**
   * Makes a REST call to send the updated ruleset to the EACC back-end.
   *
   * @function _updateRuleset
   * @private
   */
  _updateRuleset() {
    this.disableSave = true;
    const formData = new FormData();
    formData.append(ATTR.RULESET_NAME, this.props.modifyName);
    formData.append(
      ATTR.RULESET_FILE_NAME,
      this._getRulesetFileInput().files[0],
    );
    fetch(`${restCalls.eacc.putRuleset.url}/${this.props.modifyId}`, {
      method: restCalls.eacc.putRuleset.method,
      headers: restCalls.eacc.putRuleset.headers,
      body: formData,
    })
      .then(async response => {
        if (!response.ok) {
          const responseBody =
            response.status === 400
              ? await response.json().then(this._extractJsonBody)
              : null;

          const failureMessage =
            responseBody === null ? response.statusText : responseBody.detail;

          logWarning(
            `Post ruleset failed, reason: status code: ${response.status}, status text: ${failureMessage}`,
          );

          return Promise.reject({
            status: response.status,
            error: responseBody,
          });
        }
        return response.json();
      })
      .then(res => {
        this.bubble('e-create-ruleset:created', res);

        const substitutions = new Map();
        substitutions.set('%ruleset_name%', this.props.modifyName);

        createNotification(
          this,
          NotificationStatus.SUCCESS,
          updateLocaleString(this.i18n?.RULESET_UPDATED, substitutions),
        );
      })
      .catch(res => {
        let title = this.i18n?.UNABLE_TO_UPDATE_RULESET;
        let description;
        switch (res.status) {
          case 400:
            if (this.containsRuleValidationErrors) {
              this.shadowRoot
                .querySelector('eui-dialog#ruleValidationDialog')
                .showDialog();
              return;
            }
            description = this.i18n?.REST_RESPONSE_400_BAD_REQUEST;
            break;
          case 403:
            title = this.i18n?.ACCESS_DENIED;
            description = this.i18n?.REST_403_DESCRIPTION;
            break;
          case 404:
            description = this.i18n?.REST_RESPONSE_404_NOT_FOUND;
            break;
          case 409: {
            const substitutions = new Map();
            substitutions.set('%ruleset_name%', this.props.modifyName);

            description = updateLocaleString(
              this.i18n?.REST_RESPONSE_409_CONFLICT,
              substitutions,
            );
            break;
          }
          case 413:
            description = this.i18n?.REST_RESPONSE_413_PAYLOAD_TOO_LARGE;
            break;
          default:
            description = this.i18n?.RULESET_CREATION_FAILURE_LONG_DESCRIPTION;
        }
        createNotification(
          this,
          NotificationStatus.FAILURE,
          title,
          description,
          MANUAL_CLOSE,
        );
      })
      .finally(() => {
        this._getRulesetDialog().hideDialog();
      });
  }

  /**
   * Makes a REST call to send the validated ruleset to the EACC back-end.
   *
   * @function _saveRuleset
   * @private
   */
  _saveRuleset() {
    this.disableSave = true;

    const formData = new FormData();
    formData.append(
      ATTR.RULESET_NAME,
      this._getRulesetNameField().value.toLowerCase(),
    );
    formData.append(
      ATTR.RULESET_FILE_NAME,
      this._getRulesetFileInput().files[0],
    );
    fetch(restCalls.eacc.postRuleset.url, {
      method: restCalls.eacc.postRuleset.method,
      headers: restCalls.eacc.postRuleset.headers,
      body: formData,
    })
      .then(async response => {
        if (!response.ok) {
          const responseBody =
            response.status === 400
              ? await response.json().then(this._extractJsonBody)
              : null;

          const failureMessage =
            responseBody === null ? response.statusText : responseBody.detail;

          logWarning(
            `Post ruleset failed, reason: status code: ${response.status}, status text: ${failureMessage}`,
          );

          return Promise.reject({
            status: response.status,
            error: responseBody,
          });
        }
        return response.json();
      })
      .then(res => {
        this.bubble('e-create-ruleset:created', res);

        const substitutions = new Map();
        substitutions.set('%ruleset_name%', res[ATTR.RULESET_NAME]);
        createNotification(
          this,
          NotificationStatus.SUCCESS,
          updateLocaleString(this.i18n?.RULESET_CREATED, substitutions),
        );
      })
      .catch(res => {
        let title = this.i18n?.RULESET_CREATION_FAILURE_SHORT_DESCRIPTION;
        let description;
        switch (res.status) {
          case 400:
            if (this.containsRuleValidationErrors) {
              this.shadowRoot
                .querySelector('eui-dialog#ruleValidationDialog')
                .showDialog();
              return;
            }
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
            description = this.i18n?.RULESET_CREATION_FAILURE_LONG_DESCRIPTION;
        }
        createNotification(
          this,
          NotificationStatus.FAILURE,
          title,
          description,
          MANUAL_CLOSE,
        );
      })
      .finally(() => {
        this._getRulesetDialog().hideDialog();
      });
  }

  /**
   * Extracts the 'ruleValidationErrors' from the error response and returns the response object.
   *
   * @param response The response object.
   * @function _extractJsonBody
   * @private
   */
  _extractJsonBody = response => {
    if (Array.isArray(response.ruleValidationErrors)) {
      if (response.ruleValidationErrors.length > 0) {
        this.ruleValidationErrors = response.ruleValidationErrors;
        this.containsRuleValidationErrors = true;
      }
    } else if (response.ruleValidationErrors) {
      // A single response comes back as an object
      this.ruleValidationErrors = [
        ...this.ruleValidationErrors,
        response.ruleValidationErrors,
      ];
      this.containsRuleValidationErrors = true;
    }
    return Promise.resolve(response);
  };

  /**
   * Returns rule validation error dialog
   *
   * @function _ruleValidationDialog
   * @private
   */
  _ruleValidationDialog() {
    const settingErrorTable =
      this.ruleValidationErrors.length > 0
        ? html`
            <e-settings-error-table
              .columns=${this._getRuleValidationErrorDialogColumns()}
              .data=${this.ruleValidationErrors}
            ></e-settings-error-table>
          `
        : html`${nothing}`;

    return html` <eui-dialog
      id="ruleValidationDialog"
      label=${this.i18n?.RULESET_CREATION_FAILURE_SHORT_DESCRIPTION}
      no-cancel
    >
      <div slot="content">
        <p>${this.i18n?.RULESET_HAS_INVALID_RULES}</p>
        <eui-accordion
          category-title="${this.i18n?.RULE_VALIDATION_ERRORS} (${this
            .ruleValidationErrors.length})"
          line
        >
          ${settingErrorTable}
        </eui-accordion>
      </div>
      <eui-button
        slot="bottom"
        primary
        @click=${() => {
          this.shadowRoot
            .querySelector('eui-dialog#ruleValidationDialog')
            .hideDialog();
          this.ruleValidationErrors = [];
          this.containsRuleValidationErrors = false;
        }}
      >
        ${this.i18n?.OK}
      </eui-button>
    </eui-dialog>`;
  }

  /**
   * Get columns for the rule validation error dialog
   *
   * @function _getRuleValidationErrorDialogColumns
   * @private
   */
  _getRuleValidationErrorDialogColumns() {
    // prettier-ignore
    return [
          { title: this.i18n?.LINE, attribute: ATTR.RULESET_ERRORS_LINE_NUMBER, width: '70px', sort: 'asc', sortable: true },
          { title: this.i18n?.REASON, attribute: ATTR.RULESET_ERRORS_ERROR_DETAILS },
        ];
  }

  /**
   * Add custom styling to underlying UI SDK components.
   *
   * @function _addCustomStyle
   * @private
   */
  _addCustomStyle() {
    this._getRulesetFileNameField().shadowRoot.querySelector(
      'input',
    ).style.cursor = 'not-allowed';
    this._getRulesetNameField().shadowRoot.querySelector(
      'input',
    ).style.textTransform = 'lowercase';
  }

  /**
   * Disables the valid ruleset(helps determine if the save button is enabled)
   *
   * @function _disableValidRuleset
   * @private
   */
  _disableValidRuleset() {
    this.validRuleset = false;
  }

  /**
   * Enables the valid ruleset(helps determine if the save button is enabled)
   *
   * @function _enableValidRuleset
   * @private
   */
  _enableValidRuleset() {
    this.validRuleset = true;
  }

  /**
   * Disables the valid ruleset file(helps determine if the save button is enabled)
   *
   * @function _disableValidRulesetFile
   * @private
   */
  _disableValidRulesetFile() {
    this.validRulesetFile = false;
  }

  /**
   * Enables the valid ruleset file(helps determine if the save button is enabled)
   *
   * @function _enableValidRulesetFile
   * @private
   */
  _enableValidRulesetFile() {
    this.validRulesetFile = true;
  }

  /**
   * Returns the ruleset-name text field element.
   *
   * @returns The ruleset-name text field element.
   * @function _getRulesetNameField
   * @private
   */
  _getRulesetNameField() {
    return this.shadowRoot.querySelector('eui-text-field#ruleset-name');
  }

  /**
   * Returns the ruleset-file-name text field element.
   *
   * @returns The ruleset-file-name text field element.
   * @function _getRulesetFileNameField
   * @private
   */
  _getRulesetFileNameField() {
    return this.shadowRoot.querySelector('eui-text-field#ruleset-file-name');
  }

  /**
   * Returns the ruleset-file input element.
   *
   * @returns The ruleset-file input element.
   * @function _getRulesetFileInput
   * @private
   */
  _getRulesetFileInput() {
    return this.shadowRoot.querySelector('eui-file-input#ruleset-file');
  }

  /**
   * Returns the createRuleset eui dialog.
   *
   * @returns The createRuleset eui dialog.
   * @function _getRulesetDialog
   * @private
   */
  _getRulesetDialog() {
    return this.shadowRoot.querySelector('eui-dialog#createRuleset');
  }

  /**
   * Returns a boolean based on whether or not the name matches the ruleset name regexp.
   *
   * @param name The name to test against.
   * @returns True if the name is valid otherwise false.
   * @function _isrulesetNameValid
   * @private
   */
  _isrulesetNameValid(name) {
    return new RegExp(RULESET_NAME_REGEX).test(name);
  }

  /**
   * Enables the save button if the ruleset name and ruleset file are valid.
   *
   * @function _enableSaveButton
   * @private
   */
  _enableSaveButton() {
    if (this.validRuleset && this.validRulesetFile) {
      this.disableSave = false;
    } else {
      this.disableSave = true;
    }
  }

  /**
   * Closes the create ruleset dialog and resets the various fields.
   *
   * @function _close
   * @private
   */
  _close() {
    this._disableValidRuleset();
    this._disableValidRulesetFile();
    this._getRulesetNameField().value = CLEAR_CONTENT;
    this._getRulesetFileNameField().value = CLEAR_CONTENT;
    this._getRulesetFileInput().shadowRoot.querySelector('input').value =
      CLEAR_CONTENT;
    this._getRulesetNameField().setCustomValidity(CLEAR_CONTENT);
    this._getRulesetFileNameField().setCustomValidity(CLEAR_CONTENT);
    this.disableSave = true;
  }

  /**
   * Shows the create ruleset dialog.
   * @function showDialog
   */
  showDialog() {
    if (this.props.modifyRuleset) {
      this._enableValidRuleset();
    }
    this._getRulesetDialog().showDialog();
    this._addCustomStyle();
  }

  /**
   * Render the <e-create-ruleset> component. This function is called each time a
   * prop changes.
   */
  render() {
    return html` <eui-dialog
        label="${this.dialogLabel}"
        id="createRuleset"
        @eui-dialog:cancel="${() => this._close()}"
      >
        <div slot="content">
          <div>
            <p>${this.i18n?.NAME}</p>
            <p
              id="existing-ruleset-name"
              ?hidden="${!this.props.modifyRuleset}"
            >
              ${this.props.modifyName}
            </p>
            <eui-text-field
              id="ruleset-name"
              ?hidden=${this.props.modifyRuleset}
              fullwidth
              @input=${event => this._onInputHandler(event.currentTarget.value)}
              maxlength="255"
            ></eui-text-field>
          </div>
          <div>
            <eui-text-field
              id="ruleset-file-name"
              placeholder="${this.i18n?.NO_FILE_SELECTED}"
              size="22"
            ></eui-text-field>
            <eui-file-input
              id="ruleset-file"
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
      </eui-dialog>
      ${this._ruleValidationDialog()}`;
  }
}

/**
 * @property {Boolean} disableSave - disable save button.
 * @property {Array} existingRulesetNames - array of exsisting ruleset names.
 * @property {String} modifyName - modified name for ruleset.
 * @property {String} modifyId - Id of the ruleset to be modified.
 * @property {Boolean} modifyRuleset - check if name modified or not.
 * @property {Array} ruleValidationErrors - array of rule validation errors.
 */
definition('e-create-ruleset', {
  style,
  props: {
    disableSave: { attribute: false, type: Boolean, default: true },
    existingRulesetNames: { attribute: false, type: Array, default: [] },
    modifyName: { attribute: false, type: String },
    modifyId: { attribute: false, type: String },
    modifyRuleset: { attribute: true, type: Boolean, default: false },
    ruleValidationErrors: { attribute: false, type: Array, default: [] },
  },
})(CreateRuleset);
