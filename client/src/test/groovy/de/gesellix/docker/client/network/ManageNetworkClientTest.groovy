package de.gesellix.docker.client.network

import com.squareup.moshi.Moshi
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.Network
import de.gesellix.docker.remote.api.NetworkConnectRequest
import de.gesellix.docker.remote.api.NetworkCreateRequest
import de.gesellix.docker.remote.api.NetworkCreateResponse
import de.gesellix.docker.remote.api.NetworkDisconnectRequest
import de.gesellix.docker.remote.api.NetworkPruneResponse
import de.gesellix.docker.remote.api.client.NetworkApi
import spock.lang.Specification

class ManageNetworkClientTest extends Specification {

  EngineApiClient client = Mock(EngineApiClient)
  ManageNetworkClient service

  private Moshi moshi = new Moshi.Builder().build()

  def setup() {
    service = new ManageNetworkClient(client)
  }

  def "networks with query DEPRECATED"() {
    given:
    def networkApi = Mock(NetworkApi)
    client.networkApi >> networkApi
    def networksList = Mock(List)

    def filters = [name: ["a-net"], id: ["a-net-id"]]
    def expectedFilterValue = moshi.adapter(Map).toJson(filters)

    when:
    def networks = service.networks([filters: filters])

    then:
    1 * networkApi.networkList(expectedFilterValue) >> networksList
    networks.content == networksList
  }

  def "networks with filters"() {
    given:
    def networkApi = Mock(NetworkApi)
    client.networkApi >> networkApi
    def networksList = Mock(List)

    when:
    def networks = service.networks("filters")

    then:
    1 * networkApi.networkList("filters") >> networksList
    networks.content == networksList
  }

  def "inspect network"() {
    given:
    def networkApi = Mock(NetworkApi)
    client.networkApi >> networkApi
    def network = Mock(Network)

    when:
    def inspectNetwork = service.inspectNetwork("a-network")

    then:
    1 * networkApi.networkInspect("a-network", null, null) >> network
    inspectNetwork.content == network
  }

  def "create network with defaults"() {
    given:
    def networkApi = Mock(NetworkApi)
    client.networkApi >> networkApi
    def networkResponse = Mock(NetworkCreateResponse)

    when:
    def network = service.createNetwork("network-name")

    then:
    1 * networkApi.networkCreate(new NetworkCreateRequest(
        "network-name",
        true,
        null, null,
        null, null,
        null, null,
        null, null, null,
        null, null)) >> networkResponse
    network.content == networkResponse
  }

  def "create network with config"() {
    given:
    def networkApi = Mock(NetworkApi)
    client.networkApi >> networkApi
    def networkCreateRequest = new NetworkCreateRequest(
        "network-name",
        false,
        "bridge",
        null, null, null, null, null, null, null, null, [:], null
    )
    def networkResponse = Mock(NetworkCreateResponse)

    when:
    def network = service.createNetwork(networkCreateRequest)

    then:
    1 * networkApi.networkCreate(networkCreateRequest) >> networkResponse
    network.content == networkResponse
  }

  def "connect a container to a network"() {
    given:
    def networkApi = Mock(NetworkApi)
    client.networkApi >> networkApi

    when:
    service.connectNetwork("a-network", "a-container")

    then:
    1 * networkApi.networkConnect("a-network", new NetworkConnectRequest("a-container", null))
  }

  def "disconnect a container from a network"() {
    given:
    def networkApi = Mock(NetworkApi)
    client.networkApi >> networkApi

    when:
    service.disconnectNetwork("a-network", "a-container")

    then:
    1 * networkApi.networkDisconnect("a-network", new NetworkDisconnectRequest("a-container", null))
  }

  def "rm network"() {
    given:
    def networkApi = Mock(NetworkApi)
    client.networkApi >> networkApi

    when:
    service.rmNetwork("a-network")

    then:
    1 * networkApi.networkDelete("a-network")
  }

  def "pruneNetworks removes unused networks"() {
    given:
    def networkApi = Mock(NetworkApi)
    client.networkApi >> networkApi
    def pruned = Mock(NetworkPruneResponse)

    when:
    def pruneNetworks = service.pruneNetworks("filters")

    then:
    1 * networkApi.networkPrune("filters") >> pruned
    pruneNetworks.content == pruned
  }
}
