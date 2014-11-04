package de.gesellix.docker.client

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient
import org.apache.commons.io.IOUtils
import org.apache.http.client.HttpClient
import org.codehaus.groovy.runtime.MethodClosure
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DockerClientImpl implements DockerClient {

  private static Logger logger = LoggerFactory.getLogger(DockerClientImpl)

  def responseHandler = new ChunkedResponseHandler()

  def dockerHost = "http://127.0.0.1:2375/"
  def delegate

  def getDelegate() {
    if (!delegate) {
      this.delegate = createDockerClient(dockerHost)
    }
    return delegate
  }

  def createDockerClient(String dockerHost) {
    def restClient = new RESTClient(dockerHost) {
      @Override
      HttpClient getClient() {
        def httpClient = DockerHttpClientFactory.createOldHttpClient(dockerHost)
        return httpClient
      }
    }
    restClient.with {
      handler.failure = new MethodClosure(responseHandler, "handleFailure")
      handler.success = new MethodClosure(responseHandler, "handleResponse")
    }
    logger.info "using docker at '${dockerHost}'"
    return restClient
  }

  @Override
  def info() {
    logger.info "get system info"
    getDelegate().get([path: "/info"]) { response, reader ->
      logger.info "${response.statusLine}"
      return reader
    }
  }

  @Override
  def version() {
    logger.info "get docker version"
    getDelegate().get([path: "/version"]) { response, reader ->
      logger.info "${response.statusLine}"
      return reader
    }
  }

  @Override
  def auth(def authDetails) {
    logger.info "auth..."
    getDelegate().post([path              : "/auth",
                        body              : authDetails,
                        requestContentType: ContentType.JSON
    ]) { response ->
      logger.info "${response.statusLine}"
      return response.statusLine.statusCode
    }
  }

  @Override
  def encodeAuthConfig(def authConfig) {
    logger.info "encodeAuthConfig..."
    return new JsonBuilder(authConfig).toString().bytes.encodeBase64().toString()
  }

  @Override
  def build(InputStream buildContext, query = ["rm": true]) {
    logger.info "build image..."
    getDelegate().post([path              : "/build",
                        query             : query,
                        body              : IOUtils.toByteArray(buildContext),
                        requestContentType: ContentType.BINARY])

    def lastResponseDetail = responseHandler.lastResponseDetail
    logger.info "${lastResponseDetail}"
    if (!responseHandler.success || lastResponseDetail?.error) {
      throw new DockerClientException(new IllegalStateException("build failed"), lastResponseDetail)
    }
    return lastResponseDetail.stream.trim() - "Successfully built "
  }

  @Override
  def tag(imageId, repository) {
    logger.info "tag image"
    def repoAndTag = parseRepositoryTag(repository)
    getDelegate().post([path : "/images/${imageId}/tag".toString(),
                        query: [repo : repoAndTag.repo,
                                tag  : repoAndTag.tag,
                                force: false]]) { response ->
      logger.info "${response.statusLine}"
      return response.statusLine.statusCode
    }
  }

  @Override
  def push(imageName, authBase64Encoded = ".", registry = "") {
    logger.info "push image '${imageName}'"

    def actualImageName = imageName
    if (registry) {
      actualImageName = "$registry/$imageName".toString()
      tag(imageName, actualImageName)
    }
    def repoAndTag = parseRepositoryTag(actualImageName)

    getDelegate().post([path   : "/images/${repoAndTag.repo}/push".toString(),
                        query  : [registry: registry,
                                  tag     : repoAndTag.tag],
                        headers: ["X-Registry-Auth": authBase64Encoded ?: "."]])

    def lastResponseDetail = responseHandler.lastResponseDetail
    logger.info "${lastResponseDetail}"
    return lastResponseDetail
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
    logger.info "pull image '${imageName}'..."

    def actualImageName = imageName
    if (registry) {
      actualImageName = "$registry/$imageName".toString()
    }

    getDelegate().post([path : "/images/create",
                        query: [fromImage: actualImageName,
                                tag      : tag,
                                registry : registry]])

    if (!responseHandler.success) {
      throw new DockerClientException(new IllegalStateException("pull failed."), responseHandler.lastResponseDetail)
    }
    def lastResponseDetail = responseHandler.lastResponseDetail
    logger.info "${lastResponseDetail}"
    def lastChunkWithId = responseHandler.responseChunks.findAll { it.id }.last()
    return lastChunkWithId.id
  }

  @Override
  def createContainer(containerConfig, query = [name: ""]) {
    logger.info "create container..."
    def actualContainerConfig = [:] + containerConfig

    getDelegate().post([path              : "/containers/create".toString(),
                        query             : query,
                        body              : actualContainerConfig,
                        requestContentType: ContentType.JSON]) { response, reader ->
      logger.info "${response.statusLine}"
      return reader
    }
  }

  @Override
  def startContainer(containerId, hostConfig = [:]) {
    logger.info "start container..."
    def actualHostConfig = [:] + hostConfig

    getDelegate().post([path              : "/containers/${containerId}/start".toString(),
                        body              : actualHostConfig,
                        requestContentType: ContentType.JSON]) { response, reader ->
      logger.info "${response.statusLine}"
      return response.statusLine.statusCode
    }
  }

  @Override
  def run(fromImage, containerConfig, hostConfig, tag = "", name = "") {
    logger.info "run container"
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

    pull(fromImage, tag)

    def containerInfo = createContainer(containerConfigWithImageName, [name: name])
    def result = startContainer(containerInfo.Id, hostConfig)
    return [
        container: containerInfo,
        status   : result
    ]
  }

  @Override
  def stop(containerId) {
    logger.info "stop container"
    getDelegate().post([path: "/containers/${containerId}/stop".toString()]) { response ->
      logger.info "${response.statusLine}"
      return response.statusLine.statusCode
    }
  }

  @Override
  def wait(containerId) {
    logger.info "wait container"
    getDelegate().post([path: "/containers/${containerId}/wait".toString()]) { response, reader ->
      logger.info "${response.statusLine}"
      return [status  : response.statusLine,
              response: reader]
    }
  }

  @Override
  def rm(containerId) {
    logger.info "rm container"
    def response = getDelegate().delete([path: "/containers/${containerId}".toString()])
    logger.info "${response.statusLine}"
    return response.statusLine.statusCode
  }

  @Override
  def rmi(imageId) {
    logger.info "rm image"
    def response = getDelegate().delete([path: "/images/${imageId}".toString()])
    logger.info "${response.statusLine}"
    return response.statusLine.statusCode
  }

  @Override
  def ps() {
    logger.info "list containers"
    getDelegate().get([path : "/containers/json",
                       query: [all : true,
                               size: false]]) { response, reader ->
      logger.info "${response.statusLine}"
      return reader
    }
  }

  @Override
  def inspectContainer(containerId) {
    logger.info "inspect container"
    getDelegate().get([path: "/containers/${containerId}/json"]) { response, reader ->
      logger.info "${response.statusLine}"
      return reader
    }
  }

  @Override
  def images(query = [all    : false,
                      filters: [:]]) {
    logger.info "list images"
    getDelegate().get([path : "/images/json",
                       query: query]) { response, reader ->
      logger.info "${response.statusLine}"
      return reader
    }
  }

  static class ChunkedResponseHandler {

    def jsonSlurper = new JsonSlurper()

    def success
    def statusLine
    def completeResponse
    def responseChunks = []

    def handleResponse(HttpResponseDecorator response) {
      logger.info "response: $response.statusLine"
      handle(response)
    }

    def handleFailure(HttpResponseDecorator response) {
      logger.error "failure: $response.statusLine"
      handle(response)
    }

    def handle(HttpResponseDecorator response) {
      success = response.success
      statusLine = response.statusLine
      completeResponse = readResponseBody(response)
      return response
    }

    def readResponseBody(HttpResponseDecorator response) {
      def completeResponse = ""
      if (response.entity) {
        new InputStreamReader(response.entity?.content).each { chunk ->
          logger.debug("received chunk: '${chunk}'")
          completeResponse += chunk
          if (chunk.contains("}{")) {
            responseChunks.addAll(chunk.split("\\}\\{").collect {
              it = it.startsWith("{") ? it : "{${it}".toString()
              it = it.endsWith("}") ? it : "${it}}".toString()
              logger.trace("splitted chunk: '${it}'")
              jsonSlurper.parseText(it)
            })
          } else {
            logger.trace("kept chunk: '${chunk}'")
            if (chunk.startsWith("{") && chunk.endsWith("}")) {
              responseChunks << jsonSlurper.parseText(chunk)
            } else {
              responseChunks << chunk
            }
          }
        }
      }
      return completeResponse
    }

    def getLastResponseDetail() {
      if (!success) {
        return completeResponse ?: statusLine
      }

      if (responseChunks) {
        logger.debug("find last detail in: '${responseChunks}'")
        def lastResponseDetail = responseChunks.last()
        return lastResponseDetail
      } else {
        return ""
      }
    }
  }
}
