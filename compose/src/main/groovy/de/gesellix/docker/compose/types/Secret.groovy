package de.gesellix.docker.compose.types

import de.gesellix.docker.compose.adapters.ExternalType
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Secret {

    String file
    @ExternalType
    External external = new External()
    List<String> labels

}
