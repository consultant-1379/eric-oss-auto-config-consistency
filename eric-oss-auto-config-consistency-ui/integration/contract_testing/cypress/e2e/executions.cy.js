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

import { ExecutionState } from '../../../../src/utils/attributes/executionsAttributes.js';

import * as LOCALE_EN_US from '../../../../src/apps/executions/locale/en-us.json';
import * as DATA from '../utils/testData.js';

import {
  CONTAIN,
  NOT_HAVE_ATTR,
  HAVE_ATTR,
  HAVE_LENGTH,
} from '../utils/testUtils.js';

describe('Visit the executions app', () => {
  beforeEach(() => {
    cy.interceptModules();
    cy.visit('/#eacc/executions');
    cy.waitForModules();
  });
  it(`The "${LOCALE_EN_US.JOB_EXECUTIONS}" table is populated with executions and the "${LOCALE_EN_US.VIEW_REPORTS}" button is visible`, () => {
    const expectedExecutions = 4;

    cy.getExecutionsApp()
      .find('.app-header .executions-display')
      .should(
        CONTAIN,
        `${LOCALE_EN_US.JOB_EXECUTIONS} (${expectedExecutions})`,
      );

    cy.getExecutionsApp()
      .find('.app-header')
      .find('eui-button#viewReports')
      .should(CONTAIN, LOCALE_EN_US.VIEW_REPORTS)
      .and(HAVE_ATTR, 'disabled');

    const expectedColumns = 26;
    cy.log(`Checking Executions table for ${expectedColumns} columns`);
    cy.getExecutionsTable()
      .find('thead')
      .find('tr')
      .not('.filters')
      .find('th')
      .children()
      .should(HAVE_LENGTH, expectedColumns)
      .and(CONTAIN, LOCALE_EN_US.EXECUTION_ID)
      .and(CONTAIN, LOCALE_EN_US.JOB_NAME)
      .and(CONTAIN, LOCALE_EN_US.EXECUTION_TYPE)
      .and(CONTAIN, LOCALE_EN_US.EXECUTION_START)
      .and(CONTAIN, LOCALE_EN_US.EXECUTION_END)
      .and(CONTAIN, LOCALE_EN_US.STATUS)
      .and(CONTAIN, LOCALE_EN_US.ATTRIBUTES_AUDITED)
      .and(CONTAIN, LOCALE_EN_US.MOS_AUDITED)
      .and(CONTAIN, LOCALE_EN_US.INCONSISTENCIES);

    cy.log(`Checking Executions table for ${expectedExecutions} rows`);
    cy.getExecutionsTable()
      .find('tbody')
      .children()
      .should(HAVE_LENGTH, expectedExecutions);

    // prettier-ignore
    cy.getExecutionsTable()
      .find('tbody').children()
      .first() // row 1
        .children()
          .first().should(CONTAIN, DATA.ID_FOUR)                         // id
          .next().should(CONTAIN, `${DATA.ALL_IRELAND}-${DATA.ID_FOUR}`) // jobName
          .next().should(CONTAIN, DATA.OPEN_LOOP)                        // executionType
          .next().should(CONTAIN, DATA.ID_FOUR_EXECUTION_START)          // executionStartedAt
          .next().should(CONTAIN, DATA.ID_FOUR_EXECUTION_END)            // executionEndedAt
          .next().should(CONTAIN, ExecutionState.AUDIT_IN_PROGRESS)            // executionStatus
          .next().should(CONTAIN, 1)                                     // totalAttributesAudited
          .next().should(CONTAIN, 1)                                     // totalMosAudited
          .next().should(CONTAIN, 4)                                     // inconsistenciesIdentified
        .parent()
      .next() // row 2
        .children()
          .first().should(CONTAIN, DATA.ID_THREE)
          .next().should(CONTAIN, `${DATA.ALL_IRELAND}-${DATA.ID_THREE}`)
          .next().should(CONTAIN, DATA.OPEN_LOOP)
          .next().should(CONTAIN, DATA.ID_THREE_EXECUTION_START)
          .next().should(CONTAIN, DATA.ID_THREE_EXECUTION_END)
          .next().should(CONTAIN, ExecutionState.AUDIT_FAILED)
          .next().should(CONTAIN, 20)
          .next().should(CONTAIN, 40)
          .next().should(CONTAIN, 66)
        .parent()
      .next() // row 3
        .children()
          .first().should(CONTAIN, DATA.ID_TWO)
          .next().should(CONTAIN, `${DATA.ALL_IRELAND}-${DATA.ID_TWO}`)
          .next().should(CONTAIN, DATA.OPEN_LOOP)
          .next().should(CONTAIN, DATA.ID_TWO_EXECUTION_START)
          .next().should(CONTAIN, DATA.ID_TWO_EXECUTION_END)
          .next().should(CONTAIN, ExecutionState.AUDIT_SUCCESSFUL)
          .next().should(CONTAIN, 1)
          .next().should(CONTAIN, 10)
          .next().should(CONTAIN, 8)
        .parent()
      .next() // row 4
        .children()
          .first().should(CONTAIN, DATA.ID_ONE)
          .next().should(CONTAIN, `${DATA.ALL_IRELAND}-${DATA.ID_ONE}`)
          .next().should(CONTAIN, DATA.OPEN_LOOP)
          .next().should(CONTAIN, DATA.ID_ONE_EXECUTION_START)
          .next().should(CONTAIN, DATA.ID_ONE_EXECUTION_END)
          .next().should(CONTAIN, ExecutionState.AUDIT_SUCCESSFUL)
          .next().should(CONTAIN, 12)
          .next().should(CONTAIN, 1)
          .next().should(CONTAIN, 4);
  });
  it(`When "${LOCALE_EN_US.EXECUTION_ID}" ${DATA.ID_TWO} is selected and the "${LOCALE_EN_US.VIEW_REPORTS}" button is clicked it navigates to the reports app`, () => {
    // Note: Table is on default sorting of 'Execution Start'
    // click on the third row

    cy.getExecutionsApp()
      .find('.app-header')
      .find('eui-button#viewReports')
      .should(HAVE_ATTR, 'disabled');

    cy.getExecutionsTable().find('tbody').children().eq(DATA.ID_TWO).click();

    cy.getExecutionsApp()
      .find('.app-header')
      .find('eui-button#viewReports')
      .should(NOT_HAVE_ATTR, 'disabled');

    cy.getExecutionsApp()
      .find('.app-header')
      .find('eui-button#viewReports')
      .click();

    cy.url().should(
      CONTAIN,
      `/#eacc/executions/execution-reports?executionId=${DATA.ID_TWO}&jobName=${DATA.ALL_IRELAND}-${DATA.ID_TWO}`,
    );
  });
  it(`The "${LOCALE_EN_US.JOB_EXECUTIONS}" table is populated with 1 execution and the "${LOCALE_EN_US.CLEAR}" link is visible.`, () => {
    const jobName = `${DATA.ALL_IRELAND}-${DATA.ID_ONE}`;
    const expectedFilteredExecutions = 1;
    const expectedUnfilteredExecutions = 4;

    cy.visit(`/#eacc/executions?jobName=${jobName}`);

    cy.log(`Checking Executions table for ${expectedFilteredExecutions} row`);
    cy.getExecutionsTable()
      .find('tbody')
      .children()
      .should(HAVE_LENGTH, expectedFilteredExecutions);

    cy.log(`Checking filter text exists and clicking the clear filter option.`);
    cy.getExecutionsApp()
      .find('.app-header')
      .should(
        CONTAIN,
        `${LOCALE_EN_US.JOB_EXECUTIONS} (${expectedFilteredExecutions})`,
      )
      .find('.app-filter-text')
      .should(CONTAIN, `${jobName}`)
      .find('eui-link')
      .should(CONTAIN, `${LOCALE_EN_US.CLEAR}`)
      .click();

    cy.log(
      `After clearing filter checking Executions table for ${expectedUnfilteredExecutions} rows`,
    );
    cy.getExecutionsTable()
      .find('tbody')
      .children()
      .should(HAVE_LENGTH, expectedUnfilteredExecutions);
  });
});
