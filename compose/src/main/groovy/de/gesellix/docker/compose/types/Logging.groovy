package de.gesellix.docker.compose.types

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class Logging {

    String driver
    Options options

}
