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
            description "should return status 201 and the created EaccRulesetMetadata"
            request {
                urlPath("/v1/rulesets")
                method POST()
                multipart([
                        rulesetName: $(consumer(regex(nonEmpty())), producer("sample_ruleset_one")),
                        fileName   : named(
                                name: value("SampleRulesetContract.csv"),
                                content: producer(file("./SampleRulesetContract.csv")),
                                contentType: value("text/csv")
                        )
                ])
                headers {
                    contentType multipartFormData()
                }
            }
            response {
                status CREATED()
                headers {
                    contentType applicationJson()
                }
                body([
                        "id"         : "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
                        "rulesetName": "sample_ruleset_one",
                        "uri"        : "v1/rulesets/3f2504e0-4f89-11d3-9a0c-0305e82c3301"
                ])
            }
        },
        Contract.make {
            description "should return status 200 and a list of EaccRulesetMetadata"
            request {
                urlPath("/v1/rulesets")
                method GET()
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body([[
                              "id"         : "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
                              "rulesetName": "sample_ruleset",
                              "uri"        : "v1/rulesets/3f2504e0-4f89-11d3-9a0c-0305e82c3301"
                      ]]
                )
            }
        },
        Contract.make {
            description "should return status 200 and a downloadable file"
            request {
                urlPath("/v1/rulesets/3f2504e0-4f89-11d3-9a0c-0305e82c3301")
                method GET()
            }
            response {
                status OK()
                headers {
                    contentType("text/csv")
                    header(contentDisposition(), regex('attachment; filename="3f2504e0-4f89-11d3-9a0c-0305e82c3301.csv"'))
                }
                body(regex(
                        """\
                        moType,attributeName,attributeValue
                        ENodeBFunction,prachConfigEnabled,true
                        """.stripIndent()))
            }
        },
        Contract.make {
            description "should return status 204"
            request {
                urlPath("/v1/rulesets/3f2504e0-4f89-11d3-9a0c-0305e82c3301")
                method DELETE()
            }
            response {
                status NO_CONTENT()
            }
        },
        Contract.make {
            description "should update a ruleset and return status 200 with updated EaccRulesetMetadata"

            request {
                method "PUT"
                urlPath "/v1/rulesets/3f2504e0-4f89-11d3-9a0c-0305e82c3301"
                headers {
                    contentType("multipart/form-data")
                }
                multipart(
                        [
                                fileName: named(
                                        name: value("UpdatedRulesetContract.csv"),
                                        content: producer(file("./SampleRulesetContract.csv")),
                                        contentType: value("text/csv")
                                )
                        ]
                )
            }
            response {
                status 200
                headers {
                    contentType applicationJson()
                }
                body(
                        [
                                "id"         : "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
                                "rulesetName": "sample_ruleset",
                                "uri"        : "v1/rulesets/3f2504e0-4f89-11d3-9a0c-0305e82c3301"
                        ]
                )
            }
        }
]
