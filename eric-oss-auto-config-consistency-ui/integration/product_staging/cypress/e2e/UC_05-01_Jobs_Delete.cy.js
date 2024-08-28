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
  EACC_JOBS_URL,
  JOB_NAME_4G,
  JOB_NAME_5G,
  TEST_JOBS,
} from '../utils/testData.js';
import {
  UI_RENDERING_PERIOD,
  NOTIFICATION_WAIT_PERIOD,
} from '../utils/testUtils.js';

const USE_CASE_ID = 'EACC-UC_05';

/**
 * UC_05-01: It shall be possible to delete existing EACC jobs
 */
describe(`${USE_CASE_ID}: Delete existing EACC jobs`, () => {
  before(() => {
    cy.visit(EACC_JOBS_URL);
    cy.wait(UI_RENDERING_PERIOD);
  });

  it(`${USE_CASE_ID}-01: Delete; The job '${JOB_NAME_4G}' and '${JOB_NAME_5G}' can be deleted`, () => {
    TEST_JOBS.forEach(job => {
      cy.getJobsTable().getRowByKey(job.name).click();

      cy.getJobsApp()
        .find('.app-header')
        .find('.action-buttons')
        .find('#delete-job')
        .click();

      cy.getJobsApp().find('eui-dialog').find('eui-button').click();

      cy.wait(NOTIFICATION_WAIT_PERIOD);
    });
  });
});
