#
# COPYRIGHT Ericsson 2023
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

#Need to get the source code from bazaar and build the executable using the following command:
#CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -v -o helm -ldflags "-X main.version=0.3.0 -extldflags '-static'" ./cmd/helm
ADD helm-3.11.0/helm /usr/bin/helm

RUN mkdir -p /root/.local/share/helm/plugins/helm-unittest-0.3.0

#Need to get the source code from bazaar and build the executable using the following command:
#CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -v -o untt -ldflags "-X main.version=0.3.0 -extldflags '-static'" ./cmd/helm-unittest
ADD helm-unittest-0.3.0 /root/.local/share/helm/plugins/helm-unittest-0.3.0

WORKDIR /apps
VOLUME [ "/apps" ]

ENTRYPOINT ["helm", "unittest"]
CMD ["--help"]

#Build the docker image using this Dockerfile. Increment the tag number before building and pushing.
#docker build -t armdocker.rnd.ericsson.se/proj-eric-oss-dev-test/helm-unittest_image:1.1.1 . --build-arg CBOS_IMAGE_TAG=5.10.0-18 --build-arg CBOS_IMAGE_REPO=armdocker.rnd.ericsson.se/proj-ldc/common_base_os_release --build-arg CBOS_IMAGE_NAME=sles
#Push that docker image version to proj-eric-oss-dev
#docker push armdocker.rnd.ericsson.se/proj-eric-oss-dev-test/helm-unittest_image:1.1.1
