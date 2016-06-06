package de.gesellix.docker.client

import groovy.util.logging.Slf4j
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ws.WebSocket
import okhttp3.ws.WebSocketCall
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.IOUtils
import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

import static java.util.concurrent.TimeUnit.MILLISECONDS

@Slf4j
@Requires({ LocalDocker.available() })
class DockerClientImplIntegrationSpec extends Specification {

    static DockerRegistry registry

    static DockerClient dockerClient

    def setupSpec() {
        dockerClient = new DockerClientImpl(
//                config: new DockerConfig(
//                        dockerHost: "http://192.168.99.100:2376",
//                        certPath: "/Users/${System.getProperty('user.name')}/.docker/machine/machines/default")
        )
//        dockerClient.config.apiVersion = "v1.23"
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

    def info() {
        when:
        def info = dockerClient.info().content

        then:
        def expectedKeys = [
                "Architecture",
                "BridgeNfIp6tables", "BridgeNfIptables",
                "CPUSet", "CPUShares", "CgroupDriver", "ClusterAdvertise", "ClusterStore", "Containers", "ContainersPaused", "ContainersRunning", "ContainersStopped", "CpuCfsPeriod", "CpuCfsQuota",
                "Debug", "DockerRootDir", "Driver", "DriverStatus",
                "ExecutionDriver", "ExperimentalBuild",
                "HttpProxy", "HttpsProxy",
                "ID", "IPv4Forwarding", "Images", "IndexServerAddress",
                "KernelMemory", "KernelVersion",
                "Labels", "LoggingDriver",
                "MemTotal", "MemoryLimit",
                "NCPU", "NEventsListener", "NFd", "NGoroutines", "Name", "NoProxy",
                "OSType", "OomKillDisable", "OperatingSystem",
                "Plugins",
                "RegistryConfig",
                "ServerVersion", "SwapLimit", "SystemStatus", "SystemTime"]
        new ArrayList<>(info.keySet()).each { expectedKeys.contains(it) }

        and:
        info.Containers >= 0
        info.DockerRootDir =~ "(/mnt/sda1)?/var/lib/docker"
        info.DriverStatus.findAll {
            it[0] == "Root Dir" || it[0] == "Backing Filesystem" || it[0] == "Dirs" || it[0] == "Dirperm1 Supported"
        }.size() == 4
        info.ExperimentalBuild == false
        info.HttpProxy == ""
        info.HttpsProxy == ""
        info.ID =~ "([0-9A-Z]{4}:?){12}"
        info.Images > 0
        info.IndexServerAddress == "https://index.docker.io/v1/"
        info.IPv4Forwarding == true
        info.Labels == null
        info.LoggingDriver == "json-file"
        info.MemTotal > 0
        info.MemoryLimit == true
        info.NoProxy == "" || info.NoProxy == "*.local, 169.254/16"
        info.OomKillDisable == true
        info.RegistryConfig == [
                "IndexConfigs"         : [
                        "docker.io": ["Mirrors" : null,
                                      "Name"    : "docker.io",
                                      "Official": true,
                                      "Secure"  : true]
                ],
                "InsecureRegistryCIDRs": ["127.0.0.0/8"],
                "Mirrors"              : null
        ]
        info.SystemTime =~ "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2,}.(\\d{3,}Z)?"
    }

    def version() {
        when:
        def version = dockerClient.version().content

        then:
        version.ApiVersion == "1.23"
        version.Arch == "amd64"
        version.BuildTime == "2016-06-03T08:34:52.695511840+00:00"
        version.GitCommit == "8b63c77"
        version.GoVersion == "go1.5.4"
        version.KernelVersion =~ "\\d.\\d{1,2}.\\d{1,2}(-\\w+)?"
        version.Os == "linux"
        version.Version == "1.11.1"
    }

    def auth() {
        given:
        def authDetails = dockerClient.readAuthConfig(null, null)
        def authPlain = authDetails

        when:
        def authResult = dockerClient.auth(authPlain)

        then:
        authResult.status.code == 200
    }

    def "build image"() {
        given:
        def inputDirectory = new ResourceReader().getClasspathResourceAsFile('build/build/Dockerfile').parentFile

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

        def inputDirectory = new ResourceReader().getClasspathResourceAsFile('build/build_with_unknown_base_image/Dockerfile.template').parentFile
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
        ex.detail.content.last() == [error      : "Error: image missing/image not found",
                                     errorDetail: [message: "Error: image missing/image not found"]]
    }

    def "build image with custom Dockerfile"() {
        given:
        def inputDirectory = new ResourceReader().getClasspathResourceAsFile('build/custom/Dockerfile').parentFile

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
        dockerClient.tag(imageId, imageName, true)

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
        dockerClient.tag(imageId, imageName, true)

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
        dockerClient.tag(imageId, imageName, true)

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
        def server = new TestHttpServer()
        def serverAddress = server.start('/images/', new TestHttpServer.FileServer(importUrl))
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

    def "export from container"() {
        given:
        def archive = getClass().getResourceAsStream('importUrl/import-from-url.tar')
        def imageId = dockerClient.importStream(archive)
        def container = dockerClient.createContainer([Image: imageId, Cmd: ["-"]]).content.Id

        when:
        def response = dockerClient.export(container)

        then:
        listTarEntries(response.stream as InputStream).contains "something.txt"

        cleanup:
        dockerClient.rm(container)
        dockerClient.rmi(imageId)
    }

    def "list containers"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def imageName = "list_containers"
        dockerClient.tag(imageId, imageName, true)
        def containerConfig = ["Cmd"  : ["true"],
                               "Image": imageName]
        def containerId = dockerClient.createContainer(containerConfig).content.Id
        dockerClient.startContainer(containerId)

        when:
        def containers = dockerClient.ps().content

        then:
        containers.find { it.Id == containerId }.Image == "${imageName}"

        cleanup:
        dockerClient.stop(containerId)
        dockerClient.wait(containerId)
        dockerClient.rm(containerId)
        dockerClient.rmi(imageName)
    }

    def "inspect container"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def imageName = "inspect_container"
        def containerConfig = ["Cmd"       : ["true"],
                               "Image"     : "inspect_container",
                               "HostConfig": ["PublishAllPorts": true]]
        dockerClient.tag(imageId, imageName, true)
        def containerId = dockerClient.createContainer(containerConfig).content.Id
        dockerClient.startContainer(containerId)

        when:
        def containerInspection = dockerClient.inspectContainer(containerId).content

        then:
        containerInspection.HostnamePath =~ "\\w*/var/lib/docker/containers/${containerId}/hostname".toString()
        and:
        containerInspection.Config.Cmd == ["true"]
        and:
        containerInspection.Config.Image == "inspect_container"
        and:
        containerInspection.Image =~ "${imageId}\\w*"
        and:
        containerInspection.Id == containerId

        cleanup:
        dockerClient.stop(containerId)
        dockerClient.wait(containerId)
        dockerClient.rm(containerId)
        dockerClient.rmi(imageName)
    }

    def "diff"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def containerConfig = ["Cmd"  : ["/bin/sh", "-c", "echo 'hallo' > /change.txt"],
                               "Image": imageId]
        def containerId = dockerClient.run(imageId, containerConfig).container.content.Id
        dockerClient.stop(containerId)

        when:
        def changes = dockerClient.diff(containerId).content

        then:
        changes == [
                [Kind: 1, Path: "/change.txt"]
        ]

        cleanup:
        dockerClient.wait(containerId)
        dockerClient.rm(containerId)
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

    def "create container"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def containerConfig = ["Cmd"   : ["true"],
                               "Image" : imageId,
                               "Labels": [
                                       "a nice label" : "with a nice value",
                                       "another-label": "{'foo':'bar'}"
                               ]]

        when:
        def containerInfo = dockerClient.createContainer(containerConfig).content

        then:
        containerInfo.Id =~ "\\w+"

        cleanup:
        dockerClient.rm(containerInfo.Id)
    }

    def "create container with name"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def containerConfig = ["Cmd"  : ["true"],
                               "Image": imageId]

        when:
        def containerInfo = dockerClient.createContainer(containerConfig, [name: "example"]).content

        then:
        containerInfo.Id =~ "\\w+"

        cleanup:
        dockerClient.rm("example")
    }

    def "create container with unknown base image"() {
        given:
        dockerClient.rm("example")
        def containerConfig = ["Cmd"  : ["true"],
                               "Image": "gesellix/docker-client-testimage:unkown"]

        when:
        dockerClient.createContainer(containerConfig, [name: "example"])

        then:
        DockerClientException ex = thrown()
        ex.cause.message == 'docker pull failed'
        ex.detail.content.last() == [error      : "Tag unkown not found in repository docker.io/gesellix/docker-client-testimage",
                                     errorDetail: [message: "Tag unkown not found in repository docker.io/gesellix/docker-client-testimage"]]
    }

    def "start container"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def containerConfig = ["Cmd"  : ["true"],
                               "Image": imageId]
        def containerId = dockerClient.createContainer(containerConfig).content.Id

        when:
        def startContainerResult = dockerClient.startContainer(containerId)

        then:
        startContainerResult.status.code == 204

        cleanup:
        dockerClient.stop(containerId)
        dockerClient.wait(containerId)
        dockerClient.rm(containerId)
    }

    def "update container"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def name = "update-container"
        def containerStatus = dockerClient.run(imageName, containerConfig, tag, name)

        when:
        def updateConfig = [
                "Memory"    : 314572800,
                "MemorySwap": 514288000]
        def updateResult = dockerClient.updateContainer(containerStatus.container.content.Id, updateConfig)

        then:
        updateResult.status.success

        cleanup:
        dockerClient.stop(name)
        dockerClient.wait(name)
        dockerClient.rm(name)
    }

    def "run container with existing base image"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]

        when:
        def containerStatus = dockerClient.run(imageName, containerConfig, tag)

        then:
        containerStatus.status.status.code == 204

        cleanup:
        dockerClient.stop(containerStatus.container.content.Id)
        dockerClient.wait(containerStatus.container.content.Id)
        dockerClient.rm(containerStatus.container.content.Id)
    }

    def "run container with PortBindings"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd"       : cmds,
                               ExposedPorts: ["4711/tcp": [:]],
                               "HostConfig": ["PortBindings": [
                                       "4711/tcp": [
                                               ["HostIp"  : "0.0.0.0",
                                                "HostPort": "4712"]]
                               ]]]

        when:
        def containerStatus = dockerClient.run(imageName, containerConfig, tag)

        then:
        containerStatus.status.status.code == 204
        and:
        dockerClient.inspectContainer(containerStatus.container.content.Id).content.Config.ExposedPorts == ["4711/tcp": [:]]
        and:
        dockerClient.inspectContainer(containerStatus.container.content.Id).content.HostConfig.PortBindings == [
                "4711/tcp": [
                        ["HostIp"  : "0.0.0.0",
                         "HostPort": "4712"]]
        ]

        cleanup:
        dockerClient.stop(containerStatus.container.content.Id)
        dockerClient.wait(containerStatus.container.content.Id)
        dockerClient.rm(containerStatus.container.content.Id)
    }

    def "run container with name"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def name = "example-name"

        when:
        def containerStatus = dockerClient.run(imageName, containerConfig, tag, name)

        then:
        containerStatus.status.status.code == 204

        and:
        def containers = dockerClient.ps().content
        containers.findAll { it.Names == ["/example-name"] }?.size() == 1

        cleanup:
        dockerClient.stop(containerStatus.container.content.Id)
        dockerClient.wait(containerStatus.container.content.Id)
        dockerClient.rm(containerStatus.container.content.Id)
    }

    def "restart container"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def containerStatus = dockerClient.run(imageName, containerConfig, tag)

        when:
        def result = dockerClient.restart(containerStatus.container.content.Id)

        then:
        result.status.code == 204

        cleanup:
        dockerClient.stop(containerStatus.container.content.Id)
        dockerClient.wait(containerStatus.container.content.Id)
        dockerClient.rm(containerStatus.container.content.Id)
    }

    def "stop container"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def containerStatus = dockerClient.run(imageName, containerConfig, tag)

        when:
        def result = dockerClient.stop(containerStatus.container.content.Id)

        then:
        result.status.code == 204

        cleanup:
        dockerClient.wait(containerStatus.container.content.Id)
        dockerClient.rm(containerStatus.container.content.Id)
    }

    def "kill container"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def containerStatus = dockerClient.run(imageName, containerConfig, tag)

        when:
        def result = dockerClient.kill(containerStatus.container.content.Id)

        then:
        result.status.code == 204

        cleanup:
        dockerClient.wait(containerStatus.container.content.Id)
        dockerClient.rm(containerStatus.container.content.Id)
    }

    def "wait container"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def containerStatus = dockerClient.run(imageName, containerConfig, tag)
        dockerClient.stop(containerStatus.container.content.Id)

        when:
        def result = dockerClient.wait(containerStatus.container.content.Id)

        then:
        result.status.code == 200
        and:
        result.content.StatusCode == 137

        cleanup:
        dockerClient.rm(containerStatus.container.content.Id)
    }

    def "pause container"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def containerStatus = dockerClient.run(imageName, containerConfig, tag)

        when:
        def result = dockerClient.pause(containerStatus.container.content.Id)

        then:
        result.status.code == 204

        cleanup:
        dockerClient.unpause(containerStatus.container.content.Id)
        dockerClient.stop(containerStatus.container.content.Id)
        dockerClient.wait(containerStatus.container.content.Id)
        dockerClient.rm(containerStatus.container.content.Id)
    }

    def "unpause container"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def containerStatus = dockerClient.run(imageName, containerConfig, tag)
        dockerClient.pause(containerStatus.container.content.Id)

        when:
        def result = dockerClient.unpause(containerStatus.container.content.Id)

        then:
        result.status.code == 204

        cleanup:
        dockerClient.stop(containerStatus.container.content.Id)
        dockerClient.wait(containerStatus.container.content.Id)
        dockerClient.rm(containerStatus.container.content.Id)
    }

    def "rm container"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def containerConfig = ["Cmd"  : ["true"],
                               "Image": imageId]
        def containerId = dockerClient.createContainer(containerConfig).content.Id

        when:
        def rmContainerResult = dockerClient.rm(containerId)

        then:
        rmContainerResult.status.code == 204
    }

    def "rm unknown container"() {
        when:
        def rmContainerResult = dockerClient.rm("a_not_so_random_id")

        then:
        rmContainerResult.status.code == 404
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

    def "rm unkown image"() {
        when:
        def rmImageResult = dockerClient.rmi("an_unkown_image")

        then:
        rmImageResult.status.code == 404
    }

    def "rm image with existing container"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        dockerClient.tag(imageId, "an_image_with_existing_container", true)

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

    def "exec create"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def name = "create-exec"
        def containerStatus = dockerClient.run(imageName, containerConfig, tag, name)

        when:
        def execConfig = ["Cmd": [
                'echo "hello exec!"'
        ]]
        def execCreateResult = dockerClient.createExec(containerStatus.container.content.Id, execConfig).content

        then:
        execCreateResult?.Id =~ "[0-9a-f]+"

        cleanup:
        dockerClient.stop(name)
        dockerClient.wait(name)
        dockerClient.rm(name)
    }

    def "exec start"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def name = "start-exec"
        def containerStatus = dockerClient.run(imageName, containerConfig, tag, name)
        def containerId = containerStatus.container.content.Id
        def execCreateConfig = [
                "AttachStdin" : false,
                "AttachStdout": true,
                "AttachStderr": true,
                "Tty"         : false,
                "Cmd"         : [
                        "ls", "-lisah", "/"
                ]]

        def execCreateResult = dockerClient.createExec(containerId, execCreateConfig).content
        def execId = execCreateResult.Id

        when:
        def execStartConfig = [
                "Detach": false,
                "Tty"   : false]
        def execStream = dockerClient.startExec(execId, execStartConfig)

        then:
        execStream != null

        cleanup:
        dockerClient.stop(name)
        dockerClient.wait(name)
        dockerClient.rm(name)
    }

    def "copy"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def imageName = "copy_container"
        def containerConfig = ["Cmd"  : ["sh", "-c", "echo -n -e 'to be or\nnot to be' > /file1.txt"],
                               "Image": "copy_container"]
        dockerClient.tag(imageId, imageName)
        def containerInfo = dockerClient.run(imageName, containerConfig, [:])
        def containerId = containerInfo.container.content.Id

        when:
        def tarContent = dockerClient.copy(containerId, [Resource: "/file1.txt"]).stream

        then:
        def fileContent = dockerClient.extractSingleTarEntry(tarContent as InputStream, "file1.txt")
        and:
        fileContent == "to be or\nnot to be".bytes

        cleanup:
        dockerClient.stop(containerId)
        dockerClient.wait(containerId)
        dockerClient.rm(containerId)
        dockerClient.rmi(imageName)
    }

    def "rename"() {
        given:
        dockerClient.rm("a_wonderful_new_name")
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def containerConfig = ["Cmd"  : ["true"],
                               "Image": imageId]
        def containerId = dockerClient.createContainer(containerConfig).content.Id

        when:
        def renameContainerResult = dockerClient.rename(containerId, "a_wonderful_new_name")

        then:
        renameContainerResult.status.code == 204

        cleanup:
        dockerClient.rm(containerId)
    }

    def "search"() {
        when:
        def searchResult = dockerClient.search("testimage")

        then:
        searchResult.content.contains([
                description : "",
                is_automated: true,
                is_official : false,
                is_trusted  : true,
                name        : "gesellix/docker-client-testimage",
                star_count  : 0
        ])
    }

    @Requires({ LocalDocker.isTcpSocket() })
    def "attach (websocket)"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def containerConfig = [
                Tty      : true,
                OpenStdin: true,
                Cmd      : ["/bin/sh", "-c", "cat"]
        ]
        def containerId = dockerClient.run(imageId, containerConfig).container.content.Id

        def executor = Executors.newSingleThreadExecutor()
        def ourMessage = "hallo welt ${UUID.randomUUID()}!".toString()

        def openConnection = new CountDownLatch(1)
        def AtomicReference<WebSocket> webSocketReference = new AtomicReference<>()
        def receiveMessage = new CountDownLatch(1)
        def receivedMessages = []
        def listener = new DefaultWebSocketListener() {
            @Override
            void onOpen(WebSocket webSocket, Response response) {
                webSocketReference.set(webSocket)
                openConnection.countDown()
                executor.execute(new Runnable() {
                    @Override
                    void run() {
                        webSocket.sendMessage(RequestBody.create(WebSocket.TEXT, ourMessage))
                    }
                })
            }

            @Override
            void onMessage(ResponseBody message) throws IOException {
                receivedMessages << message.string()
                receiveMessage.countDown()
            }
        }

        when:
        WebSocketCall wsCall = dockerClient.attachWebsocket(
                containerId,
                [stream: 1, stdin: 1, stdout: 1, stderr: 1],
                listener)


        openConnection.await(500, MILLISECONDS)
        receiveMessage.await(500, MILLISECONDS)

        then:
        receivedMessages.contains ourMessage

        cleanup:
        webSocketReference.get().close(1000, "cleanup")
        dockerClient.stop(containerId)
        dockerClient.wait(containerId)
        dockerClient.rm(containerId)
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

    def listTarEntries(InputStream tarContent) {
        def stream = new TarArchiveInputStream(new BufferedInputStream(tarContent))

        def entryNames = []
        TarArchiveEntry entry
        while (entry = stream.nextTarEntry) {
            def entryName = entry.name
            entryNames << entryName

            log.debug("entry name: ${entryName}")
//            log.debug("entry size: ${entry.size}")
        }
        IOUtils.closeQuietly(stream)
        return entryNames
    }
}
