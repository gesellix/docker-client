package de.gesellix.docker.compose.types

import com.squareup.moshi.Json
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class RestartPolicy {

    String condition

//    Duration Delay
    String delay

    @Json(name = 'max_attempts')
    int maxAttempts

//    Duration Window
    String window
}
