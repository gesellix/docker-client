package de.gesellix.docker.compose.types

import com.squareup.moshi.Json
import de.gesellix.docker.compose.adapters.CommandType
import de.gesellix.docker.compose.adapters.EnvironmentType
import de.gesellix.docker.compose.adapters.ExtraHostsType
import de.gesellix.docker.compose.adapters.LabelsType
import de.gesellix.docker.compose.adapters.PortConfigsType
import de.gesellix.docker.compose.adapters.ServiceNetworksType
import de.gesellix.docker.compose.adapters.ServiceSecretsType
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Service {

    Object build
    @Json(name = "cap_add")
    Set<String> capAdd = null
    @Json(name = "cap_drop")
    Set<String> capDrop = null
    @Json(name = "cgroup_parent")
    String cgroupParent
    @CommandType
    Command command
    @Json(name = "container_name")
    String containerName
    @Json(name = "depends_on")
    Set<String> dependsOn = null
    Deploy deploy
    Set<String> devices = null
    List<String> dns
    @Json(name = "dns_search")
    List<String> dnsSearch
    String domainname
    List<String> entrypoint
    @Json(name = 'env_file')
    List<String> envFile
    @EnvironmentType
    Environment environment = new Environment()
    Set<String> expose = null
    @Json(name = 'external_links')
    Set<String> externalLinks
    @Json(name = 'extra_hosts')
    @ExtraHostsType
    ExtraHosts extraHosts
    Healthcheck healthcheck
    String hostname
    String image
    String ipc
    @LabelsType
    Labels labels
    Set<String> links = null
    Logging logging
    @Json(name = "mac_address")
    String macAddress
    @Json(name = "network_mode")
    String networkMode
    @ServiceNetworksType
    Map<String, ServiceNetwork> networks
    String pid
    @PortConfigsType
    PortConfigs ports = new PortConfigs()
    boolean privileged
    @Json(name = "read_only")
    boolean readOnly
    String restart
    @Json(name = 'security_opt')
    Set<String> securityOpt
    float shmSize
    @ServiceSecretsType
    List<Map<String, ServiceSecret>> secrets
    Object sysctls
    @Json(name = "stdin_open")
    boolean stdinOpen
    @Json(name = 'stop_grace_period')
    String stopGracePeriod
    @Json(name = 'stop_signal')
    String stopSignal
    List<String> tmpfs
    boolean tty
    Ulimits ulimits
    String user
    @Json(name = "userns_mode")
    String usernsMode
    Set<String> volumes = null
    @Json(name = "working_dir")
    String workingDir

}
