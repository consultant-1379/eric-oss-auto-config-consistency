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

process.env.NODE_ENV = 'test';
const webTestRunner = require('@snowpack/web-test-runner-plugin');
const { defaultReporter } = require('@web/test-runner');
const { junitReporter } = require('@web/test-runner-junit-reporter');

module.exports = {
  coverage: true,
  coverageConfig: {
    exclude: [
      'libs/**',
      '**/node_modules/**/*',
      '**/web_modules/**/*',
      '**/npm/**/*',
    ],
  },
  testRunnerHtml: testFramework =>
    `<html>
      <body>
        <script
          src="../../node_modules/@webcomponents/scoped-custom-element-registry/scoped-custom-element-registry.min.js"></script>
        <script type="module" src="${testFramework}"></script>
      </body>
    </html>`,
  nodeResolve: true,
  plugins: [webTestRunner()],
  files: 'test/**/*.test.js',
  reporters: [
    defaultReporter({ reportTestResults: true, reportTestProgress: true }),
    junitReporter({
      outputPath: './coverage/junit/test-results.xml',
      reportLogs: true,
    }),
  ],
};
