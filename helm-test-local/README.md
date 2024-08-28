# Helm Test Readme

## Run helm integration tests (on CCD environment)

1. JAVA_HOME should be set to Java 11 to run the maven build

2. Optional:
Create a file called 'artifactorypw' with your artifactory password, in this directory. gitignore will stop this from being pushed to gerrit.

3. Run the following command with the arguments described below from the project root directory.
    helm-test-local/localHelmTestScript.sh

### Arguments
There are optional 4 Arguments that can be used:
1. -r This will run the script on a remote server, Default is local
2. -m This will turn off running the maven build, Default is ON
3. -c This will clean up after the server after its finished, Default is Disabled
4. -v This will run with verbose, Default is Non-Verbose

Thus, the default will do a non-verbose maven & docker build, helm install locally and leave it installed. 

For example, to build & install on a remote server use:

helm-test-local/localHelmTestScript.sh -r

## Results
1. eric-oss-auto-config-consistency release will be installed under your signum's Namespace
