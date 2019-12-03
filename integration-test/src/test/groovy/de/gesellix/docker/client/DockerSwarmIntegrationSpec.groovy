package de.gesellix.docker.client

import de.gesellix.docker.testutil.NetworkInterfaces
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
        swarmAdvertiseAddr = new NetworkInterfaces().getFirstInet4Address()
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    def cleanup() {
        Thread.sleep(1000)
    }

    def ping() {
        when:
        def ping = dockerClient.ping()

        then:
        ping.status.code == 200
        ping.content == "OK"
    }

    def "expect inactive swarm"() {
        expect:
        dockerClient.info().content.Swarm.LocalNodeState == "inactive"
    }

    def "list nodes"() {
        given:
        def swarmConfig = dockerClient.newSwarmConfig()
        swarmConfig.AdvertiseAddr = swarmAdvertiseAddr
        def nodeId = dockerClient.initSwarm(swarmConfig).content

        when:
        def nodes = dockerClient.nodes().content

        then:
        def firstNode = nodes.first()
        firstNode.ID == nodeId
        firstNode.ManagerStatus.Leader == true

        cleanup:
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    def "inspect node"() {
        given:
        def swarmConfig = dockerClient.newSwarmConfig()
        swarmConfig.AdvertiseAddr = swarmAdvertiseAddr
        def nodeId = dockerClient.initSwarm(swarmConfig).content

        when:
        def node = dockerClient.inspectNode(nodeId).content

        then:
        node.ID == nodeId
        node.ManagerStatus.Leader == true

        cleanup:
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    def "update node"() {
        given:
        def swarmConfig = dockerClient.newSwarmConfig()
        swarmConfig.AdvertiseAddr = swarmAdvertiseAddr
        def nodeId = dockerClient.initSwarm(swarmConfig).content
        def nodeInfo = dockerClient.inspectNode(nodeId).content
        def oldSpec = nodeInfo.Spec

        when:
        def response = dockerClient.updateNode(
                nodeId,
                [
                        "version": nodeInfo.Version.Index
                ],
                [
                        Role        : "manager",
                        Membership  : "accepted",
                        Availability: "drain"
                ])

        then:
        response.status.code == 200
        and:
        def swarmInfo = dockerClient.inspectNode(nodeId).content
        def newSpec = swarmInfo.Spec
        oldSpec.Availability == "active"
        newSpec.Availability == "drain"
        dockerClient.info().content.Swarm.LocalNodeState == "active"

        cleanup:
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    def "rm node"() {
        given:
        def swarmConfig = dockerClient.newSwarmConfig()
        swarmConfig.AdvertiseAddr = swarmAdvertiseAddr
        def nodeId = dockerClient.initSwarm(swarmConfig).content

        when:
        dockerClient.rmNode(nodeId)

        then:
        def exception = thrown(DockerClientException)
        exception.message == "java.lang.IllegalStateException: docker node rm failed"
        exception.detail.content.message.contains("must be demoted")

        cleanup:
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    def "inspect swarm"() {
        given:
        def swarmConfig = dockerClient.newSwarmConfig()
        swarmConfig.AdvertiseAddr = swarmAdvertiseAddr
        def nodeId = dockerClient.initSwarm(swarmConfig).content

        when:
        def response = dockerClient.inspectSwarm()
        def self = dockerClient.info().content.Swarm.NodeID

        then:
        response.content.ID =~ /[0-9a-f]+/
        self == nodeId

        cleanup:
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    def "init swarm"() {
        given:
        def config = dockerClient.newSwarmConfig()
        config.AdvertiseAddr = swarmAdvertiseAddr

        when:
        def response = dockerClient.initSwarm(config)

        then:
        response.content =~ /[0-9a-f]+/

        cleanup:
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    def "join swarm"() {
        given:
        def managerConfig = dockerClient.newSwarmConfig()
        managerConfig.AdvertiseAddr = swarmAdvertiseAddr
        dockerClient.initSwarm(managerConfig)
        def nodeConfig = [
                "ListenAddr": "0.0.0.0:4711",
                "RemoteAddr": managerConfig.ListenAddr,
                "Secret"    : "",
                "CAHash"    : "",
                "Manager"   : false
        ]

        when:
        dockerClient.joinSwarm(nodeConfig)

        then:
        def exception = thrown(DockerClientException)
        exception.message == "java.lang.IllegalStateException: docker swarm join failed"
        exception.detail.content.message.contains("This node is already part of a swarm")

        cleanup:
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    def "leave swarm"() {
        given:
        def swarmConfig = dockerClient.newSwarmConfig()
        swarmConfig.AdvertiseAddr = swarmAdvertiseAddr
        dockerClient.initSwarm(swarmConfig)

        when:
        dockerClient.leaveSwarm([force: false])

        then:
        def exception = thrown(DockerClientException)
        exception.message == "java.lang.IllegalStateException: docker swarm leave failed"
        exception.detail.content.message.contains("Removing the last manager erases all current state of the swarm")

        cleanup:
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    def "update swarm"() {
        given:
        def swarmConfig = dockerClient.newSwarmConfig()
        swarmConfig.AdvertiseAddr = swarmAdvertiseAddr
        dockerClient.initSwarm(swarmConfig)
        def swarmInfo = dockerClient.inspectSwarm().content
        Map spec = swarmInfo.Spec
        spec.Annotations = [
                Name: "test update"
        ]

        when:
        def response = dockerClient.updateSwarm(
                [
                        "version": swarmInfo.Version.Index
                ],
                spec)

        then:
        response.status.code == 200

        cleanup:
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    def "rotate swarm worker token"() {
        given:
        def swarmConfig = dockerClient.newSwarmConfig()
        swarmConfig.AdvertiseAddr = swarmAdvertiseAddr
        dockerClient.initSwarm(swarmConfig)
        def previousToken = dockerClient.getSwarmWorkerToken()

        when:
        def newToken = dockerClient.rotateSwarmWorkerToken()

        then:
        previousToken != newToken
        and:
        previousToken.startsWith("SWMTKN")
        newToken.startsWith("SWMTKN")

        cleanup:
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    def "rotate swarm manager token"() {
        given:
        def swarmConfig = dockerClient.newSwarmConfig()
        swarmConfig.AdvertiseAddr = swarmAdvertiseAddr
        dockerClient.initSwarm(swarmConfig)
        def previousToken = dockerClient.getSwarmManagerToken()

        when:
        def newToken = dockerClient.rotateSwarmManagerToken()

        then:
        previousToken != newToken
        and:
        previousToken.startsWith("SWMTKN")
        newToken.startsWith("SWMTKN")

        cleanup:
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    def "services"() {
        given:
        def swarmConfig = dockerClient.newSwarmConfig()
        swarmConfig.AdvertiseAddr = swarmAdvertiseAddr
        dockerClient.initSwarm(swarmConfig)

        when:
        def response = dockerClient.services()

        then:
        response.status.code == 200
        response.content == []

        cleanup:
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    def "create service"() {
        given:
        def swarmConfig = dockerClient.newSwarmConfig()
        swarmConfig.AdvertiseAddr = swarmAdvertiseAddr
        dockerClient.initSwarm(swarmConfig)
        def serviceConfig = [
                "Name"        : "redis",
                "TaskTemplate": [
                        "ContainerSpec": [
                                "Image": "redis"
                        ],
                        "Resources"    : [
                                "Limits"      : [:],
                                "Reservations": [:]
                        ],
                        "RestartPolicy": [:],
                        "Placement"    : [:]
                ],
                "Mode"        : [
                        "Replicated": [
                                "Instances": 1
                        ]
                ],
                "UpdateConfig": [
                        "Parallelism": 1
                ],
                "EndpointSpec": [
                        "ExposedPorts": [
                                ["Protocol": "tcp", "Port": 6379]
                        ]
                ]]

        when:
        def response = dockerClient.createService(serviceConfig)

        then:
        response.content.ID =~ /[0-9a-f]+/
        def redisService = awaitServiceStarted("redis")
        redisService?.Spec?.Name == "redis"

        cleanup:
        performSilently { dockerClient.rmService("redis") }
        performSilently { awaitServiceRemoved("redis") }
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    def "rm service"() {
        given:
        def swarmConfig = dockerClient.newSwarmConfig()
        swarmConfig.AdvertiseAddr = swarmAdvertiseAddr
        dockerClient.initSwarm(swarmConfig)
        def serviceConfig = [
                "Name"        : "redis",
                "TaskTemplate": [
                        "ContainerSpec": [
                                "Image": "redis"
                        ]
                ],
                "Mode"        : [
                        "Replicated": ["Instances": 1]
                ],
                "UpdateConfig": [
                        "Parallelism": 1
                ]]
        def serviceId = dockerClient.createService(serviceConfig).content.ID

        when:
        def response = dockerClient.rmService(serviceId)

        then:
        response.status.code == 200

        cleanup:
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    def "inspect service"() {
        given:
        def swarmConfig = dockerClient.newSwarmConfig()
        swarmConfig.AdvertiseAddr = swarmAdvertiseAddr
        dockerClient.initSwarm(swarmConfig)
        def serviceConfig = [
                "Name"        : "redis",
                "TaskTemplate": [
                        "ContainerSpec": [
                                "Image": "redis"
                        ]
                ],
                "Mode"        : [
                        "Replicated": ["Instances": 1]
                ],
                "UpdateConfig": [
                        "Parallelism": 1
                ]]
        def serviceId = dockerClient.createService(serviceConfig).content.ID

        when:
        def response = dockerClient.inspectService(serviceId)

        then:
        response.content.ID == serviceId

        cleanup:
        performSilently { dockerClient.rmService("redis") }
        performSilently { awaitServiceRemoved("redis") }
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    def "update service"() {
        given:
        def swarmConfig = dockerClient.newSwarmConfig()
        swarmConfig.AdvertiseAddr = swarmAdvertiseAddr
        dockerClient.initSwarm(swarmConfig)
        def serviceConfig = [
                "TaskTemplate": [
                        "ContainerSpec": [
                                "Image": "redis"
                        ]
                ],
                "Mode"        : [
                        "Replicated": ["Instances": 1]
                ],
                "UpdateConfig": [
                        "Parallelism": 1
                ]]
        def serviceId = dockerClient.createService(serviceConfig).content.ID
        def serviceSpec = dockerClient.inspectService(serviceId).content.Spec
        def serviceVersion = dockerClient.inspectService(serviceId).content.Version.Index
        serviceSpec.Labels = [TestLabel: "${serviceSpec.Name}-foo"]

        when:
        def response = dockerClient.updateService(serviceId, [version: serviceVersion], serviceSpec)

        then:
        response.status.code == 200

        cleanup:
        performSilently { dockerClient.rmService(serviceSpec.Name) }
        performSilently { awaitServiceRemoved(serviceSpec.Name) }
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    def "tasks"() {
        given:
        def swarmConfig = dockerClient.newSwarmConfig()
        swarmConfig.AdvertiseAddr = swarmAdvertiseAddr
        dockerClient.initSwarm(swarmConfig)
        def serviceConfig = [
                "Name"        : "redis",
                "TaskTemplate": [
                        "ContainerSpec": [
                                "Image": "redis"
                        ]
                ],
                "Mode"        : [
                        "Replicated": ["Instances": 1]
                ],
                "UpdateConfig": [
                        "Parallelism": 1
                ]]
        def serviceId = dockerClient.createService(serviceConfig).content.ID

        when:
        def tasks = getWithRetry(
                { dockerClient.tasks().content },
                { List content -> !content })

        then:
        def firstTask = tasks.first()
        firstTask.ServiceID == serviceId
        firstTask.ID =~ /[0-9a-f]+/

        cleanup:
        performSilently { dockerClient.rmService("redis") }
        performSilently { awaitServiceRemoved("redis") }
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    def "inspect task"() {
        given:
        def swarmConfig = dockerClient.newSwarmConfig()
        swarmConfig.AdvertiseAddr = swarmAdvertiseAddr
        dockerClient.initSwarm(swarmConfig)
        def serviceConfig = [
                "Name"        : "redis",
                "TaskTemplate": [
                        "ContainerSpec": [
                                "Image": "redis"
                        ]
                ],
                "Mode"        : [
                        "Replicated": ["Instances": 1]
                ],
                "UpdateConfig": [
                        "Parallelism": 1
                ]]
        def serviceId = dockerClient.createService(serviceConfig).content.ID
        Thread.sleep(1000)
        def firstTask = dockerClient.tasks().content.first()
        awaitTaskStarted(firstTask.ID)

        when:
        def task = dockerClient.inspectTask(firstTask.ID).content

        then:
        task.ID == firstTask.ID
        task.ServiceID == serviceId
        task.DesiredState == "running"

        cleanup:
        performSilently { dockerClient.rmService("redis") }
        performSilently { awaitServiceRemoved("redis") }
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    def getWithRetry(CheckedSupplier callable, Predicate retryIf) {
        RetryPolicy retryPolicy = new RetryPolicy<>()
                .withDelay(Duration.of(100, ChronoUnit.MILLIS))
                .withMaxRetries(3)
                .handleIf(retryIf)
        return Failsafe.with(retryPolicy).get(callable)
    }

    def awaitServiceStarted(name) {
        def redisService
        CountDownLatch latch = new CountDownLatch(1)
        Thread.start {
            while (redisService == null) {
                redisService = findService(name)
                if (redisService) {
                    latch.countDown()
                }
                else {
                    Thread.sleep(1000)
                }
            }
        }
        latch.await(30, SECONDS)
        return redisService
    }

    def awaitTaskStarted(taskId) {
        def task = dockerClient.inspectTask(taskId).content
        if (task?.Status?.State != "running") {
            CountDownLatch latch = new CountDownLatch(1)
            Thread.start {
                while (task?.Status?.State != "running") {
                    task = dockerClient.inspectTask(taskId).content
                    if (task?.Status?.State == "running") {
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
        def redisService = findService(name)
        if (redisService != null) {
            CountDownLatch latch = new CountDownLatch(1)
            Thread.start {
                while (redisService != null) {
                    redisService = findService(name)
                    if (redisService == null) {
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

    def findService(name) {
        def services = dockerClient.services().content
        return services.find { it.Spec.Name == name }
    }

    def performSilently(Closure action) {
        try {
            action()
        }
        catch (Exception ignored) {
        }
    }
}
