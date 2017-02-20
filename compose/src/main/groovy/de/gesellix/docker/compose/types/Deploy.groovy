package de.gesellix.docker.compose.types

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Deploy {

    String mode
    int replicas
    List<String> labels
    UpdateConfig updateConfig
    Resources resources
    RestartPolicy restartPolicy
    Placement placement

}
