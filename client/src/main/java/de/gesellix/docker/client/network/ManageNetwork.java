package de.gesellix.docker.client.network;

import de.gesellix.docker.engine.EngineResponse;

import java.util.Map;

public interface ManageNetwork {

  EngineResponse connectNetwork(String network, String container);

  EngineResponse createNetwork(String name);

  EngineResponse createNetwork(String name, Map<String, Object> config);

  EngineResponse disconnectNetwork(String network, String container);

  EngineResponse inspectNetwork(String name);

  EngineResponse networks();

  EngineResponse networks(Map<String, Object> query);

  EngineResponse pruneNetworks();

  EngineResponse pruneNetworks(Map<String, Object> query);

  EngineResponse rmNetwork(String name);
}
