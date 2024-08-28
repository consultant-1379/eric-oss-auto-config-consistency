/*******************************************************************************
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
 ******************************************************************************/

package contracts.positive.cases

import org.springframework.cloud.contract.spec.Contract

[
        Contract.make {
            description "should return status 200 and all jobs"
            request {
                urlPath("/v1/jobs")
                method GET()
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body([[
                              "jobName"    : "all-ireland-1",
                              "schedule"   : "0 15 11 ? * *",
                              "rulesetName": "test_ruleset",
                              "scopeName"  : "test_scope"
                      ],
                      [
                              "jobName"    : "all-ireland-2",
                              "schedule"   : "0 15 11 ? * *",
                              "rulesetName": "test_ruleset",
                              "scopeName"  : "test_scope"
                      ]])
            }
        },
        Contract.make {
            description "should return status 201 Created and the Job"
            request {
                method POST()
                urlPath("/v1/jobs")
                body([
                        "jobName"    : "all-ireland-4",
                        "schedule"   : "0 15 11 ? * *",
                        "rulesetName": "sample_ruleset",
                        "scopeName"  : "athlone"
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status CREATED()
                headers {
                    contentType applicationJson()
                }
                body([
                        "jobName"    : "all-ireland-4",
                        "schedule"   : "0 15 11 ? * *",
                        "rulesetName": "sample_ruleset",
                        "scopeName"  : "athlone"
                ])
            }
        },
        Contract.make {
            description "should return status 200 OK and the Job"
            request {
                method PUT()
                urlPath("/v1/jobs/all-ireland-1")
                body([
                        "jobName"    : "all-ireland-1",
                        "schedule"   : "0 15 11 ? * *",
                        "rulesetName": "sample_ruleset",
                        "scopeName"  : "athlone"
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body([
                        "jobName"    : "all-ireland-1",
                        "schedule"   : "0 15 11 ? * *",
                        "rulesetName": "sample_ruleset",
                        "scopeName"  : "athlone"
                ])
            }
        },
        Contract.make {
            description "Delete existing job should return status 204"
            request {
                urlPath("/v1/jobs/all-ireland-2")
                method DELETE()
            }
            response {
                status NO_CONTENT()
            }
        }
]