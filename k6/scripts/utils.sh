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
KUBE_DOCKER_IMAGE="armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob-py3kubehelmbuilder:latest"
DOCKER_CONTAINER_NAME="EACC_truncate_executions_$(date +%d-%m-%y_%H-%M-%S)_${BUILD_NUMBER}"

getPgMasterPod(){
    echo "# Get EACC PG pods"
    echo ${NAMESPACE}
    COMMAND="docker run --init --rm \
       --name ${DOCKER_CONTAINER_NAME} \
       --volume ${KUBECONFIG_FILE_PATH}:/.kube/config \
       --env KUBECONFIG=/.kube/config \
       ${KUBE_DOCKER_IMAGE} kubectl get pod -n ${NAMESPACE} -l app=eric-oss-auto-config-consistency-pg -o=name
    "
    printf '# Command to collect EACC PG PODS:\n#\t%s\n\n' "$COMMAND"

    EACC_PG_PODS=$($COMMAND 2>&1)
    exit_status=$?
    if [ $exit_status -ne 0 ] || [ "${EACC_PG_PODS}" == "" ] || [ "${EACC_PG_PODS}" == "No resources found in ${NAMESPACE} namespace." ]; then
    echo "# No EACC PG PODS found. ${EACC_PG_PODS}"
    exit 1;
    fi

    echo "# Found EACC PG PODS. ${EACC_PG_PODS}"

    for pod in ${EACC_PG_PODS}
    do
    podName=$(echo "$pod" | sed 's/pod\///')

    result=$(docker run --init --rm \
         --name ${DOCKER_CONTAINER_NAME} \
         --volume ${KUBECONFIG_FILE_PATH}:/.kube/config \
         --env KUBECONFIG=/.kube/config \
         ${KUBE_DOCKER_IMAGE} kubectl exec -itn ${NAMESPACE} ${podName} -c eric-oss-auto-config-consistency-pg -- psql -d postgres -U postgres -c "select CASE WHEN pg_is_in_recovery() THEN 'standby' ELSE 'master' END as node_state;")

    if [[ $result == *"master"* ]]; then
        echo "# ${podName} is the master postgres pod"
        PG_MASTER_POD=${podName}
        break
    fi
    done

    if [ -z "$PG_MASTER_POD" ]; then
        echo "# Unable to get master pod for postgres"
        cleanTempDirectory
        exit 1;
    fi

    echo "# PG pod name: ${PG_MASTER_POD}"
}

truncateTables(){
    echo "# Executing command to TRUNCATE audit_results and executions on remote pod "
    docker run --init --rm \
       --name ${DOCKER_CONTAINER_NAME} \
       --volume ${KUBECONFIG_FILE_PATH}:/.kube/config \
       --env KUBECONFIG=/.kube/config \
       ${KUBE_DOCKER_IMAGE} kubectl exec -n ${NAMESPACE} ${PG_MASTER_POD} -it \
            -- sh -c "psql -U eacc-user -d eacc -f tmp/truncate_tables.sql -v ON_ERROR_STOP=1"
            
    exit_status=$?
    if [ $exit_status -ne 0 ]; then
        echo "# Unable to execute SQL TRUNCATE commands"
        exit 1;
    fi
}

cleanTempDirectory() {
  echo "# Cleaning local directory"
  rm -rf "${TEMP_DIR}"
}