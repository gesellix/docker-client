package de.gesellix.docker.client.container

import de.gesellix.docker.client.DockerClientException
import de.gesellix.docker.client.DockerResponse
import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.HttpClient
import de.gesellix.docker.client.image.ManageImage
import de.gesellix.docker.testutil.MemoryAppender
import groovy.json.JsonBuilder
import spock.lang.Ignore
import spock.lang.Specification

import static ch.qos.logback.classic.Level.ERROR

class ManageContainerClientTest extends Specification {

    ManageContainerClient service
    HttpClient httpClient = Mock(HttpClient)
    DockerResponseHandler responseHandler = Mock(DockerResponseHandler)

    def setup() {
        service = Spy(ManageContainerClient, constructorArgs: [
                httpClient,
                responseHandler,
                Mock(ManageImage)])
    }

    def "export container"() {
        when:
        def response = service.export("container-id")

        then:
        1 * httpClient.get([path: "/containers/container-id/export"]) >> [content: [status: "image-id"]]
        and:
        responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker export failed"
        }
        and:
        response
    }

    def "restart container"() {
        when:
        service.restart("a-container")

        then:
        1 * httpClient.post([path : "/containers/a-container/restart",
                             query: [t: 5]])
    }

    def "stop container"() {
        when:
        service.stop("a-container")

        then:
        1 * httpClient.post([path: "/containers/a-container/stop"])
    }

    def "kill container"() {
        when:
        service.kill("a-container")

        then:
        1 * httpClient.post([path: "/containers/a-container/kill"])
    }

    def "wait container"() {
        when:
        service.wait("a-container")

        then:
        1 * httpClient.post([path: "/containers/a-container/wait"])
    }

    def "pause container"() {
        when:
        service.pause("a-container")

        then:
        1 * httpClient.post([path: "/containers/a-container/pause"])
    }

    def "unpause container"() {
        when:
        service.unpause("a-container")

        then:
        1 * httpClient.post([path: "/containers/a-container/unpause"])
    }

    def "rm container"() {
        when:
        service.rm("a-container")

        then:
        1 * httpClient.delete([path : "/containers/a-container",
                               query: [:]])
    }

    def "rm container with query"() {
        when:
        service.rm("a-container", ["v": 0])

        then:
        1 * httpClient.delete([path : "/containers/a-container",
                               query: ["v": 0]])
    }

    def "ps containers"() {
        when:
        service.ps()

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
        service.ps([filters: filters])

        then:
        1 * httpClient.get([path : "/containers/json",
                            query: [all    : true,
                                    size   : false,
                                    filters: expectedFilterValue]]) >> [status: [success: true]]
    }

    def "inspect container"() {
        when:
        service.inspectContainer("a-container")

        then:
        1 * httpClient.get([path: "/containers/a-container/json"]) >> [status : [success: true],
                                                                       content: [:]]
    }

    def "diff"() {
        when:
        service.diff("a-container")

        then:
        1 * httpClient.get([path: "/containers/a-container/changes"])
    }

    def "create exec"() {
        def execCreateConfig = [:]

        when:
        service.createExec("a-container", execCreateConfig)

        then:
        1 * httpClient.post([path              : "/containers/a-container/exec",
                             body              : execCreateConfig,
                             requestContentType: "application/json"]) >> [status: [:]]
        and:
        responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
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
        service.createExec("a-missing-container", execCreateConfig)

        then:
        MemoryAppender.findLoggedEvent([level: ERROR, message: "no such container 'a-missing-container'"])
        and:
        1 * responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker exec create failed"
        }

        cleanup:
        MemoryAppender.clearLoggedEvents()
    }

    def "start exec"() {
        def execStartConfig = [:]

        when:
        service.startExec("an-exec", execStartConfig)

        then:
        1 * httpClient.get([path: "/exec/an-exec/json"]) >> [
                status : [success: true],
                content: [ProcessConfig: [tty: true]]
        ]
        and:
        1 * responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
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
        1 * responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker exec start failed"
        }
    }

    def "start exec with missing exec"() {
        def execStartConfig = [:]
        given:
        service.inspectExec('a-missing-exec') >> {
            throw new DockerClientException(new IllegalStateException("docker inspect exec failed"))
        }
        MemoryAppender.clearLoggedEvents()

        when:
        service.startExec("a-missing-exec", execStartConfig)

        then:
        thrown(DockerClientException)

        cleanup:
        MemoryAppender.clearLoggedEvents()
    }

    def "inspect exec"() {
        when:
        service.inspectExec("an-exec")

        then:
        1 * httpClient.get([path: "/exec/an-exec/json"]) >> [status: [:]]
        and:
        responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker inspect exec failed"
        }
    }

    def "inspect exec with missing exec"() {
        given:
        MemoryAppender.clearLoggedEvents()

        when:
        service.inspectExec("a-missing-exec")

        then:
        1 * httpClient.get([path: "/exec/a-missing-exec/json"]) >> new DockerResponse(status: [code: 404])
        and:
        MemoryAppender.findLoggedEvent([level: ERROR, message: "no such exec 'a-missing-exec'"])
        and:
        responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker inspect exec failed"
        }

        cleanup:
        MemoryAppender.clearLoggedEvents()
    }

    def "exec"() {
        def execConfig = [:]

        when:
        service.exec("container-id", ["command", "line"], execConfig)

        then:
        1 * service.createExec("container-id", [
                "AttachStdin" : false,
                "AttachStdout": true,
                "AttachStderr": true,
                "Detach"      : false,
                "Tty"         : false,
                "Cmd"         : ["command", "line"]]) >> [status : [:],
                                                          content: [Id: "exec-id"]]
        then:
        1 * service.startExec("exec-id", [
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
        service.createContainer(containerConfig)

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
        service.createContainer(containerConfig, query)

        then:
        1 * httpClient.post([path              : "/containers/create",
                             query             : query,
                             body              : containerConfig,
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "start container"() {
        when:
        service.startContainer("a-container")

        then:
        1 * httpClient.post([path              : "/containers/a-container/start",
                             requestContentType: "application/json"])
    }

    def "update a container's resources"() {
        def containerConfig = [:]

        when:
        service.updateContainer("a-container", containerConfig)

        then:
        1 * httpClient.post([path              : "/containers/a-container/update",
                             body              : containerConfig,
                             requestContentType: "application/json"]) >> [status: [:]]
        and:
        responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker update failed"
        }
    }

    def "update multiple containers' resources"() {
        def containerConfig = [:]

        when:
        service.updateContainers(["container1", "container2"], containerConfig)

        then:
        1 * httpClient.post([path              : "/containers/container1/update",
                             body              : containerConfig,
                             requestContentType: "application/json"]) >> [status: [:]]
        1 * httpClient.post([path              : "/containers/container2/update",
                             body              : containerConfig,
                             requestContentType: "application/json"]) >> [status: [:]]
        and:
        responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker update failed"
        }
    }

    def "run container with defaults"() {
        when:
        service.run("an-image", [:])

        then:
        1 * service.createContainer(["Image": "an-image"], [name: ""]) >> [content: [Id: "container-id"]]

        then:
        1 * service.startContainer("container-id")
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
        def stats = service.getArchiveStats("a-container", "/path/")

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
        def result = service.getArchive("a-container", "/path/")

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
        service.putArchive("a-container", "/path/", tarStream)

        then:
        1 * httpClient.put([path              : "/containers/a-container/archive",
                            query             : [path: "/path/"],
                            requestContentType: "application/x-tar",
                            body              : tarStream]) >> expectedResponse
    }

    def "rename container"() {
        when:
        service.rename("an-old-container", "a-new-container-name")

        then:
        1 * httpClient.post([path : "/containers/an-old-container/rename",
                             query: [name: "a-new-container-name"]]) >> [status: [success: true]]
    }

    def "attach"() {
        given:
        httpClient.get([path: "/containers/a-container/json"]) >> [status : [success: true],
                                                                   content: [Config: [Tty: false]]]

        when:
        service.attach("a-container", [stream: true])

        then:
        1 * httpClient.post([path            : "/containers/a-container/attach",
                             query           : [stream: true],
                             attach          : null,
                             multiplexStreams: true]) >> [stream: [:]]
    }

    // TODO
    @Ignore
    "attach websocket"() {
//        given:
//        def listener = new DefaultWebSocketListener()
//        def wsCall = new OkHttpClient.Builder().build().newWebSocket(
//                new Request.Builder()
//                        .url("").build(), (listener))
//
//        when:
//        dockerClient.attachWebsocket("a-container", [stream: true], listener)
//
//        then:
//        1 * httpClient.webSocket(
//                [path : "/containers/a-container/attach/ws",
//                 query: [stream: true]], (listener)) >> wsCall
//        and:
//        1 * wsCall.enqueue(listener)
    }

    def "commit container"() {
        when:
        service.commit("a-container", [
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
        service.commit("a-container",
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
        service.resizeTTY("a-container", 42, 31)

        then:
        1 * httpClient.post([path              : "/containers/a-container/resize",
                             query             : [w: 31, h: 42],
                             requestContentType: "text/plain"]) >> [status: [success: true]]
    }

    def "resize exec tty"() {
        when:
        service.resizeExec("an-exec", 42, 31)

        then:
        1 * httpClient.post([path              : "/exec/an-exec/resize",
                             query             : [w: 31, h: 42],
                             requestContentType: "text/plain"]) >> [status: [success: true]]
    }

    def "top"() {
        when:
        service.top("a-container", "aux")

        then:
        1 * httpClient.get([path : "/containers/a-container/top",
                            query: [ps_args: "aux"]]) >> [status: [success: true]]
    }

    def "stats"() {
        when:
        service.stats("a-container")

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
        service.logs("a-container")

        then:
        1 * httpClient.get([path : "/containers/a-container/logs",
                            query: [follow: false, stdout: true, stderr: true, timestamps: false, since: 0, tail: 'all'],
                            async: false]) >> [status: [success: true]]
    }

    def "pruneContainers removes stopped containers"() {
        when:
        service.pruneContainers()

        then:
        1 * httpClient.post([path : "/containers/prune",
                             query: [:]]) >> [status: [success: true]]
    }
}
