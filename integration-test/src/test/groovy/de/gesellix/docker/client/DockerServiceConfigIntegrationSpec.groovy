package de.gesellix.docker.client

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
    performSilently { dockerClient.leaveSwarm([force: true]) }
  }

  def cleanup() {
    Thread.sleep(1000)
  }

  def ping() {
    when:
    def ping = dockerClient.ping()

    then:
    ping.status.code == 200
    ping.content == "OK"
  }

  def "expect inactive swarm"() {
    expect:
    dockerClient.info().content.Swarm.LocalNodeState == "inactive"
  }

  def "create, list, and remove a config"() {
    given:
    def swarmConfig = dockerClient.newSwarmConfig()
    swarmConfig.AdvertiseAddr = swarmAdvertiseAddr
    dockerClient.initSwarm(swarmConfig)

    when:
    dockerClient.createConfig("test-config", "some-fancy-stuff".bytes)

    then:
    def configs = dockerClient.configs([filters: [name: ["test-config"]]]).content
    configs.size() == 1
    Map testConfig = configs.first()
    testConfig.Spec.Name == "test-config"
    Base64.decoder.decode(testConfig.Spec.Data as String) == "some-fancy-stuff".bytes

    cleanup:
    performSilently { dockerClient.rmConfig("test-config") }
    performSilently { dockerClient.leaveSwarm([force: true]) }
  }

  def performSilently(Closure action) {
    try {
      action()
    }
    catch (Exception ignored) {
    }
  }
}
