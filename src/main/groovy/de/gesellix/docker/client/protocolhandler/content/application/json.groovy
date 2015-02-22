package de.gesellix.docker.client.protocolhandler.content.application

import groovy.json.JsonSlurper
import org.apache.commons.io.IOUtils

class json extends ContentHandler {

  def jsonSlurper

  json() {
    jsonSlurper = new JsonSlurper()
  }

  @Override
  Object getContent(URLConnection connection) throws IOException {
    try {
      def stream = connection.getInputStream()
      def text = IOUtils.toString(stream)
      def jsonAsObject = jsonSlurper.parse("[${text.replaceAll("\\}[\n\r]*\\{", "},{")}]".bytes)
      return jsonAsObject
    }
    catch (IOException e) {
      throw e;
    }
  }
}
