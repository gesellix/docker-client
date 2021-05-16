# ConfigApi

All URIs are relative to *http://localhost/v1.41*

Method | HTTP request | Description | Integration tests
------------- | ------------- | ------------- | ---
[**configCreate**](ConfigApi.md#configCreate) | **POST** /configs/create | Create a config | ✅
[**configDelete**](ConfigApi.md#configDelete) | **DELETE** /configs/{id} | Delete a config | ✅
[**configInspect**](ConfigApi.md#configInspect) | **GET** /configs/{id} | Inspect a config | ✅
[**configList**](ConfigApi.md#configList) | **GET** /configs | List configs | ✅
[**configUpdate**](ConfigApi.md#configUpdate) | **POST** /configs/{id}/update | Update a Config | ✅


<a name="configCreate"></a>
# **configCreate**
> IdResponse configCreate(body)

Create a config

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ConfigApi()
val body : UNKNOWN_BASE_TYPE =  // UNKNOWN_BASE_TYPE |
try {
    val result : IdResponse = apiInstance.configCreate(body)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ConfigApi#configCreate")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ConfigApi#configCreate")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**UNKNOWN_BASE_TYPE**](UNKNOWN_BASE_TYPE.md)|  | [optional]

### Return type

[**IdResponse**](IdResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="configDelete"></a>
# **configDelete**
> configDelete(id)

Delete a config

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ConfigApi()
val id : kotlin.String = id_example // kotlin.String | ID of the config
try {
    apiInstance.configDelete(id)
} catch (e: ClientException) {
    println("4xx response calling ConfigApi#configDelete")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ConfigApi#configDelete")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| ID of the config |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="configInspect"></a>
# **configInspect**
> Config configInspect(id)

Inspect a config

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ConfigApi()
val id : kotlin.String = id_example // kotlin.String | ID of the config
try {
    val result : Config = apiInstance.configInspect(id)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ConfigApi#configInspect")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ConfigApi#configInspect")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| ID of the config |

### Return type

[**Config**](Config.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="configList"></a>
# **configList**
> kotlin.collections.List&lt;Config&gt; configList(filters)

List configs

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ConfigApi()
val filters : kotlin.String = filters_example // kotlin.String | A JSON encoded value of the filters (a `map[string][]string`) to process on the configs list.  Available filters:  - `id=<config id>` - `label=<key> or label=<key>=value` - `name=<config name>` - `names=<config name>`
try {
    val result : kotlin.collections.List<Config> = apiInstance.configList(filters)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ConfigApi#configList")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ConfigApi#configList")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **filters** | **kotlin.String**| A JSON encoded value of the filters (a &#x60;map[string][]string&#x60;) to process on the configs list.  Available filters:  - &#x60;id&#x3D;&lt;config id&gt;&#x60; - &#x60;label&#x3D;&lt;key&gt; or label&#x3D;&lt;key&gt;&#x3D;value&#x60; - &#x60;name&#x3D;&lt;config name&gt;&#x60; - &#x60;names&#x3D;&lt;config name&gt;&#x60;  | [optional]

### Return type

[**kotlin.collections.List&lt;Config&gt;**](Config.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="configUpdate"></a>
# **configUpdate**
> configUpdate(id, version, body)

Update a Config

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ConfigApi()
val id : kotlin.String = id_example // kotlin.String | The ID or name of the config
val version : kotlin.Long = 789 // kotlin.Long | The version number of the config object being updated. This is required to avoid conflicting writes.
val body : ConfigSpec =  // ConfigSpec | The spec of the config to update. Currently, only the Labels field can be updated. All other fields must remain unchanged from the [ConfigInspect endpoint](#operation/ConfigInspect) response values.
try {
    apiInstance.configUpdate(id, version, body)
} catch (e: ClientException) {
    println("4xx response calling ConfigApi#configUpdate")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ConfigApi#configUpdate")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| The ID or name of the config |
 **version** | **kotlin.Long**| The version number of the config object being updated. This is required to avoid conflicting writes.  |
 **body** | [**ConfigSpec**](ConfigSpec.md)| The spec of the config to update. Currently, only the Labels field can be updated. All other fields must remain unchanged from the [ConfigInspect endpoint](#operation/ConfigInspect) response values.  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, text/plain
 - **Accept**: application/json, text/plain

