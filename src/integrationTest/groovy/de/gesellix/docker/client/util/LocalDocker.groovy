package de.gesellix.docker.client.util

import de.gesellix.docker.client.DockerClientImpl
import de.gesellix.docker.client.util.DockerVersion
import groovy.util.logging.Slf4j

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

    static def isNamedPipe() {
        def dockerHost = new DockerClientImpl().config.dockerHost
        return dockerHost.startsWith("npipe://")
    }

    static def isUnixSocket() {
        def dockerHost = new DockerClientImpl().config.dockerHost
        return dockerHost.startsWith("unix://")
    }

    static def isTcpSocket() {
        def dockerHost = new DockerClientImpl().config.dockerHost
        return dockerHost.startsWith("tcp://") || dockerHost.startsWith("http://") || dockerHost.startsWith("https://")
    }

    static DockerVersion parseDockerVersion(String version) {
        final def versionPattern = /(\d+)\.(\d+)\.(\d+)(.*)/

        def parsedVersion = new DockerVersion()
        version.eachMatch(versionPattern) { List<String> groups ->
            parsedVersion.major = Integer.parseInt(groups[1])
            parsedVersion.minor = Integer.parseInt(groups[2])
            parsedVersion.patch = Integer.parseInt(groups[3])
            parsedVersion.meta = groups[4]
        }

        return parsedVersion
    }
}
