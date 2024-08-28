#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/ui_local_ruleset.yaml"

stage('EACC UI Build') {
    script {
            sh "${bob} -r ${ruleset} build"
        }
}