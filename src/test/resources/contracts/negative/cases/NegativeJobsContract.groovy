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

/*
* NOTE: FOLDER CONTAINING STUBS FOR NEGATIVE CONTRACT TESTS IS REMOVED FROM UI TEST IMAGE USED IN CI.
* SEE IDUN-78902
*/

[
        Contract.make {
            description "should return status 400 Bad Request and the error message"
            request {
                method PUT()
                urlPath("/v1/jobs/INVALID_JOB_NAME")
                body([
                        "jobName"    : "INVALID_JOB_NAME",
                        "schedule"   : "0 15 11 ? * *",
                        "rulesetName": "test_ruleset",
                        "scopeName"  : "test_scope"
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status BAD_REQUEST()
                body([
                        "title" : "Validation failed",
                        "status": 400,
                        "detail": "Invalid resource identifier in url: Only lower case alphanumeric, underscores and dashes allowed, with between 4 -100 characters."
                ])
            }
        },
        Contract.make {
            description "should return status 500 Internal Error and the error message"
            request {
                method PUT()
                urlPath("/v1/jobs/internal_error_job_name")
                body([
                        "jobName"    : "internal_error_job_name",
                        "schedule"   : "0 15 11 ? * *",
                        "rulesetName": "test_ruleset",
                        "scopeName"  : "test_scope"
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status INTERNAL_SERVER_ERROR()
                body([
                        "title" : "Failed to update job.",
                        "status": 500,
                        "detail": "Database operation failed."
                ])
            }
        },
        Contract.make {
            description "should return status 404 Not Found and the error message"
            request {
                method PUT()
                urlPath("/v1/jobs/non_existing_job_name")
                body([
                        "jobName"    : "non_existing_job_name",
                        "schedule"   : "0 15 11 ? * *",
                        "rulesetName": "test_ruleset",
                        "scopeName"  : "test_scope"
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status NOT_FOUND()
                body([
                        "title" : "Validation failed",
                        "status": 404,
                        "detail": "Invalid jobName: jobName does not exist"
                ])
            }
        },
        Contract.make {
            description "should return status 409 Conflict and the error message"
            request {
                method PUT()
                urlPath("/v1/jobs/execution_in_progress_with_job_name")
                body([
                        "jobName"    : "execution_in_progress_with_job_name",
                        "schedule"   : "0 15 11 ? * *",
                        "rulesetName": "test_ruleset",
                        "scopeName"  : "test_scope"
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status CONFLICT()
                body([
                        "title" : "Cannot update job as it is used by an ongoing execution.",
                        "status": 409,
                        "detail": "Job in use."
                ])
            }
        }
]