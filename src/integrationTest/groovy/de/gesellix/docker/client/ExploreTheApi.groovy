package de.gesellix.docker.client

class ExploreTheApi {

  public static void main(String[] args) {
//    def dockerClient = new DockerClientImpl(dockerHost: System.env.DOCKER_HOST)
    System.setProperty("docker.cert.path", "/Users/gesellix/.boot2docker/certs/boot2docker-vm")
    def dockerClient = new DockerClientImpl(dockerHost: "https://192.168.59.103:2376")

//    def runResult = dockerClient.run("gesellix/docker-client-testimage", [Cmd: ["ping", "127.0.0.1"]])
//    println runResult.container.content.Id
//    dockerClient.attach(runResult.content.Id)

//    def keepDataContainers = { container ->
//      container.Names.any { String name ->
//        name.replaceAll("^/", "").matches(".*data.*")
//      }
//    }
//    dockerClient.cleanupStorage(keepDataContainers)

//    def authConfig = dockerClient.readAuthConfig(null, null)
//    println authConfig

    def runResult = dockerClient.run(
        "busybox:latest",
        [
            AttachStdin : true,
            AttachStdout: true,
            AttachStderr: true,
            Tty         : false,
            Cmd         : ["/bin/sh", "-c", "ping 127.0.0.1"]
        ])
    def containerId = runResult.container.content.Id
    println "containerId: $containerId"

    def handler = new DefaultWebsocketHandler()
    def wsClient = dockerClient.attachWebsocket(containerId, [stream: 1, stdin: 1, stdout: 1, stderr: 1], handler)

    wsClient.connectBlocking()

    println "connected"
    Thread.sleep(500)
    wsClient.send("hallo welt")
    Thread.sleep(500)

//    println "closeBlocking..."
//    wsClient.closeBlocking()

    println "close..."
    wsClient.close()

    println "closed"

    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }
}
