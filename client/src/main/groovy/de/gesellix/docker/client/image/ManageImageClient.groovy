package de.gesellix.docker.client.image

import de.gesellix.docker.client.DockerAsyncCallback
import de.gesellix.docker.client.DockerAsyncConsumer
import de.gesellix.docker.client.DockerClientException
import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.Timeout
import de.gesellix.docker.client.repository.RepositoryTagParser
import de.gesellix.docker.engine.EngineClient
import de.gesellix.docker.engine.EngineResponse
import de.gesellix.util.QueryUtil
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

import java.util.concurrent.CountDownLatch

import static java.util.concurrent.Executors.newSingleThreadExecutor

@Slf4j
class ManageImageClient implements ManageImage {

    private EngineClient client
    private DockerResponseHandler responseHandler
    private RepositoryTagParser repositoryTagParser
    private QueryUtil queryUtil

    ManageImageClient(EngineClient client, DockerResponseHandler responseHandler) {
        this.client = client
        this.responseHandler = responseHandler
        this.repositoryTagParser = new RepositoryTagParser()
        this.queryUtil = new QueryUtil()
    }

    /**
     * @deprecated use buildWithLogs(java.io.InputStream, de.gesellix.docker.client.image.BuildConfig)
     * @see #buildWithLogs(java.io.InputStream, de.gesellix.docker.client.image.BuildConfig)
     */
    @Deprecated
    @Override
    buildWithLogs(InputStream buildContext, Map query, Timeout timeout = null) {
        return buildWithLogs(buildContext, new BuildConfig(query: query, timeout: timeout))
    }

    @Override
    BuildResult buildWithLogs(InputStream buildContext, BuildConfig buildConfig = new BuildConfig()) {
        if (buildConfig.callback) {
            throw new UnsupportedOperationException("Currently cannot handle two callbacks.")
        }

        def buildLatch = new CountDownLatch(1)
        def chunks = []
        def callback = new DockerAsyncCallback() {

            @Override
            onEvent(Object event) {
                log.info "$event"
                def parsedEvent = new JsonSlurper().parseText(event as String)
                chunks << parsedEvent
            }

            @Override
            onFinish() {
                log.debug "build finished"
                buildLatch.countDown()
            }
        }
        buildConfig.callback = callback
        def asyncBuildResponse = build(buildContext, buildConfig)

        def builtInTime = buildLatch.await(buildConfig.timeout.timeout, buildConfig.timeout.unit)
        asyncBuildResponse.response.taskFuture.cancel(false)

        def lastLogEvent
        if (chunks.empty) {
            log.warn("no build log collected - timeout of ${buildConfig.timeout} reached?")
            lastLogEvent = null
        }
        else {
            lastLogEvent = chunks.last()
        }

        if (!builtInTime) {
            throw new DockerClientException(new RuntimeException("docker build timeout"), lastLogEvent)
        }
        if (lastLogEvent?.error) {
            throw new DockerClientException(new RuntimeException("docker build failed"), lastLogEvent)
        }
        return [log    : chunks,
                imageId: getBuildResultAsImageId(chunks)]
    }

    /**
     * @deprecated use build(java.io.InputStream, de.gesellix.docker.client.image.BuildConfig)
     * @see #build(java.io.InputStream, de.gesellix.docker.client.image.BuildConfig)
     */
    @Deprecated
    @Override
    build(InputStream buildContext, Map query, DockerAsyncCallback callback = null) {
        BuildResult result = build(buildContext, new BuildConfig(query: query, callback: callback))
        if (callback) {
            return result.response
        }
        else {
            return result.imageId
        }
    }

    @Override
    BuildResult build(InputStream buildContext, BuildConfig config = new BuildConfig()) {
        EngineResponse response = buildAsync(buildContext, config, config.callback)
        if (config.callback) {
            def executor = newSingleThreadExecutor()
            def future = executor.submit(new DockerAsyncConsumer(response as EngineResponse, config.callback))
            response.taskFuture = future
            return new BuildResult(response: response)
        }
        else {
            String imageId = getBuildResultAsImageId(response.content as List)
            return new BuildResult(response: response, imageId: imageId)
        }
    }

    EngineResponse buildAsync(InputStream buildContext, BuildConfig config = new BuildConfig(), DockerAsyncCallback callback) {
        log.info "docker build"
        def actualQuery = config.query ?: [:]
        queryUtil.jsonEncodeBuildargs(actualQuery)
        def actualBuildOptions = config.options ?: [:]
        def request = [path              : "/build",
                       query             : actualQuery,
                       body              : buildContext,
                       requestContentType: "application/octet-stream",
                       async             : callback ? true : false]
        if (actualBuildOptions.EncodedRegistryConfig) {
            request.headers = ["X-Registry-Config": actualBuildOptions.EncodedRegistryConfig as String]
        }
        def response = client.post(request)

        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker build failed"))

        return response
    }

    String getBuildResultAsImageId(List<Map<String, String>> chunks) {
        def reversedChunks = chunks.reverse()
        def buildResultMessage = reversedChunks.find { Map<String, String> chunk ->
            chunk.aux?.ID
        }
        if (buildResultMessage) {
            return buildResultMessage.aux.ID
        }
//        throw new IllegalStateException("Couldn't find image id in build output.")

        log.info("Couldn't find aux.ID in build output, trying via fallback.")

        buildResultMessage = reversedChunks.find { Map<String, String> chunk ->
            chunk.stream?.trim()?.startsWith("Successfully built ")
        }
        return buildResultMessage.stream.trim() - "Successfully built "
    }

    @Override
    EngineResponse history(imageId) {
        log.info "docker history"
        def response = client.get([path: "/images/${imageId}/history"])
        return response
    }

    @Override
    importUrl(url, repository = "", tag = "") {
        log.info "docker import '${url}' into ${repository}:${tag}"

        def response = client.post([path : "/images/create",
                                    query: [fromSrc: url.toString(),
                                            repo   : repository ?: "",
                                            tag    : tag ?: ""]])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker import from url failed"))

        def responseBody = response.content
        return responseBody.status.last()
    }

    @Override
    String importStream(stream, repository = "", tag = "") {
        log.info "docker import stream into ${repository}:${tag}"

        def response = client.post([path              : "/images/create",
                                    body              : stream,
                                    query             : [fromSrc: "-",
                                                         repo   : repository ?: "",
                                                         tag    : tag ?: ""],
                                    requestContentType: "application/x-tar"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker import from stream failed"))

        def responseBody = response.content
        return responseBody.status
    }

    @Override
    EngineResponse inspectImage(imageId) {
        log.info "docker inspect image"
        def response = client.get([path: "/images/${imageId}/json"])
        return response
    }

    @Override
    EngineResponse load(stream) {
        log.info "docker load"

        def response = client.post([path              : "/images/load",
                                    body              : stream,
                                    requestContentType: "application/x-tar"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker load failed"))

        return response
    }

    @Override
    EngineResponse images(query = [:]) {
        log.info "docker images"
        def actualQuery = query ?: [:]
        def defaults = [all: false]
        queryUtil.applyDefaults(actualQuery, defaults)
        queryUtil.jsonEncodeFilters(actualQuery)
        def response = client.get([path : "/images/json",
                                   query: actualQuery])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker images failed"))
        return response
    }

    @Override
    EngineResponse pruneImages(query = [:]) {
        log.info "docker image prune"
        def actualQuery = query ?: [:]
        queryUtil.jsonEncodeFilters(actualQuery)
        def response = client.post([path : "/images/prune",
                                    query: actualQuery])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker image prune failed"))
        return response
    }

    @Override
    EngineResponse create(Map query = [:], Map createOptions = [:]) {
        log.info "docker image create"
        createOptions = createOptions ?: [:]
        def headers = [:]
        if (createOptions.EncodedRegistryAuth) {
            headers["X-Registry-Auth"] = createOptions.EncodedRegistryAuth as String
        }
        def actualQuery = query ?: [:]
        def response = client.post([path   : "/images/create",
                                    query  : actualQuery,
                                    headers: headers])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker images create failed"))
        return response
    }

    /**
     * @deprecated please use #create(query, createOptions)
     * @see #create(Map, Map)
     */
    @Deprecated
    @Override
    String pull(imageName, String tag = "", String authBase64Encoded = ".", String registry = "") {
        log.info "docker pull '${imageName}:${tag}'"

        def actualImageName = imageName
        if (registry) {
            actualImageName = "$registry/$imageName".toString()
        }

        def response = create([fromImage: actualImageName,
                               tag      : tag],
                              [EncodedRegistryAuth: authBase64Encoded])
//        println new JsonBuilder(response.content).toString()
        if (response.status.success) {
            return findImageId(actualImageName, tag)
        }
        else {
            return null
        }
    }

    @Override
    EngineResponse push(String imageName, String authBase64Encoded = ".", String registry = "") {
        log.info "docker push '${imageName}'"

        def actualImageName = imageName
        if (registry) {
            actualImageName = "$registry/$imageName".toString()
            tag(imageName, actualImageName)
        }
        def repoAndTag = repositoryTagParser.parseRepositoryTag(actualImageName)

        def response = client.post([path   : "/images/${repoAndTag.repo}/push".toString(),
                                    query  : [tag: repoAndTag.tag],
                                    headers: ["X-Registry-Auth": authBase64Encoded ?: "."]])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker push failed"))
        return response
    }

    @Override
    EngineResponse rmi(String imageId) {
        log.info "docker rmi"
        def response = client.delete([path: "/images/${imageId}".toString()])
        return response
    }

    @Override
    EngineResponse save(... images) {
        log.info "docker save"

        def response
        if (images.length == 1) {
            response = client.get([path: "/images/${images.first()}/get"])
        }
        else {
            response = client.get([path : "/images/get",
                                   query: [names: images]])
        }
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker save failed"))

        return response
    }

    @Override
    EngineResponse tag(imageId, repository) {
        log.info "docker tag"
        def repoAndTag = repositoryTagParser.parseRepositoryTag(repository)
        def response = client.post([path : "/images/${imageId}/tag".toString(),
                                    query: [repo: repoAndTag.repo,
                                            tag : repoAndTag.tag]])
        return response
    }

    @Override
    String findImageId(imageName, tag = "") {
        def isDigest = imageName.contains '@'
        def images = images((isDigest) ? [digests: '1'] : [:]).content
//        println new JsonBuilder(images).toString()
        def imageIdsByRepoDigest = images.collectEntries { image ->
            image.RepoDigests?.collectEntries { repoDigest ->
                def idByDigest = [:]
                idByDigest[repoDigest] = image.Id
                idByDigest
            } ?: [:]
        }
        def imageIdsByName = images.collectEntries { image ->
            image.RepoTags?.collectEntries { repoTag ->
                def idByName = [:]
                idByName[repoTag] = image.Id
                idByName
            } ?: [:]
        }

        if (isDigest) {
            if (imageIdsByRepoDigest[imageName.toString()]) {
                return imageIdsByRepoDigest[imageName.toString()]
            }
            log.warn("couldn't find imageId for `${imageName}` via `docker images`")
            return imageName
        }
        else {
            def canonicalImageName = "$imageName:${tag ?: 'latest'}".toString()
            if (imageIdsByName[canonicalImageName]) {
                return imageIdsByName[canonicalImageName]
            }
            log.warn("couldn't find imageId for `${canonicalImageName}` via `docker images`")
            return canonicalImageName
        }
    }
}
