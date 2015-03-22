package de.gesellix.docker.client.protocolhandler.content.application

import groovy.json.JsonSlurper
import groovy.json.internal.Exceptions
import groovy.json.internal.FastStringUtils
import org.apache.commons.io.IOUtils

import java.util.regex.Pattern

class json extends ContentHandler {

  def jsonSlurper
  def chunkDelimiter = "\\}[\\n\\r]*\\{"
  Pattern multipleChunks = Pattern.compile(".*${chunkDelimiter}.*", Pattern.DOTALL)

  json() {
    jsonSlurper = new JsonSlurper() {

      // due to https://github.com/groovy/groovy-core/pull/440
      // not being available in Groovy < 2.4.x, and the docker-client
      // library being used in the gradle-docker-plugin, and
      // Gradle <= 2.4 not allowing us to use Groovy 2.4.x,
      // we apply the actual fix here.
      // I love open source :-]

      public Object parse(String jsonString) {
        return parse(FastStringUtils.toCharArray(jsonString));
      }

      @Override
      public Object parse(byte[] bytes, String charset) {
        try {
          return parse(new String(bytes, charset));
        }
        catch (UnsupportedEncodingException e) {
          return Exceptions.handle(Object.class, e);
        }
      }
    }
  }

  @Override
  Object getContent(URLConnection connection) throws IOException {
    try {
      def stream = connection.getInputStream()

      def jsonAsObject
      if (connection.getHeaderField("transfer-encoding") == "chunked") {
        def text = IOUtils.toString(stream)
        if (text.matches(multipleChunks)) {
          jsonAsObject = jsonSlurper.parse("[${text.replaceAll(chunkDelimiter, "},{")}]".bytes)
        }
        else {
          jsonAsObject = jsonSlurper.parse(text.bytes)
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
