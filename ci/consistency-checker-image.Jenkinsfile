#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"
def ci_ruleset = "ci/common_ruleset2.0.yaml"

stage('Consistency Checker Image') {
    sh "${bob} -r ${ruleset} image"
    sh "${bob} -r ${ci_ruleset} image-dr-check"
}