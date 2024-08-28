#!/bin/bash
#
# COPYRIGHT Ericsson 2024
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

test_phase=$(printenv TEST_PHASE)

if [ "$test_phase" == "POST_INSTANTIATION" ]; then

  echo "#####################################################################"
  echo "# Executing k6 load tests..."
  echo "#"

#  echo "$output" | grep "Managed Elements found" > managed_elements.txt
#
#  echo "#"
#  COMMAND="chmod 777 ./k6/scripts/scope_file_generator.sh"
#  echo "# Command being run: $COMMAND"
#  $COMMAND
#  exit_status1=$?
#
#  echo "#"
#  COMMAND="./k6/scripts/scope_file_generator.sh"
#  echo "# Command being run: $COMMAND"
#  $COMMAND
#  exit_status2=$?

  echo "#"
  COMMAND="docker run --rm --env-file ${ENV_FILE} -v ${WORKSPACE}/k6:/k6 -v ${WORKSPACE}/doc:/doc --name $K6_DOCKER_NAME \
    $K6_DOCKER_IMAGE k6 run --insecure-skip-tls-verify --quiet --verbose \
    /k6/src/test/js/k6-eacc-load.main.js"
  echo "# Command being run: $COMMAND"
  output=$($COMMAND 2>&1)
  exit_status3=$?
  echo "$output"

  . k6/scripts/utils.sh
  getPgMasterPod 
  truncateTables

#  echo "# Changing permission of the file: $exit_status1 (non-zero means a problem occurred, otherwise successful execution)"
#  echo "# Scope file generator result code: $exit_status2 (non-zero means a problem occurred, otherwise successful execution)"
  echo "# K6 Load test result code: $exit_status3 (non-zero means a problem occurred, otherwise successful execution)"
  echo "###########################################################"
  echo "Process complete"
  [ $exit_status3 -ne 0 ] && exit 1

  echo "#####################################################################"
  echo "# Executing k6 rate limiter tests..."
  echo "#"

  sleep 2
  COMMAND="docker run --rm --env-file ${ENV_FILE} -v ${WORKSPACE}/k6:/k6 -v ${WORKSPACE}/doc:/doc --name $K6_DOCKER_NAME \
    $K6_DOCKER_IMAGE k6 run --insecure-skip-tls-verify --quiet --verbose \
    /k6/src/test/js/rate-limit.js"

  echo "# Command being run: $COMMAND"
  output=$($COMMAND 2>&1)
  exit_status4=$?
  echo "$output"

  echo "# K6 rate limiter test result code: $exit_status4 (non-zero means a problem occurred, otherwise successful execution)"
  echo "###########################################################"
  echo "Process complete"
  [ $exit_status4 -ne 0 ] && exit 1
  echo "SUCCESS"
  exit 0
else
  echo "# Executing k6 load tests only run at POST_INSTANTIATION..."
  exit 0
fi
