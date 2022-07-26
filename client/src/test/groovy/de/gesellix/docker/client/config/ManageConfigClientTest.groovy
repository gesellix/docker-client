package de.gesellix.docker.client.config

import de.gesellix.docker.remote.api.Config
import de.gesellix.docker.remote.api.ConfigCreateRequest
import de.gesellix.docker.remote.api.ConfigSpec
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.IdResponse
import de.gesellix.docker.remote.api.client.ConfigApi
import io.github.joke.spockmockable.Mockable
import spock.lang.Specification

@Mockable([ConfigApi, Config])
class ManageConfigClientTest extends Specification {

  EngineApiClient client = Mock(EngineApiClient)
  ManageConfigClient service

  def setup() {
    service = new ManageConfigClient(client)
  }

  def "create a config"() {
    given:
    def configApi = Mock(ConfigApi)
    client.configApi >> configApi
    def idResponse = Mock(IdResponse)

    def plainData = "config-content"
    def base64Data = "Y29uZmlnLWNvbnRlbnQ="
    def configSpec = new ConfigCreateRequest("a-config", [:], base64Data, null)

    when:
    def createConfig = service.createConfig("a-config", plainData.bytes)

    then:
    1 * configApi.configCreate(configSpec) >> idResponse
    createConfig.content == idResponse
  }

  def "inspect a config"() {
    given:
    def configApi = Mock(ConfigApi)
    client.configApi >> configApi
    def config = Mock(Config)

    when:
    def inspectConfig = service.inspectConfig("5qyxxlxqbq6s5004io33miih6")

    then:
    1 * client.configApi.configInspect("5qyxxlxqbq6s5004io33miih6") >> config
    inspectConfig.content == config
  }

  def "list all configs"() {
    given:
    def configApi = Mock(ConfigApi)
    client.configApi >> configApi
    def configList = Mock(List)

    when:
    def configs = service.configs("filt-ter")

    then:
    1 * client.configApi.configList("filt-ter") >> configList
    configs.content == configList
  }

  def "rm a config"() {
    given:
    def configApi = Mock(ConfigApi)
    client.configApi >> configApi

    when:
    service.rmConfig("5qyxxlxqbq6s5004io33miih6")

    then:
    1 * client.configApi.configDelete("5qyxxlxqbq6s5004io33miih6")
  }

  def "update a config"() {
    given:
    def configApi = Mock(ConfigApi)
    client.configApi >> configApi
    def configSpec = new ConfigSpec(null, [:], null, null)

    when:
    service.updateConfig("5qyxxlxqbq6s5004io33miih6", 11, configSpec)

    then:
    1 * client.configApi.configUpdate("5qyxxlxqbq6s5004io33miih6", 11, configSpec)
  }
}
