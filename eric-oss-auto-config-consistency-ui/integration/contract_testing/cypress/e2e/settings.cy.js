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

import * as LOCALE_EN_US_SETTINGS from '../../../../src/apps/settings/locale/en-us.json';
import * as LOCALE_EN_US_SCOPE from '../../../../src/components/scope/locale/en-us.json';
import * as LOCALE_EN_US_RULESETS from '../../../../src/components/ruleset/locale/en-us.json';
import * as LOCALE_EN_US_CREATE_RULESET from '../../../../src/components/create-ruleset/locale/en-us.json';
import * as DATA from '../utils/testData.js';
import { CONTAIN, EQUAL, HAVE_LENGTH, HAVE_TEXT } from '../utils/testUtils.js';

const RULESET_TAB_INDEX = 0;
const SCOPE_TAB_INDEX = 1;

describe('Visit the settings app', () => {
  beforeEach(() => {
    cy.interceptModules();
    cy.visit('/#eacc/settings');
    cy.waitForModules();
  });

  it(`The "Scopes" tab exists with a heading ${LOCALE_EN_US_SETTINGS.SCOPE} and the expected number of node sets`, () => {
    // Select the Scope tab
    cy.getSettingsTab(SCOPE_TAB_INDEX).click();

    const expectedNodeSets = 2;
    cy.getSettingsTab(SCOPE_TAB_INDEX).should(
      CONTAIN,
      LOCALE_EN_US_SETTINGS.SCOPE,
    );

    cy.getScope()
      .find('.app-header')
      .should(CONTAIN, `${LOCALE_EN_US_SCOPE.NODESETS} (${expectedNodeSets})`);
  });

  it(`The "Scopes" table is populated with scopes`, () => {
    cy.getSettingsTab(SCOPE_TAB_INDEX).click();

    const expectedColumns = 2;
    const expectedNodeSets = 2;
    cy.log(`Checking Scope table for ${expectedColumns} columns`);
    cy.getScopesTable()
      .find('thead')
      .find('tr')
      .not('.filters')
      .find('th')
      .children()
      .should(HAVE_LENGTH, expectedColumns)
      .and(CONTAIN, LOCALE_EN_US_SCOPE.SCOPE_NAME);

    cy.log(`Checking Scopes table for ${expectedNodeSets} rows`);
    cy.getScopesTable()
      .find('tbody')
      .children()
      .should(HAVE_LENGTH, expectedNodeSets);

    // prettier-ignore
    cy.getScopesTable()
      .find('tbody').children()
      .first()
        .children()
          .first().should(CONTAIN, DATA.SCOPE_NAME_ONE) // scopeName
        .parent()
      .next()
        .children()
          .first().should(CONTAIN, DATA.SCOPE_NAME_TWO);

    cy.getScopesTable()
      .find('tbody')
      .children()
      .first()
      .children()
      .first()
      .click();
  });

  it(`A scope file is succesfully downloaded when the download icon is clicked`, () => {
    cy.getSettingsTab(SCOPE_TAB_INDEX).click();

    cy.getScopesTable().find('tbody').children().first().click();

    cy.getScopesTable()
      .find('tbody')
      .children()
      .first()
      .children()
      .eq(1)
      .find('eui-actionable-icon')
      .click();

    cy.log(`Checking a scope file has been downloaded`);

    cy.readFile(`./cypress/downloads/${DATA.SCOPE_NAME_ONE}.csv`).should(
      CONTAIN,
      'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00031,ManagedElement=NR03gNode',
    );
  });

  it(`When a download fails, a suitable eui-notification is displayed to the user`, () => {
    cy.getSettingsTab(SCOPE_TAB_INDEX).click();

    cy.log(`Checking a scope file can be downloaded`);

    cy.getScopesTable()
      .find('tbody')
      .children()
      .first()
      .children()
      .first()
      .should(CONTAIN, DATA.SCOPE_NAME_ONE); // scopeName

    cy.getScopesTable().find('tbody').children().eq(1).click();

    cy.getScopesTable()
      .find('tbody')
      .children()
      .eq(1)
      .children()
      .eq(1)
      .find('eui-actionable-icon')
      .click();

    cy.get('div#notifications-column')
      .find('eui-notification')
      .should(CONTAIN, LOCALE_EN_US_SCOPE.FILE_DOWNLOAD_FAILED_SHORT);
  });

  it('A new node set can be created from the "Scopes" tab', () => {
    const scopeName = 'westmeath_10-07-2023';

    // Select the Scope tab
    cy.getSettingsTab(SCOPE_TAB_INDEX).click();

    // Get the node set table count and the first row
    cy.getScope()
      .find('eui-table')
      .shadow()
      .find('table')
      .find('tbody')
      .find('tr')
      .should(HAVE_LENGTH, 2);

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
      .type(scopeName);

    cy.getCreateScope()
      .find('div[slot="content"]')
      .find('eui-file-input#scope-file')
      .shadow()
      .find('input')
      .selectFile('cypress/uploads/PositiveScopeContract.csv', { force: true });

    cy.getCreateScope().find('eui-button[slot="bottom"]').click();

    cy.get('div#notifications-column')
      .find('eui-notification')
      .should(CONTAIN, `Node set '${scopeName}' was created.`);

    cy.getScopesTable()
      .find('table')
      .find('tbody')
      .find('tr')
      .should(HAVE_LENGTH, 3);

    // First row should be the newly created scope
    cy.getScopesTable()
      .find('table')
      .find('tbody')
      .find('tr')
      .eq(0)
      .find('td')
      .should(CONTAIN, scopeName);
  });

  it('An existing node set can be updated from the "Scopes" tab', () => {
    // Select the Scope tab
    cy.getSettingsTab(SCOPE_TAB_INDEX).click();

    // Get the node set table count and the first row
    cy.getScopesTable().find('tbody').children().first().click();

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
      .selectFile('cypress/uploads/UpdatedPositiveScopeContract.csv', {
        force: true,
      });

    cy.getEditScope().find('eui-button[slot="bottom"]').click();

    cy.get('div#notifications-column')
      .find('eui-notification')
      .should(CONTAIN, `Node set '${DATA.SCOPE_NAME_ONE}' was updated.`);
  });

  it(`When a scope row is selected, clicking the delete button results in successful scope deletion and a HTTP 204 response.`, () => {
    const expectedNodeSets = 1;
    cy.intercept('DELETE', `/v1/scopes/${DATA.SCOPE_ID}`).as('deleteScope');

    cy.getSettingsTab(SCOPE_TAB_INDEX).click();

    cy.getScopesTable().find('tbody').children().first().click();

    cy.getScope()
      .find('div.action-buttons')
      .find('eui-button#delete-scope')
      .click();

    cy.getScope()
      .find('eui-dialog#delete-dialog')
      .find('eui-button[slot="bottom"]')
      .click();

    cy.wait('@deleteScope').its('response.statusCode').should('eq', 204);

    cy.get('#notifications-column')
      .find('eui-notification')
      .should(
        HAVE_TEXT,
        ` ${LOCALE_EN_US_SCOPE.NODE_SET} '${DATA.SCOPE_NAME_ONE}' ${LOCALE_EN_US_SCOPE.DELETED_SUCCESS_MESSAGE}`,
      );

    cy.log(`Checking Scopes table for ${expectedNodeSets} rows`);

    cy.getScopesTable()
      .find('tbody')
      .children()
      .should(HAVE_LENGTH, expectedNodeSets);
  });

  it(`The "Rulesets" tab exists with a heading ${LOCALE_EN_US_SETTINGS.RULESETS} and the expected number of rulesets`, () => {
    // Select the Ruleset tab
    cy.getSettingsTab(RULESET_TAB_INDEX).click();

    const expectedRulesets = 1;
    cy.getSettingsTab(RULESET_TAB_INDEX).should(
      CONTAIN,
      LOCALE_EN_US_SETTINGS.RULESETS,
    );

    cy.getRuleset()
      .find('.app-header')
      .should(
        CONTAIN,
        `${LOCALE_EN_US_RULESETS.RULESETS} (${expectedRulesets})`,
      );
  });

  it(`The "Rulesets" table is populated with rulesets`, () => {
    cy.getSettingsTab(RULESET_TAB_INDEX).click();

    const expectedColumns = 2;
    const expectedRulesets = 1;
    cy.log(`Checking Ruleset table for ${expectedColumns} columns`);
    cy.getRulesetTable()
      .find('thead')
      .find('tr')
      .not('.filters')
      .find('th')
      .children()
      .should(HAVE_LENGTH, expectedColumns)
      .and(CONTAIN, LOCALE_EN_US_RULESETS.NAME);

    cy.log(`Checking Ruleset table for ${expectedRulesets} rows`);
    cy.getRulesetTable()
      .find('tbody')
      .children()
      .should(HAVE_LENGTH, expectedRulesets);

    // prettier-ignore
    cy.getRulesetTable()
      .find('tbody').children()
      .first()
        .children()
          .first().should(CONTAIN, DATA.SAMPLE_RULESET) // rulesetName
  });

  it(`A ruleset file is succesfully downloaded when the download icon is clicked`, () => {
    cy.getSettingsTab(RULESET_TAB_INDEX).click();

    cy.getRulesetTable().find('tbody').children().first().click();

    cy.getRulesetTable()
      .find('tbody')
      .children()
      .first()
      .children()
      .eq(1)
      .find('eui-actionable-icon')
      .click();

    cy.log(`Checking a ruleset file has been downloaded`);

    cy.readFile(`./cypress/downloads/${DATA.SAMPLE_RULESET}.csv`).should(
      EQUAL,
      `moType,attributeName,attributeValue\nENodeBFunction,prachConfigEnabled,true\n`,
    );
  });

  it('A new ruleset can be created from the "Ruleset" tab', () => {
    const rulesetName = 'sample_ruleset_one';

    // Select the Ruleset tab
    cy.getSettingsTab(RULESET_TAB_INDEX).click();

    // Get the ruleset table count and the first row
    cy.getRuleset()
      .find('eui-table')
      .shadow()
      .find('table')
      .find('tbody')
      .find('tr')
      .should(HAVE_LENGTH, 1);

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
      .type(rulesetName);

    cy.getCreateRuleset()
      .find('div[slot="content"]')
      .find('eui-file-input#ruleset-file')
      .shadow()
      .find('input')
      .selectFile('cypress/uploads/SampleRulesetContract.csv', { force: true });

    cy.getCreateRuleset().find('eui-button[slot="bottom"]').click();

    cy.get('div#notifications-column')
      .find('eui-notification')
      .should(CONTAIN, `Ruleset '${rulesetName}' was created.`);

    cy.getRulesetTable()
      .find('table')
      .find('tbody')
      .find('tr')
      .should(HAVE_LENGTH, 2);

    // // First row should be the newly created ruleset
    cy.getRulesetTable()
      .find('table')
      .find('tbody')
      .find('tr')
      .eq(0)
      .find('td')
      .should(CONTAIN, rulesetName);
  });

  it(`When a ruleset row is selected, clicking the delete button results in successful ruleset deletion and a HTTP 204 response.`, () => {
    cy.intercept('DELETE', `/v1/rulesets/${DATA.RULESET_VALUE}`).as(
      'deleteRuleset',
    );

    cy.getSettingsTab(RULESET_TAB_INDEX).click();

    cy.getRulesetTable().find('tbody').children().first().click();

    cy.getRuleset()
      .find('div.action-buttons')
      .find('eui-button#delete-ruleset')
      .click();

    cy.getRuleset()
      .find('eui-dialog#delete-dialog')
      .find('eui-button[slot="bottom"]')
      .click();

    cy.wait('@deleteRuleset').its('response.statusCode').should('eq', 204);

    cy.get('#notifications-column')
      .find('eui-notification')
      .should(
        HAVE_TEXT,
        ` ${LOCALE_EN_US_RULESETS.RULESET} '${DATA.SAMPLE_RULESET}' ${LOCALE_EN_US_RULESETS.DELETED_SUCCESS_MESSAGE}`,
      );
  });

  it('An existing ruleset can be updated from the "Ruleset" tab', () => {
    // Select the Ruleset tab
    cy.getSettingsTab(RULESET_TAB_INDEX).click();

    // Get the ruleset table count and the first row
    cy.getRulesetTable().find('tbody').children().first().click();

    // Open the rulset dialog by clicking on the edit ruleset button
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
      .selectFile('cypress/uploads/UpdatedRulesetContract.csv', {
        force: true,
      });

    cy.getEditRuleset().find('eui-button[slot="bottom"]').click();

    cy.get('div#notifications-column')
      .find('eui-notification')
      .should(CONTAIN, `Ruleset '${DATA.SAMPLE_RULESET}' was updated.`);
  });

  it('When a ruleset is created containing invalid rules then a 400 response code is returned and the error(s) are displayed', () => {
    cy.intercept('POST', `/v1/rulesets`).as('postInvalidRuleset');

    const rulesetName = 'invalidmo_ruleset';

    // Select the Ruleset tab
    cy.getSettingsTab(RULESET_TAB_INDEX).click();

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
      .type(rulesetName);

    cy.getCreateRuleset()
      .find('div[slot="content"]')
      .find('eui-file-input#ruleset-file')
      .shadow()
      .find('input')
      .selectFile('cypress/uploads/InvalidMoRuleset.csv', { force: true });

    cy.getCreateRuleset().find('eui-button[slot="bottom"]').click();

    cy.wait('@postInvalidRuleset').its('response.statusCode').should('eq', 400);

    cy.getRuleValidation()
      .shadow()
      .find('.dialog__title')
      .should(
        CONTAIN,
        LOCALE_EN_US_CREATE_RULESET.RULESET_CREATION_FAILURE_SHORT_DESCRIPTION,
      );

    cy.getRuleValidation()
      .find('div > p')
      .should(CONTAIN, LOCALE_EN_US_CREATE_RULESET.RULESET_HAS_INVALID_RULES);

    // Click the accordion to show the table
    cy.getRuleValidation().find('eui-accordion').click();

    cy.getSettingsErrorTable()
      .find('thead')
      .find('tr:not(.filters)')
      .find('th')
      .children()
      .should(HAVE_LENGTH, 2)
      .and(CONTAIN, LOCALE_EN_US_CREATE_RULESET.LINE)
      .and(CONTAIN, LOCALE_EN_US_CREATE_RULESET.REASON);

    // prettier-ignore
    cy.getSettingsErrorTable()
    .find('tbody')
    .children()
    .first() // row 1
      .children()
        .first().should(CONTAIN, 2)                                                          // Line
        .next().find('eui-tooltip').should(CONTAIN, 'MO not found in Managed Object Model.') // Reason

    // close the dialog
    cy.getRuleValidation().find('eui-button').click();
  });
});
