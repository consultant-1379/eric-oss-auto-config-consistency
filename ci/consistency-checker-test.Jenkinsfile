#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"

stage('Consistency Checker Test') {
    script {
        withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
            sh "${bob} -r ${ruleset} test"
        }
    }
}