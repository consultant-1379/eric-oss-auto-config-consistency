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
            description "should return status 200 and a list of EaccScopeMetadata"
            request {
                urlPath("/v1/scopes")
                method GET()
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body([[
                              "id"       : "13aa6e40-a9e2-4fa2-9468-16c4797a5ca0",
                              "scopeName": "athlone",
                              "uri"      : "v1/scopes/13aa6e40-a9e2-4fa2-9468-16c4797a5ca0"
                      ],
                      [
                              "id"       : "13aa6e40-a9e2-4fa2-9468-16c4797a5ca1",
                              "scopeName": "dublin",
                              "uri"      : "v1/scopes/13aa6e40-a9e2-4fa2-9468-16c4797a5ca1"
                      ]
                ])
            }
        },
        Contract.make {
            description "should return status 201 and a EaccScopeMetadata for the created scope"
            request {
                urlPath("/v1/scopes")
                method POST()
                multipart([
                        scopeName: $(consumer(regex(nonEmpty())), producer("westmeath_10-07-2023")),
                        fileName : named(
                                name: value("PositiveScopeContract.csv"),
                                content: producer(file("./PositiveScopeContract.csv")),
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
                        "id"       : "13aa6e40-a9e2-4fa2-9468-16c4797a5ca0",
                        "scopeName": "westmeath_10-07-2023",
                        "uri"      : "v1/scopes/13aa6e40-a9e2-4fa2-9468-16c4797a5ca0"
                ])
            }
        },
        Contract.make {
            description "should return status 204"
            request {
                urlPath("/v1/scopes/13aa6e40-a9e2-4fa2-9468-16c4797a5ca0")
                method DELETE()
            }
            response {
                status NO_CONTENT()
            }
        },
        Contract.make {
            description "should update a scope and return status 200 with updated EaccScopeMetadata"

            request {
                method "PUT"
                urlPath("/v1/scopes/13aa6e40-a9e2-4fa2-9468-16c4797a5ca0")
                headers {
                    contentType("multipart/form-data")
                }
                multipart(
                        [
                                fileName: named(
                                        name: value("UpdatedPositiveScopeContract.csv"),
                                        content: producer(file("./UpdatedPositiveScopeContract.csv")),
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
                                "id"       : "13aa6e40-a9e2-4fa2-9468-16c4797a5ca0",
                                "scopeName": "athlone",
                                "uri"      : "v1/scopes/13aa6e40-a9e2-4fa2-9468-16c4797a5ca0"
                        ]
                )
            }
        },
        Contract.make {
            description "should return status 200 and a downloadable file"
            request {
                urlPath("/v1/scopes/13aa6e40-a9e2-4fa2-9468-16c4797a5ca0")
                method GET()
            }
            response {
                status OK()
                headers {
                    contentType("text/csv")
                    header(contentDisposition(), regex('attachment; filename="13aa6e40-a9e2-4fa2-9468-16c4797a5ca0.csv"'))
                }
                body(regex(
                        """\
                        fdn
                        "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030"
                        "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00031,ManagedElement=NR03gNodeBRadio00031"
                        "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00032,ManagedElement=NR03gNodeBRadio00032"
                        """.stripIndent()))
            }
        }
]
