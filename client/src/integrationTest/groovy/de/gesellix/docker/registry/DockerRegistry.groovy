package de.gesellix.docker.registry

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.LocalDocker

class DockerRegistry {

    DockerClient dockerClient
    def registryId

    DockerRegistry(DockerClient dockerClient) {
        this.dockerClient = dockerClient
    }

    def getImageName(){
        LocalDocker.isNativeWindows() ? "gesellix/registry" : "registry"
    }

    def getImageTag(){
        LocalDocker.isNativeWindows() ? "2.5.1-windows" : "2"
    }

    def run() {
        def registryStatus = dockerClient.run(
                getImageName(),
                ["ExposedPorts": ["5000/tcp": [:]],
                 "HostConfig"  : ["PublishAllPorts": true]],
                getImageTag())
        registryId = registryStatus.container.content.Id
    }

    def address() {
//        String dockerHost = dockerClient.config.dockerHost
//        return dockerHost.replaceAll("^(tcp|http|https)://", "").replaceAll(":\\d+\$", "")

//        def registryContainer = dockerClient.inspectContainer(registryId).content
//        def portBinding = registryContainer.NetworkSettings.Ports["5000/tcp"]
//        return portBinding[0].HostIp as String

        // 'localhost' allows to use the registry without TLS
        return "localhost"
    }

    def port() {
        def registryContainer = dockerClient.inspectContainer(registryId).content
        def portBinding = registryContainer.NetworkSettings.Ports["5000/tcp"]
        return portBinding[0].HostPort as Integer
    }

    def url() {
        return "${address()}:${port()}"
    }

    def rm() {
        dockerClient.stop(registryId)
        dockerClient.wait(registryId)
        dockerClient.rm(registryId)
    }
}
