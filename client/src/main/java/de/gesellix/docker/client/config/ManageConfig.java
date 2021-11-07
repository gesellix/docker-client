package de.gesellix.docker.client.config;

import de.gesellix.docker.engine.EngineResponse;
import de.gesellix.docker.remote.api.Config;
import de.gesellix.docker.remote.api.ConfigSpec;
import de.gesellix.docker.remote.api.IdResponse;

import java.util.List;
import java.util.Map;

public interface ManageConfig {

  EngineResponse<IdResponse> createConfig(String name, byte[] configData);

  EngineResponse<IdResponse> createConfig(String name, byte[] configData, Map<String, String> labels);

  EngineResponse<Config> inspectConfig(String configId);

  /**
   * @see #configs(String)
   * @deprecated use {@link #configs(String)}
   */
  @Deprecated
  EngineResponse<List<Config>> configs(Map<String, Object> query);

  EngineResponse<List<Config>> configs();

  EngineResponse<List<Config>> configs(String filters);

  void rmConfig(String configId);

  void updateConfig(String configId, long version, ConfigSpec configSpec);
}
