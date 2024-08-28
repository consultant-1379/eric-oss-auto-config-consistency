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

const DEFAULT_LONG_DESCRIPTION = undefined;
const DEFAULT_TIMEOUT = 5000;
const MANUAL_CLOSE = 0;

/**
 * The notification status options e.g. success or failure.
 */
const NotificationStatus = {
  SUCCESS: 'success',
  FAILURE: 'failure',
};

/**
 * Create the notification component.
 *
 * @param timeout The duration of the notification.
 *
 * @function createEuiNotification
 * @private
 */
const _createEuiNotification = (parent, timeout) => {
  const notification = parent.createElement('eui-notification');
  notification.timeout = timeout;
  return notification;
};

/**
 * Create the notification icon component.
 *
 * @param status The status to create e.g. Success or Failure.
 *
 * @function createEuiIcon
 * @private
 */
const _createEuiIcon = status => {
  const icon = document.createElement('eui-icon');
  icon.size = '18px';
  icon.style = 'display: inline-block; vertical-align: middle;';
  icon.setAttribute(
    'name',
    NotificationStatus.SUCCESS === status ? 'check' : 'failed',
  );
  icon.setAttribute(
    'color',
    NotificationStatus.SUCCESS === status ? 'var(--green-35)' : 'var(--red-52)',
  );
  return icon;
};

/**
 * Creates a Notification based on the status and description provided.
 *
 * @param parent The parent app/component reponsible for creating the notification.
 * @param status The notification status e.g Success or Failure.
 * @param shortDescription The short description of the notification.
 * @param longDescription The long description of the notification.
 * @param timeout The duration in ms for the notification to be displayed.
 * 0 indicates the notification will stay open until manually closed by the user.
 * The default timeout value is 5000 ms.
 *
 * @function createNotification
 */
const createNotification = (
  parent,
  status,
  shortDescription,
  longDescription = DEFAULT_LONG_DESCRIPTION,
  timeout = DEFAULT_TIMEOUT,
  showIcon = true,
) => {
  const notification = _createEuiNotification(parent, timeout);
  notification.textContent = ` ${shortDescription}`;
  if (longDescription) {
    notification.description = longDescription;
  }
  if (showIcon) {
    const notificationIcon = _createEuiIcon(status);
    notification.prepend(notificationIcon);
  }
  notification.showNotification();
};

export {
  NotificationStatus,
  createNotification,
  DEFAULT_LONG_DESCRIPTION,
  DEFAULT_TIMEOUT,
  MANUAL_CLOSE,
};
