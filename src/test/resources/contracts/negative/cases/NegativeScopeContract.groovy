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
                urlPath("/v1/scopes")
                method POST()
                multipart([
                        scopeName: $(consumer(regex(nonEmpty())), producer("test_scope"))
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
            description "should return status 400 when invalid scopeName"
            request {
                urlPath("/v1/scopes")
                method POST()
                multipart([
                        scopeName: $(consumer(regex(nonEmpty())), producer("invalid^sample_scope")),
                        fileName : named(
                                name: value("NegativeScopeContract.csv"),
                                content: producer(file("./NegativeScopeContract.csv")),
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
                        "detail": "Invalid scopeName: Use only lower case alphanumeric, '-' and '_' characters up to a maximum of 255."
                ])
            }
        },
        Contract.make {
            description "should return status 400 when duplicate scope name"
            request {
                urlPath("/v1/scopes")
                method POST()
                multipart([
                        scopeName: $(consumer(regex(nonEmpty())), producer("duplicate_scope")),
                        fileName : named(
                                name: value("NegativeScopeContract.csv"),
                                content: producer(file("./NegativeScopeContract.csv")),
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
                        "title" : "Failed to create scope. Scope with provided name already exists.",
                        "status": 400,
                        "detail": "Use a unique scope name."
                ])
            }
        },
        Contract.make {
            description "should return status 400 when csv file is invalid"
            request {
                urlPath("/v1/scopes")
                method POST()
                multipart([
                        scopeName: $(consumer(regex(nonEmpty())), producer("null_value_scope")),
                        fileName : named(
                                name: value("NegativeScopeContract.csv"),
                                content: producer(file("./NegativeScopeContract.csv")),
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
                        "title" : "File does not match the csv format.",
                        "status": 400,
                        "detail": "Fill in all required fields with the appropriate information."
                ])
            }
        },
        Contract.make {
            description "should return status 413 when csv file is too large"
            request {
                urlPath("/v1/scopes")
                method POST()
                multipart([
                        scopeName: $(consumer(regex(nonEmpty())), producer("file_too_large_scope")),
                        fileName : named(
                                name: value("NegativeScopeContract.csv"),
                                content: producer(file("./NegativeScopeContract.csv")),
                                contentType: value("text/csv")
                        )
                ])
                headers {
                    contentType multipartFormData()
                }
            }
            response {
                status PAYLOAD_TOO_LARGE()
                body([
                        "title" : "Maximum upload size exceeded.",
                        "status": 413,
                        "detail": "File is too large: the request was rejected because its size (21577375) exceeds the configured maximum (20971520)"
                ])
            }
        },
        Contract.make {
            description "should return status 500 when scope cannot be persisted in the DB"
            request {
                urlPath("/v1/scopes")
                method POST()
                multipart([
                        scopeName: $(consumer(regex(nonEmpty())), producer("non_persisted_scope")),
                        fileName : named(
                                name: value("NegativeScopeContract.csv"),
                                content: producer(file("./NegativeScopeContract.csv")),
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
                        "title" : "Failed to create scope.",
                        "status": 500,
                        "detail": "Database operation failed."
                ])
            }
        },
        Contract.make {
            description "should return status 500 when scope is non-existent"
            request {
                urlPath("/v1/scopes")
                method POST()
                multipart([
                        scopeName: $(consumer(regex(nonEmpty())), producer("non_existent_scope")),
                        fileName : named(
                                name: value("NegativeScopeContract.csv"),
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
                        "title" : "Failed to create scope.",
                        "status": 500,
                        "detail": null
                ])
            }
        },
        Contract.make {
            description "should return status 500 when scopes cannot be retrieved from DB"
            request {
                urlPath("/v1/scopes")
                method GET()
            }
            response {
                status INTERNAL_SERVER_ERROR()
                body([
                        "title" : "Failed to get all Scope Metadata.",
                        "status": 500,
                        "detail": null
                ])
            }
        },
        Contract.make {
            description "should return status 500 when scopes cannot be read from DB"
            request {
                urlPath("/v1/scopes/70352292-24af-11ee-be56-0242ac120002")
                method GET()
            }
            response {
                status INTERNAL_SERVER_ERROR()
                body([
                        "title" : "Error creating csv file.",
                        "status": 500,
                        "detail": "Failed to read InputStream.",
                ])
            }
        },
        Contract.make {
            description "should return status 404 when scope not found in the DB"
            request {
                urlPath("/v1/scopes/d553052c-24af-11ee-be56-0242ac120002")
                method GET()
            }
            response {
                status NOT_FOUND()
                body([
                        "title" : "Scope with provided ID does not exist.",
                        "status": 404,
                        "detail": "Provide a valid Scope ID."
                ])
            }
        },
        Contract.make {
            description "should return status 400"
            request {
                urlPath("/v1/scopes/invalid_ID")
                method GET()
            }
            response {
                status BAD_REQUEST()
                body([
                        "title" : "Error parsing ID. Provided ID is invalid.",
                        "status": 400,
                        "detail": "Provide a valid ID."
                ])
            }
        },
        Contract.make {
            description "should return status 404"
            request {
                urlPath("/v1/scopes/d813c56e-209d-11ee-be56-0242ac120002")
                method DELETE()
            }
            response {
                status NOT_FOUND()
                body([
                        "title" : "Scope with provided ID does not exist.",
                        "status": 404,
                        "detail": "Provide a valid Scope ID."
                ])
            }
        },
        Contract.make {
            description "should return status 400"
            request {
                urlPath("/v1/scopes/invalid_ID")
                method DELETE()
            }
            response {
                status BAD_REQUEST()
                body([
                        "title" : "Error parsing ID. Provided ID is invalid.",
                        "status": 400,
                        "detail": "Provide a valid ID."
                ])
            }
        },
        Contract.make {
            description "should return status 409"
            request {
                urlPath("/v1/scopes/6c9863db-52ff-45e7-97d3-82fff93ba639")
                method DELETE()
            }
            response {
                status CONFLICT()
                body([
                        "title" : "Scope with provided ID is used in a Job configuration.",
                        "status": 409,
                        "detail": "Only Scopes not associated with a Job configuration can be deleted."
                ])
            }
        },
        Contract.make {
            description "should return status 409"
            request {
                method PUT()
                urlPath("/v1/scopes/6c9863db-52ff-45e7-97d3-82fff93ba639")
                headers {
                    contentType("multipart/form-data")
                }
                multipart(
                        [
                                fileName: named(
                                        name: value("NegativeScopeContract.csv"),
                                        content: producer(file("./NegativeScopeContract.csv")),
                                        contentType: value("text/csv")
                                )
                        ]
                )
            }
            response {
                status CONFLICT()
                body([
                        "title" : "Cannot update scope as it is used by an ongoing execution.",
                        "status": 409,
                        "detail": "Scope in use."
                ])
            }
        },
        Contract.make {
            description "should return status 400"
            request {
                urlPath("/v1/scopes/invalid_ID")
                method PUT()
                headers {
                    contentType("multipart/form-data")
                }
                multipart(
                        [
                                fileName: named(
                                        name: value("NegativeScopeContract.csv"),
                                        content: producer(file("./NegativeScopeContract.csv")),
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
                        "detail": "Provide a valid ID."
                ])
            }
        },
        Contract.make {
            description "should return status 404"
            request {
                urlPath("/v1/scopes/d813c56e-209d-11ee-be56-0242ac120002")
                method PUT()
                headers {
                    contentType("multipart/form-data")
                }
                multipart(
                        [
                                fileName: named(
                                        name: value("NegativeScopeContract.csv"),
                                        content: producer(file("./NegativeScopeContract.csv")),
                                        contentType: value("text/csv")
                                )
                        ]
                )
            }
            response {
                status NOT_FOUND()
                body([
                        "title" : "Scope with provided ID does not exist.",
                        "status": 404,
                        "detail": "Provide a valid Scope ID."
                ])
            }
        },
        Contract.make {
            description "should return status 413 when csv file is too large"
            request {
                urlPath("/v1/scopes/e326e766-2c63-11ee-be56-0242ac120002")
                method PUT()
                headers {
                    contentType("multipart/form-data")
                }
                multipart(
                        [
                                fileName: named(
                                        name: value("NegativeScopeContract.csv"),
                                        content: producer(file("./NegativeScopeContract.csv")),
                                        contentType: value("text/csv")
                                )
                        ]
                )
                headers {
                    contentType multipartFormData()
                }
            }
            response {
                status PAYLOAD_TOO_LARGE()
                body([
                        "title" : "Maximum upload size exceeded.",
                        "status": 413,
                        "detail": "File is too large: the request was rejected because its size (21577375) exceeds the configured maximum (20971520)"
                ]
                )
            }
        },
        Contract.make {
            description "should return status 500 when scopes cannot be read from DB for update"
            request {
                urlPath("/v1/scopes/70352292-24af-11ee-be56-0242ac120002")
                method PUT()
                headers {
                    contentType("multipart/form-data")
                }
                multipart(
                        [
                                fileName: named(
                                        name: value("NegativeScopeContract.csv"),
                                        content: producer(file("./NegativeScopeContract.csv")),
                                        contentType: value("text/csv")
                                )
                        ]
                )
            }
            response {
                status INTERNAL_SERVER_ERROR()
                body([
                        "title" : "Error updating from csv file.",
                        "status": 500,
                        "detail": "Failed to update the scope from the file.",
                ])
            }
        }
]