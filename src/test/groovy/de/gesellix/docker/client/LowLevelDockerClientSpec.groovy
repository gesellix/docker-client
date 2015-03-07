package de.gesellix.docker.client

import org.apache.commons.io.IOUtils
import org.codehaus.groovy.runtime.MethodClosure
import spock.lang.Specification

import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

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
}
