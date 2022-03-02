package de.gesellix.docker.client

import com.squareup.moshi.Moshi
import de.gesellix.docker.builder.BuildContextBuilder
import de.gesellix.docker.client.testutil.ManifestUtil
import de.gesellix.docker.client.testutil.TarUtil
import de.gesellix.docker.registry.DockerRegistry
import de.gesellix.docker.remote.api.BuildInfo
import de.gesellix.docker.remote.api.ContainerCreateRequest
import de.gesellix.docker.remote.api.CreateImageInfo
import de.gesellix.docker.remote.api.ImageSearchResponseItem
import de.gesellix.docker.remote.api.PushImageInfo
import de.gesellix.docker.remote.api.client.BuildInfoExtensionsKt
import de.gesellix.docker.remote.api.client.CreateImageInfoExtensionsKt
import de.gesellix.docker.remote.api.core.ClientException
import de.gesellix.docker.remote.api.core.StreamCallback
import de.gesellix.docker.testutil.HttpTestServer
import de.gesellix.docker.testutil.NetworkInterfaces
import de.gesellix.testutil.ResourceReader
import groovy.util.logging.Slf4j
import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Specification

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static de.gesellix.docker.client.TestConstants.CONSTANTS

@Slf4j
@Requires({ LocalDocker.available() })
class DockerImageIntegrationSpec extends Specification {

  static DockerRegistry registry

  static DockerClient dockerClient
  boolean isNativeWindows = LocalDocker.isNativeWindows()

  def setupSpec() {
    dockerClient = new DockerClientImpl()
    registry = new DockerRegistry()
    registry.run()
  }

  def cleanupSpec() {
    registry.rm()
  }

  def ping() {
    expect:
    "OK" == dockerClient.ping().content
  }

  def "build image"() {
    given:
    def dockerfile
    if (isNativeWindows) {
      dockerfile = "build/build-windows/Dockerfile"
    }
    else {
      dockerfile = "build/build/Dockerfile"
    }
    def inputDirectory = new ResourceReader().getClasspathResourceAsFile(dockerfile, DockerClient).parentFile

    List<BuildInfo> infos = []
    def latch = new CountDownLatch(1)
    def callback = new StreamCallback<BuildInfo>() {

      @Override
      void onNext(BuildInfo element) {
        log.info(element?.toString())
        infos.add(element)
      }

      @Override
      void onFailed(Exception e) {
        log.error("Build failed", e)
        latch.countDown()
      }

      @Override
      void onFinished() {
        latch.countDown()
      }
    }

    when:
    dockerClient.build(
        callback, Duration.of(1, ChronoUnit.MINUTES),
        newBuildContext(inputDirectory))
    latch.await(2, TimeUnit.MINUTES)

    then:
    def imageId = BuildInfoExtensionsKt.getImageId(infos)
    imageId?.ID =~ "[0-9a-z]{12}"

    cleanup:
    if (imageId?.ID) {
      dockerClient.rmi(imageId.getID())
    }
  }

  def "build image with tag"() {
    given:
    def dockerfile
    if (isNativeWindows) {
      dockerfile = "build/build-windows/Dockerfile"
    }
    else {
      dockerfile = "build/build/Dockerfile"
    }
    def inputDirectory = new ResourceReader().getClasspathResourceAsFile(dockerfile, DockerClient).parentFile

    List<BuildInfo> infos = []
    def latch = new CountDownLatch(1)
    def callback = new StreamCallback<BuildInfo>() {

      @Override
      void onNext(BuildInfo element) {
        log.info(element?.toString())
        infos.add(element)
      }

      @Override
      void onFailed(Exception e) {
        log.error("Build failed", e)
        latch.countDown()
      }

      @Override
      void onFinished() {
        latch.countDown()
      }
    }

    when:
    dockerClient.build(
        callback, Duration.of(1, ChronoUnit.MINUTES),
        "docker-client/tests:tag",
        newBuildContext(inputDirectory))
    latch.await(2, TimeUnit.MINUTES)

    then:
    def imageId = BuildInfoExtensionsKt.getImageId(infos)
    imageId?.ID =~ "[0-9a-z]{12}"

    cleanup:
    if (imageId?.ID) {
      dockerClient.rmi(imageId.getID())
    }
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

    List<BuildInfo> infos = []
    def latch = new CountDownLatch(1)
    def callback = new StreamCallback<BuildInfo>() {

      @Override
      void onNext(BuildInfo element) {
        log.info(element?.toString())
        infos.add(element)
      }

      @Override
      void onFailed(Exception e) {
        log.error("Build failed", e)
        latch.countDown()
      }

      @Override
      void onFinished() {
        latch.countDown()
      }
    }

    when:
    dockerClient.build(callback, Duration.of(1, ChronoUnit.MINUTES), newBuildContext(buildContextDir))
    latch.await(2, TimeUnit.MINUTES)

    then:
    notThrown(Exception)
    infos.last().error.contains(" missing/image")
  }

  def "build image with custom Dockerfile"() {
    given:
    def dockerfile
    if (isNativeWindows) {
      dockerfile = "build/custom-windows/Dockerfile"
    }
    else {
      dockerfile = "build/custom/Dockerfile"
    }
    def inputDirectory = new ResourceReader().getClasspathResourceAsFile(dockerfile, DockerClient).parentFile
    def moshi = new Moshi.Builder().build()

    List<BuildInfo> infos = []
    def latch = new CountDownLatch(1)
    def callback = new StreamCallback<BuildInfo>() {

      @Override
      void onNext(BuildInfo element) {
        log.info(element?.toString())
        infos.add(element)
      }

      @Override
      void onFailed(Exception e) {
        log.error("Build failed", e)
        latch.countDown()
      }

      @Override
      void onFinished() {
        latch.countDown()
      }
    }

    when:
    dockerClient.build(
        callback, Duration.of(1, ChronoUnit.MINUTES),
        './Dockerfile.custom',
        null, null, null, null, true,
        moshi.adapter(Map).toJson([the_arg: "custom-arg"]),
        null, null, null, newBuildContext(inputDirectory))
    latch.await(2, TimeUnit.MINUTES)

    then:
    def imageId = BuildInfoExtensionsKt.getImageId(infos)
    def history = dockerClient.history(imageId.ID).content
    def mostRecentEntry = history.first()
    mostRecentEntry.createdBy.startsWith("|1 the_arg=custom-arg ")
    mostRecentEntry.createdBy.endsWith("'custom \${the_arg}'")

    cleanup:
    dockerClient.rmi(imageId.ID)
  }

  def "tag image"() {
    given:
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)
    def imageName = "yet-another-tag"

    when:
    dockerClient.tag(CONSTANTS.imageName, imageName)

    then:
    notThrown(Exception)

    cleanup:
    dockerClient.rmi(imageName)
  }

  @Ignore
  void "push image (registry api v2)"() {
    given:
    def authDetails = dockerClient.readAuthConfig(null, null)
    def authBase64Encoded = dockerClient.encodeAuthConfig(authDetails)
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)
    def imageName = "gesellix/test:latest"
    dockerClient.tag(CONSTANTS.imageName, imageName)

    List<PushImageInfo> infos = []
    def timeout = Duration.of(1, ChronoUnit.MINUTES)
    def latch = new CountDownLatch(1)
    def callback = new StreamCallback<PushImageInfo>() {

      @Override
      void onNext(PushImageInfo element) {
        log.info(element?.toString())
        infos.add(element)
      }

      @Override
      void onFailed(Exception e) {
        log.error("push failed", e)
        latch.countDown()
      }

      @Override
      void onFinished() {
        latch.countDown()
      }
    }

    when:
    new Thread({
      dockerClient.push(callback, timeout, imageName, authBase64Encoded)
    }).start()
    latch.await(2, TimeUnit.MINUTES)

    then:
    !infos.empty
    infos.find { it.status.contains("digest") || it.status.contains("aux") }.status =~ "sha256:\\w+"
//    pushResult.content.last().aux.Digest =~ "sha256:\\w+"

    cleanup:
    dockerClient.rmi(imageName)
  }

  @Ignore
  void "push image with registry (registry api v2)"() {
    given:
    def authDetails = dockerClient.readDefaultAuthConfig()
    def authBase64Encoded = dockerClient.encodeAuthConfig(authDetails)
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)
    def imageName = "gesellix/test:latest"
    dockerClient.tag(CONSTANTS.imageName, imageName)

    List<PushImageInfo> infos = []
    def timeout = Duration.of(1, ChronoUnit.MINUTES)
    def latch = new CountDownLatch(1)
    def callback = new StreamCallback<PushImageInfo>() {

      @Override
      void onNext(PushImageInfo element) {
        log.info(element?.toString())
        infos.add(element)
      }

      @Override
      void onFailed(Exception e) {
        log.error("push failed", e)
        latch.countDown()
      }

      @Override
      void onFinished() {
        latch.countDown()
      }
    }

    when:
    new Thread({
      dockerClient.push(callback, timeout, imageName, authBase64Encoded, registry.url())
    }).start()
    latch.await(2, TimeUnit.MINUTES)

    then:
    !infos.empty
    infos.find { it.status.contains("digest") || it.status.contains("aux") }.status =~ "sha256:\\w+"
//    pushResult.content.last().aux.Digest =~ "sha256:\\w+"

    cleanup:
    dockerClient.rmi(imageName)
    dockerClient.rmi("${registry.url()}/${imageName}")
  }

  def "push image with undefined authentication"() {
    given:
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)
    def imageName = "gesellix/test:latest"
    dockerClient.tag(CONSTANTS.imageName, imageName)

    List<PushImageInfo> infos = []
    def timeout = Duration.of(1, ChronoUnit.MINUTES)
    def latch = new CountDownLatch(1)
    def callback = new StreamCallback<PushImageInfo>() {

      @Override
      void onNext(PushImageInfo element) {
        log.info(element?.toString())
        infos.add(element)
      }

      @Override
      void onFailed(Exception e) {
        log.error("push failed", e)
        latch.countDown()
      }

      @Override
      void onFinished() {
        latch.countDown()
      }
    }

    when:
    new Thread({
      dockerClient.push(callback, timeout, imageName, null, registry.url())
    }).start()
    latch.await(2, TimeUnit.MINUTES)

    then:
    !infos.empty
    infos.find { it.status.contains("digest") || it.status.contains("aux") }.status =~ "sha256:\\w+"
//    pushResult.content.last().aux.Digest =~ "sha256:\\w+"

    cleanup:
    dockerClient.rmi(imageName)
    dockerClient.rmi("${registry.url()}/${imageName}")
  }

  def "pull image"() {
    when:
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)

    then:
    notThrown(Exception)
//    imageId == CONSTANTS.imageDigest
  }

  def "pull image by digest"() {
    given:
    String digest = isNativeWindows ? "gesellix/echo-server@sha256:5521f01c05a79bdae5570955c853ec51474bad3f3f9f6ecf2047414becf4afd2" : "gesellix/echo-server@sha256:04c0275878dc243b2f92193467cb33cdb9ee2262be64b627ed476de73e399244"

    when:
    dockerClient.pull(null, null, digest)

    then:
    notThrown(Exception)
//    imageId == CONSTANTS.imageDigest
  }

  def "pull image from private registry"() {
    given:
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)
    dockerClient.push(CONSTANTS.imageName, "", registry.url())

    when:
    dockerClient.pull(null, null, "${registry.url()}/${CONSTANTS.imageRepo}", CONSTANTS.imageTag, "")

    then:
    notThrown(Exception)
//    imageId == CONSTANTS.imageDigest

    cleanup:
    dockerClient.rmi("${registry.url()}/${CONSTANTS.imageRepo}:${CONSTANTS.imageTag}")
  }

  def "import image from url"() {
    given:
    InputStream savedImage = dockerClient.save([CONSTANTS.imageName]).content
    File destDir = new TarUtil().unTar(savedImage)
    File rootLayerTar = new ManifestUtil().getRootLayerLocation(destDir)
    URL importUrl = rootLayerTar.toURI().toURL()
    def server = new HttpTestServer()
    def serverAddress = server.start('/images/', new HttpTestServer.FileServer(importUrl))
    def port = serverAddress.port
    def addresses = new NetworkInterfaces().getInet4Addresses()

    List<CreateImageInfo> infos = []
    def latch = new CountDownLatch(1)
    def callback = new StreamCallback<CreateImageInfo>() {

      @Override
      void onNext(CreateImageInfo element) {
        log.info(element.toString())
        infos.add(element)
      }

      @Override
      void onFailed(Exception e) {
        log.error("import from stream failed", e)
        latch.countDown()
      }

      @Override
      void onFinished() {
        latch.countDown()
      }
    }

    when:
    // not all interfaces addresses will be valid targets,
    // especially on a machine running a docker host
    tryUntilSuccessful(addresses) { address ->
      String url = "http://${address}:$port/images/${importUrl.path}"
      dockerClient.importUrl(callback, Duration.of(1, ChronoUnit.MINUTES), url, "import-from-url", "foo")
    }
    latch.await(2, TimeUnit.MINUTES)

    then:
    def imageId = CreateImageInfoExtensionsKt.getImageId(infos)
    imageId?.matches(/[\w:]+/)

    cleanup:
    server.stop()
    dockerClient.rmi(imageId)
  }

  def "import image from stream"() {
    given:
    InputStream savedImage = dockerClient.save([CONSTANTS.imageName]).content
    File destDir = new TarUtil().unTar(savedImage)
    File rootLayerTar = new ManifestUtil().getRootLayerLocation(destDir)
    def archive = new FileInputStream(rootLayerTar)

    List<CreateImageInfo> infos = []
    def latch = new CountDownLatch(1)
    def callback = new StreamCallback<CreateImageInfo>() {

      @Override
      void onNext(CreateImageInfo element) {
        log.info(element.toString())
        infos.add(element)
      }

      @Override
      void onFailed(Exception e) {
        log.error("import from stream failed", e)
        latch.countDown()
      }

      @Override
      void onFinished() {
        latch.countDown()
      }
    }

    when:
    dockerClient.importStream(
        callback, Duration.of(1, ChronoUnit.MINUTES),
        archive, "import-from-url", "foo")
    latch.await(2, TimeUnit.MINUTES)

    then:
    def imageId = CreateImageInfoExtensionsKt.getImageId(infos)
    imageId?.matches(/[\w:]+/)

    cleanup:
    dockerClient.rmi(imageId)
  }

  def "inspect image"() {
    given:
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)

    when:
    def imageInspection = dockerClient.inspectImage(CONSTANTS.imageName).content

    then:
    imageInspection.repoDigests.find { it.startsWith(CONSTANTS.imageRepo) }
  }

  def "history"() {
    given:
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)

    when:
    def history = dockerClient.history(CONSTANTS.imageName).content

    then:
    List<String> imageIds = history.collect { it.id }
    imageIds.first() == CONSTANTS.imageDigest
    imageIds.last() =~ ".+"
  }

  def "list images"() {
    given:
    dockerClient.pull(null, null, CONSTANTS.imageName)

    when:
    def images = dockerClient.images().content

    then:
    def imageById = images.find {
      it.id == CONSTANTS.imageDigest
    }
    imageById?.created == CONSTANTS.imageCreated
    imageById?.parentId =~ ".*"
    (imageById?.repoTags == null || imageById.repoTags.contains(CONSTANTS.imageName))
  }

  def "list images with intermediate layers"() {
    given:
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)
    def container1Info = dockerClient.createContainer(new ContainerCreateRequest().tap { image = CONSTANTS.imageName }).content
    dockerClient.commit(container1Info.id, [repo: 'repo1', tag: 'tag1'])
    dockerClient.rm(container1Info.id)
    def container2Info = dockerClient.createContainer(new ContainerCreateRequest().tap { image = "repo1:tag1" }).content
    dockerClient.commit(container2Info.id, [repo: 'repo2', tag: 'tag2'])
    dockerClient.rm(container2Info.id)
    dockerClient.rmi("repo1:tag1")

    when:
    def images = dockerClient.images([:]).content
    def fullImages = dockerClient.images([all: true]).content

    then:
    def imageIds = images.collect { image -> image.id }
    def fullImageIds = fullImages.collect { image -> image.id }
    imageIds != fullImageIds
    and:
    fullImageIds.size() > imageIds.size()

    cleanup:
    dockerClient.rmi("repo2:tag2")
  }

  def "list images filtered"() {
    given:
    def dockerfile
    if (isNativeWindows) {
      dockerfile = "build/build-windows/Dockerfile"
    }
    else {
      dockerfile = "build/build/Dockerfile"
    }
    def inputDirectory = new ResourceReader().getClasspathResourceAsFile(dockerfile, DockerClient).parentFile

    List<BuildInfo> infos = []
    def latch = new CountDownLatch(1)
    def callback = new StreamCallback<BuildInfo>() {

      @Override
      void onNext(BuildInfo element) {
        log.info(element?.toString())
        infos.add(element)
      }

      @Override
      void onFailed(Exception e) {
        log.error("Build failed", e)
        latch.countDown()
      }

      @Override
      void onFinished() {
        latch.countDown()
      }
    }
    dockerClient.build(callback, Duration.of(1, ChronoUnit.MINUTES), newBuildContext(inputDirectory))

    when:
    Thread.sleep(500)
    latch.await(2, TimeUnit.MINUTES)
    def imageId = BuildInfoExtensionsKt.getImageId(infos)

    then:
    def images = dockerClient.images([filters: [dangling: ["true"]]]).content
    println "images (1) ${images}"
    def found = images.findAll { image ->
      image.repoTags == ["<none>:<none>"] || image.repoTags == null
    }.find { image ->
      image.id =~ imageId.ID
    }
    found

    cleanup:
    dockerClient.rmi(imageId.ID)
  }

  def "rm image"() {
    given:
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)
    dockerClient.tag(CONSTANTS.imageName, "an_image_to_be_deleted")

    when:
    def rmImageResult = dockerClient.rmi("an_image_to_be_deleted")

    then:
    !rmImageResult.content.empty
  }

  def "rm unknown image"() {
    when:
    def response = dockerClient.rmi("an_unknown_image")

    then:
    notThrown(ClientException)
    response.content == []
  }

  def "rm image with existing container"() {
    given:
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)
    dockerClient.tag(CONSTANTS.imageName, "an_image_with_existing_container")

    def name = "another-example-name"
    def containerConfig = new ContainerCreateRequest().tap { c ->
      c.image = "an_image_with_existing_container:latest"
    }
    dockerClient.run(containerConfig, name)

    when:
    def rmImageResult = dockerClient.rmi("an_image_with_existing_container:latest")

    then:
    !rmImageResult.content.empty

    cleanup:
    dockerClient.stop(name)
    dockerClient.wait(name)
    dockerClient.rm(name)
  }

  def "search"() {
    when:
    def searchResult = dockerClient.search("echo-server", 100)

    then:
    ((List<ImageSearchResponseItem>) searchResult.content)
        .findAll { it.name.contains("gesellix") }
        .find {
          it.description == "A Testimage used for Docker Client integration tests." &&
          it.automated == false &&
          it.official == false &&
//            it.is_trusted == true &&
          it.name == CONSTANTS.imageRepo &&
          it.starCount == 0
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
