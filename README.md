# Docker Client

A Docker HTTP client for the Java VM written in Groovy

[![Engine API v1.31 Coverage Status (104/122 endpoints)](http://progressed.io/bar/85?title=api%20coverage%20(v1.31))](https://github.com/gesellix/docker-client/blob/master/supported-api.md)
[![Build Status](https://travis-ci.org/gesellix/docker-client.svg)](https://travis-ci.org/gesellix/docker-client)
[![Latest version](https://api.bintray.com/packages/gesellix/docker-utils/docker-client/images/download.svg) ](https://bintray.com/gesellix/docker-utils/docker-client/_latestVersion)

This client library aims at supporting all existing [Docker api endpoints](https://docs.docker.com/engine/reference/api/docker_remote_api/),
 which effectively allows to use it in place of the official Docker client binary.

See the [supported-api.md](https://github.com/gesellix/docker-client/blob/master/supported-api.md)
 for details about the current api coverage.

All platforms are natively supported, which includes:
- [Linux](https://docs.docker.com/engine/installation/linux/),
- [Docker for Mac](https://docs.docker.com/docker-for-mac/),
- [Docker for Windows](https://docs.docker.com/docker-for-windows/),
- [Docker Machine](https://docs.docker.com/machine/overview/),
- and the legacy [Docker Toolbox](https://docs.docker.com/toolbox/overview/).

Consider the client as a thin wrapper to perform HTTP requests, minimizing the need to manually configure
 TLS or auth encoding in your code. Most commonly known environment variables will work as expected,
 e.g. `DOCKER_HOST`, `DOCKER_TLS_VERIFY`, or `DOCKER_CERT_PATH`.
 Due to its thin layer this client might feel a bit less convenient, though,
 while it gives you a bit more freedom to access the engine api and some less popular endpoints.

The client also includes [Docker Compose version 3](https://docs.docker.com/compose/compose-file/) support
 as part of the `docker stack --compose ...` command.
 See the [docker stack deploy docs](https://docs.docker.com/engine/reference/commandline/stack_deploy/) for details.
 Please note that you'll need at least Java 8 when using that feature. 

## Plain Usage

For use in Gradle, add the Bintray repository first:

    repositories {
      jcenter()
    }

Then, you need to add the dependency, but please ensure to use the [latest version](https://bintray.com/gesellix/docker-utils/docker-client/_latestVersion):

    dependencies {
      compile 'de.gesellix:docker-client:2017-08-06T11-45-15'
    }

The tests in `DockerClientImplSpec` and `DockerClientImplIntegrationSpec` should give you an idea how to use the docker-client.

The default Docker host is expected to be available at `unix:///var/run/docker.sock`.
 When using Docker Machine the existing environment variables as configured by `eval "$(docker-machine env default)"` should be enough.

Even for the native packages [Docker for Mac](https://docs.docker.com/docker-for-mac/)
 and [Docker for Windows](https://docs.docker.com/docker-for-windows/) you'll be able to rely on the default configuration.
 For Mac the default is the same as for Linux at `unix:///var/run/docker.sock`, while
 Windows uses the named pipe at `//./pipe/docker_engine`.

You can override existing `DOCKER_*` environment variables with Java system properties like this:

    System.setProperty("docker.host", "192.168.99.100")
    System.setProperty("docker.cert.path", "/Users/${System.getProperty('user.name')}/.docker/machine/machines/default")

Please note that the raw responses (including headers) from the Docker daemon are returned, with the actual response body
 being available in the `content` attribute. Some endpoints return a stream, which is then available in `stream`.
 For some cases, like following the logs or events stream, you need to provide a callback which is called for every
 response line, see example 3 below.

### Example 1: `docker info`

A basic example connecting to a Docker daemon running in a VM (boot2docker/machine) looks like this:

    System.setProperty("docker.cert.path", "/Users/${System.getProperty('user.name')}/.docker/machine/machines/default")
    def dockerClient = new DockerClientImpl(System.env.DOCKER_HOST)
    def info = dockerClient.info().content

### Example 2: `docker run`

Running a container being available on the host via HTTP port 4712 can be achieved like this:

    System.setProperty("docker.cert.path", "/Users/${System.getProperty('user.name')}/.docker/machine/machines/default")
    def dockerClient = new DockerClientImpl(System.env.DOCKER_HOST)
    def image = "busybox"
    def tag = "latest"
    def cmds = ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd"         : cmds,
                           "ExposedPorts": ["4711/tcp": [:]],
                           "HostConfig"  : ["PortBindings": [
                                   "4711/tcp": [
                                           ["HostIp"  : "0.0.0.0",
                                            "HostPort": "4712"]]
                           ]]]
    def result = dockerClient.run(image, containerConfig, tag).content

### Example 3: `docker logs --follow`

    def callback = new DockerAsyncCallback() {
        def lines = []

        @Override
        def onEvent(Object line) {
            println line
            lines << line
        }
    }

    dockerClient.logs("foo", [tail: 1], callback)

    // callback.lines will now collect all log lines
    // you might implement it as a fifo instead of the List shown above

### Example 4: `docker stack deploy --compose-file docker-stack.yml example`
    
    def dockerClient = new DockerClientImpl()
    dockerClient.initSwarm()

    def namespace = "example"
    def composeStack = getClass().getResourceAsStream('docker-stack.yml')
    String workingDir = Paths.get(getClass().getResource('docker-stack.yml').toURI()).parent

    def deployConfig = new DeployConfigReader(dockerClient).loadCompose(namespace, composeStack, workingDir)

    dockerClient.stackDeploy(namespace, deployConfig)


## Usage with Gradle Docker Plugin

My personal focus implementing this Docker client was to leverage the Docker engine API in our Gradle scripts.
A convenient integration in Gradle is possible by using the [Gradle Docker Plugin](https://github.com/gesellix/gradle-docker-plugin),
which will be developed along with the Docker client library.

## Contributing and Future Plans

If something doesn't work as expected or if you have suggestions, please [create an issue](https://github.com/gesellix/docker-client/issues).
Pull requests are welcome as well!

The developer api is quite rough, but that's where you can certainly help: I'll add a more convenient layer
 on top of the raw interface so that for 80% of common use cases the Docker client
 should help _**you**_ to get it working without digging too deep into the source code.
 So, all I need is an indication about how you'd like that convenience layer to look like.
 Feel free to create an issue where we can discuss how your use case could be implemented!

## License

Copyright 2015-2017 [Tobias Gesellchen](https://www.gesellix.net/) ([@gesellix](https://twitter.com/gesellix))

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
