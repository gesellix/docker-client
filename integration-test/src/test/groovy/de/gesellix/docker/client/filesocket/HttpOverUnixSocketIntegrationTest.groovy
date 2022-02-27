package de.gesellix.docker.client.filesocket

import de.gesellix.docker.engine.EngineClient
import de.gesellix.docker.engine.OkDockerClient
import de.gesellix.docker.testutil.UnixSocketTestServer
import de.gesellix.util.IOUtils
import okio.Okio
import org.apache.commons.lang3.SystemUtils
import spock.lang.Requires
import spock.lang.Specification

@Requires({ new File("/var/run/docker.sock").exists() || SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC })
class HttpOverUnixSocketIntegrationTest extends Specification {

  File defaultDockerSocket = new File("/var/run/docker.sock")
  def runDummyDaemon = !defaultDockerSocket.exists()
  File socketFile = defaultDockerSocket
  EngineClient httpClient

  def setup() {
    String unixSocket
    if (socketFile.exists() && socketFile.canRead()) {
      unixSocket = "unix://${socketFile}".toString()
    }
    else {
//        if (true || !defaultDockerSocket.exists()) {
      runDummyDaemon = true
      socketFile = new File(new File(System.getProperty("java.io.tmpdir")), "unixsocket-dummy.sock")
      socketFile.deleteOnExit()
      unixSocket = "unix://${socketFile.getCanonicalPath()}".toString()
    }
    httpClient = new OkDockerClient(unixSocket)
  }

  def "info via unix socket"() {
    given:
    def responseBody = 'OK'
    def expectedResponse = [
        "HTTP/1.1 200 OK",
        "Content-Type: text/plain",
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
    def ping = httpClient.get([path: "/_ping"])
    def content = ping.content ?: Okio.buffer(Okio.source(ping.stream)).readUtf8()

    then:
    content == "OK"

    cleanup:
    testserver?.stop()
    if (ping.stream) {
      IOUtils.closeQuietly(Okio.source(ping.stream))
    }
  }
}
