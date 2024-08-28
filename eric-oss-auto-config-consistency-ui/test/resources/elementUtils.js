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

import { nextFrame } from '@open-wc/testing';

import { ApproveForAll } from '../../src/utils/attributes/changeAttributes.js';

export const simulateEvent = (element, name) => {
  element.dispatchEvent(
    new CustomEvent(name, {
      bubbles: true,
      cancelable: true,
    }),
  );
};

export const simulateEventWithText = (element, name, text) => {
  const event = new Event(name, {
    bubbles: true,
    cancelable: true,
  });
  element.value = text;
  element.dispatchEvent(event);
};

export const simulateFileEvent = (element, name, file) => {
  const event = new CustomEvent(name, {
    bubbles: true,
    cancelable: true,
  });
  Object.defineProperty(event, 'target', {
    writable: false,
    value: { files: [file] },
  });
  element.dispatchEvent(event);
};

export const simulateDetailEvent = (element, name, rows) => {
  const event = new CustomEvent(name, {
    bubbles: true,
    cancelable: true,
  });
  Object.defineProperty(event, 'detail', {
    writable: false,
    value: rows,
  });
  element.dispatchEvent(event);
};

export const simulateDetailEventWithText = (element, name, text) => {
  const event = new CustomEvent(name, {
    bubbles: true,
    cancelable: true,
  });
  Object.defineProperty(event, 'detail', {
    writable: false,
    value: { value: text },
  });
  element.dispatchEvent(event);
};

export const getHeaderText = tableCellElement => {
  const selection = tableCellElement.querySelector('div eui-tooltip div div');
  return selection.textContent;
};

export const getColumnText = tableCellElement => {
  const selection = tableCellElement.querySelector('div span');
  return selection.textContent;
};

export const resetNotifications = () => {
  const notificationColumn = document.querySelector('#notifications-column');
  if (notificationColumn !== null) {
    const euiNotification =
      notificationColumn.querySelector('eui-notification');
    if (euiNotification) {
      euiNotification.remove();
    }
  }
};

export const triggerDeleteNotification = async (app, objectName) => {
  const tableContainer = app.shadowRoot.querySelector('.app-table-container');

  const table = tableContainer.querySelector('eui-table');

  const tableData = table.shadowRoot
    .querySelector('tbody')
    .querySelectorAll('tr');

  simulateEvent(tableData[0], 'click');
  await nextFrame();

  const deleteButton = app.shadowRoot
    .querySelector('.action-buttons')
    .querySelector(`#delete-${objectName}`);

  simulateEvent(deleteButton, 'click');
  await nextFrame();

  const deleteDialog = app.shadowRoot.querySelector('eui-dialog');

  const deleteButtonInDialog = deleteDialog.querySelector('eui-button');

  simulateEvent(deleteButtonInDialog, 'click');
  await nextFrame();
};

export const triggerPostChangesNotification = async app => {
  const applyButton = app.shadowRoot.querySelector(
    'e-apply-button#applySelected',
  );

  simulateEvent(applyButton, 'click');
  await nextFrame();

  const confirmDialog = app.shadowRoot.querySelector('eui-dialog');

  const confirmButtonInDialog = confirmDialog.querySelector(
    'eui-button#confirm-changes',
  );

  simulateEvent(confirmButtonInDialog, 'click');
  await nextFrame();
};

export const triggerRevertChangesNotification = async app => {
  const changesTable = app.shadowRoot
    .querySelector('e-rest-paginated-table')
    .shadowRoot.querySelector('eui-table');

  const tableData = changesTable.shadowRoot
    .querySelector('table')
    .querySelector('tbody')
    .querySelectorAll('tr');

  simulateEvent(tableData[0], 'click');

  const revertDialog = app.shadowRoot.querySelector('eui-dialog#revertDialog');
  app.revertChanges(ApproveForAll.SELECTION);

  const revertButton = revertDialog.querySelector('eui-button');
  simulateEvent(revertButton, 'click');

  await nextFrame();
};
