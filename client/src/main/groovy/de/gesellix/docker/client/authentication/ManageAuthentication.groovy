package de.gesellix.docker.client.authentication

interface ManageAuthentication {

    def readDefaultAuthConfig()

    def readAuthConfig(hostnameOrNull, File dockerCfgOrNull)

    String retrieveEncodedAuthTokenForImage(String image)

    String encodeAuthConfig(authConfig)

    def auth(authDetails)
}
