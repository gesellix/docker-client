package de.gesellix.docker.client.authentication;

import java.util.Map;

public interface CredsStore {

  AuthConfig getAuthConfig(String registry);

  Map<String, AuthConfig> getAuthConfigs();
}
