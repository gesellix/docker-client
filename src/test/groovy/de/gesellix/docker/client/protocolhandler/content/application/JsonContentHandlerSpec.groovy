package de.gesellix.docker.client.protocolhandler.content.application

import groovy.json.JsonSlurper
import spock.lang.Specification

class JsonContentHandlerSpec extends Specification {

  def "should delegate to the JsonSlurper"() {
    given:
    def jsonSlurper = Mock(JsonSlurper)
    def connection = Mock(URLConnection)
    def inputStream = Mock(InputStream)
    def jsonContentHandler = new json(jsonSlurper: jsonSlurper)

    when:
    connection.inputStream >> inputStream
    def content = jsonContentHandler.getContent(connection)

    then:
    1 * jsonSlurper.parse(inputStream) >> ["key": "a-value"]
    and:
    content == ["key": "a-value"]
  }
}
