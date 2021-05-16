# ServiceApi

All URIs are relative to *http://localhost/v1.41*

Method | HTTP request | Description | Integration tests
------------- | ------------- | ------------- | ---
[**serviceCreate**](ServiceApi.md#serviceCreate) | **POST** /services/create | Create a service | ✅
[**serviceDelete**](ServiceApi.md#serviceDelete) | **DELETE** /services/{id} | Delete a service | ✅
[**serviceInspect**](ServiceApi.md#serviceInspect) | **GET** /services/{id} | Inspect a service | ✅
[**serviceList**](ServiceApi.md#serviceList) | **GET** /services | List services | ✅
[**serviceLogs**](ServiceApi.md#serviceLogs) | **GET** /services/{id}/logs | Get service logs | ✅
[**serviceUpdate**](ServiceApi.md#serviceUpdate) | **POST** /services/{id}/update | Update a service | ✅


<a name="serviceCreate"></a>
# **serviceCreate**
> ServiceCreateResponse serviceCreate(body, xRegistryAuth)

Create a service

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ServiceApi()
val body : UNKNOWN_BASE_TYPE =  // UNKNOWN_BASE_TYPE |
val xRegistryAuth : kotlin.String = xRegistryAuth_example // kotlin.String | A base64url-encoded auth configuration for pulling from private registries.  Refer to the [authentication section](#section/Authentication) for details.
try {
    val result : ServiceCreateResponse = apiInstance.serviceCreate(body, xRegistryAuth)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ServiceApi#serviceCreate")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ServiceApi#serviceCreate")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**UNKNOWN_BASE_TYPE**](UNKNOWN_BASE_TYPE.md)|  |
 **xRegistryAuth** | **kotlin.String**| A base64url-encoded auth configuration for pulling from private registries.  Refer to the [authentication section](#section/Authentication) for details.  | [optional]

### Return type

[**ServiceCreateResponse**](ServiceCreateResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="serviceDelete"></a>
# **serviceDelete**
> serviceDelete(id)

Delete a service

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ServiceApi()
val id : kotlin.String = id_example // kotlin.String | ID or name of service.
try {
    apiInstance.serviceDelete(id)
} catch (e: ClientException) {
    println("4xx response calling ServiceApi#serviceDelete")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ServiceApi#serviceDelete")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| ID or name of service. |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

<a name="serviceInspect"></a>
# **serviceInspect**
> Service serviceInspect(id, insertDefaults)

Inspect a service

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ServiceApi()
val id : kotlin.String = id_example // kotlin.String | ID or name of service.
val insertDefaults : kotlin.Boolean = true // kotlin.Boolean | Fill empty fields with default values.
try {
    val result : Service = apiInstance.serviceInspect(id, insertDefaults)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ServiceApi#serviceInspect")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ServiceApi#serviceInspect")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| ID or name of service. |
 **insertDefaults** | **kotlin.Boolean**| Fill empty fields with default values. | [optional] [default to false]

### Return type

[**Service**](Service.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

<a name="serviceList"></a>
# **serviceList**
> kotlin.collections.List&lt;Service&gt; serviceList(filters, status)

List services

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ServiceApi()
val filters : kotlin.String = filters_example // kotlin.String | A JSON encoded value of the filters (a `map[string][]string`) to process on the services list.  Available filters:  - `id=<service id>` - `label=<service label>` - `mode=[\"replicated\"|\"global\"]` - `name=<service name>`
val status : kotlin.Boolean = true // kotlin.Boolean | Include service status, with count of running and desired tasks.
try {
    val result : kotlin.collections.List<Service> = apiInstance.serviceList(filters, status)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ServiceApi#serviceList")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ServiceApi#serviceList")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **filters** | **kotlin.String**| A JSON encoded value of the filters (a &#x60;map[string][]string&#x60;) to process on the services list.  Available filters:  - &#x60;id&#x3D;&lt;service id&gt;&#x60; - &#x60;label&#x3D;&lt;service label&gt;&#x60; - &#x60;mode&#x3D;[\&quot;replicated\&quot;|\&quot;global\&quot;]&#x60; - &#x60;name&#x3D;&lt;service name&gt;&#x60;  | [optional]
 **status** | **kotlin.Boolean**| Include service status, with count of running and desired tasks.  | [optional]

### Return type

[**kotlin.collections.List&lt;Service&gt;**](Service.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

<a name="serviceLogs"></a>
# **serviceLogs**
> java.io.File serviceLogs(id, details, follow, stdout, stderr, since, timestamps, tail)

Get service logs

Get &#x60;stdout&#x60; and &#x60;stderr&#x60; logs from a service. See also [&#x60;/containers/{id}/logs&#x60;](#operation/ContainerLogs).  **Note**: This endpoint works only for services with the &#x60;local&#x60;, &#x60;json-file&#x60; or &#x60;journald&#x60; logging drivers.

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ServiceApi()
val id : kotlin.String = id_example // kotlin.String | ID or name of the service
val details : kotlin.Boolean = true // kotlin.Boolean | Show service context and extra details provided to logs.
val follow : kotlin.Boolean = true // kotlin.Boolean | Keep connection after returning logs.
val stdout : kotlin.Boolean = true // kotlin.Boolean | Return logs from `stdout`
val stderr : kotlin.Boolean = true // kotlin.Boolean | Return logs from `stderr`
val since : kotlin.Int = 56 // kotlin.Int | Only return logs since this time, as a UNIX timestamp
val timestamps : kotlin.Boolean = true // kotlin.Boolean | Add timestamps to every log line
val tail : kotlin.String = tail_example // kotlin.String | Only return this number of log lines from the end of the logs. Specify as an integer or `all` to output all log lines.
try {
    val result : java.io.File = apiInstance.serviceLogs(id, details, follow, stdout, stderr, since, timestamps, tail)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ServiceApi#serviceLogs")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ServiceApi#serviceLogs")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| ID or name of the service |
 **details** | **kotlin.Boolean**| Show service context and extra details provided to logs. | [optional] [default to false]
 **follow** | **kotlin.Boolean**| Keep connection after returning logs. | [optional] [default to false]
 **stdout** | **kotlin.Boolean**| Return logs from &#x60;stdout&#x60; | [optional] [default to false]
 **stderr** | **kotlin.Boolean**| Return logs from &#x60;stderr&#x60; | [optional] [default to false]
 **since** | **kotlin.Int**| Only return logs since this time, as a UNIX timestamp | [optional] [default to 0]
 **timestamps** | **kotlin.Boolean**| Add timestamps to every log line | [optional] [default to false]
 **tail** | **kotlin.String**| Only return this number of log lines from the end of the logs. Specify as an integer or &#x60;all&#x60; to output all log lines.  | [optional] [default to &quot;all&quot;]

### Return type

[**java.io.File**](java.io.File.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

<a name="serviceUpdate"></a>
# **serviceUpdate**
> ServiceUpdateResponse serviceUpdate(id, version, body, registryAuthFrom, rollback, xRegistryAuth)

Update a service

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ServiceApi()
val id : kotlin.String = id_example // kotlin.String | ID or name of service.
val version : kotlin.Int = 56 // kotlin.Int | The version number of the service object being updated. This is required to avoid conflicting writes. This version number should be the value as currently set on the service *before* the update. You can find the current version by calling `GET /services/{id}`
val body : UNKNOWN_BASE_TYPE =  // UNKNOWN_BASE_TYPE |
val registryAuthFrom : kotlin.String = registryAuthFrom_example // kotlin.String | If the `X-Registry-Auth` header is not specified, this parameter indicates where to find registry authorization credentials.
val rollback : kotlin.String = rollback_example // kotlin.String | Set to this parameter to `previous` to cause a server-side rollback to the previous service spec. The supplied spec will be ignored in this case.
val xRegistryAuth : kotlin.String = xRegistryAuth_example // kotlin.String | A base64url-encoded auth configuration for pulling from private registries.  Refer to the [authentication section](#section/Authentication) for details.
try {
    val result : ServiceUpdateResponse = apiInstance.serviceUpdate(id, version, body, registryAuthFrom, rollback, xRegistryAuth)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ServiceApi#serviceUpdate")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ServiceApi#serviceUpdate")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| ID or name of service. |
 **version** | **kotlin.Int**| The version number of the service object being updated. This is required to avoid conflicting writes. This version number should be the value as currently set on the service *before* the update. You can find the current version by calling &#x60;GET /services/{id}&#x60;  |
 **body** | [**UNKNOWN_BASE_TYPE**](UNKNOWN_BASE_TYPE.md)|  |
 **registryAuthFrom** | **kotlin.String**| If the &#x60;X-Registry-Auth&#x60; header is not specified, this parameter indicates where to find registry authorization credentials.  | [optional] [default to spec] [enum: spec, previous-spec]
 **rollback** | **kotlin.String**| Set to this parameter to &#x60;previous&#x60; to cause a server-side rollback to the previous service spec. The supplied spec will be ignored in this case.  | [optional]
 **xRegistryAuth** | **kotlin.String**| A base64url-encoded auth configuration for pulling from private registries.  Refer to the [authentication section](#section/Authentication) for details.  | [optional]

### Return type

[**ServiceUpdateResponse**](ServiceUpdateResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

