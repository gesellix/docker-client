package de.gesellix.docker.client.authentication

import com.squareup.moshi.Moshi
import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.distribution.ReferenceParser
import de.gesellix.docker.client.registry.RegistryElection
import de.gesellix.docker.client.system.ManageSystem
import de.gesellix.docker.engine.DockerEnv
import de.gesellix.docker.engine.EngineClient
import de.gesellix.docker.engine.EngineResponse
import groovy.util.logging.Slf4j
import okio.Okio

import static de.gesellix.docker.client.authentication.AuthConfig.EMPTY_AUTH_CONFIG

@Slf4j
class ManageAuthenticationClient implements ManageAuthentication {

    private DockerEnv env
    private EngineClient client
    private DockerResponseHandler responseHandler
    private RegistryElection registryElection

    private Moshi moshi = new Moshi.Builder().build()

    ManageAuthenticationClient(DockerEnv env,
                               EngineClient client,
                               DockerResponseHandler responseHandler,
                               ManageSystem manageSystem) {
        this.env = env
        this.client = client
        this.responseHandler = responseHandler
        this.registryElection = new RegistryElection(manageSystem, this)
    }

    @Override
    Map<String, AuthConfig> getAllAuthConfigs(File dockerCfg = null) {
        def parsedDockerCfg = readDockerConfigFile(dockerCfg)
        if (!parsedDockerCfg) {
            return [:]
        }

        CredsStore credsStore = getCredentialsStore(parsedDockerCfg)
        return credsStore.getAuthConfigs()
    }

    @Override
    AuthConfig readDefaultAuthConfig() {
        return readAuthConfig(null, env.getDockerConfigFile())
    }

    @Override
    AuthConfig readAuthConfig(String hostname, File dockerCfg) {
        log.debug "read authConfig"

        if (!hostname) {
            hostname = env.indexUrl_v1
        }

        def parsedDockerCfg = readDockerConfigFile(dockerCfg)
        if (!parsedDockerCfg) {
            return EMPTY_AUTH_CONFIG
        }

        CredsStore credsStore = getCredentialsStore(parsedDockerCfg, hostname)
        return credsStore.getAuthConfig(hostname)
    }

    Map readDockerConfigFile(File dockerCfg) {
        if (!dockerCfg) {
            dockerCfg = env.getDockerConfigFile()
        }
        if (!dockerCfg?.exists()) {
            log.info "docker config '${dockerCfg}' doesn't exist"
            return [:]
        }
        log.debug "reading auth info from ${dockerCfg}"
        return moshi.adapter(Map).fromJson(Okio.buffer(Okio.source(dockerCfg)))
    }

    CredsStore getCredentialsStore(Map parsedDockerCfg, String hostname = "") {
        if (parsedDockerCfg['credHelpers'] && hostname && parsedDockerCfg['credHelpers'][hostname]) {
            return new NativeStore(parsedDockerCfg['credHelpers'][hostname] as String)
        }
        if (parsedDockerCfg['credsStore']) {
            return new NativeStore(parsedDockerCfg['credsStore'] as String)
        }
        return new FileStore(parsedDockerCfg)
    }

    @Override
    String encodeAuthConfig(AuthConfig authConfig) {
        log.debug "encode authConfig for ${authConfig.username}@${authConfig.serveraddress}"
        String json = moshi.adapter(AuthConfig).toJson(authConfig)
        return json.bytes.encodeBase64().toString()
    }

    @Override
    String encodeAuthConfigs(Map<String, AuthConfig> authConfigs) {
        log.debug "encode authConfigs for ${authConfigs.keySet()}"
        String json = moshi.adapter(Map).toJson(authConfigs)
        return json.bytes.encodeBase64().toString()
    }

    @Override
    EngineResponse auth(Map authDetails) {
        log.info "docker login"
        def response = client.post([path              : "/auth",
                                    body              : authDetails,
                                    requestContentType: "application/json"])
        if (response == null || response.status == null || !response.status.success) {
            log.info "login failed for ${authDetails.username}@${authDetails.serveraddress}"
        }
        return response
    }

    @Override
    String retrieveEncodedAuthTokenForImage(String image) {
        def authConfig = resolveAuthConfigForImage(image)
        return encodeAuthConfig(authConfig)
    }

    def resolveAuthConfigForImage(String image) {
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

        def indexName = validateIndexName(namedRef.domain as String)
        def indexInfo = [
                name    : indexName,
                mirrors : [],
                official: false,
                secure  : false
        ]
        return registryElection.resolveAuthConfig(indexInfo.name, indexInfo.official)
    }

    def validateIndexName(String val) {
        if (val == "index.docker.io") {
            val = "docker.io"
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

    String legacyDefaultDomain = "index.docker.io"
    String defaultDomain = "docker.io"
    String officialRepoName = "library"

    // splitDockerDomain splits a repository name to domain and remotename string.
    // If no valid domain is found, the default domain is used. Repository name
    // needs to be already validated before.
    def splitDockerDomain(String name) {
        def containsAny = { String haystack, String needles ->
            needles.any { haystack.contains(it) }
        }
        String domain
        String remainder

        def i = name.indexOf('/')
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
