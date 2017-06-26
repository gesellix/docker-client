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

import static de.gesellix.docker.client.Timeout.TEN_MINUTES
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

    @Override
    buildWithLogs(InputStream buildContext, query = ["rm": true], Timeout timeout = TEN_MINUTES) {
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
        def asyncBuildResponse = build(buildContext, query, callback)

        def builtInTime = buildLatch.await(timeout.timeout, timeout.unit)
        asyncBuildResponse.taskFuture.cancel(false)

        def lastLogEvent
        if (chunks.empty) {
            log.warn("no build log collected - timeout of ${timeout} reached?")
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

    @Override
    build(InputStream buildContext, query = ["rm": true], DockerAsyncCallback callback = null) {
        log.info "docker build"
        def async = callback ? true : false
        def actualQuery = query ?: [:]
        queryUtil.jsonEncodeBuildargs(actualQuery)
        def response = client.post([path              : "/build",
                                    query             : actualQuery,
                                    body              : buildContext,
                                    requestContentType: "application/octet-stream",
                                    async             : async])

        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker build failed"))

        if (async) {
            def executor = newSingleThreadExecutor()
            def future = executor.submit(new DockerAsyncConsumer(response as EngineResponse, callback))
            response.taskFuture = future
            return response
        }
        else {
            return getBuildResultAsImageId(response.content as List)
        }
    }

    String getBuildResultAsImageId(List<Map<String, String>> chunks) {
        def buildResultMessage = chunks.find { Map<String, String> chunk ->
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
    importStream(stream, repository = "", tag = "") {
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
    String pull(imageName, tag = "", authBase64Encoded = ".", registry = "") {
        log.info "docker pull '${imageName}:${tag}'"

        def actualImageName = imageName
        if (registry) {
            actualImageName = "$registry/$imageName".toString()
        }

        def response = client.post([path   : "/images/create",
                                    query  : [fromImage: actualImageName,
                                              tag      : tag,
                                              registry : registry],
                                    headers: ["X-Registry-Auth": authBase64Encoded ?: "."]])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker pull failed"))
//        println new JsonBuilder(response.content).toString()

        return findImageId(actualImageName, tag)
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
    EngineResponse rmi(imageId) {
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
