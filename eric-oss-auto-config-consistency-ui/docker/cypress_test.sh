#!/bin/bash
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

npx wiremock --port 8080 --root-dir /eacc_client/wiremock --verbose --enable-stub-cors &
sleep 5
npm start -- --port 4200 &
echo "EACC UI starting up"
sleep 35
cd integration/contract_testing
npx cypress run --spec ./cypress/e2e