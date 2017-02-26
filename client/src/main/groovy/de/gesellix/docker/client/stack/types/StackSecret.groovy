package de.gesellix.docker.client.stack.types

import groovy.transform.ToString

@ToString(excludes = "data")
class StackSecret {

    String name
    byte[] data
    Map<String, String> labels = [:]
}
