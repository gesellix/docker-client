package de.gesellix.docker.compose.types

import com.squareup.moshi.Json
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class UpdateConfig {

    int parallelism

//    Duration Delay
    String delay

    @Json(name = 'failure_action')
    String failureAction

//    Duration Monitor
    String monitor

    @Json(name = 'max_failure_ratio')
    float maxFailureRatio
}
