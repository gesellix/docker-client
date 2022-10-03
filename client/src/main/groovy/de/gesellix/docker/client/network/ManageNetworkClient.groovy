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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ManageNetworkClient implements ManageNetwork {

  private final Logger log = LoggerFactory.getLogger(ManageNetworkClient)

  private EngineApiClient client
  private QueryUtil queryUtil

  ManageNetworkClient(EngineApiClient client) {
    this.client = client
    this.queryUtil = new QueryUtil()
  }

  /**
   * @see #networks(String)
   * @deprecated use {@link #networks(String)}
   */
  @Deprecated
  @Override
  EngineResponseContent<List<Network>> networks(Map<String, Object> query) {
    log.info("docker network ls")
    Map<String, Object> actualQuery = new HashMap<String, Object>()
    if (query != null) {
      actualQuery.putAll(query)
    }
    queryUtil.jsonEncodeQueryParameter(actualQuery, "filters")
    return networks(actualQuery.get("filters") as String)
  }

  @Override
  EngineResponseContent<List<Network>> networks(String filters = null) {
    log.info("docker network ls")
    List<Network> networks = client.getNetworkApi().networkList(filters)
    return new EngineResponseContent<List<Network>>(networks)
  }

  @Override
  EngineResponseContent<Network> inspectNetwork(String name) {
    log.info("docker network inspect")
    Network network = client.getNetworkApi().networkInspect(name, null, null)
    return new EngineResponseContent<Network>(network)
  }

  /**
   * @see #createNetwork(NetworkCreateRequest)
   * @deprecated use {@link #createNetwork(NetworkCreateRequest)}
   */
  @Deprecated
  @Override
  EngineResponseContent<NetworkCreateResponse> createNetwork(String name, Map<String, Object> config) {
    Map actualConfig = [:]
    if (config != null) {
      actualConfig.putAll(config)
    }

    NetworkCreateRequest request = new NetworkCreateRequest(
        name,
        true,
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
  EngineResponseContent<NetworkCreateResponse> createNetwork(String name) {
    NetworkCreateRequest request = new NetworkCreateRequest(
        name,
        true,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null)
    return createNetwork(request)
  }

  @Override
  EngineResponseContent<NetworkCreateResponse> createNetwork(NetworkCreateRequest networkCreateRequest) {
    log.info("docker network create")
//    if (networkCreateRequest.name == null) {
//      throw new IllegalArgumentException("Name is null")
//    }
    // TODO set defaults?
//    if (networkCreateRequest.checkDuplicate == null) {
//      networkCreateRequest.checkDuplicate = true
//    }
    NetworkCreateResponse networkCreate = client.getNetworkApi().networkCreate(networkCreateRequest)
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

  /**
   * @see #pruneNetworks(String)
   * @deprecated use {@link #pruneNetworks(String)}
   */
  @Deprecated
  @Override
  EngineResponseContent<NetworkPruneResponse> pruneNetworks(Map<String, Object> query) {
    log.info("docker network prune")
    Map<String, Object> actualQuery = new HashMap<String, Object>()
    if (query != null) {
      actualQuery.putAll(query)
    }
    queryUtil.jsonEncodeQueryParameter(actualQuery, "filters")
    return pruneNetworks(actualQuery.get("filters") as String)
  }

  @Override
  EngineResponseContent<NetworkPruneResponse> pruneNetworks(String filters = null) {
    log.info("docker network prune")
    NetworkPruneResponse networkPrune = client.getNetworkApi().networkPrune(filters)
    return new EngineResponseContent<NetworkPruneResponse>(networkPrune)
  }
}
