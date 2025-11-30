package de.gesellix.docker.client.network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.remote.api.EngineApiClient;
import de.gesellix.docker.remote.api.Network;
import de.gesellix.docker.remote.api.NetworkConnectRequest;
import de.gesellix.docker.remote.api.NetworkCreateRequest;
import de.gesellix.docker.remote.api.NetworkCreateResponse;
import de.gesellix.docker.remote.api.NetworkDisconnectRequest;
import de.gesellix.docker.remote.api.NetworkPruneResponse;
import de.gesellix.util.QueryParameterEncoder;

public class ManageNetworkClient implements ManageNetwork {

  private final Logger log = LoggerFactory.getLogger(ManageNetworkClient.class);
  private final EngineApiClient client;
  private final QueryParameterEncoder queryParameterEncoder;

  public ManageNetworkClient(EngineApiClient client) {
    this.client = client;
    this.queryParameterEncoder = new QueryParameterEncoder();
  }

  /**
   * @see #networks(String)
   * @deprecated use {@link #networks(String)}
   */
  @Deprecated
  @Override
  public EngineResponseContent<List<Network>> networks(Map<String, Object> query) {
    log.info("docker network ls");
    Map<String, Object> actualQuery = new HashMap<>();
    if (query != null) {
      actualQuery.putAll(query);
    }

    queryParameterEncoder.jsonEncodeQueryParameter(actualQuery, "filters");
    return networks((String) actualQuery.get("filters"));
  }

  @Override
  public EngineResponseContent<List<Network>> networks(String filters) {
    log.info("docker network ls");
    List<Network> networks = client.getNetworkApi().networkList(filters);
    return new EngineResponseContent<>(networks);
  }

  @Override
  public EngineResponseContent<List<Network>> networks() {
    return networks((String) null);
  }

  @Override
  public EngineResponseContent<Network> inspectNetwork(String name) {
    log.info("docker network inspect");
    Network network = client.getNetworkApi().networkInspect(name, null, null);
    return new EngineResponseContent<>(network);
  }

  @Override
  public EngineResponseContent<NetworkCreateResponse> createNetwork(String name) {
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
        null,
        null,
        null,
        null);
    return createNetwork(request);
  }

  @Override
  public EngineResponseContent<NetworkCreateResponse> createNetwork(NetworkCreateRequest networkCreateRequest) {
    log.info("docker network create");
//    if (networkCreateRequest.name == null) {
//      throw new IllegalArgumentException("Name is null")
//    }
    // TODO set defaults?
//    if (networkCreateRequest.checkDuplicate == null) {
//      networkCreateRequest.checkDuplicate = true
//    }
    NetworkCreateResponse networkCreate = client.getNetworkApi().networkCreate(networkCreateRequest);
    return new EngineResponseContent<>(networkCreate);
  }

  @Override
  public void connectNetwork(String network, String container) {
    log.info("docker network connect");
    client.getNetworkApi().networkConnect(network, new NetworkConnectRequest(container, null));
  }

  @Override
  public void disconnectNetwork(String network, String container) {
    log.info("docker network disconnect");
    client.getNetworkApi().networkDisconnect(network, new NetworkDisconnectRequest(container, null));
  }

  @Override
  public void rmNetwork(String name) {
    log.info("docker network rm");
    client.getNetworkApi().networkDelete(name);
  }

  /**
   * @see #pruneNetworks(String)
   * @deprecated use {@link #pruneNetworks(String)}
   */
  @Deprecated
  @Override
  public EngineResponseContent<NetworkPruneResponse> pruneNetworks(Map<String, Object> query) {
    log.info("docker network prune");
    Map<String, Object> actualQuery = new HashMap<>();
    if (query != null) {
      actualQuery.putAll(query);
    }
    queryParameterEncoder.jsonEncodeQueryParameter(actualQuery, "filters");
    return pruneNetworks((String) actualQuery.get("filters"));
  }

  @Override
  public EngineResponseContent<NetworkPruneResponse> pruneNetworks(String filters) {
    log.info("docker network prune");
    NetworkPruneResponse networkPrune = client.getNetworkApi().networkPrune(filters);
    return new EngineResponseContent<>(networkPrune);
  }

  @Override
  public EngineResponseContent<NetworkPruneResponse> pruneNetworks() {
    return pruneNetworks((String) null);
  }
}
