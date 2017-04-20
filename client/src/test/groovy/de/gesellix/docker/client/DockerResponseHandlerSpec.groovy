package de.gesellix.docker.client

import spock.lang.Specification
import spock.lang.Unroll

class DockerResponseHandlerSpec extends Specification {

    DockerResponseHandler responseHandler = new DockerResponseHandler()

    @Unroll
    def "should not accept response with #response"() {
        given:
        def testException = new RuntimeException("test context")
        when:
        responseHandler.ensureSuccessfulResponse(response, testException)
        then:
        DockerClientException thrown = thrown()
        thrown.cause.message == testException.message
        where:
        response << [
                null,
                new DockerResponse(),
                new DockerResponse(status: new DockerResponseStatus()),
                new DockerResponse(status: new DockerResponseStatus(success: false)),
                new DockerResponse(status: new DockerResponseStatus(success: true), content: [error: "anything"]),
                new DockerResponse(status: new DockerResponseStatus(success: true), mimeType: "application/json", content: [error: "anything"]),
                new DockerResponse(status: new DockerResponseStatus(success: true), mimeType: "application/json", content: [[foo: "bar"], [error: "anything"]]),
                new DockerResponse(status: new DockerResponseStatus(success: false), mimeType: "text/plain", content: "any error")]
    }

    @Unroll
    def "should accept response with #response"() {
        when:
        responseHandler.ensureSuccessfulResponse(response, new RuntimeException("should not be thrown"))
        then:
        notThrown(Exception)
        where:
        response << [
                new DockerResponse(status: new DockerResponseStatus(success: true)),
                new DockerResponse(status: new DockerResponseStatus(success: true), content: ["no-error": "anything"])
        ]
    }
}
