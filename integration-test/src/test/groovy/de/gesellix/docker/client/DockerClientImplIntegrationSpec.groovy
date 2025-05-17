package de.gesellix.docker.client

import de.gesellix.docker.engine.DockerEnv
import de.gesellix.docker.remote.api.AuthConfig
import groovy.util.logging.Slf4j
import spock.lang.Requires
import spock.lang.Specification

import static de.gesellix.docker.client.TestConstants.CONSTANTS

@Slf4j
@Requires({ LocalDocker.available() })
class DockerClientImplIntegrationSpec extends Specification {

  static DockerClient dockerClient
  static boolean nativeWindows = LocalDocker.isNativeWindows()

  def setupSpec() {
    dockerClient = new DockerClientImpl(
//                new DockerEnv(
//                        dockerHost: "http://192.168.99.100:2376",
//                        certPath: "/Users/${System.getProperty('user.name')}/.docker/machine/machines/default",
//                        apiVersion: "v1.23")
    )
  }

  def "ping"() {
    expect:
    "OK" == dockerClient.ping().content
  }

  def "info"() {
    when:
    def info = dockerClient.info().content

    then:
    if (nativeWindows) {
      info.dockerRootDir == "C:\\\\ProgramData\\\\Docker"
    }
    else {
      info.dockerRootDir =~ "(/mnt/sda1)?/var/lib/docker"
    }

    def expectedDriverStatusProperties
    if (nativeWindows) {
      expectedDriverStatusProperties = ["Windows", ""]
      info.driverStatus.findAll {
        it.first() in expectedDriverStatusProperties
      }.size() == expectedDriverStatusProperties.size()
    }
    else {
//      expectedDriverStatusProperties = ["driver-type", "io.containerd.snapshotter.v1"]
//      expectedDriverStatusProperties = ["Backing Filesystem", "extfs"]
      expectedDriverStatusProperties = ["Backing Filesystem", "driver-type"]
      info.driverStatus.findAll {
        it.first() in expectedDriverStatusProperties
      }.size() == expectedDriverStatusProperties.size()
    }
    info.httpProxy in ["", "http.docker.internal:3128", "docker.for.mac.http.internal:3128", "gateway.docker.internal:3128"]
    info.httpsProxy in ["", "http.docker.internal:3128", "docker.for.mac.http.internal:3129", "gateway.docker.internal:3129"]
    info.ID =~ "\\w[\\w-]+"
    info.indexServerAddress == "https://index.docker.io/v1/"
    info.ipv4Forwarding
    info.labels == null || info.labels == [] || info.labels == ["com.docker.desktop.address=npipe://\\\\.\\pipe\\docker_cli"]
    info.loggingDriver == "json-file"
    info.memTotal > 0
    info.memoryLimit != nativeWindows
    info.noProxy == "" || info.noProxy == "hubproxy.docker.internal" || info.noProxy == "*.local, 169.254/16"

    def officialRegistry = info.registryConfig.indexConfigs['docker.io']
    officialRegistry.name == "docker.io"
    officialRegistry.official
    officialRegistry.secure

    // [::1/128, 127.0.0.0/8]
    info.registryConfig.insecureRegistryCIDRs.contains("127.0.0.0/8")
    info.systemTime =~ "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2,}.(\\d{3,}Z)?"
  }

  def "version"() {
    when:
    def version = dockerClient.version().content

    then:
    def missingKeys = CONSTANTS.versionDetails.findResults { key, matcher ->
      !version[key] ? (key) : null
    }
    missingKeys.empty
  }

  def "auth"() {
    given:
//        def authDetails = dockerClient.readAuthConfig(null, null)
    def authDetails = dockerClient.readDefaultAuthConfig()
    def authPlain = new AuthConfig(
        authDetails.username,
        authDetails.password,
        null,
        authDetails.serveraddress)

    when:
    def authResult = null
    if (authDetails.username == null) {
      log.warn("no username configured to auth")
    }
    else {
      authResult = dockerClient.auth(authPlain)
    }

    then:
    authDetails.username == null || authResult?.content?.status == "Login Succeeded"
//        authResult.content.identityToken == ""
//        authResult.content.status == "Login Succeeded"
  }

  def "allows configuration via setter"() {
    given:
    def exampleHost = "tcp://foo.bar:1234"
    assert new DockerEnv().dockerHost != exampleHost

    when:
    def client = new DockerClientImpl(env: new DockerEnv(dockerHost: exampleHost))

    then:
    client.httpClient.dockerClientConfig.env.dockerHost == exampleHost
    client.dockerClientConfig.env.dockerHost == exampleHost
    client.env.dockerHost == exampleHost
  }
}
