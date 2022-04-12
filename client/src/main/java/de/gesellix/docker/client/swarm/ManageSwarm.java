package de.gesellix.docker.client.swarm;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.remote.api.Swarm;
import de.gesellix.docker.remote.api.SwarmInitRequest;
import de.gesellix.docker.remote.api.SwarmJoinRequest;
import de.gesellix.docker.remote.api.SwarmSpec;

public interface ManageSwarm {

  SwarmInitRequest newSwarmInitRequest();

  EngineResponseContent<String> initSwarm();

  EngineResponseContent<String> initSwarm(SwarmInitRequest swarmInitRequest);

  void joinSwarm(SwarmJoinRequest swarmJoinRequest);

  EngineResponseContent<Swarm> inspectSwarm();

  String getSwarmWorkerToken();

  String rotateSwarmWorkerToken();

  String getSwarmManagerToken();

  String rotateSwarmManagerToken();

  void leaveSwarm();

  void leaveSwarm(Boolean force);

  void unlockSwarm(String unlockKey);

  String getSwarmManagerUnlockKey();

  String rotateSwarmManagerUnlockKey();

  void updateSwarm(long version, SwarmSpec spec);

  void updateSwarm(long version, SwarmSpec spec, Boolean rotateWorkerToken);

  void updateSwarm(long version, SwarmSpec spec, Boolean rotateWorkerToken, Boolean rotateManagerToken);

  void updateSwarm(long version, SwarmSpec spec, Boolean rotateWorkerToken, Boolean rotateManagerToken, Boolean rotateManagerUnlockKey);
}
