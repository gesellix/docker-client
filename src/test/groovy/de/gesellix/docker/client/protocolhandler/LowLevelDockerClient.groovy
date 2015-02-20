package de.gesellix.docker.client.protocolhandler

import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LowLevelDockerClient {

  def Logger logger = LoggerFactory.getLogger(LowLevelDockerClient)

  def dockerHost = "http://127.0.0.1:2375/"
  URL dockerHostUrl

  LowLevelDockerClient() {
  }

  def getUrl() {
    if (!getDockerHostUrl()) {
      dockerHostUrl = new DockerURLHandler(dockerHost: getDockerHost()).getURL()
    }
    return dockerHostUrl
  }

  def get(path, stream = false) {
    def pingUrl = new URL("${getUrl()}${path}")
    logger.info("GET ${pingUrl}")

    def connection = pingUrl.openConnection()
//    logger.info("${pingUrl} -> ${connection.responseCode}")
//    logger.info("${pingUrl} -> ${connection.headerFields}")
//    logger.info("${pingUrl} -> ${connection.inputStream}")

/*
    def response = connection.inputStream
    String contentType = connection.getHeaderField("Content-Type")
    logger.info("Content-Type: ${contentType}")
    String charset
    for (String param : contentType.replace(" ", "").split(";")) {
      if (param.startsWith("charset=")) {
        charset = param.split("=", 2)[1]
        break
      }
    }
    logger.info("charset: ${charset}")
    if (charset != null) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(response, charset))
      for (String line; (line = reader.readLine()) != null;) {
        logger.info("${pingUrl} -> ${line}")
      }
    }
    else {
      // It's likely binary content, use InputStream/OutputStream.
      def outputStream = new ByteArrayOutputStream()
      IOUtils.copy(connection.inputStream, outputStream)
      logger.info("${pingUrl} -> ${new String(outputStream.toByteArray())}")
    }
*/

    def statusLine = connection.headerFields[null]
    def headers = connection.headerFields.findAll { key, value ->
      key != null
    }.collectEntries { key, value ->
      [key.toLowerCase(), value]
    }

    logger.info("status: ${statusLine}")

    def response = [
        statusLine: [
            text: statusLine,
            code: connection.responseCode
        ],
        headers   : headers,
        body      : stream ? connection.inputStream : connection.content
    ]

    logger.info("status: ${response.statusLine}")
    logger.info("content-length: ${response.headers['content-length']}")
    logger.info("content-type: ${response.headers['content-type']}")
    if (stream) {
      logger.info("body: ${IOUtils.toString(response.body as InputStream)}")
    }
    else {
      logger.info("body: ${response.body}")
    }

    return response
  }

  def post(path, request = null, stream = false) {
    def pingUrl = new URL("${getUrl()}${path}")
    logger.info("POST ${pingUrl}")

    def connection = pingUrl.openConnection()
//    connection.setDoOutput(true)
    connection.setRequestMethod("POST")

    def statusLine = connection.headerFields[null]
    def headers = connection.headerFields.findAll { key, value ->
      key != null
    }.collectEntries { key, value ->
      [key.toLowerCase(), value]
    }

    logger.info("status: ${statusLine}")

    def response = [
        statusLine: [
            text: statusLine,
            code: connection.responseCode
        ],
        headers   : headers,
        body      : stream ? connection.inputStream : connection.content
    ]

    logger.info("status: ${response.statusLine}")
    logger.info("content-length: ${response.headers['content-length']}")
    logger.info("content-type: ${response.headers['content-type']}")
    if (stream) {
      if (response.headers['content-type'] == ["application/vnd.docker.raw-stream"]) {
        def dataInput = new DataInputStream(response.body)

        int max = 100;
        while (true) {
          def header = new RawDockerHeader(dataInput)
          println "stream type: ${header.streamType}"
          def frameSize = header.frameSize
          println "frameSize: ${frameSize}"
          def count = 0
          while (frameSize > 0) {
            print((char) dataInput.readByte())
            count++
            frameSize--
          }

          max--
          if (count == -1 || max <= 0) {
            break
          }
        }

        dataInput.close()
      }
      else {
        logger.info("body: ${IOUtils.toString(response.body as InputStream)}")
      }
    }
    else {
      logger.info("body: ${response.body}")
    }

    return response
  }

  def test() {
    logger.info "docker ping"
    def response = get("/_ping")
//    response = get("/version")
//    response = get("/containers/json")
    response = get("/containers/test/json")
    response = post("/containers/test/attach?logs=1&stream=1&stdout=1&stderr=0&tty=false", null, true)
  }

  public static void main(String[] args) {
    def defaultDockerHost = System.env.DOCKER_HOST
    def client = new LowLevelDockerClient(dockerHost: defaultDockerHost ?: "http://172.17.42.1:4243/")
    client.test()
  }
}
