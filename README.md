# docker-client

A Docker HTTP client written in Groovy

[![Build Status](https://travis-ci.org/gesellix-docker/docker-client.svg)](https://travis-ci.org/gesellix-docker/docker-client)
[![Coverage Status](https://coveralls.io/repos/gesellix-docker/docker-client/badge.png)](https://coveralls.io/r/gesellix-docker/docker-client)
[![Latest version](https://api.bintray.com/packages/gesellix/docker-utils/docker-client/images/download.png) ](https://bintray.com/gesellix/docker-utils/docker-client/_latestVersion)

## Plain Usage

For use in Gradle, add the bintray repository first:

```
repositories {
  maven { url 'http://dl.bintray.com/gesellix/docker-utils' }
}
```

Then, you need to add the dependency:

```
dependencies {
  compile 'de.gesellix:docker-client:2014-09-20T22-32-56'
}
```

The tests in `DockerClientImplSpec` should give you an idea how to use the docker-client.

## Usage with Gradle-Docker-Plugin

My personal focus implementing this Docker client was to leverage the Docker remote API in our Gradle scripts.
A convenient integration in Gradle is possible by using the [Gradle-Docker-Plugin](https://github.com/gesellix-docker/gradle-docker-plugin),
which will be developed along with the Docker client library.
