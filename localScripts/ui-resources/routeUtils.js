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

const routes = {
  eacc: "eacc-<SIGNUM>/",
  executions: "eacc-<SIGNUM>/executions-<SIGNUM>",
  jobs: "eacc-<SIGNUM>/jobs-<SIGNUM>",
  "execution-reports":
    "eacc-<SIGNUM>/executions-<SIGNUM>/execution-reports-<SIGNUM>",
  "create-job": "eacc-<SIGNUM>/jobs-<SIGNUM>/create-job-<SIGNUM>",
  "update-job": "eacc-<SIGNUM>/jobs-<SIGNUM>/update-job-<SIGNUM>",
  settings: "eacc-<SIGNUM>/settings-<SIGNUM>",
};

export { routes };
