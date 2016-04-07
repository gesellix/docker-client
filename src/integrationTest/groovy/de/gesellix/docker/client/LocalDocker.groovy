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
}
