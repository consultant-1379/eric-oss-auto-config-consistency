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

import {
  EACC_JOBS_URL,
  JOB_NAME_4G,
  JOB_NAME_5G,
  TEST_JOBS,
} from '../utils/testData.js';
import {
  UI_RENDERING_PERIOD,
  PAGE_CHANGE_PERIOD,
  CONTAIN,
} from '../utils/testUtils.js';

/**
 * UC_02-01: It shall be possible to create and read a job in EACC.
 */
const USE_CASE_ID = 'EACC-UC_02';

describe(`${USE_CASE_ID}: Job Create, Read, and Update`, () => {
  before(() => {
    cy.visit(EACC_JOBS_URL);
    cy.wait(UI_RENDERING_PERIOD);
  });

  it(`${USE_CASE_ID}-01: Create; A new job: '${JOB_NAME_4G}' and '${JOB_NAME_5G}' can be created`, () => {
    TEST_JOBS.forEach(job => {
      cy.getCreateJobButton().click({ force: true });

      cy.wait(PAGE_CHANGE_PERIOD);

      cy.getCreateJobApp()
        .find('[id=jobName]')
        .shadow()
        .find('input')
        .type(job.name, { force: true });

      cy.selectDropdownValue('scheduleFrequency', 'Daily');

      cy.getCreateJobApp()
        .find('[id=scheduleTime')
        .shadow()
        .find('eui-combo-box[id=hours]')
        .shadow()
        .find(`eui-menu-item[label=${job.scheduleHour}]`)
        .click({ force: true });

      cy.getCreateJobApp()
        .find('[id=scheduleTime')
        .shadow()
        .find('eui-combo-box[id=minutes]')
        .shadow()
        .find(`eui-menu-item[label=${job.scheduleMinute}]`)
        .click({ force: true });

      cy.selectDropdownValue('rulesetName', job.ruleset);

      cy.selectDropdownValue('scopeName', job.scope);

      cy.getCreateJobApp().find('#saveButton').click();

      cy.wait(PAGE_CHANGE_PERIOD);
    });
  });
  it(`${USE_CASE_ID}-02: Read; The job table should contain an entry for '${JOB_NAME_4G}' and '${JOB_NAME_5G}'`, () => {
    TEST_JOBS.forEach(job => {
      // prettier-ignore
      cy.getJobsTable()
        .getRowByKey(job.name)
        .children()
          .first().should(CONTAIN, job.name)    // jobName
          .next()                               // schedule
          .next().should(CONTAIN, job.ruleset)  // ruleset
          .next().should(CONTAIN, job.scope); // scope
    });
  });
  it(`${USE_CASE_ID}-3: Update; Jobs '${JOB_NAME_4G}' and '${JOB_NAME_5G}' can be updated`, () => {
    TEST_JOBS.forEach(job => {
      cy.getJobsTable().getRowByKey(job.name).click();

      cy.getJobsApp()
        .find('.app-header')
        .find('.action-buttons')
        .find('#update-job')
        .click();

      cy.wait(PAGE_CHANGE_PERIOD);

      cy.url().should(
        CONTAIN,
        `${EACC_JOBS_URL}/update-job?updateJob=${job.name}`,
      );

      cy.selectDropdownValue('scheduleFrequency', 'Today');

      cy.selectDropdownValue('rulesetName', job.ruleset);

      cy.selectDropdownValue('scopeName', job.scope);

      cy.getCreateJobApp().find('#saveButton').click();

      cy.wait(PAGE_CHANGE_PERIOD);

      // prettier-ignore
      cy.getJobsTable()
        .getRowByKey(job.name)
        .children()
          .first().should(CONTAIN, job.name)    // jobName
          .next()                               // schedule
          .next().should(CONTAIN, job.ruleset)  // ruleset
          .next().should(CONTAIN, job.scope); // scope
    });
  });
});
