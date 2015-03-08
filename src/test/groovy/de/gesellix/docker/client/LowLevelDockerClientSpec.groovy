package de.gesellix.docker.client

import de.gesellix.docker.client.protocolhandler.RawHeaderAndPayload
import de.gesellix.docker.client.protocolhandler.RawInputStream
import org.apache.commons.io.IOUtils
import org.codehaus.groovy.runtime.MethodClosure
import spock.lang.Specification

import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

import static de.gesellix.docker.client.protocolhandler.StreamType.STDOUT

class LowLevelDockerClientSpec extends Specification {

  def "dockerBaseUrl should default to http://localhost:2375"() {
    def client = new LowLevelDockerClient()
    expect:
    client.dockerBaseUrl?.toString() == new URL("http://127.0.0.1:2375").toString()
  }

  def "dockerBaseUrl should support tcp protocol"() {
    def client = new LowLevelDockerClient(dockerHost: "tcp://127.0.0.1:2375")
    expect:
    client.dockerBaseUrl?.toString() == new URL("http://127.0.0.1:2375").toString()
  }

  def "dockerBaseUrl should support tls port"() {
    given:
    def certsPath = IOUtils.getResource("/certs").file
    def oldDockerCertPath = System.setProperty("docker.cert.path", certsPath)
    def client = new LowLevelDockerClient(dockerHost: "tcp://127.0.0.1:2376")
    expect:
    client.dockerBaseUrl?.toString() == new URL("https://127.0.0.1:2376").toString()
    cleanup:
    if (oldDockerCertPath) {
      System.setProperty("docker.cert.path", oldDockerCertPath)
    }
    else {
      System.clearProperty("docker.cert.path")
    }
  }

  def "dockerBaseUrl should support https protocol"() {
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    expect:
    client.dockerBaseUrl?.toString() == new URL("https://127.0.0.1:2376").toString()
  }

  def "getMimeType"() {
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    expect:
    client.getMimeType(contentType) == expectedMimeType
    where:
    contentType                         | expectedMimeType
    null                                | null
    "application/json"                  | "application/json"
    "text/plain"                        | "text/plain"
    "text/plain; charset=utf-8"         | "text/plain"
    "text/html"                         | "text/html"
    "application/vnd.docker.raw-stream" | "application/vnd.docker.raw-stream"
  }

  def "getCharset"() {
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    expect:
    client.getCharset(contentType) == expectedCharset
    where:
    contentType                         | expectedCharset
    "application/json"                  | "utf-8"
    "text/plain"                        | "utf-8"
    "text/plain; charset=ISO-8859-1"    | "ISO-8859-1"
    "text/html"                         | "utf-8"
    "application/vnd.docker.raw-stream" | "utf-8"
  }

  def "queryToString"() {
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    expect:
    client.queryToString(parameters) == query
    where:
    parameters                 | query
    null                       | ""
    [:]                        | ""
    [param1: "value-1"]        | "param1=value-1"
    ["p 1": "v 1"]             | "p+1=v+1"
    [param1: "v 1", p2: "v-2"] | "param1=v+1&p2=v-2"
  }

  def "generic request with bad config: #requestConfig"() {
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    when:
    client.request(requestConfig)
    then:
    def ex = thrown(RuntimeException)
    ex.message == "bad request config"
    where:
    requestConfig << [null, [], [:], ["foo": "bar"]]
  }

  def "#method request with bad config: #requestConfig"() {
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    when:
    new MethodClosure(client, method).call(requestConfig)
    then:
    def ex = thrown(RuntimeException)
    ex.message == "bad request config"
    where:
    requestConfig  | method
    null           | "get"
    null           | "post"
    []             | "get"
    []             | "post"
    [:]            | "get"
    [:]            | "post"
    ["foo": "bar"] | "get"
    ["foo": "bar"] | "post"
  }

  def "get request uses the GET method"() {
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    given:
    client.metaClass.request = { config ->
      config.method
    }
    when:
    def method = client.get("/foo")
    then:
    method == "GET"
  }

  def "post request uses the POST method"() {
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    given:
    client.metaClass.request = { config ->
      config.method
    }
    when:
    def method = client.post("/foo")
    then:
    method == "POST"
  }

  def "openConnection with path"() {
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    when:
    def connection = client.openConnection([method: "GET",
                                            path  : "/foo"])
    then:
    connection.URL == new URL("https://127.0.0.1:2376/foo")
  }

  def "openConnection with path and query"() {
    def client = new LowLevelDockerClient(dockerHost: "http://127.0.0.1:2375")
    when:
    def connection = client.openConnection([method: "GET",
                                            path  : "/bar",
                                            query : [baz: "la/la", answer: 42]])
    then:
    connection.URL == new URL("http://127.0.0.1:2375/bar?baz=la%2Fla&answer=42")
  }

  def "configureConnection with plain http connection"() {
    def client = new LowLevelDockerClient(dockerHost: "http://127.0.0.1:2375")
    def connectionMock = Mock(HttpURLConnection)
    when:
    client.configureConnection(connectionMock, [method: "HEADER"])
    then:
    1 * connectionMock.setRequestMethod("HEADER")
  }

  def "configureConnection with https connection"() {
    given:
    def certsPath = IOUtils.getResource("/certs").file
    def oldDockerCertPath = System.setProperty("docker.cert.path", certsPath)
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    def connectionMock = Mock(HttpsURLConnection)

    when:
    client.configureConnection(connectionMock, [method: "HEADER"])

    then:
    1 * connectionMock.setRequestMethod("HEADER")
    and:
    1 * connectionMock.setSSLSocketFactory(_ as SSLSocketFactory)

    cleanup:
    if (oldDockerCertPath) {
      System.setProperty("docker.cert.path", oldDockerCertPath)
    }
    else {
      System.clearProperty("docker.cert.path")
    }
  }

  def "request should return statusLine"() {
    given:
    def client = new LowLevelDockerClient(dockerHost: "http://127.0.0.1:2375")
    def connectionMock = Mock(HttpURLConnection)
    client.metaClass.openConnection = {
      connectionMock
    }
    def headerFields = [:]
    headerFields[null] = [statusLine]
    connectionMock.getHeaderFields() >> headerFields
    connectionMock.responseCode >> statusCode

    when:
    def response = client.request([method: "OPTIONS",
                                   path  : "/a-resource"])

    then:
    response.status == expectedStatusLine

    where:
    statusLine                | statusCode | expectedStatusLine
    "HTTP/1.1 200 OK"         | 200        | [text: ["HTTP/1.1 200 OK"], code: 200]
    "HTTP/1.1 204 No Content" | 204        | [text: ["HTTP/1.1 204 No Content"], code: 204]
  }

  def "request should return headers"() {
    given:
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    def connectionMock = Mock(HttpURLConnection)
    client.metaClass.openConnection = {
      connectionMock
    }
    def headerFields = [:]
    headerFields[null] = ["HTTP/1.1 200 OK"]
    headerFields["Content-Type"] = ["text/plain;encoding=utf-8"]
    headerFields["Content-Length"] = ["123456789".length()]
    connectionMock.getHeaderFields() >> headerFields
    connectionMock.responseCode >> 200
    connectionMock.inputStream >> new ByteArrayInputStream("123456789".bytes)

    when:
    def response = client.request([method: "HEADER",
                                   path  : "/a-resource"])

    then:
    response.headers == ['content-type'  : ["text/plain;encoding=utf-8"],
                         'content-length': [9]]
    and:
    response.contentType == "text/plain;encoding=utf-8"
    and:
    response.mimeType == "text/plain"
    and:
    response.contentLength == 9
  }

  def "request should return consumed content"() {
    given:
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    def connectionMock = Mock(HttpURLConnection)
    client.metaClass.openConnection = {
      connectionMock
    }
    def headerFields = [:]
    headerFields[null] = ["HTTP/1.1 200 OK"]
    headerFields["Content-Type"] = ["text/plain"]
    headerFields["Content-Length"] = ["holy ship".length()]
    connectionMock.getHeaderFields() >> headerFields
    connectionMock.responseCode >> 200
    connectionMock.inputStream >> new ByteArrayInputStream("holy ship".bytes)

    when:
    def response = client.request([method: "HEADER",
                                   path  : "/a-resource"])

    then:
    response.stream == null
    and:
    response.content == "holy ship"
  }

  def "request with stdout stream and known content length"() {
    given:
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    def connectionMock = Mock(HttpURLConnection)
    client.metaClass.openConnection = {
      connectionMock
    }
    def headerFields = [:]
    headerFields[null] = ["HTTP/1.1 200 OK"]
    headerFields["Content-Type"] = ["text/plain"]
    headerFields["Content-Length"] = ["holy ship".length()]
    connectionMock.getHeaderFields() >> headerFields
    connectionMock.responseCode >> 200
    def responseBody = new ByteArrayInputStream("holy ship".bytes)
    connectionMock.inputStream >> responseBody
    def stdout = new ByteArrayOutputStream()

    when:
    def response = client.request([method: "HEADER",
                                   path  : "/a-resource",
                                   stdout: stdout])

    then:
    stdout.toByteArray() == "holy ship".bytes
    and:
    responseBody.available() == 0
    and:
    response.stream == null
  }

  def "request with stdout stream and unknown content length"() {
    given:
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    def connectionMock = Mock(HttpURLConnection)
    client.metaClass.openConnection = {
      connectionMock
    }
    def headerFields = [:]
    headerFields[null] = ["HTTP/1.1 200 OK"]
    headerFields["Content-Type"] = ["text/html"]
    headerFields["Content-Length"] = [-1]
    connectionMock.getHeaderFields() >> headerFields
    connectionMock.responseCode >> 200
    def responseBody = new ByteArrayInputStream("holy ship".bytes)
    connectionMock.inputStream >> responseBody

    when:
    def response = client.request([method: "HEADER",
                                   path  : "/a-resource"])

    then:
    response.stream.available() == "holy ship".length()
    and:
    IOUtils.toByteArray(response.stream as InputStream) == "holy ship".bytes
  }

  def "request with unknown mime type"() {
    given:
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    def connectionMock = Mock(HttpURLConnection)
    client.metaClass.openConnection = {
      connectionMock
    }
    def headerFields = [:]
    headerFields[null] = ["HTTP/1.1 200 OK"]
    headerFields["Content-Type"] = ["unknown/mime-type"]
    headerFields["Content-Length"] = ["holy ship".length()]
    connectionMock.getHeaderFields() >> headerFields
    connectionMock.responseCode >> 200
    def responseBody = new ByteArrayInputStream("holy ship".bytes)
    connectionMock.inputStream >> responseBody

    when:
    def response = client.request([method: "HEADER",
                                   path  : "/a-resource"])

    then:
    response.stream.available() == "holy ship".length()
    and:
    IOUtils.toByteArray(response.stream as InputStream) == "holy ship".bytes
  }

  def "request with unknown mime type and stdout"() {
    given:
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    def connectionMock = Mock(HttpURLConnection)
    client.metaClass.openConnection = {
      connectionMock
    }
    def headerFields = [:]
    headerFields[null] = ["HTTP/1.1 200 OK"]
    headerFields["Content-Type"] = ["unknown/mime-type"]
    headerFields["Content-Length"] = ["holy ship".length()]
    connectionMock.getHeaderFields() >> headerFields
    connectionMock.responseCode >> 200
    def responseBody = new ByteArrayInputStream("holy ship".bytes)
    connectionMock.inputStream >> responseBody
    def stdout = new ByteArrayOutputStream()

    when:
    def response = client.request([method: "HEADER",
                                   path  : "/a-resource",
                                   stdout: stdout])

    then:
    stdout.toByteArray() == "holy ship".bytes
    and:
    responseBody.available() == 0
    and:
    response.stream == null
  }

  def "request with consumed body by ContentHandler"() {
    given:
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    def contentHandler = new TestContentHandler(result: "result")
    client.contentHandlerFactory = new TestContentHandlerFactory(contentHandler: contentHandler)
    def connectionMock = Mock(HttpURLConnection)
    client.metaClass.openConnection = {
      connectionMock
    }
    def headerFields = [:]
    headerFields[null] = ["HTTP/1.1 200 OK"]
    headerFields["Content-Type"] = ["text/plain"]
    headerFields["Content-Length"] = ["holy ship".length()]
    connectionMock.getHeaderFields() >> headerFields
    connectionMock.responseCode >> 200
    def responseBody = new ByteArrayInputStream("holy ship".bytes)
    connectionMock.inputStream >> responseBody

    when:
    def response = client.request([method: "HEADER",
                                   path  : "/a-resource"])

    then:
    response.stream == null
    and:
    response.content == "result"
  }

  def "request with json response"() {
    given:
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    def contentHandler = new TestContentHandler(result: "result")
    client.contentHandlerFactory = new TestContentHandlerFactory(contentHandler: contentHandler)
    def connectionMock = Mock(HttpURLConnection)
    client.metaClass.openConnection = {
      connectionMock
    }
    def headerFields = [:]
    headerFields[null] = ["HTTP/1.1 200 OK"]
    headerFields["Content-Type"] = ["application/json"]
    headerFields["Content-Length"] = ['{"holy":"ship"}'.length()]
    connectionMock.getHeaderFields() >> headerFields
    connectionMock.responseCode >> 200
    def responseBody = new ByteArrayInputStream('{"holy":"ship"}'.bytes)
    connectionMock.inputStream >> responseBody

    when:
    def response = client.request([method: "HEADER",
                                   path  : "/a-resource"])

    then:
    response.stream == null
    and:
    response.content == "result"
  }

  def "request with docker raw-stream response"() {
    given:
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    def actualText = "holy ship"
    def headerAndPayload = new RawHeaderAndPayload(STDOUT, actualText.bytes)
    def responseBody = new ByteArrayInputStream((byte[]) headerAndPayload.bytes)
    def responseStream = new RawInputStream(responseBody)
    def contentHandler = new TestContentHandler(result: responseStream)
    client.contentHandlerFactory = new TestContentHandlerFactory(contentHandler: contentHandler)
    def connectionMock = Mock(HttpURLConnection)
    client.metaClass.openConnection = {
      connectionMock
    }
    def headerFields = [:]
    headerFields[null] = ["HTTP/1.1 200 OK"]
    headerFields["Content-Type"] = ["application/vnd.docker.raw-stream"]
    headerFields["Content-Length"] = [headerAndPayload.bytes.size()]
    connectionMock.getHeaderFields() >> headerFields
    connectionMock.responseCode >> 200
    connectionMock.inputStream >> responseBody

    when:
    def response = client.request([method: "HEADER",
                                   path  : "/a-resource"])

    then:
    response.stream == responseStream
    and:
    response.content == null
  }

  def "request with docker raw-stream response on stdout"() {
    given:
    def client = new LowLevelDockerClient(dockerHost: "https://127.0.0.1:2376")
    def actualText = "holy ship"
    def headerAndPayload = new RawHeaderAndPayload(STDOUT, actualText.bytes)
    def responseBody = new ByteArrayInputStream((byte[]) headerAndPayload.bytes)
    def responseStream = new RawInputStream(responseBody)
    def contentHandler = new TestContentHandler(result: responseStream)
    client.contentHandlerFactory = new TestContentHandlerFactory(contentHandler: contentHandler)
    def connectionMock = Mock(HttpURLConnection)
    client.metaClass.openConnection = {
      connectionMock
    }
    def headerFields = [:]
    headerFields[null] = ["HTTP/1.1 200 OK"]
    headerFields["Content-Type"] = ["application/vnd.docker.raw-stream"]
    headerFields["Content-Length"] = [headerAndPayload.bytes.size()]
    connectionMock.getHeaderFields() >> headerFields
    connectionMock.responseCode >> 200
    connectionMock.inputStream >> responseBody
    def stdout = new ByteArrayOutputStream()

    when:
    def response = client.request([method: "HEADER",
                                   path  : "/a-resource",
                                   stdout: stdout])

    then:
    stdout.toByteArray() == actualText.bytes
    and:
    responseBody.available() == 0
    and:
    response.stream == null
    and:
    response.content == null
  }

  static class TestContentHandlerFactory implements ContentHandlerFactory {

    def contentHandler = null

    @Override
    ContentHandler createContentHandler(String mimetype) {
      return contentHandler
    }
  }

  static class TestContentHandler extends ContentHandler {

    def result = null

    @Override
    Object getContent(URLConnection urlc) throws IOException {
      return result
    }
  }
}
