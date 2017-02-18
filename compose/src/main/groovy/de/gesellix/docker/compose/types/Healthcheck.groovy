package de.gesellix.docker.compose.types

import groovy.transform.EqualsAndHashCode


@EqualsAndHashCode
class Healthcheck {

    boolean disable
    String interval
    float retries
    List<String> test
    String timeout

}
