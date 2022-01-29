package de.gesellix.docker.client.authentication

import com.squareup.moshi.Moshi
import de.gesellix.docker.authentication.AuthConfig
import de.gesellix.docker.authentication.AuthConfigReader
import de.gesellix.docker.authentication.CredsStore
import de.gesellix.docker.client.EngineResponseContent
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.SystemAuthResponse
import groovy.util.logging.Slf4j

@Slf4j
class ManageAuthenticationClient implements ManageAuthentication {

  AuthConfigReader authConfigReader
  EngineApiClient client

  private RegistryElection registryElection

  private Moshi moshi = new Moshi.Builder().build()

  ManageAuthenticationClient(EngineApiClient client, AuthConfigReader authConfigReader) {
    this.client = client
    this.authConfigReader = authConfigReader
    this.registryElection = new RegistryElection(client.getSystemApi(), authConfigReader)
  }

  @Override
  Map<String, AuthConfig> getAllAuthConfigs(File dockerCfg = null) {
    Map parsedDockerCfg = authConfigReader.readDockerConfigFile(dockerCfg)
    if (!parsedDockerCfg) {
      return [:]
    }

    CredsStore credsStore = authConfigReader.getCredentialsStore(parsedDockerCfg)
    return credsStore.getAuthConfigs()
  }

  @Override
  AuthConfig readDefaultAuthConfig() {
    return authConfigReader.readDefaultAuthConfig()
  }

  @Override
  AuthConfig readAuthConfig(String hostname, File dockerCfg) {
    return authConfigReader.readAuthConfig(hostname, dockerCfg)
  }

  @Override
  String encodeAuthConfig(AuthConfig authConfig) {
    log.debug("encode authConfig for ${authConfig.username}@${authConfig.serveraddress}")
    String json = moshi.adapter(AuthConfig).toJson(authConfig)
    return json.bytes.encodeBase64().toString()
  }

  @Override
  String encodeAuthConfigs(Map<String, AuthConfig> authConfigs) {
    log.debug("encode authConfigs for ${authConfigs.keySet()}")
    String json = moshi.adapter(Map).toJson(authConfigs)
    return json.bytes.encodeBase64().toString()
  }

  @Override
  EngineResponseContent<SystemAuthResponse> auth(de.gesellix.docker.remote.api.AuthConfig authDetails) {
    log.info("docker login")
    SystemAuthResponse systemAuth = client.systemApi.systemAuth(authDetails)
    return new EngineResponseContent(systemAuth)
  }

  @Override
  String retrieveEncodedAuthTokenForImage(String image) {
    AuthConfig authConfig = resolveAuthConfigForImage(image)
    return encodeAuthConfig(authConfig)
  }

  @Override
  AuthConfig resolveAuthConfigForImage(String image) {
    if (/^([a-f0-9]{64})$/.matches(image)) {
      throw new IllegalArgumentException("invalid repository name (${image}), cannot specify 64-byte hexadecimal strings")
    }
    String domain
    String remainder
    (domain, remainder) = splitDockerDomain(image)

    String remoteName
    if (remainder.contains(':')) {
      remoteName = remainder.substring(0, remainder.indexOf(':'))
    }
    else {
      remoteName = remainder
    }
    if (remoteName.toLowerCase() != remoteName) {
      throw new IllegalArgumentException("invalid reference format: repository name must be lowercase")
    }

    def ref = new ReferenceParser().parse(domain + "/" + remainder)

    // expect [domain: "...", path: "..."]
    def namedRef = getNamed(ref)

    String indexName = validateIndexName(namedRef.domain as String)
    Map indexInfo = [
        name    : indexName,
        mirrors : [],
        official: indexName == defaultDomain,
        secure  : false
    ]
    return registryElection.resolveAuthConfig(indexInfo.name, indexInfo.official)
  }

  String validateIndexName(String val) {
    if (val == legacyDefaultDomain) {
      val = defaultDomain
    }
    if (val.startsWith("-") || val.endsWith("-")) {
      throw new IllegalStateException("Invalid index name ($val). Cannot begin or end with a hyphen.")
    }
    return val
  }

  // A named repository has both domain and path components.
  def getNamed(Map ref) {
    if (ref.domain) {
      return ref
    }
    else if (ref.repo && ref.repo.domain) {
      return ref.repo
    }
    throw new IllegalStateException("reference ${ref} has no name")
  }

  final String legacyDefaultDomain = "index.docker.io"
  final String defaultDomain = "docker.io"
  final String officialRepoName = "library"

  // splitDockerDomain splits a repository name to domain and remotename string.
  // If no valid domain is found, the default domain is used. Repository name
  // needs to be already validated before.
  def splitDockerDomain(String name) {
    def containsAny = { String haystack, String needles ->
      needles.any { haystack.contains(it) }
    }
    String domain
    String remainder

    int i = name.indexOf('/')
    if (i == -1 || (!containsAny(name.substring(0, i), ".:") && name.substring(0, i) != 'localhost')) {
      (domain, remainder) = [defaultDomain, name]
    }
    else {
      (domain, remainder) = [name.substring(0, i), name.substring(i + 1)]
    }
    if (domain == legacyDefaultDomain) {
      domain = defaultDomain
    }
    if (domain == defaultDomain && !remainder.contains('/')) {
      remainder = officialRepoName + "/" + remainder
    }
    return [domain, remainder]
  }
}
