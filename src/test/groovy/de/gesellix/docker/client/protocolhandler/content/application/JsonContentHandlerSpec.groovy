package de.gesellix.docker.client.protocolhandler.content.application

import groovy.json.JsonSlurper
import spock.lang.Specification

class JsonContentHandlerSpec extends Specification {

  def connection = Mock(URLConnection)
  def jsonSlurper = Mock(JsonSlurper)
  def jsonContentHandler

  def setup() {
    jsonContentHandler = new json(jsonSlurper: jsonSlurper)
  }

  def "should convert json chunks to an array of json chunks"() {
    given:
    def inputStream = new ByteArrayInputStream("{'key':'a-value'}\n{'2nd':'chunk'}".bytes)
    connection.inputStream >> inputStream

    when:
    jsonContentHandler.getContent(connection)

    then:
    1 * jsonSlurper.parse("[{'key':'a-value'},{'2nd':'chunk'}]".bytes) >> ["key": "a-value", "2nd": "chunk"]
  }

  def "should delegate to the JsonSlurper"() {
    given:
    def inputStream = new ByteArrayInputStream("{'key':'a-value'}".bytes)
    connection.inputStream >> inputStream
    1 * jsonSlurper.parse(_) >> ["key": "a-value"]

    when:
    def content = jsonContentHandler.getContent(connection)

    then:
    content == ["key": "a-value"]
  }
}
