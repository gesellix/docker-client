package de.gesellix.docker.client.protocolhandler.content.application

import de.gesellix.docker.client.protocolhandler.contenthandler.RawInputStream
import spock.lang.Specification

class VndDockerRawStreamContentHandlerSpec extends Specification {

    def "should wrap the InputStream with a RawInputStream"() {
        given:
        def connection = Mock(URLConnection)
        def inputStream = Mock(InputStream)
        when:
        connection.inputStream >> inputStream
        inputStream.read() >> ((byte) 42)
        def rawInputStream = new vnd_docker_raw_stream().getContent(connection)
        then:
        rawInputStream instanceof RawInputStream
        and:
        rawInputStream.read() == 42
    }
}
