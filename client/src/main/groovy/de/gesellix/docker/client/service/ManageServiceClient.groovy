package de.gesellix.docker.client.service

import de.gesellix.docker.client.EngineResponseContent
import de.gesellix.docker.client.node.NodeUtil
import de.gesellix.docker.client.tasks.ManageTask
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.Service
import de.gesellix.docker.remote.api.ServiceCreateRequest
import de.gesellix.docker.remote.api.ServiceCreateResponse
import de.gesellix.docker.remote.api.ServiceSpecMode
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
    Map<String, Object> actualQuery = new HashMap<String, Object>()
    if (query != null) {
      actualQuery.putAll(query)
    }
    queryUtil.jsonEncodeQueryParameter(actualQuery, "filters")
    return services(
        (String) actualQuery.get("filters"),
        (Boolean) actualQuery.get("status"))
  }

  @Override
  EngineResponseContent<List<Service>> services(String filters = null, Boolean status = null) {
    log.info("docker service ls")
    List<Service> serviceList = client.serviceApi.serviceList(filters, status)
    return new EngineResponseContent<List<Service>>(serviceList)
  }

  @Override
  EngineResponseContent<ServiceCreateResponse> createService(ServiceCreateRequest serviceSpec, String encodedRegistryAuth = null) {
    log.info("docker service create")
    ServiceCreateResponse serviceCreate = client.serviceApi.serviceCreate(serviceSpec, encodedRegistryAuth)
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
    Service serviceInspect = client.serviceApi.serviceInspect(name, null)
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
    log.info("docker service update {}@{}", name, version)
    ServiceApi.RegistryAuthFromServiceUpdate authFrom = registryAuthFrom
        ? ServiceApi.RegistryAuthFromServiceUpdate.valueOf(registryAuthFrom)
        : ServiceApi.RegistryAuthFromServiceUpdate.Spec
    ServiceUpdateResponse serviceUpdate = client.serviceApi.serviceUpdate(name, version, serviceSpec, authFrom, null, encodedRegistryAuth)
    return new EngineResponseContent<ServiceUpdateResponse>(serviceUpdate)
  }

  @Override
  EngineResponseContent<ServiceUpdateResponse> updateService(String name, int version, String rollback, String registryAuthFrom = null, String encodedRegistryAuth = null) {
    log.info("docker service update {}@{}", name, version)
    ServiceApi.RegistryAuthFromServiceUpdate authFrom = registryAuthFrom
        ? ServiceApi.RegistryAuthFromServiceUpdate.valueOf(registryAuthFrom)
        : ServiceApi.RegistryAuthFromServiceUpdate.Spec
    ServiceUpdateResponse serviceUpdate = client.serviceApi.serviceUpdate(name, version, null, authFrom, rollback, encodedRegistryAuth)
    return new EngineResponseContent<ServiceUpdateResponse>(serviceUpdate)
  }

  @Override
  EngineResponseContent<ServiceUpdateResponse> scaleService(String name, int replicas) {
    log.info("docker service scale")
    Service service = inspectService(name).content
    ServiceSpecMode mode = service.spec.mode
    if (mode.replicated == null) {
      throw new IllegalStateException("scale can only be used with replicated mode")
    }
    mode.replicated.replicas = replicas
    ServiceUpdateRequest serviceUpdateRequest = new ServiceUpdateRequest(
        null, null, null,
        mode,
        null, null,
        null, null)
    return updateService(name, service.version.index, serviceUpdateRequest)
  }

  @Override
  EngineResponseContent<List<Task>> tasksOfService(String service, Map<String, Object> query = new HashMap<>()) {
    log.info("docker service ps")
    Map<String, Object> actualQuery = query ?: new HashMap<>()
    if (!actualQuery.containsKey("filters")) {
      actualQuery.put("filters", new HashMap<>())
    }
    Map<String, Object> filters = (Map<String, Object>) actualQuery.get("filters")
    filters.put("service", [service])
    if (filters.get("node") != null) {
      filters.put("node", nodeUtil.resolveNodeId(filters.get("node")))
    }
    new QueryUtil().jsonEncodeQueryParameter(actualQuery, "filters");
    return manageTask.tasks((String) actualQuery.get("filters"))
  }
}
