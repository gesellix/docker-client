# PluginApi

All URIs are relative to *http://localhost/v1.41*

Method | HTTP request | Description | Integration tests
------------- | ------------- | ------------- | ---
[**getPluginPrivileges**](PluginApi.md#getPluginPrivileges) | **GET** /plugins/privileges | Get plugin privileges | ✅
[**pluginCreate**](PluginApi.md#pluginCreate) | **POST** /plugins/create | Create a plugin | ❌
[**pluginDelete**](PluginApi.md#pluginDelete) | **DELETE** /plugins/{name} | Remove a plugin | ❌
[**pluginDisable**](PluginApi.md#pluginDisable) | **POST** /plugins/{name}/disable | Disable a plugin | ❌
[**pluginEnable**](PluginApi.md#pluginEnable) | **POST** /plugins/{name}/enable | Enable a plugin | ❌
[**pluginInspect**](PluginApi.md#pluginInspect) | **GET** /plugins/{name}/json | Inspect a plugin | ❌
[**pluginList**](PluginApi.md#pluginList) | **GET** /plugins | List plugins | ✅
[**pluginPull**](PluginApi.md#pluginPull) | **POST** /plugins/pull | Install a plugin | ❌
[**pluginPush**](PluginApi.md#pluginPush) | **POST** /plugins/{name}/push | Push a plugin | ❌
[**pluginSet**](PluginApi.md#pluginSet) | **POST** /plugins/{name}/set | Configure a plugin | ❌
[**pluginUpgrade**](PluginApi.md#pluginUpgrade) | **POST** /plugins/{name}/upgrade | Upgrade a plugin | ❌


<a name="getPluginPrivileges"></a>
# **getPluginPrivileges**
> kotlin.collections.List&lt;PluginPrivilegeItem&gt; getPluginPrivileges(remote)

Get plugin privileges

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = PluginApi()
val remote : kotlin.String = remote_example // kotlin.String | The name of the plugin. The `:latest` tag is optional, and is the default if omitted.
try {
    val result : kotlin.collections.List<PluginPrivilegeItem> = apiInstance.getPluginPrivileges(remote)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PluginApi#getPluginPrivileges")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PluginApi#getPluginPrivileges")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **remote** | **kotlin.String**| The name of the plugin. The &#x60;:latest&#x60; tag is optional, and is the default if omitted.  |

### Return type

[**kotlin.collections.List&lt;PluginPrivilegeItem&gt;**](PluginPrivilegeItem.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

<a name="pluginCreate"></a>
# **pluginCreate**
> pluginCreate(name, tarContext)

Create a plugin

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = PluginApi()
val name : kotlin.String = name_example // kotlin.String | The name of the plugin. The `:latest` tag is optional, and is the default if omitted.
val tarContext : java.io.File = BINARY_DATA_HERE // java.io.File | Path to tar containing plugin rootfs and manifest
try {
    apiInstance.pluginCreate(name, tarContext)
} catch (e: ClientException) {
    println("4xx response calling PluginApi#pluginCreate")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PluginApi#pluginCreate")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **kotlin.String**| The name of the plugin. The &#x60;:latest&#x60; tag is optional, and is the default if omitted.  |
 **tarContext** | **java.io.File**| Path to tar containing plugin rootfs and manifest | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/x-tar
 - **Accept**: application/json, text/plain

<a name="pluginDelete"></a>
# **pluginDelete**
> Plugin pluginDelete(name, force)

Remove a plugin

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = PluginApi()
val name : kotlin.String = name_example // kotlin.String | The name of the plugin. The `:latest` tag is optional, and is the default if omitted.
val force : kotlin.Boolean = true // kotlin.Boolean | Disable the plugin before removing. This may result in issues if the plugin is in use by a container.
try {
    val result : Plugin = apiInstance.pluginDelete(name, force)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PluginApi#pluginDelete")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PluginApi#pluginDelete")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **kotlin.String**| The name of the plugin. The &#x60;:latest&#x60; tag is optional, and is the default if omitted.  |
 **force** | **kotlin.Boolean**| Disable the plugin before removing. This may result in issues if the plugin is in use by a container.  | [optional] [default to false]

### Return type

[**Plugin**](Plugin.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

<a name="pluginDisable"></a>
# **pluginDisable**
> pluginDisable(name)

Disable a plugin

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = PluginApi()
val name : kotlin.String = name_example // kotlin.String | The name of the plugin. The `:latest` tag is optional, and is the default if omitted.
try {
    apiInstance.pluginDisable(name)
} catch (e: ClientException) {
    println("4xx response calling PluginApi#pluginDisable")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PluginApi#pluginDisable")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **kotlin.String**| The name of the plugin. The &#x60;:latest&#x60; tag is optional, and is the default if omitted.  |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

<a name="pluginEnable"></a>
# **pluginEnable**
> pluginEnable(name, timeout)

Enable a plugin

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = PluginApi()
val name : kotlin.String = name_example // kotlin.String | The name of the plugin. The `:latest` tag is optional, and is the default if omitted.
val timeout : kotlin.Int = 56 // kotlin.Int | Set the HTTP client timeout (in seconds)
try {
    apiInstance.pluginEnable(name, timeout)
} catch (e: ClientException) {
    println("4xx response calling PluginApi#pluginEnable")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PluginApi#pluginEnable")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **kotlin.String**| The name of the plugin. The &#x60;:latest&#x60; tag is optional, and is the default if omitted.  |
 **timeout** | **kotlin.Int**| Set the HTTP client timeout (in seconds) | [optional] [default to 0]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

<a name="pluginInspect"></a>
# **pluginInspect**
> Plugin pluginInspect(name)

Inspect a plugin

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = PluginApi()
val name : kotlin.String = name_example // kotlin.String | The name of the plugin. The `:latest` tag is optional, and is the default if omitted.
try {
    val result : Plugin = apiInstance.pluginInspect(name)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PluginApi#pluginInspect")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PluginApi#pluginInspect")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **kotlin.String**| The name of the plugin. The &#x60;:latest&#x60; tag is optional, and is the default if omitted.  |

### Return type

[**Plugin**](Plugin.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

<a name="pluginList"></a>
# **pluginList**
> kotlin.collections.List&lt;Plugin&gt; pluginList(filters)

List plugins

Returns information about installed plugins.

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = PluginApi()
val filters : kotlin.String = filters_example // kotlin.String | A JSON encoded value of the filters (a `map[string][]string`) to process on the plugin list.  Available filters:  - `capability=<capability name>` - `enable=<true>|<false>`
try {
    val result : kotlin.collections.List<Plugin> = apiInstance.pluginList(filters)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PluginApi#pluginList")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PluginApi#pluginList")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **filters** | **kotlin.String**| A JSON encoded value of the filters (a &#x60;map[string][]string&#x60;) to process on the plugin list.  Available filters:  - &#x60;capability&#x3D;&lt;capability name&gt;&#x60; - &#x60;enable&#x3D;&lt;true&gt;|&lt;false&gt;&#x60;  | [optional]

### Return type

[**kotlin.collections.List&lt;Plugin&gt;**](Plugin.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="pluginPull"></a>
# **pluginPull**
> pluginPull(remote, name, xRegistryAuth, body)

Install a plugin

Pulls and installs a plugin. After the plugin is installed, it can be enabled using the [&#x60;POST /plugins/{name}/enable&#x60; endpoint](#operation/PostPluginsEnable).

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = PluginApi()
val remote : kotlin.String = remote_example // kotlin.String | Remote reference for plugin to install.  The `:latest` tag is optional, and is used as the default if omitted.
val name : kotlin.String = name_example // kotlin.String | Local name for the pulled plugin.  The `:latest` tag is optional, and is used as the default if omitted.
val xRegistryAuth : kotlin.String = xRegistryAuth_example // kotlin.String | A base64url-encoded auth configuration to use when pulling a plugin from a registry.  Refer to the [authentication section](#section/Authentication) for details.
val body : kotlin.collections.List<PluginPrivilegeItem> =  // kotlin.collections.List<PluginPrivilegeItem> |
try {
    apiInstance.pluginPull(remote, name, xRegistryAuth, body)
} catch (e: ClientException) {
    println("4xx response calling PluginApi#pluginPull")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PluginApi#pluginPull")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **remote** | **kotlin.String**| Remote reference for plugin to install.  The &#x60;:latest&#x60; tag is optional, and is used as the default if omitted.  |
 **name** | **kotlin.String**| Local name for the pulled plugin.  The &#x60;:latest&#x60; tag is optional, and is used as the default if omitted.  | [optional]
 **xRegistryAuth** | **kotlin.String**| A base64url-encoded auth configuration to use when pulling a plugin from a registry.  Refer to the [authentication section](#section/Authentication) for details.  | [optional]
 **body** | [**kotlin.collections.List&lt;PluginPrivilegeItem&gt;**](PluginPrivilegeItem.md)|  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, text/plain
 - **Accept**: application/json

<a name="pluginPush"></a>
# **pluginPush**
> pluginPush(name)

Push a plugin

Push a plugin to the registry.

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = PluginApi()
val name : kotlin.String = name_example // kotlin.String | The name of the plugin. The `:latest` tag is optional, and is the default if omitted.
try {
    apiInstance.pluginPush(name)
} catch (e: ClientException) {
    println("4xx response calling PluginApi#pluginPush")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PluginApi#pluginPush")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **kotlin.String**| The name of the plugin. The &#x60;:latest&#x60; tag is optional, and is the default if omitted.  |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

<a name="pluginSet"></a>
# **pluginSet**
> pluginSet(name, body)

Configure a plugin

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = PluginApi()
val name : kotlin.String = name_example // kotlin.String | The name of the plugin. The `:latest` tag is optional, and is the default if omitted.
val body : kotlin.collections.List<kotlin.String> =  // kotlin.collections.List<kotlin.String> |
try {
    apiInstance.pluginSet(name, body)
} catch (e: ClientException) {
    println("4xx response calling PluginApi#pluginSet")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PluginApi#pluginSet")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **kotlin.String**| The name of the plugin. The &#x60;:latest&#x60; tag is optional, and is the default if omitted.  |
 **body** | [**kotlin.collections.List&lt;kotlin.String&gt;**](kotlin.String.md)|  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, text/plain

<a name="pluginUpgrade"></a>
# **pluginUpgrade**
> pluginUpgrade(name, remote, xRegistryAuth, body)

Upgrade a plugin

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = PluginApi()
val name : kotlin.String = name_example // kotlin.String | The name of the plugin. The `:latest` tag is optional, and is the default if omitted.
val remote : kotlin.String = remote_example // kotlin.String | Remote reference to upgrade to.  The `:latest` tag is optional, and is used as the default if omitted.
val xRegistryAuth : kotlin.String = xRegistryAuth_example // kotlin.String | A base64url-encoded auth configuration to use when pulling a plugin from a registry.  Refer to the [authentication section](#section/Authentication) for details.
val body : kotlin.collections.List<PluginPrivilegeItem> =  // kotlin.collections.List<PluginPrivilegeItem> |
try {
    apiInstance.pluginUpgrade(name, remote, xRegistryAuth, body)
} catch (e: ClientException) {
    println("4xx response calling PluginApi#pluginUpgrade")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PluginApi#pluginUpgrade")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **kotlin.String**| The name of the plugin. The &#x60;:latest&#x60; tag is optional, and is the default if omitted.  |
 **remote** | **kotlin.String**| Remote reference to upgrade to.  The &#x60;:latest&#x60; tag is optional, and is used as the default if omitted.  |
 **xRegistryAuth** | **kotlin.String**| A base64url-encoded auth configuration to use when pulling a plugin from a registry.  Refer to the [authentication section](#section/Authentication) for details.  | [optional]
 **body** | [**kotlin.collections.List&lt;PluginPrivilegeItem&gt;**](PluginPrivilegeItem.md)|  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, text/plain
 - **Accept**: application/json, text/plain

