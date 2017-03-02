package de.gesellix.docker.client.stack

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.stack.types.StackNetwork
import de.gesellix.docker.client.stack.types.StackSecret
import de.gesellix.docker.client.stack.types.StackService
import de.gesellix.docker.compose.ComposeFileReader
import de.gesellix.docker.compose.types.ComposeConfig
import de.gesellix.docker.compose.types.Config
import de.gesellix.docker.compose.types.Network
import de.gesellix.docker.compose.types.PortConfigs
import de.gesellix.docker.compose.types.Secret
import de.gesellix.docker.compose.types.Service
import de.gesellix.docker.compose.types.Volume
import groovy.util.logging.Slf4j

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
class DeployConfigReader {

    DockerClient dockerClient

    ComposeFileReader composeFileReader = new ComposeFileReader()

    DeployConfigReader(DockerClient dockerClient) {
        this.dockerClient = dockerClient
    }

    def loadCompose(String namespace, InputStream composeFile) {
        ComposeConfig composeConfig = composeFileReader.load(composeFile)
        log.info("composeContent: $composeConfig}")

        List<String> serviceNetworkNames = composeConfig.services.collect { String name, Service service ->
            service.networks.collect { String networkName, serviceNetwork ->
                networkName
            }
        }.flatten().unique()
        if (!serviceNetworkNames) {
            serviceNetworkNames = ["default"]
        }
        log.info("service network names: ${serviceNetworkNames}")

        Map<String, StackNetwork> networkConfigs
        List<String> externals
        (networkConfigs, externals) = networks(namespace, serviceNetworkNames, composeConfig.networks)
        def secrets = secrets(namespace, composeConfig.secrets)
        def services = services(namespace, composeConfig.services, composeConfig.volumes)

        def cfg = new DeployStackConfig()
        cfg.networks = networkConfigs
        cfg.secrets = secrets
        cfg.services = services
        return cfg
    }

    def services(String namespace, Map<String, Service> services, Map<String, Volume> volumes) {
        Map<String, StackService> serviceSpec = [:]
        services.each { name, service ->
//            name = ("${namespace}_${name}" as String)
            def serviceConfig = new StackService()
            serviceConfig.endpointSpec = serviceEndpoints(service.ports)
            serviceConfig.mode = serviceMode(service.deploy.mode, service.deploy.replicas)
            serviceConfig.taskTemplate = [
                    containerSpec: [
                            mounts: volumesToMounts(namespace, service.volumes as List, volumes)
                    ]
            ]

            serviceSpec[name] = serviceConfig
        }
        log.info("services $serviceSpec")
        return serviceSpec
    }

    def volumesToMounts(String namespace, List<String> serviceVolumes, Map<String, Volume> stackVolumes) {
        def mounts = serviceVolumes.collect { serviceVolume ->
            return volumeToMount(namespace, serviceVolume, stackVolumes)
        }
        return mounts
    }

    // TypeBind is the type for mounting host dir
    String TypeBind = "bind"
    // TypeVolume is the type for remote storage volumes
    String TypeVolume = "volume"
    // TypeTmpfs is the type for mounting tmpfs
    String TypeTmpfs = "tmpfs"

    Map volumeToMount(String namespace, String volumeSpec, Map<String, Volume> stackVolumes) {
        def parts = volumeSpec.split(':', 3)
        parts.each { part ->
            if (part?.trim().isEmpty()) {
                throw new IllegalArgumentException("invalid volume: $volumeSpec")
            }
        }

        String source = ""
        String target
        List<String> modes = []

        switch (parts.length) {
            case 3:
                source = parts[0]
                target = parts[1]
                modes = parts[2].split(",")
                break
            case 2:
                source = parts[0]
                target = parts[1]
                break
            case 1:
                target = parts[0]
                break
            default:
                throw new IllegalStateException("invalid volume $volumeSpec")
        }

        if (source == "") {
            // Anonymous volume
            return [
                    type  : TypeVolume,
                    target: target,
            ]
        }

        // TODO: catch Windows paths here
        if (source?.startsWith("/")) {
            return [
                    type       : TypeBind,
                    source     : source,
                    target     : target,
                    readOnly   : isReadOnly(modes),
                    bindOptions: getBindOptions(modes)
            ]
        }

        def stackVolume = stackVolumes[source]
        if (!stackVolume) {
            throw new IllegalArgumentException("undefined volume: $source")
        }

        def volumeOptions
        if (stackVolume.external.name) {
            volumeOptions = [
                    noCopy: isNoCopy(modes),
            ]
            source = stackVolume.external.name
        } else {
            def labels = stackVolume.labels?.entries ?: [:]
            labels[(ManageStackClient.LabelNamespace)] = namespace
            volumeOptions = [
                    labels: labels,
                    noCopy: isNoCopy(modes),
            ]

            if (stackVolume.driver != "") {
                volumeOptions.driverConfig = [
                        name   : stackVolume.driver,
                        options: stackVolume.driverOpts.options
                ]
            }
            source = "${namespace}_${source}" as String
        }

        return [
                type         : TypeVolume,
                target       : target,
                source       : source,
                readOnly     : isReadOnly(modes),
                volumeOptions: volumeOptions
        ]
    }

    boolean isReadOnly(List<String> modes) {
        return modes.contains("ro")
    }

    boolean isNoCopy(List<String> modes) {
        return modes.contains("nocopy")
    }

    // PropagationRPrivate RPRIVATE
    static final String PropagationRPrivate = "rprivate"
    // PropagationPrivate PRIVATE
    static final String PropagationPrivate = "private"
    // PropagationRShared RSHARED
    static final String PropagationRShared = "rshared"
    // PropagationShared SHARED
    static final String PropagationShared = "shared"
    // PropagationRSlave RSLAVE
    static final String PropagationRSlave = "rslave"
    // PropagationSlave SLAVE
    static final String PropagationSlave = "slave"

    // Propagations is the list of all valid mount propagations
    List<String> propagations = [
            PropagationRPrivate,
            PropagationPrivate,
            PropagationRShared,
            PropagationShared,
            PropagationRSlave,
            PropagationSlave,
    ]

    def getBindOptions(List<String> modes) {
        def matchedModes = modes.intersect(propagations)
        if (matchedModes) {
            return [propagation: matchedModes.first()]
        } else {
            return null
        }
    }

    def serviceEndpoints(PortConfigs portConfigs) {
        def endpointSpec = [
                ports: portConfigs.portConfigs.collect { portConfig ->
                    [
                            protocol     : portConfig.protocol,
                            targetPort   : portConfig.target,
                            publishedPort: portConfig.published,
                            publishMode  : portConfig.mode,
                    ]
                }
        ]

        return endpointSpec
    }

    def serviceMode(String mode, Integer replicas) {
        switch (mode) {
            case "global":
                if (replicas) {
                    throw new IllegalArgumentException("replicas can only be used with replicated mode")
                }
                return [global: true]

            case null:
            case "":
            case "replicated":
                return [replicated: [replicas: replicas ?: 1]]

            default:
                throw new IllegalArgumentException("Unknown mode: '$mode'")
        }
    }

    Tuple2<Map<String, StackNetwork>, List<String>> networks(
            String namespace,
            List<String> serviceNetworkNames,
            Map<String, Network> networks) {
        Map<String, StackNetwork> networkSpec = [:]

        def externalNetworkNames = []
        serviceNetworkNames.each { String internalName ->
            def network = networks[internalName]
            if (!network) {
                def createOpts = new StackNetwork()
                createOpts.labels = ["${ManageStackClient.LabelNamespace}=${namespace}" as String]
                createOpts.driver = "overlay"
                networkSpec[internalName] = createOpts
            } else if (network?.external?.external) {
                externalNetworkNames << (network.external.name ?: internalName)
            } else {
                def createOpts = new StackNetwork()

                def labels = new HashSet<String>()
                labels.addAll(network.labels?.entries?.collect { k, v -> "$k=$v" } ?: [])
                labels.add("${ManageStackClient.LabelNamespace}=${namespace}" as String)
                createOpts.labels = labels
                createOpts.driver = network.driver ?: "overlay"
                createOpts.driverOpts = network.driverOpts.options
                createOpts.internal = network.internal
                createOpts.attachable = network.attachable
                if (network.ipam?.driver || network.ipam?.config) {
                    createOpts.ipam = [:]
                }
                if (network.ipam?.driver) {
                    createOpts.ipam.driver = network.ipam.driver
                }
                if (network.ipam?.config) {
                    createOpts.ipam.config = []
                    network.ipam.config.each { Config config ->
                        createOpts.ipam.config << [subnet: config.subnet]
                    }
                }
                networkSpec[internalName] = createOpts
            }
        }

        log.info("network configs: ${networkSpec}")
        log.info("external networks: ${externalNetworkNames}")

        validateExternalNetworks(externalNetworkNames)

        return [networkSpec, externalNetworkNames]
    }

    def validateExternalNetworks(List<String> externalNetworks) {
        externalNetworks.each { name ->
            def network
            try {
                network = dockerClient.inspectNetwork(name)
            } catch (Exception e) {
                log.error("network ${name} is declared as external, but could not be inspected. You need to create the network before the stack is deployed (with overlay driver)")
                throw new IllegalStateException("network ${name} is declared as external, but could not be inspected.", e)
            }
            if (network.content.Scope != "swarm") {
                log.error("network ${name} is declared as external, but it is not in the right scope: '${network.content.Scope}' instead of 'swarm'")
                throw new IllegalStateException("network ${name} is declared as external, but is not in 'swarm' scope.")
            }
        }
    }

    Map<String, StackSecret> secrets(String namespace, Map<String, Secret> secrets) {
        Map<String, StackSecret> secretSpec = [:]
        secrets.each { name, secret ->
            if (!secret.external.external) {
                Path filePath = Paths.get(secret.file)
                byte[] data = Files.readAllBytes(filePath)

                def labels = new HashMap<String, String>()
                if (secret.labels?.entries) {
                    labels.putAll(secret.labels.entries)
                }
                labels[ManageStackClient.LabelNamespace] = namespace

                secretSpec[name] = new StackSecret(
                        name: ("${namespace}_${name}" as String),
                        data: data,
                        labels: labels
                )
            }
        }
        log.info("secrets ${secretSpec.keySet()}")
        return secretSpec
    }
}
