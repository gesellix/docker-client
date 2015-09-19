package de.gesellix.docker.client

import groovy.json.JsonBuilder
import org.slf4j.Logger
import spock.lang.Specification
import spock.lang.Unroll

class DockerClientImplSpec extends Specification {

    def DockerClientImpl dockerClient = Spy(DockerClientImpl)
    def LowLevelDockerClient httpClient = Mock(LowLevelDockerClient)

    def setup() {
        dockerClient.responseHandler = Spy(DockerResponseHandler)
        dockerClient.newDockerHttpClient = { dockerHost, proxy -> httpClient }
    }

    def "read and encode authConfig (old format)"() {
        given:
        def dockerCfg = new ResourceReader().getClasspathResourceAsFile('/auth/dockercfg')
        def authDetails = dockerClient.readAuthConfig(null, dockerCfg)
        def authPlain = authDetails

        when:
        def authResult = dockerClient.encodeAuthConfig(authPlain)

        then:
        authResult == 'eyJ1c2VybmFtZSI6Imdlc2VsbGl4IiwicGFzc3dvcmQiOiIteWV0LWFub3RoZXItcGFzc3dvcmQtIiwiZW1haWwiOiJ0b2JpYXNAZ2VzZWxsaXguZGUiLCJzZXJ2ZXJhZGRyZXNzIjoiaHR0cHM6Ly9pbmRleC5kb2NrZXIuaW8vdjEvIn0='
    }

    def "read and encode authConfig (new format)"() {
        given:
        def dockerCfg = new ResourceReader().getClasspathResourceAsFile('/auth/config.json')
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
        1 * httpClient.get([path: "/_ping"])
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
        def dockerConfigFile = dockerClient.getActualDockerConfigFile()

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
        def expectedConfigFile = new ResourceReader().getClasspathResourceAsFile('/auth/config.json')
        dockerClient.dockerConfigFile = expectedConfigFile

        when:
        dockerClient.readDefaultAuthConfig()

        then:
        1 * dockerClient.readAuthConfig(null, expectedConfigFile)
        dockerClient.legacyDockerConfigFile

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
        dockerClient.dockerConfigFile = nonExistingFile
        def expectedConfigFile = new ResourceReader().getClasspathResourceAsFile('/auth/dockercfg')
        dockerClient.legacyDockerConfigFile = expectedConfigFile

        when:
        dockerClient.readDefaultAuthConfig()

        then:
        1 * dockerClient.readAuthConfig(null, expectedConfigFile)
        dockerClient.dockerConfigFile
        dockerClient.legacyDockerConfigFile

        cleanup:
        if (oldDockerConfig) {
            System.setProperty("docker.config", oldDockerConfig)
        }
    }

    def "read auth config for official Docker index"() {
        given:
        def dockerCfg = new ResourceReader().getClasspathResourceAsFile('/auth/config.json')

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
        def dockerCfg = new ResourceReader().getClasspathResourceAsFile('/auth/config.json')

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
        def dockerCfg = new ResourceReader().getClasspathResourceAsFile('/auth/config.json')

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
                             query: [repo : "registry:port/username/image-name",
                                     tag  : "a-tag",
                                     force: false]])
    }

    def "tag with force == true"() {
        when:
        dockerClient.tag("an-image", "registry:port/username/image-name:a-tag", true)

        then:
        1 * httpClient.post([path : "/images/an-image/tag",
                             query: [repo : "registry:port/username/image-name",
                                     tag  : "a-tag",
                                     force: true]])
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
                             query: [repo : "registry:port/an-image",
                                     tag  : "",
                                     force: true]])
        then:
        1 * httpClient.post([path   : "/images/registry:port/an-image/push",
                             query  : [tag: ""],
                             headers: ["X-Registry-Auth": "."]]) >> [status: [success: true]]
    }

    def "pull with defaults"() {
        given:
        dockerClient.images() >> [content: [:]]

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
        dockerClient.images() >> [content: [:]]

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
        dockerClient.images() >> [content: [:]]

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
        dockerClient.images() >> [content: [:]]

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
        1 * httpClient.post([path   : "/images/create",
                             query  : [fromSrc: importUrl.toString(),
                                       repo   : "imported-from-url",
                                       tag    : "foo"],
                             headers: []]) >> [content: [[status: "image-id"]]]
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
                                                  tag    : "foo"],
                             headers           : []]) >> [content: [status: "image-id"]]
        and:
        dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker import from stream failed"
        }
        and:
        imageId == "image-id"
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
                             query: [t: 10]])
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

    def "findImageId with existing image"() {
        given:
        dockerClient.images() >> [content: [[RepoTags: ['anImage:latest'],
                                             Id      : 'the-id']]]

        expect:
        dockerClient.findImageId('anImage') == 'the-id'
    }

    def "findImageId with missing image"() {
        given:
        dockerClient.images() >> [content: []]

        expect:
        dockerClient.findImageId('anImage') == 'anImage:latest'
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
        dockerClient.logger = Mock(Logger)

        when:
        dockerClient.createExec("a-missing-container", execCreateConfig)

        then:
        1 * dockerClient.logger.error("no such container 'a-missing-container'")
        and:
        thrown(DockerClientException)
    }

    def "start exec"() {
        def execStartConfig = [:]

        when:
        dockerClient.startExec("an-exec", execStartConfig)

        then:
        1 * httpClient.post([path              : "/exec/an-exec/start",
                             body              : execStartConfig,
                             requestContentType: "application/json"]) >> [status: [:]]
        and:
        dockerClient.responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker exec start failed"
        }
    }

    def "start exec with missing exec"() {
        def execStartConfig = [:]
        given:
        httpClient.post([path              : "/exec/a-missing-exec/start",
                         body              : execStartConfig,
                         requestContentType: "application/json"]) >> [status: [code: 404]]
        dockerClient.logger = Mock(Logger)

        when:
        dockerClient.startExec("a-missing-exec", execStartConfig)

        then:
        1 * dockerClient.logger.error("no such exec 'a-missing-exec'")
        and:
        thrown(DockerClientException)
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

    def "run container with defaults"() {
        when:
        dockerClient.run("an-image", [:])

        then:
        1 * dockerClient.createContainer(["Image": "an-image"], [name: ""]) >> [content: [Id: "container-id"]]

        then:
        1 * dockerClient.startContainer("container-id")
    }

    def "copy from container"() {
        given:
        def tarStream = new ByteArrayInputStream("tar".bytes)

        when:
        def result = dockerClient.copy("a-container", [Resource: "/file.txt"])

        then:
        1 * httpClient.post([path              : "/containers/a-container/copy",
                             body              : [Resource: "/file.txt"],
                             requestContentType: "application/json"]) >> [status: [success: true],
                                                                          stream: tarStream]
        and:
        result.stream == tarStream
    }

    def "copy file from container"() {
        given:
        def tarStream = new ByteArrayInputStream("file-content".bytes)

        when:
        def result = dockerClient.copyFile("a-container", "/file.txt")

        then:
        1 * httpClient.post([path              : "/containers/a-container/copy",
                             body              : [Resource: "/file.txt"],
                             requestContentType: "application/json"]) >> [status: [success: true],
                                                                          stream: tarStream]
        and:
        1 * dockerClient.extractSingleTarEntry(tarStream, "/file.txt") >> "file-content".bytes
        and:
        result == "file-content".bytes
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
        expectedResponse.headers[containerPathStatHeader] = [encodedStats]

        when:
        def result = dockerClient.getArchive("a-container", "/path/")

        then:
        1 * httpClient.get([path : "/containers/a-container/archive",
                            query: [path: "/path/"]]) >> expectedResponse
        result.headers[containerPathStatHeader] == [encodedStats]
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
                             query: [name: "a-new-container-name"]]) >> [:]
    }

    def "search"() {
        when:
        dockerClient.search("ubuntu")

        then:
        1 * httpClient.get([path : "/images/search",
                            query: [term: "ubuntu"]])
    }

    def "attach"() {
        given:
        httpClient.get([path: "/containers/a-container/json"]) >> [status: [success: true],
                                                                   Config: [Tty: false]]

        when:
        dockerClient.attach("a-container", [stream: true])

        then:
        1 * httpClient.post([path : "/containers/a-container/attach",
                             query: [stream: true]]) >> [stream: [:]]
    }

    def "attach websocket"() {
        given:
        def wsHandler = [:]

        when:
        dockerClient.attachWebsocket("a-container", [stream: true], wsHandler)

        then:
        1 * httpClient.getWebsocketClient(
                [path : "/containers/a-container/attach/ws",
                 query: [stream: true]],
                wsHandler)
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
    }
}
