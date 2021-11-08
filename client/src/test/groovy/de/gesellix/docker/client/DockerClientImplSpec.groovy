package de.gesellix.docker.client

import de.gesellix.docker.client.container.ManageContainer
import de.gesellix.docker.client.image.ManageImage
import de.gesellix.docker.client.node.ManageNode
import de.gesellix.docker.client.system.ManageSystem
import de.gesellix.docker.client.volume.ManageVolume
import de.gesellix.docker.engine.DockerEnv
import de.gesellix.docker.engine.EngineClient
import de.gesellix.docker.engine.EngineResponse
import spock.lang.Specification

class DockerClientImplSpec extends Specification {

  EngineClient httpClient = Mock(EngineClient)
  DockerClientImpl dockerClient = new DockerClientImpl()

  def setup() {
    dockerClient.httpClient = httpClient
    dockerClient.responseHandler = Mock(DockerResponseHandler)

    dockerClient.manageSystem = Mock(ManageSystem)
    dockerClient.manageNode = Mock(ManageNode)
    dockerClient.manageContainer = Mock(ManageContainer)
    dockerClient.manageImage = Mock(ManageImage)
    dockerClient.manageVolume = Mock(ManageVolume)
  }

  def "passes dockerConfig and proxy to internal http client"() {
    given:
    def proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(4711))
    def env = Mock(DockerEnv)
    env.dockerHost >> "tcp://127.0.0.1:2375"

    when:
    def httpClient = new DockerClientImpl(env, proxy)

    then:
    httpClient.dockerClientConfig.env == env
    httpClient.proxy == proxy
  }

  def "get the swarm manager address"() {
    when:
    def managerAddress = dockerClient.getSwarmMangerAddress()
    then:
    1 * dockerClient.manageSystem.info() >> new EngineResponse(
        status: [success: true],
        content: [Swarm: [NodeID: "node-id"]])
    then:
    1 * dockerClient.manageNode.inspectNode("node-id") >> new EngineResponse(
        status: [success: true],
        content: [ManagerStatus: [Addr: "192.168.42.2:2377"]])
    and:
    managerAddress == "192.168.42.2:2377"
  }
}
