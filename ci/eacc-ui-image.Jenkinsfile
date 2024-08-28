#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/ui_local_ruleset.yaml"
def ci_ruleset = "ci/common_ruleset2.0.yaml"

stage('EACC UI Image') {
    script {
            sh "${bob} -r ${ruleset} image"
            sh "${bob} -r ${ruleset} image-dr-check"

            if (env.RELEASE) {
                // Don't publish yet until after IDUN-61997 is completed
            }
        }
}