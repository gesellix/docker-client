package de.gesellix.docker.client.protocolhandler.content.application

import spock.lang.Specification

class OctetStreamSpec extends Specification {

    def "delegates to original content (InputStream)"() {
        given:
        def expectedInputStream = Mock(InputStream)
        def connection = Mock(URLConnection)
        connection.inputStream >> expectedInputStream

        expect:
        new octet_stream().getContent(connection) == expectedInputStream
    }
}
