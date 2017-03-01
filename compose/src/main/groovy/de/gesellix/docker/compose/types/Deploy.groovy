package de.gesellix.docker.compose.types

import com.squareup.moshi.Json
import de.gesellix.docker.compose.adapters.LabelsType
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Deploy {

    String mode
    int replicas
    @LabelsType
    Labels labels
    @Json(name = 'update_config')
    UpdateConfig updateConfig
    Resources resources
    @Json(name = 'restart_policy')
    RestartPolicy restartPolicy
    Placement placement

}
