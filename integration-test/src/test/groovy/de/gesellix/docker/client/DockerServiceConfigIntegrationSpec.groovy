package de.gesellix.docker.client

import de.gesellix.docker.remote.api.Config
import de.gesellix.docker.remote.api.LocalNodeState
import de.gesellix.docker.remote.api.SwarmInitRequest
import de.gesellix.docker.testutil.SwarmUtil
import groovy.util.logging.Slf4j
import spock.lang.Requires
import spock.lang.Specification

@Slf4j
@Requires({ LocalDocker.available() && LocalDocker.supportsConfigs() })
class DockerServiceConfigIntegrationSpec extends Specification {

  static DockerClient dockerClient

  static String swarmAdvertiseAddr

  def setupSpec() {
    dockerClient = new DockerClientImpl()
    swarmAdvertiseAddr = new SwarmUtil().getAdvertiseAddr()
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def cleanup() {
    Thread.sleep(1000)
  }

  def ping() {
    expect:
    "OK" == dockerClient.ping().content
  }

  def "expect inactive swarm"() {
    expect:
    dockerClient.info().content.swarm.localNodeState == LocalNodeState.Inactive
  }

  def "create, list, and remove a config"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    dockerClient.initSwarm(new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null))

    when:
    dockerClient.createConfig("test-config", "some-fancy-stuff".bytes)

    then:
    def configs = dockerClient.configs([filters: [name: ["test-config"]]]).content
    configs.size() == 1
    Config testConfig = configs.first()
    testConfig.spec.name == "test-config"
    Base64.decoder.decode(testConfig.spec.data as String) == "some-fancy-stuff".bytes

    cleanup:
    performSilently { dockerClient.rmConfig("test-config") }
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
