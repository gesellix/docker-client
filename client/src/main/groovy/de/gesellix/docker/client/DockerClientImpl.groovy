package de.gesellix.docker.client

import de.gesellix.docker.authentication.AuthConfigReader
import de.gesellix.docker.client.authentication.ManageAuthentication
import de.gesellix.docker.client.authentication.ManageAuthenticationClient
import de.gesellix.docker.client.config.ManageConfig
import de.gesellix.docker.client.config.ManageConfigClient
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
import de.gesellix.docker.engine.DockerClientConfig
import de.gesellix.docker.engine.DockerEnv
import de.gesellix.docker.engine.EngineClient
import de.gesellix.docker.engine.OkDockerClient
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.EngineApiClientImpl
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
  EngineClient httpClient
  EngineApiClient engineApiClient

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
  ManageConfig manageConfig
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
    apply(dockerClientConfig, proxy)
  }

  def apply(DockerClientConfig dockerClientConfig, Proxy proxy = NO_PROXY) {
    this.dockerClientConfig = dockerClientConfig
    this.env = dockerClientConfig.env
    this.proxy = proxy

    def authConfigReader = new AuthConfigReader(env)
    this.engineApiClient = new EngineApiClientImpl(dockerClientConfig, proxy)
    this.httpClient = new OkDockerClient(dockerClientConfig, proxy)
    log.info("using docker at '${env.dockerHost}'")

    this.responseHandler = new DockerResponseHandler()
    this.repositoryTagParser = new RepositoryTagParser()
    this.queryUtil = new QueryUtil()

    this.manageSystem = new ManageSystemClient(engineApiClient)
    this.manageAuthentication = new ManageAuthenticationClient(engineApiClient, authConfigReader)
    this.manageImage = new ManageImageClient(engineApiClient, manageAuthentication)
    this.manageDistribution = new ManageDistributionService(engineApiClient)
    this.manageContainer = new ManageContainerClient(engineApiClient, httpClient, responseHandler)
    this.manageVolume = new ManageVolumeClient(engineApiClient)
    this.manageNetwork = new ManageNetworkClient(engineApiClient)
    this.manageSwarm = new ManageSwarmClient(engineApiClient)
    this.manageSecret = new ManageSecretClient(engineApiClient)
    this.manageConfig = new ManageConfigClient(engineApiClient)
    this.manageTask = new ManageTaskClient(engineApiClient)
    NodeUtil nodeUtil = new NodeUtil(manageSystem)
    this.manageService = new ManageServiceClient(engineApiClient, manageTask, nodeUtil)
    this.manageNode = new ManageNodeClient(engineApiClient, manageTask, nodeUtil)
    this.manageStack = new ManageStackClient(
        manageService,
        manageTask,
        manageNode,
        manageNetwork,
        manageSecret,
        manageConfig,
        manageSystem,
        manageAuthentication)
  }

  void setDockerClientConfig(DockerClientConfig dockerClientConfig) {
    apply(dockerClientConfig, proxy)
  }

  void setEnv(DockerEnv env) {
    setDockerClientConfig(new DockerClientConfig(env))
  }

  // TODO move to ManageSwarm?
  @Override
  String getSwarmMangerAddress() {
    log.info("docker get swarm manager address")
    def swarmNodeId = info().content.swarm.nodeID
    def node = inspectNode(swarmNodeId).content
    return node.managerStatus.addr
  }
}
