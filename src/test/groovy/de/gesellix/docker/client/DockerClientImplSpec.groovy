package de.gesellix.docker.client

import co.freeside.betamax.Betamax
import co.freeside.betamax.MatchRule
import co.freeside.betamax.Recorder
import co.freeside.betamax.httpclient.BetamaxRoutePlanner
import org.junit.Rule
import spock.lang.Specification

class DockerClientImplSpec extends Specification {

  DockerClient dockerClient

  def authDetails = ["username"     : "gesellix",
                     "password"     : "-yet-another-password-",
                     "email"        : "tobias@gesellix.de",
                     "serveraddress": "https://index.docker.io/v1/"]

  @Rule
  Recorder recorder = new Recorder()

  def setup() {
    def defaultDockerHost = System.env.DOCKER_HOST?.replaceFirst("tcp://", "http://")
    dockerClient = new DockerClientImpl(dockerHost: defaultDockerHost ?: "http://172.17.42.1:4243/")
    BetamaxRoutePlanner.configure(dockerClient.delegate.client)
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
    buildResult == "7efe20b8e170"
  }

  @Betamax(tape = 'build image with unknown base image', match = [MatchRule.method, MatchRule.path])
  def "build image with unknown base image"() {
    given:
    def buildContext = getClass().getResourceAsStream("build/build_with_unknown_base_image.tar")

    when:
    def buildResult = dockerClient.build(buildContext)

    then:
    IllegalStateException ex = thrown()
    ex.message == 'build failed. reason: [errorDetail:[message:HTTP code: 404], error:HTTP code: 404]'
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
    pushResult.status == "Pushing tag for rev [511136ea3c5a] on {https://registry-1.docker.io/v1/repositories/gesellix/test/tags/latest}"
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
    ["Command"   : "true",
     "Created"   : 1404683249,
     "Id"        : "70a1534434ec4ee0708bdfccef5c38ceee28f9023a0b1ef42c788cfc2db13428",
     "Image"     : "busybox:latest",
     "Names"     : ["/berserk_colden"],
     "Ports"     : [],
     "SizeRootFs": 0,
     "SizeRw"    : 0,
     "Status"    : "Up Less than a second"] in containers
  }

  @Betamax(tape = 'inspect container', match = [MatchRule.method, MatchRule.path])
  def "inspect container"() {
    given:
    def imageId = dockerClient.pull("busybox", "latest")
    def imageName = "inspect_container"
    def containerConfig = ["Cmd"  : ["true || false"],
                           "Image": "inspect_container"]
    def hostConfig = ["PublishAllPorts": true]
    dockerClient.tag(imageId, imageName)
    def containerId = dockerClient.createContainer(containerConfig).Id
    dockerClient.startContainer(containerId, hostConfig)

    when:
    def containerInspection = dockerClient.inspectContainer(containerId)

    then:
    [HostnamePath   : "/var/lib/docker/containers/90eb75b01e8223d01ac14b9ad925aff5755e16f80b1341a914c2d5ceb684b472/hostname",
     Config         : [User           : '',
                       OnBuild        : null,
                       Tty            : false,
                       MemorySwap     : 0,
                       StdinOnce      : false,
                       NetworkDisabled: false,
                       ExposedPorts   : null,
                       Cmd            : ["true || false"],
                       CpuShares      : 0,
                       WorkingDir     : '',
                       Cpuset         : '',
                       Env            : ["HOME=/",
                                         "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"],
                       Entrypoint     : null,
                       Memory         : 0,
                       AttachStdout   : true,
                       Domainname     : '',
                       AttachStderr   : true,
                       Image          : "inspect_container",
                       AttachStdin    : false,
                       PortSpecs      : null,
                       Hostname       : "90eb75b01e82",
                       Volumes        : null,
                       OpenStdin      : false],
     ResolvConfPath : "/var/lib/docker/containers/90eb75b01e8223d01ac14b9ad925aff5755e16f80b1341a914c2d5ceb684b472/resolv.conf",
     HostConfig     : [Dns            : [],
                       ContainerIDFile: '',
                       PublishAllPorts: true,
                       Links          : null,
                       DnsSearch      : null,
                       Privileged     : false,
                       NetworkMode    : '',
                       Binds          : [],
                       VolumesFrom    : [],
                       PortBindings   : null,
                       LxcConf        : []],
     VolumesRW      : [:],
     Path           : "true || false",
     ProcessLabel   : '',
     Driver         : "aufs",
     Name           : "/agitated_wozniak",
     Args           : [],
     Created        : "2014-07-06T21:22:49.728215358Z",
     State          : [ExitCode  : 0,
                       Paused    : false,
                       Running   : false,
                       Pid       : 0,
                       StartedAt : "0001-01-01T00:00:00Z",
                       FinishedAt: "0001-01-01T00:00:00Z"],
     ExecDriver     : "native-0.2",
     Image          : "a9eb172552348a9a49180694790b33a1097f546456d041b6e82e4d7716ddb721",
     Id             : "90eb75b01e8223d01ac14b9ad925aff5755e16f80b1341a914c2d5ceb684b472",
     NetworkSettings: [IPPrefixLen: 0,
                       IPAddress  : '',
                       Gateway    : '',
                       Bridge     : '',
                       PortMapping: null,
                       Ports      : null],
     HostsPath      : "/var/lib/docker/containers/90eb75b01e8223d01ac14b9ad925aff5755e16f80b1341a914c2d5ceb684b472/hosts",
     MountLabel     : '',
     Volumes        : [:]] == containerInspection
  }

  @Betamax(tape = 'list images', match = [MatchRule.method, MatchRule.path])
  def "list images"() {
    when:
    def images = dockerClient.images()

    then:
    ["Created"    : 1371157430,
     "Id"         : "511136ea3c5a64f264b78b5433614aec563103b4d4702f3ba7d4d2698e22c158",
     "ParentId"   : "",
     "RepoTags"   : ["scratch:latest", "yetAnotherTag:latest"],
     "Size"       : 0,
     "VirtualSize": 0] in images
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
    containerInfo.Id == "934e64deb11bfa504ac9b8a0ec6acfe85359749a2712e2436f998b78ca5dc310"
  }

  @Betamax(tape = 'create container with name', match = [MatchRule.method, MatchRule.path, MatchRule.query])
  def "create container with name"() {
    given:
    def imageId = dockerClient.pull("busybox", "latest")
    def containerConfig = ["Cmd"  : ["true"],
                           "Image": imageId]

    when:
    def containerInfo = dockerClient.createContainer(containerConfig, "example")

    then:
    containerInfo.Id == "93ef71b2892dfdeb4b657fca1a36651e6a397f2bfbc198f29aaef0ec18a059e1"
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

  @Betamax(tape = 'run container', match = [MatchRule.method, MatchRule.path, MatchRule.query])
  def "run container"() {
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
    def runResult = dockerClient.run("an_image_with_existing_container", containerConfig, [:], tag, name)

    when:
    def rmImageResult = dockerClient.rmi("an_image_with_existing_container:latest")

    then:
    rmImageResult == 200
  }
}
