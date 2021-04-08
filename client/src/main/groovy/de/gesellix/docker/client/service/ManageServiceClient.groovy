package de.gesellix.docker.client.service

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.node.NodeUtil
import de.gesellix.docker.client.tasks.ManageTask
import de.gesellix.docker.engine.EngineClient
import de.gesellix.docker.engine.EngineResponse
import de.gesellix.util.IOUtils
import de.gesellix.util.QueryUtil
import groovy.util.logging.Slf4j

@Slf4j
class ManageServiceClient implements ManageService {

  private EngineClient client
  private DockerResponseHandler responseHandler
  private QueryUtil queryUtil
  private ManageTask manageTask
  private NodeUtil nodeUtil

  ManageServiceClient(
      EngineClient client,
      DockerResponseHandler responseHandler,
      ManageTask manageTask,
      NodeUtil nodeUtil) {
    this.client = client
    this.responseHandler = responseHandler
    this.queryUtil = new QueryUtil()
    this.manageTask = manageTask
    this.nodeUtil = nodeUtil
  }

  @Override
  EngineResponse services(query = [:]) {
    log.info "docker service ls"
    def actualQuery = query ?: [:]
    queryUtil.jsonEncodeFilters(actualQuery)
    def response = client.get([path : "/services",
                               query: actualQuery])
    responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker service ls failed"))
    return response
  }

  @Override
  EngineResponse createService(config, createOptions = [:]) {
    log.info "docker service create"
    config = config ?: [:]
    createOptions = createOptions ?: [:]
    def headers = [:]
    if (createOptions.EncodedRegistryAuth) {
      headers["X-Registry-Auth"] = createOptions.EncodedRegistryAuth as String
    }
    def response = client.post([path              : "/services/create",
                                headers           : headers,
                                body              : config,
                                requestContentType: "application/json"])
    responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker service create failed"))
    return response
  }

  @Override
  rmService(name) {
    log.info "docker service rm"
    def response = client.delete([path: "/services/$name".toString()])
    responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker service rm failed"))
    IOUtils.closeQuietly(response.stream)
    return response
  }

  @Override
  EngineResponse inspectService(name) {
    log.info "docker service inspect"
    def response = client.get([path: "/services/$name".toString()])
    responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker service inspect failed"))
    return response
  }

//    @Override
//    logsOfService(service) {
//        log.info "docker service logs"
//        def response = client.get([path : "/services/$service/logs".toString(),
//                                            query: [tail: "all"]])
//        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker service logs failed"))
//        return response
//    }

  @Override
  EngineResponse updateService(name, query, config, updateOptions = [:]) {
    log.info "docker service update"
    def actualQuery = query ?: [:]
    config = config ?: [:]
    updateOptions = updateOptions ?: [:]
    def headers = [:]
    if (updateOptions.EncodedRegistryAuth) {
      headers["X-Registry-Auth"] = updateOptions.EncodedRegistryAuth as String
    }
    def response = client.post([path              : "/services/$name/update".toString(),
                                query             : actualQuery,
                                headers           : headers,
                                body              : config,
                                requestContentType: "application/json"])
    responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker service update failed"))
    return response
  }

  @Override
  scaleService(name, int replicas) {
    log.info "docker service scale"
    def service = inspectService(name).content
    def mode = service.Spec.Mode
    if (!mode.Replicated) {
      throw new IllegalStateException("scale can only be used with replicated mode")
    }
    mode.Replicated.Replicas = replicas
    return updateService(name, [version: service.Version], service.Spec)
  }

  @Override
  tasksOfService(service, query = [:]) {
    log.info "docker service ps"
    def actualQuery = query ?: [:]
    if (!actualQuery.containsKey('filters')) {
      actualQuery.filters = [:]
    }
    actualQuery.filters['service'] = [service]
    if (actualQuery.filters?.node) {
      actualQuery.filters.node = nodeUtil.resolveNodeId(query.filters.node)
    }
    return manageTask.tasks(actualQuery)
  }
}
