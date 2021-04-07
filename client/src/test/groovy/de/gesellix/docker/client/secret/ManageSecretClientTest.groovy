package de.gesellix.docker.client.secret

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.engine.EngineClient
import spock.lang.Specification

class ManageSecretClientTest extends Specification {

  EngineClient httpClient = Mock(EngineClient)
  ManageSecretClient service

  def setup() {
    service = new ManageSecretClient(httpClient, Mock(DockerResponseHandler))
  }

  def "create a secret"() {
    when:
    service.createSecret("a-secret", "secret-content".bytes)

    then:
    1 * httpClient.post([path              : "/secrets/create",
                         body              : [Name  : "a-secret",
                                              Data  : "c2VjcmV0LWNvbnRlbnQ=".bytes,
                                              Labels: [:]],
                         requestContentType: "application/json"]) >> [status: [success: true]]
  }

  def "inspect a secret"() {
    when:
    service.inspectSecret("5qyxxlxqbq6s5004io33miih6")

    then:
    1 * httpClient.get([path: "/secrets/5qyxxlxqbq6s5004io33miih6"]) >> [status: [success: true]]
  }

  def "list all secrets"() {
    when:
    service.secrets()

    then:
    1 * httpClient.get([path : "/secrets",
                        query: [:]]) >> [status: [success: true]]
  }

  def "rm a secret"() {
    when:
    service.rmSecret("5qyxxlxqbq6s5004io33miih6")

    then:
    1 * httpClient.delete([path: "/secrets/5qyxxlxqbq6s5004io33miih6"]) >> [status: [success: true]]
  }

  def "update a secret"() {
    when:
    service.updateSecret("5qyxxlxqbq6s5004io33miih6", 11, [Labels: [:]])

    then:
    1 * httpClient.post([path              : "/secrets/5qyxxlxqbq6s5004io33miih6/update",
                         query             : [version: 11],
                         body              : [Labels: [:]],
                         requestContentType: "application/json"]) >> [status: [success: true]]
  }
}
