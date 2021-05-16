# NetworkApi

All URIs are relative to *http://localhost/v1.41*

Method | HTTP request | Description | Integration tests
------------- | ------------- | ------------- | ---
[**networkConnect**](NetworkApi.md#networkConnect) | **POST** /networks/{id}/connect | Connect a container to a network | ✅
[**networkCreate**](NetworkApi.md#networkCreate) | **POST** /networks/create | Create a network | ✅
[**networkDelete**](NetworkApi.md#networkDelete) | **DELETE** /networks/{id} | Remove a network | ✅
[**networkDisconnect**](NetworkApi.md#networkDisconnect) | **POST** /networks/{id}/disconnect | Disconnect a container from a network | ✅
[**networkInspect**](NetworkApi.md#networkInspect) | **GET** /networks/{id} | Inspect a network | ✅
[**networkList**](NetworkApi.md#networkList) | **GET** /networks | List networks | ✅
[**networkPrune**](NetworkApi.md#networkPrune) | **POST** /networks/prune | Delete unused networks | ✅


<a name="networkConnect"></a>
# **networkConnect**
> networkConnect(id, container)

Connect a container to a network

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = NetworkApi()
val id : kotlin.String = id_example // kotlin.String | Network ID or name
val container : NetworkConnectRequest =  // NetworkConnectRequest |
try {
    apiInstance.networkConnect(id, container)
} catch (e: ClientException) {
    println("4xx response calling NetworkApi#networkConnect")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling NetworkApi#networkConnect")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| Network ID or name |
 **container** | [**NetworkConnectRequest**](NetworkConnectRequest.md)|  |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, text/plain

<a name="networkCreate"></a>
# **networkCreate**
> NetworkCreateResponse networkCreate(networkConfig)

Create a network

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = NetworkApi()
val networkConfig : NetworkCreateRequest =  // NetworkCreateRequest |
try {
    val result : NetworkCreateResponse = apiInstance.networkCreate(networkConfig)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling NetworkApi#networkCreate")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling NetworkApi#networkCreate")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **networkConfig** | [**NetworkCreateRequest**](NetworkCreateRequest.md)|  |

### Return type

[**NetworkCreateResponse**](NetworkCreateResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="networkDelete"></a>
# **networkDelete**
> networkDelete(id)

Remove a network

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = NetworkApi()
val id : kotlin.String = id_example // kotlin.String | Network ID or name
try {
    apiInstance.networkDelete(id)
} catch (e: ClientException) {
    println("4xx response calling NetworkApi#networkDelete")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling NetworkApi#networkDelete")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| Network ID or name |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

<a name="networkDisconnect"></a>
# **networkDisconnect**
> networkDisconnect(id, container)

Disconnect a container from a network

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = NetworkApi()
val id : kotlin.String = id_example // kotlin.String | Network ID or name
val container : NetworkDisconnectRequest =  // NetworkDisconnectRequest |
try {
    apiInstance.networkDisconnect(id, container)
} catch (e: ClientException) {
    println("4xx response calling NetworkApi#networkDisconnect")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling NetworkApi#networkDisconnect")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| Network ID or name |
 **container** | [**NetworkDisconnectRequest**](NetworkDisconnectRequest.md)|  |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, text/plain

<a name="networkInspect"></a>
# **networkInspect**
> Network networkInspect(id, verbose, scope)

Inspect a network

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = NetworkApi()
val id : kotlin.String = id_example // kotlin.String | Network ID or name
val verbose : kotlin.Boolean = true // kotlin.Boolean | Detailed inspect output for troubleshooting
val scope : kotlin.String = scope_example // kotlin.String | Filter the network by scope (swarm, global, or local)
try {
    val result : Network = apiInstance.networkInspect(id, verbose, scope)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling NetworkApi#networkInspect")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling NetworkApi#networkInspect")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **kotlin.String**| Network ID or name |
 **verbose** | **kotlin.Boolean**| Detailed inspect output for troubleshooting | [optional] [default to false]
 **scope** | **kotlin.String**| Filter the network by scope (swarm, global, or local) | [optional]

### Return type

[**Network**](Network.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="networkList"></a>
# **networkList**
> kotlin.collections.List&lt;Network&gt; networkList(filters)

List networks

Returns a list of networks. For details on the format, see the [network inspect endpoint](#operation/NetworkInspect).  Note that it uses a different, smaller representation of a network than inspecting a single network. For example, the list of containers attached to the network is not propagated in API versions 1.28 and up.

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = NetworkApi()
val filters : kotlin.String = filters_example // kotlin.String | JSON encoded value of the filters (a `map[string][]string`) to process on the networks list.  Available filters:  - `dangling=<boolean>` When set to `true` (or `1`), returns all    networks that are not in use by a container. When set to `false`    (or `0`), only networks that are in use by one or more    containers are returned. - `driver=<driver-name>` Matches a network's driver. - `id=<network-id>` Matches all or part of a network ID. - `label=<key>` or `label=<key>=<value>` of a network label. - `name=<network-name>` Matches all or part of a network name. - `scope=[\"swarm\"|\"global\"|\"local\"]` Filters networks by scope (`swarm`, `global`, or `local`). - `type=[\"custom\"|\"builtin\"]` Filters networks by type. The `custom` keyword returns all user-defined networks.
try {
    val result : kotlin.collections.List<Network> = apiInstance.networkList(filters)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling NetworkApi#networkList")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling NetworkApi#networkList")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **filters** | **kotlin.String**| JSON encoded value of the filters (a &#x60;map[string][]string&#x60;) to process on the networks list.  Available filters:  - &#x60;dangling&#x3D;&lt;boolean&gt;&#x60; When set to &#x60;true&#x60; (or &#x60;1&#x60;), returns all    networks that are not in use by a container. When set to &#x60;false&#x60;    (or &#x60;0&#x60;), only networks that are in use by one or more    containers are returned. - &#x60;driver&#x3D;&lt;driver-name&gt;&#x60; Matches a network&#39;s driver. - &#x60;id&#x3D;&lt;network-id&gt;&#x60; Matches all or part of a network ID. - &#x60;label&#x3D;&lt;key&gt;&#x60; or &#x60;label&#x3D;&lt;key&gt;&#x3D;&lt;value&gt;&#x60; of a network label. - &#x60;name&#x3D;&lt;network-name&gt;&#x60; Matches all or part of a network name. - &#x60;scope&#x3D;[\&quot;swarm\&quot;|\&quot;global\&quot;|\&quot;local\&quot;]&#x60; Filters networks by scope (&#x60;swarm&#x60;, &#x60;global&#x60;, or &#x60;local&#x60;). - &#x60;type&#x3D;[\&quot;custom\&quot;|\&quot;builtin\&quot;]&#x60; Filters networks by type. The &#x60;custom&#x60; keyword returns all user-defined networks.  | [optional]

### Return type

[**kotlin.collections.List&lt;Network&gt;**](Network.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="networkPrune"></a>
# **networkPrune**
> NetworkPruneResponse networkPrune(filters)

Delete unused networks

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = NetworkApi()
val filters : kotlin.String = filters_example // kotlin.String | Filters to process on the prune list, encoded as JSON (a `map[string][]string`).  Available filters: - `until=<timestamp>` Prune networks created before this timestamp. The `<timestamp>` can be Unix timestamps, date formatted timestamps, or Go duration strings (e.g. `10m`, `1h30m`) computed relative to the daemon machine’s time. - `label` (`label=<key>`, `label=<key>=<value>`, `label!=<key>`, or `label!=<key>=<value>`) Prune networks with (or without, in case `label!=...` is used) the specified labels.
try {
    val result : NetworkPruneResponse = apiInstance.networkPrune(filters)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling NetworkApi#networkPrune")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling NetworkApi#networkPrune")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **filters** | **kotlin.String**| Filters to process on the prune list, encoded as JSON (a &#x60;map[string][]string&#x60;).  Available filters: - &#x60;until&#x3D;&lt;timestamp&gt;&#x60; Prune networks created before this timestamp. The &#x60;&lt;timestamp&gt;&#x60; can be Unix timestamps, date formatted timestamps, or Go duration strings (e.g. &#x60;10m&#x60;, &#x60;1h30m&#x60;) computed relative to the daemon machine’s time. - &#x60;label&#x60; (&#x60;label&#x3D;&lt;key&gt;&#x60;, &#x60;label&#x3D;&lt;key&gt;&#x3D;&lt;value&gt;&#x60;, &#x60;label!&#x3D;&lt;key&gt;&#x60;, or &#x60;label!&#x3D;&lt;key&gt;&#x3D;&lt;value&gt;&#x60;) Prune networks with (or without, in case &#x60;label!&#x3D;...&#x60; is used) the specified labels.  | [optional]

### Return type

[**NetworkPruneResponse**](NetworkPruneResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

