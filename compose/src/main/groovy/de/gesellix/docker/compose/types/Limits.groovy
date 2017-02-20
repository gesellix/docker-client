package de.gesellix.docker.compose.types

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Limits {

//    String NanoCPUs
    String cpus
//    long MemoryBytes
    String memory

}
