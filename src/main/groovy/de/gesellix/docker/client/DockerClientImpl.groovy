package de.gesellix.docker.client

import groovy.json.JsonBuilder
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.IOUtils
import org.codehaus.groovy.runtime.MethodClosure
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DockerClientImpl implements DockerClient {

  def Logger logger = LoggerFactory.getLogger(DockerClientImpl)

  def responseHandler = new DockerResponseHandler()

  def dockerHost = "http://127.0.0.1:2375/"
  LowLevelDockerClient httpClient
  def Closure newDockerHttpClient

  DockerClientImpl() {
    newDockerHttpClient = new MethodClosure(this, "createDockerHttpClient")
  }

  def getHttpClient() {
    if (!httpClient) {
      this.httpClient = newDockerHttpClient(dockerHost)
      logger.info "using docker at '${dockerHost}'"
    }
    return httpClient
  }

  def createDockerHttpClient(dockerHost) {
    return new LowLevelDockerClient(dockerHost: dockerHost)
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
  def auth(def authDetails) {
    logger.info "docker login"
    def response = getHttpClient().post([path              : "/auth",
                                         body              : authDetails,
                                         requestContentType: "application/json"])
    return response
  }

  @Override
  def encodeAuthConfig(def authConfig) {
    logger.debug "encode authConfig"
    return new JsonBuilder(authConfig).toString().bytes.encodeBase64().toString()
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
                                         query  : [registry: registry,
                                                   tag     : repoAndTag.tag],
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
  def pull(imageName, tag = "", registry = "") {
    logger.info "docker pull '${imageName}:${tag}'"

    def actualImageName = imageName
    if (registry) {
      actualImageName = "$registry/$imageName".toString()
    }

    def response = getHttpClient().post([path : "/images/create",
                                         query: [fromImage: actualImageName,
                                                 tag      : tag,
                                                 registry : registry]])
    responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker pull failed"))

    def chunksWithId = response.content.findAll { it.id }
    if (chunksWithId.empty) {
      throw new DockerClientException(new IllegalStateException("cannot find 'id' in response"))
    }
    def lastChunkWithId = chunksWithId.last()
    return lastChunkWithId.id
  }

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
        logger.warn "'${repoAndTag.repo}:${repoAndTag.tag}' not found."
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
  def ps() {
    logger.info "docker ps"
    def response = getHttpClient().get([path : "/containers/json",
                                        query: [all : true,
                                                size: false]])
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
  def images(query = [all    : false,
                      filters: '']) {
    logger.info "docker images"
    def response = getHttpClient().get([path : "/images/json",
                                        query: query])
    return response
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

  @Override
  def copyFile(containerId, String filename) {
    logger.info "copy '${filename}' from '${containerId}'"

    def response = copy(containerId, [Resource: filename])
    return extractSingleTarEntry(response.stream as InputStream, filename)
  }

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

  def extractSingleTarEntry(InputStream tarContent, String filename) {
    def stream = new TarArchiveInputStream(new BufferedInputStream(tarContent))

    TarArchiveEntry entry = stream.nextTarEntry
    logger.debug("entry size: ${entry.size}")

    def entryName = entry.name
    logger.debug("entry name: ${entryName}")

    byte[] content = new byte[(int) entry.size]
    logger.debug("going to read ${content.length} bytes")

    stream.read(content, 0, content.length)
    IOUtils.closeQuietly(stream)

    return content
  }
}
