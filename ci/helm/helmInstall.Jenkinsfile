#!/usr/bin/env groovy
def bob = "./bob/bob"
def customRulesBob = "${bob} -r ci/helm/helm-install-ruleset.yaml"
def ciBob = "${bob} -r ci/common_ruleset2.0.yaml"
try {
    stage('Helm Install (Custom)') {
        sh "${ciBob} helm-dry-run"
        sh "${customRulesBob} helm-install"
        sh "${ciBob} healthcheck"
    }
} catch (e) {
    throw e
}