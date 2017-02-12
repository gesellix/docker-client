package de.gesellix.docker.client.authentication

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
    encodeAuthConfig(authConfig) {
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
}
