package de.gesellix.docker.client

import groovy.json.JsonBuilder
import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient
import org.apache.commons.io.IOUtils
import org.apache.http.client.HttpClient
import org.codehaus.groovy.runtime.MethodClosure
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DockerClientImpl implements DockerClient {

  private static Logger logger = LoggerFactory.getLogger(DockerClientImpl)

  def responseHandler = new DockerResponseHandler()

  def dockerHost = "http://127.0.0.1:2375/"
  def delegate

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
    return responseHandler.lastResponseDetail
  }

  @Override
  def version() {
    logger.info "docker version"
    getDelegate().get([path: "/version"])
    return responseHandler.lastResponseDetail
  }

  @Override
  def auth(def authDetails) {
    logger.info "docker login"
    getDelegate().post([path              : "/auth",
                        body              : authDetails,
                        requestContentType: ContentType.JSON])
    return responseHandler.statusLine.statusCode
//    return responseHandler.lastResponseDetail
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

    def lastResponseDetail = responseHandler.lastResponseDetail
    if (!responseHandler.success || lastResponseDetail?.error) {
      throw new DockerClientException(new IllegalStateException("build failed"), lastResponseDetail)
    }
    return lastResponseDetail.stream.trim() - "Successfully built "
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
//    return responseHandler.lastResponseDetail
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
    return responseHandler.lastResponseDetail
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
    def lastResponseDetail = responseHandler.lastResponseDetail
    if (!responseHandler.success || lastResponseDetail?.error) {
      throw new DockerClientException(new IllegalStateException("pull failed."), lastResponseDetail)
    }
    def lastChunkWithId = responseHandler.responseChunks.findAll { it.id }.last()
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
        logger.warn "going to pull ${repoAndTag.repo}:${repoAndTag.tag}..."
        pull(repoAndTag.repo, repoAndTag.tag)
        // retry...
        getDelegate().post([path              : "/containers/create".toString(),
                            query             : query,
                            body              : actualContainerConfig,
                            requestContentType: ContentType.JSON])
        if (!responseHandler.success) {
          throw new DockerClientException(new IllegalStateException("create container failed."), [
              statusCode    : responseHandler.statusLine?.statusCode,
              responseDetail: responseHandler.lastResponseDetail])
        }
      }
    }
    return responseHandler.lastResponseDetail
  }

  @Override
  def startContainer(containerId, hostConfig = [:]) {
    logger.info "docker start"
    def actualHostConfig = [:] + hostConfig

    getDelegate().post([path              : "/containers/${containerId}/start".toString(),
                        body              : actualHostConfig,
                        requestContentType: ContentType.JSON])
    return responseHandler.statusLine.statusCode
//    return responseHandler.lastResponseDetail
  }

  @Override
  def run(fromImage, containerConfig, hostConfig, tag = "", name = "") {
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
    def result = startContainer(containerInfo.Id, hostConfig)
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
//    return responseHandler.lastResponseDetail
  }

  @Override
  def wait(containerId) {
    logger.info "docker wait"
    getDelegate().post([path: "/containers/${containerId}/wait".toString()]) { response, reader ->
      logger.info "${response.statusLine}"
      return [status  : response.statusLine,
              response: reader]
    }
  }

  @Override
  def rm(containerId) {
    logger.info "docker rm"
    getDelegate().delete([path: "/containers/${containerId}".toString()])
    return responseHandler.statusLine.statusCode
//    return responseHandler.lastResponseDetail
  }

  @Override
  def rmi(imageId) {
    logger.info "docker rmi"
    getDelegate().delete([path: "/images/${imageId}".toString()])
    return responseHandler.statusLine.statusCode
//    return responseHandler.lastResponseDetail
  }

  @Override
  def ps() {
    logger.info "docker ps"
    getDelegate().get([path : "/containers/json",
                       query: [all : true,
                               size: false]]) { response, reader ->
      logger.info "${response.statusLine}"
      return reader
    }
  }

  @Override
  def inspectContainer(containerId) {
    logger.info "docker inspect"
    getDelegate().get([path: "/containers/${containerId}/json"]) { response, reader ->
      logger.info "${response.statusLine}"
      return reader
    }
  }

  @Override
  def images(query = [all    : false,
                      filters: [:]]) {
    logger.info "docker images"
    getDelegate().get([path : "/images/json",
                       query: query]) { response, reader ->
      logger.info "${response.statusLine}"
      return reader
    }
  }

  @Override
  def createExec(containerId, execConfig) {
    logger.info "docker create exec on '${containerId}'"

    getDelegate().post([path              : "/containers/${containerId}/exec".toString(),
                        body              : execConfig,
                        requestContentType: ContentType.JSON])


    if (responseHandler.statusLine?.statusCode == 404) {
      throw new DockerClientException(new IllegalStateException("no such container ${containerId}"), [
          statusCode    : responseHandler.statusLine?.statusCode,
          responseDetail: responseHandler.lastResponseDetail])
    }
    if (!responseHandler.success) {
      logger.error("create exec with container ${containerId} failed.")
    }
    return responseHandler.lastResponseDetail
  }

  @Override
  def startExec(execId, execConfig) {
    logger.info "docker start exec '${execId}'"

    getDelegate().post([path              : "/exec/${execId}/start".toString(),
                        body              : execConfig,
                        requestContentType: ContentType.JSON])


    if (responseHandler.statusLine?.statusCode == 404) {
      throw new DockerClientException(new IllegalStateException("no such exec ${execId}"), [
          statusCode    : responseHandler.statusLine?.statusCode,
          responseDetail: responseHandler.lastResponseDetail])
    }
    if (!responseHandler.success) {
      logger.error("start exec ${execId} failed.")
    }
    return responseHandler.lastResponseDetail
  }

  @Override
  def exec(containerId, command, execConfig = [
      "Detach"     : false,
      "AttachStdin": false,
      "Tty"        : false]) {
    logger.info "docker exec '${containerId}'"

    def actualExecConfig = [
        "AttachStdin" : execConfig.AttachStdin ?: false,
        "AttachStdout": true,
        "AttachStderr": true,
        "Detach"      : execConfig.Detach ?: false,
        "Tty"         : execConfig.Tty ?: false,
        "Cmd"         : command]

    logger.info "docker exec '${containerId}'"
    def execCreateResult = createExec(containerId, actualExecConfig)
    def execId = execCreateResult.Id
    return startExec(execId, actualExecConfig)
  }
}
