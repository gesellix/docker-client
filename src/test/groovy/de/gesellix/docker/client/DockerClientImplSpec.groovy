package de.gesellix.docker.client

import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient
import org.apache.http.StatusLine
import org.slf4j.Logger
import spock.lang.Specification
import spock.lang.Unroll

class DockerClientImplSpec extends Specification {

  def DockerClientImpl dockerClient = Spy(DockerClientImpl)
  def RESTClient delegateMock = Mock(RESTClient)

  def setup() {
    dockerClient.createDockerClient(_) >> delegateMock
    dockerClient.responseHandler = Spy(DockerResponseHandler)
    dockerClient.responseHandler.chunks = []
  }

  def "encode authConfig"() {
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

  def "shouldn't allow repository tag ending with a ':'"() {
    when:
    dockerClient.parseRepositoryTag("scratch:")

    then:
    def exc = thrown(DockerClientException)
    exc.cause.message == "'scratch:' should not end with a ':'"
  }

  def "ping"() {
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [plain: "OK"]

    when:
    dockerClient.ping()

    then:
    1 * delegateMock.get([path: "/_ping"])
  }

  def "info"() {
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [:]

    when:
    dockerClient.info()

    then:
    1 * delegateMock.get([path: "/info"])
  }

  def "version"() {
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [:]

    when:
    dockerClient.version()

    then:
    1 * delegateMock.get([path: "/version"])
  }

  def "login"() {
    def authDetails = [:]
    given:
    dockerClient.responseHandler.statusLine >> Mock(StatusLine)

    when:
    dockerClient.auth(authDetails)

    then:
    1 * delegateMock.post([path              : "/auth",
                           body              : authDetails,
                           requestContentType: ContentType.JSON])
  }

  def "build with defaults"() {
    def buildContext = new ByteArrayInputStream([42] as byte[])
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [error : false,
                                            stream: ""]

    when:
    dockerClient.build(buildContext)

    then:
    1 * delegateMock.post([path              : "/build",
                           query             : ["rm": true],
                           body              : [42],
                           requestContentType: ContentType.BINARY])
    and:
    dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
      assert arguments[0]?.message == "docker build failed"
    }
  }

  def "build with query"() {
    def buildContext = new ByteArrayInputStream([42] as byte[])
    def query = ["rm": false]
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [error : false,
                                            stream: ""]

    when:
    dockerClient.build(buildContext, query)

    then:
    1 * delegateMock.post([path              : "/build",
                           query             : ["rm": false],
                           body              : [42],
                           requestContentType: ContentType.BINARY])
    and:
    dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
      assert arguments[0]?.message == "docker build failed"
    }
  }

  def "tag with defaults"() {
    given:
    dockerClient.responseHandler.statusLine >> Mock(StatusLine)

    when:
    dockerClient.tag("an-image", "registry:port/username/image-name:a-tag")

    then:
    1 * delegateMock.post([path : "/images/an-image/tag",
                           query: [repo : "registry:port/username/image-name",
                                   tag  : "a-tag",
                                   force: false]])
  }

  def "tag with force == true"() {
    given:
    dockerClient.responseHandler.statusLine >> Mock(StatusLine)

    when:
    dockerClient.tag("an-image", "registry:port/username/image-name:a-tag", true)

    then:
    1 * delegateMock.post([path : "/images/an-image/tag",
                           query: [repo : "registry:port/username/image-name",
                                   tag  : "a-tag",
                                   force: true]])
  }

  def "push with defaults"() {
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [:]

    when:
    dockerClient.push("an-image")

    then:
    1 * delegateMock.post([path   : "/images/an-image/push",
                           query  : [registry: "",
                                     tag     : ""],
                           headers: ["X-Registry-Auth": "."]])
  }

  def "push with auth"() {
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [:]

    when:
    dockerClient.push("an-image:a-tag", "some-base64-encoded-auth")

    then:
    1 * delegateMock.post([path   : "/images/an-image/push",
                           query  : [registry: "",
                                     tag     : "a-tag"],
                           headers: ["X-Registry-Auth": "some-base64-encoded-auth"]])
  }

  def "push with registry"() {
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [:]
    dockerClient.responseHandler.statusLine >> Mock(StatusLine)

    when:
    dockerClient.push("an-image", ".", "registry:port")

    then:
    1 * delegateMock.post([path : "/images/an-image/tag",
                           query: [repo : "registry:port/an-image",
                                   tag  : "",
                                   force: true]])
    then:
    1 * delegateMock.post([path   : "/images/registry:port/an-image/push",
                           query  : [registry: "registry:port",
                                     tag     : ""],
                           headers: ["X-Registry-Auth": "."]])
  }

  def "pull with defaults"() {
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks = [[id: "image-id"]]

    when:
    dockerClient.pull("an-image")

    then:
    1 * delegateMock.post([path : "/images/create",
                           query: [fromImage: "an-image",
                                   tag      : "",
                                   registry : ""]])
    and:
    dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
      assert arguments[0]?.message == "docker pull failed"
    }
  }

  def "pull with tag"() {
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks = [[id: "image-id"]]

    when:
    dockerClient.pull("an-image", "a-tag")

    then:
    1 * delegateMock.post([path : "/images/create",
                           query: [fromImage: "an-image",
                                   tag      : "a-tag",
                                   registry : ""]])
    and:
    dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
      assert arguments[0]?.message == "docker pull failed"
    }
  }

  def "pull with registry"() {
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks = [[id: "image-id"]]

    when:
    dockerClient.pull("an-image", "", "registry:port")

    then:
    1 * delegateMock.post([path : "/images/create",
                           query: [fromImage: "registry:port/an-image",
                                   tag      : "",
                                   registry : "registry:port"]])
    and:
    dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
      assert arguments[0]?.message == "docker pull failed"
    }
  }

  def "restart container"() {
    given:
    dockerClient.responseHandler.statusLine >> Mock(StatusLine)

    when:
    dockerClient.restart("a-container")

    then:
    1 * delegateMock.post([path : "/containers/a-container/restart",
                           query: [t: 10]])
  }

  def "stop container"() {
    given:
    dockerClient.responseHandler.statusLine >> Mock(StatusLine)

    when:
    dockerClient.stop("a-container")

    then:
    1 * delegateMock.post([path: "/containers/a-container/stop"])
  }

  def "kill container"() {
    given:
    dockerClient.responseHandler.statusLine >> Mock(StatusLine)

    when:
    dockerClient.kill("a-container")

    then:
    1 * delegateMock.post([path: "/containers/a-container/kill"])
  }

  def "wait container"() {
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [:]

    when:
    dockerClient.wait("a-container")

    then:
    1 * delegateMock.post([path: "/containers/a-container/wait"])
  }

  def "pause container"() {
    given:
    dockerClient.responseHandler.statusLine >> Mock(StatusLine)

    when:
    dockerClient.pause("a-container")

    then:
    1 * delegateMock.post([path: "/containers/a-container/pause"])
  }

  def "unpause container"() {
    given:
    dockerClient.responseHandler.statusLine >> Mock(StatusLine)

    when:
    dockerClient.unpause("a-container")

    then:
    1 * delegateMock.post([path: "/containers/a-container/unpause"])
  }

  def "rm container"() {
    given:
    dockerClient.responseHandler.statusLine >> Mock(StatusLine)

    when:
    dockerClient.rm("a-container")

    then:
    1 * delegateMock.delete([path: "/containers/a-container"]) >> [statusLine: [:]]
  }

  def "ps containers"() {
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [:]

    when:
    dockerClient.ps()

    then:
    1 * delegateMock.get([path : "/containers/json",
                          query: [all : true,
                                  size: false]])
  }

  def "inspect container"() {
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [:]

    when:
    dockerClient.inspectContainer("a-container")

    then:
    1 * delegateMock.get([path: "/containers/a-container/json"])
  }

  def "diff"() {
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [:]

    when:
    dockerClient.diff("a-container")

    then:
    1 * delegateMock.get([path: "/containers/a-container/changes"])
  }

  def "inspect image"() {
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [:]

    when:
    dockerClient.inspectImage("an-image")

    then:
    1 * delegateMock.get([path: "/images/an-image/json"])
  }

  def "history"() {
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [:]

    when:
    dockerClient.history("an-image")

    then:
    1 * delegateMock.get([path: "/images/an-image/history"])
  }

  def "images with defaults"() {
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [:]

    when:
    dockerClient.images()

    then:
    1 * delegateMock.get([path : "/images/json",
                          query: [all    : false,
                                  filters: [:]]])
  }

  def "images with query"() {
    def query = [all    : true,
                 filters: ["dangling": true]]
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [:]

    when:
    dockerClient.images(query)

    then:
    1 * delegateMock.get([path : "/images/json",
                          query: query])
  }

  def "rmi image"() {
    given:
    dockerClient.responseHandler.statusLine >> Mock(StatusLine)

    when:
    dockerClient.rmi("an-image")

    then:
    1 * delegateMock.delete([path: "/images/an-image"]) >> [statusLine: [:]]
  }

  def "create exec"() {
    def execCreateConfig = [:]
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [:]

    when:
    dockerClient.createExec("a-container", execCreateConfig)

    then:
    1 * delegateMock.post([path              : "/containers/a-container/exec",
                           body              : execCreateConfig,
                           requestContentType: ContentType.JSON])
    and:
    dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
      assert arguments[0]?.message == "docker exec create failed"
    }
  }

  def "create exec with missing container"() {
    def execCreateConfig = [:]
    given:
    dockerClient.responseHandler.statusLine >> Mock(StatusLine)
    dockerClient.responseHandler.statusLine.statusCode >> 404
    dockerClient.logger = Mock(Logger)

    when:
    dockerClient.createExec("a-missing-container", execCreateConfig)

    then:
    1 * dockerClient.logger.error("no such container 'a-missing-container'")
    and:
    thrown(DockerClientException)
  }

  def "start exec"() {
    def execStartConfig = [:]
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [:]

    when:
    dockerClient.startExec("an-exec", execStartConfig)

    then:
    1 * delegateMock.post([path              : "/exec/an-exec/start",
                           body              : execStartConfig,
                           requestContentType: ContentType.JSON])
    and:
    dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
      assert arguments[0]?.message == "docker exec start failed"
    }
  }

  def "start exec with missing exec"() {
    def execStartConfig = [:]
    given:
    dockerClient.responseHandler.statusLine >> Mock(StatusLine)
    dockerClient.responseHandler.statusLine.statusCode >> 404
    dockerClient.logger = Mock(Logger)

    when:
    dockerClient.startExec("a-missing-exec", execStartConfig)

    then:
    1 * dockerClient.logger.error("no such exec 'a-missing-exec'")
    and:
    thrown(DockerClientException)
  }

  def "exec"() {
    def execConfig = [:]
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [:]

    when:
    dockerClient.exec("container-id", ["command", "line"], execConfig)

    then:
    1 * dockerClient.createExec("container-id", [
        "AttachStdin" : false,
        "AttachStdout": true,
        "AttachStderr": true,
        "Detach"      : false,
        "Tty"         : false,
        "Cmd"         : ["command", "line"]]) >> [Id: "exec-id"]
    then:
    1 * dockerClient.startExec("exec-id", [
        "AttachStdin" : false,
        "AttachStdout": true,
        "AttachStderr": true,
        "Detach"      : false,
        "Tty"         : false,
        "Cmd"         : ["command", "line"]])
  }

  def "create container with defaults"() {
    def containerConfig = [Cmd: "true"]
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [:]

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
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [:]

    when:
    dockerClient.createContainer(containerConfig, query)

    then:
    1 * delegateMock.post([path              : "/containers/create",
                           query             : query,
                           body              : containerConfig,
                           requestContentType: ContentType.JSON])
  }

  def "start container"() {
    given:
    dockerClient.responseHandler.statusLine >> Mock(StatusLine)

    when:
    dockerClient.startContainer("a-container")

    then:
    1 * delegateMock.post([path              : "/containers/a-container/start",
                           requestContentType: ContentType.JSON])
  }

  def "run container with defaults"() {
    given:
    dockerClient.responseHandler.statusLine >> Mock(StatusLine)

    when:
    dockerClient.run("an-image", [:])

    then:
    1 * dockerClient.createContainer(["Image": "an-image"], [name: ""]) >> [Id: "container-id"]

    then:
    1 * dockerClient.startContainer("container-id")
  }

  def "copy from container"() {
    given:
    dockerClient.responseHandler.statusLine >> Mock(StatusLine)
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [raw: "tar".bytes]

    when:
    def result = dockerClient.copy("a-container", [Resource: "/file.txt"])

    then:
    1 * delegateMock.post([path              : "/containers/a-container/copy",
                           body              : [Resource: "/file.txt"],
                           requestContentType: ContentType.JSON]) >> new ByteArrayInputStream()
    and:
    result == "tar".bytes
  }

  def "copy file from container"() {
    given:
    dockerClient.responseHandler.statusLine >> Mock(StatusLine)
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [raw: "file-content".bytes]

    when:
    def result = dockerClient.copyFile("a-container", "/file.txt")

    then:
    1 * delegateMock.post([path              : "/containers/a-container/copy",
                           body              : [Resource: "/file.txt"],
                           requestContentType: ContentType.JSON]) >> new ByteArrayInputStream()
    and:
    1 * dockerClient.extractSingleTarEntry(_ as byte[], "/file.txt") >> "file-content".bytes
    and:
    result == "file-content".bytes
  }

  def "rename container"() {
    given:
    dockerClient.responseHandler.statusLine >> Mock(StatusLine)

    when:
    dockerClient.rename("an-old-container", "a-new-container-name")

    then:
    1 * delegateMock.post([path : "/containers/an-old-container/rename",
                           query: [name: "a-new-container-name"]]) >> [statusLine: [:]]
  }

  def "search"() {
    given:
    dockerClient.responseHandler.success = true
    dockerClient.responseHandler.chunks << [:]

    when:
    dockerClient.search("ubuntu")

    then:
    1 * delegateMock.get([path : "/images/search",
                          query: [term: "ubuntu"]])
  }
}
