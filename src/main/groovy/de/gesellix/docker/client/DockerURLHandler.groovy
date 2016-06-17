package de.gesellix.docker.client

import groovy.util.logging.Slf4j

@Slf4j
class DockerURLHandler {

    DockerConfig config = new DockerConfig()

    def getProtocolAndHost(String dockerHost) {
        if (!dockerHost) {
            throw new IllegalStateException("dockerHost must be set")
        }
        def dockerBaseUrl = getBaseURLWithActualProtocol(dockerHost)
        return [dockerBaseUrl.protocol, dockerBaseUrl.host, dockerBaseUrl.port]
    }

    def getBaseURLWithActualProtocol(String dockerHost) {
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
                if (shouldUseTls(new URL(dockerHost.replaceFirst("^${oldProtocol}://", "https://")))) {
                    log.debug("assume 'https'")
                    protocol = "https"
                } else {
                    log.debug("assume 'http'")
                    protocol = "http"
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
                break
            case "npipe":
                log.debug("is 'named pipe'")
                def dockerNamedPipe = dockerHost.replaceFirst("npipe://", "")
                result.protocol = 'npipe'
                result.host = dockerNamedPipe
                result.port = -1
                break
            default:
                log.warn("protocol '${protocol}' not supported")
                def url = new URL(dockerHost)
                result.protocol = url.protocol
                result.host = url.host
                result.port = url.port
                break
        }
        log.debug("selected dockerHost at '${result}'")
        return result
    }

    def shouldUseTls(URL candidateURL) {
        // Setting env.DOCKER_TLS_VERIFY to the empty string disables tls verification,
        // while any other value enables tls verification.
        // See https://docs.docker.com/engine/reference/commandline/cli/#environment-variables

        // explicitly disabled?
        if (config.tlsVerify == "") {
            log.debug("dockerTlsVerify='${config.tlsVerify}'")
            return false
        }

        def certsPathExists = config.certPath && new File(config.certPath, "").isDirectory()
        if (!certsPathExists) {
            if (config.defaultCertPath && new File(config.defaultCertPath, "").isDirectory()) {
                log.debug("defaultDockerCertPath=${config.defaultCertPath}")
                config.certPath = config.defaultCertPath
                certsPathExists = true
            }
        } else {
            log.debug("dockerCertPath=${config.certPath}")
        }

        // explicitly enabled?
        if (config.tlsVerify) {
            if (!certsPathExists) {
                throw new IllegalStateException("tlsverify='${config.tlsVerify}', but '${config.certPath}' doesn't exist")
            } else {
                log.debug("certsPathExists=${certsPathExists}")
                return true
            }
        }

        // make a guess if we could use tls, when it's neither explicitly enabled nor disabled
        def isTlsPort = candidateURL.port == config.defaultTlsPort
        log.debug("certsPathExists=${certsPathExists}, isTlsPort=${isTlsPort}")
        return certsPathExists && isTlsPort
    }
}
