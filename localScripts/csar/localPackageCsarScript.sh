#! /bin/bash
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

declare -r SERVICE_NAME="eric-oss-auto-config-consistency"
declare -r REPO="proj-eric-oss-dev"

CBOS_IMAGE_REPO=$( grep "cbos-image-repo" common-properties.yaml | awk '{print $3}' | sed -e 's/^"//' -e 's/"$//')
CBOS_IMAGE_NAME=$( grep "cbos-image-name" common-properties.yaml | awk '{print $3}' | sed -e 's/^"//' -e 's/"$//')
CBOS_IMAGE_TAG=$( grep "cbos-image-version" common-properties.yaml | awk '{print $3}' | sed -e 's/^"//' -e 's/"$//')

USER_ID=$( echo "$(whoami)" | tr '[:upper:]' '[:lower:]' | awk -F'@' '{print $1}'| awk -F'+' '$2 != "" {print $2;next};{print $1}')

SERVICE_NAME_SIGNUM="${SERVICE_NAME}-${USER_ID}"

VERSION=$(cat ./VERSION_PREFIX)
IMAGE_VERSION="$VERSION-$USER_ID"

GATEWAY_URL="eacc-$USER_ID"
CSAR_VERSION="$VERSION-0"

WORKDIR=$(pwd)
TEMP_WORKDIR="$WORKDIR/tmp"

MAVEN_BUILD=false
UPDATE_HELM_DEPENDENCIES=false
CLEAN=false
VERBOSE=false
SLIMMED_CSAR=false

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

function buildImage {
  if [[ $MAVEN_BUILD = true ]]; then
    export SELI_ARTIFACTORY_REPO_USER=${USER_ID} && export SELI_ARTIFACTORY_REPO_PASS=${ARTIFACTORY_PASS}
    log "Installing maven dependencies"
    if [[ $VERBOSE = true ]]; then
        mvn clean install -Dmaven.test.skip=true
      else
        echo "Running Maven Build.."
        mvn clean install -Dmaven.test.skip=true > /dev/null 2>&1
    fi
    checkExitCode "Failed to Build Maven Project"
  fi
  cp target/*SNAPSHOT.jar target/$SERVICE_NAME-$IMAGE_VERSION.jar

  log "Building Docker Image $REPO/$SERVICE_NAME:$IMAGE_VERSION"
  if [[ $VERBOSE = true ]]; then
      docker build . --tag armdocker.rnd.ericsson.se/$REPO/$SERVICE_NAME:$IMAGE_VERSION --build-arg JAR_FILE=$SERVICE_NAME-$IMAGE_VERSION.jar --build-arg CBOS_IMAGE_REPO=${CBOS_IMAGE_REPO} --build-arg CBOS_IMAGE_NAME=${CBOS_IMAGE_NAME} --build-arg CBOS_IMAGE_TAG=${CBOS_IMAGE_TAG}
    else
      echo "Running Docker Build.."
      docker build . --tag armdocker.rnd.ericsson.se/$REPO/$SERVICE_NAME:$IMAGE_VERSION --build-arg JAR_FILE=$SERVICE_NAME-$IMAGE_VERSION.jar --build-arg CBOS_IMAGE_REPO=${CBOS_IMAGE_REPO} --build-arg CBOS_IMAGE_NAME=${CBOS_IMAGE_NAME} --build-arg CBOS_IMAGE_TAG=${CBOS_IMAGE_TAG} > /dev/null 2>&1
  fi

  log "Building Docker UI Image $REPO/$SERVICE_NAME-audit-gui:$IMAGE_VERSION"
  sed -i "s/const API_GATEWAY_URL = '\/eacc';/const API_GATEWAY_URL = '\/$GATEWAY_URL';/" "$TEMP_WORKDIR/eric-oss-auto-config-consistency-ui/src/utils/restCallUtils.js";
  if [[ $VERBOSE = true ]]; then
      docker build . -f "$TEMP_WORKDIR/eric-oss-auto-config-consistency-ui/docker/Dockerfile" --tag armdocker.rnd.ericsson.se/$REPO/$SERVICE_NAME-audit-gui:$IMAGE_VERSION --build-arg CBOS_IMAGE_REPO=${CBOS_IMAGE_REPO} --build-arg CBOS_IMAGE_NAME=${CBOS_IMAGE_NAME} --build-arg CBOS_IMAGE_TAG=${CBOS_IMAGE_TAG} --build-arg UI_DIRECTORY="tmp/eric-oss-auto-config-consistency-ui"
    else
      echo "Running Docker Build for UI.."
      docker build . -f "$TEMP_WORKDIR/eric-oss-auto-config-consistency-ui/docker/Dockerfile" --tag armdocker.rnd.ericsson.se/$REPO/$SERVICE_NAME-audit-gui:$IMAGE_VERSION --build-arg CBOS_IMAGE_REPO=${CBOS_IMAGE_REPO} --build-arg CBOS_IMAGE_NAME=${CBOS_IMAGE_NAME} --build-arg CBOS_IMAGE_TAG=${CBOS_IMAGE_TAG} --build-arg UI_DIRECTORY="tmp/eric-oss-auto-config-consistency-ui"> /dev/null 2>&1
  fi

  checkExitCode "Failed to Build Docker Image"
}

copyAndReplaceUiConfigFiles(){
  TEMP_UI_DIR="$TEMP_WORKDIR/eric-oss-auto-config-consistency-ui"
  cp -rf $WORKDIR/localScripts/ui-resources/config.json $TEMP_UI_DIR/public
  cp -rf $WORKDIR/localScripts/ui-resources/config.package.json $TEMP_UI_DIR/public
  cp -rf $WORKDIR/localScripts/ui-resources/navMenuUtils.js $TEMP_UI_DIR/src/utils
  cp -rf $WORKDIR/localScripts/ui-resources/routeUtils.js $TEMP_UI_DIR/src/utils
  cp -rf $WORKDIR/localScripts/ui-resources/appNames.js $TEMP_UI_DIR/src/utils

  sed -i "s/<SIGNUM>/$USER_ID/g"  $TEMP_UI_DIR/public/config.json
  sed -i "s/<SIGNUM>/$USER_ID/g"  $TEMP_UI_DIR/public/config.package.json
  sed -i "s/<SIGNUM>/$USER_ID/g"  $TEMP_UI_DIR/src/utils/navMenuUtils.js
  sed -i "s/<SIGNUM>/$USER_ID/g"  $TEMP_UI_DIR/src/utils/routeUtils.js
  sed -i "s/<SIGNUM>/$USER_ID/g"  $TEMP_UI_DIR/src/utils/appNames.js
}

function updateEaccCharts() {
    log "Pulling Chart dependencies ${SERVICE_NAME}"
    helm dependency update "$TEMP_WORKDIR/charts/${SERVICE_NAME}" 2>&1 | grep -v "skipping loading invalid"
}

function packageAppIntoCsar {
  mkdir -p "$TEMP_WORKDIR/output"

  cp -r "$WORKDIR/ci/csar_additional_values/" "$TEMP_WORKDIR/"
  cp -r "$WORKDIR/ci/csar_template/" "$TEMP_WORKDIR/"
  cp -r "$WORKDIR/charts/" "$TEMP_WORKDIR/"
  mkdir -p "$TEMP_WORKDIR/eric-oss-auto-config-consistency-ui"
  find "$WORKDIR/eric-oss-auto-config-consistency-ui" -mindepth 1 -maxdepth 1 -not -wholename '*node_modules*' -and -not -wholename '*build*' -exec cp -r {} "$TEMP_WORKDIR/eric-oss-auto-config-consistency-ui" \;
  copyAndReplaceUiConfigFiles
  if [[ $VERBOSE = true ]]; then
      dos2unix "$WORKDIR/entrypoint.sh"
      find "$TEMP_WORKDIR/" -type f -print0 | xargs -0 dos2unix --
  else
      dos2unix "$WORKDIR/entrypoint.sh"
      find "$TEMP_WORKDIR/" -type f -print0 | xargs -0 dos2unix -- > /dev/null 2>&1
  fi

  buildImage

  log "Populating Chart and CSAR metadata"
  # Updating VERSION in template
  sed -i "s/VERSION/$IMAGE_VERSION/" "$TEMP_WORKDIR/charts/$SERVICE_NAME/eric-product-info.yaml"
  sed -i "s/REPO_PATH/$REPO/" "$TEMP_WORKDIR/charts/$SERVICE_NAME/eric-product-info.yaml"
  sed -i "s/VERSION/$CSAR_VERSION/" "$TEMP_WORKDIR/csar_template/OtherDefinitions/ASD/eric-oss-auto-config-consistencyASD.yaml"
  sed -i "s/VERSION/$CSAR_VERSION/" "$TEMP_WORKDIR/csar_template/Definitions/AppDescriptor.yaml"

  # Update service to service-signum
  sed -i "s/APPName: $SERVICE_NAME/APPName: $SERVICE_NAME_SIGNUM/" "$TEMP_WORKDIR/csar_template/Definitions/AppDescriptor.yaml"
  sed -i "s/NameofComponent: $SERVICE_NAME/NameofComponent: $SERVICE_NAME_SIGNUM/" "$TEMP_WORKDIR/csar_template/Definitions/AppDescriptor.yaml"

  MSYS_NO_PATHCONV=1 docker run --rm -v "${TEMP_WORKDIR}":/workdir mikefarah/yq -i \
    "
      .name = \"${SERVICE_NAME_SIGNUM}\"
    " charts/$SERVICE_NAME/Chart.yaml

  MSYS_NO_PATHCONV=1 docker run --rm -v "${TEMP_WORKDIR}":/workdir mikefarah/yq -i \
    "
      .deploymentItems.artifactId = \"OtherDefinitions/ASD/${SERVICE_NAME_SIGNUM}-${CSAR_VERSION}.tgz\"
    " csar_template/OtherDefinitions/ASD/eric-oss-auto-config-consistencyASD.yaml

  # Update values.yaml name override and postgres overrides
   MSYS_NO_PATHCONV=1 docker run --rm -v "${TEMP_WORKDIR}":/workdir mikefarah/yq -i \
      "
        .nameOverride = \"eacc-${USER_ID}\" |
        .fullnameOverride = \"eacc-${USER_ID}\" |
        .apiGateway.route.pathPrefix = \"/${GATEWAY_URL}\" |
        .eric-oss-auto-config-consistency-pg.nameOverride = \"eacc-${USER_ID}-pg\" |
        .eric-oss-auto-config-consistency-pg.host = \"eacc-${USER_ID}-pg\" |
        .eric-oss-auto-config-consistency-pg.credentials.kubernetesSecretName = \"eacc-${USER_ID}-postgres-secret\" |
        .eric-oss-auto-config-consistency-pg.brAgent.backupDataModelConfig = \"eacc-${USER_ID}-backup\"
      " charts/$SERVICE_NAME/values.yaml


  if [[ $UPDATE_HELM_DEPENDENCIES = true ]]; then
    updateEaccCharts
  fi

  log "Packaging helm chart"
  helm package --version $CSAR_VERSION "$TEMP_WORKDIR/charts/$SERVICE_NAME" -d "$TEMP_WORKDIR/csar_template/OtherDefinitions/ASD"

  checkExitCode "Failed to Package helm chart"

  bundleImages

  packageCsar
}

function packageCsar {
  log "Packaging csar file to $TEMP_WORKDIR/output"

  MSYS_NO_PATHCONV=1 docker run --init --rm \
    --volume "$TEMP_WORKDIR/output":/tmp/csar/ \
    --volume "$HOME"/.docker:/root/.docker \
    --volume /var/run/docker.sock:/var/run/docker.sock \
    --workdir /target \
    --volume "$TEMP_WORKDIR/csar_template":/target \
    armdocker.rnd.ericsson.se/proj-eric-oss-dev-test/releases/eric-oss-app-package-tool:latest \
    generate --tosca /target/Metadata/Tosca.meta \
    --name "$SERVICE_NAME_SIGNUM-$CSAR_VERSION" \
    --images /tmp/csar/docker.tar \
    --helm3 \
    --output /tmp/csar

    checkExitCode "Failed to Package csar"

    echo "$SERVICE_NAME_SIGNUM-$CSAR_VERSION.csar saved"
}

function bundleImages() {
  local images
  images=$(helm template "$TEMP_WORKDIR/charts/$SERVICE_NAME/" -f "$TEMP_WORKDIR/csar_additional_values/site-values.yaml" | grep image: | awk '{ print $2 }' | uniq)
  log "Bundling images"
  echo "Images in chart:"
  echo "$images"
  echo

  if [[ "$SLIMMED_CSAR" == true ]]; then
    images=$(echo "$images" | grep "$SERVICE_NAME")
    echo "Only including EACC docker images in CSAR:"
    echo "$images"
    echo
  fi

  for image in $images
  do
    if [[ "$(docker images -q "$image" 2> /dev/null)" == "" ]]; then
      echo "Image: $image is not present. Pulling $image...";
      docker pull $image;
      echo
    fi
    echo "Retagging: $image to $(echo "${image//armdocker.rnd.ericsson.se\//}")"
    docker tag $image "$(echo "${image//armdocker.rnd.ericsson.se\//}")"
  done
  echo
  echo "Saving images to tar"
  docker save $(echo "${images//armdocker.rnd.ericsson.se\//}" | tr '\n' ' ') -o "$TEMP_WORKDIR/output/docker.tar"

  checkExitCode "Failed to bundle images"
}

function clearLocalImages(){
  docker rmi -f armdocker.rnd.ericsson.se/$REPO/$SERVICE_NAME:$IMAGE_VERSION
  docker rmi -f $REPO/$SERVICE_NAME:$IMAGE_VERSION
  docker rmi -f armdocker.rnd.ericsson.se/$REPO/$SERVICE_NAME-audit-gui:$IMAGE_VERSION
  docker rmi -f $REPO/$SERVICE_NAME-audit-gui:$IMAGE_VERSION
}

function readArgs(){
  while getopts mcvus flag
  do
    case "${flag}" in
      c) CLEAN=true;;
      v) VERBOSE=true;;
      m) MAVEN_BUILD=true;;
      u) UPDATE_HELM_DEPENDENCIES=true;;
      s) SLIMMED_CSAR=true;;
    esac
  done
}

#########
# main
#########

readArgs $@

echo "Running with: CLEAN_TEMP_WORKDIR (-c):" $CLEAN ", MAVEN_BUILD (-m):" $MAVEN_BUILD ", UPDATE_HELM_DEPENDENCIES (-u)" $UPDATE_HELM_DEPENDENCIES ", VERBOSE (-v)" $VERBOSE ", SLIMMED_CSAR (-s)" $SLIMMED_CSAR

if [[ $CLEAN = true ]]; then
    log "Cleaning temp workdir '$TEMP_WORKDIR'"
    rm -rf "$TEMP_WORKDIR"
    clearLocalImages
    checkExitCode "Failed to clean TEMP_WORKDIR '$TEMP_WORKDIR'"
fi

packageAppIntoCsar
