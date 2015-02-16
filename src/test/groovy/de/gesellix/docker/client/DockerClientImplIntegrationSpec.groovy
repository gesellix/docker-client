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

class DockerClientImplIntegrationSpec extends Specification {

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
    //defaultDockerHost = "http://172.17.42.1:4243/"
    dockerClient = new DockerClientImpl(dockerHost: defaultDockerHost ?: "http://172.17.42.1:4243/")
    BetamaxRoutePlanner.configure(dockerClient.delegate.client)
  }

  @Betamax(tape = 'ping', match = [MatchRule.method, MatchRule.path])
  def ping() {
    when:
    def ping = dockerClient.ping()

    then:
    ping.status.statusCode == 200
    ping.response == [plain: "OK"]
  }

  @Betamax(tape = 'info', match = [MatchRule.method, MatchRule.path])
  def info() {
    when:
    def info = dockerClient.info()

    then:
    info.Containers == 2
    info.Debug == 1
    info.Driver == "aufs"
    info.DriverStatus == [
        ["Root Dir", "/var/lib/docker/aufs"],
        ["Backing Filesystem", "extfs"],
        ["Dirs", "218"]]
    info.ExecutionDriver == "native-0.2"
    info.ID == "4C3F:A25Q:NBWE:P7OC:YP45:GIOR:HBTQ:BFJ7:CGYE:2YDE:5BXO:ICTB"
    info.Images == 214
    info.IndexServerAddress == "https://index.docker.io/v1/"
    info.InitPath == "/usr/bin/docker"
    info.InitSha1 == ""
    info.IPv4Forwarding == 1
    info.Labels == null
    info.MemTotal == 16262012928
    info.MemoryLimit == 1
    info.Name == "gesellix-r2"
    info.NCPU == 8
    info.NEventsListener == 0
    info.NFd == 31
    info.NGoroutines == 38
    info.KernelVersion == "3.13.0-45-generic"
    info.OperatingSystem == "Ubuntu 14.04.2 LTS"
    info.RegistryConfig == [
        "IndexConfigs"         : [
            "docker.io": ["Mirrors" : null,
                          "Name"    : "docker.io",
                          "Official": true,
                          "Secure"  : true]
        ],
        "InsecureRegistryCIDRs": ["127.0.0.0/8"]
    ]
    info.SwapLimit == 0
  }

  @Betamax(tape = 'version', match = [MatchRule.method, MatchRule.path])
  def version() {
    when:
    def version = dockerClient.version()

    then:
    version == [
        ApiVersion   : "1.17",
        Arch         : "amd64",
        GitCommit    : "a8a31ef",
        GoVersion    : "go1.4.1",
        KernelVersion: "3.13.0-45-generic",
        Os           : "linux",
        Version      : "1.5.0"]
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

  @Betamax(tape = 'build image', match = [MatchRule.method, MatchRule.path])
  def "build image"() {
    given:
    def buildContext = getClass().getResourceAsStream("build/build.tar")

    when:
    def buildResult = dockerClient.build(buildContext)

    then:
    buildResult == "bb85e57675ec"
  }

  @Betamax(tape = 'build image with unknown base image', match = [MatchRule.method, MatchRule.path])
  def "build image with unknown base image"() {
    given:
    def buildContext = getClass().getResourceAsStream("build/build_with_unknown_base_image.tar")

    when:
    dockerClient.build(buildContext)

    then:
    DockerClientException ex = thrown()
    ex.cause.message == 'docker build failed'
    ex.detail.errorDetail == [message: "Error: image missing/image:latest not found"]
    ex.detail.error == "Error: image missing/image:latest not found"
  }

  @Betamax(tape = 'tag image', match = [MatchRule.method, MatchRule.path])
  def "tag image"() {
    given:
    def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
    def imageName = "yetAnotherTag"

    when:
    def buildResult = dockerClient.tag(imageId, imageName)

    then:
    buildResult == 201

    cleanup:
    dockerClient.rmi(imageName)
  }

  @Betamax(tape = 'push image', match = [MatchRule.method, MatchRule.path, MatchRule.query, MatchRule.headers])
  def "push image"() {
    given:
    def authBase64Encoded = dockerClient.encodeAuthConfig(authDetails)
    def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
    def imageName = "gesellix/test:latest"
    dockerClient.tag(imageId, imageName, true)

    when:
    def pushResult = dockerClient.push(imageName, authBase64Encoded)

    then:
    pushResult.status == "Pushing tag for rev [3eb19b6d9332] on {https://cdn-registry-1.docker.io/v1/repositories/gesellix/test/tags/latest}"

    cleanup:
    dockerClient.rmi(imageName)
  }

  @Betamax(tape = 'push image with registry', match = [MatchRule.method, MatchRule.path, MatchRule.query, MatchRule.headers])
  def "push image with registry"() {
    given:
    def authBase64Encoded = dockerClient.encodeAuthConfig(authDetails)
    def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
    def imageName = "gesellix/test:latest"
    dockerClient.tag(imageId, imageName, true)

    when:
    def pushResult = dockerClient.push(imageName, authBase64Encoded, "localhost:5000")

    then:
    pushResult.status == "Pushing tag for rev [3eb19b6d9332] on {http://localhost:5000/v1/repositories/gesellix/test/tags/latest}"

    cleanup:
    dockerClient.rmi(imageName)
  }

  @Betamax(tape = 'push image with undefined authentication', match = [MatchRule.method, MatchRule.path, MatchRule.query, MatchRule.headers])
  def "push image with undefined authentication"() {
    given:
    def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
    def imageName = "gesellix/test:latest"
    dockerClient.tag(imageId, imageName, true)

    when:
    def pushResult = dockerClient.push(imageName, null, "localhost:5000")

    then:
    pushResult.status == "Pushing tag for rev [3eb19b6d9332] on {http://localhost:5000/v1/repositories/gesellix/test/tags/latest}"

    cleanup:
    dockerClient.rmi(imageName)
  }

  @Betamax(tape = 'pull image', match = [MatchRule.method, MatchRule.path, MatchRule.query])
  def "pull image"() {
    when:
    def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")

    then:
    imageId == "3eb19b6d9332"
  }

  @Betamax(tape = 'pull image from private registry', match = [MatchRule.method, MatchRule.path, MatchRule.query])
  def "pull image from private registry"() {
    given:
    dockerClient.pull("gesellix/docker-client-testimage", "latest")
    dockerClient.push("gesellix/docker-client-testimage:latest", "", "localhost:5000")

    when:
    def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest", "localhost:5000")

    then:
    imageId == "3eb19b6d9332"
  }

  @Betamax(tape = 'list containers', match = [MatchRule.method, MatchRule.path])
  def "list containers"() {
    given:
    def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
    def imageName = "list_containers"
    dockerClient.tag(imageId, imageName, true)
    def containerConfig = ["Cmd"  : ["true"],
                           "Image": imageName]
    def containerId = dockerClient.createContainer(containerConfig).Id
    dockerClient.startContainer(containerId)

    when:
    def containers = dockerClient.ps()

    then:
    ["Command": "true",
     "Created": 1423611158,
     "Id"     : "0ab1ccc1a8aae3c15538173a2367be6236622f82ad2c52b7702f1cc5d342d677",
     "Image"  : "gesellix/docker-client-testimage:latest",
     "Names"  : ["/elegant_ptolemy"],
     "Ports"  : [],
     "Status" : "Up Less than a second"] in containers
  }

  @Betamax(tape = 'inspect container', match = [MatchRule.method, MatchRule.path])
  def "inspect container"() {
    given:
    def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
    def imageName = "inspect_container"
    def containerConfig = ["Cmd"       : ["true"],
                           "Image"     : "inspect_container",
                           "HostConfig": ["PublishAllPorts": true]]
    dockerClient.tag(imageId, imageName, true)
    def containerId = dockerClient.createContainer(containerConfig).Id
    dockerClient.startContainer(containerId)

    when:
    def containerInspection = dockerClient.inspectContainer(containerId)

    then:
    containerInspection.HostnamePath == "/var/lib/docker/containers/453297c16c71322adf0452d0bdacbcf9af8e0e4bb6213167f437d7143ed7aa81/hostname"
    and:
    containerInspection.Config.Cmd == ["true"]
    and:
    containerInspection.Config.Image == "inspect_container"
    and:
    containerInspection.Image == "3eb19b6d933247ab513993b2b9ed43a44f0432580e6f4f974bb2071ea968b494"
    and:
    containerInspection.Id == "453297c16c71322adf0452d0bdacbcf9af8e0e4bb6213167f437d7143ed7aa81"

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
    dockerClient.rmi(imageName)
  }

  @Betamax(tape = 'inspect image', match = [MatchRule.method, MatchRule.path])
  def "inspect image"() {
    given:
    def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")

    when:
    def imageInspection = dockerClient.inspectImage(imageId)

    then:
    imageInspection.Config.Image == "3cac76e73e2b43058355dadc14cd24a4a3a8388e0041b4298372732b27d2f4bc"
    and:
    imageInspection.Id == "3eb19b6d933247ab513993b2b9ed43a44f0432580e6f4f974bb2071ea968b494"
    and:
    imageInspection.Parent == "3cac76e73e2b43058355dadc14cd24a4a3a8388e0041b4298372732b27d2f4bc"
    and:
    imageInspection.Container == "c0c18082a03537cda7a61792e50501303051b84a90849765aa0793f69ce169b3"
  }

  @Betamax(tape = 'history', match = [MatchRule.method, MatchRule.path])
  def "history"() {
    given:
    def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")

    when:
    def history = dockerClient.history(imageId)

    then:
    history == [
        ["Created"  : 1423607478,
         "CreatedBy": "/bin/sh -c #(nop) CMD [cat /gattaca.txt]",
         "Id"       : "3eb19b6d933247ab513993b2b9ed43a44f0432580e6f4f974bb2071ea968b494",
         "Size"     : 0,
         "Tags"     : ["example.com:5000/gesellix/example:latest", "gesellix/docker-client-testimage:latest"]],
        ["Created"  : 1423607478,
         "CreatedBy": "/bin/sh -c echo \"The wind caught it\" \u003e /gattaca.txt",
         "Id"       : "3cac76e73e2b43058355dadc14cd24a4a3a8388e0041b4298372732b27d2f4bc",
         "Size"     : 19,
         "Tags"     : null],
        ["Created"  : 1420064636,
         "CreatedBy": "/bin/sh -c #(nop) CMD [/bin/sh]",
         "Id"       : "4986bf8c15363d1c5d15512d5266f8777bfba4974ac56e3270e7760f6f0a8125",
         "Size"     : 0,
         "Tags"     : ["busybox:latest", "busybox:buildroot-2014.02"]],
        ["Created"  : 1420064636,
         "CreatedBy": "/bin/sh -c #(nop) ADD file:8cf517d90fe79547c474641cc1e6425850e04abbd8856718f7e4a184ea878538 in /",
         "Id"       : "ea13149945cb6b1e746bf28032f02e9b5a793523481a0a18645fc77ad53c4ea2",
         "Size"     : 2433303,
         "Tags"     : null],
        ["Created"  : 1412196367,
         "CreatedBy": "/bin/sh -c #(nop) MAINTAINER Jérôme Petazzoni \u003cjerome@docker.com\u003e",
         "Id"       : "df7546f9f060a2268024c8a230d8639878585defcc1bc6f79d2728a13957871b",
         "Size"     : 0,
         "Tags"     : null],
        ["Created"  : 1371157430,
         "CreatedBy": "",
         "Id"       : "511136ea3c5a64f264b78b5433614aec563103b4d4702f3ba7d4d2698e22c158",
         "Size"     : 0,
         "Tags"     : ["scratch:latest"]]
    ]
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
    def imageIds = images.collect { image -> image.Id }
    imageIds.containsAll([
        "3eb19b6d933247ab513993b2b9ed43a44f0432580e6f4f974bb2071ea968b494",
        "3cac76e73e2b43058355dadc14cd24a4a3a8388e0041b4298372732b27d2f4bc",
        "4986bf8c15363d1c5d15512d5266f8777bfba4974ac56e3270e7760f6f0a8125",
        "ea13149945cb6b1e746bf28032f02e9b5a793523481a0a18645fc77ad53c4ea2",
        "df7546f9f060a2268024c8a230d8639878585defcc1bc6f79d2728a13957871b",
        "511136ea3c5a64f264b78b5433614aec563103b4d4702f3ba7d4d2698e22c158"
    ])
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
    def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
    def containerConfig = ["Cmd"  : ["true"],
                           "Image": imageId]

    when:
    def containerInfo = dockerClient.createContainer(containerConfig)

    then:
    containerInfo.Id == "266e22e3e4d53041a811135f13bff8935b64b1dec7fb6c005ce4f00eca0013a1"
  }

  @Betamax(tape = 'create container with name', match = [MatchRule.method, MatchRule.path, MatchRule.query])
  def "create container with name"() {
    given:
    dockerClient.rm("example")
    def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
    def containerConfig = ["Cmd"  : ["true"],
                           "Image": imageId]

    when:
    def containerInfo = dockerClient.createContainer(containerConfig, [name: "example"])

    then:
    containerInfo.Id == "c7da7719091fd3d2f3737e681baa8be593feacce4d08ca4f40c0d15feb5acf65"
  }

  @Betamax(tape = 'create container with unknown base image', match = [MatchRule.method, MatchRule.path, MatchRule.query])
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
    ex.detail == [error      : "Tag unkown not found in repository gesellix/docker-client-testimage",
                  errorDetail: [message: "Tag unkown not found in repository gesellix/docker-client-testimage"]]
  }

  @Betamax(tape = 'start container', match = [MatchRule.method, MatchRule.path])
  def "start container"() {
    given:
    def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
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
    def imageName = "gesellix/docker-client-testimage"
    def tag = "latest"
    def cmds = ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]

    when:
    def containerStatus = dockerClient.run(imageName, containerConfig, tag)

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
    def imageName = "gesellix/docker-client-testimage"
    def tag = "latest"
    def cmds = ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def name = "example-name"

    when:
    def containerStatus = dockerClient.run(imageName, containerConfig, tag, name)

    then:
    containerStatus.status == 204

    and:
    def containers = dockerClient.ps()
    containers[0].Names == ["/example-name"]

    cleanup:
    dockerClient.stop(containerStatus.container.Id)
  }

  @Betamax(tape = 'restart container', match = [MatchRule.method, MatchRule.path])
  def "restart container"() {
    given:
    def imageName = "gesellix/docker-client-testimage"
    def tag = "latest"
    def cmds = ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def containerStatus = dockerClient.run(imageName, containerConfig, tag)

    when:
    def result = dockerClient.restart(containerStatus.container.Id)

    then:
    result.status.statusCode == 204
  }

  @Betamax(tape = 'stop container', match = [MatchRule.method, MatchRule.path])
  def "stop container"() {
    given:
    def imageName = "gesellix/docker-client-testimage"
    def tag = "latest"
    def cmds = ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def containerStatus = dockerClient.run(imageName, containerConfig, tag)

    when:
    def result = dockerClient.stop(containerStatus.container.Id)

    then:
    result == 204
  }

  @Betamax(tape = 'kill container', match = [MatchRule.method, MatchRule.path])
  def "kill container"() {
    given:
    def imageName = "gesellix/docker-client-testimage"
    def tag = "latest"
    def cmds = ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def containerStatus = dockerClient.run(imageName, containerConfig, tag)

    when:
    def result = dockerClient.kill(containerStatus.container.Id)

    then:
    result.status.statusCode == 204
  }

  @Betamax(tape = 'wait container', match = [MatchRule.method, MatchRule.path])
  def "wait container"() {
    given:
    def imageName = "gesellix/docker-client-testimage"
    def tag = "latest"
    def cmds = ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def containerStatus = dockerClient.run(imageName, containerConfig, tag)
    dockerClient.stop(containerStatus.container.Id)

    when:
    def result = dockerClient.wait(containerStatus.container.Id)

    then:
    result.status.statusCode == 200
    and:
    result.response.StatusCode == 137
  }

  @Betamax(tape = 'pause container', match = [MatchRule.method, MatchRule.path])
  def "pause container"() {
    given:
    def imageName = "gesellix/docker-client-testimage"
    def tag = "latest"
    def cmds = ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def containerStatus = dockerClient.run(imageName, containerConfig, tag)

    when:
    def result = dockerClient.pause(containerStatus.container.Id)

    then:
    result.status.statusCode == 204
  }

  @Betamax(tape = 'unpause container', match = [MatchRule.method, MatchRule.path])
  def "unpause container"() {
    given:
    def imageName = "gesellix/docker-client-testimage"
    def tag = "latest"
    def cmds = ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def containerStatus = dockerClient.run(imageName, containerConfig, tag)
    dockerClient.pause(containerStatus.container.Id)

    when:
    def result = dockerClient.unpause(containerStatus.container.Id)

    then:
    result.status.statusCode == 204
  }

  @Betamax(tape = 'rm container', match = [MatchRule.method, MatchRule.path])
  def "rm container"() {
    given:
    def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
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
    def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
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
    def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
    dockerClient.tag(imageId, "an_image_with_existing_container", true)

    def containerConfig = ["Cmd": ["true"]]
    def tag = "latest"
    def name = "another-example-name"
    dockerClient.rm(name)
    dockerClient.run("an_image_with_existing_container", containerConfig, tag, name)

    when:
    def rmImageResult = dockerClient.rmi("an_image_with_existing_container:latest")

    then:
    rmImageResult == 200
  }

  @Betamax(tape = 'exec create', match = [MatchRule.method, MatchRule.path, MatchRule.body])
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
    def execCreateResult = dockerClient.createExec(containerStatus.container.Id, execConfig)

    then:
    execCreateResult?.Id =~ "[0-9a-f]+"

    cleanup:
    dockerClient.stop(name)
    dockerClient.wait(name)
    dockerClient.rm(name)
  }

  @Betamax(tape = 'exec start', match = [MatchRule.method, MatchRule.path, MatchRule.body])
  def "exec start"() {
    given:
    def imageName = "gesellix/docker-client-testimage"
    def tag = "latest"
    def cmds = ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def name = "start-exec"
    def containerStatus = dockerClient.run(imageName, containerConfig, tag, name)
    def containerId = containerStatus.container.Id
    def execCreateConfig = [
        "AttachStdin" : false,
        "AttachStdout": true,
        "AttachStderr": true,
        "Tty"         : false,
        "Cmd"         : [
            "ls", "-lisah", "/"
        ]]

    def execCreateResult = dockerClient.createExec(containerId, execCreateConfig)
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

  @Betamax(tape = 'copy', match = [MatchRule.method, MatchRule.path])
  def "copy"() {
    given:
    def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
    def imageName = "copy_container"
    def containerConfig = ["Cmd"  : ["sh", "-c", "echo -n -e 'to be or\nnot to be' > /file1.txt"],
                           "Image": "copy_container"]
    dockerClient.tag(imageId, imageName)
    def containerInfo = dockerClient.run(imageName, containerConfig, [:])
    def containerId = containerInfo.container.Id

    when:
    def tarContent = dockerClient.copy(containerId, [Resource: "/file1.txt"])

    then:
    def fileContent = dockerClient.extractSingleTarEntry(tarContent as byte[], "file1.txt")
    and:
    fileContent == "to be or\nnot to be".bytes

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
    dockerClient.rmi(imageName)
  }

  @Betamax(tape = 'rename', match = [MatchRule.method, MatchRule.path])
  def "rename"() {
    given:
    dockerClient.rm("a_wonderful_new_name")
    def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
    def containerConfig = ["Cmd"  : ["true"],
                           "Image": imageId]
    def containerId = dockerClient.createContainer(containerConfig).Id

    when:
    def renameContainerResult = dockerClient.rename(containerId, "a_wonderful_new_name")

    then:
    renameContainerResult == 204

    cleanup:
    dockerClient.rm("a_wonderful_new_name")
  }

  @Betamax(tape = 'search', match = [MatchRule.method, MatchRule.path])
  def "search"() {
    when:
    def searchResult = dockerClient.search("testimage")

    then:
    searchResult.contains([
        description: "",
        is_official: false,
        is_trusted : true,
        name       : "gesellix/docker-client-testimage",
        star_count : 0
    ])
  }
}
