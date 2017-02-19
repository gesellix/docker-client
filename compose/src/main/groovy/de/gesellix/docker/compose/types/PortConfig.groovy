package de.gesellix.docker.compose.types

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class PortConfig {
    String mode
    int target
    int published
    String protocol
}
