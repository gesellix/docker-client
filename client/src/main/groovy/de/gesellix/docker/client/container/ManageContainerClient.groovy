package de.gesellix.docker.client.container

import de.gesellix.docker.client.EngineResponseContent
import de.gesellix.docker.client.repository.RepositoryTagParser
import de.gesellix.docker.engine.AttachConfig
import de.gesellix.docker.engine.EngineClient
import de.gesellix.docker.engine.EngineResponse
import de.gesellix.docker.remote.api.ContainerChangeResponseItem
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
import de.gesellix.docker.remote.api.client.ContainerApi
import de.gesellix.docker.remote.api.core.ClientException
import de.gesellix.docker.remote.api.core.Frame
import de.gesellix.docker.remote.api.core.StreamCallback
import de.gesellix.util.QueryUtil
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.Duration
import java.time.temporal.ChronoUnit

class ManageContainerClient implements ManageContainer {

  private final Logger log = LoggerFactory.getLogger(ManageContainerClient)

  private EngineApiClient client
  private EngineClient engineClient
  private DockerResponseHandler responseHandler
  private QueryUtil queryUtil
  private ArchiveUtil archiveUtil
  private RepositoryTagParser repositoryTagParser

  ManageContainerClient(EngineApiClient client, EngineClient engineClient) {
    this.client = client
    this.engineClient = engineClient
    this.responseHandler = new DockerResponseHandler()
    this.repositoryTagParser = new RepositoryTagParser()
    this.queryUtil = new QueryUtil()
    this.archiveUtil = new ArchiveUtil()
  }

  @Override
  EngineResponse attach(String containerId, Map<String, Object> query, AttachConfig callback = null) {
    log.info("docker attach")

    // When using the TTY setting is enabled in POST /containers/create,
    // the stream is the raw data from the process PTY and client’s stdin.
    // When the TTY is disabled, then the stream is multiplexed to separate stdout and stderr.
    EngineResponseContent<ContainerInspectResponse> container = inspectContainer(containerId)
    boolean multiplexStreams = !container.content.config.tty

    def response = engineClient.post([
        path            : "/containers/${containerId}/attach".toString(),
        query           : query,
        attach          : callback,
        multiplexStreams: multiplexStreams])

    if (!callback) {
      response.stream.multiplexStreams = multiplexStreams
    }
    return response
  }

  @Override
  void attach(String containerId, String detachKeys, Boolean logs, Boolean stream,
              Boolean stdin, Boolean stdout, Boolean stderr,
              StreamCallback<Frame> callback, Duration timeout) {
    log.info("docker attach")
    client.containerApi.containerAttach(containerId, detachKeys, logs, stream, stdin, stdout, stderr, callback, timeout.toMillis())
  }

  @Override
  WebSocket attachWebsocket(String containerId, Map<String, Object> query, WebSocketListener listener) {
    log.info("docker attach via websocket")
    WebSocket webSocket = engineClient.webSocket(
        [path : "/containers/${containerId}/attach/ws".toString(),
         query: query],
        listener
    )
    return webSocket
  }

  @Override
  void resizeTTY(String container, Integer height, Integer width) {
    log.info("docker resize container")
//    if (!inspectContainer(container).content.config.tty) {
//      log.warn("container '${container}' hasn't been configured with a TTY!")
//    }
    client.containerApi.containerResize(container, height, width)
  }

  @Override
  EngineResponseContent<IdResponse> commit(String container, Map query, Map config = [:]) {
    log.info("docker commit")

    Map finalQuery = query ?: [:]
    finalQuery.container = container

    config = config ?: [:]

    IdResponse imageCommit = client.imageApi.imageCommit(
        container,
        query.repo as String, query.tag as String,
        query.comment as String, query.author as String,
        query.pause as Boolean,
        query.changes as String,
        new ContainerConfig(
            config.Hostname as String, config.Domainname as String,
            config.User as String,
            config.AttachStdin as Boolean, config.AttachStdout as Boolean, config.AttachStderr as Boolean,
            config.ExposedPorts as Map, config.Tty as Boolean, config.OpenStdin as Boolean, config.StdinOnce as Boolean,
            config.Env as List,
            config.Cmd instanceof String ? [config.Cmd as String] : config.Cmd as List, new HealthConfig(),
            config.ArgsEscaped as Boolean, config.Image as String, config.Volumes as Map,
            config.WorkingDir as String,
            config.Entrypoint instanceof String ? [config.Entrypoint as String] : config.Entrypoint as List,
            config.NetworkDisabled as Boolean, config.MacAddress as String,
            config.OnBuild as List, config.Labels as Map,
            config.StopSignal as String, config.StopTimeout as Integer,
            config.Shell as List
        ))
    return new EngineResponseContent<IdResponse>(imageCommit)
  }

  @Override
  EngineResponseContent<Map<String, Object>> getArchiveStats(String container, String path) {
    log.info("docker archive stats ${container}|${path}")

    Map<String, Object> archiveInfo = client.containerApi.containerArchiveInfo(container, path)
    return new EngineResponseContent<Map<String, Object>>(archiveInfo)
  }

  @Override
  byte[] extractFile(String container, String filename) {
    log.info("extract '${filename}' from '${container}'")

    EngineResponseContent<InputStream> response = getArchive(container, filename)
    return archiveUtil.extractSingleTarEntry(response.content, filename)
  }

  @Override
  EngineResponseContent<InputStream> getArchive(String container, String path) {
    log.info("docker download from ${container}|${path}")

    InputStream archive = client.containerApi.containerArchive(container, path)
    return new EngineResponseContent<InputStream>(archive)
  }

  @Override
  void putArchive(String container, String path, InputStream archive) {
    log.info("docker upload to ${container}|${path}")
    client.containerApi.putContainerArchive(container, path, archive, null, null)
  }

  @Override
  EngineResponseContent<ContainerCreateResponse> createContainer(ContainerCreateRequest containerCreateRequest, String name = "", String authBase64Encoded = "") {
    log.info("docker create")
    if (!containerCreateRequest.image) {
      throw new IllegalArgumentException("'Image' missing in containerCreateRequest")
    }
    try {
      ContainerCreateResponse containerCreate = client.containerApi.containerCreate(containerCreateRequest, name)
      return new EngineResponseContent<ContainerCreateResponse>(containerCreate)
    }
    catch (ClientException exception) {
      if (exception.statusCode == 404) {
        Map<String, String> repoAndTag = repositoryTagParser.parseRepositoryTag(containerCreateRequest.image)
        log.info("'${repoAndTag.repo}:${repoAndTag.tag}' not found locally.")
        client.imageApi.imageCreate(repoAndTag.repo, null, null, repoAndTag.tag, null, authBase64Encoded, null, null, null)
        ContainerCreateResponse containerCreateWithPulledImage = client.containerApi.containerCreate(containerCreateRequest, name)
        return new EngineResponseContent<ContainerCreateResponse>(containerCreateWithPulledImage)
      }
      throw exception
    }
  }

  @Override
  EngineResponseContent<List<ContainerChangeResponseItem>> diff(String containerId) {
    log.info("docker diff")
    List<ContainerChangeResponseItem> containerChanges = client.containerApi.containerChanges(containerId)
    return new EngineResponseContent<List<ContainerChangeResponseItem>>(containerChanges)
  }

  @Override
  EngineResponseContent<IdResponse> createExec(String containerId, ExecConfig execConfig) {
    log.info("docker create exec on '${containerId}'")
    IdResponse containerExec = client.execApi.containerExec(containerId, execConfig)
    return new EngineResponseContent<IdResponse>(containerExec)
  }

  @Override
  void startExec(String execId, ExecStartConfig execStartConfig, AttachConfig attachConfig) {
    log.info("docker start exec '${execId}'")

    // When using the TTY setting is enabled in POST /containers/create,
    // the stream is the raw data from the process PTY and client’s stdin.
    // When the TTY is disabled, then the stream is multiplexed to separate stdout and stderr.
    def execInspect = client.execApi.execInspect(execId)
    boolean multiplexStreams = !execInspect.processConfig.tty
    def response = engineClient.post([
        path              : "/exec/${execId}/start".toString(),
        body              : [Detach: execStartConfig.detach, Tty: execStartConfig.tty],
        requestContentType: "application/json",
        attach            : attachConfig,
        multiplexStreams  : multiplexStreams])

    if (!attachConfig) {
      if (response.status?.code == 404) {
        log.error("no such exec '${execId}'")
      }
      responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker exec start failed"))
      response.stream.multiplexStreams = multiplexStreams
    }
//    return response
  }

  @Override
  void startExec(String execId, ExecStartConfig execStartConfig, StreamCallback<Frame> callback, Duration timeout) {
    log.info("docker start exec '${execId}'")
    client.execApi.execStart(execId, execStartConfig, callback, timeout?.toMillis())
  }

  @Override
  EngineResponseContent<ExecInspectResponse> inspectExec(String execId) {
    log.info("docker inspect exec '${execId}'")
    ExecInspectResponse execInspect = client.execApi.execInspect(execId)
    return new EngineResponseContent<ExecInspectResponse>(execInspect)
  }

  @Override
  EngineResponseContent<IdResponse> exec(String containerId, List<String> command,
                                         StreamCallback<Frame> callback, Duration timeout,
                                         Map<String, Object> execConfig = [
                                             "Detach"     : false,
                                             "AttachStdin": false,
                                             "Tty"        : false]) {
    log.info("docker exec '${containerId}' '${command}'")

    def actualExecConfig = new ExecConfig(
        (execConfig.AttachStdin ?: false) as Boolean,
        true,
        true,
        null,
        (execConfig.Tty ?: false) as Boolean,
        null,
        command,
        null,
        null,
        null)

    EngineResponseContent<IdResponse> execCreateResult = createExec(containerId, actualExecConfig)
    String execId = execCreateResult.content.id
    def execStartConfig = new ExecStartConfig(
        (execConfig.Detach ?: false) as Boolean,
        actualExecConfig.tty)
    startExec(execId, execStartConfig, callback, timeout)
    return execCreateResult
  }

  @Override
  void resizeExec(String exec, Integer height, Integer width) {
    log.info("docker resize exec")
//    if (!client.execApi.execInspect(exec).processConfig.tty) {
//      log.warn("exec '${exec}' hasn't been configured with a TTY!")
//    }
    client.execApi.execResize(exec, height, width)
  }

  @Override
  EngineResponseContent<InputStream> export(String container) {
    log.info("docker export $container")
    InputStream containerExport = client.containerApi.containerExport(container)
    return new EngineResponseContent<InputStream>(containerExport)
  }

  @Override
  EngineResponseContent<ContainerInspectResponse> inspectContainer(String containerId) {
    log.info("docker inspect container")
    ContainerInspectResponse containerInspect = client.containerApi.containerInspect(containerId, null)
    return new EngineResponseContent(containerInspect)
  }

  @Override
  void kill(String containerId) {
    log.info("docker kill")
    client.containerApi.containerKill(containerId, null)
  }

  @Override
  void logs(String container, Map<String, Object> query, StreamCallback<Frame> callback, Duration timeout) {
    log.info("docker logs")

    Map actualQuery = [:]
    if (query) {
      actualQuery.putAll(query)
    }
    Map defaults = [
        follow    : true,
        stdout    : true,
        stderr    : true,
        timestamps: false,
        since     : 0,
        tail      : "all"]
    queryUtil.applyDefaults(actualQuery, defaults)

    // When using the TTY setting is enabled in POST /containers/create,
    // the stream is the raw data from the process PTY and client’s stdin.
    // When the TTY is disabled, then the stream is multiplexed to separate stdout and stderr.
//    def multiplexStreams = !inspectContainer(container).content.config.tty

    client.containerApi.containerLogs(container,
                                      actualQuery.follow as Boolean,
                                      actualQuery.stdout as Boolean,
                                      actualQuery.stderr as Boolean,
                                      actualQuery.since as Integer,
                                      actualQuery.until as Integer,
                                      actualQuery.timestamps as Boolean,
                                      actualQuery.tail as String,
                                      callback, timeout.toMillis())
  }

  @Override
  EngineResponseContent<List<Map<String, Object>>> ps(Map<String, Object> query) {
    log.info("docker ps")
    Map actualQuery = [:]
    if (query) {
      actualQuery.putAll(query)
    }
    Map defaults = [all: true, size: false]
    queryUtil.applyDefaults(actualQuery, defaults)
    queryUtil.jsonEncodeQueryParameter(actualQuery, "filters")
    List<Map> containerList = client.containerApi.containerList(
        actualQuery.all as Boolean,
        actualQuery.limit as Integer,
        actualQuery.size as Boolean,
        actualQuery.filters as String)
    return new EngineResponseContent<List<Map<String, Object>>>(containerList)
  }

  @Override
  EngineResponseContent<List<Map<String, Object>>> ps(Boolean all = true, Integer limit = null, Boolean size = false, String filters = null) {
    log.info("docker ps")
    List<Map> containerList = client.containerApi.containerList(
        all == null ? true : all,
        limit,
        size ?: false,
        filters)
    return new EngineResponseContent<List<Map<String, Object>>>(containerList)
  }

  @Override
  void pause(String containerId) {
    log.info("docker pause")
    client.containerApi.containerPause(containerId)
  }

  @Override
  EngineResponseContent<ContainerPruneResponse> pruneContainers(String filters = null) {
    log.info("docker container prune")
    ContainerPruneResponse containerPrune = client.containerApi.containerPrune(filters)
    return new EngineResponseContent<ContainerPruneResponse>(containerPrune)
  }

  @Override
  void rename(String container, String newName) {
    log.info("docker rename")
    client.containerApi.containerRename(container, newName)
  }

  @Override
  void restart(String containerIdOrName) {
    log.info("docker restart")
    client.containerApi.containerRestart(containerIdOrName, 5)
  }

  @Override
  void rm(String containerIdOrName, Map<String, Object> query = [:]) {
    log.info("docker rm")
    client.containerApi.containerDelete(containerIdOrName, query.v as Boolean, query.force as Boolean, query.link as Boolean)
  }

  @Override
  EngineResponseContent<ContainerCreateResponse> run(ContainerCreateRequest containerCreateRequest, String name = "", String authBase64Encoded = "") {
    log.info("docker run ${containerCreateRequest.image}")
/*
    http://docs.docker.com/reference/api/docker_remote_api_v1.13/#31-inside-docker-run

    Here are the steps of ‘docker run’ :
      Create the container
      If the status code is 404, it means the image doesn’t exist:
        - Try to pull it
        - Then retry to create the container
      Start the container
      If you are not in detached mode:
        - Attach to the container, using logs=1 (to have stdout and stderr from the container’s start) and stream=1
      If in detached mode or only stdin is attached:
        - Display the container’s id
*/
    EngineResponseContent<ContainerCreateResponse> createContainerResponse = createContainer(containerCreateRequest, name, authBase64Encoded)
    log.debug("create container result: ${createContainerResponse}")
    String containerId = createContainerResponse.content.id
    startContainer(containerId)
    return createContainerResponse
  }

  @Override
  void startContainer(String containerId) {
    log.info("docker start")
    client.containerApi.containerStart(containerId, null)
  }

  @Override
  void stats(String container, Boolean stream, StreamCallback<Object> callback, Duration timeout) {
    log.info("docker stats")
    client.containerApi.containerStats(container, stream, null, callback, timeout.toMillis())
  }

  @Override
  void stop(String containerIdOrName, Integer timeoutSeconds) {
    stop(containerIdOrName, timeoutSeconds != null ? Duration.of(timeoutSeconds, ChronoUnit.SECONDS) : null)
  }

  @Override
  void stop(String containerIdOrName, Duration timeout = Duration.of(10, ChronoUnit.SECONDS)) {
    log.info("docker stop")
    long timeoutInSeconds = (timeout ?: Duration.of(10, ChronoUnit.SECONDS)).seconds
    client.containerApi.containerStop(containerIdOrName, timeoutInSeconds as int)
  }

  @Override
  EngineResponseContent<ContainerTopResponse> top(String containerIdOrName, String psArgs = null) {
    log.info("docker top")
    ContainerTopResponse containerTop = client.containerApi.containerTop(containerIdOrName, psArgs ?: null)
    return new EngineResponseContent<ContainerTopResponse>(containerTop)
  }

  @Override
  void unpause(String containerId) {
    log.info("docker unpause")
    client.containerApi.containerUnpause(containerId)
  }

  @Override
  EngineResponseContent<ContainerUpdateResponse> updateContainer(String container, ContainerUpdateRequest containerUpdateRequest) {
    log.info("docker update '${container}'")
    ContainerUpdateResponse containerUpdate = client.containerApi.containerUpdate(container, containerUpdateRequest)
    return new EngineResponseContent<ContainerUpdateResponse>(containerUpdate)
  }

  /**
   ContainerWait waits until the specified container is in a certain state
   indicated by the given condition, either "not-running" (default),
   "next-exit", or "removed".

   If this client's API version is before 1.30, condition is ignored and
   ContainerWait will return immediately with the two channels, as the server
   will wait as if the condition were "not-running".

   TODO - not yet implemented
   If this client's API version is at least 1.30, ContainerWait blocks until
   the request has been acknowledged by the server (with a response header),
   then returns two channels on which the caller can wait for the exit status
   of the container or an error if there was a problem either beginning the
   wait request or in getting the response. This allows the caller to
   synchronize ContainerWait with other calls, such as specifying a
   "next-exit" condition before issuing a ContainerStart request.
   */
  @Override
  EngineResponseContent<ContainerWaitResponse> wait(String containerIdOrName, ContainerApi.ConditionContainerWait condition = null) {
    log.info("docker wait")
    ContainerWaitResponse containerWait = client.containerApi.containerWait(
        containerIdOrName,
        (ContainerApi.ConditionContainerWait) condition)
    return new EngineResponseContent<ContainerWaitResponse>(containerWait)
  }
}
