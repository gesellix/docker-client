package de.gesellix.docker.client.swarm

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.engine.EngineClient
import de.gesellix.docker.engine.EngineResponse
import de.gesellix.util.IOUtils
import de.gesellix.util.QueryUtil
import groovy.util.logging.Slf4j

@Slf4j
class ManageSwarmClient implements ManageSwarm {

  private EngineClient client
  private DockerResponseHandler responseHandler
  private QueryUtil queryUtil

  ManageSwarmClient(EngineClient client, DockerResponseHandler responseHandler) {
    this.client = client
    this.responseHandler = responseHandler
    this.queryUtil = new QueryUtil()
  }

  @Override
  Map newSwarmConfig() {
    return [
        "ListenAddr"     : "0.0.0.0:2377",
        "ForceNewCluster": false
//                ,"Spec"           : [
//                        "AcceptancePolicy": [
//                                "Policies": [
//                                        ["Role": "MANAGER", "Autoaccept": true],
//                                        ["Role": "WORKER", "Autoaccept": true]
//                                ]
//                        ],
//                        "Orchestration"   : [:],
//                        "Raft"            : [:],
//                        "Dispatcher"      : [:],
//                        "CAConfig"        : [:]
//                ]
    ]
  }

  @Override
  EngineResponse initSwarm() {
    initSwarm(newSwarmConfig())
  }

  @Override
  EngineResponse initSwarm(Map config) {
    log.info "docker swarm init"

    /*
func runInit(dockerCli *client.DockerCli, flags *pflag.FlagSet, opts initOptions) error {
client := dockerCli.Client()
ctx := context.Background()

// If no secret was specified, we create a random one
if !flags.Changed("secret") {
opts.secret = generateRandomSecret()
fmt.Fprintf(dockerCli.Out(), "No --secret provided. Generated random secret:\n\t%s\n\n", opts.secret)
}

req := swarm.InitRequest{
ListenAddr:      opts.listenAddr.String(),
ForceNewCluster: opts.forceNewCluster,
Spec:            opts.swarmOptions.ToSpec(),
}

nodeID, err := client.SwarmInit(ctx, req)
if err != nil {
return err
}

fmt.Fprintf(dockerCli.Out(), "Swarm initialized: current node (%s) is now a manager.\n\n", nodeID)

// Fetch CAHash and Address from the API
info, err := client.Info(ctx)
if err != nil {
return err
}

node, _, err := client.NodeInspectWithRaw(ctx, nodeID)
if err != nil {
return err
}

if node.ManagerStatus != nil && info.Swarm.CACertHash != "" {
var secretArgs string
if opts.secret != "" {
  secretArgs = "--secret " + opts.secret
}
fmt.Fprintf(dockerCli.Out(), "To add a worker to this swarm, run the following command:\n\tdocker swarm join %s \\\n\t--ca-hash %s \\\n\t%s\n", secretArgs, info.Swarm.CACertHash, node.ManagerStatus.Addr)
}

return nil
}
     */

    config = config ?: [:]
    def response = client.post([path              : "/swarm/init",
                                body              : config,
                                requestContentType: "application/json"])
    responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker swarm init failed"))
    return response
  }

  @Override
  EngineResponse joinSwarm(Map config) {
    log.info "docker swarm join"
    config = config ?: [:]
    EngineResponse response = client.post([path              : "/swarm/join",
                                           body              : config,
                                           requestContentType: "application/json"])
    responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker swarm join failed"))
    IOUtils.closeQuietly(response.stream)
    return response
  }

  @Override
  EngineResponse leaveSwarm(Map query = [:]) {
    log.info "docker swarm leave"
    def actualQuery = query ?: [:]
    EngineResponse response = client.post([path : "/swarm/leave",
                                           query: actualQuery])
    responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker swarm leave failed"))
    IOUtils.closeQuietly(response.stream)
    return response
  }

  @Override
  EngineResponse updateSwarm(Map query, Map config) {
    log.info "docker swarm update"
    def actualQuery = query ?: [:]
    config = config ?: [:]
    EngineResponse response = client.post([path              : "/swarm/update",
                                           query             : actualQuery,
                                           body              : config,
                                           requestContentType: "application/json"])
    responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker swarm update failed"))
    IOUtils.closeQuietly(response.stream)
    return response
  }

  @Override
  String getSwarmWorkerToken() {
    log.info "docker swarm join-token worker"
    def swarm = inspectSwarm().content
    return swarm.JoinTokens.Worker
  }

  @Override
  String rotateSwarmWorkerToken() {
    log.info "docker swarm join-token rotate worker token"

    def swarm = inspectSwarm().content
    def updateResponse = updateSwarm(
        [
            "version"          : swarm.Version.Index,
            "rotateWorkerToken": true
        ],
        swarm.Spec)
    log.info "rotate worker token: ${updateResponse.status}"
    return getSwarmWorkerToken()
  }

  @Override
  String getSwarmManagerToken() {
    log.info "docker swarm join-token manager"
    def swarm = inspectSwarm().content
    return swarm.JoinTokens.Manager
  }

  @Override
  String rotateSwarmManagerToken() {
    log.info "docker swarm join-token rotate manager token"

    def swarm = inspectSwarm().content
    def updateResponse = updateSwarm(
        [
            "version"           : swarm.Version.Index,
            "rotateManagerToken": true
        ],
        swarm.Spec)
    log.info "rotate manager token: ${updateResponse.status}"
    return getSwarmManagerToken()
  }

  @Override
  String getSwarmManagerUnlockKey() {
    log.info "docker swarm manager unlock key"
    def response = client.get([path: "/swarm/unlockkey"])
    responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("get swarm manager unlock key failed"))
    return response.content.UnlockKey
  }

  @Override
  String rotateSwarmManagerUnlockKey() {
    log.info "docker swarm join-token rotate manager unlock key"

    def swarm = inspectSwarm().content
    def updateResponse = updateSwarm(
        [
            "version"               : swarm.Version.Index,
            "rotateManagerUnlockKey": true
        ],
        swarm.Spec)
    log.info "rotate manager unlock key: ${updateResponse.status}"
    return getSwarmManagerUnlockKey()
  }

  @Override
  EngineResponse unlockSwarm(String unlockKey) {
    log.info "docker swarm unlock"
    EngineResponse response = client.post([path              : "/swarm/unlock",
                                           body              : [UnlockKey: unlockKey],
                                           requestContentType: "application/json"])
    responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("unlock swarm failed"))
    return response
  }

  @Override
  EngineResponse inspectSwarm(Map query = [:]) {
    log.info "docker swarm inspect"
    def actualQuery = query ?: [:]
    queryUtil.jsonEncodeFilters(actualQuery)
    EngineResponse response = client.get([path : "/swarm",
                                          query: actualQuery])
    responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker swarm inspect failed"))
    return response
  }
}
