package de.gesellix.docker.client

import de.gesellix.docker.client.rawstream.RawInputStream
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import okhttp3.ws.WebSocketCall
import okhttp3.ws.WebSocketListener
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.IOUtils
import org.codehaus.groovy.runtime.MethodClosure

import static java.net.Proxy.NO_PROXY
import static java.util.concurrent.Executors.newSingleThreadExecutor

@Slf4j
class DockerClientImpl implements DockerClient {

    def responseHandler = new DockerResponseHandler()

    def proxy

    DockerConfig config = new DockerConfig()

    HttpClient httpClient
    def Closure<HttpClient> newDockerHttpClient

    DockerClientImpl() {
        this(new DockerConfig())
    }

    DockerClientImpl(String dockerHost) {
        this(new DockerConfig(dockerHost: dockerHost))
    }

    DockerClientImpl(DockerConfig config) {
        this.config = config
        this.proxy = NO_PROXY
        newDockerHttpClient = new MethodClosure(this, "createDockerHttpClient")
    }

    def getHttpClient() {
        if (!httpClient) {
            this.httpClient = newDockerHttpClient(config, proxy)
            log.info "using docker at '${config.dockerHost}'"
        }
        return httpClient
    }

    def createDockerHttpClient(dockerConfig, proxy) {
        return new OkDockerClient(config: dockerConfig, proxy: proxy)
    }

    @Override
    def cleanupStorage(Closure shouldKeepContainer, Closure shouldKeepVolume = { true }) {
        cleanupContainers shouldKeepContainer
        cleanupImages()
        cleanupVolumes shouldKeepVolume
    }

    @Override
    def cleanupContainers(Closure shouldKeepContainer) {
        def allContainers = ps([filters: [status: ["exited"]]]).content
        allContainers.findAll { Map container ->
            !shouldKeepContainer(container)
        }.each { container ->
            log.debug "docker rm ${container.Id} (${container.Names.first()})"
            rm(container.Id)
        }
    }

    @Override
    def cleanupImages() {
        images([filters: [dangling: ["true"]]]).content.each { image ->
            log.debug "docker rmi ${image.Id}"
            rmi(image.Id)
        }
    }

    @Override
    def cleanupVolumes(Closure shouldKeepVolume) {
        def allVolumes = volumes([filters: [dangling: ["true"]]]).content.Volumes
        allVolumes.findAll { Map volume ->
            !shouldKeepVolume(volume)
        }.each { volume ->
            log.debug "docker volume rm ${volume.Id}"
            rmVolume(volume.Id)
        }
    }

    @Override
    def ping() {
        log.info "docker ping"
        def response = getHttpClient().get([path: "/_ping", timeout: 2000])
        return response
    }

    @Override
    def info() {
        log.info "docker info"
        def response = getHttpClient().get([path: "/info"])
        return response
    }

    @Override
    def version() {
        log.info "docker version"
        def response = getHttpClient().get([path: "/version"])
        return response
    }

    @Override
    def readDefaultAuthConfig() {
        return readAuthConfig(null, config.getDockerConfigFile())
    }

    @Override
    def readAuthConfig(def hostname, File dockerCfg) {
        log.debug "read authConfig"

        if (!dockerCfg) {
            dockerCfg = config.getDockerConfigFile()
        }
        if (!dockerCfg?.exists()) {
            log.warn "${dockerCfg} doesn't exist"
            return [:]
        }
        log.debug "reading auth info from ${dockerCfg}"
        def parsedDockerCfg = new JsonSlurper().parse(dockerCfg)

        if (!hostname) {
            hostname = config.indexUrl_v1
        }

        def authConfig
        if (parsedDockerCfg['auths']) {
            authConfig = parsedDockerCfg.auths
        } else {
            authConfig = parsedDockerCfg
        }

        if (!authConfig[hostname]) {
            return [:]
        }

        def authDetails = ["username"     : "UNKNOWN-USERNAME",
                           "password"     : "UNKNOWN-PASSWORD",
                           "email"        : "UNKNOWN-EMAIL",
                           "serveraddress": hostname]


        def auth = authConfig[hostname].auth as String
        def (username, password) = new String(auth.decodeBase64()).split(":")
        authDetails.username = username
        authDetails.password = password
        authDetails.email = authConfig[hostname].email

        return authDetails
    }

    @Override
    def encodeAuthConfig(def authConfig) {
        log.debug "encode authConfig for ${authConfig.username}@${authConfig.serveraddress}"
        return new JsonBuilder(authConfig).toString().bytes.encodeBase64().toString()
    }

    @Override
    def auth(def authDetails) {
        log.info "docker login"
        def response = getHttpClient().post([path              : "/auth",
                                             body              : authDetails,
                                             requestContentType: "application/json"])
        return response
    }

    @Override
    def build(InputStream buildContext, query = ["rm": true]) {
        log.info "docker build"
        def response = getHttpClient().post([path              : "/build",
                                             query             : query,
                                             body              : buildContext,
                                             requestContentType: "application/octet-stream"])

        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker build failed"))
        def lastChunk = response.content.last()
        return lastChunk.stream.trim() - "Successfully built "
    }

    @Override
    def tag(imageId, repository) {
        log.info "docker tag"
        def repoAndTag = parseRepositoryTag(repository)
        def response = getHttpClient().post([path : "/images/${imageId}/tag".toString(),
                                             query: [repo: repoAndTag.repo,
                                                     tag : repoAndTag.tag]])
        return response
    }

    @Override
    def push(imageName, authBase64Encoded = ".", registry = "") {
        log.info "docker push '${imageName}'"

        def actualImageName = imageName
        if (registry) {
            actualImageName = "$registry/$imageName".toString()
            tag(imageName, actualImageName)
        }
        def repoAndTag = parseRepositoryTag(actualImageName)

        def response = getHttpClient().post([path   : "/images/${repoAndTag.repo}/push".toString(),
                                             query  : [tag: repoAndTag.tag],
                                             headers: ["X-Registry-Auth": authBase64Encoded ?: "."]])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker push failed"))
        return response
    }

    @Override
    def parseRepositoryTag(name) {
        if (name.endsWith(':')) {
            throw new DockerClientException(new IllegalArgumentException("'$name' should not end with a ':'"))
        }

        // see https://github.com/docker/docker/blob/master/pkg/parsers/parsers.go#L72:
        // Get a repos name and returns the right reposName + tag
        // The tag can be confusing because of a port in a repository name.
        //     Ex: localhost.localdomain:5000/samalba/hipache:latest

        def lastColonIndex = name.lastIndexOf(':')
        if (lastColonIndex < 0) {
            return [
                    repo: name,
                    tag : ""
            ]
        }

        def tag = name.substring(lastColonIndex + 1)
        if (!tag.contains('/')) {
            return [
                    repo: name.substring(0, lastColonIndex),
                    tag : tag
            ]
        }

        return [
                repo: name,
                tag : ""
        ]
    }

    @Override
    def pull(imageName, tag = "", authBase64Encoded = ".", registry = "") {
        log.info "docker pull '${imageName}:${tag}'"

        def actualImageName = imageName
        if (registry) {
            actualImageName = "$registry/$imageName".toString()
        }

        def response = getHttpClient().post([path   : "/images/create",
                                             query  : [fromImage: actualImageName,
                                                       tag      : tag,
                                                       registry : registry],
                                             headers: ["X-Registry-Auth": authBase64Encoded ?: "."]])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker pull failed"))
//        println new JsonBuilder(response.content).toString()

        return findImageId(actualImageName, tag)
    }

    @Override
    def importUrl(url, repository = "", tag = "") {
        log.info "docker import '${url}' into ${repository}:${tag}"

        def response = getHttpClient().post([path : "/images/create",
                                             query: [fromSrc: url.toString(),
                                                     repo   : repository ?: "",
                                                     tag    : tag ?: ""]])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker import from url failed"))

        def responseBody = response.content
        return responseBody.status.last()
    }

    @Override
    def importStream(stream, repository = "", tag = "") {
        log.info "docker import stream into ${repository}:${tag}"

        def response = getHttpClient().post([path              : "/images/create",
                                             body              : stream,
                                             query             : [fromSrc: "-",
                                                                  repo   : repository ?: "",
                                                                  tag    : tag ?: ""],
                                             requestContentType: "application/x-tar"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker import from stream failed"))

        def responseBody = response.content
        return responseBody.status
    }

    @Override
    def export(container) {
        log.info "docker export $container"

        def response = getHttpClient().get([path: "/containers/$container/export"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker export failed"))

        return response
    }

    @Override
    def save(... images) {
        log.info "docker save"

        def response
        if (images.length == 1) {
            response = getHttpClient().get([path: "/images/${images.first()}/get"])
        } else {
            response = getHttpClient().get([path : "/images/get",
                                            query: [names: images]])
        }
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker save failed"))

        return response
    }

    @Override
    def load(stream) {
        log.info "docker load"

        def response = getHttpClient().post([path              : "/images/load",
                                             body              : stream,
                                             requestContentType: "application/x-tar"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker load failed"))

        return response
    }

    // TODO we might need some authentication here for the pull(...) step
    @Override
    def createContainer(containerConfig, query = [name: ""]) {
        log.info "docker create"
        def actualContainerConfig = [:] + containerConfig

        def response = getHttpClient().post([path              : "/containers/create".toString(),
                                             query             : query,
                                             body              : actualContainerConfig,
                                             requestContentType: "application/json"])

        if (!response.status.success) {
            if (response.status?.code == 404) {
                def repoAndTag = parseRepositoryTag(containerConfig.Image)
                log.info "'${repoAndTag.repo}:${repoAndTag.tag}' not found."
                pull(repoAndTag.repo, repoAndTag.tag)
                // retry...
                response = getHttpClient().post([path              : "/containers/create".toString(),
                                                 query             : query,
                                                 body              : actualContainerConfig,
                                                 requestContentType: "application/json"])
                responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker create failed after retry"))
            }
            responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker create failed"))
        }
        return response
    }

    @Override
    def startContainer(containerId) {
        log.info "docker start"
        def response = getHttpClient().post([path              : "/containers/${containerId}/start".toString(),
                                             requestContentType: "application/json"])
        return response
    }

    @Override
    def updateContainer(container, updateConfig) {
        return updateContainers([container], updateConfig)[container]
    }

    @Override
    def updateContainers(List containers, updateConfig) {
        log.info "docker update '${containers}'"

        def responses = containers.collectEntries { container ->
            def response = getHttpClient().post([path              : "/containers/${container}/update".toString(),
                                                 body              : updateConfig,
                                                 requestContentType: "application/json"])
            if (response.status?.code != 200) {
                log.error("error updating container '${container}': {}", response.content)
            }
            def updateResult = [:]
            updateResult[container] = response
            return updateResult
        }
        return responses
    }

    @Override
    def run(fromImage, containerConfig, tag = "", name = "") {
        log.info "docker run"
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
        def containerConfigWithImageName = [:] + containerConfig
        containerConfigWithImageName.Image = fromImage + (tag ? ":$tag" : "")

        def createContainerResponse = createContainer(containerConfigWithImageName, [name: name ?: ""])
        def startContainerResponse = startContainer(createContainerResponse.content.Id)
        return [
                container: createContainerResponse,
                status   : startContainerResponse
        ]
    }

    @Override
    def restart(containerId) {
        log.info "docker restart"
        def response = getHttpClient().post([path : "/containers/${containerId}/restart".toString(),
                                             query: [t: 5]])
        return response
    }

    @Override
    def stop(containerId) {
        log.info "docker stop"
        def response = getHttpClient().post([path: "/containers/${containerId}/stop".toString()])
        return response
    }

    @Override
    def kill(containerId) {
        log.info "docker kill"
        def response = getHttpClient().post([path: "/containers/${containerId}/kill".toString()])
        return response
    }

    @Override
    def wait(containerId) {
        log.info "docker wait"
        def response = getHttpClient().post([path: "/containers/${containerId}/wait".toString()])
        return response
    }

    @Override
    def pause(containerId) {
        log.info "docker pause"
        def response = getHttpClient().post([path: "/containers/${containerId}/pause".toString()])
        return response
    }

    @Override
    def unpause(containerId) {
        log.info "docker unpause"
        def response = getHttpClient().post([path: "/containers/${containerId}/unpause".toString()])
        return response
    }

    @Override
    def rm(containerId) {
        log.info "docker rm"
        def response = getHttpClient().delete([path: "/containers/${containerId}".toString()])
        return response
    }

    @Override
    def rmi(imageId) {
        log.info "docker rmi"
        def response = getHttpClient().delete([path: "/images/${imageId}".toString()])
        return response
    }

    @Override
    def ps(query = [:]) {
        log.info "docker ps"
        def actualQuery = query ?: [:]
        def defaults = [all: true, size: false]
        applyDefaults(actualQuery, defaults)
        jsonEncodeFilters(actualQuery)
        def response = getHttpClient().get([path : "/containers/json",
                                            query: actualQuery])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker ps failed"))
        return response
    }

    @Override
    def inspectContainer(containerId) {
        log.info "docker inspect container"
        def response = getHttpClient().get([path: "/containers/${containerId}/json"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker inspect failed"))
        return response
    }

    @Override
    def diff(containerId) {
        log.info "docker diff"
        def response = getHttpClient().get([path: "/containers/${containerId}/changes"])
        return response
    }

    @Override
    def inspectImage(imageId) {
        log.info "docker inspect image"
        def response = getHttpClient().get([path: "/images/${imageId}/json"])
        return response
    }

    @Override
    def history(imageId) {
        log.info "docker history"
        def response = getHttpClient().get([path: "/images/${imageId}/history"])
        return response
    }

    @Override
    def images(query = [:]) {
        log.info "docker images"
        def actualQuery = query ?: [:]
        def defaults = [all: false]
        applyDefaults(actualQuery, defaults)
        jsonEncodeFilters(actualQuery)
        def response = getHttpClient().get([path : "/images/json",
                                            query: actualQuery])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker images failed"))
        return response
    }

    def findImageId(imageName, tag = "") {
        def isDigest = imageName.contains '@'
        def images = images((isDigest) ? [digests: '1'] : [:]).content
//        println new JsonBuilder(images).toString()
        def imageIdsByRepoDigest = images.collectEntries { image ->
            image.RepoDigests?.collectEntries { repoDigest ->
                def idByDigest = [:]
                idByDigest[repoDigest] = image.Id
                idByDigest
            } ?: [:]
        }
        def imageIdsByName = images.collectEntries { image ->
            image.RepoTags?.collectEntries { repoTag ->
                def idByName = [:]
                idByName[repoTag] = image.Id
                idByName
            } ?: [:]
        }

        if (isDigest) {
            if (imageIdsByRepoDigest[imageName.toString()]) {
                return imageIdsByRepoDigest[imageName.toString()]
            }
            log.warn("couldn't find imageId for `${imageName}` via `docker images`")
            return imageName
        } else {
            def canonicalImageName = "$imageName:${tag ?: 'latest'}".toString()
            if (imageIdsByName[canonicalImageName]) {
                return imageIdsByName[canonicalImageName]
            }
            log.warn("couldn't find imageId for `${canonicalImageName}` via `docker images`")
            return canonicalImageName
        }
    }

    @Override
    def createExec(containerId, execConfig) {
        log.info "docker create exec on '${containerId}'"

        def response = getHttpClient().post([path              : "/containers/${containerId}/exec".toString(),
                                             body              : execConfig,
                                             requestContentType: "application/json"])


        if (response.status?.code == 404) {
            log.error("no such container '${containerId}'")
        }
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker exec create failed"))
        return response
    }

    @Override
    def startExec(execId, execConfig) {
        log.info "docker start exec '${execId}'"

        def response = getHttpClient().post([path              : "/exec/${execId}/start".toString(),
                                             body              : execConfig,
                                             requestContentType: "application/json"])


        if (response.status?.code == 404) {
            log.error("no such exec '${execId}'")
        }
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker exec start failed"))
        return response
    }

    @Override
    def inspectExec(execId) {
        log.info "docker inspect exec '${execId}'"

        def response = getHttpClient().get([path: "/exec/${execId}/json".toString()])

        if (response.status?.code == 404) {
            log.error("no such exec '${execId}'")
        }
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker inspect exec failed"))
        return response
    }

    @Override
    def exec(containerId, command, execConfig = [
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
    def getArchiveStats(container, path) {
        log.info "docker archive stats ${container}|${path}"

        def response = getHttpClient().head([path : "/containers/${container}/archive".toString(),
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
    def extractFile(container, String filename) {
        log.info "extract '${filename}' from '${container}'"

        def response = getArchive(container, filename)
        return extractSingleTarEntry(response.stream as InputStream, filename)
    }

    @Override
    def getArchive(container, path) {
        log.info "docker download from ${container}|${path}"

        def response = getHttpClient().get([path : "/containers/${container}/archive".toString(),
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
    def putArchive(container, path, InputStream archive, query = [:]) {
        log.info "docker upload to ${container}|${path}"

        def finalQuery = query ?: [:]
        finalQuery.path = path

        def response = getHttpClient().put([path              : "/containers/${container}/archive".toString(),
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
    def rename(containerId, newName) {
        log.info "docker rename"
        def response = getHttpClient().post([path : "/containers/${containerId}/rename".toString(),
                                             query: [name: newName]])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker rename failed"))
        return response
    }

    @Override
    def search(term) {
        log.info "docker search"
        def response = getHttpClient().get([path : "/images/search".toString(),
                                            query: [term: term]])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker search failed"))
        return response
    }

    @Override
    def attach(containerId, query) {
        log.info "docker attach"
        def container = inspectContainer(containerId)
        def response = getHttpClient().post([path : "/containers/${containerId}/attach".toString(),
                                             query: query])

        // When using the TTY setting is enabled in POST /containers/create,
        // the stream is the raw data from the process PTY and client’s stdin.
        // When the TTY is disabled, then the stream is multiplexed to separate stdout and stderr.
        response.stream.multiplexStreams = !container.content.Config.Tty
        return response
    }

    @Override
    def attachWebsocket(containerId, query, WebSocketListener listener) {
        log.info "docker attach via websocket"
        WebSocketCall webSocketCall = getHttpClient().webSocketCall(
                [path : "/containers/${containerId}/attach/ws".toString(),
                 query: query]
        )

        webSocketCall.enqueue(listener)
        return webSocketCall
    }

    @Override
    def commit(container, query, config = [:]) {
        log.info "docker commit"

        def finalQuery = query ?: [:]
        finalQuery.container = container

        config = config ?: [:]

        def response = getHttpClient().post([path              : "/commit",
                                             query             : finalQuery,
                                             requestContentType: "application/json",
                                             body              : config])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker commit failed"))
        return response
    }

    @Override
    def resizeTTY(container, height, width) {
        log.info "docker resize container"
//        if (!inspectContainer(container).Config.Tty) {
//            log.warn "container '${container}' hasn't been configured with a TTY!"
//        }
        def response = getHttpClient().post([path              : "/containers/${container}/resize".toString(),
                                             query             : [h: height,
                                                                  w: width],
                                             requestContentType: "text/plain"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker resize(tty) failed"))
        return response
    }

    @Override
    def resizeExec(exec, height, width) {
        log.info "docker resize exec"
//        if (!inspectExec(exec).ProcessConfig.tty) {
//            log.warn "exec '${exec}' hasn't been configured with a TTY!"
//        }
        def response = getHttpClient().post([path              : "/exec/${exec}/resize".toString(),
                                             query             : [h: height,
                                                                  w: width],
                                             requestContentType: "text/plain"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker resize(exec) failed"))
        return response
    }

    @Override
    def events(DockerAsyncCallback callback, query = [:]) {
        log.info "docker events"

        jsonEncodeFilters(query)
        def response = getHttpClient().get([path : "/events",
                                            query: query,
                                            async: true])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker events failed"))
        def executor = newSingleThreadExecutor()
        executor.submit(new DockerAsyncConsumer(response, callback))
        return response
    }

    @Override
    def top(container, ps_args = null) {
        log.info "docker top"

        def query = ps_args ? [ps_args: ps_args] : [:]
        def response = getHttpClient().get([path : "/containers/${container}/top",
                                            query: query])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker top failed"))
        return response
    }

    @Override
    def stats(container, DockerAsyncCallback callback = null) {
        log.info "docker stats"

        def async = callback ? true : false
        def response = getHttpClient().get([path : "/containers/${container}/stats",
                                            query: [stream: async],
                                            async: async])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker stats failed"))
        if (async) {
            def executor = newSingleThreadExecutor()
            executor.submit(new DockerAsyncConsumer(response, callback))
        }
        return response
    }

    @Override
    def logs(container, DockerAsyncCallback callback = null) {
        return logs(container, [:], callback)
    }

    @Override
    def logs(container, query, DockerAsyncCallback callback = null) {
        log.info "docker logs"

        def async = callback ? true : false
        def actualQuery = query ?: [:]
        def defaults = [follow    : async,
                        stdout    : true,
                        stderr    : true,
                        timestamps: false,
                        since     : 0,
                        tail      : "all"]
        applyDefaults(actualQuery, defaults)

        // When using the TTY setting is enabled in POST /containers/create,
        // the stream is the raw data from the process PTY and client’s stdin.
        // When the TTY is disabled, then the stream is multiplexed to separate stdout and stderr.
        def multiplexStreams = !inspectContainer(container).content.Config.Tty
        def response = getHttpClient().get([path : "/containers/${container}/logs",
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
            executor.submit(new DockerAsyncConsumer(response, callback))
        }
        return response
    }

    @Override
    def volumes(query = [:]) {
        log.info "docker volume ls"
        def actualQuery = query ?: [:]
        jsonEncodeFilters(actualQuery)
        def response = getHttpClient().get([path : "/volumes",
                                            query: actualQuery])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker volume ls failed"))
        return response
    }

    @Override
    def inspectVolume(name) {
        log.info "docker volume inspect"
        def response = getHttpClient().get([path: "/volumes/$name"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker volume inspect failed"))
        return response
    }

    @Override
    def createVolume(config = [:]) {
        log.info "docker volume create"
        def response = getHttpClient().post([path              : "/volumes/create",
                                             body              : config ?: [:],
                                             requestContentType: "application/json"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker volume create failed"))
        return response
    }

    @Override
    def rmVolume(name) {
        log.info "docker volume rm"
        def response = getHttpClient().delete([path: "/volumes/$name"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker volume rm failed"))
        return response
    }

    @Override
    def networks(query = [:]) {
        log.info "docker network ls"
        def actualQuery = query ?: [:]
        jsonEncodeFilters(actualQuery)
        def response = getHttpClient().get([path : "/networks",
                                            query: actualQuery])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker network ls failed"))
        return response
    }

    @Override
    def inspectNetwork(name) {
        log.info "docker network inspect"
        def response = getHttpClient().get([path: "/networks/$name"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker network inspect failed"))
        return response
    }

    @Override
    def createNetwork(name, config = [:]) {
        log.info "docker network create"
        def actualConfig = config ?: [:]
        def defaults = [Name: name]
        applyDefaults(actualConfig, defaults)
        def response = getHttpClient().post([path              : "/networks/create",
                                             body              : actualConfig ?: [:],
                                             requestContentType: "application/json"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker network create failed"))
        return response
    }

    @Override
    def connectNetwork(network, container) {
        log.info "docker network connect"
        def response = getHttpClient().post([path              : "/networks/$network/connect",
                                             body              : [container: container],
                                             requestContentType: "application/json"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker network connect failed"))
        return response
    }

    @Override
    def disconnectNetwork(network, container) {
        log.info "docker network disconnect"
        def response = getHttpClient().post([path              : "/networks/$network/disconnect",
                                             body              : [container: container],
                                             requestContentType: "application/json"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker network disconnect failed"))
        return response
    }

    @Override
    def rmNetwork(name) {
        log.info "docker network rm"
        def response = getHttpClient().delete([path: "/networks/$name"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker network rm failed"))
        return response
    }

    @Override
    def nodes(query = [:]) {
        log.info "docker node ls"
        def actualQuery = query ?: [:]
        jsonEncodeFilters(actualQuery)
        def response = getHttpClient().get([path : "/nodes",
                                            query: actualQuery])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker node ls failed"))
        return response
    }

    @Override
    def inspectNode(name) {
        log.info "docker node inspect"
        def response = getHttpClient().get([path: "/nodes/$name"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker node inspect failed"))
        return response
    }

    @Override
    def rmNode(name) {
        log.info "docker node rm"
        def response = getHttpClient().delete([path: "/nodes/$name"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker node rm failed"))
        return response
    }

    @Override
    def updateNode(name, query, config) {
        log.info "docker node update"
        def actualQuery = query ?: [:]
        config = config ?: [:]
        def response = getHttpClient().post([path              : "/nodes/$name/update",
                                             query             : actualQuery,
                                             body              : config,
                                             requestContentType: "application/json"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker node update failed"))
        return response
    }

    @Override
    def inspectSwarm(query = [:]) {
        log.info "docker swarm inspect"
        def actualQuery = query ?: [:]
        jsonEncodeFilters(actualQuery)
        def response = getHttpClient().get([path : "/swarm",
                                            query: actualQuery])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker swarm inspect failed"))
        return response
    }

    @Override
    def initSwarm(config) {
        log.info "docker swarm init"
        config = config ?: [:]
        def response = getHttpClient().post([path              : "/swarm/init",
                                             body              : config,
                                             requestContentType: "application/json"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker swarm init failed"))
        return response
    }

    @Override
    def joinSwarm(config) {
        log.info "docker swarm join"
        config = config ?: [:]
        def response = getHttpClient().post([path              : "/swarm/join",
                                             body              : config,
                                             requestContentType: "application/json"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker swarm join failed"))
        return response
    }

    @Override
    def leaveSwarm(query = [:]) {
        log.info "docker swarm leave"
        def actualQuery = query ?: [:]
        def response = getHttpClient().post([path : "/swarm/leave",
                                             query: actualQuery])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker swarm leave failed"))
        return response
    }

    @Override
    def updateSwarm(query, config) {
        log.info "docker swarm update"
        def actualQuery = query ?: [:]
        config = config ?: [:]
        def response = getHttpClient().post([path              : "/swarm/update",
                                             query             : actualQuery,
                                             body              : config,
                                             requestContentType: "application/json"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker swarm update failed"))
        return response
    }

    @Override
    def services(query = [:]) {
        log.info "docker service ls"
        def actualQuery = query ?: [:]
        jsonEncodeFilters(actualQuery)
        def response = getHttpClient().get([path : "/services",
                                            query: actualQuery])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker service ls failed"))
        return response
    }

    @Override
    def createService(config) {
        log.info "docker service create"
        config = config ?: [:]
        def response = getHttpClient().post([path              : "/services/create",
                                             body              : config,
                                             requestContentType: "application/json"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker service create failed"))
        return response
    }

    @Override
    def rmService(name) {
        log.info "docker service rm"
        def response = getHttpClient().delete([path: "/services/$name"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker service rm failed"))
        return response
    }

    @Override
    def inspectService(name) {
        log.info "docker service inspect"
        def response = getHttpClient().get([path: "/services/$name"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker service inspect failed"))
        return response
    }

    @Override
    def updateService(name, query, config) {
        log.info "docker service update"
        def actualQuery = query ?: [:]
        config = config ?: [:]
        def response = getHttpClient().post([path              : "/services/$name/update",
                                             query             : actualQuery,
                                             body              : config,
                                             requestContentType: "application/json"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker service update failed"))
        return response
    }

    @Override
    def tasks(query = [:]) {
        log.info "docker service tasks"
        def actualQuery = query ?: [:]
        jsonEncodeFilters(actualQuery)
        def response = getHttpClient().get([path : "/tasks",
                                            query: actualQuery])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker service tasks failed"))
        return response
    }

    @Override
    def inspectTask(name) {
        log.info "docker task inspect"
        def response = getHttpClient().get([path: "/tasks/$name"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker task inspect failed"))
        return response
    }

    def extractSingleTarEntry(InputStream tarContent, String filename) {
        def stream = new TarArchiveInputStream(new BufferedInputStream(tarContent))

        TarArchiveEntry entry = stream.nextTarEntry
        log.debug("entry size: ${entry.size}")

        def entryName = entry.name
        if (!filename.endsWith(entryName)) {
            log.warn("entry name '${entryName}' doesn't match expected filename '${filename}'")
        } else {
            log.debug("entry name: ${entryName}")
        }

        byte[] content = new byte[(int) entry.size]
        log.debug("going to read ${content.length} bytes")

        stream.read(content, 0, content.length)
        IOUtils.closeQuietly(stream)

        return content
    }

    def applyDefaults(query, defaults) {
        defaults.each { k, v ->
            if (!query.containsKey(k)) {
                query[k] = v
            }
        }
    }

    def jsonEncodeFilters(query) {
        query.each { k, v ->
            if (k == "filters") {
                query[k] = new JsonBuilder(v).toString()
            }
        }
    }
}
