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

const HOST = new URL(window.location);
const HTTP_GET = 'GET';
const HTTP_DELETE = 'DELETE';
const HTTP_POST = 'POST';
const HTTP_PUT = 'PUT';
const API_GATEWAY_URL = '/eacc';
const APPLICATION_JSON = 'application/json';
const LAUNCHER_URL = `/#launcher`;
const EACC_URL =
  HOST.hostname === 'localhost'
    ? `${'http://'}${HOST.hostname}${':8080'}`
    : API_GATEWAY_URL;

const restCalls = {
  eacc: {
    getJobs: {
      url: `${EACC_URL}/v1/jobs`,
      method: HTTP_GET,
    },
    deleteJob: {
      url: `${EACC_URL}/v1/jobs`,
      method: HTTP_DELETE,
      headers: {
        'Content-Type': `${APPLICATION_JSON}; charset=utf-8`,
      },
    },
    postJob: {
      url: `${EACC_URL}/v1/jobs`,
      method: HTTP_POST,
      headers: {
        'Content-Type': `${APPLICATION_JSON}; charset=utf-8`,
        Accept: APPLICATION_JSON,
      },
    },
    putJob: {
      url: `${EACC_URL}/v1/jobs`,
      method: HTTP_PUT,
      headers: {
        'Content-Type': `${APPLICATION_JSON}; charset=utf-8`,
        Accept: APPLICATION_JSON,
      },
    },
    getExecutions: {
      url: `${EACC_URL}/v1/executions`,
      method: HTTP_GET,
    },
    postProposedChanges: {
      url: `${EACC_URL}/v1/executions`,
      method: HTTP_POST,
      headers: {
        'Content-Type': `${APPLICATION_JSON}`,
      },
    },
    getRulesets: {
      url: `${EACC_URL}/v1/rulesets`,
      method: HTTP_GET,
    },
    deleteRuleset: {
      url: `${EACC_URL}/v1/rulesets`,
      method: HTTP_DELETE,
      headers: {
        'Content-Type': `${APPLICATION_JSON}; charset=utf-8`,
      },
    },
    getNodeSets: {
      url: `${EACC_URL}/v1/scopes`,
      method: HTTP_GET,
    },
    postNodeSet: {
      url: `${EACC_URL}/v1/scopes`,
      method: HTTP_POST,
      headers: {
        Accept: APPLICATION_JSON,
      },
    },
    postRuleset: {
      url: `${EACC_URL}/v1/rulesets`,
      method: HTTP_POST,
      headers: {
        Accept: APPLICATION_JSON,
      },
    },
    deleteNodeSet: {
      url: `${EACC_URL}/v1/scopes`,
      method: HTTP_DELETE,
      headers: {
        'Content-Type': `${APPLICATION_JSON}; charset=utf-8`,
      },
    },
    putNodeSet: {
      url: `${EACC_URL}/v1/scopes`,
      method: HTTP_PUT,
      headers: {
        Accept: APPLICATION_JSON,
      },
    },
    putRuleset: {
      url: `${EACC_URL}/v1/rulesets`,
      method: HTTP_PUT,
      headers: {
        Accept: APPLICATION_JSON,
      },
    },
  },
};

// URL encoding
const PERCENTAGE = '%';
const DOLLAR = '$';

export { restCalls, EACC_URL, LAUNCHER_URL, PERCENTAGE, DOLLAR };
