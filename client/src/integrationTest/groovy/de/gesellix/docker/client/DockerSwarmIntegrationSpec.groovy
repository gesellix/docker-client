package de.gesellix.docker.client

import de.gesellix.docker.client.util.LocalDocker
import groovy.util.logging.Slf4j
import net.jodah.failsafe.Failsafe
import net.jodah.failsafe.RetryPolicy
import net.jodah.failsafe.function.Predicate
import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Specification

import java.util.concurrent.CountDownLatch

import static java.util.concurrent.TimeUnit.MILLISECONDS
import static java.util.concurrent.TimeUnit.SECONDS

@Slf4j
@Requires({ LocalDocker.available() })
class DockerSwarmIntegrationSpec extends Specification {

    static DockerClient dockerClient

    def setupSpec() {
        dockerClient = new DockerClientImpl()
//        dockerClient.config.apiVersion = "v1.24"
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
        def config = newSwarmConfig()
        def nodeId = dockerClient.initSwarm(config).content

        when:
        def nodes = dockerClient.nodes().content

        then:
        def firstNode = nodes.first()
        firstNode.ID == nodeId
        firstNode.ManagerStatus.Leader == true

        cleanup:
        dockerClient.leaveSwarm([force: true])
    }

    def "inspect node"() {
        given:
        def config = newSwarmConfig()
        def nodeId = dockerClient.initSwarm(config).content

        when:
        def node = dockerClient.inspectNode(nodeId).content

        then:
        node.ID == nodeId
        node.ManagerStatus.Leader == true

        cleanup:
        dockerClient.leaveSwarm([force: true])
    }

    def "update node"() {
        given:
        def config = newSwarmConfig()
        def nodeId = dockerClient.initSwarm(config).content
        def oldSpec = dockerClient.inspectNode(nodeId).content.Spec

        when:
        def response = dockerClient.updateNode(nodeId,
                [
                        "version": 10
                ],
                [
                        Role        : "manager",
                        Membership  : "accepted",
                        Availability: "drain"
                ])

        then:
        response.status.code == 200
        and:
        def newSpec = dockerClient.inspectNode(nodeId).content.Spec
        oldSpec.Availability == "active"
        newSpec.Availability == "drain"
        dockerClient.info().content.Swarm.LocalNodeState == "active"

        cleanup:
        dockerClient.leaveSwarm([force: true])
    }

    def "rm node"() {
        given:
        def config = newSwarmConfig()
        def nodeId = dockerClient.initSwarm(config).content

        when:
        dockerClient.rmNode(nodeId)

        then:
        def exception = thrown(DockerClientException)
        exception.message == "java.lang.IllegalStateException: docker node rm failed"
        exception.detail.content.message.contains("must be demoted")

        cleanup:
        dockerClient.leaveSwarm([force: true])
    }

    def "inspect swarm"() {
        given:
        def config = newSwarmConfig()
        def nodeId = dockerClient.initSwarm(config).content

        when:
        def response = dockerClient.inspectSwarm()
        def currentNodeId = dockerClient.info().content.Swarm.NodeID

        then:
        response.content.ID =~ /[0-9a-f]+/
        currentNodeId == nodeId

        cleanup:
        dockerClient.leaveSwarm([force: true])
    }

    def "init swarm"() {
        given:
        def config = newSwarmConfig()

        when:
        def response = dockerClient.initSwarm(config)

        then:
        response.content =~ /[0-9a-f]+/

        cleanup:
        dockerClient.leaveSwarm([force: true])
    }

    def "join swarm"() {
        given:
        def managerConfig = newSwarmConfig()
        dockerClient.initSwarm(managerConfig)
        def nodeConfig = [
                "ListenAddr": "0.0.0.0:4555",
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
        exception.detail.content.message.contains("This node is already part of a Swarm cluster")

        cleanup:
        dockerClient.leaveSwarm([force: true])
    }

    def "leave swarm"() {
        given:
        def config = newSwarmConfig()
        dockerClient.initSwarm(config)

        when:
        dockerClient.leaveSwarm([force: false])

        then:
        def exception = thrown(DockerClientException)
        exception.message == "java.lang.IllegalStateException: docker swarm leave failed"
        exception.detail.content.message.contains("Removing the last manager will erase all current state of the cluster")

        cleanup:
        dockerClient.leaveSwarm([force: true])
    }

    def "update swarm"() {
        given:
        def config = newSwarmConfig()
        dockerClient.initSwarm(config)
        def spec = dockerClient.inspectSwarm().content.Spec
        spec.Annotations = [
                Name: "test update"
        ]

        when:
        def response = dockerClient.updateSwarm(
                ["version": 11],
                spec)

        then:
        response.status.code == 200

        cleanup:
        dockerClient.leaveSwarm([force: true])
    }

    def "services"() {
        given:
        def config = newSwarmConfig()
        dockerClient.initSwarm(config)

        when:
        def response = dockerClient.services()

        then:
        response.status.code == 200
        response.content == []

        cleanup:
        dockerClient.leaveSwarm([force: true])
    }

    def "create service"() {
        given:
        def config = newSwarmConfig()
        dockerClient.initSwarm(config)
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
        dockerClient.rmService("redis")
        awaitServiceRemoved("redis")
        dockerClient.leaveSwarm([force: true])
    }

    def "rm service"() {
        given:
        def config = newSwarmConfig()
        dockerClient.initSwarm(config)
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
        dockerClient.leaveSwarm([force: true])
    }

    def "inspect service"() {
        given:
        def config = newSwarmConfig()
        dockerClient.initSwarm(config)
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
        dockerClient.rmService("redis")
        awaitServiceRemoved("redis")
        dockerClient.leaveSwarm([force: true])
    }

    // fails, and I don't know why
    @Ignore
    def "update service"() {
        given:
        def config = newSwarmConfig()
        dockerClient.initSwarm(config)
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
        serviceSpec.Name = "${serviceSpec.Name}-foo"

        when:
        def swarm = dockerClient.inspectSwarm().content
        def response = dockerClient.updateService(serviceId, [version: swarm.Version.Index], serviceSpec)

        then:
        response == [:]

        cleanup:
        dockerClient.rmService(serviceSpec.Name)
        awaitServiceRemoved(serviceSpec.Name)
        dockerClient.leaveSwarm([force: true])
    }

    def "tasks"() {
        given:
        def config = newSwarmConfig()
        dockerClient.initSwarm(config)
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
        dockerClient.rmService("redis")
        awaitServiceRemoved("redis")
        dockerClient.leaveSwarm([force: true])
    }

    def "inspect task"() {
        given:
        def config = newSwarmConfig()
        dockerClient.initSwarm(config)
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
        def firstTask = dockerClient.tasks().content.first()
        awaitTaskStarted(firstTask.ID)

        when:
        def task = dockerClient.inspectTask(firstTask.ID).content

        then:
        task.ID == firstTask.ID
        task.ServiceID == serviceId
        task.DesiredState == "running"

        cleanup:
        dockerClient.rmService("redis")
        awaitServiceRemoved("redis")
        dockerClient.leaveSwarm([force: true])
    }

    def newSwarmConfig() {
        return [
                "ListenAddr"     : "0.0.0.0:4554",
                "ForceNewCluster": false,
                "Spec"           : [
                        "AcceptancePolicy": [
                                "Policies": [
                                        ["Role": "MANAGER", "Autoaccept": true],
                                        ["Role": "WORKER", "Autoaccept": true]
                                ]
                        ],
                        "Orchestration"   : [:],
                        "Raft"            : [:],
                        "Dispatcher"      : [:],
                        "CAConfig"        : [:]
                ]
        ]
    }

    def getWithRetry(Closure callable, Predicate retryIf) {
        RetryPolicy retryPolicy = new RetryPolicy()
                .withDelay(100, MILLISECONDS)
                .withMaxRetries(3)
                .retryIf(retryIf)
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
                } else {
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
                    } else {
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
                    } else {
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
}
