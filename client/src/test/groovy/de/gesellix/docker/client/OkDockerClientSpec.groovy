package de.gesellix.docker.client

import de.gesellix.docker.client.config.DockerEnv
import de.gesellix.docker.client.rawstream.RawHeaderAndPayload
import de.gesellix.docker.client.rawstream.RawInputStream
import de.gesellix.docker.client.ssl.DockerSslSocket
import de.gesellix.docker.client.ssl.SslSocketConfigFactory
import de.gesellix.util.IOUtils
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.codehaus.groovy.runtime.MethodClosure
import spock.lang.IgnoreIf
import spock.lang.Specification
import spock.lang.Unroll

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocket

import static de.gesellix.docker.client.rawstream.StreamType.STDOUT
import static java.net.Proxy.Type.DIRECT
import static java.net.Proxy.Type.HTTP

class OkDockerClientSpec extends Specification {

    @IgnoreIf({ System.env.DOCKER_HOST })
    "getProtocolAndHost should fallback to unix:///var/run/docker.sock (npipe on Windows)"() {
        given:
        def client = new OkDockerClient()
        def isWindows = System.properties['os.name'].toLowerCase().contains('windows')
        def expectedScheme = isWindows ? "npipe" : "unix"
        def defaultHost = isWindows ? "//./pipe/docker_engine" : "/var/run/docker.sock"

        expect:
        client.dockerClientConfig.scheme == expectedScheme
        client.dockerClientConfig.host == defaultHost
        client.dockerClientConfig.port == -1
    }

    @IgnoreIf({ System.env.DOCKER_HOST })
    "getProtocolAndHost should use to docker.host system property when set"() {
        given:
        def oldDockerHost = System.setProperty("docker.host", "http://127.0.0.1:2375")
        def client = new OkDockerClient()
        expect:
        client.dockerClientConfig.scheme == "http"
        client.dockerClientConfig.host == "127.0.0.1"
        client.dockerClientConfig.port == 2375
        cleanup:
        if (oldDockerHost) {
            System.setProperty("docker.host", oldDockerHost)
        } else {
            System.clearProperty("docker.host")
        }
    }

    def "getProtocolAndHost should support tcp protocol"() {
        def client = new OkDockerClient()
        client.dockerClientConfig.apply(new DockerEnv(dockerHost: "tcp://127.0.0.1:2375"))
        when:
        def protocol = client.dockerClientConfig.scheme
        def host = client.dockerClientConfig.host
        def port = client.dockerClientConfig.port
        then:
        protocol =~ /https?/
        and:
        host == "127.0.0.1"
        and:
        port == 2375
    }

    def "getProtocolAndHost should support tls port"() {
        given:
        def certsPath = IOUtils.getResource("/certs").file
        def oldDockerCertPath = System.setProperty("docker.cert.path", certsPath)
        def client = new OkDockerClient()
        client.dockerClientConfig.apply(new DockerEnv(
                dockerHost: "tcp://127.0.0.1:2376"))
        when:
        def protocol = client.dockerClientConfig.scheme
        def host = client.dockerClientConfig.host
        def port = client.dockerClientConfig.port
        then:
        protocol == "https"
        and:
        host == "127.0.0.1"
        and:
        port == 2376
        cleanup:
        if (oldDockerCertPath) {
            System.setProperty("docker.cert.path", oldDockerCertPath)
        } else {
            System.clearProperty("docker.cert.path")
        }
    }

    def "getProtocolAndHost should support https protocol"() {
        given:
        def certsPath = IOUtils.getResource("/certs").file
        def oldDockerCertPath = System.setProperty("docker.cert.path", certsPath)
        def client = new OkDockerClient()
        client.dockerClientConfig.apply(new DockerEnv(
                dockerHost: "https://127.0.0.1:2376"))
        expect:
        client.dockerClientConfig.scheme == "https"
        client.dockerClientConfig.host == "127.0.0.1"
        client.dockerClientConfig.port == 2376
        cleanup:
        if (oldDockerCertPath) {
            System.setProperty("docker.cert.path", oldDockerCertPath)
        } else {
            System.clearProperty("docker.cert.path")
        }
    }

    def "getMimeType"() {
        def client = new OkDockerClient()
        client.dockerClientConfig.apply(new DockerEnv(
                dockerHost: "https://127.0.0.1:2376"))
        expect:
        client.getMimeType(contentType) == expectedMimeType
        where:
        contentType                         | expectedMimeType
        null                                | null
        "application/json"                  | "application/json"
        "text/plain"                        | "text/plain"
        "text/plain; charset=utf-8"         | "text/plain"
        "text/html"                         | "text/html"
        "application/vnd.docker.raw-stream" | "application/vnd.docker.raw-stream"
    }

    def "getCharset"() {
        def client = new OkDockerClient()
        client.dockerClientConfig.apply(new DockerEnv(
                dockerHost: "https://127.0.0.1:2376"))
        expect:
        client.getCharset(contentType) == expectedCharset
        where:
        contentType                         | expectedCharset
        "application/json"                  | "utf-8"
        "text/plain"                        | "utf-8"
        "text/plain; charset=ISO-8859-1"    | "ISO-8859-1"
        "text/html"                         | "utf-8"
        "application/vnd.docker.raw-stream" | "utf-8"
    }

    def "queryToString"() {
        def client = new OkDockerClient()
        client.dockerClientConfig.apply(new DockerEnv(
                dockerHost: "https://127.0.0.1:2376"))
        expect:
        client.queryToString(parameters) == query
        where:
        parameters                           | query
        null                                 | ""
        [:]                                  | ""
        [param1: "value-1"]                  | "param1=value-1"
        ["p 1": "v 1"]                       | "p+1=v+1"
        [param1: "v 1", p2: "v-2"]           | "param1=v+1&p2=v-2"
        [params: ["v 1", "v-2"]]             | "params=v+1&params=v-2"
        [params: ["a 1", "a-2"] as String[]] | "params=a+1&params=a-2"
    }

    @Unroll
    "generic request with bad config: #requestConfig"() {
        def client = new OkDockerClient()
        client.dockerClientConfig.apply(new DockerEnv(
                dockerHost: "https://127.0.0.1:2376"))
        when:
        client.request(requestConfig as Map)
        then:
        def ex = thrown(RuntimeException)
        ex.message == "bad request config"
        where:
        requestConfig << [null, [:], ["foo": "bar"]]
    }

    @Unroll
    "#method request with bad config: #requestConfig"() {
        def client = new OkDockerClient()
        client.dockerClientConfig.apply(new DockerEnv(
                dockerHost: "https://127.0.0.1:2376"))
        when:
        new MethodClosure(client, method).call(requestConfig as Map)
        then:
        def ex = thrown(RuntimeException)
        ex.message == "bad request config"
        where:
        requestConfig  | method
        null           | "get"
        null           | "post"
        [:]            | "get"
        [:]            | "post"
        ["foo": "bar"] | "get"
        ["foo": "bar"] | "post"
    }

    def "head request uses the HEAD method"() {
        given:
        def recordedCall = [:]
        def client = new OkDockerClient()
        client.dockerClientConfig.apply(new DockerEnv(dockerHost: "https://127.0.0.1:2376"))
        client.metaClass.request = { Map config ->
            recordedCall['method'] = config.method
            return null
        }
        when:
        client.head([path: "/foo"])
        then:
        recordedCall['method'] == "HEAD"
    }

    def "get request uses the GET method"() {
        given:
        def recordedCall = [:]
        def client = new OkDockerClient()
        client.dockerClientConfig.apply(new DockerEnv(dockerHost: "https://127.0.0.1:2376"))
        client.metaClass.request = { Map config ->
            recordedCall['method'] = config.method
            return null
        }
        when:
        client.get([path: "/foo"])
        then:
        recordedCall['method'] == "GET"
    }

    def "put request uses the PUT method"() {
        given:
        def recordedCall = [:]
        def client = new OkDockerClient()
        client.dockerClientConfig.apply(new DockerEnv(dockerHost: "https://127.0.0.1:2376"))
        client.metaClass.request = { Map config ->
            recordedCall['method'] = config.method
            return null
        }
        when:
        client.put([path: "/foo"])
        then:
        recordedCall['method'] == "PUT"
    }

    def "post request uses the POST method"() {
        given:
        def recordedCall = [:]
        def client = new OkDockerClient()
        client.dockerClientConfig.apply(new DockerEnv(dockerHost: "https://127.0.0.1:2376"))
        client.metaClass.request = { Map config ->
            recordedCall['method'] = config.method
            return null
        }
        when:
        client.post([path: "/foo"])
        then:
        recordedCall['method'] == "POST"
    }

    def "delete request uses the DELETE method"() {
        given:
        def recordedCall = [:]
        def client = new OkDockerClient()
        client.dockerClientConfig.apply(new DockerEnv(dockerHost: "https://127.0.0.1:2376"))
        client.metaClass.request = { Map config ->
            recordedCall['method'] = config.method
            return null
        }
        when:
        client.delete([path: "/foo"])
        then:
        recordedCall['method'] == "DELETE"
    }

    def "webSocket prepares a websocket call"() {
        given:
        def certsPath = IOUtils.getResource("/certs").file
        def oldDockerCertPath = System.setProperty("docker.cert.path", certsPath)
        def client = new OkDockerClient()
        client.dockerClientConfig.apply(new DockerEnv(dockerHost: "https://127.0.0.1:2376"))

        when:
        def wsCall = client.webSocket([path: "/foo"], Mock(WebSocketListener))

        then:
        wsCall != null

        cleanup:
        if (oldDockerCertPath) {
            System.setProperty("docker.cert.path", oldDockerCertPath)
        } else {
            System.clearProperty("docker.cert.path")
        }
    }

    def "uses DIRECT proxy by default"() {
        given:
        def mockWebServer = new MockWebServer()
        mockWebServer.enqueue(new MockResponse().setBody("mock-response"))
        mockWebServer.start()

        def hasVerified = false
        Closure<Boolean> verifyResponse = { Response response, Interceptor.Chain chain ->
            if (chain.connection()?.route()?.proxy()?.type() != DIRECT) {
                throw new AssertionError("got ${chain.connection()?.route()?.proxy()}")
            }
            hasVerified = true
            true
        }
        def client = new OkDockerClient() {
            @Override
            OkHttpClient newClient(OkHttpClient.Builder clientBuilder) {
                clientBuilder
                        .addNetworkInterceptor(new TestInterceptor({ true }, verifyResponse))
                        .build()
            }
        }
        client.dockerClientConfig.apply(new DockerEnv(dockerHost: mockWebServer.url("/").toString()))

        when:
        def response = client.request([method: "OPTIONS",
                                       path  : "/a-resource"])
        then:
        response.status.success
        and:
        hasVerified

        cleanup:
        mockWebServer.shutdown()
    }

    def "uses configured proxy"() {
        def mockWebServer = new MockWebServer()
        mockWebServer.enqueue(new MockResponse().setBody("mock-response"))
        mockWebServer.start()

        def serverUrl = mockWebServer.url("/")
        def proxy = new Proxy(HTTP, new InetSocketAddress(serverUrl.host(), serverUrl.port()))

        def hasVerified = false
        Closure<Boolean> verifyResponse = { Response response, Interceptor.Chain chain ->
            def actualProxy = chain.connection()?.route()?.proxy()
            if (actualProxy != proxy) {
                throw new AssertionError("got ${actualProxy}")
            }
            hasVerified = true
            true
        }
        def client = new OkDockerClient() {
            @Override
            OkHttpClient newClient(OkHttpClient.Builder clientBuilder) {
                clientBuilder
                        .addNetworkInterceptor(new TestInterceptor({ true }, verifyResponse))
                        .build()
            }
        }
        client.proxy = proxy
        client.dockerClientConfig.apply(new DockerEnv(
                dockerHost: "http://any.thi.ng:4711"))

        when:
        def response = client.request([method: "OPTIONS",
                                       path  : "/a-resource"])
        then:
        response.status.success
        and:
        hasVerified

        cleanup:
        mockWebServer.shutdown()
    }

    def "request with path"() {
        given:
        def mockWebServer = new MockWebServer()
        mockWebServer.enqueue(new MockResponse().setBody("mock-response"))
        mockWebServer.start()

        def serverUrl = mockWebServer.url("/")
        def hasVerified = false
        Closure<Boolean> verifyResponse = { Response response, Interceptor.Chain chain ->
            def expectedUrl = serverUrl.newBuilder()
                    .encodedPath("/a-resource")
                    .build()
            if (!expectedUrl.equals(chain.request().url())) {
                throw new AssertionError("expected ${expectedUrl}, got ${chain.request().url()}")
            }
            hasVerified = true
            true
        }
        def client = new OkDockerClient() {
            @Override
            OkHttpClient newClient(OkHttpClient.Builder clientBuilder) {
                clientBuilder
                        .addNetworkInterceptor(new TestInterceptor({ true }, verifyResponse))
                        .build()
            }
        }
        client.dockerClientConfig.apply(new DockerEnv(
                dockerHost: mockWebServer.url("/").toString()))

        when:
        def response = client.request([method: "OPTIONS",
                                       path  : "/a-resource"])
        then:
        response.status.success
        and:
        hasVerified

        cleanup:
        mockWebServer.shutdown()
    }

    def "request with explicit api version"() {
        given:
        def mockWebServer = new MockWebServer()
        mockWebServer.enqueue(new MockResponse().setBody("mock-response"))
        mockWebServer.start()

        def serverUrl = mockWebServer.url("/")
        def hasVerified = false
        Closure<Boolean> verifyResponse = { Response response, Interceptor.Chain chain ->
            def expectedUrl = serverUrl.newBuilder()
                    .encodedPath("/v1.23/a-resource")
                    .build()
            if (!expectedUrl.equals(chain.request().url())) {
                throw new AssertionError("expected ${expectedUrl}, got ${chain.request().url()}")
            }
            hasVerified = true
            true
        }
        def client = new OkDockerClient() {
            @Override
            OkHttpClient newClient(OkHttpClient.Builder clientBuilder) {
                clientBuilder
                        .addNetworkInterceptor(new TestInterceptor({ true }, verifyResponse))
                        .build()
            }
        }
        client.dockerClientConfig.apply(new DockerEnv(
                dockerHost: mockWebServer.url("/").toString()))

        when:
        def response = client.request([method    : "OPTIONS",
                                       path      : "/a-resource",
                                       apiVersion: "v1.23"])
        then:
        response.status.success
        and:
        hasVerified

        cleanup:
        mockWebServer.shutdown()
    }

    def "request with path and query"() {
        given:
        def mockWebServer = new MockWebServer()
        mockWebServer.enqueue(new MockResponse().setBody("mock-response"))
        mockWebServer.start()

        def serverUrl = mockWebServer.url("/")
        def hasVerified = false
        Closure<Boolean> verifyResponse = { Response response, Interceptor.Chain chain ->
            def expectedUrl = serverUrl.newBuilder()
                    .encodedPath("/a-resource")
                    .encodedQuery("baz=la%2Fla&answer=42")
                    .build()
            if (!expectedUrl.equals(chain.request().url())) {
                throw new AssertionError("expected ${expectedUrl}, got ${chain.request().url()}")
            }
            hasVerified = true
            true
        }
        def client = new OkDockerClient() {
            @Override
            OkHttpClient newClient(OkHttpClient.Builder clientBuilder) {
                clientBuilder
                        .addNetworkInterceptor(new TestInterceptor({ true }, verifyResponse))
                        .build()
            }
        }
        client.dockerClientConfig.apply(new DockerEnv(
                dockerHost: mockWebServer.url("/").toString()))

        when:
        def response = client.request([method: "OPTIONS",
                                       path  : "/a-resource",
                                       query : [baz: "la/la", answer: 42]])
        then:
        response.status.success
        and:
        hasVerified

        cleanup:
        mockWebServer.shutdown()
    }

    def "connect via plain http connection"() {
        given:
        def mockWebServer = new MockWebServer()
        mockWebServer.enqueue(new MockResponse().setBody("mock-response"))
        mockWebServer.start()

        def hasVerified = false
        Closure<Boolean> verifyResponse = { Response response, Interceptor.Chain chain ->
            if (chain.connection()?.socket() instanceof SSLSocket) {
                throw new AssertionError("didn't expect a SSLSocket, got ${chain.connection()?.socket()}")
            }
            hasVerified = true
            true
        }
        def client = new OkDockerClient() {
            @Override
            OkHttpClient newClient(OkHttpClient.Builder clientBuilder) {
                clientBuilder
                        .addNetworkInterceptor(new TestInterceptor({ true }, verifyResponse))
                        .build()
            }
        }
        client.dockerClientConfig.apply(new DockerEnv(
                dockerHost: mockWebServer.url("/").toString()))

        when:
        def response = client.request([method: "OPTIONS",
                                       path  : "/a-resource"])
        then:
        response.status.success
        and:
        hasVerified

        cleanup:
        mockWebServer.shutdown()
    }

    def "connect via https connection"() {
        given:
        def certsPath = IOUtils.getResource("/certs").file
        def oldDockerCertPath = System.setProperty("docker.cert.path", certsPath)

        def mockWebServer = new MockWebServer()
        mockWebServer.useHttps(new SslSocketConfigFactory().createDockerSslSocket(certsPath).sslSocketFactory, false)
        mockWebServer.enqueue(new MockResponse().setBody("mock-response"))
        mockWebServer.start()

        def hasVerified = false
        Closure<Boolean> verifyResponse = { Response response, Interceptor.Chain chain ->
            if (!(chain.connection()?.socket() instanceof SSLSocket)) {
                throw new AssertionError("expected a SSLSocket, got ${chain.connection()?.socket()}")
            }
            hasVerified = true
            true
        }
        def client = new OkDockerClient() {
            @Override
            OkHttpClient newClient(OkHttpClient.Builder clientBuilder) {
                clientBuilder
                        .hostnameVerifier(new LocalhostHostnameVerifier())
                        .addNetworkInterceptor(new TestInterceptor({ true }, verifyResponse))
                        .build()
            }
        }
        client.dockerClientConfig.apply(new DockerEnv(
                dockerHost: mockWebServer.url("/").toString(),
                tlsVerify: "1"))

        when:
        def response = client.request([method: "OPTIONS",
                                       path  : "/a-resource"])

        then:
        response.status.success
        and:
        hasVerified

        cleanup:
        mockWebServer.shutdown()
        if (oldDockerCertPath) {
            System.setProperty("docker.cert.path", oldDockerCertPath)
        } else {
            System.clearProperty("docker.cert.path")
        }
    }

    def "request should return statusLine"() {
        given:
        def code = statusCode
        def message = statusMessage
        def mediaType = MediaType.parse("text/plain")
        def responseBody = "holy ship"
        def client = new OkDockerClient() {
            @Override
            OkHttpClient newClient(OkHttpClient.Builder clientBuilder) {
                clientBuilder
                        .addInterceptor(new ConstantResponseInterceptor(code, message, ResponseBody.create(mediaType, responseBody)))
                        .build()
            }
        }
        client.dockerClientConfig.apply(new DockerEnv(dockerHost: "http://127.0.0.1:2375"))

        when:
        def response = client.request([method: "OPTIONS",
                                       path  : "/a-resource"])

        then:
        response.status == expectedStatusLine

        where:
        statusMessage           | statusCode | expectedStatusLine
        "Continue"              | 100        | [text: "Continue", code: 100, success: false]
        "OK"                    | 200        | [text: "OK", code: 200, success: true]
        "No Content"            | 204        | [text: "No Content", code: 204, success: true]
        "Found"                 | 302        | [text: "Found", code: 302, success: false]
        "Not Found"             | 404        | [text: "Not Found", code: 404, success: false]
        "Internal Server Error" | 500        | [text: "Internal Server Error", code: 500, success: false]
    }

    def "request should return headers"() {
        given:
        def mediaType = MediaType.parse("text/plain")
        def responseBody = "holy ship"
        def client = new OkDockerClient() {
            @Override
            OkHttpClient newClient(OkHttpClient.Builder clientBuilder) {
                clientBuilder
                        .addInterceptor(new ConstantResponseInterceptor(ResponseBody.create(mediaType, responseBody)))
                        .build()
            }
        }
        client.dockerClientConfig.apply(new DockerEnv(dockerHost: "http://127.0.0.1:2375"))

        when:
        def response = client.request([method: "HEADER",
                                       path  : "/a-resource"])

        then:
        response.headers['Content-Type'] == "text/plain; charset=utf-8"
        response.headers['Content-Length'] == "9"
        and:
        response.contentType == "text/plain; charset=utf-8"
        and:
        response.mimeType == "text/plain"
        and:
        response.contentLength == "9"
    }

    def "request should return consumed content"() {
        given:
        def mediaType = MediaType.parse("text/plain")
        def responseBody = "holy ship"
        def client = new OkDockerClient() {
            @Override
            OkHttpClient newClient(OkHttpClient.Builder clientBuilder) {
                clientBuilder
                        .addInterceptor(new ConstantResponseInterceptor(ResponseBody.create(mediaType, responseBody)))
                        .build()
            }
        }
        client.dockerClientConfig.apply(new DockerEnv(dockerHost: "http://127.0.0.1:2375"))

        when:
        def response = client.request([method: "HEADER",
                                       path  : "/a-resource"])

        then:
        response.stream == null
        and:
        response.content == "holy ship"
    }

    def "request with stdout stream and known content length"() {
        given:
        def mediaType = MediaType.parse("text/html")
        def responseBody = "holy ship"
        def client = new OkDockerClient() {
            @Override
            OkHttpClient newClient(OkHttpClient.Builder clientBuilder) {
                clientBuilder
                        .addInterceptor(new ConstantResponseInterceptor(ResponseBody.create(mediaType, responseBody)))
                        .build()
            }
        }
        client.dockerClientConfig.apply(new DockerEnv(dockerHost: "http://127.0.0.1:2375"))
        def stdout = new ByteArrayOutputStream()

        when:
        def response = client.request([method: "HEADER",
                                       path  : "/a-resource",
                                       stdout: stdout])

        then:
        stdout.toByteArray() == "holy ship".bytes
        and:
        response.stream == null
    }

    def "request with stdout stream and unknown content length"() {
        given:
        def mediaType = MediaType.parse("text/html")
        def responseBody = "holy ship"
        def client = new OkDockerClient() {
            @Override
            OkHttpClient newClient(OkHttpClient.Builder clientBuilder) {
                clientBuilder
                        .addInterceptor(new ConstantResponseInterceptor(ResponseBody.create(mediaType, -1, new Buffer().write(responseBody.bytes))))
                        .build()
            }
        }
        client.dockerClientConfig.apply(new DockerEnv(dockerHost: "http://127.0.0.1:2375"))

        when:
        def response = client.request([method: "HEADER",
                                       path  : "/a-resource"])

        then:
        response.stream.available() == "holy ship".length()
        and:
        IOUtils.toString(response.stream as InputStream) == "holy ship"
    }

    def "request with unknown mime type"() {
        given:
        def mediaType = MediaType.parse("unknown/mime-type")
        def responseBody = "holy ship"
        def client = new OkDockerClient() {
            @Override
            OkHttpClient newClient(OkHttpClient.Builder clientBuilder) {
                clientBuilder
                        .addInterceptor(new ConstantResponseInterceptor(ResponseBody.create(mediaType, -1, new Buffer().write(responseBody.bytes))))
                        .build()
            }
        }
        client.dockerClientConfig.apply(new DockerEnv(dockerHost: "http://127.0.0.1:2375"))

        when:
        def response = client.request([method: "HEADER",
                                       path  : "/a-resource"])

        then:
        response.stream.available() == "holy ship".length()
        and:
        IOUtils.toString(response.stream as InputStream) == "holy ship"
    }

    def "request with unknown mime type and stdout"() {
        given:
        def mediaType = MediaType.parse("unknown/mime-type")
        def responseBody = "holy ship"
        def client = new OkDockerClient() {
            @Override
            OkHttpClient newClient(OkHttpClient.Builder clientBuilder) {
                clientBuilder
                        .addInterceptor(new ConstantResponseInterceptor(ResponseBody.create(mediaType, -1, new Buffer().write(responseBody.bytes))))
                        .build()
            }
        }
        client.dockerClientConfig.apply(new DockerEnv(dockerHost: "http://127.0.0.1:2375"))
        def stdout = new ByteArrayOutputStream()

        when:
        def response = client.request([method: "HEADER",
                                       path  : "/a-resource",
                                       stdout: stdout])

        then:
        stdout.toByteArray() == "holy ship".bytes
        and:
        response.stream == null
    }

    def "request with json response"() {
        given:
        def mediaType = MediaType.parse("application/json")
        def responseBody = '{"holy":"ship"}'
        def client = new OkDockerClient() {
            @Override
            OkHttpClient newClient(OkHttpClient.Builder clientBuilder) {
                clientBuilder
                        .addInterceptor(new ConstantResponseInterceptor(ResponseBody.create(mediaType, responseBody)))
                        .build()
            }
        }
        client.dockerClientConfig.apply(new DockerEnv(dockerHost: "http://127.0.0.1:2375"))

        when:
        def response = client.request([method: "HEADER",
                                       path  : "/a-resource"])

        then:
        response.stream == null
        and:
        response.content == [holy: 'ship']
    }

    def "request with docker raw-stream response"() {
        given:
        def actualText = "holy ship"
        def headerAndPayload = new RawHeaderAndPayload(STDOUT, actualText.bytes)
        def responseBody = new ByteArrayInputStream(headerAndPayload.bytes as byte[])
        def mediaType = MediaType.parse("application/vnd.docker.raw-stream")
        def client = new OkDockerClient() {
            @Override
            OkHttpClient newClient(OkHttpClient.Builder clientBuilder) {
                clientBuilder
                        .addInterceptor(new ConstantResponseInterceptor(ResponseBody.create(mediaType, responseBody.bytes)))
                        .build()
            }
        }
        client.dockerClientConfig.apply(new DockerEnv(dockerHost: "http://127.0.0.1:2375"))

        when:
        def response = client.request([method: "HEADER",
                                       path  : "/a-resource"])

        then:
        response.stream instanceof RawInputStream
        and:
        IOUtils.toString(response.stream as RawInputStream) == actualText
        and:
        response.content == null
    }

    def "request with docker raw-stream response on stdout"() {
        given:
        def actualText = "holy ship"
        def headerAndPayload = new RawHeaderAndPayload(STDOUT, actualText.bytes)
        def responseBody = new ByteArrayInputStream(headerAndPayload.bytes as byte[])
        def mediaType = MediaType.parse("application/vnd.docker.raw-stream")
        def client = new OkDockerClient() {
            @Override
            OkHttpClient newClient(OkHttpClient.Builder clientBuilder) {
                clientBuilder
                        .addInterceptor(new ConstantResponseInterceptor(ResponseBody.create(mediaType, responseBody.bytes)))
                        .build()
            }
        }
        client.dockerClientConfig.apply(new DockerEnv(dockerHost: "https://127.0.0.1:2376"))
        client.socketFactories["https"] = new SslSocketConfigFactory() {
            @Override
            DockerSslSocket createDockerSslSocket(String certPath) {
                return null
            }
        }
        def stdout = new ByteArrayOutputStream()

        when:
        def response = client.request([method: "HEADER",
                                       path  : "/a-resource",
                                       stdout: stdout])

        then:
        stdout.toByteArray() == actualText.bytes
        and:
        responseBody.available() == 0
        and:
        response.stream == null
        and:
        response.content == null
    }

    static class TestContentHandlerFactory implements ContentHandlerFactory {

        def contentHandler = null

        @Override
        ContentHandler createContentHandler(String mimetype) {
            return contentHandler
        }
    }

    static class TestContentHandler extends ContentHandler {

        def result = null

        @Override
        Object getContent(URLConnection urlc) throws IOException {
            return result
        }
    }

    static class ConstantResponseInterceptor implements Interceptor {
        int statusCode
        String statusMessage
        ResponseBody responseBody

        ConstantResponseInterceptor(ResponseBody responseBody) {
            this(200, "OK", responseBody)
        }

        ConstantResponseInterceptor(int statusCode, String statusMessage, ResponseBody responseBody) {
            this.statusCode = statusCode
            this.statusMessage = statusMessage
            this.responseBody = responseBody
        }

        @Override
        Response intercept(Interceptor.Chain chain) throws IOException {
            new Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(statusCode)
                    .message(statusMessage)
                    .body(responseBody)
                    .addHeader("Content-Type", responseBody.contentType().toString())
                    .addHeader("Content-Length", Long.toString(responseBody.contentLength()))
                    .build()
        }
    }

    static class TestInterceptor implements Interceptor {
        Closure<Boolean> requestVerifier
        Closure<Boolean> responseVerifier

        int statusCode
        String statusMessage
        ResponseBody responseBody

        TestInterceptor(Closure<Boolean> requestVerifier, Closure<Boolean> responseVerifier) {
            this.requestVerifier = requestVerifier
            this.responseVerifier = responseVerifier
            this.statusCode = 200
            this.statusMessage = "OK"
            this.responseBody = ResponseBody.create(MediaType.parse("text/plain"), "ok by ${getClass().simpleName}")
        }

        @Override
        Response intercept(Interceptor.Chain chain) throws IOException {
            requestVerifier(chain)
            def response = chain.proceed(chain.request())
            responseVerifier(response, chain)
            return response
        }
    }

    static class LocalhostHostnameVerifier implements HostnameVerifier {
        @Override
        boolean verify(String host, SSLSession sslSession) {
            return host == "localhost"
        }
    }
}
