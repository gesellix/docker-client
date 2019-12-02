package de.gesellix.docker.client

import de.gesellix.docker.engine.DockerVersion
import groovy.util.logging.Slf4j

import static de.gesellix.docker.engine.DockerVersion.parseDockerVersion

@Slf4j
class LocalDocker {

    static void main(String[] args) {
        log.debug available() ? "connection success" : "failed to connect"
    }

    static available() {
        try {
            return new DockerClientImpl().ping().status.code == 200
        }
        catch (Exception e) {
            log.info("Docker not available", e)
            return false
        }
    }

    static supportsSwarmMode() {
        try {
            def version = getDockerVersion()
            return (version.major >= 1 && version.minor >= 12) || version.major >= 17
        }
        catch (Exception e) {
            log.info("Docker not available", e)
            return false
        }
    }

    static supportsSecrets() {
        try {
            def version = getDockerVersion()
            return (version.major >= 1 && version.minor >= 13) || version.major >= 17
        }
        catch (Exception e) {
            log.info("Docker not available", e)
            return false
        }
    }

    static supportsConfigs() {
        try {
            def version = getDockerVersion()
            return version.major >= 17 && version.minor >= 6
        }
        catch (Exception e) {
            log.info("Docker not available", e)
            return false
        }
    }

    static supportsStack() {
        try {
            def version = getDockerVersion()
            return (version.major >= 1 && version.minor >= 13) || version.major >= 17
        }
        catch (Exception e) {
            log.info("Docker not available", e)
            return false
        }
    }

    static DockerVersion getDockerVersion() {
        try {
            def version = new DockerClientImpl().version().content.Version as String
            return parseDockerVersion(version)
        }
        catch (Exception e) {
            log.info("Docker not available", e)
            throw new RuntimeException(e)
        }
    }

    static boolean isNativeWindows(DockerClient client = null) {
        try {
            def dockerClient = (client ?: new DockerClientImpl())
            def arch = dockerClient.version().content.Arch as String
            def os = dockerClient.version().content.Os as String
            return "$os/$arch".toString() == "windows/amd64"
        }
        catch (Exception e) {
            log.info("Docker not available", e)
            throw new RuntimeException(e)
        }
    }

    static boolean isPausable(DockerClient client = null) {
        def dockerClient = (client ?: new DockerClientImpl())
        def daemonPlatform = getDaemonPlatform(dockerClient)
        def daemonIsolation = getDaemonIsolation(dockerClient)
        return daemonPlatform != "windows" || daemonIsolation != "process"
    }

    static boolean isLinuxContainersOnWindows() {
        String clientOS = System.getProperty("os.name")
        return clientOS?.toLowerCase()?.contains("windows") && !isNativeWindows()
    }

    static String getDaemonPlatform(DockerClient client = null) {
        try {
            def dockerClient = (client ?: new DockerClientImpl())
            def osType = dockerClient.info().content.OSType as String
            return osType
        }
        catch (Exception e) {
            log.info("Docker not available", e)
            throw new RuntimeException(e)
        }
    }

    static String getDaemonIsolation(DockerClient client = null) {
        try {
            def dockerClient = (client ?: new DockerClientImpl())
            def isolation = dockerClient.info().content.Isolation as String
            return isolation
        }
        catch (Exception e) {
            log.info("Docker not available", e)
            throw new RuntimeException(e)
        }
    }

    static isNamedPipe() {
        def dockerHost = new DockerClientImpl().env.dockerHost
        return dockerHost.startsWith("npipe://")
    }

    static isUnixSocket() {
        def dockerHost = new DockerClientImpl().env.dockerHost
        return dockerHost.startsWith("unix://")
    }

    static isTcpSocket() {
        def dockerHost = new DockerClientImpl().env.dockerHost
        return dockerHost.startsWith("tcp://") || dockerHost.startsWith("http://") || dockerHost.startsWith("https://")
    }
}
