#
# COPYRIGHT Ericsson 2023 - 2024
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

# Base OS related params.
ARG CBOS_IMAGE_TAG
ARG CBOS_IMAGE_REPO
ARG CBOS_IMAGE_NAME

ARG NODEJS_BUILDER_URL=armdocker.rnd.ericsson.se/proj-adp-cicd-drop/adp-nodejs-lts-builder-image
ARG NODEJS_BUILDER_VERSION=16.16.0-1

# Builder: EACC client
#   Copy the GUI related files and call build.
FROM $NODEJS_BUILDER_URL:$NODEJS_BUILDER_VERSION as builder_eacc_client

ARG UI_DIRECTORY=eric-oss-auto-config-consistency-ui

WORKDIR /
# Copy the npm project files first, as these are changed rarely
COPY $UI_DIRECTORY/package.json package.json
COPY $UI_DIRECTORY/.npmrc .npmrc
RUN npm run install:all

# Now copy the remaining files which can change frequently
# Call the GUI build which creates the runtime artifacts
COPY $UI_DIRECTORY/ .
RUN npm run build:dev

# Runtime: Apache Server
FROM ${CBOS_IMAGE_REPO}/${CBOS_IMAGE_NAME}:${CBOS_IMAGE_TAG}
ARG CBOS_IMAGE_TAG
ARG CBOS_REPO_URL=https://arm.sero.gic.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles/${CBOS_IMAGE_TAG}

#Install Apache server
RUN zypper ar -C -G -f $CBOS_REPO_URL?ssl_verify=no \
    COMMON_BASE_OS_SLES_REPO \
    && zypper ref -f -r COMMON_BASE_OS_SLES_REPO \
    && zypper install -l -y shadow \
    && zypper --non-interactive in apache2 \
    && zypper clean all \
    && rm -rf /var/cache/zypp \
    && zypper clean -a
# Other params

ENV APP_DIR /app
WORKDIR ${APP_DIR}

#Add user_id - id is generated as per ADP security guidelines
ARG USER_ID=290882
#In openshift USER_NAME should be numeric and same as user id
ARG USER_NAME=290882

RUN chown -R ${USER_NAME}:0 ${APP_DIR} \
    && chmod -R g=u ${APP_DIR}
RUN echo "${USER_ID}:x:${USER_ID}:${USER_ID}:An Identity for ${USER_NAME}:/nonexistent:/bin/false" >>/etc/passwd && \
    echo "${USER_ID}:!::0:::::" >>/etc/shadow

# Copy UI
COPY --chown=${USER_NAME}:0 --from=builder_eacc_client build/ /srv/www/htdocs/eric-oss-acc-ui
COPY --chown=${USER_NAME}:0 --from=builder_eacc_client docker/httpd.conf /tmp/apache2/httpd.conf
COPY --chown=${USER_NAME}:0 --from=builder_eacc_client docker/entrypoint.sh /usr/sbin/entrypoint.sh

# Configure entrypoint
RUN chmod +x /usr/sbin/entrypoint.sh

RUN chown -R ${USER_NAME}:0 /srv/www/htdocs/ /etc/apache2/ /usr/sbin/*apache* /var/log/apache2/ /usr/sbin/entrypoint.sh && \
    chmod -R g=u /srv/www/htdocs/ /etc/apache2/ /usr/sbin/*apache* /var/log/apache2/ /usr/sbin/entrypoint.sh
ENTRYPOINT ["/usr/bin/catatonit", "--"]
CMD ["/usr/sbin/entrypoint.sh"]
EXPOSE 4200
USER $USER_ID
# Set base dir to the backend
ARG COMMIT
ARG BUILD_DATE
ARG APP_VERSION
ARG RSTATE
ARG TITLE
ARG IMAGE_PRODUCT_NUMBER
LABEL \
     org.opencontainers.image.title=$TITLE \
     org.opencontainers.image.created=$BUILD_DATE \
     org.opencontainers.image.revision=$COMMIT \
     org.opencontainers.image.vendor=Ericsson \
     org.opencontainers.image.version=$APP_VERSION \
     com.ericsson.product-revision="${RSTATE}" \
     com.ericsson.product-number="$IMAGE_PRODUCT_NUMBER"