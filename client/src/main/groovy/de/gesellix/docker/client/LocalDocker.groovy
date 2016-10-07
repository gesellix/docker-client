package de.gesellix.docker.client

import de.gesellix.docker.client.config.DockerVersion
import groovy.util.logging.Slf4j

import static de.gesellix.docker.client.config.DockerVersion.parseDockerVersion

@Slf4j
class LocalDocker {

    public static void main(String[] args) {
        log.debug available() ? "connection success" : "failed to connect"
    }

    static def available() {
        try {
            return new DockerClientImpl().ping().status.code == 200
        }
        catch (Exception e) {
            log.info("Docker not available", e)
            return false
        }
    }

    static def hasSwarmMode() {
        try {
            def version = getDockerVersion()
            return version.major >= 1 && version.minor >= 12
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

    static boolean isNativeWindows() {
        try {
            def arch = new DockerClientImpl().version().content.Arch as String
            def os = new DockerClientImpl().version().content.Os as String
            return "$os/$arch".toString() == "windows/amd64"
        }
        catch (Exception e) {
            log.info("Docker not available", e)
            throw new RuntimeException(e)
        }
    }

    static def isNamedPipe() {
        def dockerHost = new DockerClientImpl().env.dockerHost
        return dockerHost.startsWith("npipe://")
    }

    static def isUnixSocket() {
        def dockerHost = new DockerClientImpl().env.dockerHost
        return dockerHost.startsWith("unix://")
    }

    static def isTcpSocket() {
        def dockerHost = new DockerClientImpl().env.dockerHost
        return dockerHost.startsWith("tcp://") || dockerHost.startsWith("http://") || dockerHost.startsWith("https://")
    }
}
