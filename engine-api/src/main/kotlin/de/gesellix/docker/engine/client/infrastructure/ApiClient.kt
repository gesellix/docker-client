package de.gesellix.docker.engine.client.infrastructure

import de.gesellix.docker.client.filesocket.NamedPipeSocket
import de.gesellix.docker.client.filesocket.NamedPipeSocketFactory
import de.gesellix.docker.client.filesocket.UnixSocket
import de.gesellix.docker.client.filesocket.UnixSocketFactory
import de.gesellix.docker.client.filesocket.UnixSocketFactorySupport
import de.gesellix.docker.engine.DockerClientConfig
import de.gesellix.docker.engine.EngineRequest
import de.gesellix.docker.engine.RequestMethod.DELETE
import de.gesellix.docker.engine.RequestMethod.GET
import de.gesellix.docker.engine.RequestMethod.HEAD
import de.gesellix.docker.engine.RequestMethod.OPTIONS
import de.gesellix.docker.engine.RequestMethod.PATCH
import de.gesellix.docker.engine.RequestMethod.POST
import de.gesellix.docker.engine.RequestMethod.PUT
import de.gesellix.docker.engine.api.Frame
import de.gesellix.docker.ssl.SslSocketConfigFactory
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okio.Source
import java.io.File
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

open class ApiClient(val baseUrl: String, val dockerClientConfig: DockerClientConfig = DockerClientConfig()) {
  companion object {

    protected const val ContentType = "Content-Type"
    protected const val Accept = "Accept"
    protected const val Authorization = "Authorization"
    protected const val TextPlainMediaType = "text/plain"
    protected const val JsonMediaType = "application/json"
    protected const val OctetStreamMediaType = "application/octet-stream"
    protected const val DockerRawStreamMediaType = "application/vnd.docker.raw-stream"

    //    val apiKey: MutableMap<String, String> = mutableMapOf()
//    val apiKeyPrefix: MutableMap<String, String> = mutableMapOf()
    var username: String? = null
    var password: String? = null
//    var accessToken: String? = null

    val socketFactories: MutableMap<String, (OkHttpClient.Builder) -> OkHttpClient.Builder> = mutableMapOf()

//    @JvmStatic
//    val engineClient: EngineClient by lazy {
//      OkDockerClient()
//    }

    @JvmStatic
    val client: OkHttpClient by lazy {
      builder.build()
    }

    @JvmStatic
    val builder: OkHttpClient.Builder = OkHttpClient.Builder()
  }

  protected inline fun <reified T> requestBody(content: T, mediaType: String = JsonMediaType): RequestBody =
    when {
      content is File -> content.asRequestBody(
        mediaType.toMediaTypeOrNull()
      )
      content is Source -> content.asRequestBody(
        mediaType.toMediaTypeOrNull()
      )
      mediaType == JsonMediaType -> Serializer.moshi.adapter(T::class.java).toJson(content).toRequestBody(
        mediaType.toMediaTypeOrNull()
      )
      else -> throw UnsupportedOperationException("requestBody only supports JSON body and File body, not $mediaType.")
    }

  protected inline fun <reified T : Any?> responseBody(
    body: ResponseBody?,
    mediaType: String? = JsonMediaType,
    type: Type? = null
  ): T? {
    if (body == null) {
      return null
    }
//    val bodyContent = body.string()
//    if (bodyContent.isEmpty()) {
//      return null
//    }
//    if (mediaType == null && body.contentLength() == 0L) {
//      return null
//    }
    if (T::class.java == File::class.java) {
      return body.consumeFile() as T
    }
    return when (mediaType) {
      JsonMediaType -> when (type) {
        null -> body.consumeJson()
        else -> body.consumeJson(type)
      }
      TextPlainMediaType -> body.string() as T
      null -> {
        body.close()
        null
      }
      else -> throw UnsupportedOperationException("responseBody currently does not support media type $mediaType.")
    }
  }

  protected inline fun <reified T : Any?> request(requestConfig: RequestConfig): ApiInfrastructureResponse<T?> {
    val engineRequest = EngineRequest(requestConfig.method, requestConfig.path).also {
      it.headers = requestConfig.headers
      it.query = requestConfig.query
      it.body = requestConfig.body
    }
    val request = prepareRequest(engineRequest)
    val client = prepareClient(engineRequest)
    return request<T>(request, client, requestConfig.elementType)
  }

  protected inline fun <reified T : Any?> requestStream(requestConfig: RequestConfig): ApiInfrastructureResponse<T?> {
    val engineRequest = EngineRequest(requestConfig.method, requestConfig.path).also {
      it.headers = requestConfig.headers
      it.query = requestConfig.query
      it.body = requestConfig.body
    }
    val request = prepareRequest(engineRequest, JsonMediaType)
    val client = prepareClient(engineRequest)
    return requestStream(request, client)
  }

  protected inline fun requestFrames(requestConfig: RequestConfig, expectMultiplexedResponse: Boolean = false): ApiInfrastructureResponse<Frame> {
    val engineRequest = EngineRequest(requestConfig.method, requestConfig.path).also {
      it.headers = requestConfig.headers
      it.query = requestConfig.query
      it.body = requestConfig.body
    }
    val request = prepareRequest(engineRequest, DockerRawStreamMediaType)
    val client = prepareClient(engineRequest)
    return requestFrames(request, client, expectMultiplexedResponse)
  }

  protected fun prepareRequest(requestConfig: EngineRequest, fallbackContentType: String = ""): Request {
    val httpUrl = buildHttpUrl().build()

    val pathWithOptionalApiVersion = when {
      requestConfig.apiVersion != null -> {
        requestConfig.apiVersion + "/" + requestConfig.path
      }
      else -> {
        requestConfig.path
      }
    }

    val url = httpUrl.newBuilder()
      .addPathSegments(pathWithOptionalApiVersion.trimStart('/'))
      .apply {
        requestConfig.query.forEach { query ->
          query.value.forEach { queryValue ->
            addQueryParameter(query.key, queryValue)
          }
        }
      }.build()

    // take content-type/accept from spec or set to default (application/json) if not defined
    if (requestConfig.headers[ContentType].isNullOrEmpty()) {
      requestConfig.headers[ContentType] = JsonMediaType
    }
    if (requestConfig.headers[Accept].isNullOrEmpty()) {
      requestConfig.headers[Accept] = JsonMediaType
    }
    val headers = requestConfig.headers

    if ((headers[ContentType] ?: "") == "") {
      throw IllegalStateException("Missing Content-Type header. This is required.")
    }

    if ((headers[Accept] ?: "") == "") {
      throw IllegalStateException("Missing Accept header. This is required.")
    }

    // TODO: support multiple contentType options here.
    val mediaType = (headers[ContentType] as String).substringBefore(";").toLowerCase()

    val request = when (requestConfig.method) {
      DELETE -> Request.Builder().url(url).delete(requestBody(requestConfig.body, mediaType))
      GET -> Request.Builder().url(url)
      HEAD -> Request.Builder().url(url).head()
      PATCH -> Request.Builder().url(url).patch(requestBody(requestConfig.body, mediaType))
      PUT -> Request.Builder().url(url).put(requestBody(requestConfig.body, mediaType))
      POST -> Request.Builder().url(url).post(requestBody(requestConfig.body, mediaType))
      OPTIONS -> Request.Builder().url(url).method("OPTIONS", null)
      null -> throw IllegalStateException("Request method is null")
    }.apply {
      headers.forEach { header -> addHeader(header.key, header.value) }
    }.apply {
      tag(EnforceResponseContentTypeConfig::class.java, EnforceResponseContentTypeConfig(fallbackContentType))
    }.build()
    return request
  }

  protected fun prepareClient(requestConfig: EngineRequest): OkHttpClient {
//    val engineResponse = engineClient.request(requestConfig)
    val actualClient = buildHttpClient(client.newBuilder())
      //      .proxy(proxy) // TODO
      // do we need to disable the timeout for streaming?
      .connectTimeout(requestConfig.timeout.toLong(), TimeUnit.MILLISECONDS)
      .readTimeout(requestConfig.timeout.toLong(), TimeUnit.MILLISECONDS)
      .addInterceptor(EnforceResponseContentTypeInterceptor())
    return actualClient.build()
  }

  protected inline fun <reified T : Any?> request(request: Request, client: OkHttpClient, elementType: Type? = null): ApiInfrastructureResponse<T?> {
    val response = client.newCall(request).execute()
    val mediaType = response.header(ContentType)?.substringBefore(";")?.toLowerCase()

    // TODO: handle specific mapping types. e.g. Map<int, Class<?>>
    when {
      response.isRedirect -> return Redirection(
        response.code,
        response.headers.toMultimap()
      )
      response.isInformational -> return Informational(
        response.message,
        response.code,
        response.headers.toMultimap()
      )
      response.isSuccessful -> return Success(
        responseBody(response.body, mediaType, elementType),
        response.code,
        response.headers.toMultimap()
      )
      response.isClientError -> return ClientError(
        response.message,
        response.body?.string(),
        response.code,
        response.headers.toMultimap()
      )
      else -> return ServerError(
        response.message,
        response.body?.string(),
        response.code,
        response.headers.toMultimap()
      )
    }
  }

  protected inline fun <reified T : Any?> requestStream(request: Request, client: OkHttpClient): ApiInfrastructureResponse<T?> {
    val response = client.newCall(request).execute()
    val mediaType = response.header(ContentType)?.substringBefore(";")?.toLowerCase()

    // TODO: handle specific mapping types. e.g. Map<int, Class<?>>
    when {
      response.isRedirect -> return Redirection(
        response.code,
        response.headers.toMultimap()
      )
      response.isInformational -> return Informational(
        response.message,
        response.code,
        response.headers.toMultimap()
      )
      response.isSuccessful -> return SuccessStream(
        response.body.consumeStream(mediaType),
        response.code,
        response.headers.toMultimap()
      )
      response.isClientError -> return ClientError(
        response.message,
        response.body?.string(),
        response.code,
        response.headers.toMultimap()
      )
      else -> return ServerError(
        response.message,
        response.body?.string(),
        response.code,
        response.headers.toMultimap()
      )
    }
  }

  protected inline fun requestFrames(request: Request, client: OkHttpClient, expectMultiplexedResponse: Boolean = false): ApiInfrastructureResponse<Frame> {
    val response = client.newCall(request).execute()
    val mediaType = response.header(ContentType)?.substringBefore(";")?.toLowerCase()

    // TODO: handle specific mapping types. e.g. Map<int, Class<?>>
    when {
      response.isRedirect -> return Redirection(
        response.code,
        response.headers.toMultimap()
      )
      response.isInformational -> return Informational(
        response.message,
        response.code,
        response.headers.toMultimap()
      )
      response.isSuccessful -> return SuccessStream(
        response.body.consumeFrames(mediaType, expectMultiplexedResponse),
        response.code,
        response.headers.toMultimap()
      )
      response.isClientError -> return ClientError(
        response.message,
        response.body?.string(),
        response.code,
        response.headers.toMultimap()
      )
      else -> return ServerError(
        response.message,
        response.body?.string(),
        response.code,
        response.headers.toMultimap()
      )
    }
  }

  open fun buildHttpUrl(): HttpUrl.Builder {
//    baseUrl.toHttpUrlOrNull() ?: throw IllegalStateException("baseUrl is invalid.")
    return when (dockerClientConfig.scheme) {
      "unix" -> HttpUrl.Builder()
        .scheme("http")
        .host(UnixSocket().encodeHostname(dockerClientConfig.host))
      // port(/not/allowed/for/unix/socket/)
      "npipe" -> HttpUrl.Builder()
        .scheme("http")
        .host(NamedPipeSocket().encodeHostname(dockerClientConfig.host))
      // port(/not/allowed/for/npipe/socket/)
      else -> HttpUrl.Builder()
        .scheme(dockerClientConfig.scheme)
        .host(dockerClientConfig.host)
        .port(dockerClientConfig.port)
    }
  }

  open fun buildHttpClient(builder: OkHttpClient.Builder): OkHttpClient.Builder {
    val protocol = dockerClientConfig.scheme
    val socketFactoryConfiguration = socketFactories[protocol]
    if (socketFactoryConfiguration != null) {
      return socketFactoryConfiguration(builder)
    }
    throw IllegalStateException("$protocol socket not supported.")
  }

  init {
    if (UnixSocketFactorySupport().isSupported) {
      socketFactories["unix"] = { builder ->
        val factory = UnixSocketFactory()
        builder
          .socketFactory(factory)
          .dns(factory)
      }
    }
    socketFactories["npipe"] = { builder ->
      val factory = NamedPipeSocketFactory()
      builder
        .socketFactory(factory)
        .dns(factory)
    }
    socketFactories["https"] = { builder ->
      val dockerSslSocket = SslSocketConfigFactory().createDockerSslSocket(dockerClientConfig.certPath)
      builder
        .sslSocketFactory(dockerSslSocket.sslSocketFactory, dockerSslSocket.trustManager)
    }
  }
}
