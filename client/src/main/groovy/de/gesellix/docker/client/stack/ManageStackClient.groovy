package de.gesellix.docker.client.stack

import de.gesellix.docker.client.DockerResponse
import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.HttpClient
import de.gesellix.docker.client.network.ManageNetwork
import de.gesellix.docker.client.node.ManageNode
import de.gesellix.docker.client.secret.ManageSecret
import de.gesellix.docker.client.service.ManageService
import de.gesellix.docker.client.tasks.ManageTask
import de.gesellix.util.QueryUtil
import groovy.util.logging.Slf4j

@Slf4j
class ManageStackClient implements ManageStack {

    private HttpClient client
    private DockerResponseHandler responseHandler
    private QueryUtil queryUtil
    private ManageService manageService
    private ManageTask manageTask
    private ManageNode manageNode
    private ManageNetwork manageNetwork
    private ManageSecret manageSecret

    // see docker/docker/cli/compose/convert/compose.go:14
    final String LabelNamespace = "com.docker.stack.namespace"

    ManageStackClient(
            HttpClient client,
            DockerResponseHandler responseHandler,
            ManageService manageService,
            ManageTask manageTask,
            ManageNode manageNode,
            ManageNetwork manageNetwork,
            ManageSecret manageSecret) {
        this.client = client
        this.responseHandler = responseHandler
        this.queryUtil = new QueryUtil()
        this.manageService = manageService
        this.manageTask = manageTask
        this.manageNode = manageNode
        this.manageNetwork = manageNetwork
        this.manageSecret = manageSecret
    }

    @Override
    lsStacks() {
        log.info "docker stack ls"

        Map<String, Stack> stacksByName = [:]

        DockerResponse services = manageService.services([filters: [label: [(LabelNamespace): true]]])
        services.content?.each { service ->
            String stackName = service.Spec.Labels[(LabelNamespace)]
            if (!stacksByName[(stackName)]) {
                stacksByName[(stackName)] = new Stack(name: stackName, services: 0)
            }
            stacksByName[(stackName)].services++
        }

        return stacksByName.values()
    }

    @Override
    stackDeploy(String namespace, DeployStackConfig deployConfig) {
        log.info "docker stack deploy"
        throw new UnsupportedOperationException("NYI")
    }

    @Override
    stackPs(String namespace, Map filters = [:]) {
        log.info "docker stack ps"

        String namespaceFilter = "${LabelNamespace}=${namespace}"

        def actualFilters = filters ?: [:]
        if (actualFilters.label) {
            actualFilters.label[(namespaceFilter)] = true
        } else {
            actualFilters['label'] = [(namespaceFilter): true]
        }
        def tasks = manageTask.tasks([filters: actualFilters])
        return tasks
    }

    @Override
    stackRm(String namespace) {
        log.info "docker stack rm"

        String namespaceFilter = "${LabelNamespace}=${namespace}"

        def services = manageService.services([filters: [label: [(namespaceFilter): true]]])
        def networks = manageNetwork.networks([filters: [label: [(namespaceFilter): true]]])
        def secrets = manageSecret.secrets([filters: [label: [(namespaceFilter): true]]])

        services.content.each { service ->
            manageService.rmService(service.ID)
        }
        networks.content.each { network ->
            manageNetwork.rmNetwork(network.Id)
        }
        secrets.content.each { secret ->
            manageSecret.rmSecret(secret.ID as String)
        }
    }

    @Override
    stackServices(String namespace, Map filters = [:]) {
        log.info "docker stack services"

        String namespaceFilter = "${LabelNamespace}=${namespace}"
        def actualFilters = filters ?: [:]
        if (actualFilters.label) {
            actualFilters.label[(namespaceFilter)] = true
        } else {
            actualFilters['label'] = [(namespaceFilter): true]
        }
        def services = manageService.services([filters: actualFilters])
//        def infoByServiceId = getInfoByServiceId(services)
        return services
    }

    def getInfoByServiceId(DockerResponse services) {
        def nodes = manageNode.nodes()
        List<String> activeNodes = nodes.content.findResults { node ->
            node.Status.State != 'down' ? node.ID : null
        }

        Map<String, Integer> running = [:]
        Map<String, Integer> tasksNoShutdown = [:]

        def serviceFilter = [service: [:]]
        services.content.each { service ->
            serviceFilter.service[(service.ID as String)] = true
        }
        def tasks = manageTask.tasks([filters: serviceFilter])
        tasks.content.each { task ->
            if (task.DesiredState != 'shutdown') {
                if (!tasksNoShutdown[task.ServiceID as String]) {
                    tasksNoShutdown[task.ServiceID as String] = 0
                }
                tasksNoShutdown[task.ServiceID as String]++
            }
            if (activeNodes.contains(task.NodeID as String) && task.Status.State == 'running') {
                if (!running[task.ServiceID as String]) {
                    running[task.ServiceID as String] = 0
                }
                running[task.ServiceID as String]++
            }
        }

        def infoByServiceId = [:]
        services.content.each { service ->
            if (service.Spec.Mode.Replicated && service.Spec.Mode.Replicated.Replicas) {
                infoByServiceId[service.ID] = new ServiceInfo(mode: 'replicated', replicas: "${running[service.ID as String] ?: 0}/${service.Spec.Mode.Replicated.Replicas}")
            } else if (service.Spec.Mode.Global) {
                infoByServiceId[service.ID] = new ServiceInfo(mode: 'global', replicas: "${running[service.ID as String] ?: 0}}/${tasksNoShutdown[service.ID as String]}")
            }
        }
        return infoByServiceId
    }

    static class Stack {
        String name
        int services

        @Override
        String toString() {
            "$name: $services"
        }
    }

    static class ServiceInfo {
        String mode
        String replicas

        @Override
        String toString() {
            "$mode, $replicas"
        }
    }
}
