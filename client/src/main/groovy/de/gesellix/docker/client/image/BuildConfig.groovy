package de.gesellix.docker.client.image

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import de.gesellix.docker.client.DockerAsyncCallback

@EqualsAndHashCode
@ToString
class BuildConfig {
  InputStream buildContext
  Map<String, String> query = ["rm": true]
  DockerAsyncCallback callback = null
  Map<String, String> options = [:]
}