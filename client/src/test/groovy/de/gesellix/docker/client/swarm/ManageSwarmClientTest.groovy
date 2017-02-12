package de.gesellix.docker.client.swarm

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.HttpClient
import groovy.json.JsonBuilder
import spock.lang.Specification

class ManageSwarmClientTest extends Specification {
    HttpClient httpClient = Mock(HttpClient)
    ManageSwarmClient service

    def setup() {
        service = new ManageSwarmClient(httpClient, Mock(DockerResponseHandler))
    }

    def "inspect swarm"() {
        given:
        def filters = [membership: ["accepted"], role: ["worker"]]
        def expectedFilterValue = new JsonBuilder(filters).toString()
        def query = [filters: filters]

        when:
        service.inspectSwarm(query)

        then:
        1 * httpClient.get([path : "/swarm",
                            query: [filters: expectedFilterValue]]) >> [status: [success: true]]
    }

    def "initialize a swarm"() {
        given:
        def config = [
                "ListenAddr"     : "0.0.0.0:4500",
                "ForceNewCluster": false,
                "Spec"           : [
                        "AcceptancePolicy": [
                                "Policies": [
                                        ["Role": "MANAGER", "Autoaccept": false],
                                        ["Role": "WORKER", "Autoaccept": true]
                                ]
                        ],
                        "Orchestration"   : [:],
                        "Raft"            : [:],
                        "Dispatcher"      : [:],
                        "CAConfig"        : [:]
                ]
        ]

        when:
        service.initSwarm(config)

        then:
        1 * httpClient.post([path              : "/swarm/init",
                             body              : config,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "join a swarm"() {
        given:
        def config = [
                "ListenAddr": "0.0.0.0:4500",
                "RemoteAddr": "node1:4500",
                "Secret"    : "",
                "CAHash"    : "",
                "Manager"   : false
        ]

        when:
        service.joinSwarm(config)

        then:
        1 * httpClient.post([path              : "/swarm/join",
                             body              : config,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "leave a swarm"() {
        when:
        service.leaveSwarm()

        then:
        1 * httpClient.post([path : "/swarm/leave",
                             query: [:]]) >> [status: [success: true]]
    }

    def "update a swarm"() {
        given:
        def query = [version: 42]
        def config = [
                "AcceptancePolicy": [
                        "Policies": [
                                ["Role": "MANAGER", "Autoaccept": false],
                                ["Role": "WORKER", "Autoaccept": false]
                        ]
                ]
        ]

        when:
        service.updateSwarm(query, config)

        then:
        1 * httpClient.post([path              : "/swarm/update",
                             query             : query,
                             body              : config,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "get the swarm worker token"() {
        when:
        def token = service.getSwarmWorkerToken()
        then:
        1 * httpClient.get([path : "/swarm",
                            query: [:]]) >> [status : [success: true],
                                             content: [JoinTokens: [Worker: "worker-token"]]]
        and:
        token == "worker-token"
    }

    def "rotate the swarm worker token"() {
        when:
        def token = service.rotateSwarmWorkerToken()
        then:
        1 * httpClient.get([path : "/swarm",
                            query: [:]]) >> [status : [success: true],
                                             content: [JoinTokens: [Worker: "worker-token-1"],
                                                       Spec      : [Key: "Value"],
                                                       Version   : [Index: "1"]]]
        then:
        1 * httpClient.post([path              : "/swarm/update",
                             query             : [version          : "1",
                                                  rotateWorkerToken: true],
                             body              : [Key: "Value"],
                             requestContentType: "application/json"]) >> [status: [success: true]]
        then:
        1 * httpClient.get([path : "/swarm",
                            query: [:]]) >> [status : [success: true],
                                             content: [JoinTokens: [Worker: "worker-token-2"],
                                                       Spec      : [Key: "Value"],
                                                       Version   : [Index: "2"]]]
        and:
        token == "worker-token-2"
    }

    def "get the swarm manager token"() {
        when:
        def token = service.getSwarmManagerToken()
        then:
        1 * httpClient.get([path : "/swarm",
                            query: [:]]) >> [status : [success: true],
                                             content: [JoinTokens: [Manager: "manager-token"]]]
        and:
        token == "manager-token"
    }

    def "rotate the swarm manager token"() {
        when:
        def token = service.rotateSwarmManagerToken()
        then:
        1 * httpClient.get([path : "/swarm",
                            query: [:]]) >> [status : [success: true],
                                             content: [JoinTokens: [Manager: "manager-token-1"],
                                                       Spec      : [Key: "Value"],
                                                       Version   : [Index: "1"]]]
        then:
        1 * httpClient.post([path              : "/swarm/update",
                             query             : [version           : "1",
                                                  rotateManagerToken: true],
                             body              : [Key: "Value"],
                             requestContentType: "application/json"]) >> [status: [success: true]]
        then:
        1 * httpClient.get([path : "/swarm",
                            query: [:]]) >> [status : [success: true],
                                             content: [JoinTokens: [Manager: "manager-token-2"],
                                                       Spec      : [Key: "Value"],
                                                       Version   : [Index: "2"]]]
        and:
        token == "manager-token-2"
    }

    def "get the swarm manager unlock key"() {
        when:
        def unlockKey = service.getSwarmManagerUnlockKey()
        then:
        1 * httpClient.get([path: "/swarm/unlockkey"]) >> [
                status : [success: true],
                content: [UnlockKey: "manager-unlock-key"]]
        and:
        unlockKey == "manager-unlock-key"
    }

    def "rotate the swarm manager unlock key"() {
        when:
        def unlockKey = service.rotateSwarmManagerUnlockKey()
        then:
        1 * httpClient.get([path : "/swarm",
                            query: [:]]) >> [status : [success: true],
                                             content: [Spec   : [Key: "Value"],
                                                       Version: [Index: "1"]]]
        then:
        1 * httpClient.post([path              : "/swarm/update",
                             query             : [version               : "1",
                                                  rotateManagerUnlockKey: true],
                             body              : [Key: "Value"],
                             requestContentType: "application/json"]) >> [status: [success: true]]
        then:
        1 * httpClient.get([path: "/swarm/unlockkey"]) >> [
                status : [success: true],
                content: [UnlockKey: "manager-unlock-key"]]
        and:
        unlockKey == "manager-unlock-key"
    }

    def "unlock swarm"() {
        when:
        service.unlockSwarm("SWMKEY-1-4711")
        then:
        1 * httpClient.post([path              : "/swarm/unlock",
                             body              : [UnlockKey: "SWMKEY-1-4711"],
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }
}
