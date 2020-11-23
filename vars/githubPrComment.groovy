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
  Add a comment or edit an existing comment in the GitHub.

  githubPrComment()

  githubPrComment(message: 'foo bar')

*/
def call(Map args = [:]){
  def commentFile = args.get('commentFile', 'comment.id')
  def message = args.containsKey('message') ? args.message : ''

  if (isPR()) {
    // Add some metadata to support the githubPrLatestComment step
    def commentWithMetadata =  message + "\n${metadata(args)}"
    addOrEditComment(commentFile: commentFile, details: commentWithMetadata)
  } else {
    echo 'githubPrComment: is only available for PRs.'
  }
}

def addOrEditComment(Map args = [:]) {
  def commentFile = args.commentFile
  def details = args.details
  def id = getCommentIfAny(args)
  if (id != errorId()) {
    id = editComment(id, details)
  } else {
    id = addComment(details)
  }
  writeFile(file: "${commentFile}", text: "${id}")
  archiveArtifacts(artifacts: commentFile)
}

def addComment(String details) {
  def id
  try {
    id = pullRequest.comment(details)?.id
  } catch (err) {
    echo "githubPrComment: pullRequest.comment failed with message: ${err.toString()}"
  }
  return id
}

def editComment(id, details) {
  echo "githubPrComment: Edit comment with id '${id}'. If comment still exists."
  try {
    pullRequest.editComment(id, details)
  } catch (errorWithEdit) {
    echo "githubPrComment: Edit comment with id '${id}' failed with error '${err.toString()}'. Let's fallback to add a comment."
    id = addComment(details)
  }
  return id
}

def getCommentFromFile(Map args = [:]) {
  def commentFile = args.commentFile
  copyArtifacts(filter: commentFile, flatten: true, optional: true, projectName: env.JOB_NAME, selector: lastWithArtifacts())
  if (fileExists(commentFile)) {
    return readFile(commentFile)?.trim()
  } else {
    return ''
  }
}

/**
  Support search for the comment id.
**/
def getCommentIfAny(Map args = [:]) {
  def commentId = getCommentFromFile(args)
  def id = errorId()
  if (commentId?.trim() && commentId.isInteger()) {
    id = commentId as Integer
  } else {
    echo "githubPrLatestComment: failed. Therefore a new GitHub comment will be created."
  }
  return id
}

def errorId() {
  return -1000
}

def metadata(Map args = [:]){
  // .toString() to avoid org.codehaus.groovy.runtime.GStringImpl issues when comparing Strings.
  return "<!--COMMENT_GENERATED_WITH_ID_${args.commentFile}-->".toString()
}
