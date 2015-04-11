package de.gesellix.docker.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DockerResponseHandler {

  final Logger logger = LoggerFactory.getLogger(DockerResponseHandler)

  def ensureSuccessfulResponse(def response, Throwable context) {
    if (!response || !response.status.success || hasError(response)) {
      logger.error "request failed: ${response?.content?.error}"
      throw new DockerClientException(context, response?.content)
    }
  }

  def hasError(response) {
    if (!response?.content) {
      return false
    }

    def content = response.content

    switch (response.mimeType) {
      case "application/vnd.docker.raw-stream":
        return false
      case "application/json":
        def foundErrors = false
        if (content instanceof List) {
          foundErrors = content.find { it.error }
        }
        else if (content instanceof Map) {
          foundErrors = content.error ?: null
        }
        else {
          logger.warn("won't search for errors in ${content.class}")
        }
        return foundErrors ? true : false
      case "text/html":
        return content.error
      case "text/plain":
        return content.error
    }
    return false
  }
}
