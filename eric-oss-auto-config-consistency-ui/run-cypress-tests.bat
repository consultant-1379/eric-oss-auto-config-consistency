@REM
@REM COPYRIGHT Ericsson 2023
@REM
@REM
@REM
@REM The copyright to the computer program(s) herein is the property of
@REM
@REM Ericsson Inc. The programs may be used and/or copied only with written
@REM
@REM permission from Ericsson Inc. or in accordance with the terms and
@REM
@REM conditions stipulated in the agreement/contract under which the
@REM
@REM program(s) have been supplied.
@REM

@ECHO OFF

for /f %%i in (..\VERSION_PREFIX) do SET STUBS_VERSION=%%i-SNAPSHOT
ECHO Stubs version: %STUBS_VERSION%

ECHO Building Stubs
CMD /c mvn clean spring-cloud-contract:convert -f ..\pom.xml

ECHO Starting Wiremock Server
START "Wiremock" cmd /k npx wiremock --port 8080 --root-dir ..\target\stubs\META-INF\com.ericsson.oss.apps\eric-oss-auto-config-consistency\%STUBS_VERSION%\ --verbose --enable-stub-cors

ECHO Starting EACC UI
START "EACC" npm start -- --port 4200

ECHO Running Cypress Tests
TIMEOUT /t 7
CD .\integration\contract_testing
CMD /c npx cypress run --spec .\cypress\e2e
