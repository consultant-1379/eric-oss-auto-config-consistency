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

import * as LOCALE_EN_US from '../../../../src/apps/jobs/locale/en-us.json';
import * as DATA from '../utils/testData.js';
import { CONTAIN, HAVE_LENGTH, HAVE_TEXT } from '../utils/testUtils.js';

describe('Visit the jobs app', () => {
  beforeEach(() => {
    cy.interceptModules();
    cy.visit('/#eacc/jobs');
    cy.waitForModules();
  });
  it(`The "Jobs" table is populated with jobs and the job buttons are visible`, () => {
    const expectedJobs = 2;

    cy.getJobsApp()
      .find('.app-header .executions-display')
      .should(CONTAIN, `${LOCALE_EN_US.JOBS} (${expectedJobs})`);

    cy.getJobsApp()
      .find('.app-header')
      .find('.action-buttons')
      .children()
      .first()
      .should(HAVE_TEXT, LOCALE_EN_US.VIEW_EXECUTIONS)
      .next()
      .should(HAVE_TEXT, LOCALE_EN_US.UPDATE_JOB)
      .next()
      .should(HAVE_TEXT, LOCALE_EN_US.DELETE);

    const expectedColumns = 4;
    cy.log(`Checking Jobs table for ${expectedColumns} columns`);
    cy.getJobsTable()
      .find('thead')
      .find('tr')
      .not('.filters')
      .find('th')
      .children()
      .should(HAVE_LENGTH, expectedColumns)
      .and(CONTAIN, LOCALE_EN_US.JOB_NAME)
      .and(CONTAIN, LOCALE_EN_US.SCHEDULE)
      .and(CONTAIN, LOCALE_EN_US.RULESET_NAME)
      .and(CONTAIN, LOCALE_EN_US.SCOPE_NAME);

    cy.log(`Checking Jobs table for ${expectedJobs} rows`);
    cy.getJobsTable()
      .find('tbody')
      .children()
      .should(HAVE_LENGTH, expectedJobs);

    // prettier-ignore
    cy.getJobsTable()
      .find('tbody').children()
      .first()
      .children()
      .first().should(CONTAIN, `${DATA.ALL_IRELAND}-${DATA.ID_ONE}`)      // jobName
      .next().should(CONTAIN, DATA.DAILY_AT_QUARTER_PAST_ELEVEN)          // schedule
      .next().should(CONTAIN, DATA.TEST_RULESET)                          // rulesetName
      .next().should(CONTAIN, DATA.TEST_SCOPE); // scopeName
  });
  it(`Clicking on a job then ${LOCALE_EN_US.VIEW_EXECUTIONS} brings you the executions page`, () => {
    cy.getJobsTable()
      .find('tbody')
      .children()
      .first()
      .children()
      .first()
      .click();

    cy.getJobsApp()
      .find('.app-header')
      .find('.action-buttons')
      .find('#view-executions')
      .click();

    cy.url().should(
      CONTAIN,
      `/#eacc/executions?jobName=${DATA.ALL_IRELAND}-${DATA.ID_ONE}`,
    );
  });
  it(`Clicking on a job then ${LOCALE_EN_US.DELETE} then ${LOCALE_EN_US.DELETE_JOB} returns a 204 status code`, () => {
    cy.intercept('DELETE', `/v1/jobs/${DATA.ALL_IRELAND}-${DATA.ID_TWO}`).as(
      'deleteJob',
    );

    cy.getJobsTable()
      .find('tbody')
      .children()
      .first()
      .next()
      .children()
      .first()
      .click();

    cy.getJobsApp()
      .find('.app-header')
      .find('.action-buttons')
      .find('#delete-job')
      .click();

    cy.getJobsApp().find('eui-dialog').find('eui-button').click();

    cy.wait('@deleteJob').its('response.statusCode').should('eq', 204);

    cy.get('#notifications-column')
      .find('eui-notification')
      .should(
        CONTAIN,
        `${DATA.ALL_IRELAND}-${DATA.ID_TWO} ${LOCALE_EN_US.DELETED_SUCCESS_MESSAGE}`,
      );
  });
  it(`Clicking on a job then ${LOCALE_EN_US.UPDATE_JOB} brings you to the update job page`, () => {
    cy.getJobsTable().find('tbody').children().first().click();

    cy.getJobsApp()
      .find('.app-header')
      .find('.action-buttons')
      .find('#update-job')
      .click();

    cy.url().should(
      CONTAIN,
      `/#eacc/jobs/update-job?updateJob=${DATA.ALL_IRELAND}-${DATA.ID_ONE}`,
    );
  });
});
