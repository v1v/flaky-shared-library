// Licensed to Elasticsearch B.V. under one or more contributor
// license agreements. See the NOTICE file distributed with
// this work for additional information regarding copyright
// ownership. Elasticsearch B.V. licenses this file to you under
// the Apache License, Version 2.0 (the "License"); you may
// not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import groovy.text.StreamingTemplateEngine

def call(Map args = [:]) {
  def es = args.containsKey('es') ? args.es : error('analyzeFlakey: es parameter is not valid')
  def secret = args.containsKey('es_secret') ? args.es_secret : null
  def flakyReportIdx = args.containsKey('flakyReportIdx') ? args.flakyReportIdx : error('analyzeFlakey: flakyReportIdx parameter is not valid')
  def testsErrors = args.containsKey('testsErrors') ? args.testsErrors : []
  def flakyThreshold = args.containsKey('flakyThreshold') ? args.flakyThreshold : 0.0
  def testsSummary = args.containsKey('testsSummary') ? args.testsSummary : null
  def querySize = args.get('querySize', 500)
  def queryTimeout = args.get('queryTimeout', '20s')

  def labels = 'flaky-test,ci-reported'
  def flakyTestsWithIssues = [:]
  def genuineTestFailures = []

  if (!flakyReportIdx?.trim()) {
    error 'analyzeFlakey: did not receive flakyReportIdx data'
  }

  // Only if there are test failures to analyse
  if(testsErrors.size() > 0) {

    // Query only the test_name field since it's the only used and don't want to overkill the
    // jenkins instance when using the toJSON step since it reads in memory the json response.
    // for 500 entries it's about 2500 lines versus 8000 lines if no filter_path
    def query = "/${flakyReportIdx}/_search?size=${querySize}&filter_path=hits.hits._source.test_name,hits.hits._index"
    def flakeyTestsRaw = sendDataToElasticsearch(es: es,
                                                secret: secret,
                                                data: queryFilter(queryTimeout, flakyThreshold),
                                                restCall: query)
    def flakeyTestsParsed = toJSON(flakeyTestsRaw)

    // Normalise both data structures with their names
    // Intesection what tests are failing and also scored as flaky.
    // Subset of genuine test failures, aka, those failures that were not scored as flaky previously.
    def testFailures = testsErrors.collect { it.name }
    def testFlaky = flakeyTestsParsed?.hits?.hits?.collect { it['_source']['test_name'] }
    def foundFlakyList = testFlaky?.size() > 0 ? testFailures.intersect(testFlaky) : []
    genuineTestFailures = testFailures.minus(foundFlakyList)
    log(level: 'DEBUG', text: "analyzeFlakey: Flaky tests raw: ${flakeyTestsRaw}")
    log(level: 'DEBUG', text: "analyzeFlakey: Flaky matched tests: ${foundFlakyList.join('\n')}")

    def tests = lookForGitHubIssues(flakyList: foundFlakyList, labelsFilter: labels)
    // To avoid creating a few dozens of issues, let's say we won't create more than 3 issues per build
    def numberOfSupportedIssues = 3
    def numberOfCreatedtedIssues = 0
    tests.each { k, v ->
      def issue = v
      def issueDescription = buildTemplate([
          "template": 'flaky-github-issue.template',
          "testName": k,
          "jobUrl": env.BUILD_URL,
          "PR": env.CHANGE_ID?.trim() ? "#${env.CHANGE_ID}" : '',
          "commit": env.GIT_BASE_COMMIT?.trim() ?: '',
          "testData": testsErrors?.find { it.name.equals(k) }])
      if (v?.trim()) {
        try {
          issueWithoutUrl = v.startsWith('https') ? v.replaceAll('.*/', '') : v
          githubCommentIssue(id: issueWithoutUrl, comment: issueDescription)
        } catch(err) {
          log(level: 'WARN', text: "Something bad happened when commenting the issue '${v}'. See: ${err.toString()}")
        }
      } else {
        def title = "Flaky Test [${k}]"
        try {
          if (numberOfCreatedtedIssues < numberOfSupportedIssues) {
            retryWithSleep(retries: 2, seconds: 5, backoff: true) {
              issue = githubCreateIssue(title: title, description: issueDescription, labels: labels)
            }
            numberOfCreatedtedIssues++
          } else {
            log(level: 'INFO', text: "'${title}' issue has not been created since ${numberOfSupportedIssues} issues has been created.")
          }
        } catch(err) {
          log(level: 'WARN', text: "Something bad happened when creating '${title}' issue. See: ${err.toString()}")
          issue = ''
        } finally {
          if(!issue?.trim()) {
            issue = ''
          }
        }
      }
      flakyTestsWithIssues[k] = issue
    }
  }

  // Decorate comment
  def body = buildTemplate([
    "template": 'flaky-github-comment-markdown.template',
    "flakyTests": flakyTestsWithIssues,
    "jobUrl": env.BUILD_URL,
    "testsErrors": genuineTestFailures,
    "testsSummary": testsSummary
  ])
  githubPrComment(commentFile: 'flaky.id', message: body)
  return body
}

def queryFilter(timeout, flakyThreshold) {
  return """{
                "timeout": "${timeout}",
                "sort" : [
                  { "timestamp" : "desc" },
                  { "test_score" : "desc" }
                ],
                "query" : {
                  "range" : {
                    "test_score" : {
                      "gt" : ${flakyThreshold}
                    }
                  }
                }
              }"""
}