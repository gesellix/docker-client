package de.gesellix.docker.client

import de.gesellix.docker.client.authentication.ManageAuthentication
import de.gesellix.docker.client.authentication.ManageAuthenticationClient
import de.gesellix.docker.client.config.DockerClientConfig
import de.gesellix.docker.client.config.DockerEnv
import de.gesellix.docker.client.container.ManageContainer
import de.gesellix.docker.client.container.ManageContainerClient
import de.gesellix.docker.client.distribution.ManageDistribution
import de.gesellix.docker.client.distribution.ManageDistributionService
import de.gesellix.docker.client.image.ManageImage
import de.gesellix.docker.client.image.ManageImageClient
import de.gesellix.docker.client.network.ManageNetwork
import de.gesellix.docker.client.network.ManageNetworkClient
import de.gesellix.docker.client.node.ManageNode
import de.gesellix.docker.client.node.ManageNodeClient
import de.gesellix.docker.client.node.NodeUtil
import de.gesellix.docker.client.repository.RepositoryTagParser
import de.gesellix.docker.client.secret.ManageSecret
import de.gesellix.docker.client.secret.ManageSecretClient
import de.gesellix.docker.client.service.ManageService
import de.gesellix.docker.client.service.ManageServiceClient
import de.gesellix.docker.client.stack.ManageStack
import de.gesellix.docker.client.stack.ManageStackClient
import de.gesellix.docker.client.swarm.ManageSwarm
import de.gesellix.docker.client.swarm.ManageSwarmClient
import de.gesellix.docker.client.system.ManageSystem
import de.gesellix.docker.client.system.ManageSystemClient
import de.gesellix.docker.client.tasks.ManageTask
import de.gesellix.docker.client.tasks.ManageTaskClient
import de.gesellix.docker.client.volume.ManageVolume
import de.gesellix.docker.client.volume.ManageVolumeClient
import de.gesellix.util.QueryUtil
import groovy.util.logging.Slf4j

import static java.net.Proxy.NO_PROXY

@Slf4j
class DockerClientImpl implements DockerClient {

    DockerResponseHandler responseHandler
    RepositoryTagParser repositoryTagParser
    QueryUtil queryUtil

    Proxy proxy
    DockerClientConfig dockerClientConfig
    DockerEnv env
    HttpClient httpClient

    @Delegate
    ManageSystem manageSystem
    @Delegate
    ManageAuthentication manageAuthentication
    @Delegate
    ManageImage manageImage
    @Delegate
    ManageContainer manageContainer
    @Delegate
    ManageVolume manageVolume
    @Delegate
    ManageNetwork manageNetwork
    @Delegate
    ManageSwarm manageSwarm
    @Delegate
    ManageSecret manageSecret
    @Delegate
    ManageService manageService
    @Delegate
    ManageStack manageStack
    @Delegate
    ManageNode manageNode
    @Delegate
    ManageTask manageTask
    @Delegate
    ManageDistribution manageDistribution

    DockerClientImpl() {
        this(new DockerClientConfig())
    }

    DockerClientImpl(String dockerHost) {
        this(new DockerClientConfig(dockerHost))
    }

    DockerClientImpl(DockerEnv env, Proxy proxy = NO_PROXY) {
        this(new DockerClientConfig(env), proxy)
    }

    DockerClientImpl(DockerClientConfig dockerClientConfig, Proxy proxy = NO_PROXY) {
        this.dockerClientConfig = dockerClientConfig
        this.env = dockerClientConfig.env
        this.proxy = proxy

        this.httpClient = new OkDockerClient(dockerClientConfig, proxy)
        log.info "using docker at '${env.dockerHost}'"

        this.responseHandler = new DockerResponseHandler()
        this.repositoryTagParser = new RepositoryTagParser()
        this.queryUtil = new QueryUtil()

        this.manageSystem = new ManageSystemClient(httpClient, responseHandler)
        this.manageAuthentication = new ManageAuthenticationClient(env, httpClient, responseHandler)
        this.manageImage = new ManageImageClient(httpClient, responseHandler)
        this.manageDistribution = new ManageDistributionService(httpClient, responseHandler)
        this.manageContainer = new ManageContainerClient(httpClient, responseHandler, manageImage)
        this.manageVolume = new ManageVolumeClient(httpClient, responseHandler)
        this.manageNetwork = new ManageNetworkClient(httpClient, responseHandler)
        this.manageSwarm = new ManageSwarmClient(httpClient, responseHandler)
        this.manageSecret = new ManageSecretClient(httpClient, responseHandler)
        this.manageTask = new ManageTaskClient(httpClient, responseHandler)
        NodeUtil nodeUtil = new NodeUtil(manageSystem)
        this.manageService = new ManageServiceClient(httpClient, responseHandler, manageTask, nodeUtil)
        this.manageNode = new ManageNodeClient(httpClient, responseHandler, manageTask, nodeUtil)
        this.manageStack = new ManageStackClient(
                httpClient,
                responseHandler,
                manageService,
                manageTask,
                manageNode,
                manageNetwork,
                manageSecret,
                manageSystem,
                manageAuthentication)
    }

    @Override
    cleanupStorage(Closure shouldKeepContainer, Closure shouldKeepVolume = { true }) {
        cleanupContainers shouldKeepContainer
        cleanupImages()
        cleanupVolumes shouldKeepVolume
    }

    @Override
    cleanupContainers(Closure shouldKeepContainer) {
        def allContainers = ps([filters: [status: ["exited"]]]).content
        allContainers.findAll { Map container ->
            !shouldKeepContainer(container)
        }.each { container ->
            log.debug "docker rm ${container.Id} (${container.Names.first()})"
            rm(container.Id)
        }
    }

    @Override
    cleanupImages() {
        images([filters: [dangling: ["true"]]]).content.each { image ->
            log.debug "docker rmi ${image.Id}"
            rmi(image.Id)
        }
    }

    @Override
    cleanupVolumes(Closure shouldKeepVolume) {
        def allVolumes = volumes([filters: [dangling: ["true"]]]).content.Volumes
        allVolumes.findAll { Map volume ->
            !shouldKeepVolume(volume)
        }.each { volume ->
            log.debug "docker volume rm ${volume.Name}"
            rmVolume(volume.Name)
        }
    }

    @Override
    search(term) {
        log.info "docker search"
        def response = httpClient.get([path : "/images/search".toString(),
                                       query: [term: term]])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker search failed"))
        return response
    }

    @Override
    getSwarmMangerAddress() {
        log.info "docker get swarm manager address"
        def swarmNodeId = info().content.Swarm.NodeID
        def node = inspectNode(swarmNodeId).content
        return node.ManagerStatus.Addr
    }
}
