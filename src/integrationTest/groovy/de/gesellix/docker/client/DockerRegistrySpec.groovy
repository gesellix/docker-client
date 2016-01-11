package de.gesellix.docker.client

import spock.lang.IgnoreIf
import spock.lang.Specification

@IgnoreIf({ !System.env.DOCKER_HOST })
class DockerRegistrySpec extends Specification {

    def "can determine registry url"() {
        given:
        def registry = new DockerRegistry(dockerClient: new DockerClientImpl())
        registry.run()
        when:
        def registryUrl = registry.url()
        then:
        registryUrl =~ /^(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})|localhost:\d{1,5}\u0024/
        cleanup:
        registry.rm()
    }
}
