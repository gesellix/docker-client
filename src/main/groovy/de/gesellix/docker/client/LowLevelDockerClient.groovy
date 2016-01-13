package de.gesellix.docker.client

import de.gesellix.docker.client.protocolhandler.DockerContentHandlerFactory
import de.gesellix.docker.client.protocolhandler.DockerURLHandler
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import org.apache.commons.io.IOUtils
import org.apache.commons.io.output.NullOutputStream

import javax.net.ssl.*
import java.nio.charset.Charset
import java.util.regex.Pattern

import static de.gesellix.docker.client.KeyStoreUtil.KEY_STORE_PASSWORD
import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm

@Slf4j
class LowLevelDockerClient {

    DockerURLHandler dockerURLHandler

    Proxy proxy
    DockerConfig config = new DockerConfig()

    SSLContext sslContext
    SSLSocketFactory sslSocketFactory

    LowLevelDockerClient() {
        proxy = Proxy.NO_PROXY
        sslContext = null
        sslSocketFactory = null
    }

    def head(requestConfig) {
        def config = ensureValidRequestConfig(requestConfig)
        config.method = "HEAD"
        return request(config)
    }

    def get(requestConfig) {
        def config = ensureValidRequestConfig(requestConfig)
        config.method = "GET"
        return request(config)
    }

    def put(requestConfig) {
        def config = ensureValidRequestConfig(requestConfig)
        config.method = "PUT"
        return request(config)
    }

    def post(requestConfig) {
        def config = ensureValidRequestConfig(requestConfig)
        config.method = "POST"
        return request(config)
    }

    def delete(requestConfig) {
        def config = ensureValidRequestConfig(requestConfig)
        config.method = "DELETE"
        return request(config)
    }

    def getWebsocketClient(requestConfig, handler) {
        def config = ensureValidRequestConfig(requestConfig)
        config.method = "GET"

        def requestUrl = getRequestUrlWithOptionalQuery(config).toExternalForm()
        requestUrl = requestUrl.replaceFirst("^http", "ws")

        log.debug "websocket uri: '$requestUrl'"

        def websocketClient
        if (requestUrl.startsWith("wss://")) {
            websocketClient = new DockerWebsocketClient(new URI(requestUrl), handler, initSSLContext())
        } else {
            websocketClient = new DockerWebsocketClient(new URI(requestUrl), handler)
        }
        return websocketClient
    }

    def request(config) {
        config = ensureValidRequestConfig(config)

        HttpURLConnection connection = openConnection(config as Map)
        configureConnection(connection, config as Map)

        // since we listen to a stream we disable the timeout
//    connection.setConnectTimeout(0)
//    connection.setReadTimeout(0)

        if (config.body) {
            InputStream postBody
            int postDataLength
            switch (config.requestContentType) {
                case "application/json":
                    def json = new JsonBuilder()
                    json config.body
                    def postData = json.toString().getBytes(Charset.forName("UTF-8"))
                    postBody = new ByteArrayInputStream(postData)
                    postDataLength = postData.length
                    connection.setRequestProperty("charset", "utf-8")
                    break
                case "application/octet-stream":
                    postBody = config.body
                    postDataLength = -1
                    break
                default:
                    postBody = config.body
                    postDataLength = -1
                    break
            }

            connection.setDoOutput(true)
            connection.setDoInput(true)
            connection.setInstanceFollowRedirects(false)

            if (config.requestContentType) {
                connection.setRequestProperty("Content-Type", config.requestContentType as String)
            }
            if (postDataLength >= 0) {
                connection.setRequestProperty("Content-Length", Integer.toString(postDataLength))
            }
            IOUtils.copy(postBody, connection.getOutputStream())
        }

        config.headers?.each { key, value ->
            connection.setRequestProperty(key, value)
        }

        def response = handleResponse(connection, config as Map)
        return response
    }

    def handleResponse(HttpURLConnection connection, Map config) {
        def response = readHeaders(connection)

        if (response.status.code == 204) {
            if (response.stream) {
                // redirect the response body to /dev/null, since it's expected to be empty
                IOUtils.copy(response.stream as InputStream, new NullOutputStream())
            }
            return response
        }

        ContentHandlerFactory contentHandlerFactory = newDockerContentHandlerFactory(config)
        def contentHandler = contentHandlerFactory.createContentHandler(response.mimeType as String)
        if (contentHandler == null) {
            if (response.stream) {
                log.warn("couldn't find a specific ContentHandler for '${response.contentType}'.")
                if (config.stdout) {
                    log.debug("redirecting to stdout.")
                    IOUtils.copy(response.stream as InputStream, config.stdout as OutputStream)
                    response.stream = null
                } else {
                    log.warn("stream won't be consumed.")
                }
            }
            return response
        }

        switch (response.mimeType) {
            case "application/vnd.docker.raw-stream":
                def content = contentHandler.getContent(connection)
                InputStream rawStream = content as InputStream
                response.stream = rawStream
                if (config.stdout) {
                    log.debug("redirecting to stdout.")
                    IOUtils.copy(response.stream as InputStream, config.stdout as OutputStream)
                    response.stream = null
                }
                break
            case "application/json":
                def content = contentHandler.getContent(connection)
                consumeResponseBody(response, content, config)
                break
            case "text/html":
                def content = contentHandler.getContent(connection)
                consumeResponseBody(response, content, config)
                break
            case "text/plain":
                def content = contentHandler.getContent(connection)
                consumeResponseBody(response, content, config)
                break
            default:
                log.warn("unexpected mime type '${response.mimeType}'.")
                def content = contentHandler.getContent(connection)
                if (content instanceof InputStream) {
                    log.debug("passing through via `response.stream`.")
                    if (config.stdout) {
                        IOUtils.copy(content as InputStream, config.stdout as OutputStream)
                        response.stream = null
                    } else {
                        response.stream = content as InputStream
                    }
                } else {
                    log.debug("passing through via `response.content`.")
                    response.content = content
                    response.stream = null
                }
                break
        }

        return response
    }

    def newDockerContentHandlerFactory(Map config) {
        return new DockerContentHandlerFactory(config.async as boolean)
    }

    def readHeaders(HttpURLConnection connection) {
        def response = new DockerResponse()

        def statusLine = connection.headerFields[null]
        log.debug("status: ${statusLine}")
        response.status = [text   : statusLine,
                           code   : connection.responseCode,
                           success: 200 <= connection.responseCode && connection.responseCode < 300]

        def headers = connection.headerFields.findAll { key, value ->
            key != null
        }.collectEntries { key, value ->
            [key.toLowerCase(), value]
        }
        log.debug("headers: ${headers}")
        response.headers = headers

        String contentType = headers['content-type']?.first()
        log.trace("content-type: ${contentType}")
        response.contentType = contentType

        String contentLength = headers['content-length']?.first() ?: "-1"
        log.trace("content-length: ${contentLength}")
        response.contentLength = contentLength

        String mimeType = getMimeType(contentType)
        log.trace("mime type: ${mimeType}")
        response.mimeType = mimeType

        if (response.status.success) {
            response.stream = connection.inputStream
        } else {
            response.stream = null
        }
        return response
    }

    def consumeResponseBody(DockerResponse response, Object content, Map config) {
        if (content instanceof InputStream) {
            if (config.async) {
                response.stream = content as InputStream
            } else if (config.stdout) {
                IOUtils.copy(content as InputStream, config.stdout as OutputStream)
                response.stream = null
            } else if (response.contentLength && Integer.parseInt(response.contentLength as String) >= 0) {
                response.content = IOUtils.toString(content as InputStream)
                response.stream = null
            } else {
                response.stream = content as InputStream
            }
        } else {
            response.content = content
            response.stream = null
        }
    }

    def ensureValidRequestConfig(Object config) {
        def validConfig = (config instanceof String) ? [path: config] : config
        if (!validConfig?.path) {
            log.error("bad request config: ${config}")
            throw new IllegalArgumentException("bad request config")
        }
        return validConfig
    }

    def getRequestUrl(String path, String query) {
        return getDockerURLHandler().getRequestUrl(config.dockerHost, path, query)
    }

    def getRequestUrlWithOptionalQuery(config) {
        String queryAsString = (config.query) ? "?${queryToString(config.query)}" : ""
        return getRequestUrl(config.path as String, queryAsString)
    }

    def openConnection(config) {
        def requestUrl = getRequestUrlWithOptionalQuery(config)
        log.debug("${config.method} ${requestUrl} using proxy: ${proxy}")

        def connection = requestUrl.openConnection(proxy)
        return connection as HttpURLConnection
    }

    def queryToString(Map queryParameters) {
        def queryAsString = queryParameters.collect { key, value ->
            if (value instanceof String) {
                "${URLEncoder.encode("$key".toString(), "UTF-8")}=${URLEncoder.encode("$value".toString(), "UTF-8")}"
            } else {
                value.collect {
                    "${URLEncoder.encode("$key".toString(), "UTF-8")}=${URLEncoder.encode("$it".toString(), "UTF-8")}"
                }.join("&")
            }
        }
        return queryAsString.join("&")
    }

    def configureConnection(HttpURLConnection connection, Map config) {
        connection.setUseCaches(false)
        connection.setRequestMethod(config.method as String)
        configureSSL(connection)
    }

    def configureSSL(HttpURLConnection connection) {
        if (connection instanceof HttpsURLConnection) {
            SSLSocketFactory sslSocketFactory = initSSLSocketFactory()
            ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory)
        }
    }

    def getDockerURLHandler() {
        if (!dockerURLHandler) {
            dockerURLHandler = new DockerURLHandler(config: config)
        }
        dockerURLHandler
    }

    SSLSocketFactory initSSLSocketFactory() {
        if (!sslSocketFactory) {
            sslSocketFactory = initSSLContext().socketFactory
        }
        return sslSocketFactory
    }

    SSLContext initSSLContext() {
        if (!sslContext) {
            def dockerCertPath = config.certPath

            def keyStore = KeyStoreUtil.createDockerKeyStore(new File(dockerCertPath as String).absolutePath)
            final KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(getDefaultAlgorithm())
            kmfactory.init(keyStore, KEY_STORE_PASSWORD as char[])

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(getDefaultAlgorithm())
            tmf.init(keyStore)

            sslContext = SSLContext.getInstance("TLS")
            sslContext.init(kmfactory.keyManagers, tmf.trustManagers, null)
        }
        return sslContext
    }

    String getMimeType(String contentTypeHeader) {
        return contentTypeHeader?.replace(" ", "")?.split(";")?.first()
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
