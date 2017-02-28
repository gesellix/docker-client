package de.gesellix.docker.client.stack.types

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString(excludes = "data")
class StackSecret {

    String name
    byte[] data
    Map<String, String> labels = [:]
}
