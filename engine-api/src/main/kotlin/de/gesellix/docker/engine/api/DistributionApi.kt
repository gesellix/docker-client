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

import de.gesellix.docker.engine.RequestMethod.GET
import de.gesellix.docker.engine.client.infrastructure.ApiClient
import de.gesellix.docker.engine.client.infrastructure.ClientError
import de.gesellix.docker.engine.client.infrastructure.ClientException
import de.gesellix.docker.engine.client.infrastructure.MultiValueMap
import de.gesellix.docker.engine.client.infrastructure.RequestConfig
import de.gesellix.docker.engine.client.infrastructure.ResponseType
import de.gesellix.docker.engine.client.infrastructure.ServerError
import de.gesellix.docker.engine.client.infrastructure.ServerException
import de.gesellix.docker.engine.client.infrastructure.Success
import de.gesellix.docker.engine.model.DistributionInspect

class DistributionApi(basePath: String = defaultBasePath) : ApiClient(basePath) {
  companion object {

    @JvmStatic
    val defaultBasePath: String by lazy {
      System.getProperties().getProperty("docker.client.baseUrl", "http://localhost/v1.41")
    }
  }

  /**
   * Get image information from the registry
   * Return image digest and platform information by contacting the registry.
   * @param name Image name or id
   * @return DistributionInspectResponse
   * @throws UnsupportedOperationException If the API returns an informational or redirection response
   * @throws ClientException If the API returns a client error response
   * @throws ServerException If the API returns a server error response
   */
  @Suppress("UNCHECKED_CAST")
  @Throws(UnsupportedOperationException::class, ClientException::class, ServerException::class)
  fun distributionInspect(name: String): DistributionInspect {
    val localVariableConfig = distributionInspectRequestConfig(name = name)

    val localVarResponse = request<DistributionInspect>(
      localVariableConfig
    )

    return when (localVarResponse.responseType) {
      ResponseType.Success -> (localVarResponse as Success<*>).data as DistributionInspect
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
   * To obtain the request config of the operation distributionInspect
   *
   * @param name Image name or id
   * @return RequestConfig
   */
  fun distributionInspectRequestConfig(name: String): RequestConfig {
    val localVariableBody: Any? = null
    val localVariableQuery: MultiValueMap = mutableMapOf()
    val localVariableHeaders: MutableMap<String, String> = mutableMapOf()

    val localVariableConfig = RequestConfig(
      method = GET,
      path = "/distribution/{name}/json".replace("{" + "name" + "}", name),
      query = localVariableQuery,
      headers = localVariableHeaders,
      body = localVariableBody
    )

    return localVariableConfig
  }
}
