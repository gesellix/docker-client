package de.gesellix.docker.client.service

import de.gesellix.docker.client.EngineResponseContent
import de.gesellix.docker.client.node.NodeUtil
import de.gesellix.docker.client.tasks.ManageTask
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.ObjectVersion
import de.gesellix.docker.remote.api.Service
import de.gesellix.docker.remote.api.ServiceCreateRequest
import de.gesellix.docker.remote.api.ServiceCreateResponse
import de.gesellix.docker.remote.api.ServiceSpec
import de.gesellix.docker.remote.api.ServiceSpecMode
import de.gesellix.docker.remote.api.ServiceSpecModeReplicated
import de.gesellix.docker.remote.api.ServiceUpdateRequest
import de.gesellix.docker.remote.api.ServiceUpdateResponse
import de.gesellix.docker.remote.api.client.ServiceApi
import io.github.joke.spockmockable.Mockable
import spock.lang.Specification

@Mockable([ServiceApi, Service, ServiceSpec, ServiceCreateRequest, ServiceCreateResponse, ServiceUpdateRequest, ServiceUpdateResponse, ServiceSpecMode, ServiceSpecModeReplicated])
class ManageServiceClientTest extends Specification {

  EngineApiClient client = Mock(EngineApiClient)
  ManageTask manageTask = Mock(ManageTask)
  NodeUtil nodeUtil = Mock(NodeUtil)

  ManageServiceClient service

  def setup() {
    service = new ManageServiceClient(client, manageTask, nodeUtil)
  }

  def "list services with query"() {
    given:
    def serviceApi = Mock(ServiceApi)
    client.serviceApi >> serviceApi
    def filters = '{"name":["node-name"]}'
    def services = Mock(List)

    when:
    def responseContent = service.services(filters, null)

    then:
    1 * serviceApi.serviceList(filters, null) >> services
    responseContent.content == services
  }

  def "create a service"() {
    given:
    def serviceApi = Mock(ServiceApi)
    client.serviceApi >> serviceApi
    def serviceSpec = Mock(ServiceCreateRequest)
    def encodedAuth = "base64"
    def createResponse = Mock(ServiceCreateResponse)

    when:
    def responseContent = service.createService(serviceSpec, encodedAuth)

    then:
    1 * serviceApi.serviceCreate(serviceSpec, encodedAuth) >> createResponse
    responseContent.content == createResponse
  }

  def "rm service"() {
    given:
    def serviceApi = Mock(ServiceApi)
    client.serviceApi >> serviceApi

    when:
    service.rmService("service-name")

    then:
    1 * serviceApi.serviceDelete("service-name")
  }

  def "inspect service"() {
    given:
    def serviceApi = Mock(ServiceApi)
    client.serviceApi >> serviceApi
    def inspected = Mock(Service)

    when:
    def responseContent = service.inspectService("service-name")

    then:
    1 * serviceApi.serviceInspect("service-name", null) >> inspected
    responseContent.content == inspected
  }

  def "update service"() {
    given:
    def serviceApi = Mock(ServiceApi)
    client.serviceApi >> serviceApi
    def serviceSpec = Mock(ServiceUpdateRequest)
    def updateResponse = Mock(ServiceUpdateResponse)

    when:
    def responseContent = service.updateService("service-name", 42, serviceSpec, null, null)

    then:
    1 * serviceApi.serviceUpdate("service-name", 42, serviceSpec, ServiceApi.RegistryAuthFromServiceUpdate.Spec, null, null) >> updateResponse
    responseContent.content == updateResponse
  }

  def "scale service"() {
    given:
    def serviceApi = Mock(ServiceApi)
    client.serviceApi >> serviceApi

    def originalReplicated = Mock(ServiceSpecModeReplicated)
    def originalMode = Mock(ServiceSpecMode, { it.replicated >> originalReplicated })
    def originalSpec = Mock(ServiceSpec, { it.mode >> originalMode })
    def inspected = Mock(Service, { it.spec >> originalSpec; it.version >> new ObjectVersion(5) })
    def updateRequest = new ServiceUpdateRequest(
        null, null, null,
        originalMode,
        null, null, null, null
    )
    def updateResponse = Mock(ServiceUpdateResponse)

    when:
    def responseContent = service.scaleService("service-id", 42)

    then:
    1 * serviceApi.serviceInspect("service-id", null) >> inspected
    then:
    1 * originalReplicated.setReplicas(42)
    1 * serviceApi.serviceUpdate("service-id", 5, updateRequest, ServiceApi.RegistryAuthFromServiceUpdate.Spec, null, null) >> updateResponse
    and:
    responseContent.content == updateResponse
  }

  def "list tasks of service with query"() {
    given:
    def filters = [service: ["service-name"]]
    def query = [filters: filters]
    def expectedResponse = new EngineResponseContent([])

    when:
    def result = service.tasksOfService("service-name", query)

    then:
    1 * manageTask.tasks('{"service":["service-name"]}') >> expectedResponse
    result == expectedResponse
  }
}
