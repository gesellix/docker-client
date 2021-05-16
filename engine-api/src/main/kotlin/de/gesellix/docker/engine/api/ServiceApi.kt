/**
 * Docker Engine API
 * The Engine API is an HTTP API served by Docker Engine. It is the API the Docker client uses to communicate with the Engine, so everything the Docker client can do can be done with the API.  Most of the client's commands map directly to API endpoints (e.g. `docker ps` is `GET /containers/json`). The notable exception is running containers, which consists of several API calls.  # Errors  The API uses standard HTTP status codes to indicate the success or failure of the API call. The body of the response will be JSON in the following format:  ``` {   \"message\": \"page not found\" } ```  # Versioning  The API is usually changed in each release, so API calls are versioned to ensure that clients don't break. To lock to a specific version of the API, you prefix the URL with its version, for example, call `/v1.30/info` to use the v1.30 version of the `/info` endpoint. If the API version specified in the URL is not supported by the daemon, a HTTP `400 Bad Request` error message is returned.  If you omit the version-prefix, the current version of the API (v1.41) is used. For example, calling `/info` is the same as calling `/v1.41/info`. Using the API without a version-prefix is deprecated and will be removed in a future release.  Engine releases in the near future should support this version of the API, so your client will continue to work even if it is talking to a newer Engine.  The API uses an open schema model, which means server may add extra properties to responses. Likewise, the server will ignore any extra query parameters and request body properties. When you write clients, you need to ignore additional properties in responses to ensure they do not break when talking to newer daemons.   # Authentication  Authentication for registries is handled client side. The client has to send authentication details to various endpoints that need to communicate with registries, such as `POST /images/(name)/push`. These are sent as `X-Registry-Auth` header as a [base64url encoded](https://tools.ietf.org/html/rfc4648#section-5) (JSON) string with the following structure:  ``` {   \"username\": \"string\",   \"password\": \"string\",   \"email\": \"string\",   \"serveraddress\": \"string\" } ```  The `serveraddress` is a domain/IP without a protocol. Throughout this structure, double quotes are required.  If you have already got an identity token from the [`/auth` endpoint](#operation/SystemAuth), you can just pass this instead of credentials:  ``` {   \"identitytoken\": \"9cbaf023786cd7...\" } ```
 *
 * The version of the OpenAPI document: 1.41
 *
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package de.gesellix.docker.engine.api

import de.gesellix.docker.engine.RequestMethod.DELETE
import de.gesellix.docker.engine.RequestMethod.GET
import de.gesellix.docker.engine.RequestMethod.POST
import de.gesellix.docker.engine.client.infrastructure.ApiClient
import de.gesellix.docker.engine.client.infrastructure.ClientError
import de.gesellix.docker.engine.client.infrastructure.ClientException
import de.gesellix.docker.engine.client.infrastructure.MultiValueMap
import de.gesellix.docker.engine.client.infrastructure.RequestConfig
import de.gesellix.docker.engine.client.infrastructure.ResponseType
import de.gesellix.docker.engine.client.infrastructure.ServerError
import de.gesellix.docker.engine.client.infrastructure.ServerException
import de.gesellix.docker.engine.client.infrastructure.Success
import de.gesellix.docker.engine.client.infrastructure.SuccessStream
import de.gesellix.docker.engine.model.Service
import de.gesellix.docker.engine.model.ServiceCreateResponse
import de.gesellix.docker.engine.model.ServiceSpec
import de.gesellix.docker.engine.model.ServiceUpdateResponse
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.net.HttpURLConnection

class ServiceApi(basePath: String = defaultBasePath) : ApiClient(basePath) {
  companion object {

    @JvmStatic
    val defaultBasePath: String by lazy {
      System.getProperties().getProperty("docker.client.baseUrl", "http://localhost/v1.41")
    }
  }

  /**
   * Create a service
   *
   * @param body
   * @param xRegistryAuth A base64url-encoded auth configuration for pulling from private registries.  Refer to the [authentication section](#section/Authentication) for details.  (optional)
   * @return ServiceCreateResponse
   * @throws UnsupportedOperationException If the API returns an informational or redirection response
   * @throws ClientException If the API returns a client error response
   * @throws ServerException If the API returns a server error response
   */
  @Suppress("UNCHECKED_CAST")
  @Throws(UnsupportedOperationException::class, ClientException::class, ServerException::class)
  fun serviceCreate(body: ServiceSpec, xRegistryAuth: String?): ServiceCreateResponse {
    val localVariableConfig = serviceCreateRequestConfig(body = body, xRegistryAuth = xRegistryAuth)

    val localVarResponse = request<ServiceCreateResponse>(
      localVariableConfig
    )

    return when (localVarResponse.responseType) {
      ResponseType.Success -> (localVarResponse as Success<*>).data as ServiceCreateResponse
      ResponseType.Informational -> throw UnsupportedOperationException("Client does not support Informational responses.")
      ResponseType.Redirection -> throw UnsupportedOperationException("Client does not support Redirection responses.")
      ResponseType.ClientError -> {
        val localVarError = localVarResponse as ClientError<*>
        throw ClientException("Client error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
      }
      ResponseType.ServerError -> {
        val localVarError = localVarResponse as ServerError<*>
        throw ServerException("Server error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
      }
    }
  }

  /**
   * To obtain the request config of the operation serviceCreate
   *
   * @param body
   * @param xRegistryAuth A base64url-encoded auth configuration for pulling from private registries.  Refer to the [authentication section](#section/Authentication) for details.  (optional)
   * @return RequestConfig
   */
  fun serviceCreateRequestConfig(body: ServiceSpec, xRegistryAuth: String?): RequestConfig {
    val localVariableBody: Any? = body
    val localVariableQuery: MultiValueMap = mutableMapOf()
    val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
    xRegistryAuth?.apply { localVariableHeaders["X-Registry-Auth"] = this }

    return RequestConfig(
      method = POST,
      path = "/services/create",
      query = localVariableQuery,
      headers = localVariableHeaders,
      body = localVariableBody
    )
  }

  /**
   * Delete a service
   *
   * @param id ID or name of service.
   * @return void
   * @throws UnsupportedOperationException If the API returns an informational or redirection response
   * @throws ClientException If the API returns a client error response
   * @throws ServerException If the API returns a server error response
   */
  @Throws(UnsupportedOperationException::class, ClientException::class, ServerException::class)
  fun serviceDelete(id: String) {
    val localVariableConfig = serviceDeleteRequestConfig(id = id)

    val localVarResponse = request<Any?>(
      localVariableConfig
    )

    return when (localVarResponse.responseType) {
      ResponseType.Success -> Unit
      ResponseType.Informational -> throw UnsupportedOperationException("Client does not support Informational responses.")
      ResponseType.Redirection -> throw UnsupportedOperationException("Client does not support Redirection responses.")
      ResponseType.ClientError -> {
        val localVarError = localVarResponse as ClientError<*>
        if (localVarError.statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
          return
        }
        throw ClientException("Client error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
      }
      ResponseType.ServerError -> {
        val localVarError = localVarResponse as ServerError<*>
        throw ServerException("Server error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
      }
    }
  }

  /**
   * To obtain the request config of the operation serviceDelete
   *
   * @param id ID or name of service.
   * @return RequestConfig
   */
  fun serviceDeleteRequestConfig(id: String): RequestConfig {
    val localVariableBody: Any? = null
    val localVariableQuery: MultiValueMap = mutableMapOf()
    val localVariableHeaders: MutableMap<String, String> = mutableMapOf()

    return RequestConfig(
      method = DELETE,
      path = "/services/{id}".replace("{" + "id" + "}", id),
      query = localVariableQuery,
      headers = localVariableHeaders,
      body = localVariableBody
    )
  }

  /**
   * Inspect a service
   *
   * @param id ID or name of service.
   * @param insertDefaults Fill empty fields with default values. (optional, default to false)
   * @return Service
   * @throws UnsupportedOperationException If the API returns an informational or redirection response
   * @throws ClientException If the API returns a client error response
   * @throws ServerException If the API returns a server error response
   */
  @Suppress("UNCHECKED_CAST")
  @Throws(UnsupportedOperationException::class, ClientException::class, ServerException::class)
  fun serviceInspect(id: String, insertDefaults: Boolean?): Service {
    val localVariableConfig = serviceInspectRequestConfig(id = id, insertDefaults = insertDefaults)

    val localVarResponse = request<Service>(
      localVariableConfig
    )

    return when (localVarResponse.responseType) {
      ResponseType.Success -> (localVarResponse as Success<*>).data as Service
      ResponseType.Informational -> throw UnsupportedOperationException("Client does not support Informational responses.")
      ResponseType.Redirection -> throw UnsupportedOperationException("Client does not support Redirection responses.")
      ResponseType.ClientError -> {
        val localVarError = localVarResponse as ClientError<*>
        throw ClientException("Client error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
      }
      ResponseType.ServerError -> {
        val localVarError = localVarResponse as ServerError<*>
        throw ServerException("Server error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
      }
    }
  }

  /**
   * To obtain the request config of the operation serviceInspect
   *
   * @param id ID or name of service.
   * @param insertDefaults Fill empty fields with default values. (optional, default to false)
   * @return RequestConfig
   */
  fun serviceInspectRequestConfig(id: String, insertDefaults: Boolean?): RequestConfig {
    val localVariableBody: Any? = null
    val localVariableQuery: MultiValueMap = mutableMapOf<String, List<String>>()
      .apply {
        if (insertDefaults != null) {
          put("insertDefaults", listOf(insertDefaults.toString()))
        }
      }
    val localVariableHeaders: MutableMap<String, String> = mutableMapOf()

    return RequestConfig(
      method = GET,
      path = "/services/{id}".replace("{" + "id" + "}", id),
      query = localVariableQuery,
      headers = localVariableHeaders,
      body = localVariableBody
    )
  }

  /**
   * List services
   *
   * @param filters A JSON encoded value of the filters (a &#x60;map[string][]string&#x60;) to process on the services list.  Available filters:  - &#x60;id&#x3D;&lt;service id&gt;&#x60; - &#x60;label&#x3D;&lt;service label&gt;&#x60; - &#x60;mode&#x3D;[\&quot;replicated\&quot;|\&quot;global\&quot;]&#x60; - &#x60;name&#x3D;&lt;service name&gt;&#x60;  (optional)
   * @param status Include service status, with count of running and desired tasks.  (optional)
   * @return kotlin.collections.List<Service>
   * @throws UnsupportedOperationException If the API returns an informational or redirection response
   * @throws ClientException If the API returns a client error response
   * @throws ServerException If the API returns a server error response
   */
  @Suppress("UNCHECKED_CAST")
  @Throws(UnsupportedOperationException::class, ClientException::class, ServerException::class)
  fun serviceList(filters: String?, status: Boolean?): List<Service> {
    val localVariableConfig = serviceListRequestConfig(filters = filters, status = status)

    val localVarResponse = request<List<Service>>(
      localVariableConfig
    )

    return when (localVarResponse.responseType) {
      ResponseType.Success -> (localVarResponse as Success<*>).data as List<Service>
      ResponseType.Informational -> throw UnsupportedOperationException("Client does not support Informational responses.")
      ResponseType.Redirection -> throw UnsupportedOperationException("Client does not support Redirection responses.")
      ResponseType.ClientError -> {
        val localVarError = localVarResponse as ClientError<*>
        throw ClientException("Client error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
      }
      ResponseType.ServerError -> {
        val localVarError = localVarResponse as ServerError<*>
        throw ServerException("Server error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
      }
    }
  }

  /**
   * To obtain the request config of the operation serviceList
   *
   * @param filters A JSON encoded value of the filters (a &#x60;map[string][]string&#x60;) to process on the services list.  Available filters:  - &#x60;id&#x3D;&lt;service id&gt;&#x60; - &#x60;label&#x3D;&lt;service label&gt;&#x60; - &#x60;mode&#x3D;[\&quot;replicated\&quot;|\&quot;global\&quot;]&#x60; - &#x60;name&#x3D;&lt;service name&gt;&#x60;  (optional)
   * @param status Include service status, with count of running and desired tasks.  (optional)
   * @return RequestConfig
   */
  fun serviceListRequestConfig(filters: String?, status: Boolean?): RequestConfig {
    val localVariableBody: Any? = null
    val localVariableQuery: MultiValueMap = mutableMapOf<String, List<String>>()
      .apply {
        if (filters != null) {
          put("filters", listOf(filters.toString()))
        }
        if (status != null) {
          put("status", listOf(status.toString()))
        }
      }
    val localVariableHeaders: MutableMap<String, String> = mutableMapOf()

    return RequestConfig(
      method = GET,
      path = "/services",
      query = localVariableQuery,
      headers = localVariableHeaders,
      body = localVariableBody,
      elementType = Service::class.java
    )
  }

  /**
   * Get service logs
   * Get &#x60;stdout&#x60; and &#x60;stderr&#x60; logs from a service. See also [&#x60;/containers/{id}/logs&#x60;](#operation/ContainerLogs).  **Note**: This endpoint works only for services with the &#x60;local&#x60;, &#x60;json-file&#x60; or &#x60;journald&#x60; logging drivers.
   * @param id ID or name of the service
   * @param details Show service context and extra details provided to logs. (optional, default to false)
   * @param follow Keep connection after returning logs. (optional, default to false)
   * @param stdout Return logs from &#x60;stdout&#x60; (optional, default to false)
   * @param stderr Return logs from &#x60;stderr&#x60; (optional, default to false)
   * @param since Only return logs since this time, as a UNIX timestamp (optional, default to 0)
   * @param timestamps Add timestamps to every log line (optional, default to false)
   * @param tail Only return this number of log lines from the end of the logs. Specify as an integer or &#x60;all&#x60; to output all log lines.  (optional, default to "all")
   * @return java.io.File
   * @throws UnsupportedOperationException If the API returns an informational or redirection response
   * @throws ClientException If the API returns a client error response
   * @throws ServerException If the API returns a server error response
   */
  @Suppress("UNCHECKED_CAST")
  @Throws(UnsupportedOperationException::class, ClientException::class, ServerException::class)
  fun serviceLogs(
    id: String,
    details: Boolean?,
    follow: Boolean?,
    stdout: Boolean?,
    stderr: Boolean?,
    since: Int?,
    timestamps: Boolean?,
    tail: String?,
    callback: StreamCallback<Frame>, timeoutMillis: Long /*= 24.hours.toLongMilliseconds()*/
  ) {
    val localVariableConfig = serviceLogsRequestConfig(id = id, details = details, follow = follow, stdout = stdout, stderr = stderr, since = since, timestamps = timestamps, tail = tail)

    val localVarResponse = requestFrames(
      localVariableConfig, true /* do services/tasks always have container.tty == false? */
    )

    when (localVarResponse.responseType) {
      ResponseType.Success -> {
        runBlocking {
          launch {
            withTimeout(timeoutMillis) {
              callback.onStarting(this@launch::cancel)
              ((localVarResponse as SuccessStream<*>).data as Flow<Frame>).collect { callback.onNext(it) }
              callback.onFinished()
            }
          }
        }
      }
      ResponseType.Informational -> throw UnsupportedOperationException("Client does not support Informational responses.")
      ResponseType.Redirection -> throw UnsupportedOperationException("Client does not support Redirection responses.")
      ResponseType.ClientError -> {
        val localVarError = localVarResponse as ClientError<*>
        throw ClientException("Client error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
      }
      ResponseType.ServerError -> {
        val localVarError = localVarResponse as ServerError<*>
        throw ServerException("Server error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
      }
    }
  }

  /**
   * To obtain the request config of the operation serviceLogs
   *
   * @param id ID or name of the service
   * @param details Show service context and extra details provided to logs. (optional, default to false)
   * @param follow Keep connection after returning logs. (optional, default to false)
   * @param stdout Return logs from &#x60;stdout&#x60; (optional, default to false)
   * @param stderr Return logs from &#x60;stderr&#x60; (optional, default to false)
   * @param since Only return logs since this time, as a UNIX timestamp (optional, default to 0)
   * @param timestamps Add timestamps to every log line (optional, default to false)
   * @param tail Only return this number of log lines from the end of the logs. Specify as an integer or &#x60;all&#x60; to output all log lines.  (optional, default to "all")
   * @return RequestConfig
   */
  fun serviceLogsRequestConfig(
    id: String,
    details: Boolean?,
    follow: Boolean?,
    stdout: Boolean?,
    stderr: Boolean?,
    since: Int?,
    timestamps: Boolean?,
    tail: String?
  ): RequestConfig {
    val localVariableBody: Any? = null
    val localVariableQuery: MultiValueMap = mutableMapOf<String, List<String>>()
      .apply {
        if (details != null) {
          put("details", listOf(details.toString()))
        }
        if (follow != null) {
          put("follow", listOf(follow.toString()))
        }
        if (stdout != null) {
          put("stdout", listOf(stdout.toString()))
        }
        if (stderr != null) {
          put("stderr", listOf(stderr.toString()))
        }
        if (since != null) {
          put("since", listOf(since.toString()))
        }
        if (timestamps != null) {
          put("timestamps", listOf(timestamps.toString()))
        }
        if (tail != null) {
          put("tail", listOf(tail.toString()))
        }
      }
    val localVariableHeaders: MutableMap<String, String> = mutableMapOf()

    return RequestConfig(
      method = GET,
      path = "/services/{id}/logs".replace("{" + "id" + "}", id),
      query = localVariableQuery,
      headers = localVariableHeaders,
      body = localVariableBody
    )
  }

  /**
   * Update a service
   *
   * @param id ID or name of service.
   * @param version The version number of the service object being updated. This is required to avoid conflicting writes. This version number should be the value as currently set on the service *before* the update. You can find the current version by calling &#x60;GET /services/{id}&#x60;
   * @param body
   * @param registryAuthFrom If the &#x60;X-Registry-Auth&#x60; header is not specified, this parameter indicates where to find registry authorization credentials.  (optional, default to spec)
   * @param rollback Set to this parameter to &#x60;previous&#x60; to cause a server-side rollback to the previous service spec. The supplied spec will be ignored in this case.  (optional)
   * @param xRegistryAuth A base64url-encoded auth configuration for pulling from private registries.  Refer to the [authentication section](#section/Authentication) for details.  (optional)
   * @return ServiceUpdateResponse
   * @throws UnsupportedOperationException If the API returns an informational or redirection response
   * @throws ClientException If the API returns a client error response
   * @throws ServerException If the API returns a server error response
   */
  @Suppress("UNCHECKED_CAST")
  @Throws(UnsupportedOperationException::class, ClientException::class, ServerException::class)
  fun serviceUpdate(id: String, version: Int, body: ServiceSpec, registryAuthFrom: String?, rollback: String?, xRegistryAuth: String?): ServiceUpdateResponse {
    val localVariableConfig = serviceUpdateRequestConfig(id = id, version = version, body = body, registryAuthFrom = registryAuthFrom, rollback = rollback, xRegistryAuth = xRegistryAuth)

    val localVarResponse = request<ServiceUpdateResponse>(
      localVariableConfig
    )

    return when (localVarResponse.responseType) {
      ResponseType.Success -> (localVarResponse as Success<*>).data as ServiceUpdateResponse
      ResponseType.Informational -> throw UnsupportedOperationException("Client does not support Informational responses.")
      ResponseType.Redirection -> throw UnsupportedOperationException("Client does not support Redirection responses.")
      ResponseType.ClientError -> {
        val localVarError = localVarResponse as ClientError<*>
        throw ClientException("Client error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
      }
      ResponseType.ServerError -> {
        val localVarError = localVarResponse as ServerError<*>
        throw ServerException("Server error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
      }
    }
  }

  /**
   * To obtain the request config of the operation serviceUpdate
   *
   * @param id ID or name of service.
   * @param version The version number of the service object being updated. This is required to avoid conflicting writes. This version number should be the value as currently set on the service *before* the update. You can find the current version by calling &#x60;GET /services/{id}&#x60;
   * @param body
   * @param registryAuthFrom If the &#x60;X-Registry-Auth&#x60; header is not specified, this parameter indicates where to find registry authorization credentials.  (optional, default to spec)
   * @param rollback Set to this parameter to &#x60;previous&#x60; to cause a server-side rollback to the previous service spec. The supplied spec will be ignored in this case.  (optional)
   * @param xRegistryAuth A base64url-encoded auth configuration for pulling from private registries.  Refer to the [authentication section](#section/Authentication) for details.  (optional)
   * @return RequestConfig
   */
  fun serviceUpdateRequestConfig(
    id: String,
    version: Int,
    body: ServiceSpec,
    registryAuthFrom: String?,
    rollback: String?,
    xRegistryAuth: String?
  ): RequestConfig {
    val localVariableBody: Any? = body
    val localVariableQuery: MultiValueMap = mutableMapOf<String, List<String>>()
      .apply {
        put("version", listOf(version.toString()))
        if (registryAuthFrom != null) {
          put("registryAuthFrom", listOf(registryAuthFrom.toString()))
        }
        if (rollback != null) {
          put("rollback", listOf(rollback.toString()))
        }
      }
    val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
    xRegistryAuth?.apply { localVariableHeaders["X-Registry-Auth"] = this }

    return RequestConfig(
      method = POST,
      path = "/services/{id}/update".replace("{" + "id" + "}", id),
      query = localVariableQuery,
      headers = localVariableHeaders,
      body = localVariableBody
    )
  }
}
