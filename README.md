# docker-client

A Docker HTTP client written in Groovy

[![Remote API Coverage Status (59/59 endpoints)](http://progressed.io/bar/100?title=api%20coverage)](https://github.com/gesellix/docker-client/blob/master/roadmap.md)
[![Build Status](https://travis-ci.org/gesellix/docker-client.svg)](https://travis-ci.org/gesellix/docker-client)
[![Latest version](https://api.bintray.com/packages/gesellix/docker-utils/docker-client/images/download.svg) ](https://bintray.com/gesellix/docker-utils/docker-client/_latestVersion)

This client library aims at supporting all existing api endpoints, which effectively allows
 to use it in place of the official Docker client binary. It might feel a bit less convenient, though,
 while it gives you a bit more freedom to access the remote api and some less popular endpoints.
 See the [roadmap.md](https://github.com/gesellix/docker-client/blob/master/roadmap.md) for details
 about the current api coverage.

Consider the client as a thin wrapper to perform HTTP requests, minimizing the need to manually configure
 TLS or auth encoding in your code. Most commonly known environment variables will work as expected,
 e.g. `DOCKER_HOST` or `DOCKER_CERT_PATH`. You can override existing environment variables with
 Java system properties like this:

    System.setProperty("docker.host", "192.168.99.100")
    System.setProperty("docker.cert.path", "/Users/${System.getProperty('user.name')}/.docker/machine/machines/default")

[Docker for Mac](https://beta.docker.com/) users: you need to configure your `DOCKER_HOST` to be set to `unix:///var/tmp/docker.sock`.

Please note that the raw responses (including headers) from the Docker daemon are returned, with the actual response body
 being available in the `content` attribute. Some endpoints return a stream, which is then available in `stream`.
 For some cases, like following the logs or events stream, you need to provide a callback which is called for every
 response line, see example 3 below.

## Plain Usage

For use in Gradle, add the bintray repository first:

    repositories {
      maven { url 'http://dl.bintray.com/gesellix/docker-utils' }
    }

Then, you need to add the dependency, but please ensure to use the [latest version](https://bintray.com/gesellix/docker-utils/docker-client/_latestVersion):

    dependencies {
      compile 'de.gesellix:docker-client:2016-04-07T22-50-17'
    }

The tests in `DockerClientImplSpec` and `DockerClientImplIntegrationSpec` should give you an idea how to use the docker-client.

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
    def containerConfig = ["Cmd"       : cmds,
                           ExposedPorts: ["4711/tcp": [:]],
                           "HostConfig": ["PortBindings": [
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


## Usage with Gradle-Docker-Plugin

My personal focus implementing this Docker client was to leverage the Docker remote API in our Gradle scripts.
A convenient integration in Gradle is possible by using the [Gradle-Docker-Plugin](https://github.com/gesellix/gradle-docker-plugin),
which will be developed along with the Docker client library.
