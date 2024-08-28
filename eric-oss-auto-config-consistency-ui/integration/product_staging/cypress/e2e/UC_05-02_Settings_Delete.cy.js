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

import { SCOPE_TAB_INDEX, RULESET_TAB_INDEX } from '../utils/eaccUIUtils.js';
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

const USE_CASE_ID = 'EACC-UC_05';

/**
 * UC_05-02: It shall be possible to delete existing EACC settings e.g. node sets, rule sets.
 */
describe(`${USE_CASE_ID}: Delete existing EACC settings`, () => {
  before(() => {
    cy.visit(EACC_SETTINGS_URL);
    cy.wait(UI_RENDERING_PERIOD);
  });
  it(`${USE_CASE_ID}-02: Delete; The node set '${NODE_SET_NAME_4G}' and '${NODE_SET_NAME_5G}' should be deleted`, () => {
    // Select the Scope tab
    cy.getSettingsTab(SCOPE_TAB_INDEX).click();

    TEST_SCOPES.forEach(scope => {
      cy.getScopesTable().getRowByKey(scope.name).click();

      cy.getScope()
        .find('div.action-buttons')
        .find('eui-button#delete-scope')
        .click();

      cy.getScope()
        .find('eui-dialog#delete-dialog')
        .find('eui-button[slot="bottom"]')
        .click();

      cy.get('#notifications-column')
        .find('eui-notification')
        .should(CONTAIN, `Node set '${scope.name}' was deleted.`);

      cy.wait(NOTIFICATION_WAIT_PERIOD);
    });
  });
  it(`${USE_CASE_ID}-03: Delete; The ruleset '${RULESET_NAME_4G}' and '${RULESET_NAME_5G}' should be deleted`, () => {
    // Select the Ruleset tab
    cy.getSettingsTab(RULESET_TAB_INDEX).click();

    TEST_RULESETS.forEach(ruleset => {
      cy.getRulesetTable().getRowByKey(ruleset.name).click();

      cy.getRuleset()
        .find('div.action-buttons')
        .find('eui-button#delete-ruleset')
        .click();

      cy.getRuleset()
        .find('eui-dialog#delete-dialog')
        .find('eui-button[slot="bottom"]')
        .click();

      cy.get('#notifications-column')
        .find('eui-notification')
        .should(CONTAIN, `Ruleset '${ruleset.name}' was deleted.`);

      cy.wait(NOTIFICATION_WAIT_PERIOD);
    });
  });
});
