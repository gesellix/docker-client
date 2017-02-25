package de.gesellix.docker.client.stack

import de.gesellix.docker.client.DockerResponse
import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.HttpClient
import de.gesellix.docker.client.network.ManageNetwork
import de.gesellix.docker.client.node.ManageNode
import de.gesellix.docker.client.secret.ManageSecret
import de.gesellix.docker.client.service.ManageService
import de.gesellix.docker.client.system.ManageSystem
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
    private ManageSystem manageSystem

    // see docker/docker/cli/compose/convert/compose.go:14
    static final String LabelNamespace = "com.docker.stack.namespace"

    ManageStackClient(
            HttpClient client,
            DockerResponseHandler responseHandler,
            ManageService manageService,
            ManageTask manageTask,
            ManageNode manageNode,
            ManageNetwork manageNetwork,
            ManageSecret manageSecret,
            ManageSystem manageSystem) {
        this.client = client
        this.responseHandler = responseHandler
        this.queryUtil = new QueryUtil()
        this.manageService = manageService
        this.manageTask = manageTask
        this.manageNode = manageNode
        this.manageNetwork = manageNetwork
        this.manageSecret = manageSecret
        this.manageSystem = manageSystem
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

//        checkDaemonIsSwarmManager()

        deployConfig.networks.each { network ->
            log.info("network: $network")
            if (network.external) {

            } else {
                manageNetwork.createNetwork(network.name)
            }
        }

        throw new UnsupportedOperationException("NYI")
    }

/*

func deployCompose(ctx context.Context, dockerCli *command.DockerCli, opts deployOptions) error {
	configDetails, err := getConfigDetails(opts)
	if err != nil {
		return err
	}

	config, err := loader.Load(configDetails)
	if err != nil {
		if fpe, ok := err.(*loader.ForbiddenPropertiesError); ok {
			return fmt.Errorf("Compose file contains unsupported options:\n\n%s\n",
				propertyWarnings(fpe.Properties))
		}

		return err
	}

	unsupportedProperties := loader.GetUnsupportedProperties(configDetails)
	if len(unsupportedProperties) > 0 {
		fmt.Fprintf(dockerCli.Err(), "Ignoring unsupported options: %s\n\n",
			strings.Join(unsupportedProperties, ", "))
	}

	deprecatedProperties := loader.GetDeprecatedProperties(configDetails)
	if len(deprecatedProperties) > 0 {
		fmt.Fprintf(dockerCli.Err(), "Ignoring deprecated options:\n\n%s\n\n",
			propertyWarnings(deprecatedProperties))
	}

	if err := checkDaemonIsSwarmManager(ctx, dockerCli); err != nil {
		return err
	}

	namespace := convert.NewNamespace(opts.namespace)

	serviceNetworks := getServicesDeclaredNetworks(config.Services)

	networks, externalNetworks := convert.Networks(namespace, config.Networks, serviceNetworks)
	if err := validateExternalNetworks(ctx, dockerCli, externalNetworks); err != nil {
		return err
	}
	if err := createNetworks(ctx, dockerCli, namespace, networks); err != nil {
		return err
	}

	secrets, err := convert.Secrets(namespace, config.Secrets)
	if err != nil {
		return err
	}
	if err := createSecrets(ctx, dockerCli, namespace, secrets); err != nil {
		return err
	}

	services, err := convert.Services(namespace, config, dockerCli.Client())
	if err != nil {
		return err
	}
	return deployServices(ctx, dockerCli, services, namespace, opts.sendRegistryAuth)
}

 */

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
