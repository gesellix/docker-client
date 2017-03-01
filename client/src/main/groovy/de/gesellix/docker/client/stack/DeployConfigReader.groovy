package de.gesellix.docker.client.stack

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.stack.types.StackNetwork
import de.gesellix.docker.client.stack.types.StackSecret
import de.gesellix.docker.client.stack.types.StackService
import de.gesellix.docker.compose.ComposeFileReader
import de.gesellix.docker.compose.types.ComposeConfig
import de.gesellix.docker.compose.types.Config
import de.gesellix.docker.compose.types.Network
import de.gesellix.docker.compose.types.Secret
import de.gesellix.docker.compose.types.Service
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
        (networkConfigs, externals) = networks(namespace, serviceNetworkNames, composeConfig.networks)
        def secrets = secrets(namespace, composeConfig.secrets)

        def serviceConfigs = [:]
        composeConfig.services.each { name, service ->
//            name = ("${namespace}_${name}" as String)
            def serviceConfig = new StackService()
            def ports = service.ports.portConfigs.collect { portConfig ->
                [
                        protocol     : portConfig.protocol,
                        targetPort   : portConfig.target,
                        publishedPort: portConfig.published,
                        publishMode  : portConfig.mode,
                ]
            }
            serviceConfig.endpointSpec.ports = ports

            serviceConfigs[name] = serviceConfig
        }
        log.info("services $serviceConfigs")

        def cfg = new DeployStackConfig()
        cfg.networks = networkConfigs
        cfg.secrets = secrets
        return cfg
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
        return secretSpec
    }
}
