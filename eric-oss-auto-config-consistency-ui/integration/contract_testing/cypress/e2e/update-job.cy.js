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

/* eslint-disable */
import * as LOCALE_EN_US from '../../../../src/apps/create-job/locale/en-us.json';
import {
  SCHEDULE_FREQUENCY,
  SCHEDULE_HOUR,
  SCHEDULE_MINUTE,
} from '../utils/testData.js';

import { HAVE_TEXT, CONTAIN } from '../utils/testUtils';

describe('Visit the create-job app', () => {
  beforeEach(() => {
    cy.visit('/#eacc/jobs/update-job?updateJob=all-ireland-1');
  });
  it(`Pre-populates the data fields with the job information retrieved from REST request, where possible.`, () => {
    cy.getCreateJobApp()
      .find('.app-header')
      .should(CONTAIN, `${LOCALE_EN_US.UPDATE_JOB}`);

    cy.getCreateJobApp()
      .find('[id=jobName]')
      .should(HAVE_TEXT, 'all-ireland-1');

    cy.getCreateJobApp()
      .find('[id=scheduleFrequency]')
      .shadow()
      .find('eui-menu')
      .find('eui-menu-item')
      .then(items => {
        for (let i = 0; i < items.length; i += 1) {
          if (items[i].value === SCHEDULE_FREQUENCY) {
            expect(items[i].selected).to.equal(true);
          } else {
            expect(items[i].selected).to.not.equal(true);
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
            expect(items[i].selected === true);
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
            expect(items[i].selected === true);
          }
        }
      });

    cy.getCreateJobApp().find('[id=saveButton]').click();
  });
});
