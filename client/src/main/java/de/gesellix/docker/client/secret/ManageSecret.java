package de.gesellix.docker.client.secret;

import de.gesellix.docker.engine.EngineResponse;
import de.gesellix.docker.remote.api.IdResponse;
import de.gesellix.docker.remote.api.Secret;
import de.gesellix.docker.remote.api.SecretSpec;

import java.util.List;
import java.util.Map;

public interface ManageSecret {

  EngineResponse<IdResponse> createSecret(String name, byte[] secretData);

  EngineResponse<IdResponse> createSecret(String name, byte[] secretData, Map<String, String> labels);

  EngineResponse<Secret> inspectSecret(String secretId);

  /**
   * @see #secrets(String)
   * @deprecated use {@link #secrets(String)}
   */
  @Deprecated
  EngineResponse<List<Secret>> secrets(Map<String, Object> query);

  EngineResponse<List<Secret>> secrets();

  EngineResponse<List<Secret>> secrets(String filters);

  void rmSecret(String secretId);

  void updateSecret(String secretId, long version, SecretSpec secretSpec);
}
