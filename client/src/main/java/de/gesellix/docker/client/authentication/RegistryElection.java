package de.gesellix.docker.client.authentication;

import de.gesellix.docker.authentication.AuthConfig;
import de.gesellix.docker.authentication.AuthConfigReader;
import de.gesellix.docker.engine.DockerEnv;

public class RegistryElection {

  private final AuthConfigReader authConfigReader;

  public RegistryElection(AuthConfigReader authConfigReader) {
    this.authConfigReader = authConfigReader;
  }

  // ResolveAuthConfig is like registry.ResolveAuthConfig, but if using the
  // default index, it uses the default index name for the daemon's platform,
  // not the client's platform.
  public AuthConfig resolveAuthConfig(String indexName, boolean officialIndex) {
    String configKey = indexName;
    if (officialIndex) {
      configKey = electAuthServer();
    }

    return authConfigReader.readAuthConfig(configKey, null);
  }

  // ElectAuthServer returns the default registry to use
  public String electAuthServer() {
    final String defaultServerAddress = new DockerEnv().getIndexUrl_v1();
    return defaultServerAddress;
  }
}
