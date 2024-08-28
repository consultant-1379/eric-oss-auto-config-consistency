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
import {
  CONTAIN,
  MATCH,
  NOT_BE_EMPTY,
  DIALOG_WAIT_PERIOD,
  PAGE_CHANGE_PERIOD,
  UI_RENDERING_PERIOD,
  POLLING_TIMEOUT,
  DIGIT_REGEX,
  CHANGES_POLLING_TIMEOUT,
  APPLY_CHANGES_TIMEOUT,
  EXECUTIONS_TABLE_POLLING_TIMEOUT,
} from '../utils/testUtils.js';
import {
  EACC_EXECUTIONS_URL,
  JOB_NAME_4G,
  JOB_NAME_5G,
  TEST_EXECUTIONS,
} from '../utils/testData.js';
import * as TABS_LOCALE_EN_US from '../../../../src/components/executions-tab-area/locale/en-us.json';
import * as AUDIT_TAB_LOCALE_EN_US from '../../../../src/components/audit-report-tab/locale/en-us.json';
import { ChangeStatus } from '../../../../src/utils/attributes/changeAttributes.js';

const USE_CASE_ID = 'EACC-UC_03';

/**
 * UC_03-01: It shall be possible to view the executions and execution-reports.
 */
describe(`${USE_CASE_ID}: View executions and execution reports`, () => {
  before(() => {
    cy.visit(EACC_EXECUTIONS_URL);
    cy.wait(UI_RENDERING_PERIOD);
  });
  it(`${USE_CASE_ID}-01: View execution results for '${JOB_NAME_4G}' and '${JOB_NAME_5G}' then verify audit results`, () => {
    TEST_EXECUTIONS.forEach(execution => {
      const requiredExecution = cy
        .getExecutionsTable(EXECUTIONS_TABLE_POLLING_TIMEOUT)
        .getRowByKey(execution.jobName, POLLING_TIMEOUT);

      // prettier-ignore
      requiredExecution
                .children()
                .first().contains('span', DIGIT_REGEX)                                             // id
                .parents('td')
                .next().should(CONTAIN, execution.jobName)                                         // jobName
                .next().should(CONTAIN, execution.executionType)                                   // executionType
                .next().find('span').should(NOT_BE_EMPTY)                                          // executionStartedAt
                .parents('td')
                .next().find('span', {timeout: UI_RENDERING_PERIOD}).should(NOT_BE_EMPTY)          // executionEndedAt
                .parents('td')
                .next({timeout: UI_RENDERING_PERIOD}).should(CONTAIN, execution.status)            // executionStatus
                .next({timeout: UI_RENDERING_PERIOD}).contains('span', DIGIT_REGEX)                // totalAttributesAudited
                .parents('td')
                .next().contains('span', DIGIT_REGEX)                                              // totalCellsAudited
                .parents('td')
                .next().contains('span', DIGIT_REGEX); // inconsistenciesIdentified

      // click on the execution row
      requiredExecution.click();

      // View the execution reports
      cy.getExecutionsApp()
        .find('.app-header')
        .find('eui-button#viewReports')
        .click();

      // Verify the url is as expected
      const reportsUrl = `${EACC_EXECUTIONS_URL}/execution-reports\\?executionId=\\d+&jobName=${execution.jobName}`;
      cy.url().should(MATCH, new RegExp(reportsUrl, 'g'));

      cy.wait(PAGE_CHANGE_PERIOD);

      // Proposed changes are displayed by default.
      // With virtual scroll the table is too large to render all rows, the results count is the length of the table
      cy.getAuditReportsTabTile()
        .find('.row-count-text')
        .invoke('text')
        .should(CONTAIN, `${execution.reports.filteredSize} items`);

      // Click the pill to show the entire audit table.
      cy.getAuditReportsTabTile().find('eui-pill').click();

      cy.getAuditReportsTabTile()
        .find('.row-count-text')
        .invoke('text')
        .should(CONTAIN, `${execution.reports.unfilteredSize} items`);

      cy.getExecutionsReportsTab(0).click();
      cy.visit(EACC_EXECUTIONS_URL);
    });
  });

  it(`${USE_CASE_ID}-02: Filter the audit reports for '${JOB_NAME_4G}' and '${JOB_NAME_5G}' then verify results`, () => {
    TEST_EXECUTIONS.forEach(execution => {
      const requiredExecution = cy
        .getExecutionsTable()
        .getRowByKey(execution.jobName, POLLING_TIMEOUT);

      requiredExecution.children().first().click();

      cy.getExecutionsApp()
        .find('.app-header')
        .find('eui-button#viewReports')
        .click();

      cy.wait(PAGE_CHANGE_PERIOD);

      cy.getAuditReportsTabTile().find('eui-panel-button').click();

      // Filter by partial FDN
      cy.getAuditReportsTabTile()
        .find('eui-tile-panel')
        .find('#managed-object-text-field')
        .shadow()
        .find('input')
        .type(execution.filtering.partialFDN);

      cy.getAuditReportsTabTile()
        .find('eui-tile-panel')
        .find('#apply-filters-button')
        .click();

      // Partial FDN AND inconsistent results
      cy.getAuditReportsTabTile()
        .find('.row-count-text')
        .invoke('text')
        .should(
          CONTAIN,
          `${execution.filtering.inconsistentCountForPartialMO} item`,
        );

      cy.getAuditReportsTabTile().find('eui-pill').click();

      // Partial FDN AND all results
      cy.getAuditReportsTabTile()
        .find('.row-count-text')
        .invoke('text')
        .should(CONTAIN, `${execution.filtering.countForPartialMO} items`);

      // Clear filters
      cy.getAuditReportsTabTile()
        .find('eui-tile-panel')
        .find('#reset-filters-button')
        .click();

      cy.getAuditReportsTabTile().find('eui-pill').click();

      // Filter by full FDN
      cy.getAuditReportsTabTile()
        .find('eui-tile-panel')
        .find('#managed-object-text-field')
        .shadow()
        .find('input')
        .type(execution.filtering.fullFDN);

      cy.getAuditReportsTabTile()
        .find('eui-tile-panel')
        .find('#apply-filters-button')
        .click();

      // Full FDN and inconsistent results
      cy.getAuditReportsTabTile()
        .find('.row-count-text')
        .invoke('text')
        .should(
          CONTAIN,
          `${execution.filtering.inconsistentCountForFullMO} item`,
        );

      cy.getAuditReportsTabTile().find('eui-pill').click();

      // Full FDN and all results
      cy.getAuditReportsTabTile()
        .find('.row-count-text')
        .invoke('text')
        .should(CONTAIN, `${execution.filtering.countForFullMO} items`);

      // Clear filters
      cy.getAuditReportsTabTile()
        .find('eui-tile-panel')
        .find('#reset-filters-button')
        .click();

      cy.getAuditReportsTabTile().find('eui-actionable-icon').click();
      cy.getExecutionsReportsTab(0).click();
      cy.visit(EACC_EXECUTIONS_URL);
    });
  });

  it(`${USE_CASE_ID}-03: Apply changes for '${JOB_NAME_4G}' and '${JOB_NAME_5G}' then verify results`, () => {
    TEST_EXECUTIONS.forEach(execution => {
      const requiredExecution = cy
        .getExecutionsTable()
        .getRowByKey(execution.jobName, POLLING_TIMEOUT);

      requiredExecution.children().first().click();

      cy.getExecutionsApp()
        .find('.app-header')
        .find('eui-button#viewReports')
        .click();

      cy.wait(CHANGES_POLLING_TIMEOUT);

      if (!execution.applyAll) {
        cy.getAuditReportsTable()
          .find('thead')
          .find('tr')
          .not('.filters')
          .find('th')
          .first()
          .find('eui-checkbox')
          .shadow()
          .find('input')
          .click({ force: true });

        cy.getAuditReportsTabTile()
          .find('e-apply-button#applySelected')
          .click();
      } else {
        cy.getAuditReportsTabTile().find('e-apply-button#applyAll').click();
      }

      cy.getAuditReportsTabTile()
        .find('eui-dialog')
        .find('eui-button#confirm-changes')
        .click();

      cy.get('#notifications-column')
        .find('eui-notification')
        .should(CONTAIN, `${AUDIT_TAB_LOCALE_EN_US.AUDIT_RESULTS_UPDATED}`);

      cy.wait(CHANGES_POLLING_TIMEOUT);

      cy.getExecutionsReportsTab(1).click();

      cy.getExecutionsReportsTabsCurrentHeaderTile()
        .shadow()
        .find('.tile__header__left__title')
        .should(CONTAIN, `${TABS_LOCALE_EN_US.CHANGES}`);

      cy.wait(APPLY_CHANGES_TIMEOUT);

      cy.getExecutionsReportsSelectedTab()
        .find('.changes-selection')
        .invoke('text')
        .should(
          MATCH,
          new RegExp(
            `${execution.changes.expectedRows}\\n\\s+${TABS_LOCALE_EN_US.ITEMS}`,
          ),
        );

      cy.getExecutionsReportsChangesTable()
        .find('tbody')
        .children()
        .siblings()
        .find('td')
        .eq(7)
        .find('span')
        .invoke('text')
        .should(CONTAIN, ChangeStatus.IMPLEMENTATION_COMPLETE);

      cy.getExecutionsReportsTab(0).click();
      cy.visit(EACC_EXECUTIONS_URL);
    });
  });

  it(`${USE_CASE_ID}-04: Revert changes for '${JOB_NAME_4G}' and '${JOB_NAME_5G}' then verify results`, () => {
    TEST_EXECUTIONS.forEach(execution => {
      const requiredExecution = cy
        .getExecutionsTable()
        .getRowByKey(execution.jobName, POLLING_TIMEOUT);

      requiredExecution.children().first().click();

      cy.getExecutionsApp()
        .find('.app-header')
        .find('eui-button#viewReports')
        .click();

      cy.wait(PAGE_CHANGE_PERIOD);

      cy.getExecutionsReportsTab(1).click();

      cy.wait(CHANGES_POLLING_TIMEOUT);

      if (!execution.revertAll) {
        cy.getExecutionsReportsChangesTable()
          .find('thead')
          .find('tr')
          .not('.filters')
          .find('th')
          .first()
          .find('eui-checkbox')
          .shadow()
          .find('input')
          .click({ force: true });

        cy.getExecutionsReportsSelectedTab()
          .find('.tab-header-changes')
          .find('#action-buttons')
          .find('#revertSelected')
          .click();
      } else {
        cy.getExecutionsReportsSelectedTab()
          .find('.tab-header-changes')
          .find('#action-buttons')
          .find('#revertAll')
          .click();
      }

      // Fix for IDUN-117132, allow the dialog to appear.
      cy.wait(DIALOG_WAIT_PERIOD);

      cy.getExecutionsReportsSelectedTab()
        .find('e-changes-poll')
        .shadow()
        .find('#revertDialog')
        .find('eui-button')
        .click({ force: true });

      cy.wait(APPLY_CHANGES_TIMEOUT);

      cy.getExecutionsReportsChangesTable()
        .find('tbody')
        .children()
        .siblings()
        .find('td')
        .eq(7)
        .find('span')
        .invoke('text')
        .should(CONTAIN, ChangeStatus.REVERSION_COMPLETE);

      cy.getExecutionsReportsTab(0).click();
      cy.visit(EACC_EXECUTIONS_URL);
    });
  });
});
