package de.gesellix.docker.client.stack

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.stack.types.StackNetwork
import de.gesellix.docker.client.stack.types.StackSecret
import de.gesellix.docker.compose.ComposeFileReader
import de.gesellix.docker.compose.types.ComposeConfig
import de.gesellix.docker.compose.types.Config
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

        def externalNetworkNames = []
        def networkCreateConfigs = [:]
        serviceNetworkNames.each { String internalName ->
            def network = composeConfig.networks[internalName]
            if (!network) {
                def createOpts = new StackNetwork()
                createOpts.labels = ["${ManageStackClient.LabelNamespace}=${namespace}" as String]
                createOpts.driver = "overlay"
                networkCreateConfigs[internalName] = createOpts
            } else if (network?.external?.external) {
                externalNetworkNames << (network.external.name ?: internalName)
            } else {
                def createOpts = new StackNetwork()

                def labels = new HashSet<String>()
                labels.addAll(network.labels ?: [])
                labels["${ManageStackClient.LabelNamespace}=${namespace}" as String]
                createOpts.labels = labels
                createOpts.driver = network.driver ?: "overlay"
                createOpts.driverOpts = network.driverOpts.options
                createOpts.internal = network.internal
                createOpts.attachable = network.attachable
                if (network.ipam.driver || network.ipam.config) {
                    createOpts.ipam = [:]
                }
                if (network.ipam.driver) {
                    createOpts.ipam.driver = network.ipam.driver
                }
                if (network.ipam.config) {
                    createOpts.ipam.config = []
                    network.ipam.config.each { Config config ->
                        createOpts.ipam.config << [subnet: config.subnet]
                    }
                }
                networkCreateConfigs[internalName] = createOpts
            }
        }

        log.info("network configs: ${networkCreateConfigs}")
        log.info("external networks: ${externalNetworkNames}")

        validateExternalNetworks(externalNetworkNames)

        def cfg = new DeployStackConfig()
        cfg.networks = networkCreateConfigs
        cfg.secrets = secrets(namespace, composeConfig.secrets)
        return cfg
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
            if (!network.content.Scope != "swarm") {
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
