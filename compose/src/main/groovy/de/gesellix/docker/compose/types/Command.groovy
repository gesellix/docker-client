package de.gesellix.docker.compose.types

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Command {
    List<String> parts = []
}
