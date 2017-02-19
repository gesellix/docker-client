package de.gesellix.docker.compose.types

import de.gesellix.docker.compose.adapters.CommandType
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString


@EqualsAndHashCode
@ToString
class Healthcheck {

    boolean disable
    String interval
    float retries
    @CommandType
    Command test = new Command()
    String timeout

}
