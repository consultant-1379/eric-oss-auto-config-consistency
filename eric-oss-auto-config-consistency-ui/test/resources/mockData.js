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

import { ChangeStatus } from '../../src/utils/attributes/changeAttributes.js';

// Mocked Apps Data

export const MOCK_JOBS_EMPTY_ARRAY = () => [];

export const MOCK_JOBS_DATA_ARRAY = () => [
  {
    jobName: 'unit_test_job1',
    schedule: '0 15 11 ? * *',
    rulesetName: 'ruleset1',
    scopeName: 'athlone',
  },
  {
    jobName: 'unit_test_job2',
    schedule: '0 15 11 ? * *',
    rulesetName: 'ruleset2',
    scopeName: 'dublin',
  },
];

export const MOCK_JOBS_ONE_DELETED_DATA_ARRAY = () => [
  {
    jobName: 'unit_test_job1',
    schedule: '0 15 11 ? * *',
    rulesetName: 'test_ruleset',
    scopeName: 'test_scope',
  },
];

export const MOCK_EXECUTIONS_EMPTY_ARRAY = () => [];

export const MOCK_EXECUTIONS_DATA_ARRAY = () => [
  {
    id: '1',
    jobName: 'unit_test_job1',
    executionStatus: 'Audit Successful',
    executionType: 'Open Loop',
    executionStartedAt: '2023-03-13T15:18:44.033Z',
    executionEndedAt: '2023-03-13T15:18:44.033Z',
    consistencyAuditStartedAt: '2023-03-13T15:18:44.033Z',
    consistencyAuditEndedAt: '2023-03-13T15:18:44.033Z',
    totalMosAudited: '1',
    totalAttributesAudited: '12',
    inconsistenciesIdentified: '3',
  },
  {
    id: '2',
    jobName: 'unit_test_job2',
    executionStatus: 'Audit Successful',
    executionType: 'Open Loop',
    executionStartedAt: '2023-03-14T15:18:44.033Z',
    executionEndedAt: '2023-03-14T15:18:44.033Z',
    consistencyAuditStartedAt: '2023-03-14T15:18:44.033Z',
    consistencyAuditEndedAt: '2023-03-14T15:18:44.033Z',
    totalMosAudited: '10',
    totalAttributesAudited: '1',
    inconsistenciesIdentified: '8',
  },
  {
    id: '3',
    jobName: 'unit_test_job3',
    executionStatus: 'Audit Failed',
    executionType: 'Open Loop',
    executionStartedAt: '2023-03-15T15:18:44.033Z',
    executionEndedAt: '2023-03-15T15:18:44.033Z',
    consistencyAuditStartedAt: '2023-03-15T15:18:44.033Z',
    consistencyAuditEndedAt: '2023-03-15T15:18:44.033Z',
    totalMosAudited: '40',
    totalAttributesAudited: '20',
    inconsistenciesIdentified: '66',
  },
  {
    id: '4',
    jobName: 'unit_test_job4',
    executionStatus: 'Audit in Progress',
    executionType: 'Open Loop',
    executionStartedAt: '2023-03-16T15:18:44.033Z',
    executionEndedAt: '2023-03-16T15:18:44.033Z',
    consistencyAuditStartedAt: '2023-03-16T15:18:44.033Z',
    consistencyAuditEndedAt: '2023-03-16T15:18:44.033Z',
    totalMosAudited: '1',
    totalAttributesAudited: '1',
    inconsistenciesIdentified: '4',
  },
  {
    id: '5',
    jobName: 'unit_test_job5',
    executionStatus: 'Changes Failed',
    executionType: 'Open Loop',
    executionStartedAt: '2023-03-17T15:18:44.033Z',
    executionEndedAt: '2023-03-17T15:18:44.033Z',
    consistencyAuditStartedAt: '2023-03-17T15:18:44.033Z',
    consistencyAuditEndedAt: '2023-03-17T15:18:44.033Z',
    totalMosAudited: '40',
    totalAttributesAudited: '20',
    inconsistenciesIdentified: '66',
  },
  {
    id: '6',
    jobName: 'unit_test_job6',
    executionStatus: 'Changes in Progress',
    executionType: 'Open Loop',
    executionStartedAt: '2023-03-18T15:18:44.033Z',
    executionEndedAt: '2023-03-18T15:18:44.033Z',
    consistencyAuditStartedAt: '2023-03-18T15:18:44.033Z',
    consistencyAuditEndedAt: '2023-03-18T15:18:44.033Z',
    totalMosAudited: '1',
    totalAttributesAudited: '1',
    inconsistenciesIdentified: '4',
  },
];

export const MOCK_EXECUTIONS_DATA_ARRAY_WITH_DIFFERENT_EXECUTION_STATUSES =
  () => [
    {
      id: '1',
      jobName: 'unit_test_job1',
      executionStatus: 'Audit Successful',
      executionType: 'Open Loop',
      executionStartedAt: '2023-03-13T15:18:44.033Z',
      executionEndedAt: '2023-03-13T15:18:44.033Z',
      consistencyAuditStartedAt: '2023-03-13T15:18:44.033Z',
      consistencyAuditEndedAt: '2023-03-13T15:18:44.033Z',
      totalMosAudited: '1',
      totalAttributesAudited: '12',
      inconsistenciesIdentified: '3',
    },
    {
      id: '2',
      jobName: 'unit_test_job2',
      executionStatus: 'Audit Failed',
      executionType: 'Open Loop',
      executionStartedAt: '2023-03-14T15:18:44.033Z',
      executionEndedAt: '2023-03-14T15:18:44.033Z',
      consistencyAuditStartedAt: '2023-03-14T15:18:44.033Z',
      consistencyAuditEndedAt: '2023-03-14T15:18:44.033Z',
      totalMosAudited: '10',
      totalAttributesAudited: '1',
      inconsistenciesIdentified: '8',
    },
    {
      id: '3',
      jobName: 'unit_test_job3',
      executionStatus: 'Audit in Progress',
      executionType: 'Open Loop',
      executionStartedAt: '2023-03-15T15:18:44.033Z',
      executionEndedAt: '2023-03-15T15:18:44.033Z',
      consistencyAuditStartedAt: '2023-03-15T15:18:44.033Z',
      consistencyAuditEndedAt: '2023-03-15T15:18:44.033Z',
      totalMosAudited: '40',
      totalAttributesAudited: '20',
      inconsistenciesIdentified: '66',
    },
    {
      id: '4',
      jobName: 'unit_test_job4',
      executionStatus: 'Audit Partially Successful',
      executionType: 'Open Loop',
      executionStartedAt: '2023-03-16T15:18:44.033Z',
      executionEndedAt: '2023-03-16T15:18:44.033Z',
      consistencyAuditStartedAt: '2023-03-16T15:18:44.033Z',
      consistencyAuditEndedAt: '2023-03-16T15:18:44.033Z',
      totalMosAudited: '1',
      totalAttributesAudited: '1',
      inconsistenciesIdentified: '4',
    },
    {
      id: '5',
      jobName: 'unit_test_job5',
      executionStatus: 'Changes Failed',
      executionType: 'Open Loop',
      executionStartedAt: '2023-03-17T15:18:44.033Z',
      executionEndedAt: '2023-03-17T15:18:44.033Z',
      consistencyAuditStartedAt: '2023-03-17T15:18:44.033Z',
      consistencyAuditEndedAt: '2023-03-17T15:18:44.033Z',
      totalMosAudited: '40',
      totalAttributesAudited: '20',
      inconsistenciesIdentified: '66',
    },
    {
      id: '6',
      jobName: 'unit_test_job6',
      executionStatus: 'Changes in Progress',
      executionType: 'Open Loop',
      executionStartedAt: '2023-03-18T15:18:44.033Z',
      executionEndedAt: '2023-03-18T15:18:44.033Z',
      consistencyAuditStartedAt: '2023-03-18T15:18:44.033Z',
      consistencyAuditEndedAt: '2023-03-18T15:18:44.033Z',
      totalMosAudited: '1',
      totalAttributesAudited: '1',
      inconsistenciesIdentified: '4',
    },
    {
      id: '7',
      jobName: 'unit_test_job7',
      executionStatus: 'Changes Successful',
      executionType: 'Open Loop',
      executionStartedAt: '2023-03-17T15:18:44.033Z',
      executionEndedAt: '2023-03-17T15:18:44.033Z',
      consistencyAuditStartedAt: '2023-03-17T15:18:44.033Z',
      consistencyAuditEndedAt: '2023-03-17T15:18:44.033Z',
      totalMosAudited: '40',
      totalAttributesAudited: '20',
      inconsistenciesIdentified: '66',
    },
    {
      id: '8',
      jobName: 'unit_test_job8',
      executionStatus: 'Changes Partially Successful',
      executionType: 'Open Loop',
      executionStartedAt: '2023-03-18T15:18:44.033Z',
      executionEndedAt: '2023-03-18T15:18:44.033Z',
      consistencyAuditStartedAt: '2023-03-18T15:18:44.033Z',
      consistencyAuditEndedAt: '2023-03-18T15:18:44.033Z',
      totalMosAudited: '1',
      totalAttributesAudited: '1',
      inconsistenciesIdentified: '4',
    },
    {
      id: '9',
      jobName: 'unit_test_job9',
      executionStatus: 'Random Status',
      executionType: 'Open Loop',
      executionStartedAt: '2023-03-18T15:18:44.033Z',
      executionEndedAt: '2023-03-18T15:18:44.033Z',
      consistencyAuditStartedAt: '2023-03-18T15:18:44.033Z',
      consistencyAuditEndedAt: '2023-03-18T15:18:44.033Z',
      totalMosAudited: '1',
      totalAttributesAudited: '1',
      inconsistenciesIdentified: '4',
    },
    {
      id: '10',
      jobName: 'unit_test_job10',
      executionStatus: 'Audit Skipped',
      executionType: 'Open Loop',
      executionStartedAt: '2023-03-12T15:18:44.033Z',
      executionEndedAt: '',
      consistencyAuditStartedAt: '2023-03-12T15:18:44.033Z',
      consistencyAuditEndedAt: '2023-03-12T15:18:44.033Z',
      totalMosAudited: '0',
      totalAttributesAudited: '0',
      inconsistenciesIdentified: '0',
    },
  ];

export const MOCK_EXECUTIONS_DATA_ARRAY_TWO_ENTRIES = () => [
  {
    id: '100',
    jobName: 'unit_test_job1',
    executionStatus: 'Audit Successful',
    executionType: 'Open Loop',
    executionStartedAt: '2023-03-13T15:18:44.033Z',
    executionEndedAt: '2023-03-13T15:18:44.033Z',
    consistencyAuditStartedAt: '2023-03-13T15:18:44.033Z',
    consistencyAuditEndedAt: '2023-03-13T15:18:44.033Z',
    totalMosAudited: '1',
    totalAttributesAudited: '12',
    inconsistenciesIdentified: '3',
  },
  {
    id: '1',
    jobName: 'unit_test_job1',
    executionStatus: '',
    executionType: 'Open Loop',
    executionStartedAt: '2023-03-12T15:18:44.033Z',
    executionEndedAt: '',
    consistencyAuditStartedAt: '2023-03-12T15:18:44.033Z',
    consistencyAuditEndedAt: '2023-03-12T15:18:44.033Z',
    totalMosAudited: '1',
    totalAttributesAudited: '12',
    inconsistenciesIdentified: '3',
  },
];

export const MOCK_EXECUTIONS_EXTRA_LARGE_DATA_ARRAY = () =>
  Array.from({ length: 1000 }, () => ({
    id: 'objID',
    jobName: 'very_frequent_jobs',
    executionStatus: 'Audit in Progress',
    executionType: 'Open Loop',
    executionStartedAt: '2023-03-13T15:18:44.033Z',
    executionEndedAt: '2023-03-13T15:18:44.033Z',
    consistencyAuditStartedAt: '2023-03-13T15:18:44.033Z',
    consistencyAuditEndedAt: '2023-03-13T15:18:44.033Z',
    totalMosAudited: '1',
    totalAttributesAudited: '1',
    inconsistenciesIdentified: '4',
  }));

export const MOCK_AUDIT_RESULTS_EMPTY_ARRAY = () => ({
  totalElements: 0,
  totalPages: 0,
  currentPage: 1,
  perPage: 3,
  hasNext: false,
  hasPrev: false,
  results: [],
});

export const MOCK_AUDIT_RESULTS_DATA_ARRAY = () => [
  {
    id: '1',
    managedObjectFdn:
      'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4',
    managedObjectType: 'NRCellDU',
    attributeName: 'csiRsShiftingPrimary',
    currentValue: 'DEACTIVATED',
    preferredValue: 'DEACTIVATED',
    auditStatus: 'Consistent',
    executionId: '3',
    ruleId: '16',
    changeStatus: '',
  },
  {
    id: '2',
    managedObjectFdn:
      'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4',
    managedObjectType: 'NRCellDU',
    attributeName: 'subCarrierSpacing',
    currentValue: '120',
    preferredValue: '110',
    auditStatus: 'Inconsistent',
    executionId: '3',
    ruleId: '17',
    changeStatus: ChangeStatus.IMPLEMENTATION_COMPLETE,
  },
];

export const MOCK_AUDIT_RESULTS_FOR_CHANGES_DATA_ARRAY = () => ({
  totalElements: 4,
  totalPages: 0,
  currentPage: null,
  perPage: null,
  hasNext: false,
  hasPrev: false,
  results: [
    {
      id: '1',
      managedObjectFdn:
        'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4',
      managedObjectType: 'NRCellDU',
      attributeName: 'csiRsShiftingPrimary',
      currentValue: 'DEACTIVATED',
      preferredValue: 'DEACTIVATED',
      auditStatus: 'Consistent',
      executionId: '3',
      ruleId: '16',
      changeStatus: ChangeStatus.IMPLEMENTATION_COMPLETE,
    },
    {
      id: '2',
      managedObjectFdn:
        'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4',
      managedObjectType: 'NRCellDU',
      attributeName: 'subCarrierSpacing',
      currentValue: '120',
      preferredValue: '110',
      auditStatus: 'Inconsistent',
      executionId: '3',
      ruleId: '17',
      changeStatus: ChangeStatus.IMPLEMENTATION_COMPLETE,
    },
    {
      id: '3',
      managedObjectFdn:
        'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-5',
      managedObjectType: 'NRCellDU',
      attributeName: 'subCarrierSpacing',
      currentValue: '120',
      preferredValue: '110',
      auditStatus: 'Inconsistent',
      executionId: '3',
      ruleId: '17',
      changeStatus: ChangeStatus.IMPLEMENTATION_IN_PROGRESS,
    },
    {
      id: '4',
      managedObjectFdn:
        'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-6',
      managedObjectType: 'NRCellDU',
      attributeName: 'subCarrierSpacing',
      currentValue: '120',
      preferredValue: '110',
      auditStatus: 'Inconsistent',
      executionId: '3',
      ruleId: '17',
      changeStatus: ChangeStatus.IMPLEMENTATION_FAILED,
    },
  ],
});

export const MOCK_AUDIT_RESULTS_IS_PROPOSED_CHANGES_DATA_ARRAY = () => ({
  totalElements: 3,
  totalPages: 1,
  currentPage: 1,
  perPage: 3,
  hasNext: false,
  hasPrev: false,
  results: [
    {
      id: '2',
      managedObjectFdn:
        'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4',
      managedObjectType: 'NRCellDU',
      attributeName: 'subCarrierSpacing',
      currentValue: '120',
      preferredValue: '110',
      auditStatus: 'Inconsistent',
      executionId: '3',
      ruleId: '17',
      changeStatus: ChangeStatus.IMPLEMENTATION_COMPLETE,
    },
    {
      id: '3',
      managedObjectFdn:
        'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-5',
      managedObjectType: 'NRCellDU',
      attributeName: 'subCarrierSpacing',
      currentValue: '120',
      preferredValue: '110',
      auditStatus: 'Inconsistent',
      executionId: '3',
      ruleId: '17',
      changeStatus: ChangeStatus.IMPLEMENTATION_IN_PROGRESS,
    },
    {
      id: '4',
      managedObjectFdn:
        'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-6',
      managedObjectType: 'NRCellDU',
      attributeName: 'subCarrierSpacing',
      currentValue: '120',
      preferredValue: '110',
      auditStatus: 'Inconsistent',
      executionId: '3',
      ruleId: '17',
      changeStatus: ChangeStatus.IMPLEMENTATION_FAILED,
    },
  ],
});

export const MOCK_AUDIT_RESULTS_IN_PROGRESS_CHANGES_DATA_ARRAY = () => ({
  totalElements: 1,
  totalPages: 1,
  currentPage: 1,
  perPage: 1,
  hasNext: false,
  hasPrev: false,
  results: [
    {
      id: '2',
      managedObjectFdn:
        'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4',
      managedObjectType: 'NRCellDU',
      attributeName: 'subCarrierSpacing',
      currentValue: '120',
      preferredValue: '110',
      auditStatus: 'Inconsistent',
      executionId: '3',
      ruleId: '17',
      changeStatus: ChangeStatus.IMPLEMENTATION_IN_PROGRESS,
    },
  ],
});

export const MOCK_AUDIT_RESULTS_ALL_REVERTED_CHANGES_DATA_ARRAY = () => ({
  totalElements: 2,
  totalPages: 1,
  currentPage: 1,
  perPage: 1,
  hasNext: false,
  hasPrev: false,
  results: [
    {
      id: '1',
      managedObjectFdn:
        'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4',
      managedObjectType: 'NRCellDU',
      attributeName: 'subCarrierSpacing',
      currentValue: '120',
      preferredValue: '110',
      auditStatus: 'Inconsistent',
      executionId: '3',
      ruleId: '17',
      changeStatus: ChangeStatus.REVERSION_COMPLETE,
    },
    {
      id: '2',
      managedObjectFdn:
        'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4',
      managedObjectType: 'NRCellDU',
      attributeName: 'subCarrierSpacing',
      currentValue: '120',
      preferredValue: '110',
      auditStatus: 'Inconsistent',
      executionId: '3',
      ruleId: '17',
      changeStatus: ChangeStatus.REVERSION_COMPLETE,
    },
  ],
});

export const MOCK_AUDIT_RESULTS_ZERO_IN_PROGRESS_CHANGES_DATA_ARRAY = () => ({
  totalElements: 0,
  totalPages: 1,
  currentPage: 1,
  perPage: 1,
  hasNext: false,
  hasPrev: false,
  results: [],
});

export const MOCK_AUDIT_RESULTS_IS_NOT_PROPOSED_CHANGES_DATA_ARRAY_PAGE1 =
  () => ({
    totalElements: 4,
    totalPages: 2,
    currentPage: 1,
    perPage: 3,
    hasNext: true,
    hasPrev: false,
    results: [
      {
        id: '2',
        managedObjectFdn:
          'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4',
        managedObjectType: 'NRCellDU',
        attributeName: 'subCarrierSpacing',
        currentValue: '120',
        preferredValue: '110',
        auditStatus: 'Inconsistent',
        executionId: '3',
        ruleId: '17',
        changeStatus: ChangeStatus.IMPLEMENTATION_COMPLETE,
      },
      {
        id: '3',
        managedObjectFdn:
          'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-5',
        managedObjectType: 'NRCellDU',
        attributeName: 'subCarrierSpacing',
        currentValue: '120',
        preferredValue: '110',
        auditStatus: 'Inconsistent',
        executionId: '3',
        ruleId: '17',
        changeStatus: ChangeStatus.IMPLEMENTATION_IN_PROGRESS,
      },
      {
        id: '4',
        managedObjectFdn:
          'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-6',
        managedObjectType: 'NRCellDU',
        attributeName: 'subCarrierSpacing',
        currentValue: '120',
        preferredValue: '110',
        auditStatus: 'Inconsistent',
        executionId: '3',
        ruleId: '17',
        changeStatus: ChangeStatus.IMPLEMENTATION_FAILED,
      },
    ],
  });

export const MOCK_AUDIT_RESULTS_IS_NOT_PROPOSED_CHANGES_DATA_ARRAY_PAGE2 =
  () => ({
    totalElements: 4,
    totalPages: 2,
    currentPage: 2,
    perPage: 3,
    hasNext: false,
    hasPrev: true,
    results: [
      {
        id: '4',
        managedObjectFdn:
          'SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-6',
        managedObjectType: 'NRCellDU',
        attributeName: 'subCarrierSpacing',
        currentValue: '120',
        preferredValue: '110',
        auditStatus: 'Inconsistent',
        executionId: '3',
        ruleId: '17',
        changeStatus: ChangeStatus.IMPLEMENTATION_FAILED,
      },
    ],
  });

export const MOCK_RULESET_RESULTS_DATA_ARRAY = () => [
  {
    id: '1',
    rulesetName: 'ruleset1',
    uri: 'v1/rulesets/1',
  },
  {
    id: '2',
    rulesetName: 'ruleset2',
    uri: 'v1/rulesets/2',
  },
];

export const MOCK_JOBS_SUCCESSFUL_RESPONSE_DATA = () => ({
  jobName: 'all-ireland-1',
  schedule: '0 15 11 ? * *',
  rulesetName: 'ruleset1',
  scopeName: 'test_scope',
});

export const MOCK_JOBS_SUCCESSFUL_UPDATE_RESPONSE_DATA = () => ({
  jobName: 'unit_test_job1',
  schedule: '0 15 11 ? * *',
  rulesetName: 'ruleset2',
  scopeName: 'dublin',
});

export const SCOPE_FDNS = `
"SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00001,ManagedElement=NR03gNodeBRadio00001"
"SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002"
"SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00003,ManagedElement=NR03gNodeBRadio00003"`;

export const RULES = `
EUtranCellFDD,cellCapMinCellSubCap,500
EUtranCellFDD,cellCapMinMaxWriProt,true
EUtranCellFDD,cfraEnable,false`;

export const MOCK_EMPTY_ARRAY = () => [];

export const MOCK_SCOPES_DATA_ARRAY = () => [
  {
    id: '1',
    scopeName: 'athlone',
    uri: 'v1/scopes/1',
  },
  {
    id: '2',
    scopeName: 'dublin',
    uri: 'v1/scopes/2',
  },
];

export const MOCK_SCOPES_CREATED_SCOPE = () => ({
  id: '13aa6e40-a9e2-4fa2-9468-16c4797a5ca0',
  scopeName: 'scope_athlone-01',
  uri: 'v1/scopes/13aa6e40-a9e2-4fa2-9468-16c4797a5ca0',
});

export const MOCK_RULESETS_CREATED_RULESETS = () => ({
  id: '13aa6e40-a9e2-4fa2-9468-16c4797a5ca0',
  rulesetName: 'ruleset-01',
  uri: 'v1/rulesets/13aa6e40-a9e2-4fa2-9468-16c4797a5ca0',
});

export const MOCK_RULESETS_INVALID_RULES_ARRAY = async () => ({
  title: 'Problems found in ruleset.',
  status: 400,
  detail: 'Ruleset cannot contain any invalid MO types, attributes or values.',
  ruleValidationErrors: [
    {
      lineNumber: 2,
      errorType: 'Invalid Condition.',
      errorDetails:
        "MoType not specified, Attributes must be specified as '<MO Type>.<Attribute Name>'",
      additionalInfo:
        "Attributes must be specified as '<MO Type>.<Attribute Name>'",
    },
  ],
});

export const MOCK_RULESETS_INVALID_RULES_OBJECT = async () => ({
  title: 'Problems found in ruleset.',
  status: 400,
  detail: 'Ruleset cannot contain any invalid MO types, attributes or values.',
  ruleValidationErrors: {
    lineNumber: 2,
    errorType: 'Invalid MO.',
    errorDetails: 'MO not found in Managed Object Model.',
    additionalInfo: '',
  },
});

export const MOCK_RULESETS_NULL_VALUES = async () => ({
  title:
    'File cannot contain null values and column headers must match the Rule model',
  status: 400,
  detail: 'Fill in all required fields with the appropriate information',
});
