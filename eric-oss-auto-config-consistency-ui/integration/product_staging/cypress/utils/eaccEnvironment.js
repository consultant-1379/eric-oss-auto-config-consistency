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

const INGRESS_LOGIN_USER_ENV = 'INGRESS_LOGIN_USER';
const INGRESS_LOGIN_PASSWORD_ENV = 'INGRESS_LOGIN_PASSWORD';
const BUILD_NUMBER_ENV = 'BUILD_NUMBER';

/**
 * Returns the GAS username.
 *
 * @returns the GAS username.
 */
const getGasUsername = () => Cypress.env(INGRESS_LOGIN_USER_ENV);

/**
 * Returns the GAS password.
 *
 * @returns the GAS password.
 */
const getGasPassword = () => Cypress.env(INGRESS_LOGIN_PASSWORD_ENV);

/**
 * Returns the GAS build number.
 *
 * @returns the GAS build number.
 */
const getGasBuildNumber = () => Cypress.env(BUILD_NUMBER_ENV);

export { getGasUsername, getGasPassword, getGasBuildNumber };
