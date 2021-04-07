package de.gesellix.docker.client.config

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.engine.EngineClient
import spock.lang.Specification

class ManageConfigClientTest extends Specification {

  EngineClient httpClient = Mock(EngineClient)
  ManageConfigClient service

  def setup() {
    service = new ManageConfigClient(httpClient, Mock(DockerResponseHandler))
  }

  def "create a config"() {
    when:
    service.createConfig("a-config", "config-content".bytes)

    then:
    1 * httpClient.post([path              : "/configs/create",
                         body              : [Name  : "a-config",
                                              Data  : "config-content".bytes,
                                              Labels: [:]],
                         requestContentType: "application/json"]) >> [status: [success: true]]
  }

  def "inspect a config"() {
    when:
    service.inspectConfig("5qyxxlxqbq6s5004io33miih6")

    then:
    1 * httpClient.get([path: "/configs/5qyxxlxqbq6s5004io33miih6"]) >> [status: [success: true]]
  }

  def "list all configs"() {
    when:
    service.configs()

    then:
    1 * httpClient.get([path : "/configs",
                        query: [:]]) >> [status: [success: true]]
  }

  def "rm a config"() {
    when:
    service.rmConfig("5qyxxlxqbq6s5004io33miih6")

    then:
    1 * httpClient.delete([path: "/configs/5qyxxlxqbq6s5004io33miih6"]) >> [status: [success: true]]
  }

  def "update a config"() {
    when:
    service.updateConfig("5qyxxlxqbq6s5004io33miih6", 11, [Labels: [:]])

    then:
    1 * httpClient.post([path              : "/configs/5qyxxlxqbq6s5004io33miih6/update",
                         query             : [version: 11],
                         body              : [Labels: [:]],
                         requestContentType: "application/json"]) >> [status: [success: true]]
  }
}
