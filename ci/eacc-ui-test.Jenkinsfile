#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/ui_local_ruleset.yaml"

stage('EACC UI Test') {
     script {
            ansiColor('xterm') {
                sh "${bob} -r ${ruleset} test"
                sh "${bob} -r ${ruleset} delete-images:delete-ui-test-image"
            }
        }
}