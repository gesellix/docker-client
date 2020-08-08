package de.gesellix.docker.client.authentication

import groovy.util.logging.Slf4j

@Slf4j
class NativeStore implements CredsStore {

    private CredsStoreHelper credsStoreHelper
    private String credStoreName

    NativeStore(CredsStoreHelper credsStoreHelper, String credStoreName) {
        this.credsStoreHelper = credsStoreHelper
        this.credStoreName = credStoreName
    }

    @Override
    AuthConfig getAuthConfig(String registry) {
        def creds = credsStoreHelper.getAuthentication(credStoreName, registry)
        if (creds.error) {
            log.info("Error reading credentials from 'credsStore=${credStoreName}' for authentication at ${registry}: ${creds.error}")
            return new AuthConfig()
        }
        else if (creds.auth) {
            log.info("Got credentials from 'credsStore=${credStoreName}'")
            def authDetails = new AuthConfig()
            authDetails.serveraddress = registry
            authDetails.username = creds.auth.Username
            authDetails.password = creds.auth.Secret
            return authDetails
        }
        else {
            log.warn("Using 'credsStore=${credStoreName}' for authentication at ${registry} is currently not supported")
            return new AuthConfig()
        }
    }

    @Override
    Map<String, AuthConfig> getAuthConfigs() {
        // TODO
        return [:]
    }
}
