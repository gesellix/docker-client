package de.gesellix.docker.client

import de.gesellix.docker.client.protocolhandler.RawInputStream
import org.apache.commons.io.IOUtils
import org.java_websocket.client.WebSocketClient
import spock.lang.Ignore
import spock.lang.Specification

class DockerClientImplExplorationTest extends Specification {

  DockerClient dockerClient

  def setup() {
    def defaultDockerHost = System.env.DOCKER_HOST?.replaceFirst("tcp://", "http://")
//    System.setProperty("docker.cert.path", "C:\\Users\\gesellix\\.boot2docker\\certs\\boot2docker-vm")
    System.setProperty("docker.cert.path", "/Users/gesellix/.boot2docker/certs/boot2docker-vm")
    dockerClient = new DockerClientImpl(dockerHost: defaultDockerHost ?: "https://192.168.59.103:2376")
  }

  @Ignore("only for explorative testing")
  def info() {
    when:
    def info = dockerClient.info()

    then:
    info == [
        Containers        : 0,
        Debug             : 1,
        Driver            : "aufs",
        DriverStatus      : [
            ["Root Dir", "/mnt/sda1/var/lib/docker/aufs"],
            ["Dirs", "75"]],
        ExecutionDriver   : "native-0.2",
        Images            : 75,
        IndexServerAddress: "https://index.docker.io/v1/",
        InitPath          : "/usr/local/bin/docker",
        InitSha1          : "",
        IPv4Forwarding    : 1,
        NEventsListener   : 0,
        NFd               : 10,
        NGoroutines       : 11,
        KernelVersion     : "3.16.4-tinycore64",
        MemoryLimit       : 1,
        OperatingSystem   : "Boot2Docker 1.3.0 (TCL 5.4); master : a083df4 - Thu Oct 16 17:05:03 UTC 2014",
        SwapLimit         : 1]
  }

  @Ignore("only for explorative testing")
  def version() {
    when:
    def version = dockerClient.version()

    then:
    version == [
        ApiVersion   : "1.15",
        Arch         : "amd64",
        GitCommit    : "c78088f",
        GoVersion    : "go1.3.3",
        KernelVersion: "3.16.4-tinycore64",
        Os           : "linux",
        Version      : "1.3.0"]
  }

  @Ignore("only for explorative testing")
  def "attach with container.config.tty=false"() {
    when:
    def attached = dockerClient.attach("test-d", [logs  : false,
                                                  stream: true,
                                                  stdin : false,
                                                  stdout: true,
                                                  stderr: false])

    then:
    attached.status.code == 200
    and:
    attached.stream instanceof RawInputStream
    and:
    attached.stream.multiplexStreams == true
    IOUtils.copy(attached.stream, System.out)
  }

  @Ignore("only for explorative testing")
  def "attach with container.config.tty=true"() {
    when:
    def attached = dockerClient.attach("test-it", [logs  : false,
                                                   stream: true,
                                                   stdin : false,
                                                   stdout: true,
                                                   stderr: false])

    then:
    attached.status.code == 200
    and:
    attached.stream instanceof RawInputStream
    and:
    attached.stream.multiplexStreams == false
    IOUtils.copy(attached.stream, System.out)
  }

  def "attach via websocket"() {
    when:
    WebSocketClient wsClient = dockerClient.attachWebsocket("test-it", [logs  : false,
                                                                        stream: true,
                                                                        stdin : false,
                                                                        stdout: true,
                                                                        stderr: true])

    def lines = [
        "123",
        "456",
        "close"
    ]

    for (String line : lines) {
      if (line.equals("close")) {
        wsClient.closeBlocking()
      } else if (line) {
        wsClient.send(line as String)
      }
    }

    println ". ok ."

//    boolean closed = false
//    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))
//    while (!closed) {
//      String line = reader.readLine()
//      if (line.equals("close")) {
//        wsClient.close()
//        closed = true
//      } else if (line) {
//        wsClient.send(line as String)
//      }
//    }

    then:
    wsClient
  }
}
