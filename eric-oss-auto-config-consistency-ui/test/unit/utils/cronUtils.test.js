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

import { expect } from '@open-wc/testing';

import {
  convertToCron,
  adjustCronToTimezone,
  COMPLEX_SCHEDULE_ERROR,
} from '../../../src/utils/cronUtils.js';

/**
 * Unit tests for Cron Utils
 */

describe('Given: A Spring Cron generator', () => {
  describe('When: Schedule Frequency(Today) & Schedule Time are given', () => {
    it('Then: A corresponding spring cron is generated', async () => {
      const date = new Date();
      date.setHours(16);
      date.setMinutes(14);
      expect(convertToCron('Today', '16:14')).to.equal(
        `0 ${date.getUTCMinutes()} ${date.getUTCHours()} ${date.getUTCDate()} ${
          date.getUTCMonth() + 1
        } *`,
      );
    });
  });

  describe('When: Schedule Frequency(Daily) & Schedule Time are given', () => {
    it('Then: A corresponding spring cron is generated', async () => {
      const date = new Date();
      date.setHours(16);
      date.setMinutes(14);
      expect(convertToCron('Daily', '16:14')).to.equal(
        `0 ${date.getUTCMinutes()} ${date.getUTCHours()} ? * *`,
      );
    });
  });

  describe('When: Schedule Frequency(Today) & Schedule Time given as 00:00', () => {
    it('Then: A corresponding spring cron is generated, where UTC calculation is ', async () => {
      const date = new Date();
      date.setHours(0);
      date.setMinutes(0);
      expect(convertToCron('Today', '00:00')).to.equal(
        `0 ${date.getUTCMinutes()} ${date.getUTCHours()} ${date.getUTCDate()} ${
          date.getUTCMonth() + 1
        } *`,
      );
    });
  });

  describe('When: A daily UTC Cron is converted to local time', () => {
    it('Then: minutes and hours are correctly adjusted', async () => {
      const date = new Date();
      const utcCron = `0 ${date.getUTCMinutes()} ${date.getUTCHours()} ? * *`;
      const localCron = `0 ${date.getMinutes()} ${date.getHours()} ? * *`;
      expect(adjustCronToTimezone(utcCron)).to.equal(localCron);
    });
  });

  describe('When: A UTC Cron set to run on a specific date is converted to local time', () => {
    it('Then: minutes, hours, date, and month, are all correctly adjusted', async () => {
      const testCases = [
        ['0 30 23 31 12 *', '0 30 0 1 1 *'],
        ['0 27 18 14 11 *', '0 27 19 14 11 *'],
      ];

      testCases.forEach(testCase => {
        expect(adjustCronToTimezone(testCase[0], -60)).to.equal(testCase[1]);
      });
    });
  });

  describe('When: adjustCronToTimezone is called and the month is too small and it is not a leap year', () => {
    it('Then: The month does not roll over to the next', async () => {
      const testCases = [
        ['0 59 23 27 2 *', '0 59 0 28 2 *'],
        ['0 59 23 28 2 *', '0 59 0 1 3 *'],
        ['0 59 23 29 4 *', '0 59 0 30 4 *'],
        ['0 59 23 29 6 *', '0 59 0 30 6 *'],
        ['0 59 23 29 9 *', '0 59 0 30 9 *'],
        ['0 59 23 29 11 *', '0 59 0 30 11 *'],
      ];

      testCases.forEach(testCase => {
        expect(
          adjustCronToTimezone(
            testCase[0],
            -60,
            new Date('December 31, 2023 12:00:00'),
          ),
        ).to.equal(testCase[1]);

        expect(
          adjustCronToTimezone(
            testCase[1],
            60,
            new Date('December 31, 2023 12:00:00'),
          ),
        ).to.equal(testCase[0]);
      });
    });
  });

  describe('When: adjustCronToTimezone is called and the month is too small and it is a leap year', () => {
    it('Then: The month does not roll over to the next', async () => {
      const testCase = ['0 59 23 28 2 *', '0 59 0 29 2 *'];

      expect(
        adjustCronToTimezone(
          testCase[0],
          -60,
          new Date('December 31, 2024 12:00:00'),
        ),
      ).to.equal(testCase[1]);
    });
  });

  describe('When: A complex UTC CRON which was not created via UI is converted to local time', () => {
    it('Then: a COMPLEX_SCHEDULE_ERROR is thrown', async () => {
      expect(() => adjustCronToTimezone('0 0/30 12 ? * MON')).to.throw(
        COMPLEX_SCHEDULE_ERROR,
      );
    });
  });
});
