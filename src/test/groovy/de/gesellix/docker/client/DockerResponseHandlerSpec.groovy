package de.gesellix.docker.client

import spock.lang.Specification
import spock.lang.Unroll

class DockerResponseHandlerSpec extends Specification {

  def DockerResponseHandler responseHandler = new DockerResponseHandler()

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
    response << [null, [], [:], [status: [:]], [status: [success: false]], [status: [content: [error: "anything"]]]]
  }

  @Unroll
  def "should accept response with #response"() {
    when:
    responseHandler.ensureSuccessfulResponse(response, new RuntimeException("should not be thrown"))
    then:
    notThrown(Exception)
    where:
    response << [
        [status: [success: true]],
        [status: [success: true], content: ["no-error": "anything"]]
    ]
  }
}
