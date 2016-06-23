package de.gesellix.docker.client.util

import spock.lang.Specification
import spock.lang.Unroll

class LocalDockerSpec extends Specification {

    @Unroll
    def "parse version"() {
        expect:
        LocalDocker.parseDockerVersion(versionString) == version
        where:
        versionString | version
        "1.12.0"      | new DockerVersion(major: 1, minor: 12, patch: 0, meta: "")
        "1.12.0-rc2"  | new DockerVersion(major: 1, minor: 12, patch: 0, meta: "-rc2")
    }
}
