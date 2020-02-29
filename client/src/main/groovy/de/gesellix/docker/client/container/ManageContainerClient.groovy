package de.gesellix.docker.client.container

import de.gesellix.docker.client.DockerAsyncCallback
import de.gesellix.docker.client.DockerAsyncConsumer
import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.image.ManageImage
import de.gesellix.docker.client.repository.RepositoryTagParser
import de.gesellix.docker.engine.AttachConfig
import de.gesellix.docker.engine.EngineClient
import de.gesellix.docker.engine.EngineResponse
import de.gesellix.docker.rawstream.RawInputStream
import de.gesellix.util.QueryUtil
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import okhttp3.WebSocket
import okhttp3.WebSocketListener

import static java.util.concurrent.Executors.newSingleThreadExecutor

@Slf4j
class ManageContainerClient implements ManageContainer {

    private EngineClient client
    private DockerResponseHandler responseHandler
    private QueryUtil queryUtil
    private ArchiveUtil archiveUtil
    private RepositoryTagParser repositoryTagParser
    private ManageImage manageImage

    ManageContainerClient(EngineClient client, DockerResponseHandler responseHandler, ManageImage manageImage) {
        this.client = client
        this.responseHandler = responseHandler
        this.manageImage = manageImage
        this.repositoryTagParser = new RepositoryTagParser()
        this.queryUtil = new QueryUtil()
        this.archiveUtil = new ArchiveUtil()
    }

    @Override
    EngineResponse attach(containerId, query, AttachConfig callback = null) {
        log.info "docker attach"

        // When using the TTY setting is enabled in POST /containers/create,
        // the stream is the raw data from the process PTY and client’s stdin.
        // When the TTY is disabled, then the stream is multiplexed to separate stdout and stderr.
        def container = inspectContainer(containerId)
        def multiplexStreams = !container.content.Config.Tty

        def response = client.post([path            : "/containers/${containerId}/attach".toString(),
                                    query           : query,
                                    attach          : callback,
                                    multiplexStreams: multiplexStreams])

        if (!callback) {
            response.stream.multiplexStreams = !container.content.Config.Tty
        }
        return response
    }

    @Override
    attachWebsocket(containerId, query, WebSocketListener listener) {
        log.info "docker attach via websocket"
        WebSocket webSocket = client.webSocket(
                [path : "/containers/${containerId}/attach/ws".toString(),
                 query: query],
                listener
        )
        return webSocket
    }

    @Override
    EngineResponse resizeTTY(container, height, width) {
        log.info "docker resize container"
//        if (!inspectContainer(container).Config.Tty) {
//            log.warn "container '${container}' hasn't been configured with a TTY!"
//        }
        def response = client.post([path              : "/containers/${container}/resize".toString(),
                                    query             : [h: height,
                                                         w: width],
                                    requestContentType: "text/plain"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker resize(tty) failed"))
        return response
    }

    @Override
    EngineResponse commit(container, query, config = [:]) {
        log.info "docker commit"

        def finalQuery = query ?: [:]
        finalQuery.container = container

        config = config ?: [:]

        def response = client.post([path              : "/commit",
                                    query             : finalQuery,
                                    requestContentType: "application/json",
                                    body              : config])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker commit failed"))
        return response
    }

    @Override
    getArchiveStats(container, path) {
        log.info "docker archive stats ${container}|${path}"

        def response = client.head([path : "/containers/${container}/archive".toString(),
                                    query: [path: path]])

        if (response.status.code == 404) {
            log.error("no such container ${container} or path ${path}")
        }
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker head archive failed"))

        def pathInfo = response.headers['X-Docker-Container-Path-Stat'.toLowerCase()] as List
        if (!pathInfo) {
            log.error "didn't find 'X-Docker-Container-Path-Stat' header in response"
            return response
        }

        def firstPathInfo = pathInfo.first() as String
        log.debug firstPathInfo
        def decodedPathInfo = new JsonSlurper().parseText(new String(firstPathInfo.decodeBase64()))
        return decodedPathInfo
    }

    @Override
    byte[] extractFile(String container, String filename) {
        log.info "extract '${filename}' from '${container}'"

        def response = getArchive(container, filename)
        return archiveUtil.extractSingleTarEntry(response.stream as InputStream, filename)
    }

    @Override
    EngineResponse getArchive(String container, String path) {
        log.info "docker download from ${container}|${path}"

        def response = client.get([path : "/containers/${container}/archive".toString(),
                                   query: [path: path]])

        if (response.status.code == 404) {
            log.error("no such container ${container} or path ${path}")
        }
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker get archive failed"))

        String pathInfo = response.headers['X-Docker-Container-Path-Stat'.toLowerCase()]
        if (pathInfo) {
            log.debug "archiveStats: ${new JsonSlurper().parseText(new String(pathInfo.decodeBase64()))}"
        }
        return response
    }

    @Override
    EngineResponse putArchive(String container, String path, InputStream archive, Map<String, ?> query = [:]) {
        log.info "docker upload to ${container}|${path}"

        def finalQuery = query ?: [:]
        finalQuery.path = path

        def response = client.put([path              : "/containers/${container}/archive".toString(),
                                   query             : finalQuery,
                                   requestContentType: "application/x-tar",
                                   body              : archive])

        if (response.status.code == 404) {
            log.error("no such container ${container} or path ${path}")
        }
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker put archive failed"))
        return response
    }

    @Override
    EngineResponse createContainer(Map<String, ?> containerConfig, Map<String, ?> query = [name: ""], String authBase64Encoded = "") {
        log.info "docker create"
        def actualContainerConfig = [:] + containerConfig

        def response = client.post([path              : "/containers/create".toString(),
                                    query             : query,
                                    body              : actualContainerConfig,
                                    requestContentType: "application/json"])

        if (!response.status.success) {
            if (response.status?.code == 404) {
                def repoAndTag = repositoryTagParser.parseRepositoryTag(containerConfig.Image)
                log.info "'${repoAndTag.repo}:${repoAndTag.tag}' not found."
                manageImage.create([fromImage: repoAndTag.repo,
                                    tag      : repoAndTag.tag],
                                   [EncodedRegistryAuth: authBase64Encoded ?: ""])
//                manageImage.pull(repoAndTag.repo, repoAndTag.tag, authBase64Encoded)
                // retry...
                response = client.post([path              : "/containers/create".toString(),
                                        query             : query,
                                        body              : actualContainerConfig,
                                        requestContentType: "application/json"])
                responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker create failed after retry"))
            }
            else {
                responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker create failed"))
            }
        }
        return response
    }

    @Override
    EngineResponse diff(containerId) {
        log.info "docker diff"
        def response = client.get([path: "/containers/${containerId}/changes"])
        return response
    }

    @Override
    EngineResponse createExec(containerId, Map execConfig) {
        log.info "docker create exec on '${containerId}'"

        def response = client.post([path              : "/containers/${containerId}/exec".toString(),
                                    body              : execConfig,
                                    requestContentType: "application/json"])

        if (response.status?.code == 404) {
            log.error("no such container '${containerId}'")
        }
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker exec create failed"))
        return response
    }

    @Override
    EngineResponse startExec(execId, Map execConfig, AttachConfig attachConfig = null) {
        log.info "docker start exec '${execId}'"

        // When using the TTY setting is enabled in POST /containers/create,
        // the stream is the raw data from the process PTY and client’s stdin.
        // When the TTY is disabled, then the stream is multiplexed to separate stdout and stderr.
        def exec = inspectExec(execId)
        def multiplexStreams = !exec.content.ProcessConfig.tty

        def response = client.post([path              : "/exec/${execId}/start".toString(),
                                    body              : execConfig,
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

        return response
    }

    @Override
    EngineResponse inspectExec(execId) {
        log.info "docker inspect exec '${execId}'"

        def response = client.get([path: "/exec/${execId}/json".toString()])

        if (response.status?.code == 404) {
            log.error("no such exec '${execId}'")
        }
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker inspect exec failed"))
        return response
    }

    @Override
    EngineResponse exec(containerId, command, Map execConfig = [
            "Detach"     : false,
            "AttachStdin": false,
            "Tty"        : false]) {
        log.info "docker exec '${containerId}' '${command}'"

        def actualExecConfig = [
                "AttachStdin" : execConfig.AttachStdin ?: false,
                "AttachStdout": true,
                "AttachStderr": true,
                "Detach"      : execConfig.Detach ?: false,
                "Tty"         : execConfig.Tty ?: false,
                "Cmd"         : command]

        def execCreateResult = createExec(containerId, actualExecConfig)
        def execId = execCreateResult.content.Id
        return startExec(execId, actualExecConfig)
    }

    @Override
    EngineResponse resizeExec(exec, height, width) {
        log.info "docker resize exec"
//        if (!inspectExec(exec).ProcessConfig.tty) {
//            log.warn "exec '${exec}' hasn't been configured with a TTY!"
//        }
        def response = client.post([path              : "/exec/${exec}/resize".toString(),
                                    query             : [h: height,
                                                         w: width],
                                    requestContentType: "text/plain"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker resize(exec) failed"))
        return response
    }

    @Override
    EngineResponse export(container) {
        log.info "docker export $container"

        def response = client.get([path: "/containers/$container/export"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker export failed"))

        return response
    }

    @Override
    EngineResponse inspectContainer(containerId) {
        log.info "docker inspect container"
        def response = client.get([path: "/containers/${containerId}/json"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker inspect failed"))
        return response
    }

    @Override
    EngineResponse kill(containerId) {
        log.info "docker kill"
        def response = client.post([path: "/containers/${containerId}/kill".toString()])
        return response
    }

    @Override
    EngineResponse logs(container, DockerAsyncCallback callback = null) {
        return logs(container, [:], callback)
    }

    @Override
    EngineResponse logs(container, query, DockerAsyncCallback callback = null) {
        log.info "docker logs"

        def async = callback ? true : false
        def actualQuery = query ?: [:]
        def defaults = [follow    : async,
                        stdout    : true,
                        stderr    : true,
                        timestamps: false,
                        since     : 0,
                        tail      : "all"]
        queryUtil.applyDefaults(actualQuery, defaults)

        // When using the TTY setting is enabled in POST /containers/create,
        // the stream is the raw data from the process PTY and client’s stdin.
        // When the TTY is disabled, then the stream is multiplexed to separate stdout and stderr.
        def multiplexStreams = !inspectContainer(container).content.Config.Tty
        def response = client.get([path : "/containers/${container}/logs",
                                   query: actualQuery,
                                   async: async])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker logs failed"))
        if (async) {
            // TODO this one would work automatically, when the response content-type would be set correctly :-/
            // see https://github.com/gesellix/docker-client/issues/21
            if (multiplexStreams) {
                response.stream = new RawInputStream(response.stream as InputStream)
            }
            def executor = newSingleThreadExecutor()
            def future = executor.submit(new DockerAsyncConsumer(response as EngineResponse, callback))
            response.taskFuture = future
        }
        return response
    }

    @Override
    EngineResponse ps(query = [:]) {
        log.info "docker ps"
        def actualQuery = query ?: [:]
        def defaults = [all: true, size: false]
        queryUtil.applyDefaults(actualQuery, defaults)
        queryUtil.jsonEncodeFilters(actualQuery)
        def response = client.get([path : "/containers/json",
                                   query: actualQuery])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker ps failed"))
        return response
    }

    @Override
    EngineResponse pause(containerId) {
        log.info "docker pause"
        def response = client.post([path: "/containers/${containerId}/pause".toString()])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker pause failed"))
        return response
    }

    @Override
    EngineResponse pruneContainers(query = [:]) {
        log.info "docker container prune"
        def actualQuery = query ?: [:]
        queryUtil.jsonEncodeFilters(actualQuery)
        def response = client.post([path : "/containers/prune",
                                    query: actualQuery])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker container prune failed"))
        return response
    }

    @Override
    EngineResponse rename(String container, String newName) {
        log.info "docker rename"
        def response = client.post([path : "/containers/${container}/rename".toString(),
                                    query: [name: newName]])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker rename failed"))
        return response
    }

    @Override
    EngineResponse restart(String containerIdOrName) {
        log.info "docker restart"
        def response = client.post([path : "/containers/${containerIdOrName}/restart".toString(),
                                    query: [t: 5]])
        return response
    }

    @Override
    EngineResponse rm(String containerIdOrName, query = [:]) {
        log.info "docker rm"
        def response = client.delete([path : "/containers/${containerIdOrName}".toString(),
                                      query: query])
        return response
    }

    @Override
    run(String fromImage, containerConfig, String tag = "", String name = "", String authBase64Encoded = "") {
        log.info "docker run ${fromImage}${tag ? ':' : ''}${tag}"
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
        Map<String, ?> containerConfigWithImageName = [:] + containerConfig
        containerConfigWithImageName.Image = fromImage + (tag ? ":$tag" : "")

        def createContainerResponse = createContainer(containerConfigWithImageName, [name: name ?: ""], authBase64Encoded)
        log.debug "create container result: ${createContainerResponse}"
        def startContainerResponse = startContainer(createContainerResponse.content.Id)
        return [
                container: createContainerResponse,
                status   : startContainerResponse
        ]
    }

    @Override
    EngineResponse startContainer(containerId) {
        log.info "docker start"
        def response = client.post([path              : "/containers/${containerId}/start".toString(),
                                    requestContentType: "application/json"])
        return response
    }

    @Override
    EngineResponse stats(container, DockerAsyncCallback callback = null) {
        log.info "docker stats"

        def async = callback ? true : false
        def response = client.get([path : "/containers/${container}/stats",
                                   query: [stream: async],
                                   async: async])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker stats failed"))
        if (async) {
            def executor = newSingleThreadExecutor()
            def future = executor.submit(new DockerAsyncConsumer(response as EngineResponse, callback))
            response.taskFuture = future
        }
        return response
    }

    @Override
    EngineResponse stop(String containerIdOrName, Integer timeoutSeconds = 10) {
        log.info "docker stop"
        def query = [t: timeoutSeconds ?: 10]
        def response = client.post([path : "/containers/${containerIdOrName}/stop".toString(),
                                    query: query])
        return response
    }

    @Override
    EngineResponse top(String containerIdOrName, ps_args = null) {
        log.info "docker top"

        def query = ps_args ? [ps_args: ps_args] : [:]
        def response = client.get([path : "/containers/${containerIdOrName}/top",
                                   query: query])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker top failed"))
        return response
    }

    @Override
    EngineResponse unpause(containerId) {
        log.info "docker unpause"
        def response = client.post([path: "/containers/${containerId}/unpause".toString()])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker unpause failed"))
        return response
    }

    @Override
    EngineResponse updateContainer(String container, updateConfig) {
        return updateContainers([container], updateConfig)[container]
    }

    @Override
    Map<String, EngineResponse> updateContainers(List<String> containers, updateConfig) {
        log.info "docker update '${containers}'"

        Map<String, EngineResponse> responses = containers.collectEntries { String container ->
            def response = client.post([path              : "/containers/${container}/update".toString(),
                                        body              : updateConfig,
                                        requestContentType: "application/json"])
            if (response.status?.code != 200) {
                log.error("error updating container '${container}': {}", response.content)
            }
            Map<String, EngineResponse> updateResult = [:]
            updateResult[container] = response
            return updateResult
        }
        return responses
    }

// TODO
// ContainerWait waits until the specified container is in a certain state
// indicated by the given condition, either "not-running" (default),
// "next-exit", or "removed".
//
// If this client's API version is before 1.30, condition is ignored and
// ContainerWait will return immediately with the two channels, as the server
// will wait as if the condition were "not-running".
//
// If this client's API version is at least 1.30, ContainerWait blocks until
// the request has been acknowledged by the server (with a response header),
// then returns two channels on which the caller can wait for the exit status
// of the container or an error if there was a problem either beginning the
// wait request or in getting the response. This allows the caller to
// synchronize ContainerWait with other calls, such as specifying a
// "next-exit" condition before issuing a ContainerStart request.

    @Override
    EngineResponse wait(String containerIdOrName) {
        log.info "docker wait"
        def response = client.post([path: "/containers/${containerIdOrName}/wait".toString()])
        return response
    }
}
