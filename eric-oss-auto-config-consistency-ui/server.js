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

/* eslint-disable */
const express = require('express');
const bodyParser = require('body-parser');

const cors = require('cors');
const multer = require('multer');
const upload = multer();

// create application/json parser
const jsonParser = bodyParser.json();

const app = express();
app.use(express.json());

const port = process.env.port || 3010;

const startTime = new Date().toISOString();

const executionsData = [
  {
    id: '1',
    jobName: 'all-ireland-01',
    executionType: 'Open Loop',
    executionStartedAt: `${startTime}`,
    executionEndedAt: '',
    consistencyAuditStartedAt: '2023-03-13T15:18:44.033Z',
    consistencyAuditEndedAt: '2023-03-13T15:18:44.033Z',
    executionStatus: 'Audit in Progress',
    totalMosAudited: '0',
    totalAttributesAudited: '0',
    inconsistenciesIdentified: '3',
  },
  {
    id: '2',
    jobName: 'all-ireland-02',
    executionType: 'Closed Loop',
    executionStartedAt: `${startTime}`,
    executionEndedAt: '',
    consistencyAuditStartedAt: '2023-04-13T19:18:25.033Z',
    consistencyAuditEndedAt: '2023-04-13T19:18:25.033Z',
    executionStatus: 'Audit Partially Successful',
    totalMosAudited: '0',
    totalAttributesAudited: '0',
    inconsistenciesIdentified: '0',
  },
  {
    id: '3',
    jobName: 'all-ireland-03',
    executionType: 'Open Loop',
    executionStartedAt: `${startTime}`,
    executionEndedAt: null,
    consistencyAuditStartedAt: '2023-03-13T16:18:44.033Z',
    consistencyAuditEndedAt: '2023-03-13T16:18:44.033Z',
    executionStatus: 'Audit in Progress',
    totalMosAudited: '1000',
    totalAttributesAudited: '5000',
    inconsistenciesIdentified: '5000',
  },
  {
    id: '4',
    jobName: 'all-ireland-04',
    executionType: 'Open Loop',
    executionStartedAt: `${startTime}`,
    executionEndedAt: '',
    consistencyAuditStartedAt: '2023-03-19T15:18:44.033Z',
    consistencyAuditEndedAt: '2023-03-19T15:18:44.033Z',
    executionStatus: 'Reversion Successful',
    totalMosAudited: '1000',
    totalAttributesAudited: '2333',
    inconsistenciesIdentified: '2',
  },
];

const auditResults = [
  {
    id: '5',
    managedObjectFdn:
      'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4',
    managedObjectType: 'NRCellDU',
    attributeName: 'csiRsShiftingPrimary',
    currentValue: 'DEACTIVATED',
    preferredValue: 'DEACTIVATED',
    auditStatus: 'Consistent',
    executionId: '3',
    ruleId: '16',
    changeStatus: 'Not applied',
  },
  {
    id: '6',
    managedObjectFdn:
      'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4',
    managedObjectType: 'NRCellDU',
    attributeName: 'subCarrierSpacing',
    currentValue: '120',
    preferredValue: '110',
    auditStatus: 'Inconsistent',
    executionId: '3',
    ruleId: '17',
    changeStatus: 'Not applied',
  },
  {
    id: '7',
    managedObjectFdn:
      'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-2',
    managedObjectType: 'NRCellDU',
    attributeName: 'csiRsShiftingPrimary',
    currentValue: 'DEACTIVATED',
    preferredValue: 'DEACTIVATED',
    auditStatus: 'Consistent',
    executionId: '3',
    ruleId: '16',
    changeStatus: 'Not applied',
  },
  {
    id: '8',
    managedObjectFdn:
      'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-2',
    managedObjectType: 'NRCellDU',
    attributeName: 'subCarrierSpacing',
    currentValue: '120',
    preferredValue: '110',
    auditStatus: 'Inconsistent',
    executionId: '3',
    ruleId: '17',
    changeStatus: 'Not applied',
  },
  {
    id: '9',
    managedObjectFdn:
      'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-3',
    managedObjectType: 'NRCellDU',
    attributeName: 'csiRsShiftingPrimary',
    currentValue: 'DEACTIVATED',
    preferredValue: 'DEACTIVATED',
    auditStatus: 'Consistent',
    executionId: '3',
    ruleId: '16',
    changeStatus: 'Not applied',
  },
  {
    id: '10',
    managedObjectFdn:
      'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-3',
    managedObjectType: 'NRCellDU',
    attributeName: 'subCarrierSpacing',
    currentValue: '120',
    preferredValue: '110',
    auditStatus: 'Inconsistent',
    executionId: '3',
    ruleId: '17',
    changeStatus: 'Not applied',
  },
  {
    id: '11',
    managedObjectFdn:
      'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-1',
    managedObjectType: 'NRCellDU',
    attributeName: 'csiRsShiftingPrimary',
    currentValue: 'DEACTIVATED',
    preferredValue: 'DEACTIVATED',
    auditStatus: 'Consistent',
    executionId: '3',
    ruleId: '16',
    changeStatus: 'Not applied',
  },
  {
    id: '12',
    managedObjectFdn:
      'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-1',
    managedObjectType: 'NRCellDU',
    attributeName: 'subCarrierSpacing',
    currentValue: '120',
    preferredValue: '110',
    auditStatus: 'Inconsistent',
    executionId: '3',
    ruleId: '17',
    changeStatus: 'Not applied',
  },
  {
    id: '13',
    managedObjectFdn:
      'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1',
    managedObjectType: 'GNBDUFunction',
    attributeName: 'dlBbCapacityMaxLimit',
    currentValue: 'null',
    preferredValue: '100',
    auditStatus: 'Inconsistent',
    executionId: '3',
    ruleId: '15',
    changeStatus: 'Not applied',
  },
  {
    id: '14',
    managedObjectFdn:
      'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00030-1',
    managedObjectType: 'NRCellCU',
    attributeName: 'mcpcPSCellEnabled',
    currentValue: 'false',
    preferredValue: 'true',
    auditStatus: 'Inconsistent',
    executionId: '3',
    ruleId: '14',
    changeStatus: 'Not applied',
  },
  {
    id: '15',
    managedObjectFdn:
      'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00030-2',
    managedObjectType: 'NRCellCU',
    attributeName: 'mcpcPSCellEnabled',
    currentValue: 'false',
    preferredValue: 'true',
    auditStatus: 'Inconsistent',
    executionId: '1',
    ruleId: '14',
    changeStatus: 'Not applied',
  },
  {
    id: '16',
    managedObjectFdn:
      'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00030-3',
    managedObjectType: 'NRCellCU',
    attributeName: 'mcpcPSCellEnabled',
    currentValue: 'false',
    preferredValue: 'true',
    auditStatus: 'Inconsistent',
    executionId: '1',
    ruleId: '14',
    changeStatus: 'Not applied',
  },
  {
    id: '17',
    managedObjectFdn:
      'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00030-4',
    managedObjectType: 'NRCellCU',
    attributeName: 'mcpcPSCellEnabled',
    currentValue: 'false',
    preferredValue: 'true',
    auditStatus: 'Inconsistent',
    executionId: '1',
    ruleId: '14',
    changeStatus: 'Not applied',
  },
  {
    id: '18',
    managedObjectFdn:
      'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1',
    managedObjectType: 'GNBCUCPFunction',
    attributeName: 'maxNgRetryTime',
    currentValue: '30',
    preferredValue: '20',
    auditStatus: 'Inconsistent',
    executionId: '5',
    ruleId: '12',
    changeStatus: 'Not applied',
  },
  {
    id: '19',
    managedObjectFdn:
      'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1',
    managedObjectType: 'GNBCUCPFunction',
    attributeName: 'endpointResDepHEnabled',
    currentValue: 'true',
    preferredValue: 'true',
    auditStatus: 'Consistent',
    executionId: '5',
    ruleId: '13',
    changeStatus: 'Not applied',
  },
];

let jobsData = [
  {
    jobName: 'all-ireland-03',
    schedule: '0 30 12 ? * *',
    rulesetName: 'ruleset1',
    scopeName: 'athlone',
  },
  {
    jobName: 'rest-posted-schedule',
    schedule: '0 0/30 12 ? * MON',
    rulesetName: 'ruleset1',
    scopeName: 'athlone',
  },
  {
    jobName: 'all-ireland-01',
    schedule: '0 45 23 31 12 *',
    rulesetName: 'ruleset1',
    scopeName: 'dublin',
  },
];

let rulesetData = [
  {
    id: '3f2504e0-4f89-11d3-9a0c-0305e82c3301',
    rulesetName: 'ruleset1',
    uri: 'v1/rulesets/3f2504e0-4f89-11d3-9a0c-0305e82c3301',
  },
  {
    id: '3f2504e0-4f89-11d3-9a0c-0305e82c3302',
    rulesetName: 'ruleset2',
    uri: 'v1/rulesets/3f2504e0-4f89-11d3-9a0c-0305e82c3302',
  },
];

let scopeData = [
  {
    id: '13aa6e40-a9e2-4fa2-9468-16c4797a5ca0',
    scopeName: 'athlone',
    uri: 'v1/scopes/13aa6e40-a9e2-4fa2-9468-16c4797a5ca0',
  },
  {
    id: '13aa6e40-a9e2-4fa2-9468-16c4797a5ca1',
    scopeName: 'dublin',
    uri: 'v1/scopes/13aa6e40-a9e2-4fa2-9468-16c4797a5ca1',
  },
];

const ruleValidationErrorsData = [
  {
    lineNumber: 2,
    errorType: 'Invalid Condition.',
    errorDetails:
      "Attributes in Conditions must be from the moType field (ENodeBFunction)'s attributes",
    additionalInfo: '',
  },
  {
    lineNumber: 3,
    errorType: 'Invalid priority value.',
    errorDetails:
      'Priority value must be given if the moType & attrName combination is not unique.',
    additionalInfo: '',
  },
  {
    lineNumber: 4,
    errorType: 'Invalid Condition.',
    errorDetails:
      "MoType not specified, Attributes must be specified as '<MO Type>.<Attribute Name>'",
    additionalInfo:
      "Attributes must be specified as '<MO Type>.<Attribute Name>'",
  },
  {
    lineNumber: 5,
    errorType: 'Invalid Rule values combination.',
    errorDetails:
      'This moType, attribute name and priority combination already exists.',
    additionalInfo: '',
  },
  {
    lineNumber: 6,
    errorType: 'Invalid priority value.',
    errorDetails:
      'Priority value must be given if the moType & attrName combination is not unique.',
    additionalInfo: '',
  },
  {
    lineNumber: 7,
    errorType: 'Invalid Condition.',
    errorDetails:
      'Attribute(s) in Conditions not found in Managed Object model.',
    additionalInfo: '',
  },
  {
    lineNumber: 8,
    errorType: 'Invalid Rule values combination.',
    errorDetails:
      'This moType, attribute name and priority combination already exists.',
    additionalInfo: '',
  },
  {
    lineNumber: 9,
    errorType: 'Invalid priority value.',
    errorDetails:
      'Priority value must be given if the moType & attrName combination is not unique.',
    additionalInfo: '',
  },
  {
    lineNumber: 10,
    errorType: 'Invalid Condition.',
    errorDetails:
      "MoType not specified, Attributes must be specified as '<MO Type>.<Attribute Name>'",
    additionalInfo:
      "Attributes must be specified as '<MO Type>.<Attribute Name>'",
  },
  {
    lineNumber: 11,
    errorType: 'Invalid Rule values combination.',
    errorDetails:
      'This moType, attribute name and priority combination already exists.',
    additionalInfo: '',
  },
  {
    lineNumber: 12,
    errorType: 'Invalid priority value.',
    errorDetails:
      'Priority value must be given if the moType & attrName combination is not unique.',
    additionalInfo: '',
  },
  {
    lineNumber: 13,
    errorType: 'Invalid Condition.',
    errorDetails:
      "MoType not specified, Attributes must be specified as '<MO Type>.<Attribute Name>'",
    additionalInfo:
      "Attributes must be specified as '<MO Type>.<Attribute Name>'",
  },
  {
    lineNumber: 14,
    errorType: 'Invalid Condition.',
    errorDetails:
      "Attributes in Conditions must be from the moType field (EUtranCellFDD)'s attributes",
    additionalInfo: '',
  },
];

const ONE_SECOND = 1000;

let begin = false;

function sleep(seconds) {
  return new Promise(resolve => {
    setTimeout(resolve, seconds * ONE_SECOND);
  });
}

async function simulateApplyChanges(
  approveForAll,
  auditResultIds,
  executionId,
) {
  let results = approveForAll
    ? auditResults.filter(
        auditResult =>
          auditResult.executionId.match(executionId) &&
          auditResult.auditStatus.match('Inconsistent'),
      )
    : auditResults.filter(auditResult =>
        auditResultIds.includes(auditResult.id),
      );
  const finalStatus = ['Implementation complete', 'Implementation failed'];
  results.map(auditResult => {
    auditResult.changeStatus = 'Implementation in progress';
    auditResult.ack = true;
  });
  await sleep(15);
  results.map(
    auditResult =>
      (auditResult.changeStatus =
        finalStatus[Math.floor(Math.random() * finalStatus.length)]),
  );
}

async function simulateRevert(approveForAll, auditResultIds) {
  let results = approveForAll
    ? auditResults.filter(auditResult => auditResult?.ack === true)
    : auditResults.filter(
        auditResult =>
          auditResultIds.includes(auditResult.id) && auditResult?.ack === true,
      );
  const finalStatus = ['Reversion complete', 'Reversion failed'];
  results.map(
    auditResult => (auditResult.changeStatus = 'Reversion in progress'),
  );
  await sleep(20);
  results.map(
    auditResult =>
      (auditResult.changeStatus =
        finalStatus[Math.floor(Math.random() * finalStatus.length)]),
  );
}

async function simulateExecutions(deleteExecutions) {
  // prettier-ignore
  while (!begin) { // NOSONAR
    /* eslint-disable no-await-in-loop */
    await sleep(5);
  }

  executionsData[0].executionStatus = 'Audit Partially Successful';
  executionsData[0].executionEndedAt = new Date().toISOString();

  const today = new Date().toISOString();
  executionsData.push({
    id: '5',
    jobName: 'all-ireland-05',
    executionType: 'Open Loop',
    executionStartedAt: `${today}`,
    executionEndedAt: '',
    consistencyAuditStartedAt: `${today}`,
    consistencyAuditEndedAt: `${today}`,
    executionStatus: 'Audit in Progress',
    totalMosAudited: '1',
    totalAttributesAudited: '50',
    inconsistenciesIdentified: '17',
  });

  executionsData.push({
    id: '6',
    jobName: 'all-ireland-06',
    executionType: 'Open Loop',
    executionStartedAt: `${today}`,
    executionEndedAt: '',
    consistencyAuditStartedAt: `${today}`,
    consistencyAuditEndedAt: `${today}`,
    executionStatus: 'Audit Skipped',
    totalMosAudited: '0',
    totalAttributesAudited: '0',
    inconsistenciesIdentified: '0',
  });

  await sleep(25);

  executionsData[1].executionStatus = 'Audit Failed';
  executionsData[1].executionEndedAt = new Date().toISOString();

  await sleep(25);

  executionsData[2].executionStatus = 'Audit Successful';
  executionsData[2].executionEndedAt = new Date().toISOString();

  await sleep(25);

  executionsData[3].executionStatus = 'Audit Failed';
  executionsData[3].executionEndedAt = new Date().toISOString();

  await sleep(25);

  executionsData[4].executionStatus = 'Audit Successful';
  executionsData[4].executionEndedAt = new Date().toISOString();

  if (deleteExecutions) {
    await sleep(25);

    executionsData.splice(1, 1);

    await sleep(50);
    executionsData.splice(2, 1);

    await sleep(25);
    executionsData.splice(0, executionsData.length);
  }

  begin = false;
}

app.use(
  cors({
    origin: ['http://localhost:8080'],
    methods: ['*'],
  }),
);

app.use(express.static('build'));

app.get('/', (req, res) => {
  res.sendFile(`${__dirname}/build/index.html`);
});

app.get('/v1/executions/', (req, res) => {
  /* eslint-disable no-console */
  console.log('/v1/executions/', new Date().toTimeString());
  begin = true;

  res.send(executionsData);
});

app.get('/v1/executions/:id/audit-results', (req, res) => {
  /* eslint-disable no-console */
  console.log(
    `/v1/executions/${req.params.id}/audit-results`,
    req.query,
    new Date().toTimeString(),
  );

  const alignedAuditResults = auditResults.filter(
    e => e.executionId === req.params.id,
  );

  let subFilters = new Map();
  if (req.query.filter !== undefined) {
    let filterParts = req.query.filter.split('$');
    filterParts.forEach(filter => {
      let [filterKey, filterValue] = filter.split(':');
      subFilters.set(filterKey, filterValue);
    });
  }

  const page = req.query.page === undefined ? 1 : req.query.page;
  const pageSize = req.query.pageSize === undefined ? 2000 : req.query.pageSize;

  let filteredResults = alignedAuditResults;

  subFilters.forEach((value, key) => {
    console.log({ key }, { value });
    filteredResults = filteredResults.filter(element => {
      // LIKE
      if (value.match(/\%.+\%/)) {
        return element[key]
          .toLowerCase()
          .includes(value.replaceAll('%', '').toLowerCase());
      }
      // IN
      else if (value.match(/\(.+\)/)) {
        return value
          .replace('(', '')
          .replace(')', '')
          .split(',')
          .includes(element[key]);
      }
      // EQUALITY
      else {
        return element[key] === value;
      }
    }, key);
  });

  const paginatedResponse = {
    totalElements: filteredResults.length,
    totalPages: Math.ceil(alignedAuditResults.length / pageSize),
    currentPage: page,
    perPage: pageSize,
    hasNext: true,
    hasPrev: true,
    results: filteredResults,
  };
  res.send(paginatedResponse);
});

app.post('/v1/executions/:id/audit-results', (req, res) => {
  /* eslint-disable no-console */
  console.log(
    `POST /v1/executions/${req.params.id}/audit-results`,
    new Date().toTimeString(),
  );
  if (req.body.operation === 'APPLY_CHANGE') {
    simulateApplyChanges(
      req.body.approveForAll,
      req.body.auditResultIds,
      req.params.id,
    );
  } else {
    simulateRevert(req.body.approveForAll, req.body.auditResultIds);
  }

  res.statusCode = 200;
  res.send();
});

app.get('/v1/jobs/', (req, res) => {
  /* eslint-disable no-console */
  console.log('/v1/jobs/', new Date().toTimeString());

  res.send(jobsData);
});

app.delete('/v1/jobs/:jobName', (req, res) => {
  /* eslint-disable no-console */
  console.log(
    `DELETE /v1/jobs/${req.params.jobName}`,
    new Date().toTimeString(),
  );
  jobsData = jobsData.filter(job => job.jobName !== req.params.jobName);
  res.sendStatus(204);
});

app.post('/v1/jobs/', jsonParser, (req, res) => {
  /* eslint-disable no-console */
  console.log('POST /v1/jobs/', new Date().toTimeString(), req.body);
  jobsData.push(req.body);
  res.statusCode = 201;
  res.send(JSON.stringify(req.body));
});

app.put('/v1/jobs/:jobName', jsonParser, (req, res) => {
  /* eslint-disable no-console */
  console.log(
    `PUT /v1/jobs/${req.params.jobName}`,
    new Date().toTimeString(),
    req.body,
  );
  let jobToUpdateIndex = jobsData.findIndex(
    job => job.jobName === req.body.jobName,
  );
  jobsData[jobToUpdateIndex] = req.body;
  res.statusCode = 200;
  res.send(req.body);
});

app.get('/v1/rulesets/', (req, res) => {
  console.log('/v1/rulesets/', new Date().toTimeString());

  res.statusCode = 200;
  res.send(rulesetData);
});

app.delete('/v1/rulesets/:id', (req, res) => {
  console.log(
    `DELETE /v1/rulesets/${req.params.id}`,
    new Date().toTimeString(),
  );
  rulesetData = rulesetData.filter(ruleset => ruleset.id !== req.params.id);
  res.sendStatus(204);
});

app.get('/v1/scopes/', (req, res) => {
  console.log('/v1/scopes/', new Date().toTimeString());

  res.send(scopeData);
});

const scopeUploadFields = upload.fields([
  { name: 'scopeName', maxCount: 1 },
  { name: 'fileName', maxCount: 1 },
]);

app.post('/v1/scopes/', scopeUploadFields, (req, res) => {
  console.log(
    'POST /v1/scopes/',
    new Date().toTimeString(),
    req.body,
    req.files,
  );

  const createdScope = {
    id: '13aa6e40-a9e2-4fa2-9468-16c4797a5ca0',
    scopeName: req.body['scopeName'],
    uri: 'v1/scopes/13aa6e40-a9e2-4fa2-9468-16c4797a5ca0',
  };
  scopeData.push(createdScope);
  res.send(createdScope);
});

const rulesetUploadFields = upload.fields([
  { name: 'rulesetName', maxCount: 1 },
  { name: 'fileName', maxCount: 1 },
]);

app.post('/v1/rulesets/', rulesetUploadFields, (req, res) => {
  console.log(
    'POST /v1/rulesets/',
    new Date().toTimeString(),
    req.body,
    req.files,
  );

  const _rulesetName = req.body['rulesetName'];

  if (_rulesetName.includes('invalid')) {
    const errorResponse = {
      title: 'Problems found in ruleset.',
      status: 400,
      detail:
        'Ruleset cannot contain any invalid MO types, attributes or values.',
      ruleValidationErrors: ruleValidationErrorsData,
    };
    res.status(400).send(errorResponse);
  } else {
    const createdRuleset = {
      id: '13aa6e40-a9e2-4fa2-9468-16c4797a5ca0',
      rulesetName: _rulesetName,
      uri: 'v1/rulesets/13aa6e40-a9e2-4fa2-9468-16c4797a5ca0',
    };
    rulesetData.push(createdRuleset);
    res.send(createdRuleset);
  }
});

app.put('/v1/rulesets/:id', rulesetUploadFields, (req, res) => {
  console.log(
    `PUT /v1/rulesets/${req.params.id}`,
    new Date().toTimeString(),
    req.body,
    req.files,
  );
  // Can't actually do anything here since we only simulate metadata, so just send back the original ruleset.
  const rulesetToUpdate = rulesetData.find(
    ruleset => ruleset.id === req.params.id,
  );
  res.send(rulesetToUpdate);
});

app.put('/v1/scopes/:id', scopeUploadFields, (req, res) => {
  console.log(
    `PUT /v1/scopes/${req.params.id}`,
    new Date().toTimeString(),
    req.body,
    req.files,
  );
  // Can't actually do anything here since we only simulate metadata, so just send back the original scope.
  const scopeToUpdate = scopeData.find(scope => scope.id === req.params.id);
  res.send(scopeToUpdate);
});

app.delete('/v1/scopes/:id', (req, res) => {
  console.log(`DELETE /v1/scopes/${req.params.id}`, new Date().toTimeString());
  scopeData = scopeData.filter(scope => scope.id !== req.params.id);
  res.sendStatus(204);
});

app.listen(port, () => {
  /* eslint-disable-next-line */
  console.log(
    `Backend Service - "EACC mock server" is running on port http://localhost:${port}`,
  );
  simulateExecutions(false);
});
