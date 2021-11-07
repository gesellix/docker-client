package de.gesellix.docker.client.secret

import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.IdResponse
import de.gesellix.docker.remote.api.Secret
import de.gesellix.docker.remote.api.SecretSpec
import de.gesellix.docker.remote.api.client.SecretApi
import io.github.joke.spockmockable.Mockable
import spock.lang.Specification

@Mockable([SecretApi, Secret])
class ManageSecretClientTest extends Specification {

  EngineApiClient client = Mock(EngineApiClient)
  ManageSecretClient service

  def setup() {
    service = new ManageSecretClient(client)
  }

  def "create a secret"() {
    given:
    def secretApi = Mock(SecretApi)
    client.secretApi >> secretApi
    def idResponse = Mock(IdResponse)

    def plainData = "secret-content"
    def base64Data = "c2VjcmV0LWNvbnRlbnQ="
    def secretSpec = new SecretSpec("a-secret", [:], base64Data, null, null)

    when:
    def createSecret = service.createSecret(secretSpec.name, plainData.bytes)

    then:
    1 * secretApi.secretCreate(secretSpec) >> idResponse
    createSecret.content == idResponse
  }

  def "inspect a secret"() {
    given:
    def secretApi = Mock(SecretApi)
    client.secretApi >> secretApi
    def secret = Mock(Secret)

    when:
    def inspectSecret = service.inspectSecret("5qyxxlxqbq6s5004io33miih6")

    then:
    1 * secretApi.secretInspect("5qyxxlxqbq6s5004io33miih6") >> secret
    inspectSecret.content == secret
  }

  def "list all secrets"() {
    given:
    def secretApi = Mock(SecretApi)
    client.secretApi >> secretApi
    def secrets = Mock(List)

    when:
    def responseContent = service.secrets("fil-ter")

    then:
    1 * secretApi.secretList("fil-ter") >> secrets
    responseContent.content == secrets
  }

  def "rm a secret"() {
    given:
    def secretApi = Mock(SecretApi)
    client.secretApi >> secretApi

    when:
    service.rmSecret("5qyxxlxqbq6s5004io33miih6")

    then:
    1 * secretApi.secretDelete("5qyxxlxqbq6s5004io33miih6")
  }

  def "update a secret"() {
    given:
    def secretApi = Mock(SecretApi)
    client.secretApi >> secretApi
    def secretSpec = new SecretSpec(null, [:], null, null, null)

    when:
    service.updateSecret("5qyxxlxqbq6s5004io33miih6", 11, secretSpec)

    then:
    1 * secretApi.secretUpdate("5qyxxlxqbq6s5004io33miih6", 11, secretSpec)
  }
}
