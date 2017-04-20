package de.gesellix.docker.client

import groovy.util.logging.Slf4j

@Slf4j
class DockerResponseHandler {

    def ensureSuccessfulResponse(DockerResponse response, Throwable context) {
        if (!response || !response.status?.success || hasError(response)) {
            logError(response)
            throw new DockerClientException(context, response)
        }
    }

    def logError(response) {
        if (response?.content instanceof String) {
            log.error "request failed: '${response?.content}'"
        } else {
            log.error "request failed: ${getErrors(response)}"
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
            } else if (content instanceof Map) {
                if (content.error) {
                    foundErrors << content.error
                } else if (content.message) {
                    foundErrors << content.message
                }
            } else {
                log.debug("won't search for errors in ${content.getClass()}")
            }
            return foundErrors
        }
        return []
    }
}
