package de.gesellix.docker.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DockerResponseHandler {

  final Logger logger = LoggerFactory.getLogger(DockerResponseHandler)

  def ensureSuccessfulResponse(def response, Throwable context) {
    if (!response || !response.status.success || hasError(response)) {
      logError(response)
      throw new DockerClientException(context, response)
    }
  }

  def logError(response){
    if (response?.content instanceof String){
      logger.error "request failed: ${response?.content}"
    } else {
      logger.error "request failed: ${getErrors(response)}"
    }
  }

  def hasError(response) {
    return getErrors(response).size()
  }

  def getErrors(response) {
    if (!response?.content) {
      return []
    }

    def content = response.content
    if (!response.mimeType || response.mimeType == "application/json") {
      def foundErrors = []
      if (content instanceof List) {
        foundErrors.addAll content.findAll { it.error }
      } else if (content instanceof Map && content.error) {
        foundErrors << content.error
      } else {
        logger.warn("won't search for errors in ${content.class}")
      }
      return foundErrors
    }
    return []
  }
}
