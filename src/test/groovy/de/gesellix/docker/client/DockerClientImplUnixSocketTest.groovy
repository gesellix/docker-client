package de.gesellix.docker.client

import spock.lang.IgnoreIf
import spock.lang.Specification

@IgnoreIf({ !new File("/var/run/docker.sock").exists() })
class DockerClientImplUnixSocketTest extends Specification {

  File socketFile = new File("/var/run/docker.sock")
//  File socketFile = new File(new File(System.getProperty("java.io.tmpdir")), "unixsocket-test.sock")
  DockerClient dockerClient

  def setup() {
    socketFile.createNewFile() || println("$socketFile already exists")
    def unixSocket = "unix://${socketFile.getCanonicalPath()}".toString()
    dockerClient = new DockerClientImpl(dockerHost: unixSocket)
  }

  def "info via unix socket"() {
//    given:
//    UnixSocketTestServer.runOnce(socketFile, ["the wind": "caught it"].toString().bytes)
    when:
    def info = dockerClient.info()

    then:
    info == [
        Containers        : 0,
        Debug             : 1,
        Driver            : "aufs",
        DriverStatus      : [["Root Dir", "/var/lib/docker/aufs"],
                             ["Dirs", "252"]],
        ExecutionDriver   : "native-0.2",
        IPv4Forwarding    : 1,
        Images            : 252,
        IndexServerAddress: "https://index.docker.io/v1/",
        InitPath          : "/usr/bin/docker",
        InitSha1          : "",
        KernelVersion     : "3.13.0-36-generic",
        MemoryLimit       : 1,
        NEventsListener   : 0,
        NFd               : 11,
        NGoroutines       : 11,
        OperatingSystem   : "Ubuntu 14.04.1 LTS",
        SwapLimit         : 0]
  }
}
