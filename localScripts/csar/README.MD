# CSAR packaging Readme

## Run CSAR packaging locally

1. Run the following command with the arguments described below from the project root directory.
   ./localScripts/localPackageCsarScript.sh

### Arguments
There are optional 4 Arguments that can be used:
1. -c This will force delete the Temp Workdir (./tmp), Default is OFF
2. -u This will force update the Helm dependencies, Default is OFF
3. -m This will turn on running the maven build, Default is OFF
4. -v This will run with verbose, Default is Non-Verbose
5. -s This will 'slim down' the size of the CSAR and include only EACC docker images, Default is OFF. NOTE: When using the slimmed version, a CSAR with the same Postgres and BRA images must already be onboard. (e.g. A release CSAR)

## Results
1. eric-oss-auto-config-consistency will be packaged to ./tmp/output/eric-oss-auto-config-consistency-<SIGNUM>-<VERSION>.csar

When instantiating the name override is set to eacc-<SIGNUM> by default. 
The EACC endpoints for your instance will be /eacc-<SIGNUM>

## CI/CD

This is **not** used as part of the CI Pipeline, and is to be used for local testing only. CSAR Packaging for auto-apps are handled by Auto-Apps product staging.

Some resources regarding CSAR generation in CI:
* [Auto Apps Test Strategy](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/IDUN/Auto+Apps+Test+Strategy#AutoAppsProductStaging)
* [EIAP Auto Apps Product Staging Pipeline](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/DGBase/EIAP+Auto+Apps+Product+Staging+Pipeline)
* [Job: EIAP-AUTO-APP-CSAR-Builder](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/DGBase/Job%3A+EIAP-AUTO-APP-CSAR-Builder)