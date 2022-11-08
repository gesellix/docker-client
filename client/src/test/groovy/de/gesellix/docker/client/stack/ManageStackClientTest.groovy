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
import de.gesellix.docker.remote.api.IdResponse
import de.gesellix.docker.remote.api.Network
import de.gesellix.docker.remote.api.NetworkCreateRequest
import de.gesellix.docker.remote.api.Secret
import de.gesellix.docker.remote.api.Service
import de.gesellix.docker.remote.api.ServiceCreateRequest
import de.gesellix.docker.remote.api.ServiceSpec
import de.gesellix.docker.remote.api.SwarmInfo
import de.gesellix.docker.remote.api.SystemInfo
import de.gesellix.docker.remote.api.TaskSpec
import spock.lang.Specification

import static de.gesellix.docker.client.stack.ManageStackClient.LabelNamespace

class ManageStackClientTest extends Specification {

  ManageService manageService = Mock(ManageService)
  ManageTask manageTask = Mock(ManageTask)
  ManageNode manageNode = Mock(ManageNode)
  ManageNetwork manageNetwork = Mock(ManageNetwork)
  ManageSecret manageSecret = Mock(ManageSecret)
  ManageConfig manageConfig = Mock(ManageConfig)
  ManageSystem manageSystem = Mock(ManageSystem)
  ManageAuthentication manageAuthentication = Mock(ManageAuthentication)

  ManageStackClient service

  def setup() {
    service = new ManageStackClient(
        manageService,
        manageTask,
        manageNode,
        manageNetwork,
        manageSecret,
        manageConfig,
        manageSystem,
        manageAuthentication)
  }

  def "list stacks"() {
    given:
    def service1 = Mock(Service, { it.spec >> Mock(ServiceSpec, { it.labels >> [(LabelNamespace): "service1"] }) })
    def service2 = Mock(Service, { it.spec >> Mock(ServiceSpec, { it.labels >> [(LabelNamespace): "service2"] }) })
    def service3 = Mock(Service, { it.spec >> Mock(ServiceSpec, { it.labels >> [(LabelNamespace): "service1"] }) })

    when:
    Collection<Stack> stacks = service.lsStacks()

    then:
    1 * manageService.services([filters: [label: [(LabelNamespace): true]]]) >> new EngineResponseContent([
        service1,
        service2,
        service3]
    )
    and:
    stacks as List == [
        new Stack(name: "service1", services: 2),
        new Stack(name: "service2", services: 1)
    ]
  }

  def "list tasks in stack"() {
    given:
    String namespace = "the-stack"
    String namespaceFilter = "${LabelNamespace}=${namespace}"
    def expectedResponse = new EngineResponseContent([])

    when:
    def tasks = service.stackPs(namespace)

    then:
    1 * manageTask.tasks([filters: [label: [(namespaceFilter): true]]]) >> expectedResponse
    and:
    tasks == expectedResponse
  }

  def "list filtered tasks in stack"() {
    given:
    String namespace = "the-stack"
    String namespaceFilter = "${LabelNamespace}=${namespace}"
    def expectedResponse = new EngineResponseContent([])

    when:
    def tasks = service.stackPs(namespace, [label: [foo: true]])

    then:
    1 * manageTask.tasks([filters: [
        label: [
            foo              : true,
            (namespaceFilter): true]]]) >> expectedResponse
    and:
    tasks == expectedResponse
  }

  def "list services in stack"() {
    given:
    String namespace = "the-stack"
    String namespaceFilter = "${LabelNamespace}=${namespace}"
    def expectedResponse = new EngineResponseContent([])

    when:
    def services = service.stackServices(namespace)

    then:
    1 * manageService.services([filters: [label: [(namespaceFilter): true]]]) >> expectedResponse
    and:
    services == expectedResponse
  }

  def "list filtered services in stack"() {
    given:
    String namespace = "the-stack"
    String namespaceFilter = "${LabelNamespace}=${namespace}"
    def expectedResponse = new EngineResponseContent([])

    when:
    def services = service.stackServices(namespace, [label: [bar: true]])

    then:
    1 * manageService.services([filters: [
        label: [
            bar              : true,
            (namespaceFilter): true]]]) >> expectedResponse
    and:
    services == expectedResponse
  }

  def "remove a stack"() {
    given:
    String namespace = "the-stack"
    String namespaceFilter = "${LabelNamespace}=${namespace}"
    def service1 = Mock(Service)
    service1.ID >> "service1-id"
    def network1 = Mock(Network)
    network1.id >> "network1-id"
    def secret1 = Mock(Secret)
    secret1.ID >> "secret1-id"
    def config1 = Mock(Config)
    config1.ID >> "config1-id"

    when:
    service.stackRm(namespace)

    then:
    1 * manageService.services([filters: [label: [(namespaceFilter): true]]]) >> new EngineResponseContent<List<Service>>([service1])
    then:
    1 * manageNetwork.networks([filters: [label: [(namespaceFilter): true]]]) >> new EngineResponseContent<List<Network>>([network1])
    then:
    1 * manageSecret.secrets([filters: [label: [(namespaceFilter): true]]]) >> new EngineResponseContent<List<Secret>>([secret1])
    then:
    1 * manageConfig.configs([filters: [label: [(namespaceFilter): true]]]) >> new EngineResponseContent<List<Config>>([config1])

    then:
    1 * manageService.rmService("service1-id")
    then:
    1 * manageNetwork.rmNetwork("network1-id")
    then:
    1 * manageSecret.rmSecret("secret1-id")
    then:
    1 * manageConfig.rmConfig("config1-id")
  }

  def "deploy an empty stack"() {
    given:
    String namespace = "the-stack"
    String namespaceFilter = "${LabelNamespace}=${namespace}"
    def swarmInfo = Mock(SwarmInfo)
    swarmInfo.controlAvailable >> true
    def systemInfo = Mock(SystemInfo)
    systemInfo.swarm >> swarmInfo

    when:
    service.stackDeploy(namespace, new DeployStackConfig(), new DeployStackOptions())

    then:
    1 * manageSystem.info() >> new EngineResponseContent<SystemInfo>(systemInfo)
    1 * manageNetwork.networks([
        filters: [label: [(namespaceFilter): true]]]) >> new EngineResponseContent([])
    1 * manageService.services([
        filters: ['label': [(namespaceFilter): true]]]) >> new EngineResponseContent([])
  }

  def "deploy a stack"() {
    given:
    String namespace = "the-stack"
    String namespaceFilter = "${LabelNamespace}=${namespace}"
    DeployStackConfig config = new DeployStackConfig()
    def serviceSpec = new ServiceSpec().tap {
      taskTemplate = new TaskSpec()
    }
    config.services["service1"] = serviceSpec
    config.networks["network1"] = new NetworkCreateRequest(
        "network1",
        null, null, false, false,
        null, null, null,
        null, [foo: 'bar']
    )
    config.secrets["secret1"] = new StackSecret(name: "secret-name-1", data: 'secret'.bytes)
    config.configs["config1"] = new StackConfig(name: "config-name-1", data: 'config'.bytes)
    def serviceCreateRequest = new ServiceCreateRequest().tap {
      name = "${namespace}_service1"
      labels = [(LabelNamespace): namespace]
      taskTemplate = serviceSpec.taskTemplate
    }
    def swarmInfo = Mock(SwarmInfo)
    swarmInfo.controlAvailable >> true
    def systemInfo = Mock(SystemInfo)
    systemInfo.swarm >> swarmInfo

    when:
    service.stackDeploy(namespace, config, new DeployStackOptions())

    then:
    1 * manageSystem.info() >> new EngineResponseContent<SystemInfo>(systemInfo)

    and:
    1 * manageNetwork.networks([
        filters: [label: [(namespaceFilter): true]]]) >> new EngineResponseContent([])
    1 * manageNetwork.createNetwork({ NetworkCreateRequest r ->
      r.name == "the-stack_network1"
          && r.getIPAM() == null
          && r.driver == null
          && r.labels == [
          'foo'           : 'bar',
          (LabelNamespace): namespace]
          && r.internal == false
          && r.attachable == false
    })

    and:
    1 * manageSecret.secrets([filters: [name: ["secret-name-1"]]]) >> new EngineResponseContent([])
    1 * manageSecret.createSecret("secret-name-1", 'secret'.bytes, [(LabelNamespace): namespace]) >> new EngineResponseContent(
        new IdResponse("secret-id")
    )

    and:
    1 * manageConfig.configs([filters: [name: ["config-name-1"]]]) >> new EngineResponseContent([])
    1 * manageConfig.createConfig("config-name-1", 'config'.bytes, [(LabelNamespace): namespace]) >> new EngineResponseContent(
        new IdResponse("config-id")
    )

    and:
    1 * manageService.services([
        filters: ['label': [(namespaceFilter): true]]]) >> new EngineResponseContent([])
    1 * manageService.createService(serviceCreateRequest, null)
  }
}
