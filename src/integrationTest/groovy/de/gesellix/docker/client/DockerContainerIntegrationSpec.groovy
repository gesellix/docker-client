package de.gesellix.docker.client

import de.gesellix.docker.client.util.DockerRegistry
import de.gesellix.docker.client.util.LocalDocker
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ws.WebSocket
import okhttp3.ws.WebSocketCall
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.joda.time.DateTime
import spock.lang.Requires
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

import static de.gesellix.docker.client.util.WebsocketStatusCode.NORMAL_CLOSURE
import static java.util.concurrent.TimeUnit.MILLISECONDS
import static java.util.concurrent.TimeUnit.SECONDS

@Slf4j
@Requires({ LocalDocker.available() })
class DockerContainerIntegrationSpec extends Specification {

    static DockerRegistry registry

    static DockerClient dockerClient

    def setupSpec() {
        dockerClient = new DockerClientImpl()
        registry = new DockerRegistry(dockerClient: dockerClient)
        registry.run()
    }

    def cleanupSpec() {
        registry.rm()
    }

    def ping() {
        when:
        def ping = dockerClient.ping()

        then:
        ping.status.code == 200
        ping.content == "OK"
    }

    def "export from container"() {
        given:
        def archive = getClass().getResourceAsStream('importUrl/import-from-url.tar')
        def imageId = dockerClient.importStream(archive)
        def container = dockerClient.createContainer([Image: imageId, Cmd: ["-"]]).content.Id

        when:
        def response = dockerClient.export(container)

        then:
        listTarEntries(response.stream as InputStream).contains "something.txt"

        cleanup:
        dockerClient.rm(container)
        dockerClient.rmi(imageId)
    }

    def "list containers"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def imageName = "list_containers"
        dockerClient.tag(imageId, imageName)
        def containerConfig = ["Cmd"  : ["true"],
                               "Image": imageName]
        def containerId = dockerClient.createContainer(containerConfig).content.Id
        dockerClient.startContainer(containerId)

        when:
        def containers = dockerClient.ps().content

        then:
        containers.find { it.Id == containerId }.Image == "${imageName}"

        cleanup:
        dockerClient.stop(containerId)
        dockerClient.wait(containerId)
        dockerClient.rm(containerId)
        dockerClient.rmi(imageName)
    }

    def "inspect container"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def imageName = "inspect_container"
        def containerConfig = ["Cmd"       : ["true"],
                               "Image"     : "inspect_container",
                               "HostConfig": ["PublishAllPorts": true]]
        dockerClient.tag(imageId, imageName)
        def containerId = dockerClient.createContainer(containerConfig).content.Id
        dockerClient.startContainer(containerId)

        when:
        def containerInspection = dockerClient.inspectContainer(containerId).content

        then:
        containerInspection.HostnamePath =~ "\\w*/var/lib/docker/containers/${containerId}/hostname".toString()
        and:
        containerInspection.Config.Cmd == ["true"]
        and:
        containerInspection.Config.Image == "inspect_container"
        and:
        containerInspection.Image =~ "${imageId}\\w*"
        and:
        containerInspection.Id == containerId

        cleanup:
        dockerClient.stop(containerId)
        dockerClient.wait(containerId)
        dockerClient.rm(containerId)
        dockerClient.rmi(imageName)
    }

    def "diff"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def containerConfig = ["Cmd"  : ["/bin/sh", "-c", "echo 'hallo' > /change.txt"],
                               "Image": imageId]
        def containerId = dockerClient.run(imageId, containerConfig).container.content.Id
        dockerClient.stop(containerId)

        when:
        def changes = dockerClient.diff(containerId).content

        then:
        changes == [
                [Kind: 1, Path: "/change.txt"]
        ]

        cleanup:
        dockerClient.wait(containerId)
        dockerClient.rm(containerId)
    }

    def "create container"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def containerConfig = ["Cmd"   : ["true"],
                               "Image" : imageId,
                               "Labels": [
                                       "a nice label" : "with a nice value",
                                       "another-label": "{'foo':'bar'}"
                               ]]

        when:
        def containerInfo = dockerClient.createContainer(containerConfig).content

        then:
        containerInfo.Id =~ "\\w+"

        cleanup:
        dockerClient.rm(containerInfo.Id)
    }

    def "create container with name"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def containerConfig = ["Cmd"  : ["true"],
                               "Image": imageId]

        when:
        def containerInfo = dockerClient.createContainer(containerConfig, [name: "example"]).content

        then:
        containerInfo.Id =~ "\\w+"

        cleanup:
        dockerClient.rm("example")
    }

    def "create container with unknown base image"() {
        given:
        dockerClient.rm("example")
        def containerConfig = ["Cmd"  : ["true"],
                               "Image": "gesellix/docker-client-testimage:unkown"]

        when:
        dockerClient.createContainer(containerConfig, [name: "example"])

        then:
        DockerClientException ex = thrown()
        ex.cause.message == 'docker pull failed'
        ex.detail.content.last() == [error      : "Tag unkown not found in repository docker.io/gesellix/docker-client-testimage",
                                     errorDetail: [message: "Tag unkown not found in repository docker.io/gesellix/docker-client-testimage"]]
    }

    def "start container"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def containerConfig = ["Cmd"  : ["true"],
                               "Image": imageId]
        def containerId = dockerClient.createContainer(containerConfig).content.Id

        when:
        def startContainerResult = dockerClient.startContainer(containerId)

        then:
        startContainerResult.status.code == 204

        cleanup:
        dockerClient.stop(containerId)
        dockerClient.wait(containerId)
        dockerClient.rm(containerId)
    }

    def "update container"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def name = "update-container"
        def containerStatus = dockerClient.run(imageName, containerConfig, tag, name)

        when:
        def updateConfig = [
                "Memory"    : 314572800,
                "MemorySwap": 514288000]
        def updateResult = dockerClient.updateContainer(containerStatus.container.content.Id, updateConfig)

        then:
        updateResult.status.success

        cleanup:
        dockerClient.stop(name)
        dockerClient.wait(name)
        dockerClient.rm(name)
    }

    def "run container with existing base image"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]

        when:
        def containerStatus = dockerClient.run(imageName, containerConfig, tag)

        then:
        containerStatus.status.status.code == 204

        cleanup:
        dockerClient.stop(containerStatus.container.content.Id)
        dockerClient.wait(containerStatus.container.content.Id)
        dockerClient.rm(containerStatus.container.content.Id)
    }

    def "run container with PortBindings"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd"       : cmds,
                               ExposedPorts: ["4711/tcp": [:]],
                               "HostConfig": ["PortBindings": [
                                       "4711/tcp": [
                                               ["HostIp"  : "0.0.0.0",
                                                "HostPort": "4712"]]
                               ]]]

        when:
        def containerStatus = dockerClient.run(imageName, containerConfig, tag)

        then:
        containerStatus.status.status.code == 204
        and:
        dockerClient.inspectContainer(containerStatus.container.content.Id).content.Config.ExposedPorts == ["4711/tcp": [:]]
        and:
        dockerClient.inspectContainer(containerStatus.container.content.Id).content.HostConfig.PortBindings == [
                "4711/tcp": [
                        ["HostIp"  : "0.0.0.0",
                         "HostPort": "4712"]]
        ]

        cleanup:
        dockerClient.stop(containerStatus.container.content.Id)
        dockerClient.wait(containerStatus.container.content.Id)
        dockerClient.rm(containerStatus.container.content.Id)
    }

    def "run container with name"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def name = "example-name"

        when:
        def containerStatus = dockerClient.run(imageName, containerConfig, tag, name)

        then:
        containerStatus.status.status.code == 204

        and:
        def containers = dockerClient.ps().content
        containers.findAll { it.Names == ["/example-name"] }?.size() == 1

        cleanup:
        dockerClient.stop(containerStatus.container.content.Id)
        dockerClient.wait(containerStatus.container.content.Id)
        dockerClient.rm(containerStatus.container.content.Id)
    }

    def "restart container"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def containerStatus = dockerClient.run(imageName, containerConfig, tag)

        when:
        def result = dockerClient.restart(containerStatus.container.content.Id)

        then:
        result.status.code == 204

        cleanup:
        dockerClient.stop(containerStatus.container.content.Id)
        dockerClient.wait(containerStatus.container.content.Id)
        dockerClient.rm(containerStatus.container.content.Id)
    }

    def "stop container"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def containerStatus = dockerClient.run(imageName, containerConfig, tag)

        when:
        def result = dockerClient.stop(containerStatus.container.content.Id)

        then:
        result.status.code == 204

        cleanup:
        dockerClient.wait(containerStatus.container.content.Id)
        dockerClient.rm(containerStatus.container.content.Id)
    }

    def "kill container"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def containerStatus = dockerClient.run(imageName, containerConfig, tag)

        when:
        def result = dockerClient.kill(containerStatus.container.content.Id)

        then:
        result.status.code == 204

        cleanup:
        dockerClient.wait(containerStatus.container.content.Id)
        dockerClient.rm(containerStatus.container.content.Id)
    }

    def "wait container"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def containerStatus = dockerClient.run(imageName, containerConfig, tag)
        dockerClient.stop(containerStatus.container.content.Id)

        when:
        def result = dockerClient.wait(containerStatus.container.content.Id)

        then:
        result.status.code == 200
        and:
        result.content.StatusCode == 137

        cleanup:
        dockerClient.rm(containerStatus.container.content.Id)
    }

    def "pause container"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def containerStatus = dockerClient.run(imageName, containerConfig, tag)

        when:
        def result = dockerClient.pause(containerStatus.container.content.Id)

        then:
        result.status.code == 204

        cleanup:
        dockerClient.unpause(containerStatus.container.content.Id)
        dockerClient.stop(containerStatus.container.content.Id)
        dockerClient.wait(containerStatus.container.content.Id)
        dockerClient.rm(containerStatus.container.content.Id)
    }

    def "unpause container"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def containerStatus = dockerClient.run(imageName, containerConfig, tag)
        dockerClient.pause(containerStatus.container.content.Id)

        when:
        def result = dockerClient.unpause(containerStatus.container.content.Id)

        then:
        result.status.code == 204

        cleanup:
        dockerClient.stop(containerStatus.container.content.Id)
        dockerClient.wait(containerStatus.container.content.Id)
        dockerClient.rm(containerStatus.container.content.Id)
    }

    def "rm container"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def containerConfig = ["Cmd"  : ["true"],
                               "Image": imageId]
        def containerId = dockerClient.createContainer(containerConfig).content.Id

        when:
        def rmContainerResult = dockerClient.rm(containerId)

        then:
        rmContainerResult.status.code == 204
    }

    def "rm unknown container"() {
        when:
        def rmContainerResult = dockerClient.rm("a_not_so_random_id")

        then:
        rmContainerResult.status.code == 404
    }

    def "exec create"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def name = "create-exec"
        def containerStatus = dockerClient.run(imageName, containerConfig, tag, name)

        when:
        def execConfig = ["Cmd": [
                'echo "hello exec!"'
        ]]
        def execCreateResult = dockerClient.createExec(containerStatus.container.content.Id, execConfig).content

        then:
        execCreateResult?.Id =~ "[0-9a-f]+"

        cleanup:
        dockerClient.stop(name)
        dockerClient.wait(name)
        dockerClient.rm(name)
    }

    def "exec start"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def name = "start-exec"
        def containerStatus = dockerClient.run(imageName, containerConfig, tag, name)
        def containerId = containerStatus.container.content.Id
        def execCreateConfig = [
                "AttachStdin" : false,
                "AttachStdout": true,
                "AttachStderr": true,
                "Tty"         : false,
                "Cmd"         : [
                        "ls", "-lisah", "/"
                ]]

        def execCreateResult = dockerClient.createExec(containerId, execCreateConfig).content
        def execId = execCreateResult.Id

        when:
        def execStartConfig = [
                "Detach": false,
                "Tty"   : false]
        def execStream = dockerClient.startExec(execId, execStartConfig)

        then:
        execStream != null

        cleanup:
        dockerClient.stop(name)
        dockerClient.wait(name)
        dockerClient.rm(name)
    }

    def "exec (interactive)"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def tag = "latest"
        def cmds = ["sh", "-c", "ping 127.0.0.1"]
        def containerConfig = ["Cmd": cmds]
        def name = "attach-exec"
        def containerStatus = dockerClient.run(imageName, containerConfig, tag, name)
        def containerId = containerStatus.container.content.Id

        def logFileName = "/log.txt"
        def execCreateConfig = [
                "AttachStdin" : true,
                "AttachStdout": true,
                "AttachStderr": true,
                "Tty"         : true,
                "Cmd"         : ["/bin/sh", "-c", "read line && echo \"->\$line<-\" > ${logFileName}"]
        ]

        def execCreateResult = dockerClient.createExec(containerId, execCreateConfig).content
        def execId = execCreateResult.Id

        def input = "exec ${UUID.randomUUID()}"
        def expectedOutput = "->$input<-"
        def outputStream = new ByteArrayOutputStream()

        def onSinkClosed = new CountDownLatch(1)
        def onSourceConsumed = new CountDownLatch(1)

        def attachConfig = new AttachConfig()
        attachConfig.streams.stdin = new ByteArrayInputStream("$input\n".bytes)
        attachConfig.streams.stdout = outputStream
        attachConfig.onFailure = { Exception e ->
            log.error("exec failed", e)
        }
        attachConfig.onResponse = {
            log.trace("onResponse")
        }
        attachConfig.onSinkClosed = { Response response ->
            log.trace("onSinkClosed")
            onSinkClosed.countDown()
        }
        attachConfig.onSourceConsumed = {
            log.trace("onFinish")
            onSourceConsumed.countDown()
        }

        when:
        def execStartConfig = [
                "Detach": false,
                "Tty"   : true]
        dockerClient.startExec(execId, execStartConfig, attachConfig)
        onSinkClosed.await(5, SECONDS)
        onSourceConsumed.await(5, SECONDS)

        then:
        def logContent = new String(dockerClient.extractFile(name, logFileName))
        logContent.trim() == expectedOutput.toString()

        cleanup:
        dockerClient.stop(name)
        dockerClient.wait(name)
        dockerClient.rm(name)
    }

    def "get archive (copy from container)"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def imageName = "copy_container"
        def containerConfig = ["Cmd"  : ["sh", "-c", "echo -n -e 'to be or\nnot to be' > /file1.txt"],
                               "Image": "copy_container"]
        dockerClient.tag(imageId, imageName)
        def containerInfo = dockerClient.run(imageName, containerConfig, [:])
        def containerId = containerInfo.container.content.Id

        when:
        def tarContent = dockerClient.getArchive(containerId, "/file1.txt").stream

        then:
        def fileContent = dockerClient.extractSingleTarEntry(tarContent as InputStream, "file1.txt")
        and:
        fileContent == "to be or\nnot to be".bytes

        cleanup:
        dockerClient.stop(containerId)
        dockerClient.wait(containerId)
        dockerClient.rm(containerId)
        dockerClient.rmi(imageName)
    }

    def "rename"() {
        given:
        dockerClient.rm("a_wonderful_new_name")
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def containerConfig = ["Cmd"  : ["true"],
                               "Image": imageId]
        def containerId = dockerClient.createContainer(containerConfig).content.Id

        when:
        def renameContainerResult = dockerClient.rename(containerId, "a_wonderful_new_name")

        then:
        renameContainerResult.status.code == 204

        cleanup:
        dockerClient.rm(containerId)
    }

    def "events (async)"() {
        given:
        def latch = new CountDownLatch(1)
        def callback = new DockerAsyncCallback() {
            def events = []

            @Override
            def onEvent(Object event) {
                println event
                events << new JsonSlurper().parseText(event as String)
                latch.countDown()
            }
        }
        dockerClient.events(callback)

        when:
        def response = dockerClient.createContainer([Cmd: "-"])
        latch.await(5, SECONDS)

        then:
        callback.events.size() == 1
        and:
        callback.events.first().status == "create"
        callback.events.first().id == response.content.Id

        cleanup:
        dockerClient.rm(response.content.Id)
    }

    def "events (poll)"() {
        // meh. boot2docker/docker-machine sometimes need a time update, e.g. via:
        // docker-machine ssh default 'sudo ntpclient -s -h pool.ntp.org'

        given:
        def dockerSystemTime = DateTime.parse(dockerClient.info().content.SystemTime as String)
        long dockerEpoch = dockerSystemTime.millis / 1000

        def localSystemTime = DateTime.now()
        long localEpoch = localSystemTime.millis / 1000

        long timeOffset = localEpoch - dockerEpoch

        def latch = new CountDownLatch(1)
        def callback = new DockerAsyncCallback() {
            def events = []

            @Override
            def onEvent(Object event) {
                println event
                events << new JsonSlurper().parseText(event as String)
                latch.countDown()
            }
        }

        def container1 = dockerClient.createContainer([Cmd: "-"]).content.Id
        def container2 = dockerClient.createContainer([Cmd: "-"]).content.Id

        Thread.sleep(1000)
        long epochBeforeRm = (DateTime.now().millis / 1000) + timeOffset
        dockerClient.rm(container1)

        when:
        dockerClient.events(callback, [since: epochBeforeRm])
        latch.await(5, SECONDS)

        then:
        callback.events.size() == 1
        and:
        callback.events.first().status == "destroy"
        callback.events.first().id == container1

        cleanup:
        dockerClient.rm(container2)
    }

    def "top"() {
        given:
        def imageName = "gesellix/docker-client-testimage"
        def containerConfig = ["Cmd": ["sh", "-c", "ping 127.0.0.1"]]
        def containerStatus = dockerClient.run(imageName, containerConfig, "latest", "top-example")
        def containerId = containerStatus.container.content.Id

        when:
        def top = dockerClient.top(containerId).content

        then:
        top.Titles == ["PID", "USER", "TIME", "COMMAND"]
        and:
        top.Processes.last()[0] =~ "\\d{4}"
        top.Processes.last()[1] == "root"
        top.Processes.last()[2] == "0:00"
        top.Processes.last()[3] == "ping 127.0.0.1"

        cleanup:
        dockerClient.stop(containerId)
        dockerClient.wait(containerId)
        dockerClient.rm(containerId)
    }

    def "stats"() {
        given:
        def latch = new CountDownLatch(1)
        def callback = new DockerAsyncCallback() {
            def stats = []

            @Override
            def onEvent(Object stat) {
                println stat
                stats << new JsonSlurper().parseText(stat as String)
                latch.countDown()
            }
        }
        def imageName = "gesellix/docker-client-testimage"
        def containerConfig = ["Cmd": ["sh", "-c", "ping 127.0.0.1"]]
        def containerStatus = dockerClient.run(imageName, containerConfig, "latest", "stats-example")
        def containerId = containerStatus.container.content.Id

        when:
        dockerClient.stats(containerId, callback)
        latch.await(5, SECONDS)

        then:
        callback.stats.size() == 1
        callback.stats.first().blkio_stats

        cleanup:
        dockerClient.stop(containerId)
        dockerClient.wait(containerId)
        dockerClient.rm(containerId)
    }

    def "logs"() {
        given:
        def latch = new CountDownLatch(1)
        def callback = new DockerAsyncCallback() {
            def lines = []

            @Override
            def onEvent(Object line) {
                println line
                lines << line
                latch.countDown()
            }
        }
        def imageName = "gesellix/docker-client-testimage"
        def containerConfig = ["Cmd": ["sh", "-c", "ping 127.0.0.1"]]
        def containerStatus = dockerClient.run(imageName, containerConfig, "latest", "logs-example")
        def containerId = containerStatus.container.content.Id

        when:
        dockerClient.logs(containerId, [tail: 1], callback)
        latch.await(5, SECONDS)

        then:
        callback.lines.size() == 1
        callback.lines.first().startsWith("64 bytes from 127.0.0.1")

        cleanup:
        dockerClient.stop(containerId)
        dockerClient.wait(containerId)
        dockerClient.rm(containerId)
    }

    def "attach (interactive)"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def containerConfig = [
                Tty      : true,
                OpenStdin: true,
                Cmd      : ["/bin/sh", "-c", "read line && echo \"->\$line\""]
        ]
        def containerId = dockerClient.run(imageId, containerConfig).container.content.Id

        def input = "attach ${UUID.randomUUID()}"
        def expectedOutput = "$input\r\n->$input\r\n"
        def outputStream = new ByteArrayOutputStream()

        def onSinkClosed = new CountDownLatch(1)
        def onSourceConsumed = new CountDownLatch(1)

        def attachConfig = new AttachConfig()
        attachConfig.streams.stdin = new ByteArrayInputStream("$input\n".bytes)
        attachConfig.streams.stdout = outputStream
        attachConfig.onSinkClosed = { Response response ->
            onSinkClosed.countDown()
        }
        attachConfig.onSourceConsumed = {
            onSourceConsumed.countDown()
        }

        when:
        dockerClient.attach(
                containerId,
                [stream: 1, stdin: 1, stdout: 1, stderr: 1],
                attachConfig)
        onSinkClosed.await(5, SECONDS)
        onSourceConsumed.await(5, SECONDS)

        then:
        outputStream.toByteArray() == expectedOutput.bytes

        cleanup:
        dockerClient.stop(containerId)
        dockerClient.wait(containerId)
        dockerClient.rm(containerId)
    }

    @Requires({ LocalDocker.isTcpSocket() })
    def "attach (websocket)"() {
        given:
        def imageId = dockerClient.pull("gesellix/docker-client-testimage", "latest")
        def containerConfig = [
                Tty      : true,
                OpenStdin: true,
                Cmd      : ["/bin/sh", "-c", "cat"]
        ]
        def containerId = dockerClient.run(imageId, containerConfig).container.content.Id

        def executor = Executors.newSingleThreadExecutor()
        def ourMessage = "hallo welt ${UUID.randomUUID()}!".toString()

        def openConnection = new CountDownLatch(1)
        def AtomicReference<WebSocket> webSocketReference = new AtomicReference<>()
        def receiveMessage = new CountDownLatch(1)
        def receivedMessages = []
        def listener = new DefaultWebSocketListener() {
            @Override
            void onOpen(WebSocket webSocket, Response response) {
                webSocketReference.set(webSocket)
                openConnection.countDown()
                executor.execute(new Runnable() {
                    @Override
                    void run() {
                        webSocket.sendMessage(RequestBody.create(WebSocket.TEXT, ourMessage))
                    }
                })
            }

            @Override
            void onMessage(ResponseBody message) throws IOException {
                receivedMessages << message.string()
                receiveMessage.countDown()
            }
        }

        when:
        WebSocketCall wsCall = dockerClient.attachWebsocket(
                containerId,
                [stream: 1, stdin: 1, stdout: 1, stderr: 1],
                listener)


        openConnection.await(500, MILLISECONDS)
        receiveMessage.await(500, MILLISECONDS)

        then:
        receivedMessages.contains ourMessage

        cleanup:
        webSocketReference.get().close(NORMAL_CLOSURE.code, "cleanup")
        dockerClient.stop(containerId)
        dockerClient.wait(containerId)
        dockerClient.rm(containerId)
    }

    def listTarEntries(InputStream tarContent) {
        def stream = new TarArchiveInputStream(new BufferedInputStream(tarContent))

        def entryNames = []
        TarArchiveEntry entry
        while (entry = stream.nextTarEntry) {
            def entryName = entry.name
            entryNames << entryName

            log.debug("entry name: ${entryName}")
//            log.debug("entry size: ${entry.size}")
        }
        IOUtils.closeQuietly(stream)
        return entryNames
    }
}
