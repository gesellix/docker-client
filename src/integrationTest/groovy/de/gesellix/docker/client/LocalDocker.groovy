package de.gesellix.docker.client

class LocalDocker {

    static def available() {
        try {
            return new DockerClientImpl().ping().status.code == 200
        }
        catch (Exception ignored) {
            return false
        }
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
