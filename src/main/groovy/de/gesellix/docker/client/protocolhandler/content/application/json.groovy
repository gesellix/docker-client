package de.gesellix.docker.client.protocolhandler.content.application

import groovy.json.JsonSlurper

class json extends ContentHandler {

  def jsonSlurper

  json() {
    jsonSlurper = new JsonSlurper()
  }

  @Override
  Object getContent(URLConnection connection) throws IOException {
    try {
      def jsonAsObject = jsonSlurper.parse(connection.getInputStream())
      return jsonAsObject
    }
    catch (IOException e) {
      throw e;
    }
  }
}
