package sun.net.www.protocol.unix

import de.gesellix.docker.client.DockerConfig
import de.gesellix.docker.client.LowLevelDockerClient
import org.apache.commons.lang.SystemUtils
import spock.lang.IgnoreIf
import spock.lang.Specification

@IgnoreIf({ !SystemUtils.IS_OS_LINUX })
class HttpOverUnixSocketIntegrationTest extends Specification {

    File defaultDockerSocket = new File("/var/run/docker.sock")
    def runDummyDaemon = !defaultDockerSocket.exists()
    File socketFile = defaultDockerSocket
    LowLevelDockerClient dockerClient

    def setup() {
        if (true || !defaultDockerSocket.exists()) {
            runDummyDaemon = true
            socketFile = new File(new File(System.getProperty("java.io.tmpdir")), "unixsocket-dummy.sock")
            socketFile.deleteOnExit()
        }
        def unixSocket = "unix://${socketFile.getCanonicalPath()}".toString()
        dockerClient = new LowLevelDockerClient(config: new DockerConfig(dockerHost: unixSocket))
    }

    def "info via unix socket"() {
        given:
        def responseBody = '{"a-key":42,"another-key":4711}'
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
        def ping = dockerClient.get([path: "/_ping"])

        then:
        ping.content == ["a-key": 42, "another-key": 4711]

        cleanup:
        testserver?.stop()
    }
}
