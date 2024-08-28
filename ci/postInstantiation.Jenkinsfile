#!/usr/bin/env groovy

pipeline {
    agent {
        label env.SLAVE_LABEL
    }

    parameters {
        string(name: 'GERRIT_REFSPEC',
                defaultValue: 'refs/heads/master',
                description: 'Referencing to a commit by Gerrit RefSpec')
        string(name: 'SLAVE_LABEL',
                defaultValue: 'evo_docker_engine_gic_IDUN',
                description: 'Specify the slave label that you want the job to run on')
        string(name: 'INGRESS_PREFIX',
                defaultValue: '',
                description: 'The prefix to the ingress URL')
        string(name: 'INGRESS_HOST',
                defaultValue: '',
                description: 'The EIC APIGW Host')
        string(name: 'INGRESS_LOGIN_USER',
                defaultValue: '',
                description: 'The user name to use for login')
        string(name: 'INGRESS_CPS_USER',
                defaultValue: '',
                description: 'The user name to use for cps')
        string(name: 'INGRESS_LOGIN_PASSWORD',
                defaultValue: '',
                description: 'The password to use')
        string(name: 'INGRESS_EACC_USER',
                defaultValue: '',
                description: 'The user name to use for eacc')
        string(name: 'INGRESS_EACC_ADMIN',
                defaultValue: '',
                description: 'The user name to use for eacc admin')
        string(name: 'INGRESS_GAS_USER',
                defaultValue: '',
                description: 'The user name to use for gas')
        string(name: 'RESTSIM_HOST',
                defaultValue: 'RESTSIM_HOST',
                description: 'The hostname of restsim')
        string(name: 'NAMESPACE',
                defaultValue: 'eric-eic',
                description: 'The namespace of the environment')
        string(name: 'KUBECONFIG_FILE_CREDENTIAL_ID',
                defaultValue: 'stsossflexeiap1003_kubeconfig',
                description: 'The jenkins credential for the kubeconfig of the environment')
    }

    options {
        timestamps()
        timeout(time: 115, unit: 'MINUTES')
        buildDiscarder(logRotator(daysToKeepStr: '14', numToKeepStr: '40', artifactNumToKeepStr: '40', artifactDaysToKeepStr: '14'))
    }

     environment {
        INGRESS_SCHEMA = "${params.INGRESS_PREFIX}"
        INGRESS_HOST = "${params.INGRESS_HOST}"
        RESTSIM_HOST = "${params.RESTSIM_HOST}"
        INGRESS_LOGIN_USER = "${params.INGRESS_LOGIN_USER}"
        INGRESS_CPS_USER = "${params.INGRESS_CPS_USER}"
        INGRESS_LOGIN_PASSWORD = "${params.INGRESS_LOGIN_PASSWORD}"
        STAGING_LEVEL = "PRODUCT"
        TEST_PHASE = "POST_INSTANTIATION"
        INGRESS_EACC_USER = "${params.INGRESS_EACC_USER}"
        INGRESS_EACC_ADMIN = "${params.INGRESS_EACC_ADMIN}"
        INGRESS_GAS_USER = "${params.INGRESS_GAS_USER}"
        VALIDATE_EACC_RBAC = "true"
        NAMESPACE = "${params.NAMESPACE}"
        KUBECONFIG_FILE_CREDENTIAL_ID = "${params.KUBECONFIG_FILE_CREDENTIAL_ID}"
    }

    // Stage names (with descriptions) taken from ADP Microservice CI Pipeline Step Naming Guideline: https://confluence.lmera.ericsson.se/pages/viewpage.action?pageId=122564754
    stages {
        stage('Clean') {
            steps {
                sh "rm -rf ./.aws ./.kube/ ./.cache/"
                archiveArtifacts allowEmptyArchive: true, artifacts: 'ci/postInstantiation.Jenkinsfile'
            }
        }
        stage('K6 Post Instantiation E2E Tests') {
            steps {
                 withCredentials([file(credentialsId: "$KUBECONFIG_FILE_CREDENTIAL_ID", variable: 'KUBECONFIG_FILE_PATH')]) {
                     sh "chmod 777 ./k6/scripts/run_k6_end2end_staging.sh"
                     sh "./k6/scripts/run_k6_end2end_staging.sh"
                     sh "chmod 777 ./k6/scripts/populate_database.sh"
                     sh "./k6/scripts/populate_database.sh"
                     sh "chmod 777 ./k6/scripts/run_k6_load_end2end_staging.sh"
                     sh "./k6/scripts/run_k6_load_end2end_staging.sh"
                 }
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'doc/Test_Report/k6-test-results.html'
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'doc/Test_Report/k6-load-test-results.html'
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'doc/Test_Report/summary.json'
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'doc/Test_Report/load-test-summary.json'
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'doc/Test_Report/rate-limit-test-summary.json'
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'doc/Test_Report/rate-limit-test-results.html'
                    publishHTML([allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: '',
                        reportFiles: 'doc/Test_Report/k6-test-results.html',
                        reportName: 'K6 Test Results',
                        reportTitles: ''])
                }
            }
        }
        stage('Cypress Post Instantiation UI Tests') {
            steps {
                sh "rm -rf ./eric-oss-auto-config-consistency-ui/integration/product_staging/cypress/results/cypress_tests.json || true"
                sh "chmod 777 ./eric-oss-auto-config-consistency-ui/docker/cypress_product_staging.sh"
                sh "./eric-oss-auto-config-consistency-ui/docker/cypress_product_staging.sh"
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'eric-oss-auto-config-consistency-ui/integration/product_staging/cypress/results/cypress_test_results.html'
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'eric-oss-auto-config-consistency-ui/integration/product_staging/cypress/results/cypress_tests.json'
                }
            }
        }
    }
}

