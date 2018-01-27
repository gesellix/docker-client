package de.gesellix.docker.client.registry

import de.gesellix.docker.client.authentication.ManageAuthentication
import de.gesellix.docker.client.system.ManageSystem
import de.gesellix.docker.engine.DockerEnv
import groovy.util.logging.Slf4j

@Slf4j
class RegistryElection {

    private ManageSystem system
    private ManageAuthentication authentication

    RegistryElection(ManageSystem system, ManageAuthentication authentication) {
        this.authentication = authentication
        this.system = system
    }

    // ResolveAuthConfig is like registry.ResolveAuthConfig, but if using the
    // default index, it uses the default index name for the daemon's platform,
    // not the client's platform.
    Map resolveAuthConfig(String indexName, boolean officialIndex) {//types.AuthConfig {
        String configKey = indexName
        if (officialIndex) {
            configKey = electAuthServer()
        }

        return authentication.readAuthConfig(configKey, null)
    }

    // ElectAuthServer returns the default registry to use (by asking the daemon)
    String electAuthServer() {
        // The daemon `/info` endpoint informs us of the default registry being
        // used. This is essential in cross-platforms environment, where for
        // example a Linux client might be interacting with a Windows daemon, hence
        // the default registry URL might be Windows specific.
        def serverAddress = new DockerEnv().indexUrl_v1
        try {
            def info = system.info().content
            if (!info?.IndexServerAddress) {
                log.warn("Empty registry endpoint from daemon. Using system default: ${serverAddress}")
            }
            else {
                serverAddress = info.IndexServerAddress
            }
        }
        catch (Exception e) {
            log.warn("Failed to get default registry endpoint from daemon. Using system default: ${serverAddress}", e)
        }
        return serverAddress
    }
}
