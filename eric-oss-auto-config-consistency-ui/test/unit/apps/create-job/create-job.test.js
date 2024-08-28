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

/**
 * Integration tests for <e-create-job>
 */
import { expect, fixture, nextFrame } from '@open-wc/testing';
import sinon from 'sinon';

import {
  resetNotifications,
  simulateEvent,
  simulateEventWithText,
} from '../../../resources/elementUtils.js';
import CreateJob from '../../../../src/apps/create-job/create-job.js';

import { default as LOCALE_EN_US } from '../../../../src/apps/create-job/locale/en-us.json'; // eslint-disable-line import/no-named-default

import {
  MOCK_JOBS_SUCCESSFUL_RESPONSE_DATA,
  MOCK_RULESET_RESULTS_DATA_ARRAY,
  MOCK_JOBS_DATA_ARRAY,
  MOCK_SCOPES_DATA_ARRAY,
  MOCK_JOBS_SUCCESSFUL_UPDATE_RESPONSE_DATA,
} from '../../../resources/mockData.js';
import {
  MOCK_ACCESS_DENIED_RESPONSE,
  MOCK_BAD_REQUEST_RESPONSE,
  MOCK_NOT_FOUND_RESPONSE,
  MOCK_CONFLICT_RESPONSE,
} from '../../../resources/mockResponses.js';
import {
  JOB_NAME,
  SCHEDULE_FREQUENCY,
  SCHEDULE_TIME,
} from '../../../../src/utils/attributes/jobAttributes.js';
import { SCOPE_NAME } from '../../../../src/utils/attributes/scopeAttributes.js';
import { RULESET_NAME } from '../../../../src/utils/attributes/rulesetAttributes.js';
import { updateLocaleString } from '../../../../src/utils/localization/localeUtils.js';
import { adjustCronToTimezone } from '../../../../src/utils/cronUtils.js';

const JOBS_API = '/v1/jobs';
const RULESETS_API = '/v1/rulesets';
const SCOPES_API = '/v1/scopes';

const MOCK_SUCCESSFUL_CREATE_JOB_RESPONSE = {
  json: MOCK_JOBS_SUCCESSFUL_RESPONSE_DATA,
  ok: true,
  status: 201,
};

const MOCK_SUCCESSFUL_UPDATE_JOB_RESPONSE = {
  json: MOCK_JOBS_SUCCESSFUL_UPDATE_RESPONSE_DATA,
  ok: true,
  status: 200,
};

const MOCK_POPULATED_RULESETS_RESPONSE = {
  json: MOCK_RULESET_RESULTS_DATA_ARRAY,
  ok: true,
  status: 200,
};

const MOCK_POPULATED_SCOPE_RESPONSE = {
  json: MOCK_SCOPES_DATA_ARRAY,
  ok: true,
  status: 200,
};

const MOCK_POPULATED_JOB_RESPONSE = {
  json: MOCK_JOBS_DATA_ARRAY,
  ok: true,
  status: 200,
};

const MOCK_LOCALE_RESPONSE = {
  json: () => LOCALE_EN_US,
  ok: true,
  status: 200,
};

const SAVE_BUTTON = 'Save';
const CANCEL_BUTTON = 'Cancel';
const jobName = 'all-ireland-1';
const UNIT_TEST_JOB = 'unit_test_job1';

let fetchStub;
let routerSpy;

function compareTheDropDowns(expected, actual) {
  for (let i = 0; i < expected.length; i += 1) {
    expect(expected[i].label).to.equal(actual[i].label);
    expect(expected[i].value).to.equal(actual[i].value);
  }
}

function compareArrayLengths(expected, actual) {
  expect(actual.length).to.equal(expected.length);
}

function compareTheWarnings(expectedWarningText, paramName, createJobApp) {
  const col1 = createJobApp.shadowRoot
    .querySelector('.row')
    .querySelector('.col-1');
  expect(
    col1
      .querySelector(`#${paramName}Warning`)
      .querySelector('.input__validation')
      .querySelector('.input__validation-message').textContent,
  ).to.equal(expectedWarningText);
}

function compareTheJobNameWarnings(expectedWarningText, createJobApp) {
  const col1 = createJobApp.shadowRoot
    .querySelector('.row')
    .querySelector('.col-1');
  expect(
    col1.querySelector(`#jobName`).getAttribute('custom-validation'),
  ).to.equal(expectedWarningText);
}

describe('Given: An <e-create-job>', () => {
  before(() => {
    CreateJob.register('e-create-job');
    fetchStub = sinon.stub(window, 'fetch');
    // Mock the window.EUI.Router.goto call as it is not available.
    window.EUI = {
      Router: {
        goto: path => {
          console.log('goto', path);
        },
      },
    };
    routerSpy = sinon.spy(window.EUI.Router, 'goto');
  });

  after(() => {
    sinon.restore();
  });

  beforeEach(() => {
    fetchStub.callsFake(url => {
      if (url.indexOf('locale') !== -1) {
        return Promise.resolve(MOCK_LOCALE_RESPONSE);
      }
      return Promise.resolve(true);
    });
  });

  afterEach(() => {
    fetchStub.reset();
    routerSpy.resetHistory();
    resetNotifications();
  });

  describe(`When: Create Job Page is loaded`, () => {
    it('Then: All components are rendered properly.', async () => {
      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_POPULATED_RULESETS_RESPONSE);

      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_POPULATED_SCOPE_RESPONSE);

      fetchStub
        .withArgs(sinon.match(JOBS_API))
        .resolves(MOCK_POPULATED_JOB_RESPONSE);

      const createJobApp = await fixture('<e-create-job></e-create-job>');

      // All app sub-headers are rendered properly.
      const appSubHeadersExpected = [
        LOCALE_EN_US.JOB_NAME,
        LOCALE_EN_US.SCHEDULE,
        LOCALE_EN_US.RULESET,
        LOCALE_EN_US.SCOPE,
      ];
      const appSubHeadersActual = [];
      createJobApp.shadowRoot
        .querySelectorAll('.app-sub-header')
        .forEach(element => {
          appSubHeadersActual.push(element.textContent);
        });

      expect(appSubHeadersActual.length).to.equal(appSubHeadersExpected.length);

      for (let i = 0; i < appSubHeadersExpected.length; i += 1) {
        expect(appSubHeadersActual[i].trim()).to.equal(
          appSubHeadersExpected[i],
        );
      }

      const col1 = createJobApp.shadowRoot
        .querySelector('.row')
        .querySelector('.col-1');
      const col2 = createJobApp.shadowRoot
        .querySelector('.row')
        .querySelector('.col-2');

      // Col-1 -> ScheduleFrequency - checking dropdown populates schedule Frequency values.
      const expectedScheduleFrequencyData = [
        { label: 'Daily', value: 'Daily' },
        { label: 'Today', value: 'Today' },
      ];

      const actualScheduleFrequencyData = [];
      col1
        .querySelector(`#${SCHEDULE_FREQUENCY}`)
        .shadowRoot.querySelectorAll('eui-menu > eui-menu-item')
        .forEach(element => {
          actualScheduleFrequencyData.push({
            label: element.label,
            value: element.value,
          });
        });

      compareArrayLengths(
        expectedScheduleFrequencyData,
        actualScheduleFrequencyData,
      );
      compareTheDropDowns(
        expectedScheduleFrequencyData,
        actualScheduleFrequencyData,
      );

      // Col1 -> ScheduleTime - checking time populated correctly
      const expectedScheduleTime = '16:19';
      col1.querySelector(`#${SCHEDULE_TIME}`).value = expectedScheduleTime;
      await nextFrame();
      const actualScheduleTime = col1.querySelector(`#${SCHEDULE_TIME}`).value;
      expect(actualScheduleTime).to.equal(expectedScheduleTime);

      // Col1 -> Ruleset - checking dropdown popluated correctly
      const expectedRulesetsData = [
        { label: 'ruleset1', value: '1' },
        { label: 'ruleset2', value: '2' },
      ];
      const actualRulesetsData = [];
      col1
        .querySelector(`#${RULESET_NAME}`)
        .shadowRoot.querySelectorAll('eui-menu > eui-menu-item')
        .forEach(element => {
          actualRulesetsData.push({
            label: element.label,
            value: element.value,
          });
        });

      compareArrayLengths(expectedRulesetsData, actualRulesetsData);
      compareTheDropDowns(expectedRulesetsData, actualRulesetsData);

      // Col1 -> Scope - checking dropdown populated correctly
      const expectedScopesData = [
        { label: 'athlone', value: '1' },
        { label: 'dublin', value: '2' },
      ];
      const actualScopesData = [];
      col1
        .querySelector(`#${SCOPE_NAME}`)
        .shadowRoot.querySelectorAll('eui-menu > eui-menu-item')
        .forEach(element => {
          actualScopesData.push({
            label: element.label,
            value: element.value,
          });
        });

      compareArrayLengths(expectedScopesData, actualScopesData);
      compareTheDropDowns(expectedScopesData, actualScopesData);

      // Col2 -> Save Button
      const saveButton = col2.querySelector('#saveButton');
      expect(saveButton.textContent).to.equal(SAVE_BUTTON);

      // Col2 -> Cancel Button
      const cancelButton = col2.querySelector('#cancelButton');
      expect(cancelButton.textContent).to.equal(CANCEL_BUTTON);
    });
  });

  describe(`When: A call is made to ${JOBS_API}, along with Create Job Parameters and a 200 is returned`, () => {
    it(`Then: The UI displays a notification saying, "Job '${jobName}' was created"`, async () => {
      fetchStub
        .withArgs(sinon.match(JOBS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_SUCCESSFUL_CREATE_JOB_RESPONSE);

      fetchStub
        .withArgs(sinon.match(JOBS_API))
        .resolves(MOCK_POPULATED_JOB_RESPONSE);

      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_POPULATED_SCOPE_RESPONSE);

      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_POPULATED_RULESETS_RESPONSE);

      const createJobApp = await fixture('<e-create-job></e-create-job>');
      const col1 = createJobApp.shadowRoot
        .querySelector('.row')
        .querySelector('.col-1');
      const col2 = createJobApp.shadowRoot
        .querySelector('.row')
        .querySelector('.col-2');

      col1.querySelector(`#${JOB_NAME}`).value = jobName;

      col1
        .querySelector(`#${SCHEDULE_FREQUENCY}`)
        .shadowRoot.querySelectorAll('eui-menu > eui-menu-item')
        .forEach(element => {
          if (element.value === 'Today') {
            element.selected = true;
          }
        });

      col1.querySelector(`#${SCHEDULE_TIME}`).value = '16:19';
      col1
        .querySelector(`#${RULESET_NAME}`)
        .shadowRoot.querySelectorAll('eui-menu > eui-menu-item')
        .forEach(element => {
          if (element.value === '1') {
            element.selected = true;
          }
        });

      col1
        .querySelector(`#${SCOPE_NAME}`)
        .shadowRoot.querySelectorAll('eui-menu > eui-menu-item')
        .forEach(element => {
          if (element.value === '1') {
            element.selected = true;
          }
        });

      const saveButton = col2.querySelector('#saveButton');
      simulateEvent(saveButton, 'click');

      await nextFrame();
      const notification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(notification.textContent.trim()).to.equal(
        `Job '${jobName}' was created.`,
      );

      await nextFrame();
      expect(routerSpy.args[0][0]).to.equal(
        `/#eacc/jobs?createdJob=${jobName}`,
      );
    });
  });

  describe(`When: User types in a Job Name, with special characters except(lowercase alpanumeric,underscore & dash)`, () => {
    it(`Then: The UI displays a warning for job name`, async () => {
      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_POPULATED_RULESETS_RESPONSE);

      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_POPULATED_SCOPE_RESPONSE);

      fetchStub
        .withArgs(sinon.match(JOBS_API))
        .resolves(MOCK_POPULATED_JOB_RESPONSE);

      const jobNameWithSpecialCharacter = 'all@ireland@1';
      const createJobApp = await fixture('<e-create-job></e-create-job>');
      const col1 = createJobApp.shadowRoot
        .querySelector('.row')
        .querySelector('.col-1');
      const jobNameField = col1.querySelector(`#${JOB_NAME}`);

      simulateEventWithText(jobNameField, 'input', jobNameWithSpecialCharacter);

      compareTheJobNameWarnings(
        LOCALE_EN_US.JOB_NAME_PATTERN_MISMATCH_WARNING,
        createJobApp,
      );
    });
  });

  describe(`When: User types in a Job Name, which exceeds permitted length`, () => {
    it(`Then: The UI displays a warning for job name`, async () => {
      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_POPULATED_RULESETS_RESPONSE);

      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_POPULATED_SCOPE_RESPONSE);

      fetchStub
        .withArgs(sinon.match(JOBS_API))
        .resolves(MOCK_POPULATED_JOB_RESPONSE);
      const jobNameWithLessThan4Chars = 'all';
      const jobNameWithOver100Chars =
        'all-ireland-001all-ireland-001all-ireland-001all-ireland-001all-ireland-001all-ireland-001all-irelandd';
      const createJobApp = await fixture('<e-create-job></e-create-job>');
      const col1 = createJobApp.shadowRoot
        .querySelector('.row')
        .querySelector('.col-1');
      const jobNameField = col1.querySelector(`#${JOB_NAME}`);
      simulateEventWithText(jobNameField, 'input', jobNameWithLessThan4Chars);
      compareTheJobNameWarnings(
        LOCALE_EN_US.JOB_NAME_LENGTH_WARNING,
        createJobApp,
      );

      simulateEventWithText(jobNameField, 'input', jobNameWithOver100Chars);

      compareTheJobNameWarnings(
        LOCALE_EN_US.JOB_NAME_LENGTH_WARNING,
        createJobApp,
      );
    });
  });

  describe(`When: User types in a Job Name, which is existing`, () => {
    it(`Then: The UI displays a warning for job name`, async () => {
      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_POPULATED_RULESETS_RESPONSE);

      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_POPULATED_SCOPE_RESPONSE);

      fetchStub
        .withArgs(sinon.match(JOBS_API))
        .resolves(MOCK_POPULATED_JOB_RESPONSE);
      const jobNameExsisting = 'unit_test_job1';
      const createJobApp = await fixture('<e-create-job></e-create-job>');
      const col1 = createJobApp.shadowRoot
        .querySelector('.row')
        .querySelector('.col-1');
      const jobNameField = col1.querySelector('#jobName');
      simulateEventWithText(jobNameField, 'input', jobNameExsisting);

      compareTheJobNameWarnings(
        LOCALE_EN_US.JOB_NAME_EXISTS_WARNING,
        createJobApp,
      );
    });
  });

  describe(`When: Schedule Frequency and Schedule Time are filled in by the user`, () => {
    it(`Then: The UI displays a Schedule description`, async () => {
      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_POPULATED_RULESETS_RESPONSE);

      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_POPULATED_SCOPE_RESPONSE);

      fetchStub
        .withArgs(sinon.match(JOBS_API))
        .resolves(MOCK_POPULATED_JOB_RESPONSE);

      const createJobApp = await fixture('<e-create-job></e-create-job>');
      const col1 = createJobApp.shadowRoot
        .querySelector('.row')
        .querySelector('.col-1');

      col1
        .querySelector(`#${SCHEDULE_FREQUENCY}`)
        .shadowRoot.querySelectorAll('eui-menu > eui-menu-item')
        .forEach(element => {
          if (element.value === 'Today') {
            element.selected = true;
          }
        });

      col1.querySelector(`#${SCHEDULE_TIME}`).value = '04:33';

      await nextFrame();

      expect(col1.querySelector('#scheduleString').textContent.trim()).to.equal(
        'Today at 04:33',
      );
    });
  });

  describe(`When: A call is made to ${JOBS_API}, along with Create Job Parameters and a 400 is returned`, () => {
    it(`Then: The UI displays a notification saying, "Unable to save job"`, async () => {
      fetchStub
        .withArgs(sinon.match(JOBS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_BAD_REQUEST_RESPONSE);

      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_POPULATED_SCOPE_RESPONSE);

      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_POPULATED_RULESETS_RESPONSE);

      fetchStub
        .withArgs(sinon.match(JOBS_API))
        .resolves(MOCK_POPULATED_JOB_RESPONSE);

      const createJobApp = await fixture('<e-create-job></e-create-job>');
      const col1 = createJobApp.shadowRoot
        .querySelector('.row')
        .querySelector('.col-1');
      const col2 = createJobApp.shadowRoot
        .querySelector('.row')
        .querySelector('.col-2');

      col1.querySelector(`#${JOB_NAME}`).value = jobName;

      col1
        .querySelector(`#${SCHEDULE_FREQUENCY}`)
        .shadowRoot.querySelectorAll('eui-menu > eui-menu-item')
        .forEach(element => {
          if (element.value === 'Today') {
            element.selected = true;
          }
        });

      col1.querySelector(`#${SCHEDULE_TIME}`).value = '16:19';

      col1
        .querySelector(`#${RULESET_NAME}`)
        .shadowRoot.querySelectorAll('eui-menu > eui-menu-item')
        .forEach(element => {
          if (element.value === '1') {
            element.selected = true;
          }
        });

      col1
        .querySelector(`#${SCOPE_NAME}`)
        .shadowRoot.querySelectorAll('eui-menu > eui-menu-item')
        .forEach(element => {
          if (element.value === '1') {
            element.selected = true;
          }
        });

      const saveButton = col2.querySelector('#saveButton');
      simulateEvent(saveButton, 'click');

      await nextFrame();
      const notification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');
      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.UNABLE_TO_SAVE_JOB,
      );
    });
  });

  describe(`When: A call is made to ${JOBS_API}, along with Create Job Parameters and an unknown error occurs`, () => {
    it(`Then: The UI displays a notification saying, ${LOCALE_EN_US.SERVER_ERROR_LONG}`, async () => {
      fetchStub
        .withArgs(sinon.match(JOBS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(Promise.reject(new Error(LOCALE_EN_US.SERVER_ERROR_LONG)));

      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_POPULATED_RULESETS_RESPONSE);

      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_POPULATED_SCOPE_RESPONSE);

      fetchStub
        .withArgs(sinon.match(JOBS_API))
        .resolves(MOCK_POPULATED_JOB_RESPONSE);

      const createJobApp = await fixture('<e-create-job></e-create-job>');
      const col1 = createJobApp.shadowRoot
        .querySelector('.row')
        .querySelector('.col-1');
      const col2 = createJobApp.shadowRoot
        .querySelector('.row')
        .querySelector('.col-2');

      col1.querySelector(`#${JOB_NAME}`).value = jobName;

      col1
        .querySelector(`#${SCHEDULE_FREQUENCY}`)
        .shadowRoot.querySelectorAll('eui-menu > eui-menu-item')
        .forEach(element => {
          if (element.value === 'Today') {
            element.selected = true;
          }
        });

      col1.querySelector(`#${SCHEDULE_TIME}`).value = '16:19';

      col1
        .querySelector(`#${RULESET_NAME}`)
        .shadowRoot.querySelectorAll('eui-menu > eui-menu-item')
        .forEach(element => {
          if (element.value === '1') {
            element.selected = true;
          }
        });

      col1
        .querySelector(`#${SCOPE_NAME}`)
        .shadowRoot.querySelectorAll('eui-menu > eui-menu-item')
        .forEach(element => {
          if (element.value === '1') {
            element.selected = true;
          }
        });

      const saveButton = col2.querySelector('#saveButton');
      simulateEvent(saveButton, 'click');

      await nextFrame();
      const notification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.UNABLE_TO_SAVE_JOB,
      );
      expect(notification.description).to.equal(LOCALE_EN_US.SERVER_ERROR_LONG);
    });
  });

  describe(`When: A call is made to ${JOBS_API}, and User clicks the Save Button without filling values`, () => {
    it(`Then: The UI validates and displays warning notifications`, async () => {
      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_POPULATED_RULESETS_RESPONSE);

      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_POPULATED_SCOPE_RESPONSE);

      fetchStub
        .withArgs(sinon.match(JOBS_API))
        .resolves(MOCK_POPULATED_JOB_RESPONSE);

      const createJobApp = await fixture('<e-create-job></e-create-job>');
      const col2 = createJobApp.shadowRoot
        .querySelector('.row')
        .querySelector('.col-2');

      const saveButton = col2.querySelector('#saveButton');
      simulateEvent(saveButton, 'click');

      await nextFrame();

      compareTheJobNameWarnings(
        LOCALE_EN_US.JOB_NAME_REQUIRED_WARNING,
        createJobApp,
      );
      compareTheWarnings(
        LOCALE_EN_US.FIELD_REQUIRED_WARNING,
        SCHEDULE_TIME,
        createJobApp,
      );
      compareTheWarnings(
        LOCALE_EN_US.FIELD_REQUIRED_WARNING,
        RULESET_NAME,
        createJobApp,
      );
      compareTheWarnings(
        LOCALE_EN_US.FIELD_REQUIRED_WARNING,
        SCOPE_NAME,
        createJobApp,
      );
    });
  });

  describe(`When: Rulesets are fetched and a 404 response code is returned`, () => {
    it(`Then: The UI does not populate the Rulesets dropdown and displays a notification saying, ${LOCALE_EN_US.RULESETS_FETCHING_FAILURE}
      `, async () => {
      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_NOT_FOUND_RESPONSE);

      const createJobApp = await fixture('<e-create-job></e-create-job>');
      const col1 = createJobApp.shadowRoot
        .querySelector('.row')
        .querySelector('.col-1');

      const actualRulesetErrorMsg = col1
        .querySelector(`#${RULESET_NAME}`)
        .shadowRoot.querySelector(
          '.dropdown > eui-menu > eui-menu-item > div',
        ).innerHTML;

      expect(
        actualRulesetErrorMsg.includes(LOCALE_EN_US.RETRIEVAL_ERROR_MSG_SHORT),
      );
      expect(actualRulesetErrorMsg.includes(LOCALE_EN_US.SERVER_ERROR_SHORT));
    });
  });

  describe(`When: A call is made to ${JOBS_API}, fetch the Scopes and a 400 response is returned for Rulesets`, () => {
    it('Then: The UI does not populate the Scopes dropdown', async () => {
      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_NOT_FOUND_RESPONSE);

      const createJobApp = await fixture('<e-create-job></e-create-job>');
      const col1 = createJobApp.shadowRoot
        .querySelector('.row')
        .querySelector('.col-1');

      const actualScopesErrorMsg = col1
        .querySelector(`#${SCOPE_NAME}`)
        .shadowRoot.querySelector(
          '.dropdown > eui-menu > eui-menu-item > div',
        ).innerHTML;

      expect(
        actualScopesErrorMsg.includes(LOCALE_EN_US.RETRIEVAL_ERROR_MSG_SHORT),
      );
      expect(actualScopesErrorMsg.includes(LOCALE_EN_US.SERVER_ERROR_SHORT));
    });
  });

  describe(`When: Rulesets are fetched and an unknown error occurs`, () => {
    it(`Then: The UI does not populate the Rulesets dropdown and an error message is displayed, ${LOCALE_EN_US.RULESETS_FETCHING_FAILURE}
      `, async () => {
      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(Promise.reject(new Error(LOCALE_EN_US.SERVER_ERROR_SHORT)));

      const createJobApp = await fixture('<e-create-job></e-create-job>');
      const col1 = createJobApp.shadowRoot
        .querySelector('.row')
        .querySelector('.col-1');

      const actualRulesetErrorMsg = col1
        .querySelector(`#${RULESET_NAME}`)
        .shadowRoot.querySelector(
          '.dropdown > eui-menu > eui-menu-item > div',
        ).innerHTML;

      expect(
        actualRulesetErrorMsg.includes(LOCALE_EN_US.RETRIEVAL_ERROR_MSG_SHORT),
      );
      expect(actualRulesetErrorMsg.includes(LOCALE_EN_US.SERVER_ERROR_SHORT));
    });
  });

  describe(`When: A call is made to ${SCOPES_API} and an unknown error occurs`, () => {
    it('Then: The UI does not populate the Scopes dropdown and an error message is displayed', async () => {
      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(Promise.reject(new Error(LOCALE_EN_US.SERVER_ERROR_SHORT)));

      const createJobApp = await fixture('<e-create-job></e-create-job>');
      const col1 = createJobApp.shadowRoot
        .querySelector('.row')
        .querySelector('.col-1');

      const actualScopesErrorMsg = col1
        .querySelector(`#${SCOPE_NAME}`)
        .shadowRoot.querySelector(
          '.dropdown > eui-menu > eui-menu-item > div',
        ).innerHTML;

      expect(
        actualScopesErrorMsg.includes(LOCALE_EN_US.RETRIEVAL_ERROR_MSG_SHORT),
      );
      expect(actualScopesErrorMsg.includes(LOCALE_EN_US.SERVER_ERROR_SHORT));
    });
  });

  describe(`When: Editing a job`, () => {
    it('Then: The UI for job editing is displayed and input fields are pre-populated', async () => {
      fetchStub
        .withArgs(sinon.match(JOBS_API))
        .resolves(MOCK_POPULATED_JOB_RESPONSE);

      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_POPULATED_SCOPE_RESPONSE);

      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_POPULATED_RULESETS_RESPONSE);

      const createJobApp = await fixture(
        `<e-create-job update-job="${UNIT_TEST_JOB}"></e-create-job>`,
      );

      expect(
        createJobApp.shadowRoot.querySelector('.app-header').innerText,
      ).to.equal(LOCALE_EN_US.UPDATE_JOB);

      expect(
        createJobApp.shadowRoot.querySelector('#jobName').innerText,
      ).to.equal(UNIT_TEST_JOB);

      const schedule = createJobApp.shadowRoot.querySelector('#scheduleString');
      const localCron = adjustCronToTimezone(createJobApp.job.schedule);
      const hour = localCron.split(' ')[2];
      expect(schedule.textContent).to.equal(`Repeat Daily at ${hour}:15`);

      const ruleSetDropdown = createJobApp.shadowRoot
        .querySelector('#rulesetName')
        .shadowRoot.querySelector('eui-button');

      expect(ruleSetDropdown.textContent).to.equal('ruleset1');

      const scopeDropdown = createJobApp.shadowRoot
        .querySelector('#scopeName')
        .shadowRoot.querySelector('eui-button');

      expect(scopeDropdown.textContent).to.equal('athlone');
    });
  });

  describe('When: Editing a job and clicking save', () => {
    it('Then: The job is updated and a succcess notification is displayed', async () => {
      fetchStub
        .withArgs(sinon.match(JOBS_API))
        .resolves(MOCK_POPULATED_JOB_RESPONSE);

      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_POPULATED_SCOPE_RESPONSE);

      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_POPULATED_RULESETS_RESPONSE);

      fetchStub
        .withArgs(sinon.match(JOBS_API), {
          method: 'PUT',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_SUCCESSFUL_UPDATE_JOB_RESPONSE);

      const createJobApp = await fixture(
        `<e-create-job update-job="${UNIT_TEST_JOB}"></e-create-job>`,
      );

      const newRuleset = createJobApp.shadowRoot
        .querySelector('#rulesetName')
        .shadowRoot.querySelector('eui-menu')
        .querySelector('eui-menu-item[label="ruleset2"]');

      simulateEvent(newRuleset, 'click');

      const newScope = createJobApp.shadowRoot
        .querySelector('#scopeName')
        .shadowRoot.querySelector('eui-menu')
        .querySelector('eui-menu-item[label="dublin"]');

      simulateEvent(newScope, 'click');

      await nextFrame();

      const saveButton = createJobApp.shadowRoot.querySelector('#saveButton');
      simulateEvent(saveButton, 'click');

      await nextFrame();
      const notification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      const substitutions = new Map();
      substitutions.set('%job_name%', UNIT_TEST_JOB);
      expect(notification.textContent.trim()).to.equal(
        updateLocaleString(LOCALE_EN_US.JOB_UPDATED, substitutions),
      );
    });
  });

  describe('When: Editing a job and a 409 response is returned', () => {
    it(`Then: The job is not updated and a notification saying ${LOCALE_EN_US.JOB_USED_IN_EXECUTION}`, async () => {
      fetchStub
        .withArgs(sinon.match(JOBS_API))
        .resolves(MOCK_POPULATED_JOB_RESPONSE);

      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_POPULATED_SCOPE_RESPONSE);

      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_POPULATED_RULESETS_RESPONSE);

      fetchStub
        .withArgs(sinon.match(JOBS_API), {
          method: 'PUT',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_CONFLICT_RESPONSE);

      const createJobApp = await fixture(
        `<e-create-job update-job="${UNIT_TEST_JOB}"></e-create-job>`,
      );

      const saveButton = createJobApp.shadowRoot.querySelector('#saveButton');
      simulateEvent(saveButton, 'click');

      await nextFrame();
      const notification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(notification.description).to.equal(
        `${LOCALE_EN_US.JOB_USED_IN_EXECUTION}`,
      );
    });
  });

  describe('When: Editing a job and a 500 response is returned', () => {
    it(`Then: The job is not updated and a notification saying ${LOCALE_EN_US.SERVER_ERROR_LONG} is displayed`, async () => {
      fetchStub
        .withArgs(sinon.match(JOBS_API))
        .resolves(MOCK_POPULATED_JOB_RESPONSE);

      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_POPULATED_SCOPE_RESPONSE);

      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_POPULATED_RULESETS_RESPONSE);

      fetchStub
        .withArgs(sinon.match(JOBS_API), {
          method: 'PUT',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(Promise.reject(new Error('Unable to contact server')));

      const createJobApp = await fixture(
        `<e-create-job update-job="${UNIT_TEST_JOB}"></e-create-job>`,
      );

      const saveButton = createJobApp.shadowRoot.querySelector('#saveButton');
      simulateEvent(saveButton, 'click');

      await nextFrame();
      const notification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(notification.description).to.equal(
        `${LOCALE_EN_US.SERVER_ERROR_LONG}`,
      );
    });
  });

  describe(`When: A call is made to ${JOBS_API} and a 403 response is returned`, () => {
    it('Then: The UI displays the access denied error component', async () => {
      fetchStub
        .withArgs(sinon.match(JOBS_API))
        .resolves(MOCK_ACCESS_DENIED_RESPONSE);

      const createJobApp = await fixture('<e-create-job></e-create-job>');

      const accessDeniedError = createJobApp.shadowRoot.querySelector(
        'e-access-denied-error',
      );

      expect(accessDeniedError).to.exist;
    });
  });

  describe(`When: A creating a job and a 403 response is returned`, () => {
    it(`Then: The UI displays a notification saying, ${LOCALE_EN_US.REST_403_DESCRIPTION}`, async () => {
      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_POPULATED_RULESETS_RESPONSE);

      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_POPULATED_SCOPE_RESPONSE);

      fetchStub
        .withArgs(sinon.match(JOBS_API))
        .resolves(MOCK_POPULATED_JOB_RESPONSE);

      const createJobApp = await fixture('<e-create-job></e-create-job>');
      const col1 = createJobApp.shadowRoot
        .querySelector('.row')
        .querySelector('.col-1');
      const col2 = createJobApp.shadowRoot
        .querySelector('.row')
        .querySelector('.col-2');

      col1.querySelector(`#${JOB_NAME}`).value = jobName;

      col1
        .querySelector(`#${SCHEDULE_FREQUENCY}`)
        .shadowRoot.querySelectorAll('eui-menu > eui-menu-item')
        .forEach(element => {
          if (element.value === 'Today') {
            element.selected = true;
          }
        });

      col1.querySelector(`#${SCHEDULE_TIME}`).value = '16:19';

      col1
        .querySelector(`#${RULESET_NAME}`)
        .shadowRoot.querySelectorAll('eui-menu > eui-menu-item')
        .forEach(element => {
          if (element.value === '1') {
            element.selected = true;
          }
        });

      col1
        .querySelector(`#${SCOPE_NAME}`)
        .shadowRoot.querySelectorAll('eui-menu > eui-menu-item')
        .forEach(element => {
          if (element.value === '1') {
            element.selected = true;
          }
        });

      fetchStub
        .withArgs(sinon.match(JOBS_API), {
          method: 'POST',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_ACCESS_DENIED_RESPONSE);

      const saveButton = col2.querySelector('#saveButton');
      simulateEvent(saveButton, 'click');

      await nextFrame();
      const notification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.ACCESS_DENIED,
      );
      expect(notification.description).to.equal(
        LOCALE_EN_US.REST_403_DESCRIPTION,
      );
    });
  });

  describe('When: Editing a job and a 403 response is returned', () => {
    it(`Then: The job is not updated and a notification saying ${LOCALE_EN_US.REST_403_DESCRIPTION} is displayed`, async () => {
      fetchStub
        .withArgs(sinon.match(JOBS_API))
        .resolves(MOCK_POPULATED_JOB_RESPONSE);

      fetchStub
        .withArgs(sinon.match(SCOPES_API))
        .resolves(MOCK_POPULATED_SCOPE_RESPONSE);

      fetchStub
        .withArgs(sinon.match(RULESETS_API))
        .resolves(MOCK_POPULATED_RULESETS_RESPONSE);

      const createJobApp = await fixture(
        `<e-create-job update-job="${UNIT_TEST_JOB}"></e-create-job>`,
      );

      fetchStub
        .withArgs(sinon.match(JOBS_API), {
          method: 'PUT',
          headers: sinon.match.any,
          body: sinon.match.any,
        })
        .resolves(MOCK_ACCESS_DENIED_RESPONSE);

      const saveButton = createJobApp.shadowRoot.querySelector('#saveButton');
      simulateEvent(saveButton, 'click');

      await nextFrame();
      const notification = document
        .querySelector('#notifications-column')
        .querySelector('eui-notification');

      expect(notification.textContent.trim()).to.equal(
        LOCALE_EN_US.ACCESS_DENIED,
      );
      expect(notification.description).to.equal(
        `${LOCALE_EN_US.REST_403_DESCRIPTION}`,
      );
    });
  });
});
