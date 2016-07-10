package de.gesellix.docker.client

import de.gesellix.docker.client.config.DockerEnv
import de.gesellix.docker.testutil.MemoryAppender
import de.gesellix.docker.testutil.ResourceReader
import groovy.json.JsonBuilder
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.CountDownLatch

import static ch.qos.logback.classic.Level.ERROR

class DockerClientImplSpec extends Specification {

    def DockerClientImpl dockerClient = Spy(DockerClientImpl)
    def HttpClient httpClient = Mock(HttpClient)

    def setup() {
        dockerClient.responseHandler = Spy(DockerResponseHandler)
        dockerClient.newDockerHttpClient = { DockerEnv dockerEnv, proxy -> httpClient }
    }

    def "passes dockerConfig and proxy to internal http client"() {
        given:
        def proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(4711))
        def env = Mock(DockerEnv)
        env.dockerHost >> "tcp://127.0.0.1:2375"

        when:
        def httpClient = dockerClient.createDockerHttpClient(env, proxy)

        then:
        httpClient.dockerClientConfig.env == env
        httpClient.proxy == proxy
    }

    def "read and encode authConfig (old format)"() {
        given:
        def dockerCfg = new ResourceReader().getClasspathResourceAsFile('/auth/dockercfg', DockerClient)
        def authDetails = dockerClient.readAuthConfig(null, dockerCfg)
        def authPlain = authDetails

        when:
        def authResult = dockerClient.encodeAuthConfig(authPlain)

        then:
        authResult == 'eyJ1c2VybmFtZSI6Imdlc2VsbGl4IiwicGFzc3dvcmQiOiIteWV0LWFub3RoZXItcGFzc3dvcmQtIiwiZW1haWwiOiJ0b2JpYXNAZ2VzZWxsaXguZGUiLCJzZXJ2ZXJhZGRyZXNzIjoiaHR0cHM6Ly9pbmRleC5kb2NrZXIuaW8vdjEvIn0='
    }

    def "read and encode authConfig (new format)"() {
        given:
        def dockerCfg = new ResourceReader().getClasspathResourceAsFile('/auth/config.json', DockerClient)
        def authDetails = dockerClient.readAuthConfig(null, dockerCfg)
        def authPlain = authDetails

        when:
        def authResult = dockerClient.encodeAuthConfig(authPlain)

        then:
        authResult == 'eyJ1c2VybmFtZSI6Imdlc2VsbGl4IiwicGFzc3dvcmQiOiIteWV0LWFub3RoZXItcGFzc3dvcmQtIiwiZW1haWwiOiJ0b2JpYXNAZ2VzZWxsaXguZGUiLCJzZXJ2ZXJhZGRyZXNzIjoiaHR0cHM6Ly9pbmRleC5kb2NrZXIuaW8vdjEvIn0='
    }

    @Unroll
    def "parse repository tag #name to repo '#repo' and tag '#tag'"() {
        when:
        def result = dockerClient.parseRepositoryTag(name)

        then:
        result.repo == repo
        and:
        result.tag == tag

        where:
        name                      || repo | tag
        "scratch"                 || "scratch" | ""
        "root:tag"                || "root" | "tag"
        "user/repo"               || "user/repo" | ""
        "user/repo:tag"           || "user/repo" | "tag"
        "url:5000/repo"           || "url:5000/repo" | ""
        "url:5000/repo:tag"       || "url:5000/repo" | "tag"
        "url:5000/user/image:tag" || "url:5000/user/image" | "tag"
    }

    def "shouldn't allow repository tag ending with a ':'"() {
        when:
        dockerClient.parseRepositoryTag("scratch:")

        then:
        def exc = thrown(DockerClientException)
        exc.cause.message == "'scratch:' should not end with a ':'"
    }

    def "ping"() {
        when:
        dockerClient.ping()

        then:
        1 * httpClient.get([path: "/_ping", timeout: 2000])
    }

    def "info"() {
        when:
        dockerClient.info()

        then:
        1 * httpClient.get([path: "/info"])
    }

    def "version"() {
        when:
        dockerClient.version()

        then:
        1 * httpClient.get([path: "/version"])
    }

    def "login"() {
        def authDetails = [:]
        when:
        dockerClient.auth(authDetails)

        then:
        1 * httpClient.post([path              : "/auth",
                             body              : authDetails,
                             requestContentType: "application/json"])
    }

    def "read configured docker config.json"() {
        given:
        def expectedConfigDir = new File('.').absoluteFile
        def oldDockerConfigDir = System.setProperty("docker.config", expectedConfigDir.absolutePath)

        when:
        def dockerConfigFile = dockerClient.env.getDockerConfigFile()

        then:
        dockerConfigFile.absolutePath == new File(expectedConfigDir, 'config.json').absolutePath

        cleanup:
        if (oldDockerConfigDir) {
            System.setProperty("docker.config", oldDockerConfigDir)
        } else {
            System.clearProperty("docker.config")
        }
    }

    def "read default docker config file"() {
        given:
        def oldDockerConfig = System.clearProperty("docker.config")
        def expectedConfigFile = new ResourceReader().getClasspathResourceAsFile('/auth/config.json', DockerClient)
        dockerClient.env.configFile = expectedConfigFile

        when:
        dockerClient.readDefaultAuthConfig()

        then:
        1 * dockerClient.readAuthConfig(null, expectedConfigFile)
        dockerClient.env.legacyConfigFile

        cleanup:
        if (oldDockerConfig) {
            System.setProperty("docker.config", oldDockerConfig)
        }
    }

    def "read legacy docker config file"() {
        given:
        def oldDockerConfig = System.clearProperty("docker.config")
        def nonExistingFile = new File('./I should not exist')
        assert !nonExistingFile.exists()
        dockerClient.env.configFile = nonExistingFile
        def expectedConfigFile = new ResourceReader().getClasspathResourceAsFile('/auth/dockercfg', DockerClient)
        dockerClient.env.legacyConfigFile = expectedConfigFile

        when:
        dockerClient.readDefaultAuthConfig()

        then:
        1 * dockerClient.readAuthConfig(null, expectedConfigFile)
        dockerClient.env.configFile
        dockerClient.env.legacyConfigFile

        cleanup:
        if (oldDockerConfig) {
            System.setProperty("docker.config", oldDockerConfig)
        }
    }

    def "read auth config for official Docker index"() {
        given:
        def dockerCfg = new ResourceReader().getClasspathResourceAsFile('/auth/config.json', DockerClient)

        when:
        def authDetails = dockerClient.readAuthConfig(null, dockerCfg)

        then:
        authDetails.username == "gesellix"
        and:
        authDetails.password == "-yet-another-password-"
        and:
        authDetails.email == "tobias@gesellix.de"
        and:
        authDetails.serveraddress == "https://index.docker.io/v1/"
    }

    def "read auth config for quay.io"() {
        given:
        def dockerCfg = new ResourceReader().getClasspathResourceAsFile('/auth/config.json', DockerClient)

        when:
        def authDetails = dockerClient.readAuthConfig("quay.io", dockerCfg)

        then:
        authDetails.username == "gesellix"
        and:
        authDetails.password == "-a-password-for-quay-"
        and:
        authDetails.email == "tobias@gesellix.de"
        and:
        authDetails.serveraddress == "quay.io"
    }

    def "read auth config for unknown registry hostname"() {
        given:
        def dockerCfg = new ResourceReader().getClasspathResourceAsFile('/auth/config.json', DockerClient)

        when:
        def authDetails = dockerClient.readAuthConfig("unkown.example.com", dockerCfg)

        then:
        authDetails == [:]
    }

    def "build with defaults"() {
        def buildContext = new ByteArrayInputStream([42] as byte[])

        when:
        dockerClient.build(buildContext)

        then:
        1 * httpClient.post([path              : "/build",
                             query             : ["rm": true],
                             body              : buildContext,
                             requestContentType: "application/octet-stream"]) >> [content: [[stream: ""]]]
        and:
        dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker build failed"
        }
    }

    def "build with query"() {
        def buildContext = new ByteArrayInputStream([42] as byte[])
        def query = ["rm": false]

        when:
        dockerClient.build(buildContext, query)

        then:
        1 * httpClient.post([path              : "/build",
                             query             : ["rm": false],
                             body              : buildContext,
                             requestContentType: "application/octet-stream"]) >> [content: [[stream: ""]]]
        and:
        dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker build failed"
        }
    }

    def "tag with defaults"() {
        when:
        dockerClient.tag("an-image", "registry:port/username/image-name:a-tag")

        then:
        1 * httpClient.post([path : "/images/an-image/tag",
                             query: [repo: "registry:port/username/image-name",
                                     tag : "a-tag"]])
    }

    def "push with defaults"() {
        when:
        dockerClient.push("an-image")

        then:
        1 * httpClient.post([path   : "/images/an-image/push",
                             query  : [tag: ""],
                             headers: ["X-Registry-Auth": "."]]) >> [status: [success: true]]
    }

    def "push with auth"() {
        when:
        dockerClient.push("an-image:a-tag", "some-base64-encoded-auth")

        then:
        1 * httpClient.post([path   : "/images/an-image/push",
                             query  : [tag: "a-tag"],
                             headers: ["X-Registry-Auth": "some-base64-encoded-auth"]]) >> [status: [success: true]]
    }

    def "push with registry"() {
        when:
        dockerClient.push("an-image", ".", "registry:port")

        then:
        1 * httpClient.post([path : "/images/an-image/tag",
                             query: [repo: "registry:port/an-image",
                                     tag : ""]])
        then:
        1 * httpClient.post([path   : "/images/registry:port/an-image/push",
                             query  : [tag: ""],
                             headers: ["X-Registry-Auth": "."]]) >> [status: [success: true]]
    }

    def "pull with defaults"() {
        given:
        dockerClient.images([:]) >> [content: [:]]

        when:
        dockerClient.pull("an-image")

        then:
        1 * httpClient.post([path   : "/images/create",
                             query  : [fromImage: "an-image",
                                       tag      : "",
                                       registry : ""],
                             headers: ["X-Registry-Auth": "."]]) >> [content: [[id: "image-id"]]]
        and:
        dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker pull failed"
        }
    }

    def "pull with tag"() {
        given:
        dockerClient.images([:]) >> [content: [:]]

        when:
        dockerClient.pull("an-image", "a-tag")

        then:
        1 * httpClient.post([path   : "/images/create",
                             query  : [fromImage: "an-image",
                                       tag      : "a-tag",
                                       registry : ""],
                             headers: ["X-Registry-Auth": "."]]) >> [content: [[id: "image-id"]]]
        and:
        dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker pull failed"
        }
    }

    def "pull with registry"() {
        given:
        dockerClient.images([:]) >> [content: [:]]

        when:
        dockerClient.pull("an-image", "", ".", "registry:port")

        then:
        1 * httpClient.post([path   : "/images/create",
                             query  : [fromImage: "registry:port/an-image",
                                       tag      : "",
                                       registry : "registry:port"],
                             headers: ["X-Registry-Auth": "."]]) >> [content: [[id: "image-id"]]]
        and:
        dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker pull failed"
        }
    }

    def "pull with auth"() {
        given:
        dockerClient.images([:]) >> [content: [:]]

        when:
        dockerClient.pull("an-image", "", "some-base64-encoded-auth", "registry:port")

        then:
        1 * httpClient.post([path   : "/images/create",
                             query  : [fromImage: "registry:port/an-image",
                                       tag      : "",
                                       registry : "registry:port"],
                             headers: ["X-Registry-Auth": "some-base64-encoded-auth"]]) >> [content: [[id: "image-id"]]]
        and:
        dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker pull failed"
        }
    }

    def "import from url"() {
        given:
        def importUrl = getClass().getResource('importUrl/import-from-url.tar')

        when:
        def imageId = dockerClient.importUrl(importUrl.toString(), "imported-from-url", "foo")

        then:
        1 * httpClient.post([path : "/images/create",
                             query: [fromSrc: importUrl.toString(),
                                     repo   : "imported-from-url",
                                     tag    : "foo"]]) >> [content: [[status: "image-id"]]]
        and:
        dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker import from url failed"
        }
        and:
        imageId == "image-id"
    }

    def "import from stream"() {
        given:
        def archive = getClass().getResourceAsStream('importUrl/import-from-url.tar')

        when:
        def imageId = dockerClient.importStream(archive, "imported-from-url", "foo")

        then:
        1 * httpClient.post([path              : "/images/create",
                             body              : archive,
                             requestContentType: "application/x-tar",
                             query             : [fromSrc: '-',
                                                  repo   : "imported-from-url",
                                                  tag    : "foo"]]) >> [content: [status: "image-id"]]
        and:
        dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker import from stream failed"
        }
        and:
        imageId == "image-id"
    }

    def "save one repository"() {
        given:
        def tarStream = new ByteArrayInputStream("tar".bytes)

        when:
        def response = dockerClient.save("image:tag")

        then:
        1 * httpClient.get([path: "/images/image:tag/get"]) >> [status: [success: true],
                                                                stream: tarStream]
        and:
        dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker save failed"
        }
        and:
        response.stream == tarStream
    }

    def "save multiple repositories"() {
        given:
        def tarStream = new ByteArrayInputStream("tar".bytes)

        when:
        def response = dockerClient.save("image:tag1", "an-id")

        then:
        1 * httpClient.get([path : "/images/get",
                            query: [names: ["image:tag1", "an-id"]]]) >> [status: [success: true],
                                                                          stream: tarStream]
        and:
        dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker save failed"
        }
        and:
        response.stream == tarStream
    }

    def "load"() {
        given:
        def archive = getClass().getResourceAsStream('importUrl/import-from-url.tar')

        when:
        def response = dockerClient.load(archive)

        then:
        1 * httpClient.post([path              : "/images/load",
                             body              : archive,
                             requestContentType: "application/x-tar"]) >> [status: [success: true]]
        and:
        dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker load failed"
        }
        and:
        response.status == [success: true]
    }

    def "export container"() {
        when:
        def response = dockerClient.export("container-id")

        then:
        1 * httpClient.get([path: "/containers/container-id/export"]) >> [content: [status: "image-id"]]
        and:
        dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker export failed"
        }
        and:
        response
    }

    def "restart container"() {
        when:
        dockerClient.restart("a-container")

        then:
        1 * httpClient.post([path : "/containers/a-container/restart",
                             query: [t: 5]])
    }

    def "stop container"() {
        when:
        dockerClient.stop("a-container")

        then:
        1 * httpClient.post([path: "/containers/a-container/stop"])
    }

    def "kill container"() {
        when:
        dockerClient.kill("a-container")

        then:
        1 * httpClient.post([path: "/containers/a-container/kill"])
    }

    def "wait container"() {
        when:
        dockerClient.wait("a-container")

        then:
        1 * httpClient.post([path: "/containers/a-container/wait"])
    }

    def "pause container"() {
        when:
        dockerClient.pause("a-container")

        then:
        1 * httpClient.post([path: "/containers/a-container/pause"])
    }

    def "unpause container"() {
        when:
        dockerClient.unpause("a-container")

        then:
        1 * httpClient.post([path: "/containers/a-container/unpause"])
    }

    def "rm container"() {
        when:
        dockerClient.rm("a-container")

        then:
        1 * httpClient.delete([path: "/containers/a-container"])
    }

    def "ps containers"() {
        when:
        dockerClient.ps()

        then:
        1 * httpClient.get([path : "/containers/json",
                            query: [all : true,
                                    size: false]]) >> [status: [success: true]]
    }

    def "ps containers with query"() {
        given:
        def filters = [status: ["exited"]]
        def expectedFilterValue = new JsonBuilder(filters).toString()

        when:
        dockerClient.ps([filters: filters])

        then:
        1 * httpClient.get([path : "/containers/json",
                            query: [all    : true,
                                    size   : false,
                                    filters: expectedFilterValue]]) >> [status: [success: true]]
    }

    def "inspect container"() {
        when:
        dockerClient.inspectContainer("a-container")

        then:
        1 * httpClient.get([path: "/containers/a-container/json"]) >> [status : [success: true],
                                                                       content: [:]]
    }

    def "diff"() {
        when:
        dockerClient.diff("a-container")

        then:
        1 * httpClient.get([path: "/containers/a-container/changes"])
    }

    def "inspect image"() {
        when:
        dockerClient.inspectImage("an-image")

        then:
        1 * httpClient.get([path: "/images/an-image/json"]) >> [status : [success: true],
                                                                content: [:]]
    }

    def "history"() {
        when:
        dockerClient.history("an-image")

        then:
        1 * httpClient.get([path: "/images/an-image/history"])
    }

    def "images with defaults"() {
        when:
        dockerClient.images()

        then:
        1 * httpClient.get([path : "/images/json",
                            query: [all: false]]) >> [status: [success: true]]
    }

    def "images with query"() {
        given:
        def filters = [dangling: ["true"]]
        def expectedFilterValue = new JsonBuilder(filters).toString()
        def query = [all    : true,
                     filters: filters]

        when:
        dockerClient.images(query)

        then:
        1 * httpClient.get([path : "/images/json",
                            query: [all    : true,
                                    filters: expectedFilterValue]]) >> [status: [success: true]]
    }

    def "findImageId by image name"() {
        given:
        dockerClient.images([:]) >> [content: [[RepoTags: ['anImage:latest'],
                                                Id      : 'the-id']]]

        expect:
        dockerClient.findImageId('anImage') == 'the-id'
    }

    def "findImageId with missing image"() {
        given:
        dockerClient.images([:]) >> [content: []]

        expect:
        dockerClient.findImageId('anImage') == 'anImage:latest'
    }

    def "findImageId by digest"() {

        given:
        dockerClient.images(_) >> [content: [[RepoDigests: ['anImage@sha256:4711'],
                                              Id         : 'the-id']]]

        expect:
        dockerClient.findImageId('anImage@sha256:4711') == 'the-id'
    }

    def "rmi image"() {
        when:
        dockerClient.rmi("an-image")

        then:
        1 * httpClient.delete([path: "/images/an-image"])
    }

    def "create exec"() {
        def execCreateConfig = [:]

        when:
        dockerClient.createExec("a-container", execCreateConfig)

        then:
        1 * httpClient.post([path              : "/containers/a-container/exec",
                             body              : execCreateConfig,
                             requestContentType: "application/json"]) >> [status: [:]]
        and:
        dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker exec create failed"
        }
    }

    def "create exec with missing container"() {
        def execCreateConfig = [:]
        given:
        httpClient.post([path              : "/containers/a-missing-container/exec",
                         body              : execCreateConfig,
                         requestContentType: "application/json"]) >> [status: [code: 404]]
        MemoryAppender.clearLoggedEvents()

        when:
        dockerClient.createExec("a-missing-container", execCreateConfig)

        then:
        MemoryAppender.findLoggedEvent([level: ERROR, message: "no such container 'a-missing-container'"])
        and:
        thrown(DockerClientException)

        cleanup:
        MemoryAppender.clearLoggedEvents()
    }

    def "start exec"() {
        def execStartConfig = [:]

        when:
        dockerClient.startExec("an-exec", execStartConfig)

        then:
        1 * httpClient.get([path: "/exec/an-exec/json"]) >> [
                status : [success: true],
                content: [ProcessConfig: [tty: true]]
        ]
        and:
        1 * dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker inspect exec failed"
        }

        then:
        1 * httpClient.post([path              : "/exec/an-exec/start",
                             body              : execStartConfig,
                             requestContentType: "application/json",
                             attach            : null,
                             multiplexStreams  : false]) >> [status: [:],
                                                             stream: [:]]
        and:
        1 * dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker exec start failed"
        }
    }

    def "start exec with missing exec"() {
        def execStartConfig = [:]
        given:
        httpClient.get([path: "/exec/a-missing-exec/json"]) >> [status: [code: 404]]
//        httpClient.post([path              : "/exec/a-missing-exec/start",
//                         body              : execStartConfig,
//                         requestContentType: "application/json"]) >> [status: [code: 404]]
        MemoryAppender.clearLoggedEvents()

        when:
        dockerClient.startExec("a-missing-exec", execStartConfig)

        then:
        MemoryAppender.findLoggedEvent([level: ERROR, message: "no such exec 'a-missing-exec'"])
        and:
        thrown(DockerClientException)

        cleanup:
        MemoryAppender.clearLoggedEvents()
    }

    def "inspect exec"() {
        when:
        dockerClient.inspectExec("an-exec")

        then:
        1 * httpClient.get([path: "/exec/an-exec/json"]) >> [status: [:]]
        and:
        dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker inspect exec failed"
        }
    }

    def "inspect exec with missing exec"() {
        given:
        MemoryAppender.clearLoggedEvents()

        when:
        dockerClient.inspectExec("a-missing-exec")

        then:
        1 * httpClient.get([path: "/exec/a-missing-exec/json"]) >> [status: [code: 404]]
        and:
        MemoryAppender.findLoggedEvent([level: ERROR, message: "no such exec 'a-missing-exec'"])
        and:
        thrown(DockerClientException)

        cleanup:
        MemoryAppender.clearLoggedEvents()
    }

    def "exec"() {
        def execConfig = [:]

        when:
        dockerClient.exec("container-id", ["command", "line"], execConfig)

        then:
        1 * dockerClient.createExec("container-id", [
                "AttachStdin" : false,
                "AttachStdout": true,
                "AttachStderr": true,
                "Detach"      : false,
                "Tty"         : false,
                "Cmd"         : ["command", "line"]]) >> [status : [:],
                                                          content: [Id: "exec-id"]]
        then:
        1 * dockerClient.startExec("exec-id", [
                "AttachStdin" : false,
                "AttachStdout": true,
                "AttachStderr": true,
                "Detach"      : false,
                "Tty"         : false,
                "Cmd"         : ["command", "line"]]) >> [status: [:]]
    }

    def "create container with defaults"() {
        def containerConfig = [Cmd: "true"]

        when:
        dockerClient.createContainer(containerConfig)

        then:
        1 * httpClient.post([path              : "/containers/create",
                             query             : [name: ""],
                             body              : containerConfig,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "create container with query"() {
        def containerConfig = [Cmd: "true"]
        def query = [name: "foo"]

        when:
        dockerClient.createContainer(containerConfig, query)

        then:
        1 * httpClient.post([path              : "/containers/create",
                             query             : query,
                             body              : containerConfig,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "start container"() {
        when:
        dockerClient.startContainer("a-container")

        then:
        1 * httpClient.post([path              : "/containers/a-container/start",
                             requestContentType: "application/json"])
    }

    def "update a container's resources"() {
        def containerConfig = [:]

        when:
        dockerClient.updateContainer("a-container", containerConfig)

        then:
        1 * httpClient.post([path              : "/containers/a-container/update",
                             body              : containerConfig,
                             requestContentType: "application/json"]) >> [status: [:]]
        and:
        dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker update failed"
        }
    }

    def "update multiple containers' resources"() {
        def containerConfig = [:]

        when:
        dockerClient.updateContainers(["container1", "container2"], containerConfig)

        then:
        1 * httpClient.post([path              : "/containers/container1/update",
                             body              : containerConfig,
                             requestContentType: "application/json"]) >> [status: [:]]
        1 * httpClient.post([path              : "/containers/container2/update",
                             body              : containerConfig,
                             requestContentType: "application/json"]) >> [status: [:]]
        and:
        dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker update failed"
        }
    }

    def "run container with defaults"() {
        when:
        dockerClient.run("an-image", [:])

        then:
        1 * dockerClient.createContainer(["Image": "an-image"], [name: ""]) >> [content: [Id: "container-id"]]

        then:
        1 * dockerClient.startContainer("container-id")
    }

    def "retrieve file/folder stats"() {
        given:
        def containerPathStatHeader = 'X-Docker-Container-Path-Stat'.toLowerCase()
        def expectedStats = [key: 42]
        def encodedStats = new JsonBuilder(expectedStats).toString().bytes.encodeBase64()
        def expectedResponse = [status : [success: true],
                                headers: [:]]
        expectedResponse.headers[containerPathStatHeader] = [encodedStats]

        when:
        def stats = dockerClient.getArchiveStats("a-container", "/path/")

        then:
        1 * httpClient.head([path : "/containers/a-container/archive",
                             query: [path: "/path/"]]) >> expectedResponse
        stats == [key: 42]
    }

    def "download file/folder from container"() {
        given:
        def tarStream = new ByteArrayInputStream("tar".bytes)
        def containerPathStatHeader = 'X-Docker-Container-Path-Stat'.toLowerCase()
        def expectedStats = [key: 42]
        def encodedStats = new JsonBuilder(expectedStats).toString().bytes.encodeBase64()
        def expectedResponse = [status : [success: true],
                                headers: [:],
                                stream : tarStream]
        expectedResponse.headers[containerPathStatHeader] = encodedStats

        when:
        def result = dockerClient.getArchive("a-container", "/path/")

        then:
        1 * httpClient.get([path : "/containers/a-container/archive",
                            query: [path: "/path/"]]) >> expectedResponse
        result.headers[containerPathStatHeader] == encodedStats
        result.stream == tarStream
    }

    def "upload file/folder to container"() {
        given:
        def tarStream = new ByteArrayInputStream("tar".bytes)
        def expectedResponse = [status: [success: true]]

        when:
        dockerClient.putArchive("a-container", "/path/", tarStream)

        then:
        1 * httpClient.put([path              : "/containers/a-container/archive",
                            query             : [path: "/path/"],
                            requestContentType: "application/x-tar",
                            body              : tarStream]) >> expectedResponse
    }

    def "rename container"() {
        when:
        dockerClient.rename("an-old-container", "a-new-container-name")

        then:
        1 * httpClient.post([path : "/containers/an-old-container/rename",
                             query: [name: "a-new-container-name"]]) >> [status: [success: true]]
    }

    def "search"() {
        when:
        dockerClient.search("ubuntu")

        then:
        1 * httpClient.get([path : "/images/search",
                            query: [term: "ubuntu"]]) >> [status: [success: true]]
    }

    def "attach"() {
        given:
        httpClient.get([path: "/containers/a-container/json"]) >> [status : [success: true],
                                                                   content: [Config: [Tty: false]]]

        when:
        dockerClient.attach("a-container", [stream: true])

        then:
        1 * httpClient.post([path            : "/containers/a-container/attach",
                             query           : [stream: true],
                             attach          : null,
                             multiplexStreams: true]) >> [stream: [:]]
    }

    // TODO
    @Ignore
    def "attach websocket"() {
//        given:
//        def listener = new DefaultWebSocketListener()
//        def wsCall = WebSocketCall.create(
//                new OkHttpClient.Builder().build(),
//                new Request.Builder()
//                        .url("").build())
//
//        when:
//        dockerClient.attachWebsocket("a-container", [stream: true], listener)
//
//        then:
//        1 * httpClient.webSocketCall(
//                [path : "/containers/a-container/attach/ws",
//                 query: [stream: true]]) >> wsCall
//        and:
//        1 * wsCall.enqueue(listener)
    }

    def "commit container"() {
        when:
        dockerClient.commit("a-container", [
                repo   : 'a-repo',
                tag    : 'the-tag',
                comment: 'a test',
                author : 'Andrew Niccol <g@tta.ca>'
        ])

        then:
        1 * httpClient.post([path              : "/commit",
                             query             : [
                                     container: "a-container",
                                     repo     : 'a-repo',
                                     tag      : 'the-tag',
                                     comment  : 'a test',
                                     author   : 'Andrew Niccol <g@tta.ca>'
                             ],
                             requestContentType: "application/json",
                             body              : [:]]) >> [status: [success: true]]
    }

    def "commit container with changed container config"() {
        when:
        dockerClient.commit("a-container",
                [
                        repo   : 'a-repo',
                        tag    : 'the-tag',
                        comment: 'a test',
                        author : 'Andrew Niccol <g@tta.ca>'
                ],
                [Cmd: "date"])

        then:
        1 * httpClient.post([path              : "/commit",
                             query             : [
                                     container: "a-container",
                                     repo     : 'a-repo',
                                     tag      : 'the-tag',
                                     comment  : 'a test',
                                     author   : 'Andrew Niccol <g@tta.ca>'
                             ],
                             requestContentType: "application/json",
                             body              : [Cmd: "date"]]) >> [status: [success: true]]
    }

    def "resize container tty"() {
        when:
        dockerClient.resizeTTY("a-container", 42, 31)

        then:
        1 * httpClient.post([path              : "/containers/a-container/resize",
                             query             : [w: 31, h: 42],
                             requestContentType: "text/plain"]) >> [status: [success: true]]
    }

    def "resize exec tty"() {
        when:
        dockerClient.resizeExec("an-exec", 42, 31)

        then:
        1 * httpClient.post([path              : "/exec/an-exec/resize",
                             query             : [w: 31, h: 42],
                             requestContentType: "text/plain"]) >> [status: [success: true]]
    }

    def "events (streaming)"() {
        given:
        def latch = new CountDownLatch(1)
        def content = new ByteArrayInputStream('{"status":"created"}\n'.bytes)
        DockerAsyncCallback callback = new DockerAsyncCallback() {
            def events = []

            @Override
            def onEvent(Object event) {
                events << event
                latch.countDown()
            }
        }

        when:
        dockerClient.events(callback)
        latch.await()

        then:
        1 * httpClient.get([path: "/events", query: [:], async: true]) >> new DockerResponse(
                status: [success: true],
                stream: content)
        and:
        callback.events.first() == '{"status":"created"}'
    }

    def "events (polling)"() {
        given:
        def latch = new CountDownLatch(1)
        def content = new ByteArrayInputStream('{"status":"created"}\n'.bytes)
        DockerAsyncCallback callback = new DockerAsyncCallback() {
            def events = []

            @Override
            def onEvent(Object event) {
                events << event
                latch.countDown()
            }
        }
        def since = new Date().time

        when:
        dockerClient.events(callback, [since: since])
        latch.await()

        then:
        1 * httpClient.get([path: "/events", query: [since: since], async: true]) >> new DockerResponse(
                status: [success: true],
                stream: content)
        and:
        callback.events.first() == '{"status":"created"}'
    }

    def "events (with filters)"() {
        when:
        dockerClient.events(Mock(DockerAsyncCallback), [filters: [container: ["foo"]]])

        then:
        1 * httpClient.get([path: "/events", query: ['filters': '{"container":["foo"]}'], async: true]) >> new DockerResponse(
                status: [success: true])
    }

    def "top"() {
        when:
        dockerClient.top("a-container", "aux")

        then:
        1 * httpClient.get([path : "/containers/a-container/top",
                            query: [ps_args: "aux"]]) >> [status: [success: true]]
    }

    def "stats"() {
        when:
        dockerClient.stats("a-container")

        then:
        1 * httpClient.get([path : "/containers/a-container/stats",
                            query: [stream: false],
                            async: false]) >> [status: [success: true]]
    }

    def "logs"() {
        given:
        httpClient.get([path: "/containers/a-container/json"]) >> [status : [success: true],
                                                                   content: [Config: [Tty: false]]]

        when:
        dockerClient.logs("a-container")

        then:
        1 * httpClient.get([path : "/containers/a-container/logs",
                            query: [follow: false, stdout: true, stderr: true, timestamps: false, since: 0, tail: 'all'],
                            async: false]) >> [status: [success: true]]
    }

    def "volumes with query"() {
        given:
        def filters = [dangling: ["true"]]
        def expectedFilterValue = new JsonBuilder(filters).toString()
        def query = [filters: filters]

        when:
        dockerClient.volumes(query)

        then:
        1 * httpClient.get([path : "/volumes",
                            query: [filters: expectedFilterValue]]) >> [status: [success: true]]
    }

    def "inspect volume"() {
        when:
        dockerClient.inspectVolume("a-volume")

        then:
        1 * httpClient.get([path: "/volumes/a-volume"]) >> [status: [success: true]]
    }

    def "create volume with config"() {
        def volumeConfig = [Name      : "my-volume",
                            Driver    : "local",
                            DriverOpts: [:]]

        when:
        dockerClient.createVolume(volumeConfig)

        then:
        1 * httpClient.post([path              : "/volumes/create",
                             body              : volumeConfig,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "rm volume"() {
        when:
        dockerClient.rmVolume("a-volume")

        then:
        1 * httpClient.delete([path: "/volumes/a-volume"]) >> [status: [success: true]]
    }

    def "networks with query"() {
        given:
        def filters = [name: ["a-net"], id: ["a-net-id"]]
        def expectedFilterValue = new JsonBuilder(filters).toString()
        def query = [filters: filters]

        when:
        dockerClient.networks(query)

        then:
        1 * httpClient.get([path : "/networks",
                            query: [filters: expectedFilterValue]]) >> [status: [success: true]]
    }

    def "inspect network"() {
        when:
        dockerClient.inspectNetwork("a-network")

        then:
        1 * httpClient.get([path: "/networks/a-network"]) >> [status: [success: true]]
    }

    def "create network with config"() {
        given:
        def networkConfig = [Driver        : "bridge",
                             Options       : [:],
                             CheckDuplicate: true]
        def expectedNetworkConfig = [Name          : "network-name",
                                     Driver        : "bridge",
                                     Options       : [:],
                                     CheckDuplicate: true]

        when:
        dockerClient.createNetwork("network-name", networkConfig)

        then:
        1 * httpClient.post([path              : "/networks/create",
                             body              : expectedNetworkConfig,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "connect a container to a network"() {
        when:
        dockerClient.connectNetwork("a-network", "a-container")

        then:
        1 * httpClient.post([path              : "/networks/a-network/connect",
                             body              : [container: "a-container"],
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "disconnect a container from a network"() {
        when:
        dockerClient.disconnectNetwork("a-network", "a-container")

        then:
        1 * httpClient.post([path              : "/networks/a-network/disconnect",
                             body              : [container: "a-container"],
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "rm network"() {
        when:
        dockerClient.rmNetwork("a-network")

        then:
        1 * httpClient.delete([path: "/networks/a-network"]) >> [status: [success: true]]
    }

    def "list nodes with query"() {
        given:
        def filters = [membership: ["accepted"], role: ["worker"]]
        def expectedFilterValue = new JsonBuilder(filters).toString()
        def query = [filters: filters]

        when:
        dockerClient.nodes(query)

        then:
        1 * httpClient.get([path : "/nodes",
                            query: [filters: expectedFilterValue]]) >> [status: [success: true]]
    }

    def "inspect node"() {
        when:
        dockerClient.inspectNode("node-id")

        then:
        1 * httpClient.get([path: "/nodes/node-id"]) >> [status: [success: true]]
    }

    def "update node"() {
        given:
        def query = [version: 42]
        def config = [
                "AcceptancePolicy": [
                        "Policies": [
                                ["Role": "MANAGER", "Autoaccept": false],
                                ["Role": "WORKER", "Autoaccept": true]
                        ]
                ]
        ]

        when:
        dockerClient.updateNode("node-id", query, config)

        then:
        1 * httpClient.post([path              : "/nodes/node-id/update",
                             query             : query,
                             body              : config,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "rm node"() {
        when:
        dockerClient.rmNode("node-id")

        then:
        1 * httpClient.delete([path: "/nodes/node-id"]) >> [status: [success: true]]
    }

    def "inspect swarm"() {
        given:
        def filters = [membership: ["accepted"], role: ["worker"]]
        def expectedFilterValue = new JsonBuilder(filters).toString()
        def query = [filters: filters]

        when:
        dockerClient.inspectSwarm(query)

        then:
        1 * httpClient.get([path : "/swarm",
                            query: [filters: expectedFilterValue]]) >> [status: [success: true]]
    }

    def "initialize a swarm"() {
        given:
        def config = [
                "ListenAddr"     : "0.0.0.0:4500",
                "ForceNewCluster": false,
                "Spec"           : [
                        "AcceptancePolicy": [
                                "Policies": [
                                        ["Role": "MANAGER", "Autoaccept": false],
                                        ["Role": "WORKER", "Autoaccept": true]
                                ]
                        ],
                        "Orchestration"   : [:],
                        "Raft"            : [:],
                        "Dispatcher"      : [:],
                        "CAConfig"        : [:]
                ]
        ]

        when:
        dockerClient.initSwarm(config)

        then:
        1 * httpClient.post([path              : "/swarm/init",
                             body              : config,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "join a swarm"() {
        given:
        def config = [
                "ListenAddr": "0.0.0.0:4500",
                "RemoteAddr": "node1:4500",
                "Secret"    : "",
                "CAHash"    : "",
                "Manager"   : false
        ]

        when:
        dockerClient.joinSwarm(config)

        then:
        1 * httpClient.post([path              : "/swarm/join",
                             body              : config,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "leave a swarm"() {
        when:
        dockerClient.leaveSwarm()

        then:
        1 * httpClient.post([path : "/swarm/leave",
                             query: [:]]) >> [status: [success: true]]
    }

    def "update a swarm"() {
        given:
        def query = [version: 42]
        def config = [
                "AcceptancePolicy": [
                        "Policies": [
                                ["Role": "MANAGER", "Autoaccept": false],
                                ["Role": "WORKER", "Autoaccept": false]
                        ]
                ]
        ]

        when:
        dockerClient.updateSwarm(query, config)

        then:
        1 * httpClient.post([path              : "/swarm/update",
                             query             : query,
                             body              : config,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "list services with query"() {
        given:
        def filters = [name: ["node-name"]]
        def expectedFilterValue = new JsonBuilder(filters).toString()
        def query = [filters: filters]

        when:
        dockerClient.services(query)

        then:
        1 * httpClient.get([path : "/services",
                            query: [filters: expectedFilterValue]]) >> [status: [success: true]]
    }

    def "create a service"() {
        given:
        def config = [
                "Name"        : "redis",
                "Task"        : [
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

        when:
        dockerClient.createService(config)

        then:
        1 * httpClient.post([path              : "/services/create",
                             body              : config,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "rm service"() {
        when:
        dockerClient.rmService("service-name")

        then:
        1 * httpClient.delete([path: "/services/service-name"]) >> [status: [success: true]]
    }

    def "inspect service"() {
        when:
        dockerClient.inspectService("service-name")

        then:
        1 * httpClient.get([path: "/services/service-name"]) >> [status: [success: true]]
    }

    def "update service"() {
        given:
        def query = [version: 42]
        def config = [
                "Name"        : "redis",
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

        when:
        dockerClient.updateService("service-name", query, config)

        then:
        1 * httpClient.post([path              : "/services/service-name/update",
                             query             : query,
                             body              : config,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "list tasks with query"() {
        given:
        def filters = [name: ["service-name"]]
        def expectedFilterValue = new JsonBuilder(filters).toString()
        def query = [filters: filters]

        when:
        dockerClient.tasks(query)

        then:
        1 * httpClient.get([path : "/tasks",
                            query: [filters: expectedFilterValue]]) >> [status: [success: true]]
    }

    def "inspect task"() {
        when:
        dockerClient.inspectTask("task-id")

        then:
        1 * httpClient.get([path: "/tasks/task-id"]) >> [status: [success: true]]
    }

    def "cleanupStorage removes exited containers"() {
        given:
        def keepContainer = { container ->
            container.Names.any { String name ->
                name.replaceAll("^/", "").matches(".*data.*")
            }
        }

        when:
        dockerClient.cleanupStorage keepContainer

        then:
        1 * dockerClient.ps([filters: [status: ["exited"]]]) >> [
                content: [
                        [Command: "ping 127.0.0.1",
                         Id     : "container-id-1",
                         Image  : "gesellix/docker-client-testimage:latest",
                         Names  : ["/agitated_bardeen"],
                         Status : "Exited (137) 13 minutes ago"],
                        [Command: "ping 127.0.0.1",
                         Id     : "container-id-2",
                         Image  : "gesellix/docker-client-testimage:latest",
                         Names  : ["/my_data"],
                         Status : "Exited (137) 13 minutes ago"]
                ]
        ]
        then:
        1 * dockerClient.rm("container-id-1")
        and:
        0 * dockerClient.rm("container-id-2")
        and:
        1 * dockerClient.images([filters: [dangling: ["true"]]]) >> [:]
        and:
        1 * dockerClient.volumes([filters: [dangling: ["true"]]]) >> [content: [[Name: "volume-id"]]]
        and:
        0 * dockerClient.rmVolume(_)
    }

    def "cleanupStorage removes dangling images"() {
        when:
        dockerClient.cleanupStorage { container -> false }

        then:
        1 * dockerClient.ps([filters: [status: ["exited"]]]) >> [:]
        and:
        1 * dockerClient.images([filters: [dangling: ["true"]]]) >> [
                content: [
                        [Created    : 1420075526,
                         Id         : "image-id-1",
                         ParentId   : "f62feddc05dc67da9b725361f97d7ae72a32e355ce1585f9a60d090289120f73",
                         RepoTags   : ["<none>": "<none>"],
                         Size       : 0,
                         VirtualSize: 188299119]
                ]
        ]
        then:
        1 * dockerClient.rmi("image-id-1")
        and:
        1 * dockerClient.volumes([filters: [dangling: ["true"]]]) >> [content: [[Name: "volume-id"]]]
        and:
        0 * dockerClient.rmVolume(_)
    }

    def "cleanupStorage doesn't remove dangling volumes by default"() {
        when:
        dockerClient.cleanupStorage { container -> false }

        then:
        1 * dockerClient.ps([filters: [status: ["exited"]]]) >> [:]
        and:
        1 * dockerClient.images([filters: [dangling: ["true"]]]) >> [:]
        and:
        1 * dockerClient.volumes([filters: [dangling: ["true"]]]) >> [
                content: [
                        [Name: "volume-id"]]]
        and:
        0 * dockerClient.rmVolume(_)
    }

    def "cleanupStorage removes dangling volumes when desired"() {
        when:
        dockerClient.cleanupStorage({ container -> false }, { volume -> volume.Name != "volume-id-1" })

        then:
        1 * dockerClient.ps([filters: [status: ["exited"]]]) >> [:]
        and:
        1 * dockerClient.images([filters: [dangling: ["true"]]]) >> [:]
        and:
        1 * dockerClient.volumes([filters: [dangling: ["true"]]]) >> [
                content: [
                        Volumes: [
                                [Name: "volume-id-1"],
                                [Name: "volume-id-2"]]]]
        and:
        1 * dockerClient.rmVolume("volume-id-1") >> [status: [success: true]]
        and:
        0 * dockerClient.rmVolume("volume-id-2")
    }
}
