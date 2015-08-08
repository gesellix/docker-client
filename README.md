# docker-client

A Docker HTTP client written in Groovy

[![Build Status](https://travis-ci.org/gesellix/docker-client.svg)](https://travis-ci.org/gesellix/docker-client)
[![Latest version](https://api.bintray.com/packages/gesellix/docker-utils/docker-client/images/download.svg) ](https://bintray.com/gesellix/docker-utils/docker-client/_latestVersion)

## Plain Usage

For use in Gradle, add the bintray repository first:

```
repositories {
  maven { url 'http://dl.bintray.com/gesellix/docker-utils' }
}
```

Then, you need to add the dependency, but please ensure to use the [latest version](https://bintray.com/gesellix/docker-utils/docker-client/_latestVersion):

```
dependencies {
  compile 'de.gesellix:docker-client:2015-08-08T20-25-24'
}
```

The tests in `DockerClientImplSpec` and `DockerClientImplIntegrationSpec` should give you an idea how to use the docker-client.

## Usage with Gradle-Docker-Plugin

My personal focus implementing this Docker client was to leverage the Docker remote API in our Gradle scripts.
A convenient integration in Gradle is possible by using the [Gradle-Docker-Plugin](https://github.com/gesellix/gradle-docker-plugin),
which will be developed along with the Docker client library.
