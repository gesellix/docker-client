package de.gesellix.docker.client.protocolhandler

import de.gesellix.docker.client.protocolhandler.urlstreamhandler.HttpOverUnixSocketClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sun.net.www.protocol.unix.Handler

class DockerURLHandler {

    Logger logger = LoggerFactory.getLogger(DockerURLHandler)

    String dockerTlsVerify
    String dockerCertPath
    File defaultDockerCertPath
    final int defaultDockerTlsPort = 2376

    DockerURLHandler() {
        this.dockerTlsVerify = System.getProperty("docker.tls.verify", System.env.DOCKER_TLS_VERIFY as String)
        this.dockerCertPath = System.getProperty("docker.cert.path", System.env.DOCKER_CERT_PATH as String)
        this.defaultDockerCertPath = new File(System.properties['user.home'] as String, ".docker")
    }

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
        if (falsyValues.contains(dockerTlsVerify)) {
            logger.debug("dockerTlsVerify=${dockerTlsVerify}")
            return false
        }

        def certsPathExists = dockerCertPath && new File(dockerCertPath).isDirectory()
        if (!certsPathExists) {
            if (defaultDockerCertPath && defaultDockerCertPath.isDirectory()) {
                logger.debug("defaultDockerCertPath=${defaultDockerCertPath}")
                dockerCertPath = defaultDockerCertPath
                certsPathExists = true
            }
        } else {
            logger.debug("dockerCertPath=${dockerCertPath}")
        }

        // explicitly enabled?
        def truthyValues = ["1", "yes", "true"]
        if (truthyValues.contains(dockerTlsVerify)) {
            if (!certsPathExists) {
                throw new IllegalStateException("tlsverify=${dockerTlsVerify}, but ${dockerCertPath} doesn't exist")
            } else {
                logger.debug("certsPathExists=${certsPathExists}")
                return true
            }
        }

        // make a guess if we could use tls, when it's neither explicitly enabled nor disabled
        def isTlsPort = candidateURL.port == defaultDockerTlsPort
        logger.debug("certsPathExists=${certsPathExists}, isTlsPort=${isTlsPort}")
        return certsPathExists && isTlsPort
    }
}
