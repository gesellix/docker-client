package de.gesellix.docker.client.authentication

interface CredsStore {

    AuthConfig getAuthConfig(String registry)

    Map<String, AuthConfig> getAuthConfigs()
}
