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

Cypress.Commands.add('interceptModules', () => {
  cy.intercept('/libs/shared/@eui/**').as('eui-shared');
  cy.intercept('/libs/@eui/**').as('eui-libs');
  cy.intercept('/libs/pkg/**').as('misc');
});

Cypress.Commands.add('waitForModules', () => {
  cy.wait(['@eui-shared', '@eui-libs', '@misc']);
});

Cypress.Commands.add('getExecutionsApp', () => {
  cy.get('eui-container').shadow().find('e-executions').shadow();
});

Cypress.Commands.add('getExecutionsTable', () => {
  cy.getExecutionsApp().find('eui-table').shadow().find('table');
});

Cypress.Commands.add('getExecutionsReportsApp', () => {
  cy.get('eui-container').shadow().find('e-execution-reports').shadow();
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

Cypress.Commands.add('getExecutionsReportsSelectedTab', () => {
  cy.getExecutionsReportsApp()
    .find('e-executions-tab-area')
    .shadow()
    .find('eui-tabs')
    .find('div[selected="true"]')
    .first();
});

Cypress.Commands.add('getAuditReportsTabTile', () => {
  cy.getExecutionsReportsSelectedTab()
    .find('e-audit-report-tab')
    .shadow()
    .find('eui-multi-panel-tile');
});

Cypress.Commands.add('getExecutionsReportsTabsCurrentHeader', () => {
  cy.getExecutionsReportsSelectedTab().find('.tab-header');
});

Cypress.Commands.add('getExecutionsReportsTabsCurrentHeaderTile', () => {
  cy.getExecutionsReportsSelectedTab().find('eui-tile');
});

Cypress.Commands.add('getExecutionsReportsCurrentTable', () => {
  cy.getExecutionsReportsSelectedTab()
    .find('e-rest-paginated-table')
    .shadow()
    .find('eui-table')
    .shadow()
    .find('table');
});

Cypress.Commands.add('getAuditReportsTable', () => {
  cy.getAuditReportsTabTile()
    .find('e-rest-paginated-table')
    .shadow()
    .find('eui-table')
    .shadow()
    .find('table');
});

Cypress.Commands.add('getExecutionsReportsPagination', () => {
  cy.getExecutionsReportsSelectedTab()
    .find('e-audit-report-tab')
    .shadow()
    .find('e-rest-paginated-table')
    .shadow()
    .find('eui-pagination');
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

Cypress.Commands.add('getExecutionsReportsPaginationForChanges', () => {
  cy.getExecutionsReportsSelectedTab()
    .find('e-changes-poll')
    .shadow()
    .find('e-rest-paginated-table')
    .shadow()
    .find('eui-pagination');
});

Cypress.Commands.add('getJobsApp', () => {
  cy.get('eui-container').shadow().find('e-jobs').shadow();
});

Cypress.Commands.add('getJobsTable', () => {
  cy.getJobsApp().find('eui-table').shadow().find('table');
});

Cypress.Commands.add('getSettingsApp', () => {
  cy.get('eui-container').shadow().find('e-settings').shadow();
});

Cypress.Commands.add('getSettingsTabs', () => {
  cy.getSettingsApp().find('eui-tabs');
});

// Use this to select the tab you want
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

Cypress.Commands.add('getCreateRuleset', () => {
  cy.getRuleset()
    .find('e-create-ruleset#create-ruleset')
    .shadow()
    .find('eui-dialog#createRuleset');
});

Cypress.Commands.add('getRuleValidation', () => {
  cy.getRuleset()
    .find('e-create-ruleset#create-ruleset')
    .shadow()
    .find('eui-dialog#ruleValidationDialog');
});

Cypress.Commands.add('getSettingsErrorTable', () => {
  cy.getRuleValidation()
    .find('eui-accordion')
    .find('e-settings-error-table')
    .shadow();
});

Cypress.Commands.add('getCreateJobApp', () => {
  cy.get('eui-container').shadow().find('e-create-job').shadow();
});

Cypress.Commands.add('getEditScope', () => {
  cy.getScope()
    .find('e-create-scope#edit-scope')
    .shadow()
    .find('eui-dialog#createScope');
});

Cypress.Commands.add('getEditRuleset', () => {
  cy.getRuleset()
    .find('e-create-ruleset#edit-ruleset')
    .shadow()
    .find('eui-dialog#createRuleset');
});

Cypress.Commands.add('getRuleset', () => {
  cy.getSettingsTabs().find('div[selected="true"]').find('e-ruleset').shadow();
});

Cypress.Commands.add('getRulesetTable', () => {
  cy.getRuleset().find('eui-table').shadow();
});
