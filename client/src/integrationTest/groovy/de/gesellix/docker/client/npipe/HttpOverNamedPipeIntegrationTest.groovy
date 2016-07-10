package de.gesellix.docker.client.npipe

import de.gesellix.docker.client.HttpClient
import de.gesellix.docker.client.OkDockerClient
import spock.lang.Ignore
import spock.lang.Specification

@Ignore
//@Requires({ SystemUtils.IS_OS_WINDOWS })
class HttpOverNamedPipeIntegrationTest extends Specification {

    def "info via named pipe"() {
        given:
        File namedPipeFile = File.createTempFile("named-pipe", null)
        namedPipeFile.deleteOnExit()
        def namedPipe = "npipe://${namedPipeFile.getCanonicalPath()}".toString()
        HttpClient httpClient = new OkDockerClient(namedPipe)

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

        namedPipeFile.withWriter { writer ->
            writer.append(expectedResponse.join("\n"))
        }
        println namedPipeFile.text

        when:
        def ping = httpClient.get([path: "/_ping"])

        then:
        ping.content == ["a-key": 42, "another-key": 4711]

        cleanup:
        try {
            namedPipeFile.delete()
        } catch (Exception ignore) {
        }
    }
}
