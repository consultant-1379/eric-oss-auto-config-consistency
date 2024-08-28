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
 * Unit tests for notifications
 */
import { expect, fixture } from '@open-wc/testing';
import sinon from 'sinon';

import Jobs from '../../../../src/apps/jobs/jobs.js';

/* eslint-disable import/no-named-default */
import { default as LOCALE_EN_US } from '../../../../src/apps/jobs/locale/en-us.json';

import {
  NotificationStatus,
  createNotification,
} from '../../../../src/utils/notification/notifications.js';

import { resetNotifications } from '../../../resources/elementUtils.js';

const MOCK_LOCALE_RESPONSE = {
  json: () => LOCALE_EN_US,
  ok: true,
  status: 200,
};

// attributes
const TIMEOUT = 'timeout';
const NAME = 'name';
const COLOR = 'color';
// attribute values
const DEFAULT_TIMEOUT = 5000;
const FAILED = 'failed';
const RED = 'var(--red-52)';
// query selectors
const EUI_ICON = 'eui-icon';

const SHORT_DESCRIPTION = 'A short desc...';

const RADIX = 10;

const getNotification = () =>
  document
    .querySelector('#notifications-column')
    .querySelector('eui-notification');

let fetchStub;

describe('Given: An app that wants to notify the user', () => {
  before(() => {
    Jobs.register('e-jobs');

    fetchStub = sinon.stub(window, 'fetch');

    fetchStub.callsFake(url => {
      if (url.indexOf('locale') !== -1) {
        return Promise.resolve(MOCK_LOCALE_RESPONSE);
      }
      return Promise.resolve(true);
    });
  });

  afterEach(() => {
    resetNotifications();
  });

  describe('When: a successfull minimum arg notification is created', () => {
    it('Then: It is displayed with a green tick and short description', async () => {
      const clientApp = await fixture('<e-jobs></e-jobs>');

      createNotification(
        clientApp,
        NotificationStatus.SUCCESS,
        SHORT_DESCRIPTION,
      );

      const notification = getNotification();
      const euiIcon = notification.querySelector(EUI_ICON);

      const timeoutValue = parseInt(notification.getAttribute(TIMEOUT), RADIX);
      expect(timeoutValue).to.equal(DEFAULT_TIMEOUT);

      expect(euiIcon.getAttribute(NAME)).to.equal('check');
      expect(euiIcon.getAttribute(COLOR)).to.equal('var(--green-35)');
      expect(notification.textContent.trim()).to.equal(SHORT_DESCRIPTION);
    });
  });

  describe('When: a failure minimum arg notification is created', () => {
    it('Then: It is displayed with a red x and short description', async () => {
      const clientApp = await fixture('<e-jobs></e-jobs>');

      createNotification(
        clientApp,
        NotificationStatus.FAILURE,
        SHORT_DESCRIPTION,
      );

      const notification = getNotification();
      const euiIcon = notification.querySelector(EUI_ICON);

      const timeoutValue = parseInt(notification.getAttribute(TIMEOUT), RADIX);
      expect(timeoutValue).to.equal(DEFAULT_TIMEOUT);

      expect(euiIcon.getAttribute(NAME)).to.equal(FAILED);
      expect(euiIcon.getAttribute(COLOR)).to.equal(RED);
      expect(notification.textContent.trim()).to.equal(SHORT_DESCRIPTION);
    });
  });

  describe('When: a failure all arg notification is created', () => {
    it('Then: It is displayed with a red x, a short and long description', async () => {
      const clientApp = await fixture('<e-jobs></e-jobs>');
      const longDescription = 'A long description';
      const timeout = 0;

      createNotification(
        clientApp,
        NotificationStatus.FAILURE,
        SHORT_DESCRIPTION,
        longDescription,
        timeout,
      );

      const notification = getNotification();
      const euiIcon = notification.querySelector(EUI_ICON);

      const timeoutValue = parseInt(notification.getAttribute(TIMEOUT), RADIX);
      expect(timeoutValue).to.equal(timeout);

      const description = notification.getAttribute('description');
      expect(description).to.equal(longDescription);

      expect(euiIcon.getAttribute(NAME)).to.equal(FAILED);
      expect(euiIcon.getAttribute(COLOR)).to.equal(RED);
      expect(notification.textContent.trim()).to.equal(SHORT_DESCRIPTION);
    });
  });
});
