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

export const EACC_PREFIX = __ENV.EACC_PREFIX !== undefined ? __ENV.EACC_PREFIX : '/eacc'
export const EXECUTION_URI = `${EACC_PREFIX}/v1/executions`;
export const JOB_URI = `${EACC_PREFIX}/v1/jobs`;
export const RULESET_URI = `${EACC_PREFIX}/v1/rulesets`;
export const SCOPE_URI = `${EACC_PREFIX}/v1/scopes`;
export const CM_HANDLE_4G = '4ABC774344A175D8C61A68B16AA3A8BA'; // SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE63dg2ERBST00068
export const MANAGED_ELEMENT_4G_ID = 'LTE63dg2ERBST00068';
export const FDD_CELL_ID = 'LTE63dg2ERBST00068-12';
export const CM_HANDLE_5G = 'F30B8F23ABD45358D83D59519BF887C3'; // SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR127gNodeBRadioT00005,ManagedElement=NR127gNodeBRadioT00005
export const SEARCH_ENGINE_ENTRIES_URI= '/_search?pretty&size=1';

export const FILTER_PAYLOAD = {
      "query": {
        "bool": {
          "filter": [
          {
          "match_phrase": {
               "service_id" : "rapp-eric-oss-auto-config-consistency"
              }
          },
          {
            "range": {
                "timestamp": {
                    "gt": "now-5m"
                }
            }
          }
          ]
        }
      }
};

export const AUDIT_LOG_FILTER_PAYLOAD = {
    "query": {
        "bool": {
            "filter": [
                {
                    "query_string": {
                        "query":"(facility:\"log audit\")"
                    }
                },
                {
                    "match_phrase": {
                        "service_id" : "rapp-eric-oss-auto-config-consistency"
                    }
                },
                {
                    "range": {
                        "timestamp": {
                            "gt": "now-5m"
                        }
                    }
                }
            ]
        }
    }
};

export const APP_JSON_PARAMS = {
    headers: {
        'Content-Type': 'application/json'
    }
};

export const APPLY_CHANGES_TIMEOUT = {
  timeout: 480,
};

export const APPLY_CHANGE = "APPLY_CHANGE";
export const REVERT_CHANGE = "REVERT_CHANGE";
export const ChangeStatus = {
  IMPLEMENTATION_IN_PROGRESS: 'Implementation in progress',
  IMPLEMENTATION_COMPLETE: 'Implementation complete',
  IMPLEMENTATION_FAILED: 'Implementation failed',
  REVERSION_IN_PROGRESS: 'Reversion in progress',
  REVERSION_COMPLETE: 'Reversion complete',
  REVERSION_FAILED: 'Reversion failed',
};
export const AuditStatus = {
  CONSISTENT: "Consistent",
  INCONSISTENT: "Inconsistent",
};
export const ExecutionStatus = {
  REVERSION_FAILED: "Reversion Failed",
  REVERSION_SUCCESSFUL: "Reversion Successful",
  REVERSION_PARTIALLY_SUCCESSFUL: "Reversion Partially Successful",
}

export const getChangeStatusFilter = (status) => encodeURI(`changeStatus:(${status})`)
export const getAuditStatusFilter = (status) => encodeURI(`auditStatus:(${status})`)