package de.gesellix.docker.client.service

import de.gesellix.docker.client.EngineResponseContent
import de.gesellix.docker.client.node.NodeUtil
import de.gesellix.docker.client.tasks.ManageTask
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.Service
import de.gesellix.docker.remote.api.ServiceCreateRequest
import de.gesellix.docker.remote.api.ServiceCreateResponse
import de.gesellix.docker.remote.api.ServiceUpdateRequest
import de.gesellix.docker.remote.api.ServiceUpdateResponse
import de.gesellix.docker.remote.api.Task
import de.gesellix.docker.remote.api.client.ServiceApi
import de.gesellix.util.QueryUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ManageServiceClient implements ManageService {

  private final Logger log = LoggerFactory.getLogger(ManageServiceClient)

  private EngineApiClient client
  private QueryUtil queryUtil
  private ManageTask manageTask
  private NodeUtil nodeUtil

  ManageServiceClient(
      EngineApiClient client,
      ManageTask manageTask,
      NodeUtil nodeUtil) {
    this.client = client
    this.queryUtil = new QueryUtil()
    this.manageTask = manageTask
    this.nodeUtil = nodeUtil
  }

  @Override
  EngineResponseContent<List<Service>> services(Map<String, Object> query) {
    log.info("docker service ls")
    def actualQuery = [:]
    if (query) {
      actualQuery.putAll(query)
    }
    queryUtil.jsonEncodeQueryParameter(actualQuery, "filters")
    return services(actualQuery.filters as String, actualQuery.status as Boolean)
  }

  @Override
  EngineResponseContent<List<Service>> services(String filters = null, Boolean status = null) {
    log.info("docker service ls")
    def serviceList = client.serviceApi.serviceList(filters, status)
    return new EngineResponseContent<List<Service>>(serviceList)
  }

  @Override
  EngineResponseContent<ServiceCreateResponse> createService(ServiceCreateRequest serviceSpec, String encodedRegistryAuth = null) {
    log.info("docker service create")
    def serviceCreate = client.serviceApi.serviceCreate(serviceSpec, encodedRegistryAuth)
    return new EngineResponseContent<ServiceCreateResponse>(serviceCreate)
  }

  @Override
  void rmService(String name) {
    log.info("docker service rm")
    client.serviceApi.serviceDelete(name)
  }

  @Override
  EngineResponseContent<Service> inspectService(String name) {
    log.info("docker service inspect")
    def serviceInspect = client.serviceApi.serviceInspect(name, null)
    return new EngineResponseContent<Service>(serviceInspect)
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
  EngineResponseContent<ServiceUpdateResponse> updateService(String name, int version, ServiceUpdateRequest serviceSpec, String registryAuthFrom = null, String encodedRegistryAuth = null) {
    log.info("docker service update $name@$version")
    def authFrom = registryAuthFrom
        ? ServiceApi.RegistryAuthFromServiceUpdate.valueOf(registryAuthFrom)
        : ServiceApi.RegistryAuthFromServiceUpdate.Spec
    def serviceUpdate = client.serviceApi.serviceUpdate(name, version, serviceSpec, authFrom, null, encodedRegistryAuth)
    return new EngineResponseContent<ServiceUpdateResponse>(serviceUpdate)
  }

  @Override
  EngineResponseContent<ServiceUpdateResponse> updateService(String name, int version, String rollback, String registryAuthFrom = null, String encodedRegistryAuth = null) {
    log.info("docker service update $name@$version")
    def authFrom = registryAuthFrom
        ? ServiceApi.RegistryAuthFromServiceUpdate.valueOf(registryAuthFrom)
        : ServiceApi.RegistryAuthFromServiceUpdate.Spec
    def serviceUpdate = client.serviceApi.serviceUpdate(name, version, null, authFrom, rollback, encodedRegistryAuth)
    return new EngineResponseContent<ServiceUpdateResponse>(serviceUpdate)
  }

  @Override
  EngineResponseContent<ServiceUpdateResponse> scaleService(String name, int replicas) {
    log.info("docker service scale")
    def service = inspectService(name).content
    def mode = service.spec.mode
    if (!mode.replicated) {
      throw new IllegalStateException("scale can only be used with replicated mode")
    }
    mode.replicated.replicas = replicas
    def serviceUpdateRequest = new ServiceUpdateRequest(
        null, null, null,
        mode,
        null, null,
        null, null)
    return updateService(name, service.version.index, serviceUpdateRequest)
  }

  @Override
  EngineResponseContent<List<Task>> tasksOfService(String service, Map<String, Object> query = [:]) {
    log.info("docker service ps")
    Map actualQuery = query ?: [:]
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
