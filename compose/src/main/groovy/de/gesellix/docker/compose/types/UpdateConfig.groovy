package de.gesellix.docker.compose.types

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class UpdateConfig {

    int parallelism

//    Duration Delay
    String delay

    // `mapstructure:"failure_action"`
    String failureAction

//    Duration Monitor
    String monitor

    // `mapstructure:"max_failure_ratio"`
    float maxFailureRatio
}
