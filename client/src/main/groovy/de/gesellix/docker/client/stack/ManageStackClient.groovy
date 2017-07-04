package de.gesellix.docker.client.stack

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.authentication.ManageAuthentication
import de.gesellix.docker.client.network.ManageNetwork
import de.gesellix.docker.client.node.ManageNode
import de.gesellix.docker.client.secret.ManageSecret
import de.gesellix.docker.client.service.ManageService
import de.gesellix.docker.client.stack.types.StackNetwork
import de.gesellix.docker.client.stack.types.StackSecret
import de.gesellix.docker.client.stack.types.StackService
import de.gesellix.docker.client.system.ManageSystem
import de.gesellix.docker.client.tasks.ManageTask
import de.gesellix.docker.engine.EngineClient
import de.gesellix.docker.engine.EngineResponse
import de.gesellix.util.QueryUtil
import groovy.transform.EqualsAndHashCode
import groovy.util.logging.Slf4j

@Slf4j
class ManageStackClient implements ManageStack {

    private EngineClient client
    private DockerResponseHandler responseHandler
    private QueryUtil queryUtil
    private ManageService manageService
    private ManageTask manageTask
    private ManageNode manageNode
    private ManageNetwork manageNetwork
    private ManageSecret manageSecret
    private ManageSystem manageSystem
    private ManageAuthentication manageAuthentication

    // see docker/docker/cli/compose/convert/compose.go:14
    static final String LabelNamespace = "com.docker.stack.namespace"

    ManageStackClient(
            EngineClient client,
            DockerResponseHandler responseHandler,
            ManageService manageService,
            ManageTask manageTask,
            ManageNode manageNode,
            ManageNetwork manageNetwork,
            ManageSecret manageSecret,
            ManageSystem manageSystem,
            ManageAuthentication manageAuthentication) {
        this.client = client
        this.responseHandler = responseHandler
        this.queryUtil = new QueryUtil()
        this.manageService = manageService
        this.manageTask = manageTask
        this.manageNode = manageNode
        this.manageNetwork = manageNetwork
        this.manageSecret = manageSecret
        this.manageSystem = manageSystem
        this.manageAuthentication = manageAuthentication
    }

    @Override
    lsStacks() {
        log.info "docker stack ls"

        Map<String, Stack> stacksByName = [:]

        EngineResponse services = manageService.services([filters: [label: [(LabelNamespace): true]]])
        services.content?.each { service ->
            String stackName = service.Spec.Labels[(LabelNamespace)]
            if (!stacksByName[(stackName)]) {
                stacksByName[(stackName)] = new Stack(name: stackName, services: 0)
            }
            stacksByName[(stackName)].services++
        }

        return stacksByName.values()
    }

    Map toMap(object) {
        return object?.properties?.findAll {
            (it.key != 'class')
        }?.collectEntries {
            it.value == null || it.value instanceof Serializable ? [it.key, it.value] : [it.key, toMap(it.value)]
        }
    }

    @Override
    stackDeploy(String namespace, DeployStackConfig config, DeployStackOptions options) {
        log.info "docker stack deploy"

        checkDaemonIsSwarmManager()

        if (options.pruneServices) {
            def serviceNames = config.services.keySet()
            pruneServices(namespace, serviceNames)
        }

        createNetworks(namespace, config.networks)
        createSecrets(namespace, config.secrets)
        createOrUpdateServices(namespace, config.services, options.sendRegistryAuth)
    }

    def createNetworks(String namespace, Map<String, StackNetwork> networks) {
        def existingNetworks = manageNetwork.networks([
                filters: [
                        label: [("${LabelNamespace}=${namespace}" as String): true]]])
        def existingNetworkNames = []
        existingNetworks.content.each {
            existingNetworkNames << it.Name
        }
        networks.each { name, network ->
            name = "${namespace}_${name}" as String
            if (!existingNetworkNames.contains(name)) {
                log.info("create network $name: $network")
                if (!network.labels) {
                    network.labels = [:]
                }
                network.labels[(LabelNamespace)] = namespace
                manageNetwork.createNetwork(name, toMap(network))
            }
        }
    }

    def createSecrets(String namespace, Map<String, StackSecret> secrets) {
        secrets.each { name, secret ->
            List knownSecrets = manageSecret.secrets([filters: [names: [secret.name]]]).content
            log.debug("known: $knownSecrets")

            if (!secret.labels) {
                secret.labels = [:]
            }
            secret.labels[(LabelNamespace)] = namespace

            if (knownSecrets.empty) {
                log.info("create secret ${secret.name}: $secret")
                manageSecret.createSecret(secret.name, secret.data, secret.labels)
            } else {
                if (knownSecrets.size() != 1) {
                    throw new IllegalStateException("ambiguous secret name '${secret.name}'")
                }
                def knownSecret = knownSecrets.first()
                log.info("update secret ${secret.name}: $secret")
                manageSecret.updateSecret(knownSecret.ID as String, knownSecret.Version.Index, toMap(secret))
            }
        }
    }

    def pruneServices(String namespace, Collection<String> services) {
        // Descope returns the name without the namespace prefix
        def descope = { String name ->
            return name.substring("${namespace}_".length())
        }

        def oldServices = stackServices(namespace)
        def pruneServices = oldServices.content.findResults {
            return services.contains(descope(it.Spec.Name as String)) ? null : it
        }

        pruneServices.each { service ->
            manageService.rmService(service.ID)
        }
    }

    def createOrUpdateServices(String namespace, Map<String, StackService> services, boolean sendRegistryAuth) {
        def existingServicesByName = [:]
        def existingServices = stackServices(namespace)
        existingServices.content.each { service ->
            existingServicesByName[service.Spec.Name] = service
        }

        services.each { internalName, serviceSpec ->
            def name = "${namespace}_${internalName}" as String
            serviceSpec.name = serviceSpec.name ?: name
            if (!serviceSpec.labels) {
                serviceSpec.labels = [:]
            }
            serviceSpec.labels[(LabelNamespace)] = namespace

            def encodedAuth = ""
            if (sendRegistryAuth) {
                // Retrieve encoded auth token from the image reference
                String image = serviceSpec.taskTemplate.containerSpec.image
                encodedAuth = manageAuthentication.retrieveEncodedAuthTokenForImage(image)
            }

            def service = existingServicesByName[name]
            if (service) {
                log.info("Updating service ${name} (id ${service.ID}): ${toMap(serviceSpec)}")

                def updateOptions = [:]
                if (sendRegistryAuth) {
                    updateOptions.EncodedRegistryAuth = encodedAuth
                }
                def response = manageService.updateService(
                        service.ID,
                        [version: service.Version.Index],
                        toMap(serviceSpec),
                        updateOptions)
                response.content.Warnings.each { String warning ->
                    log.warn(warning)
                }
            } else {
                log.info("Creating service ${name}: ${serviceSpec}")

                def createOptions = [:]
                if (sendRegistryAuth) {
                    createOptions.EncodedRegistryAuth = encodedAuth
                }
                def response = manageService.createService(toMap(serviceSpec), createOptions)
            }
        }
    }

    // checkDaemonIsSwarmManager does an Info API call to verify that the daemon is
    // a swarm manager. This is necessary because we must create networks before we
    // create services, but the API call for creating a network does not return a
    // proper status code when it can't create a network in the "global" scope.
    def checkDaemonIsSwarmManager() {
        if (!manageSystem.info()?.content?.Swarm?.ControlAvailable) {
            throw new IllegalStateException("This node is not a swarm manager. Use \"docker swarm init\" or \"docker swarm join\" to connect this node to swarm and try again.")
        }
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

    def getInfoByServiceId(EngineResponse services) {
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

    @EqualsAndHashCode
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
