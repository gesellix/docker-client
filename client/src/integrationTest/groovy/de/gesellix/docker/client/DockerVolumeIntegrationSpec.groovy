package de.gesellix.docker.client

import de.gesellix.docker.registry.DockerRegistry
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
        registry = new DockerRegistry(dockerClient)
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

    def "create volume"() {
        given:
        def volumeConfig = [Name      : "my-volume",
                            Driver    : "local",
                            DriverOpts: [:]]

        when:
        def volume = dockerClient.createVolume(volumeConfig).content

        then:
        volume.Name == "my-volume"
        and:
        volume.Driver == "local"
        and:
        volume.Mountpoint
        and:
        volume.Scope == "local"

        cleanup:
        dockerClient.rmVolume("my-volume")
    }
}
