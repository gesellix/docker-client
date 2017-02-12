package de.gesellix.docker.client.network

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.HttpClient
import groovy.json.JsonBuilder
import spock.lang.Specification

class ManageNetworkClientTest extends Specification {
    HttpClient httpClient = Mock(HttpClient)
    ManageNetworkClient service

    def setup() {
        service = new ManageNetworkClient(httpClient, Mock(DockerResponseHandler))
    }

    def "networks with query"() {
        given:
        def filters = [name: ["a-net"], id: ["a-net-id"]]
        def expectedFilterValue = new JsonBuilder(filters).toString()
        def query = [filters: filters]

        when:
        service.networks(query)

        then:
        1 * httpClient.get([path : "/networks",
                            query: [filters: expectedFilterValue]]) >> [status: [success: true]]
    }

    def "inspect network"() {
        when:
        service.inspectNetwork("a-network")

        then:
        1 * httpClient.get([path: "/networks/a-network"]) >> [status: [success: true]]
    }

    def "create network with defaults"() {
        given:
        def expectedNetworkConfig = [Name          : "network-name",
                                     CheckDuplicate: true]

        when:
        service.createNetwork("network-name")

        then:
        1 * httpClient.post([path              : "/networks/create",
                             body              : expectedNetworkConfig,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "create network with config"() {
        given:
        def networkConfig = [Driver        : "bridge",
                             Options       : [:],
                             CheckDuplicate: false]
        def expectedNetworkConfig = [Name          : "network-name",
                                     Driver        : "bridge",
                                     Options       : [:],
                                     CheckDuplicate: false]

        when:
        service.createNetwork("network-name", networkConfig)

        then:
        1 * httpClient.post([path              : "/networks/create",
                             body              : expectedNetworkConfig,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "connect a container to a network"() {
        when:
        service.connectNetwork("a-network", "a-container")

        then:
        1 * httpClient.post([path              : "/networks/a-network/connect",
                             body              : [container: "a-container"],
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "disconnect a container from a network"() {
        when:
        service.disconnectNetwork("a-network", "a-container")

        then:
        1 * httpClient.post([path              : "/networks/a-network/disconnect",
                             body              : [container: "a-container"],
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "rm network"() {
        when:
        service.rmNetwork("a-network")

        then:
        1 * httpClient.delete([path: "/networks/a-network"]) >> [status: [success: true]]
    }

    def "pruneNetworks removes unused networks"() {
        when:
        service.pruneNetworks()

        then:
        1 * httpClient.post([path : "/networks/prune",
                             query: [:]]) >> [status: [success: true]]
    }
}
