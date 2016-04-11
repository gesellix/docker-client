package de.gesellix.docker.client

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
}
