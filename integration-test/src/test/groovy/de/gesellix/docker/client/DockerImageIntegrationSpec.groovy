package de.gesellix.docker.client

import de.gesellix.docker.client.builder.BuildContextBuilder
import de.gesellix.docker.registry.DockerRegistry
import de.gesellix.docker.testutil.HttpTestServer
import de.gesellix.testutil.ResourceReader
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.regex.Pattern

import static TestConstants.CONSTANTS
import static java.util.concurrent.TimeUnit.SECONDS

@Slf4j
@Requires({ LocalDocker.available() })
class DockerImageIntegrationSpec extends Specification {

    static DockerRegistry registry

    static DockerClient dockerClient
    boolean isNativeWindows = LocalDocker.isNativeWindows()

    def setupSpec() {
        dockerClient = new DockerClientImpl()
        registry = new DockerRegistry(dockerClient)
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
        def dockerfile = "build/build/Dockerfile"
        if (isNativeWindows) {
            dockerfile = "build/build-windows/Dockerfile"
        }
        def inputDirectory = new ResourceReader().getClasspathResourceAsFile(dockerfile, DockerClient).parentFile

        when:
        def buildResult = dockerClient.build(newBuildContext(inputDirectory))

        then:
        buildResult =~ "[0-9a-z]{12}"

        cleanup:
        dockerClient.rmi(buildResult)
    }

    def "build image with tag"() {
        given:
        def dockerfile = "build/build/Dockerfile"
        if (isNativeWindows) {
            dockerfile = "build/build-windows/Dockerfile"
        }
        def inputDirectory = new ResourceReader().getClasspathResourceAsFile(dockerfile, DockerClient).parentFile

        when:
        def buildResult = dockerClient.build(
                newBuildContext(inputDirectory),
                [rm: true,
                 t : "docker-client/tests:tag"])

        then:
        buildResult =~ "[0-9a-z]{12}"

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
        ex.detail.content.last().error.contains " missing/image"
        ex.detail.content.last().errorDetail.message.contains " missing/image"
    }

    def "build image with custom Dockerfile"() {
        given:
        def dockerfile = "build/custom/Dockerfile"
        if (isNativeWindows) {
            dockerfile = "build/custom-windows/Dockerfile"
        }
        def inputDirectory = new ResourceReader().getClasspathResourceAsFile(dockerfile, DockerClient).parentFile

        when:
        def buildResult = dockerClient.build(newBuildContext(inputDirectory), [
                rm        : true,
                dockerfile: './Dockerfile.custom',
                buildargs : [the_arg: "custom-arg"]
        ])

        then:
        def history = dockerClient.history(buildResult).content
        def mostRecentEntry = history.first()
        mostRecentEntry.CreatedBy.startsWith("|1 the_arg=custom-arg ")
        mostRecentEntry.CreatedBy.endsWith("'custom \${the_arg}'")

        cleanup:
        dockerClient.rmi(buildResult)
    }

    def "build image with custom stream callback"() {
        given:
        def dockerfile = "build/log/Dockerfile"
        if (isNativeWindows) {
            dockerfile = "build/log-windows/Dockerfile"
        }
        def inputDirectory = new ResourceReader().getClasspathResourceAsFile(dockerfile, DockerClient).parentFile

        when:
        CountDownLatch latch = new CountDownLatch(1)
        def events = []
        def response = dockerClient.build(newBuildContext(inputDirectory), [rm: true], new DockerAsyncCallback() {
            @Override
            onEvent(Object event) {
                def parsedEvent = new JsonSlurper().parseText(event as String)
                events << parsedEvent
            }

            @Override
            onFinish() {
                latch.countDown()
            }
        })
        latch.await(5, SECONDS)

        then:
        if (isNativeWindows) {
            events.first().stream =~ "Step 1(/10)? : FROM microsoft/nanoserver\n"
        } else {
            events.first().stream =~ "Step 1(/10)? : FROM alpine:edge\n"
        }
        def imageId = events.last().stream.trim() - "Successfully built "

        cleanup:
        response.taskFuture.cancel(true)
        dockerClient.rmi(imageId)
    }

    def "build image with logs"() {
        given:
        def dockerfile = "build/log/Dockerfile"
        if (isNativeWindows) {
            dockerfile = "build/log-windows/Dockerfile"
        }
        def inputDirectory = new ResourceReader().getClasspathResourceAsFile(dockerfile, DockerClient).parentFile

        when:
        def result = dockerClient.buildWithLogs(newBuildContext(inputDirectory))

        then:
        if (isNativeWindows) {
            result.log.first().stream =~ "Step 1(/10)? : FROM microsoft/nanoserver\n"
        } else {
            result.log.first().stream =~ "Step 1(/10)? : FROM alpine:edge\n"
        }
        result.log.last().stream.startsWith("Successfully built ")
        def imageId = result.log.last().stream.trim() - "Successfully built "
        result.imageId == imageId

        cleanup:
        dockerClient.rmi(imageId)
    }

    def "build image async and fail"() {
        given:
        def dockerfile = "build/fail/Dockerfile"
        if (isNativeWindows) {
            dockerfile = "build/fail-windows/Dockerfile"
        }
        def inputDirectory = new ResourceReader().getClasspathResourceAsFile(dockerfile, DockerClient).parentFile

        when:
        CountDownLatch latch = new CountDownLatch(1)
        def events = []
        def result = dockerClient.build(newBuildContext(inputDirectory), [rm: true], new DockerAsyncCallback() {
            @Override
            onEvent(Object event) {
                def parsedEvent = new JsonSlurper().parseText(event as String)
                events << parsedEvent
            }

            @Override
            onFinish() {
                latch.countDown()
            }
        })
        latch.await(20, SECONDS)

        then:
        println "count: ${events.size()}"
        println events
        if (isNativeWindows) {
            events.get(0).stream =~ "Step 1(/2)? : FROM microsoft/nanoserver\n"
        } else {
            events.get(0).stream =~ "Step 1(/2)? : FROM alpine:edge\n"
        }
        events.get(1).stream =~ "\\s---> \\w+\n"
        events.get(2).stream =~ "Step 2(/2)? : RUN i-will-fail\n"
        if (isNativeWindows) {
            events.get(5).error == "The command 'cmd /S /C i-will-fail' returned a non-zero code: 1"
        } else {
            events.get(5).error == "The command '/bin/sh -c i-will-fail' returned a non-zero code: 127"
        }
        def containerId = getContainerId(events.get(3).stream as String)
        def imageId = getImageId(events.get(1).stream as String)

        cleanup:
        result.taskFuture.cancel(true)
        dockerClient.rm(containerId)
        dockerClient.rmi(imageId)
    }

// TODO
//    def "build image async with logs and fail"() {
//        def buildContext = new ByteArrayInputStream([42] as byte[])
//        def response = [
//                ["stream": "Step 1/2 : FROM alpine:edge"],
//                ["stream": " ---\u003e a1a3cae7a75e"],
//                ["stream": "Step 2/2 : RUN i-will-fail"],
//                ["stream": " ---\u003e Running in e01136c552bc"],
//                ["stream": "\u001b[91m/bin/sh: i-will-fail: not found\n\u001b[0m"],
//                ["errorDetail": ["code": 127, "message": "The command '/bin/sh -c i-will-fail' returned a non-zero code: 127"],
//                 "error"      : "The command '/bin/sh -c i-will-fail' returned a non-zero code: 127"]
//        ].collect { entry ->
//            "${new JsonBuilder(entry).toString()}"
//        }
//
//        def server = new HttpTestServer()
//        def serverAddress = server.start('/images/', new ChunkedResponseServer(response))
//        def port = serverAddress.port
//        def addresses = listPublicIps()
//        def fileServerIp = addresses.first()
//
//        def headersBuilder = new Headers.Builder()
//        [
//                "Content-Type: application/json",
//                "Date: Fri, 13 Jan 2017 22:09:24 GMT",
//                "Docker-Experimental: true",
//                "Server: Docker/1.13.0-rc6 (linux)",
//                "Transfer-Encoding: chunked"
//        ].each { line ->
//            headersBuilder.add(line)
//        }
//
//        when:
//        dockerClient.buildWithLogs(buildContext, ["rm": true], new Timeout(5, SECONDS))
//
//        then:
//        1 * httpClient.post([path              : "/build",
//                             query             : ["rm": true],
//                             body              : buildContext,
//                             requestContentType: "application/octet-stream",
//                             async             : true]) >> new EngineResponse(
//                headers: headersBuilder.build(),
//                stream: new ByteArrayInputStream(new JsonBuilder(response).toString().bytes))
//        and:
//        dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
//            assert arguments[1]?.message == "docker build failed"
//        }
//
//        cleanup:
//        server.stop()
//    }
//    static class ChunkedResponseServer implements HttpHandler {
//
//        DockerClient delegate
//        String[] chunks
//
//        ChunkedResponseServer(DockerClient delegate, List<String>... chunks) {
//            this.delegate = delegate
//            this.chunks = chunks
//        }
//
//        @Override
//        void handle(HttpExchange httpExchange) {
//            if (httpExchange.requestMethod == 'POST' && httpExchange.requestURI.path == '/build') {
//                httpExchange.sendResponseHeaders(200, 0)
//                chunks.each { String chunk ->
//                    httpExchange.responseBody.write(chunk.bytes)
//                }
//                httpExchange.responseBody.close()
//            } else {
//                return delegate.
//            }
//        }
//    }

    def getContainerId(String buildImageLogEvent) {
        return getFirstMatchingGroup("\\s---> Running in (\\w+)\n", buildImageLogEvent)
    }

    def getImageId(String buildImageLogEvent) {
        return getFirstMatchingGroup("\\s---> (\\w+)\n", buildImageLogEvent)
    }

    def getFirstMatchingGroup(String pattern, String input) {
        def matcher = Pattern.compile(pattern).matcher(input)
        if (matcher.find()) {
            return matcher.group(1)
        } else {
            return null
        }
    }

//    def "buildWithLogs and fail"() {
//        given:
//        def inputDirectory = new ResourceReader().getClasspathResourceAsFile('build/fail/Dockerfile', DockerClient).parentFile
//
//        when:
//        dockerClient.buildWithLogs(newBuildContext(inputDirectory))
//
//        then:
//        DockerClientException exc = thrown()
//        exc.detail.error == 'The command \'/bin/sh -c i-will-fail\' returned a non-zero code: 127'
//    }

    def "tag image"() {
        given:
        def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)
        def imageName = "yet-another-tag"

        when:
        def buildResult = dockerClient.tag(imageId, imageName)

        then:
        buildResult.status.code == 201

        cleanup:
        dockerClient.rmi(imageName)
    }

    @Ignore
    "push image (registry api v2)"() {
        given:
        def authDetails = dockerClient.readAuthConfig(null, null)
        def authBase64Encoded = dockerClient.encodeAuthConfig(authDetails)
        def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)
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
    "push image with registry (registry api v2)"() {
        given:
        def authDetails = dockerClient.readDefaultAuthConfig()
        def authBase64Encoded = dockerClient.encodeAuthConfig(authDetails)
        def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)
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
        def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)
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
        def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)

        then:
        imageId == CONSTANTS.imageDigest
    }

    def "pull image by digest"() {
        when:
        def imageId = dockerClient.pull("nginx@sha256:b555f8c64ab4e85405e0d8b03f759b73ce88deb802892a3b155ef55e3e832806")

        then:
        imageId == "sha256:3c69047c6034e48a93cc1c4a769a680045104ef6d51306720409029d6e1fa364"
    }

    def "pull image from private registry"() {
        given:
        dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)
        dockerClient.push(CONSTANTS.imageName, "", registry.url())

        when:
        def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag, "", registry.url())

        then:
        imageId == CONSTANTS.imageDigest

        cleanup:
        dockerClient.rmi("${registry.url()}/${CONSTANTS.imageRepo}")
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
        def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)

        when:
        def imageInspection = dockerClient.inspectImage(imageId).content

        then:
        imageInspection.Config.Image == isNativeWindows ? "todo" : "sha256:728f7fae29a7bc4c1166cc3206eec3e8271bf3408034051b742251d8bdc07db8"
        and:
        imageInspection.Id == CONSTANTS.imageDigest
        and:
        imageInspection.Parent == ""
        and:
        imageInspection.Container == isNativeWindows ? "todo" : "7ddb235457b38a125d107ec7d53f95254cbf579069f29a2c731bc9471a153524"
    }

    def "history"() {
        given:
        def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)

        when:
        def history = dockerClient.history(imageId).content

        then:
        history.collect { it.Id } == [
                CONSTANTS.imageDigest,
                "<missing>",
                "<missing>"
        ]
    }

    def "list images"() {
        given:
        dockerClient.pull(CONSTANTS.imageName)

        when:
        def images = dockerClient.images().content

        then:
        def imageById = images.find {
            it.Id == CONSTANTS.imageDigest
        }
        imageById.Created == 1483093519
        imageById.ParentId == ""
        imageById.RepoTags.contains CONSTANTS.imageName
    }

    def "list images with intermediate layers"() {
        given:
        def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)
        def container1Info = dockerClient.createContainer(["Cmd": ["-"], "Image": imageId]).content
        dockerClient.commit(container1Info.Id, [repo: 'repo1', tag: 'tag1'])
        dockerClient.rm(container1Info.Id)
        def container2Info = dockerClient.createContainer(["Cmd": ["-"], "Image": "repo1:tag1"]).content
        dockerClient.commit(container2Info.Id, [repo: 'repo2', tag: 'tag2'])
        dockerClient.rm(container2Info.Id)
        dockerClient.rmi("repo1:tag1")

        when:
        def images = dockerClient.images([:]).content
        def fullImages = dockerClient.images([all: true]).content

        then:
        def imageIds = images.collect { image -> image.Id }
        def fullImageIds = fullImages.collect { image -> image.Id }
        imageIds != fullImageIds
        and:
        fullImageIds.size() > imageIds.size()

        cleanup:
        dockerClient.rmi("repo2:tag2")
    }

    def "list images filtered"() {
        given:
        def dockerfile = "build/build/Dockerfile"
        if (isNativeWindows) {
            dockerfile = "build/build-windows/Dockerfile"
        }
        def inputDirectory = new ResourceReader().getClasspathResourceAsFile(dockerfile, DockerClient).parentFile
        def buildResult = dockerClient.build(newBuildContext(inputDirectory))

        when:
        def images = dockerClient.images([filters: [dangling: ["true"]]]).content

        then:
        images.findAll { image ->
            image.RepoTags == ["<none>:<none>"] || image.RepoTags == null
        }.find { image ->
            image.Id.startsWith "sha256:$buildResult"
        }

        cleanup:
        dockerClient.rmi(buildResult)
    }

    def "rm image"() {
        given:
        def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)
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
        def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)
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
                description : "A Testimage used for Docker Client integration tests.\n",
                is_automated: true,
                is_official : false,
//                is_trusted  : true,
                name        : CONSTANTS.imageRepo,
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
