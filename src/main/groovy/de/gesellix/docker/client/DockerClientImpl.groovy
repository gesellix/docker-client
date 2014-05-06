package de.gesellix.docker.client

import groovy.json.JsonSlurper
import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient
import org.apache.commons.io.IOUtils
import org.codehaus.groovy.runtime.MethodClosure
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DockerClientImpl implements DockerClient {

  private static Logger logger = LoggerFactory.getLogger(DockerClientImpl)

  def hostname
  def port
  def delegate

  DockerClientImpl(hostname = "172.17.42.1", port = 4243) {
    this.hostname = hostname
    this.port = port

    def dockerUri = "http://$hostname:$port/"
    this.delegate = new RESTClient(dockerUri)
    logger.info "using docker at '${dockerUri}'"
  }

  @Override
  def auth(def authDetails) {
    logger.info "auth..."
    delegate.post([path: "/auth",
                 body              : authDetails,
                 requestContentType: ContentType.JSON
    ]) { response ->
      logger.info "${response.statusLine}"
      return response.statusLine.statusCode
    }
  }

  @Override
  def build(InputStream buildContext) {
    logger.info "build image..."
    def responseHandler = new ChunkedResponseHandler()
    delegate.handler.'200' = new MethodClosure(responseHandler, "handleResponse")
    delegate.post([path: "/build",
                 body              : IOUtils.toByteArray(buildContext),
                 requestContentType: ContentType.BINARY])

    def lastResponseDetail = responseHandler.lastResponseDetail
    logger.info "${lastResponseDetail}"
    return lastResponseDetail.stream.trim()
  }

  @Override
  def tag(imageId, repositoryName) {
    logger.info "tag image"
    delegate.post([path: "/images/${imageId}/tag".toString(),
                 query: [repo : repositoryName,
                         force: 0]]) { response ->
      logger.info "${response.statusLine}"
      return response.statusLine.statusCode
    }
  }

  @Override
  def push(repositoryName, authBase64Encoded) {
    logger.info "push image '${repositoryName}'"

    def responseHandler = new ChunkedResponseHandler()
    delegate.handler.'200' = new MethodClosure(responseHandler, "handleResponse")
    delegate.post([path: "/images/${repositoryName}/push".toString(),
                 headers: ["X-Registry-Auth": authBase64Encoded]])

    def lastResponseDetail = responseHandler.lastResponseDetail
    logger.info "${lastResponseDetail}"
    return lastResponseDetail
  }

  @Override
  def pull(imageName) {
    logger.info "pull image '${imageName}'..."

    def responseHandler = new ChunkedResponseHandler()
    delegate.handler.'200' = new MethodClosure(responseHandler, "handleResponse")
    delegate.post([path: "/images/create",
                 query: [fromImage: imageName]])

    def lastResponseDetail = responseHandler.lastResponseDetail
    logger.info "${lastResponseDetail}"
    return lastResponseDetail.id
  }

  @Override
  def createContainer(containerConfig) {
    logger.info "create container..."
    delegate.post([path: "/containers/create".toString(),
                   body: containerConfig,
                 requestContentType: ContentType.JSON]) { response, reader ->
      logger.info "${response.statusLine}"
      return reader
    }
  }

  @Override
  def startContainer(containerId) {
    logger.info "start container..."
    delegate.post([path: "/containers/${containerId}/start".toString(),
                 body              : [:],
                 requestContentType: ContentType.JSON]) { response, reader ->
      logger.info "${response.statusLine}"
      return response.statusLine.statusCode
    }
  }

  @Override
  def run(fromImage, cmds) {
    logger.info "run container"
/*
    http://docs.docker.io/reference/api/docker_remote_api_v1.10/#3-going-further

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
    def containerConfig = ["Hostname"      : "",
                           "User"          : "",
                           "Memory"        : 0,
                           "MemorySwap"    : 0,
                           "AttachStdin"   : false,
                           "AttachStdout"  : true,
                           "AttachStderr"  : true,
                           "PortSpecs"     : null,
                           "Tty"           : false,
                           "OpenStdin"     : false,
                           "StdinOnce"     : false,
                           "Env"           : null,
                           "Cmd"           : cmds,
                           "Image"         : fromImage,
                           "Volumes"       : [],
                           "WorkingDir"    : "",
                           "DisableNetwork": false,
                           "ExposedPorts"  : [
                               "DisableNetwork": false
                           ]]

    pull(fromImage)

    def containerInfo = createContainer(containerConfig)
    def result = startContainer(containerInfo.Id)
    return [
        container: containerInfo,
        status   : result
    ]
  }

  @Override
  def stop(containerId) {
    logger.info "stop container"
    delegate.post([path: "/containers/${containerId}/stop".toString()]) { response ->
      logger.info "${response.statusLine}"
      return response.statusLine.statusCode
    }
  }

  @Override
  def rm(containerId) {
    logger.info "rm container"
    def response = delegate.delete([path: "/containers/${containerId}".toString()])
    logger.info "${response.statusLine}"
    return response.statusLine.statusCode
  }

  @Override
  def rmi() {
    logger.info "rm image"
  }

  @Override
  def ps() {
    logger.info "list containers"
    def responseHandler = new ChunkedResponseHandler()
    delegate.handler.'200' = new MethodClosure(responseHandler, "handleResponse")
    delegate.get([path: "/containers/json",
                query: [all : true,
                        size: true]])

    def completeResponse = responseHandler.completeResponse
    def containersAsJson = new JsonSlurper().parseText(completeResponse)
    logger.info "${containersAsJson}"
    return containersAsJson
  }

  @Override
  def images() {
    logger.info "list images"
    delegate.get([path: "/images/json",
                query: [all: 0]]) { response, reader ->
      logger.info "${response.statusLine}"
      return reader
    }
  }

  static class ChunkedResponseHandler {

    def completeResponse = ""

    def handleResponse(HttpResponseDecorator response) {
      new InputStreamReader(response.entity.content).each { chunk ->
        logger.debug("received chunk: '${chunk}'")
        completeResponse += chunk
      }
    }

    def completeResponse() {
      return completeResponse
    }

    def getLastResponseDetail() {
      logger.debug("find last detail in: '${completeResponse}'")
      def lastResponseDetail = completeResponse.substring(completeResponse.lastIndexOf("}{") + 1)
      return new JsonSlurper().parseText(lastResponseDetail)
    }
  }
}
