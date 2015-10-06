package de.gesellix.docker.client.protocolhandler

import de.gesellix.docker.client.protocolhandler.content.application.json
import de.gesellix.docker.client.protocolhandler.content.application.octet_stream
import de.gesellix.docker.client.protocolhandler.content.application.vnd_docker_raw_stream
import spock.lang.Specification
import spock.lang.Unroll
import sun.net.www.content.text.plain

class DockerContentHandlerFactorySpec extends Specification {
    DockerContentHandlerFactory contentHandlerFactory

    def setup() {
        contentHandlerFactory = new DockerContentHandlerFactory(false)
    }

    @Unroll
    def "can handle '#contentType'"() {
        expect:
        expectedContentHandlerType.isInstance(contentHandlerFactory.createContentHandler(contentType))

        where:
        contentType                         || expectedContentHandlerType
        "text/plain"                        || plain
        "text/plain; charset=utf-8"         || plain
        "text/html"                         || plain
        "application/json"                  || json
        "application/octet-stream"          || octet_stream
        "application/vnd.docker.raw-stream" || vnd_docker_raw_stream
    }

    @Unroll
    def "returns null for unknown content-type '#contentType'"() {
        expect:
        contentHandlerFactory.createContentHandler(contentType) == null

        where:
        contentType << ["unknown/type"]
    }
}
