#!/bin/bash

################################################################################
# * COPYRIGHT Ericsson 2023
# *
# *
# *
# * The copyright to the computer program(s) herein is the property of
# *
# * Ericsson Inc. The programs may be used and/or copied only with written
# *
# * permission from Ericsson Inc. or in accordance with the terms and
# *
# * conditions stipulated in the agreement/contract under which the
# *
# * program(s) have been supplied.
################################################################################

PROJECT_ROOT=`pwd`

USER_ID=$( echo "$(whoami)" | tr '[:upper:]' '[:lower:]' | awk -F'@' '{print $1}'| awk -F'+' '{print $2}')
TAG=$(date +%Y-%m-%d-%H-%M-%S)

SERVICE_NAME="eric-oss-auto-config-consistency"
SERVICE_RELEASE_NAME=${SERVICE_NAME}-release
SERVICE_REPO_PATH="proj-eric-oss-dev"
SERVICE_TAG="${TAG}-${USER_ID}"
NS=${USER_ID}

CBOS_IMAGE_REPO=$( grep "cbos-image-repo" common-properties.yaml | awk '{print $3}' | sed -e 's/^"//' -e 's/"$//')
CBOS_IMAGE_NAME=$( grep "cbos-image-name" common-properties.yaml | awk '{print $3}' | sed -e 's/^"//' -e 's/"$//')
CBOS_IMAGE_TAG=$( grep "cbos-image-version" common-properties.yaml | awk '{print $3}' | sed -e 's/^"//' -e 's/"$//')


MAVEN_BUILD=true
REMOTE=false
CLEAN=false
VERBOSE=false

TIMEOUT_HELM_TESTS=10s

ARTIFACTORY_USER=$USER_ID
ARTIFACTORY_PASS=""

GATEWAY_USER=""
GATEWAY_PASSWORD=""
GATEWAY_HOST=""
HOST="https://${GATEWAY_HOST}"

SERVICE_VALUES=" --set imageCredentials.${SERVICE_NAME}.repoPath=${SERVICE_REPO_PATH},\
imageCredentials.${SERVICE_NAME}.tag=${SERVICE_TAG},\
imageCredentials.pullSecret=armdocker,\
global.registry.imagePullPolicy=IfNotPresent,\
eric-log-shipper.logshipper.autodiscover.namespace=${NS}"

NC='\033[0m' # No Color
BROWN='\033[0;33m'

function log() {
    echo -e "\n${BROWN} --- ${1} --- ${NC}\n"
}

function checkExitCode() {
    if [ $? -ne 0 ]; then
          log "ERROR: $1 "
          exit 255
    fi
}

function cleanUpHelmEnvironment() {
    log "Cleaning up Helm environment"
    helm uninstall ${SERVICE_RELEASE_NAME} -n ${NS}

    log "Deleting namespace ${NS}"
    kubectl delete namespace ${NS}
}

function ensureOnCorrectServer() {
  if [[ "$REMOTE" = false && ! -z "$KUBECONFIG" ]]; then
    log "Changing KUBECONFIG from $KUBECONFIG to empty to install locally."
    export KUBECONFIG=""
  fi
  if [[ "$REMOTE" = true && -z "$KUBECONFIG" ]]; then
    log "Kubeconfig not set.."
    log "Exiting.."
    exit 255
  fi
}

function updateEaccCharts() {
    log "Packaging ${SERVICE_NAME}"

    cd "${PROJECT_ROOT}/charts/${SERVICE_NAME}"
    helm dependency update 2>&1 | grep -v "skipping loading invalid"
    cd ${PROJECT_ROOT}
}

function buildDockerImage() {
  log "Building Docker Image"
  if [[ $MAVEN_BUILD = true ]]; then
    export SELI_ARTIFACTORY_REPO_USER=${USER_ID} && export SELI_ARTIFACTORY_REPO_PASS=${ARTIFACTORY_PASS}
    if [[ $VERBOSE = true ]]; then
        mvn clean install -Dmaven.test.skip=true
      else
        echo "Running Maven Build.."
        mvn clean install -Dmaven.test.skip=true > /dev/null 2>&1
    fi
    checkExitCode "Failed to Build Maven Project"
  fi
  cp target/*SNAPSHOT.jar target/${SERVICE_NAME}-${SERVICE_TAG}.jar

  log "Building Docker Image ${SERVICE_NAME}:${SERVICE_TAG}"
  if [[ $VERBOSE = true ]]; then
      docker build . --tag armdocker.rnd.ericsson.se/${SERVICE_REPO_PATH}/${SERVICE_NAME}:${SERVICE_TAG} --build-arg JAR_FILE=${SERVICE_NAME}-${SERVICE_TAG}.jar --build-arg CBOS_IMAGE_REPO=${CBOS_IMAGE_REPO} --build-arg CBOS_IMAGE_NAME=${CBOS_IMAGE_NAME} --build-arg CBOS_IMAGE_TAG=${CBOS_IMAGE_TAG}
    else
      echo "Running Docker Build.."
      docker build . --tag armdocker.rnd.ericsson.se/${SERVICE_REPO_PATH}/${SERVICE_NAME}:${SERVICE_TAG} --build-arg JAR_FILE=${SERVICE_NAME}-${SERVICE_TAG}.jar --build-arg CBOS_IMAGE_REPO=${CBOS_IMAGE_REPO} --build-arg CBOS_IMAGE_NAME=${CBOS_IMAGE_NAME} --build-arg CBOS_IMAGE_TAG=${CBOS_IMAGE_TAG} > /dev/null 2>&1
  fi

  checkExitCode "Failed to Build Docker Image"

  if [[ "$REMOTE" = true ]]; then
    log "Pushing Docker image to $SERVICE_REPO_PATH"

    if [[ $VERBOSE = true ]]; then
        docker push armdocker.rnd.ericsson.se/${SERVICE_REPO_PATH}/${SERVICE_NAME}:${SERVICE_TAG}
      else
        echo "Pushing image to ${SERVICE_REPO_PATH}.."
        docker push armdocker.rnd.ericsson.se/${SERVICE_REPO_PATH}/${SERVICE_NAME}:${SERVICE_TAG} > /dev/null 2>&1
    fi
    checkExitCode "Failed to Push Docker Image"
  fi
}

function installService() {
  log "Installing $SERVICE_NAME"
  kubectl create ns ${NS}
  checkExitCode "Failed to create Namespace '${NS}'"

  kubectl create secret docker-registry armdocker --docker-server=armdocker.rnd.ericsson.se --docker-username=${ARTIFACTORY_USER} --docker-password=${ARTIFACTORY_PASS} -n ${NS}
  checkExitCode "Failed to create Secret 'armdocker'"

  INSTALL_COMMAND="upgrade --install ${SERVICE_NAME}-release "${PROJECT_ROOT}/charts/${SERVICE_NAME}" --namespace ${NS} ${SERVICE_VALUES},ncmp.url=${HOST}${GATEWAY_HOST},ncmp.host=${GATEWAY_HOST},ncmp.credentials.login=${GATEWAY_USER},ncmp.credentials.password=${GATEWAY_PASSWORD},global.log.streamingMethod=indirect --timeout 5m0s --wait"
  echo -e "\nhelm " ${INSTALL_COMMAND}
  helm ${INSTALL_COMMAND}
  checkExitCode "Failed to install $SERVICE_NAME"
}

function getUserEncryptedPassword() {
  ARTIFACTORY_PASS=$(<helm-test-local/artifactorypw)
  if [ -z "${ARTIFACTORY_PASS}" ]; then
    echo "Enter your artifactory password:"
    read -p 'Artifactory Password: ' ARTIFACTORY_PASS
  fi
}

function readGatewayInputs() {

  while [[ $GATEWAY_HOST = "" ]]; do
    echo "Enter your gateway host:"
    read -p 'Gateway host e.g ("th.eiap.hall131-x1.ews.gic.ericsson.se"): ' GATEWAY_HOST
  done

  while [[ $GATEWAY_USER = "" ]]; do
    echo "Enter your gateway user:"
    read -p 'Gateway user e.g ("cps-user"): ' GATEWAY_USER
  done

  while [[ $GATEWAY_PASSWORD = "" ]]; do
    echo "Enter your gateway password:"
    read -p 'Gateway password e.g ("DefaultP12345!"): ' GATEWAY_PASSWORD
  done

}

function userInput() {
  local _prompt _default _response

  _prompt=$1

  while true; do
    read -r -p "$_prompt " _response
    case "$_response" in
    [Yy][Ee][Ss] | [Yy])
      echo "Yes"
      return 1
      ;;
    [Nn][Oo] | [Nn])
      echo "No"
      return 0
      ;;
    *) # Anything else (including a blank) is invalid.
      ;;
    esac
  done
}

function readArgs(){
  while getopts mcvr flag
  do
    case "${flag}" in
      c) CLEAN=true;;
      v) VERBOSE=true;;
      m) MAVEN_BUILD=false;;
      r) REMOTE=true;;
    esac
  done
}

#########
# main
#########

readArgs $@

echo "Running with: MAVEN_BUILD: " $MAVEN_BUILD ", REMOTE" $REMOTE ", CLEAN_AFTER" $CLEAN ", VERBOSE" $VERBOSE

if [[ "$REMOTE" = false ]]; then
  SERVICE_VALUES="${SERVICE_VALUES},appArmorProfile.type=unconfined"
  echo "Running Locally, with service_values: " $SERVICE_VALUES
fi

getUserEncryptedPassword
readGatewayInputs

SECONDS=0
ensureOnCorrectServer
cleanUpHelmEnvironment

updateEaccCharts
buildDockerImage
installService

if [[ "$CLEAN" = true ]]; then
  cleanUpHelmEnvironment
fi

duration=$SECONDS
echo -e "\nScript took - $(($duration / 60)) minutes and $(($duration % 60)) seconds to run."
