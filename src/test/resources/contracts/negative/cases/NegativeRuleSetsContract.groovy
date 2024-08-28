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
package contracts.negative.cases

import org.springframework.cloud.contract.spec.Contract

/*
* NOTE: FOLDER CONTAINING STUBS FOR NEGATIVE CONTRACT TESTS IS REMOVED FROM UI TEST IMAGE USED IN CI.
* SEE IDUN-78902
*/

[
        Contract.make {
            description "should return status 400 when fileName is missing"
            request {
                urlPath("/v1/rulesets")
                method POST()
                multipart([
                        rulesetName: $(consumer(regex(nonEmpty())), producer("test_ruleset"))
                ])
                headers {
                    contentType multipartFormData()
                }
            }
            response {
                status BAD_REQUEST()
            }
        },
        Contract.make {
            description "should return status 400 when ruleset contains invalid rules"
            request {
                urlPath("/v1/rulesets")
                method POST()
                multipart([
                        rulesetName: $(consumer(regex(nonEmpty())), producer("invalidmo_ruleset")),
                        fileName   : named(
                                name: value("InvalidMoRuleset.csv"),
                                content: producer(file("./InvalidMoRuleset.csv")),
                                contentType: value("text/csv")
                        )
                ])
                headers {
                    contentType multipartFormData()
                }
            }
            response {
                status BAD_REQUEST()
                body([
                        "title"               : "Problems found in ruleset.",
                        "status"              : 400,
                        "detail"              : "Ruleset cannot contain any invalid MO types, attributes or values.",
                        "ruleValidationErrors": [[
                                "lineNumber"    : 2,
                                "errorType"     : "Invalid MO.",
                                "errorDetails"  : "MO not found in Managed Object Model.",
                                "additionalInfo": ""
                        ]]
                ])
            }
        },
        Contract.make {
            description "should return status 400 when invalid rulesetName"
            request {
                urlPath("/v1/rulesets")
                method POST()
                multipart([
                        rulesetName: $(consumer(regex(nonEmpty())), producer("£ample_ruleset")),
                        fileName   : named(
                                name: value("FailingRulesetContract.csv"),
                                content: producer(file("./FailingRulesetContract.csv")),
                                contentType: value("text/csv")
                        )
                ])
                headers {
                    contentType multipartFormData()
                }
            }
            response {
                status BAD_REQUEST()
                body([
                        "title" : "Validation failed",
                        "status": 400,
                        "detail": "Invalid rulesetName: Use only lower case alphanumeric, underscores and dashes up to a maximum of 100"
                ])
            }
        },
        Contract.make {
            description "should return status 400 when Duplicate Ruleset"
            request {
                urlPath("/v1/rulesets")
                method POST()
                multipart([
                        rulesetName: $(consumer(regex(nonEmpty())), producer("duplicate_ruleset")),
                        fileName   : named(
                                name: value("FailingRulesetContract.csv"),
                                content: producer(file("./FailingRulesetContract.csv")),
                                contentType: value("text/csv")
                        )
                ])
                headers {
                    contentType multipartFormData()
                }
            }
            response {
                status BAD_REQUEST()
                body([
                        "title" : "Failed to create ruleset. Ruleset with provided name already exists.",
                        "status": 400,
                        "detail": "Use a unique Ruleset name."
                ])
            }
        },
        Contract.make {
            description "should return status 400 when Ruleset has null values"
            request {
                urlPath("/v1/rulesets")
                method POST()
                multipart([
                        rulesetName: $(consumer(regex(nonEmpty())), producer("null_value_ruleset")),
                        fileName   : named(
                                name: value("FailingRulesetContract.csv"),
                                content: producer(file("./FailingRulesetContract.csv")),
                                contentType: value("text/csv")
                        )
                ])
                headers {
                    contentType multipartFormData()
                }
            }
            response {
                status BAD_REQUEST()
                body([
                        "title" : "File cannot contain missing values and column headers must match the Rule model.",
                        "status": 400,
                        "detail": "Fill in all required fields with the appropriate information."
                ])
            }
        },
        Contract.make {
            description "should return status 500 when Ruleset is non-existent"
            request {
                urlPath("/v1/rulesets")
                method POST()
                multipart([
                        rulesetName: $(consumer(regex(nonEmpty())), producer("non_existent_ruleset")),
                        fileName   : named(
                                name: value("FailingRulesetContract.csv"),
                                content: absent(),
                                contentType: value("text/csv")
                        )
                ])
                headers {
                    contentType multipartFormData()
                }
            }
            response {
                status INTERNAL_SERVER_ERROR()
                body([
                        "title" : "Failed to create ruleset.",
                        "status": 500,
                        "detail": null
                ])
            }
        },
        Contract.make {
            description "should return status 500 when Get all rulesets exception"
            request {
                urlPath("/v1/rulesets")
                method GET()
                headers {
                    contentType multipartFormData()
                }
            }
            response {
                status INTERNAL_SERVER_ERROR()
                body([
                        "title" : "Failed to get all RuleSet metadata.",
                        "status": 500,
                        "detail": null
                ])
            }
        },
        Contract.make {
            description "should return status 500 when Get by Id IOException"
            request {
                urlPath("/v1/rulesets/99ea562f-87cc-424d-9835-f569b15b8eef")
                method GET()
            }
            response {
                status INTERNAL_SERVER_ERROR()
                body([
                        "title" : "Error parsing ID. Provided ID is invalid.",
                        "status": 500,
                        "detail": "Failed to read InputStream"
                ])
            }
        },
        Contract.make {
            description "should return status 400 when Get By Id IllegalArgumentException"
            request {
                urlPath("/v1/rulesets/IllegalArgument")
                method GET()
            }
            response {
                status BAD_REQUEST()
                body([
                        "title" : "Error parsing ID. Provided ID is invalid.",
                        "status": 400,
                        "detail": "Provide a valid Ruleset ID."
                ])
            }
        },
        Contract.make {
            description "should return status 404 when Get By Id ruleset doesn't exist"
            request {
                urlPath("/v1/rulesets/4b40f3b8-0c51-4d7e-90e6-00c366238f05")
                method GET()
            }
            response {
                status NOT_FOUND()
                body([
                        "title" : "Failed to find ruleset. Ruleset doesn't exist.",
                        "status": 404,
                        "detail": "Enter a valid Ruleset ID."
                ])
            }
        },
        Contract.make {
            description "should return status 400 when ruleset contains invalid rules"
            request {
                urlPath("/v1/rulesets/3f2504e0-4f89-11d3-9a0c-0305e82c3301")
                method PUT()
                multipart(
                        [
                                fileName: named(
                                        name: value("InvalidMoRuleset.csv"),
                                        content: producer(file("./InvalidMoRuleset.csv")),
                                        contentType: value("text/csv")
                                )
                        ]
                )
            }
            response {
                status BAD_REQUEST()
                body([
                        "title"               : "Problems found in ruleset.",
                        "status"              : 400,
                        "detail"              : "Ruleset cannot contain any invalid MO types, attributes or values.",
                        "ruleValidationErrors": [
                                "lineNumber"    : 2,
                                "errorType"     : "Invalid MO.",
                                "errorDetails"  : "MO not found in Managed Object Model.",
                                "additionalInfo": ""
                        ]
                ])
            }
        },
        Contract.make {
            description "should return status 404 with error description when rulset does not exist"
            request {
                urlPath("/v1/rulesets/4b40f3b8-0c51-4d7e-90e6-00c366238f04")
                method DELETE()
            }
            response {
                status NOT_FOUND()
                body([
                        "title" : "Ruleset with provided ID does not exist.",
                        "status": 404,
                        "detail": "Provide a valid Ruleset ID."
                ])
            }
        },
        Contract.make {
            description "should return status 409 with error description when ruleset is in use by a job configuration"
            request {
                urlPath("/v1/rulesets/8f80f692-0c51-4d7e-5d84-00a644238187")
                method DELETE()
            }
            response {
                status CONFLICT()
                body([
                        "title" : "Ruleset with provided ID is used in a Job configuration.",
                        "status": 409,
                        "detail": "Only Rulesets not associated with a Job configuration can be deleted."
                ])
            }
        },
        Contract.make {
            description "should return status 400 when Get By Id has an invalid id"
            request {
                urlPath("/v1/rulesets/Invalid-Id")
                method DELETE()
            }
            response {
                status BAD_REQUEST()
                body([
                        "title" : "Error parsing ID. Provided ID is invalid.",
                        "status": 400,
                        "detail": "Provide a valid Ruleset ID."
                ])
            }
        },
        Contract.make {
            description "should return status 400 when PUT Id CsvRequiredFieldEmptyException"
            request {
                urlPath("/v1/rulesets/99ea562f-87cc-424d-9835-f569b15b8eef")
                method PUT()
                multipart(
                        [
                                fileName: named(
                                        name: value("FailingRulesetContract.csv"),
                                        content: producer(file("./FailingRulesetContract.csv")),
                                        contentType: value("text/csv")
                                )
                        ]
                )
            }
            response {
                status BAD_REQUEST()
                body([
                        "title" : "File cannot contain null values and column headers must match the Rule model",
                        "status": 400,
                        "detail": "Fill in all required fields with the appropriate information"
                ])
            }
        },
        Contract.make {
            description "should return status 500 when PUT Id IOException"
            request {
                urlPath("/v1/rulesets/270994ce-249a-4a9b-a87d-2f1c4df0bf94")
                method PUT()
                multipart(
                        [
                                fileName: named(
                                        name: value("FailingRulesetContract.csv"),
                                        content: producer(file("./FailingRulesetContract.csv")),
                                        contentType: value("text/csv")
                                )
                        ]
                )
            }
            response {
                status INTERNAL_SERVER_ERROR()
                body([
                        "title" : "Failed to update ruleset.",
                        "status": 500,
                        "detail": "Failed to read InputStream"
                ])
            }
        },
        Contract.make {
            description "should return status 400 when Get By Id IllegalArgumentException"
            request {
                urlPath("/v1/rulesets/Invalid-Id")
                method PUT()
                multipart(
                        [
                                fileName: named(
                                        name: value("FailingRulesetContract.csv"),
                                        content: producer(file("./FailingRulesetContract.csv")),
                                        contentType: value("text/csv")
                                )
                        ]
                )
            }
            response {
                status BAD_REQUEST()
                body([
                        "title" : "Error parsing ID. Provided ID is invalid.",
                        "status": 400,
                        "detail": "Provide a valid Ruleset ID."
                ])
            }
        },
        Contract.make {
            description "should return status 404 when PUT Id ruleset doesn't exist"
            request {
                urlPath("/v1/rulesets/4b40f3b8-0c51-4d7e-90e6-00c366238f05")
                method PUT()
                multipart(
                        [
                                fileName: named(
                                        name: value("FailingRulesetContract.csv"),
                                        content: producer(file("./FailingRulesetContract.csv")),
                                        contentType: value("text/csv")
                                )
                        ]
                )
            }
            response {
                status NOT_FOUND()
                body([
                        "title" : "Failed to find ruleset. Ruleset doesn't exist.",
                        "status": 404,
                        "detail": "Enter a valid Ruleset ID."
                ])
            }
        }
]