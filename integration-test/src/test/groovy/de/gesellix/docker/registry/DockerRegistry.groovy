package de.gesellix.docker.registry

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.LocalDocker
import de.gesellix.docker.remote.api.ContainerCreateRequest
import de.gesellix.docker.remote.api.HostConfig

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
    LocalDocker.isNativeWindows() ? "2.7.1-windows" : "2.8"
  }

  void run() {
    def containerConfig = new ContainerCreateRequest().tap { c ->
      c.image = "${getImageName()}:${getImageTag()}"
      c.env = ["REGISTRY_VALIDATION_DISABLED=true"]
      c.exposedPorts = ["5000/tcp": [:]]
      c.hostConfig = new HostConfig().tap { h -> h.publishAllPorts = true }
    }
    def registryStatus = dockerClient.run(containerConfig)
    registryId = registryStatus.content.id
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
    def portBinding = registryContainer.networkSettings.ports["5000/tcp"]
    return portBinding.first().hostPort as Integer
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
