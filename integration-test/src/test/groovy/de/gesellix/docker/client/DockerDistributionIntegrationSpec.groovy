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
                        digest   : "sha256:84cada644406eed697ebf7fcd619764dd0140e516544e6690f4f7153a77410fd",
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
