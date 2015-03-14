package de.gesellix.docker.client.protocolhandler

import de.gesellix.docker.client.protocolhandler.urlstreamhandler.HttpOverUnixSocketClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static java.lang.Boolean.FALSE

class DockerURLHandler {

  Logger logger = LoggerFactory.getLogger(DockerURLHandler)

  String dockerHost
  String dockerTlsVerify
  String dockerCertPath
  final int defaultDockerTlsPort = 2376

  URL dockerUrl

  DockerURLHandler() {
    this.dockerHost = System.getProperty("docker.host", System.env.DOCKER_HOST)
    this.dockerTlsVerify = System.getProperty("docker.tlsverify", System.env.DOCKER_TLS_VERIFY)
    this.dockerCertPath = System.getProperty("docker.cert.path", System.env.DOCKER_CERT_PATH)
  }

  def getURL() {
    if (!dockerUrl) {
      if (!dockerHost) {
        throw new IllegalStateException("dockerHost must be set")
      }
      dockerUrl = getURLWithActualProtocol(dockerHost)
    }
    return dockerUrl
  }

  def getURLWithActualProtocol(String dockerHost) {
    def result
    def oldProtocol = dockerHost.split("://", 2)[0]
    def protocol = oldProtocol
    switch (protocol) {
      case "http":
      case "https":
        result = new URL(dockerHost)
        break
      case "tcp":
        if (shouldUseTls(new URL(dockerHost.replaceFirst("^${oldProtocol}://", "https://")))) {
          logger.info("assume 'https'")
          protocol = "https"
        }
        else {
          logger.info("assume 'http'")
          protocol = "http"
        }
        result = new URL(dockerHost.replaceFirst("^${oldProtocol}://", "${protocol}://"))
        break
      case "unix":
        logger.info("is 'unix'")
        def dockerUnixSocket = dockerHost.replaceFirst("unix://", "")
        HttpOverUnixSocketClient.dockerUnixSocket = dockerUnixSocket
        result = new URL("unix", "socket", dockerUnixSocket)
        break
      default:
        logger.warn("protocol '${protocol}' not supported")
        result = new URL(dockerHost)
        break
    }
    logger.debug("selected dockerHost at '${result}'")
    return result
  }

  def shouldUseTls(candidateURL) {
    // explicitly disabled?
    if ((dockerTlsVerify && Boolean.valueOf(dockerTlsVerify) == FALSE) || "0".equals(dockerTlsVerify)) {
      logger.debug("dockerTlsVerify=${dockerTlsVerify}")
      return false
    }

    def certsPathExists = dockerCertPath && new File(dockerCertPath).isDirectory()
    if (!certsPathExists) {
      String userHome = System.properties['user.home']
      def defaultDockerCertPath = new File(userHome, ".docker")
      if (defaultDockerCertPath.isDirectory()) {
        logger.debug("dockerCertPath=${defaultDockerCertPath}")
        certsPathExists = true
      }
    }
    else {
      logger.debug("dockerCertPath=${dockerCertPath}")
    }

    // explicitly enabled?
    def isTlsVerifyEnabled = "1".equals(dockerTlsVerify) || Boolean.valueOf(dockerTlsVerify)
    if (isTlsVerifyEnabled) {
      if (!certsPathExists) {
        throw new IllegalStateException("tlsverify=${dockerTlsVerify}, but ${dockerCertPath} doesn't exist")
      }
      else {
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
