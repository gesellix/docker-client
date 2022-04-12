package de.gesellix.docker.client.secret;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.remote.api.IdResponse;
import de.gesellix.docker.remote.api.Secret;
import de.gesellix.docker.remote.api.SecretSpec;

import java.util.List;
import java.util.Map;

public interface ManageSecret {

  EngineResponseContent<IdResponse> createSecret(String name, byte[] secretData);

  EngineResponseContent<IdResponse> createSecret(String name, byte[] secretData, Map<String, String> labels);

  EngineResponseContent<Secret> inspectSecret(String secretId);

  /**
   * @see #secrets(String)
   * @deprecated use {@link #secrets(String)}
   */
  @Deprecated
  EngineResponseContent<List<Secret>> secrets(Map<String, Object> query);

  EngineResponseContent<List<Secret>> secrets();

  EngineResponseContent<List<Secret>> secrets(String filters);

  void rmSecret(String secretId);

  void updateSecret(String secretId, long version, SecretSpec secretSpec);
}
