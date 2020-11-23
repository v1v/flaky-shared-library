@Library('shared-library@master') _

pipeline {
  agent { label 'linux' }
  options {
    buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '20'))
    timestamps()
    ansiColor('xterm')
    disableResume()
    durabilityHint('PERFORMANCE_OPTIMIZED')
  }
  stages {
    stage('Test') {
      steps {
        sh './mvnw clean test'
      }
      post {
        always {
          junit(allowEmptyResults: true, keepLongStdio: true, testResults: "target/surefire-reports/*.xml")
        }
      }
    }
  }
  post {
    cleanup {
      sh "curl http://localhost:8080/blue/rest/organizations/jenkins/pipelines/${JOB_NAME}/runs/${BUILD_NUMBER}/tests/?status=FAILED -o tests-errors.json"
      sh "curl http://localhost:8080/blue/rest/organizations/jenkins/pipelines/${JOB_NAME}/runs/${BUILD_NUMBER}/blueTestSummary/ -o tests-summary.json"
      analyzeFlaky(flakyReportIdx: 'reporter-flaky',
                    es: 'localhost:9200',
                    testsErrors: readJSON(file: 'tests-errors.json'),
                    testsSummary: readJSON(file: 'tests-summary.json'))
    }
  }
}
