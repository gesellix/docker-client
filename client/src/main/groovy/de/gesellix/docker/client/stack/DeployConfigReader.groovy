package de.gesellix.docker.client.stack

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.stack.types.MountPropagation
import de.gesellix.docker.client.stack.types.ResolutionMode
import de.gesellix.docker.client.stack.types.RestartPolicyCondition
import de.gesellix.docker.client.stack.types.StackNetwork
import de.gesellix.docker.client.stack.types.StackSecret
import de.gesellix.docker.client.stack.types.StackService
import de.gesellix.docker.compose.ComposeFileReader
import de.gesellix.docker.compose.types.ComposeConfig
import de.gesellix.docker.compose.types.Config
import de.gesellix.docker.compose.types.Environment
import de.gesellix.docker.compose.types.ExtraHosts
import de.gesellix.docker.compose.types.Healthcheck
import de.gesellix.docker.compose.types.Logging
import de.gesellix.docker.compose.types.Network
import de.gesellix.docker.compose.types.PortConfigs
import de.gesellix.docker.compose.types.Resources
import de.gesellix.docker.compose.types.RestartPolicy
import de.gesellix.docker.compose.types.Secret
import de.gesellix.docker.compose.types.Service
import de.gesellix.docker.compose.types.ServiceNetwork
import de.gesellix.docker.compose.types.UpdateConfig
import de.gesellix.docker.compose.types.Volume
import groovy.util.logging.Slf4j

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.regex.Pattern

import static de.gesellix.docker.client.stack.types.RestartPolicyCondition.RestartPolicyConditionAny
import static de.gesellix.docker.client.stack.types.RestartPolicyCondition.RestartPolicyConditionOnFailure
import static java.lang.Double.parseDouble
import static java.lang.Integer.parseInt

@Slf4j
class DeployConfigReader {

    DockerClient dockerClient

    ComposeFileReader composeFileReader = new ComposeFileReader()

    DeployConfigReader(DockerClient dockerClient) {
        this.dockerClient = dockerClient
    }

    def loadCompose(String namespace, InputStream composeFile, String workingDir) {
        ComposeConfig composeConfig = composeFileReader.load(composeFile)
        log.info("composeContent: $composeConfig}")

        List<String> serviceNetworkNames = composeConfig.services.collect { String name, Service service ->
            if (!service.networks) {
                return ["default"]
            }
            return service.networks.collect { String networkName, serviceNetwork ->
                networkName
            }
        }.flatten().unique()
        log.info("service network names: ${serviceNetworkNames}")

        Map<String, StackNetwork> networkConfigs
        List<String> externals
        (networkConfigs, externals) = networks(namespace, serviceNetworkNames, composeConfig.networks)
        def secrets = secrets(namespace, composeConfig.secrets, workingDir)
        def services = services(namespace, composeConfig.services, composeConfig.networks, composeConfig.volumes)

        def cfg = new DeployStackConfig()
        cfg.networks = networkConfigs
        cfg.secrets = secrets
        cfg.services = services
        return cfg
    }

    def services(
            String namespace,
            Map<String, Service> services,
            Map<String, Network> networks,
            Map<String, Volume> volumes) {
        Map<String, StackService> serviceSpec = [:]
        services.each { name, service ->
            def serviceLabels = service.deploy.labels?.entries ?: [:]
            serviceLabels[ManageStackClient.LabelNamespace] = namespace

            def containerLabels = service.labels?.entries ?: [:]
            containerLabels[ManageStackClient.LabelNamespace] = namespace

            Long stopGracePeriod = null
            if (service.stopGracePeriod) {
                stopGracePeriod = parseDuration(service.stopGracePeriod).toNanos()
            }

            def serviceConfig = new StackService()
            serviceConfig.name = ("${namespace}_${name}" as String)
            serviceConfig.labels = serviceLabels
            serviceConfig.endpointSpec = serviceEndpoints(service.deploy.endpointMode, service.ports)
            serviceConfig.mode = serviceMode(service.deploy.mode, service.deploy.replicas)
            serviceConfig.networks = convertServiceNetworks(service.networks, networks, namespace, name)
            serviceConfig.updateConfig = convertUpdateConfig(service.deploy.updateConfig)
            serviceConfig.taskTemplate = [
                    containerSpec: [
                            image          : service.image,
                            command        : service.entrypoint,
                            args           : service.command?.parts ?: [],
                            hostname       : service.hostname,
                            hosts          : Collections.sort(convertExtraHosts(service.extraHosts)),
                            healthcheck    : convertHealthcheck(service.healthcheck),
                            env            : Collections.sort(convertEnvironment(service.environment)),
                            labels         : containerLabels,
                            dir            : service.workingDir,
                            user           : service.user,
                            mounts         : volumesToMounts(namespace, service.volumes as List, volumes),
                            stopGracePeriod: stopGracePeriod,
                            tty            : service.tty,
                            openStdin      : service.stdinOpen,
//                            secrets        : secrets,
                    ],
                    logDriver    : logDriver(service.logging),
                    resources    : serviceResources(service.deploy.resources),
                    restartPolicy: restartPolicy(service.restart, service.deploy.restartPolicy),
                    placement    : [
                            constraints: service.deploy?.placement?.constraints,
                    ],
            ]

            serviceSpec[name] = serviceConfig
        }
        log.info("services $serviceSpec")
        return serviceSpec
    }

    List<String> convertEnvironment(Environment environment) {
        environment?.entries?.collect { name, value ->
            "${name}=${value}" as String
        } ?: []
    }

    List<String> convertExtraHosts(ExtraHosts extraHosts) {
        extraHosts?.entries?.collect { host, ip ->
            "${host} ${ip}" as String
        } ?: []
    }

    def convertUpdateConfig(UpdateConfig updateConfig) {
        if (!updateConfig) {
            return null
        }

        def parallel = 1
        if (updateConfig.parallelism) {
            parallel = updateConfig.parallelism
        }

        def delay = 0
        if (updateConfig.delay) {
            delay = parseDuration(updateConfig.delay).toNanos()
        }

        def monitor = 0
        if (updateConfig.monitor) {
            monitor = parseDuration(updateConfig.monitor).toNanos()
        }

        return [
                parallelism    : parallel,
                delay          : delay,
                failureAction  : updateConfig.failureAction,
                monitor        : monitor,
                maxFailureRatio: updateConfig.maxFailureRatio,
        ]
    }

    def convertServiceNetworks(
            Map<String, ServiceNetwork> serviceNetworks,
            Map<String, Network> networkConfigs,
            String namespace,
            String serviceName) {

        if (serviceNetworks == null || serviceNetworks.isEmpty()) {
            serviceNetworks = ["default": null]
        }

        def serviceNetworkConfigs = []

        serviceNetworks.each { networkName, serviceNetwork ->
            if (!networkConfigs.containsKey(networkName) && networkName != "default") {
                throw new IllegalStateException("service ${serviceName} references network ${networkName}, which is not declared")
            }
            def networkConfig = networkConfigs[networkName]

            List<String> aliases = []
            if (serviceNetwork) {
                aliases = serviceNetwork.aliases
            }
            aliases << serviceName

            String namespacedName = "${namespace}_${networkName}" as String

            String target = namespacedName
            if (networkConfig?.external?.external && networkConfig?.external?.name) {
                target = networkConfig.external.name
            }

            serviceNetworkConfigs << [
                    target : target,
                    aliases: aliases,
            ]
        }

        Collections.sort(serviceNetworkConfigs, new NetworkConfigByTargetComparator())
        return serviceNetworkConfigs
    }

    static class NetworkConfigByTargetComparator implements Comparator {

        @Override
        int compare(Object o1, Object o2) {
            return o1.target.compareTo(o2.target)
        }
    }

    def logDriver(Logging logging) {
        if (logging) {
            return [
                    name   : logging.driver,
                    options: logging.options,
            ]
        }
        return null
    }

    def convertHealthcheck(Healthcheck healthcheck) {

        if (!healthcheck) {
            return null
        }

        Integer retries = null
        Long timeout = null
        Long interval = null

        if (healthcheck.disable) {
            if (healthcheck.test?.parts) {
                throw new IllegalArgumentException("test and disable can't be set at the same time")
            }
            return [
                    test: ["NONE"]
            ]
        }

        if (healthcheck.timeout) {
            timeout = parseDuration(healthcheck.timeout).toNanos()
        }
        if (healthcheck.interval) {
            interval = parseDuration(healthcheck.interval).toNanos()
        }
        if (healthcheck.retries) {
            retries = new Float(healthcheck.retries).intValue()
        }

        return [
                test    : healthcheck.test.parts,
                timeout : timeout ?: 0,
                interval: interval ?: 0,
                retries : retries,
        ]
    }

    Map<String, ChronoUnit> unitBySymbol = [
            "ns": ChronoUnit.NANOS,
            "us": ChronoUnit.MICROS,
            "µs": ChronoUnit.MICROS, // U+00B5 = micro symbol
            "μs": ChronoUnit.MICROS, // U+03BC = Greek letter mu
            "ms": ChronoUnit.MILLIS,
            "s" : ChronoUnit.SECONDS,
            "m" : ChronoUnit.MINUTES,
            "h" : ChronoUnit.HOURS
    ]

    final def numberWithUnitRegex = /(\d*)\.?(\d*)(\D+)/
    final def pattern = Pattern.compile(numberWithUnitRegex)

    def parseDuration(String durationAsString) {
        def sign = '+'
        if (durationAsString.matches(/[-+].+/)) {
            sign = durationAsString.substring(0, '-'.length())
            durationAsString = durationAsString.substring('-'.length())
        }
        def matcher = pattern.matcher(durationAsString)

        def duration = Duration.of(0, ChronoUnit.NANOS)
        def ok = false
        while (matcher.find()) {
            if (matcher.groupCount() != 3) {
                throw new IllegalStateException("expected 3 groups, but got ${matcher.groupCount()}")
            }
            def pre = matcher.group(1) ?: "0"
            def post = matcher.group(2) ?: "0"
            def symbol = matcher.group(3)
            if (!symbol) {
                throw new IllegalArgumentException("missing unit in duration '${durationAsString}'")
            }
            def unit = unitBySymbol[symbol]
            if (!unit) {
                throw new IllegalArgumentException("unknown unit ${symbol} in duration '${durationAsString}'")
            }

            def scale = Math.pow(10, post.length())

            duration = duration
                    .plus(parseInt(pre), unit)
                    .plus((int) (parseInt(post) * (unit.duration.nano / scale)), ChronoUnit.NANOS)

            ok = true
        }

        if (!ok) {
            throw new IllegalStateException("duration couldn't be parsed: '${durationAsString}'")
        }
        return duration.multipliedBy(sign == '-' ? -1 : 1)
    }

    def restartPolicy(String restart, RestartPolicy restartPolicy) {
        // TODO: log if restart is being ignored
        if (restartPolicy == null) {
            def policy = parseRestartPolicy(restart)
            if (!policy) {
                return null
            }
            switch (policy.name) {
                case "":
                case "no":
                    return null

                case "always":
                case "unless-stopped":
                    return [
                            condition: RestartPolicyConditionAny.value
                    ]

                case "on-failure":
                    return [
                            condition  : RestartPolicyConditionOnFailure.value,
                            maxAttempts: policy.maximumRetryCount
                    ]

                default:
                    throw new IllegalArgumentException("unknown restart policy: ${restart}")
            }
        } else {
            Long delay = null
            if (restartPolicy.delay) {
                delay = parseDuration(restartPolicy.delay).toNanos()
            }
            Long window = null
            if (restartPolicy.window) {
                window = parseDuration(restartPolicy.window).toNanos()
            }
            return [
                    condition  : RestartPolicyCondition.byValue(restartPolicy.condition).value,
                    delay      : delay,
                    maxAttempts: restartPolicy.maxAttempts,
                    window     : window,
            ]
        }
    }

    def parseRestartPolicy(String policy) {
        def restartPolicy = [
                name: ""
        ]
        if (!policy) {
            return restartPolicy
        }

        def parts = policy.split(':')
        if (parts.length > 2) {
            throw new IllegalArgumentException("invalid restart policy format: '${policy}")
        }

        if (parts.length == 2) {
            if (!parts[1].isInteger()) {
                throw new IllegalArgumentException("maximum retry count must be an integer")
            }

            restartPolicy.maximumRetryCount = parseInt(parts[1])
        }
        restartPolicy.name = parts[0]
        return restartPolicy
    }

    def serviceResources(Resources resources) {
        def resourceRequirements = [:]
        def nanoMultiplier = Math.pow(10, 9)
        if (resources?.limits) {
            resourceRequirements['limits'] = [:]
            if (resources.limits.nanoCpus) {
                if (resources.limits.nanoCpus.contains('/')) {
                    // TODO
                    throw new UnsupportedOperationException("not supported, yet")
                } else {
                    resourceRequirements['limits'].nanoCPUs = parseDouble(resources.limits.nanoCpus) * nanoMultiplier
                }
            }
            resourceRequirements['limits'].memoryBytes = parseInt(resources.limits.memory)
        }
        if (resources?.reservations) {
            resourceRequirements['reservations'] = [:]
            if (resources.reservations.nanoCpus) {
                if (resources.reservations.nanoCpus.contains('/')) {
                    // TODO
                    throw new UnsupportedOperationException("not supported, yet")
                } else {
                    resourceRequirements['reservations'].nanoCPUs = parseDouble(resources.reservations.nanoCpus) * nanoMultiplier
                }
            }
            resourceRequirements['reservations'].memoryBytes = parseInt(resources.reservations.memory)
        }
        return resourceRequirements
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

        if (!stackVolumes.containsKey(source)) {
            throw new IllegalArgumentException("undefined volume: $source")
        }
        def stackVolume = stackVolumes[source]

        def volumeOptions
        if (stackVolume?.external?.name) {
            volumeOptions = [
                    noCopy: isNoCopy(modes),
            ]
            source = stackVolume.external.name
        } else {
            def labels = stackVolume?.labels?.entries ?: [:]
            labels[(ManageStackClient.LabelNamespace)] = namespace
            volumeOptions = [
                    labels: labels,
                    noCopy: isNoCopy(modes),
            ]

            if (stackVolume?.driver && stackVolume?.driver != "") {
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

    def getBindOptions(List<String> modes) {
        def matchedModes = modes.intersect(MountPropagation.values().collect { it.value })
        if (matchedModes) {
            return [propagation: matchedModes.first()]
        } else {
            return null
        }
    }

    def serviceEndpoints(String endpointMode, PortConfigs portConfigs) {
        def endpointSpec = [
                mode : endpointMode ? ResolutionMode.byValue(endpointMode).value : null,
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
                createOpts.labels = [(ManageStackClient.LabelNamespace): namespace]
                createOpts.driver = "overlay"
                networkSpec[internalName] = createOpts
            } else if (network?.external?.external) {
                externalNetworkNames << (network.external.name ?: internalName)
            } else {
                def createOpts = new StackNetwork()

                def labels = [:]
                labels.putAll(network.labels?.entries ?: [:])
                labels[(ManageStackClient.LabelNamespace)] = namespace
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

    Map<String, StackSecret> secrets(String namespace, Map<String, Secret> secrets, String workingDir) {
        Map<String, StackSecret> secretSpec = [:]
        secrets.each { name, secret ->
            if (!secret.external.external) {
                Path filePath = Paths.get(workingDir, secret.file)
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
