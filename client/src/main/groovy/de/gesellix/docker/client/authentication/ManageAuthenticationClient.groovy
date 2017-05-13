package de.gesellix.docker.client.authentication

import com.google.re2j.Pattern
import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.HttpClient
import de.gesellix.docker.client.config.DockerEnv
import de.gesellix.util.QueryUtil
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

@Slf4j
class ManageAuthenticationClient implements ManageAuthentication {

    private DockerEnv env
    private HttpClient client
    private DockerResponseHandler responseHandler
    private QueryUtil queryUtil

    ManageAuthenticationClient(DockerEnv env, HttpClient client, DockerResponseHandler responseHandler) {
        this.env = env
        this.client = client
        this.responseHandler = responseHandler
        this.queryUtil = new QueryUtil()
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
        } else {
            authConfig = parsedDockerCfg
        }

        if (!authConfig[hostname]) {
            if (parsedDockerCfg['credsStore']) {
                log.warn("Using 'credsStore=${parsedDockerCfg['credsStore']}' for authentication at ${hostname} is currently not supported")
            }
            return [:]
        }

        def authDetails = ["username"     : "UNKNOWN-USERNAME",
                           "password"     : "UNKNOWN-PASSWORD",
                           "email"        : "UNKNOWN-EMAIL",
                           "serveraddress": hostname]


        def auth = authConfig[hostname].auth as String
        def (username, password) = new String(auth.decodeBase64()).split(":")
        authDetails.username = username
        authDetails.password = password
        authDetails.email = authConfig[hostname].email

        return authDetails
    }

    @Override
    String encodeAuthConfig(authConfig) {
        log.debug "encode authConfig for ${authConfig.username}@${authConfig.serveraddress}"
        return new JsonBuilder(authConfig).toString().bytes.encodeBase64().toString()
    }

    @Override
    auth(authDetails) {
        log.info "docker login"
        def response = client.post([path              : "/auth",
                                    body              : authDetails,
                                    requestContentType: "application/json"])
        return response
    }

    String retrieveEncodedAuthTokenForImage(String image) {
        return ""
//        def authConfig = resolveAuthConfigForImage(image)
//        return encodeAuthConfig(authConfig)
    }

    def resolveAuthConfigForImage(String image) {
//        registryRef, err := reference.ParseNormalizedNamed(image)
//        if err != nil {
//            return types.AuthConfig{}, err
//        }
//        repoInfo, err := registry.ParseRepositoryInfo(registryRef)
//        if err != nil {
//            return types.AuthConfig{}, err
//        }
//        return ResolveAuthConfig(ctx, cli, repoInfo.Index), nil

        if (/^([a-f0-9]{64})$/.matches(image)) {
            throw new IllegalArgumentException("invalid repository name (${image}), cannot specify 64-byte hexadecimal strings")
        }
        String domain
        String remainder
        (domain, remainder) = splitDockerDomain(image)

        String remoteName
        if (remainder.contains(':')) {
            remoteName = remainder.substring(0, remainder.indexOf(':'))
        } else {
            remoteName = remainder
        }
        if (remoteName.toLowerCase() != remoteName) {
            throw new IllegalArgumentException("invalid reference format: repository name must be lowercase")
        }

//        def ref = parse(domain + "/" + remainder)
//        named, isNamed: = ref.(Named)
//        if (!isNamed) {
//            throw new IllegalStateException("reference ${ref.String()} has no name")
//        }
//        return named, nil
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
        } else {
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

    String domainComponentRegexp = "(?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9])"

    String separatorRegexp = "(?:[._]|__|[-]*)"

    String nameComponentRegexp = "[a-z0-9]+(?:(?:${separatorRegexp}[a-z0-9]+)+)?"

    String DomainRegexp = "${domainComponentRegexp}(?:(?:\\.${domainComponentRegexp})+)?(?:\\:[0-9]+)?"

    String NameRegexp = "(?:${DomainRegexp})?\\/${nameComponentRegexp}(?:(?:\\/)+)?${nameComponentRegexp}"

    String TagRegexp = "[\\w][\\w.-]{0,127}"

    String DigestRegexp = "[A-Za-z][A-Za-z0-9]*(?:[-_+.][A-Za-z][A-Za-z0-9]*)*[:][[:xdigit:]]{32,}"

    String ReferenceRegexp = "^(${NameRegexp})(?:\\:(${TagRegexp}))?(?:\\@(${DigestRegexp}))?\$"

    String anchoredNameRegexp = "^(?:(${DomainRegexp})\\/)?(${nameComponentRegexp}(?:(?:\\/${nameComponentRegexp})+)?)\$"

    int NameTotalLengthMax = 255

// Parse parses s and returns a syntactically valid Reference.
// If an error was encountered it is returned, along with a nil Reference.
// NOTE: Parse will not handle short digests.
    def parse(String s) { //(Reference, error) {
        if (!s) {
            throw new IllegalArgumentException("repository name must have at least one component")
        }

        def pattern = Pattern.compile(ReferenceRegexp)
        def matcher = pattern.matcher(s)
        if (!matcher.matches()) {
            if (pattern.matches(s.toLowerCase())) {
                throw new IllegalArgumentException("repository name must be lowercase")
            }
            throw new IllegalArgumentException("invalid reference format")
        }

        println "group count: ${matcher.groupCount()}"
        if (matcher.group(1).length() > NameTotalLengthMax) {
            throw new IllegalArgumentException("repository name must not be more than ${NameTotalLengthMax} characters")
        }

        def repo = [
                domain: "",
                path  : ""
        ]
        def anchoredNamePattern = Pattern.compile(anchoredNameRegexp)
        def anchoredNameMatcher = anchoredNamePattern.matcher(matcher.group(1))

        if (anchoredNameMatcher.matches()
                && anchoredNameMatcher.groupCount() == 3
                && anchoredNameMatcher.group(1) != null
                && anchoredNameMatcher.group(2) != null) {
            repo.domain = anchoredNameMatcher.group(1)
            repo.path = anchoredNameMatcher.group(2)
        } else {
            repo.domain = ""
            repo.path = anchoredNameMatcher.group(1)
        }

        def ref = [
                namedRepository: repo,
                tag            : matcher.group(2),
                digest         : ""
        ]
        if (matcher.group(3) != null) {
            ValidateDigest(matcher.group(3))
            ref.digest = matcher.group(3)
        }

        def r = getBestReferenceType(ref)
        if (!r) {
            throw new IllegalArgumentException("repository name must have at least one component")
        }
        return r
    }

    String RepoName(Map repo) {
        return repo.domain == "" ? repo.path : repo.domain + "/" + repo.path
    }

    def getBestReferenceType(Map ref) {
        if (!RepoName(ref.namedRepository as Map)) {
            // Allow digest only references
            if (ref.digest) {
                return ref.digest
            }
            return null
        }
        if (!ref.tag) {
            if (ref.digest) {
                return [
                        namedRepository: ref.namedRepository,
                        digest         : ref.digest
                ]
            }
            return ref.namedRepository
        }
        if (!ref.digest) {
            return [
                    namedRepository: ref.namedRepository,
                    tag            : ref.tag
            ]
        }
        return ref
    }

    // why not DigestRegex from above?!
    String DigestRegexp2 = "[a-zA-Z0-9-_+.]+:[a-fA-F0-9]+"
    String DigestRegexpAnchored = "^${DigestRegexp2}\$"

    def ValidateDigest(String s) {
        def i = s.indexOf(':')

        // validate i then run through regexp
        if (i < 0 || i + 1 == s.length() || !s.matches(DigestRegexpAnchored)) {
            throw new IllegalArgumentException("invalid checksum digest format")
        }

        def algorithm = s.substring(0, i)
        if (!knownAlgorithms.contains(algorithm)) {
            throw new IllegalArgumentException("unsupported digest algorithm")
        }

        // Digests much always be hex-encoded, ensuring that their hex portion will always be size*2
        if (algorithmDigestSizes[algorithm] * 2 != s.substring(i + 1)) {
            throw new IllegalArgumentException("invalid checksum digest length")
        }
    }

    def knownAlgorithms = [
            'SHA256',
            'SHA384',
            'SHA512'
    ]

    def algorithmDigestSizes = [
            'SHA256': 32,
            'SHA384': 48,
            'SHA512': 64
    ]
}
