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
import de.gesellix.docker.engine.model.Config
import de.gesellix.docker.engine.model.ConfigSpec
import de.gesellix.docker.engine.model.IdResponse

class ConfigApi(basePath: String = defaultBasePath) : ApiClient(basePath) {
  companion object {

    @JvmStatic
    val defaultBasePath: String by lazy {
      System.getProperties().getProperty("docker.client.baseUrl", "http://localhost/v1.41")
    }
  }

  /**
   * Create a config
   *
   * @param body  (optional)
   * @return IdResponse
   * @throws UnsupportedOperationException If the API returns an informational or redirection response
   * @throws ClientException If the API returns a client error response
   * @throws ServerException If the API returns a server error response
   */
  @Suppress("UNCHECKED_CAST")
  @Throws(UnsupportedOperationException::class, ClientException::class, ServerException::class)
  fun configCreate(body: ConfigSpec?): IdResponse {
    val localVariableConfig = configCreateRequestConfig(body = body)

    val localVarResponse = request<IdResponse>(
      localVariableConfig
    )

    return when (localVarResponse.responseType) {
      ResponseType.Success -> (localVarResponse as Success<*>).data as IdResponse
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
   * To obtain the request config of the operation configCreate
   *
   * @param body  (optional)
   * @return RequestConfig
   */
  fun configCreateRequestConfig(body: ConfigSpec?): RequestConfig {
    val localVariableBody: Any? = body
    val localVariableQuery: MultiValueMap = mutableMapOf()
    val localVariableHeaders: MutableMap<String, String> = mutableMapOf()

    return RequestConfig(
      method = POST,
      path = "/configs/create",
      query = localVariableQuery,
      headers = localVariableHeaders,
      body = localVariableBody
    )
  }

  /**
   * Delete a config
   *
   * @param id ID of the config
   * @return void
   * @throws UnsupportedOperationException If the API returns an informational or redirection response
   * @throws ClientException If the API returns a client error response
   * @throws ServerException If the API returns a server error response
   */
  @Throws(UnsupportedOperationException::class, ClientException::class, ServerException::class)
  fun configDelete(id: String) {
    val localVariableConfig = configDeleteRequestConfig(id = id)

    val localVarResponse = request<Any?>(
      localVariableConfig
    )

    return when (localVarResponse.responseType) {
      ResponseType.Success -> Unit
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
   * To obtain the request config of the operation configDelete
   *
   * @param id ID of the config
   * @return RequestConfig
   */
  fun configDeleteRequestConfig(id: String): RequestConfig {
    val localVariableBody: Any? = null
    val localVariableQuery: MultiValueMap = mutableMapOf()
    val localVariableHeaders: MutableMap<String, String> = mutableMapOf()

    return RequestConfig(
      method = DELETE,
      path = "/configs/{id}".replace("{" + "id" + "}", id),
      query = localVariableQuery,
      headers = localVariableHeaders,
      body = localVariableBody
    )
  }

  /**
   * Inspect a config
   *
   * @param id ID of the config
   * @return Config
   * @throws UnsupportedOperationException If the API returns an informational or redirection response
   * @throws ClientException If the API returns a client error response
   * @throws ServerException If the API returns a server error response
   */
  @Suppress("UNCHECKED_CAST")
  @Throws(UnsupportedOperationException::class, ClientException::class, ServerException::class)
  fun configInspect(id: String): Config {
    val localVariableConfig = configInspectRequestConfig(id = id)

    val localVarResponse = request<Config>(
      localVariableConfig
    )

    return when (localVarResponse.responseType) {
      ResponseType.Success -> (localVarResponse as Success<*>).data as Config
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
   * To obtain the request config of the operation configInspect
   *
   * @param id ID of the config
   * @return RequestConfig
   */
  fun configInspectRequestConfig(id: String): RequestConfig {
    val localVariableBody: Any? = null
    val localVariableQuery: MultiValueMap = mutableMapOf()
    val localVariableHeaders: MutableMap<String, String> = mutableMapOf()

    return RequestConfig(
      method = GET,
      path = "/configs/{id}".replace("{" + "id" + "}", id),
      query = localVariableQuery,
      headers = localVariableHeaders,
      body = localVariableBody
    )
  }

  /**
   * List configs
   *
   * @param filters A JSON encoded value of the filters (a &#x60;map[string][]string&#x60;) to process on the configs list.  Available filters:  - &#x60;id&#x3D;&lt;config id&gt;&#x60; - &#x60;label&#x3D;&lt;key&gt; or label&#x3D;&lt;key&gt;&#x3D;value&#x60; - &#x60;name&#x3D;&lt;config name&gt;&#x60; - &#x60;names&#x3D;&lt;config name&gt;&#x60;  (optional)
   * @return kotlin.collections.List<Config>
   * @throws UnsupportedOperationException If the API returns an informational or redirection response
   * @throws ClientException If the API returns a client error response
   * @throws ServerException If the API returns a server error response
   */
  @Suppress("UNCHECKED_CAST")
  @Throws(UnsupportedOperationException::class, ClientException::class, ServerException::class)
  fun configList(filters: String?): List<Config> {
    val localVariableConfig = configListRequestConfig(filters = filters)

    val localVarResponse = request<List<Config>>(
      localVariableConfig
    )

    return when (localVarResponse.responseType) {
      ResponseType.Success -> (localVarResponse as Success<*>).data as List<Config>
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
   * To obtain the request config of the operation configList
   *
   * @param filters A JSON encoded value of the filters (a &#x60;map[string][]string&#x60;) to process on the configs list.  Available filters:  - &#x60;id&#x3D;&lt;config id&gt;&#x60; - &#x60;label&#x3D;&lt;key&gt; or label&#x3D;&lt;key&gt;&#x3D;value&#x60; - &#x60;name&#x3D;&lt;config name&gt;&#x60; - &#x60;names&#x3D;&lt;config name&gt;&#x60;  (optional)
   * @return RequestConfig
   */
  fun configListRequestConfig(filters: String?): RequestConfig {
    val localVariableBody: Any? = null
    val localVariableQuery: MultiValueMap = mutableMapOf<String, List<String>>()
      .apply {
        if (filters != null) {
          put("filters", listOf(filters.toString()))
        }
      }
    val localVariableHeaders: MutableMap<String, String> = mutableMapOf()

    return RequestConfig(
      method = GET,
      path = "/configs",
      query = localVariableQuery,
      headers = localVariableHeaders,
      body = localVariableBody,
      elementType = Config::class.java
    )
  }

  /**
   * Update a Config
   *
   * @param id The ID or name of the config
   * @param version The version number of the config object being updated. This is required to avoid conflicting writes.
   * @param body The spec of the config to update. Currently, only the Labels field can be updated. All other fields must remain unchanged from the [ConfigInspect endpoint](#operation/ConfigInspect) response values.  (optional)
   * @return void
   * @throws UnsupportedOperationException If the API returns an informational or redirection response
   * @throws ClientException If the API returns a client error response
   * @throws ServerException If the API returns a server error response
   */
  @Throws(UnsupportedOperationException::class, ClientException::class, ServerException::class)
  fun configUpdate(id: String, version: Long, body: ConfigSpec?) {
    val localVariableConfig = configUpdateRequestConfig(id = id, version = version, body = body)

    val localVarResponse = request<Any?>(
      localVariableConfig
    )

    return when (localVarResponse.responseType) {
      ResponseType.Success -> Unit
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
   * To obtain the request config of the operation configUpdate
   *
   * @param id The ID or name of the config
   * @param version The version number of the config object being updated. This is required to avoid conflicting writes.
   * @param body The spec of the config to update. Currently, only the Labels field can be updated. All other fields must remain unchanged from the [ConfigInspect endpoint](#operation/ConfigInspect) response values.  (optional)
   * @return RequestConfig
   */
  fun configUpdateRequestConfig(id: String, version: Long, body: ConfigSpec?): RequestConfig {
    val localVariableBody: Any? = body
    val localVariableQuery: MultiValueMap = mutableMapOf<String, List<String>>()
      .apply {
        put("version", listOf(version.toString()))
      }
    val localVariableHeaders: MutableMap<String, String> = mutableMapOf()

    return RequestConfig(
      method = POST,
      path = "/configs/{id}/update".replace("{" + "id" + "}", id),
      query = localVariableQuery,
      headers = localVariableHeaders,
      body = localVariableBody
    )
  }
}
