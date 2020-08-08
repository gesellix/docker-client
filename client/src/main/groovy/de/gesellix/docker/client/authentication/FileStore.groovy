package de.gesellix.docker.client.authentication

class FileStore implements CredsStore {

    private Map config

    FileStore(Map config) {
        this.config = config['auths'] ? config.auths as Map : config
    }

    @Override
    AuthConfig getAuthConfig(String registry) {
        if (config[registry]) {
            def auth = config[registry].auth as String
            def (username, password) = new String(auth.decodeBase64()).split(":")

            def authDetails = new AuthConfig()
            authDetails.serveraddress = registry
            authDetails.username = username
            authDetails.password = password
            authDetails.email = config[registry].email
            return authDetails
        }
        else {
            return new AuthConfig()
        }
    }

    @Override
    Map<String, AuthConfig> getAuthConfigs() {
        // TODO convert each value to AuthConfig
//        return config
        return [:]
    }
}
