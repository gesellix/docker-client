package de.gesellix.docker.client

import de.gesellix.docker.engine.DockerVersion
import groovy.util.logging.Slf4j
import spock.lang.Requires
import spock.lang.Specification

@Slf4j
@Requires({ LocalDocker.available() })
class DockerDistributionIntegrationSpec extends Specification {

    static DockerClient dockerClient

    def setupSpec() {
        dockerClient = new DockerClientImpl()
    }

    @Requires({ LocalDocker.dockerVersion >= DockerVersion.parseDockerVersion("17.06") })
    def descriptor() {
        when:
        def alpineDescriptor = dockerClient.descriptor("alpine:edge")

        then:
        alpineDescriptor.status.code == 200
        alpineDescriptor.content == [
                Descriptor: [
                        mediaType: "application/vnd.docker.distribution.manifest.v2+json",
                        digest   : "sha256:79d50d15bd7ea48ea00cf3dd343b0e740c1afaa8e899bee475236ef338e1b53b",
                        size     : 528
                ],
                Platforms : [
                        [
                                architecture: "amd64",
                                os          : "linux"
                        ]
                ]
        ]
    }
}
