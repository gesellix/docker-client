package de.gesellix.docker.client.authentication;

import de.gesellix.docker.authentication.AuthConfig;
import de.gesellix.docker.authentication.AuthConfigReader;
import de.gesellix.docker.engine.DockerEnv;
import de.gesellix.docker.remote.api.SystemInfo;
import de.gesellix.docker.remote.api.client.SystemApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistryElection {

  private static final Logger log = LoggerFactory.getLogger(RegistryElection.class);

  private final SystemApi systemApi;
  private final AuthConfigReader authConfigReader;

  public RegistryElection(SystemApi systemApi, AuthConfigReader authConfigReader) {
    this.systemApi = systemApi;
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

  // ElectAuthServer returns the default registry to use (by asking the daemon)
  public String electAuthServer() {
    // The daemon `/info` endpoint informs us of the default registry being
    // used. This is essential in cross-platforms environment, where for
    // example a Linux client might be interacting with a Windows daemon, hence
    // the default registry URL might be Windows specific.
    final String defaultServerAddress = new DockerEnv().getIndexUrl_v1();
    try {
      SystemInfo info = systemApi.systemInfo();
      if (info == null || info.getIndexServerAddress() == null || info.getIndexServerAddress().isEmpty()) {
        log.warn("Empty registry endpoint from daemon. Using system default: " + defaultServerAddress);
      }
      else {
        return info.getIndexServerAddress();
      }
    }
    catch (Exception e) {
      log.warn("Failed to get default registry endpoint from daemon. Using system default: " + defaultServerAddress, e);
    }
    return defaultServerAddress;
  }
}
