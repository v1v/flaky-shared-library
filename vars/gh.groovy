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
  Wrapper to interact with the gh command line. It returns the stdout output.

  // List all the open issues with the label 
  gh(command: 'issue list', flags: [ label: ['flaky-test'], state: 'open' ])

  // Create issue with title and body
  gh(command: 'issue create', flags: [ title: "I found a bug", body: "Nothing works" ])

*/

import groovy.transform.Field

@Field def ghLocation = ''

def call(Map args = [:]) {
  def command = args.containsKey('command') ? args.command : error('gh: command argument is required.')
  def credentialsId = args.get('credentialsId', 'Token')
  def flags = args.get('flags', [:])

  // Use the current location as the git repo otherwise uses the env variables to pass
  // the repo information to the gh command
  def isGitWorkspace = sh(label: 'isGitWorkspace', script: 'git rev-list HEAD -1 1> /dev/null 2>&1', returnStatus: true) == 0
  if (isGitWorkspace) {
    echo 'gh: running within a git workspace.'
  } else {
    echo 'gh: running outside of a git workspace. Using REPO_NAME and ORG_NAME if they are set'
    if (env.REPO_NAME?.trim() && env.ORG_NAME?.trim()) {
      flags['repo'] = "${env.ORG_NAME}/${env.REPO_NAME}"
    }
  }

  if (ghLocation?.trim()) {
    echo 'gh: get the ghLocation from cache.'
  } else {
    echo 'gh: set the ghLocation.'
    ghLocation = pwd(tmp: true)
  }

  withEnv(["PATH+GH=${ghLocation}"]) {
    downloadInstaller(ghLocation)
    withCredentials([string(credentialsId: "${credentialsId}", variable: 'GITHUB_TOKEN')]) {
      def flagsCommand = ''
      if (flags) {
        flags.each { k, v ->
          echo "gh: k ${k} - v ${v}"
          if (v instanceof java.util.ArrayList || v instanceof List) {
            v.findAll { it }.each { value ->
              flagsCommand += "--${k}='${normalise(value)}' "
            }
          } else {
            if (v) {
              flagsCommand += "--${k}='${normalise(v)}' "
            }
          }
        }
      }
      return runCommand(command, flagsCommand)
    }
  }
}

def runCommand(command, flagsCommand) {
  def output = sh(label: "gh ${command}", script: "gh ${command} ${flagsCommand}", returnStdout: true)
  return output
}

def downloadInstaller(where) {
  def url = 'https://github.com/cli/cli/releases/download/v1.2.1/gh_1.2.1_linux_amd64.tar.gz'
  if (isUnix()) {
    def uname = sh script: 'uname', returnStdout: true
    if (uname.startsWith("Darwin")) {
      url = 'https://github.com/cli/cli/releases/download/v1.2.1/gh_1.2.1_macOS_amd64.tar.gz'
    }
  }
  def tarball = 'gh.tar.gz'
  dir(where) {
    sh(label: 'download gh', script: "wget -q -O ${tarball} ${url}")
    sh(label: 'untar gh', script: "tar -xpf ${tarball} --strip-components=2")
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
