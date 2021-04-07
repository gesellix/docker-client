package de.gesellix.docker.client.volume

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.engine.EngineClient
import groovy.json.JsonBuilder
import spock.lang.Specification

class ManageVolumeClientTest extends Specification {

  EngineClient httpClient = Mock(EngineClient)
  ManageVolumeClient service

  def setup() {
    service = new ManageVolumeClient(httpClient, Mock(DockerResponseHandler))
  }

  def "volumes with query"() {
    given:
    def filters = [dangling: ["true"]]
    def expectedFilterValue = new JsonBuilder(filters).toString()
    def query = [filters: filters]

    when:
    service.volumes(query)

    then:
    1 * httpClient.get([path : "/volumes",
                        query: [filters: expectedFilterValue]]) >> [status: [success: true]]
  }

  def "inspect volume"() {
    when:
    service.inspectVolume("a-volume")

    then:
    1 * httpClient.get([path: "/volumes/a-volume"]) >> [status: [success: true]]
  }

  def "create volume with config"() {
    def volumeConfig = [Name      : "my-volume",
                        Driver    : "local",
                        DriverOpts: [:]]

    when:
    service.createVolume(volumeConfig)

    then:
    1 * httpClient.post([path              : "/volumes/create",
                         body              : volumeConfig,
                         requestContentType: "application/json"]) >> [status: [success: true]]
  }

  def "rm volume"() {
    when:
    service.rmVolume("a-volume")

    then:
    1 * httpClient.delete([path: "/volumes/a-volume"]) >> [status: [success: true]]
  }

  def "pruneVolumes removes unused volumes"() {
    when:
    service.pruneVolumes()

    then:
    1 * httpClient.post([path : "/volumes/prune",
                         query: [:]]) >> [status: [success: true]]
  }
}
