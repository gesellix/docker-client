package de.gesellix.docker.client.stack

import de.gesellix.docker.client.EngineResponseContent
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
import de.gesellix.docker.remote.api.Config
import de.gesellix.docker.remote.api.ConfigSpec
import de.gesellix.docker.remote.api.Network
import de.gesellix.docker.remote.api.NetworkCreateRequest
import de.gesellix.docker.remote.api.Node
import de.gesellix.docker.remote.api.NodeState
import de.gesellix.docker.remote.api.Secret
import de.gesellix.docker.remote.api.SecretSpec
import de.gesellix.docker.remote.api.Service
import de.gesellix.docker.remote.api.ServiceCreateResponse
import de.gesellix.docker.remote.api.ServiceSpec
import de.gesellix.docker.remote.api.ServiceUpdateResponse
import de.gesellix.docker.remote.api.Task
import de.gesellix.docker.remote.api.TaskSpecContainerSpecConfigsInner
import de.gesellix.docker.remote.api.TaskSpecContainerSpecSecretsInner
import de.gesellix.docker.remote.api.TaskState
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ManageStackClient implements ManageStack {

  private final Logger log = LoggerFactory.getLogger(ManageStackClient)

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

    EngineResponseContent<List<Service>> services = manageService.services([filters: [label: [(LabelNamespace): true]]])
    services.content?.each { Service service ->
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
      Set<String> serviceNames = config.services.keySet()
      pruneServices(namespace, serviceNames)
    }

    createNetworks(namespace, config.networks)
    Map<String, String> changedSecrets = createSecrets(namespace, config.secrets)
    Map<String, String> changedConfigs = createConfigs(namespace, config.configs)

    config.services.each { Map.Entry<String, ServiceSpec> service ->
      List<TaskSpecContainerSpecSecretsInner> containerSpecSecrets = service.value.taskTemplate?.containerSpec?.secrets
      if (containerSpecSecrets) {
        changedSecrets.each { String name, String secretId ->
          int index = containerSpecSecrets.findIndexOf { TaskSpecContainerSpecSecretsInner spec -> spec.secretName == name }
          if (index >= 0) {
            containerSpecSecrets.set(index, new TaskSpecContainerSpecSecretsInner(
                containerSpecSecrets.get(index).file,
                secretId,
                name
            ))
          }
        }
      }

      List<TaskSpecContainerSpecConfigsInner> containerSpecConfigs = service.value.taskTemplate?.containerSpec?.configs
      if (containerSpecConfigs) {
        changedConfigs.each { String name, String configId ->
          int index = containerSpecConfigs.findIndexOf { TaskSpecContainerSpecConfigsInner spec -> spec.configName == name }
          if (index >= 0) {
            containerSpecConfigs.set(index, new TaskSpecContainerSpecConfigsInner(
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
    EngineResponseContent<List<Network>> existingNetworks = manageNetwork.networks([
        filters: [
            label: [("${LabelNamespace}=${namespace}" as String): true]]])
    List<String> existingNetworkNames = []
    existingNetworks.content.each { Network network ->
      existingNetworkNames << network.name
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
    return secrets.collectEntries { String name, StackSecret secret ->
      List knownSecrets = manageSecret.secrets([filters: [name: [secret.name]]]).content
      log.debug("known: $knownSecrets")

      if (!secret.labels) {
        secret.labels = [:]
      }
      secret.labels[(LabelNamespace)] = namespace

      String secretId
      if (knownSecrets.empty) {
        log.info("create secret ${secret.name}: $secret")
        secretId = manageSecret.createSecret(secret.name, secret.data, secret.labels).content.id
      }
      else {
        if (knownSecrets.size() != 1) {
          throw new IllegalStateException("ambiguous secret name '${secret.name}'")
        }
        def knownSecret = knownSecrets.first()
        log.info("update secret ${secret.name}: $secret")
        secretId = knownSecret.ID
        manageSecret.updateSecret(
            knownSecret.ID,
            knownSecret.version.index,
            new SecretSpec(
                secret.name,
                secret.labels,
                new String(secret.data),
                secret.driver,
                secret.templating))
      }
      return [(secret.name): secretId]
    }
  }

  Map<String, String> createConfigs(String namespace, Map<String, StackConfig> configs) {
    return configs.collectEntries { String name, StackConfig config ->
      List knownConfigs = manageConfig.configs([filters: [name: [config.name]]]).content
      log.debug("known: $knownConfigs")

      if (!config.labels) {
        config.labels = [:]
      }
      config.labels[(LabelNamespace)] = namespace

      String configId
      if (knownConfigs.empty) {
        log.info("create config ${config.name}: $config")
        configId = manageConfig.createConfig(config.name, config.data, config.labels).content.id
      }
      else {
        if (knownConfigs.size() != 1) {
          throw new IllegalStateException("ambiguous config name '${config.name}'")
        }
        Config knownConfig = knownConfigs.first()
        log.info("update config ${config.name}: $config")
        configId = knownConfig.ID
        manageConfig.updateConfig(
            knownConfig.ID,
            knownConfig.version.index,
            new ConfigSpec(
                config.name,
                config.labels,
                new String(config.data),
                config.templating))
      }
      return [(config.name): configId]
    }
  }

  void pruneServices(String namespace, Collection<String> services) {
    // Descope returns the name without the namespace prefix
    Closure<String> descope = { String name ->
      return name.substring("${namespace}_".length())
    }

    EngineResponseContent<List<Service>> oldServices = stackServices(namespace)
    Collection<Service> pruneServices = oldServices.content.findResults { Service service ->
      return services.contains(descope(service.spec.name)) ? null : service
    }

    pruneServices.each { Service service ->
      manageService.rmService(service.ID)
    }
  }

  void createOrUpdateServices(String namespace, Map<String, ServiceSpec> services, boolean sendRegistryAuth) {
    Map<String, Service> existingServicesByName = [:]
    EngineResponseContent<List<Service>> existingServices = stackServices(namespace)
    existingServices.content.each { Service service ->
      existingServicesByName[service.spec.name] = service
    }

    services.each { String internalName, ServiceSpec serviceSpec ->
      String name = "${namespace}_${internalName}"
      serviceSpec.name = serviceSpec.name ?: name
      if (!serviceSpec.labels) {
        serviceSpec.labels = [:]
      }
      serviceSpec.labels[(LabelNamespace)] = namespace

      String encodedAuth = ""
      if (sendRegistryAuth) {
        // Retrieve encoded auth token from the image reference
        String image = serviceSpec.taskTemplate.containerSpec.image
        encodedAuth = manageAuthentication.retrieveEncodedAuthTokenForImage(image)
      }

      Service service = existingServicesByName[name]
      if (service) {
        log.info("Updating service ${name} (id ${service.ID}): ${serviceSpec}")

        Map updateOptions = [:]
        if (sendRegistryAuth) {
          updateOptions.EncodedRegistryAuth = encodedAuth
        }
        EngineResponseContent<ServiceUpdateResponse> response = manageService.updateService(
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
        EngineResponseContent<ServiceCreateResponse> response = manageService.createService(serviceSpec, sendRegistryAuth ? encodedAuth : null)
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
  EngineResponseContent<List<Task>> stackPs(String namespace, Map filters = [:]) {
    log.info("docker stack ps")

    String namespaceFilter = "${LabelNamespace}=${namespace}"

    Map actualFilters = filters ?: [:]
    if (actualFilters.label) {
      actualFilters.label[(namespaceFilter)] = true
    }
    else {
      actualFilters['label'] = [(namespaceFilter): true]
    }
    EngineResponseContent<List<Task>> tasks = manageTask.tasks([filters: actualFilters])
    return tasks
  }

  @Override
  void stackRm(String namespace) {
    log.info("docker stack rm")

    String namespaceFilter = "${LabelNamespace}=${namespace}"

    EngineResponseContent<List<Service>> services = manageService.services([filters: [label: [(namespaceFilter): true]]])
    EngineResponseContent<List<Network>> networks = manageNetwork.networks([filters: [label: [(namespaceFilter): true]]])
    EngineResponseContent<List<Secret>> secrets = manageSecret.secrets([filters: [label: [(namespaceFilter): true]]])
    EngineResponseContent<List<Config>> configs = manageConfig.configs([filters: [label: [(namespaceFilter): true]]])

    services.content.each { Service service ->
      manageService.rmService(service.ID)
    }
    networks.content.each { Network network ->
      manageNetwork.rmNetwork(network.id)
    }
    secrets.content.each { Secret secret ->
      manageSecret.rmSecret(secret.ID)
    }
    configs.content.each { Config config ->
      manageConfig.rmConfig(config.ID)
    }
  }

  @Override
  EngineResponseContent<List<Service>> stackServices(String namespace, Map filters = [:]) {
    log.info("docker stack services")

    String namespaceFilter = "${LabelNamespace}=${namespace}"
    Map actualFilters = filters ?: [:]
    if (actualFilters.label) {
      actualFilters.label[(namespaceFilter)] = true
    }
    else {
      actualFilters['label'] = [(namespaceFilter): true]
    }
    EngineResponseContent<List<Service>> services = manageService.services([filters: actualFilters])
//    def infoByServiceId = getInfoByServiceId(services)
    return services
  }

  Map<String, ServiceInfo> getInfoByServiceId(EngineResponseContent<List<Service>> services) {
    EngineResponseContent<List<Node>> nodes = manageNode.nodes()
    List<String> activeNodes = nodes.content.findResults { Node node ->
      node.status.state != NodeState.Down ? node.ID : null
    }

    Map<String, Integer> running = [:]
    Map<String, Integer> tasksNoShutdown = [:]

    Map serviceFilter = [service: [:]]
    services.content.each { Service service ->
      serviceFilter.service[(service.ID as String)] = true
    }
    EngineResponseContent<List<Task>> tasks = manageTask.tasks([filters: serviceFilter])
    tasks.content.each { Task task ->
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

    Map<String, ServiceInfo> infoByServiceId = [:]
    services.content.each { Service service ->
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
