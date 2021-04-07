package de.gesellix.docker.client.network

import de.gesellix.docker.engine.EngineResponse

interface ManageNetwork {

//    connect     Connect a container to a network

  EngineResponse connectNetwork(network, container)

//    create      Create a network

  EngineResponse createNetwork(name)

  EngineResponse createNetwork(name, config)

//    disconnect  Disconnect a container from a network

  EngineResponse disconnectNetwork(network, container)

//    inspect     Display detailed information on one or more networks

  EngineResponse inspectNetwork(name)

//    ls          List networks

  EngineResponse networks()

  EngineResponse networks(query)

//    prune       Remove all unused networks

  EngineResponse pruneNetworks()

  EngineResponse pruneNetworks(query)

//    rm          Remove one or more networks

  EngineResponse rmNetwork(name)
}
