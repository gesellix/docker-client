package de.gesellix.docker.client

import de.gesellix.docker.client.container.ManageContainer
import de.gesellix.docker.client.image.ManageImage
import de.gesellix.docker.client.node.ManageNode
import de.gesellix.docker.client.system.ManageSystem
import de.gesellix.docker.client.volume.ManageVolume
import de.gesellix.docker.engine.DockerEnv
import de.gesellix.docker.engine.EngineClient
import de.gesellix.docker.engine.EngineResponse
import de.gesellix.docker.remote.api.SwarmInfo
import de.gesellix.docker.remote.api.SystemInfo
import io.github.joke.spockmockable.Mockable
import spock.lang.Specification

@Mockable([SystemInfo, SwarmInfo])
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
    given:
    def swarmInfo = Mock(SwarmInfo)
    swarmInfo.nodeID >> "node-id"
    def systemInfo = Mock(SystemInfo)
    systemInfo.swarm >> swarmInfo

    when:
    def managerAddress = dockerClient.getSwarmMangerAddress()

    then:
    1 * dockerClient.manageSystem.info() >> new EngineResponseContent<SystemInfo>(systemInfo)
    then:
    1 * dockerClient.manageNode.inspectNode("node-id") >> new EngineResponse(
        status: [success: true],
        content: [managerStatus: [addr: "192.168.42.2:2377"]])
    and:
    managerAddress == "192.168.42.2:2377"
  }
}
