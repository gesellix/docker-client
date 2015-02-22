package de.gesellix.docker.client.protocolhandler

import de.gesellix.docker.client.DockerClientImpl
import de.gesellix.socketfactory.https.KeyStoreUtil
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

import static de.gesellix.socketfactory.https.KeyStoreUtil.getKEY_STORE_PASSWORD
import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm

class LowLevelDockerClient {

  def Logger logger = LoggerFactory.getLogger(LowLevelDockerClient)

  ContentHandlerFactory contentHandlerFactory = new DockerContentHandlerFactory()

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
    logger.info("header: ${headers}")

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
      if (response.headers['content-type'].first() == "application/vnd.docker.raw-stream") {
        def contentType = response.headers['content-type'].first() as String
        InputStream rawStream = contentHandlerFactory.createContentHandler(contentType).getContent(connection)
        IOUtils.copy(rawStream, System.out)
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
//    response = get("/containers/test/json")
    response = post("/containers/test/attach?logs=1&stream=1&stdout=1&stderr=0&tty=false", null, true)
  }

  public static void main(String[] args) {
    def defaultDockerHost = System.env.DOCKER_HOST
    def client = new LowLevelDockerClient(dockerHost: defaultDockerHost ?: "http://172.17.42.1:4243/")
    client.test()
  }

// Groovy version of https://gist.github.com/m451/1dfd41460b45ea17eb71
// Proof-of-concept for https://docs.docker.com/reference/api/docker_remote_api_v1.17/#attach-to-a-container
  static class RawDockerStreamReader {

    private String dockerHost
    private sslContext

    public static void main(String[] args) throws IOException {
      def dockerHost = System.env.DOCKER_HOST.replace("tcp://", "http://")
//    def dockerHost = System.env.DOCKER_HOST.replace("tcp://", "https://")
      def dockerClient = new DockerClientImpl(dockerHost: dockerHost)
      dockerClient.ping()

      def testUrl = "${dockerHost}/containers/test/attach?logs=1&stream=1&stdout=1&stderr=0&tty=false"
      new RawDockerStreamReader(dockerHost).attach(testUrl)
    }

    RawDockerStreamReader(dockerHost) {
      this.dockerHost = dockerHost

      def dockerCertPath = System.getProperty("docker.cert.path", System.env.DOCKER_CERT_PATH)
      if (dockerCertPath) {
        def keyStore = KeyStoreUtil.createDockerKeyStore(new File(dockerCertPath).absolutePath)
        final KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(getDefaultAlgorithm());
        kmfactory.init(keyStore, KEY_STORE_PASSWORD as char[]);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(getDefaultAlgorithm());
        tmf.init(keyStore)
        sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmfactory.keyManagers, tmf.trustManagers, null)
      }
    }

    def attach(testUrl) {
      // Remember that we will only see output if there is output in the specified container stream (Stdout and/or Stderr)
      URL url = new URL(testUrl)
      HttpURLConnection con = (HttpURLConnection) url.openConnection()
//    HttpsURLConnection con = (HttpsURLConnection) url.openConnection()
//    con.setSSLSocketFactory(sslContext.socketFactory)
      con.setRequestMethod("POST")
      // since we listen to a stream we disable the timeout
      con.setConnectTimeout(0)
      // since we listen to a stream we disable the timeout
      con.setReadTimeout(0)
      con.setUseCaches(false)

      int status = con.getResponseCode()
      println "Returncode: $status"

      InputStream stream = con.getInputStream()
      def dataInput = new DataInputStream(stream)

      int max = 10;
      while (true) {
        def streamType = dataInput.readByte()
        dataInput.readByte()
        dataInput.readByte()
        dataInput.readByte()
        def frameSize = dataInput.readInt()
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
      stream.close()
    }
  }
}
