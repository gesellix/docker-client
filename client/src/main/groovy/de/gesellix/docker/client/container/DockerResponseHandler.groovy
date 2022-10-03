package de.gesellix.docker.client.container

import de.gesellix.docker.engine.EngineResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DockerResponseHandler {

  private final Logger log = LoggerFactory.getLogger(DockerResponseHandler)

  void ensureSuccessfulResponse(EngineResponse response, Throwable throwable) {
    if (response == null || !response.status.success || hasError(response)) {
      logError(response)
      throw throwable
    }
  }

  void logError(EngineResponse response) {
    if (response != null && response.content instanceof String) {
      log.error("request failed: '${response.content}'")
    }
    else {
      log.error("request failed: ${getErrors(response)}")
    }
  }

  boolean hasError(EngineResponse response) {
    return getErrors(response).size() > 0
  }

  List getErrors(EngineResponse response) {
    if (response == null || response.content == null) {
      return Collections.emptyList()
    }

    if (response.mimeType == null || response.mimeType == "application/json") {
      List foundErrors = new ArrayList()
      if (response.content instanceof List) {
        List content = (List) response.content
        foundErrors.addAll(content.findAll { element ->
          if (element instanceof Map) {
            element.get("error")
          }
          else {
            element?.toString()
          }
        })
      }
      else if (response.content instanceof Map) {
        Map content = (Map) response.content
        if (content.containsKey("error")) {
          foundErrors.add(content.get("error"))
        }
        else if (content.containsKey("message")) {
          foundErrors.add(content.get("message"))
        }
      }
      else {
        log.debug("won't search for errors in ${response.content.getClass()}")
      }
      return foundErrors
    }

    return Collections.emptyList()
  }
}
