package de.gesellix.docker.client.secret

import de.gesellix.docker.client.EngineResponseContent
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.IdResponse
import de.gesellix.docker.remote.api.Secret
import de.gesellix.docker.remote.api.SecretCreateRequest
import de.gesellix.docker.remote.api.SecretSpec
import de.gesellix.util.QueryUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ManageSecretClient implements ManageSecret {

  private final Logger log = LoggerFactory.getLogger(ManageSecretClient)

  private EngineApiClient client

  ManageSecretClient(EngineApiClient client) {
    this.client = client
  }

  @Override
  EngineResponseContent<IdResponse> createSecret(String name, byte[] secretData, Map<String, String> labels = [:]) {
    log.info("docker secret create")
    String secretDataBase64 = Base64.encoder.encodeToString(secretData)
    SecretCreateRequest secretConfig = new SecretCreateRequest(name, labels, secretDataBase64, null, null)
    IdResponse secretCreate = client.secretApi.secretCreate(secretConfig)
    return new EngineResponseContent<IdResponse>(secretCreate)
  }

  @Override
  EngineResponseContent<Secret> inspectSecret(String secretId) {
    log.info("docker secret inspect")
    Secret secretInspect = client.secretApi.secretInspect(secretId)
    return new EngineResponseContent<Secret>(secretInspect)
  }

  @Override
  EngineResponseContent<List<Secret>> secrets(Map<String, Object> query) {
    Map<String, Object> actualQuery = new HashMap<String, Object>()
    if (query) {
      actualQuery.putAll(query)
    }
    new QueryUtil().jsonEncodeQueryParameter(actualQuery, "filters")
    return secrets(actualQuery.filters as String)
  }

  @Override
  EngineResponseContent<List<Secret>> secrets(String filters = null) {
    log.info("docker secret ls")
    List<Secret> secrets = client.secretApi.secretList(filters)
    return new EngineResponseContent<List<Secret>>(secrets)
  }

  @Override
  void rmSecret(String secretId) {
    log.info("docker secret rm")
    client.secretApi.secretDelete(secretId)
  }

  @Override
  void updateSecret(String secretId, long version, SecretSpec secretSpec) {
    log.info("docker secret update")
    client.secretApi.secretUpdate(secretId, version, secretSpec)
  }
}
