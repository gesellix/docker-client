package de.gesellix.docker.client

import de.gesellix.docker.client.protocolhandler.DockerContentHandlerFactory
import de.gesellix.docker.client.protocolhandler.DockerURLHandler
import de.gesellix.docker.client.protocolhandler.RawInputStream
import de.gesellix.socketfactory.https.KeyStoreUtil
import groovy.json.JsonBuilder
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.net.ssl.*
import java.nio.charset.Charset
import java.util.regex.Pattern

import static de.gesellix.socketfactory.https.KeyStoreUtil.getKEY_STORE_PASSWORD
import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm

// Proof-of-concept for https://docs.docker.com/reference/api/docker_remote_api_v1.17/#attach-to-a-container
class LowLevelDockerClient {

  def Logger logger = LoggerFactory.getLogger(LowLevelDockerClient)

  ContentHandlerFactory contentHandlerFactory
  DockerURLHandler dockerURLHandler

  def dockerHost
  URL dockerHostUrl

  def sslSocketFactory

  LowLevelDockerClient() {
    dockerURLHandler = new DockerURLHandler()
    contentHandlerFactory = new DockerContentHandlerFactory()
    dockerHost = "http://127.0.0.1:2375"
    sslSocketFactory = null
  }

  def getDockerBaseUrl() {
    if (!getDockerHostUrl()) {
      dockerURLHandler.dockerHost = getDockerHost()
      dockerHostUrl = dockerURLHandler.getURL()
    }
    return dockerHostUrl
  }

  def get(requestConfig) {
    def config = ensureValidRequestConfig(requestConfig)
    config.method = "GET"
    return request(config)
  }

  def post(requestConfig) {
    def config = ensureValidRequestConfig(requestConfig)
    config.method = "POST"
    return request(config)
  }

  def request(config) {
    config = ensureValidRequestConfig(config)

    HttpURLConnection connection = openConnection(config)
    configureConnection(connection, config)

    // since we listen to a stream we disable the timeout
//    connection.setConnectTimeout(0)
    // since we listen to a stream we disable the timeout
//    connection.setReadTimeout(0)

    if (config.body) {
      byte[] postData
      int postDataLength
      switch (config.contentType) {
        case "application/json":
          def json = new JsonBuilder()
          json config.body
          def bodyAsString = json.toString()
          postData = bodyAsString.getBytes(Charset.forName("UTF-8"))
          postDataLength = postData.length
          break;
        default:
          postData = config.body.toString().getBytes(Charset.forName("UTF-8"))
          postDataLength = postData.length
          break;
      }

      connection.setDoOutput(true)
      connection.setDoInput(true)
      connection.setInstanceFollowRedirects(false)

      connection.setRequestProperty("Content-Type", config.contentType as String)
      connection.setRequestProperty("charset", "utf-8")
      connection.setRequestProperty("Content-Length", Integer.toString(postDataLength))
      IOUtils.copy(new ByteArrayInputStream(postData), connection.getOutputStream())
    }

    def response = handleResponse(connection, config)
    return response
  }

  def handleResponse(connection, config) {
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

    def mimetype = getMimeType(contentType)
    logger.debug("mime type: ${mimetype}")
    def contentHandler = contentHandlerFactory.createContentHandler(mimetype)
    if (contentHandler == null) {
      logger.warn("couldn't find a specific ContentHandler for '${contentType}'. redirecting to stdout.")
      if (config.stdout) {
        IOUtils.copy(response.stream as InputStream, config.stdout as OutputStream)
      }
      else {
        IOUtils.copy(response.stream as InputStream, System.out)
        println()
        response.stream = null
      }
    }
    else {
      switch (mimetype) {
        case "application/vnd.docker.raw-stream":
          InputStream rawStream = contentHandler.getContent(connection) as RawInputStream
          if (config.stdout) {
            IOUtils.copy(rawStream as InputStream, config.stdout as OutputStream)
          }
          else {
            IOUtils.copy(rawStream, System.out)
            println()
            response.stream = null
          }
          break;
        case "application/json":
          def body = contentHandler.getContent(connection)
          if (config.stdout && body instanceof InputStream) {
            IOUtils.copy(body as InputStream, config.stdout as OutputStream)
          }
          else {
            if (body instanceof InputStream) {
              response.content = IOUtils.toString(body as InputStream)
              response.stream = null
            }
            else {
              response.content = body
              response.stream = null
            }
          }
          break;
        case "text/html":
          def body = contentHandler.getContent(connection)
          if (config.stdout && body instanceof InputStream) {
            IOUtils.copy(body as InputStream, config.stdout as OutputStream)
          }
          else {
            response.content = IOUtils.toString(body as InputStream)
            response.stream = null
          }
          break;
        case "text/plain":
          def body = contentHandler.getContent(connection)
          if (config.stdout && body instanceof InputStream) {
            IOUtils.copy(body as InputStream, config.stdout as OutputStream)
          }
          else {
            response.content = IOUtils.toString(body as InputStream)
            response.stream = null
          }
          break;
        default:
          if (config.stdout) {
            IOUtils.copy(response.stream as InputStream, config.stdout as OutputStream)
          }
          else {
            response.content = IOUtils.toString(response.stream as InputStream)
            response.stream = null
          }
          println()
          break
      }
    }
    return response
  }

  def ensureValidRequestConfig(config) {
    def validConfig = config
    if (config instanceof String) {
      validConfig = [path: config]
    }
    if (!validConfig?.path) {
      logger.error("bad request config: ${config}")
      throw new IllegalArgumentException("bad request config")
    }
    return validConfig
  }

  def openConnection(config) {
    config.query = (config.query) ? "?${queryToString(config.query)}" : ""
    def requestUrl = new URL("${getDockerBaseUrl()}${config.path}${config.query}")
    logger.info("${config.method} ${requestUrl}")

    def connection = requestUrl.openConnection()
    return connection as HttpURLConnection
  }

  def queryToString(query) {
//    Charset.forName("UTF-8")
    def queryAsString = query.collect { key, value ->
      "${URLEncoder.encode("$key".toString(), "UTF-8")}=${URLEncoder.encode("$value".toString(), "UTF-8")}"
    }
    return queryAsString.join("&")
  }

  def configureConnection(HttpURLConnection connection, config) {
    connection.setUseCaches(false)
    connection.setRequestMethod(config.method as String)
    configureSSL(connection)
  }

  def configureSSL(connection) {
    if (connection instanceof HttpsURLConnection) {
      SSLSocketFactory sslSocketFactory = initSSLSocketFactory()
      ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory)
    }
  }

  SSLSocketFactory initSSLSocketFactory() {
    if (!sslSocketFactory) {
      def dockerCertPath = dockerURLHandler.dockerCertPath

      def keyStore = KeyStoreUtil.createDockerKeyStore(new File(dockerCertPath).absolutePath)
      final KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(getDefaultAlgorithm());
      kmfactory.init(keyStore, KEY_STORE_PASSWORD as char[]);

      TrustManagerFactory tmf = TrustManagerFactory.getInstance(getDefaultAlgorithm());
      tmf.init(keyStore)

      def sslContext = SSLContext.getInstance("TLS")
      sslContext.init(kmfactory.keyManagers, tmf.trustManagers, null)
      sslSocketFactory = sslContext.socketFactory
    }
    return sslSocketFactory
  }

  String getMimeType(String contentTypeHeader) {
    return contentTypeHeader.replace(" ", "").split(";").first()
  }

  String getCharset(String contentTypeHeader) {
    String charset = "utf-8"
    def matcher = Pattern.compile("[^;]+;\\s*charset=([^;]+)(;[^;]*)*").matcher(contentTypeHeader)
    if (matcher.find()) {
      charset = matcher.group(1)
    }
    return charset
  }
}
