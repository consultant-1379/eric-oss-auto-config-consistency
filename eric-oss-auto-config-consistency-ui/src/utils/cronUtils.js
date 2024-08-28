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

const MILLISECONDS_PER_MINUTE = 60000;

const COMPLEX_SCHEDULE_ERROR = new Error('Schedule not created via UI');

const isWildcard = token => /[*?]/.test(token);
const isNumeric = token => /^\d+$/.test(token);

/**
 * Identifies whether a cron is too complex to have been created through the UI.
 *
 * @function scheduleNotCreatedThroughUI
 *
 * @param cron The cron expression to assess for complexity.
 *
 * @returns true if the cron is too complex to have been created through the UI. Otherwise, false.
 */
const scheduleNotCreatedThroughUI = cron => {
  const [seconds, minutes, hours, dayofmonth, month, dayofweek] =
    cron.split(' ');

  if (!(isNumeric(seconds) && isNumeric(minutes) && isNumeric(hours))) {
    return true;
  }
  if (
    !(isNumeric(dayofmonth) || isWildcard(dayofmonth)) ||
    !(isNumeric(month) || isWildcard(month))
  ) {
    return true;
  }
  if (!isWildcard(dayofweek)) {
    return true;
  }
  return false;
};

const convertToCron = (scheduleFrequency, scheduleTime) => {
  const date = new Date();
  let minute = '';
  let hour = '';
  let dayOfMonth = '';
  let month = '';

  [hour, minute] = scheduleTime.split(':');
  date.setHours(hour);
  date.setMinutes(minute);
  hour = date.getUTCHours();
  minute = date.getUTCMinutes();

  if (scheduleFrequency === 'Today') {
    dayOfMonth = date.getUTCDate();
    month = date.getUTCMonth() + 1;
  } else if (scheduleFrequency === 'Daily') {
    dayOfMonth = '?';
    month = '*';
  }

  return `0 ${minute} ${hour} ${dayOfMonth} ${month} *`;
};

/**
 * Adjust the hours and seconds fields in a UTC CRON expression to reflect local time.
 *
 * @function adjustCronToTimezone
 *
 * @throws COMPLEX_SCHEDULE_ERROR if the cron cannot be converted to a different timezone.
 *
 * @param utcCron The CRON in UTC time
 * @param offset Optional param. Number of minutes to offset CRON by. If unspecified, system time will be used.
 * @param utcTime Optional param. The date object in UTC, defaults to current time.
 *
 * @returns A CRON schedule with hours and seconds adjusted to local time zone.
 */
const adjustCronToTimezone = (
  utcCron,
  offset = new Date().getTimezoneOffset(),
  utcTime = new Date(),
) => {
  if (scheduleNotCreatedThroughUI(utcCron)) {
    throw COMPLEX_SCHEDULE_ERROR;
  }

  const [seconds, minutes, hours, dayofmonth, month, dayofweek] =
    utcCron.split(' ');

  utcTime.setMinutes(minutes);
  utcTime.setHours(hours);
  // Month MUST be set before day of month to prevent rollover if there are too many days.
  if (!isWildcard(month)) {
    utcTime.setMonth(month - 1); // -1 accounts for indexing difference between Date() and CRON
  }

  if (!isWildcard(dayofmonth)) {
    utcTime.setDate(dayofmonth);
    // If the month rolls over due to the date, set the month again
    if (utcTime.getMonth() !== month - 1 && !isWildcard(month)) {
      utcTime.setMonth(month - 1);
    }
  }

  const localTime = new Date(
    utcTime.getTime() - offset * MILLISECONDS_PER_MINUTE,
  );

  const returnDate = isWildcard(dayofmonth) ? dayofmonth : localTime.getDate();
  const returnMonth = isWildcard(month) ? month : localTime.getMonth() + 1; // +1 accounts for indexing difference between Date() and CRON

  return `${seconds} ${localTime.getMinutes()} ${localTime.getHours()} ${returnDate} ${returnMonth} ${dayofweek}`;
};

export {
  convertToCron,
  adjustCronToTimezone,
  isNumeric,
  COMPLEX_SCHEDULE_ERROR,
  MILLISECONDS_PER_MINUTE,
};
