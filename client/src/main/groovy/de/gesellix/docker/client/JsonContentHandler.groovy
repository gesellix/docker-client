package de.gesellix.docker.client

import de.gesellix.util.IOUtils
import groovy.json.JsonSlurper

import java.util.regex.Pattern

class JsonContentHandler {

    def jsonSlurper
    def async = false

    def chunkDelimiter = "\\}[\\n\\r]*\\{"
    Pattern multipleChunks = Pattern.compile(".*${chunkDelimiter}.*", Pattern.DOTALL)

    JsonContentHandler(boolean async) {
        this.jsonSlurper = new JsonSlurper()
        this.async = async
    }

    Object getContent(InputStream stream, boolean chunked) throws IOException {
        return readJsonObject(stream, chunked)
    }

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
