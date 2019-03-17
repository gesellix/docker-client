package de.gesellix.docker.client.stack.types

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Driver {

    String name
    Map<String, String> options
}
