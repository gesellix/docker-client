package de.gesellix.docker.compose.types

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class Resources {

    Limits limits
    Reservations reservations
}
