# TaskApi

All URIs are relative to *http://localhost/v1.41*

Method | HTTP request | Description | Integration tests
------------- | ------------- | ------------- | ---
[**taskInspect**](TaskApi.md#taskInspect) | **GET** /tasks/{id} | Inspect a task | ✅
[**taskList**](TaskApi.md#taskList) | **GET** /tasks | List tasks | ✅
[**taskLogs**](TaskApi.md#taskLogs) | **GET** /tasks/{id}/logs | Get task logs | ✅


<a name="taskInspect"></a>
# **taskInspect**
> Task taskInspect(id)

Inspect a task

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = TaskApi()
val id : kotlin.String = id_example // kotlin.String | ID of the task
try {
    val result : Task = apiInstance.taskInspect(id)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaskApi#taskInspect")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaskApi#taskInspect")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| ID of the task |

### Return type

[**Task**](Task.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="taskList"></a>
# **taskList**
> kotlin.collections.List&lt;Task&gt; taskList(filters)

List tasks

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = TaskApi()
val filters : kotlin.String = filters_example // kotlin.String | A JSON encoded value of the filters (a `map[string][]string`) to process on the tasks list.  Available filters:  - `desired-state=(running | shutdown | accepted)` - `id=<task id>` - `label=key` or `label=\"key=value\"` - `name=<task name>` - `node=<node id or name>` - `service=<service name>`
try {
    val result : kotlin.collections.List<Task> = apiInstance.taskList(filters)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaskApi#taskList")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaskApi#taskList")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **filters** | **kotlin.String**| A JSON encoded value of the filters (a &#x60;map[string][]string&#x60;) to process on the tasks list.  Available filters:  - &#x60;desired-state&#x3D;(running | shutdown | accepted)&#x60; - &#x60;id&#x3D;&lt;task id&gt;&#x60; - &#x60;label&#x3D;key&#x60; or &#x60;label&#x3D;\&quot;key&#x3D;value\&quot;&#x60; - &#x60;name&#x3D;&lt;task name&gt;&#x60; - &#x60;node&#x3D;&lt;node id or name&gt;&#x60; - &#x60;service&#x3D;&lt;service name&gt;&#x60;  | [optional]

### Return type

[**kotlin.collections.List&lt;Task&gt;**](Task.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="taskLogs"></a>
# **taskLogs**
> java.io.File taskLogs(id, details, follow, stdout, stderr, since, timestamps, tail)

Get task logs

Get &#x60;stdout&#x60; and &#x60;stderr&#x60; logs from a task. See also [&#x60;/containers/{id}/logs&#x60;](#operation/ContainerLogs).  **Note**: This endpoint works only for services with the &#x60;local&#x60;, &#x60;json-file&#x60; or &#x60;journald&#x60; logging drivers.

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = TaskApi()
val id : kotlin.String = id_example // kotlin.String | ID of the task
val details : kotlin.Boolean = true // kotlin.Boolean | Show task context and extra details provided to logs.
val follow : kotlin.Boolean = true // kotlin.Boolean | Keep connection after returning logs.
val stdout : kotlin.Boolean = true // kotlin.Boolean | Return logs from `stdout`
val stderr : kotlin.Boolean = true // kotlin.Boolean | Return logs from `stderr`
val since : kotlin.Int = 56 // kotlin.Int | Only return logs since this time, as a UNIX timestamp
val timestamps : kotlin.Boolean = true // kotlin.Boolean | Add timestamps to every log line
val tail : kotlin.String = tail_example // kotlin.String | Only return this number of log lines from the end of the logs. Specify as an integer or `all` to output all log lines.
try {
    val result : java.io.File = apiInstance.taskLogs(id, details, follow, stdout, stderr, since, timestamps, tail)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaskApi#taskLogs")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaskApi#taskLogs")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| ID of the task |
 **details** | **kotlin.Boolean**| Show task context and extra details provided to logs. | [optional] [default to false]
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

