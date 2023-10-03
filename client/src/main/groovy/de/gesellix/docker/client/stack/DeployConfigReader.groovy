package de.gesellix.docker.client.stack

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.EnvFileParser
import de.gesellix.docker.client.LocalDocker
import de.gesellix.docker.client.stack.types.StackConfig
import de.gesellix.docker.client.stack.types.StackSecret
import de.gesellix.docker.compose.ComposeFileReader
import de.gesellix.docker.compose.types.ComposeConfig
import de.gesellix.docker.compose.types.Environment
import de.gesellix.docker.compose.types.ExtraHosts
import de.gesellix.docker.compose.types.Healthcheck
import de.gesellix.docker.compose.types.IpamConfig
import de.gesellix.docker.compose.types.Limits
import de.gesellix.docker.compose.types.Logging
import de.gesellix.docker.compose.types.PlacementPreferences
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
import de.gesellix.docker.compose.types.StackNetwork
import de.gesellix.docker.compose.types.StackService
import de.gesellix.docker.compose.types.StackVolume
import de.gesellix.docker.compose.types.UpdateConfig
import de.gesellix.docker.remote.api.EndpointPortConfig
import de.gesellix.docker.remote.api.EndpointSpec
import de.gesellix.docker.remote.api.HealthConfig
import de.gesellix.docker.remote.api.IPAM
import de.gesellix.docker.remote.api.IPAMConfig
import de.gesellix.docker.remote.api.Limit
import de.gesellix.docker.remote.api.Mount
import de.gesellix.docker.remote.api.MountBindOptions
import de.gesellix.docker.remote.api.MountVolumeOptions
import de.gesellix.docker.remote.api.MountVolumeOptionsDriverConfig
import de.gesellix.docker.remote.api.Network
import de.gesellix.docker.remote.api.NetworkAttachmentConfig
import de.gesellix.docker.remote.api.NetworkCreateRequest
import de.gesellix.docker.remote.api.ResourceObject
import de.gesellix.docker.remote.api.ServiceSpec
import de.gesellix.docker.remote.api.ServiceSpecMode
import de.gesellix.docker.remote.api.ServiceSpecModeReplicated
import de.gesellix.docker.remote.api.ServiceSpecUpdateConfig
import de.gesellix.docker.remote.api.TaskSpec
import de.gesellix.docker.remote.api.TaskSpecContainerSpec
import de.gesellix.docker.remote.api.TaskSpecContainerSpecConfigsInner
import de.gesellix.docker.remote.api.TaskSpecContainerSpecConfigsInnerFile
import de.gesellix.docker.remote.api.TaskSpecContainerSpecSecretsInner
import de.gesellix.docker.remote.api.TaskSpecContainerSpecSecretsInnerFile
import de.gesellix.docker.remote.api.TaskSpecLogDriver
import de.gesellix.docker.remote.api.TaskSpecPlacement
import de.gesellix.docker.remote.api.TaskSpecPlacementPreferencesInner
import de.gesellix.docker.remote.api.TaskSpecPlacementPreferencesInnerSpread
import de.gesellix.docker.remote.api.TaskSpecResources
import de.gesellix.docker.remote.api.TaskSpecRestartPolicy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.regex.Matcher
import java.util.regex.Pattern

import static java.lang.Double.parseDouble
import static java.lang.Integer.parseInt
import static java.lang.Long.parseLong

class DeployConfigReader {

  private final Logger log = LoggerFactory.getLogger(DeployConfigReader)

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

    List<String> serviceNetworkNames = composeConfig.services.collect { String name, StackService service ->
      if (!service.networks) {
        return ["default"]
      }
      return service.networks.collect { String networkName, ServiceNetwork serviceNetwork ->
        networkName
      }
    }.flatten().unique()
    log.info("service network names: ${serviceNetworkNames}")

    Map<String, NetworkCreateRequest> networkConfigs
    List<String> externalNetworks
    (networkConfigs, externalNetworks) = networks(namespace, serviceNetworkNames, composeConfig.networks ?: [:])
    Map<String, StackSecret> secrets = secrets(namespace, composeConfig.secrets, workingDir)
    Map<String, StackConfig> configs = configs(namespace, composeConfig.configs, workingDir)
    Map<String, ServiceSpec> services = services(namespace, composeConfig.services, composeConfig.networks, composeConfig.volumes)

    DeployStackConfig cfg = new DeployStackConfig()
    cfg.networks = networkConfigs
    cfg.secrets = secrets
    cfg.configs = configs
    cfg.services = services
    return cfg
  }

  Map<String, ServiceSpec> services(
      String namespace,
      Map<String, StackService> services,
      Map<String, StackNetwork> networks,
      Map<String, StackVolume> volumes) {
    Map<String, ServiceSpec> serviceSpec = [:]
    services.each { String name, StackService service ->
      Map<String, String> serviceLabels = service.deploy?.labels?.entries ?: [:]
      serviceLabels[ManageStackClient.LabelNamespace] = namespace

      Map<String, String> containerLabels = service.labels?.entries ?: [:]
      containerLabels[ManageStackClient.LabelNamespace] = namespace

      Long stopGracePeriod = null
      if (service.stopGracePeriod) {
        stopGracePeriod = parseDuration(service.stopGracePeriod).toNanos()
      }

      List<String> env = convertEnvironment(service.workingDir, service.envFile, service.environment)
      Collections.sort(env)

      List<String> extraHosts = convertExtraHosts(service.extraHosts)
      Collections.sort(extraHosts)

      ServiceSpec serviceConfig = new ServiceSpec()
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

  List<TaskSpecContainerSpecConfigsInner> prepareServiceConfigs(String namespace, List<Map<String, ServiceConfig>> configs) {
    configs?.collect { Map<String, ServiceConfig> item ->
      if (item.size() > 1) {
        throw new RuntimeException("expected a unique config entry")
      }
      List<TaskSpecContainerSpecConfigsInner> converted = item.entrySet().collect { Map.Entry<String, ServiceConfig> entry ->
        new TaskSpecContainerSpecConfigsInner(
            new TaskSpecContainerSpecConfigsInnerFile(
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

  List<TaskSpecContainerSpecSecretsInner> prepareServiceSecrets(String namespace, List<Map<String, ServiceSecret>> secrets) {
    secrets?.collect { Map<String, ServiceSecret> item ->
      if (item.size() > 1) {
        throw new RuntimeException("expected a unique secret entry")
      }
      List<TaskSpecContainerSpecSecretsInner> converted = item.entrySet().collect { Map.Entry<String, ServiceSecret> entry ->
        new TaskSpecContainerSpecSecretsInner(
            new TaskSpecContainerSpecSecretsInnerFile(
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
    envFiles.each { String filename ->
      File file = new File(filename)
      if (!file.isAbsolute()) {
        file = new File(workingDir, filename)
      }
      entries.addAll(new EnvFileParser().parse(file))
    }
    entries.addAll(environment?.entries?.collect { String name, String value ->
      return "${name}=${value}" as String
    } ?: [])
    return entries
  }

  List<String> convertExtraHosts(ExtraHosts extraHosts) {
    extraHosts?.entries?.collect { String host, String ip ->
      "${host} ${ip}" as String
    } ?: []
  }

  ServiceSpecUpdateConfig convertUpdateConfig(UpdateConfig updateConfig) {
    if (!updateConfig) {
      return null
    }

    long parallel = 1
    if (updateConfig.parallelism) {
      parallel = updateConfig.parallelism
    }

    long delay = 0
    if (updateConfig.delay) {
      delay = parseDuration(updateConfig.delay).toNanos()
    }

    long monitor = 0
    if (updateConfig.monitor) {
      monitor = parseDuration(updateConfig.monitor).toNanos()
    }

    return new ServiceSpecUpdateConfig(
        parallel,
        delay,
        updateConfig.failureAction
            ? ServiceSpecUpdateConfig.FailureAction.values().find { ServiceSpecUpdateConfig.FailureAction action -> action.value == updateConfig.failureAction }
            : null,
        monitor,
        new BigDecimal(updateConfig.maxFailureRatio),
        updateConfig.order
            ? ServiceSpecUpdateConfig.Order.values().find { ServiceSpecUpdateConfig.Order order -> order.value == updateConfig.order }
            : null
    )
  }

  List<NetworkAttachmentConfig> convertServiceNetworks(
      Map<String, ServiceNetwork> serviceNetworks,
      Map<String, StackNetwork> networkConfigs,
      String namespace,
      String serviceName) {

    boolean isWindows = LocalDocker.isNativeWindows(dockerClient)

    if (serviceNetworks == null || serviceNetworks.isEmpty()) {
      serviceNetworks = ["default": null as ServiceNetwork]
    }

    List<NetworkAttachmentConfig> serviceNetworkConfigs = []

    serviceNetworks.each { String networkName, ServiceNetwork serviceNetwork ->
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

      serviceNetworkConfigs << new NetworkAttachmentConfig(
          target,
          aliases,
          null
      )
    }

    Collections.sort(serviceNetworkConfigs, new NetworkConfigByTargetComparator())
    return serviceNetworkConfigs
  }

  String getTargetNetworkName(String namespace, String networkName, Map<String, StackNetwork> networkConfigs) {
    if (networkConfigs?.containsKey(networkName)) {
      StackNetwork networkConfig = networkConfigs[networkName]
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

  static class NetworkConfigByTargetComparator implements Comparator<NetworkAttachmentConfig> {

    @Override
    int compare(NetworkAttachmentConfig o1, NetworkAttachmentConfig o2) {
      return o1.target <=> o2.target
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
    Long startPeriod = null
    Long startInterval = null

    if (healthcheck.disable) {
      if (healthcheck.test?.parts) {
        throw new IllegalArgumentException("test and disable can't be set at the same time")
      }
      return new HealthConfig(["NONE"], null, null, null, null, null)
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
    if (healthcheck.startPeriod) {
      startPeriod = parseDuration(healthcheck.startPeriod).toNanos()
    }
    // TODO add this one
//    if (healthcheck.startInterval) {
//      startInterval = parseDuration(healthcheck.startInterval).toNanos()
//    }

    return new HealthConfig(
        healthcheck.test.parts,
        interval ?: 0,
        timeout ?: 0,
        retries,
        startPeriod,
        startInterval
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

  final String numberWithUnitRegex = /(\d*)\.?(\d*)(\D+)/
  final Pattern pattern = Pattern.compile(numberWithUnitRegex)

  Duration parseDuration(String durationAsString) {
    String sign = '+'
    if (durationAsString.matches(/[-+].+/)) {
      sign = durationAsString.substring(0, '-'.length())
      durationAsString = durationAsString.substring('-'.length())
    }
    Matcher matcher = pattern.matcher(durationAsString)

    Duration duration = Duration.of(0, ChronoUnit.NANOS)
    boolean ok = false
    while (matcher.find()) {
      if (matcher.groupCount() != 3) {
        throw new IllegalStateException("expected 3 groups, but got ${matcher.groupCount()}")
      }
      String pre = matcher.group(1) ?: "0"
      String post = matcher.group(2) ?: "0"
      String symbol = matcher.group(3)
      if (!symbol) {
        throw new IllegalArgumentException("missing unit in duration '${durationAsString}'")
      }
      ChronoUnit unit = unitBySymbol[symbol]
      if (!unit) {
        throw new IllegalArgumentException("unknown unit ${symbol} in duration '${durationAsString}'")
      }

      double scale = Math.pow(10, post.length())

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
      Map<String, String> policy = parseRestartPolicy(restart)
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

  Map<String, String> parseRestartPolicy(String policy) {
    Map<String, String> restartPolicy = [
        name: ""
    ]
    if (!policy) {
      return restartPolicy
    }

    String[] parts = policy.split(':')
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
      double nanoMultiplier = Math.pow(10, 9)
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
    StackVolume stackVolume = stackVolumes[volumeSpec.source]

    String source = volumeSpec.source
    MountVolumeOptions volumeOptions
    if (stackVolume?.external?.name) {
      volumeOptions = new MountVolumeOptions(
          volumeSpec.volume?.nocopy ?: false,
          null,
          null)
      source = stackVolume.external.name
    }
    else {
      Map<String, String> labels = stackVolume?.labels?.entries ?: [:]
      labels[(ManageStackClient.LabelNamespace)] = namespace
      volumeOptions = new MountVolumeOptions(
          volumeSpec.volume?.nocopy ?: false,
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
      return new MountBindOptions(MountBindOptions.Propagation.values().find { MountBindOptions.Propagation propagation -> propagation.getValue() == bind.propagation }, null)
    }
    else {
      return null
    }
  }

  EndpointSpec serviceEndpoints(String endpointMode, PortConfigs portConfigs) {
    EndpointSpec endpointSpec = new EndpointSpec(
        endpointMode ? EndpointSpec.Mode.values().find { it.value == endpointMode } : EndpointSpec.Mode.Vip,
        portConfigs.portConfigs.collect { portConfig ->
          new EndpointPortConfig(
              null,
              portConfig.protocol ? EndpointPortConfig.Protocol.values().find { it.value == portConfig.protocol } : null,
              portConfig.target,
              portConfig.published,
              portConfig.mode ? EndpointPortConfig.PublishMode.values().find { it.value == portConfig.mode } : null
          )
        }
    )
    return endpointSpec
  }

  ServiceSpecMode serviceMode(String mode, Integer replicas) {
    switch (mode) {
      case "global":
        if (replicas) {
          throw new IllegalArgumentException("replicas can only be used with replicated mode")
        }
        return new ServiceSpecMode(null, [:], null, null)

      case null:
      case "":
      case "replicated":
        return new ServiceSpecMode(new ServiceSpecModeReplicated(replicas ?: 1), null, null, null)

      default:
        throw new IllegalArgumentException("Unknown mode: '$mode'")
    }
  }

  Tuple2<Map<String, NetworkCreateRequest>, List<String>> networks(
      String namespace,
      List<String> serviceNetworkNames,
      Map<String, StackNetwork> networks) {
    Map<String, NetworkCreateRequest> networkSpec = [:]

    List<String> externalNetworkNames = []
    serviceNetworkNames.each { String internalName ->
      StackNetwork network = networks[internalName]
      if (!network) {
        networkSpec[internalName] = new NetworkCreateRequest(
            internalName,
            true,
            "overlay",
            null, null,
            null, null, null,
            null,
            getLabels(namespace, null)
        )
      }
      else if (network?.external?.external) {
        externalNetworkNames << (network.external.name ?: internalName)
      }
      else {
        networkSpec[internalName] = new NetworkCreateRequest(
            internalName,
            true,
            network.driver ?: "overlay",
            Boolean.valueOf(network.internal),
            network.attachable,
            null,
            getIpam(network),
            null,
            network.driverOpts.options,
            getLabels(namespace, network)
        )
      }
    }

    log.info("network configs: ${networkSpec}")
    log.info("external networks: ${externalNetworkNames}")

    validateExternalNetworks(externalNetworkNames)

    return [networkSpec, externalNetworkNames]
  }

  IPAM getIpam(StackNetwork network) {
    if (!network) {
      return null
    }
    if (!network.ipam?.driver && !network.ipam?.config) {
      return null
    }
    List<IPAMConfig> ipamConfig = []
    if (network.ipam?.config) {
      network.ipam.config.each { IpamConfig config ->
        ipamConfig << new IPAMConfig().tap {
          subnet = config.subnet
        }
      }
    }
    return new IPAM(
        network.ipam?.driver,
        ipamConfig,
        null
    )
  }

  Map<String, String> getLabels(String namespace, StackNetwork network) {
    Map<String, String> labels = [:]
    labels.putAll(network?.labels?.entries ?: [:])
    labels[(ManageStackClient.LabelNamespace)] = namespace
    return labels
  }

  boolean isContainerNetwork(String networkName) {
    String[] elements = networkName?.split(':', 2)
    return elements?.size() > 1 && elements[0] == "container"
  }

  boolean isUserDefined(String networkName, boolean isWindows) {
    List<String> blacklist = isWindows ? ["default", "none", "nat"] : ["default", "bridge", "host", "none"]
    return !(networkName in blacklist || isContainerNetwork(networkName))
  }

  void validateExternalNetworks(List<String> externalNetworks) {
    boolean isWindows = LocalDocker.isNativeWindows(dockerClient)
    externalNetworks.findAll { name ->
      // Networks that are not user defined always exist on all nodes as
      // local-scoped networks, so there's no need to inspect them.
      isUserDefined(name, isWindows)
    }.each { name ->
      Network network
      try {
        network = dockerClient.inspectNetwork(name).content
      }
      catch (Exception e) {
        log.error("network ${name} is declared as external, but could not be inspected. You need to create the network before the stack is deployed (with overlay driver)")
        throw new IllegalStateException("network ${name} is declared as external, but could not be inspected.", e)
      }

      if (network.scope != "swarm") {
        log.error("network ${name} is declared as external, but it is not in the right scope: '${network.scope}' instead of 'swarm'")
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

        Map<String, String> labels = new HashMap<String, String>()
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

        Map<String, String> labels = new HashMap<String, String>()
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

  List<TaskSpecPlacementPreferencesInner> placementPreferences(List<PlacementPreferences> preferences) {
    log.info("placementPreferences: ${preferences}")
    if (preferences == null) {
      return null
    }
    TaskSpecPlacementPreferencesInnerSpread spread = new TaskSpecPlacementPreferencesInnerSpread(preferences[0].spread)
    log.info("spread: ${spread}")
    return [new TaskSpecPlacementPreferencesInner(spread)]
  }
}
