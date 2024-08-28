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

import { getGasUsername, getGasPassword } from '../utils/eaccEnvironment.js';
import { CONTAIN, LOGIN_REDIRECT_PERIOD } from '../utils/testUtils.js';
import { APP_ID } from '../utils/testData.js';

// GAS Login section.

Cypress.Commands.add('getGASLoginForm', () => {
  cy.get('div#kc-form').find('form');
});

Cypress.Commands.add('login', () => {
  cy.session('GAS_Login', () => {
    cy.visit('/');
    cy.getGASLoginForm().find('input#username').type(getGasUsername());
    cy.getGASLoginForm().find('input#password').type(getGasPassword());
    cy.getGASLoginForm().find('input#kc-login-input').click();
    // A valid login should bring the user to the launcher page.
    cy.wait(LOGIN_REDIRECT_PERIOD);
    cy.url().should(CONTAIN, '/#launcher');
  });
});

Cypress.Commands.add('getEaccAppCard', () => {
  cy.get('e-launcher', { includeShadowDom: true })
    .shadow()
    .find('e-launcher-component')
    .shadow()
    .find('e-app-view')
    .shadow()
    .find('e-card-container')
    .shadow()
    .find(`e-app-card[display-name="${APP_ID}"]`);
});

// Settings section.

Cypress.Commands.add('getSettingsApp', () => {
  cy.get('eui-container').shadow().find('e-settings').shadow();
});

Cypress.Commands.add('getSettingsTabs', () => {
  cy.getSettingsApp().find('eui-tabs');
});

// Use this to select the settings tab you want
Cypress.Commands.add('getSettingsTab', index => {
  cy.getSettingsApp().find('eui-tabs').find('eui-tab').eq(index).find('label');
});

Cypress.Commands.add('getScope', () => {
  cy.getSettingsTabs().find('div[selected="true"]').find('e-scope').shadow();
});

Cypress.Commands.add('getScopesTable', () => {
  cy.getScope().find('eui-table').shadow();
});

Cypress.Commands.add('getCreateScope', () => {
  cy.getScope()
    .find('e-create-scope#create-scope')
    .shadow()
    .find('eui-dialog#createScope');
});

Cypress.Commands.add('getEditScope', () => {
  cy.getScope()
    .find('e-create-scope#edit-scope')
    .shadow()
    .find('eui-dialog#createScope');
});

Cypress.Commands.add('getCreateJobApp', () => {
  cy.get('eui-container').shadow().find('e-create-job').shadow();
});

Cypress.Commands.add('selectDropdownValue', (dropdownId, value) => {
  cy.getCreateJobApp()
    .find(`[id=${dropdownId}]`)
    .shadow()
    .find('eui-menu')
    .find(`eui-menu-item[label=${value}]`)
    .click({ force: true });
});

Cypress.Commands.add('getJobsApp', () => {
  cy.get('eui-container').shadow().find('e-jobs').shadow();
});

Cypress.Commands.add('getJobsTable', () => {
  cy.getJobsApp().find('eui-table').shadow().find('table');
});

// This command is intended to be chained off a table getter
Cypress.Commands.add(
  'getRowByKey',
  { prevSubject: true },
  (table, key, pollingTimeout = 4000) => {
    cy.wrap(table)
      .find('tbody')
      .children()
      .find(`div:contains("${key}")`, { timeout: pollingTimeout })
      .parents('tr');
  },
);

Cypress.Commands.add('getCreateJobButton', () => {
  cy.get('eui-container')
    .shadow()
    .find('eui-app-bar')
    .shadow()
    .find('eui-button')
    .shadow()
    .find('a');
});

// Executions section
Cypress.Commands.add('getExecutionsApp', () => {
  cy.get('eui-container').shadow().find('e-executions').shadow();
});

Cypress.Commands.add('getExecutionsTable', (pollingTimeout = 4000) => {
  cy.getExecutionsApp()
    .find('eui-table', { timeout: pollingTimeout })
    .shadow()
    .find('table');
});

// Execution Reports section
Cypress.Commands.add('getExecutionsReportsApp', () => {
  cy.get('eui-container').shadow().find('e-execution-reports').shadow();
});

Cypress.Commands.add('getExecutionsReportsSelectedTab', () => {
  cy.getExecutionsReportsApp()
    .find('e-executions-tab-area')
    .shadow()
    .find('eui-tabs')
    .find('div[selected="true"]')
    .first();
});

Cypress.Commands.add('getExecutionsReportsCurrentTable', () => {
  cy.getExecutionsReportsSelectedTab()
    .find('e-audit-report-table')
    .shadow()
    .find('table');
});

Cypress.Commands.add('getAuditReportsTabTile', () => {
  cy.getExecutionsReportsSelectedTab()
    .find('e-audit-report-tab')
    .shadow()
    .find('eui-multi-panel-tile');
});

Cypress.Commands.add('getRuleset', () => {
  cy.getSettingsTabs().find('div[selected="true"]').find('e-ruleset').shadow();
});

Cypress.Commands.add('getCreateRuleset', () => {
  cy.getRuleset()
    .find('e-create-ruleset#create-ruleset')
    .shadow()
    .find('eui-dialog#createRuleset');
});

Cypress.Commands.add('getEditRuleset', () => {
  cy.getRuleset()
    .find('e-create-ruleset#edit-ruleset')
    .shadow()
    .find('eui-dialog#createRuleset');
});

Cypress.Commands.add('getRulesetTable', () => {
  cy.getRuleset().find('eui-table').shadow();
});

Cypress.Commands.add('getExecutionsReportsChangesTable', () => {
  cy.getExecutionsReportsSelectedTab()
    .find('e-changes-poll')
    .shadow()
    .find('e-rest-paginated-table')
    .shadow()
    .find('eui-table')
    .shadow()
    .find('table');
});

Cypress.Commands.add('getExecutionsReportsTab', index => {
  cy.getExecutionsReportsApp()
    .find('e-executions-tab-area')
    .shadow()
    .find('eui-tabs')
    .find('eui-tab')
    .eq(index)
    .find('label');
});

Cypress.Commands.add('getExecutionsReportsTabsCurrentHeaderTile', () => {
  cy.getExecutionsReportsSelectedTab().find('eui-tile');
});

Cypress.Commands.add('getAuditReportsTable', () => {
  cy.getAuditReportsTabTile()
    .find('e-rest-paginated-table')
    .shadow()
    .find('eui-table')
    .shadow()
    .find('table');
});
