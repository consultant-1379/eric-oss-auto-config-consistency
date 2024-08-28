#
# COPYRIGHT Ericsson 2022
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

ARG CBOS_IMAGE_TAG
ARG CBOS_IMAGE_REPO
ARG CBOS_IMAGE_NAME

FROM ${CBOS_IMAGE_REPO}/${CBOS_IMAGE_NAME}:${CBOS_IMAGE_TAG}
ARG CBOS_IMAGE_TAG
ARG CBOS_REPO_URL=https://arm.sero.gic.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles/${CBOS_IMAGE_TAG}

RUN zypper ar -C -G -f $CBOS_REPO_URL?ssl_verify=no \
    COMMON_BASE_OS_SLES_REPO \
    && zypper install -l -y java-11-openjdk-headless \
    && zypper clean --all \
    && zypper rr COMMON_BASE_OS_SLES_REPO

ARG USER_ID=40514
RUN echo "$USER_ID:!::0:::::" >>/etc/shadow

ARG USER_NAME="eric-oss-auto-config-consistency"
RUN echo "$USER_ID:x:$USER_ID:0:An Identity for $USER_NAME:/nonexistent:/bin/false" >>/etc/passwd

ARG JAR_FILE
ADD consistency-checker/audit-job/target/${JAR_FILE} audit-job.jar

USER $USER_ID

CMD ["/bin/sh", "-c", "java ${JAVA_OPTS} -jar audit-job.jar"]

ARG COMMIT
ARG BUILD_DATE
ARG APP_VERSION
ARG RSTATE
ARG IMAGE_PRODUCT_NUMBER
LABEL \
    org.opencontainers.image.title=eric-oss-auto-config-consistency-jsb \
    org.opencontainers.image.created=$BUILD_DATE \
    org.opencontainers.image.revision=$COMMIT \
    org.opencontainers.image.vendor=Ericsson \
    org.opencontainers.image.version=$APP_VERSION \
    com.ericsson.product-revision="${RSTATE}" \
    com.ericsson.product-number="$IMAGE_PRODUCT_NUMBER"