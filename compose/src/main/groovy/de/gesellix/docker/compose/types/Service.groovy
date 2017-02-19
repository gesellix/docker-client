package de.gesellix.docker.compose.types

import de.gesellix.docker.compose.adapters.CommandType
import de.gesellix.docker.compose.adapters.EnvironmentType
import de.gesellix.docker.compose.adapters.LabelsType
import de.gesellix.docker.compose.adapters.NetworksType
import de.gesellix.docker.compose.adapters.PortConfigsType
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Service {

    Deploy deploy
    Object build
    Set<String> capAdd = null
    Set<String> capDrop = null
    String cgroupParent
    @CommandType
    Command command
    String containerName
    Set<String> dependsOn = null
    Set<String> devices = null
    Object dns
    Object dnsSearch
    String domainname
    List<String> entrypoint
    Object envFile
    @EnvironmentType
    Environment environment = new Environment()
    Set<String> expose = null
    Set<String> externalLinks = null
    Object extraHosts
    Healthcheck healthcheck
    String hostname
    String image
    String ipc
    @LabelsType
    Labels labels
    Set<String> links = null
    Logging logging
    String macAddress
    String networkMode
    @NetworksType
    Map<String, Network> networks
    String pid
    @PortConfigsType
    PortConfigs ports = new PortConfigs()
    boolean privileged
    boolean readOnly
    String restart
    Set<String> securityOpt = null
    float shmSize
    List<Object> secrets = null
    Object sysctls
    boolean stdinOpen
    String stopGracePeriod
    String stopSignal
    Object tmpfs
    boolean tty
    Ulimits ulimits
    String user
    String usernsMode
    Set<String> volumes = null
    String workingDir

}
