package de.gesellix.docker.client.system

import de.gesellix.docker.client.DockerAsyncCallback
import de.gesellix.docker.engine.EngineResponse

interface ManageSystem {

//    df          Show docker disk usage

  EngineResponse systemDf()

  EngineResponse systemDf(Map query)

//    events      Get real time events from the server

  EngineResponse events(DockerAsyncCallback callback)

  EngineResponse events(DockerAsyncCallback callback, Map query)

  EngineResponse ping()

  EngineResponse version()

//    info        Display system-wide information

  EngineResponse info()

//    TODO prune       Remove unused data
}
