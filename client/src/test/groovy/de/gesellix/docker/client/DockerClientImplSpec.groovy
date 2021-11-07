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

  def "cleanupStorage removes exited containers"() {
    given:
    def keepContainer = { container ->
      container.Names.any { String name ->
        name.replaceAll("^/", "").matches(".*data.*")
      }
    }

    when:
    dockerClient.cleanupStorage keepContainer

    then:
    1 * dockerClient.manageContainer.ps([filters: [status: ["exited"]]]) >> new EngineResponse(
        content: [
            [Command: "ping 127.0.0.1",
             Id     : "container-id-1",
             Image  : "gesellix/testimage:latest",
             Names  : ["/agitated_bardeen"],
             Status : "Exited (137) 13 minutes ago"],
            [Command: "ping 127.0.0.1",
             Id     : "container-id-2",
             Image  : "gesellix/testimage:latest",
             Names  : ["/my_data"],
             Status : "Exited (137) 13 minutes ago"]
        ])
    then:
    1 * dockerClient.manageContainer.rm("container-id-1")
    and:
    0 * dockerClient.manageContainer.rm("container-id-2")
    and:
    1 * dockerClient.manageImage.images([filters: [dangling: ["true"]]]) >> new EngineResponse()
    and:
    1 * dockerClient.manageVolume.volumes([filters: [dangling: ["true"]]]) >> new EngineResponse(
        content: [[Name: "volume-id"]])
    and:
    0 * dockerClient.manageVolume.rmVolume(_)
  }

  def "cleanupStorage removes dangling images"() {
    when:
    dockerClient.cleanupStorage { container -> false }

    then:
    1 * dockerClient.manageContainer.ps([filters: [status: ["exited"]]]) >> new EngineResponse()
    and:
    1 * dockerClient.manageImage.images([filters: [dangling: ["true"]]]) >> new EngineResponse(
        content: [
            [Created    : 1420075526,
             Id         : "image-id-1",
             ParentId   : "f62feddc05dc67da9b725361f97d7ae72a32e355ce1585f9a60d090289120f73",
             RepoTags   : ["<none>": "<none>"],
             Size       : 0,
             VirtualSize: 188299119]
        ])
    then:
    1 * dockerClient.manageImage.rmi("image-id-1")
    and:
    1 * dockerClient.manageVolume.volumes([filters: [dangling: ["true"]]]) >> new EngineResponse(
        content: [[Name: "volume-id"]])
    and:
    0 * dockerClient.manageVolume.rmVolume(_)
  }

  def "cleanupStorage doesn't remove dangling volumes by default"() {
    when:
    dockerClient.cleanupStorage { container -> false }

    then:
    1 * dockerClient.manageContainer.ps([filters: [status: ["exited"]]]) >> new EngineResponse()
    and:
    1 * dockerClient.manageImage.images([filters: [dangling: ["true"]]]) >> new EngineResponse()
    and:
    1 * dockerClient.manageVolume.volumes([filters: [dangling: ["true"]]]) >> new EngineResponse(
        content: [[Name: "volume-id"]])
    and:
    0 * dockerClient.manageVolume.rmVolume(_)
  }

  def "cleanupStorage removes dangling volumes when desired"() {
    when:
    dockerClient.cleanupStorage({ container -> false }, { volume -> volume.Name != "volume-id-1" })

    then:
    1 * dockerClient.manageContainer.ps([filters: [status: ["exited"]]]) >> new EngineResponse()
    and:
    1 * dockerClient.manageImage.images([filters: [dangling: ["true"]]]) >> new EngineResponse()
    and:
    1 * dockerClient.manageVolume.volumes([filters: [dangling: ["true"]]]) >> new EngineResponse(
        content: [
            Volumes: [
                [Name: "volume-id-1"],
                [Name: "volume-id-2"]]])
    and:
    1 * dockerClient.manageVolume.rmVolume("volume-id-1") >> new EngineResponse(
        status: [success: true])
    and:
    0 * dockerClient.manageVolume.rmVolume("volume-id-2")
  }
}
