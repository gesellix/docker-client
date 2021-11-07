package de.gesellix.docker.client.stack

import de.gesellix.docker.client.authentication.ManageAuthentication
import de.gesellix.docker.client.config.ManageConfig
import de.gesellix.docker.client.network.ManageNetwork
import de.gesellix.docker.client.node.ManageNode
import de.gesellix.docker.client.secret.ManageSecret
import de.gesellix.docker.client.service.ManageService
import de.gesellix.docker.client.stack.types.StackConfig
import de.gesellix.docker.client.stack.types.StackSecret
import de.gesellix.docker.client.system.ManageSystem
import de.gesellix.docker.client.tasks.ManageTask
import de.gesellix.docker.engine.EngineResponse
import de.gesellix.docker.remote.api.ConfigSpec
import de.gesellix.docker.remote.api.NetworkCreateRequest
import de.gesellix.docker.remote.api.NodeState
import de.gesellix.docker.remote.api.SecretSpec
import de.gesellix.docker.remote.api.Service
import de.gesellix.docker.remote.api.ServiceSpec
import de.gesellix.docker.remote.api.Task
import de.gesellix.docker.remote.api.TaskSpecContainerSpecConfigs
import de.gesellix.docker.remote.api.TaskSpecContainerSpecSecrets
import de.gesellix.docker.remote.api.TaskState
import groovy.util.logging.Slf4j

@Slf4j
class ManageStackClient implements ManageStack {

  private ManageService manageService
  private ManageTask manageTask
  private ManageNode manageNode
  private ManageNetwork manageNetwork
  private ManageSecret manageSecret
  private ManageConfig manageConfig
  private ManageSystem manageSystem
  private ManageAuthentication manageAuthentication

  ManageStackClient(
      ManageService manageService,
      ManageTask manageTask,
      ManageNode manageNode,
      ManageNetwork manageNetwork,
      ManageSecret manageSecret,
      ManageConfig manageConfig,
      ManageSystem manageSystem,
      ManageAuthentication manageAuthentication) {
    this.manageService = manageService
    this.manageTask = manageTask
    this.manageNode = manageNode
    this.manageNetwork = manageNetwork
    this.manageSecret = manageSecret
    this.manageConfig = manageConfig
    this.manageSystem = manageSystem
    this.manageAuthentication = manageAuthentication
  }

  @Override
  Collection<Stack> lsStacks() {
    log.info("docker stack ls")

    Map<String, Stack> stacksByName = [:]

    EngineResponse services = manageService.services([filters: [label: [(LabelNamespace): true]]])
    services.content?.each { service ->
      String stackName = service.spec.labels[(LabelNamespace)]
      if (!stacksByName[(stackName)]) {
        stacksByName[(stackName)] = new Stack(name: stackName, services: 0)
      }
      stacksByName[(stackName)].services++
    }

    return stacksByName.values()
  }

  @Override
  void stackDeploy(String namespace, DeployStackConfig config, DeployStackOptions options) {
    log.info("docker stack deploy")

    checkDaemonIsSwarmManager()

    if (options.pruneServices) {
      def serviceNames = config.services.keySet()
      pruneServices(namespace, serviceNames)
    }

    createNetworks(namespace, config.networks)
    Map<String, String> changedSecrets = createSecrets(namespace, config.secrets)
    Map<String, String> changedConfigs = createConfigs(namespace, config.configs)

    config.services.each { service ->
      def containerSpecSecrets = service.value.taskTemplate?.containerSpec?.secrets
      if (containerSpecSecrets) {
        changedSecrets.each { name, secretId ->
          int index = containerSpecSecrets.findIndexOf { it.secretName == name }
          if (index >= 0) {
            containerSpecSecrets.set(index, new TaskSpecContainerSpecSecrets(
                containerSpecSecrets.get(index).file,
                secretId,
                name
            ))
          }
        }
      }

      def containerSpecConfigs = service.value.taskTemplate?.containerSpec?.configs
      if (containerSpecConfigs) {
        changedConfigs.each { name, configId ->
          int index = containerSpecConfigs.findIndexOf { it.configName == name }
          if (index >= 0) {
            containerSpecConfigs.set(index, new TaskSpecContainerSpecConfigs(
                containerSpecConfigs.get(index).file,
                containerSpecConfigs.get(index).runtime,
                configId,
                name
            ))
          }
        }
      }
    }

    createOrUpdateServices(namespace, config.services, options.sendRegistryAuth)
  }

  void createNetworks(String namespace, Map<String, NetworkCreateRequest> networks) {
    def existingNetworks = manageNetwork.networks([
        filters: [
            label: [("${LabelNamespace}=${namespace}" as String): true]]])
    def existingNetworkNames = []
    existingNetworks.content.each {
      existingNetworkNames << it.name
    }
    networks.each { String name, NetworkCreateRequest network ->
      name = "${namespace}_${name}" as String
      if (!existingNetworkNames.contains(name)) {
        log.info("create network $name: $network")
        network.name = name
        if (!network.labels) {
          network.labels = [:]
        }
        network.labels[(LabelNamespace)] = namespace
        manageNetwork.createNetwork(network)
      }
    }
  }

  Map<String, String> createSecrets(String namespace, Map<String, StackSecret> secrets) {
    return secrets.collectEntries { name, secret ->
      List knownSecrets = manageSecret.secrets([filters: [name: [secret.name]]]).content
      log.debug("known: $knownSecrets")

      if (!secret.labels) {
        secret.labels = [:]
      }
      secret.labels[(LabelNamespace)] = namespace

      EngineResponse response
      if (knownSecrets.empty) {
        log.info("create secret ${secret.name}: $secret")
        response = manageSecret.createSecret(secret.name, secret.data, secret.labels)
      }
      else {
        if (knownSecrets.size() != 1) {
          throw new IllegalStateException("ambiguous secret name '${secret.name}'")
        }
        def knownSecret = knownSecrets.first()
        log.info("update secret ${secret.name}: $secret")
        response = manageSecret.updateSecret(
            knownSecret.ID,
            knownSecret.version.index,
            new SecretSpec(
                secret.name,
                secret.labels,
                new String(secret.data),
                secret.driver,
                secret.templating))
      }
      return [(secret.name): response.content.id]
    }
  }

  Map<String, String> createConfigs(String namespace, Map<String, StackConfig> configs) {
    return configs.collectEntries { name, config ->
      List knownConfigs = manageConfig.configs([filters: [name: [config.name]]]).content
      log.debug("known: $knownConfigs")

      if (!config.labels) {
        config.labels = [:]
      }
      config.labels[(LabelNamespace)] = namespace

      EngineResponse response
      if (knownConfigs.empty) {
        log.info("create config ${config.name}: $config")
        response = manageConfig.createConfig(config.name, config.data, config.labels)
      }
      else {
        if (knownConfigs.size() != 1) {
          throw new IllegalStateException("ambiguous config name '${config.name}'")
        }
        def knownConfig = knownConfigs.first()
        log.info("update config ${config.name}: $config")
        response = manageConfig.updateConfig(
            knownConfig.ID,
            knownConfig.version.index,
            new ConfigSpec(
                config.name,
                config.labels,
                new String(config.data),
                config.templating))
      }
      return [(config.name): response.content.id]
    }
  }

  void pruneServices(String namespace, Collection<String> services) {
    // Descope returns the name without the namespace prefix
    def descope = { String name ->
      return name.substring("${namespace}_".length())
    }

    def oldServices = stackServices(namespace)
    def pruneServices = oldServices.content.findResults {
      return services.contains(descope(it.Spec.Name as String)) ? null : it
    }

    pruneServices.each { service ->
      manageService.rmService(service.ID)
    }
  }

  void createOrUpdateServices(String namespace, Map<String, ServiceSpec> services, boolean sendRegistryAuth) {
    Map<String, Service> existingServicesByName = [:]
    def existingServices = stackServices(namespace)
    existingServices.content.each { service ->
      existingServicesByName[service.spec.name] = service
    }

    services.each { internalName, ServiceSpec serviceSpec ->
      def name = "${namespace}_${internalName}" as String
      serviceSpec.name = serviceSpec.name ?: name
      if (!serviceSpec.labels) {
        serviceSpec.labels = [:]
      }
      serviceSpec.labels[(LabelNamespace)] = namespace

      def encodedAuth = ""
      if (sendRegistryAuth) {
        // Retrieve encoded auth token from the image reference
        String image = serviceSpec.taskTemplate.containerSpec.image
        encodedAuth = manageAuthentication.retrieveEncodedAuthTokenForImage(image)
      }

      def service = existingServicesByName[name]
      if (service) {
        log.info("Updating service ${name} (id ${service.ID}): ${serviceSpec}")

        def updateOptions = [:]
        if (sendRegistryAuth) {
          updateOptions.EncodedRegistryAuth = encodedAuth
        }
        def response = manageService.updateService(
            service.ID,
            service.version.index,
            serviceSpec,
            null,
            sendRegistryAuth ? encodedAuth : null)
        response.content.warnings.each { String warning ->
          log.warn(warning)
        }
      }
      else {
        log.info("Creating service ${name}: ${serviceSpec}")

        Map<String, Object> createOptions = [:]
        if (sendRegistryAuth) {
          createOptions.EncodedRegistryAuth = encodedAuth
        }
        def response = manageService.createService(serviceSpec, sendRegistryAuth ? encodedAuth : null)
      }
    }
  }

  // checkDaemonIsSwarmManager does an Info API call to verify that the daemon is
  // a swarm manager. This is necessary because we must create networks before we
  // create services, but the API call for creating a network does not return a
  // proper status code when it can't create a network in the "global" scope.
  void checkDaemonIsSwarmManager() {
    if (!manageSystem.info()?.content?.swarm?.controlAvailable) {
      throw new IllegalStateException("This node is not a swarm manager. Use \"docker swarm init\" or \"docker swarm join\" to connect this node to swarm and try again.")
    }
  }

  @Override
  EngineResponse<List<Task>> stackPs(String namespace, Map filters = [:]) {
    log.info("docker stack ps")

    String namespaceFilter = "${LabelNamespace}=${namespace}"

    def actualFilters = filters ?: [:]
    if (actualFilters.label) {
      actualFilters.label[(namespaceFilter)] = true
    }
    else {
      actualFilters['label'] = [(namespaceFilter): true]
    }
    def tasks = manageTask.tasks([filters: actualFilters])
    return tasks
  }

  @Override
  void stackRm(String namespace) {
    log.info("docker stack rm")

    String namespaceFilter = "${LabelNamespace}=${namespace}"

    def services = manageService.services([filters: [label: [(namespaceFilter): true]]])
    def networks = manageNetwork.networks([filters: [label: [(namespaceFilter): true]]])
    def secrets = manageSecret.secrets([filters: [label: [(namespaceFilter): true]]])
    def configs = manageConfig.configs([filters: [label: [(namespaceFilter): true]]])

    services.content.each { service ->
      manageService.rmService(service.ID)
    }
    networks.content.each { network ->
      manageNetwork.rmNetwork(network.id)
    }
    secrets.content.each { secret ->
      manageSecret.rmSecret(secret.ID)
    }
    configs.content.each { config ->
      manageConfig.rmConfig(config.ID)
    }
  }

  @Override
  EngineResponse<List<Service>> stackServices(String namespace, Map filters = [:]) {
    log.info("docker stack services")

    String namespaceFilter = "${LabelNamespace}=${namespace}"
    def actualFilters = filters ?: [:]
    if (actualFilters.label) {
      actualFilters.label[(namespaceFilter)] = true
    }
    else {
      actualFilters['label'] = [(namespaceFilter): true]
    }
    def services = manageService.services([filters: actualFilters])
//    def infoByServiceId = getInfoByServiceId(services)
    return services
  }

  def getInfoByServiceId(EngineResponse<List<Service>> services) {
    def nodes = manageNode.nodes()
    List<String> activeNodes = nodes.content.findResults { node ->
      node.status.state != NodeState.Down ? node.ID : null
    }

    Map<String, Integer> running = [:]
    Map<String, Integer> tasksNoShutdown = [:]

    def serviceFilter = [service: [:]]
    services.content.each { service ->
      serviceFilter.service[(service.ID as String)] = true
    }
    def tasks = manageTask.tasks([filters: serviceFilter])
    tasks.content.each { task ->
      if (task.desiredState != TaskState.Shutdown) {
        if (!tasksNoShutdown[task.serviceID]) {
          tasksNoShutdown[task.serviceID] = 0
        }
        tasksNoShutdown[task.serviceID]++
      }
      if (activeNodes.contains(task.nodeID) && task.status.state == TaskState.Running) {
        if (!running[task.serviceID]) {
          running[task.serviceID] = 0
        }
        running[task.serviceID]++
      }
    }

    def infoByServiceId = [:]
    services.content.each { service ->
      if (service.spec.mode.replicated && service.spec.mode.replicated.replicas) {
        infoByServiceId[service.ID] = new ServiceInfo(mode: 'replicated', replicas: "${running[service.ID as String] ?: 0}/${service.spec.mode.replicated.replicas}")
      }
      else if (service.spec.mode.global) {
        infoByServiceId[service.ID] = new ServiceInfo(mode: 'global', replicas: "${running[service.ID as String] ?: 0}}/${tasksNoShutdown[service.ID as String]}")
      }
    }
    return infoByServiceId
  }

  static class ServiceInfo {

    String mode
    String replicas

    @Override
    String toString() {
      "$mode, $replicas"
    }
  }
}
