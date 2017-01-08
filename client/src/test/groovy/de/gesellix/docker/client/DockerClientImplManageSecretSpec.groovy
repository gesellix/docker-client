package de.gesellix.docker.client

import de.gesellix.docker.client.config.DockerEnv
import spock.lang.Specification

class DockerClientImplManageSecretSpec extends Specification {

    DockerClientImpl dockerClient = Spy(DockerClientImpl)
    HttpClient httpClient = Mock(HttpClient)

    def setup() {
        dockerClient.responseHandler = Spy(DockerResponseHandler)
        dockerClient.newDockerHttpClient = { DockerEnv dockerEnv, proxy -> httpClient }
    }

    def "create a secret"() {
        given:
        when:
        dockerClient.createSecret("a-secret", "secret-content".bytes)

        then:
        1 * httpClient.post([path              : "/secrets/create",
                             body              : [Name  : "a-secret",
                                                  Data  : "secret-content".bytes,
                                                  Labels: [:]],
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }
}
