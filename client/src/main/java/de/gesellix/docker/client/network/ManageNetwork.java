package de.gesellix.docker.client.network;

import de.gesellix.docker.engine.EngineResponse;
import de.gesellix.docker.remote.api.Network;
import de.gesellix.docker.remote.api.NetworkCreateRequest;
import de.gesellix.docker.remote.api.NetworkCreateResponse;
import de.gesellix.docker.remote.api.NetworkPruneResponse;

import java.util.List;
import java.util.Map;

public interface ManageNetwork {

  void connectNetwork(String network, String container);

  void disconnectNetwork(String network, String container);

  /**
   * @see #createNetwork(NetworkCreateRequest)
   * @deprecated use {@link #createNetwork(NetworkCreateRequest)}
   */
  @Deprecated
  EngineResponse<NetworkCreateResponse> createNetwork(String name, Map<String, Object> config);

  EngineResponse<NetworkCreateResponse> createNetwork(String name);

  EngineResponse<NetworkCreateResponse> createNetwork(NetworkCreateRequest networkCreateRequest);

  EngineResponse<Network> inspectNetwork(String name);

  /**
   * @see #networks(String)
   * @deprecated use {@link #networks(String)}
   */
  @Deprecated
  EngineResponse<List<Network>> networks(Map<String, Object> query);

  EngineResponse<List<Network>> networks();

  EngineResponse<List<Network>> networks(String filters);

  /**
   * @see #pruneNetworks(String)
   * @deprecated use {@link #pruneNetworks(String)}
   */
  @Deprecated
  EngineResponse<NetworkPruneResponse> pruneNetworks(Map<String, Object> query);

  EngineResponse<NetworkPruneResponse> pruneNetworks();

  EngineResponse<NetworkPruneResponse> pruneNetworks(String filters);

  void rmNetwork(String name);
}
