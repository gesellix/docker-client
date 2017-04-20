package de.gesellix.docker.client

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString(includeNames = true)
class DockerResponseStatus {

    String text
    int code
    boolean success
}
