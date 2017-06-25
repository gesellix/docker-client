package de.gesellix.docker.engine

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString(includeNames = true)
class EngineResponseStatus {

    String text
    int code
    boolean success
}
