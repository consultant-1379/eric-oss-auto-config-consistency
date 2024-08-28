/*******************************************************************************
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
 ******************************************************************************/

package contracts.positive.cases

import org.springframework.cloud.contract.spec.Contract

[
        Contract.make {
            description "should return status 200 and all executions"
            request {
                urlPath("/v1/executions")
                method GET()
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body([[
                              "id"                       : "1",
                              "jobName"                  : "all-ireland-1",
                              "executionStatus"          : "Audit Successful",
                              "executionType"            : "Open Loop",
                              "executionStartedAt"       : "2023-03-14T15:18:44.033Z",
                              "executionEndedAt"         : "2023-03-14T15:23:44.033Z",
                              "consistencyAuditStartedAt": "2023-03-14T15:18:44.033Z",
                              "consistencyAuditEndedAt"  : "2023-03-14T15:23:44.033Z",
                              "totalMosAudited"          : "1",
                              "totalAttributesAudited"   : "12",
                              "inconsistenciesIdentified": "4"
                      ],
                      [
                              "id"                       : "2",
                              "jobName"                  : "all-ireland-2",
                              "executionStatus"          : "Audit Successful",
                              "executionType"            : "Open Loop",
                              "executionStartedAt"       : "2023-03-15T15:18:44.033Z",
                              "executionEndedAt"         : "2023-03-15T15:23:44.033Z",
                              "consistencyAuditStartedAt": "2023-03-15T15:18:44.033Z",
                              "consistencyAuditEndedAt"  : "2023-03-15T15:23:44.033Z",
                              "totalMosAudited"          : "10",
                              "totalAttributesAudited"   : "1",
                              "inconsistenciesIdentified": "8"
                      ],
                      [
                              "id"                       : "3",
                              "jobName"                  : "all-ireland-3",
                              "executionStatus"          : "Audit Failed",
                              "executionType"            : "Open Loop",
                              "executionStartedAt"       : "2023-03-16T15:18:44.033Z",
                              "executionEndedAt"         : "2023-03-16T15:23:44.033Z",
                              "consistencyAuditStartedAt": "2023-03-16T15:18:44.033Z",
                              "consistencyAuditEndedAt"  : "2023-03-16T15:23:44.033Z",
                              "totalMosAudited"          : "40",
                              "totalAttributesAudited"   : "20",
                              "inconsistenciesIdentified": "66"
                      ],
                      [
                              "id"                       : "4",
                              "jobName"                  : "all-ireland-4",
                              "executionStatus"          : "Audit in Progress",
                              "executionType"            : "Open Loop",
                              "executionStartedAt"       : "2023-03-17T15:18:44.033Z",
                              "executionEndedAt"         : "2023-03-17T15:23:44.033Z",
                              "consistencyAuditStartedAt": "2023-03-17T15:18:44.033Z",
                              "consistencyAuditEndedAt"  : "2023-03-17T15:23:44.033Z",
                              "totalMosAudited"          : "1",
                              "totalAttributesAudited"   : "1",
                              "inconsistenciesIdentified": "4"
                      ]]
                )
            }
        },
        Contract.make {
            description "should return status 200 and the specified Job"
            request {
                urlPath("/v1/executions?jobName=all-ireland-1")
                method GET()
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body([[
                              "id"                       : "1",
                              "jobName"                  : "all-ireland-1",
                              "executionStatus"          : "Audit Successful",
                              "executionType"            : "Open Loop",
                              "executionStartedAt"       : "2023-03-14T15:18:44.033Z",
                              "executionEndedAt"         : "2023-03-14T15:23:44.033Z",
                              "consistencyAuditStartedAt": "2023-03-14T15:18:44.033Z",
                              "consistencyAuditEndedAt"  : "2023-03-14T15:23:44.033Z",
                              "totalMosAudited"          : "1",
                              "totalAttributesAudited"   : "12",
                              "inconsistenciesIdentified": "4"
                      ]])
            }
        },
        Contract.make {
            description "should return status 200 and page 0 audit results with page size 2000 and auditStatus " +
                    "Inconsistent"
            priority 1
            request {
                urlPath("/v1/executions/1/audit-results") {
                    queryParameters {
                        parameter 'page': 0
                        parameter 'pageSize': 2000
                        /*This is hardcoded to 2000 for now in the client so faking it here for now.*/
                        parameter 'filter': "auditStatus:Inconsistent"
                    }
                }
                method GET()
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body([
                        "totalElements": "4",
                        "totalPages"   : "1",
                        "currentPage"  : "0",
                        "perPage"      : "2000",
                        "hasNext"      : false,
                        "hasPrev"      : false,
                        "results"      : [
                                [
                                        "id"               : "2",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4",
                                        "managedObjectType": "NRCellDU",
                                        "attributeName"    : "subCarrierSpacing",
                                        "currentValue"     : "120",
                                        "preferredValue"   : "110",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "17",
                                        "changeStatus"     : "Implementation in progress"
                                ],
                                [
                                        "id"               : "3",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00030-4",
                                        "managedObjectType": "NRCellCU",
                                        "attributeName"    : "mcpcPSCellEnabled",
                                        "currentValue"     : "false",
                                        "preferredValue"   : "true",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "14",
                                        "changeStatus"     : "Implementation failed"
                                ],
                                [
                                        "id"               : "4",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1",
                                        "managedObjectType": "GNBCUCPFunction",
                                        "attributeName"    : "maxNgRetryTime",
                                        "currentValue"     : "30",
                                        "preferredValue"   : "20",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "12",
                                        "changeStatus"     : "Implementation complete"
                                ],
                                [
                                        "id"               : "5",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1",
                                        "managedObjectType": "GNBCUCPFunction",
                                        "attributeName"    : "endpointResDepHEnabled",
                                        "currentValue"     : "true",
                                        "preferredValue"   : "false",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "18",
                                        "changeStatus"     : "Implementation complete"
                                ]
                        ]
                ])
            }
        },
        Contract.make {
            description "should return status 200 and all audit results"
            priority 3
            request {
                urlPath("/v1/executions/1/audit-results")
                method GET()
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body([
                        "totalElements": "5",
                        "totalPages"   : "1",
                        "currentPage"  : null,
                        "perPage"      : null,
                        "hasNext"      : false,
                        "hasPrev"      : false,
                        "results"      : [
                                [
                                        "id"               : "1",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4",
                                        "managedObjectType": "NRCellDU",
                                        "attributeName"    : "csiRsShiftingPrimary",
                                        "currentValue"     : "DEACTIVATED",
                                        "preferredValue"   : "DEACTIVATED",
                                        "auditStatus"      : "Consistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "16",
                                        "changeStatus"     : "Not applied"
                                ],
                                [
                                        "id"               : "2",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4",
                                        "managedObjectType": "NRCellDU",
                                        "attributeName"    : "subCarrierSpacing",
                                        "currentValue"     : "120",
                                        "preferredValue"   : "110",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "17",
                                        "changeStatus"     : "Implementation in progress"
                                ],
                                [
                                        "id"               : "3",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00030-4",
                                        "managedObjectType": "NRCellCU",
                                        "attributeName"    : "mcpcPSCellEnabled",
                                        "currentValue"     : "false",
                                        "preferredValue"   : "true",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "14",
                                        "changeStatus"     : "Implementation failed"
                                ],
                                [
                                        "id"               : "4",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1",
                                        "managedObjectType": "GNBCUCPFunction",
                                        "attributeName"    : "maxNgRetryTime",
                                        "currentValue"     : "30",
                                        "preferredValue"   : "20",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "12",
                                        "changeStatus"     : "Implementation complete"
                                ],
                                [
                                        "id"               : "5",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1",
                                        "managedObjectType": "GNBCUCPFunction",
                                        "attributeName"    : "endpointResDepHEnabled",
                                        "currentValue"     : "true",
                                        "preferredValue"   : "false",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "18",
                                        "changeStatus"     : "Implementation complete"
                                ]
                        ]
                ])
            }
        },
        Contract.make {
            description "should return status 200 and page 0 audit results with page size 2000"
            priority 2
            request {
                urlPath("/v1/executions/1/audit-results") {
                    queryParameters {
                        parameter 'page': 0
                        parameter 'pageSize': 2000
                        /*This is hardcoded to 2000 for now in the client so faking it here for now.*/
                    }
                }
                method GET()
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body([
                        "totalElements": "5",
                        "totalPages"   : "2",
                        "currentPage"  : "0",
                        "perPage"      : "2000",
                        "hasNext"      : true,
                        "hasPrev"      : false,
                        "results"      : [
                                [
                                        "id"               : "1",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4",
                                        "managedObjectType": "NRCellDU",
                                        "attributeName"    : "csiRsShiftingPrimary",
                                        "currentValue"     : "DEACTIVATED",
                                        "preferredValue"   : "DEACTIVATED",
                                        "auditStatus"      : "Consistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "16",
                                        "changeStatus"     : "Not applied"
                                ],
                                [
                                        "id"               : "2",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4",
                                        "managedObjectType": "NRCellDU",
                                        "attributeName"    : "subCarrierSpacing",
                                        "currentValue"     : "120",
                                        "preferredValue"   : "110",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "17",
                                        "changeStatus"     : "Implementation in progress"
                                ],
                                [
                                        "id"               : "3",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00030-4",
                                        "managedObjectType": "NRCellCU",
                                        "attributeName"    : "mcpcPSCellEnabled",
                                        "currentValue"     : "false",
                                        "preferredValue"   : "true",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "14",
                                        "changeStatus"     : "Implementation failed"
                                ],
                                [
                                        "id"               : "4",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1",
                                        "managedObjectType": "GNBCUCPFunction",
                                        "attributeName"    : "maxNgRetryTime",
                                        "currentValue"     : "30",
                                        "preferredValue"   : "20",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "12",
                                        "changeStatus"     : "Implementation complete"
                                ]
                        ]
                ])
            }
        },

        Contract.make {
            description "should return status 200 and page 1 audit results with page size 2000"
            priority 2
            request {
                urlPath("/v1/executions/1/audit-results") {
                    queryParameters {
                        parameter 'page': 1
                        parameter 'pageSize': 2000
                        /*This is hardcoded to 2000 for now in the client so faking it here for now.*/
                    }
                }
                method GET()
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body([
                        "totalElements": "5",
                        "totalPages"   : "2",
                        "currentPage"  : "1",
                        "perPage"      : "2000",
                        "hasNext"      : false,
                        "hasPrev"      : true,
                        "results"      : [
                                [
                                        "id"               : "5",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1",
                                        "managedObjectType": "GNBCUCPFunction",
                                        "attributeName"    : "endpointResDepHEnabled",
                                        "currentValue"     : "true",
                                        "preferredValue"   : "false",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "18",
                                        "changeStatus"     : "Implementation complete"
                                ]
                        ]
                ])
            }
        },
        Contract.make {
            description "should return status 200 and page 0 audit results with page size 2 and changeStatus " +
                    "of Implementation in progress,Implementation complete,Implementation failed,Reversion in " +
                    "progress,Reversion complete or Reversion failed"
            priority 1
            request {
                urlPath("/v1/executions/1/audit-results") {
                    queryParameters {
                        parameter 'page': 0
                        parameter 'pageSize': 2000
                        /*This is hardcoded to 2000 for now in the client so faking it here for now.*/
                        parameter 'filter': "changeStatus:(Implementation in progress,Implementation complete,Implementation failed,Implementation aborted,Reversion in progress,Reversion complete,Reversion failed,Reversion aborted)"
                    }
                }
                method GET()
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body([
                        "totalElements": "4",
                        "totalPages"   : "2",
                        "currentPage"  : "0",
                        "perPage"      : "2000",
                        "hasNext"      : true,
                        "hasPrev"      : false,
                        "results"      : [
                                [
                                        "id"               : "2",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4",
                                        "managedObjectType": "NRCellDU",
                                        "attributeName"    : "subCarrierSpacing",
                                        "currentValue"     : "120",
                                        "preferredValue"   : "110",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "17",
                                        "changeStatus"     : "Implementation in progress"
                                ],
                                [
                                        "id"               : "3",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00030-4",
                                        "managedObjectType": "NRCellCU",
                                        "attributeName"    : "mcpcPSCellEnabled",
                                        "currentValue"     : "false",
                                        "preferredValue"   : "true",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "14",
                                        "changeStatus"     : "Implementation failed"
                                ],
                                [
                                        "id"               : "4",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1",
                                        "managedObjectType": "GNBCUCPFunction",
                                        "attributeName"    : "maxNgRetryTime",
                                        "currentValue"     : "30",
                                        "preferredValue"   : "20",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "12",
                                        "changeStatus"     : "Implementation complete"
                                ]
                        ]
                ])
            }
        },
        Contract.make {
            description "should return status 200 and page 0 audit results with page size 2 and changeStatus " +
                    "of Implementation in progress,Implementation complete,Implementation failed,Reversion in " +
                    "progress,Reversion complete or Reversion failed"
            priority 1
            request {
                urlPath("/v1/executions/1/audit-results") {
                    queryParameters {
                        parameter 'page': 1
                        parameter 'pageSize': 2000
                        /*This is hardcoded to 2000 for now in the client so faking it here for now.*/
                        parameter 'filter': "changeStatus:(Implementation in progress,Implementation complete,Implementation failed,Implementation aborted,Reversion in progress,Reversion complete,Reversion failed,Reversion aborted)"
                    }
                }
                method GET()
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body([
                        "totalElements": "4",
                        "totalPages"   : "2",
                        "currentPage"  : "1",
                        "perPage"      : "2000",
                        "hasNext"      : false,
                        "hasPrev"      : true,
                        "results"      : [
                                [
                                        "id"               : "5",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1",
                                        "managedObjectType": "GNBCUCPFunction",
                                        "attributeName"    : "endpointResDepHEnabled",
                                        "currentValue"     : "true",
                                        "preferredValue"   : "false",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "18",
                                        "changeStatus"     : "Implementation complete"
                                ]
                        ]
                ])
            }
        },
        Contract.make {
            description "should return status 200 and no elements when changeStatus " +
                    "of Implementation in progress or Reversion in progress"
            priority 2
            request {
                urlPath("/v1/executions/1/audit-results") {
                    queryParameters {
                        parameter 'page': 0
                        parameter 'pageSize': 1
                        parameter 'filter': "changeStatus:(Implementation in progress,Reversion in progress)"
                    }
                }
                method GET()
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body([
                        "totalElements": "0",
                        "totalPages"   : "0",
                        "currentPage"  : "0",
                        "perPage"      : "1",
                        "hasNext"      : false,
                        "hasPrev"      : false,
                        "results"      : []
                ])
            }
        },
        Contract.make {
            description "should return status 200 and audit results for given managed object"
            request {
                urlPath("/v1/executions/1/audit-results") {
                    queryParameters {
                        parameter 'page': 0
                        parameter 'pageSize': 2000
                        parameter 'filter': "managedObjectFdn:SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4"
                    }
                }
                method GET()
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body([
                        "totalElements": "1",
                        "totalPages"   : "1",
                        "currentPage"  : "0",
                        "perPage"      : "2000",
                        "hasNext"      : false,
                        "hasPrev"      : false,
                        "results"      : [
                                [
                                        "id"               : "2",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4",
                                        "managedObjectType": "NRCellDU",
                                        "attributeName"    : "subCarrierSpacing",
                                        "currentValue"     : "120",
                                        "preferredValue"   : "110",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "17",
                                        "changeStatus"     : "Implementation in progress"
                                ]
                        ]
                ])
            }
        },
        Contract.make {
            description "should return status 200 and audit results for a partial managed object"
            request {
                urlPath("/v1/executions/1/audit-results") {
                    queryParameters {
                        parameter 'page': 0
                        parameter 'pageSize': 2000
                        parameter 'filter': "managedObjectFdn:%NRCellDU=NR03gNodeBRadio00030-4%"
                    }
                }
                method GET()
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body([
                        "totalElements": "2",
                        "totalPages"   : "1",
                        "currentPage"  : "0",
                        "perPage"      : "2000",
                        "hasNext"      : false,
                        "hasPrev"      : false,
                        "results"      : [
                                [
                                        "id"               : "1",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4",
                                        "managedObjectType": "NRCellDU",
                                        "attributeName"    : "csiRsShiftingPrimary",
                                        "currentValue"     : "DEACTIVATED",
                                        "preferredValue"   : "DEACTIVATED",
                                        "auditStatus"      : "Consistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "16",
                                        "changeStatus"     : "Not applied"
                                ],
                                [
                                        "id"               : "2",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4",
                                        "managedObjectType": "NRCellDU",
                                        "attributeName"    : "subCarrierSpacing",
                                        "currentValue"     : "120",
                                        "preferredValue"   : "110",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "17",
                                        "changeStatus"     : "Implementation in progress"
                                ]
                        ]
                ])
            }
        },
        Contract.make {
            description "should return status 200 and audit results for a left like match on string"
            request {
                urlPath("/v1/executions/1/audit-results") {
                    queryParameters {
                        parameter 'page': 0
                        parameter 'pageSize': 2000
                        parameter 'filter': "managedObjectFdn:%NRCellCU=NR03gNodeBRadio00030-4"
                    }
                }
                method GET()
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body([
                        "totalElements": "1",
                        "totalPages"   : "1",
                        "currentPage"  : "0",
                        "perPage"      : "2000",
                        "hasNext"      : false,
                        "hasPrev"      : false,
                        "results"      : [
                                [
                                        "id"               : "3",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00030-4",
                                        "managedObjectType": "NRCellCU",
                                        "attributeName"    : "mcpcPSCellEnabled",
                                        "currentValue"     : "false",
                                        "preferredValue"   : "true",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "14",
                                        "changeStatus"     : "Implementation failed"
                                ]
                        ]
                ])
            }
        },
        Contract.make {
            description "should return status 200 and audit results for a right like match on string"
            request {
                urlPath("/v1/executions/1/audit-results") {
                    queryParameters {
                        parameter 'page': 0
                        parameter 'pageSize': 2000
                        parameter 'filter': "managedObjectFdn:SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,%"
                    }
                }
                method GET()
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body([
                        "totalElements": "3",
                        "totalPages"   : "1",
                        "currentPage"  : "0",
                        "perPage"      : "2000",
                        "hasNext"      : false,
                        "hasPrev"      : false,
                        "results"      : [
                                [
                                        "id"               : "1",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4",
                                        "managedObjectType": "NRCellDU",
                                        "attributeName"    : "csiRsShiftingPrimary",
                                        "currentValue"     : "DEACTIVATED",
                                        "preferredValue"   : "DEACTIVATED",
                                        "auditStatus"      : "Consistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "16",
                                        "changeStatus"     : "Not applied"
                                ],
                                [
                                        "id"               : "2",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4",
                                        "managedObjectType": "NRCellDU",
                                        "attributeName"    : "subCarrierSpacing",
                                        "currentValue"     : "120",
                                        "preferredValue"   : "110",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "17",
                                        "changeStatus"     : "Implementation in progress"
                                ],
                                [
                                        "id"               : "3",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00030-4",
                                        "managedObjectType": "NRCellCU",
                                        "attributeName"    : "mcpcPSCellEnabled",
                                        "currentValue"     : "false",
                                        "preferredValue"   : "true",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "14",
                                        "changeStatus"     : "Implementation failed"
                                ]
                        ]
                ])
            }
        },
        Contract.make {
            description "should return status 200 and audit results for given auditStatus and given managed object"
            request {
                urlPath("/v1/executions/1/audit-results") {
                    queryParameters {
                        parameter 'page': 0
                        parameter 'pageSize': 2000
                        parameter 'filter': "auditStatus:Inconsistent\$managedObjectFdn:SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4"
                    }
                }
                method GET()
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body([
                        "totalElements": "1",
                        "totalPages"   : "1",
                        "currentPage"  : "0",
                        "perPage"      : "2000",
                        "hasNext"      : false,
                        "hasPrev"      : false,
                        "results"      : [
                                [
                                        "id"               : "2",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4",
                                        "managedObjectType": "NRCellDU",
                                        "attributeName"    : "subCarrierSpacing",
                                        "currentValue"     : "120",
                                        "preferredValue"   : "110",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "17",
                                        "changeStatus"     : "Implementation in progress"
                                ]
                        ]
                ])
            }
        },
        Contract.make {
            description "should return status 200 and Inconsistent audit results for a partial managed object"
            priority 1
            request {
                urlPath("/v1/executions/1/audit-results") {
                    queryParameters {
                        parameter 'page': 0
                        parameter 'pageSize': 2000
                        parameter 'filter': "auditStatus:Inconsistent\$managedObjectFdn:%NR03gNodeBRadio00030-4%"
                    }
                }
                method GET()
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body([
                        "totalElements": "2",
                        "totalPages"   : "1",
                        "currentPage"  : "0",
                        "perPage"      : "2000",
                        "hasNext"      : false,
                        "hasPrev"      : false,
                        "results"      : [
                                [
                                        "id"               : "2",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4",
                                        "managedObjectType": "NRCellDU",
                                        "attributeName"    : "subCarrierSpacing",
                                        "currentValue"     : "120",
                                        "preferredValue"   : "110",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "17",
                                        "changeStatus"     : "Implementation in progress"
                                ],
                                [
                                        "id"               : "3",
                                        "managedObjectFdn" : "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00030-4",
                                        "managedObjectType": "NRCellCU",
                                        "attributeName"    : "mcpcPSCellEnabled",
                                        "currentValue"     : "false",
                                        "preferredValue"   : "true",
                                        "auditStatus"      : "Inconsistent",
                                        "executionId"      : "1",
                                        "ruleId"           : "14",
                                        "changeStatus"     : "Implementation failed"
                                ]
                        ]
                ])
            }
        },
        Contract.make {
            description "When apply all should return status 202"
            request {
                urlPath("/v1/executions/1/audit-results")
                method POST()
                headers {
                    contentType applicationJson()
                }
                body([
                        auditResultIds: [],
                        approveForAll : true,
                        operation     : "APPLY_CHANGE"
                ])
            }
            response {
                status ACCEPTED()
            }
        },
        Contract.make {
            description "When applying all changes, should return status 202"
            request {
                urlPath("/v1/executions/1/audit-results")
                method POST()
                headers {
                    contentType applicationJson()
                }
                body([
                        auditResultIds: [],
                        approveForAll : true,
                        operation     : "APPLY_CHANGE"
                ])
            }
            response {
                status ACCEPTED()
            }
        },
        Contract.make {
            description "When apply selected should return status 202"
            request {
                urlPath("/v1/executions/1/audit-results")
                method POST()
                headers {
                    contentType applicationJson()
                }
                body([
                        auditResultIds: ["2", "3", "4"],
                        approveForAll : false,
                        operation     : "APPLY_CHANGE"
                ])
            }
            response {
                status ACCEPTED()
            }
        },
        Contract.make {
            description "When revert selection of changes, should return status 202"
            request {
                urlPath("/v1/executions/1/audit-results")
                method POST()
                headers {
                    contentType applicationJson()
                }
                body([
                        auditResultIds: ["4"],
                        approveForAll : false,
                        operation     : "REVERT_CHANGE"
                ])
            }
            response {
                status ACCEPTED()
            }
        },
        Contract.make {
            description "When revert changes, should return status 202"
            request {
                urlPath("/v1/executions/1/audit-results")
                method POST()
                headers {
                    contentType applicationJson()
                }
                body([
                        auditResultIds: ["2", "3", "4"],
                        approveForAll : false,
                        operation     : "REVERT_CHANGE"
                ])
            }
            response {
                status ACCEPTED()
            }
        },
        Contract.make {
            description "When revert all changes, should return status 202"
            request {
                urlPath("/v1/executions/1/audit-results")
                method POST()
                headers {
                    contentType applicationJson()
                }
                body([
                        auditResultIds: [],
                        approveForAll : true,
                        operation     : "REVERT_CHANGE"
                ])
            }
            response {
                status ACCEPTED()
            }
        }
]