package de.gesellix.docker.client

import de.gesellix.docker.engine.DockerVersion
import de.gesellix.docker.remote.api.SystemInfo
import groovy.util.logging.Slf4j

import static de.gesellix.docker.engine.DockerVersion.parseDockerVersion

@Slf4j
class LocalDocker {

  static void main(String[] args) {
//    println(available() ? "connection success" : "failed to connect")
    log.debug(available() ? "connection success" : "failed to connect")
  }

  static boolean available() {
    try {
      return new DockerClientImpl().ping().content == "OK"
    }
    catch (Exception e) {
      log.info("Docker not available", e)
      return false
    }
  }

  static boolean supportsSwarmMode() {
    try {
      DockerVersion version = getDockerVersion()
      return (version.major >= 1 && version.minor >= 12) || version.major >= 17
    }
    catch (Exception e) {
      log.info("Docker not available", e)
      return false
    }
  }

  static boolean supportsSecrets() {
    try {
      DockerVersion version = getDockerVersion()
      return (version.major >= 1 && version.minor >= 13) || version.major >= 17
    }
    catch (Exception e) {
      log.info("Docker not available", e)
      return false
    }
  }

  static boolean supportsConfigs() {
    try {
      DockerVersion version = getDockerVersion()
      return version.major >= 17 && version.minor >= 6
    }
    catch (Exception e) {
      log.info("Docker not available", e)
      return false
    }
  }

  static boolean supportsStack() {
    try {
      DockerVersion version = getDockerVersion()
      return (version.major >= 1 && version.minor >= 13) || version.major >= 17
    }
    catch (Exception e) {
      log.info("Docker not available", e)
      return false
    }
  }

  static DockerVersion getDockerVersion() {
    try {
      String version = new DockerClientImpl().version().content.version
      return parseDockerVersion(version)
    }
    catch (Exception e) {
      log.info("Docker not available", e)
      throw new RuntimeException(e)
    }
  }

  static boolean isNativeWindows(DockerClient client = null) {
    try {
      DockerClient dockerClient = (client ?: new DockerClientImpl())
      def version = dockerClient.version()
      String arch = version.content.arch
      String os = version.content.os
      return "$os/$arch".toString() == "windows/amd64"
    }
    catch (Exception e) {
      log.info("Docker not available", e)
      throw new RuntimeException(e)
    }
  }

  static boolean isPausable(DockerClient client = null) {
    DockerClient dockerClient = (client ?: new DockerClientImpl())
    String daemonPlatform = getDaemonPlatform(dockerClient)
    SystemInfo.Isolation daemonIsolation = getDaemonIsolation(dockerClient)
    return daemonPlatform != "windows" || daemonIsolation != SystemInfo.Isolation.Process
  }

  static boolean isLinuxContainersOnWindows() {
    String clientOS = System.getProperty("os.name")
    return clientOS?.toLowerCase()?.contains("windows") && !isNativeWindows()
  }

  static String getDaemonPlatform(DockerClient client = null) {
    try {
      DockerClient dockerClient = (client ?: new DockerClientImpl())
      String osType = dockerClient.info().content.osType
      return osType
    }
    catch (Exception e) {
      log.info("Docker not available", e)
      throw new RuntimeException(e)
    }
  }

  static SystemInfo.Isolation getDaemonIsolation(DockerClient client = null) {
    try {
      DockerClient dockerClient = (client ?: new DockerClientImpl())
      SystemInfo.Isolation isolation = dockerClient.info().content.isolation
      return isolation
    }
    catch (Exception e) {
      log.info("Docker not available", e)
      throw new RuntimeException(e)
    }
  }

  static boolean isNamedPipe() {
    String dockerHost = new DockerClientImpl().env.dockerHost
    return dockerHost.startsWith("npipe://")
  }

  static boolean isUnixSocket() {
    String dockerHost = new DockerClientImpl().env.dockerHost
    return dockerHost.startsWith("unix://")
  }

  static boolean isTcpSocket() {
    String dockerHost = new DockerClientImpl().env.dockerHost
    return dockerHost.startsWith("tcp://") || dockerHost.startsWith("http://") || dockerHost.startsWith("https://")
  }
}
