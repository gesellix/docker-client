package de.gesellix.docker.client

class ExploreTheApi {

  public static void main(String[] args) {
    def dockerClient = new DockerClientImpl(dockerHost: System.env.DOCKER_HOST)
//    def runResult = dockerClient.run("gesellix/docker-client-testimage", [Cmd: ["ping", "127.0.0.1"]])
//    println runResult.container.content.Id
//    dockerClient.attach(runResult.content.Id)
    def keepDataContainers = { container ->
      container.Names.any { String name ->
        name.replaceAll("^/", "").matches(".*data.*")
      }
    }
    dockerClient.cleanupStorage(keepDataContainers)

    def authConfig = dockerClient.readAuthConfig(null, null)
    println authConfig
  }
}
