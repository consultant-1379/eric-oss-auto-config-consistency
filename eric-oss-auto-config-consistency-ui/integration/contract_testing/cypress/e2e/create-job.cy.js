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

import * as LOCALE_EN_US from '../../../../src/apps/create-job/locale/en-us.json';
import {
  ALL_IRELAND,
  ID_ONE,
  JOB_NAME,
  JOB_SUCCESS_MSG,
  JOB_UPDATE_MSG,
  RULESET_VALUE,
  SCHEDULE_FREQUENCY,
  SCHEDULE_HOUR,
  SCHEDULE_MINUTE,
  SCOPE_VALUE,
} from '../utils/testData.js';
import { CONTAIN, HAVE_LENGTH, HAVE_TEXT } from '../utils/testUtils.js';

describe('Visit the create-job app', () => {
  beforeEach(() => {
    cy.visit('/#eacc/jobs/create-job');
  });
  it(`All Input components are loaded, user clicks the Save button and successful Job creation.`, () => {
    cy.getCreateJobApp()
      .find('.app-header')
      .should(CONTAIN, `${LOCALE_EN_US.CREATE_JOB}`);

    cy.getCreateJobApp()
      .find('[id=jobName]')
      .shadow()
      .find('input')
      .type(JOB_NAME);

    cy.getCreateJobApp()
      .find('[id=scheduleFrequency]')
      .shadow()
      .find('eui-menu')
      .find('eui-menu-item')
      .then(items => {
        for (let i = 0; i < items.length; i += 1) {
          if (items[i].value === SCHEDULE_FREQUENCY) {
            items[i].selected = true;
          }
        }
      });

    cy.getCreateJobApp()
      .find('[id=scheduleTime]')
      .shadow()
      .find('.time-picker > eui-combo-box')
      .eq(0)
      .shadow()
      .find('eui-menu > eui-menu-item')
      .then(items => {
        for (let i = 0; i < items.length; i += 1) {
          if (items[i].value === SCHEDULE_HOUR) {
            items[i].selected = true;
          }
        }
      });

    cy.getCreateJobApp()
      .find('[id=scheduleTime]')
      .shadow()
      .find('.time-picker > eui-combo-box')
      .eq(1)
      .shadow()
      .find('eui-menu > eui-menu-item')
      .then(items => {
        for (let i = 0; i < items.length; i += 1) {
          if (items[i].value === SCHEDULE_MINUTE) {
            items[i].selected = true;
          }
        }
      });

    cy.getCreateJobApp()
      .find('[id=rulesetName]')
      .shadow()
      .find('eui-menu')
      .find('eui-menu-item')
      .should(HAVE_LENGTH, 1)
      .then(items => {
        for (let i = 0; i < items.length; i += 1) {
          if (items[i].value === RULESET_VALUE) {
            items[i].selected = true;
          }
        }
      });

    cy.getCreateJobApp()
      .find('[id=scopeName]')
      .shadow()
      .find('eui-menu')
      .find('eui-menu-item')
      .should(HAVE_LENGTH, 2)
      .then(items => {
        for (let i = 0; i < items.length; i += 1) {
          if (items[i].value === SCOPE_VALUE) {
            items[i].selected = true;
          }
        }
      });

    cy.getCreateJobApp().find('[id=saveButton]').click();

    cy.get('eui-notification').should(HAVE_TEXT, JOB_SUCCESS_MSG);
  });
  it('Update a job successfully', () => {
    const jobName = `${ALL_IRELAND}-${ID_ONE}`;
    cy.visit(`/#eacc/jobs/create-job?updateJob=${jobName}`);

    cy.intercept('PUT', `/v1/jobs/${jobName}`).as('updateJob');

    cy.getCreateJobApp()
      .find('[id=rulesetName]')
      .shadow()
      .find('eui-menu')
      .find('eui-menu-item')
      .should(HAVE_LENGTH, 1)
      .then(items => {
        for (let i = 0; i < items.length; i += 1) {
          if (items[i].value === RULESET_VALUE) {
            items[i].selected = true;
          }
        }
      });
    cy.getCreateJobApp()
      .find('[id=scopeName]')
      .shadow()
      .find('eui-menu')
      .find('eui-menu-item')
      .then(items => {
        for (let i = 0; i < items.length; i += 1) {
          if (items[i].value === SCOPE_VALUE) {
            items[i].selected = true;
          }
        }
      });

    cy.getCreateJobApp()
      .find('[id=scheduleTime]')
      .shadow()
      .find('.time-picker > eui-combo-box')
      .eq(1)
      .shadow()
      .find('eui-menu > eui-menu-item')
      .then(items => {
        for (let i = 0; i < items.length; i += 1) {
          if (items[i].value === SCHEDULE_MINUTE) {
            items[i].selected = true;
          }
        }
      });

    cy.getCreateJobApp().find('[id=saveButton]').click();

    cy.get('eui-notification').should(HAVE_TEXT, JOB_UPDATE_MSG);

    cy.wait('@updateJob').its('response.statusCode').should('eq', 200);
  });
});
