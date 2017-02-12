package de.gesellix.docker.client.node

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.HttpClient
import de.gesellix.docker.client.tasks.ManageTask
import groovy.json.JsonBuilder
import spock.lang.Specification

class ManageNodeClientTest extends Specification {

    HttpClient httpClient = Mock(HttpClient)
    ManageTask manageTask = Mock(ManageTask)
    NodeUtil nodeUtil = Mock(NodeUtil)

    ManageNodeClient service

    def setup() {
        service = new ManageNodeClient(httpClient, Mock(DockerResponseHandler), manageTask, nodeUtil)
    }

    def "list nodes with query"() {
        given:
        def filters = [membership: ["accepted"], role: ["worker"]]
        def expectedFilterValue = new JsonBuilder(filters).toString()
        def query = [filters: filters]

        when:
        service.nodes(query)

        then:
        1 * httpClient.get([path : "/nodes",
                            query: [filters: expectedFilterValue]]) >> [status: [success: true]]
    }

    def "inspect node"() {
        when:
        service.inspectNode("node-id")

        then:
        1 * httpClient.get([path: "/nodes/node-id"]) >> [status: [success: true]]
    }

    def "update node"() {
        given:
        def query = [version: 42]
        def config = [
                "AcceptancePolicy": [
                        "Policies": [
                                ["Role": "MANAGER", "Autoaccept": false],
                                ["Role": "WORKER", "Autoaccept": true]
                        ]
                ]
        ]

        when:
        service.updateNode("node-id", query, config)

        then:
        1 * httpClient.post([path              : "/nodes/node-id/update",
                             query             : query,
                             body              : config,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "promote nodes"() {
        given:
        def node1Config = [
                Version: [Index: 1],
                Spec   : [Role: "worker"]]
        def node1ChangedSpec = [Role: "manager"]
        def node2Config = [
                Version: [Index: 1],
                Spec   : [Role: "manager"]]

        when:
        service.promoteNodes("node-1", "node-2")

        then:
        1 * httpClient.get([path: "/nodes/node-1"]) >> [
                status : [success: true],
                content: node1Config]
        1 * httpClient.get([path: "/nodes/node-2"]) >> [
                status : [success: true],
                content: node2Config]
        1 * httpClient.post([path              : "/nodes/node-1/update",
                             query             : ["version": 1],
                             body              : node1ChangedSpec,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "demote nodes"() {
        given:
        def node1Config = [
                Version: [Index: 1],
                Spec   : [Role: "worker"]]
        def node2Config = [
                Version: [Index: 1],
                Spec   : [Role: "manager"]]
        def node2ChangedSpec = [Role: "worker"]

        when:
        service.demoteNodes("node-1", "node-2")

        then:
        1 * httpClient.get([path: "/nodes/node-1"]) >> [
                status : [success: true],
                content: node1Config]
        1 * httpClient.get([path: "/nodes/node-2"]) >> [
                status : [success: true],
                content: node2Config]
        1 * httpClient.post([path              : "/nodes/node-2/update",
                             query             : ["version": 1],
                             body              : node2ChangedSpec,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "rm node"() {
        when:
        service.rmNode("node-id")

        then:
        1 * httpClient.delete([path: "/nodes/node-id"]) >> [status: [success: true]]
    }

    def "list tasks on node 'self' with query"() {
        given:
        def originalQuery = [filters: [param: "value"]]
        def modifiedQuery = [filters: [param: "value",
                                       node : "node-id"]]

        when:
        service.tasksOnNode("self", originalQuery)

        then:
        1 * nodeUtil.resolveNodeId('self') >> "node-id"
        1 * manageTask.tasks(modifiedQuery)
    }
}
