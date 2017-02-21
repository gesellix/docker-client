package de.gesellix.docker.compose.types

import com.squareup.moshi.Json
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Deploy {

    String mode
    int replicas
    List<String> labels
    @Json(name = 'update_config')
    UpdateConfig updateConfig
    Resources resources
    @Json(name = 'restart_policy')
    RestartPolicy restartPolicy
    Placement placement

}
