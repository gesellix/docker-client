package de.gesellix.docker.client.authentication

import groovy.util.logging.Slf4j

import static de.gesellix.docker.client.authentication.AuthConfig.EMPTY_AUTH_CONFIG

@Slf4j
class NativeStore implements CredsStore {

  CredsStoreHelper credsStoreHelper
  private String credStoreName

  final String TOKEN_USERNAME = "<token>"

  NativeStore(String credStoreName) {
    this.credStoreName = credStoreName
    this.credsStoreHelper = new CredsStoreHelper()
  }

  @Override
  AuthConfig getAuthConfig(String registry) {
    def creds = credsStoreHelper.getAuthentication(credStoreName, registry)
    if (creds.error) {
      log.info("Error reading credentials from 'credsStore=${credStoreName}' for authentication at ${registry}: ${creds.error}")
      return EMPTY_AUTH_CONFIG
    }
    else if (creds.data) {
      log.info("Got credentials from 'credsStore=${credStoreName}'")
      AuthConfig result = parseCreds(creds.data)
      result.serveraddress = registry
      return result
    }
    else {
      log.warn("Using 'credsStore=${credStoreName}' for authentication at ${registry} is currently not supported")
      return EMPTY_AUTH_CONFIG
    }
  }

  @Override
  Map<String, AuthConfig> getAuthConfigs() {
    Map<String, AuthConfig> result = [:]
    def creds = credsStoreHelper.getAllAuthentications(credStoreName)
    if (creds.error) {
      log.info("Error reading credentials from 'credsStore=${credStoreName}': ${creds.error}")
      return result
    }
    else if (creds.data) {
      log.info("Got credentials from 'credsStore=${credStoreName}'")
      creds.data.keySet().each { String registry ->
        result[registry] = getAuthConfig(registry)
      }
      return result
    }
    else {
      log.warn("Using 'credsStore=${credStoreName}' is currently not supported")
      return result
    }
  }

  private AuthConfig parseCreds(Map creds) {
    def authDetails
    if (creds.Username == TOKEN_USERNAME) {
      authDetails = new AuthConfig()
      authDetails.identitytoken = creds.Secret
    }
    else {
      authDetails = new AuthConfig()
      authDetails.username = creds.Username
      authDetails.password = creds.Secret
    }
    authDetails
  }
}
