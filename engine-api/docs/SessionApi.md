# SessionApi

All URIs are relative to *http://localhost/v1.41*

Method | HTTP request | Description | Integration tests
------------- | ------------- | ------------- | ---
[**session**](SessionApi.md#session) | **POST** /session | Initialize interactive session | ❌


<a name="session"></a>
# **session**
> session()

Initialize interactive session

Start a new interactive session with a server. Session allows server to call back to the client for advanced capabilities.  ### Hijacking  This endpoint hijacks the HTTP connection to HTTP2 transport that allows the client to expose gPRC services on that connection.  For example, the client sends this request to upgrade the connection:  &#x60;&#x60;&#x60; POST /session HTTP/1.1 Upgrade: h2c Connection: Upgrade &#x60;&#x60;&#x60;  The Docker daemon responds with a &#x60;101 UPGRADED&#x60; response follow with the raw stream:  &#x60;&#x60;&#x60; HTTP/1.1 101 UPGRADED Connection: Upgrade Upgrade: h2c &#x60;&#x60;&#x60;

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = SessionApi()
try {
    apiInstance.session()
} catch (e: ClientException) {
    println("4xx response calling SessionApi#session")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SessionApi#session")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/vnd.docker.raw-stream

