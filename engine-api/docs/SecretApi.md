# SecretApi

All URIs are relative to *http://localhost/v1.41*

Method | HTTP request | Description | Integration tests
------------- | ------------- | ------------- | ---
[**secretCreate**](SecretApi.md#secretCreate) | **POST** /secrets/create | Create a secret | ✅
[**secretDelete**](SecretApi.md#secretDelete) | **DELETE** /secrets/{id} | Delete a secret | ✅
[**secretInspect**](SecretApi.md#secretInspect) | **GET** /secrets/{id} | Inspect a secret | ✅
[**secretList**](SecretApi.md#secretList) | **GET** /secrets | List secrets | ✅
[**secretUpdate**](SecretApi.md#secretUpdate) | **POST** /secrets/{id}/update | Update a Secret | ✅


<a name="secretCreate"></a>
# **secretCreate**
> IdResponse secretCreate(body)

Create a secret

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = SecretApi()
val body : UNKNOWN_BASE_TYPE =  // UNKNOWN_BASE_TYPE |
try {
    val result : IdResponse = apiInstance.secretCreate(body)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling SecretApi#secretCreate")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SecretApi#secretCreate")
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

<a name="secretDelete"></a>
# **secretDelete**
> secretDelete(id)

Delete a secret

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = SecretApi()
val id : kotlin.String = id_example // kotlin.String | ID of the secret
try {
    apiInstance.secretDelete(id)
} catch (e: ClientException) {
    println("4xx response calling SecretApi#secretDelete")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SecretApi#secretDelete")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| ID of the secret |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="secretInspect"></a>
# **secretInspect**
> Secret secretInspect(id)

Inspect a secret

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = SecretApi()
val id : kotlin.String = id_example // kotlin.String | ID of the secret
try {
    val result : Secret = apiInstance.secretInspect(id)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling SecretApi#secretInspect")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SecretApi#secretInspect")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| ID of the secret |

### Return type

[**Secret**](Secret.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="secretList"></a>
# **secretList**
> kotlin.collections.List&lt;Secret&gt; secretList(filters)

List secrets

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = SecretApi()
val filters : kotlin.String = filters_example // kotlin.String | A JSON encoded value of the filters (a `map[string][]string`) to process on the secrets list.  Available filters:  - `id=<secret id>` - `label=<key> or label=<key>=value` - `name=<secret name>` - `names=<secret name>`
try {
    val result : kotlin.collections.List<Secret> = apiInstance.secretList(filters)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling SecretApi#secretList")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SecretApi#secretList")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **filters** | **kotlin.String**| A JSON encoded value of the filters (a &#x60;map[string][]string&#x60;) to process on the secrets list.  Available filters:  - &#x60;id&#x3D;&lt;secret id&gt;&#x60; - &#x60;label&#x3D;&lt;key&gt; or label&#x3D;&lt;key&gt;&#x3D;value&#x60; - &#x60;name&#x3D;&lt;secret name&gt;&#x60; - &#x60;names&#x3D;&lt;secret name&gt;&#x60;  | [optional]

### Return type

[**kotlin.collections.List&lt;Secret&gt;**](Secret.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="secretUpdate"></a>
# **secretUpdate**
> secretUpdate(id, version, body)

Update a Secret

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = SecretApi()
val id : kotlin.String = id_example // kotlin.String | The ID or name of the secret
val version : kotlin.Long = 789 // kotlin.Long | The version number of the secret object being updated. This is required to avoid conflicting writes.
val body : SecretSpec =  // SecretSpec | The spec of the secret to update. Currently, only the Labels field can be updated. All other fields must remain unchanged from the [SecretInspect endpoint](#operation/SecretInspect) response values.
try {
    apiInstance.secretUpdate(id, version, body)
} catch (e: ClientException) {
    println("4xx response calling SecretApi#secretUpdate")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SecretApi#secretUpdate")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| The ID or name of the secret |
 **version** | **kotlin.Long**| The version number of the secret object being updated. This is required to avoid conflicting writes.  |
 **body** | [**SecretSpec**](SecretSpec.md)| The spec of the secret to update. Currently, only the Labels field can be updated. All other fields must remain unchanged from the [SecretInspect endpoint](#operation/SecretInspect) response values.  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, text/plain
 - **Accept**: application/json, text/plain

