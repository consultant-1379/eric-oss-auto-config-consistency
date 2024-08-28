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

import { RULESET_TAB_INDEX, SCOPE_TAB_INDEX } from '../utils/eaccUIUtils.js';
import {
  CONTAIN,
  UI_RENDERING_PERIOD,
  NOTIFICATION_WAIT_PERIOD,
} from '../utils/testUtils.js';
import {
  EACC_SETTINGS_URL,
  NODE_SET_NAME_4G,
  NODE_SET_NAME_5G,
  RULESET_NAME_4G,
  RULESET_NAME_5G,
  TEST_SCOPES,
  TEST_RULESETS,
} from '../utils/testData.js';

const USE_CASE_ID = 'EACC-UC_01';

/**
 * UC_01-01: It shall be possible to create, read, and update settings in EACC.
 */
describe(`${USE_CASE_ID}: Settings Create, Read, and Update`, () => {
  before(() => {
    cy.visit(EACC_SETTINGS_URL);
    cy.wait(UI_RENDERING_PERIOD);
  });
  it(`${USE_CASE_ID}-01: Create; A new node set '${NODE_SET_NAME_4G}' and '${NODE_SET_NAME_5G}' can be created`, () => {
    // Select the Scope tab
    cy.getSettingsTab(SCOPE_TAB_INDEX).click();

    TEST_SCOPES.forEach(scope => {
      // Open the create scope dialog by clicking on the create scope button
      cy.getScope()
        .find('div.action-buttons')
        .find('div.app-create-button')
        .find('eui-button#create-scope-btn')
        .click();

      cy.getCreateScope()
        .find('div[slot="content"]')
        .find('eui-text-field#scope-name')
        .shadow()
        .find('input')
        .type(scope.name, { force: true });

      cy.getCreateScope()
        .find('div[slot="content"]')
        .find('eui-file-input#scope-file')
        .shadow()
        .find('input')
        .selectFile(`cypress/uploads/${scope.createFile}`, {
          force: true,
        });

      cy.getCreateScope().find('eui-button[slot="bottom"]').click();

      // A successful popup should appear
      cy.get('div#notifications-column')
        .find('eui-notification')
        .should(CONTAIN, `Node set '${scope.name}' was created.`);

      // First row should be the newly created scope
      cy.getScopesTable()
        .find('table')
        .find('tbody')
        .find('tr')
        .eq(0)
        .find('td')
        .should(CONTAIN, scope.name);

      // wait for the notification to disappear
      cy.wait(NOTIFICATION_WAIT_PERIOD);
    });
  });
  it(`${USE_CASE_ID}-02: Read; The node set table should contain an entry for '${NODE_SET_NAME_4G}' and '${NODE_SET_NAME_5G}'`, () => {
    // Select the Scope tab
    cy.getSettingsTab(SCOPE_TAB_INDEX).click();

    TEST_SCOPES.forEach(scope => {
      cy.getScopesTable().getRowByKey(scope.name);
    });
  });
  it(`${USE_CASE_ID}-03: Update; The node set '${NODE_SET_NAME_4G}' and '${NODE_SET_NAME_5G}' can be updated`, () => {
    // Select the Scope tab
    cy.getSettingsTab(SCOPE_TAB_INDEX).click();

    TEST_SCOPES.forEach(scope => {
      cy.getScopesTable().getRowByKey(scope.name).click();

      // Open the edit scope dialog by clicking on the edit scope button
      cy.getScope()
        .find('div.action-buttons')
        .find('div.app-modify-buttons')
        .find('eui-button#edit-scope-btn')
        .click();

      cy.getEditScope()
        .find('div[slot="content"]')
        .find('eui-file-input#scope-file')
        .shadow()
        .find('input')
        .selectFile(`cypress/uploads/${scope.updateFile}`, {
          force: true,
        });

      cy.getEditScope().find('eui-button[slot="bottom"]').click();

      cy.get('div#notifications-column')
        .find('eui-notification')
        .should(CONTAIN, `Node set '${scope.name}' was updated.`);

      // wait for the notification to disappear
      cy.wait(NOTIFICATION_WAIT_PERIOD);
    });
  });
  it(`${USE_CASE_ID}-04: Create; A new ruleset '${RULESET_NAME_4G}' and '${RULESET_NAME_5G}' can be created`, () => {
    // Select the Ruleset tab
    cy.getSettingsTab(RULESET_TAB_INDEX).click();

    TEST_RULESETS.forEach(ruleset => {
      // Open the create ruleset dialog by clicking on the create ruleset button
      cy.getRuleset()
        .find('div.action-buttons')
        .find('div.app-create-button')
        .find('eui-button#create-ruleset-btn')
        .click();

      cy.getCreateRuleset()
        .find('div[slot="content"]')
        .find('eui-text-field#ruleset-name')
        .shadow()
        .find('input')
        .type(ruleset.name, { force: true });

      cy.getCreateRuleset()
        .find('div[slot="content"]')
        .find('eui-file-input#ruleset-file')
        .shadow()
        .find('input')
        .selectFile(`cypress/uploads/${ruleset.createFile}`, {
          force: true,
        });

      cy.getCreateRuleset().find('eui-button[slot="bottom"]').click();

      // A successful popup should appear
      cy.get('div#notifications-column')
        .find('eui-notification')
        .should(CONTAIN, `Ruleset '${ruleset.name}' was created.`);

      // First row should be the newly created ruleset
      cy.getRulesetTable()
        .find('table')
        .find('tbody')
        .find('tr')
        .eq(0)
        .find('td')
        .should(CONTAIN, ruleset.name);

      // wait for the notification to disappear
      cy.wait(NOTIFICATION_WAIT_PERIOD);
    });
  });
  it(`${USE_CASE_ID}-05: Read; The ruleset table should contain an entry for '${RULESET_NAME_4G}' and '${RULESET_NAME_5G}'`, () => {
    // Select the Ruleset tab
    cy.getSettingsTab(RULESET_TAB_INDEX).click();

    TEST_RULESETS.forEach(ruleset => {
      cy.getRulesetTable().getRowByKey(ruleset.name);
    });
  });
  it(`${USE_CASE_ID}-06: Update; The ruleset '${RULESET_NAME_4G}' and '${RULESET_NAME_5G}' can be updated`, () => {
    // Select the Ruleset tab
    cy.getSettingsTab(RULESET_TAB_INDEX).click();

    TEST_RULESETS.forEach(ruleset => {
      cy.getRulesetTable().getRowByKey(ruleset.name).click();

      // Open the edit ruleset dialog by clicking on the edit ruleset button
      cy.getRuleset()
        .find('div.action-buttons')
        .find('div.app-modify-buttons')
        .find('eui-button#edit-ruleset-btn')
        .click();

      cy.getEditRuleset()
        .find('div[slot="content"]')
        .find('eui-file-input#ruleset-file')
        .shadow()
        .find('input')
        .selectFile(`cypress/uploads/${ruleset.updateFile}`, {
          force: true,
        });

      cy.getEditRuleset().find('eui-button[slot="bottom"]').click();

      cy.get('div#notifications-column')
        .find('eui-notification')
        .should(CONTAIN, `Ruleset '${ruleset.name}' was updated.`);

      // wait for the notification to disappear
      cy.wait(NOTIFICATION_WAIT_PERIOD);
    });
  });
  it(`${USE_CASE_ID}-07: Download; The node set '${NODE_SET_NAME_4G}' and '${NODE_SET_NAME_5G}' can be downloaded`, () => {
    cy.getSettingsTab(SCOPE_TAB_INDEX).click();

    TEST_SCOPES.forEach(scope => {
      cy.readFile(`cypress/uploads/${scope.updateFile}`).then(
        originalScopeFile => {
          cy.getScopesTable().getRowByKey(scope.name).click();

          cy.getScopesTable()
            .getRowByKey(scope.name)
            .children()
            .eq(1)
            .find('eui-actionable-icon')
            .click();

          cy.readFile(`cypress/downloads/${scope.name}.csv`).then(
            downloadedFile => {
              const processFileContent = content =>
                content
                  .replace(/\r/g, '')
                  .split('\n')
                  .map(line => line.trim())
                  .filter(line => line);

              const processedOriginal = processFileContent(originalScopeFile);
              const processedDownloaded = processFileContent(downloadedFile);

              expect(processedDownloaded).to.have.deep.members(
                processedOriginal,
              );
            },
          );
        },
      );
    });
  });

  it(`${USE_CASE_ID}-08: Download; The ruleset '${RULESET_NAME_4G}' and '${RULESET_NAME_5G}' can be downloaded`, () => {
    cy.getSettingsTab(RULESET_TAB_INDEX).click();

    TEST_RULESETS.forEach(ruleset => {
      cy.readFile(`cypress/uploads/${ruleset.updateFile}`).then(
        originalRulesetFile => {
          cy.getRulesetTable().getRowByKey(ruleset.name).click();

          cy.getRulesetTable()
            .getRowByKey(ruleset.name)
            .children()
            .eq(1)
            .find('eui-actionable-icon')
            .click();

          cy.readFile(`cypress/downloads/${ruleset.name}.csv`).then(
            downloadedFile => {
              expect(
                downloadedFile
                  .replaceAll('\r', '')
                  .replaceAll('\n', '')
                  .replaceAll('"', ''),
              ).to.equal(
                originalRulesetFile
                  .replaceAll('\r', '')
                  .replaceAll('\n', '')
                  .replaceAll('"', ''),
              );
            },
          );
        },
      );
    });
  });
});
