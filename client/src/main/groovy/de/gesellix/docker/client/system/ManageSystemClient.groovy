package de.gesellix.docker.client.system

import de.gesellix.docker.client.DockerAsyncCallback
import de.gesellix.docker.client.DockerAsyncConsumer
import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.engine.EngineClient
import de.gesellix.docker.engine.EngineResponse
import de.gesellix.util.IOUtils
import de.gesellix.util.QueryUtil
import groovy.util.logging.Slf4j
import okio.Okio

import static java.util.concurrent.Executors.newSingleThreadExecutor

@Slf4j
class ManageSystemClient implements ManageSystem {

  private EngineClient client
  private DockerResponseHandler responseHandler
  private QueryUtil queryUtil

  ManageSystemClient(EngineClient client, DockerResponseHandler responseHandler) {
    this.client = client
    this.responseHandler = responseHandler
    this.queryUtil = new QueryUtil()
  }

  @Override
  EngineResponse systemDf() {
    log.info("docker system df")
    def actualQuery = [:]
    def response = client.get([path : "/system/df",
                               query: actualQuery])
    return response
  }

  @Override
  EngineResponse events(DockerAsyncCallback callback, Map query = [:]) {
    log.info("docker events")

    queryUtil.jsonEncodeFilters(query)
    def response = client.get([path : "/events",
                               query: query,
                               async: true])
    responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker events failed"))
    def executor = newSingleThreadExecutor()
    def future = executor.submit(new DockerAsyncConsumer(response as EngineResponse, callback))
    response.taskFuture = future
    return response
  }

  @Override
  EngineResponse ping() {
    log.info("docker ping")
    def response = client.get([path: "/_ping", timeout: 2000])

    // Recently (no idea since when), the engine responds with a chunked transfer-encoding on this endpoint,
    // which we don't fully consume by default. In case of /_ping we try to keep the old behaviour
    // for consumers of our .ping() method, though.
    if (!response.content && response.stream) {
      response.content = Okio.buffer(Okio.source(response.stream)).readUtf8()
      IOUtils.closeQuietly(response.stream)
    }
    return response
  }

  @Override
  EngineResponse version() {
    log.info("docker version")
    def response = client.get([path: "/version"])
    return response
  }

  @Override
  EngineResponse info() {
    log.info("docker info")
    def response = client.get([path: "/info"])
    return response
  }
}
