package de.gesellix.docker.client

import spock.lang.Specification

class DockerRegistrySpec extends Specification {

    def "can determine registry url"() {
        given:
        def registry = new DockerRegistry(dockerClient: new DockerClientImpl(
                config: new DockerConfig(
                        dockerHost: "tcp://192.168.99.100:2376",
                        certPath: "/Users/${System.getProperty('user.name')}/.docker/machine/machines/default")
        ))
        registry.run()
        when:
        def registryUrl = registry.url()
        then:
        registryUrl =~ /^(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})|localhost:\d{1,5}\u0024/
        cleanup:
        registry.rm()
    }
}
