package de.gesellix.docker.client.swarm

import de.gesellix.docker.client.EngineResponseContent
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.Swarm
import de.gesellix.docker.remote.api.SwarmInitRequest
import de.gesellix.docker.remote.api.SwarmJoinRequest
import de.gesellix.docker.remote.api.SwarmSpec
import de.gesellix.docker.remote.api.SwarmUnlockRequest
import groovy.util.logging.Slf4j

@Slf4j
class ManageSwarmClient implements ManageSwarm {

  private EngineApiClient client

  ManageSwarmClient(EngineApiClient client) {
    this.client = client
  }

  @Override
  SwarmInitRequest newSwarmInitRequest() {
    return new SwarmInitRequest(
        "0.0.0.0:2377", null,
        null, null,
        null,
        false,
        null,
        null)
  }

  @Override
  EngineResponseContent<String> initSwarm() {
    return initSwarm(newSwarmInitRequest())
  }

  @Override
  EngineResponseContent<String> initSwarm(SwarmInitRequest swarmInitRequest) {
    log.info("docker swarm init")
    def nodeId = client.swarmApi.swarmInit(swarmInitRequest)
    return new EngineResponseContent<String>(nodeId)

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
  }

  @Override
  void joinSwarm(SwarmJoinRequest swarmJoinRequest) {
    log.info("docker swarm join")
    client.swarmApi.swarmJoin(swarmJoinRequest)
  }

  @Override
  void leaveSwarm(Boolean force = null) {
    log.info("docker swarm leave")
    client.swarmApi.swarmLeave(force)
  }

  @Override
  void updateSwarm(long version, SwarmSpec spec, Boolean rotateWorkerToken = null, Boolean rotateManagerToken = null, Boolean rotateManagerUnlockKey = null) {
    log.info("docker swarm update")
    client.swarmApi.swarmUpdate(version, spec, rotateWorkerToken, rotateManagerToken, rotateManagerUnlockKey)
  }

  @Override
  String getSwarmWorkerToken() {
    log.info("docker swarm join-token worker")
    def swarm = inspectSwarm().content
    return swarm.joinTokens.worker
  }

  @Override
  String rotateSwarmWorkerToken() {
    log.info("docker swarm join-token rotate worker token")
    def swarm = inspectSwarm().content
    client.swarmApi.swarmUpdate(swarm.version.index, swarm.spec, true, false, false)
    return getSwarmWorkerToken()
  }

  @Override
  String getSwarmManagerToken() {
    log.info("docker swarm join-token manager")
    def swarm = inspectSwarm().content
    return swarm.joinTokens.manager
  }

  @Override
  String rotateSwarmManagerToken() {
    log.info("docker swarm join-token rotate manager token")
    def swarm = inspectSwarm().content
    client.swarmApi.swarmUpdate(swarm.version.index, swarm.spec, false, true, false)
    return getSwarmManagerToken()
  }

  @Override
  String getSwarmManagerUnlockKey() {
    log.info("docker swarm manager unlock key")
    def unlockkey = client.swarmApi.swarmUnlockkey()
    return unlockkey.unlockKey
  }

  @Override
  String rotateSwarmManagerUnlockKey() {
    log.info("docker swarm join-token rotate manager unlock key")
    def swarm = inspectSwarm().content
    client.swarmApi.swarmUpdate(swarm.version.index, swarm.spec, false, false, true)
    return getSwarmManagerUnlockKey()
  }

  @Override
  void unlockSwarm(String unlockKey) {
    log.info("docker swarm unlock")
    client.swarmApi.swarmUnlock(new SwarmUnlockRequest(unlockKey))
  }

  @Override
  EngineResponseContent<Swarm> inspectSwarm() {
    log.info("docker swarm inspect")
    def swarmInspect = client.swarmApi.swarmInspect()
    return new EngineResponseContent<Swarm>(swarmInspect)
  }
}
