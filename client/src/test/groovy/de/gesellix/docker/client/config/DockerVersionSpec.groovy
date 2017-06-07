package de.gesellix.docker.client.config

import spock.lang.Specification
import spock.lang.Unroll

import static de.gesellix.docker.client.config.DockerVersion.parseDockerVersion

class DockerVersionSpec extends Specification {

    @Unroll
    def "parse version #versionString"() {
        expect:
        parseDockerVersion(versionString) == version
        where:
        versionString    | version
        "1.12.0"         | new DockerVersion(major: 1, minor: 12, patch: 0, meta: "")
        "1.12.0-rc2"     | new DockerVersion(major: 1, minor: 12, patch: 0, meta: "-rc2")
        "17.03.0-ce-rc1" | new DockerVersion(major: 17, minor: 3, patch: 0, meta: "-ce-rc1")
    }

    @Unroll
    def "compare #v1 with #2"() {
        expect:
        parseDockerVersion(v1) <=> parseDockerVersion(v2) == result
        where:
        v1      | v2      || result
        "1.13"  | "17.04" || -1
        "17.05" | "17.04" || 1
        "17.05" | "17.05" || 0
    }
}
