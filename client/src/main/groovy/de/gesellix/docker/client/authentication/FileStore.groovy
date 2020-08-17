package de.gesellix.docker.client.authentication

import static de.gesellix.docker.client.authentication.AuthConfig.EMPTY_AUTH_CONFIG

class FileStore implements CredsStore {

    private Map<String, Map> config

    private transient Map<String, AuthConfig> allAuthConfigs

    FileStore(Map<String, Map> config) {
        this.config = config['auths'] ? config.auths as Map : config
    }

    @Override
    AuthConfig getAuthConfig(String registry) {
        return getAuthConfigs()[registry] ?: EMPTY_AUTH_CONFIG
    }

    @Override
    Map<String, AuthConfig> getAuthConfigs() {
        if (!allAuthConfigs) {
            allAuthConfigs = config.findAll { it.value?.auth }.collectEntries { String registry, Map value ->
                def (username, password) = new String(value.auth.decodeBase64()).split(":")
                return [(registry): new AuthConfig(
                        serveraddress: registry,
                        username: username,
                        password: password,
                        email: value.email)]
            }
        }
        return allAuthConfigs
    }
}
