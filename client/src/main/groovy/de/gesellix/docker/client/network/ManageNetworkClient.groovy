package de.gesellix.docker.client.network

import de.gesellix.docker.client.EngineResponseContent
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.IPAM
import de.gesellix.docker.remote.api.Network
import de.gesellix.docker.remote.api.NetworkConnectRequest
import de.gesellix.docker.remote.api.NetworkCreateRequest
import de.gesellix.docker.remote.api.NetworkCreateResponse
import de.gesellix.docker.remote.api.NetworkDisconnectRequest
import de.gesellix.docker.remote.api.NetworkPruneResponse
import de.gesellix.util.QueryUtil
import groovy.util.logging.Slf4j

@Slf4j
class ManageNetworkClient implements ManageNetwork {

  private EngineApiClient client
  private QueryUtil queryUtil

  ManageNetworkClient(EngineApiClient client) {
    this.client = client
    this.queryUtil = new QueryUtil()
  }

  @Override
  EngineResponseContent<List<Network>> networks(Map<String, Object> query) {
    log.info("docker network ls")
    def actualQuery = [:]
    if (query) {
      actualQuery.putAll(query)
    }
    queryUtil.jsonEncodeQueryParameter(actualQuery, "filters")
    return networks(actualQuery.filters as String)
  }

  @Override
  EngineResponseContent<List<Network>> networks(String filters = null) {
    log.info("docker network ls")
    def networks = client.getNetworkApi().networkList(filters)
    return new EngineResponseContent<List<Network>>(networks)
  }

  @Override
  EngineResponseContent<Network> inspectNetwork(String name) {
    log.info("docker network inspect")
    def network = client.getNetworkApi().networkInspect(name, null, null)
    return new EngineResponseContent<Network>(network)
  }

  @Override
  EngineResponseContent<NetworkCreateResponse> createNetwork(String name, Map<String, Object> config = [:]) {
    def actualConfig = [:]
    if (config) {
      actualConfig.putAll(config)
    }
    def defaults = [
        Name          : name,
        CheckDuplicate: true]
    queryUtil.applyDefaults(actualConfig, defaults)

    def request = new NetworkCreateRequest(
        actualConfig.Name as String,
        actualConfig.CheckDuplicate as Boolean,
        actualConfig.Driver as String,
        actualConfig.Internal as Boolean,
        actualConfig.Attachable as Boolean,
        actualConfig.Ingress as Boolean,
        actualConfig.IPAM == null ? null : new IPAM(
            actualConfig.IPAM?.Driver as String,
            actualConfig.IPAM?.Config as List,
            actualConfig.IPAM?.Options as Map),
        actualConfig.EnableIPv6 as Boolean,
        actualConfig.Options as Map,
        actualConfig.Labels as Map)
    return createNetwork(request)
  }

  @Override
  EngineResponseContent<NetworkCreateResponse> createNetwork(NetworkCreateRequest networkCreateRequest) {
    log.info("docker network create")
    // TODO set defaults
    def networkCreate = client.getNetworkApi().networkCreate(networkCreateRequest)
    return new EngineResponseContent<NetworkCreateResponse>(networkCreate)
  }

  @Override
  void connectNetwork(String network, String container) {
    log.info("docker network connect")
    client.getNetworkApi().networkConnect(network, new NetworkConnectRequest(container, null))
  }

  @Override
  void disconnectNetwork(String network, String container) {
    log.info("docker network disconnect")
    client.getNetworkApi().networkDisconnect(network, new NetworkDisconnectRequest(container, null))
  }

  @Override
  void rmNetwork(String name) {
    log.info("docker network rm")
    client.getNetworkApi().networkDelete(name)
  }

  @Override
  EngineResponseContent<NetworkPruneResponse> pruneNetworks(Map<String, Object> query) {
    log.info("docker network prune")
    def actualQuery = [:]
    if (query) {
      actualQuery.putAll(query)
    }
    queryUtil.jsonEncodeQueryParameter(actualQuery, "filters")
    return pruneNetworks(actualQuery.filters as String)
  }

  @Override
  EngineResponseContent<NetworkPruneResponse> pruneNetworks(String filters = null) {
    log.info("docker network prune")
    def networkPrune = client.getNetworkApi().networkPrune(filters)
    return new EngineResponseContent<NetworkPruneResponse>(networkPrune)
  }
}
