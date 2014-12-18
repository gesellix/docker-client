package de.gesellix.docker.client

import co.freeside.betamax.Betamax
import co.freeside.betamax.MatchRule
import co.freeside.betamax.Recorder
import co.freeside.betamax.httpclient.BetamaxRoutePlanner
import co.freeside.betamax.tape.yaml.OrderedPropertyComparator
import co.freeside.betamax.tape.yaml.TapePropertyUtils
import org.junit.Rule
import org.yaml.snakeyaml.introspector.Property
import spock.lang.Specification
import spock.lang.Unroll

class DockerClientImplSpec extends Specification {

  DockerClient dockerClient

  def authDetails = ["username"     : "gesellix",
                     "password"     : "-yet-another-password-",
                     "email"        : "tobias@gesellix.de",
                     "serveraddress": "https://index.docker.io/v1/"]

  @Rule
  Recorder recorder = new Recorder()

  def setup() {
    // see https://github.com/robfletcher/betamax/issues/141#issuecomment-48077632
    TapePropertyUtils.metaClass.sort = { Set<Property> properties, List<String> names ->
      new LinkedHashSet(properties.sort(true, new OrderedPropertyComparator(names)))
    }

    def defaultDockerHost = System.env.DOCKER_HOST?.replaceFirst("tcp://", "http://")
    dockerClient = new DockerClientImpl(dockerHost: defaultDockerHost ?: "http://172.17.42.1:4243/")
    BetamaxRoutePlanner.configure(dockerClient.delegate.client)
  }

  @Betamax(tape = 'info', match = [MatchRule.method, MatchRule.path])
  def info() {
    when:
    def info = dockerClient.info()

    then:
    info == [
        Containers        : 1,
        Debug             : 1,
        Driver            : "aufs",
        DriverStatus      : [
            ["Root Dir", "/var/lib/docker/aufs"],
            ["Dirs", "138"]],
        ExecutionDriver   : "native-0.2",
        Images            : 136,
        IndexServerAddress: "https://index.docker.io/v1/",
        InitPath          : "/usr/bin/docker",
        InitSha1          : "",
        IPv4Forwarding    : 1,
        NEventsListener   : 0,
        NFd               : 16,
        NGoroutines       : 16,
        KernelVersion     : "3.13.0-41-generic",
        MemoryLimit       : 1,
        OperatingSystem   : "Ubuntu 14.04.1 LTS",
        SwapLimit         : 0]
  }

  @Betamax(tape = 'version', match = [MatchRule.method, MatchRule.path])
  def version() {
    when:
    def version = dockerClient.version()

    then:
    version == [
        ApiVersion   : "1.15",
        Arch         : "amd64",
        GitCommit    : "39fa2fa",
        GoVersion    : "go1.3.3",
        KernelVersion: "3.13.0-41-generic",
        Os           : "linux",
        Version      : "1.3.2"]
  }

  @Betamax(tape = 'auth', match = [MatchRule.method, MatchRule.path])
  def auth() {
    given:
    def authPlain = authDetails

    when:
    def authResult = dockerClient.auth(authPlain)

    then:
    authResult == 200
  }

  def encodeAuthConfig() {
    given:
    def authPlain = authDetails

    when:
    def authResult = dockerClient.encodeAuthConfig(authPlain)

    then:
    authResult == 'eyJ1c2VybmFtZSI6Imdlc2VsbGl4IiwicGFzc3dvcmQiOiIteWV0LWFub3RoZXItcGFzc3dvcmQtIiwiZW1haWwiOiJ0b2JpYXNAZ2VzZWxsaXguZGUiLCJzZXJ2ZXJhZGRyZXNzIjoiaHR0cHM6Ly9pbmRleC5kb2NrZXIuaW8vdjEvIn0='
  }

  @Betamax(tape = 'build image', match = [MatchRule.method, MatchRule.path])
  def "build image"() {
    given:
    def buildContext = getClass().getResourceAsStream("build/build.tar")

    when:
    def buildResult = dockerClient.build(buildContext)

    then:
    buildResult == "6f8c064827e0"
  }

  @Betamax(tape = 'build image with unknown base image', match = [MatchRule.method, MatchRule.path])
  def "build image with unknown base image"() {
    given:
    def buildContext = getClass().getResourceAsStream("build/build_with_unknown_base_image.tar")

    when:
    dockerClient.build(buildContext)

    then:
    DockerClientException ex = thrown()
    ex.cause.message == 'build failed'
    ex.detail == [errorDetail: [message: "Error: image missing/image not found"], error: "Error: image missing/image not found"]
  }

  @Betamax(tape = 'tag image', match = [MatchRule.method, MatchRule.path])
  def "tag image"() {
    given:
    def imageId = dockerClient.pull("scratch")
    def imageName = "yetAnotherTag"

    when:
    def buildResult = dockerClient.tag(imageId, imageName)

    then:
    buildResult == 201
  }

  @Betamax(tape = 'push image', match = [MatchRule.method, MatchRule.path, MatchRule.query, MatchRule.headers])
  def "push image"() {
    given:
    def authBase64Encoded = dockerClient.encodeAuthConfig(authDetails)
    def imageId = dockerClient.pull("scratch")
    def imageName = "gesellix/test"
    dockerClient.tag(imageId, imageName)

    when:
    def pushResult = dockerClient.push(imageName, authBase64Encoded)

    then:
    pushResult.status == "Pushing tag for rev [511136ea3c5a] on {https://cdn-registry-1.docker.io/v1/repositories/gesellix/test/tags/latest}"
  }

  @Betamax(tape = 'push image with registry', match = [MatchRule.method, MatchRule.path, MatchRule.query, MatchRule.headers])
  def "push image with registry"() {
    given:
    def authBase64Encoded = dockerClient.encodeAuthConfig(authDetails)
    def imageId = dockerClient.pull("scratch")
    def imageName = "gesellix/test"
    dockerClient.tag(imageId, imageName)

    when:
    def pushResult = dockerClient.push(imageName, authBase64Encoded, "localhost:5000")

    then:
    pushResult.status == "Pushing tag for rev [511136ea3c5a] on {http://localhost:5000/v1/repositories/gesellix/test/tags/latest}"
  }

  @Betamax(tape = 'push image with undefined authentication', match = [MatchRule.method, MatchRule.path, MatchRule.query, MatchRule.headers])
  def "push image with undefined authentication"() {
    given:
    def imageId = dockerClient.pull("scratch")
    def imageName = "gesellix/test"
    dockerClient.tag(imageId, imageName)

    when:
    def pushResult = dockerClient.push(imageName, null, "localhost:5000")

    then:
    pushResult.status == "Pushing tag for rev [511136ea3c5a] on {http://localhost:5000/v1/repositories/gesellix/test/tags/latest}"
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

  @Betamax(tape = 'pull image', match = [MatchRule.method, MatchRule.path, MatchRule.query])
  def "pull image"() {
    when:
    def imageId = dockerClient.pull("scratch")

    then:
    imageId == "511136ea3c5a"
  }

  @Betamax(tape = 'pull image from private registry', match = [MatchRule.method, MatchRule.path, MatchRule.query])
  def "pull image from private registry"() {
    given:
    dockerClient.pull("scratch")
    dockerClient.push("scratch", "", "localhost:5000")

    when:
    def imageId = dockerClient.pull("scratch", "", "localhost:5000")

    then:
    imageId == "511136ea3c5a"
  }

  @Betamax(tape = 'list containers', match = [MatchRule.method, MatchRule.path])
  def "list containers"() {
    given:
    def imageId = dockerClient.pull("busybox", "latest")
    def imageName = "list_containers"
    dockerClient.tag(imageId, imageName)
    def containerConfig = ["Cmd"  : ["true"],
                           "Image": imageName]
    def containerId = dockerClient.createContainer(containerConfig).Id
    dockerClient.startContainer(containerId)

    when:
    def containers = dockerClient.ps()

    then:
    ["Command": "true",
     "Created": 1417856859,
     "Id"     : "58f8ef5ba7664908343ce4dae6afda8dd02c5f1e24371fafaf22d4c902785a12",
     "Image"  : "busybox:latest",
     "Names"  : ["/insane_wilson"],
     "Ports"  : [],
     "Status" : "Up Less than a second"] in containers
  }

  @Betamax(tape = 'inspect container', match = [MatchRule.method, MatchRule.path])
  def "inspect container"() {
    given:
    def imageId = dockerClient.pull("busybox", "latest")
    def imageName = "inspect_container"
    def containerConfig = ["Cmd"  : ["true"],
                           "Image": "inspect_container"]
    def hostConfig = ["PublishAllPorts": true]
    dockerClient.tag(imageId, imageName)
    def containerId = dockerClient.createContainer(containerConfig).Id
    dockerClient.startContainer(containerId, hostConfig)

    when:
    def containerInspection = dockerClient.inspectContainer(containerId)

    then:
    containerInspection.HostnamePath == "/var/lib/docker/containers/0f645d50932b06cb464bf8b34c440291d1b764009d654aeb4d0353484603b218/hostname"
    and:
    containerInspection.Config.Cmd == ["true"]
    and:
    containerInspection.Config.Image == "inspect_container"
    and:
    containerInspection.Image == "e72ac664f4f0c6a061ac4ef332557a70d69b0c624b6add35f1c181ff7fff2287"
    and:
    containerInspection.Id == "0f645d50932b06cb464bf8b34c440291d1b764009d654aeb4d0353484603b218"
  }

  @Betamax(tape = 'list images', match = [MatchRule.method, MatchRule.path, MatchRule.query])
  def "list images"() {
    when:
    def images = dockerClient.images()

    then:
    ["Created"    : 1371157430,
     "Id"         : "511136ea3c5a64f264b78b5433614aec563103b4d4702f3ba7d4d2698e22c158",
     "ParentId"   : "",
     "RepoTags"   : ["scratch:latest"],
     "Size"       : 0,
     "VirtualSize": 0] in images
  }

  @Betamax(tape = 'list images with intermediate layers', match = [MatchRule.method, MatchRule.path, MatchRule.query])
  def "list images with intermediate layers"() {
    when:
    def images = dockerClient.images([all: true])

    then:
    [Created    : 1412196368,
     Id         : "e72ac664f4f0c6a061ac4ef332557a70d69b0c624b6add35f1c181ff7fff2287",
     ParentId   : "e433a6c5b276a31aa38bf6eaba9cd1cfd69ea33f706ed72b3f20bafde5cd8644",
     RepoTags   : ["busybox:latest"],
     Size       : 0,
     VirtualSize: 2433303] in images

    and:
    [Created    : 1412196368,
     Id         : "e433a6c5b276a31aa38bf6eaba9cd1cfd69ea33f706ed72b3f20bafde5cd8644",
     ParentId   : "df7546f9f060a2268024c8a230d8639878585defcc1bc6f79d2728a13957871b",
     RepoTags   : ["<none>:<none>"],
     Size       : 2433303,
     VirtualSize: 2433303] in images

    and:
    [Created    : 1412196367,
     Id         : "df7546f9f060a2268024c8a230d8639878585defcc1bc6f79d2728a13957871b",
     ParentId   : "511136ea3c5a64f264b78b5433614aec563103b4d4702f3ba7d4d2698e22c158",
     RepoTags   : ["<none>:<none>"],
     Size       : 0,
     VirtualSize: 0] in images

    and:
    [Created    : 1371157430,
     Id         : "511136ea3c5a64f264b78b5433614aec563103b4d4702f3ba7d4d2698e22c158",
     ParentId   : "",
     RepoTags   : ["scratch:latest"],
     Size       : 0,
     VirtualSize: 0] in images
  }

  @Betamax(tape = 'list images filtered', match = [MatchRule.method, MatchRule.path, MatchRule.query])
  def "list images filtered"() {
    when:
    def images = dockerClient.images([filters: '{"dangling":["true"]}'])

    then:
    images.every { image ->
      image.RepoTags == ["<none>:<none>"]
    }
  }

  @Betamax(tape = 'create container', match = [MatchRule.method, MatchRule.path, MatchRule.query])
  def "create container"() {
    given:
    def imageId = dockerClient.pull("busybox", "latest")
    def containerConfig = ["Cmd"  : ["true"],
                           "Image": imageId]

    when:
    def containerInfo = dockerClient.createContainer(containerConfig)

    then:
    containerInfo.Id == "86e08f80c8841790c56532ec0aabfe56df28fc28a4f05bc38523148036898e47"
  }

  @Betamax(tape = 'create container with name', match = [MatchRule.method, MatchRule.path, MatchRule.query])
  def "create container with name"() {
    given:
    dockerClient.rm("example")
    def imageId = dockerClient.pull("busybox", "latest")
    def containerConfig = ["Cmd"  : ["true"],
                           "Image": imageId]

    when:
    def containerInfo = dockerClient.createContainer(containerConfig, [name: "example"])

    then:
    containerInfo.Id == "68bcb493ea471e7aa44fbbec86cee9820ade9ba7b0c7dc63528dee863f9f5dd0"
  }

  @Betamax(tape = 'create container with unknown base image', match = [MatchRule.method, MatchRule.path, MatchRule.query])
  def "create container with unknown base image"() {
    given:
    dockerClient.rm("example")
    def containerConfig = ["Cmd"  : ["true"],
                           "Image": "busybox:unkown"]

    when:
    dockerClient.createContainer(containerConfig, [name: "example"])

    then:
    DockerClientException ex = thrown()
    ex.cause.message == 'pull failed.'
    ex.detail == [error      : "Tag unkown not found in repository busybox",
                  errorDetail: [message: "Tag unkown not found in repository busybox"]]
  }

  @Betamax(tape = 'start container', match = [MatchRule.method, MatchRule.path])
  def "start container"() {
    given:
    def imageId = dockerClient.pull("busybox", "latest")
    def containerConfig = ["Cmd"  : ["true"],
                           "Image": imageId]
    def containerId = dockerClient.createContainer(containerConfig).Id

    when:
    def startContainerResult = dockerClient.startContainer(containerId)

    then:
    startContainerResult == 204
  }

  @Betamax(tape = 'run container with existing base image', match = [MatchRule.method, MatchRule.path, MatchRule.query])
  def "run container with existing base image"() {
    given:
    def imageName = "busybox"
    def tag = "latest"
    def cmds = ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def hostConfig = [:]

    when:
    def containerStatus = dockerClient.run(imageName, containerConfig, hostConfig, tag)

    then:
    containerStatus.status == 204

    cleanup:
    dockerClient.stop(containerStatus.container.Id)
    dockerClient.wait(containerStatus.container.Id)
    dockerClient.rm(containerStatus.container.Id)
  }

  @Betamax(tape = 'run container with PortBindings', match = [MatchRule.method, MatchRule.path, MatchRule.query])
  def "run container with PortBindings"() {
    given:
    def imageName = "busybox"
    def tag = "latest"
    def cmds = ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd"       : cmds,
                           ExposedPorts: ["4711/tcp": [:]]]
    def hostConfig = ["PortBindings": [
        "4711/tcp": [
            ["HostIp"  : "0.0.0.0",
             "HostPort": "4712"]]
    ]]

    when:
    def containerStatus = dockerClient.run(imageName, containerConfig, hostConfig, tag)

    then:
    containerStatus.status == 204
    and:
    dockerClient.inspectContainer(containerStatus.container.Id).Config.ExposedPorts == ["4711/tcp": [:]]
    and:
    dockerClient.inspectContainer(containerStatus.container.Id).HostConfig.PortBindings == [
        "4711/tcp": [
            ["HostIp"  : "0.0.0.0",
             "HostPort": "4712"]]
    ]

    cleanup:
    dockerClient.stop(containerStatus.container.Id)
  }

  @Betamax(tape = 'run container with name', match = [MatchRule.method, MatchRule.path, MatchRule.query])
  def "run container with name"() {
    given:
    def imageName = "busybox"
    def tag = "latest"
    def cmds = ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def hostConfig = [:]
    def name = "example-name"

    when:
    def containerStatus = dockerClient.run(imageName, containerConfig, hostConfig, tag, name)

    then:
    containerStatus.status == 204

    and:
    def containers = dockerClient.ps()
    containers[0].Names == ["/example-name"]

    cleanup:
    dockerClient.stop(containerStatus.container.Id)
  }

  @Betamax(tape = 'stop container', match = [MatchRule.method, MatchRule.path])
  def "stop container"() {
    given:
    def imageName = "busybox"
    def tag = "latest"
    def cmds = ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def hostConfig = [:]
    def containerStatus = dockerClient.run(imageName, containerConfig, hostConfig, tag)

    when:
    def result = dockerClient.stop(containerStatus.container.Id)

    then:
    result == 204
  }

  @Betamax(tape = 'wait container', match = [MatchRule.method, MatchRule.path])
  def "wait container"() {
    given:
    def imageName = "busybox"
    def tag = "latest"
    def cmds = ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def hostConfig = [:]
    def containerStatus = dockerClient.run(imageName, containerConfig, hostConfig, tag)
    dockerClient.stop(containerStatus.container.Id)

    when:
    def result = dockerClient.wait(containerStatus.container.Id)

    then:
    result.status.statusCode == 200
    and:
    result.response.StatusCode == -1
  }

  @Betamax(tape = 'rm container', match = [MatchRule.method, MatchRule.path])
  def "rm container"() {
    given:
    def imageId = dockerClient.pull("busybox", "latest")
    def containerConfig = ["Cmd"  : ["true"],
                           "Image": imageId]
    def containerId = dockerClient.createContainer(containerConfig).Id

    when:
    def rmContainerResult = dockerClient.rm(containerId)

    then:
    rmContainerResult == 204
  }

  @Betamax(tape = 'rm unkown container', match = [MatchRule.method, MatchRule.path])
  def "rm unknown container"() {
    when:
    def rmContainerResult = dockerClient.rm("a_not_so_random_id")

    then:
    rmContainerResult == 404
  }

  @Betamax(tape = 'rm image', match = [MatchRule.method, MatchRule.path])
  def "rm image"() {
    given:
    def imageId = dockerClient.pull("scratch", "latest")
    dockerClient.tag(imageId, "an_image_to_be_deleted")

    when:
    def rmImageResult = dockerClient.rmi("an_image_to_be_deleted")

    then:
    rmImageResult == 200
  }

  @Betamax(tape = 'rm unkown image', match = [MatchRule.method, MatchRule.path])
  def "rm unkown image"() {
    when:
    def rmImageResult = dockerClient.rmi("an_unkown_image")

    then:
    rmImageResult == 404
  }

  @Betamax(tape = 'rm image with existing container', match = [MatchRule.method, MatchRule.path])
  def "rm image with existing container"() {
    given:
    def imageId = dockerClient.pull("busybox", "latest")
    dockerClient.tag(imageId, "an_image_with_existing_container")

    def containerConfig = ["Cmd": ["true"]]
    def tag = "latest"
    def name = "another-example-name"
    dockerClient.run("an_image_with_existing_container", containerConfig, [:], tag, name)

    when:
    def rmImageResult = dockerClient.rmi("an_image_with_existing_container:latest")

    then:
    rmImageResult == 200
  }
}
