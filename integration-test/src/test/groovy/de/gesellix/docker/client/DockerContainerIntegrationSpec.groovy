package de.gesellix.docker.client

import de.gesellix.docker.client.container.ArchiveUtil
import de.gesellix.docker.engine.AttachConfig
import de.gesellix.docker.engine.EngineResponse
import de.gesellix.docker.websocket.DefaultWebSocketListener
import de.gesellix.util.IOUtils
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import okhttp3.Response
import okhttp3.WebSocket
import okio.ByteString
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import spock.lang.Requires
import spock.lang.Specification

import java.time.Instant
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

import static de.gesellix.docker.client.TestConstants.CONSTANTS
import static de.gesellix.docker.websocket.WebsocketStatusCode.NORMAL_CLOSURE
import static java.util.concurrent.TimeUnit.MILLISECONDS
import static java.util.concurrent.TimeUnit.SECONDS

@Slf4j
@Requires({ LocalDocker.available() })
class DockerContainerIntegrationSpec extends Specification {

  static DockerClient dockerClient
  boolean isNativeWindows = LocalDocker.isNativeWindows()

  def setupSpec() {
    dockerClient = new DockerClientImpl()
  }

  def ping() {
    when:
    def ping = dockerClient.ping()

    then:
    ping.status.code == 200
    ping.content == "OK"
  }

  // WCOW does not support exporting containers
  // See https://github.com/moby/moby/issues/33581
  @Requires({ !LocalDocker.isNativeWindows() })
  def "export from container"() {
    given:
    def archive = getClass().getResourceAsStream('importUrl/import-from-url.tar')
    def imageId = dockerClient.importStream(archive)
    String container = dockerClient.createContainer([Image: imageId, Cmd: ["-"]]).content.Id

    when:
    def response = dockerClient.export(container)

    then:
    listTarEntries(response.stream as InputStream).contains "something.txt"

    cleanup:
    dockerClient.rm(container)
    dockerClient.rmi(imageId)
    IOUtils.closeQuietly(response.stream)
  }

  def "list containers"() {
    given:
    def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)
    def imageName = "list_containers"
    dockerClient.tag(imageId, imageName)
    def containerConfig = ["Cmd"  : ["true"],
                           "Image": imageName]
    String containerId = dockerClient.createContainer(containerConfig).content.Id
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
    def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)
    def imageName = "inspect_container"
    def containerConfig = ["Cmd"       : isNativeWindows ? ["cmd", "exit"] : ["true"],
                           "Image"     : "inspect_container",
                           "HostConfig": ["PublishAllPorts": true]]
    dockerClient.tag(imageId, imageName)
    String containerId = dockerClient.createContainer(containerConfig).content.Id
    dockerClient.startContainer(containerId)

    when:
    def containerInspection = dockerClient.inspectContainer(containerId).content

    then:
    containerInspection.HostnamePath =~ isNativeWindows ? "" : "\\w*/var/lib/docker/containers/${containerId}/hostname".toString()
    and:
    containerInspection.Config.Cmd == containerConfig.Cmd
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

  def "inspect missing container"() {
    when:
    dockerClient.inspectContainer("random-${UUID.randomUUID()}").content

    then:
    def exception = thrown(DockerClientException)
    ((EngineResponse) exception.detail).status.code == 404
  }

  def "diff"() {
    given:
    def cmd = ["-c", "echo 'hallo' > /tmp/change.txt"]
    if (isNativeWindows) {
      cmd = ["/C", "echo The wind caught it. > /change.txt"]
    }
    def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)
    def containerConfig = [
        "Entrypoint": isNativeWindows ? "cmd.exe" : "/bin/sh",
        "Cmd"       : cmd,
        "Image"     : imageId]
    String containerId = dockerClient.run(imageId, containerConfig).container.content.Id
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    Thread.sleep(1000)

    when:
    def changes = dockerClient.diff(containerId).content

    then:
    def aChange = changes.find {
      it.Path.endsWith("/change.txt")
    }
    log.info("change: ${aChange}")
    aChange != null
    and:
    aChange?.Kind != null

    cleanup:
    dockerClient.rm(containerId)
  }

  def "create container"() {
    given:
    def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)
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
    def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)
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
                           "Image": "gesellix/testimage:unknown"]

    when:
    dockerClient.createContainer(containerConfig, [name: "example"])

    then:
    DockerClientException ex = thrown()
    ex.cause.message == 'docker images create failed'
    if (expectManifestNotFound(ex.detail.content)) {
      ex.detail.content.message == "manifest for gesellix/testimage:unknown not found"
    }
    else {
      ex.detail.content.last().error == "Tag unknown not found in repository docker.io/gesellix/testimage"
    }
  }

  def expectManifestNotFound(def content) {
    if (Map.isInstance(content)) {
      return true
    }
//        if ((LocalDocker.getDockerVersion().major >= 1 && LocalDocker.getDockerVersion().minor >= 13)
//                || LocalDocker.getDockerVersion().major >= 17) {
//            return true
//        }
    return false
  }

  def "start container"() {
    given:
    def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)
    def containerConfig = ["Cmd"  : isNativeWindows ? ["cmd", "exit"] : ["true"],
                           "Image": imageId]
    String containerId = dockerClient.createContainer(containerConfig).content.Id

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
    def cmds = isNativeWindows ? ["cmd", "ping 127.0.0.1"] : ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def name = "update-container"
    def containerStatus = dockerClient.run(CONSTANTS.imageRepo, containerConfig, CONSTANTS.imageTag, name)
    String containerId = containerStatus.container.content.Id

    when:
    def updateConfig = isNativeWindows
        ? ["RestartPolicy":
               ["Name": "unless-stopped"]]
        : ["Memory"    : 314572800,
           "MemorySwap": 514288000]
    def updateResult = dockerClient.updateContainer(containerId, updateConfig)

    then:
    updateResult.status.success

    cleanup:
    dockerClient.stop(name)
    dockerClient.wait(name)
    dockerClient.rm(name)
  }

  def "run container with existing base image"() {
    given:
    def cmds = isNativeWindows ? ["cmd", "ping 127.0.0.1"] : ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]

    when:
    def containerStatus = dockerClient.run(CONSTANTS.imageRepo, containerConfig, CONSTANTS.imageTag)
    String containerId = containerStatus.container.content.Id

    then:
    containerStatus.status.status.code == 204

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  def "run container with PortBindings"() {
    given:
    def containerConfig = [
        "ExposedPorts": ["8080/tcp": [:]],
        "HostConfig"  : ["PortBindings": [
            "8080/tcp": [
                ["HostIp"  : "0.0.0.0",
                 "HostPort": "8081"]]
        ]]]

    when:
    def containerStatus = dockerClient.run(CONSTANTS.imageRepo, containerConfig, CONSTANTS.imageTag)
    String containerId = containerStatus.container.content.Id

    then:
    containerStatus.status.status.code == 204
    and:
    dockerClient.inspectContainer(containerId).content.Config.ExposedPorts == ["8080/tcp": [:]]
    and:
    dockerClient.inspectContainer(containerId).content.HostConfig.PortBindings == [
        "8080/tcp": [
            ["HostIp"  : "0.0.0.0",
             "HostPort": "8081"]]
    ]

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  def "run container with name"() {
    given:
    def cmds = isNativeWindows ? ["cmd", "ping 127.0.0.1"] : ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def name = "example-name"

    when:
    def containerStatus = dockerClient.run(CONSTANTS.imageRepo, containerConfig, CONSTANTS.imageTag, name)
    String containerId = containerStatus.container.content.Id

    then:
    containerStatus.status.status.code == 204

    and:
    def containers = dockerClient.ps().content
    containers.findAll { it.Names == ["/example-name"] }?.size() == 1

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  def "restart container"() {
    given:
    def cmds = isNativeWindows ? ["cmd", "ping 127.0.0.1"] : ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def containerStatus = dockerClient.run(CONSTANTS.imageRepo, containerConfig, CONSTANTS.imageTag)
    String containerId = containerStatus.container.content.Id

    when:
    def result = dockerClient.restart(containerId)

    then:
    result.status.code == 204

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  def "stop container"() {
    given:
    def cmds = isNativeWindows ? ["cmd", "ping 127.0.0.1"] : ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def containerStatus = dockerClient.run(CONSTANTS.imageRepo, containerConfig, CONSTANTS.imageTag)
    String containerId = containerStatus.container.content.Id

    when:
    def result = dockerClient.stop(containerId)

    then:
    result.status.code == 204

    cleanup:
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  def "kill container"() {
    given:
    def cmds = isNativeWindows ? ["cmd", "ping 127.0.0.1"] : ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def containerStatus = dockerClient.run(CONSTANTS.imageRepo, containerConfig, CONSTANTS.imageTag)
    String containerId = containerStatus.container.content.Id

    when:
    def result = dockerClient.kill(containerId)

    then:
    result.status.code == 204

    cleanup:
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  def "wait container"() {
    given:
    def cmds = isNativeWindows ? ["cmd", "ping 127.0.0.1"] : ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def containerStatus = dockerClient.run(CONSTANTS.imageRepo, containerConfig, CONSTANTS.imageTag)
    String containerId = containerStatus.container.content.Id
    dockerClient.stop(containerId)

    when:
    def result = dockerClient.wait(containerId)

    then:
    result.status.code == 200
    and:
    // 3221225786 == 0xC000013A == STATUS_CONTROL_C_EXIT
    // About the 3221225786 status code: https://stackoverflow.com/a/25444766/372019
    // See "2.3.1 NTSTATUS Values": https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-erref/596a1078-e883-4972-9bbc-49e60bebca55
    result.content.StatusCode == isNativeWindows ? 3221225786 : 137

    cleanup:
    dockerClient.rm(containerId)
  }

  @Requires({ LocalDocker.isPausable() })
  def "pause container"() {
    given:
    def cmds = isNativeWindows ? ["cmd", "ping 127.0.0.1"] : ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def containerStatus = dockerClient.run(CONSTANTS.imageRepo, containerConfig, CONSTANTS.imageTag)
    String containerId = containerStatus.container.content.Id

    when:
    def result = dockerClient.pause(containerId)

    then:
    result.status.code == 204

    cleanup:
    dockerClient.unpause(containerId)
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  @Requires({ LocalDocker.isPausable() })
  def "unpause container"() {
    given:
    def cmds = isNativeWindows ? ["cmd", "ping 127.0.0.1"] : ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def containerStatus = dockerClient.run(CONSTANTS.imageRepo, containerConfig, CONSTANTS.imageTag)
    String containerId = containerStatus.container.content.Id
    dockerClient.pause(containerId)

    when:
    def result = dockerClient.unpause(containerId)

    then:
    result.status.code == 204

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  def "rm container"() {
    given:
    def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)
    def containerConfig = ["Cmd"  : ["true"],
                           "Image": imageId]
    String containerId = dockerClient.createContainer(containerConfig).content.Id

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

  def "commit container"() {
    given:
    def cmds = isNativeWindows ? ["cmd", "ping 127.0.0.1"] : ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def containerStatus = dockerClient.run(CONSTANTS.imageRepo, containerConfig, CONSTANTS.imageTag)
    String containerId = containerStatus.container.content.Id

    when:
    if (isNativeWindows) {
      // windows does not support commit of a running container
      // see https://github.com/moby/moby/pull/15568
      dockerClient.stop(containerId)
      dockerClient.wait(containerId)
    }
    def result = dockerClient.commit(containerId, [
        repo   : 'committed-repo',
        tag    : 'the-tag',
        comment: 'commit container test',
        author : 'Andrew Niccol <g@tta.ca>'
    ])

    then:
    result.status.code == 201

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
    dockerClient.rmi('committed-repo:the-tag')
  }

  def "exec create"() {
    given:
    def cmds = isNativeWindows ? ["cmd", "ping 127.0.0.1"] : ["sh", "-c", "ping 127.0.0.1"]
    def containerConfig = ["Cmd": cmds]
    def name = "create-exec"
    def containerStatus = dockerClient.run(CONSTANTS.imageRepo, containerConfig, CONSTANTS.imageTag, name)

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
    String name = "start-exec"
    def containerStatus = dockerClient.run(CONSTANTS.imageRepo, [:], CONSTANTS.imageTag, name)
    String containerId = containerStatus.container.content.Id
    def execCreateConfig = [
        "AttachStdin" : false,
        "AttachStdout": true,
        "AttachStderr": true,
        "Tty"         : false,
        "Cmd"         : [
            "ls", "-lisah", "/"
        ]]

    def execCreateResult = dockerClient.createExec(containerId, execCreateConfig).content
    String execId = execCreateResult.Id

    when:
    def execStartConfig = [
        "Detach": false,
        "Tty"   : false]
    def response = dockerClient.startExec(execId, execStartConfig)

    then:
    response.stream != null

    cleanup:
    dockerClient.stop(name)
    dockerClient.wait(name)
    dockerClient.rm(name)
    IOUtils.closeQuietly(response.stream)
  }

  def "exec (interactive)"() {
    given:
    String name = "attach-exec"
    def containerStatus = dockerClient.run(CONSTANTS.imageRepo, [:], CONSTANTS.imageTag, name)
    String containerId = containerStatus.container.content.Id
    log.info("container: ${JsonOutput.toJson(dockerClient.inspectContainer(containerId).content)}")

    String logFileName = "/tmp/log.txt"
    List<String> execCmd = isNativeWindows
        ? ["cmd", "/V:ON", "/C", "set /p line= & echo #!line!# > ${logFileName}".toString()]
        : ["/bin/sh", "-c", "read line && echo \"#\$line#\" > ${logFileName}".toString()]

    def execCreateConfig = [
        "AttachStdin" : true,
        "AttachStdout": true,
        "AttachStderr": true,
        "Tty"         : true,
        "Cmd"         : execCmd
    ]

    def execCreateResult = dockerClient.createExec(containerId, execCreateConfig).content
    String execId = execCreateResult.Id

    String input = "exec ${UUID.randomUUID()}"
    String expectedOutput = "#$input#"
    def outputStream = new ByteArrayOutputStream()

    def onSinkClosed = new CountDownLatch(1)
    def onSourceConsumed = new CountDownLatch(1)

    def attachConfig = new AttachConfig()
    attachConfig.streams.stdin = new ByteArrayInputStream("$input\n".bytes)
    attachConfig.streams.stdout = outputStream
    attachConfig.onFailure = { Exception e ->
      log.error("exec failed", e)
    }
    attachConfig.onResponse = { Response response ->
      log.trace("onResponse (${response})")
    }
    attachConfig.onSinkClosed = { Response response ->
      log.trace("onSinkClosed (${response})")
      onSinkClosed.countDown()
    }
    attachConfig.onSourceConsumed = {
      log.trace("onSourceConsumed")
      onSourceConsumed.countDown()
    }

    when:
    def execStartConfig = [
        "Detach": false,
        "Tty"   : true]
    dockerClient.startExec(execId, execStartConfig, attachConfig)
    onSinkClosed.await(5, SECONDS)
    onSourceConsumed.await(5, SECONDS)

    def isolation = dockerClient.inspectContainer(containerId).content.HostConfig?.Isolation
    isolation = isolation ?: LocalDocker.getDaemonIsolation()
    if (isolation == "hyperv") {
      // filesystem operations against a running Hyper-V container are not supported
      // see https://github.com/moby/moby/pull/31864
      dockerClient.stop(containerId)
      dockerClient.wait(containerId)
    }

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
    String imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)
    String imageName = "copy_container"
    dockerClient.tag(imageId, imageName)
    def containerInfo = dockerClient.run(imageName, [:])
    String containerId = containerInfo.container.content.Id

    when:
    def isolation = dockerClient.inspectContainer(containerId).content.HostConfig?.Isolation
    isolation = isolation ?: LocalDocker.getDaemonIsolation()
    if (isolation == "hyperv") {
      // filesystem operations against a running Hyper-V container are not supported
      // see https://github.com/moby/moby/pull/31864
      dockerClient.stop(containerId)
      dockerClient.wait(containerId)
    }
    def tarContent = dockerClient.getArchive(containerId, "/gattaca.txt").stream

    then:
    def fileContent = new ArchiveUtil().extractSingleTarEntry(tarContent as InputStream, "file.txt")
    and:
    new String(fileContent) =~ "The wind\r?\ncaught it.\r?\n"

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
    dockerClient.rmi(imageName)
  }

  def "rename"() {
    given:
    dockerClient.rm("a_wonderful_new_name")
    def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)
    def containerConfig = ["Cmd"  : ["true"],
                           "Image": imageId]
    String containerId = dockerClient.createContainer(containerConfig).content.Id

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
      onEvent(Object event) {
        log.info("[events (async)] $event")
        if (event instanceof String) {
          events << new JsonSlurper().parseText(event as String)
        }
        else {
          events << event
        }
        latch.countDown()
      }

      @Override
      onFinish() {
      }
    }
    def response = dockerClient.events(callback)

    when:
    String containerId = dockerClient.createContainer([Cmd: "-"], [name: "event-test-async"]).content.Id
    latch.await(5, SECONDS)

    then:
    callback.events.size() == 1
    and:
    callback.events.first().status == "create"
    callback.events.first().id == containerId

    cleanup:
    response.taskFuture.cancel(true)
    dockerClient.rm(containerId)
  }

  def "events (poll)"() {
    // meh. boot2docker/docker-machine sometimes need a time update, e.g. via:
    // docker-machine ssh default 'sudo ntpclient -s -h pool.ntp.org'

    given:
    def dockerSystemTime = ZonedDateTime.parse(dockerClient.info().content.SystemTime as String).toInstant()
    long dockerEpoch = (long) (dockerSystemTime.toEpochMilli() / 1000)

    def localSystemTime = Instant.now()
    long localEpoch = (long) (localSystemTime.toEpochMilli() / 1000)

    long timeOffset = localEpoch - dockerEpoch

    def latch = new CountDownLatch(1)
    def callback = new DockerAsyncCallback() {

      def events = []

      @Override
      onEvent(Object event) {
        log.info("[events (poll)] $event")
        if (event instanceof String) {
          events << new JsonSlurper().parseText(event as String)
        }
        else {
          events << event
        }
        if (events.last().status == "destroy") {
          latch.countDown()
        }
      }

      @Override
      onFinish() {
      }
    }

    String container1 = dockerClient.createContainer([Cmd: "-"], [name: "c1"]).content.Id
    log.debug "container1: ${container1}"
    String container2 = dockerClient.createContainer([Cmd: "-"], [name: "c2"]).content.Id
    log.debug "container2: ${container2}"

    Thread.sleep(1000)
    long epochBeforeRm = (long) ((Instant.now().toEpochMilli() / 1000) + timeOffset - 1000)
    dockerClient.rm(container1)

    when:
    def response = dockerClient.events(callback, [since: epochBeforeRm, filters: [container: [container1]]])
    latch.await(10, SECONDS)

    then:
    !callback.events.empty
    and:
    def destroyEvents = new ArrayList<>(callback.events).findAll { it.status == "destroy" }
    destroyEvents.find { it.id == container1 }

    cleanup:
    response.taskFuture.cancel(true)
    dockerClient.rm(container1)
    dockerClient.rm(container2)
  }

  // the api reference v1.41 says: "On Unix systems, this is done by running the ps command. This endpoint is not supported on Windows."
  def "top"() {
    given:
    def containerStatus = dockerClient.run(CONSTANTS.imageRepo, [:], CONSTANTS.imageTag, "top-example")
    String containerId = containerStatus.container.content.Id

    when:
    def top = dockerClient.top(containerId).content

    then:
    if (isNativeWindows) {
      top.Titles == ["Name", "PID", "CPU", "Private Working Set"]
    }
    else {
      def reducedTitleSet = LocalDocker.getDockerVersion().major >= 1 && LocalDocker.getDockerVersion().minor >= 13
      top.Titles == (reducedTitleSet ? ["PID", "USER", "TIME", "COMMAND"] : ["UID", "PID", "PPID", "C", "STIME", "TTY", "TIME", "CMD"])
    }
    and:
    0 < top.Processes.collect { it.join(" ") }.findAll { it.contains("main") }.size()

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
      onEvent(Object stat) {
        log.info("[stats] $stat")
        if (stat instanceof String) {
          stats << new JsonSlurper().parseText(stat as String)
        }
        else {
          stats << stat
        }
        latch.countDown()
      }

      @Override
      onFinish() {
      }
    }
    def containerConfig = ["Cmd": isNativeWindows ? ["cmd", "/C", "ping -t 127.0.0.1"] : ["sh", "-c", "ping 127.0.0.1"]]
    def containerStatus = dockerClient.run(CONSTANTS.imageRepo, containerConfig, CONSTANTS.imageTag, "stats-example")
    String containerId = containerStatus.container.content.Id

    when:
    def response = dockerClient.stats(containerId, callback)
    latch.await(5, SECONDS)

    then:
    callback.stats.size() == 1
    callback.stats.first().blkio_stats

    cleanup:
    response.taskFuture.cancel(true)
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
      onEvent(Object line) {
        log.info("[logs] $line")
        if (line) {
          lines << line
        }
      }

      @Override
      onFinish() {
      }
    }
    def containerStatus = dockerClient.run(CONSTANTS.imageRepo, [:], CONSTANTS.imageTag, "logs-example")
    String containerId = containerStatus.container.content.Id

    when:
    def response = dockerClient.logs(containerId, [tail: 1], callback)
    latch.await(5, SECONDS)

    then:
    !callback.lines.empty
    callback.lines.any { it.contains("Listening and serving HTTP") }

    cleanup:
    response.taskFuture.cancel(true)
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  def "attach (read only)"() {
    given:
    def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)
    def containerConfig = [Cmd: ["ping", "127.0.0.1"]]
    String containerId = dockerClient.run(imageId, containerConfig).container.content.Id

    when:
    dockerClient.attach(
        containerId,
        [logs: 1, stream: 1, stdin: 0, stdout: 1, stderr: 1],
        new AttachConfig())
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)

    then:
    // Something like `*PING 127.0.0.1 (127.0.0.1): 56 data bytes` should appear on StdOut.
    notThrown(Throwable)

    cleanup:
    dockerClient.rm(containerId)
  }

  def "attach (interactive)"() {
    given:
    def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)
    def containerConfig = [
        Tty       : true,
        OpenStdin : true,
        Entrypoint: ["/bin/sh", "-c", "read line && echo \"->\$line\""]
    ]
    String containerId = dockerClient.run(imageId, containerConfig).container.content.Id

    def content = "attach ${UUID.randomUUID()}"
    def expectedOutput = "$content\r\n->$content\r\n"

    def outputStream = new ByteArrayOutputStream() {

      @Override
      synchronized void write(byte[] b, int off, int len) {
        log.info("write ${off}/${len} to ${b.length} bytes")
        super.write(b, off, len)
      }
    }
    def inputStream = new ByteArrayInputStream("$content\n".bytes) {

      @Override
      synchronized int read(byte[] b, int off, int len) {
        log.info("read ${off}/${len} from ${b.length} bytes")
        return super.read(b, off, len)
      }
    }

    def onSinkClosed = new CountDownLatch(1)
    def onSourceConsumed = new CountDownLatch(1)

    def attachConfig = new AttachConfig()
    attachConfig.streams.stdin = inputStream
    attachConfig.streams.stdout = outputStream
    attachConfig.onSinkClosed = { Response response ->
      log.info("[attach (interactive)] sink closed \n${outputStream.toString()}")
      onSinkClosed.countDown()
    }
    attachConfig.onSourceConsumed = {
      if (outputStream.toByteArray() == expectedOutput.bytes) {
        log.info("[attach (interactive)] fully consumed \n${outputStream.toString()}")
        onSourceConsumed.countDown()
      }
      else {
        log.info("[attach (interactive)] partially consumed \n${outputStream.toString()}")
      }
    }

    when:
    dockerClient.attach(
        containerId,
        [stream: 1, stdin: 1, stdout: 1, stderr: 1],
        attachConfig)
    def sinkClosed = onSinkClosed.await(5, SECONDS)
    def sourceConsumed = onSourceConsumed.await(5, SECONDS)

    then:
    sinkClosed
    sourceConsumed
    outputStream.size() > 0
    outputStream.toByteArray() == expectedOutput.bytes

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  @Requires({ (LocalDocker.isTcpSocket() || LocalDocker.isUnixSocket()) && !['Mac OS X'].contains(System.properties['os.name']) })
  "attach (websocket)"() {
    given:
    def tcpClient = dockerClient
    String socatId
    if (LocalDocker.isUnixSocket()) {
      // use a socat "tcp proxy" to test the websocket communication
      dockerClient.pull("gesellix/socat", "os-linux")
      def socatInfo = dockerClient.run(
          "gesellix/socat",
          [
              Tty       : true,
              OpenStdin : true,
              HostConfig: [
                  AutoRemove     : true,
                  PublishAllPorts: true,
                  Binds          : [
                      "/var/run/docker.sock:/var/run/docker.sock"
                  ]
              ]
          ],
          "os-linux")
      socatId = socatInfo.container.content.Id
      def socatContainerDetails = dockerClient.inspectContainer(socatId).content
      def socatContainerPort = socatContainerDetails.NetworkSettings.Ports['2375/tcp']['HostPort'].first()
      tcpClient = new DockerClientImpl("tcp://localhost:${socatContainerPort}")
      assert tcpClient.ping().status.code == 200
    }

    def imageId = dockerClient.pull(CONSTANTS.imageRepo, CONSTANTS.imageTag)
    def containerConfig = [
        Tty       : true,
        OpenStdin : true,
        Cmd       : ["/bin/sh", "-c", "cat"],
        HostConfig: [
            AutoRemove: true
        ]
    ]
    String containerId = dockerClient.run(imageId, containerConfig).container.content.Id

    def executor = Executors.newSingleThreadExecutor()
    def ourMessage = "hallo welt ${UUID.randomUUID()}!".toString()

    def openConnection = new CountDownLatch(1)
    AtomicReference<WebSocket> webSocketReference = new AtomicReference<>()
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
            webSocket.send(ourMessage)
          }
        })
      }

      @Override
      void onMessage(WebSocket webSocket, String text) {
        receivedMessages << text
        receiveMessage.countDown()
      }

      @Override
      void onMessage(WebSocket webSocket, ByteString bytes) {
        receivedMessages << bytes.toString()
        receiveMessage.countDown()
      }
    }

    when:
    WebSocket wsCall = tcpClient.attachWebsocket(
        containerId,
        [stream: 1, stdin: 1, stdout: 1, stderr: 1],
        listener)

    openConnection.await(500, MILLISECONDS)
    receiveMessage.await(500, MILLISECONDS)

    then:
    !receivedMessages.empty
    receivedMessages.find { message ->
      message.contains ourMessage
    }

    cleanup:
    webSocketReference?.get()?.close(NORMAL_CLOSURE.code, "cleanup")
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)

    if (socatId) {
      dockerClient.stop(socatId)
      dockerClient.wait(socatId)
      dockerClient.rm(socatId)
    }
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
