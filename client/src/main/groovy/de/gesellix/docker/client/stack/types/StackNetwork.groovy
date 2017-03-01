package de.gesellix.docker.client.stack.types

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class StackNetwork {
    Set<String> labels = []
    String driver
    Map<String, String> driverOpts = [:]
    boolean internal
    boolean attachable
    Map<String, Object> ipam = [:]
}
