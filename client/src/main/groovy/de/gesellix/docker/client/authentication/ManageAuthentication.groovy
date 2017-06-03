package de.gesellix.docker.client.authentication

import de.gesellix.docker.client.DockerResponse

interface ManageAuthentication {

    def readDefaultAuthConfig()

    def readAuthConfig(hostnameOrNull, File dockerCfgOrNull)

    String retrieveEncodedAuthTokenForImage(String image)

    String encodeAuthConfig(authConfig)

    DockerResponse auth(authDetails)
}
