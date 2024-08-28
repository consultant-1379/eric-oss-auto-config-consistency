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

import { AuditStatus } from '../../../../src/utils/attributes/auditAttributes.js';
import {
  ChangeStatus,
  Operation,
} from '../../../../src/utils/attributes/changeAttributes.js';

import * as LOCALE_EN_US from '../../../../src/apps/execution-reports/locale/en-us.json';
import * as TABS_LOCALE_EN_US from '../../../../src/components/executions-tab-area/locale/en-us.json';
import * as CHANGES_LOCALE_EN_US from '../../../../src/components/changes-poll/locale/en-us.json';
import * as AUDIT_TAB_LOCALE_EN_US from '../../../../src/components/audit-report-tab/locale/en-us.json';

import * as DATA from '../utils/testData.js';
import {
  CONTAIN,
  HAVE_LENGTH,
  HAVE_ATTR,
  HAVE_TEXT,
  NOT_HAVE_ATTR,
  MATCH,
} from '../utils/testUtils.js';

const JOB_NAME = `${DATA.ALL_IRELAND}-${DATA.ID_ONE}`;
/* This is hardcoded to 2000 for now in the client so faking it here for now. Will be looked at in IDUN-106443 */
const NUM_ENTRIES = '2000';

describe('Visit the execution-reports app', () => {
  beforeEach(() => {
    cy.interceptModules();
    cy.visit(
      `/#eacc/executions/execution-reports?executionId=${DATA.ID_ONE}&jobName=${JOB_NAME}`,
    );
  });
  it(`The breadcrumb is: ${LOCALE_EN_US.AUTOMATED_CONFIGURATION_CONSISTENCY} > ${LOCALE_EN_US.EXECUTIONS_OVERVIEW} > ${JOB_NAME}`, () => {
    cy.wait(500);

    cy.get('eui-app-bar', { includeShadowDom: true })
      .shadow()
      .find('eui-breadcrumb')
      .shadow()
      .find('#breadcrumb')
      .children('.breadcrumb-wrap')
      .should(HAVE_LENGTH, 3)
      .first()
      .find('eui-tooltip')
      .should(CONTAIN, LOCALE_EN_US.AUTOMATED_CONFIGURATION_CONSISTENCY)
      .parent()
      .next()
      .find('eui-tooltip')
      .should(CONTAIN, LOCALE_EN_US.EXECUTIONS_OVERVIEW)
      .parent()
      .next()
      .find('span')
      .should(CONTAIN, JOB_NAME);

    // clicking the executions overview should bring us back to executions
    cy.get('eui-app-bar', { includeShadowDom: true })
      .shadow()
      .find('eui-breadcrumb')
      .shadow()
      .find('#breadcrumb')
      .children('.breadcrumb-wrap')
      .eq(1)
      .click();

    cy.url().should(CONTAIN, '/#eacc/executions');
  });

  it(`The "${TABS_LOCALE_EN_US.AUDIT_REPORT}" tab is present and has a tile with title ${AUDIT_TAB_LOCALE_EN_US.RESULTS}`, () => {
    cy.getExecutionsReportsTab(0)
      .should(CONTAIN, TABS_LOCALE_EN_US.AUDIT_REPORT)
      .parent()
      .should(HAVE_ATTR, 'selected');

    cy.getAuditReportsTabTile().should(
      HAVE_ATTR,
      'tile-title',
      AUDIT_TAB_LOCALE_EN_US.RESULTS,
    );
  });
  it(`The "${TABS_LOCALE_EN_US.AUDIT_REPORT}" table contains all the audit results`, () => {
    const expectedRows = 4;
    cy.getAuditReportsTabTile()
      .find('.row-count-text')
      .invoke('text')
      .should(CONTAIN, `${expectedRows} ${AUDIT_TAB_LOCALE_EN_US.ITEMS}`);

    cy.getAuditReportsTable()
      .find('thead')
      .find('tr')
      .not('.filters')
      .find('th')
      .children()
      .and(CONTAIN, AUDIT_TAB_LOCALE_EN_US.AUDIT_ID)
      .and(CONTAIN, AUDIT_TAB_LOCALE_EN_US.MANAGED_OBJECT)
      .and(CONTAIN, AUDIT_TAB_LOCALE_EN_US.MO_CLASS)
      .and(CONTAIN, AUDIT_TAB_LOCALE_EN_US.ATTRIBUTE_NAME)
      .and(CONTAIN, AUDIT_TAB_LOCALE_EN_US.CURRENT_VALUE)
      .and(CONTAIN, AUDIT_TAB_LOCALE_EN_US.PREFERRED_VALUE)
      .and(CONTAIN, AUDIT_TAB_LOCALE_EN_US.AUDIT_STATUS)
      .and(CONTAIN, AUDIT_TAB_LOCALE_EN_US.EXECUTION_ID);

    // prettier-ignore
    // By default inconsistent results will be shown first
    cy.getAuditReportsTable()
            .find('tbody')
            .children()
            .should('have.length', 4)
            .first() // row 1
            .children()
            .first()
            .next().should(CONTAIN, DATA.ID_TWO)                   // id
            .next().should(CONTAIN, DATA.MANAGED_OBJECT_TWO)       // managedObjectFdn
            .next().should(CONTAIN, DATA.NR_CELL_DU)               // managedObjectType
            .next().should(CONTAIN, DATA.SUB_CARRIER_SPACING)      // attributeName
            .next().should(CONTAIN, 120)                           // currentValue
            .next().should(CONTAIN, 110)                           // preferredValue
            .next().should(CONTAIN, AuditStatus.INCONSISTENT)      // auditStatus
            .next().should(CONTAIN, DATA.ID_ONE)                   // executionId
            .parent()
            .next() // row 2
            .children()
            .first()
            .next().should(CONTAIN, DATA.ID_THREE)
            .next().should(CONTAIN, DATA.MANAGED_OBJECT_THREE)
            .next().should(CONTAIN, DATA.NR_CELL_CU)
            .next().should(CONTAIN, DATA.MCPC_PS_CELL_ENABLED)
            .next().should(CONTAIN, false)
            .next().should(CONTAIN, true)
            .next().should(CONTAIN, AuditStatus.INCONSISTENT)
            .next().should(CONTAIN, DATA.ID_ONE)
            .parent()
            .next() // row 3
            .children()
            .first()
            .next().should(CONTAIN, DATA.ID_FOUR)
            .next().should(CONTAIN, DATA.MANAGED_OBJECT_FOUR)
            .next().should(CONTAIN, DATA.GNBCUCP_FUNCTION)
            .next().should(CONTAIN, DATA.MAX_NG_RETRY_TIME)
            .next().should(CONTAIN, 30)
            .next().should(CONTAIN, 20)
            .next().should(CONTAIN, AuditStatus.INCONSISTENT)
            .next().should(CONTAIN, DATA.ID_ONE)
            .parent()
            .next() // row 4
            .children()
            .first()
            .next().should(CONTAIN, DATA.ID_FIVE)
            .next().should(CONTAIN, DATA.MANAGED_OBJECT_FOUR)
            .next().should(CONTAIN, DATA.GNBCUCP_FUNCTION)
            .next().should(CONTAIN, DATA.ENDPOINT_RES_DEP_H_ENABLED)
            .next().should(CONTAIN, true)
            .next().should(CONTAIN, false)
            .next().should(CONTAIN, AuditStatus.INCONSISTENT)
            .next().should(CONTAIN, DATA.ID_ONE);

    cy.getExecutionsReportsPagination()
      .should(HAVE_ATTR, 'current-page', '1')
      .should(HAVE_ATTR, 'num-entries', NUM_ENTRIES)
      .should(HAVE_ATTR, 'num-pages', '1');
  });

  it(`The "${TABS_LOCALE_EN_US.AUDIT_REPORT}" table contains dialog to apply all and  apply selected`, () => {
    cy.getExecutionsReportsTab(0).click();

    // Test dialog for apply all
    cy.getAuditReportsTabTile()
      .find('e-apply-button#applyAll')
      .should(NOT_HAVE_ATTR, 'hidden');

    cy.getAuditReportsTabTile()
      .find('e-apply-button#applySelected')
      .should(HAVE_ATTR, 'hidden');

    // Test dialog for apply all. Need to wait as polling to check if anything is in progress
    // doesn't kick in for 15 secs so button is disabled till then.
    cy.wait(17000);
    cy.getAuditReportsTabTile().find('e-apply-button#applyAll').click();

    cy.getAuditReportsTabTile()
      .find('eui-dialog#changesDialog')
      .invoke('attr', 'label')
      .should(CONTAIN, AUDIT_TAB_LOCALE_EN_US.CONFIRM_CHANGES);

    cy.getAuditReportsTabTile()
      .find('eui-dialog#changesDialog')
      .find('div')
      .and(CONTAIN, AUDIT_TAB_LOCALE_EN_US.APPLY_CONFIRMATION);

    cy.getAuditReportsTabTile()
      .find('eui-dialog#changesDialog')
      .shadow()
      .find('eui-button.cancel')
      .click();

    // Test dialog for apply selected
    cy.getAuditReportsTable(0).find('tbody').children().first().click();

    cy.getAuditReportsTabTile()
      .find('e-apply-button#applySelected')
      .should(NOT_HAVE_ATTR, 'hidden');

    cy.getAuditReportsTabTile()
      .find('e-apply-button#applyAll')
      .should(HAVE_ATTR, 'hidden');

    cy.getAuditReportsTabTile().find('e-apply-button#applySelected').click();

    cy.getAuditReportsTabTile()
      .find('eui-dialog')
      .find('div')
      .and(CONTAIN, AUDIT_TAB_LOCALE_EN_US.APPLY_CONFIRMATION);
  });

  it(`The "${TABS_LOCALE_EN_US.AUDIT_REPORT}" table shows selection count only when an audit result is selected`, () => {
    cy.getExecutionsReportsTab(0).click();
    cy.getAuditReportsTabTile().should(
      NOT_HAVE_ATTR,
      'subtitle',
      `${AUDIT_TAB_LOCALE_EN_US.SELECTED} (1)`,
    );
    cy.getAuditReportsTable().find('tbody').children().first().click();

    cy.getAuditReportsTabTile().should(
      HAVE_ATTR,
      'subtitle',
      `${AUDIT_TAB_LOCALE_EN_US.SELECTED} (1)`,
    );
  });

  it(`Apply multiple audit results will return a 202 Accepted status code`, () => {
    cy.intercept('POST', `/v1/executions/${DATA.ID_ONE}/audit-results`).as(
      'postAuditResults',
    );

    cy.getExecutionsReportsTab(0).click();

    cy.getAuditReportsTable()
      .find('tbody')
      .children()
      .first()
      .find('eui-checkbox')
      .shadow()
      .find('input')
      .click({ force: true });

    cy.getAuditReportsTable()
      .find('tbody')
      .children()
      .first()
      .next()
      .find('eui-checkbox')
      .shadow()
      .find('input')
      .click({ force: true });

    cy.getAuditReportsTable()
      .find('tbody')
      .children()
      .first()
      .next()
      .next()
      .find('eui-checkbox')
      .shadow()
      .find('input')
      .click({ force: true });

    cy.getAuditReportsTabTile().find('e-apply-button#applySelected').click();

    cy.getAuditReportsTabTile()
      .find('eui-dialog')
      .find('eui-button#confirm-changes')
      .click();

    cy.wait('@postAuditResults').then(interception => {
      expect(interception.request.body).to.deep.equal({
        auditResultIds: ['2', '3', '4'],
        approveForAll: false,
        operation: Operation.APPLY_CHANGE,
      });
      expect(interception.response.statusCode).to.deep.equal(202);
    });

    cy.get('#notifications-column')
      .find('eui-notification')
      .should(CONTAIN, `${AUDIT_TAB_LOCALE_EN_US.AUDIT_RESULTS_UPDATED}`);
  });

  it(`Apply all audit results will return a 202 Accepted status code`, () => {
    cy.intercept('POST', `/v1/executions/${DATA.ID_ONE}/audit-results`).as(
      'postAuditResults',
    );

    cy.getExecutionsReportsTab(0).click();
    // Need to wait as polling to check if anything is in progress
    // doesn't kick in for 15 secs so button is disabled till then.
    cy.wait(17000);
    cy.getAuditReportsTabTile().find('e-apply-button#applyAll').click();

    cy.getAuditReportsTabTile()
      .find('eui-dialog')
      .find('eui-button#confirm-changes')
      .click();

    cy.wait('@postAuditResults').then(interception => {
      expect(interception.request.body).to.deep.equal({
        approveForAll: true,
        operation: Operation.APPLY_CHANGE,
      });
      expect(interception.response.statusCode).to.deep.equal(202);
    });

    cy.get('#notifications-column')
      .find('eui-notification')
      .should(CONTAIN, `${AUDIT_TAB_LOCALE_EN_US.AUDIT_RESULTS_UPDATED}`);
  });

  it(`The "${TABS_LOCALE_EN_US.CHANGES}" table contains all the relevant audit results`, () => {
    cy.getExecutionsReportsTab(1).click();
    cy.getExecutionsReportsTabsCurrentHeaderTile()
      .shadow()
      .find('.tile__header__left__title')
      .should(CONTAIN, `${TABS_LOCALE_EN_US.CHANGES}`);

    cy.getExecutionsReportsSelectedTab()
      .find('.changes-selection')
      .invoke('text')
      .should(MATCH, new RegExp(`${4}\\n\\s+${TABS_LOCALE_EN_US.ITEMS}`));

    cy.getExecutionsReportsChangesTable()
      .find('thead')
      .find('tr')
      .not('.filters')
      .find('th')
      .children()
      .and(CONTAIN, CHANGES_LOCALE_EN_US.AUDIT_ID)
      .and(CONTAIN, CHANGES_LOCALE_EN_US.MANAGED_OBJECT)
      .and(CONTAIN, CHANGES_LOCALE_EN_US.MO_CLASS)
      .and(CONTAIN, CHANGES_LOCALE_EN_US.ATTRIBUTE_NAME)
      .and(CONTAIN, CHANGES_LOCALE_EN_US.INITIAL_VALUE)
      .and(CONTAIN, CHANGES_LOCALE_EN_US.PREFERRED_VALUE)
      .and(CONTAIN, CHANGES_LOCALE_EN_US.STATUS);

    // prettier-ignore
    cy.getExecutionsReportsChangesTable()
            .find('tbody')
            .children()
            .first() // row 1
            .children()
            .first().find('eui-checkbox').parent()                                // checkbox
            .next().should(CONTAIN, DATA.ID_TWO)                                  // id
            .next().should(CONTAIN, DATA.MANAGED_OBJECT_TWO)                      // managedObjectFdn
            .next().should(CONTAIN, DATA.NR_CELL_DU)                              // managedObjectType
            .next().should(CONTAIN, DATA.SUB_CARRIER_SPACING)                     // attributeName
            .next().should(CONTAIN, 120)                                          // currentValue(initial value)
            .next().should(CONTAIN, 110)                                          // preferredValue
            .next().should(CONTAIN, ChangeStatus.IMPLEMENTATION_IN_PROGRESS)      // status
            .parent()
            .next() // row 2
            .children()
            .first().find('eui-checkbox').parent()
            .next().should(CONTAIN, DATA.ID_THREE)
            .next().should(CONTAIN, DATA.MANAGED_OBJECT_THREE)
            .next().should(CONTAIN, DATA.NR_CELL_CU)
            .next().should(CONTAIN, DATA.MCPC_PS_CELL_ENABLED)
            .next().should(CONTAIN, false)
            .next().should(CONTAIN, true)
            .next().should(CONTAIN, ChangeStatus.IMPLEMENTATION_FAILED)
            .parent()

            .next() // row 3
            .children()
            .first().find('eui-checkbox').parent()
            .next().should(CONTAIN, DATA.ID_FOUR)
            .next().should(CONTAIN, DATA.MANAGED_OBJECT_FOUR)
            .next().should(CONTAIN, DATA.GNBCUCP_FUNCTION)
            .next().should(CONTAIN, DATA.MAX_NG_RETRY_TIME)
            .next().should(CONTAIN, 30)
            .next().should(CONTAIN, 20)
            .next().should(CONTAIN, ChangeStatus.IMPLEMENTATION_COMPLETE);

    cy.getExecutionsReportsPaginationForChanges()
      .should(HAVE_ATTR, 'current-page', '1')
      .should(HAVE_ATTR, 'num-entries', NUM_ENTRIES)
      .should(HAVE_ATTR, 'num-pages', '2')
      .shadow()
      .find('ul.pagination')
      .children('li[data-value="2"]')
      .click();

    // prettier-ignore
    cy.getExecutionsReportsChangesTable()
            .find('tbody')
            .children()
            .first()
            .children()
            .first().find('eui-checkbox').parent()
            .next().should(CONTAIN, DATA.ID_FIVE)
            .next().should(CONTAIN, DATA.MANAGED_OBJECT_FOUR)
            .next().should(CONTAIN, DATA.GNBCUCP_FUNCTION)
            .next().should(CONTAIN, DATA.ENDPOINT_RES_DEP_H_ENABLED)
            .next().should(CONTAIN, true)
            .next().should(CONTAIN, false)
            .next().should(CONTAIN, ChangeStatus.IMPLEMENTATION_COMPLETE);

    cy.getExecutionsReportsPaginationForChanges()
      .should(HAVE_ATTR, 'current-page', '2')
      .should(HAVE_ATTR, 'num-entries', NUM_ENTRIES)
      .should(HAVE_ATTR, 'num-pages', '2')
      .shadow()
      .find('ul.pagination')
      .children('li[data-value="1"]')
      .click();
  });

  it(`Revert multiple changes on the "${TABS_LOCALE_EN_US.CHANGES}" table`, () => {
    cy.intercept('POST', `/v1/executions/${DATA.ID_ONE}/audit-results`).as(
      'revertAudit',
    );
    cy.getExecutionsReportsTab(1).click();

    // Selecting all the elements in the table, in progress won't get selected.
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

    const expectedItems = 4;
    const selected = 1;
    cy.getExecutionsReportsSelectedTab()
      .find('.changes-selection')
      .invoke('text')
      .should(
        MATCH,
        new RegExp(
          `${expectedItems}\\n\\s+${
            TABS_LOCALE_EN_US.ITEMS
          }\\s+\\|\\s+${selected}\\n\\s+${TABS_LOCALE_EN_US.SELECTED.toLowerCase()}`,
        ),
      );

    cy.getExecutionsReportsSelectedTab()
      .find('.tab-header-changes')
      .find('#action-buttons')
      .find('#revertSelected')
      .click();

    cy.getExecutionsReportsSelectedTab()
      .find('e-changes-poll')
      .shadow()
      .find('#revertDialog')
      .find('eui-button')
      .click({ force: true });

    cy.wait('@revertAudit').then(interception => {
      expect(interception.request.body).to.deep.equal({
        auditResultIds: ['4'],
        approveForAll: false,
        operation: Operation.REVERT_CHANGE,
      });
      expect(interception.response.statusCode).to.deep.equal(202);
    });

    cy.get('#notifications-column')
      .find('eui-notification')
      .should(CONTAIN, `${CHANGES_LOCALE_EN_US.REVERT_STARTED}`);
  });

  it(`Revert all changes on the "${TABS_LOCALE_EN_US.CHANGES}" table`, () => {
    cy.getExecutionsReportsTab(1).click();

    // Revert all covered by unit & Product Staging tests.
    cy.getExecutionsReportsSelectedTab()
      .find('.tab-header-changes')
      .find('#action-buttons')
      .find('#revertAll')
      .should(HAVE_ATTR, 'disabled');
  });

  it(`The "${TABS_LOCALE_EN_US.AUDIT_REPORT}" page will have the default ${AUDIT_TAB_LOCALE_EN_US.PROPOSED_CHANGES} Pill state, no table row selection and a hidden ${AUDIT_TAB_LOCALE_EN_US.APPLY_SELECTED} button while navigating pages`, () => {
    const expectedRows = 5;
    cy.getExecutionsReportsTab(0).click();

    cy.getAuditReportsTable()
      .find('tbody')
      .children()
      .should('have.length', 4)
      .first()
      .find('eui-checkbox')
      .shadow()
      .find('input')
      .click({ force: true });

    cy.getAuditReportsTabTile()
      .find('e-apply-button#applySelected')
      .should(NOT_HAVE_ATTR, 'hidden');

    cy.getAuditReportsTabTile().should(
      HAVE_ATTR,
      'subtitle',
      `${TABS_LOCALE_EN_US.SELECTED} (1)`,
    );

    cy.getAuditReportsTabTile()
      .find('eui-pill#filterPill')
      .click()
      .should(HAVE_ATTR, 'unselected');

    cy.getAuditReportsTabTile()
      .find('.row-count-text')
      .invoke('text')
      .should(CONTAIN, `${expectedRows} ${AUDIT_TAB_LOCALE_EN_US.ITEMS}`);

    // Make sure pagination is as expected and click on second page
    cy.getExecutionsReportsPagination()
      .should(HAVE_ATTR, 'current-page', '1')
      .should(HAVE_ATTR, 'num-entries', NUM_ENTRIES)
      .should(HAVE_ATTR, 'num-pages', '2')
      .shadow()
      .find('ul.pagination')
      .children('li[data-value="2"]')
      .click();

    cy.getAuditReportsTable()
      .find('thead')
      .find('tr')
      .not('.filters')
      .find('th')
      .children()
      .and(CONTAIN, AUDIT_TAB_LOCALE_EN_US.AUDIT_ID)
      .and(CONTAIN, AUDIT_TAB_LOCALE_EN_US.MANAGED_OBJECT)
      .and(CONTAIN, AUDIT_TAB_LOCALE_EN_US.MO_CLASS)
      .and(CONTAIN, AUDIT_TAB_LOCALE_EN_US.ATTRIBUTE_NAME)
      .and(CONTAIN, AUDIT_TAB_LOCALE_EN_US.CURRENT_VALUE)
      .and(CONTAIN, AUDIT_TAB_LOCALE_EN_US.PREFERRED_VALUE)
      .and(CONTAIN, AUDIT_TAB_LOCALE_EN_US.AUDIT_STATUS)
      .and(CONTAIN, AUDIT_TAB_LOCALE_EN_US.EXECUTION_ID);

    // prettier-ignore
    // On the second page should be one row
    cy.getAuditReportsTable()
            .find('tbody')
            .children()
            .should('have.length', 1)
            .first() // row 1
            .children()
            .next().should(CONTAIN, DATA.ID_FIVE)
            .next().should(CONTAIN, DATA.MANAGED_OBJECT_FOUR)
            .next().should(CONTAIN, DATA.GNBCUCP_FUNCTION)
            .next().should(CONTAIN, DATA.ENDPOINT_RES_DEP_H_ENABLED)
            .next().should(CONTAIN, true)
            .next().should(CONTAIN, false)
            .next().should(CONTAIN, AuditStatus.INCONSISTENT)
            .next().should(CONTAIN, DATA.ID_ONE)
            .parent();

    // Make sure pagination is as expected and click on first page
    cy.getExecutionsReportsPagination()
      .should(HAVE_ATTR, 'current-page', '2')
      .should(HAVE_ATTR, 'num-entries', NUM_ENTRIES)
      .should(HAVE_ATTR, 'num-pages', '2')
      .shadow()
      .find('ul.pagination')
      .children('li[data-value="1"]')
      .click();

    // Navigate to a new page
    cy.get('eui-app-bar', { includeShadowDom: true })
      .shadow()
      .find('eui-breadcrumb')
      .shadow()
      .find('#breadcrumb')
      .children('.breadcrumb-wrap')
      .eq(1)
      .click();

    // Click back into the report page
    cy.getExecutionsTable().find('tbody').children().eq(DATA.ID_THREE).click();

    // Check everything is reset to default
    cy.getExecutionsApp()
      .find('.app-header')
      .find('eui-button#viewReports')
      .click();

    cy.getAuditReportsTabTile()
      .find('e-apply-button#applySelected')
      .should(HAVE_ATTR, 'hidden');

    cy.getAuditReportsTabTile()
      .find('eui-pill#filterPill')
      .should(NOT_HAVE_ATTR, 'unselected');

    cy.getAuditReportsTabTile().should(HAVE_ATTR, 'subtitle', '');

    cy.getAuditReportsTabTile()
      .find('.row-count-text')
      .invoke('text')
      .should(CONTAIN, `4 ${AUDIT_TAB_LOCALE_EN_US.ITEMS}`);
  });

  /*
   * Test to make sure the current page gets set to 1 when the filter changes.
   *
   * For example when the proposed changes pill is unselected you could have more pages than when it is selected.
   * If when unselected you go to a page number that doesn't exist when proposed changes is selected  and then select
   * the proposed changes pill again to only show inconsistencies the current page needs to be reset to 1
   * or else it won't find any data as it would be looking for a page that doesn't exist when using that particular filter.
   */
  it(`The "${TABS_LOCALE_EN_US.AUDIT_REPORT}" current page gets set to 1 when ${AUDIT_TAB_LOCALE_EN_US.PROPOSED_CHANGES} Pill state changes`, () => {
    cy.getExecutionsReportsTab(0).click();

    cy.getAuditReportsTable()
      .find('tbody')
      .children()
      .should('have.length', 4)
      .first()
      .find('eui-checkbox')
      .shadow()
      .find('input')
      .click({ force: true });

    // Click on filter pill to show all results. Will have two pages
    cy.getAuditReportsTabTile()
      .find('eui-pill#filterPill')
      .click()
      .should(HAVE_ATTR, 'unselected');

    // Click the second Page
    cy.getExecutionsReportsPagination()
      .should(HAVE_ATTR, 'current-page', '1')
      .should(HAVE_ATTR, 'num-entries', NUM_ENTRIES)
      .should(HAVE_ATTR, 'num-pages', '2')
      .shadow()
      .find('ul.pagination')
      .children('li[data-value="2"]')
      .click();

    // Check current page is now 2
    cy.getExecutionsReportsPagination().should(HAVE_ATTR, 'current-page', '2');

    // Click the pill to change the filter to show just inconsistencies which only has one page
    cy.getAuditReportsTabTile().find('eui-pill#filterPill').click();

    // Make sure current page has been set to 1
    cy.getExecutionsReportsPagination().should(HAVE_ATTR, 'current-page', '1');
  });

  it('The table contains only filtered results when the user searches by FDN and Inconsistent audits', () => {
    cy.getAuditReportsTabTile().find('eui-panel-button').click();

    cy.getAuditReportsTabTile()
      .find('eui-tile-panel')
      .should(HAVE_ATTR, 'show');

    cy.getAuditReportsTabTile()
      .find('eui-tile-panel')
      .find('.filter-label')
      .should(HAVE_TEXT, AUDIT_TAB_LOCALE_EN_US.MANAGED_OBJECT);

    cy.getAuditReportsTabTile()
      .find('eui-tile-panel')
      .find('#managed-object-text-field')
      .shadow()
      .find('input')
      .type(DATA.PARTIAL_MATCH_FDN);

    cy.getAuditReportsTabTile()
      .find('eui-tile-panel')
      .find('#apply-filters-button')
      .click();

    // prettier-ignore
    cy.getAuditReportsTable()
            .find('tbody')
            .children()
            .should('have.length', 2)
            .first() // row 1
            .children()
            .next().should(CONTAIN, DATA.ID_TWO)
            .next().should(CONTAIN, DATA.MANAGED_OBJECT_ONE)
            .next().should(CONTAIN, DATA.NR_CELL_DU)
            .next().should(CONTAIN, DATA.SUB_CARRIER_SPACING)
            .next().should(CONTAIN, 120)
            .next().should(CONTAIN, 110)
            .next().should(CONTAIN, AuditStatus.INCONSISTENT)
            .next().should(CONTAIN, DATA.ID_ONE)
            .parent()
            .next() // row 2
            .children()
            .first().find('eui-checkbox').parent()
            .next().should(CONTAIN, DATA.ID_THREE)
            .next().should(CONTAIN, DATA.MANAGED_OBJECT_THREE)
            .next().should(CONTAIN, DATA.NR_CELL_CU)
            .next().should(CONTAIN, DATA.MCPC_PS_CELL_ENABLED)
            .next().should(CONTAIN, false)
            .next().should(CONTAIN, true)
            .next().should(CONTAIN, AuditStatus.INCONSISTENT)
            .next().should(CONTAIN, DATA.ID_ONE)
  });

  // This test will be re-introduced when UI SDK provide this capibility. see IDUN-107668 Align with EDS table: support disabled row
  /* it(`The "${TABS_LOCALE_EN_US.AUDIT}" page will have the disabled "${LOCALE_EN_US.CONSISTENT}" rows`, () => {
      cy.getExecutionsReportsTab(0).click();

      cy.getExecutionsReportsTabsCurrentHeader()
        .parent()
        .parent()
        .find('eui-pill#filterPill')
        .click();

      cy.getExecutionsReportsTabsCurrentHeader()
        .parent()
        .parent()
        .find('eui-pill#filterPill')
        .should(HAVE_ATTR, 'unselected');

      cy.getExecutionsReportsCurrentTable(0)
        .find('tbody')
        .children()
        .first()
        .find('eui-checkbox')
        .shadow()
        .find('input')
        .click({ force: true });

      cy.getExecutionsReportsCurrentTable(0)
        .find('tbody')
        .children()
        .first()
        .find('eui-checkbox')
        .should(HAVE_ATTR, 'disabled');

      cy.getExecutionsReportsTabsCurrentHeader()
        .parent()
        .parent()
        .find('eui-button#applySelected')
        .should(HAVE_ATTR, 'hidden');
    }); */
});
