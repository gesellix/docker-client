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
    def "can retrieve an image descriptor for alpine:edge"() {
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

    @Requires({ LocalDocker.dockerVersion >= DockerVersion.parseDockerVersion("17.06") })
    def "can retrieve an image descriptor for debian:latest"() {
        when:
        def debianDescriptor = dockerClient.descriptor("debian:latest")

        then:
        debianDescriptor.status.code == 200
        debianDescriptor.content == [
                Descriptor: [
                        mediaType: "application/vnd.docker.distribution.manifest.list.v2+json",
                        digest   : "sha256:5fafd38cdee6c7e6b97356092b97389faa0aa069595f1c3cc3344428b5fd2339",
                        size     : 2364
                ],
                Platforms : [
                        [architecture: "amd64", os: "linux"],
                        [architecture: "arm", os: "linux", variant: "v5"],
                        [architecture: "arm", os: "linux", variant: "v7"],
                        [architecture: "arm64", os: "linux", variant: "v8"],
                        [architecture: "386", os: "linux"],
                        [architecture: "ppc64le", os: "linux"],
                        [architecture: "s390x", os: "linux"]
                ]
        ]
    }
}
