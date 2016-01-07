package de.gesellix.docker.client.protocolhandler

import de.gesellix.docker.client.DockerConfig
import de.gesellix.docker.client.protocolhandler.urlstreamhandler.HttpOverUnixSocketClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sun.net.www.protocol.unix.Handler

class DockerURLHandler {

    Logger logger = LoggerFactory.getLogger(DockerURLHandler)

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
                    logger.debug("assume 'https'")
                    protocol = "https"
                } else {
                    logger.debug("assume 'http'")
                    protocol = "http"
                }
                result = new URL(dockerHost.replaceFirst("^${oldProtocol}://", "${protocol}://"))
                break
            case "unix":
                logger.debug("is 'unix'")
                def dockerUnixSocket = dockerHost.replaceFirst("unix://", "")
                HttpOverUnixSocketClient.dockerUnixSocket = dockerUnixSocket
                try {
                    result = new URL("unix", "socket", dockerUnixSocket)
                }
                catch (MalformedURLException ignored) {
                    logger.warn("could not use the 'unix' protocol to connect to $dockerUnixSocket - retry.")
                    try {
                        result = new URL("unix", "socket", -1, dockerUnixSocket, new Handler())
                    }
                    catch (MalformedURLException finalException) {
                        logger.error("retry failed", finalException)
                        throw finalException
                    }
                }
                break
            default:
                logger.warn("protocol '${protocol}' not supported")
                result = new URL(dockerHost)
                break
        }
        logger.debug("selected dockerHost at '${result}'")
        return result
    }

    def shouldUseTls(URL candidateURL) {
        // explicitly disabled?
        def falsyValues = ["0", "no", "false"]
        if (falsyValues.contains(config.tlsVerify)) {
            logger.debug("dockerTlsVerify=${config.tlsVerify}")
            return false
        }

        def certsPathExists = config.certPath && new File(config.certPath).isDirectory()
        if (!certsPathExists) {
            if (config.defaultCertPath && config.defaultCertPath.isDirectory()) {
                logger.debug("defaultDockerCertPath=${config.defaultCertPath}")
                config.certPath = config.defaultCertPath
                certsPathExists = true
            }
        } else {
            logger.debug("dockerCertPath=${config.certPath}")
        }

        // explicitly enabled?
        def truthyValues = ["1", "yes", "true"]
        if (truthyValues.contains(config.tlsVerify)) {
            if (!certsPathExists) {
                throw new IllegalStateException("tlsverify=${config.tlsVerify}, but ${config.certPath} doesn't exist")
            } else {
                logger.debug("certsPathExists=${certsPathExists}")
                return true
            }
        }

        // make a guess if we could use tls, when it's neither explicitly enabled nor disabled
        def isTlsPort = candidateURL.port == config.defaultTlsPort
        logger.debug("certsPathExists=${certsPathExists}, isTlsPort=${isTlsPort}")
        return certsPathExists && isTlsPort
    }
}
