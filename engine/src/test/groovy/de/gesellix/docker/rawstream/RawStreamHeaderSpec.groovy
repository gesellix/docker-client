package de.gesellix.docker.rawstream

import spock.lang.Specification

class RawStreamHeaderSpec extends Specification {

    def "should parse a valid header"() {
        given:
        int[] bytes = [
                2, 0, 0, 0,
                0, 0, 0, 42
        ]

        when:
        def header = new RawStreamHeader(bytes)

        then:
        header.streamType == StreamType.STDERR
        and:
        header.frameSize == 42
    }
}
