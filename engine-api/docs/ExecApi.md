# ExecApi

All URIs are relative to *http://localhost/v1.41*

Method | HTTP request | Description | Integration tests
------------- | ------------- | ------------- | ---
[**containerExec**](ExecApi.md#containerExec) | **POST** /containers/{id}/exec | Create an exec instance | ✅
[**execInspect**](ExecApi.md#execInspect) | **GET** /exec/{id}/json | Inspect an exec instance | ✅
[**execResize**](ExecApi.md#execResize) | **POST** /exec/{id}/resize | Resize an exec instance | ❌
[**execStart**](ExecApi.md#execStart) | **POST** /exec/{id}/start | Start an exec instance | ✅


<a name="containerExec"></a>
# **containerExec**
> IdResponse containerExec(id, execConfig)

Create an exec instance

Run a command inside a running container.

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ExecApi()
val id : kotlin.String = id_example // kotlin.String | ID or name of container
val execConfig : ExecConfig =  // ExecConfig |
try {
    val result : IdResponse = apiInstance.containerExec(id, execConfig)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ExecApi#containerExec")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ExecApi#containerExec")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| ID or name of container |
 **execConfig** | [**ExecConfig**](ExecConfig.md)|  |

### Return type

[**IdResponse**](IdResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="execInspect"></a>
# **execInspect**
> ExecInspectResponse execInspect(id)

Inspect an exec instance

Return low-level information about an exec instance.

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ExecApi()
val id : kotlin.String = id_example // kotlin.String | Exec instance ID
try {
    val result : ExecInspectResponse = apiInstance.execInspect(id)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ExecApi#execInspect")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ExecApi#execInspect")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| Exec instance ID |

### Return type

[**ExecInspectResponse**](ExecInspectResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="execResize"></a>
# **execResize**
> execResize(id, h, w)

Resize an exec instance

Resize the TTY session used by an exec instance. This endpoint only works if &#x60;tty&#x60; was specified as part of creating and starting the exec instance.

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ExecApi()
val id : kotlin.String = id_example // kotlin.String | Exec instance ID
val h : kotlin.Int = 56 // kotlin.Int | Height of the TTY session in characters
val w : kotlin.Int = 56 // kotlin.Int | Width of the TTY session in characters
try {
    apiInstance.execResize(id, h, w)
} catch (e: ClientException) {
    println("4xx response calling ExecApi#execResize")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ExecApi#execResize")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| Exec instance ID |
 **h** | **kotlin.Int**| Height of the TTY session in characters | [optional]
 **w** | **kotlin.Int**| Width of the TTY session in characters | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

<a name="execStart"></a>
# **execStart**
> execStart(id, execStartConfig)

Start an exec instance

Starts a previously set up exec instance. If detach is true, this endpoint returns immediately after starting the command. Otherwise, it sets up an interactive session with the command.

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ExecApi()
val id : kotlin.String = id_example // kotlin.String | Exec instance ID
val execStartConfig : ExecStartConfig =  // ExecStartConfig |
try {
    apiInstance.execStart(id, execStartConfig)
} catch (e: ClientException) {
    println("4xx response calling ExecApi#execStart")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ExecApi#execStart")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| Exec instance ID |
 **execStartConfig** | [**ExecStartConfig**](ExecStartConfig.md)|  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/vnd.docker.raw-stream

