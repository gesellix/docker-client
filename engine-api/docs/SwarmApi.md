# SwarmApi

All URIs are relative to *http://localhost/v1.41*

Method | HTTP request | Description | Integration tests
------------- | ------------- | ------------- | ---
[**swarmInit**](SwarmApi.md#swarmInit) | **POST** /swarm/init | Initialize a new swarm | ✅
[**swarmInspect**](SwarmApi.md#swarmInspect) | **GET** /swarm | Inspect swarm | ✅
[**swarmJoin**](SwarmApi.md#swarmJoin) | **POST** /swarm/join | Join an existing swarm | ❌
[**swarmLeave**](SwarmApi.md#swarmLeave) | **POST** /swarm/leave | Leave a swarm | ✅
[**swarmUnlock**](SwarmApi.md#swarmUnlock) | **POST** /swarm/unlock | Unlock a locked manager | ❌
[**swarmUnlockkey**](SwarmApi.md#swarmUnlockkey) | **GET** /swarm/unlockkey | Get the unlock key | ✅
[**swarmUpdate**](SwarmApi.md#swarmUpdate) | **POST** /swarm/update | Update a swarm | ❌


<a name="swarmInit"></a>
# **swarmInit**
> kotlin.String swarmInit(body)

Initialize a new swarm

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = SwarmApi()
val body : SwarmInitRequest =  // SwarmInitRequest |
try {
    val result : kotlin.String = apiInstance.swarmInit(body)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling SwarmApi#swarmInit")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SwarmApi#swarmInit")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**SwarmInitRequest**](SwarmInitRequest.md)|  |

### Return type

**kotlin.String**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, text/plain
 - **Accept**: application/json, text/plain

<a name="swarmInspect"></a>
# **swarmInspect**
> Swarm swarmInspect()

Inspect swarm

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = SwarmApi()
try {
    val result : Swarm = apiInstance.swarmInspect()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling SwarmApi#swarmInspect")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SwarmApi#swarmInspect")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**Swarm**](Swarm.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

<a name="swarmJoin"></a>
# **swarmJoin**
> swarmJoin(body)

Join an existing swarm

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = SwarmApi()
val body : SwarmJoinRequest =  // SwarmJoinRequest |
try {
    apiInstance.swarmJoin(body)
} catch (e: ClientException) {
    println("4xx response calling SwarmApi#swarmJoin")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SwarmApi#swarmJoin")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**SwarmJoinRequest**](SwarmJoinRequest.md)|  |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, text/plain
 - **Accept**: application/json, text/plain

<a name="swarmLeave"></a>
# **swarmLeave**
> swarmLeave(force)

Leave a swarm

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = SwarmApi()
val force : kotlin.Boolean = true // kotlin.Boolean | Force leave swarm, even if this is the last manager or that it will break the cluster.
try {
    apiInstance.swarmLeave(force)
} catch (e: ClientException) {
    println("4xx response calling SwarmApi#swarmLeave")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SwarmApi#swarmLeave")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **force** | **kotlin.Boolean**| Force leave swarm, even if this is the last manager or that it will break the cluster.  | [optional] [default to false]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

<a name="swarmUnlock"></a>
# **swarmUnlock**
> swarmUnlock(body)

Unlock a locked manager

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = SwarmApi()
val body : SwarmUnlockRequest =  // SwarmUnlockRequest |
try {
    apiInstance.swarmUnlock(body)
} catch (e: ClientException) {
    println("4xx response calling SwarmApi#swarmUnlock")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SwarmApi#swarmUnlock")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**SwarmUnlockRequest**](SwarmUnlockRequest.md)|  |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="swarmUnlockkey"></a>
# **swarmUnlockkey**
> UnlockKeyResponse swarmUnlockkey()

Get the unlock key

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = SwarmApi()
try {
    val result : UnlockKeyResponse = apiInstance.swarmUnlockkey()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling SwarmApi#swarmUnlockkey")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SwarmApi#swarmUnlockkey")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**UnlockKeyResponse**](UnlockKeyResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

<a name="swarmUpdate"></a>
# **swarmUpdate**
> swarmUpdate(version, body, rotateWorkerToken, rotateManagerToken, rotateManagerUnlockKey)

Update a swarm

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = SwarmApi()
val version : kotlin.Long = 789 // kotlin.Long | The version number of the swarm object being updated. This is required to avoid conflicting writes.
val body : SwarmSpec =  // SwarmSpec |
val rotateWorkerToken : kotlin.Boolean = true // kotlin.Boolean | Rotate the worker join token.
val rotateManagerToken : kotlin.Boolean = true // kotlin.Boolean | Rotate the manager join token.
val rotateManagerUnlockKey : kotlin.Boolean = true // kotlin.Boolean | Rotate the manager unlock key.
try {
    apiInstance.swarmUpdate(version, body, rotateWorkerToken, rotateManagerToken, rotateManagerUnlockKey)
} catch (e: ClientException) {
    println("4xx response calling SwarmApi#swarmUpdate")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SwarmApi#swarmUpdate")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **version** | **kotlin.Long**| The version number of the swarm object being updated. This is required to avoid conflicting writes.  |
 **body** | [**SwarmSpec**](SwarmSpec.md)|  |
 **rotateWorkerToken** | **kotlin.Boolean**| Rotate the worker join token. | [optional] [default to false]
 **rotateManagerToken** | **kotlin.Boolean**| Rotate the manager join token. | [optional] [default to false]
 **rotateManagerUnlockKey** | **kotlin.Boolean**| Rotate the manager unlock key. | [optional] [default to false]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, text/plain
 - **Accept**: application/json, text/plain

