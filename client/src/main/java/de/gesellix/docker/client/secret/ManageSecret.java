package de.gesellix.docker.client.secret;

import de.gesellix.docker.engine.EngineResponse;

import java.util.Map;

public interface ManageSecret {

  EngineResponse createSecret(String name, byte[] secretData);

  EngineResponse createSecret(String name, byte[] secretData, Map<String, String> labels);

  EngineResponse inspectSecret(String secretId);

  EngineResponse secrets();

  EngineResponse secrets(Map<String, Object> query);

  EngineResponse rmSecret(String secretId);

  EngineResponse updateSecret(String secretId, int version, Map<String, Object> secretSpec);
}
