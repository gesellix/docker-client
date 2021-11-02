package de.gesellix.docker.client.config;

import de.gesellix.docker.engine.EngineResponse;

import java.util.Map;

public interface ManageConfig {

  EngineResponse createConfig(String name, byte[] configData);

  EngineResponse createConfig(String name, byte[] configData, Map<String, String> labels);

  EngineResponse inspectConfig(String configId);

  EngineResponse configs();

  EngineResponse configs(Map query);

  EngineResponse rmConfig(String configId);

  EngineResponse updateConfig(String configId, int version, Map<String, ?> configSpec);
}
