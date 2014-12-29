package de.gesellix.docker.client

import groovy.json.JsonSlurper
import groovyx.net.http.HttpResponseDecorator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ChunkedResponseHandler {

  private static Logger logger = LoggerFactory.getLogger(ChunkedResponseHandler)

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

  def isDockerRawStream(response) {
    return ((HttpResponseDecorator) response).getHeaders("Content-Type").find {
      ((org.apache.http.message.BufferedHeader) it).value == "application/vnd.docker.raw-stream"
    }
  }

  def readResponseBody(HttpResponseDecorator response) {
    def completeResponse = ""
    if (response.entity) {
      def content = response.entity?.content
      if (isDockerRawStream(response)) {
        logger.warn("TODO: collect raw stream")
      }

      new InputStreamReader(content).each { chunk ->
        logger.debug("received chunk: '${chunk}'")
        completeResponse += chunk
        if (chunk.contains("}{")) {
          responseChunks.addAll(chunk.split("\\}\\{").collect {
            it = it.startsWith("{") ? it : "{${it}".toString()
            it = it.endsWith("}") ? it : "${it}}".toString()
            logger.trace("splitted chunk: '${it}'")
            jsonSlurper.parseText(it)
          })
        }
        else {
          logger.trace("kept chunk: '${chunk}'")
          if (chunk.startsWith("{") && chunk.endsWith("}")) {
            responseChunks << jsonSlurper.parseText(chunk)
          }
          else {
            responseChunks << ['plain': chunk]
          }
        }
      }
    }
    return completeResponse
  }

  def getLastResponseDetail() {
    def lastResponseDetail = ""
    if (!success) {
      lastResponseDetail = completeResponse ?: statusLine
    }
    else {
      if (responseChunks) {
        logger.debug("find last detail in: '${responseChunks}'")
        lastResponseDetail = responseChunks.last()
      }
    }

    logger.info "${lastResponseDetail}"
    return lastResponseDetail
  }
}
