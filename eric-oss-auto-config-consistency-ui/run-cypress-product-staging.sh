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

# Usage
# run-cypress-product-staging.sh -h gas.stsvp1eic28.stsoss.sero.gic.ericsson.se

DEFAULT_HOST_NAME="http://host.docker.internal:4200"
DEFAULT_USER_NAME="eacc-admin"
DEFAULT_PASSWORD="idunEr!css0n"
DEFAULT_BUILD_NUMBER="619"

HOST_NAME=${DEFAULT_HOST_NAME}
USER_NAME=${DEFAULT_USER_NAME}
PASSWORD=${DEFAULT_PASSWORD}
BUILD_NUMBER=${DEFAULT_BUILD_NUMBER}

while getopts ":h:u:p:b:" option; do
  case $option in
    h)
      HOST_NAME="$OPTARG"
      ;;
    u)
      USER_NAME="$OPTARG"
      ;;
    p)
      PASSWORD="$OPTARG"
      ;;
    b)
      BUILD_NUMBER="$OPTARG"
      ;;
    *)
      echo "Usage: $0 [-h host_name] [-u user_name] [-p password] [-b build_number]"
      exit 1
      ;;
  esac
done

export MSYS_NO_PATHCONV=1
export WORKSPACE="$(dirname "$(pwd)")"
export INGRESS_HOST=${HOST_NAME}
export INGRESS_LOGIN_USER=${USER_NAME}
export INGRESS_LOGIN_PASSWORD=${PASSWORD}
export BUILD_NUMBER=${BUILD_NUMBER}

echo "############################################################################"
echo "# WORKSPACE:              ${WORKSPACE}"
echo "# INGRESS_HOST:           ${INGRESS_HOST}"
echo "# INGRESS_LOGIN_USER:     ${INGRESS_LOGIN_USER}"
echo "# INGRESS_LOGIN_PASSWORD: ${INGRESS_LOGIN_PASSWORD}"
echo "# BUILD_NUMBER:           ${BUILD_NUMBER}"
echo "############################################################################"

./docker/cypress_product_staging.sh