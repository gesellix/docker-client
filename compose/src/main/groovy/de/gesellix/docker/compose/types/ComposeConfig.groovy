package de.gesellix.docker.compose.types

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class ComposeConfig {

    String version
    Map<String, Service> services
    Map<String, Network> networks
    Map<String, Volume> volumes
    Map<String, Secret> secrets
}
