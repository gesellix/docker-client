package de.gesellix.docker.client

import de.gesellix.docker.client.util.DockerRegistry
import de.gesellix.docker.client.util.LocalDocker
import groovy.util.logging.Slf4j
import spock.lang.Requires
import spock.lang.Specification

@Slf4j
@Requires({ LocalDocker.available() })
class DockerVolumeIntegrationSpec extends Specification {

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

    def "list volumes"() {
        when:
        def volumes = dockerClient.volumes().content

        then:
        volumes.Volumes instanceof List
    }
}
