package de.gesellix.docker.client.authentication

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.distribution.ReferenceParser
import de.gesellix.docker.client.system.ManageSystem
import de.gesellix.docker.engine.DockerEnv
import de.gesellix.docker.engine.EngineClient
import de.gesellix.docker.engine.EngineResponse
import de.gesellix.util.QueryUtil
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

@Slf4j
class ManageAuthenticationClient implements ManageAuthentication {

    private DockerEnv env
    private EngineClient client
    private DockerResponseHandler responseHandler
    private ManageSystem manageSystem
    private QueryUtil queryUtil
    private CredsStoreHelper credsStoreHelper

    ManageAuthenticationClient(DockerEnv env,
                               EngineClient client,
                               DockerResponseHandler responseHandler,
                               ManageSystem manageSystem) {
        this.env = env
        this.client = client
        this.responseHandler = responseHandler
        this.manageSystem = manageSystem
        this.queryUtil = new QueryUtil()
        this.credsStoreHelper = new CredsStoreHelper()
    }

    @Override
    readDefaultAuthConfig() {
        return readAuthConfig(null, env.getDockerConfigFile())
    }

    @Override
    readAuthConfig(hostname, File dockerCfg) {
        log.debug "read authConfig"

        if (!dockerCfg) {
            dockerCfg = env.getDockerConfigFile()
        }
        if (!dockerCfg?.exists()) {
            log.info "docker config '${dockerCfg}' doesn't exist"
            return [:]
        }
        log.debug "reading auth info from ${dockerCfg}"
        def parsedDockerCfg = new JsonSlurper().parse(dockerCfg)

        if (!hostname) {
            hostname = env.indexUrl_v1
        }

        def authConfig
        if (parsedDockerCfg['auths']) {
            authConfig = parsedDockerCfg.auths
        }
        else {
            authConfig = parsedDockerCfg
        }

        def authDetails = ["username"     : "UNKNOWN-USERNAME",
                           "password"     : "UNKNOWN-PASSWORD",
                           "email"        : "UNKNOWN-EMAIL",
                           "serveraddress": hostname]

        if (!authConfig[hostname]) {
            if (parsedDockerCfg['credsStore']) {
                def creds = credsStoreHelper.getAuthentication(parsedDockerCfg['credsStore'] as String, hostname)
                if (creds.error) {
                    log.info("Error reading credentials from 'credsStore=${parsedDockerCfg['credsStore']}' for authentication at ${hostname}: ${creds.error}")
                    return [:]
                }
                else if (creds.auth) {
                    log.info("Got credentials from 'credsStore=${parsedDockerCfg['credsStore']}'")
                    authDetails.username = creds.auth.Username
                    authDetails.password = creds.auth.Secret
                    authDetails.email = null
                }
                else {
                    log.warn("Using 'credsStore=${parsedDockerCfg['credsStore']}' for authentication at ${hostname} is currently not supported")
                    return [:]
                }
            }
            else {
                return [:]
            }
        }
        else {
            def auth = authConfig[hostname].auth as String
            def (username, password) = new String(auth.decodeBase64()).split(":")
            authDetails.username = username
            authDetails.password = password
            authDetails.email = authConfig[hostname].email
        }

        return authDetails
    }

    @Override
    String encodeAuthConfig(authConfig) {
        log.debug "encode authConfig for ${authConfig.username}@${authConfig.serveraddress}"
        return new JsonBuilder(authConfig).toString().bytes.encodeBase64().toString()
    }

    @Override
    EngineResponse auth(authDetails) {
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
        return resolveAuthConfig(indexInfo)
    }

    // ResolveAuthConfig is like registry.ResolveAuthConfig, but if using the
    // default index, it uses the default index name for the daemon's platform,
    // not the client's platform.
    def resolveAuthConfig(Map indexInfo) {//types.AuthConfig {
        def configKey = indexInfo.name
        if (indexInfo.official) {
            configKey = electAuthServer()
        }
        return readAuthConfig(configKey, null)
    }

    // ElectAuthServer returns the default registry to use (by asking the daemon)
    def electAuthServer() {
        // The daemon `/info` endpoint informs us of the default registry being
        // used. This is essential in cross-platforms environment, where for
        // example a Linux client might be interacting with a Windows daemon, hence
        // the default registry URL might be Windows specific.
        def serverAddress = new DockerEnv().indexUrl_v1
        try {
            def info = manageSystem.info().content
            if (info.IndexServerAddress == "") {
                log.warn("Warning: Empty registry endpoint from daemon. Using system default: ${serverAddress}")
            }
            else {
                serverAddress = info.IndexServerAddress
            }
        }
        catch (Exception e) {
            log.warn("Warning: failed to get default registry endpoint from daemon. Using system default: ${serverAddress}", e)
        }
        return serverAddress
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
