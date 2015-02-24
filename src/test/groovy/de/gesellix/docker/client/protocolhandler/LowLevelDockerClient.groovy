package de.gesellix.docker.client.protocolhandler

import de.gesellix.socketfactory.https.KeyStoreUtil
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

import static de.gesellix.socketfactory.https.KeyStoreUtil.getKEY_STORE_PASSWORD
import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm

// Proof-of-concept for https://docs.docker.com/reference/api/docker_remote_api_v1.17/#attach-to-a-container
class LowLevelDockerClient {

  def Logger logger = LoggerFactory.getLogger(LowLevelDockerClient)

  ContentHandlerFactory contentHandlerFactory = new DockerContentHandlerFactory()

  def dockerHost = "http://127.0.0.1:2375/"
  URL dockerHostUrl

  def sslSocketFactory = null

  LowLevelDockerClient() {
  }

  def getDockerBaseUrl() {
    if (!getDockerHostUrl()) {
      dockerHostUrl = new DockerURLHandler(dockerHost: getDockerHost()).getURL()
    }
    return dockerHostUrl
  }

  def get(path) {
    return request("GET", path)
  }

  def post(path) {
    return request("POST", path)
  }

  def request(method, path) {
    def requestUrl = new URL("${getDockerBaseUrl()}${path}")
    logger.info("${method} ${requestUrl}")

    def connection = connect(method, requestUrl)
//    connection.setDoOutput(true)
    // since we listen to a stream we disable the timeout
//    connection.setConnectTimeout(0)
    // since we listen to a stream we disable the timeout
//    connection.setReadTimeout(0)
//    connection.setUseCaches(false)

    def statusLine = connection.headerFields[null]
    logger.debug("status: ${statusLine}")

    def headers = connection.headerFields.findAll { key, value ->
      key != null
    }.collectEntries { key, value ->
      [key.toLowerCase(), value]
    }
    String contentType = headers['content-type']?.first()
    logger.debug("header: ${headers}")

    logger.debug("content-length: ${headers['content-length']}")
    logger.debug("content-type: ${contentType}")

    def response = [
        statusLine: [
            text: statusLine,
            code: connection.responseCode
        ],
        headers   : headers,
        stream    : connection.inputStream
    ]

    def contentHandler = contentHandlerFactory.createContentHandler(contentType)
    if (contentHandler == null) {
      logger.warn("couldn't find a specific ContentHandler for '${contentType}'.")
      IOUtils.copy(response.stream, System.out)
      println()
    } else {
      switch (contentType) {
        case "application/vnd.docker.raw-stream":
          InputStream rawStream = contentHandler.getContent(connection) as RawInputStream
          IOUtils.copy(rawStream, System.out)
          println()
          break;
        case "application/json":
          def body = contentHandler.getContent(connection)
          println body
          break;
        case "text/html":
          def body = contentHandler.getContent(connection)
          println body
          break;
        case "text/plain":
          def body = contentHandler.getContent(connection)
          println body
          break;
        default:
          IOUtils.copy(response.stream, System.out)
          println()
          break
      }
    }

    return response
  }

  def connect(method, URL requestUrl) {
    def connection = requestUrl.openConnection()
    connection.setRequestMethod(method)

    def dockerCertPath = System.getProperty("docker.cert.path", System.env.DOCKER_CERT_PATH)
    if (connection instanceof HttpsURLConnection) {
      if (!sslSocketFactory) {
        def keyStore = KeyStoreUtil.createDockerKeyStore(new File(dockerCertPath).absolutePath)
        final KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(getDefaultAlgorithm());
        kmfactory.init(keyStore, KEY_STORE_PASSWORD as char[]);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(getDefaultAlgorithm());
        tmf.init(keyStore)
        def sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmfactory.keyManagers, tmf.trustManagers, null)
        sslSocketFactory = sslContext.socketFactory
      }
      ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory)
    }

    return connection
  }

  def getCharset(contentTypeHeader) {
    String charset = "utf-8"
    for (String param : contentTypeHeader.replace(" ", "").split(";")) {
      if (param.startsWith("charset=")) {
        charset = param.split("=", 2)[1]
        break
      }
    }
    logger.info("charset: ${charset}")
    return charset
  }

  def test() {
    def response
    response = get("/_ping")
    response = get("/version")
    response = get("/images/json")
    response = get("/containers/json")
//    response = get("/containers/test/json")
//    response = post("/containers/test/attach?logs=1&stream=1&stdout=1&stderr=0&tty=false")
    response = post("/images/create?fromImage=gesellix%2Fdocker-client-testimage&tag=latest&registry=")
  }

  public static void main(String[] args) {
    def defaultDockerHost = System.env.DOCKER_HOST
    def client = new LowLevelDockerClient(dockerHost: defaultDockerHost ?: "http://172.17.42.1:4243/")
    client.test()
  }
}
