## Jenkins Shared library

A shared library with some vitamins to help you to speed up your local development and test the flakiness approach.

## Context

This is an example of a shared library for the Jenkins pipelines based on:

- [JCasC](https://jenkins.io/projects/jcasc/) to configure a local jenkins instance.
- [JobDSL](https://github.com/jenkinsci/job-dsl-plugin/wiki) to configure the pipelines to test the steps.
- [JenkinsUnitPipeline](https://github.com/jenkinsci/JenkinsPipelineUnit) to test the shared library steps.
- [Spock](http://spockframework.org/spock/docs/1.0/introduction.html) to test the shared library steps with some specifications approach.
- [Gradle](https://docs.gradle.org/current/userguide/userguide.html) to orchestrate the build/tests of this library.
- [Vagrant](https://www.vagrantup.com/docs/index.html) and [VirtualBox](https://www.virtualbox.org/wiki/Documentation) to spin up jenkins agents using the Swarm connection.

## System Requirements

- Docker >= 19.x.x
- Docker Compose >= 1.25.0
- Vagrant >= 2.2.4
- VirtualBox >= 6
- Java >= 8
- [Ngrok](https://ngrok.com/)

## Layout

```
(root)
+- src                             # Groovy source files
|   +- Bar.groovy                  # for Bar class
+- test
|   +- groovy
|       +- FooStepTest.groovy      # Tests for the foo step
|   +- resources                   # resource files for the tests
+- vars
|   +- foo.groovy                  # for global 'foo' variable
|   +- foo.txt                     # help for 'foo' variable
+- resources                       # resource files (external libraries only)
|   +- org
|       +- v1v
|           +- bar.json            # static helper data for org.foo.Bar
+- local                           # to enable a jenkins instance with this library
|   +- configs
|   |   +- jenkins.yaml
|   |   +- plugins.txt
|   +- workers
|       +- linux
|           +- Vagrantfile
|   +- docker-compose.yml
|   +- Dockerfile
|   +- Makefile
|
```

## How to test it

```bash
  ./mvnw clean test
```

### How to test it within the local Jenkins instance

1. Add your GitHub user and GitHub token each one in the file `local/configs/user` and `local/configs/key`

2. Build docker image by running:

```bash
  make -C local build
```

3. Start the local Jenkins master service by running:

```bash
  make -C local start
```

4. Browse to <http://localhost:8080> in your web browser.

5. Enable jenkins local instance to be publically accessible

```bash
  make -C local public
```

Pick the https URL

6. Configure webhooks automatically

NOTE: in another shell

```bash
  USER_PASS="$(cat local/configs/user):$(cat local/configs/key)" URL=<see_previous_point> make -C local make public
```

#### Enable the local agent

You can enable your own machine to become an agent, as simple as:

1. Run in your terminal:

```bash
  make -C local start-local-worker
```
    NOTE: Java is required.

#### Enable the linux vagrant worker

```bash
  make -C local start-linux-worker
```

#### Customise what plugins are installed

You can configure this jenkins instance as you wish, if so please change:

* local/configs/jenkins.yaml using the [JCasC](https://jenkins.io/projects/jcasc/)
* local/configs/plugins.txt

## What's next?

- Configure the flaky ES data

```bash
  make -C resources create-flaky-index add-flaky-failure get-flaky-failure
```
## Further details

This is a subset of what it has been implemented in the https://github.com/elastic/apm-pipeline-library and https://github.com/v1v/jenkins-pipeline-library-skeleton
