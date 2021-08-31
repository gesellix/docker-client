package de.gesellix.docker.registry

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.LocalDocker

class DockerRegistry {

  DockerClient dockerClient
  String registryId

  DockerRegistry(DockerClient dockerClient) {
    this.dockerClient = dockerClient
  }

  String getImageName() {
    LocalDocker.isNativeWindows() ? "gesellix/registry" : "registry"
  }

  String getImageTag() {
    LocalDocker.isNativeWindows() ? "2.7.1-windows" : "2.7.1"
  }

  void run() {
    def registryStatus = dockerClient.run(
        getImageName(),
        ["Env"         : ["REGISTRY_VALIDATION_DISABLED=true"],
         "ExposedPorts": ["5000/tcp": [:]],
         "HostConfig"  : ["PublishAllPorts": true]],
        getImageTag())
    registryId = registryStatus.container.content.Id
  }

  String address() {
//        String dockerHost = dockerClient.config.dockerHost
//        return dockerHost.replaceAll("^(tcp|http|https)://", "").replaceAll(":\\d+\$", "")

//        def registryContainer = dockerClient.inspectContainer(registryId).content
//        def portBinding = registryContainer.NetworkSettings.Ports["5000/tcp"]
//        return portBinding[0].HostIp as String

    // 'localhost' allows to use the registry without TLS
    return "localhost"
  }

  int port() {
    def registryContainer = dockerClient.inspectContainer(registryId).content
    def portBinding = registryContainer.NetworkSettings.Ports["5000/tcp"]
    return portBinding[0].HostPort as Integer
  }

  String url() {
    return "${address()}:${port()}"
  }

  void rm() {
    dockerClient.stop(registryId)
    dockerClient.wait(registryId)
    dockerClient.rm(registryId)
  }
}
