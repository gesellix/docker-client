package de.gesellix.docker.client

import de.gesellix.docker.client.config.DockerClientConfig
import de.gesellix.docker.client.filesocket.FileSocket
import de.gesellix.docker.client.filesocket.NpipeSocketFactory
import de.gesellix.docker.client.filesocket.UnixSocketFactory
import de.gesellix.docker.client.rawstream.RawInputStream
import de.gesellix.docker.client.ssl.SslSocketConfigFactory
import de.gesellix.docker.client.util.IOUtils
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.internal.http.HttpMethod
import okio.Okio

import java.util.regex.Pattern

import static java.net.Proxy.NO_PROXY
import static java.util.concurrent.TimeUnit.MILLISECONDS

@Slf4j
class OkDockerClient implements HttpClient {

    def socketFactories = [:]

    DockerClientConfig dockerClientConfig
    Proxy proxy

    OkDockerClient() {
        this(new DockerClientConfig())
    }

    OkDockerClient(String dockerHost) {
        this(new DockerClientConfig(dockerHost))
    }

    OkDockerClient(DockerClientConfig dockerClientConfig, Proxy proxy = NO_PROXY) {
        if (UnixSocketFactory.supported) {
            socketFactories.unix = new UnixSocketFactory()
        }
        socketFactories.npipe = new NpipeSocketFactory()
        socketFactories.https = new SslSocketConfigFactory()

        this.dockerClientConfig = dockerClientConfig
        this.proxy = proxy
    }

    @Override
    DockerResponse head(Map requestConfig) {
        def config = ensureValidRequestConfig(requestConfig)
        config.method = "HEAD"
        return request(config)
    }

    @Override
    DockerResponse get(Map requestConfig) {
        def config = ensureValidRequestConfig(requestConfig)
        config.method = "GET"
        return request(config)
    }

    @Override
    DockerResponse put(Map requestConfig) {
        def config = ensureValidRequestConfig(requestConfig)
        config.method = "PUT"
        return request(config)
    }

    @Override
    DockerResponse post(Map requestConfig) {
        def config = ensureValidRequestConfig(requestConfig)
        config.method = "POST"
        return request(config)
    }

    @Override
    DockerResponse delete(Map requestConfig) {
        def config = ensureValidRequestConfig(requestConfig)
        config.method = "DELETE"
        return request(config)
    }

    @Override
    WebSocket webSocket(Map requestConfig, WebSocketListener listener) {
        def config = ensureValidRequestConfig(requestConfig)
        config.method = "GET"

        Request.Builder requestBuilder = prepareRequest(new Request.Builder(), config)
        def request = requestBuilder.build()

        OkHttpClient.Builder clientBuilder = prepareClient(new OkHttpClient.Builder(), config.timeout ?: 0)
        def client = newClient(clientBuilder)

        def wsCall = client.newWebSocket(request, listener)
        wsCall
    }

    DockerResponse request(Map requestConfig) {
        def config = ensureValidRequestConfig(requestConfig)

        // https://docs.docker.com/engine/reference/api/docker_remote_api_v1.24/#attach-to-a-container
        if (requestConfig.attach) {
            config.headers = [
                    "Upgrade"   : "tcp",
                    "Connection": "Upgrade"
            ]
        }
        AttachConfig attachConfig = requestConfig.attach ?: null
//        boolean multiplexStreams = config.multiplexStreams

        Request.Builder requestBuilder = prepareRequest(new Request.Builder(), config)
        def request = requestBuilder.build()

        OkHttpClient.Builder clientBuilder = prepareClient(new OkHttpClient.Builder(), config.timeout ?: 0)
        def connectionProvider = new ConnectionProvider()
        clientBuilder.addNetworkInterceptor(connectionProvider)
        def client = newClient(clientBuilder)

        log.debug("${request.method()} ${request.url()} using proxy: ${client.proxy()}")

        def call = client.newCall(request)
        if (attachConfig) {
            def cb = new OkResponseCallback(client, connectionProvider, attachConfig)
            def response = call.enqueue(cb)
            log.debug("response: ${response.toString()}")
            def dockerResponse = new DockerResponse()
            return dockerResponse
        } else {
            def response = call.execute()
            log.debug("response: ${response.toString()}")
            def dockerResponse = handleResponse(response, config)
            if (!dockerResponse.stream) {
//            log.warn("closing response...")
                response.close()
            }
            return dockerResponse
        }
    }

    def prepareRequest(Request.Builder builder, Map config) {
        def method = config.method as String
        def contentType = config.requestContentType as String
        def additionalHeaders = config.headers
        def body = config.body

        String protocol = dockerClientConfig.scheme
        String host = dockerClientConfig.host
        int port = dockerClientConfig.port

        def path = config.path as String
        if (config.apiVersion) {
            path = "${config.apiVersion}/${path}".toString()
        }
        String queryAsString = (config.query) ? "${queryToString(config.query as Map)}" : ""

        def urlBuilder = new HttpUrl.Builder()
                .addPathSegments(path)
                .encodedQuery(queryAsString ?: null)
        def httpUrl = createUrl(urlBuilder, protocol, host, port)


        def requestBody = createRequestBody(method, contentType, body)
        builder
                .method(method, requestBody)
                .url(httpUrl)
                .cacheControl(CacheControl.FORCE_NETWORK)

        additionalHeaders?.each { String key, String value ->
            builder.header(key, value)
        }
        builder
    }

    private OkHttpClient.Builder prepareClient(OkHttpClient.Builder builder, int currentTimeout) {
        String protocol = dockerClientConfig.scheme
        if (protocol == "unix") {
            def unixSocketFactory = socketFactories[protocol] as UnixSocketFactory
            builder
                    .socketFactory(unixSocketFactory)
                    .dns(unixSocketFactory)
                    .build()
        } else if (protocol == 'npipe') {
            def npipeSocketFactory = socketFactories[protocol] as NpipeSocketFactory
            builder
                    .socketFactory(npipeSocketFactory)
                    .dns(npipeSocketFactory)
                    .build()
        } else if (protocol == 'https') {
            def certPath = this.dockerClientConfig.certPath as String
            def sslSocketFactory = socketFactories[protocol] as SslSocketConfigFactory
            def dockerSslSocket = sslSocketFactory.createDockerSslSocket(certPath)
            if (dockerSslSocket) {
                builder
                        .sslSocketFactory(dockerSslSocket.sslSocketFactory, dockerSslSocket.trustManager)
                        .build()
            }
        }
        builder.proxy(proxy)

        // do we need to disable the timeout for streaming?
        builder
                .connectTimeout(currentTimeout as int, MILLISECONDS)
                .readTimeout(currentTimeout as int, MILLISECONDS)
        builder
    }

    OkHttpClient newClient(OkHttpClient.Builder clientBuilder) {
        clientBuilder.build()
    }

    private HttpUrl createUrl(HttpUrl.Builder urlBuilder, String protocol, String host, int port) {
        def httpUrl
        if (protocol == "unix") {
            httpUrl = urlBuilder
                    .scheme("http")
                    .host(FileSocket.encodeHostname(host))
//                    .port(/not/allowed/for/unix/socket/)
                    .build()
        } else if (protocol == "npipe") {
            httpUrl = urlBuilder
                    .scheme("http")
                    .host(FileSocket.encodeHostname(host))
//                    .port(/not/allowed/for/npipe/socket/)
                    .build()
        } else {
            httpUrl = urlBuilder
                    .scheme(protocol)
                    .host(host)
                    .port(port)
                    .build()
        }
        httpUrl
    }

    def createRequestBody(String method, String contentType, body) {
        if (!body && HttpMethod.requiresRequestBody(method)) {
            return RequestBody.create(MediaType.parse("application/json"), "{}")
        }

        def requestBody = null
        if (body) {
            switch (contentType) {
                case "application/json":
                    def json = new JsonBuilder()
                    json body
                    requestBody = RequestBody.create(MediaType.parse("application/json"), json.toString())
                    break
                case "application/octet-stream":
                    def source = Okio.source(body as InputStream)
                    def buffer = Okio.buffer(source)
                    requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), buffer.readByteArray())
                    break
                default:
                    def source = Okio.source(body as InputStream)
                    def buffer = Okio.buffer(source)
                    requestBody = RequestBody.create(MediaType.parse(contentType), buffer.readByteArray())
                    break
            }
        }
        requestBody
    }

    DockerResponse handleResponse(Response httpResponse, Map config) {
        def response = readHeaders(httpResponse)

        if (response.status.code == 204) {
            if (response.stream) {
                // redirect the response body to /dev/null, since it's expected to be empty
                IOUtils.consumeToDevNull(response.stream as InputStream)
            }
            return response
        }

        switch (response.mimeType) {
            case "application/vnd.docker.raw-stream":
                InputStream rawStream = new RawInputStream(httpResponse.body().byteStream())
                response.stream = rawStream
                if (config.stdout) {
                    log.debug("redirecting to stdout.")
                    IOUtils.copy(response.stream as InputStream, config.stdout as OutputStream)
                    response.stream = null
                }
                break
            case "application/json":
                def content = new JsonContentHandler(config.async as boolean).getContent(
                        httpResponse.body().byteStream(),
                        httpResponse.header("transfer-encoding") == "chunked")
                consumeResponseBody(response, content, config)
                break
            case "text/html":
            case "text/plain":
                def stream = httpResponse.body().byteStream()
                consumeResponseBody(response, stream, config)
                break
            case "application/octet-stream":
                def stream = httpResponse.body().byteStream()
                log.debug("passing through via `response.stream`.")
                if (config.stdout) {
                    IOUtils.copy(stream, config.stdout as OutputStream)
                    response.stream = null
                } else {
                    response.stream = stream
                }
                break
            case "application/x-tar":
                if (response.stream) {
                    if (config.stdout) {
                        log.debug("redirecting to stdout.")
                        IOUtils.copy(response.stream as InputStream, config.stdout as OutputStream)
                        response.stream = null
                    } else {
                        log.warn("stream won't be consumed.")
                    }
                }
                break
            default:
                log.debug("unexpected mime type '${response.mimeType}'.")
                def body = httpResponse.body()
                if (body.contentLength() == -1) {
                    def stream = body.byteStream()
                    log.debug("passing through via `response.stream`.")
                    if (config.stdout) {
                        IOUtils.copy(stream, config.stdout as OutputStream)
                        response.stream = null
                    } else {
                        response.stream = stream
                    }
                } else {
                    log.debug("passing through via `response.content`.")
                    response.content = body.string()
                    response.stream = null
                }
                break
        }

        return response
    }

    def readHeaders(Response httpResponse) {
        def dockerResponse = new DockerResponse()

        dockerResponse.status = [text   : httpResponse.message(),
                                 code   : httpResponse.code(),
                                 success: httpResponse.successful]
        log.debug("status: ${dockerResponse.status}")

        def headers = httpResponse.headers()
        log.debug("headers: \n${headers}")
        dockerResponse.headers = headers

        String contentType = headers['content-type']
        dockerResponse.contentType = contentType

        String contentLength = headers['content-length'] ?: "-1"
        dockerResponse.contentLength = contentLength

        String mimeType = getMimeType(contentType)
        dockerResponse.mimeType = mimeType

        if (dockerResponse.status.success) {
            dockerResponse.stream = httpResponse.body().byteStream()
        } else {
            dockerResponse.stream = null
        }
        return dockerResponse
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
