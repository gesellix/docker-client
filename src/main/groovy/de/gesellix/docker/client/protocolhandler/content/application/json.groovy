package de.gesellix.docker.client.protocolhandler.content.application

import groovy.json.JsonSlurper
import org.apache.commons.io.IOUtils

import java.util.regex.Pattern

class json extends ContentHandler {

    def jsonSlurper
    def async = false

    def chunkDelimiter = "\\}[\\n\\r]*\\{"
    Pattern multipleChunks = Pattern.compile(".*${chunkDelimiter}.*", Pattern.DOTALL)

    json() {
        this(false)
    }

    json(boolean async) {
        this.jsonSlurper = new JsonSlurper()
        this.async = async
    }

    Object getContent(InputStream stream, boolean chunked) throws IOException {
        return readJsonObject(stream, chunked)
    }

    @Override
    Object getContent(URLConnection connection) throws IOException {
        def stream = connection.getInputStream()
        def chunked = connection.getHeaderField("transfer-encoding") == "chunked"
        return readJsonObject(stream, chunked)
    }

    private Object readJsonObject(InputStream stream, boolean chunked) {
        if (async) {
            return stream
        }

        def jsonAsObject
        if (chunked) {
            def text = IOUtils.toString(stream)
            if (text.matches(multipleChunks)) {
                jsonAsObject = jsonSlurper.parseText("[${text.replaceAll(chunkDelimiter, "},{")}]")
            } else {
                jsonAsObject = jsonSlurper.parseText(text)
            }
        } else {
            jsonAsObject = jsonSlurper.parse(stream)
        }
        return jsonAsObject
    }
}
