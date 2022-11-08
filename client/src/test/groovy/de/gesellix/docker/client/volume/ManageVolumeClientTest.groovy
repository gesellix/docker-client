package de.gesellix.docker.client.volume

import com.squareup.moshi.Moshi
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.Volume
import de.gesellix.docker.remote.api.VolumeCreateOptions
import de.gesellix.docker.remote.api.VolumeListResponse
import de.gesellix.docker.remote.api.VolumePruneResponse
import de.gesellix.docker.remote.api.client.VolumeApi
import spock.lang.Specification

class ManageVolumeClientTest extends Specification {

  EngineApiClient client = Mock(EngineApiClient)
  ManageVolumeClient service

  private Moshi moshi = new Moshi.Builder().build()

  def setup() {
    service = new ManageVolumeClient(client)
  }

  def "volumes with query DEPRECATED"() {
    given:
    def volumeApi = Mock(VolumeApi)
    client.volumeApi >> volumeApi
    def volumesList = Mock(VolumeListResponse)

    def filters = [dangling: ["true"]]
    def expectedFilterValue = moshi.adapter(Map).toJson(filters)

    when:
    def volumes = service.volumes([filters: filters])

    then:
    1 * volumeApi.volumeList(expectedFilterValue) >> volumesList
    volumes.content == volumesList
  }

  def "volumes with filters"() {
    given:
    def filters = moshi.adapter(Map).toJson([dangling: ["true"]])
    def volumeApi = Mock(VolumeApi)
    client.volumeApi >> volumeApi
    def volumesList = Mock(VolumeListResponse)

    when:
    def volumes = service.volumes(filters)

    then:
    1 * volumeApi.volumeList(filters) >> volumesList
    volumes.content == volumesList
  }

  def "inspect volume"() {
    given:
    def volumeApi = Mock(VolumeApi)
    client.volumeApi >> volumeApi
    def volumeInspect = Mock(Volume)

    when:
    def volume = service.inspectVolume("a-volume")

    then:
    1 * volumeApi.volumeInspect("a-volume") >> volumeInspect
    volume.content == volumeInspect
  }

  def "create volume with config DEPRECATED"() {
    given:
    def volumeApi = Mock(VolumeApi)
    client.volumeApi >> volumeApi
    def volumeResponse = Mock(Volume)

    when:
    def volume = service.createVolume([
        Name      : "my-fancy-volume",
        Driver    : "local",
        DriverOpts: [:]])

    then:
    1 * volumeApi.volumeCreate(new VolumeCreateOptions("my-fancy-volume", "local", [:], null)) >> volumeResponse
    volume.content == volumeResponse
  }

  def "create volume with config"() {
    given:
    def volumeApi = Mock(VolumeApi)
    client.volumeApi >> volumeApi
    def volumeResponse = Mock(Volume)
    def volumeConfig = new VolumeCreateOptions("my-volume", "local", [:], [:])

    when:
    def volume = service.createVolume(volumeConfig)

    then:
    1 * volumeApi.volumeCreate(volumeConfig) >> volumeResponse
    volume.content == volumeResponse
  }

  def "rm volume"() {
    given:
    def volumeApi = Mock(VolumeApi)
    client.volumeApi >> volumeApi

    when:
    service.rmVolume("a-volume")

    then:
    1 * volumeApi.volumeDelete("a-volume", null)
  }

  def "pruneVolumes removes unused volumes"() {
    given:
    def volumeApi = Mock(VolumeApi)
    client.volumeApi >> volumeApi
    def response = Mock(VolumePruneResponse)

    when:
    def pruneVolumes = service.pruneVolumes("filter")

    then:
    1 * volumeApi.volumePrune("filter") >> response
    pruneVolumes.content == response
  }
}
