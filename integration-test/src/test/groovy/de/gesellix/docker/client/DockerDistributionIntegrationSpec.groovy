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
    def "can retrieve an image descriptor"() {
        when:
        def alpineDescriptor = dockerClient.descriptor("alpine:edge")

        then:
        alpineDescriptor.status.code == 200
        alpineDescriptor.content == [
                Descriptor: [
                        mediaType: "application/vnd.docker.distribution.manifest.list.v2+json",
                        digest   : "sha256:2b796ae57cb164a11ce4dcc9e62a9ad10b64b38c4cc9748e456b5c11a19dc0f3",
                        size     : 433
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
