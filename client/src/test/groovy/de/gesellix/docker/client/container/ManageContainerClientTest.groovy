package de.gesellix.docker.client.container

import de.gesellix.docker.engine.EngineClient
import de.gesellix.docker.remote.api.ContainerConfig
import de.gesellix.docker.remote.api.ContainerCreateRequest
import de.gesellix.docker.remote.api.ContainerCreateResponse
import de.gesellix.docker.remote.api.ContainerInspectResponse
import de.gesellix.docker.remote.api.ContainerPruneResponse
import de.gesellix.docker.remote.api.ContainerTopResponse
import de.gesellix.docker.remote.api.ContainerUpdateRequest
import de.gesellix.docker.remote.api.ContainerUpdateResponse
import de.gesellix.docker.remote.api.ContainerWaitResponse
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.ExecConfig
import de.gesellix.docker.remote.api.ExecInspectResponse
import de.gesellix.docker.remote.api.ExecStartConfig
import de.gesellix.docker.remote.api.HealthConfig
import de.gesellix.docker.remote.api.IdResponse
import de.gesellix.docker.remote.api.ProcessConfig
import de.gesellix.docker.remote.api.client.ContainerApi
import de.gesellix.docker.remote.api.client.ExecApi
import de.gesellix.docker.remote.api.client.ImageApi
import de.gesellix.docker.remote.api.core.StreamCallback
import io.github.joke.spockmockable.Mockable
import spock.lang.Ignore
import spock.lang.Specification

import java.time.Duration
import java.time.temporal.ChronoUnit

@Mockable([
    ContainerApi, ContainerCreateRequest, ContainerCreateResponse, ContainerInspectResponse, ContainerConfig, ContainerWaitResponse, ContainerTopResponse, ContainerPruneResponse, ContainerUpdateRequest, ContainerUpdateResponse,
    ExecApi, ExecConfig, IdResponse, ExecInspectResponse, ProcessConfig])
class ManageContainerClientTest extends Specification {

  ManageContainerClient service
  EngineApiClient client = Mock(EngineApiClient)
  EngineClient httpClient = Mock(EngineClient)

  def setup() {
    service = Spy(ManageContainerClient, constructorArgs: [
        client,
        httpClient])
  }

  def "export container"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi
    def exportedFile = Mock(InputStream)

    when:
    def response = service.export("container-id")

    then:
    1 * containerApi.containerExport("container-id") >> exportedFile
    and:
    response.content == exportedFile
  }

  def "restart container"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi

    when:
    service.restart("a-container")

    then:
    1 * containerApi.containerRestart("a-container", 5)
  }

  def "stop container"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi

    when:
    service.stop("a-container")

    then:
    1 * containerApi.containerStop("a-container", 10)
  }

  def "kill container"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi

    when:
    service.kill("a-container")

    then:
    1 * containerApi.containerKill("a-container", null)
  }

  def "wait container"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi
    def response = Mock(ContainerWaitResponse)

    when:
    def result = service.wait("a-container")

    then:
    1 * containerApi.containerWait("a-container", ContainerApi.ConditionContainerWait.NotMinusRunning) >> response
    result.content == response
  }

  def "pause container"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi

    when:
    service.pause("a-container")

    then:
    1 * containerApi.containerPause("a-container")
  }

  def "unpause container"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi

    when:
    service.unpause("a-container")

    then:
    1 * containerApi.containerUnpause("a-container")
  }

  def "rm container"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi

    when:
    service.rm("a-container")

    then:
    1 * containerApi.containerDelete("a-container", null, null, null)
  }

  def "rm container with query"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi

    when:
    service.rm("a-container", [v: false, force: true, link: false])

    then:
    1 * containerApi.containerDelete("a-container", false, true, false)
  }

  def "ps containers"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi
    String filters = '{"status":["exited"]}'
    def containers = Mock(List)

    when:
    def responseContent = service.ps(null, null, null, filters)

    then:
    1 * containerApi.containerList(true, null, false, filters) >> containers
    responseContent.content == containers
  }

  def "inspect container"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.getContainerApi() >> containerApi
    def inspect = Mock(ContainerInspectResponse)

    when:
    def inspectContainer = service.inspectContainer("a-container")

    then:
    1 * containerApi.containerInspect("a-container", null) >> inspect
    inspectContainer.content == inspect
  }

  def "diff"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi
    def changes = Mock(List)

    when:
    def diff = service.diff("a-container")

    then:
    1 * containerApi.containerChanges("a-container") >> changes
    diff.content == changes
  }

  def "create exec"() {
    given:
    def execApi = Mock(ExecApi)
    client.execApi >> execApi
    def execConfig = Mock(ExecConfig)
    def idResponse = Mock(IdResponse)

    when:
    def exec = service.createExec("a-container", execConfig)

    then:
    1 * execApi.containerExec("a-container", execConfig) >> idResponse
    exec.content == idResponse
  }

  def "start exec"() {
    given:
    def execApi = Mock(ExecApi)
    client.execApi >> execApi
    def execStartConfig = new ExecStartConfig(true, false)
    def callback = Mock(StreamCallback)

    when:
    service.startExec("an-exec", execStartConfig, callback, Duration.of(5, ChronoUnit.SECONDS))

    then:
    1 * execApi.execStart("an-exec", execStartConfig, callback, 5000)
  }

  def "inspect exec"() {
    given:
    def execApi = Mock(ExecApi)
    client.execApi >> execApi
    def inspectResponse = Mock(ExecInspectResponse)

    when:
    def inspectExec = service.inspectExec("an-exec")

    then:
    1 * execApi.execInspect("an-exec") >> inspectResponse
    inspectExec.content == inspectResponse
  }

  def "exec"() {
    given:
    def execApi = Mock(ExecApi)
    client.execApi >> execApi
    def idResponse = Mock(IdResponse)
    idResponse.id >> "exec-id"
    def callback = Mock(StreamCallback)

    when:
    def exec = service.exec("container-id", ["command", "line"], callback, Duration.of(1, ChronoUnit.SECONDS), [:])

    then:
    1 * execApi.containerExec("container-id",
                              new ExecConfig(false, true, true,
                                             null, false,
                                             null, ["command", "line"],
                                             null, null, null)) >> idResponse
    then:
    1 * execApi.execStart("exec-id",
                          new ExecStartConfig(false, false),
                          callback, 1000)
    and:
    exec.content == idResponse
  }

  def "create container with defaults"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi
    def containerConfig = new ContainerCreateRequest().tap { c ->
      c.cmd = ["true"]
      c.image = "example"
    }
    def createResponse = Mock(ContainerCreateResponse)

    when:
    def createContainer = service.createContainer(containerConfig)

    then:
    1 * containerApi.containerCreate(containerConfig, "") >> createResponse
    createContainer.content == createResponse
  }

  def "create container with name"() {
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi
    def containerConfig = new ContainerCreateRequest().tap { c ->
      c.image = "example"
    }
    def createResponse = Mock(ContainerCreateResponse)

    when:
    def createContainer = service.createContainer(containerConfig, "foo")

    then:
    1 * containerApi.containerCreate(containerConfig, "foo") >> createResponse
    createContainer.content == createResponse
  }

  def "start container"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi

    when:
    service.startContainer("a-container")

    then:
    1 * containerApi.containerStart("a-container", null)
  }

  def "update a container's resources"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi
    def updateRequest = Mock(ContainerUpdateRequest)
    def updateResponse = Mock(ContainerUpdateResponse)

    when:
    def responseContent = service.updateContainer("a-container", updateRequest)

    then:
    1 * containerApi.containerUpdate("a-container", updateRequest) >> updateResponse
    responseContent.content == updateResponse
  }

  def "run container with defaults"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi
    def createRequest = Mock(ContainerCreateRequest, { it.image >> "an-image" })
    def createResponse = Mock(ContainerCreateResponse, { it.id >> "container-id" })

    when:
    def responseContent = service.run(createRequest)

    then:
    1 * containerApi.containerCreate(createRequest, "") >> createResponse
    then:
    1 * containerApi.containerStart("container-id", null)
    and:
    responseContent.content == createResponse
  }

  def "retrieve file/folder stats"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi
    def expectedStats = [key: 42]

    when:
    def stats = service.getArchiveStats("a-container", "/path/")

    then:
    1 * containerApi.containerArchiveInfo("a-container", "/path/") >> expectedStats
    stats.content == expectedStats
  }

  def "download file/folder from container"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi
    def archive = Mock(InputStream)

    when:
    def result = service.getArchive("a-container", "/path/")

    then:
    1 * containerApi.containerArchive("a-container", "/path/") >> archive
    result.content == archive
  }

  def "upload file/folder to container"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi
    def tarStream = new ByteArrayInputStream("tar".bytes)

    when:
    service.putArchive("a-container", "/path/", tarStream)

    then:
    1 * containerApi.putContainerArchive("a-container", "/path/", tarStream, null, null)
  }

  def "rename container"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi

    when:
    service.rename("an-old-container", "a-new-container-name")

    then:
    1 * containerApi.containerRename("an-old-container", "a-new-container-name")
  }

  def "attach"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.getContainerApi() >> containerApi
    def callback = Mock(StreamCallback)
    def timeout = Duration.of(1, ChronoUnit.SECONDS)

    when:
    service.attach("a-container", null, true, true, false, true, true, callback, timeout)

    then:
    1 * containerApi.containerAttach("a-container", null, true, true, false, true, true, callback, timeout.toMillis())
  }

  // TODO
  @Ignore
  "attach websocket"() {
//        given:
//        def listener = new DefaultWebSocketListener()
//        def wsCall = new OkHttpClient.Builder().build().newWebSocket(
//                new Request.Builder()
//                        .url("").build(), (listener))
//
//        when:
//        dockerClient.attachWebsocket("a-container", [stream: true], listener)
//
//        then:
//        1 * httpClient.webSocket(
//                [path : "/containers/a-container/attach/ws",
//                 query: [stream: true]], (listener)) >> wsCall
//        and:
//        1 * wsCall.enqueue(listener)
  }

  def "commit container"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    def result = Mock(IdResponse)

    when:
    def responseContent = service.commit("a-container", [
        repo   : 'a-repo',
        tag    : 'the-tag',
        comment: 'a test',
        author : 'Andrew Niccol <g@tta.ca>'
    ])

    then:
    1 * imageApi.imageCommit("a-container",
                             'a-repo', 'the-tag',
                             'a test', 'Andrew Niccol <g@tta.ca>',
                             null, null,
                             new ContainerConfig(
                                 null, null, null, null, null, null, null, null,
                                 null, null, null, null, new HealthConfig(), null, null,
                                 null, null, null, null, null, null, null, null, null, null
                             )) >> result
    responseContent.content == result
  }

  def "commit container with changed container config"() {
    given:
    def imageApi = Mock(ImageApi)
    client.imageApi >> imageApi
    def result = Mock(IdResponse)

    when:
    def responseContent = service.commit("a-container",
                                         [
                                             repo   : 'a-repo',
                                             tag    : 'the-tag',
                                             comment: 'a test',
                                             author : 'Andrew Niccol <g@tta.ca>'
                                         ],
                                         [Cmd: "date"])

    then:
    1 * imageApi.imageCommit("a-container",
                             'a-repo', 'the-tag',
                             'a test', 'Andrew Niccol <g@tta.ca>',
                             null, null,
                             new ContainerConfig(
                                 null, null, null, null, null, null, null, null,
                                 null, null, null, ["date"], new HealthConfig(),
                                 null, null, null, null, null,
                                 null, null, null, null, null, null, null
                             )) >> result
    responseContent.content == result
  }

  def "resize container tty"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi

    when:
    service.resizeTTY("a-container", 42, 31)

    then:
    1 * containerApi.containerResize("a-container", 42, 31)
  }

  def "resize exec tty"() {
    given:
    def execApi = Mock(ExecApi)
    client.execApi >> execApi

    when:
    service.resizeExec("an-exec", 11, 44)

    then:
    1 * execApi.execResize("an-exec", 11, 44)
  }

  def "top"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi
    def topResponse = Mock(ContainerTopResponse)

    when:
    def response = service.top("a-container", "aux")

    then:
    1 * containerApi.containerTop("a-container", "aux") >> topResponse
    response.content == topResponse
  }

  def "stats"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi
    def callback = Mock(StreamCallback)
    def timeout = Duration.of(1, ChronoUnit.SECONDS)

    when:
    service.stats("a-container", true, callback, timeout)

    then:
    1 * containerApi.containerStats("a-container", true, null, callback, timeout.toMillis())
  }

  def "logs"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.getContainerApi() >> containerApi
    def callback = Mock(StreamCallback)

    when:
    service.logs("a-container", null, callback, Duration.of(1, ChronoUnit.SECONDS))

    then:
    1 * containerApi.containerLogs("a-container", true, true, true, 0, null, false, 'all', callback, 1000)
  }

  def "pruneContainers removes containers"() {
    given:
    def containerApi = Mock(ContainerApi)
    client.containerApi >> containerApi
    def prunedContainers = Mock(ContainerPruneResponse)

    when:
    def responseContent = service.pruneContainers('a-filter')

    then:
    1 * containerApi.containerPrune('a-filter') >> prunedContainers
    responseContent.content == prunedContainers
  }
}
