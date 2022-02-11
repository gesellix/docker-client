package de.gesellix.docker.client.image

import de.gesellix.docker.client.EngineResponseContent
import de.gesellix.docker.client.authentication.ManageAuthentication
import de.gesellix.docker.client.repository.RepositoryTagParser
import de.gesellix.docker.remote.api.BuildInfo
import de.gesellix.docker.remote.api.CreateImageInfo
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.HistoryResponseItem
import de.gesellix.docker.remote.api.Image
import de.gesellix.docker.remote.api.ImageDeleteResponseItem
import de.gesellix.docker.remote.api.ImagePruneResponse
import de.gesellix.docker.remote.api.ImageSearchResponseItem
import de.gesellix.docker.remote.api.ImageSummary
import de.gesellix.docker.remote.api.PushImageInfo
import de.gesellix.docker.remote.api.core.StreamCallback
import de.gesellix.util.QueryUtil
import groovy.util.logging.Slf4j

import java.time.Duration

@Slf4j
class ManageImageClient implements ManageImage {

  private EngineApiClient client
  private RepositoryTagParser repositoryTagParser
  private QueryUtil queryUtil
  private ManageAuthentication manageAuthentication

  ManageImageClient(EngineApiClient client, ManageAuthentication manageAuthentication) {
    this.client = client
    this.manageAuthentication = manageAuthentication
    this.repositoryTagParser = new RepositoryTagParser()
    this.queryUtil = new QueryUtil()
  }

  @Override
  EngineResponseContent<List<ImageSearchResponseItem>> search(String term, Integer limit = 25) {
    log.info("docker search")
    def imageSearch = client.imageApi.imageSearch(term, limit, null)
    return new EngineResponseContent<List<ImageSearchResponseItem>>(imageSearch)
  }

  @Override
  void build(InputStream buildContext) {
    build(null, null,
          buildContext)
  }

  @Override
  void build(StreamCallback<BuildInfo> callback, Duration timeout,
             InputStream buildContext) {
    build(callback, timeout,
          null, null, null, null, null, null, null, null, null, null, buildContext)
  }

  @Override
  void build(String tag,
             InputStream buildContext) {
    build(null, null,
          buildContext)
  }

  @Override
  void build(StreamCallback<BuildInfo> callback, Duration timeout,
             String tag,
             InputStream buildContext) {
    build(callback, timeout,
          null, tag, null, null, null, null, null, null, null, null, buildContext)
  }

  @Override
  void build(String dockerfile, String tag, Boolean quiet, Boolean nocache, String pull, Boolean rm,
             String buildargs, String labels, String encodedRegistryConfig, String contentType, InputStream buildContext) {
    build(null, null,
          dockerfile, tag, quiet, nocache, pull, rm, buildargs, labels, encodedRegistryConfig, contentType, buildContext)
  }

  @Override
  void build(StreamCallback<BuildInfo> callback, Duration timeout,
             String dockerfile, String tag, Boolean quiet, Boolean nocache, String pull, Boolean rm,
             String buildargs, String labels, String encodedRegistryConfig, String contentType, InputStream buildContext) {
    log.info("docker build")

    if (!encodedRegistryConfig) {
      encodedRegistryConfig = manageAuthentication.encodeAuthConfigs(manageAuthentication.getAllAuthConfigs())
    }

    client.imageApi.imageBuild(dockerfile,
                               tag, null, null, quiet, nocache, null, pull,
                               rm == null ? true : rm, null,
                               null, null, null, null, null, null,
                               buildargs,
                               null,
                               null,
                               labels,
                               null,
                               contentType ?: "application/x-tar",
                               encodedRegistryConfig,
                               null, null,
                               null,
                               buildContext,
                               callback, timeout ? timeout.toMillis() : null)
  }

  @Override
  EngineResponseContent<List<HistoryResponseItem>> history(String imageId) {
    log.info("docker history")
    def imageHistory = client.imageApi.imageHistory(imageId)
    return new EngineResponseContent<List<HistoryResponseItem>>(imageHistory)
  }

  @Override
  EngineResponseContent<Image> inspectImage(String imageId) {
    log.info("docker inspect image")
    def imageInspect = client.imageApi.imageInspect(imageId)
    return new EngineResponseContent<Image>(imageInspect)
  }

  @Override
  void load(InputStream imagesTarball) {
    log.info("docker load")
    client.imageApi.imageLoad(null, imagesTarball)
  }

  @Override
  EngineResponseContent<List<ImageSummary>> images(Map<String, Object> query) {
    def actualQuery = [:]
    if (query) {
      actualQuery.putAll(query)
    }
    def defaults = [all: false]
    queryUtil.applyDefaults(actualQuery, defaults)
    queryUtil.jsonEncodeQueryParameter(actualQuery, "filters")
    return images(actualQuery.all as Boolean, actualQuery.filters as String, actualQuery.digests as Boolean)
  }

  @Override
  EngineResponseContent<List<ImageSummary>> images(Boolean all = false, String filters = null, Boolean digests = null) {
    log.info("docker images")
    def imageList = client.imageApi.imageList(all, filters, digests)
    return new EngineResponseContent<List<ImageSummary>>(imageList)
  }

  @Override
  EngineResponseContent<ImagePruneResponse> pruneImages(Map<String, Object> query) {
    def actualQuery = [:]
    if (query) {
      actualQuery.putAll(query)
    }
    queryUtil.jsonEncodeQueryParameter(actualQuery, "filters")
    return pruneImages(actualQuery.filters as String)
  }

  @Override
  EngineResponseContent<ImagePruneResponse> pruneImages(String filters = null) {
    log.info("docker image prune")
    def imagePrune = client.imageApi.imagePrune(filters)
    return new EngineResponseContent<ImagePruneResponse>(imagePrune)
  }

  @Override
  void pull(StreamCallback<CreateImageInfo> callback, Duration timeout,
            String imageName, String tag = "", String authBase64Encoded = ".") {
    log.info("docker pull '${imageName}:${tag}'")

    client.imageApi.imageCreate(
        imageName,
        null,
        null,
        tag ?: "", // "latest" as default?
        null,
        authBase64Encoded,
        null,
        null,
        null,
        callback,
        timeout ? timeout.toMillis() : null
    )
//    return findImageId(actualImageName, tag)
  }

  @Override
  void importUrl(StreamCallback<CreateImageInfo> callback, Duration timeout,
                 String url, String repository = "", String tag = "") {
    log.info("docker import '${url}' into ${repository}:${tag}")

    client.imageApi.imageCreate(
        null,
        url,
        repository ?: "",
        tag ?: "", // "latest" as default?
        null,
        null,
        null,
        null,
        null,
        callback,
        timeout ? timeout.toMillis() : null
    )
  }

  @Override
  void importStream(StreamCallback<CreateImageInfo> callback, Duration timeout,
                    InputStream stream, String repository = "", String tag = "") {
    log.info("docker import stream into ${repository}:${tag}")

    client.imageApi.imageCreate(
        null,
        "-",
        repository ?: "",
        tag ?: "", // "latest" as default?
        null,
        null,
        null,
        null,
        stream,
        callback,
        timeout ? timeout.toMillis() : null
    )
  }

  @Override
  void push(String imageName, String authBase64Encoded = ".", String registry = "") {
    push(null, null, imageName, authBase64Encoded, registry)
  }

  @Override
  void push(StreamCallback<PushImageInfo> callback, Duration timeout, String imageName, String authBase64Encoded = ".", String registry = "") {
    log.info("docker push '${imageName}'")

    String actualImageName = imageName
    if (registry) {
      actualImageName = "$registry/$imageName".toString()
      tag(imageName, actualImageName)
    }
    Map<String, String> repoAndTag = repositoryTagParser.parseRepositoryTag(actualImageName)

    client.imageApi.imagePush(repoAndTag.repo as String,
                              authBase64Encoded ?: ".",
                              repoAndTag.tag as String,
                              callback,
                              timeout ? timeout.toMillis() : null)
  }

  @Override
  EngineResponseContent<List<ImageDeleteResponseItem>> rmi(String imageId) {
    log.info("docker rmi")
    def imageDelete = client.imageApi.imageDelete(imageId, null, null)
    return new EngineResponseContent<List<ImageDeleteResponseItem>>(imageDelete)
  }

  @Override
  EngineResponseContent<InputStream> save(List<String> images) {
    log.info("docker save")
    def savedImages = client.imageApi.imageGetAll(images)
    return new EngineResponseContent<InputStream>(savedImages)
  }

  @Override
  void tag(String imageId, String repository) {
    log.info("docker tag")
    def repoAndTag = repositoryTagParser.parseRepositoryTag(repository)
    client.imageApi.imageTag(imageId, repoAndTag.repo, repoAndTag.tag)
  }

  @Override
  String findImageId(String imageName, String tag = "") {
    def isDigest = imageName.contains '@'
    def images = images((isDigest) ? [digests: '1'] : [:]).content
//        println new JsonBuilder(images).toString()
    def imageIdsByRepoDigest = images.collectEntries { image ->
      image.repoDigests?.collectEntries { String repoDigest ->
        def idByDigest = [:]
        idByDigest[repoDigest] = (String) image.id
        idByDigest
      } ?: [:]
    }
    def imageIdsByName = images.collectEntries { image ->
      image.repoTags?.collectEntries { String repoTag ->
        def idByName = [:]
        idByName[repoTag] = (String) image.id
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
