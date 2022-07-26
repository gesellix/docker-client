package de.gesellix.docker.client

import de.gesellix.docker.remote.api.EndpointPortConfig
import de.gesellix.docker.remote.api.EndpointSpec
import de.gesellix.docker.remote.api.LocalNodeState
import de.gesellix.docker.remote.api.NodeSpec
import de.gesellix.docker.remote.api.Service
import de.gesellix.docker.remote.api.ServiceCreateRequest
import de.gesellix.docker.remote.api.ServiceSpecMode
import de.gesellix.docker.remote.api.ServiceSpecModeReplicated
import de.gesellix.docker.remote.api.ServiceSpecUpdateConfig
import de.gesellix.docker.remote.api.ServiceUpdateRequest
import de.gesellix.docker.remote.api.SwarmInitRequest
import de.gesellix.docker.remote.api.SwarmJoinRequest
import de.gesellix.docker.remote.api.SwarmSpec
import de.gesellix.docker.remote.api.Task
import de.gesellix.docker.remote.api.TaskSpec
import de.gesellix.docker.remote.api.TaskSpecContainerSpec
import de.gesellix.docker.remote.api.TaskState
import de.gesellix.docker.remote.api.core.ClientException
import de.gesellix.docker.remote.api.core.ServerException
import de.gesellix.docker.testutil.SwarmUtil
import groovy.util.logging.Slf4j
import net.jodah.failsafe.Failsafe
import net.jodah.failsafe.RetryPolicy
import net.jodah.failsafe.function.CheckedSupplier
import spock.lang.Requires
import spock.lang.Specification

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch
import java.util.function.Predicate

import static java.util.concurrent.TimeUnit.SECONDS

@Slf4j
@Requires({ LocalDocker.available() })
class DockerSwarmIntegrationSpec extends Specification {

  static DockerClient dockerClient

  static String swarmAdvertiseAddr

  def setupSpec() {
    dockerClient = new DockerClientImpl()
//        dockerClient.config.apiVersion = "v1.24"
    swarmAdvertiseAddr = new SwarmUtil().getAdvertiseAddr()
    performSilently { dockerClient.leaveSwarm(true) }
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

  def "list nodes"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    def nodeId = dockerClient.initSwarm(new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null)).content

    when:
    def nodes = dockerClient.nodes().content

    then:
    def firstNode = nodes.first()
    firstNode.ID == nodeId
    firstNode.managerStatus.leader == true

    cleanup:
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "inspect node"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    def nodeId = dockerClient.initSwarm(new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null)).content

    when:
    def node = dockerClient.inspectNode(nodeId).content

    then:
    node.ID == nodeId
    node.managerStatus.leader == true

    cleanup:
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "update node"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    def nodeId = dockerClient.initSwarm(new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null)).content
    def nodeInfo = dockerClient.inspectNode(nodeId).content
    def oldSpec = nodeInfo.spec

    when:
    dockerClient.updateNode(
        nodeId,
        nodeInfo.version.index,
        new NodeSpec(oldSpec.name, oldSpec.labels, NodeSpec.Role.Manager, NodeSpec.Availability.Drain))

    then:
    def swarmInfo = dockerClient.inspectNode(nodeId).content
    def newSpec = swarmInfo.spec
    oldSpec.availability == NodeSpec.Availability.Active
    newSpec.availability == NodeSpec.Availability.Drain
    dockerClient.info().content.swarm.localNodeState == LocalNodeState.Active

    cleanup:
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "rm node"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    def nodeId = dockerClient.initSwarm(new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null)).content

    when:
    dockerClient.rmNode(nodeId)

    then:
    def exception = thrown(ClientException)
    exception.statusCode == 400
    exception.toString() =~ ".*node .+ is a cluster manager and is a member of the raft cluster. It must be demoted to worker before removal.*"

    cleanup:
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "inspect swarm"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    def nodeId = dockerClient.initSwarm(new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null)).content

    when:
    def response = dockerClient.inspectSwarm()
    def self = dockerClient.info().content.swarm.nodeID

    then:
    response.content.ID =~ /[0-9a-f]+/
    self == nodeId

    cleanup:
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "init swarm"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    def request = new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null)

    when:
    def nodeId = dockerClient.initSwarm(request).content

    then:
    nodeId =~ /[0-9a-f]+/

    cleanup:
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "join swarm"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    def managerRequest = new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null)
    dockerClient.initSwarm(managerRequest)
    def joinRequest = new SwarmJoinRequest(
        "0.0.0.0:4711",
        null,
        null,
        [managerRequest.listenAddr],
        null)

    when:
    dockerClient.joinSwarm(joinRequest)

    then:
    def exception = thrown(ServerException)
    exception.message == "Server error : 503 Service Unavailable"
    exception.toString().contains("This node is already part of a swarm")

    cleanup:
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "leave swarm"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    dockerClient.initSwarm(new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null)).content

    when:
    dockerClient.leaveSwarm(false)

    then:
    def exception = thrown(ServerException)
    exception.message == "Server error : 503 Service Unavailable"
    exception.toString().contains("Removing the last manager erases all current state of the swarm")

    cleanup:
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "update swarm"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    dockerClient.initSwarm(new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null)).content
    def swarmInfo = dockerClient.inspectSwarm().content
    SwarmSpec spec = new SwarmSpec(
        swarmInfo.spec.name,
        swarmInfo.spec.labels.tap { put("another", "label") },
        swarmInfo.spec.orchestration,
        swarmInfo.spec.raft,
        swarmInfo.spec.dispatcher,
        swarmInfo.spec.caConfig,
        swarmInfo.spec.encryptionConfig,
        swarmInfo.spec.taskDefaults)

    when:
    dockerClient.updateSwarm(
        swarmInfo.version.index,
        spec)

    then:
    notThrown(Exception)

    cleanup:
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "rotate swarm worker token"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    dockerClient.initSwarm(new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null)).content
    def previousToken = dockerClient.getSwarmWorkerToken()

    when:
    def newToken = dockerClient.rotateSwarmWorkerToken()

    then:
    previousToken != newToken
    and:
    previousToken.startsWith("SWMTKN")
    newToken.startsWith("SWMTKN")

    cleanup:
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "rotate swarm manager token"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    dockerClient.initSwarm(new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null)).content
    def previousToken = dockerClient.getSwarmManagerToken()

    when:
    def newToken = dockerClient.rotateSwarmManagerToken()

    then:
    previousToken != newToken
    and:
    previousToken.startsWith("SWMTKN")
    newToken.startsWith("SWMTKN")

    cleanup:
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "services"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    dockerClient.initSwarm(new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null)).content

    when:
    def response = dockerClient.services()

    then:
    response.content == []

    cleanup:
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "create service"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    dockerClient.initSwarm(new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null)).content
    def serviceConfig = new ServiceCreateRequest().tap { s ->
      s.name = "echo-server"
      s.taskTemplate = new TaskSpec().tap { t ->
        t.containerSpec = new TaskSpecContainerSpec().tap { c ->
          c.image = TestConstants.CONSTANTS.imageName
        }
      }
      s.mode = new ServiceSpecMode().tap { m ->
        m.replicated = new ServiceSpecModeReplicated(1)
      }
      s.updateConfig = new ServiceSpecUpdateConfig().tap { u ->
        u.parallelism = 1
      }
      s.endpointSpec = new EndpointSpec().tap { e ->
        e.ports = [new EndpointPortConfig().tap { p ->
          p.protocol = EndpointPortConfig.Protocol.Tcp
          p.publishedPort = 8080
        }]
      }
    }

    when:
    def response = dockerClient.createService(serviceConfig)

    then:
    response.content.ID =~ /[0-9a-f]+/
    def echoService = awaitServiceStarted("echo-server")
    echoService?.spec?.name == "echo-server"

    cleanup:
    performSilently { dockerClient.rmService("echo-server") }
    performSilently { awaitServiceRemoved("echo-server") }
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "rm service"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    dockerClient.initSwarm(new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null)).content
    def serviceConfig = new ServiceCreateRequest().tap { s ->
      s.name = "echo-server"
      s.taskTemplate = new TaskSpec().tap { t ->
        t.containerSpec = new TaskSpecContainerSpec().tap { c ->
          c.image = TestConstants.CONSTANTS.imageName
        }
      }
      s.mode = new ServiceSpecMode().tap { m ->
        m.replicated = new ServiceSpecModeReplicated(1)
      }
      s.updateConfig = new ServiceSpecUpdateConfig().tap { u ->
        u.parallelism = 1
      }
    }

    String serviceId = dockerClient.createService(serviceConfig).content.ID

    when:
    dockerClient.rmService(serviceId)

    then:
    notThrown(Exception)

    cleanup:
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "inspect service"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    dockerClient.initSwarm(new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null)).content
    def serviceConfig = new ServiceCreateRequest().tap { s ->
      s.name = "echo-server"
      s.taskTemplate = new TaskSpec().tap { t ->
        t.containerSpec = new TaskSpecContainerSpec().tap { c ->
          c.image = TestConstants.CONSTANTS.imageName
        }
      }
      s.mode = new ServiceSpecMode().tap { m ->
        m.replicated = new ServiceSpecModeReplicated(1)
      }
      s.updateConfig = new ServiceSpecUpdateConfig().tap { u ->
        u.parallelism = 1
      }
    }

    String serviceId = dockerClient.createService(serviceConfig).content.ID

    when:
    def response = dockerClient.inspectService(serviceId)

    then:
    response.content.ID == serviceId

    cleanup:
    performSilently { dockerClient.rmService("echo-server") }
    performSilently { awaitServiceRemoved("echo-server") }
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "update service"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    dockerClient.initSwarm(new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null)).content
    def serviceConfig = new ServiceCreateRequest(
        "update-service",
        [BeforeUpdate: "test-label"],
        new TaskSpec().tap { t ->
          t.containerSpec = new TaskSpecContainerSpec().tap { c ->
            c.image = TestConstants.CONSTANTS.imageName
          }
        },
        new ServiceSpecMode().tap { m ->
          m.replicated = new ServiceSpecModeReplicated(1)
        },
        new ServiceSpecUpdateConfig().tap { u ->
          u.parallelism = 1
        },
        null,
        null,
        null
    )
    String serviceId = dockerClient.createService(serviceConfig).content.ID
    def inspectService = dockerClient.inspectService(serviceId)
    def serviceVersion = inspectService.content.version.index
    def serviceSpec = new ServiceUpdateRequest().tap { s ->
      s.name = inspectService.content.spec.name
      s.labels = inspectService.content.spec.labels + [TestLabel: "update-service-foo"]
      s.taskTemplate = new TaskSpec().tap { t ->
        t.containerSpec = new TaskSpecContainerSpec().tap { c ->
          c.image = inspectService.content.spec.taskTemplate.containerSpec.image
        }
      }
    }

    when:
    def response = dockerClient.updateService("update-service", serviceVersion, serviceSpec)

    then:
    notThrown(Exception)
    (response.content.warnings ?: []).empty

    cleanup:
    performSilently { dockerClient.rmService(serviceSpec.name) }
    performSilently { awaitServiceRemoved(serviceSpec.name) }
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "tasks"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    dockerClient.initSwarm(new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null)).content
    def serviceConfig = new ServiceCreateRequest().tap { s ->
      s.name = "echo-server"
      s.taskTemplate = new TaskSpec().tap { t ->
        t.containerSpec = new TaskSpecContainerSpec().tap { c ->
          c.image = TestConstants.CONSTANTS.imageName
        }
      }
      s.mode = new ServiceSpecMode().tap { m ->
        m.replicated = new ServiceSpecModeReplicated(1)
      }
      s.updateConfig = new ServiceSpecUpdateConfig().tap { u ->
        u.parallelism = 1
      }
    }

    def serviceId = dockerClient.createService(serviceConfig).content.ID

    when:
    def tasks = getWithRetry(
        { dockerClient.tasks().content },
        { List content -> !content })

    then:
    Task firstTask = tasks.first()
    firstTask.serviceID == serviceId
    firstTask.ID =~ /[0-9a-f]+/

    cleanup:
    performSilently { dockerClient.rmService("echo-server") }
    performSilently { awaitServiceRemoved("echo-server") }
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def "inspect task"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    dockerClient.initSwarm(new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null)).content
    def serviceConfig = new ServiceCreateRequest().tap { s ->
      s.name = "echo-server"
      s.taskTemplate = new TaskSpec().tap { t ->
        t.containerSpec = new TaskSpecContainerSpec().tap { c ->
          c.image = TestConstants.CONSTANTS.imageName
        }
      }
      s.mode = new ServiceSpecMode().tap { m ->
        m.replicated = new ServiceSpecModeReplicated(1)
      }
      s.updateConfig = new ServiceSpecUpdateConfig().tap { u ->
        u.parallelism = 1
      }
    }

    def serviceId = dockerClient.createService(serviceConfig).content.ID
    Thread.sleep(1000)
    def firstTask = dockerClient.tasks().content.first()
    awaitTaskStarted(firstTask.ID)

    when:
    def task = dockerClient.inspectTask(firstTask.ID).content

    then:
    task.ID == firstTask.ID
    task.serviceID == serviceId
    task.desiredState == TaskState.Running

    cleanup:
    performSilently { dockerClient.rmService("echo-server") }
    performSilently { awaitServiceRemoved("echo-server") }
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def <R> R getWithRetry(CheckedSupplier<R> callable, Predicate retryIf) {
    RetryPolicy retryPolicy = new RetryPolicy<>()
        .withDelay(Duration.of(100, ChronoUnit.MILLIS))
        .withMaxRetries(3)
        .handleIf(retryIf)
    return Failsafe.with(retryPolicy).get(callable)
  }

  Service awaitServiceStarted(name) {
    Service theService
    CountDownLatch latch = new CountDownLatch(1)
    Thread.start {
      while (theService == null) {
        theService = findService(name)
        if (theService) {
          latch.countDown()
        }
        else {
          Thread.sleep(1000)
        }
      }
    }
    latch.await(30, SECONDS)
    return theService
  }

  def awaitTaskStarted(taskId) {
    def task = dockerClient.inspectTask(taskId).content
    if (task?.status?.state != TaskState.Running) {
      CountDownLatch latch = new CountDownLatch(1)
      Thread.start {
        while (task?.status?.state != TaskState.Running) {
          task = dockerClient.inspectTask(taskId).content
          if (task?.status?.state == TaskState.Running) {
            latch.countDown()
          }
          else {
            Thread.sleep(1000)
          }
        }
      }
      latch.await(30, SECONDS)
    }
    return task
  }

  def awaitServiceRemoved(name) {
    def theService = findService(name)
    if (theService != null) {
      CountDownLatch latch = new CountDownLatch(1)
      Thread.start {
        while (theService != null) {
          theService = findService(name)
          if (theService == null) {
            latch.countDown()
          }
          else {
            Thread.sleep(1000)
          }
        }
      }
      latch.await(30, SECONDS)
    }
  }

  Service findService(name) {
    def services = dockerClient.services().content
    return services.find { it.spec.name == name }
  }

  def performSilently(Closure action) {
    try {
      action()
    }
    catch (Exception ignored) {
    }
  }
}
