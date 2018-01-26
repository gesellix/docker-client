package de.gesellix.docker.client.image

import de.gesellix.docker.client.DockerAsyncCallback
import de.gesellix.docker.client.Timeout
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import static de.gesellix.docker.client.Timeout.TEN_MINUTES

@EqualsAndHashCode
@ToString
class BuildConfig {

    Map<String, Object> query = ["rm": true]
    Map<String, Object> options = [:]

    DockerAsyncCallback callback
    Timeout timeout = TEN_MINUTES
}
