package de.gesellix.docker.client

import de.gesellix.docker.client.OkHttpClient
import de.gesellix.docker.client.protocolhandler.DockerContentHandlerFactory
import de.gesellix.docker.client.protocolhandler.DockerURLHandler
import de.gesellix.docker.client.protocolhandler.content.application.json
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import okhttp3.*
import okhttp3.internal.http.HttpMethod
import org.apache.commons.io.IOUtils
import org.apache.commons.io.input.ReaderInputStream
import org.apache.commons.io.output.NullOutputStream

import java.util.regex.Pattern

import static java.util.concurrent.TimeUnit.MILLISECONDS

@Slf4j
class OkHttpClient {

    okhttp3.OkHttpClient client

    DockerURLHandler dockerURLHandler

    Proxy proxy
    DockerConfig config = new DockerConfig()
    DockerSslSocketFactory.DockerSslSocket dockerSslSocket

    OkHttpClient() {
        client = new okhttp3.OkHttpClient.Builder().build()
        proxy = Proxy.NO_PROXY
        dockerSslSocket = null
    }

    def head(String path) {
        return head(ensureValidRequestConfig(path))
    }

    def head(Map requestConfig) {
        def config = ensureValidRequestConfig(requestConfig)
        config.method = "HEAD"
        return request(config)
    }

    def get(String path) {
        return get(ensureValidRequestConfig(path))
    }

    def get(Map requestConfig) {
        def config = ensureValidRequestConfig(requestConfig)
        config.method = "GET"
        return request(config)
    }

    def put(String path) {
        return put(ensureValidRequestConfig(path))
    }

    def put(Map requestConfig) {
        def config = ensureValidRequestConfig(requestConfig)
        config.method = "PUT"
        return request(config)
    }

    def post(String path) {
        return post(ensureValidRequestConfig(path))
    }

    def post(Map requestConfig) {
        def config = ensureValidRequestConfig(requestConfig)
        config.method = "POST"
        return request(config)
    }

    def delete(String path) {
        return delete(ensureValidRequestConfig(path))
    }

    def delete(Map requestConfig) {
        def config = ensureValidRequestConfig(requestConfig)
        config.method = "DELETE"
        return request(config)
    }

    def getWebsocketClient(String path, handler) {
        def config = ensureValidRequestConfig(path)
        return getWebsocketClient(config, handler)
    }

    def getWebsocketClient(Map requestConfig, handler) {
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

    def request(Map config) {
        config = ensureValidRequestConfig(config)

        def requestBuilder = new Request.Builder()

        def (String protocol, String host) = getDockerURLHandler().getProtocolAndHost(this.config.dockerHost)
        String queryAsString = (config.query) ? "${queryToString(config.query as Map)}" : ""
        if (protocol == "unix") {
            def unixSocketFactory = new UnixSocketFactory()
            client = client.newBuilder()
                    .socketFactory(unixSocketFactory)
                    .dns(unixSocketFactory)
                    .build()

            requestBuilder.url(new HttpUrl.Builder()
                    .scheme("http")
                    .host(UnixSocketFactory.UnixSocket.encodeHostname(host))
                    .addPathSegments(config.path as String)
                    .encodedQuery(queryAsString)
                    .build())
        } else if (protocol == "npipe") {
            throw new UnsupportedOperationException("not yet implemented")
        } else {
            requestBuilder.url(new HttpUrl.Builder()
                    .scheme(protocol)
                    .host(host)
                    .addPathSegments(config.path as String)
                    .query(queryAsString)
                    .build())
        }

        if (protocol == 'https') {
            def dockerSslSocket = getSslContext()
            client = client.newBuilder()
                    .sslSocketFactory(
                    dockerSslSocket.sslSocketFactory,
                    dockerSslSocket.trustManager)
                    .build()
        }

        client = client.newBuilder()
                .proxy(proxy)
                .build()

        requestBuilder.cacheControl(CacheControl.FORCE_NETWORK)

        // do we need to disable the timeout for streaming?
        if (config.timeout) {
            client = client.newBuilder()
                    .connectTimeout(config.timeout as int, MILLISECONDS)
                    .readTimeout(config.timeout as int, MILLISECONDS)
                    .build()
        }

        def requestBody = null
        if (HttpMethod.requiresRequestBody(config.method)) {
            requestBody = RequestBody.create(MediaType.parse("application/json"), "{}")
        }

        if (config.body) {
            InputStream postBody
            int postDataLength
            switch (config.requestContentType) {
                case "application/json":
                    def json = new JsonBuilder()
                    json config.body
                    requestBody = RequestBody.create(MediaType.parse("application/json"), json.toString())
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

//        config.headers?.each { String key, String value ->
//            requestBuilder.header(key, value)
//        }

        requestBuilder.method(config.method as String, requestBody)
        def request = requestBuilder.build()
        log.debug("${request.method()} ${request.url()} using proxy: ${client.proxy()}")
//        log.info("request: ${request.toString()}")

        def response = client.newCall(request).execute()
        log.debug("response: ${response.toString()}")
        def dockerResponse = handleResponse(response, config)
//        dockerResponse.content = response.body().string()
        return dockerResponse
    }

    DockerResponse handleResponse(Response httpResponse, Map config) {
        def response = readHeaders(httpResponse)

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
                def content = new json(config.async as boolean).getContent(
                        httpResponse.body().charStream(),
                        httpResponse.header("transfer-encoding") == "chunked")
                consumeResponseBody(response, content, config)
                break
            case "text/html":
                def content = contentHandler.getContent(connection)
                consumeResponseBody(response, content, config)
                break
            case "text/plain":
                def content = httpResponse.body().charStream()
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

    def readHeaders(Response httpResponse) {
        def dockerResponse = new DockerResponse()

        dockerResponse.status = [text   : httpResponse.message(),
                                 code   : httpResponse.code(),
                                 success: httpResponse.successful]
        log.debug("status: ${dockerResponse.status}")

        def headers = httpResponse.headers()
        log.debug("headers: ${headers}")
        dockerResponse.headers = headers

        String contentType = headers['content-type']
        log.trace("content-type: ${contentType}")
        dockerResponse.contentType = contentType

        String contentLength = headers['content-length'] ?: "-1"
        log.trace("content-length: ${contentLength}")
        dockerResponse.contentLength = contentLength

        String mimeType = getMimeType(contentType)
        log.trace("mime type: ${mimeType}")
        dockerResponse.mimeType = mimeType

        if (dockerResponse.status.success) {
            dockerResponse.stream = new ReaderInputStream(httpResponse.body().charStream())
        } else {
            dockerResponse.stream = null
        }
        return dockerResponse
    }

    def consumeResponseBody(DockerResponse response, Object content, Map config) {
        if (content instanceof Reader) {
            content = new ReaderInputStream(content as Reader)
        }
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

    Map ensureValidRequestConfig(String path) {
        return ensureValidRequestConfig([path: path])
    }

    Map ensureValidRequestConfig(Map config) {
        if (!config?.path) {
            log.error("bad request config: ${config}")
            throw new IllegalArgumentException("bad request config")
        }
        if (config.path.startsWith("/")) {
            config.path = config.path.substring("/".length())
        }
        return config
    }

    HttpUrl getRequestUrl(String path, String query) {
        def (String protocol, String host) = getDockerURLHandler().getProtocolAndHost(config.dockerHost)
        new HttpUrl.Builder()
//                .scheme(protocol)
                .host(host)
                .addPathSegment(path)
                .query(query)
                .build()

        return getDockerURLHandler().getRequestUrl(config.dockerHost, path, query)
    }

    HttpUrl getRequestUrlWithOptionalQuery(config) {
        String queryAsString = (config.query) ? "?${queryToString(config.query)}" : ""
        return getRequestUrl(config.path as String, queryAsString)
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

    def getDockerURLHandler() {
        if (!dockerURLHandler) {
            dockerURLHandler = new DockerURLHandler(config: config)
        }
        dockerURLHandler
    }

    DockerSslSocketFactory.DockerSslSocket getSslContext() {
        if (!dockerSslSocket) {
            dockerSslSocket = new DockerSslSocketFactory().createDockerSslSocket(config.certPath)
        }
        return dockerSslSocket
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
