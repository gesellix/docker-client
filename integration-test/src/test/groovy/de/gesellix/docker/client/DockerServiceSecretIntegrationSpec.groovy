package de.gesellix.docker.client

import de.gesellix.docker.testutil.SwarmUtil
import groovy.util.logging.Slf4j
import spock.lang.Requires
import spock.lang.Specification

@Slf4j
@Requires({ LocalDocker.available() && LocalDocker.supportsSecrets() })
class DockerServiceSecretIntegrationSpec extends Specification {

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

  def "create, list, and remove a secret"() {
    given:
    def swarmConfig = dockerClient.newSwarmConfig()
    swarmConfig.AdvertiseAddr = swarmAdvertiseAddr
    dockerClient.initSwarm(swarmConfig)

    when:
    dockerClient.createSecret("test-secret", "some-secret-stuff".bytes)

    then:
    def secrets = dockerClient.secrets([filters: [names: ["test-secret"]]]).content
    secrets.size() == 1
    Map testSecret = secrets.first()
    testSecret.Spec.Name == "test-secret"

    cleanup:
    performSilently { dockerClient.rmSecret("test-secret") }
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
