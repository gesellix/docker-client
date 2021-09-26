package de.gesellix.docker.client.authentication;

import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

import static de.gesellix.docker.client.authentication.AuthConfig.EMPTY_AUTH_CONFIG;

public class FileStore implements CredsStore {

  private final Map<String, Map> config;
  private transient Map<String, AuthConfig> allAuthConfigs;

  public FileStore(Map<String, Map> config) {
    this.config = config.containsKey("auths") ? (Map) config.get("auths") : config;
  }

  @Override
  public AuthConfig getAuthConfig(String registry) {
    final AuthConfig authConfig = getAuthConfigs().get(registry);
    return authConfig != null ? authConfig : EMPTY_AUTH_CONFIG;
  }

  @Override
  public Map<String, AuthConfig> getAuthConfigs() {
    if (allAuthConfigs == null) {
      allAuthConfigs = config.entrySet().stream()
          .filter((e) -> e.getValue() != null && e.getValue().get("auth") != null)
          .collect(Collectors.toMap(
              Map.Entry::getKey,
              e -> {
                String registry = e.getKey();
                Map value = e.getValue();
                String[] login = new String(Base64.getDecoder().decode((String) value.get("auth"))).split(":");
                String username = login[0];
                String password = login[1];

                AuthConfig authConfig = new AuthConfig();
                authConfig.setServeraddress(registry);
                authConfig.setUsername(username);
                authConfig.setPassword(password);
                authConfig.setEmail((String) value.get("email"));
                return authConfig;
              }
          ));
    }
    return allAuthConfigs;
  }
}
