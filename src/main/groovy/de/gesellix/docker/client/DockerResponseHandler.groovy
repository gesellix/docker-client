package de.gesellix.docker.client

import groovy.json.JsonSlurper
import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator
import org.codehaus.groovy.runtime.MethodClosure
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DockerResponseHandler {

  private static Logger logger = LoggerFactory.getLogger(DockerResponseHandler)

  def jsonSlurper = new JsonSlurper()

  def success
  def statusLine
  def completeResponse
  def chunks = []

  def handleSuccess(HttpResponseDecorator response) {
    logger.info "success: $response.statusLine"
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
      def contentType = getContentType(response)
      def reader = contentTypeReaders()[contentType]
      if (!reader) {
        throw new IllegalStateException("no reader for '${contentType}' found.")
      }
      completeResponse = reader(response)
    }
    return completeResponse
  }

  def ensureSuccessfulResponse(Throwable context) {
    if (!success || lastChunk?.error) {
      throw new DockerClientException(context, lastChunk);
    }
  }

  def getLastChunk() {
    def lastChunk
    if (!success) {
      logger.warn("failed request")
      lastChunk = completeResponse ?: statusLine
    }
    else if (chunks) {
      logger.debug("find last chunk in: '${chunks}'")
      lastChunk = chunks.last()
    }
    else {
      lastChunk = ""
    }

    logger.info "${lastChunk}"
    return lastChunk
  }

  def getContentType(response) {
//    def contentTypeHeaders = response.headers."Content-Type"
    def contentTypeHeaders = ((HttpResponseDecorator) response).getHeaders("Content-Type")
    if (contentTypeHeaders.length == 0) {
      return ContentType.ANY.toString()
    }
    else if (contentTypeHeaders.length > 1) {
      return ContentType.ANY.toString()
    }
    else {
      def uniqueContentType = contentTypeHeaders.first()
      def elements = uniqueContentType.elements
      def firstElement = elements.first()
      return firstElement.name
    }
  }

  def contentTypeReaders() {
    return [
        "application/vnd.docker.raw-stream": new MethodClosure(this, "readRawDockerStream"),
        "application/json"                 : new MethodClosure(this, "readJson"),
        "text/plain"                       : new MethodClosure(this, "readText"),
        "text/html"                        : new MethodClosure(this, "readText"),
        "*/*"                              : new MethodClosure(this, "readText")
    ]
  }

  def readJson(response) {
    def content = response.entity.content
    def text = content.text
    text = text.replaceAll("\\}[\n\r]*\\{", "},{")
    def parsedJson = jsonSlurper.parseText("[$text]")
    chunks.addAll(parsedJson)
    return text
  }

  def readText(response) {
    def content = response.entity.content
    def text = content.text
    chunks << ['plain': text]
    return text
  }

  def readRawDockerStream(response) {
    logger.warn("TODO: collect raw stream")
    return readText(response)
  }
}
