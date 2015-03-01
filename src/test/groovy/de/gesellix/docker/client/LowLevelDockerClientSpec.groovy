package de.gesellix.docker.client

import spock.lang.IgnoreIf
import spock.lang.Specification

@IgnoreIf({ !System.env.DOCKER_HOST })
class LowLevelDockerClientSpec extends Specification {

  def "dockerBaseUrl should default to http://localhost:2375"() {
    def client = new LowLevelDockerClient()
    expect:
    client.dockerBaseUrl?.toString() == new URL("http://127.0.0.1:2375").toString()
  }

  def "dockerBaseUrl should support tcp protocol"() {
    def client = new LowLevelDockerClient(dockerHost: "tcp://127.0.0.1:2375")
    expect:
    client.dockerBaseUrl?.toString() == new URL("http://127.0.0.1:2375").toString()
  }

  def "dockerBaseUrl should support tls port"() {
    def client = new LowLevelDockerClient(dockerHost: "tcp://127.0.0.1:2376")
    expect:
    client.dockerBaseUrl?.toString() == new URL("https://127.0.0.1:2376").toString()
  }

  def "dockerBaseUrl should support https protocol"() {
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    expect:
    client.dockerBaseUrl?.toString() == new URL("https://127.0.0.1:2376").toString()
  }

  def "should allow GET requests"() {
    def client = new LowLevelDockerClient(dockerHost: System.env.DOCKER_HOST)
    expect:
    client.get("/_ping").content == "OK"
  }

  def "should allow POST requests"() {
    given:
    def client = new LowLevelDockerClient(dockerHost: System.env.DOCKER_HOST)
    def request = [path : "/images/create",
                   query: [fromImage: "gesellix/docker-client-testimage",
                           tag      : "latest",
                           registry : ""]]

    when:
    def response = client.post(request)
    then:
    response.content.last() == [status: "Status: Image is up to date for gesellix/docker-client-testimage:latest"]
  }

  def "should optionally stream a response"() {
    def client = new LowLevelDockerClient(dockerHost: System.env.DOCKER_HOST)
    def outputStream = new ByteArrayOutputStream()
    when:
    client.get([path        : "/_ping",
                outputStream: outputStream])
    then:
    outputStream.toString() == "OK"
  }

  def "should parse application/json"() {
    def client = new LowLevelDockerClient(dockerHost: System.env.DOCKER_HOST)
    when:
    def response = client.get("/version")
    then:
    def firstChunk = response.content.first()
    firstChunk.ApiVersion == "1.17"
    firstChunk.Arch == "amd64"
    firstChunk.GitCommit == "a8a31ef"
    firstChunk.GoVersion == "go1.4.1"
    firstChunk.KernelVersion ==~ "3\\.\\d+\\.\\d+-\\w+"
    firstChunk.Os == "linux"
    firstChunk.Version == "1.5.0"
  }
}
