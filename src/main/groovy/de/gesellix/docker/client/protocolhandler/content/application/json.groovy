package de.gesellix.docker.client.protocolhandler.content.application

import groovy.json.JsonSlurper
import org.apache.commons.io.IOUtils

import java.util.regex.Pattern

class json extends ContentHandler {

  def jsonSlurper
  def chunkDelimiter = "\\}[\\n\\r]*\\{"
  Pattern multipleChunks = Pattern.compile(".*${chunkDelimiter}.*", Pattern.DOTALL)

  json() {
    jsonSlurper = new JsonSlurper()
  }

  @Override
  Object getContent(URLConnection connection) throws IOException {
    try {
      def stream = connection.getInputStream()

      def jsonAsObject
      if (connection.getHeaderField("transfer-encoding") == "chunked") {
        def text = IOUtils.toString(stream)
        if (text.matches(multipleChunks)) {
          jsonAsObject = jsonSlurper.parseText("[${text.replaceAll(chunkDelimiter, "},{")}]")
        }
        else {
          jsonAsObject = jsonSlurper.parseText(text)
        }
      }
      else {
        jsonAsObject = jsonSlurper.parse(stream)
      }
      return jsonAsObject
    }
    catch (IOException e) {
      throw e
    }
  }
}
