package de.gesellix.docker.compose.types

import com.squareup.moshi.Json
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Reservations {

    @Json(name = 'cpus')
    String nanoCpus
//    long MemoryBytes
     String memory

}
