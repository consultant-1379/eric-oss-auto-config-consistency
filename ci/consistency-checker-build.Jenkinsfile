#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"

stage('Consistency Checker Build') {
        sh "${bob} -r ${ruleset} build"
}

stage('Static Analysis Checker') {
        sh "${bob} -r ${ruleset} static-analysis"
}
