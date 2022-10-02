package de.gesellix.docker.client.container

import de.gesellix.docker.client.DockerClientException
import de.gesellix.docker.engine.EngineResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DockerResponseHandler {

  private final Logger log = LoggerFactory.getLogger(DockerResponseHandler)

  def ensureSuccessfulResponse(EngineResponse response, Throwable context) {
    if (!response || !response.status?.success || hasError(response)) {
      logError(response)
      throw new DockerClientException(context, response)
    }
  }

  void logError(EngineResponse response) {
    if (response?.content instanceof String) {
      log.error("request failed: '${response?.content}'")
    }
    else {
      log.error("request failed: ${getErrors(response)}")
    }
  }

  boolean hasError(EngineResponse response) {
    return getErrors(response).size() > 0
  }

  List getErrors(EngineResponse response) {
    if (!response?.content) {
      return Collections.emptyList()
    }

    if (!response.mimeType || response.mimeType == "application/json") {
      List foundErrors = new ArrayList()
      if (response.content instanceof List) {
        foundErrors.addAll(response.content.findAll { it.error })
      }
      else if (response.content instanceof Map) {
        if (response.content.error) {
          foundErrors << response.content.error
        }
        else if (response.content.message) {
          foundErrors << response.content.message
        }
      }
      else {
        log.debug("won't search for errors in ${response.content?.getClass()}")
      }
      return foundErrors
    }

    return Collections.emptyList()
  }
}
