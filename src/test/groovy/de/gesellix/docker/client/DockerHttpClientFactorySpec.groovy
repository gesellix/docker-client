package de.gesellix.docker.client

import de.gesellix.socketfactory.https.HttpsSocketFactory
import de.gesellix.socketfactory.unix.UnixSocketFactory
import org.apache.commons.io.IOUtils
import org.apache.http.conn.scheme.PlainSocketFactory
import org.apache.http.conn.ssl.SSLSocketFactory
import spock.lang.Specification

class DockerHttpClientFactorySpec extends Specification {

  def "should keep http:// uris"() {
    given:
    def factory = new DockerHttpClientFactory("http://foo")
    expect:
    factory.sanitizedUri == "http://foo"
  }

  def "should keep https:// uris"() {
    given:
    System.setProperty("docker.cert.path", "/path/to/certs")
    def factory = new DockerHttpClientFactory("https://bar")
    expect:
    factory.sanitizedUri == "https://bar"
  }

  def "should sanitize unix:// uris"() {
    given:
    def factory = new DockerHttpClientFactory("unix://path/to/socket")
    expect:
    factory.sanitizedUri == "unix://localhostpath/to/socket"
  }

  def "should create old http client with valid schemes"() {
    given:
    def factory = new DockerHttpClientFactory("http://foo")
    when:
    def client = factory.createOldHttpClient()
    then:
    client.connectionManager.schemeRegistry.schemeNames.sort() == ["http", "https"].sort()
    and:
    client.connectionManager.schemeRegistry.get("http").schemeSocketFactory instanceof PlainSocketFactory
    and:
    client.connectionManager.schemeRegistry.get("https").schemeSocketFactory instanceof SSLSocketFactory
  }

  def "should create old https client with valid schemes"() {
    given:
    def factory = new DockerHttpClientFactory("https://foo")
    when:
    def client = factory.createOldHttpClient()
    then:
    client.connectionManager.schemeRegistry.schemeNames.sort() == ["http", "https"].sort()
    and:
    client.connectionManager.schemeRegistry.get("http").schemeSocketFactory instanceof PlainSocketFactory
    and:
    client.connectionManager.schemeRegistry.get("https").schemeSocketFactory instanceof SSLSocketFactory
  }

  def "should create secure docker client with valid schemes"() {
    given:
    def certsPath = IOUtils.getResource("/certs").file
    System.setProperty("docker.cert.path", certsPath)
    def factory = new DockerHttpClientFactory("https://foo:2376")
    when:
    def client = factory.createOldHttpClient()
    then:
    client.connectionManager.schemeRegistry.schemeNames.sort() == ["http", "https"].sort()
    and:
    client.connectionManager.schemeRegistry.get("http").schemeSocketFactory instanceof PlainSocketFactory
    and:
    client.connectionManager.schemeRegistry.get("https").schemeSocketFactory instanceof HttpsSocketFactory
  }

  def "should create unix socket docker client with valid schemes"() {
    given:
    def socket = IOUtils.getResource("/unixsocket/docker.sock").file
    def factory = new DockerHttpClientFactory("unix://${socket}")
    when:
    def client = factory.createOldHttpClient()
    then:
    client.connectionManager.schemeRegistry.schemeNames.sort() == ["http", "https", "unix"].sort()
    and:
    client.connectionManager.schemeRegistry.get("http").schemeSocketFactory instanceof PlainSocketFactory
    and:
    client.connectionManager.schemeRegistry.get("https").schemeSocketFactory instanceof SSLSocketFactory
    and:
    client.connectionManager.schemeRegistry.get("unix").schemeSocketFactory instanceof UnixSocketFactory
  }
}
