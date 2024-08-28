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

package com.ericsson.oss.apps.controller;

import static org.springframework.http.HttpStatus.OK;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ControllerDetailException extends ResponseStatusException {

    private static final long serialVersionUID = 1920932903767979511L;
    private final String detail;
    private final String title;

    public ControllerDetailException(final HttpStatus status, final String detail, final String title) {
        super(status, title);
        this.detail = detail;
        this.title = title;
    }

    public ControllerDetailException(final HttpStatus status, final String detail, final String title, final Throwable cause) {
        super(status, title, cause);
        this.detail = detail;
        this.title = title;
    }

    public ControllerDetailException(final HttpStatus status, final String title) {
        this(status, null, title);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getReason() {
        return title;
    } //TODO: consistently use title or reason [IDUN-99789]

    public String getDetail() {
        return detail;
    }

    static class Builder {
        private HttpStatus status;
        private String detail;
        private String title;

        private Builder() {
            status = OK;
            detail = null;
            title = null;
        }

        public ControllerDetailException build() {
            return new ControllerDetailException(status, detail, title);
        }

        public Builder withStatus(final HttpStatus status) {
            this.status = status;
            return this;
        }

        public Builder withTitle(final String title) {
            this.title = title;
            return this;
        }

        public Builder withDetail(final String detail) {
            this.detail = detail;
            return this;
        }

    }
}
