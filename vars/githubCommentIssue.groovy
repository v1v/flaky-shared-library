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

/**
  Comment an existing GitHub issue

  NOTE: It uses hub. No supported yet by gh see https://github.com/cli/cli/issues/517

  // Add a new comment to the issue 123 using the REPO_NAME and ORG_NAME env variables
  githubCommentIssue(id: 123, comment: 'My new comment')

  // Add a new comment to the issue 123 from foo/repo
  githubCommentIssue(org: 'foo', repo: 'repo', id: 123, comment: 'My new comment')
*/

import groovy.transform.Field

@Field def hubLocation = ''

def call(Map args = [:]) {
  if(!isUnix()) {
    error 'githubCommentIssue: windows is not supported yet.'
  }
  def comment = args.containsKey('comment') ? normalise(args.comment) : error('githubCommentIssue: comment argument is required.')
  def credentialsId = args.get('credentialsId', 'Token')
  def id = args.containsKey('id') ? args.id : error('githubCommentIssue: id argument is required.')
  def org = args.containsKey('org') ? args.org : env.ORG_NAME
  def repo = args.containsKey('repo') ? args.repo : env.REPO_NAME

  if (!org?.trim() || !repo?.trim()) {
    error("githubCommentIssue: org/repo are empty ${org}/${repo}. Please use the org and repo arguments or set the ORG_NAME and REPO_NAME variables (if you use the gitCheckout step then it should be there)")
  }

  if (hubLocation?.trim()) {
    echo 'githubCommentIssue: get the hubLocation from cache.'
  } else {
    echo 'githubCommentIssue: set the hubLocation.'
    hubLocation = pwd(tmp: true)
  }

  withEnv(["PATH+HUB=${hubLocation}"]) {
    downloadInstallerIfNeeded(hubLocation)
    withCredentials([string(credentialsId: "${credentialsId}", variable: 'GITHUB_TOKEN')]) {
      return sh(label: 'Comment GitHub issue', script: "hub api repos/${org}/${repo}/issues/${id}/comments -f body='${comment}'", returnStdout: true).trim()
    }
  }
}

/**
* Ensure ' is replaced to avoid any issues when using the markdown templating.
*/
def normalise(v) {
  if (v instanceof String) {
    return v?.toString()?.replaceAll("'",'"')
  }
  return v
}

def downloadInstallerIfNeeded(where) {
  def url = 'https://github.com/github/hub/releases/download/v2.14.2/hub-linux-amd64-2.14.2.tgz'
  def tarball = 'hub.tar.gz'
  def isHubInstalled = sh(script: 'hub --version', returnStatus: true)
  if(isHubInstalled == 0) {
    echo 'githubCommentIssue: hub is available.'
    return
  }
  if(isInstalled(tool: 'wget', flag: '--version')) {
    dir(where) {
      sh(label: 'download hub', script: "wget -q -O ${tarball} ${url}")
      sh(label: 'untar hub', script: "tar -xpf ${tarball} --strip-components=2")
    }
  } else {
    echo'githubCommentIssue: wget is not available. hub will not be installed then.'
  }
}
