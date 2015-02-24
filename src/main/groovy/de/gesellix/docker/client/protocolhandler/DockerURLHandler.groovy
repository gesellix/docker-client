package de.gesellix.docker.client.protocolhandler

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
    def oldProtocol = dockerHost.split("://", 2)[0]
    def protocol = oldProtocol
    switch (protocol) {
      case "http":
      case "https":
        break;
      case "tcp":
        if (shouldUseTls(new URL(dockerHost.replaceFirst("^${oldProtocol}://", "https://")))) {
          logger.info("assume 'https'")
          protocol = "https"
        }
        else {
          logger.info("assume 'http'")
          protocol = "http"
        }
        break;
      case "unix":
        logger.info("is 'unix'")
        break;
      default:
        logger.warn("protocol '${protocol}' not supported")
        break;
    }
    logger.info("selected protocol '${protocol}'")
    return new URL(dockerHost.replaceFirst("^${oldProtocol}://", "${protocol}://"))
  }

  def shouldUseTls(candidateURL) {
    // explicitly disabled?
    if ((dockerTlsVerify && Boolean.valueOf(dockerTlsVerify) == FALSE) || "0".equals(dockerTlsVerify)) {
      return false
    }

    def certsPathExists = dockerCertPath && new File(dockerCertPath).isDirectory()
    if (!certsPathExists) {
      String userHome = System.properties['user.home']
      if (new File(userHome, ".docker").isDirectory()) {
        certsPathExists = true
      }
    }

    // explicitly enabled?
    def isTlsVerifyEnabled = "1".equals(dockerTlsVerify) || Boolean.valueOf(dockerTlsVerify)
    if (isTlsVerifyEnabled) {
      if (!certsPathExists) {
        throw new IllegalStateException("tlsverify=${dockerTlsVerify}, but ${dockerCertPath} doesn't exist")
      }
      else {
        return true
      }
    }

    // make a guess if we could use tls, when it's neither explicitly enabled nor disabled
    def isTlsPort = candidateURL.port == defaultDockerTlsPort
    return certsPathExists && isTlsPort
  }
}
