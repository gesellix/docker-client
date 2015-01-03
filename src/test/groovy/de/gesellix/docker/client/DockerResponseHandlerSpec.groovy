package de.gesellix.docker.client

import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator
import org.apache.http.*
import org.apache.http.message.BasicHeader
import org.apache.http.message.BasicStatusLine
import org.codehaus.groovy.runtime.MethodClosure
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse

class DockerResponseHandlerSpec extends Specification {

  def DockerResponseHandler responseHandler = Spy(DockerResponseHandler)

  HttpEntity entity = Mock(HttpEntity)
  def responseBase
  def responseDecorator

  def setup() {
    responseBase = Mock(HttpResponse)
    _ * responseBase.statusLine >> httpStatusWith(HttpServletResponse.SC_OK)
    _ * responseBase.getHeaders("Content-Type") >> contentTypeHeader(ContentType.ANY.toString())
    _ * responseBase.entity >> entity
    _ * entity.content >> new ByteArrayInputStream()
    responseDecorator = new HttpResponseDecorator(responseBase, null)
  }

  def "handleSuccess() delegates to handle() method"() {
    when:
    responseHandler.handleSuccess(responseDecorator)
    then:
    1 * responseHandler.handle(responseDecorator)
  }

  def "handleFailure() delegates to handle() method"() {
    when:
    responseHandler.handleFailure(responseDecorator)
    then:
    1 * responseHandler.handle(responseDecorator)
  }

  def "handle saves the response status"() {
    given:
    responseBase.statusLine = Mock(StatusLine)
    def responseStatusLine = httpStatusWith(HttpStatus.SC_BAD_REQUEST)
    when:
    responseHandler.handle(responseDecorator)
    then:
    2 * responseBase.statusLine >> responseStatusLine
    and:
    responseHandler.success == false
    and:
    responseHandler.statusLine == responseStatusLine
  }

  def "handle reads the response body"() {
    given:
    responseBase.statusLine = Mock(StatusLine)
    when:
    responseHandler.handle(responseDecorator)
    then:
    1 * responseHandler.readResponseBody(responseDecorator) >> "complete response"
    and:
    responseHandler.completeResponse == "complete response"
  }

  def "readResponseBody returns empty response when entity is empty"() {
    when:
    def result = responseHandler.readResponseBody(responseDecorator)
    then:
    1 * responseBase.entity >> null
    and:
    result == ""
  }

  def "readResponseBody uses contentType reader when entity is not empty"() {
    given:
    def contentTypeReaders = ["test/example": new MethodClosure(new TestReader("expected result"), "read")]
    when:
    def result = responseHandler.readResponseBody(responseDecorator)
    then:
    1 * responseHandler.getContentType(responseDecorator) >> "test/example"
    and:
    1 * responseHandler.contentTypeReaders() >> contentTypeReaders
    and:
    result == "expected result"
  }

  def "readResponseBody throws Exception when contentType reader cannot be determined"() {
    given:
    def contentTypeReaders = ["test/example": new MethodClosure(new TestReader("expected result"), "read")]
    when:
    responseHandler.readResponseBody(responseDecorator)
    then:
    1 * responseHandler.getContentType(responseDecorator) >> "test/unknown"
    and:
    1 * responseHandler.contentTypeReaders() >> contentTypeReaders
    and:
    def exc = thrown(IllegalStateException)
    exc.message == "no reader for 'test/unknown' found."
  }

  @Unroll
  def "getContentType should find Content-Type response headers"(contentType, expectedContentType) {
    when:
    def actualContentType = responseHandler.getContentType(responseDecorator)
    then:
    1 * responseBase.getHeaders("Content-Type") >> contentType
    and:
    actualContentType == expectedContentType

    where:
    contentType                                              | expectedContentType
    []                                                       | "*/*"
    [
        contentTypeHeader(ContentType.HTML.toString()),
        contentTypeHeader(ContentType.TEXT.toString())]      | "*/*"
    [contentTypeHeader(ContentType.JSON.toString())]         | "application/json"
    [contentTypeHeader(ContentType.HTML.toString())]         | "text/html"
    [contentTypeHeader(ContentType.TEXT.toString())]         | "text/plain"
    [contentTypeHeader("application/vnd.docker.raw-stream")] | "application/vnd.docker.raw-stream"
  }

  @Unroll
  def "contentTypeReader is configured for #contentType"(contentType, delegatedMethodName) {
    when:
    def contentTypeReaders = responseHandler.contentTypeReaders()
    then:
    contentTypeReaders[contentType]?.method == delegatedMethodName

    where:
    contentType                         | delegatedMethodName
    "*/*"                               | "readText"
    "text/html"                         | "readText"
    "text/plain"                        | "readText"
    "application/json"                  | "readJson"
    "application/vnd.docker.raw-stream" | "readRawDockerStream"
  }

  def "readRawDockerStream delegates to readText"() {
    when:
    responseHandler.readRawDockerStream(responseDecorator)
    then:
    1 * entity.content >> new ByteArrayInputStream()
    and:
    1 * responseHandler.readText(responseDecorator)
  }

  def "readText reads plain text chunks"() {
    when:
    def result = responseHandler.readText(responseDecorator)
    then:
    1 * entity.content >> new ByteArrayInputStream("some plain text".bytes)
    and:
    responseHandler.chunks == [[plain: "some plain text"]]
    and:
    result == "some plain text"
  }

  def "readJson reads single json chunk"() {
    when:
    def result = responseHandler.readJson(responseDecorator)
    then:
    1 * entity.content >> new ByteArrayInputStream("{\"key1\":\"value1\"}".bytes)
    and:
    responseHandler.chunks == [[key1: "value1"]]
    and:
    result == "[{\"key1\":\"value1\"}]"
  }

  def "readJson reads multiple json chunks"() {
    when:
    def result = responseHandler.readJson(responseDecorator)
    then:
    1 * entity.content >> new ByteArrayInputStream("{\"key\":\"value\"}\n{\"key2\":\"valueX\"}".bytes)
    and:
    responseHandler.chunks == [[key: "value"], [key2: "valueX"]]
    and:
    result == "[{\"key\":\"value\"},{\"key2\":\"valueX\"}]"
  }

  def httpStatusWith(int statusCode) {
    return new BasicStatusLine(HttpVersion.HTTP_1_1, statusCode, "-> ${statusCode}")
  }

  def contentTypeHeader(String value) {
    return new BasicHeader("Content-Type", value)
  }

  static class TestReader {

    private Object expectedResult

    TestReader(expectedResult) {
      this.expectedResult = expectedResult
    }

    def read(response) {
      return expectedResult
    }
  }
}
