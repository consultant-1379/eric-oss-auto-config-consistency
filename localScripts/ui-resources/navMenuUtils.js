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

import { routes } from "./routeUtils";

const MENU_ITEMS = [
    {
      "id": "eacc-<SIGNUM>",
      "name": "eacc-<SIGNUM>",
      "displayName": "Automated Configuration Consistency <SIGNUM>",
      "childNames": ["executions-<SIGNUM>", "jobs-<SIGNUM>", "settings-<SIGNUM>"],
      "menuPath": routes.eacc
    },
    {
      "id": "executions-<SIGNUM>",
      "name": "executions-<SIGNUM>",
      "displayName": "Executions Overview <SIGNUM>",
      "menuPath": routes.executions,
      "descriptionLong": "Listing all executions"
    },
    {
      "id": "jobs-<SIGNUM>",
      "name": "jobs-<SIGNUM>",
      "displayName": "Jobs <SIGNUM>",
      "menuPath": routes.jobs,
      "descriptionLong": "Listing all jobs"
    },
    {
      "id": "settings-<SIGNUM>",
      "name": "settings-<SIGNUM>",
      "displayName": "Settings <SIGNUM>",
      "menuPath": routes.settings,
      "descriptionLong": "Settings for rulesets and scopes"
    }
]

/**
   * Configures navigation menu items
   *
   * @function setupNavigationMenu
   */
function setupNavigationMenu(bubbleSource, currentPage) {
    bubbleSource.bubble('portal:set-local-menu', MENU_ITEMS);
    requestAnimationFrame(() => bubbleSource.bubble('portal:activate-menu-item', currentPage));
}   

/**
 * Opens the navigation menu
 *
 * @function openNavigationMenu
 */
function openNavigationMenu() {
    document.querySelector("body > eui-container").shadowRoot
        .querySelector("main > div > eui-app-bar")
        .menuOpen = true;
}

export {
    setupNavigationMenu,
    openNavigationMenu
}