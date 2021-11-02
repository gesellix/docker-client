package de.gesellix.docker.client.swarm;

import de.gesellix.docker.engine.EngineResponse;

import java.util.Map;

public interface ManageSwarm {

  Map newSwarmConfig();

  EngineResponse initSwarm();

  EngineResponse initSwarm(Map config);

  EngineResponse joinSwarm(Map config);

  EngineResponse inspectSwarm();

  EngineResponse inspectSwarm(Map query);

  String getSwarmWorkerToken();

  String rotateSwarmWorkerToken();

  String getSwarmManagerToken();

  String rotateSwarmManagerToken();

  EngineResponse leaveSwarm();

  EngineResponse leaveSwarm(Map query);

  EngineResponse unlockSwarm(String unlockKey);

  String getSwarmManagerUnlockKey();

  String rotateSwarmManagerUnlockKey();

  EngineResponse updateSwarm(Map query, Map config);
}
