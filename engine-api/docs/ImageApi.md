# ImageApi

All URIs are relative to *http://localhost/v1.41*

Method | HTTP request | Description | Integration tests
------------- | ------------- | ------------- | ---
[**buildPrune**](ImageApi.md#buildPrune) | **POST** /build/prune | Delete builder cache | ✅
[**imageBuild**](ImageApi.md#imageBuild) | **POST** /build | Build an image | ✅
[**imageCommit**](ImageApi.md#imageCommit) | **POST** /commit | Create a new image from a container | ✅
[**imageCreate**](ImageApi.md#imageCreate) | **POST** /images/create | Create an image | ✅
[**imageDelete**](ImageApi.md#imageDelete) | **DELETE** /images/{name} | Remove an image | ✅
[**imageGet**](ImageApi.md#imageGet) | **GET** /images/{name}/get | Export an image | ✅
[**imageGetAll**](ImageApi.md#imageGetAll) | **GET** /images/get | Export several images | ✅
[**imageHistory**](ImageApi.md#imageHistory) | **GET** /images/{name}/history | Get the history of an image | ✅
[**imageInspect**](ImageApi.md#imageInspect) | **GET** /images/{name}/json | Inspect an image | ✅
[**imageList**](ImageApi.md#imageList) | **GET** /images/json | List Images | ✅
[**imageLoad**](ImageApi.md#imageLoad) | **POST** /images/load | Import images | ✅
[**imagePrune**](ImageApi.md#imagePrune) | **POST** /images/prune | Delete unused images | ✅
[**imagePush**](ImageApi.md#imagePush) | **POST** /images/{name}/push | Push an image | ✅
[**imageSearch**](ImageApi.md#imageSearch) | **GET** /images/search | Search images | ✅
[**imageTag**](ImageApi.md#imageTag) | **POST** /images/{name}/tag | Tag an image | ✅


<a name="buildPrune"></a>
# **buildPrune**
> BuildPruneResponse buildPrune(keepStorage, all, filters)

Delete builder cache

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ImageApi()
val keepStorage : kotlin.Long = 789 // kotlin.Long | Amount of disk space in bytes to keep for cache
val all : kotlin.Boolean = true // kotlin.Boolean | Remove all types of build cache
val filters : kotlin.String = filters_example // kotlin.String | A JSON encoded value of the filters (a `map[string][]string`) to process on the list of build cache objects.  Available filters:  - `until=<duration>`: duration relative to daemon's time, during which build cache was not used, in Go's duration format (e.g., '24h') - `id=<id>` - `parent=<id>` - `type=<string>` - `description=<string>` - `inuse` - `shared` - `private`
try {
    val result : BuildPruneResponse = apiInstance.buildPrune(keepStorage, all, filters)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ImageApi#buildPrune")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ImageApi#buildPrune")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **keepStorage** | **kotlin.Long**| Amount of disk space in bytes to keep for cache | [optional]
 **all** | **kotlin.Boolean**| Remove all types of build cache | [optional]
 **filters** | **kotlin.String**| A JSON encoded value of the filters (a &#x60;map[string][]string&#x60;) to process on the list of build cache objects.  Available filters:  - &#x60;until&#x3D;&lt;duration&gt;&#x60;: duration relative to daemon&#39;s time, during which build cache was not used, in Go&#39;s duration format (e.g., &#39;24h&#39;) - &#x60;id&#x3D;&lt;id&gt;&#x60; - &#x60;parent&#x3D;&lt;id&gt;&#x60; - &#x60;type&#x3D;&lt;string&gt;&#x60; - &#x60;description&#x3D;&lt;string&gt;&#x60; - &#x60;inuse&#x60; - &#x60;shared&#x60; - &#x60;private&#x60;  | [optional]

### Return type

[**BuildPruneResponse**](BuildPruneResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="imageBuild"></a>
# **imageBuild**
> imageBuild(dockerfile, t, extrahosts, remote, q, nocache, cachefrom, pull, rm, forcerm, memory, memswap, cpushares, cpusetcpus, cpuperiod, cpuquota, buildargs, shmsize, squash, labels, networkmode, contentType, xRegistryConfig, platform, target, outputs, inputStream)

Build an image

Build an image from a tar archive with a &#x60;Dockerfile&#x60; in it.  The &#x60;Dockerfile&#x60; specifies how the image is built from the tar archive. It is typically in the archive&#39;s root, but can be at a different path or have a different name by specifying the &#x60;dockerfile&#x60; parameter. [See the &#x60;Dockerfile&#x60; reference for more information](https://docs.docker.com/engine/reference/builder/).  The Docker daemon performs a preliminary validation of the &#x60;Dockerfile&#x60; before starting the build, and returns an error if the syntax is incorrect. After that, each instruction is run one-by-one until the ID of the new image is output.  The build is canceled if the client drops the connection by quitting or being killed.

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ImageApi()
val dockerfile : kotlin.String = dockerfile_example // kotlin.String | Path within the build context to the `Dockerfile`. This is ignored if `remote` is specified and points to an external `Dockerfile`.
val t : kotlin.String = t_example // kotlin.String | A name and optional tag to apply to the image in the `name:tag` format. If you omit the tag the default `latest` value is assumed. You can provide several `t` parameters.
val extrahosts : kotlin.String = extrahosts_example // kotlin.String | Extra hosts to add to /etc/hosts
val remote : kotlin.String = remote_example // kotlin.String | A Git repository URI or HTTP/HTTPS context URI. If the URI points to a single text file, the file’s contents are placed into a file called `Dockerfile` and the image is built from that file. If the URI points to a tarball, the file is downloaded by the daemon and the contents therein used as the context for the build. If the URI points to a tarball and the `dockerfile` parameter is also specified, there must be a file with the corresponding path inside the tarball.
val q : kotlin.Boolean = true // kotlin.Boolean | Suppress verbose build output.
val nocache : kotlin.Boolean = true // kotlin.Boolean | Do not use the cache when building the image.
val cachefrom : kotlin.String = cachefrom_example // kotlin.String | JSON array of images used for build cache resolution.
val pull : kotlin.String = pull_example // kotlin.String | Attempt to pull the image even if an older image exists locally.
val rm : kotlin.Boolean = true // kotlin.Boolean | Remove intermediate containers after a successful build.
val forcerm : kotlin.Boolean = true // kotlin.Boolean | Always remove intermediate containers, even upon failure.
val memory : kotlin.Int = 56 // kotlin.Int | Set memory limit for build.
val memswap : kotlin.Int = 56 // kotlin.Int | Total memory (memory + swap). Set as `-1` to disable swap.
val cpushares : kotlin.Int = 56 // kotlin.Int | CPU shares (relative weight).
val cpusetcpus : kotlin.String = cpusetcpus_example // kotlin.String | CPUs in which to allow execution (e.g., `0-3`, `0,1`).
val cpuperiod : kotlin.Int = 56 // kotlin.Int | The length of a CPU period in microseconds.
val cpuquota : kotlin.Int = 56 // kotlin.Int | Microseconds of CPU time that the container can get in a CPU period.
val buildargs : kotlin.String = buildargs_example // kotlin.String | JSON map of string pairs for build-time variables. Users pass these values at build-time. Docker uses the buildargs as the environment context for commands run via the `Dockerfile` RUN instruction, or for variable expansion in other `Dockerfile` instructions. This is not meant for passing secret values.  For example, the build arg `FOO=bar` would become `{\"FOO\":\"bar\"}` in JSON. This would result in the query parameter `buildargs={\"FOO\":\"bar\"}`. Note that `{\"FOO\":\"bar\"}` should be URI component encoded.  [Read more about the buildargs instruction.](https://docs.docker.com/engine/reference/builder/#arg)
val shmsize : kotlin.Int = 56 // kotlin.Int | Size of `/dev/shm` in bytes. The size must be greater than 0. If omitted the system uses 64MB.
val squash : kotlin.Boolean = true // kotlin.Boolean | Squash the resulting images layers into a single layer. *(Experimental release only.)*
val labels : kotlin.String = labels_example // kotlin.String | Arbitrary key/value labels to set on the image, as a JSON map of string pairs.
val networkmode : kotlin.String = networkmode_example // kotlin.String | Sets the networking mode for the run commands during build. Supported standard values are: `bridge`, `host`, `none`, and `container:<name|id>`. Any other value is taken as a custom network's name or ID to which this container should connect to.
val contentType : kotlin.String = contentType_example // kotlin.String |
val xRegistryConfig : kotlin.String = xRegistryConfig_example // kotlin.String | This is a base64-encoded JSON object with auth configurations for multiple registries that a build may refer to.  The key is a registry URL, and the value is an auth configuration object, [as described in the authentication section](#section/Authentication). For example:  ``` {   \"docker.example.com\": {     \"username\": \"janedoe\",     \"password\": \"hunter2\"   },   \"https://index.docker.io/v1/\": {     \"username\": \"mobydock\",     \"password\": \"conta1n3rize14\"   } } ```  Only the registry domain name (and port if not the default 443) are required. However, for legacy reasons, the Docker Hub registry must be specified with both a `https://` prefix and a `/v1/` suffix even though Docker will prefer to use the v2 registry API.
val platform : kotlin.String = platform_example // kotlin.String | Platform in the format os[/arch[/variant]]
val target : kotlin.String = target_example // kotlin.String | Target build stage
val outputs : kotlin.String = outputs_example // kotlin.String | BuildKit output configuration
val inputStream : java.io.File = BINARY_DATA_HERE // java.io.File | A tar archive compressed with one of the following algorithms: identity (no compression), gzip, bzip2, xz.
try {
    apiInstance.imageBuild(dockerfile, t, extrahosts, remote, q, nocache, cachefrom, pull, rm, forcerm, memory, memswap, cpushares, cpusetcpus, cpuperiod, cpuquota, buildargs, shmsize, squash, labels, networkmode, contentType, xRegistryConfig, platform, target, outputs, inputStream)
} catch (e: ClientException) {
    println("4xx response calling ImageApi#imageBuild")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ImageApi#imageBuild")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **dockerfile** | **kotlin.String**| Path within the build context to the &#x60;Dockerfile&#x60;. This is ignored if &#x60;remote&#x60; is specified and points to an external &#x60;Dockerfile&#x60;. | [optional] [default to &quot;Dockerfile&quot;]
 **t** | **kotlin.String**| A name and optional tag to apply to the image in the &#x60;name:tag&#x60; format. If you omit the tag the default &#x60;latest&#x60; value is assumed. You can provide several &#x60;t&#x60; parameters. | [optional]
 **extrahosts** | **kotlin.String**| Extra hosts to add to /etc/hosts | [optional]
 **remote** | **kotlin.String**| A Git repository URI or HTTP/HTTPS context URI. If the URI points to a single text file, the file’s contents are placed into a file called &#x60;Dockerfile&#x60; and the image is built from that file. If the URI points to a tarball, the file is downloaded by the daemon and the contents therein used as the context for the build. If the URI points to a tarball and the &#x60;dockerfile&#x60; parameter is also specified, there must be a file with the corresponding path inside the tarball. | [optional]
 **q** | **kotlin.Boolean**| Suppress verbose build output. | [optional] [default to false]
 **nocache** | **kotlin.Boolean**| Do not use the cache when building the image. | [optional] [default to false]
 **cachefrom** | **kotlin.String**| JSON array of images used for build cache resolution. | [optional]
 **pull** | **kotlin.String**| Attempt to pull the image even if an older image exists locally. | [optional]
 **rm** | **kotlin.Boolean**| Remove intermediate containers after a successful build. | [optional] [default to true]
 **forcerm** | **kotlin.Boolean**| Always remove intermediate containers, even upon failure. | [optional] [default to false]
 **memory** | **kotlin.Int**| Set memory limit for build. | [optional]
 **memswap** | **kotlin.Int**| Total memory (memory + swap). Set as &#x60;-1&#x60; to disable swap. | [optional]
 **cpushares** | **kotlin.Int**| CPU shares (relative weight). | [optional]
 **cpusetcpus** | **kotlin.String**| CPUs in which to allow execution (e.g., &#x60;0-3&#x60;, &#x60;0,1&#x60;). | [optional]
 **cpuperiod** | **kotlin.Int**| The length of a CPU period in microseconds. | [optional]
 **cpuquota** | **kotlin.Int**| Microseconds of CPU time that the container can get in a CPU period. | [optional]
 **buildargs** | **kotlin.String**| JSON map of string pairs for build-time variables. Users pass these values at build-time. Docker uses the buildargs as the environment context for commands run via the &#x60;Dockerfile&#x60; RUN instruction, or for variable expansion in other &#x60;Dockerfile&#x60; instructions. This is not meant for passing secret values.  For example, the build arg &#x60;FOO&#x3D;bar&#x60; would become &#x60;{\&quot;FOO\&quot;:\&quot;bar\&quot;}&#x60; in JSON. This would result in the query parameter &#x60;buildargs&#x3D;{\&quot;FOO\&quot;:\&quot;bar\&quot;}&#x60;. Note that &#x60;{\&quot;FOO\&quot;:\&quot;bar\&quot;}&#x60; should be URI component encoded.  [Read more about the buildargs instruction.](https://docs.docker.com/engine/reference/builder/#arg)  | [optional]
 **shmsize** | **kotlin.Int**| Size of &#x60;/dev/shm&#x60; in bytes. The size must be greater than 0. If omitted the system uses 64MB. | [optional]
 **squash** | **kotlin.Boolean**| Squash the resulting images layers into a single layer. *(Experimental release only.)* | [optional]
 **labels** | **kotlin.String**| Arbitrary key/value labels to set on the image, as a JSON map of string pairs. | [optional]
 **networkmode** | **kotlin.String**| Sets the networking mode for the run commands during build. Supported standard values are: &#x60;bridge&#x60;, &#x60;host&#x60;, &#x60;none&#x60;, and &#x60;container:&lt;name|id&gt;&#x60;. Any other value is taken as a custom network&#39;s name or ID to which this container should connect to.  | [optional]
 **contentType** | **kotlin.String**|  | [optional] [default to application/x-tar] [enum: application/x-tar]
 **xRegistryConfig** | **kotlin.String**| This is a base64-encoded JSON object with auth configurations for multiple registries that a build may refer to.  The key is a registry URL, and the value is an auth configuration object, [as described in the authentication section](#section/Authentication). For example:  &#x60;&#x60;&#x60; {   \&quot;docker.example.com\&quot;: {     \&quot;username\&quot;: \&quot;janedoe\&quot;,     \&quot;password\&quot;: \&quot;hunter2\&quot;   },   \&quot;https://index.docker.io/v1/\&quot;: {     \&quot;username\&quot;: \&quot;mobydock\&quot;,     \&quot;password\&quot;: \&quot;conta1n3rize14\&quot;   } } &#x60;&#x60;&#x60;  Only the registry domain name (and port if not the default 443) are required. However, for legacy reasons, the Docker Hub registry must be specified with both a &#x60;https://&#x60; prefix and a &#x60;/v1/&#x60; suffix even though Docker will prefer to use the v2 registry API.  | [optional]
 **platform** | **kotlin.String**| Platform in the format os[/arch[/variant]] | [optional]
 **target** | **kotlin.String**| Target build stage | [optional]
 **outputs** | **kotlin.String**| BuildKit output configuration | [optional]
 **inputStream** | **java.io.File**| A tar archive compressed with one of the following algorithms: identity (no compression), gzip, bzip2, xz. | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/octet-stream
 - **Accept**: application/json

<a name="imageCommit"></a>
# **imageCommit**
> IdResponse imageCommit(container, repo, tag, comment, author, pause, changes, containerConfig)

Create a new image from a container

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ImageApi()
val container : kotlin.String = container_example // kotlin.String | The ID or name of the container to commit
val repo : kotlin.String = repo_example // kotlin.String | Repository name for the created image
val tag : kotlin.String = tag_example // kotlin.String | Tag name for the create image
val comment : kotlin.String = comment_example // kotlin.String | Commit message
val author : kotlin.String = author_example // kotlin.String | Author of the image (e.g., `John Hannibal Smith <hannibal@a-team.com>`)
val pause : kotlin.Boolean = true // kotlin.Boolean | Whether to pause the container before committing
val changes : kotlin.String = changes_example // kotlin.String | `Dockerfile` instructions to apply while committing
val containerConfig : ContainerConfig =  // ContainerConfig | The container configuration
try {
    val result : IdResponse = apiInstance.imageCommit(container, repo, tag, comment, author, pause, changes, containerConfig)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ImageApi#imageCommit")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ImageApi#imageCommit")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **container** | **kotlin.String**| The ID or name of the container to commit | [optional]
 **repo** | **kotlin.String**| Repository name for the created image | [optional]
 **tag** | **kotlin.String**| Tag name for the create image | [optional]
 **comment** | **kotlin.String**| Commit message | [optional]
 **author** | **kotlin.String**| Author of the image (e.g., &#x60;John Hannibal Smith &lt;hannibal@a-team.com&gt;&#x60;) | [optional]
 **pause** | **kotlin.Boolean**| Whether to pause the container before committing | [optional] [default to true]
 **changes** | **kotlin.String**| &#x60;Dockerfile&#x60; instructions to apply while committing | [optional]
 **containerConfig** | [**ContainerConfig**](ContainerConfig.md)| The container configuration | [optional]

### Return type

[**IdResponse**](IdResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="imageCreate"></a>
# **imageCreate**
> imageCreate(fromImage, fromSrc, repo, tag, message, xRegistryAuth, changes, platform, inputImage)

Create an image

Create an image by either pulling it from a registry or importing it.

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ImageApi()
val fromImage : kotlin.String = fromImage_example // kotlin.String | Name of the image to pull. The name may include a tag or digest. This parameter may only be used when pulling an image. The pull is cancelled if the HTTP connection is closed.
val fromSrc : kotlin.String = fromSrc_example // kotlin.String | Source to import. The value may be a URL from which the image can be retrieved or `-` to read the image from the request body. This parameter may only be used when importing an image.
val repo : kotlin.String = repo_example // kotlin.String | Repository name given to an image when it is imported. The repo may include a tag. This parameter may only be used when importing an image.
val tag : kotlin.String = tag_example // kotlin.String | Tag or digest. If empty when pulling an image, this causes all tags for the given image to be pulled.
val message : kotlin.String = message_example // kotlin.String | Set commit message for imported image.
val xRegistryAuth : kotlin.String = xRegistryAuth_example // kotlin.String | A base64url-encoded auth configuration.  Refer to the [authentication section](#section/Authentication) for details.
val changes : kotlin.collections.List<kotlin.String> =  // kotlin.collections.List<kotlin.String> | Apply Dockerfile instruction to the created image.
val platform : kotlin.String = platform_example // kotlin.String | Platform in the format os[/arch[/variant]]
val inputImage : kotlin.String = inputImage_example // kotlin.String | Image content if the value `-` has been specified in fromSrc query parameter
try {
    apiInstance.imageCreate(fromImage, fromSrc, repo, tag, message, xRegistryAuth, changes, platform, inputImage)
} catch (e: ClientException) {
    println("4xx response calling ImageApi#imageCreate")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ImageApi#imageCreate")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **fromImage** | **kotlin.String**| Name of the image to pull. The name may include a tag or digest. This parameter may only be used when pulling an image. The pull is cancelled if the HTTP connection is closed. | [optional]
 **fromSrc** | **kotlin.String**| Source to import. The value may be a URL from which the image can be retrieved or &#x60;-&#x60; to read the image from the request body. This parameter may only be used when importing an image. | [optional]
 **repo** | **kotlin.String**| Repository name given to an image when it is imported. The repo may include a tag. This parameter may only be used when importing an image. | [optional]
 **tag** | **kotlin.String**| Tag or digest. If empty when pulling an image, this causes all tags for the given image to be pulled. | [optional]
 **message** | **kotlin.String**| Set commit message for imported image. | [optional]
 **xRegistryAuth** | **kotlin.String**| A base64url-encoded auth configuration.  Refer to the [authentication section](#section/Authentication) for details.  | [optional]
 **changes** | [**kotlin.collections.List&lt;kotlin.String&gt;**](kotlin.String.md)| Apply Dockerfile instruction to the created image. | [optional]
 **platform** | **kotlin.String**| Platform in the format os[/arch[/variant]] | [optional]
 **inputImage** | **kotlin.String**| Image content if the value &#x60;-&#x60; has been specified in fromSrc query parameter | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: text/plain, application/octet-stream
 - **Accept**: application/json

<a name="imageDelete"></a>
# **imageDelete**
> kotlin.collections.List&lt;ImageDeleteResponseItem&gt; imageDelete(name, force, noprune)

Remove an image

Remove an image, along with any untagged parent images that were referenced by that image.  Images can&#39;t be removed if they have descendant images, are being used by a running container or are being used by a build.

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ImageApi()
val name : kotlin.String = name_example // kotlin.String | Image name or ID
val force : kotlin.Boolean = true // kotlin.Boolean | Remove the image even if it is being used by stopped containers or has other tags
val noprune : kotlin.Boolean = true // kotlin.Boolean | Do not delete untagged parent images
try {
    val result : kotlin.collections.List<ImageDeleteResponseItem> = apiInstance.imageDelete(name, force, noprune)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ImageApi#imageDelete")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ImageApi#imageDelete")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **kotlin.String**| Image name or ID |
 **force** | **kotlin.Boolean**| Remove the image even if it is being used by stopped containers or has other tags | [optional] [default to false]
 **noprune** | **kotlin.Boolean**| Do not delete untagged parent images | [optional] [default to false]

### Return type

[**kotlin.collections.List&lt;ImageDeleteResponseItem&gt;**](ImageDeleteResponseItem.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="imageGet"></a>
# **imageGet**
> java.io.File imageGet(name)

Export an image

Get a tarball containing all images and metadata for a repository.  If &#x60;name&#x60; is a specific name and tag (e.g. &#x60;ubuntu:latest&#x60;), then only that image (and its parents) are returned. If &#x60;name&#x60; is an image ID, similarly only that image (and its parents) are returned, but with the exclusion of the &#x60;repositories&#x60; file in the tarball, as there were no image names referenced.  ### Image tarball format  An image tarball contains one directory per image layer (named using its long ID), each containing these files:  - &#x60;VERSION&#x60;: currently &#x60;1.0&#x60; - the file format version - &#x60;json&#x60;: detailed layer information, similar to &#x60;docker inspect layer_id&#x60; - &#x60;layer.tar&#x60;: A tarfile containing the filesystem changes in this layer  The &#x60;layer.tar&#x60; file contains &#x60;aufs&#x60; style &#x60;.wh..wh.aufs&#x60; files and directories for storing attribute changes and deletions.  If the tarball defines a repository, the tarball should also include a &#x60;repositories&#x60; file at the root that contains a list of repository and tag names mapped to layer IDs.  &#x60;&#x60;&#x60;json {   \&quot;hello-world\&quot;: {     \&quot;latest\&quot;: \&quot;565a9d68a73f6706862bfe8409a7f659776d4d60a8d096eb4a3cbce6999cc2a1\&quot;   } } &#x60;&#x60;&#x60;

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ImageApi()
val name : kotlin.String = name_example // kotlin.String | Image name or ID
try {
    val result : java.io.File = apiInstance.imageGet(name)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ImageApi#imageGet")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ImageApi#imageGet")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **kotlin.String**| Image name or ID |

### Return type

[**java.io.File**](java.io.File.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/x-tar

<a name="imageGetAll"></a>
# **imageGetAll**
> java.io.File imageGetAll(names)

Export several images

Get a tarball containing all images and metadata for several image repositories.  For each value of the &#x60;names&#x60; parameter: if it is a specific name and tag (e.g. &#x60;ubuntu:latest&#x60;), then only that image (and its parents) are returned; if it is an image ID, similarly only that image (and its parents) are returned and there would be no names referenced in the &#39;repositories&#39; file for this image ID.  For details on the format, see the [export image endpoint](#operation/ImageGet).

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ImageApi()
val names : kotlin.collections.List<kotlin.String> =  // kotlin.collections.List<kotlin.String> | Image names to filter by
try {
    val result : java.io.File = apiInstance.imageGetAll(names)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ImageApi#imageGetAll")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ImageApi#imageGetAll")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **names** | [**kotlin.collections.List&lt;kotlin.String&gt;**](kotlin.String.md)| Image names to filter by | [optional]

### Return type

[**java.io.File**](java.io.File.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/x-tar

<a name="imageHistory"></a>
# **imageHistory**
> kotlin.collections.List&lt;HistoryResponseItem&gt; imageHistory(name)

Get the history of an image

Return parent layers of an image.

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ImageApi()
val name : kotlin.String = name_example // kotlin.String | Image name or ID
try {
    val result : kotlin.collections.List<HistoryResponseItem> = apiInstance.imageHistory(name)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ImageApi#imageHistory")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ImageApi#imageHistory")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **kotlin.String**| Image name or ID |

### Return type

[**kotlin.collections.List&lt;HistoryResponseItem&gt;**](HistoryResponseItem.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="imageInspect"></a>
# **imageInspect**
> Image imageInspect(name)

Inspect an image

Return low-level information about an image.

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ImageApi()
val name : kotlin.String = name_example // kotlin.String | Image name or id
try {
    val result : Image = apiInstance.imageInspect(name)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ImageApi#imageInspect")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ImageApi#imageInspect")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **kotlin.String**| Image name or id |

### Return type

[**Image**](Image.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="imageList"></a>
# **imageList**
> kotlin.collections.List&lt;ImageSummary&gt; imageList(all, filters, digests)

List Images

Returns a list of images on the server. Note that it uses a different, smaller representation of an image than inspecting a single image.

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ImageApi()
val all : kotlin.Boolean = true // kotlin.Boolean | Show all images. Only images from a final layer (no children) are shown by default.
val filters : kotlin.String = filters_example // kotlin.String | A JSON encoded value of the filters (a `map[string][]string`) to process on the images list.  Available filters:  - `before`=(`<image-name>[:<tag>]`,  `<image id>` or `<image@digest>`) - `dangling=true` - `label=key` or `label=\"key=value\"` of an image label - `reference`=(`<image-name>[:<tag>]`) - `since`=(`<image-name>[:<tag>]`,  `<image id>` or `<image@digest>`)
val digests : kotlin.Boolean = true // kotlin.Boolean | Show digest information as a `RepoDigests` field on each image.
try {
    val result : kotlin.collections.List<ImageSummary> = apiInstance.imageList(all, filters, digests)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ImageApi#imageList")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ImageApi#imageList")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **all** | **kotlin.Boolean**| Show all images. Only images from a final layer (no children) are shown by default. | [optional] [default to false]
 **filters** | **kotlin.String**| A JSON encoded value of the filters (a &#x60;map[string][]string&#x60;) to process on the images list.  Available filters:  - &#x60;before&#x60;&#x3D;(&#x60;&lt;image-name&gt;[:&lt;tag&gt;]&#x60;,  &#x60;&lt;image id&gt;&#x60; or &#x60;&lt;image@digest&gt;&#x60;) - &#x60;dangling&#x3D;true&#x60; - &#x60;label&#x3D;key&#x60; or &#x60;label&#x3D;\&quot;key&#x3D;value\&quot;&#x60; of an image label - &#x60;reference&#x60;&#x3D;(&#x60;&lt;image-name&gt;[:&lt;tag&gt;]&#x60;) - &#x60;since&#x60;&#x3D;(&#x60;&lt;image-name&gt;[:&lt;tag&gt;]&#x60;,  &#x60;&lt;image id&gt;&#x60; or &#x60;&lt;image@digest&gt;&#x60;)  | [optional]
 **digests** | **kotlin.Boolean**| Show digest information as a &#x60;RepoDigests&#x60; field on each image. | [optional] [default to false]

### Return type

[**kotlin.collections.List&lt;ImageSummary&gt;**](ImageSummary.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="imageLoad"></a>
# **imageLoad**
> imageLoad(quiet, imagesTarball)

Import images

Load a set of images and tags into a repository.  For details on the format, see the [export image endpoint](#operation/ImageGet).

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ImageApi()
val quiet : kotlin.Boolean = true // kotlin.Boolean | Suppress progress details during load.
val imagesTarball : java.io.File = BINARY_DATA_HERE // java.io.File | Tar archive containing images
try {
    apiInstance.imageLoad(quiet, imagesTarball)
} catch (e: ClientException) {
    println("4xx response calling ImageApi#imageLoad")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ImageApi#imageLoad")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **quiet** | **kotlin.Boolean**| Suppress progress details during load. | [optional] [default to false]
 **imagesTarball** | **java.io.File**| Tar archive containing images | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/x-tar
 - **Accept**: application/json

<a name="imagePrune"></a>
# **imagePrune**
> ImagePruneResponse imagePrune(filters)

Delete unused images

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ImageApi()
val filters : kotlin.String = filters_example // kotlin.String | Filters to process on the prune list, encoded as JSON (a `map[string][]string`). Available filters:  - `dangling=<boolean>` When set to `true` (or `1`), prune only    unused *and* untagged images. When set to `false`    (or `0`), all unused images are pruned. - `until=<string>` Prune images created before this timestamp. The `<timestamp>` can be Unix timestamps, date formatted timestamps, or Go duration strings (e.g. `10m`, `1h30m`) computed relative to the daemon machine’s time. - `label` (`label=<key>`, `label=<key>=<value>`, `label!=<key>`, or `label!=<key>=<value>`) Prune images with (or without, in case `label!=...` is used) the specified labels.
try {
    val result : ImagePruneResponse = apiInstance.imagePrune(filters)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ImageApi#imagePrune")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ImageApi#imagePrune")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **filters** | **kotlin.String**| Filters to process on the prune list, encoded as JSON (a &#x60;map[string][]string&#x60;). Available filters:  - &#x60;dangling&#x3D;&lt;boolean&gt;&#x60; When set to &#x60;true&#x60; (or &#x60;1&#x60;), prune only    unused *and* untagged images. When set to &#x60;false&#x60;    (or &#x60;0&#x60;), all unused images are pruned. - &#x60;until&#x3D;&lt;string&gt;&#x60; Prune images created before this timestamp. The &#x60;&lt;timestamp&gt;&#x60; can be Unix timestamps, date formatted timestamps, or Go duration strings (e.g. &#x60;10m&#x60;, &#x60;1h30m&#x60;) computed relative to the daemon machine’s time. - &#x60;label&#x60; (&#x60;label&#x3D;&lt;key&gt;&#x60;, &#x60;label&#x3D;&lt;key&gt;&#x3D;&lt;value&gt;&#x60;, &#x60;label!&#x3D;&lt;key&gt;&#x60;, or &#x60;label!&#x3D;&lt;key&gt;&#x3D;&lt;value&gt;&#x60;) Prune images with (or without, in case &#x60;label!&#x3D;...&#x60; is used) the specified labels.  | [optional]

### Return type

[**ImagePruneResponse**](ImagePruneResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="imagePush"></a>
# **imagePush**
> imagePush(name, xRegistryAuth, tag)

Push an image

Push an image to a registry.  If you wish to push an image on to a private registry, that image must already have a tag which references the registry. For example, &#x60;registry.example.com/myimage:latest&#x60;.  The push is cancelled if the HTTP connection is closed.

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ImageApi()
val name : kotlin.String = name_example // kotlin.String | Image name or ID.
val xRegistryAuth : kotlin.String = xRegistryAuth_example // kotlin.String | A base64url-encoded auth configuration.  Refer to the [authentication section](#section/Authentication) for details.
val tag : kotlin.String = tag_example // kotlin.String | The tag to associate with the image on the registry.
try {
    apiInstance.imagePush(name, xRegistryAuth, tag)
} catch (e: ClientException) {
    println("4xx response calling ImageApi#imagePush")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ImageApi#imagePush")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **kotlin.String**| Image name or ID. |
 **xRegistryAuth** | **kotlin.String**| A base64url-encoded auth configuration.  Refer to the [authentication section](#section/Authentication) for details.  |
 **tag** | **kotlin.String**| The tag to associate with the image on the registry. | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

<a name="imageSearch"></a>
# **imageSearch**
> kotlin.collections.List&lt;ImageSearchResponseItem&gt; imageSearch(term, limit, filters)

Search images

Search for an image on Docker Hub.

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ImageApi()
val term : kotlin.String = term_example // kotlin.String | Term to search
val limit : kotlin.Int = 56 // kotlin.Int | Maximum number of results to return
val filters : kotlin.String = filters_example // kotlin.String | A JSON encoded value of the filters (a `map[string][]string`) to process on the images list. Available filters:  - `is-automated=(true|false)` - `is-official=(true|false)` - `stars=<number>` Matches images that has at least 'number' stars.
try {
    val result : kotlin.collections.List<ImageSearchResponseItem> = apiInstance.imageSearch(term, limit, filters)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ImageApi#imageSearch")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ImageApi#imageSearch")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **term** | **kotlin.String**| Term to search |
 **limit** | **kotlin.Int**| Maximum number of results to return | [optional]
 **filters** | **kotlin.String**| A JSON encoded value of the filters (a &#x60;map[string][]string&#x60;) to process on the images list. Available filters:  - &#x60;is-automated&#x3D;(true|false)&#x60; - &#x60;is-official&#x3D;(true|false)&#x60; - &#x60;stars&#x3D;&lt;number&gt;&#x60; Matches images that has at least &#39;number&#39; stars.  | [optional]

### Return type

[**kotlin.collections.List&lt;ImageSearchResponseItem&gt;**](ImageSearchResponseItem.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="imageTag"></a>
# **imageTag**
> imageTag(name, repo, tag)

Tag an image

Tag an image so that it becomes part of a repository.

### Example
```kotlin
// Import classes:
//import de.gesellix.docker.engine.client.infrastructure.*
//import de.gesellix.docker.engine.model.*

val apiInstance = ImageApi()
val name : kotlin.String = name_example // kotlin.String | Image name or ID to tag.
val repo : kotlin.String = repo_example // kotlin.String | The repository to tag in. For example, `someuser/someimage`.
val tag : kotlin.String = tag_example // kotlin.String | The name of the new tag.
try {
    apiInstance.imageTag(name, repo, tag)
} catch (e: ClientException) {
    println("4xx response calling ImageApi#imageTag")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ImageApi#imageTag")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **kotlin.String**| Image name or ID to tag. |
 **repo** | **kotlin.String**| The repository to tag in. For example, &#x60;someuser/someimage&#x60;. | [optional]
 **tag** | **kotlin.String**| The name of the new tag. | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

