package de.gesellix.docker.compose.types

import com.squareup.moshi.Json
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class ServiceNetwork {

    List<String> aliases

    @Json(name = "ipv4_address")
    String ipv4Address

    @Json(name = "ipv6_address")
    String ipv6Address
}
