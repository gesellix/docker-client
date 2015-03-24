package de.gesellix.docker.client.protocolhandler

import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files

class DockerURLHandlerTest extends Specification {

  @Unroll
  def "should assume TLS when tlsVerify==#tlsVerify"() {
    def certsDir = Files.createTempDirectory("certs")
    def dockerUrlHandler = new DockerURLHandler(
        dockerHost: "https://foo:2376",
        dockerTlsVerify: tlsVerify,
        dockerCertPath: certsDir.toString())
    when:
    def assumeTls = dockerUrlHandler.shouldUseTls(new URL("https://example.com:2376"))
    then:
    assumeTls
    where:
    tlsVerify << ["1", "true", "yes"]
  }

  @Unroll
  def "should not assume TLS when tlsVerify==#tlsVerify"() {
    def certsDir = Files.createTempDirectory("certs")
    def dockerUrlHandler = new DockerURLHandler(
        dockerHost: "https://foo:2376",
        dockerTlsVerify: tlsVerify,
        dockerCertPath: certsDir.toString())
    when:
    def assumeTls = dockerUrlHandler.shouldUseTls(new URL("https://example.com:2376"))
    then:
    !assumeTls
    where:
    tlsVerify << ["0", "false", "no"]
  }

  def "should not assume TLS when port !== 2376"() {
    def dockerUrlHandler = new DockerURLHandler(
        dockerHost: "https://foo:2375",
        dockerTlsVerify: null,
        dockerCertPath: "/some/non-existing/path")
    when:
    def assumeTls = dockerUrlHandler.shouldUseTls(new URL("https://example.com:2375"))
    then:
    !assumeTls
  }
}
