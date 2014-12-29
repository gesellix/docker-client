package de.gesellix.docker.client

import spock.lang.Ignore
import spock.lang.Specification

@Ignore("only for explorative testing")
class DockerClientImplExplorationTest extends Specification {

  DockerClient dockerClient

  def setup() {
    def defaultDockerHost = System.env.DOCKER_HOST?.replaceFirst("tcp://", "http://")
    System.setProperty("docker.cert.path", "C:\\Users\\gesellix\\.boot2docker\\certs\\boot2docker-vm")
    dockerClient = new DockerClientImpl(dockerHost: defaultDockerHost ?: "http://172.17.42.1:2375/")
  }

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
}
