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

import { routes } from './routeUtils.js';

const MENU_ITEMS = [
  {
    id: 'eacc',
    name: 'eacc',
    displayName: 'Automated Configuration Consistency',
    childNames: ['executions', 'jobs', 'settings'],
    menuPath: routes.eacc,
  },
  {
    id: 'executions',
    name: 'executions',
    displayName: 'Executions Overview',
    menuPath: routes.executions,
    descriptionLong: 'Listing all executions',
  },
  {
    id: 'jobs',
    name: 'jobs',
    displayName: 'Jobs',
    menuPath: routes.jobs,
    descriptionLong: 'Listing all jobs',
  },
  {
    id: 'settings',
    name: 'settings',
    displayName: 'Settings',
    menuPath: routes.settings,
    descriptionLong: 'Settings for rulesets and scopes',
  },
];

/**
 * Configures navigation menu items
 *
 * @function setupNavigationMenu
 */
function setupNavigationMenu(bubbleSource, currentPage) {
  bubbleSource.bubble('portal:set-local-menu', MENU_ITEMS);
  requestAnimationFrame(() =>
    bubbleSource.bubble('portal:activate-menu-item', currentPage),
  );
}

/**
 * Opens the navigation menu
 *
 * @function openNavigationMenu
 */
function openNavigationMenu() {
  document
    .querySelector('body > eui-container')
    .shadowRoot.querySelector('main > div > eui-app-bar').menuOpen = true;
}

export { setupNavigationMenu, openNavigationMenu };
