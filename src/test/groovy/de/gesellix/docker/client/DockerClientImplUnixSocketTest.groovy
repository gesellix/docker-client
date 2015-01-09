package de.gesellix.docker.client

import org.apache.commons.lang.SystemUtils
import spock.lang.IgnoreIf
import spock.lang.Specification

@IgnoreIf({ !SystemUtils.IS_OS_LINUX })
class DockerClientImplUnixSocketTest extends Specification {

  File defaultDockerSocket = new File("/var/run/docker.sock")
  def runDummyDaemon = !defaultDockerSocket.exists()
  File socketFile = defaultDockerSocket
  DockerClient dockerClient

  def setup() {
    if (!defaultDockerSocket.exists()) {
      socketFile = new File(new File(System.getProperty("java.io.tmpdir")), "unixsocket-dummy.sock")
      socketFile.deleteOnExit()
    }
    def unixSocket = "unix://${socketFile.getCanonicalPath()}".toString()
    dockerClient = new DockerClientImpl(dockerHost: unixSocket)
  }

  def "info via unix socket"() {
    given:
    def responseBody = '{"Containers":2,"Images":42}'
    def expectedResponse = [
        "HTTP/1.1 200 OK",
        "Content-Type: application/json",
        "Job-Name: unix socket test",
        "Date: Thu, 08 Jan 2015 23:05:55 GMT",
        "Content-Length: ${responseBody.length()}",
        "",
        responseBody
    ]

    def testserver = null
    if (runDummyDaemon) {
      testserver = new UnixSocketTestServer(socketFile)
      testserver.with {
        constantResponse = expectedResponse.join("\n")
      }
      testserver.runInNewThread()
    }

    when:
    def info = dockerClient.info()

    then:
    info.Images >= 0
    info.Containers >= 0

    cleanup:
    testserver?.stop()
  }
}
