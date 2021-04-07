package de.gesellix.docker.client

import de.gesellix.docker.engine.EngineResponse
import de.gesellix.docker.engine.EngineResponseStatus
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
        new EngineResponse(),
        new EngineResponse(status: new EngineResponseStatus()),
        new EngineResponse(status: new EngineResponseStatus(success: false)),
        new EngineResponse(status: new EngineResponseStatus(success: true), content: [error: "anything"]),
        new EngineResponse(status: new EngineResponseStatus(success: true), mimeType: "application/json", content: [error: "anything"]),
        new EngineResponse(status: new EngineResponseStatus(success: true), mimeType: "application/json", content: [[foo: "bar"], [error: "anything"]]),
        new EngineResponse(status: new EngineResponseStatus(success: false), mimeType: "text/plain", content: "any error")]
  }

  @Unroll
  def "should accept response with #response"() {
    when:
    responseHandler.ensureSuccessfulResponse(response, new RuntimeException("should not be thrown"))
    then:
    notThrown(Exception)
    where:
    response << [
        new EngineResponse(status: new EngineResponseStatus(success: true)),
        new EngineResponse(status: new EngineResponseStatus(success: true), content: ["no-error": "anything"])
    ]
  }
}
