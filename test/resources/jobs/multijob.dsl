NAME = 'multijob'
multibranchPipelineJob(NAME) {
  branchSources {
    factory {
      workflowBranchProjectFactory {
        scriptPath('Jenkinsfile')
      }
    }
    branchSource {
      source {
        github {
          id('20200109') // IMPORTANT: use a constant and unique identifier
          credentialsId('UserAndToken')
          repoOwner('v1v')
          repository('flaky-shared-library')
          repositoryUrl('https://github.com/v1v/flaky-shared-library')
          configuredByUrl(false)
          traits {
            gitHubBranchDiscovery {
              strategyId(1)
            }
            gitHubPullRequestDiscovery {
              strategyId(1)
            }
          }
        }
      }
    }
  }
  orphanedItemStrategy {
    discardOldItems {
      numToKeep(20)
    }
  }
}
