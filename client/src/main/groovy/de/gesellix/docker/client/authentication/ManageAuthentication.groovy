package de.gesellix.docker.client.authentication

import de.gesellix.docker.engine.EngineResponse

interface ManageAuthentication {

    Map readDefaultAuthConfig()

    Map readAuthConfig(String hostnameOrNull, File dockerCfgOrNull)

    String retrieveEncodedAuthTokenForImage(String image)

    String encodeAuthConfig(authConfig)

    EngineResponse auth(authDetails)
}
