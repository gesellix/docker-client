package de.gesellix.docker.client

import de.gesellix.docker.remote.api.Volume
import de.gesellix.docker.remote.api.VolumeCreateOptions
import groovy.util.logging.Slf4j
import spock.lang.Requires
import spock.lang.Specification

@Slf4j
@Requires({ LocalDocker.available() })
class DockerVolumeIntegrationSpec extends Specification {

  static DockerClient dockerClient

  def setupSpec() {
    dockerClient = new DockerClientImpl()
  }

  def ping() {
    expect:
    "OK" == dockerClient.ping().content
  }

  def "list volumes"() {
    when:
    def volumes = dockerClient.volumes().content

    then:
    volumes.volumes instanceof List
  }

  def "create volume"() {
    given:
    def volumeConfig = new VolumeCreateOptions()
    volumeConfig.setName("my-volume")
    volumeConfig.setDriver("local")
    volumeConfig.setDriverOpts([:])

    when:
    def volume = dockerClient.createVolume(volumeConfig).content

    then:
    volume.name == "my-volume"
    and:
    volume.driver == "local"
    and:
    volume.mountpoint?.contains("my-volume")
    and:
    volume.scope == null || volume.scope == Volume.Scope.Local

    cleanup:
    dockerClient.rmVolume("my-volume")
  }
}
