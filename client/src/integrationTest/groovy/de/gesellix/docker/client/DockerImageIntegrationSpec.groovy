package de.gesellix.docker.client

import de.gesellix.docker.client.builder.BuildContextBuilder
import de.gesellix.docker.client.util.DockerRegistry
import de.gesellix.docker.client.util.LocalDocker
import de.gesellix.docker.testutil.HttpTestServer
import de.gesellix.docker.testutil.ResourceReader
import groovy.util.logging.Slf4j
import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Specification

@Slf4j
@Requires({ LocalDocker.available() })
class DockerImageIntegrationSpec extends Specification {

    static DockerRegistry registry

    static DockerClient dockerClient

    def setupSpec() {
        dockerClient = new DockerClientImpl()
        registry = new DockerRegistry(dockerClient: dockerClient)
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

    def "build image"() {
        given:
        def inputDirectory = new ResourceReader().getClasspathResourceAsFile('build/build/Dockerfile', DockerClient).parentFile

        when:
        def buildResult = dockerClient.build(newBuildContext(inputDirectory))

        then:
        buildResult =~ "\\w{12}"

        cleanup:
        dockerClient.rmi(buildResult)
    }

    def "build image with unknown base image"() {
        given:
        def buildContextDir = File.createTempDir()
        def dockerfile = new File(buildContextDir, "Dockerfile")

        def inputDirectory = new ResourceReader().getClasspathResourceAsFile('build/build_with_unknown_base_image/Dockerfile.template', DockerClient).parentFile
        new File(inputDirectory, "Dockerfile.template").newReader().transformLine(dockerfile.newWriter()) { line ->
            line.replaceAll("\\{\\{registry}}", "")
            // TODO using the local registry only works without certificates when it's available on 'localhost'
//            line.replaceAll("\\{\\{registry}}", "${registry.url()}/")
        }

        when:
        dockerClient.build(newBuildContext(buildContextDir))

        then:
        DockerClientException ex = thrown()
        ex.cause.message == 'docker build failed'
        ex.detail.content.last() == [error      : "Error: image missing/image:latest not found",
                                     errorDetail: [message: "Error: image missing/image:latest not found"]]
    }

    def "build image with custom Dockerfile"() {
        given:
        def inputDirectory = new ResourceReader().getClasspathResourceAsFile('build/custom/Dockerfile', DockerClient).parentFile

        when:
        def buildResult = dockerClient.build(newBuildContext(inputDirectory), [rm: true, dockerfile: './Dockerfile.custom'])

        then:
        dockerClient.history(buildResult).content.first().CreatedBy.endsWith("'custom'")

        cleanup:
        dockerClient.rmi(buildResult)
    }

    def "tag image"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def imageName = "yet-another-tag"

        when:
        def buildResult = dockerClient.tag(imageId, imageName)

        then:
        buildResult.status.code == 201

        cleanup:
        dockerClient.rmi(imageName)
    }

    @Ignore
    def "push image (registry api v2)"() {
        given:
        def authDetails = dockerClient.readAuthConfig(null, null)
        def authBase64Encoded = dockerClient.encodeAuthConfig(authDetails)
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def imageName = "gesellix/test:latest"
        dockerClient.tag(imageId, imageName)

        when:
        def pushResult = dockerClient.push(imageName, authBase64Encoded)

        then:
        pushResult.status.code == 200
        and:
        pushResult.content.last().aux.Digest =~ "sha256:\\w+"

        cleanup:
        dockerClient.rmi(imageName)
    }

    @Ignore
    def "push image with registry (registry api v2)"() {
        given:
        def authDetails = dockerClient.readDefaultAuthConfig()
        def authBase64Encoded = dockerClient.encodeAuthConfig(authDetails)
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def imageName = "gesellix/test:latest"
        dockerClient.tag(imageId, imageName)

        when:
        def pushResult = dockerClient.push(imageName, authBase64Encoded, registry.url())

        then:
        pushResult.status.code == 200
        and:
        pushResult.content.last().aux.Digest =~ "sha256:\\w+"

        cleanup:
        dockerClient.rmi(imageName)
        dockerClient.rmi("${registry.url()}/${imageName}")
    }

    def "push image with undefined authentication"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def imageName = "gesellix/test:latest"
        dockerClient.tag(imageId, imageName)

        when:
        def pushResult = dockerClient.push(imageName, null, registry.url())

        then:
        pushResult.status.code == 200
        and:
        pushResult.content.last().aux.Digest =~ "sha256:\\w+"

        cleanup:
        dockerClient.rmi(imageName)
        dockerClient.rmi("${registry.url()}/${imageName}")
    }

    def "pull image"() {
        when:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")

        then:
        imageId == "sha256:6b552ee013ffc56b05df78b83a7b9717ebb99aa32224cf012c5dbea811b42334"
    }

    def "pull image by digest"() {
        when:
        def imageId = dockerClient.pull("nginx@sha256:b555f8c64ab4e85405e0d8b03f759b73ce88deb802892a3b155ef55e3e832806")

        then:
        imageId == "sha256:3c69047c6034e48a93cc1c4a769a680045104ef6d51306720409029d6e1fa364"
    }

    def "pull image from private registry"() {
        given:
        dockerClient.pull("gesellix/docker-client-testimage", "latest")
        dockerClient.push("gesellix/docker-client-testimage:latest", "", registry.url())

        when:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest", "", registry.url())

        then:
        imageId == "sha256:6b552ee013ffc56b05df78b83a7b9717ebb99aa32224cf012c5dbea811b42334"

        cleanup:
        dockerClient.rmi("${registry.url()}/gesellix/docker-client-testimage")
    }

    def "import image from url"() {
        given:
        def importUrl = getClass().getResource('importUrl/import-from-url.tar')
        def server = new HttpTestServer()
        def serverAddress = server.start('/images/', new HttpTestServer.FileServer(importUrl))
        def port = serverAddress.port
        def addresses = listPublicIps()
        def fileServerIp = addresses.first()

        when:
        def imageId = dockerClient.importUrl("http://${fileServerIp}:$port/images/${importUrl.path}", "import-from-url", "foo")

        then:
        imageId =~ "\\w+"

        cleanup:
        server.stop()
        dockerClient.rmi(imageId)
    }

    def "import image from stream"() {
        given:
        def archive = getClass().getResourceAsStream('importUrl/import-from-url.tar')

        when:
        def imageId = dockerClient.importStream(archive, "import-from-url", "foo")

        then:
        imageId =~ "\\w+"

        cleanup:
        dockerClient.rmi(imageId)
    }

    def "inspect image"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")

        when:
        def imageInspection = dockerClient.inspectImage(imageId).content

        then:
        imageInspection.Config.Image == "d3ca5fb4cded236f37a1aca37b81059378bcb6e39f6386d538a3cb630d7d6c4e"
        and:
        imageInspection.Id == "sha256:6b552ee013ffc56b05df78b83a7b9717ebb99aa32224cf012c5dbea811b42334"
        and:
        imageInspection.Parent == ""
        and:
        imageInspection.Container == "bb3a2d1eb5404835149e3639f6f8a220555a8640f4f8fe6e8877c5618ba5cd40"
    }

    def "history"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")

        when:
        def history = dockerClient.history(imageId).content

        then:
        history.collect { it.Id } == [
                "sha256:6b552ee013ffc56b05df78b83a7b9717ebb99aa32224cf012c5dbea811b42334",
                "<missing>",
                "<missing>",
                "<missing>"
        ]
    }

    def "list images"() {
        given:
        dockerClient.pull("gesellix/docker-client-testimage:latest")

        when:
        def images = dockerClient.images().content

        then:
        def imageById = images.find {
            it.Id == "sha256:6b552ee013ffc56b05df78b83a7b9717ebb99aa32224cf012c5dbea811b42334"
        }
        imageById.Created == 1454887777
        imageById.ParentId == ""
        imageById.RepoTags.contains "gesellix/docker-client-testimage:latest"
    }

    def "list images with intermediate layers"() {
        when:
        def images = dockerClient.images([:]).content
        def fullImages = dockerClient.images([all: true]).content

        then:
        def imageIds = images.collect { image -> image.Id }
        def fullImageIds = fullImages.collect { image -> image.Id }
        imageIds != fullImageIds
        and:
        fullImageIds.size() > imageIds.size()
    }

    def "list images filtered"() {
        when:
        def images = dockerClient.images([filters: [dangling: ["true"]]]).content

        then:
        images.every { image ->
            image.RepoTags == ["<none>:<none>"]
        }
    }

    def "rm image"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        dockerClient.tag(imageId, "an_image_to_be_deleted")

        when:
        def rmImageResult = dockerClient.rmi("an_image_to_be_deleted")

        then:
        rmImageResult.status.code == 200
    }

    def "rm unknown image"() {
        when:
        def rmImageResult = dockerClient.rmi("an_unknown_image")

        then:
        rmImageResult.status.code == 404
    }

    def "rm image with existing container"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        dockerClient.tag(imageId, "an_image_with_existing_container")

        def containerConfig = ["Cmd": ["true"]]
        def tag = "latest"
        def name = "another-example-name"
        dockerClient.run("an_image_with_existing_container", containerConfig, tag, name)

        when:
        def rmImageResult = dockerClient.rmi("an_image_with_existing_container:latest")

        then:
        rmImageResult.status.code == 200

        cleanup:
        dockerClient.stop(name)
        dockerClient.wait(name)
        dockerClient.rm(name)
    }

    def "search"() {
        when:
        def searchResult = dockerClient.search("testimage")

        then:
        searchResult.content.contains([
                description : "",
                is_automated: true,
                is_official : false,
//                is_trusted  : true,
                name        : "gesellix/docker-client-testimage",
                star_count  : 0
        ])
    }

    InputStream newBuildContext(File baseDirectory) {
        def buildContext = File.createTempFile("buildContext", ".tar")
        buildContext.deleteOnExit()
        BuildContextBuilder.archiveTarFilesRecursively(baseDirectory, buildContext)
        return new FileInputStream(buildContext)
    }

    def matchIpv4 = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\$"

    def listPublicIps() {
        def addresses = []
        NetworkInterface.getNetworkInterfaces()
                .findAll { !it.loopback }
                .each { NetworkInterface iface ->
            iface.inetAddresses.findAll {
                it.hostAddress.matches(matchIpv4)
            }.each {
                addresses.add(it.hostAddress)
            }
        }
        addresses
    }
}
