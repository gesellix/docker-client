package de.gesellix.docker.client.authentication;

import de.gesellix.docker.authentication.AuthConfig;
import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.remote.api.SystemAuthResponse;

import java.io.File;
import java.util.Map;

public interface ManageAuthentication {

  Map<String, AuthConfig> getAllAuthConfigs();

  Map<String, AuthConfig> getAllAuthConfigs(File dockerCfgOrNull);

  AuthConfig readDefaultAuthConfig();

  AuthConfig readAuthConfig(String hostnameOrNull, File dockerCfgOrNull);

  AuthConfig resolveAuthConfigForImage(String image);

  String retrieveEncodedAuthTokenForImage(String image);

  String encodeAuthConfig(AuthConfig authConfig);

  String encodeAuthConfigs(Map<String, AuthConfig> authConfigs);

  EngineResponseContent<SystemAuthResponse> auth(de.gesellix.docker.remote.api.AuthConfig authDetails);
}
