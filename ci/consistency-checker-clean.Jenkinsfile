#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"

stage('Consistency Checker Clean') {
    sh "${bob} -r ${ruleset} clean"
}