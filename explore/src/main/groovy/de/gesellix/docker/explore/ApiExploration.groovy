package de.gesellix.docker.explore

import de.gesellix.docker.client.DockerClientImpl
import de.gesellix.docker.client.stack.DeployConfigReader
import de.gesellix.docker.client.stack.DeployStackOptions

import java.nio.file.Paths

class ApiExploration {

  static void main(String[] args) {
//    def dockerClient = new DockerClientImpl(dockerHost: System.getenv("DOCKER_HOST"))
//    System.setProperty("docker.cert.path", "/Users/gesellix/.boot2docker/certs/boot2docker-vm")
//    def dockerClient = new DockerClientImpl(dockerHost: "https://192.168.59.103:2376")
//        System.setProperty("docker.cert.path", "/Users/gesellix/.docker/machine/machines/default")
//        def dockerClient = new DockerClientImpl("https://192.168.99.100:2376")
//        def dockerClient = new DockerClientImpl("unix:///var/run/docker.sock")
    def dockerClient = new DockerClientImpl()

//        println dockerClient.ping().content
//        println dockerClient.info().content
//        println dockerClient.version().content

//        try {
//            def runResult = dockerClient.run("gesellix/testimage:os-linux",
//                                             [Cmd: ["ping", "127.0.0.1"]])
//            def containerId = runResult.container.content.Id
//            println containerId
//            dockerClient.attach(containerId,
//                                [logs: 1, stream: 1, stdin: 0, stdout: 1, stderr: 1],
//                                new AttachConfig())
//            Thread.sleep(500)
//            dockerClient.stop(containerId)
//            dockerClient.wait(containerId)
//            dockerClient.rm(containerId)
//        }
//        catch (Throwable t) {
//            println("## $t")
//        }

//    def keepDataContainers = { container ->
//      container.Names.any { String name ->
//        name.replaceAll("^/", "").matches(".*data.*")
//      }
//    }
//    dockerClient.cleanupStorage(keepDataContainers)

//    def authConfig = dockerClient.readAuthConfig(null, null)
//    println authConfig

//    println new JsonBuilder(dockerClient.images().content).toPrettyString()
//        def imageId = dockerClient.pull("gesellix/testimage")
//        println imageId

//        def cmds = ["sh", "-c", "mkdir -p /foo; touch /foo/bar"]
//        def runResult = dockerClient.run("gesellix/testimage", [Cmd: cmds])
//        println runResult.container.content.Id

//        def archiveInfo = dockerClient.getArchiveStats('cp-test', '/tst.txt')
//        println archiveInfo

//        def archive = dockerClient.getArchive('cp-test', '/tst.txt')
//        println IOUtils.toString(archive.stream as InputStream)

//        def archive = dockerClient.extractFile('cp-test', '/tst.txt')
//        println new String(archive)

//        dockerClient.putArchive(container, path, file)

//        def archive = dockerClient.save("a-repo:the-tag", "74c4aa413f9a")
//        println IOUtils.copy(archive.stream as InputStream, new FileOutputStream("./foo2.tar"))

//        try {
//            dockerClient.run("alpine:edge", [Cmd: ["id"], Tty: true], "", "run-me")
//            def logs = dockerClient.logs("run-me")
//            println IOUtils.toString(logs.stream as InputStream)
//        } catch (Exception e) {
//            e.printStackTrace()
//        } finally {
//            dockerClient.stop("run-me")
//            dockerClient.wait("run-me")
//            dockerClient.rm("run-me")
//        }

//        dockerClient.initSwarm([
//                "ListenAddr"     : "0.0.0.0:2377",
//                "AdvertiseAddr"  : "192.168.1.1:2377",
//                "ForceNewCluster": false,
//                "Spec"           : [
//                        "Orchestration": [:],
//                        "Raft"         : [:],
//                        "Dispatcher"   : [:],
//                        "CAConfig"     : [:]
//                ]
//        ])

//        println dockerClient.getSwarmWorkerToken()
//        println dockerClient.rotateSwarmWorkerToken()
//        println dockerClient.getSwarmWorkerToken()
//        println dockerClient.getSwarmMangerAddress()

//        println dockerClient.systemDf().content

//        println dockerClient.createSecret("test", "a-secret".bytes).content
//        println dockerClient.inspectSecret("5qyxxlxqbq6s5004io33miih6").content
//        println dockerClient.secrets().content
//        println dockerClient.rmSecret("5qyxxlxqbq6s5004io33miih6").content

    try {
//            dockerClient.initSwarm()
//            def serviceConfig = newServiceConfig()
//            dockerClient.createService(serviceConfig)
//            println dockerClient.services()

//            println dockerClient.lsStacks()

//            println dockerClient.stackPs("example")
//            println dockerClient.stackPs("example", [label: ['APP=VOTING': true]])

//            println dockerClient.stackServices("example")
//            println dockerClient.stackServices("example", [label: ['APP=VOTING': true]])

//            println dockerClient.stackRm("example")

      def namespace = "example"
      def composeStack = ApiExploration.class.getResourceAsStream('docker-stack.yml')
      String workingDir = Paths.get(ApiExploration.class.getResource('docker-stack.yml').toURI()).parent
      def deployConfig = new DeployConfigReader(dockerClient).loadCompose(namespace, composeStack, workingDir)
      dockerClient.stackDeploy(namespace, deployConfig, new DeployStackOptions())
    }
    finally {
//            dockerClient.leaveSwarm([force: true])
    }

//        def okDockerClient = new OkDockerClient()
//        def configs = okDockerClient.get([path: "/configs"])
//        println configs

//        def alpineDetails = dockerClient.descriptor("alpine")
//        println alpineDetails

//        def createResult = dockerClient.createContainer([
//                Image    : "mongo:3",
//                OpenStdin: true,
//                Tty      : true
//        ])
//        println "created: ${createResult}"
//        def startResult = dockerClient.startContainer(createResult.content.Id)
//        def startResult = dockerClient.startContainer("3dba0b583987")
//        println "started: ${startResult}"

//        def base =new File("/Users/gesellix/dev/pku/vorgangsmanagement/ep-vm-frontend-webapp/static")
//        def filtered = new DockerignoreFileFilter(base, []).collectFiles(base)
//        println "filtered: ${filtered.size()}"
  }

  static Map newServiceConfig() {
    [
        "Name"        : "redis",
        "TaskTemplate": [
            "ContainerSpec": [
                "Image": "redis"
            ],
            "Resources"    : [
                "Limits"      : [:],
                "Reservations": [:]
            ],
            "RestartPolicy": [:],
            "Placement"    : [:]
        ],
        "Mode"        : [
            "Replicated": [
                "Instances": 1
            ]
        ],
        "UpdateConfig": [
            "Parallelism": 1
        ],
        "EndpointSpec": [
            "ExposedPorts": [
                ["Protocol": "tcp", "Port": 6379]
            ]
        ]
    ]
  }
}
