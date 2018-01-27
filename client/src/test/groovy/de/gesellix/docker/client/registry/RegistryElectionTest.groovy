package de.gesellix.docker.client.registry

import de.gesellix.docker.client.authentication.ManageAuthentication
import de.gesellix.docker.client.system.ManageSystem
import de.gesellix.docker.engine.EngineResponse
import spock.lang.Specification

class RegistryElectionTest extends Specification {

    RegistryElection election
    ManageSystem system = Mock(ManageSystem)
    ManageAuthentication authentication = Mock(ManageAuthentication)

    def setup() {
        election = new RegistryElection(system, authentication)
    }

    def "leaves non-official index name unchanged"() {
        given:
        def expectedConfig = [key: "value"]

        when:
        def actualConfig = election.resolveAuthConfig("private.registry", false)

        then:
        1 * authentication.readAuthConfig("private.registry", null) >> expectedConfig
        actualConfig == expectedConfig
    }

    def "elects v1 server url if system info fails"() {
        given:
        def expectedConfig = [key: "value"]

        when:
        def actualConfig = election.resolveAuthConfig("official.registry", true)

        then:
        1 * system.info() >> { throw new RuntimeException("for-test") }
        1 * authentication.readAuthConfig("https://index.docker.io/v1/", null) >> expectedConfig
        actualConfig == expectedConfig
    }

    def "elects v1 server url if system info doesn't provide an `IndexServerAddress`"() {
        given:
        def expectedConfig = [key: "value"]

        when:
        def actualConfig = election.resolveAuthConfig("official.registry", true)

        then:
        1 * system.info() >> new EngineResponse(content: [IndexServerAddress: ""])
        1 * authentication.readAuthConfig("https://index.docker.io/v1/", null) >> expectedConfig
        actualConfig == expectedConfig
    }

    def "elects the platform's IndexServerAddress"() {
        given:
        def expectedConfig = [key: "value"]

        when:
        def actualConfig = election.resolveAuthConfig("official.registry", true)

        then:
        1 * system.info() >> new EngineResponse(content: [IndexServerAddress: "platform.registry"])
        1 * authentication.readAuthConfig("platform.registry", null) >> expectedConfig
        actualConfig == expectedConfig
    }
}
