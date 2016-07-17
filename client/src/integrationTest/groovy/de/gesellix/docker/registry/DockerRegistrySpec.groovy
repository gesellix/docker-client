package de.gesellix.docker.registry

import de.gesellix.docker.client.DockerClientImpl
import de.gesellix.docker.client.util.LocalDocker
import spock.lang.Requires
import spock.lang.Specification

@Requires({ LocalDocker.available() })
class DockerRegistrySpec extends Specification {

    def "can determine registry url"() {
        given:
        def registry = new DockerRegistry(new DockerClientImpl())
        registry.run()
        when:
        def registryUrl = registry.url()
        then:
        registryUrl =~ /^(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})|localhost:\d{1,5}\u0024/
        cleanup:
        registry.rm()
    }
}
