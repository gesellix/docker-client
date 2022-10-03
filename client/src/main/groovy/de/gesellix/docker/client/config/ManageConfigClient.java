package de.gesellix.docker.client.config;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.remote.api.Config;
import de.gesellix.docker.remote.api.ConfigCreateRequest;
import de.gesellix.docker.remote.api.ConfigSpec;
import de.gesellix.docker.remote.api.EngineApiClient;
import de.gesellix.docker.remote.api.IdResponse;
import de.gesellix.util.QueryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageConfigClient implements ManageConfig {

  private final Logger log = LoggerFactory.getLogger(ManageConfigClient.class);
  private final EngineApiClient client;

  public ManageConfigClient(EngineApiClient client) {
    this.client = client;
  }

  @Override
  public EngineResponseContent<IdResponse> createConfig(String name, byte[] configData, Map<String, String> labels) {
    log.info("docker config create");
    String configDataBase64 = Base64.getEncoder().encodeToString(configData);
    ConfigCreateRequest configConfig = new ConfigCreateRequest(name, labels, configDataBase64, null);
    IdResponse response = client.getConfigApi().configCreate(configConfig);
    return new EngineResponseContent<>(response);
  }

  @Override
  public EngineResponseContent<IdResponse> createConfig(String name, byte[] configData) {
    return createConfig(name, configData, new HashMap<>());
  }

  @Override
  public EngineResponseContent<Config> inspectConfig(String configId) {
    log.info("docker config inspect");
    Config configInspect = client.getConfigApi().configInspect(configId);
    return new EngineResponseContent<>(configInspect);
  }

  /**
   * @see #configs(String)
   * @deprecated use {@link #configs(String)}
   */
  @Deprecated
  @Override
  public EngineResponseContent<List<Config>> configs(Map<String, Object> query) {
    Map<String, Object> actualQuery = new HashMap<>();
    if (query != null) {
      actualQuery.putAll(query);
    }

    new QueryUtil().jsonEncodeQueryParameter(actualQuery, "filters");
    return configs((String) actualQuery.get("filters"));
  }

  @Override
  public EngineResponseContent<List<Config>> configs(String filters) {
    log.info("docker config ls");
    List<Config> configs = client.getConfigApi().configList(filters);
    return new EngineResponseContent<>(configs);
  }

  @Override
  public EngineResponseContent<List<Config>> configs() {
    return configs((String) null);
  }

  @Override
  public void rmConfig(String configId) {
    log.info("docker config rm");
    client.getConfigApi().configDelete(configId);
  }

  @Override
  public void updateConfig(String configId, long version, ConfigSpec configSpec) {
    log.info("docker config update");
    client.getConfigApi().configUpdate(configId, version, configSpec);
  }
}
