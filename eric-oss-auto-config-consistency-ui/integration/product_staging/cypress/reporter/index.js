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

const fs = require('fs');

const RESULTS_DIR = './cypress/results';
const RESULTS_FILE = `${RESULTS_DIR}/cypress_tests.json`;

const COUNT = 'count';
const PASSED = 'passed';
const FAILED = 'failed';
const RESULTS = 'results';

const PASS = 'pass';
const FAIL = 'fail';

const DEFAULT_REPORT = { count: 0, passed: 0, failed: 0, results: {} };

/* eslint-disable no-console */
function createResultsDir() {
  try {
    if (!fs.existsSync(RESULTS_DIR)) {
      fs.mkdirSync(RESULTS_DIR);
    }
  } catch (err) {
    console.error(`Failed to create: ${RESULTS_DIR}`, err);
  }
}

function createResultsFile() {
  try {
    if (!fs.existsSync(RESULTS_FILE)) {
      fs.writeFileSync(RESULTS_FILE, JSON.stringify(DEFAULT_REPORT), err => {
        if (err) {
          throw err;
        }
        console.log(`Created ${RESULTS_FILE}`);
      });
    }
  } catch (error) {
    console.error(`Failed to create: ${RESULTS_FILE}`, error);
  }
}

function readResultsFile() {
  createResultsDir();
  createResultsFile();
  try {
    return fs.readFileSync(RESULTS_FILE, 'UTF-8');
  } catch (error) {
    console.error(`Failed to read: ${RESULTS_FILE}`, error);
    throw error;
  }
}

function addSuite(data, suite) {
  if (data[RESULTS][suite] === undefined) {
    data[RESULTS][suite] = [];
  }
}

function addResult(data, result, suite, test, err) {
  addSuite(data, suite);

  const resultObject = err
    ? { title: test, status: result, message: err }
    : { title: test, status: result };

  data[RESULTS][suite].push(resultObject);
}

function addTest(data, result, suite, test, err) {
  data[COUNT] += 1;
  if (result === PASS) {
    data[PASSED] += 1;
  } else {
    data[FAILED] += 1;
  }

  addResult(data, result, suite, test, err);
}

function writeResults(data) {
  const json = JSON.stringify(data, null, 2);
  try {
    fs.writeFileSync(RESULTS_FILE, json, err => {
      if (err) {
        throw err;
      }
      console.log(`Updated ${RESULTS_FILE}`);
    });
  } catch (error) {
    console.error(`Failed to write file: ${RESULTS_FILE}`, error);
  }
}

function Reporter(runner, options) {
  this._options = options;
  this._runner = runner;
  this.results = {};
  this.suiteTitle = '';

  this._runner.on('start', () => {
    this.results = JSON.parse(readResultsFile());
  });

  this._runner.on('suite', suite => {
    if (suite.title) {
      console.log('▢', suite.title);
      this.suiteTitle = suite.title;
    }
  });

  this._runner.on('pass', test => {
    console.log(' ✓', test.title);
    addTest(this.results, PASS, this.suiteTitle, test.title);
  });

  this._runner.on('fail', (test, err) => {
    console.log(' ✗', test.title, err.message);
    addTest(this.results, FAIL, this.suiteTitle, test.title, err.message);
  });

  this._runner.on('end', () => {
    writeResults(this.results);
  });
}
/* eslint-enable no-console */

module.exports = Reporter;
