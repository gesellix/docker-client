package de.gesellix.docker.client

import groovy.util.logging.Slf4j
import spock.lang.Requires
import spock.lang.Specification

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
    }

    def "leave swarm"() {
    }

    def "update swarm"() {
    }

    def "services"() {
    }

    def "create service"() {
    }

    def "rm service"() {
    }

    def "inspect service"() {
    }

    def "update service"() {
    }

    def "tasks"() {
    }

    def "inspect task"() {
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
}
