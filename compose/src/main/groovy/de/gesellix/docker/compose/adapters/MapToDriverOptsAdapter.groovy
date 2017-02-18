package de.gesellix.docker.compose.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import de.gesellix.docker.compose.types.DriverOpts

class MapToDriverOptsAdapter {

    @ToJson
    Map<String, String> toJson(@DriverOptsType DriverOpts driverOpts) {
        throw new UnsupportedOperationException()
    }

    @FromJson
    @DriverOptsType
    DriverOpts fromJson(Map<String, String> options) {
        def driverOpts = new DriverOpts()
        driverOpts.options.putAll(options)
        return driverOpts
    }
}
