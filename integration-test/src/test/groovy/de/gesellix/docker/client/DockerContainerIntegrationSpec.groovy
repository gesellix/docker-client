package de.gesellix.docker.client

import de.gesellix.docker.client.container.ArchiveUtil
import de.gesellix.docker.client.testutil.TeeOutputStream
import de.gesellix.docker.engine.AttachConfig
import de.gesellix.docker.remote.api.ChangeType
import de.gesellix.docker.remote.api.ContainerCreateRequest
import de.gesellix.docker.remote.api.ContainerUpdateRequest
import de.gesellix.docker.remote.api.CreateImageInfo
import de.gesellix.docker.remote.api.ExecConfig
import de.gesellix.docker.remote.api.ExecStartConfig
import de.gesellix.docker.remote.api.HostConfig
import de.gesellix.docker.remote.api.PortBinding
import de.gesellix.docker.remote.api.RestartPolicy
import de.gesellix.docker.remote.api.SystemInfo
import de.gesellix.docker.remote.api.client.CreateImageInfoExtensionsKt
import de.gesellix.docker.remote.api.core.ClientException
import de.gesellix.docker.remote.api.core.Frame
import de.gesellix.docker.remote.api.core.StreamCallback
import de.gesellix.docker.websocket.DefaultWebSocketListener
import de.gesellix.util.IOUtils
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import okhttp3.Response
import okhttp3.WebSocket
import okio.ByteString
import okio.Okio
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import spock.lang.IgnoreIf
import spock.lang.Requires
import spock.lang.Specification

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Predicate

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
    expect:
    "OK" == dockerClient.ping().content
  }

  // WCOW does not support exporting containers
  // See https://github.com/moby/moby/issues/33581
  @Requires({ !LocalDocker.isNativeWindows() })
  def "export from container"() {
    given:
    def archive = getClass().getResourceAsStream('importUrl/import-from-url.tar')
    List<CreateImageInfo> infos = []
    dockerClient.importStream(new StreamCallback<CreateImageInfo>() {

      @Override
      void onNext(CreateImageInfo createImageInfo) {
        log.info(createImageInfo.toString())
        infos.add(createImageInfo)
      }
    }, null, archive)

    def imageId = CreateImageInfoExtensionsKt.getImageId(infos)
    String container = dockerClient.createContainer(new ContainerCreateRequest().tap {
      image = imageId;
      cmd = ["-"]
    }).content.id

    when:
    def response = dockerClient.export(container)
    def stream = response.content

    then:
    listTarEntries(stream).contains("something.txt")

    cleanup:
    dockerClient.rm(container)
    dockerClient.rmi(imageId)
    IOUtils.closeQuietly(Okio.source(stream))
  }

  def "list containers"() {
    given:
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)
    def imageName = "list_containers"
    dockerClient.tag(CONSTANTS.imageName, imageName)
    String containerId = dockerClient.createContainer(new ContainerCreateRequest().tap { image = imageName }).content.id
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
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)
    def imageName = "inspect_container"
    dockerClient.tag(CONSTANTS.imageName, imageName)
    def containerId = dockerClient.createContainer(
        new ContainerCreateRequest().tap { c ->
          c.image = imageName
          c.hostConfig = new HostConfig().tap { publishAllPorts = true }
        },
        "example").content.id
    dockerClient.startContainer(containerId)

    when:
    def containerInspection = dockerClient.inspectContainer(containerId).content

    then:
    containerInspection.hostnamePath =~ isNativeWindows ? "" : "\\w*/var/lib/docker/containers/${containerId}/hostname".toString()
    and:
    containerInspection.config.image == "inspect_container"
    and:
    containerInspection.id == containerId

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
    def exception = thrown(ClientException)
    exception.statusCode == 404
  }

  def "diff"() {
    given:
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)
    def containerConfig = new ContainerCreateRequest().tap { c ->
      c.image = CONSTANTS.imageName
      c.cmd = isNativeWindows
          ? ["/C", "echo The wind caught it. > /change.txt"]
          : ["-c", "echo 'hallo' > /tmp/change.txt"]
      c.entrypoint = isNativeWindows
          ? ["cmd.exe"]
          : ["/bin/sh"]
    }
    String containerId = dockerClient.run(containerConfig).content.id
    Thread.sleep(1000)
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)

    def awaitChange = { String container ->
      CountDownLatch latch = new CountDownLatch(1)
      Thread.start {
        while (true) {
          def changes = dockerClient.diff(container).content
          log.info("changes: ${changes}")
          def aChange = changes.find {
            it.path?.endsWith("/change.txt")
          }
          if (aChange != null && aChange.kind in ChangeType.values()) {
            latch.countDown()
            return
          } else {
            Thread.sleep(1000)
          }
        }
      }
      def success = latch.await(5, SECONDS)
      return success
    }

    when:
    def changesFound = awaitChange(containerId)

    then:
    changesFound

    cleanup:
    dockerClient.rm(containerId)
  }

  def "create container"() {
    given:
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)

    when:
    def containerInfo = dockerClient.createContainer(
        new ContainerCreateRequest().tap { c ->
          c.image = CONSTANTS.imageName
          c.labels = ["a nice label" : "with a nice value",
                      "another-label": "{'foo':'bar'}"]
        },
        "example").content

    then:
    containerInfo.id =~ "\\w+"

    cleanup:
    dockerClient.rm(containerInfo.id)
  }

  def "create container with name"() {
    given:
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)

    when:
    def containerInfo = dockerClient.createContainer(new ContainerCreateRequest().tap { image = CONSTANTS.imageName }, "example").content

    then:
    containerInfo.id =~ "\\w+"

    cleanup:
    dockerClient.rm("example")
  }

  def "create container with unknown base image"() {
    given:
    dockerClient.rm("example")

    when:
    dockerClient.createContainer(new ContainerCreateRequest().tap { image = "gesellix/testimage:unknown" }, "example")

    then:
    ClientException ex = thrown()
    ex.statusCode == 404
    ex.toString() =~ /.* (docker.io\/)?gesellix\/testimage:unknown:? not found.*/
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
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)
    String containerId = dockerClient.createContainer(new ContainerCreateRequest().tap { image = CONSTANTS.imageName }).content.id

    when:
    dockerClient.startContainer(containerId)

    then:
    notThrown(Exception)

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  def "update container"() {
    given:
    def name = "update-container"
    def containerConfig = new ContainerCreateRequest().tap { c -> c.image = CONSTANTS.imageName }
    def containerStatus = dockerClient.run(containerConfig, name)
    String containerId = containerStatus.content.id

    when:
    def updateConfig = isNativeWindows
        ? new ContainerUpdateRequest().tap { restartPolicy = new RestartPolicy(RestartPolicy.Name.UnlessStopped, null) }
        : new ContainerUpdateRequest().tap { memory = 314572800; memorySwap = 514288000 }

    def updateResult = dockerClient.updateContainer(containerId, updateConfig)

    then:
    def warnings = updateResult.content.warnings ?: []
    // https://docs.docker.com/engine/install/linux-postinstall/#your-kernel-does-not-support-cgroup-swap-limit-capabilities
    warnings.empty || warnings.first().contains("kernel does not support swap limit capabilities")

    cleanup:
    dockerClient.stop(name)
    dockerClient.wait(name)
    dockerClient.rm(name)
  }

  def "run container with existing base image"() {
    given:
    def containerConfig = new ContainerCreateRequest().tap { image = CONSTANTS.imageName }

    when:
    def containerStatus = dockerClient.run(containerConfig)
    String containerId = containerStatus.content.id

    then:
    notThrown(Exception)

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  def "run container with PortBindings"() {
    given:
    def containerConfig = new ContainerCreateRequest().tap { c ->
      c.image = CONSTANTS.imageName
      c.exposedPorts = ["8080/tcp": [:]]
      c.hostConfig = new HostConfig().tap { h -> h.portBindings = ["8080/tcp": [new PortBinding("0.0.0.0", "8081")]]
      }
    }

    when:
    def containerStatus = dockerClient.run(containerConfig)
    String containerId = containerStatus.content.id

    then:
    notThrown(Exception)
    and:
    dockerClient.inspectContainer(containerId).content.config.exposedPorts == ["8080/tcp": [:]]
    and:
    dockerClient.inspectContainer(containerId).content.hostConfig.portBindings == ["8080/tcp": [new PortBinding("0.0.0.0", "8081")]]

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  def "run container with name"() {
    given:
    def containerConfig = new ContainerCreateRequest().tap { c -> c.image = CONSTANTS.imageName }
    def name = "example-name"

    when:
    def containerStatus = dockerClient.run(containerConfig, name)
    String containerId = containerStatus.content.id

    then:
    notThrown(Exception)

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
    def containerConfig = new ContainerCreateRequest().tap { c -> c.image = CONSTANTS.imageName }
    def containerStatus = dockerClient.run(containerConfig)
    String containerId = containerStatus.content.id

    when:
    dockerClient.restart(containerId)

    then:
    notThrown(Exception)

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  def "stop container"() {
    given:
    def containerConfig = new ContainerCreateRequest().tap { c -> c.image = CONSTANTS.imageName }
    def containerStatus = dockerClient.run(containerConfig)
    String containerId = containerStatus.content.id

    when:
    dockerClient.stop(containerId)

    then:
    notThrown(Exception)

    cleanup:
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  def "kill container"() {
    given:
    def containerConfig = new ContainerCreateRequest().tap { c -> c.image = CONSTANTS.imageName }
    def containerStatus = dockerClient.run(containerConfig)
    String containerId = containerStatus.content.id

    when:
    dockerClient.kill(containerId)

    then:
    notThrown(Exception)

    cleanup:
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  def "wait container"() {
    given:
    def containerConfig = new ContainerCreateRequest().tap { c -> c.image = CONSTANTS.imageName }
    def containerStatus = dockerClient.run(containerConfig)
    String containerId = containerStatus.content.id
    dockerClient.stop(containerId)

    when:
    def result = dockerClient.wait(containerId)

    then:
    result.content.error == null
    and:
    if (isNativeWindows) {
      // 3221225786 == 0xC000013A == STATUS_CONTROL_C_EXIT
      // About the 3221225786 status code: https://stackoverflow.com/a/25444766/372019
      // See "2.3.1 NTSTATUS Values": https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-erref/596a1078-e883-4972-9bbc-49e60bebca55
      result.content.statusCode == 3221225786
    } else {
      result.content.statusCode >= 0
    }

    cleanup:
    dockerClient.rm(containerId)
  }

  @Requires({ LocalDocker.isPausable() })
  def "pause container"() {
    given:
    def containerConfig = new ContainerCreateRequest().tap { c -> c.image = CONSTANTS.imageName }
    def containerStatus = dockerClient.run(containerConfig)
    String containerId = containerStatus.content.id

    when:
    dockerClient.pause(containerId)

    then:
    notThrown(Exception)

    cleanup:
    dockerClient.unpause(containerId)
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  @Requires({ LocalDocker.isPausable() })
  def "unpause container"() {
    given:
    def containerConfig = new ContainerCreateRequest().tap { c -> c.image = CONSTANTS.imageName }
    def containerStatus = dockerClient.run(containerConfig)
    String containerId = containerStatus.content.id
    dockerClient.pause(containerId)

    when:
    dockerClient.unpause(containerId)

    then:
    notThrown(Exception)

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  def "rm container"() {
    given:
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)
    String containerId = dockerClient.createContainer(new ContainerCreateRequest().tap { image = CONSTANTS.imageName }).content.id

    when:
    dockerClient.rm(containerId)

    then:
    notThrown(Exception)
  }

  def "rm unknown container"() {
    when:
    dockerClient.rm("a_not_so_random_id")

    then:
    notThrown(Exception)
  }

  def "commit container"() {
    given:
    def containerConfig = new ContainerCreateRequest().tap { c -> c.image = CONSTANTS.imageName }
    def containerStatus = dockerClient.run(containerConfig)
    String containerId = containerStatus.content.id

    when:
    if (isNativeWindows) {
      // windows does not support commit of a running container
      // see https://github.com/moby/moby/pull/15568
      dockerClient.stop(containerId)
      dockerClient.wait(containerId)
    }
    def result = dockerClient.commit(containerId, [repo   : 'committed-repo',
                                                   tag    : 'the-tag',
                                                   comment: 'commit container test',
                                                   author : 'Andrew Niccol <g@tta.ca>'])

    then:
    result.content.id =~ /\w+/

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
    dockerClient.rmi('committed-repo:the-tag')
  }

  def "exec create"() {
    given:
    def name = "create-exec"
    def containerConfig = new ContainerCreateRequest().tap { c -> c.image = CONSTANTS.imageName }
    def containerStatus = dockerClient.run(containerConfig, name)
    String containerId = containerStatus.content.id

    when:
    def execConfig = new ExecConfig(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        ['echo "hello exec!"'],
        null,
        null,
        null)
    def execCreateResult = dockerClient.createExec(containerId, execConfig).content

    then:
    execCreateResult?.id =~ "[\\da-f]+"

    cleanup:
    dockerClient.stop(name)
    dockerClient.wait(name)
    dockerClient.rm(name)
  }

  def "exec start"() {
    given:
    String name = "start-exec"
    def containerConfig = new ContainerCreateRequest().tap { c -> c.image = CONSTANTS.imageName }
    def containerStatus = dockerClient.run(containerConfig, name)
    String containerId = containerStatus.content.id
    def execCreateConfig = new ExecConfig(
        false,
        true,
        true,
        null,
        null,
        false,
        null,
        ["ls", "-lisah", "/"],
        null,
        null,
        null)
    def execCreateResult = dockerClient.createExec(containerId, execCreateConfig).content
    String execId = execCreateResult.id

    List<Frame> frames = []
    def latch = new CountDownLatch(1)
    def callback = new StreamCallback<Frame>() {

      @Override
      void onNext(Frame element) {
        log.info(element?.toString())
        frames.add(element)
        latch.countDown()
      }

      @Override
      void onFailed(Exception e) {
        log.error("Exec start failed", e)
        latch.countDown()
      }

      @Override
      void onFinished() {
        latch.countDown()
      }
    }

    when:
    def execStartConfig = new ExecStartConfig(false, false, null)
    dockerClient.startExec(execId, execStartConfig, callback, Duration.of(5, ChronoUnit.SECONDS))
    latch.await(10, SECONDS)

    then:
    !frames?.empty

    cleanup:
    dockerClient.stop(name)
    dockerClient.wait(name)
    dockerClient.rm(name)
  }

  // TODO Currently not supported, partially broken/unreliable
  @IgnoreIf({ Boolean.parseBoolean(System.getenv("CI")) })
  def "exec (interactive)"() {
    given:
    String name = "attach-exec"
    def containerConfig = new ContainerCreateRequest().tap { c -> c.image = CONSTANTS.imageName }
    def containerStatus = dockerClient.run(containerConfig, name)
    String containerId = containerStatus.content.id
    log.info("container: ${JsonOutput.toJson(dockerClient.inspectContainer(containerId).content)}")

    String logFileName = isNativeWindows
        ? "log.txt"
        : "/tmp/log.txt"
    List<String> execCmd = isNativeWindows
        ? ["cmd", "/V:ON", "/C", "set /p line= & echo #!line!# > ${logFileName}".toString()]
        : ["/bin/sh", "-c", "read line && echo \"#\$line#\" > ${logFileName}".toString()]

    def execCreateConfig = new ExecConfig(
        true,
        true,
        true,
        null,
        null,
        true,
        null,
        execCmd,
        null,
        null,
        null)
    def execCreateResult = dockerClient.createExec(containerId, execCreateConfig).content
    String execId = execCreateResult.id

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
    def execStartConfig = new ExecStartConfig(false, true, null)
    dockerClient.startExec(execId, execStartConfig, attachConfig)
//    dockerClient.startExec(execId, execStartConfig, callback, Duration.of(1, ChronoUnit.MINUTES))
    onSinkClosed.await(5, SECONDS)
    onSourceConsumed.await(5, SECONDS)

    def containerIsolation = dockerClient.inspectContainer(containerId).content.hostConfig?.isolation
    def actualIsolation = containerIsolation ? SystemInfo.Isolation.values().find { it.value == containerIsolation.value } : LocalDocker.getDaemonIsolation()
    if (actualIsolation == SystemInfo.Isolation.Hyperv) {
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
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)
    String imageName = "copy_container"
    dockerClient.tag(CONSTANTS.imageName, imageName)
    def containerInfo = dockerClient.run(new ContainerCreateRequest().tap { image = imageName })
    String containerId = containerInfo.content.id

    when:
    def containerIsolation = dockerClient.inspectContainer(containerId).content.hostConfig?.isolation
    def actualIsolation = containerIsolation ? SystemInfo.Isolation.values().find { it.value == containerIsolation.value } : LocalDocker.getDaemonIsolation()
    if (actualIsolation == SystemInfo.Isolation.Hyperv) {
      // filesystem operations against a running Hyper-V container are not supported
      // see https://github.com/moby/moby/pull/31864
      dockerClient.stop(containerId)
      dockerClient.wait(containerId)
    }
    def tarContent = dockerClient.getArchive(containerId, "/gattaca.txt").content

    then:
    def output = new ByteArrayOutputStream()
    def bytesRead = new ArchiveUtil().copySingleTarEntry(tarContent, "file.txt", output)
    def fileContent = output.toByteArray()
    and:
    new String(fileContent) =~ "The wind\r?\ncaught it.\r?\n"

    cleanup:
    try {
      tarContent?.close()
    } catch (Exception ignored) {
    }
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
    dockerClient.rmi(imageName)
  }

  def "rename"() {
    given:
    dockerClient.rm("a_wonderful_new_name")
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)
    String containerId = dockerClient.createContainer(new ContainerCreateRequest().tap { image = CONSTANTS.imageName }).content.id

    when:
    dockerClient.rename(containerId, "a_wonderful_new_name")

    then:
    notThrown(Exception)

    cleanup:
    dockerClient.rm(containerId)
  }

  // the api reference v1.41 says: "On Unix systems, this is done by running the ps command. This endpoint is not supported on Windows."
  def "top"() {
    given:
    def containerConfig = new ContainerCreateRequest().tap { c -> c.image = CONSTANTS.imageName }
    def containerStatus = dockerClient.run(containerConfig, "top-example")
    String containerId = containerStatus.content.id

    when:
    def top = dockerClient.top(containerId).content

    then:
    if (isNativeWindows) {
      top.titles == ["Name", "PID", "CPU", "Private Working Set"]
    } else {
      def reducedTitleSet = LocalDocker.getDockerVersion().major >= 1 && LocalDocker.getDockerVersion().minor >= 13
      top.titles == (reducedTitleSet ? ["PID", "USER", "TIME", "COMMAND"] : ["UID", "PID", "PPID", "C", "STIME", "TTY", "TIME", "CMD"])
    }
    and:
    0 < top.processes.collect { it.join(" ") }.findAll { it.contains("main") }.size()

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  def "stats"() {
    given:
    def stats = []
    def latch = new CountDownLatch(1)
    def callback = new StreamCallback() {

      @Override
      void onNext(Object element) {
        log.info(element?.toString())
        stats.add(element)
        latch.countDown()
      }

      @Override
      void onFailed(Exception e) {
        log.error("Stats failed", e)
        latch.countDown()
      }

      @Override
      void onFinished() {
        latch.countDown()
      }
    }
    def containerConfig = new ContainerCreateRequest().tap { c -> c.image = CONSTANTS.imageName }
    def containerStatus = dockerClient.run(containerConfig, "stats-example")
    String containerId = containerStatus.content.id

    when:
    dockerClient.stats(containerId, true, callback, Duration.of(2, ChronoUnit.SECONDS))
    latch.await(5, SECONDS)

    then:
    !stats.empty
    stats.first().blkio_stats

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  def "logs"() {
    given:
    List<Frame> logs = []
    def latch = new CountDownLatch(1)
    def callback = new StreamCallback<Frame>() {

      @Override
      void onNext(Frame element) {
        log.info(element?.toString())
        logs.add(element)
        latch.countDown()
      }

      @Override
      void onFailed(Exception e) {
        log.error("Logs failed", e)
        latch.countDown()
      }

      @Override
      void onFinished() {
        latch.countDown()
      }
    }
    def containerConfig = new ContainerCreateRequest().tap { c -> c.image = CONSTANTS.imageName }
    def containerStatus = dockerClient.run(containerConfig, "logs-example")
    String containerId = containerStatus.content.id

    when:
    new Thread({
      dockerClient.logs(containerId, [tail: 1], callback, Duration.of(5, ChronoUnit.SECONDS))
    }).start()
    latch.await(6, SECONDS)

    then:
    !logs.empty
    logs.any {
      it.streamType == Frame.StreamType.STDOUT && it.payloadAsString.contains("Listening and serving HTTP")
    }

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  def "waitForLogEvent"() {
    given:
    def matcherMatched = false
    Predicate<Frame> matcher = new Predicate<Frame>() {

      @Override
      boolean test(Frame frame) {
        log.info(frame?.toString())
        def matches = frame.streamType == Frame.StreamType.STDOUT && frame.payloadAsString.contains("Listening and serving HTTP")
        matcherMatched = matcherMatched || matches
        return matches
      }
    }
    def containerConfig = new ContainerCreateRequest().tap { c -> c.image = CONSTANTS.imageName }
    def containerStatus = dockerClient.run(containerConfig, "logs-example")
    String containerId = containerStatus.content.id

    when:
    dockerClient.waitForLogEvent(containerId, [tail: 0], matcher, Duration.of(5, ChronoUnit.SECONDS))

    then:
    matcherMatched

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  def "attach (read only)"() {
    given:
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)
    String containerId = dockerClient.run(new ContainerCreateRequest().tap { image = CONSTANTS.imageName }).content.id
    List<Frame> frames = []
    def latch = new CountDownLatch(1)
    def callback = new StreamCallback<Frame>() {

      @Override
      void onNext(Frame element) {
        log.info(element?.toString())
        frames.add(element)
        latch.countDown()
      }

      @Override
      void onFailed(Exception e) {
        log.error("Attach failed", e)
        latch.countDown()
      }

      @Override
      void onFinished() {
        latch.countDown()
      }
    }

    when:
    new Thread({
      dockerClient.attach(
          containerId, null, true, true,
          false, true, true,
          callback, Duration.of(10, ChronoUnit.SECONDS))
    }).start()
    latch.await(15, SECONDS)

    then:
    notThrown(Throwable)
    Thread.sleep(500)
    def matchingFrames = frames.findAll {
      // we receive a RAW response because the connection is not upgraded - which is ok for non-interactive usage
//      it.streamType == Frame.StreamType.STDOUT
      it.streamType == Frame.StreamType.RAW
          && it.payloadAsString.contains("Listening and serving HTTP")
    }
    1 == matchingFrames?.size()

    cleanup:
    dockerClient.stop(containerId)
    dockerClient.wait(containerId)
    dockerClient.rm(containerId)
  }

  // TODO Currently not supported, partially broken/unreliable
  @IgnoreIf({ Boolean.parseBoolean(System.getenv("CI")) })
  def "attach (interactive)"() {
    given:
    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)
    def containerConfig = new ContainerCreateRequest().tap { c ->
      c.tty = true
      //c.tty = false
      c.openStdin = true
      c.image = CONSTANTS.imageName
      c.entrypoint = LocalDocker.isNativeWindows()
          ? ["cmd"]
          : ["/bin/sh"]
      c.cmd = LocalDocker.isNativeWindows()
          ? ["/V:ON", "/C", "set /p line= & echo #!line!#"]
          : ["-c", "read line && echo \"#\$line#\""]
    }
    String containerId = dockerClient.run(containerConfig).content.id

    String content = "attach ${UUID.randomUUID()}"
    String expectedOutput = containerConfig.tty ? "$content\r\n#$content#\r\n" : "#$content#\n"

    def stdout = new ByteArrayOutputStream(expectedOutput.length())
    def stdin = new PipedOutputStream()

    def onSinkClosed = new CountDownLatch(1)
    def onSinkWritten = new CountDownLatch(1)
    def onSourceConsumed = new CountDownLatch(1)

    def attachConfig = new AttachConfig(!containerConfig.tty)
    attachConfig.streams.stdin = new PipedInputStream(stdin)
    attachConfig.streams.stdout = new TeeOutputStream(stdout, System.out)
    attachConfig.onResponse = { Response response ->
      log.info("[attach (interactive)] got response")
    }
    attachConfig.onSinkClosed = { Response response ->
      log.info("[attach (interactive)] sink closed (complete: ${stdout.toString() == expectedOutput})\n${stdout.toString()}")
      onSinkClosed.countDown()
    }
    attachConfig.onSinkWritten = { Response response ->
      log.info("[attach (interactive)] sink written (complete: ${stdout.toString() == expectedOutput})\n${stdout.toString()}")
      onSinkWritten.countDown()
    }
    attachConfig.onSourceConsumed = {
      if (stdout.toString() == expectedOutput) {
        log.info("[attach (interactive)] consumed (complete: ${stdout.toString() == expectedOutput})\n${stdout.toString()}")
        onSourceConsumed.countDown()
      } else {
        log.info("[attach (interactive)] consumed (complete: ${stdout.toString() == expectedOutput})\n${stdout.toString()}")
      }
    }
    dockerClient.attach(containerId,
        [logs: 1, stream: 1, stdin: 1, stdout: 1, stderr: 1],
        attachConfig)

    when:
    stdin.write("$content\n".bytes)
    stdin.flush()
    stdin.close()
    boolean sourceConsumed = onSourceConsumed.await(5, SECONDS)
    boolean sinkWritten = onSinkWritten.await(5, SECONDS)
    boolean sinkClosed = onSinkClosed.await(5, SECONDS)

    then:
    sinkClosed
    sinkWritten
    sourceConsumed
    stdout.size() > 0
    stdout.toByteArray() == expectedOutput.bytes

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
      dockerClient.pull(null, null, "gesellix/socat", "os-linux")
      def socatContainerConfig = new ContainerCreateRequest().tap { c ->
        c.image = "gesellix/socat:os-linux"
        c.tty = true
        c.openStdin = true
        c.hostConfig = new HostConfig().tap { h ->
          h.autoRemove = true
          h.publishAllPorts = true
          h.binds = ["/var/run/docker.sock:/var/run/docker.sock"]
        }
      }
      def socatInfo = dockerClient.run(socatContainerConfig)
      socatId = socatInfo.content.id
      def socatContainerDetails = dockerClient.inspectContainer(socatId).content
      def socatContainerPort = socatContainerDetails.networkSettings.ports['2375/tcp'].hostPort.first()
      tcpClient = new DockerClientImpl("tcp://localhost:${socatContainerPort}")
      assert tcpClient.ping().content == "OK"
    }

    dockerClient.pull(null, null, CONSTANTS.imageRepo, CONSTANTS.imageTag)
    def containerConfig = new ContainerCreateRequest().tap { c ->
      c.image = CONSTANTS.imageName
      // TODO this one should be operating system agnostic?!
      c.entrypoint = ["/bin/sh"]
      c.cmd = ["-c", "cat"]
      c.tty = true
      c.openStdin = true
      c.hostConfig = new HostConfig().tap { autoRemove = true }
    }
    String containerId = dockerClient.run(containerConfig).content.id

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
    WebSocket wsCall = tcpClient.attachWebsocket(containerId,
        [stream: 1, stdin: 1, stdout: 1, stderr: 1],
        listener)

    openConnection.await(500, MILLISECONDS)
    receiveMessage.await(500, MILLISECONDS)

    then:
    !receivedMessages.empty
    receivedMessages.find { message -> message.contains ourMessage
    }

    cleanup:
    webSocketReference?.get()?.close(NORMAL_CLOSURE.code, "cleanup")
    dockerClient.stop(containerId)

    if (socatId) {
      dockerClient.stop(socatId)
    }
  }

  def listTarEntries(InputStream tarContent) {
    def stream = new TarArchiveInputStream(new BufferedInputStream(tarContent))

    def entryNames = []
    TarArchiveEntry entry
    while (entry = stream.nextEntry) {
      def entryName = entry.name
      entryNames << entryName

      log.debug("entry name: ${entryName}")
//            log.debug("entry size: ${entry.size}")
    }
    IOUtils.closeQuietly(Okio.source(stream))
    return entryNames
  }
}
