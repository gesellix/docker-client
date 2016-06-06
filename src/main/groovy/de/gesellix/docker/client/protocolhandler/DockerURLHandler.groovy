package de.gesellix.docker.client.protocolhandler

import de.gesellix.docker.client.DockerConfig
import groovy.util.logging.Slf4j

@Slf4j
class DockerURLHandler {

    DockerConfig config = new DockerConfig()

    def getRequestUrl(String dockerHost, String path, String query = "") {
        def (String protocol, String host, int port) = getProtocolAndHost(dockerHost)
        if (path && !path.startsWith("/")) {
            path = "/$path"
        }
        if (config.apiVersion) {
            path = "/${config.apiVersion}${path}".toString()
        }
        query = query ?: ""
        if (["npipe", "unix"].contains(protocol)) {
            // slashes need to be escaped, because the file name is used as host name
            return new URL(protocol, URLEncoder.encode(host, "UTF-8"), -1, "${path}${query}", newHandler(protocol))
        }
        return new URL("${protocol}://${host}:${port}${path}${query}")
    }

    def getProtocolAndHost(String dockerHost) {
        if (!dockerHost) {
            throw new IllegalStateException("dockerHost must be set")
        }
        def dockerBaseUrl = getURLWithActualProtocol(dockerHost)
        return [dockerBaseUrl.protocol, dockerBaseUrl.host, dockerBaseUrl.port]
    }

    def newHandler(String protocol) {
        switch (protocol) {
            case "unix": return new sun.net.www.protocol.unix.Handler()
            case "npipe": return new sun.net.www.protocol.npipe.Handler()
            default: throw new IllegalStateException("cannot handle '${protocol}'")
        }
    }

    URL getURLWithActualProtocol(String dockerHost) {
        if (!dockerHost) {
            throw new IllegalStateException("dockerHost must be set")
        }
        def result
        def oldProtocol = dockerHost.split("://", 2)[0]
        def protocol = oldProtocol
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
                result = new URL(dockerHost.replaceFirst("^${oldProtocol}://", "${protocol}://"))
                break
            case "unix":
                log.debug("is 'unix'")
                def dockerUnixSocket = dockerHost.replaceFirst("unix://", "")
                try {
                    result = new URL("unix", dockerUnixSocket, "")
                }
                catch (MalformedURLException ignored) {
                    log.info("retrying to connect to '$dockerUnixSocket'")
                    try {
                        result = new URL("unix", dockerUnixSocket, -1, "", new sun.net.www.protocol.unix.Handler())
                    }
                    catch (MalformedURLException finalException) {
                        log.error("could not use the 'unix' protocol to connect to $dockerUnixSocket - retry failed.", finalException)
                        throw finalException
                    }
                }
                break
            case "npipe":
                log.debug("is 'named pipe'")
                def dockerNamedPipe = dockerHost.replaceFirst("npipe://", "")
                try {
                    result = new URL("npipe", dockerNamedPipe, "")
                }
                catch (MalformedURLException ignored) {
                    log.info("retrying to connect to '$dockerNamedPipe'")
                    try {
                        result = new URL("npipe", dockerNamedPipe, -1, "", new sun.net.www.protocol.npipe.Handler())
                    }
                    catch (MalformedURLException finalException) {
                        log.error("could not use the 'npipe' protocol to connect to $dockerNamedPipe - retry failed.", finalException)
                        throw finalException
                    }
                }
                break
            default:
                log.warn("protocol '${protocol}' not supported")
                result = new URL(dockerHost)
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
