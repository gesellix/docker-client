package de.gesellix.docker.client

import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient
import spock.lang.Specification
import spock.lang.Unroll

class DockerClientImplSpec extends Specification {

  def DockerClientImpl dockerClient = Spy(DockerClientImpl)
  def RESTClient delegateMock = Mock(RESTClient)

  def setup() {
    dockerClient.createDockerClient(_) >> delegateMock
    dockerClient.responseHandler = new Object() {

      def success = true
      def lastResponseDetail = []
      def responseChunks = []
      def statusLine = [:]
    }
  }

  def encodeAuthConfig() {
    given:
    def authDetails = ["username"     : "gesellix",
                       "password"     : "-yet-another-password-",
                       "email"        : "tobias@gesellix.de",
                       "serveraddress": "https://index.docker.io/v1/"]
    def authPlain = authDetails

    when:
    def authResult = dockerClient.encodeAuthConfig(authPlain)

    then:
    authResult == 'eyJ1c2VybmFtZSI6Imdlc2VsbGl4IiwicGFzc3dvcmQiOiIteWV0LWFub3RoZXItcGFzc3dvcmQtIiwiZW1haWwiOiJ0b2JpYXNAZ2VzZWxsaXguZGUiLCJzZXJ2ZXJhZGRyZXNzIjoiaHR0cHM6Ly9pbmRleC5kb2NrZXIuaW8vdjEvIn0='
  }

  @Unroll
  def "parse repository tag #name to repo '#repo' and tag '#tag'"() {
    when:
    def result = dockerClient.parseRepositoryTag(name)

    then:
    result.repo == repo
    and:
    result.tag == tag

    where:
    name                      || repo | tag
    "scratch"                 || "scratch" | ""
    "root:tag"                || "root" | "tag"
    "user/repo"               || "user/repo" | ""
    "user/repo:tag"           || "user/repo" | "tag"
    "url:5000/repo"           || "url:5000/repo" | ""
    "url:5000/repo:tag"       || "url:5000/repo" | "tag"
    "url:5000/user/image:tag" || "url:5000/user/image" | "tag"
  }

  def "info"() {
    when:
    dockerClient.info()

    then:
    1 * delegateMock.get([path: "/info"], _ as Closure)
  }

  def "version"() {
    when:
    dockerClient.version()

    then:
    1 * delegateMock.get([path: "/version"], _ as Closure)
  }

  def "auth"() {
    def authDetails = [:]
    when:
    dockerClient.auth(authDetails)

    then:
    1 * delegateMock.post([path              : "/auth",
                           body              : authDetails,
                           requestContentType: ContentType.JSON], _ as Closure)
  }

  def "build with defaults"() {
    def buildContext = new ByteArrayInputStream([42] as byte[])
    given:
    dockerClient.responseHandler.lastResponseDetail = [error : false,
                                                       stream: ""]

    when:
    dockerClient.build(buildContext)

    then:
    1 * delegateMock.post([path              : "/build",
                           query             : ["rm": true],
                           body              : [42],
                           requestContentType: ContentType.BINARY])
  }

  def "build with query"() {
    def buildContext = new ByteArrayInputStream([42] as byte[])
    def query = ["rm": false]
    given:
    dockerClient.responseHandler.lastResponseDetail = [error : false,
                                                       stream: ""]

    when:
    dockerClient.build(buildContext, query)

    then:
    1 * delegateMock.post([path              : "/build",
                           query             : ["rm": false],
                           body              : [42],
                           requestContentType: ContentType.BINARY])
  }

  def "tag with defaults"() {
    when:
    dockerClient.tag("an-image", "registry:port/username/image-name:a-tag")

    then:
    1 * delegateMock.post([path : "/images/an-image/tag",
                           query: [repo : "registry:port/username/image-name",
                                   tag  : "a-tag",
                                   force: false]], _ as Closure)
  }

  def "tag with force == true"() {
    when:
    dockerClient.tag("an-image", "registry:port/username/image-name:a-tag", true)

    then:
    1 * delegateMock.post([path : "/images/an-image/tag",
                           query: [repo : "registry:port/username/image-name",
                                   tag  : "a-tag",
                                   force: true]], _ as Closure)
  }

  def "push with defaults"() {
    when:
    dockerClient.push("an-image")

    then:
    1 * delegateMock.post([path   : "/images/an-image/push",
                           query  : [registry: "",
                                     tag     : ""],
                           headers: ["X-Registry-Auth": "."]])
  }

  def "push with auth"() {
    when:
    dockerClient.push("an-image:a-tag", "some-base64-encoded-auth")

    then:
    1 * delegateMock.post([path   : "/images/an-image/push",
                           query  : [registry: "",
                                     tag     : "a-tag"],
                           headers: ["X-Registry-Auth": "some-base64-encoded-auth"]])
  }

  def "push with registry"() {
    when:
    dockerClient.push("an-image", ".", "registry:port")

    then:
    1 * delegateMock.post([path : "/images/an-image/tag",
                           query: [repo : "registry:port/an-image",
                                   tag  : "",
                                   force: true]], _ as Closure)
    then:
    1 * delegateMock.post([path   : "/images/registry:port/an-image/push",
                           query  : [registry: "registry:port",
                                     tag     : ""],
                           headers: ["X-Registry-Auth": "."]])
  }

  def "pull with defaults"() {
    given:
    dockerClient.responseHandler.responseChunks = [[id: "image-id"]]

    when:
    dockerClient.pull("an-image")

    then:
    1 * delegateMock.post([path : "/images/create",
                           query: [fromImage: "an-image",
                                   tag      : "",
                                   registry : ""]])
  }

  def "pull with tag"() {
    given:
    dockerClient.responseHandler.responseChunks = [[id: "image-id"]]

    when:
    dockerClient.pull("an-image", "a-tag")

    then:
    1 * delegateMock.post([path : "/images/create",
                           query: [fromImage: "an-image",
                                   tag      : "a-tag",
                                   registry : ""]])
  }

  def "pull with registry"() {
    given:
    dockerClient.responseHandler.responseChunks = [[id: "image-id"]]

    when:
    dockerClient.pull("an-image", "", "registry:port")

    then:
    1 * delegateMock.post([path : "/images/create",
                           query: [fromImage: "registry:port/an-image",
                                   tag      : "",
                                   registry : "registry:port"]])
  }

  def "stop container"() {
    when:
    dockerClient.stop("a-container")

    then:
    1 * delegateMock.post([path: "/containers/a-container/stop"], _ as Closure)
  }

  def "wait container"() {
    when:
    dockerClient.wait("a-container")

    then:
    1 * delegateMock.post([path: "/containers/a-container/wait"], _ as Closure)
  }

  def "rm container"() {
    when:
    dockerClient.rm("a-container")

    then:
    1 * delegateMock.delete([path: "/containers/a-container"]) >> [statusLine: [:]]
  }

  def "ps containers"() {
    when:
    dockerClient.ps()

    then:
    1 * delegateMock.get([path : "/containers/json",
                          query: [all : true,
                                  size: false]], _ as Closure)
  }

  def "inspect container"() {
    when:
    dockerClient.inspectContainer("a-container")

    then:
    1 * delegateMock.get([path: "/containers/a-container/json"], _ as Closure)
  }

  def "images with defaults"() {
    when:
    dockerClient.images()

    then:
    1 * delegateMock.get([path : "/images/json",
                          query: [all    : false,
                                  filters: [:]]], _ as Closure)
  }

  def "images with query"() {
    def query = [all    : true,
                 filters: ["dangling": true]]
    when:
    dockerClient.images(query)

    then:
    1 * delegateMock.get([path : "/images/json",
                          query: query], _ as Closure)
  }

  def "rmi image"() {
    when:
    dockerClient.rmi("an-image")

    then:
    1 * delegateMock.delete([path: "/images/an-image"]) >> [statusLine: [:]]
  }

  def "create exec"() {
    def execCreateConfig = [:]
    when:
    dockerClient.createExec("a-container", execCreateConfig)

    then:
    1 * delegateMock.post([path              : "/containers/a-container/exec",
                           body              : execCreateConfig,
                           requestContentType: ContentType.JSON])
  }

  def "start exec"() {
    def execStartConfig = [:]
    when:
    dockerClient.startExec("an-exec", execStartConfig)

    then:
    1 * delegateMock.post([path              : "/exec/an-exec/start",
                           body              : execStartConfig,
                           requestContentType: ContentType.JSON])
  }

  def "create container with defaults"() {
    def containerConfig = [Cmd: "true"]
    when:
    dockerClient.createContainer(containerConfig)

    then:
    1 * delegateMock.post([path              : "/containers/create",
                           query             : [name: ""],
                           body              : containerConfig,
                           requestContentType: ContentType.JSON])
  }

  def "create container with query"() {
    def containerConfig = [Cmd: "true"]
    def query = [name: "foo"]
    when:
    dockerClient.createContainer(containerConfig, query)

    then:
    1 * delegateMock.post([path              : "/containers/create",
                           query             : query,
                           body              : containerConfig,
                           requestContentType: ContentType.JSON])
  }

  def "start container"() {
    when:
    dockerClient.startContainer("a-container")

    then:
    1 * delegateMock.post([path              : "/containers/a-container/start",
                           body              : [:],
                           requestContentType: ContentType.JSON], _ as Closure)
  }

  def "run container with defaults"() {
    when:
    dockerClient.run("an-image", [:], [:])

    then:
    1 * dockerClient.createContainer(["Image": "an-image"], [name: ""]) >> [Id: "container-id"]

    then:
    1 * dockerClient.startContainer("container-id", [:])
  }
}
