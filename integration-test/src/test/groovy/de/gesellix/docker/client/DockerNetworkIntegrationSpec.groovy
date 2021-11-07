package de.gesellix.docker.client

import de.gesellix.docker.remote.api.SwarmInitRequest
import de.gesellix.docker.testutil.SwarmUtil
import groovy.util.logging.Slf4j
import spock.lang.Requires
import spock.lang.Specification

@Slf4j
@Requires({ LocalDocker.available() })
class DockerNetworkIntegrationSpec extends Specification {

  static DockerClient dockerClient

  def setupSpec() {
    dockerClient = new DockerClientImpl()
  }

  def ping() {
    expect:
    "OK" == dockerClient.ping().content
  }

  def "list networks"() {
    when:
    def networks = dockerClient.networks().content

    then:
    networks.find { it.name == "none" }
  }

  def "create default network"() {
    given:
    !dockerClient.networks().content.find { it.name == "test-net" }

    when:
    dockerClient.createNetwork('test-net')

    then:
    dockerClient.networks().content.find { it.name == "test-net" }

    cleanup:
    performSilently { dockerClient.rmNetwork('test-net') }
  }

  @Requires({ LocalDocker.supportsSwarmMode() })
  def "create overlay network"() {
    given:
    performSilently { dockerClient.leaveSwarm(true) }
    !dockerClient.networks().content.find { it.name == "test-net" }
    dockerClient.initSwarm(new SwarmInitRequest(
        "0.0.0.0",
        new SwarmUtil().getAdvertiseAddr(),
        null, null, null,
        true,
        null, null
    ))

    when:
    dockerClient.createNetwork('test-net', [
        Driver: "overlay",
        "IPAM": [
            "Driver": "default"
        ]
    ])

    then:
    dockerClient.networks().content.find { it.name == "test-net" }

    cleanup:
    performSilently { dockerClient.rmNetwork('test-net') }
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def performSilently(Closure action) {
    try {
      action()
    }
    catch (Exception ignored) {
    }
  }
}
