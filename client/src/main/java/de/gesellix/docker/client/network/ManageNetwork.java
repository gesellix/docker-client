package de.gesellix.docker.client.network;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.remote.api.Network;
import de.gesellix.docker.remote.api.NetworkCreateRequest;
import de.gesellix.docker.remote.api.NetworkCreateResponse;
import de.gesellix.docker.remote.api.NetworkPruneResponse;

import java.util.List;
import java.util.Map;

public interface ManageNetwork {

  void connectNetwork(String network, String container);

  void disconnectNetwork(String network, String container);

  EngineResponseContent<NetworkCreateResponse> createNetwork(String name);

  EngineResponseContent<NetworkCreateResponse> createNetwork(NetworkCreateRequest networkCreateRequest);

  EngineResponseContent<Network> inspectNetwork(String name);

  /**
   * @see #networks(String)
   * @deprecated use {@link #networks(String)}
   */
  @Deprecated
  EngineResponseContent<List<Network>> networks(Map<String, Object> query);

  EngineResponseContent<List<Network>> networks();

  EngineResponseContent<List<Network>> networks(String filters);

  /**
   * @see #pruneNetworks(String)
   * @deprecated use {@link #pruneNetworks(String)}
   */
  @Deprecated
  EngineResponseContent<NetworkPruneResponse> pruneNetworks(Map<String, Object> query);

  EngineResponseContent<NetworkPruneResponse> pruneNetworks();

  EngineResponseContent<NetworkPruneResponse> pruneNetworks(String filters);

  void rmNetwork(String name);
}
