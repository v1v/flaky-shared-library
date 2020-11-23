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
      fetchData()
      analyzeFlaky(flakyReportIdx: 'reporter-flaky',
                    es: 'localhost:9200',
                    testsErrors: readJSON(file: 'tests-errors.json'),
                    testsSummary: readJSON(file: 'tests-summary.json'))
    }
  }
}

def fetchData() {
  def restURLJob = "http://localhost:8080/blue/rest/organizations/jenkins/pipelines/${JOB_NAME}/"
  def restURLBuild = "${restURLJob}runs/${BUILD_NUMBER}"

  def scriptFile = 'generate-build-data.sh'
  def resourceContent = libraryResource(scriptFile)
  writeFile file: scriptFile, text: resourceContent
  sh(label: 'generate-build-data', returnStatus: true, script: """#!/bin/bash -x
    chmod 755 ${scriptFile}
    ./${scriptFile} ${restURLJob} ${restURLBuild} ${currentBuild.currentResult} ${currentBuild.duration}""")
}