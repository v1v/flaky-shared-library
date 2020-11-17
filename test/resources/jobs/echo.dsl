NAME = 'it/echo'
DSL = '''pipeline {
  agent any
  stages {
    stage('echo') {
      steps { echo 'hi' }
    }
  }
}'''

pipelineJob(NAME) {
  definition {
    cps {
      script(DSL.stripIndent())
    }
  }
}

// If required to be triggered automatically
queue(NAME)
