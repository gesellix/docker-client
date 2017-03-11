package de.gesellix.docker.compose.types

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class ServiceSecret {

    String source
    String target
    String uid
    String gid
    Integer mode
}
