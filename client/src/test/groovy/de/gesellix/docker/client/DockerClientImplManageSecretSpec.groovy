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
        when:
        dockerClient.createSecret("a-secret", "secret-content".bytes)

        then:
        1 * httpClient.post([path              : "/secrets/create",
                             body              : [Name  : "a-secret",
                                                  Data  : "secret-content".bytes,
                                                  Labels: [:]],
                             requestContentType: "application/json"]) >> [status: [success: true]]
    }

    def "inspect a secret"() {
        when:
        dockerClient.inspectSecret("5qyxxlxqbq6s5004io33miih6")

        then:
        1 * httpClient.get([path: "/secrets/5qyxxlxqbq6s5004io33miih6"]) >> [status: [success: true]]
    }

    def "list all secrets"() {
        when:
        dockerClient.secrets()

        then:
        1 * httpClient.get([path: "/secrets"]) >> [status: [success: true]]
    }
}
