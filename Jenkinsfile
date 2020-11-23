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
          junit(allowEmptyResults: true, keepLongStdio: true, testResults: "target/surefire-reports/junit-*.xml")
        }
      }
    }
  }
  post {
    cleanup {
        // TODO fetch tests errors
        // TODO fetch testsSummary
      analyzeFlaky(flakyReportIdx: 'reporter-flaky',
                    es: 'localhost:9200',
                    testsErrors: '',
                    testsSummary: '')
    }
  }
}
