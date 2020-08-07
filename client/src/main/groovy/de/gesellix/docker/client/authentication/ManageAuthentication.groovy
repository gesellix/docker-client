package de.gesellix.docker.client.authentication

import de.gesellix.docker.engine.EngineResponse

interface ManageAuthentication {

    AuthConfig readDefaultAuthConfig()

    AuthConfig readAuthConfig(String hostnameOrNull, File dockerCfgOrNull)

    String retrieveEncodedAuthTokenForImage(String image)

    String encodeAuthConfig(AuthConfig authConfig)

    EngineResponse auth(Map authDetails)
}
