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

# If this script fail to COPY due to changes in schemas;
# Ensure there is only 1 execution and its respective audit results in the DB, the configuration being the load_test_ruleset and load_test_scope
# Run the below commands in postgres database pod
# pg_dump -U eacc-user -d eacc --data-only -v -t eacc_executions > /tmp/executions_import.sql
# pg_dump -U eacc-user -d eacc --data-only -v -t audit_results > /tmp/audit_results_import.sql
# Copy to local machine and replace files in k6/src/test/resources/sql/ directory
# If the execution id in either file changes position, update the sed regex to reflect this.

echo "#####################################################################"
KUBE_DOCKER_IMAGE="armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob-py3kubehelmbuilder:latest"
DOCKER_CONTAINER_NAME="EACC_test_script_$(date +%d-%m-%y_%H-%M-%S)_${BUILD_NUMBER}"

MAX_EXECUTIONS=180

echo "# Creating env file:"
cd ${WORKSPACE}
ENV_FILE="EACC_populate_database.txt"
echo
> $ENV_FILE
printenv | sort > $ENV_FILE

echo "# Parameterized variables:"
cat $ENV_FILE
echo

echo "#####################################################################"
echo "# Executing populate_database.sh..."
echo "#"

TEMP_DIR="${WORKSPACE}/k6/tmp/output"
mkdir -p "${TEMP_DIR}/sql"
. k6/scripts/utils.sh

printCounts() {
  echo "# Printing counts from executions and audit results "
  docker run --init --rm \
         --name ${DOCKER_CONTAINER_NAME} \
         --volume ${KUBECONFIG_FILE_PATH}:/.kube/config \
         --env KUBECONFIG=/.kube/config \
         ${KUBE_DOCKER_IMAGE} kubectl exec -n ${NAMESPACE} ${PG_MASTER_POD} -it \
         -- sh -c "psql -U eacc-user -d eacc -c 'select count(*) as execution_count from eacc_executions;' && psql -U eacc-user -d eacc -c 'select count(*) as audit_result_count from audit_results;'"
}

getPgMasterPod

echo "# Copying reset file to remote pod"
COMMAND="docker run --init --rm \
       --name ${DOCKER_CONTAINER_NAME} \
       --volume ${KUBECONFIG_FILE_PATH}:/.kube/config \
       -v ${WORKSPACE}/k6:/k6 \
       --env KUBECONFIG=/.kube/config \
       ${KUBE_DOCKER_IMAGE} kubectl -n ${NAMESPACE} cp k6/src/test/resources/sql/truncate_tables.sql ${PG_MASTER_POD}:/tmp/truncate_tables.sql"


echo "# Running command: ${COMMAND}"
$COMMAND
exit_status=$?
if [ $exit_status -ne 0 ]; then
    echo "# Unable to copy reset file to remote directory"
    cleanTempDirectory
    exit 1;
fi
printCounts

truncateTables
 
printCounts

echo "# Expanding SQL files"

for ((  id = 1;  id <= MAX_EXECUTIONS;  id++ )); do
  # Replace execution id and sequence key, then write to new file
  echo "# Creating copy of execution with id: ${id}"
  sed -Ee "s/[0-9]+\t/${id}\t/" -e "s/[0-9]+, true/${id}, true/g" "${WORKSPACE}/k6/src/test/resources/sql/executions_import.sql" >> "${TEMP_DIR}/sql/eacc_executions_expanded.sql"
  sed -E $"s/CONSISTENT\t[0-9]+/CONSISTENT\t${id}/g" "${WORKSPACE}/k6/src/test/resources/sql/audit_results_import.sql" >> "${TEMP_DIR}/sql/eacc_audit_results_expanded.sql"
done

TAR_FILE_NAME="expanded_eacc.tar.gz"

echo "# Compressing files in tar ball"
tar -czvf "${TEMP_DIR}/${TAR_FILE_NAME}" -C "${TEMP_DIR}/sql" .
exit_status=$?
if [ $exit_status -ne 0 ]; then
  echo "# Unable to compress sql files"
  cleanTempDirectory
  exit 1;
fi

echo "# Running command: ${COMMAND}"
$COMMAND

COMMAND="docker run --init --rm \
       --name ${DOCKER_CONTAINER_NAME} \
       --volume ${KUBECONFIG_FILE_PATH}:/.kube/config \
       -v ${WORKSPACE}/k6:/k6 \
       --env KUBECONFIG=/.kube/config \
       ${KUBE_DOCKER_IMAGE} kubectl -n ${NAMESPACE} cp k6/tmp/output/${TAR_FILE_NAME} ${PG_MASTER_POD}:/tmp/${TAR_FILE_NAME}"

echo "# Running command: ${COMMAND}"
$COMMAND
exit_status=$?
if [ $exit_status -ne 0 ]; then
    echo "# Unable to copy tar ball to remote directory"
    cleanTempDirectory
    exit 1;
fi

echo "# Decompressing files in remote directory"

docker run --init --rm \
       --name ${DOCKER_CONTAINER_NAME} \
       --volume ${KUBECONFIG_FILE_PATH}:/.kube/config \
       --env KUBECONFIG=/.kube/config \
       ${KUBE_DOCKER_IMAGE} kubectl exec -n ${NAMESPACE} ${PG_MASTER_POD} -it -- sh -c "cd /tmp && tar -xzvf expanded_eacc.tar.gz --strip-components=1"

exit_status=$?
if [ $exit_status -ne 0 ]; then
    echo "# Unable to decompress archive in remote directory"
    cleanTempDirectory
    exit 1;
fi

echo "# Executing SQL COPY commands "
docker run --init --rm \
       --name ${DOCKER_CONTAINER_NAME} \
       --volume ${KUBECONFIG_FILE_PATH}:/.kube/config \
       --env KUBECONFIG=/.kube/config \
       ${KUBE_DOCKER_IMAGE} kubectl exec -n ${NAMESPACE} ${PG_MASTER_POD} -it \
       -- sh -c "psql -U eacc-user -d eacc -f tmp/eacc_executions_expanded.sql -v ON_ERROR_STOP=1 && psql -U eacc-user -d eacc -f tmp/eacc_audit_results_expanded.sql -v ON_ERROR_STOP=1" > "${WORKSPACE}/insert_load_data.log"

exit_status=$?
if [ $exit_status -ne 0 ]; then
    echo "# Unable to execute SQL COPY commands"
    cleanTempDirectory
    exit 1;
fi

printCounts

cleanTempDirectory

echo "SUCCESS"
exit 0
