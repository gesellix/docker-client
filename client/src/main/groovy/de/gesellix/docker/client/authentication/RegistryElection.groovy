package de.gesellix.docker.client.authentication

import de.gesellix.docker.authentication.AuthConfig
import de.gesellix.docker.authentication.AuthConfigReader
import de.gesellix.docker.engine.DockerEnv
import de.gesellix.docker.remote.api.client.SystemApi
import groovy.util.logging.Slf4j

@Slf4j
class RegistryElection {

  private SystemApi systemApi
  private AuthConfigReader authConfigReader

  RegistryElection(SystemApi systemApi, AuthConfigReader authConfigReader) {
    this.systemApi = systemApi
    this.authConfigReader = authConfigReader
  }

  // ResolveAuthConfig is like registry.ResolveAuthConfig, but if using the
  // default index, it uses the default index name for the daemon's platform,
  // not the client's platform.
  AuthConfig resolveAuthConfig(String indexName, boolean officialIndex) {
    String configKey = indexName
    if (officialIndex) {
      configKey = electAuthServer()
    }

    return authConfigReader.readAuthConfig(configKey, null)
  }

  // ElectAuthServer returns the default registry to use (by asking the daemon)
  String electAuthServer() {
    // The daemon `/info` endpoint informs us of the default registry being
    // used. This is essential in cross-platforms environment, where for
    // example a Linux client might be interacting with a Windows daemon, hence
    // the default registry URL might be Windows specific.
    String serverAddress = new DockerEnv().indexUrl_v1
    try {
      def info = systemApi.systemInfo()
      if (!info?.indexServerAddress) {
        log.warn("Empty registry endpoint from daemon. Using system default: ${serverAddress}")
      }
      else {
        serverAddress = info.indexServerAddress
      }
    }
    catch (Exception e) {
      log.warn("Failed to get default registry endpoint from daemon. Using system default: ${serverAddress}", e)
    }
    return serverAddress
  }
}
