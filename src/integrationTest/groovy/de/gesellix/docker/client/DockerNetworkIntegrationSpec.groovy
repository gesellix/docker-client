package de.gesellix.docker.client

import de.gesellix.docker.client.util.DockerRegistry
import de.gesellix.docker.client.util.LocalDocker
import groovy.util.logging.Slf4j
import spock.lang.Requires
import spock.lang.Specification

@Slf4j
@Requires({ LocalDocker.available() })
class DockerNetworkIntegrationSpec extends Specification {

    static DockerRegistry registry

    static DockerClient dockerClient

    def setupSpec() {
        dockerClient = new DockerClientImpl()
        registry = new DockerRegistry(dockerClient: dockerClient)
        registry.run()
    }

    def cleanupSpec() {
        registry.rm()
    }

    def ping() {
        when:
        def ping = dockerClient.ping()

        then:
        ping.status.code == 200
        ping.content == "OK"
    }

    def "list networks"() {
        when:
        def networks = dockerClient.networks().content

        then:
        networks.find { it.Name == "bridge" }
    }

    def "create default network"() {
        given:
        !dockerClient.networks().content.find { it.Name == "test-net" }

        when:
        dockerClient.createNetwork('test-net')

        then:
        dockerClient.networks().content.find { it.Name == "test-net" }

        cleanup:
        dockerClient.rmNetwork('test-net')
    }

    @Requires({ LocalDocker.hasSwarmMode() })
    def "create overlay network"() {
        given:
        !dockerClient.networks().content.find { it.Name == "test-net" }
        dockerClient.initSwarm([
                "ListenAddr"     : "0.0.0.0:4500",
                "ForceNewCluster": false
        ])

        when:
        dockerClient.createNetwork('test-net', [
                Driver: "overlay",
                "IPAM": [
                        "Driver": "default"
                ]
        ])

        then:
        dockerClient.networks().content.find { it.Name == "test-net" }

        cleanup:
        dockerClient.rmNetwork('test-net')
        dockerClient.leaveSwarm([force: true])
    }
}
