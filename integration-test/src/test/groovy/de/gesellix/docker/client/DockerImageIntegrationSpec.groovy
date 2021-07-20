package de.gesellix.docker.client

import de.gesellix.docker.client.builder.BuildContextBuilder
import de.gesellix.docker.client.image.BuildConfig
import de.gesellix.docker.client.image.BuildResult
import de.gesellix.docker.registry.DockerRegistry
import de.gesellix.docker.testutil.HttpTestServer
import de.gesellix.docker.testutil.NetworkInterfaces
import de.gesellix.testutil.ResourceReader
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.regex.Pattern

import static de.gesellix.docker.client.TestConstants.CONSTANTS
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
    buildResult.imageId =~ "[0-9a-z]{12}"

    cleanup:
    dockerClient.rmi(buildResult.imageId)
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
        new BuildConfig(query: [
            rm: true,
            t : 'docker-client/tests:tag'
        ]))

    then:
    buildResult.imageId =~ "[0-9a-z]{12}"

    cleanup:
    dockerClient.rmi(buildResult.imageId)
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
    def buildResult = dockerClient.build(
        newBuildContext(inputDirectory),
        new BuildConfig(query: [
            rm        : true,
            dockerfile: './Dockerfile.custom',
            buildargs : [the_arg: "custom-arg"]
        ]))

    then:
    def history = dockerClient.history(buildResult.imageId).content
    def mostRecentEntry = history.first()
    mostRecentEntry.CreatedBy.startsWith("|1 the_arg=custom-arg ")
    mostRecentEntry.CreatedBy.endsWith("'custom \${the_arg}'")

    cleanup:
    dockerClient.rmi(buildResult.imageId)
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
    BuildResult result = dockerClient.build(
        newBuildContext(inputDirectory),
        new BuildConfig(query: [rm: true],
                        callback: new DockerAsyncCallback() {

                          @Override
                          onEvent(Object event) {
                            if (event instanceof String) {
                              events << new JsonSlurper().parseText(event as String)
                            }
                            else {
                              events << event
                            }
                          }

                          @Override
                          onFinish() {
                            latch.countDown()
                          }
                        }))
    latch.await(5, SECONDS)

    then:
    if (isNativeWindows) {
      events.first().stream =~ "Step 1(/10)? : FROM microsoft/nanoserver\n"
    }
    else {
      events.first().stream =~ "Step 1(/10)? : FROM alpine:edge\n"
    }
    String imageId = events.last().stream.trim() - "Successfully built "

    cleanup:
    result?.response?.taskFuture?.cancel(true)
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
    }
    else {
      result.log.first().stream =~ "Step 1(/10)? : FROM alpine:edge\n"
    }
    result.log.last().stream.startsWith("Successfully built ")
    String imageId = result.log.last().stream.trim() - "Successfully built "
    result.imageId =~ "(sha256:)?$imageId"

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

    def toBeMatched = []
    if (isNativeWindows) {
      toBeMatched << new EventMatch(pattern: "Step 1(/2)? : FROM mcr.microsoft.com/windows/nanoserver:1809\n?")
    }
    else {
      toBeMatched << new EventMatch(pattern: "Step 1(/2)? : FROM alpine:edge\n?")
    }
    toBeMatched << new EventMatch(pattern: "\\s---> \\w+\n?")
    toBeMatched << new EventMatch(pattern: "Step 2(/2)? : RUN i-will-fail\n?")
    if (isNativeWindows) {
      toBeMatched << new EventMatch(matches: EventMatch.EXACT_MATCH, message: EventMatch.GET_ERROR, pattern: "The command 'cmd /S /C i-will-fail' returned a non-zero code: 1")
    }
    else {
      toBeMatched << new EventMatch(matches: EventMatch.EXACT_MATCH, message: EventMatch.GET_ERROR, pattern: "The command '/bin/sh -c i-will-fail' returned a non-zero code: 127")
    }

    when:
    CountDownLatch latch = new CountDownLatch(1)
    def events = []
    BuildResult result = dockerClient.build(
        newBuildContext(inputDirectory),
        new BuildConfig(query: [rm: true],
                        callback: new DockerAsyncCallback() {

                          @Override
                          onEvent(Object event) {
                            if (event instanceof String) {
                              events << new JsonSlurper().parseText(event as String)
                            }
                            else {
                              events << event
                            }
                          }

                          @Override
                          onFinish() {
                            latch.countDown()
                          }
                        }))
    latch.await(20, SECONDS)

    then:
    println "count: ${events.size()}"
    println events
    def nonMatched = filterNeedlesInSequence(events, toBeMatched)
    nonMatched.empty
    def containerId = getContainerId(events)
    def imageId = getImageId(events)

    cleanup:
    result.response.taskFuture.cancel(true)
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

  static class EventMatch {

    String pattern

    static Closure PATTERN_MATCH = { String msg, String pattern -> msg =~ pattern }
    static Closure EXACT_MATCH = { String msg, String pattern -> msg == pattern }

    Closure matches = PATTERN_MATCH

    static Closure GET_STREAM = { event -> event.stream }
    static Closure GET_ERROR = { event -> event.error }

    Closure message = GET_STREAM

    @Override
    String toString() {
      return pattern
    }
  }

  def filterNeedlesInSequence(List<Map> events, List<EventMatch> needles) {
    List<EventMatch> toBeMatched = new ArrayList<>(needles)
    events.each { Map event ->
      def expected = toBeMatched.first()
      if (expected.matches(expected.message(event), expected.pattern)) {
        toBeMatched.remove(expected)
      }
    }
    return toBeMatched
  }

  def findNeedleInSequence(List<Map> events, EventMatch needle) {
    return events.find { Map event ->
      if (needle.matches(needle.message(event), needle.pattern)) {
        return event
      }
    }
  }

  def getContainerId(List events) {
    def event = findNeedleInSequence(events, new EventMatch(pattern: "\\s---> Running in (\\w+)\n?"))
    return getFirstMatchingGroup("\\s---> Running in (\\w+)\n?", event.stream as String)
  }

  def getImageId(List events) {
    def event = findNeedleInSequence(events, new EventMatch(pattern: "\\s---> (\\w+)\n?"))
    return getFirstMatchingGroup("\\s---> (\\w+)\n?", event.stream as String)
  }

  def getFirstMatchingGroup(String pattern, String input) {
    def matcher = Pattern.compile(pattern).matcher(input)
    if (matcher.find()) {
      return matcher.group(1)
    }
    else {
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
    given:
    def digest = isNativeWindows ? "gesellix/testimage@sha256:afee42c0e4653763a5ee2d386afbd8f34ae80ba7644d31830e7aff826ebb1be0" : "gesellix/testimage@sha256:583ada96d700552c6c00d56aa581848264db1a8e2032d67bbaa761ff2d73ec2d"

    when:
    def imageId = dockerClient.pull(digest)

    then:
    imageId == (isNativeWindows ? "sha256:5880c6a3a386d67cd02b0ee4684709f9c966225270e97e0396157894ae74dbe6" : "sha256:0ce18ad10d281bef97fe2333a9bdcc2dbf84b5302f66d796fed73aac675320db")
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
    dockerClient.rmi("${registry.url()}/${CONSTANTS.imageRepo}:${CONSTANTS.imageTag}")
  }

  def "import image from url"() {
    given:
    def importUrl = getClass().getResource('importUrl/import-from-url.tar')
    def server = new HttpTestServer()
    def serverAddress = server.start('/images/', new HttpTestServer.FileServer(importUrl))
    def port = serverAddress.port
    def addresses = new NetworkInterfaces().getInet4Addresses()

    when:
    // not all interfaces addresses will be valid targets,
    // especially on a machine running a docker host
    String imageId = tryUntilSuccessful(addresses) { address ->
      String url = "http://${address}:$port/images/${importUrl.path}"
      dockerClient.importUrl(url, "import-from-url", "foo")
    }

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
    String imageId = dockerClient.importStream(archive, "import-from-url", "foo")

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
    imageById.Created == CONSTANTS.imageCreated
    imageById.ParentId == ""
    (imageById.RepoTags == null || imageById.RepoTags.contains(CONSTANTS.imageName))
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
    def imageId = dockerClient.build(newBuildContext(inputDirectory)).imageId
    log.info("buildResult: $imageId")

    when:
    Thread.sleep(500)

    then:
    def images = dockerClient.images([filters: [dangling: ["true"]]]).content
    println "images (1) ${images}"
    def found = images.findAll { image ->
      image.RepoTags == ["<none>:<none>"] || image.RepoTags == null
    }.find { image ->
      image.Id =~ imageId
    }
    def images2 = dockerClient.images([filters: [dangling: ["true"]]]).content
    println "images (2) ${images2}"
    found

    cleanup:
    dockerClient.rmi(imageId)
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
    ((List) searchResult.content).find {
      it.description == "A Testimage used for Docker Client integration tests.\n" &&
      it.is_automated == true &&
      it.is_official == false &&
//            it.is_trusted == true &&
      it.name == CONSTANTS.imageRepo &&
      it.star_count == 0
    }
  }

  InputStream newBuildContext(File baseDirectory) {
    def buildContext = File.createTempFile("buildContext", ".tar")
    buildContext.deleteOnExit()
    BuildContextBuilder.archiveTarFilesRecursively(baseDirectory, buildContext)
    return new FileInputStream(buildContext)
  }

  def tryUntilSuccessful(List<String> listOfClosureArgsToTry, Closure oneArgClosure) {
    def retVal = null
    for (arg in listOfClosureArgsToTry) {
      try {
        retVal = oneArgClosure(arg)
        break
      }
      catch (Exception ignored) {
        /* ignore here; already logged by DockerResponseHandler */
      }
    }

    retVal
  }
}
