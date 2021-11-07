package de.gesellix.docker.client

import de.gesellix.docker.client.stack.DeployStackConfig
import de.gesellix.docker.client.stack.DeployStackOptions
import de.gesellix.docker.client.stack.Stack
import de.gesellix.docker.remote.api.LocalNodeState
import de.gesellix.docker.remote.api.ServiceSpec
import de.gesellix.docker.remote.api.ServiceSpecMode
import de.gesellix.docker.remote.api.ServiceSpecModeReplicated
import de.gesellix.docker.remote.api.SwarmInitRequest
import de.gesellix.docker.remote.api.TaskSpec
import de.gesellix.docker.remote.api.TaskSpecContainerSpec
import de.gesellix.docker.testutil.SwarmUtil
import groovy.util.logging.Slf4j
import spock.lang.Requires
import spock.lang.Specification

@Slf4j
@Requires({ LocalDocker.available() && LocalDocker.supportsStack() })
class DockerStackIntegrationSpec extends Specification {

  static DockerClient dockerClient

  static String swarmAdvertiseAddr

  static String testImage = "gesellix/echo-server:${TestConstants.CONSTANTS.imageTag}"

  def setupSpec() {
    dockerClient = new DockerClientImpl()
//        dockerClient.config.apiVersion = "v1.24"
    swarmAdvertiseAddr = new SwarmUtil().getAdvertiseAddr()
    performSilently { dockerClient.leaveSwarm(true) }
    dockerClient.pull(null, null, testImage)
    println "images: ${dockerClient.images().content}"
  }

  def cleanup() {
    Thread.sleep(1000)
  }

  def ping() {
    expect:
    "OK" == dockerClient.ping().content
  }

  def "expect inactive swarm"() {
    expect:
    dockerClient.info().content.swarm.localNodeState == LocalNodeState.Inactive
  }

  def "deploy a new stack"() {
    given:
    dockerClient.initSwarm(newSwarmConfig())

    def namespace = "new-stack"
    def config = new DeployStackConfig()
    config.services = ["service1": new ServiceSpec().with {
      networks = []
      taskTemplate = new TaskSpec(null,
                                  new TaskSpecContainerSpec(testImage, null, null, null, null, null, null,
                                                            null, null, null, null, null, null, null, null, null, null, null, null, null,
                                                            null, null, null, null, null, null, null),
                                  null, null, null, null, null, null, null, null)
      it
    }]

    when:
    dockerClient.stackDeploy(namespace, config, new DeployStackOptions(sendRegistryAuth: true))

    then:
    Thread.sleep(1000)
    def tasks = dockerClient.stackPs(namespace).content
    tasks.size() == config.services.size()

    cleanup:
    performSilently { dockerClient.stackRm(namespace) }
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "update an existing stack"() {
    given:
    dockerClient.initSwarm(newSwarmConfig())

    def namespace = "existing-stack"
    def config = new DeployStackConfig()
    config.services = ["service2": new ServiceSpec().with {
      networks = []
      mode = new ServiceSpecMode(new ServiceSpecModeReplicated(1), null, null, null)
      taskTemplate = new TaskSpec(null,
                                  new TaskSpecContainerSpec(testImage, null, null, null, null, null, null,
                                                            null, null, null, null, null, null, null, null, null, null, null, null, null,
                                                            null, null, null, null, null, null, null),
                                  null, null, null, null, null, null, null, null)
      it
    }]
    dockerClient.stackDeploy(namespace, config, new DeployStackOptions(sendRegistryAuth: true))
    Thread.sleep(1000)
    def originalTasks = dockerClient.stackPs(namespace).content

    when:
    config.services["service2"].mode = new ServiceSpecMode(new ServiceSpecModeReplicated(2), null, null, null)
    dockerClient.stackDeploy(namespace, config, new DeployStackOptions(sendRegistryAuth: true))

    then:
    Thread.sleep(1000)
    def updatedTasks = dockerClient.stackPs(namespace).content

    originalTasks.size() == 1
    originalTasks.first().slot == 1
//        originalTasks.first() == [Version:[Index:12], Slot:1, Status:[State:new, Message:created, ContainerStatus:[:], PortStatus:[:]], DesiredState:running]

    updatedTasks.size() == 2
//        updatedTasks == []

    cleanup:
    performSilently { dockerClient.stackRm(namespace) }
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "list stacks"() {
    given:
    dockerClient.initSwarm(newSwarmConfig())

    def namespace = "some-stack"
    def config = new DeployStackConfig()
    config.services = ["service1": new ServiceSpec().with {
      networks = []
      taskTemplate = new TaskSpec(null,
                                  new TaskSpecContainerSpec(testImage, null, null, null, null, null, null,
                                                            null, null, null, null, null, null, null, null, null, null, null, null, null,
                                                            null, null, null, null, null, null, null),
                                  null, null, null, null, null, null, null, null)
      it
    }]

    when:
    dockerClient.stackDeploy(namespace, config, new DeployStackOptions(sendRegistryAuth: true))

    then:
    dockerClient.lsStacks().find { Stack stack ->
      stack.name == namespace && stack.services == config.services.size()
    }

    cleanup:
    performSilently { dockerClient.stackRm(namespace) }
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "list tasks in a stack"() {
    given:
    dockerClient.initSwarm(newSwarmConfig())

    def namespace = "new-stack"
    def config = new DeployStackConfig()
    config.services = ["service1": new ServiceSpec().with {
      networks = []
      taskTemplate = new TaskSpec(null,
                                  new TaskSpecContainerSpec(testImage, null, null, null, null, null, null,
                                                            null, null, null, null, null, null, null, null, null, null, null, null, null,
                                                            null, null, null, null, null, null, null),
                                  null, null, null, null, null, null, null, null)
      it
    }]

    when:
    dockerClient.stackDeploy(namespace, config, new DeployStackOptions(sendRegistryAuth: true))

    then:
    Thread.sleep(1000)
    def tasks = dockerClient.stackPs(namespace).content
    tasks.size() == config.services.size()

    cleanup:
    performSilently { dockerClient.stackRm(namespace) }
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "list services in a stack"() {
    given:
    dockerClient.initSwarm(newSwarmConfig())

    def namespace = "new-stack"
    def config = new DeployStackConfig()
    config.services = ["service1": new ServiceSpec().with {
      networks = []
      taskTemplate = new TaskSpec(null,
                                  new TaskSpecContainerSpec(testImage, null, null, null, null, null, null,
                                                            null, null, null, null, null, null, null, null, null, null, null, null, null,
                                                            null, null, null, null, null, null, null),
                                  null, null, null, null, null, null, null, null)
      it
    }]

    when:
    dockerClient.stackDeploy(namespace, config, new DeployStackOptions(sendRegistryAuth: true))

    then:
    Thread.sleep(1000)
    def services = dockerClient.stackServices(namespace).content
    services.size() == config.services.size()

    cleanup:
    performSilently { dockerClient.stackRm(namespace) }
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "remove a stack"() {
    given:
    dockerClient.initSwarm(newSwarmConfig())

    def namespace = "new-stack"
    def config = new DeployStackConfig()
    config.services = ["service1": new ServiceSpec().with {
      networks = []
      taskTemplate = new TaskSpec(null,
                                  new TaskSpecContainerSpec(testImage, null, null, null, null, null, null,
                                                            null, null, null, null, null, null, null, null, null, null, null, null, null,
                                                            null, null, null, null, null, null, null),
                                  null, null, null, null, null, null, null, null)
      it
    }]
    dockerClient.stackDeploy(namespace, config, new DeployStackOptions(sendRegistryAuth: true))
    Thread.sleep(1000)
    def originalServices = dockerClient.stackServices(namespace).content

    when:
    dockerClient.stackRm(namespace)

    then:
    originalServices.size() == 1

    Thread.sleep(1000)
    def services = dockerClient.stackServices(namespace).content
    services.isEmpty()

    cleanup:
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def newSwarmConfig() {
    return new SwarmInitRequest(
        "0.0.0.0:4554",
        swarmAdvertiseAddr,
        null,
        null,
        null,
        false,
        null,
        null
    )
  }

  def performSilently(Closure action) {
    try {
      action()
    }
    catch (Exception ignored) {
    }
  }
}
