package de.gesellix.docker.client

class ExploreTheApi {

    public static void main(String[] args) {
//    def dockerClient = new DockerClientImpl(dockerHost: System.env.DOCKER_HOST)
//    System.setProperty("docker.cert.path", "/Users/gesellix/.boot2docker/certs/boot2docker-vm")
//    def dockerClient = new DockerClientImpl(dockerHost: "https://192.168.59.103:2376")
        System.setProperty("docker.cert.path", "/Users/gesellix/.docker/machine/machines/default")
        def dockerClient = new DockerClientImpl(dockerHost: "https://192.168.99.100:2376")

        println dockerClient.info().content
        println dockerClient.version().content

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

//    println new JsonBuilder(dockerClient.images().content).toPrettyString()
//        def imageId = dockerClient.pull("gesellix/docker-client-testimage")
//        println imageId

        def cmds = ["sh", "-c", "mkdir -p /foo; touch /foo/bar"]
        def runResult = dockerClient.run("gesellix/docker-client-testimage", [Cmd: cmds])
        println runResult.container.content.Id

        def archiveInfo = dockerClient.getArchiveStats(runResult.container.content.Id, '/foo/')
        println archiveInfo
//        dockerClient.downloadArchive(container, path)
//        dockerClient.uploadArchive(container, path, file)
    }
}
