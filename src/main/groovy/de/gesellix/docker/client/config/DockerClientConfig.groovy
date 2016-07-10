package de.gesellix.docker.client.config

import groovy.util.logging.Slf4j

@Slf4j
class DockerClientConfig {

    DockerEnv env

    def scheme
    def host
    def port
    def certPath

    DockerClientConfig() {
        this(new DockerEnv())
    }

    DockerClientConfig(String dockerHost) {
        this(new DockerEnv(dockerHost: dockerHost))
    }

    DockerClientConfig(DockerEnv config) {
        apply(config)
    }

    def apply(DockerEnv env) {
        if (!env.dockerHost) {
            throw new IllegalStateException("dockerHost must be set")
        }
        this.env = env

        def dockerClientConfig = getActualConfig(env)
        this.scheme = dockerClientConfig.protocol
        this.host = dockerClientConfig.host
        this.port = dockerClientConfig.port
        this.certPath = dockerClientConfig.certPath
    }

    def getActualConfig(DockerEnv env) {
        String dockerHost = env.dockerHost
        if (!dockerHost) {
            throw new IllegalStateException("dockerHost must be set")
        }
        def oldProtocol = dockerHost.split("://", 2)[0]
        def protocol = oldProtocol
        def result = [:]
        switch (protocol) {
            case "http":
            case "https":
            case "tcp":
                URL candidateURL = new URL(dockerHost.replaceFirst("^${oldProtocol}://", "https://"))
                TlsConfig tlsConfig = getTlsConfig(candidateURL, env)
                if (tlsConfig.tlsVerify) {
                    log.debug("assume 'https'")
                    protocol = "https"
                    result.certPath = tlsConfig.certPath
                } else {
                    log.debug("assume 'http'")
                    protocol = "http"
                    result.certPath = null
                }
                def tcpUrl = new URL(dockerHost.replaceFirst("^${oldProtocol}://", "${protocol}://"))
                result.protocol = tcpUrl.protocol
                result.host = tcpUrl.host
                result.port = tcpUrl.port
                break
            case "unix":
                log.debug("is 'unix'")
                def dockerUnixSocket = dockerHost.replaceFirst("unix://", "")
                result.protocol = 'unix'
                result.host = dockerUnixSocket
                result.port = -1
                result.certPath = null
                break
            case "npipe":
                log.debug("is 'named pipe'")
                def dockerNamedPipe = dockerHost.replaceFirst("npipe://", "")
                result.protocol = 'npipe'
                result.host = dockerNamedPipe
                result.port = -1
                result.certPath = null
                break
            default:
                log.warn("protocol '${protocol}' not supported")
                def url = new URL(dockerHost)
                result.protocol = url.protocol
                result.host = url.host
                result.port = url.port
                result.certPath = null
                break
        }
        log.debug("selected dockerHost at '${result}'")
        return result
    }

    def getTlsConfig(URL candidateURL, DockerEnv env) {
        // Setting env.DOCKER_TLS_VERIFY to the empty string disables tls verification,
        // while any other value (including "0" or "false") enables tls verification.
        // See https://docs.docker.com/engine/reference/commandline/cli/#environment-variables

        // explicitly disabled?
        if (env.tlsVerify == "") {
            log.debug("dockerTlsVerify='${env.tlsVerify}'")
            return new TlsConfig(false, null)
        }

        def certPath = getCertPathOrNull(env)
        def certsPathExists = certPath != null

        // explicitly enabled?
        if (env.tlsVerify) {
            if (!certsPathExists) {
                throw new IllegalStateException("tlsverify='${env.tlsVerify}', but '${env.certPath}' doesn't exist")
            } else {
                log.debug("certsPathExists=${certsPathExists}")
                return new TlsConfig(true, certPath)
            }
        }

        // make a guess if we could use tls, when it's neither explicitly enabled nor disabled
        def isTlsPort = candidateURL.port == env.defaultTlsPort
        log.debug("certsPathExists=${certsPathExists}, isTlsPort=${isTlsPort}")
        return new TlsConfig(certsPathExists && isTlsPort, certPath)
    }

    String getCertPathOrNull(DockerEnv env) {
        def certsPathExists = env.certPath && new File(env.certPath, "").isDirectory()
        if (!certsPathExists) {
            if (env.defaultCertPath && new File(env.defaultCertPath, "").isDirectory()) {
                log.debug("defaultDockerCertPath=${env.defaultCertPath}")
                return env.defaultCertPath
            }
            return null
        } else {
            log.debug("dockerCertPath=${env.certPath}")
            return env.certPath
        }
    }

    static class TlsConfig {

        boolean tlsVerify = false
        String certPath = null

        TlsConfig(boolean tlsVerify, String certPath) {
            this.tlsVerify = tlsVerify
            this.certPath = certPath
        }
    }
}
