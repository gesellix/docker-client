package de.gesellix.docker.client.config

import de.gesellix.docker.client.EngineResponseContent
import de.gesellix.docker.remote.api.Config
import de.gesellix.docker.remote.api.ConfigCreateRequest
import de.gesellix.docker.remote.api.ConfigSpec
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.IdResponse
import de.gesellix.util.QueryUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ManageConfigClient implements ManageConfig {

  private final Logger log = LoggerFactory.getLogger(ManageConfigClient)

  private EngineApiClient client

  ManageConfigClient(EngineApiClient client) {
    this.client = client
  }

  @Override
  EngineResponseContent<IdResponse> createConfig(String name, byte[] configData, Map<String, String> labels = [:]) {
    log.info("docker config create")
    String configDataBase64 = Base64.encoder.encodeToString(configData)
    ConfigCreateRequest configConfig = new ConfigCreateRequest(name, labels, configDataBase64, null)
    IdResponse response = client.configApi.configCreate(configConfig)
    return new EngineResponseContent<IdResponse>(response)
  }

  @Override
  EngineResponseContent<Config> inspectConfig(String configId) {
    log.info("docker config inspect")
    Config configInspect = client.configApi.configInspect(configId)
    return new EngineResponseContent(configInspect)
  }

  @Override
  EngineResponseContent<List<Config>> configs(Map query) {
    Map<String, Object> actualQuery = new HashMap<String, Object>()
    if (query) {
      actualQuery.putAll(query)
    }
    new QueryUtil().jsonEncodeQueryParameter(actualQuery, "filters")
    return configs(actualQuery.filters as String)
  }

  @Override
  EngineResponseContent<List<Config>> configs(String filters = null) {
    log.info("docker config ls")
    List<Config> configs = client.configApi.configList(filters)
    return new EngineResponseContent<List<Config>>(configs)
  }

  @Override
  void rmConfig(String configId) {
    log.info("docker config rm")
    client.configApi.configDelete(configId)
  }

  @Override
  void updateConfig(String configId, long version, ConfigSpec configSpec) {
    log.info("docker config update")
    client.configApi.configUpdate(configId, version, configSpec)
  }
}
