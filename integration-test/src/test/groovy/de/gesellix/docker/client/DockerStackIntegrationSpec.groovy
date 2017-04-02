package de.gesellix.docker.client

import de.gesellix.docker.client.stack.DeployStackConfig
import de.gesellix.docker.client.stack.ManageStackClient
import de.gesellix.docker.client.stack.types.StackService
import groovy.util.logging.Slf4j
import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Specification

@Slf4j
@Requires({ LocalDocker.available() && LocalDocker.supportsStack() })
class DockerStackIntegrationSpec extends Specification {

    static DockerClient dockerClient

    def setupSpec() {
        dockerClient = new DockerClientImpl()
//        dockerClient.config.apiVersion = "v1.24"
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

    def "deploy a new stack"() {
        given:
        dockerClient.initSwarm(newSwarmConfig())

        def namespace = "new-stack"
        def config = new DeployStackConfig()
        config.services = ["service1": new StackService().with {
            networks = []
            taskTemplate = [
                    containerSpec: [
                            image: "redis:3-alpine",
                    ],
            ]
            it
        }]

        when:
        dockerClient.stackDeploy(namespace, config)

        then:
        dockerClient.lsStacks().find { ManageStackClient.Stack stack ->
            stack.name == namespace && stack.services == config.services.size()
        }

        cleanup:
        performSilently { dockerClient.stackRm(namespace) }
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    def "update an existing stack"() {
        given:
        dockerClient.initSwarm(newSwarmConfig())

        def namespace = "existing-stack"
        def config = new DeployStackConfig()
        config.services = ["service2": new StackService().with {
            networks = []
            mode = [replicated: [replicas: 1]]
            taskTemplate = [
                    containerSpec: [
                            image: "redis:3-alpine",
                    ],
            ]
            it
        }]
        dockerClient.stackDeploy(namespace, config)
        Thread.sleep(1000)
        def originalTasks = dockerClient.stackPs(namespace).content

        when:
        config.services["service2"].mode = [replicated: [replicas: 2]]
        dockerClient.stackDeploy(namespace, config)

        then:
        Thread.sleep(1000)
        def updatedTasks = dockerClient.stackPs(namespace).content

        originalTasks.size() == 1
        originalTasks.first().Slot == 1
//        originalTasks.first() == [Version:[Index:12], Slot:1, Status:[State:new, Message:created, ContainerStatus:[:], PortStatus:[:]], DesiredState:running]

        updatedTasks.size() == 2
//        updatedTasks == []

        cleanup:
        performSilently { dockerClient.stackRm(namespace) }
        performSilently { dockerClient.leaveSwarm([force: true]) }
    }

    @Ignore
    def "list stacks"() {
//        dockerClient.lsStacks()
    }

    @Ignore
    def "list tasks in a stack"() {
//        dockerClient.stackPs("a-stack")
    }

    @Ignore
    def "list services in a stack"() {
//        dockerClient.stackServices("a-stack")
    }

    @Ignore
    def "remove a stack"() {
//        dockerClient.stackRm("a-stack")
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

    def performSilently(Closure action) {
        try {
            action()
        } catch (Exception ignored) {
        }
    }
}
