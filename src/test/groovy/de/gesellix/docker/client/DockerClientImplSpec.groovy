package de.gesellix.docker.client

import co.freeside.betamax.Betamax
import co.freeside.betamax.MatchRule
import co.freeside.betamax.Recorder
import co.freeside.betamax.httpclient.BetamaxRoutePlanner
import groovy.json.JsonBuilder
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
    dockerClient = new DockerClientImpl(dockerHost: "http://172.17.42.1:4243/")
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

  @Betamax(tape = 'build image', match = [MatchRule.method, MatchRule.path])
  def "build image"() {
    given:
    def buildContext = getClass().getResourceAsStream("build/build.tar")

    when:
    def buildResult = dockerClient.build(buildContext)

    then:
    buildResult == "87746d9ade98"
  }

  @Betamax(tape = 'tag image', match = [MatchRule.method, MatchRule.path])
  def "tag image"() {
    given:
    def imageId = dockerClient.pull("scratch")
    def repositoryName = "yetAnotherTag"

    when:
    def buildResult = dockerClient.tag(imageId, repositoryName)

    then:
    buildResult == 201
  }

  @Betamax(tape = 'push image', match = [MatchRule.method, MatchRule.path])
  def "push image"() {
    given:
    def authBase64Encoded = new JsonBuilder(authDetails).toString().bytes.encodeBase64()
    def imageId = dockerClient.pull("scratch")
    def repositoryName = "gesellix/test"
    dockerClient.tag(imageId, repositoryName)

    when:
    def pushResult = dockerClient.push(repositoryName, authBase64Encoded)

    then:
    pushResult.status == "Pushing tag for rev [511136ea3c5a] on {https://registry-1.docker.io/v1/repositories/gesellix/test/tags/latest}"
  }

  @Betamax(tape = 'pull image', match = [MatchRule.method, MatchRule.path, MatchRule.query])
  def "pull image"() {
    when:
    def imageId = dockerClient.pull("scratch")

    then:
    imageId == "511136ea3c5a"
  }

  @Betamax(tape = 'list containers', match = [MatchRule.method, MatchRule.path])
  def "get containers"() {
    given:
    def imageId = dockerClient.pull("busybox", "latest")
    def repositoryName = "list_containers"
    def containerConfig = ["Cmd"  : ["true || false"],
                           "Image": "list_containers"]
    dockerClient.tag(imageId, repositoryName)
    def containerId = dockerClient.createContainer(containerConfig).Id
    dockerClient.startContainer(containerId)

    when:
    def containers = dockerClient.ps()

    then:
    ["Command"   : "true || false",
     "Created"   : 1403447968,
     "Id"        : "795cdc234ed9684f7e7bad454b4499e9d359b5ed91940269a8fe7d0e2028c16b",
     "Image"     : "busybox:latest",
     "Names"     : ["/sharp_hopper"],
     "Ports"     : [],
     "SizeRootFs": 0,
     "SizeRw"    : 0,
     "Status"    : "Up Less than a second"] in containers
  }

  @Betamax(tape = 'list images', match = [MatchRule.method, MatchRule.path])
  def "get images"() {
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

  @Betamax(tape = 'create container', match = [MatchRule.method, MatchRule.path])
  def "create container"() {
    given:
    def imageId = dockerClient.pull("busybox", "latest")
    def containerConfig = ["Cmd"  : ["true"],
                           "Image": imageId]

    when:
    def containerInfo = dockerClient.createContainer(containerConfig)

    then:
    containerInfo.Id == "acf71166506eb9eca56b7e4d505eef6ae87101a188ff3a7cec0566552f86de63"
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
    def cmds = ["sh", "-c", "ping 127.0.0.1"]
    def imageName = "busybox"
    def tag = "latest"

    when:
    def containerStatus = dockerClient.run(["Cmd": cmds], imageName, tag)

    then:
    containerStatus.status == 204

    cleanup:
    dockerClient.stop(containerStatus.container.Id)
  }

  @Betamax(tape = 'stop container', match = [MatchRule.method, MatchRule.path])
  def "stop container"() {
    given:
    def cmds = ["sh", "-c", "ping 127.0.0.1"]
    def imageName = "busybox"
    def tag = "latest"
    def containerStatus = dockerClient.run(["Cmd": cmds], imageName, tag)

    when:
    def result = dockerClient.stop(containerStatus.container.Id)

    then:
    result == 204
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
}
