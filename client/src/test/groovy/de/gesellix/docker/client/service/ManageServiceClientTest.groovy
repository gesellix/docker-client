package de.gesellix.docker.client.service

import de.gesellix.docker.client.DockerResponse
import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.HttpClient
import de.gesellix.docker.client.node.NodeUtil
import de.gesellix.docker.client.tasks.ManageTask
import groovy.json.JsonBuilder
import spock.lang.Specification

class ManageServiceClientTest extends Specification {

    ManageTask manageTask = Mock(ManageTask)
    NodeUtil nodeUtil = Mock(NodeUtil)
    HttpClient httpClient = Mock(HttpClient)

    ManageServiceClient service

    def setup() {
        service = new ManageServiceClient(httpClient, Mock(DockerResponseHandler), manageTask, nodeUtil)
    }

    def "list services with query"() {
        given:
        def filters = [name: ["node-name"]]
        def expectedFilterValue = new JsonBuilder(filters).toString()
        def query = [filters: filters]

        when:
        service.services(query)

        then:
        1 * httpClient.get([path : "/services",
                            query: [filters: expectedFilterValue]]) >> [status: [success: true]]
    }

    def "create a service"() {
        given:
        def config = [
                "Name"        : "redis",
                "Task"        : [
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
                ]
        ]

        when:
        service.createService(config)

        then:
        1 * httpClient.post([path              : "/services/create",
                             body              : config,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "rm service"() {
        when:
        service.rmService("service-name")

        then:
        1 * httpClient.delete([path: "/services/service-name"]) >> [status: [success: true]]
    }

    def "inspect service"() {
        when:
        service.inspectService("service-name")

        then:
        1 * httpClient.get([path: "/services/service-name"]) >> [status: [success: true]]
    }

    def "update service"() {
        given:
        def query = [version: 42]
        def config = [
                "Name"        : "redis",
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
                ]
        ]

        when:
        service.updateService("service-name", query, config)

        then:
        1 * httpClient.post([path              : "/services/service-name/update",
                             query             : query,
                             body              : config,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "scale service"() {
        when:
        service.scaleService("service-id", 42)
        then:
        1 * httpClient.get([path: "/services/service-id"]) >> [status : [success: true],
                                                               content: [Version: 1,
                                                                         Spec   : [Mode: [Replicated: [Replicas: 1]]]]]
        then:
        service.updateService("service-id", [version: 1], [Mode: [Replicated: [Replicas: 42]]]) >> [status: [success: true]]
    }

    def "list tasks of service with query"() {
        given:
        def filters = [service: ["service-name"]]
        def query = [filters: filters]
        def expectedResponse = new DockerResponse()

        when:
        def result = service.tasksOfService("service-name", query)

        then:
        1 * manageTask.tasks([filters: [service: ["service-name"]]]) >> expectedResponse
        result == expectedResponse
    }
}
