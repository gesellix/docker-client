package de.gesellix.docker.client.protocolhandler.urlstreamhandler

import spock.lang.Specification

class UnixSocketURLStreamHandlerSpec extends Specification {

    def "openConnection should return a HttpOverUnixSocketURLConnection"() {
        given:
        def handler = new UnixSocketURLStreamHandler() {}

        when:
        def connection = handler.openConnection(new URL("http://example.com"))

        then:
        connection instanceof HttpOverUnixSocketURLConnection
    }
}
