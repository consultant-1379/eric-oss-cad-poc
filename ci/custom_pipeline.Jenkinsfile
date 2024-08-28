#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"
def ci_ruleset = "ci/common_ruleset2.0.yaml"

stage('Custom Init') {
    sh "${bob} -r ${ruleset} init-precodereview"
}

try {
    stage('Custom Lint') {
        parallel(
            "lint markdown": {
                sh "${bob} -r ${ruleset} lint:markdownlint lint:vale"
            },
            "lint helm": {
                sh "${bob} -r ${ruleset} lint:helm"
            },
            "lint helm design rule checker": {
                sh returnStatus: true, script: "${bob} -r ${ruleset} lint:helm-chart-check"
            },
            "lint code": {
                sh "${bob} -r ${ruleset} lint:license-check"
            },
            "static code analysis": {
                sh "${bob} -r ${ruleset} lint:static-code-analysis"
            }
        )
    }
} catch (e) {
    throw e
} finally {
    archiveArtifacts allowEmptyArchive: true, artifacts: '**/*bth-linter-output.html, **/design-rule-check-report.*'
}

stage('Custom Test') {
    sh "${bob} -r ${ruleset} test"
}

stage('Custom SonarQube') {
    withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]){
        withSonarQubeEnv("${env.SQ_SERVER}") {
            sh "${bob} -r ${ruleset} sonar-enterprise-pcr"
        }
    }
    timeout(time: 5, unit: 'MINUTES') {
        waitUntil {
            withSonarQubeEnv("${env.SQ_SERVER}") {
                script {
                    return true //ci_pipeline_scripts.getQualityGate()
                }
            }
        }
    }
}

stage('Custom Image') {
    sh "${bob} -r ${ruleset} image"
}