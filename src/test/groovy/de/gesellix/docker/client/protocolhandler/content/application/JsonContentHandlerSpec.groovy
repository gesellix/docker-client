package de.gesellix.docker.client.protocolhandler.content.application

import groovy.json.JsonSlurper
import org.apache.commons.io.IOUtils
import spock.lang.Specification

class JsonContentHandlerSpec extends Specification {

    def connection = Mock(URLConnection)
    def jsonSlurper = Mock(JsonSlurper)
    def jsonContentHandler

    def setup() {
        jsonContentHandler = new json(false)
        jsonContentHandler.jsonSlurper = jsonSlurper
    }

    def "should convert json chunks from Connection to an array of json chunks"() {
        given:
        def inputStream = new ByteArrayInputStream("{'key':'a-value'}\n{'2nd':'chunk'}".bytes)
        connection.inputStream >> inputStream
        connection.getHeaderField("transfer-encoding") >> "chunked"

        when:
        jsonContentHandler.getContent(connection)

        then:
        1 * jsonSlurper.parseText("[{'key':'a-value'},{'2nd':'chunk'}]") >> ["key": "a-value", "2nd": "chunk"]
    }

    def "should convert json chunks from InputStream to an array of json chunks"() {
        given:
        def inputStream = new ByteArrayInputStream("{'key':'a-value'}\n{'2nd':'chunk'}".bytes)
        def chunked = true

        when:
        jsonContentHandler.getContent(inputStream, chunked)

        then:
        1 * jsonSlurper.parseText("[{'key':'a-value'},{'2nd':'chunk'}]") >> ["key": "a-value", "2nd": "chunk"]
    }

    def "should return underlying InputStream when async is allowed"() {
        given:
        def inputStream = new ByteArrayInputStream("{'key':'a-value'}\n{'2nd':'chunk'}".bytes)
        connection.inputStream >> inputStream
        connection.getHeaderField("transfer-encoding") >> "chunked"
        jsonContentHandler.async = true

        when:
        def content = jsonContentHandler.getContent(connection)

        then:
        content == inputStream
    }

    def "should return InputStream when async is allowed"() {
        given:
        def inputStream = new ByteArrayInputStream("{'key':'a-value'}\n{'2nd':'chunk'}".bytes)
        def chunked = true
        jsonContentHandler.async = true

        when:
        def content = jsonContentHandler.getContent(inputStream, chunked)

        then:
        content instanceof InputStream
        and:
        IOUtils.toString(content as InputStream) == "{'key':'a-value'}\n{'2nd':'chunk'}"
    }

    def "should delegate the connection's InputStream to the JsonSlurper"() {
        given:
        def inputStream = new ByteArrayInputStream("{'key':'a-value'}".bytes)
        connection.inputStream >> inputStream
        1 * jsonSlurper.parse(inputStream) >> ["key": "a-value"]

        when:
        def content = jsonContentHandler.getContent(connection)

        then:
        content == ["key": "a-value"]
    }

    def "should delegate InputStream to the JsonSlurper"() {
        given:
        def inputStream = new ByteArrayInputStream("{'key':'a-value'}".bytes)
        def chunked = false
        1 * jsonSlurper.parse(_) >> ["key": "a-value"]

        when:
        def content = jsonContentHandler.getContent(inputStream, chunked)

        then:
        content == ["key": "a-value"]
    }
}
