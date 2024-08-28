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

const { defineConfig } = require('cypress');

module.exports = defineConfig({
  projectId: 'eacc-ui',
  retries: 2,
  e2e: {
    setupNodeEvents(on, config) {
      return config;
    },
    experimentalRunAllSpecs: true,
    trashAssetsBeforeRuns: true,
    baseUrl: 'http://localhost:4200',
    specPattern: 'cypress/e2e/**/*.{js,jsx,ts,tsx}',
  },
});
