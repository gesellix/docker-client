package de.gesellix.docker.client

import groovy.json.JsonBuilder
import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.IOUtils
import org.apache.http.client.HttpClient
import org.codehaus.groovy.runtime.MethodClosure
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DockerClientImpl implements DockerClient {

  def Logger logger = LoggerFactory.getLogger(DockerClientImpl)

  def responseHandler = new DockerResponseHandler()

  def dockerHost = "http://127.0.0.1:2375/"
  RESTClient delegate

  def getDelegate() {
    if (!delegate) {
      this.delegate = createDockerClient(dockerHost)
    }
    return delegate
  }

  def createDockerClient(String dockerHost) {
    def httpClientFactory = new DockerHttpClientFactory(dockerHost)
    dockerHost = httpClientFactory.sanitizedUri
    def restClient = new RESTClient(dockerHost) {

      private client

      @Override
      HttpClient getClient() {
        if (client == null) {
          this.client = httpClientFactory.createOldHttpClient()
        }
        return this.client
      }
    }
    restClient.with {
      handler.failure = new MethodClosure(responseHandler, "handleFailure")
      handler.success = new MethodClosure(responseHandler, "handleSuccess")
    }
    logger.info "using docker at '${dockerHost}'"
    return restClient
  }

  @Override
  def info() {
    logger.info "docker info"
    getDelegate().get([path: "/info"])
    return responseHandler.lastChunk
  }

  @Override
  def version() {
    logger.info "docker version"
    getDelegate().get([path: "/version"])
    return responseHandler.lastChunk
  }

  @Override
  def auth(def authDetails) {
    logger.info "docker login"
    getDelegate().post([path              : "/auth",
                        body              : authDetails,
                        requestContentType: ContentType.JSON])
    return responseHandler.statusLine.statusCode
//    return responseHandler.lastChunk
  }

  @Override
  def encodeAuthConfig(def authConfig) {
    logger.debug "encode authConfig"
    return new JsonBuilder(authConfig).toString().bytes.encodeBase64().toString()
  }

  @Override
  def build(InputStream buildContext, query = ["rm": true]) {
    logger.info "docker build"
    getDelegate().post([path              : "/build",
                        query             : query,
                        body              : IOUtils.toByteArray(buildContext),
                        requestContentType: ContentType.BINARY])

    responseHandler.ensureSuccessfulResponse(new IllegalStateException("docker build failed"))
    def lastChunk = responseHandler.lastChunk
    return lastChunk.stream.trim() - "Successfully built "
  }

  @Override
  def tag(imageId, repository, force = false) {
    logger.info "docker tag"
    def repoAndTag = parseRepositoryTag(repository)
    getDelegate().post([path : "/images/${imageId}/tag".toString(),
                        query: [repo : repoAndTag.repo,
                                tag  : repoAndTag.tag,
                                force: force]])
    return responseHandler.statusLine.statusCode
//    return responseHandler.lastChunk
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

    getDelegate().post([path   : "/images/${repoAndTag.repo}/push".toString(),
                        query  : [registry: registry,
                                  tag     : repoAndTag.tag],
                        headers: ["X-Registry-Auth": authBase64Encoded ?: "."]])
    responseHandler.ensureSuccessfulResponse(new IllegalStateException("docker push failed"))
    return responseHandler.lastChunk
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

    getDelegate().post([path : "/images/create",
                        query: [fromImage: actualImageName,
                                tag      : tag,
                                registry : registry]])
    responseHandler.ensureSuccessfulResponse(new IllegalStateException("docker pull failed"))
    def lastChunkWithId = responseHandler.chunks.findAll { it.id }.last()
    return lastChunkWithId.id
  }

  @Override
  def createContainer(containerConfig, query = [name: ""]) {
    logger.info "docker create"
    def actualContainerConfig = [:] + containerConfig

    getDelegate().post([path              : "/containers/create".toString(),
                        query             : query,
                        body              : actualContainerConfig,
                        requestContentType: ContentType.JSON])

    if (!responseHandler.success) {
      if (responseHandler.statusLine?.statusCode == 404) {
        def repoAndTag = parseRepositoryTag(containerConfig.Image)
        logger.warn "'${repoAndTag.repo}:${repoAndTag.tag}' not found."
        pull(repoAndTag.repo, repoAndTag.tag)
        // retry...
        getDelegate().post([path              : "/containers/create".toString(),
                            query             : query,
                            body              : actualContainerConfig,
                            requestContentType: ContentType.JSON])
        responseHandler.ensureSuccessfulResponse(new IllegalStateException("docker create failed"))
//        if (!responseHandler.success) {
//          throw new DockerClientException(new IllegalStateException("create container failed."), [
//              statusCode    : responseHandler.statusLine?.statusCode,
//              responseDetail: responseHandler.lastChunk])
//        }
      }
    }
    return responseHandler.lastChunk
  }

  @Override
  def startContainer(containerId) {
    logger.info "docker start"
    getDelegate().post([path              : "/containers/${containerId}/start".toString(),
                        requestContentType: ContentType.JSON])
    return responseHandler.statusLine.statusCode
//    return responseHandler.lastChunk
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

    def containerInfo = createContainer(containerConfigWithImageName, [name: name])
    def result = startContainer(containerInfo.Id)
    return [
        container: containerInfo,
        status   : result
    ]
  }

  @Override
  def stop(containerId) {
    logger.info "docker stop"
    getDelegate().post([path: "/containers/${containerId}/stop".toString()])
    return responseHandler.statusLine.statusCode
//    return responseHandler.lastChunk
  }

  @Override
  def wait(containerId) {
    logger.info "docker wait"
    getDelegate().post([path: "/containers/${containerId}/wait".toString()])
    return [status  : responseHandler.statusLine,
            response: responseHandler.lastChunk]
  }

  @Override
  def rm(containerId) {
    logger.info "docker rm"
    getDelegate().delete([path: "/containers/${containerId}".toString()])
    return responseHandler.statusLine.statusCode
//    return responseHandler.lastChunk
  }

  @Override
  def rmi(imageId) {
    logger.info "docker rmi"
    getDelegate().delete([path: "/images/${imageId}".toString()])
    return responseHandler.statusLine.statusCode
//    return responseHandler.lastChunk
  }

  @Override
  def ps() {
    logger.info "docker ps"
    getDelegate().get([path : "/containers/json",
                       query: [all : true,
                               size: false]])
    return responseHandler.lastChunk
  }

  @Override
  def inspectContainer(containerId) {
    logger.info "docker inspect"
    getDelegate().get([path: "/containers/${containerId}/json"])
    return responseHandler.lastChunk
  }

  @Override
  def images(query = [all    : false,
                      filters: [:]]) {
    logger.info "docker images"
    getDelegate().get([path : "/images/json",
                       query: query])
    return responseHandler.lastChunk
  }

  @Override
  def createExec(containerId, execConfig) {
    logger.info "docker create exec on '${containerId}'"

    getDelegate().post([path              : "/containers/${containerId}/exec".toString(),
                        body              : execConfig,
                        requestContentType: ContentType.JSON])


    if (responseHandler.statusLine?.statusCode == 404) {
      logger.error("no such container '${containerId}'")
    }
    responseHandler.ensureSuccessfulResponse(new IllegalStateException("docker exec create failed"))
    return responseHandler.lastChunk
  }

  @Override
  def startExec(execId, execConfig) {
    logger.info "docker start exec '${execId}'"

    getDelegate().post([path              : "/exec/${execId}/start".toString(),
                        body              : execConfig,
                        requestContentType: ContentType.JSON])


    if (responseHandler.statusLine?.statusCode == 404) {
      logger.error("no such exec '${execId}'")
    }
    responseHandler.ensureSuccessfulResponse(new IllegalStateException("docker exec start failed"))
    return responseHandler.lastChunk
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
    def execId = execCreateResult.Id
    return startExec(execId, actualExecConfig)
  }

  @Override
  def copyFile(containerId, String filename) {
    logger.info "copy '${filename}' from '${containerId}'"

    def fileAsTar = copy(containerId, [Resource: filename])
    return extractSingleTarEntry(fileAsTar as byte[], filename)
  }

  @Override
  def copy(containerId, resourceBody) {
    logger.info "docker cp ${containerId} ${resourceBody}"

    getDelegate().post([path              : "/containers/${containerId}/copy".toString(),
                        body              : resourceBody,
                        requestContentType: ContentType.JSON])

    if (responseHandler.statusLine?.statusCode == 404) {
      logger.error("no such container ${containerId}")
    }
    responseHandler.ensureSuccessfulResponse(new IllegalStateException("docker cp failed"))
    return responseHandler.lastChunk.raw
  }

  @Override
  def rename(containerId, newName) {
    logger.info "docker rename"
    getDelegate().post([path : "/containers/${containerId}/rename".toString(),
                        query: [name: newName]])
    return responseHandler.statusLine.statusCode
  }

  def extractSingleTarEntry(byte[] tarContent, String filename) {
    def stream = new TarArchiveInputStream(new BufferedInputStream(new ByteArrayInputStream(tarContent)))

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
