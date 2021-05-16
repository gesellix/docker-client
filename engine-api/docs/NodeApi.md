# NodeApi

All URIs are relative to *http://localhost/v1.41*

Method | HTTP request | Description | Integration tests
------------- | ------------- | ------------- | ---
[**nodeDelete**](NodeApi.md#nodeDelete) | **DELETE** /nodes/{id} | Delete a node | ✅
[**nodeInspect**](NodeApi.md#nodeInspect) | **GET** /nodes/{id} | Inspect a node | ✅
[**nodeList**](NodeApi.md#nodeList) | **GET** /nodes | List nodes | ✅
[**nodeUpdate**](NodeApi.md#nodeUpdate) | **POST** /nodes/{id}/update | Update a node | ✅


<a name="nodeDelete"></a>
# **nodeDelete**
> nodeDelete(id, force)

Delete a node

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = NodeApi()
val id : kotlin.String = id_example // kotlin.String | The ID or name of the node
val force : kotlin.Boolean = true // kotlin.Boolean | Force remove a node from the swarm
try {
    apiInstance.nodeDelete(id, force)
} catch (e: ClientException) {
    println("4xx response calling NodeApi#nodeDelete")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling NodeApi#nodeDelete")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| The ID or name of the node |
 **force** | **kotlin.Boolean**| Force remove a node from the swarm | [optional] [default to false]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

<a name="nodeInspect"></a>
# **nodeInspect**
> Node nodeInspect(id)

Inspect a node

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = NodeApi()
val id : kotlin.String = id_example // kotlin.String | The ID or name of the node
try {
    val result : Node = apiInstance.nodeInspect(id)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling NodeApi#nodeInspect")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling NodeApi#nodeInspect")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| The ID or name of the node |

### Return type

[**Node**](Node.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

<a name="nodeList"></a>
# **nodeList**
> kotlin.collections.List&lt;Node&gt; nodeList(filters)

List nodes

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = NodeApi()
val filters : kotlin.String = filters_example // kotlin.String | Filters to process on the nodes list, encoded as JSON (a `map[string][]string`).  Available filters: - `id=<node id>` - `label=<engine label>` - `membership=`(`accepted`|`pending`)` - `name=<node name>` - `node.label=<node label>` - `role=`(`manager`|`worker`)`
try {
    val result : kotlin.collections.List<Node> = apiInstance.nodeList(filters)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling NodeApi#nodeList")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling NodeApi#nodeList")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **filters** | **kotlin.String**| Filters to process on the nodes list, encoded as JSON (a &#x60;map[string][]string&#x60;).  Available filters: - &#x60;id&#x3D;&lt;node id&gt;&#x60; - &#x60;label&#x3D;&lt;engine label&gt;&#x60; - &#x60;membership&#x3D;&#x60;(&#x60;accepted&#x60;|&#x60;pending&#x60;)&#x60; - &#x60;name&#x3D;&lt;node name&gt;&#x60; - &#x60;node.label&#x3D;&lt;node label&gt;&#x60; - &#x60;role&#x3D;&#x60;(&#x60;manager&#x60;|&#x60;worker&#x60;)&#x60;  | [optional]

### Return type

[**kotlin.collections.List&lt;Node&gt;**](Node.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

<a name="nodeUpdate"></a>
# **nodeUpdate**
> nodeUpdate(id, version, body)

Update a node

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = NodeApi()
val id : kotlin.String = id_example // kotlin.String | The ID of the node
val version : kotlin.Long = 789 // kotlin.Long | The version number of the node object being updated. This is required to avoid conflicting writes.
val body : NodeSpec =  // NodeSpec |
try {
    apiInstance.nodeUpdate(id, version, body)
} catch (e: ClientException) {
    println("4xx response calling NodeApi#nodeUpdate")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling NodeApi#nodeUpdate")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| The ID of the node |
 **version** | **kotlin.Long**| The version number of the node object being updated. This is required to avoid conflicting writes.  |
 **body** | [**NodeSpec**](NodeSpec.md)|  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, text/plain
 - **Accept**: application/json, text/plain

