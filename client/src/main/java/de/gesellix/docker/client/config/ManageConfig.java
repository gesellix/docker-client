package de.gesellix.docker.client.config;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.remote.api.Config;
import de.gesellix.docker.remote.api.ConfigSpec;
import de.gesellix.docker.remote.api.IdResponse;

import java.util.List;
import java.util.Map;

public interface ManageConfig {

  EngineResponseContent<IdResponse> createConfig(String name, byte[] configData);

  EngineResponseContent<IdResponse> createConfig(String name, byte[] configData, Map<String, String> labels);

  EngineResponseContent<Config> inspectConfig(String configId);

  /**
   * @see #configs(String)
   * @deprecated use {@link #configs(String)}
   */
  @Deprecated
  EngineResponseContent<List<Config>> configs(Map<String, Object> query);

  EngineResponseContent<List<Config>> configs();

  EngineResponseContent<List<Config>> configs(String filters);

  void rmConfig(String configId);

  void updateConfig(String configId, long version, ConfigSpec configSpec);
}
