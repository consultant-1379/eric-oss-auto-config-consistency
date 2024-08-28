#!/bin/bash
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

echo "#####################################################################"
K6_DOCKER_IMAGE="armdocker.rnd.ericsson.se/proj-eric-oss-drop/k6-base-image:latest"
K6_DOCKER_NAME="EACC_rApp_end2end_k6_$(date +%d-%m-%y_%H-%M-%S)_${BUILD_NUMBER}"

echo "# Creating env file:"
cd ${WORKSPACE}
ENV_FILE="EACC_end2end_connected_sys.env.txt"
echo
> $ENV_FILE
printenv | sort > $ENV_FILE

echo "# Parameterized variables:"
cat $ENV_FILE
echo

echo "#####################################################################"
echo "# Executing k6 tests..."
echo "#"

echo "# Get k6 docker version"
docker run --rm $K6_DOCKER_IMAGE k6 version

echo "#"
COMMAND="docker run --rm --env-file ${ENV_FILE} -v ${WORKSPACE}/k6:/k6 -v ${WORKSPACE}/doc:/doc --name $K6_DOCKER_NAME \
  $K6_DOCKER_IMAGE k6 run --insecure-skip-tls-verify --quiet --verbose \
  /k6/src/test/js/k6.main.js"

echo "# Command being run: $COMMAND"
output=$($COMMAND 2>&1)
exit_status=$?
echo "$output"

echo "# k6 functional execution result code: $exit_status (non-zero means a problem occurred, otherwise successful execution)"
echo "###########################################################"
echo "Process complete"
[ $exit_status -ne 0 ] && exit 1
echo "SUCCESS"
exit 0
