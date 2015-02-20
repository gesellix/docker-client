package de.gesellix.docker.client.protocolhandler

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DockerURLHandler {

  Logger logger = LoggerFactory.getLogger(DockerURLHandler)

  String dockerHost
  String dockerCertPath
  int defaultDockerTlsPort = 2376

  URL dockerUrl

  DockerURLHandler() {
    this.dockerHost = System.getProperty("docker.host", System.env.DOCKER_HOST)
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
        if (canTlsBeAssumed(new URL(dockerHost.replaceFirst("^${oldProtocol}://", "https://")))) {
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

  def canTlsBeAssumed(candidateURL) {
    def isTlsPort = candidateURL.port == defaultDockerTlsPort
    def certsPathExists = dockerCertPath && new File(dockerCertPath).isDirectory()
    return isTlsPort && certsPathExists
  }
}
