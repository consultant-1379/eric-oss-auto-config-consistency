/*******************************************************************************
 * COPYRIGHT Ericsson 2024
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

[
        Contract.make {
            name "When reverting a change with invalid execution ID"
            description "should return status 400"
            request {
                urlPath("/v1/executions/A*/audit-results")
                method POST()
                headers {
                    contentType applicationJson()
                }
                body([
                        auditResultIds: ["2", "3"],
                        operation     : "REVERT_CHANGE"
                ])
            }
            response {
                status BAD_REQUEST()
                body([
                        "title" : "Validation failed",
                        "status": 400,
                        "detail": "Execution ID can only contain numeric characters."
                ])
            }
        },
        Contract.make {
            name "When reverting a change with invalid operation"
            description "should return status 400"
            request {
                urlPath("/v1/executions/1/audit-results")
                method POST()
                headers {
                    contentType applicationJson()
                }
                body([
                        auditResultIds: ["2", "3"],
                        operation     : "UNDO_CHANGE"
                ])
            }
            response {
                status BAD_REQUEST()
                body([
                        "title" : "Validation failed",
                        "status": 400,
                        "detail": "Invalid operation. Operation must be APPLY_CHANGE or REVERT_CHANGE."
                ])
            }
        },
        Contract.make {
            name "When reverting a change and changes do not exist"
            description "should return status 400"
            request {
                urlPath("/v1/executions/3/audit-results")
                method POST()
                headers {
                    contentType applicationJson()
                }
                body([
                        auditResultIds: ["2", "3"],
                        operation     : "REVERT_CHANGE"
                ])
            }
            response {
                status BAD_REQUEST()
                body([
                        "title" : "Validation failed",
                        "status": 400,
                        "detail": "No inconsistencies found for proposed change id(s)"
                ])
            }
        },
        Contract.make {
            name "When applying a change and changes do not exist"
            description "should return status 400"
            request {
                urlPath("/v1/executions/3/audit-results")
                method POST()
                headers {
                    contentType applicationJson()
                }
                body([
                        auditResultIds: ["2", "3"],
                        operation     : "APPLY_CHANGE"
                ])
            }
            response {
                status BAD_REQUEST()
                body([
                        "title" : "Validation failed",
                        "status": 400,
                        "detail": "No inconsistencies found for proposed change id(s)"
                ])
            }
        },
        Contract.make {
            name "When reverting all change and changes do not exist"
            description "should return status 400"
            request {
                urlPath("/v1/executions/3/audit-results")
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
                status BAD_REQUEST()
                body([
                        "title" : "Validation failed",
                        "status": 400,
                        "detail": "No inconsistencies found for proposed change id(s)"
                ])
            }
        },
        Contract.make {
            name "When applying all changes and changes do not exist"
            description "should return status 400"
            request {
                urlPath("/v1/executions/3/audit-results")
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
                status BAD_REQUEST()
                body([
                        "title" : "Validation failed",
                        "status": 400,
                        "detail": "No inconsistencies found for proposed change id(s)"
                ])
            }
        },
        Contract.make {
            name "When reverting a change and changes are In Progress"
            description "should return status 400"
            request {
                urlPath("/v1/executions/2/audit-results")
                method POST()
                headers {
                    contentType applicationJson()
                }
                body([
                        auditResultIds: ["2", "3"],
                        operation     : "REVERT_CHANGE"
                ])
            }
            response {
                status BAD_REQUEST()
                body([
                        "title" : "Validation failed",
                        "status": 400,
                        "detail": "Cannot apply/revert changes with status 'Implementation in progress'"
                ])
            }
        },
        Contract.make {
            name "When applying a change and changes are In Progress"
            description "should return status 400"
            request {
                urlPath("/v1/executions/2/audit-results")
                method POST()
                headers {
                    contentType applicationJson()
                }
                body([
                        auditResultIds: ["2", "3"],
                        operation     : "APPLY_CHANGE"
                ])
            }
            response {
                status BAD_REQUEST()
                body([
                        "title" : "Validation failed",
                        "status": 400,
                        "detail": "Cannot apply/revert changes with status 'Implementation in progress'"
                ])
            }
        },
        Contract.make {
            name "When reverting all changes and changes are In Progress"
            description "should return status 400"
            request {
                urlPath("/v1/executions/2/audit-results")
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
                status BAD_REQUEST()
                body([
                        "title" : "Validation failed",
                        "status": 400,
                        "detail": "Cannot apply/revert changes with status 'Implementation in progress'"
                ])
            }
        },
        Contract.make {
            name "When applying all changes and changes are In Progress"
            description "should return status 400"
            request {
                urlPath("/v1/executions/2/audit-results")
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
                status BAD_REQUEST()
                body([
                        "title" : "Validation failed",
                        "status": 400,
                        "detail": "Cannot apply/revert changes with status 'Implementation in progress'"
                ])
            }
        },
        Contract.make {
            name "When reverting a change with a non existing execution ID"
            description "should return status 404"
            request {
                urlPath("/v1/executions/9999/audit-results")
                method POST()
                headers {
                    contentType applicationJson()
                }
                body([
                        auditResultIds: ["2", "3"],
                        operation     : "REVERT_CHANGE"
                ])
            }
            response {
                status NOT_FOUND()
                body([
                        "title" : "Execution ID does not exist.",
                        "status": 404,
                        "detail": "Provide a valid Execution ID."
                ])
            }
        },
        Contract.make {
            name "When applying a change with a non existing execution ID"
            description "should return status 404"
            request {
                urlPath("/v1/executions/9999/audit-results")
                method POST()
                headers {
                    contentType applicationJson()
                }
                body([
                        auditResultIds: ["2", "3"],
                        operation     : "APPLY_CHANGE"
                ])
            }
            response {
                status NOT_FOUND()
                body([
                        "title" : "Execution ID does not exist.",
                        "status": 404,
                        "detail": "Provide a valid Execution ID."
                ])
            }
        },
        Contract.make {
            description "should return status 404 when the execution ID does not exist"
            request {
                urlPath("/v1/executions/9999/audit-results")
                method GET()
            }
            response {
                status NOT_FOUND()
                body([
                        "title" : "Execution ID does not exist.",
                        "status": 404,
                        "detail": "Provide a valid Execution ID."
                ])
            }
        },
        Contract.make {
            name "should return status 400 when the execution ID does not exist"
            description "should return status 400"
            request {
                urlPath("/v1/executions/A*/audit-results")
                method GET()
            }
            response {
                status BAD_REQUEST()
                body([
                        "title" : "Validation failed",
                        "status": 400,
                        "detail": "Execution ID can only contain numeric characters."
                ])
            }
        },
        Contract.make {
            name "When reverting a change on an old execution"
            description "should return status 400"
            request {
                urlPath("/v1/executions/4/audit-results")
                method POST()
                headers {
                    contentType applicationJson()
                }
                body([
                        auditResultIds: ["2", "3"],
                        operation     : "REVERT_CHANGE"
                ])
            }
            response {
                status BAD_REQUEST()
                body([
                        "title" : "Validation failed",
                        "status": 400,
                        "detail": "Changes can only be reverted from the latest execution"
                ])
            }
        },
        Contract.make {
            name "When reverting all changes on an old execution"
            description "should return status 400"
            request {
                urlPath("/v1/executions/4/audit-results")
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
                status BAD_REQUEST()
                body([
                        "title" : "Validation failed",
                        "status": 400,
                        "detail": "Changes can only be reverted from the latest execution"
                ])
            }
        }
]