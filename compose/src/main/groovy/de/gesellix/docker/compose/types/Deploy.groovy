package de.gesellix.docker.compose.types

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class Deploy {

    String mode
    int replicas
    List<String> labels
    UpdateConfig updateConfig
    Resources resources
    RestartPolicy restartPolicy
    Placement placement

}
