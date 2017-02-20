package de.gesellix.docker.compose.types

import com.squareup.moshi.Json
import de.gesellix.docker.compose.adapters.DriverOptsType
import de.gesellix.docker.compose.adapters.ExternalType
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Network {

    String driver
    @Json(name = "driver_opts")
    @DriverOptsType
    DriverOpts driverOpts = new DriverOpts()
    Ipam ipam
    @ExternalType
    External external = new External()
    boolean internal
    boolean attachable
    List<String> labels

}
