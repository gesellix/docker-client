package de.gesellix.docker.client.authentication

import de.gesellix.docker.engine.EngineResponse

interface ManageAuthentication {

    def readDefaultAuthConfig()

    def readAuthConfig(hostnameOrNull, File dockerCfgOrNull)

    String retrieveEncodedAuthTokenForImage(String image)

    String encodeAuthConfig(authConfig)

    EngineResponse auth(authDetails)
}
