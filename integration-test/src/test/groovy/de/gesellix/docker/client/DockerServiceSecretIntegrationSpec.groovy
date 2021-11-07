package de.gesellix.docker.client

import de.gesellix.docker.remote.api.LocalNodeState
import de.gesellix.docker.remote.api.Secret
import de.gesellix.docker.remote.api.SwarmInitRequest
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

  def "create, list, and remove a secret"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    dockerClient.initSwarm(new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null))

    when:
    dockerClient.createSecret("test-secret", "some-secret-stuff".bytes)

    then:
    def secrets = dockerClient.secrets([filters: [names: ["test-secret"]]]).content
    secrets.size() == 1
    Secret testSecret = secrets.first()
    testSecret.spec.name == "test-secret"

    cleanup:
    performSilently { dockerClient.rmSecret("test-secret") }
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
