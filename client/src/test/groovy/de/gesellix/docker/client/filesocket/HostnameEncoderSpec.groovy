package de.gesellix.docker.client.filesocket

import de.gesellix.docker.client.filesocket.HostnameEncoder
import spock.lang.Specification

class HostnameEncoderSpec extends Specification {

    def "encode String"() {
        given:
        def label = "npipe:////./pipe/docker_engine"
        when:
        def encoded = HostnameEncoder.encode(label)
        then:
        encoded == "6e706970653a2f2f2f2f2e2f706970652f646f636b65725f656e67696e65"
    }

    def "decode String"() {
        given:
        def encoded = "6e706970653a2f2f2f2f2e2f706970652f646f636b65725f656e67696e65"
        when:
        def label = HostnameEncoder.decode(encoded)
        then:
        label == "npipe:////./pipe/docker_engine"
    }

    def "encode, then split long label"() {
        given:
        def label = "C:\\Users\\gesellix\\AppData\\Local\\Temp\\named-pipe9191419262972291772.tmp"
        when:
        def encoded = HostnameEncoder.encode(label)
        then:
        encoded == "433a5c55736572735c676573656c6c69785c417070446174615c4c6f63616c5.c54656d705c6e616d65642d7069706539313931343139323632393732323931.3737322e746d70"
    }

    def "decode splitted label"() {
        given:
        def encoded = "433a5c55736572735c676573656c6c69785c417070446174615c4c6f63616c5.c54656d705c6e616d65642d7069706539313931343139323632393732323931.3737322e746d70"
        when:
        def label = HostnameEncoder.decode(encoded)
        then:
        label == "C:\\Users\\gesellix\\AppData\\Local\\Temp\\named-pipe9191419262972291772.tmp"
    }
}
