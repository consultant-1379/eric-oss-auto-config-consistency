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

#Usage:
# Pre-requisite:
  # Set environment variable TEST_PHASE=PRE_ONBOARDING
  # Run run_k6_end2end_staging.sh
# Get all nodes:
#   sh scope_file_generator.sh
#   will generate an LTE and NR scope file with all nodes available in NCMP for each
# Get specific number of nodes
#   sh scope_file_generator.sh <number of nodes required>
#   e.g. sh scope_file_generator.sh 3
#   will generate an LTE and NR scope file with 3 nodes each
echo "#####################################################################"
numberOfNodesRequested=$1
LTE_MANGED_ELEMENTS_GREP="LTE Managed Elements"
NR_MANGED_ELEMENTS_GREP="NR Managed Elements"
LTE="LTE"
NR="NR"
lteFilter=("LTE74" "LTE63" "LTE49" "LTE52")
nrFilter=("NR01")
cd ${WORKSPACE}

buildFdnsAndWriteToFile(){
  echo "Building $1 FDNs and writing to file"
  number_of_nodes_requested_counter=0;
  printf "fdn\n" > $1ScopeFile.csv
  for item in "$@"; do
      if(echo $item | grep "dnPrefix" | grep -q "$nodeType"); then
        json=$(echo "$item" | awk -F '}}' '{print $1"}}"}')
        id=$(echo "$json" | jq -r '.id')
        dnPrefix=$(echo "$json" | jq -r '.attributes.dnPrefix')
        printf "\"$dnPrefix,ManagedElement=$id\"\n" >> $1ScopeFile.csv
        ((number_of_nodes_requested_counter++))
      fi

      if [ "$number_of_nodes_requested_counter" == "$numberOfNodesRequested" ]; then
        break
      fi
  done

  number_of_nodes_written=$(cat $1ScopeFile.csv | wc -l)
  #First line is not an FDN
  ((number_of_nodes_written--))
  echo "$number_of_nodes_written $1 nodes successfully written to $1ScopeFile.csv"
}

requestScopes(){
  nodeTypeRequested=$1
  nodeType=$2
  echo "Requesting $nodeType Managed Elements"
  MANAGED_ELEMENTS=$(cat managed_elements.txt | grep -i "$nodeTypeRequested" | tr -d '\\')
  position=$(expr index "$MANAGED_ELEMENTS" "[")
  MANAGED_ELEMENTS_JSON="${MANAGED_ELEMENTS:$position-1}"
  delimiter="["
  IFS=$delimiter read -ra managedElementsArray <<< "$MANAGED_ELEMENTS_JSON"
  buildFdnsAndWriteToFile $nodeType ${managedElementsArray[@]}
}

lteFilter()
{
  for i in "${lteFilter[@]}"
  do
    tail -n+2 "$LTE"ScopeFile.csv | grep $i >> "$LTE"ScopeFileUpdated.csv
  done
}

nrFilter()
{
  for i in "${nrFilter[@]}"
  do
    tail -n+2 "$NR"ScopeFile.csv | grep -v $i >> "$NR"ScopeFileUpdated.csv
  done
}

printStatusMessage(){
   echo "# scope_file_generator execution result code: $exit_status (non-zero means a problem occurred, otherwise successful execution)"
   echo "###########################################################"
}

checkStatus(){
  exit_status=$?
  [ $exit_status -ne 0 ] && {
  printStatusMessage
    exit 1;
  }
}

requestScopes "$LTE_MANGED_ELEMENTS_GREP" $LTE
checkStatus
requestScopes "$NR_MANGED_ELEMENTS_GREP" $NR
checkStatus
lteFilter
nrFilter
chmod 777 ./k6/src/test/resources/load_test_scope.csv
echo "fdn" > ./k6/src/test/resources/load_test_scope.csv
cat "$LTE"ScopeFileUpdated.csv >> ./k6/src/test/resources/load_test_scope.csv
cat "$NR"ScopeFileUpdated.csv >> ./k6/src/test/resources/load_test_scope.csv
echo "SUCCESS"
printStatusMessage
exit 0

}