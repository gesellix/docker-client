package de.gesellix.docker.client

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.IOUtils
import org.codehaus.groovy.runtime.MethodClosure
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DockerClientImpl implements DockerClient {

    def Logger logger = LoggerFactory.getLogger(DockerClientImpl)

    def responseHandler = new DockerResponseHandler()

    def proxy
    def dockerHost = "http://127.0.0.1:2375"
    def indexUrl = 'https://index.docker.io/v1/'
    def dockerConfigFile = new File("${System.getProperty('user.home')}/.docker", "config.json")
    def legacyDockerConfigFile = new File("${System.getProperty('user.home')}", ".dockercfg")

    LowLevelDockerClient httpClient
    def Closure<LowLevelDockerClient> newDockerHttpClient

    DockerClientImpl() {
        proxy = Proxy.NO_PROXY
        newDockerHttpClient = new MethodClosure(this, "createDockerHttpClient")
    }

    def getHttpClient() {
        if (!httpClient) {
            this.httpClient = newDockerHttpClient(dockerHost, proxy)
            logger.info "using docker at '${dockerHost}'"
        }
        return httpClient
    }

    def createDockerHttpClient(dockerHost, proxy) {
        return new LowLevelDockerClient(dockerHost: dockerHost, proxy: proxy)
    }

    File getActualDockerConfigFile() {
        String dockerConfig = System.getProperty("docker.config", System.env.DOCKER_CONFIG as String)
        if (dockerConfig) {
            return new File(dockerConfig, 'config.json')
        }
        if (dockerConfigFile.exists()) {
            return dockerConfigFile
        } else if (legacyDockerConfigFile.exists()) {
            return legacyDockerConfigFile
        }
        return null
    }

    @Override
    def cleanupStorage(Closure shouldKeepContainer) {
        def allContainers = ps([filters: [status: ["exited"]]]).content
        allContainers.findAll { Map container ->
            !shouldKeepContainer(container)
        }.each { container ->
            logger.info "docker rm ${container.Id} (${container.Names.first()})"
            rm(container.Id)
        }

        images([filters: [dangling: ["true"]]]).content.each { image ->
            logger.info "docker rmi ${image.Id}"
            rmi(image.Id)
        }
    }

    @Override
    def ping() {
        logger.info "docker ping"
        def response = getHttpClient().get([path: "/_ping"])
        return response
    }

    @Override
    def info() {
        logger.info "docker info"
        def response = getHttpClient().get([path: "/info"])
        return response
    }

    @Override
    def version() {
        logger.info "docker version"
        def response = getHttpClient().get([path: "/version"])
        return response
    }

    @Override
    def readDefaultAuthConfig() {
        return readAuthConfig(null, getActualDockerConfigFile())
    }

    @Override
    def readAuthConfig(def hostname, File dockerCfg) {
        logger.debug "read authConfig"

        if (!dockerCfg) {
            dockerCfg = getActualDockerConfigFile()
        }
        if (!dockerCfg?.exists()) {
            logger.warn "${dockerCfg} doesn't exist"
            return [:]
        }
        logger.debug "reading auth info from ${dockerCfg}"
        def parsedDockerCfg = new JsonSlurper().parse(dockerCfg)

        if (!hostname) {
            hostname = indexUrl
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
        logger.debug "encode authConfig for ${authConfig.username}@${authConfig.serveraddress}"
        return new JsonBuilder(authConfig).toString().bytes.encodeBase64().toString()
    }

    @Override
    def auth(def authDetails) {
        logger.info "docker login"
        def response = getHttpClient().post([path              : "/auth",
                                             body              : authDetails,
                                             requestContentType: "application/json"])
        return response
    }

    @Override
    def build(InputStream buildContext, query = ["rm": true]) {
        logger.info "docker build"
        def response = getHttpClient().post([path              : "/build",
                                             query             : query,
                                             body              : buildContext,
                                             requestContentType: "application/octet-stream"])

        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker build failed"))
        def lastChunk = response.content.last()
        return lastChunk.stream.trim() - "Successfully built "
    }

    @Override
    def tag(imageId, repository, force = false) {
        logger.info "docker tag"
        def repoAndTag = parseRepositoryTag(repository)
        def response = getHttpClient().post([path : "/images/${imageId}/tag".toString(),
                                             query: [repo : repoAndTag.repo,
                                                     tag  : repoAndTag.tag,
                                                     force: force]])
        return response
    }

    @Override
    def push(imageName, authBase64Encoded = ".", registry = "") {
        logger.info "docker push '${imageName}'"

        def actualImageName = imageName
        if (registry) {
            actualImageName = "$registry/$imageName".toString()
            tag(imageName, actualImageName, true)
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
        logger.info "docker pull '${imageName}:${tag}'"

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

        return findImageId(actualImageName, tag)
    }

    @Override
    def importUrl(url, repository = "", tag = "") {
        logger.info "docker import '${url}' into ${repository}:${tag}"

        def response = getHttpClient().post([path : "/images/create",
                                             query: [fromSrc: url,
                                                     repo   : repository ?: "",
                                                     tag    : tag ?: ""]])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker import from url failed"))

        def responseBody = response.content
        return responseBody.status.last()
    }

    @Override
    def importStream(stream, repository = "", tag = "") {
        logger.info "docker import stream into ${repository}:${tag}"

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
        logger.info "docker export $container"

        def response = getHttpClient().get([path: "/containers/$container/export"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker export failed"))

        return response
    }

    @Override
    def save(... images) {
        logger.info "docker save"

        def response
        if (images.length == 1) {
            response = getHttpClient().get([path: "/images/${images.first()}/get"])
        } else {
            throw new UnsupportedOperationException("not yet implemented")
        }
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker save failed"))

        return response
    }

    @Override
    def load(stream) {
        logger.info "docker load"

        def response = getHttpClient().post([path              : "/images/load",
                                             body              : stream,
                                             requestContentType: "application/x-tar"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker load failed"))

        return response
    }

    // TODO we might need some authentication here for the pull(...) step
    @Override
    def createContainer(containerConfig, query = [name: ""]) {
        logger.info "docker create"
        def actualContainerConfig = [:] + containerConfig

        def response = getHttpClient().post([path              : "/containers/create".toString(),
                                             query             : query,
                                             body              : actualContainerConfig,
                                             requestContentType: "application/json"])

        if (!response.status.success) {
            if (response.status?.code == 404) {
                def repoAndTag = parseRepositoryTag(containerConfig.Image)
                logger.info "'${repoAndTag.repo}:${repoAndTag.tag}' not found."
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
        logger.info "docker start"
        def response = getHttpClient().post([path              : "/containers/${containerId}/start".toString(),
                                             requestContentType: "application/json"])
        return response
    }

    @Override
    def run(fromImage, containerConfig, tag = "", name = "") {
        logger.info "docker run"
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
        logger.info "docker restart"
        def response = getHttpClient().post([path : "/containers/${containerId}/restart".toString(),
                                             query: [t: 10]])
        return response
    }

    @Override
    def stop(containerId) {
        logger.info "docker stop"
        def response = getHttpClient().post([path: "/containers/${containerId}/stop".toString()])
        return response
    }

    @Override
    def kill(containerId) {
        logger.info "docker kill"
        def response = getHttpClient().post([path: "/containers/${containerId}/kill".toString()])
        return response
    }

    @Override
    def wait(containerId) {
        logger.info "docker wait"
        def response = getHttpClient().post([path: "/containers/${containerId}/wait".toString()])
        return response
    }

    @Override
    def pause(containerId) {
        logger.info "docker pause"
        def response = getHttpClient().post([path: "/containers/${containerId}/pause".toString()])
        return response
    }

    @Override
    def unpause(containerId) {
        logger.info "docker unpause"
        def response = getHttpClient().post([path: "/containers/${containerId}/unpause".toString()])
        return response
    }

    @Override
    def rm(containerId) {
        logger.info "docker rm"
        def response = getHttpClient().delete([path: "/containers/${containerId}".toString()])
        return response
    }

    @Override
    def rmi(imageId) {
        logger.info "docker rmi"
        def response = getHttpClient().delete([path: "/images/${imageId}".toString()])
        return response
    }

    @Override
    def ps(query = [:]) {
        logger.info "docker ps"
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
        logger.info "docker inspect container"
        def response = getHttpClient().get([path: "/containers/${containerId}/json"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker inspect failed"))
        return response
    }

    @Override
    def diff(containerId) {
        logger.info "docker diff"
        def response = getHttpClient().get([path: "/containers/${containerId}/changes"])
        return response
    }

    @Override
    def inspectImage(imageId) {
        logger.info "docker inspect image"
        def response = getHttpClient().get([path: "/images/${imageId}/json"])
        return response
    }

    @Override
    def history(imageId) {
        logger.info "docker history"
        def response = getHttpClient().get([path: "/images/${imageId}/history"])
        return response
    }

    @Override
    def images(query = [:]) {
        logger.info "docker images"
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
        def images = images().content
        def imageIdsByName = images.collectEntries { image ->
            image.RepoTags.collectEntries { repoTag ->
                def idByName = [:]
                idByName[repoTag] = image.Id
                idByName
            }
        }
        def canonicalImageName = "$imageName:${tag ?: 'latest'}".toString()
        if (imageIdsByName[canonicalImageName]) {
            return imageIdsByName[canonicalImageName]
        } else {
            logger.warn("couldn't find imageId for `${canonicalImageName}` via `docker images`")
            return canonicalImageName
        }
    }

    @Override
    def createExec(containerId, execConfig) {
        logger.info "docker create exec on '${containerId}'"

        def response = getHttpClient().post([path              : "/containers/${containerId}/exec".toString(),
                                             body              : execConfig,
                                             requestContentType: "application/json"])


        if (response.status?.code == 404) {
            logger.error("no such container '${containerId}'")
        }
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker exec create failed"))
        return response
    }

    @Override
    def startExec(execId, execConfig) {
        logger.info "docker start exec '${execId}'"

        def response = getHttpClient().post([path              : "/exec/${execId}/start".toString(),
                                             body              : execConfig,
                                             requestContentType: "application/json"])


        if (response.status?.code == 404) {
            logger.error("no such exec '${execId}'")
        }
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker exec start failed"))
        return response
    }

    @Override
    def exec(containerId, command, execConfig = [
            "Detach"     : false,
            "AttachStdin": false,
            "Tty"        : false]) {
        logger.info "docker exec '${containerId}' '${command}'"

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

    /**
     * @deprecated use #extractFile
     * @see #extractFile(java.lang.String, java.lang.String)
     */
    @Deprecated
    @Override
    def copyFile(containerId, String filename) {
        logger.info "copy '${filename}' from '${containerId}'"

        def response = copy(containerId, [Resource: filename])
        return extractSingleTarEntry(response.stream as InputStream, filename)
    }

    /**
     * @deprecated use #getArchive
     * @see #getArchive(java.lang.String, java.lang.String)
     */
    @Deprecated
    @Override
    def copy(containerId, resourceBody) {
        logger.info "docker cp ${containerId} ${resourceBody}"

        def response = getHttpClient().post([path              : "/containers/${containerId}/copy".toString(),
                                             body              : resourceBody,
                                             requestContentType: "application/json"])

        if (response.status.code == 404) {
            logger.error("no such container ${containerId}")
        }
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker cp failed"))
        return response
    }

    @Override
    def getArchiveStats(container, path) {
        logger.info "docker archive stats ${container}|${path}"

        def response = getHttpClient().head([path : "/containers/${container}/archive".toString(),
                                             query: [path: path]])

        if (response.status.code == 404) {
            logger.error("no such container ${container} or path ${path}")
        }
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker head archive failed"))

        def pathInfo = response.headers['X-Docker-Container-Path-Stat'.toLowerCase()] as List
        if (!pathInfo) {
            logger.error "didn't find 'X-Docker-Container-Path-Stat' header in response"
            return response
        }

        def firstPathInfo = pathInfo.first() as String
        logger.debug firstPathInfo
        def decodedPathInfo = new JsonSlurper().parseText(new String(firstPathInfo.decodeBase64()))
        return decodedPathInfo
    }

    @Override
    def extractFile(container, String filename) {
        logger.info "extract '${filename}' from '${container}'"

        def response = getArchive(container, filename)
        return extractSingleTarEntry(response.stream as InputStream, filename)
    }

    @Override
    def getArchive(container, path) {
        logger.info "docker download from ${container}|${path}"

        def response = getHttpClient().get([path : "/containers/${container}/archive".toString(),
                                            query: [path: path]])

        if (response.status.code == 404) {
            logger.error("no such container ${container} or path ${path}")
        }
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker get archive failed"))

        def pathInfo = response.headers['X-Docker-Container-Path-Stat'.toLowerCase()] as List
        if (pathInfo) {
            def firstPathInfo = pathInfo.first() as String
            logger.debug "archiveStats: ${new JsonSlurper().parseText(new String(firstPathInfo.decodeBase64()))}"
        }
        return response
    }

    @Override
    def putArchive(container, path, InputStream archive, query = [:]) {
        logger.info "docker upload to ${container}|${path}"

        def finalQuery = query ?: [:]
        finalQuery.path = path

        def response = getHttpClient().put([path              : "/containers/${container}/archive".toString(),
                                            query             : finalQuery,
                                            requestContentType: "application/x-tar",
                                            body              : archive])

        if (response.status.code == 404) {
            logger.error("no such container ${container} or path ${path}")
        }
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker put archive failed"))
        return response
    }

    @Override
    def rename(containerId, newName) {
        logger.info "docker rename"
        def response = getHttpClient().post([path : "/containers/${containerId}/rename".toString(),
                                             query: [name: newName]])
        return response
    }

    @Override
    def search(term) {
        logger.info "docker search"
        def response = getHttpClient().get([path : "/images/search".toString(),
                                            query: [term: term]])
        return response
    }

    @Override
    def attach(containerId, query) {
        logger.info "docker attach"
        def container = inspectContainer(containerId)
        def response = getHttpClient().post([path : "/containers/${containerId}/attach".toString(),
                                             query: query])
        response.stream.multiplexStreams = !container.Config.Tty
        return response
    }

    @Override
    def attachWebsocket(containerId, query, handler) {
        logger.info "docker attach via websocket"
        DockerWebsocketClient wsClient = getHttpClient().getWebsocketClient(
                [path : "/containers/${containerId}/attach/ws".toString(),
                 query: query],
                handler)
        // TODO we should connect here, shouldn't we?
//    wsClient.connectBlocking()
        return wsClient
    }

    @Override
    def commit(container, query, config = [:]) {
        logger.info "docker commit"

        def finalQuery = query ?: [:]
        finalQuery.container = container

        config = config ?: [:]

        def response = getHttpClient().post([path              : "/commit",
                                             query             : finalQuery,
                                             requestContentType: "application/json",
                                             body              : config])
        return response
    }

    def extractSingleTarEntry(InputStream tarContent, String filename) {
        def stream = new TarArchiveInputStream(new BufferedInputStream(tarContent))

        TarArchiveEntry entry = stream.nextTarEntry
        logger.debug("entry size: ${entry.size}")

        def entryName = entry.name
        if (!filename.endsWith(entryName)) {
            logger.warn("entry name '${entryName}' doesn't match expected filename '${filename}'")
        } else {
            logger.debug("entry name: ${entryName}")
        }

        byte[] content = new byte[(int) entry.size]
        logger.debug("going to read ${content.length} bytes")

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
