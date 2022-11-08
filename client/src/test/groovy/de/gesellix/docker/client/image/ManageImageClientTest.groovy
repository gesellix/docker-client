package de.gesellix.docker.client.image

import de.gesellix.docker.authentication.AuthConfig
import de.gesellix.docker.client.authentication.ManageAuthentication
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.ImageInspect
import de.gesellix.docker.remote.api.ImagePruneResponse
import de.gesellix.docker.remote.api.ImageSummary
import de.gesellix.docker.remote.api.client.ImageApi
import spock.lang.Specification

class ManageImageClientTest extends Specification {

  ManageImageClient service
  EngineApiClient client = Mock(EngineApiClient)
  ManageAuthentication manageAuthentication = Mock(ManageAuthentication)

  def setup() {
    service = Spy(ManageImageClient, constructorArgs: [
        client,
        manageAuthentication])
  }

  def "search"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    def images = Mock(List)

    when:
    def imageSearch = service.search("ubuntu", 22)

    then:
    client.imageApi.imageSearch("ubuntu", 22, null) >> images
    imageSearch.content == images
  }

  def "build with defaults"() {
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    def buildContext = new ByteArrayInputStream([42] as byte[])
    def authConfigs = ["for-test": new AuthConfig(username: "foo")]

    when:
    service.build(buildContext)

    then:
    1 * manageAuthentication.getAllAuthConfigs() >> authConfigs
    1 * manageAuthentication.encodeAuthConfigs(authConfigs) >> "base-64-encoded"
    1 * imageApi.imageBuild(null, null, null, null, null, null, null, null, true, null,
                            null, null, null, null, null, null, null, null, null, null, null,
                            ImageApi.ContentTypeImageBuild.ApplicationSlashXMinusTar, "base-64-encoded", null, null, null, buildContext,
                            null, null)
  }

  def "tag with defaults"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi

    when:
    service.tag("an-image", "registry:port/username/image-name:a-tag")

    then:
    1 * imageApi.imageTag("an-image", "registry:port/username/image-name", "a-tag")
  }

  def "push with defaults"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi

    when:
    service.push("an-image")

    then:
    1 * imageApi.imagePush("an-image", ".", "", *_)
  }

  def "push with auth"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi

    when:
    service.push("an-image:a-tag", "some-base64-encoded-auth")

    then:
    1 * imageApi.imagePush("an-image", "some-base64-encoded-auth", "a-tag", *_)
  }

  def "push with registry"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi

    when:
    service.push("an-image", ".", "registry:port")

    then:
    1 * imageApi.imageTag("an-image", "registry:port/an-image", "")
    then:
    1 * imageApi.imagePush("registry:port/an-image", ".", "", *_)
  }

  def "pull with defaults"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    service.images([:]) >> [content: [:]]

    when:
    service.pull(null, null, "an-image")

    then:
    1 * imageApi.imageCreate("an-image", null, null, "", null, ".", null, null, null, null, null)
  }

  def "pull with tag"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    service.images([:]) >> [content: [:]]

    when:
    service.pull(null, null, "an-image", "a-tag")

    then:
    1 * imageApi.imageCreate("an-image", null, null, "a-tag", null, ".", null, null, null, null, null)
  }

  def "pull with auth"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    service.images([:]) >> [content: [:]]

    when:
    service.pull(null, null, "registry:port/an-image", "", "some-base64-encoded-auth")

    then:
    1 * imageApi.imageCreate("registry:port/an-image", null, null, "", null, "some-base64-encoded-auth", null, null, null, null, null)
  }

  def "import from url"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    def importUrl = getClass().getResource('importUrl/import-from-url.tar')

    when:
    service.importUrl(null, null,
                      importUrl.toString(), "imported-from-url", "foo")

    then:
    1 * imageApi.imageCreate(null,
                             importUrl.toString(), "imported-from-url", "foo",
                             null, null, null, null,
                             null,
                             null, null)
  }

  def "import from stream"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    def archive = getClass().getResourceAsStream('importUrl/import-from-url.tar')

    when:
    service.importStream(null, null,
                         archive, "imported-from-stream", "foo")

    then:
    1 * imageApi.imageCreate(null,
                             "-", "imported-from-stream", "foo",
                             null, null, null, null,
                             archive,
                             null, null)
  }

  def "save one repository"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    def archive = Mock(InputStream)

    when:
    def response = service.save(["image:tag"])

    then:
    1 * imageApi.imageGetAll(["image:tag"]) >> archive
    and:
    response.content == archive
  }

  def "save multiple repositories"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    def archive = Mock(InputStream)

    when:
    def response = service.save(["image:tag1", "an-id"])

    then:
    1 * imageApi.imageGetAll(["image:tag1", "an-id"]) >> archive
    and:
    response.content == archive
  }

  def "load"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    def archive = Mock(InputStream)

    when:
    service.load(archive)

    then:
    1 * imageApi.imageLoad(null, archive)
  }

  def "inspect image"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    def image = Mock(ImageInspect)

    when:
    def inspectImage = service.inspectImage("an-image")

    then:
    1 * imageApi.imageInspect("an-image") >> image
    inspectImage.content == image
  }

  def "history"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    def historyItems = Mock(List)

    when:
    def history = service.history("an-image")

    then:
    1 * imageApi.imageHistory("an-image") >> historyItems
    history.content == historyItems
  }

  def "images with defaults"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    def images = Mock(List)

    when:
    def responseContent = service.images()

    then:
    1 * imageApi.imageList(false, null, null) >> images
    responseContent.content == images
  }

  def "images with filters"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    def filters = '{"dangling":["true"]}'

    when:
    service.images(true, filters)

    then:
    1 * imageApi.imageList(true, filters, null)
  }

  def "findImageId by image name"() {
    given:
    def imageSummary = Mock(ImageSummary)
    imageSummary.id >> 'the-id'
    imageSummary.repoTags >> ['anImage:latest']
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    imageApi.imageList(*_) >> [imageSummary]

    expect:
    service.findImageId('anImage') == 'the-id'
  }

  def "findImageId with missing image"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    imageApi.imageList(*_) >> []

    expect:
    service.findImageId('anImage') == 'anImage:latest'
  }

  def "findImageId by digest"() {
    given:
    def imageSummary = Mock(ImageSummary)
    imageSummary.id >> 'the-id'
    imageSummary.repoDigests >> ['anImage@sha256:4711']
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    imageApi.imageList(*_) >> [imageSummary]

    expect:
    service.findImageId('anImage@sha256:4711') == 'the-id'
  }

  def "rmi image"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    def deleteResponse = Mock(List)

    when:
    def responseContent = service.rmi("an-image")

    then:
    1 * imageApi.imageDelete("an-image", null, null) >> deleteResponse
    responseContent.content == deleteResponse
  }

  def "pruneImages"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    def filters = '{"dangling":true}'
    def pruneResponse = Mock(ImagePruneResponse)

    when:
    def pruneImages = service.pruneImages(filters)

    then:
    1 * imageApi.imagePrune(filters) >> pruneResponse
    pruneImages.content == pruneResponse
  }
}
