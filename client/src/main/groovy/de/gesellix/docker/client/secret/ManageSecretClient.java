package de.gesellix.docker.client.secret;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.remote.api.EngineApiClient;
import de.gesellix.docker.remote.api.IdResponse;
import de.gesellix.docker.remote.api.Secret;
import de.gesellix.docker.remote.api.SecretCreateRequest;
import de.gesellix.docker.remote.api.SecretSpec;
import de.gesellix.util.QueryParameterEncoder;

public class ManageSecretClient implements ManageSecret {

  private final Logger log = LoggerFactory.getLogger(ManageSecretClient.class);
  private final EngineApiClient client;

  public ManageSecretClient(EngineApiClient client) {
    this.client = client;
  }

  @Override
  public EngineResponseContent<IdResponse> createSecret(String name, byte[] secretData, Map<String, String> labels) {
    log.info("docker secret create");
    String secretDataBase64 = Base64.getEncoder().encodeToString(secretData);
    SecretCreateRequest secretConfig = new SecretCreateRequest(name, labels, secretDataBase64, null, null);
    IdResponse secretCreate = client.getSecretApi().secretCreate(secretConfig);
    return new EngineResponseContent<>(secretCreate);
  }

  @Override
  public EngineResponseContent<IdResponse> createSecret(String name, byte[] secretData) {
    return createSecret(name, secretData, new HashMap<>());
  }

  @Override
  public EngineResponseContent<Secret> inspectSecret(String secretId) {
    log.info("docker secret inspect");
    Secret secretInspect = client.getSecretApi().secretInspect(secretId);
    return new EngineResponseContent<>(secretInspect);
  }

  /**
   * @see #secrets(String)
   * @deprecated use {@link #secrets(String)}
   */
  @Deprecated
  @Override
  public EngineResponseContent<List<Secret>> secrets(Map<String, Object> query) {
    Map<String, Object> actualQuery = new HashMap<>();
    if (query != null) {
      actualQuery.putAll(query);
    }

    new QueryParameterEncoder().jsonEncodeQueryParameter(actualQuery, "filters");
    return secrets((String) actualQuery.get("filters"));
  }

  @Override
  public EngineResponseContent<List<Secret>> secrets(String filters) {
    log.info("docker secret ls");
    List<Secret> secrets = client.getSecretApi().secretList(filters);
    return new EngineResponseContent<>(secrets);
  }

  @Override
  public EngineResponseContent<List<Secret>> secrets() {
    return secrets((String) null);
  }

  @Override
  public void rmSecret(String secretId) {
    log.info("docker secret rm");
    client.getSecretApi().secretDelete(secretId);
  }

  @Override
  public void updateSecret(String secretId, long version, SecretSpec secretSpec) {
    log.info("docker secret update");
    client.getSecretApi().secretUpdate(secretId, version, secretSpec);
  }
}
