package de.gesellix.docker.client.protocolhandler

import de.gesellix.docker.client.DockerConfig
import de.gesellix.docker.client.protocolhandler.urlstreamhandler.HttpOverUnixSocketClient
import groovy.util.logging.Slf4j
import sun.net.www.protocol.unix.Handler

@Slf4j
class DockerURLHandler {

    DockerConfig config = new DockerConfig()

    def getRequestUrl(String dockerHost, String path, String query) {
        if (!dockerHost) {
            throw new IllegalStateException("dockerHost must be set")
        }
        def dockerBaseUrl = getURLWithActualProtocol(dockerHost)
        if (dockerBaseUrl.protocol == "unix") {
            return new URL(dockerBaseUrl.protocol, dockerBaseUrl.host, -1, "${path}${query}", new Handler())
        }
        return new URL("${dockerBaseUrl}${path}${query}")
    }

    URL getURLWithActualProtocol(String dockerHost) {
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
                HttpOverUnixSocketClient.dockerUnixSocket = dockerUnixSocket
                try {
                    result = new URL("unix", "socket", dockerUnixSocket)
                }
                catch (MalformedURLException ignored) {
                    log.info("retrying to connect to '$dockerUnixSocket'")
                    try {
                        result = new URL("unix", "socket", -1, dockerUnixSocket, new Handler())
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
                // slashes need to be escaped, because the pipe name is used as host name
                dockerNamedPipe = URLEncoder.encode(dockerNamedPipe, "UTF-8")
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
        // explicitly disabled?
        def falsyValues = ["0", "no", "false"]
        if (falsyValues.contains(config.tlsVerify)) {
            log.debug("dockerTlsVerify=${config.tlsVerify}")
            return false
        }

        def certsPathExists = config.certPath && new File(config.certPath, "").isDirectory()
        if (!certsPathExists) {
            if (config.defaultCertPath && config.defaultCertPath.isDirectory()) {
                log.debug("defaultDockerCertPath=${config.defaultCertPath}")
                config.certPath = config.defaultCertPath
                certsPathExists = true
            }
        } else {
            log.debug("dockerCertPath=${config.certPath}")
        }

        // explicitly enabled?
        def truthyValues = ["1", "yes", "true"]
        if (truthyValues.contains(config.tlsVerify)) {
            if (!certsPathExists) {
                throw new IllegalStateException("tlsverify=${config.tlsVerify}, but ${config.certPath} doesn't exist")
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
