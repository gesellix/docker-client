package de.gesellix.docker.client.swarm

import de.gesellix.docker.engine.EngineResponse

interface ManageSwarm {

  Map newSwarmConfig()

//    init        Initialize a swarm

  EngineResponse initSwarm()

  EngineResponse initSwarm(Map config)

//    join        Join a swarm as a node and/or manager

  EngineResponse joinSwarm(Map config)

//    join-token  Manage join tokens

  EngineResponse inspectSwarm()

  EngineResponse inspectSwarm(Map query)

  String getSwarmWorkerToken()

  String rotateSwarmWorkerToken()

  String getSwarmManagerToken()

  String rotateSwarmManagerToken()

//    leave       Leave the swarm

  EngineResponse leaveSwarm()

  EngineResponse leaveSwarm(Map query)

//    unlock      Unlock swarm

  EngineResponse unlockSwarm(String unlockKey)

//    unlock-key  Manage the unlock key

  String getSwarmManagerUnlockKey()

  String rotateSwarmManagerUnlockKey()

//    update      Update the swarm

  EngineResponse updateSwarm(Map query, Map config)
}
