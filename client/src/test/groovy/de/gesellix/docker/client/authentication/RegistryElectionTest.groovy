package de.gesellix.docker.client.authentication

import de.gesellix.docker.authentication.AuthConfig
import de.gesellix.docker.authentication.AuthConfigReader
import de.gesellix.docker.remote.api.SystemInfo
import spock.lang.Specification

class RegistryElectionTest extends Specification {

  RegistryElection election
  AuthConfigReader authConfigReader = Mock(AuthConfigReader)

  def setup() {
    election = new RegistryElection(authConfigReader)
  }

  def "leaves non-official index name unchanged"() {
    given:
    def expectedConfig = new AuthConfig(username: "foo-bar")

    when:
    def actualConfig = election.resolveAuthConfig("private.registry", false)

    then:
    1 * authConfigReader.readAuthConfig("private.registry", null) >> expectedConfig
    actualConfig == expectedConfig
  }

  def "elects v1 server url if system info fails"() {
    given:
    def expectedConfig = new AuthConfig(username: "bar-baz")

    when:
    def actualConfig = election.resolveAuthConfig("official.registry", true)

    then:
    1 * authConfigReader.readAuthConfig("https://index.docker.io/v1/", null) >> expectedConfig
    actualConfig == expectedConfig
  }

  def "elects v1 server url if system info doesn't provide an `IndexServerAddress`"() {
    given:
    def expectedConfig = new AuthConfig(username: "foo-baz")
    def systemInfo = Mock(SystemInfo)
    systemInfo.indexServerAddress >> ""

    when:
    def actualConfig = election.resolveAuthConfig("official.registry", true)

    then:
    1 * authConfigReader.readAuthConfig("https://index.docker.io/v1/", null) >> expectedConfig
    actualConfig == expectedConfig
  }
}
