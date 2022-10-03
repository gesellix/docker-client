package de.gesellix.docker.client.swarm;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.remote.api.EngineApiClient;
import de.gesellix.docker.remote.api.Swarm;
import de.gesellix.docker.remote.api.SwarmInitRequest;
import de.gesellix.docker.remote.api.SwarmJoinRequest;
import de.gesellix.docker.remote.api.SwarmSpec;
import de.gesellix.docker.remote.api.SwarmUnlockRequest;
import de.gesellix.docker.remote.api.UnlockKeyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManageSwarmClient implements ManageSwarm {

  private final Logger log = LoggerFactory.getLogger(ManageSwarmClient.class);
  private final EngineApiClient client;

  public ManageSwarmClient(EngineApiClient client) {
    this.client = client;
  }

  @Override
  public SwarmInitRequest newSwarmInitRequest() {
    return new SwarmInitRequest("0.0.0.0:2377", null, null, null, null, false, null, null);
  }

  @Override
  public EngineResponseContent<String> initSwarm() {
    return initSwarm(newSwarmInitRequest());
  }

  @Override
  public EngineResponseContent<String> initSwarm(SwarmInitRequest swarmInitRequest) {
    log.info("docker swarm init");
    String nodeId = client.getSwarmApi().swarmInit(swarmInitRequest);
    return new EngineResponseContent<>(nodeId);

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
  public void joinSwarm(SwarmJoinRequest swarmJoinRequest) {
    log.info("docker swarm join");
    client.getSwarmApi().swarmJoin(swarmJoinRequest);
  }

  @Override
  public void leaveSwarm(Boolean force) {
    log.info("docker swarm leave");
    client.getSwarmApi().swarmLeave(force);
  }

  @Override
  public void leaveSwarm() {
    leaveSwarm(null);
  }

  @Override
  public void updateSwarm(long version, SwarmSpec spec, Boolean rotateWorkerToken, Boolean rotateManagerToken, Boolean rotateManagerUnlockKey) {
    log.info("docker swarm update");
    client.getSwarmApi().swarmUpdate(version, spec, rotateWorkerToken, rotateManagerToken, rotateManagerUnlockKey);
  }

  @Override
  public void updateSwarm(long version, SwarmSpec spec, Boolean rotateWorkerToken, Boolean rotateManagerToken) {
    updateSwarm(version, spec, rotateWorkerToken, rotateManagerToken, null);
  }

  @Override
  public void updateSwarm(long version, SwarmSpec spec, Boolean rotateWorkerToken) {
    updateSwarm(version, spec, rotateWorkerToken, null, null);
  }

  @Override
  public void updateSwarm(long version, SwarmSpec spec) {
    updateSwarm(version, spec, null, null, null);
  }

  @Override
  public String getSwarmWorkerToken() {
    log.info("docker swarm join-token worker");
    Swarm swarm = inspectSwarm().getContent();
    return swarm.getJoinTokens().getWorker();
  }

  @Override
  public String rotateSwarmWorkerToken() {
    log.info("docker swarm join-token rotate worker token");
    Swarm swarm = inspectSwarm().getContent();
    client.getSwarmApi().swarmUpdate(swarm.getVersion().getIndex(), swarm.getSpec(), true, false, false);
    return getSwarmWorkerToken();
  }

  @Override
  public String getSwarmManagerToken() {
    log.info("docker swarm join-token manager");
    Swarm swarm = inspectSwarm().getContent();
    return swarm.getJoinTokens().getManager();
  }

  @Override
  public String rotateSwarmManagerToken() {
    log.info("docker swarm join-token rotate manager token");
    Swarm swarm = inspectSwarm().getContent();
    client.getSwarmApi().swarmUpdate(swarm.getVersion().getIndex(), swarm.getSpec(), false, true, false);
    return getSwarmManagerToken();
  }

  @Override
  public String getSwarmManagerUnlockKey() {
    log.info("docker swarm manager unlock key");
    UnlockKeyResponse unlockkey = client.getSwarmApi().swarmUnlockkey();
    return unlockkey.getUnlockKey();
  }

  @Override
  public String rotateSwarmManagerUnlockKey() {
    log.info("docker swarm join-token rotate manager unlock key");
    Swarm swarm = inspectSwarm().getContent();
    client.getSwarmApi().swarmUpdate(swarm.getVersion().getIndex(), swarm.getSpec(), false, false, true);
    return getSwarmManagerUnlockKey();
  }

  @Override
  public void unlockSwarm(String unlockKey) {
    log.info("docker swarm unlock");
    client.getSwarmApi().swarmUnlock(new SwarmUnlockRequest(unlockKey));
  }

  @Override
  public EngineResponseContent<Swarm> inspectSwarm() {
    log.info("docker swarm inspect");
    Swarm swarmInspect = client.getSwarmApi().swarmInspect();
    return new EngineResponseContent<>(swarmInspect);
  }
}
