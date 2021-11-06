package de.gesellix.docker.client.stack

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.EnvFileParser
import de.gesellix.docker.client.LocalDocker
import de.gesellix.docker.client.stack.types.StackConfig
import de.gesellix.docker.client.stack.types.StackNetwork
import de.gesellix.docker.client.stack.types.StackSecret
import de.gesellix.docker.client.stack.types.StackService
import de.gesellix.docker.compose.ComposeFileReader
import de.gesellix.docker.compose.types.ComposeConfig
import de.gesellix.docker.compose.types.Environment
import de.gesellix.docker.compose.types.ExtraHosts
import de.gesellix.docker.compose.types.Healthcheck
import de.gesellix.docker.compose.types.IpamConfig
import de.gesellix.docker.compose.types.Limits
import de.gesellix.docker.compose.types.Logging
import de.gesellix.docker.compose.types.PortConfigs
import de.gesellix.docker.compose.types.Reservations
import de.gesellix.docker.compose.types.Resources
import de.gesellix.docker.compose.types.RestartPolicy
import de.gesellix.docker.compose.types.ServiceConfig
import de.gesellix.docker.compose.types.ServiceNetwork
import de.gesellix.docker.compose.types.ServiceSecret
import de.gesellix.docker.compose.types.ServiceVolume
import de.gesellix.docker.compose.types.ServiceVolumeBind
import de.gesellix.docker.compose.types.ServiceVolumeType
import de.gesellix.docker.compose.types.StackVolume
import de.gesellix.docker.compose.types.UpdateConfig
import de.gesellix.docker.remote.api.EndpointSpec
import de.gesellix.docker.remote.api.HealthConfig
import de.gesellix.docker.remote.api.Limit
import de.gesellix.docker.remote.api.Mount
import de.gesellix.docker.remote.api.MountBindOptions
import de.gesellix.docker.remote.api.MountVolumeOptions
import de.gesellix.docker.remote.api.MountVolumeOptionsDriverConfig
import de.gesellix.docker.remote.api.ResourceObject
import de.gesellix.docker.remote.api.TaskSpec
import de.gesellix.docker.remote.api.TaskSpecContainerSpec
import de.gesellix.docker.remote.api.TaskSpecContainerSpecConfigs
import de.gesellix.docker.remote.api.TaskSpecContainerSpecFile
import de.gesellix.docker.remote.api.TaskSpecContainerSpecFile1
import de.gesellix.docker.remote.api.TaskSpecContainerSpecSecrets
import de.gesellix.docker.remote.api.TaskSpecLogDriver
import de.gesellix.docker.remote.api.TaskSpecPlacement
import de.gesellix.docker.remote.api.TaskSpecPlacementPreferences
import de.gesellix.docker.remote.api.TaskSpecPlacementSpread
import de.gesellix.docker.remote.api.TaskSpecResources
import de.gesellix.docker.remote.api.TaskSpecRestartPolicy
import groovy.util.logging.Slf4j

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.regex.Pattern

import static java.lang.Double.parseDouble
import static java.lang.Integer.parseInt
import static java.lang.Long.parseLong

@Slf4j
class DeployConfigReader {

  DockerClient dockerClient

  ComposeFileReader composeFileReader = new ComposeFileReader()

  DeployConfigReader(DockerClient dockerClient) {
    this.dockerClient = dockerClient
  }

  @Deprecated
  DeployStackConfig loadCompose(String namespace, InputStream composeFile, String workingDir) {
    loadCompose(namespace, composeFile, workingDir, System.getenv())
  }

  // TODO test me
  DeployStackConfig loadCompose(String namespace, InputStream composeFile, String workingDir, Map<String, String> environment) {
    ComposeConfig composeConfig = composeFileReader.load(composeFile, workingDir, environment)
    log.info("composeContent: $composeConfig}")

    List<String> serviceNetworkNames = composeConfig.services.collect { String name, de.gesellix.docker.compose.types.StackService service ->
      if (!service.networks) {
        return ["default"]
      }
      return service.networks.collect { String networkName, serviceNetwork ->
        networkName
      }
    }.flatten().unique()
    log.info("service network names: ${serviceNetworkNames}")

    Map<String, StackNetwork> networkConfigs
    List<String> externalNetworks
    (networkConfigs, externalNetworks) = networks(namespace, serviceNetworkNames, composeConfig.networks ?: [:])
    def secrets = secrets(namespace, composeConfig.secrets, workingDir)
    def configs = configs(namespace, composeConfig.configs, workingDir)
    def services = services(namespace, composeConfig.services, composeConfig.networks, composeConfig.volumes)

    def cfg = new DeployStackConfig()
    cfg.networks = networkConfigs
    cfg.secrets = secrets
    cfg.configs = configs
    cfg.services = services
    return cfg
  }

  Map<String, StackService> services(
      String namespace,
      Map<String, de.gesellix.docker.compose.types.StackService> services,
      Map<String, de.gesellix.docker.compose.types.StackNetwork> networks,
      Map<String, StackVolume> volumes) {
    Map<String, StackService> serviceSpec = [:]
    services.each { name, service ->
      def serviceLabels = service.deploy?.labels?.entries ?: [:]
      serviceLabels[ManageStackClient.LabelNamespace] = namespace

      Map<String, String> containerLabels = service.labels?.entries ?: [:]
      containerLabels[ManageStackClient.LabelNamespace] = namespace

      Long stopGracePeriod = null
      if (service.stopGracePeriod) {
        stopGracePeriod = parseDuration(service.stopGracePeriod).toNanos()
      }

      List<String> env = convertEnvironment(service.workingDir, service.envFile, service.environment)
      Collections.sort(env)

      def extraHosts = convertExtraHosts(service.extraHosts)
      Collections.sort(extraHosts)

      def serviceConfig = new StackService()
      serviceConfig.name = ("${namespace}_${name}" as String)
      serviceConfig.labels = serviceLabels
      serviceConfig.endpointSpec = serviceEndpoints(service.deploy?.endpointMode, service.ports)
      serviceConfig.mode = serviceMode(service.deploy?.mode, service.deploy?.replicas)
      serviceConfig.networks = convertServiceNetworks(service.networks ?: [:], networks, namespace, name)
      serviceConfig.updateConfig = convertUpdateConfig(service.deploy?.updateConfig)
      serviceConfig.taskTemplate = new TaskSpec(
          null,
          new TaskSpecContainerSpec(
              service.image,
              containerLabels,
              service.entrypoint?.parts ?: [],
              service.command?.parts ?: [],
              service.hostname,
              env,
              service.workingDir,
              service.user,
              [],
              null,
              service.tty,
              service.stdinOpen,
              null,
              volumesToMounts(namespace, service.volumes as List<ServiceVolume>, volumes),
              service.stopSignal,
              stopGracePeriod,
              convertHealthcheck(service.healthcheck),
              extraHosts,
              null,
              prepareServiceSecrets(namespace, service.secrets),
              prepareServiceConfigs(namespace, service.configs),
              null,
              null,
              [:],
              [],
              [],
              []
          ),
          null,
          serviceResources(service.deploy?.resources),
          restartPolicy(service.restart, service.deploy?.restartPolicy),
          new TaskSpecPlacement(
              service.deploy?.placement?.constraints,
              placementPreferences(service.deploy?.placement?.preferences),
              service.deploy?.maxReplicasPerNode,
              null
          ),
          null,
          null,
          [],
          logDriver(service.logging)
      )
      serviceSpec[name] = serviceConfig
    }
    log.info("services $serviceSpec")
    return serviceSpec
  }

  List<TaskSpecContainerSpecConfigs> prepareServiceConfigs(String namespace, List<Map<String, ServiceConfig>> configs) {
    configs?.collect { item ->
      if (item.size() > 1) {
        throw new RuntimeException("expected a unique config entry")
      }
      List<TaskSpecContainerSpecConfigs> converted = item.entrySet().collect { entry ->
        new TaskSpecContainerSpecConfigs(
            new TaskSpecContainerSpecFile1(
                entry.value?.target ?: (entry.value?.source ?: entry.key),
                entry.value?.uid ?: "0",
                entry.value?.gid ?: "0",
                entry.value?.mode ?: 0444
            ),
            null,
            "<WILL_BE_PROVIDED_DURING_DEPLOY>",
            "${namespace}_${entry.key}".toString()
        )
      }
      converted.first()
    } ?: []
  }

  List<TaskSpecContainerSpecSecrets> prepareServiceSecrets(String namespace, List<Map<String, ServiceSecret>> secrets) {
    secrets?.collect { item ->
      if (item.size() > 1) {
        throw new RuntimeException("expected a unique secret entry")
      }
      List<TaskSpecContainerSpecSecrets> converted = item.entrySet().collect { entry ->
        new TaskSpecContainerSpecSecrets(
            new TaskSpecContainerSpecFile(
                entry.value?.target ?: (entry.value?.source ?: entry.key),
                entry.value?.uid ?: "0",
                entry.value?.gid ?: "0",
                entry.value?.mode ?: 0444
            ),
            "<WILL_BE_PROVIDED_DURING_DEPLOY>",
            "${namespace}_${entry.key}".toString()
        )
      }
      converted.first()
    } ?: []
  }

  List<String> convertEnvironment(String workingDir, List<String> envFiles, Environment environment) {
    List<String> entries = []
    envFiles.each { filename ->
      File file = new File(filename)
      if (!file.isAbsolute()) {
        file = new File(workingDir, filename)
      }
      entries.addAll(new EnvFileParser().parse(file))
    }
    entries.addAll(environment?.entries?.collect { name, value ->
      return "${name}=${value}" as String
    } ?: [])
    return entries
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
        order          : updateConfig.order
    ]
  }

  def convertServiceNetworks(
      Map<String, ServiceNetwork> serviceNetworks,
      Map<String, de.gesellix.docker.compose.types.StackNetwork> networkConfigs,
      String namespace,
      String serviceName) {

    boolean isWindows = LocalDocker.isNativeWindows(dockerClient)

    if (serviceNetworks == null || serviceNetworks.isEmpty()) {
      serviceNetworks = ["default": null as ServiceNetwork]
    }

    def serviceNetworkConfigs = []

    serviceNetworks.each { networkName, serviceNetwork ->
      if (!networkConfigs?.containsKey(networkName) && networkName != "default") {
        throw new IllegalStateException("service ${serviceName} references network ${networkName}, which is not declared")
      }

      List<String> aliases = []
      if (serviceNetwork) {
        aliases = serviceNetwork.aliases
      }

      String target = getTargetNetworkName(namespace, networkName, networkConfigs)
      if (isUserDefined(target, isWindows)) {
        aliases << serviceName
      }

      serviceNetworkConfigs << [
          target : target,
          aliases: aliases,
      ]
    }

    Collections.sort(serviceNetworkConfigs, new NetworkConfigByTargetComparator())
    return serviceNetworkConfigs
  }

  String getTargetNetworkName(String namespace, String networkName, Map<String, de.gesellix.docker.compose.types.StackNetwork> networkConfigs) {
    if (networkConfigs?.containsKey(networkName)) {
      de.gesellix.docker.compose.types.StackNetwork networkConfig = networkConfigs[networkName]
      if (networkConfig?.external?.external) {
        if (networkConfig?.external?.name) {
          return networkConfig.external.name
        }
        else {
          return networkName
        }
      }
    }
    return "${namespace}_${networkName}" as String
  }

  static class NetworkConfigByTargetComparator implements Comparator {

    @Override
    int compare(Object o1, Object o2) {
      return o1.target.compareTo(o2.target)
    }
  }

  TaskSpecLogDriver logDriver(Logging logging) {
    if (logging) {
      return new TaskSpecLogDriver(
          logging.driver,
          logging.options
      )
    }
    return null
  }

  HealthConfig convertHealthcheck(Healthcheck healthcheck) {
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
      return new HealthConfig(["NONE"], null, null, null, null)
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

    return new HealthConfig(
        healthcheck.test.parts,
        interval?.intValue() ?: 0,
        timeout?.intValue() ?: 0,
        retries,
        null
    )
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

  TaskSpecRestartPolicy restartPolicy(String restart, RestartPolicy restartPolicy) {
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
          return new TaskSpecRestartPolicy(
              TaskSpecRestartPolicy.Condition.Any,
              null,
              null,
              null
          )

        case "on-failure":
          return new TaskSpecRestartPolicy(
              TaskSpecRestartPolicy.Condition.OnMinusFailure,
              null,
              policy.maximumRetryCount as int,
              null
          )

        default:
          throw new IllegalArgumentException("unknown restart policy: ${restart}")
      }
    }
    else {
      Long delay = null
      if (restartPolicy.delay) {
        delay = parseDuration(restartPolicy.delay).toNanos()
      }
      Long window = null
      if (restartPolicy.window) {
        window = parseDuration(restartPolicy.window).toNanos()
      }
      return new TaskSpecRestartPolicy(
          TaskSpecRestartPolicy.Condition.values().find { it.value == restartPolicy.condition },
          delay,
          restartPolicy.maxAttempts,
          window
      )
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

  TaskSpecResources serviceResources(Resources resources) {
    if (resources?.limits || resources?.reservations) {
      def nanoMultiplier = Math.pow(10, 9)
      return new TaskSpecResources(
          getTaskSpecResourcesLimits(resources?.limits, nanoMultiplier),
          getTaskSpecResourcesReservation(resources?.reservations, nanoMultiplier)
      )
    }
    return new TaskSpecResources()
  }

  Limit getTaskSpecResourcesLimits(Limits limits, double nanoMultiplier) {
    if (limits) {
      if (limits.nanoCpus) {
        if (limits.nanoCpus.contains('/')) {
          // TODO
          throw new UnsupportedOperationException("not supported, yet")
        }
        else {
          return new Limit(
              (parseDouble(limits.nanoCpus) * nanoMultiplier).longValue(),
              parseLong(limits.memory),
              null
          )
        }
      }
      return new Limit(
          null,
          parseLong(limits.memory),
          null
      )
    }
    return null
  }

  ResourceObject getTaskSpecResourcesReservation(Reservations reservations, double nanoMultiplier) {
    if (reservations) {
      if (reservations.nanoCpus) {
        if (reservations.nanoCpus.contains('/')) {
          // TODO
          throw new UnsupportedOperationException("not supported, yet")
        }
        else {
          return new ResourceObject(
              (parseDouble(reservations.nanoCpus) * nanoMultiplier).longValue(),
              parseLong(reservations.memory),
              null
          )
        }
      }
      return new ResourceObject(
          null,
          parseLong(reservations.memory),
          null
      )
    }
    return null
  }

  List<Mount> volumesToMounts(String namespace, List<ServiceVolume> serviceVolumes, Map<String, StackVolume> stackVolumes) {
    List<Mount> mounts = serviceVolumes.collect { serviceVolume ->
      return volumeToMount(namespace, serviceVolume, stackVolumes)
    }
    return mounts
  }

  Mount volumeToMount(String namespace, ServiceVolume volumeSpec, Map<String, StackVolume> stackVolumes) {
    if (volumeSpec.source == "") {
      // Anonymous volume
      return new Mount(
          volumeSpec.target,
          null,
          Mount.Type.values().find { it.getValue() == volumeSpec.type },
          null,
          null,
          null,
          null,
          null
      )
    }

    if (volumeSpec.type == ServiceVolumeType.TypeBind.typeName) {
      return new Mount(
          volumeSpec.target,
          volumeSpec.source,
          Mount.Type.Bind,
          volumeSpec.readOnly,
          null,
          getBindOptions(volumeSpec.bind),
          null,
          null)
    }

    if (!stackVolumes.containsKey(volumeSpec.source)) {
      throw new IllegalArgumentException("undefined volume: ${volumeSpec.source}")
    }
    def stackVolume = stackVolumes[volumeSpec.source]

    String source = volumeSpec.source
    MountVolumeOptions volumeOptions
    if (stackVolume?.external?.name) {
      volumeOptions = new MountVolumeOptions(
          volumeSpec.volume?.noCopy ?: false,
          null,
          null)
      source = stackVolume.external.name
    }
    else {
      def labels = stackVolume?.labels?.entries ?: [:]
      labels[(ManageStackClient.LabelNamespace)] = namespace
      volumeOptions = new MountVolumeOptions(
          volumeSpec.volume?.noCopy ?: false,
          labels,
          getMountVolumeOptionsDriverConfig(stackVolume)
      )
      source = "${namespace}_${volumeSpec.source}" as String
      if (stackVolume?.name) {
        source = stackVolume.name
      }
    }

    return new Mount(
        volumeSpec.target,
        source,
        Mount.Type.Volume,
        volumeSpec.readOnly,
        null,
        null,
        volumeOptions,
        null
    )
  }

  boolean isReadOnly(List<String> modes) {
    return modes.contains("ro")
  }

  boolean isNoCopy(List<String> modes) {
    return modes.contains("nocopy")
  }

  MountVolumeOptionsDriverConfig getMountVolumeOptionsDriverConfig(StackVolume stackVolume) {
    if (stackVolume?.driver && stackVolume?.driver != "") {
      return new MountVolumeOptionsDriverConfig(
          stackVolume.driver,
          stackVolume.driverOpts.options
      )
    }
    return null
  }

  MountBindOptions getBindOptions(ServiceVolumeBind bind) {
    if (bind?.propagation) {
      return new MountBindOptions(MountBindOptions.Propagation.values().find { it.getValue() == bind.propagation }, null)
    }
    else {
      return null
    }
  }

  def serviceEndpoints(String endpointMode, PortConfigs portConfigs) {
    def endpointSpec = [
        mode : endpointMode ? EndpointSpec.Mode.values().find { it.value == endpointMode }.value : EndpointSpec.Mode.Vip.value,
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
        return [global: [:]]

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
      Map<String, de.gesellix.docker.compose.types.StackNetwork> networks) {
    Map<String, StackNetwork> networkSpec = [:]

    def externalNetworkNames = []
    serviceNetworkNames.each { String internalName ->
      def network = networks[internalName]
      if (!network) {
        def createOpts = new StackNetwork()
        createOpts.labels = [(ManageStackClient.LabelNamespace): namespace]
        createOpts.driver = "overlay"
        networkSpec[internalName] = createOpts
      }
      else if (network?.external?.external) {
        externalNetworkNames << (network.external.name ?: internalName)
      }
      else {
        def createOpts = new StackNetwork()

        Map<String, String> labels = [:]
        labels.putAll(network.labels?.entries ?: [:])
        labels[(ManageStackClient.LabelNamespace)] = namespace
        createOpts.labels = labels
        createOpts.driver = network.driver ?: "overlay"
        createOpts.driverOpts = network.driverOpts.options
        createOpts.internal = Boolean.valueOf(network.internal)
        createOpts.attachable = network.attachable
        if (network.ipam?.driver || network.ipam?.config) {
          createOpts.ipam = [:]
        }
        if (network.ipam?.driver) {
          createOpts.ipam.driver = network.ipam.driver
        }
        if (network.ipam?.config) {
          createOpts.ipam.config = []
          network.ipam.config.each { IpamConfig config ->
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

  boolean isContainerNetwork(String networkName) {
    String[] elements = networkName?.split(':', 2)
    return elements?.size() > 1 && elements[0] == "container"
  }

  boolean isUserDefined(String networkName, boolean isWindows) {
    List<String> blacklist = isWindows ? ["default", "none", "nat"] : ["default", "bridge", "host", "none"]
    return !(networkName in blacklist || isContainerNetwork(networkName))
  }

  def validateExternalNetworks(List<String> externalNetworks) {
    boolean isWindows = LocalDocker.isNativeWindows(dockerClient)
    externalNetworks.findAll { name ->
      // Networks that are not user defined always exist on all nodes as
      // local-scoped networks, so there's no need to inspect them.
      isUserDefined(name, isWindows)
    }.each { name ->
      def network
      try {
        network = dockerClient.inspectNetwork(name)
      }
      catch (Exception e) {
        log.error("network ${name} is declared as external, but could not be inspected. You need to create the network before the stack is deployed (with overlay driver)")
        throw new IllegalStateException("network ${name} is declared as external, but could not be inspected.", e)
      }

      if (network.content.Scope != "swarm") {
        log.error("network ${name} is declared as external, but it is not in the right scope: '${network.content.Scope}' instead of 'swarm'")
        throw new IllegalStateException("network ${name} is declared as external, but is not in 'swarm' scope.")
      }
    }
  }

  Map<String, StackSecret> secrets(String namespace, Map<String, de.gesellix.docker.compose.types.StackSecret> secrets, String workingDir) {
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

  Map<String, StackConfig> configs(String namespace, Map<String, de.gesellix.docker.compose.types.StackConfig> configs, String workingDir) {
    Map<String, StackConfig> configSpec = [:]
    configs.each { name, config ->
      if (!config.external.external) {
        Path filePath = Paths.get(workingDir, config.file)
        byte[] data = Files.readAllBytes(filePath)

        def labels = new HashMap<String, String>()
        if (config.labels?.entries) {
          labels.putAll(config.labels.entries)
        }
        labels[ManageStackClient.LabelNamespace] = namespace

        configSpec[name] = new StackConfig(
            name: ("${namespace}_${name}" as String),
            data: data,
            labels: labels
        )
      }
    }
    log.info("config ${configSpec.keySet()}")
    return configSpec
  }

  List<TaskSpecPlacementPreferences> placementPreferences(List<de.gesellix.docker.compose.types.PlacementPreferences> preferences) {
    log.info("placementPreferences: ${preferences}")
    if (preferences == null) {
      return null
    }
    def spread = new TaskSpecPlacementSpread(preferences[0].spread)
    log.info("spread: ${spread}")
    return [new TaskSpecPlacementPreferences(spread)]
  }
}
