package de.gesellix.docker.engine

import groovy.transform.ToString

import java.util.concurrent.Future

@ToString(includeNames = true)
class EngineResponse {

    EngineResponseStatus status = new EngineResponseStatus()
    def headers
    def contentType
    def mimeType
    def contentLength
    def stream
    def content

    Future taskFuture
}
